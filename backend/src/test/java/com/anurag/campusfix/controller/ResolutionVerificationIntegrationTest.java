package com.anurag.campusfix.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
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
import com.anurag.campusfix.entity.VerificationStatus;
import com.anurag.campusfix.jwt.JwtService;
import com.anurag.campusfix.repository.ComplaintHistoryRepository;
import com.anurag.campusfix.repository.ComplaintImageRepository;
import com.anurag.campusfix.repository.ComplaintRepository;
import com.anurag.campusfix.repository.ComplaintVerificationRepository;
import com.anurag.campusfix.repository.UserRepository;
import com.anurag.campusfix.service.ComplaintVerificationService;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class ResolutionVerificationIntegrationTest {

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

    @Autowired
    private ComplaintVerificationService verificationService;

    @Value("${app.complaint.upload.dir:target/test-uploads}")
    private String uploadDirPath;

    private MockMvc mockMvc;

    private UserEntity studentReporter;
    private UserEntity otherStudent;
    private UserEntity assignedStaff;
    private UserEntity otherStaff;
    private UserEntity admin;

    private String reporterToken;
    private String otherStudentToken;
    private String assignedStaffToken;
    private String otherStaffToken;
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

        studentReporter = createUser("Alice Student", "student@campus.edu", Role.STUDENT);
        otherStudent = createUser("Bob Student", "otherstudent@campus.edu", Role.STUDENT);
        assignedStaff = createUser("Charlie Staff", "staff@campus.edu", Role.MAINTENANCE_STAFF);
        otherStaff = createUser("Dave Staff", "otherstaff@campus.edu", Role.MAINTENANCE_STAFF);
        admin = createUser("Diana Admin", "admin@campus.edu", Role.ADMIN);

        reporterToken = jwtService.generateToken(studentReporter.getEmail(), studentReporter.getRole().name());
        otherStudentToken = jwtService.generateToken(otherStudent.getEmail(), otherStudent.getRole().name());
        assignedStaffToken = jwtService.generateToken(assignedStaff.getEmail(), assignedStaff.getRole().name());
        otherStaffToken = jwtService.generateToken(otherStaff.getEmail(), otherStaff.getRole().name());
        adminToken = jwtService.generateToken(admin.getEmail(), admin.getRole().name());
    }

    @AfterEach
    void tearDown() {
        File uploadDir = new File(uploadDirPath);
        if (uploadDir.exists()) {
            try {
                Files.walk(uploadDir.toPath())
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (Exception ignored) {
            }
        }
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

    private ComplaintEntity createComplaint(UserEntity reporter, UserEntity staff, ComplaintStatus status) {
        ComplaintEntity complaint = new ComplaintEntity();
        complaint.setReferenceNumber("CMP-VER-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        complaint.setTitle("Leaking laboratory pipe");
        complaint.setDescription("Water dripping near computer terminals in lab 2");
        complaint.setCategory(ComplaintCategory.PLUMBING);
        complaint.setPriority(ComplaintPriority.HIGH);
        complaint.setStatus(status);
        complaint.setBuilding("Engineering Block A");
        complaint.setFloor("2nd Floor");
        complaint.setRoom("Lab 204");
        complaint.setReporter(reporter);
        complaint.setAssignedStaff(staff);
        if (status == ComplaintStatus.RESOLVED) {
            complaint.setResolvedAt(LocalDateTime.now().minusHours(2));
            complaint.setVerificationStatus(VerificationStatus.PENDING);
            complaint.setResolutionNotes("Initial repair completed");
        }
        return complaintRepository.save(complaint);
    }

    @Test
    @DisplayName("POST /api/complaints/{id}/resolution (JSON): Assigned staff successfully resolves complaint")
    void submitResolution_Success_Json() throws Exception {
        ComplaintEntity complaint = createComplaint(studentReporter, assignedStaff, ComplaintStatus.IN_PROGRESS);

        String payload = """
                {
                    "resolutionNotes": "Replaced valve assembly and tested water pressure under 4 bar load."
                }
                """;

        mockMvc.perform(post("/api/complaints/{id}/resolution", complaint.getId())
                        .header("Authorization", "Bearer " + assignedStaffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.complaintId").value(complaint.getId()))
                .andExpect(jsonPath("$.complaintStatus").value("RESOLVED"))
                .andExpect(jsonPath("$.verificationStatus").value("PENDING"))
                .andExpect(jsonPath("$.cycleNumber").value(1))
                .andExpect(jsonPath("$.resolutionNotes", containsString("Replaced valve assembly")));

        ComplaintEntity updated = complaintRepository.findById(complaint.getId()).orElseThrow();
        assertEquals(ComplaintStatus.RESOLVED, updated.getStatus());
        assertEquals(VerificationStatus.PENDING, updated.getVerificationStatus());
        assertNotNull(updated.getResolvedAt());
    }

    @Test
    @DisplayName("POST /api/complaints/{id}/resolution (Multipart): Assigned staff resolves with after-repair image")
    void submitResolution_Success_Multipart_WithImage() throws Exception {
        ComplaintEntity complaint = createComplaint(studentReporter, assignedStaff, ComplaintStatus.IN_PROGRESS);

        // Valid PNG header (8 bytes magic) + dummy bytes
        byte[] validPngBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D};
        MockMultipartFile imageFile = new MockMultipartFile(
                "image", "repair_done.png", "image/png", validPngBytes);

        mockMvc.perform(multipart("/api/complaints/{id}/resolution", complaint.getId())
                        .file(imageFile)
                        .param("resolutionNotes", "Installed brand new copper pipe and sealed joint.")
                        .header("Authorization", "Bearer " + assignedStaffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.complaintStatus").value("RESOLVED"))
                .andExpect(jsonPath("$.verificationStatus").value("PENDING"))
                .andExpect(jsonPath("$.afterImageUrl", notNullValue()));

        // Verify image download is accessible for reporter
        mockMvc.perform(get("/api/complaints/{id}/verification/after-image", complaint.getId())
                        .header("Authorization", "Bearer " + reporterToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG_VALUE));
    }

    @Test
    @DisplayName("POST /api/complaints/{id}/resolution: Unassigned staff is forbidden (403)")
    void submitResolution_Forbidden_WhenNotAssigned() throws Exception {
        ComplaintEntity complaint = createComplaint(studentReporter, assignedStaff, ComplaintStatus.IN_PROGRESS);

        String payload = """
                {
                    "resolutionNotes": "Attempted by wrong staff"
                }
                """;

        mockMvc.perform(post("/api/complaints/{id}/resolution", complaint.getId())
                        .header("Authorization", "Bearer " + otherStaffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/complaints/{id}/resolution: Fails with 400 when resolution notes are blank")
    void submitResolution_BadRequest_WhenNotesBlank() throws Exception {
        ComplaintEntity complaint = createComplaint(studentReporter, assignedStaff, ComplaintStatus.IN_PROGRESS);

        String payload = """
                {
                    "resolutionNotes": "   "
                }
                """;

        mockMvc.perform(post("/api/complaints/{id}/resolution", complaint.getId())
                        .header("Authorization", "Bearer " + assignedStaffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/complaints/{id}/verification: Reporter confirms repair -> CLOSED and VERIFIED")
    void verifyResolution_Confirm_Success() throws Exception {
        ComplaintEntity complaint = createComplaint(studentReporter, assignedStaff, ComplaintStatus.IN_PROGRESS);

        // Staff resolves first
        String resolvePayload = """
                {"resolutionNotes": "Fixed wiring defect"}
                """;
        mockMvc.perform(post("/api/complaints/{id}/resolution", complaint.getId())
                        .header("Authorization", "Bearer " + assignedStaffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resolvePayload))
                .andExpect(status().isOk());

        // Reporter confirms
        String verifyPayload = """
                {
                    "decision": "VERIFIED",
                    "comment": "Checked lab switchboard. Everything works smoothly."
                }
                """;

        mockMvc.perform(post("/api/complaints/{id}/verification", complaint.getId())
                        .header("Authorization", "Bearer " + reporterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verifyPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.complaintStatus").value("CLOSED"))
                .andExpect(jsonPath("$.verificationStatus").value("VERIFIED"))
                .andExpect(jsonPath("$.verifiedBy.name").value("Alice Student"))
                .andExpect(jsonPath("$.comment", containsString("Checked lab switchboard")));

        ComplaintEntity updated = complaintRepository.findById(complaint.getId()).orElseThrow();
        assertEquals(ComplaintStatus.CLOSED, updated.getStatus());
        assertEquals(VerificationStatus.VERIFIED, updated.getVerificationStatus());
    }

    @Test
    @DisplayName("POST /api/complaints/{id}/verification: Reporter rejects repair -> REOPENED and REJECTED")
    void verifyResolution_Reject_Success_ReopensComplaint() throws Exception {
        ComplaintEntity complaint = createComplaint(studentReporter, assignedStaff, ComplaintStatus.IN_PROGRESS);

        // Staff resolves
        mockMvc.perform(post("/api/complaints/{id}/resolution", complaint.getId())
                        .header("Authorization", "Bearer " + assignedStaffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resolutionNotes\": \"Replaced fuse\"}"))
                .andExpect(status().isOk());

        // Reporter rejects with reason
        String rejectPayload = """
                {
                    "decision": "REJECTED",
                    "comment": "Switchboard sparks when flipped. Defect still persists."
                }
                """;

        mockMvc.perform(post("/api/complaints/{id}/verification", complaint.getId())
                        .header("Authorization", "Bearer " + reporterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rejectPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.complaintStatus").value("REOPENED"))
                .andExpect(jsonPath("$.verificationStatus").value("REJECTED"))
                .andExpect(jsonPath("$.comment", containsString("Switchboard sparks")));

        ComplaintEntity reopened = complaintRepository.findById(complaint.getId()).orElseThrow();
        assertEquals(ComplaintStatus.REOPENED, reopened.getStatus());
        assertEquals(VerificationStatus.REJECTED, reopened.getVerificationStatus());

        // Subsequent resolution starts Cycle #2 without deleting cycle #1!
        mockMvc.perform(post("/api/complaints/{id}/resolution", complaint.getId())
                        .header("Authorization", "Bearer " + assignedStaffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resolutionNotes\": \"Replaced entire circuit breaker assembly.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cycleNumber").value(2))
                .andExpect(jsonPath("$.complaintStatus").value("RESOLVED"))
                .andExpect(jsonPath("$.verificationStatus").value("PENDING"));

        // Verify history contains both cycles
        mockMvc.perform(get("/api/complaints/{id}/verification-history", complaint.getId())
                        .header("Authorization", "Bearer " + reporterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].cycleNumber").value(1))
                .andExpect(jsonPath("$[0].verificationStatus").value("REJECTED"))
                .andExpect(jsonPath("$[1].cycleNumber").value(2))
                .andExpect(jsonPath("$[1].verificationStatus").value("PENDING"));
    }

    @Test
    @DisplayName("POST /api/complaints/{id}/verification: Staff cannot verify their own resolution (403)")
    void verifyResolution_Forbidden_StaffSelfVerification() throws Exception {
        ComplaintEntity complaint = createComplaint(studentReporter, assignedStaff, ComplaintStatus.IN_PROGRESS);

        mockMvc.perform(post("/api/complaints/{id}/resolution", complaint.getId())
                        .header("Authorization", "Bearer " + assignedStaffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resolutionNotes\": \"Fixed pipe\"}"))
                .andExpect(status().isOk());

        String verifyPayload = """
                {"decision": "VERIFIED", "comment": "I verified my own fix"}
                """;

        mockMvc.perform(post("/api/complaints/{id}/verification", complaint.getId())
                        .header("Authorization", "Bearer " + assignedStaffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verifyPayload))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/complaints/{id}/verification: Other student cannot verify (403)")
    void verifyResolution_Forbidden_OtherStudent() throws Exception {
        ComplaintEntity complaint = createComplaint(studentReporter, assignedStaff, ComplaintStatus.IN_PROGRESS);

        mockMvc.perform(post("/api/complaints/{id}/resolution", complaint.getId())
                        .header("Authorization", "Bearer " + assignedStaffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resolutionNotes\": \"Fixed pipe\"}"))
                .andExpect(status().isOk());

        String verifyPayload = """
                {"decision": "VERIFIED", "comment": "Other student approving"}
                """;

        mockMvc.perform(post("/api/complaints/{id}/verification", complaint.getId())
                        .header("Authorization", "Bearer " + otherStudentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verifyPayload))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/complaints/{id}/verification: Re-verifying completed cycle throws 409 Conflict")
    void verifyResolution_Conflict_WhenReverifyingAlreadyVerifiedCycle() throws Exception {
        ComplaintEntity complaint = createComplaint(studentReporter, assignedStaff, ComplaintStatus.IN_PROGRESS);

        mockMvc.perform(post("/api/complaints/{id}/resolution", complaint.getId())
                        .header("Authorization", "Bearer " + assignedStaffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resolutionNotes\": \"Fixed pipe\"}"))
                .andExpect(status().isOk());

        String verifyPayload = """
                {"decision": "VERIFIED", "comment": "Approved"}
                """;

        // First verification succeeds
        mockMvc.perform(post("/api/complaints/{id}/verification", complaint.getId())
                        .header("Authorization", "Bearer " + reporterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verifyPayload))
                .andExpect(status().isOk());

        // Repeat verification yields 409 Conflict (or invalid status transition 409)
        mockMvc.perform(post("/api/complaints/{id}/verification", complaint.getId())
                        .header("Authorization", "Bearer " + reporterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verifyPayload))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/complaints/{id}/verification: Rejecting without comment throws 400 Bad Request")
    void verifyResolution_BadRequest_WhenRejectWithoutReason() throws Exception {
        ComplaintEntity complaint = createComplaint(studentReporter, assignedStaff, ComplaintStatus.IN_PROGRESS);

        mockMvc.perform(post("/api/complaints/{id}/resolution", complaint.getId())
                        .header("Authorization", "Bearer " + assignedStaffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resolutionNotes\": \"Fixed pipe\"}"))
                .andExpect(status().isOk());

        String rejectPayload = """
                {"decision": "REJECTED", "comment": "   "}
                """;

        mockMvc.perform(post("/api/complaints/{id}/verification", complaint.getId())
                        .header("Authorization", "Bearer " + reporterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rejectPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/admin/complaints/{id}/verification/override: Admin override succeeds when enabled after delay")
    void adminOverride_Success_WhenConfigured() throws Exception {
        ComplaintEntity complaint = createComplaint(studentReporter, assignedStaff, ComplaintStatus.IN_PROGRESS);

        mockMvc.perform(post("/api/complaints/{id}/resolution", complaint.getId())
                        .header("Authorization", "Bearer " + assignedStaffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resolutionNotes\": \"Fixed pipe\"}"))
                .andExpect(status().isOk());

        // Enable override with 0 delay for test
        verificationService.setAdminOverrideEnabled(true);
        verificationService.setAdminOverrideDelayHours(0);

        String overridePayload = """
                {
                    "decision": "VERIFIED",
                    "reason": "Administrative closure after student verbal confirmation over phone."
                }
                """;

        mockMvc.perform(post("/api/admin/complaints/{id}/verification/override", complaint.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(overridePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.complaintStatus").value("CLOSED"))
                .andExpect(jsonPath("$.verificationStatus").value("VERIFIED"))
                .andExpect(jsonPath("$.overriddenByAdmin").value(true))
                .andExpect(jsonPath("$.verifiedBy.name").value("Diana Admin"));
    }

    @Test
    @DisplayName("POST /api/complaints/{id}/reopen: Explicit reopen endpoint returns 200 OK")
    void reopenComplaint_ExplicitEndpoint_Success() throws Exception {
        ComplaintEntity complaint = createComplaint(studentReporter, assignedStaff, ComplaintStatus.IN_PROGRESS);

        mockMvc.perform(post("/api/complaints/{id}/resolution", complaint.getId())
                        .header("Authorization", "Bearer " + assignedStaffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resolutionNotes\": \"Fixed pipe\"}"))
                .andExpect(status().isOk());

        String reopenPayload = """
                {"reason": "Issue has reoccurred immediately"}
                """;

        mockMvc.perform(post("/api/complaints/{id}/reopen", complaint.getId())
                        .header("Authorization", "Bearer " + reporterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reopenPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REOPENED"));
    }
}
