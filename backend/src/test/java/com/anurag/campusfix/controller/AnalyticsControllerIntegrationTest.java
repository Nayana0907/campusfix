package com.anurag.campusfix.controller;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.anurag.campusfix.entity.ComplaintCategory;
import com.anurag.campusfix.entity.ComplaintEntity;
import com.anurag.campusfix.entity.ComplaintPriority;
import com.anurag.campusfix.entity.ComplaintStatus;
import com.anurag.campusfix.entity.ComplaintVerificationEntity;
import com.anurag.campusfix.entity.Role;
import com.anurag.campusfix.entity.UserEntity;
import com.anurag.campusfix.entity.VerificationStatus;
import com.anurag.campusfix.jwt.JwtService;
import com.anurag.campusfix.repository.ComplaintHistoryRepository;
import com.anurag.campusfix.repository.ComplaintImageRepository;
import com.anurag.campusfix.repository.ComplaintRepository;
import com.anurag.campusfix.repository.ComplaintVerificationRepository;
import com.anurag.campusfix.repository.UserRepository;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class AnalyticsControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private ComplaintVerificationRepository complaintVerificationRepository;

    @Autowired
    private ComplaintHistoryRepository complaintHistoryRepository;

    @Autowired
    private ComplaintImageRepository complaintImageRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private MockMvc mockMvc;

    private UserEntity student;
    private UserEntity staff;
    private UserEntity admin;

    private String studentToken;
    private String staffToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        complaintVerificationRepository.deleteAll();
        complaintImageRepository.deleteAll();
        complaintHistoryRepository.deleteAll();
        complaintRepository.deleteAll();
        userRepository.deleteAll();

        student = createUser("Alice Student", "student@campus.edu", Role.STUDENT);
        staff = createUser("Charlie Staff", "staff@campus.edu", Role.MAINTENANCE_STAFF);
        admin = createUser("Diana Admin", "admin@campus.edu", Role.ADMIN);

        studentToken = jwtService.generateToken(student.getEmail(), student.getRole().name());
        staffToken = jwtService.generateToken(staff.getEmail(), staff.getRole().name());
        adminToken = jwtService.generateToken(admin.getEmail(), admin.getRole().name());
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        complaintVerificationRepository.deleteAll();
        complaintImageRepository.deleteAll();
        complaintHistoryRepository.deleteAll();
        complaintRepository.deleteAll();
        userRepository.deleteAll();
    }

    private UserEntity createUser(String name, String email, Role role) {
        UserEntity user = new UserEntity();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("Test@1234"));
        user.setRole(role);
        user.setIsActive(true);
        return userRepository.save(user);
    }

    private ComplaintEntity createComplaint(
            String title, ComplaintCategory category, ComplaintPriority priority,
            ComplaintStatus status, String building, String floor, String room,
            UserEntity reporter, UserEntity staffMember,
            LocalDateTime createdAt, LocalDateTime resolvedAt, VerificationStatus verificationStatus) {

        ComplaintEntity c = new ComplaintEntity();
        c.setReferenceNumber("CMP-AN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        c.setTitle(title);
        c.setDescription("Description for " + title);
        c.setCategory(category);
        c.setPriority(priority);
        c.setStatus(status);
        c.setBuilding(building);
        c.setFloor(floor);
        c.setRoom(room);
        c.setReporter(reporter);
        c.setAssignedStaff(staffMember);
        c.setCreatedAt(createdAt);
        c.setResolvedAt(resolvedAt);
        c.setVerificationStatus(verificationStatus);
        if (status == ComplaintStatus.CLOSED) {
            c.setVerifiedAt(resolvedAt != null ? resolvedAt.plusHours(1) : LocalDateTime.now());
            c.setVerifiedBy(reporter);
        }
        ComplaintEntity saved = complaintRepository.save(c);

        if (resolvedAt != null) {
            ComplaintVerificationEntity cycle = new ComplaintVerificationEntity(
                    saved, 1, resolvedAt, "Repair completed", null);
            cycle.setVerificationStatus(verificationStatus != null ? verificationStatus : VerificationStatus.PENDING);
            if (status == ComplaintStatus.CLOSED) {
                cycle.setVerifiedAt(saved.getVerifiedAt());
                cycle.setVerifiedBy(reporter);
            }
            complaintVerificationRepository.save(cycle);
        }

        return saved;
    }

    @Test
    @DisplayName("Security: Unauthenticated access is denied with 401 Unauthorized")
    void security_Unauthenticated_Denied() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/summary"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Security: Student access is denied with 403 Forbidden")
    void security_Student_Denied() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/summary")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security: Maintenance staff access is denied with 403 Forbidden")
    void security_Staff_Denied() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/staff-performance")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/admin/analytics/summary: returns real calculated counts for admin")
    void getSummary_Success() throws Exception {
        LocalDateTime now = LocalDateTime.now();

        // 1. SUBMITTED
        createComplaint("Tap leaking", ComplaintCategory.PLUMBING, ComplaintPriority.LOW,
                ComplaintStatus.SUBMITTED, "Block A", "1st Floor", "Room 101", student, null, now.minusHours(5), null, null);

        // 2. IN_PROGRESS
        createComplaint("Light flickering", ComplaintCategory.ELECTRICAL, ComplaintPriority.HIGH,
                ComplaintStatus.IN_PROGRESS, "Block A", "1st Floor", "Room 101", student, staff, now.minusHours(2), null, null);

        // 3. RESOLVED / Awaiting verification
        createComplaint("Broken chair", ComplaintCategory.FURNITURE, ComplaintPriority.MEDIUM,
                ComplaintStatus.RESOLVED, "Block B", "2nd Floor", "Room 201", student, staff, now.minusHours(10), now.minusHours(2), VerificationStatus.PENDING);

        // 4. CLOSED
        createComplaint("WiFi down", ComplaintCategory.WIFI, ComplaintPriority.URGENT,
                ComplaintStatus.CLOSED, "Block B", "2nd Floor", "Room 202", student, staff, now.minusHours(20), now.minusHours(5), VerificationStatus.VERIFIED);

        mockMvc.perform(get("/api/admin/analytics/summary")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalComplaints").value(4))
                .andExpect(jsonPath("$.newlySubmitted").value(1))
                .andExpect(jsonPath("$.inProgress").value(1))
                .andExpect(jsonPath("$.resolved").value(1))
                .andExpect(jsonPath("$.awaitingVerification").value(1))
                .andExpect(jsonPath("$.closed").value(1))
                .andExpect(jsonPath("$.timezone").value("UTC"));
    }

    @Test
    @DisplayName("GET /api/admin/analytics/monthly: returns monthly time series")
    void getMonthlyTrends_Success() throws Exception {
        LocalDateTime now = LocalDateTime.now();

        createComplaint("Projector glitch", ComplaintCategory.EQUIPMENT, ComplaintPriority.MEDIUM,
                ComplaintStatus.CLOSED, "Block C", "3rd Floor", "Room 301", student, staff, now.minusDays(5), now.minusDays(2), VerificationStatus.VERIFIED);

        mockMvc.perform(get("/api/admin/analytics/monthly")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interval").value("MONTH"))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("GET /api/admin/analytics/categories: returns distribution for all categories & priorities")
    void getCategories_Success() throws Exception {
        LocalDateTime now = LocalDateTime.now();

        createComplaint("AC defective", ComplaintCategory.EQUIPMENT, ComplaintPriority.HIGH,
                ComplaintStatus.SUBMITTED, "Block A", "1st Floor", "Room 101", student, null, now, null, null);

        mockMvc.perform(get("/api/admin/analytics/categories")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryCounts", hasSize(ComplaintCategory.values().length)))
                .andExpect(jsonPath("$.priorityCounts", hasSize(ComplaintPriority.values().length)))
                .andExpect(jsonPath("$.mostFrequentCategories[0].category").value("EQUIPMENT"))
                .andExpect(jsonPath("$.mostFrequentCategories[0].count").value(1));
    }

    @Test
    @DisplayName("GET /api/admin/analytics/locations: detects recurring location defect spots")
    void getLocations_Success() throws Exception {
        LocalDateTime now = LocalDateTime.now();

        // 2 issues in same room -> recurring!
        createComplaint("Tap drip", ComplaintCategory.PLUMBING, ComplaintPriority.LOW,
                ComplaintStatus.CLOSED, "Engineering Block", "Ground Floor", "Lab 1", student, staff, now, now, VerificationStatus.VERIFIED);
        createComplaint("Pipe crack", ComplaintCategory.PLUMBING, ComplaintPriority.MEDIUM,
                ComplaintStatus.IN_PROGRESS, "Engineering Block", "Ground Floor", "Lab 1", student, staff, now, null, null);

        mockMvc.perform(get("/api/admin/analytics/locations")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buildingCounts[0].building").value("Engineering Block"))
                .andExpect(jsonPath("$.buildingCounts[0].count").value(2))
                .andExpect(jsonPath("$.recurringLocations", hasSize(1)))
                .andExpect(jsonPath("$.recurringLocations[0].room").value("Lab 1"))
                .andExpect(jsonPath("$.recurringLocations[0].complaintCount").value(2));
    }

    @Test
    @DisplayName("GET /api/admin/analytics/resolution-performance: returns real duration and completion rate metrics")
    void getResolutionPerformance_Success() throws Exception {
        LocalDateTime now = LocalDateTime.now();

        createComplaint("Router reset", ComplaintCategory.WIFI, ComplaintPriority.HIGH,
                ComplaintStatus.CLOSED, "Hostel 1", "1st Floor", "Room 12", student, staff, now.minusHours(6), now.minusHours(2), VerificationStatus.VERIFIED);

        mockMvc.perform(get("/api/admin/analytics/resolution-performance")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalResolvedInScope").value(1))
                .andExpect(jsonPath("$.totalClosedInScope").value(1))
                .andExpect(jsonPath("$.verifiedAndClosedRate").value(100.0))
                .andExpect(jsonPath("$.resolutionSampleSize").value(1))
                .andExpect(jsonPath("$.averageSubmissionToResolutionHours").value(notNullValue()));
    }

    @Test
    @DisplayName("GET /api/admin/analytics/staff-performance: returns staff metrics with unconfigured feedback safeguards")
    void getStaffPerformance_Success() throws Exception {
        LocalDateTime now = LocalDateTime.now();

        createComplaint("Socket replacement", ComplaintCategory.ELECTRICAL, ComplaintPriority.MEDIUM,
                ComplaintStatus.IN_PROGRESS, "Main Library", "2nd Floor", "Stack A", student, staff, now, null, null);

        mockMvc.perform(get("/api/admin/analytics/staff-performance")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].staffId").value(staff.getId()))
                .andExpect(jsonPath("$.items[0].staffName").value("Charlie Staff"))
                .andExpect(jsonPath("$.items[0].assignedCount").value(1))
                .andExpect(jsonPath("$.items[0].inProgressCount").value(1))
                .andExpect(jsonPath("$.items[0].feedbackStatus").value("UNAVAILABLE"))
                .andExpect(jsonPath("$.items[0].averageRating").doesNotExist());
    }

    @Test
    @DisplayName("Validation: Inverted date range (from > to) returns 400 Bad Request")
    void dateValidation_InvertedRange_BadRequest() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/summary")
                        .param("from", "2026-12-31")
                        .param("to", "2026-01-01")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("must not be after")));
    }
}
