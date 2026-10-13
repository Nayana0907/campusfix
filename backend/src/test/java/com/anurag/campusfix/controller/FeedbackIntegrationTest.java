package com.anurag.campusfix.controller;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.anurag.campusfix.entity.ComplaintCategory;
import com.anurag.campusfix.entity.ComplaintEntity;
import com.anurag.campusfix.entity.ComplaintPriority;
import com.anurag.campusfix.entity.ComplaintStatus;
import com.anurag.campusfix.entity.Role;
import com.anurag.campusfix.entity.UserEntity;
import com.anurag.campusfix.jwt.JwtService;
import com.anurag.campusfix.repository.ComplaintRepository;
import com.anurag.campusfix.repository.FeedbackRepository;
import com.anurag.campusfix.repository.UserRepository;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class FeedbackIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;
    private UserEntity student;
    private UserEntity otherStudent;
    private UserEntity admin;
    private UserEntity staff;
    private ComplaintEntity resolvedComplaint;
    private ComplaintEntity pendingComplaint;
    private String studentToken;
    private String otherStudentToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        student = new UserEntity(
                "Feedback Student",
                "fb_student@campusfix.edu",
                passwordEncoder.encode("Password123"),
                "9876543200",
                Role.STUDENT
        );
        student = userRepository.save(student);

        otherStudent = new UserEntity(
                "Other Student",
                "fb_other@campusfix.edu",
                passwordEncoder.encode("Password123"),
                "9876543201",
                Role.STUDENT
        );
        otherStudent = userRepository.save(otherStudent);

        admin = new UserEntity(
                "Feedback Admin",
                "fb_admin@campusfix.edu",
                passwordEncoder.encode("Password123"),
                "9876543202",
                Role.ADMIN
        );
        admin = userRepository.save(admin);

        staff = new UserEntity(
                "Tech Dave",
                "fb_tech@campusfix.edu",
                passwordEncoder.encode("Password123"),
                "9876543203",
                Role.MAINTENANCE_STAFF
        );
        staff = userRepository.save(staff);

        resolvedComplaint = new ComplaintEntity();
        resolvedComplaint.setReferenceNumber("CMP-FB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        resolvedComplaint.setTitle("Resolved laboratory leak");
        resolvedComplaint.setDescription("Water leak repaired by technician");
        resolvedComplaint.setCategory(ComplaintCategory.PLUMBING);
        resolvedComplaint.setPriority(ComplaintPriority.MEDIUM);
        resolvedComplaint.setStatus(ComplaintStatus.RESOLVED);
        resolvedComplaint.setBuilding("Science Block");
        resolvedComplaint.setFloor("2nd");
        resolvedComplaint.setRoom("Lab 204");
        resolvedComplaint.setReporter(student);
        resolvedComplaint.setResolvedAt(LocalDateTime.now().minusHours(1));
        resolvedComplaint = complaintRepository.save(resolvedComplaint);

        pendingComplaint = new ComplaintEntity();
        pendingComplaint.setReferenceNumber("CMP-FB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        pendingComplaint.setTitle("Broken desk in classroom");
        pendingComplaint.setDescription("Wood cracked");
        pendingComplaint.setCategory(ComplaintCategory.FURNITURE);
        pendingComplaint.setPriority(ComplaintPriority.LOW);
        pendingComplaint.setStatus(ComplaintStatus.SUBMITTED);
        pendingComplaint.setBuilding("Academic Block");
        pendingComplaint.setFloor("1st");
        pendingComplaint.setRoom("Room 101");
        pendingComplaint.setReporter(student);
        pendingComplaint = complaintRepository.save(pendingComplaint);

        studentToken = jwtService.generateToken(student.getEmail(), student.getRole().name());
        otherStudentToken = jwtService.generateToken(otherStudent.getEmail(), otherStudent.getRole().name());
        adminToken = jwtService.generateToken(admin.getEmail(), admin.getRole().name());
    }

    @AfterEach
    void tearDown() {
        feedbackRepository.deleteAll();
        complaintRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/complaints/{id}/feedback should allow reporter to submit rating and comments")
    void testSubmitFeedbackSuccess() throws Exception {
        String payload = """
                {
                    "rating": 5,
                    "comment": "Fixed very fast and clean!"
                }
                """;

        mockMvc.perform(post("/api/complaints/" + resolvedComplaint.getId() + "/feedback")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.complaintId").value(resolvedComplaint.getId()))
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.comment").value("Fixed very fast and clean!"))
                .andExpect(jsonPath("$.studentName").value("Feedback Student"));

        // GET feedback should return the recorded feedback
        mockMvc.perform(get("/api/complaints/" + resolvedComplaint.getId() + "/feedback")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(5));
    }

    @Test
    @DisplayName("POST /api/complaints/{id}/feedback should reject feedback if complaint is not resolved or closed")
    void testCannotSubmitFeedbackOnUnresolvedComplaint() throws Exception {
        String payload = """
                {
                    "rating": 4,
                    "comment": "Still waiting..."
                }
                """;

        mockMvc.perform(post("/api/complaints/" + pendingComplaint.getId() + "/feedback")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/complaints/{id}/feedback should reject feedback from non-reporter student")
    void testNonReporterCannotSubmitFeedback() throws Exception {
        String payload = """
                {
                    "rating": 3,
                    "comments": "Not my complaint"
                }
                """;

        mockMvc.perform(post("/api/complaints/" + resolvedComplaint.getId() + "/feedback")
                        .header("Authorization", "Bearer " + otherStudentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/admin/complaints/staff should return active maintenance staff members")
    void testGetActiveStaffList() throws Exception {
        mockMvc.perform(get("/api/admin/complaints/staff")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Tech Dave"))
                .andExpect(jsonPath("$[0].role").value("MAINTENANCE_STAFF"));
    }
}
