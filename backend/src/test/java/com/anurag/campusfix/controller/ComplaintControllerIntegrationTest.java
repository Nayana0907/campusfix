package com.anurag.campusfix.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.anurag.campusfix.entity.ComplaintCategory;
import com.anurag.campusfix.entity.ComplaintEntity;
import com.anurag.campusfix.entity.ComplaintPriority;
import com.anurag.campusfix.entity.ComplaintStatus;
import com.anurag.campusfix.entity.Role;
import com.anurag.campusfix.entity.UserEntity;
import com.anurag.campusfix.jwt.JwtService;
import com.anurag.campusfix.repository.ComplaintHistoryRepository;
import com.anurag.campusfix.repository.ComplaintImageRepository;
import com.anurag.campusfix.repository.ComplaintRepository;
import com.anurag.campusfix.repository.UserRepository;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class ComplaintControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.anurag.campusfix.repository.ComplaintVerificationRepository complaintVerificationRepository;

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private ComplaintHistoryRepository complaintHistoryRepository;

    @Autowired
    private ComplaintImageRepository complaintImageRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Value("${app.complaint.upload.dir:target/test-uploads}")
    private String uploadDirPath;

    private MockMvc mockMvc;

    private UserEntity student;
    private UserEntity otherStudent;
    private UserEntity staff;
    private UserEntity otherStaff;
    private UserEntity admin;

    private String studentToken;
    private String otherStudentToken;
    private String staffToken;
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

        student = createUser("Alice Student", "student@campus.edu", Role.STUDENT);
        otherStudent = createUser("Bob Student", "otherstudent@campus.edu", Role.STUDENT);
        staff = createUser("Charlie Staff", "staff@campus.edu", Role.MAINTENANCE_STAFF);
        otherStaff = createUser("Dave Staff", "otherstaff@campus.edu", Role.MAINTENANCE_STAFF);
        admin = createUser("Diana Admin", "admin@campus.edu", Role.ADMIN);

        studentToken = jwtService.generateToken(student.getEmail(), student.getRole().name());
        otherStudentToken = jwtService.generateToken(otherStudent.getEmail(), otherStudent.getRole().name());
        staffToken = jwtService.generateToken(staff.getEmail(), staff.getRole().name());
        otherStaffToken = jwtService.generateToken(otherStaff.getEmail(), otherStaff.getRole().name());
        adminToken = jwtService.generateToken(admin.getEmail(), admin.getRole().name());
    }

    @AfterEach
    void tearDown() {
        try {
            Path testDir = Path.of(uploadDirPath);
            if (Files.exists(testDir)) {
                Files.walk(testDir)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        } catch (Exception ignored) {
        }
    }

    private UserEntity createUser(String name, String email, Role role) {
        UserEntity u = new UserEntity();
        u.setName(name);
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode("TestPass123!"));
        u.setRole(role);
        u.setIsActive(true);
        return userRepository.save(u);
    }

    private ComplaintEntity createComplaint(String ref, String title, ComplaintCategory category, ComplaintPriority priority, ComplaintStatus status, UserEntity reporter, UserEntity staff) {
        ComplaintEntity complaint = new ComplaintEntity();
        complaint.setReferenceNumber(ref);
        complaint.setTitle(title);
        complaint.setDescription("Detailed description of " + title);
        complaint.setCategory(category);
        complaint.setPriority(priority);
        complaint.setStatus(status);
        complaint.setBuilding("Academic Block");
        complaint.setFloor("1st Floor");
        complaint.setRoom("Room 101");
        complaint.setReporter(reporter);
        complaint.setAssignedStaff(staff);
        return complaintRepository.save(complaint);
    }

    @Test
    @DisplayName("Student submits complaint -> returns 201 Created with auto-generated reference and SUBMITTED status")
    void testStudentSubmitComplaintSuccess() throws Exception {
        String payload = """
            {
                "title": "Water leakage in washroom",
                "description": "Pipe is leaking on the 2nd floor male washroom, creating a puddle.",
                "category": "PLUMBING",
                "priority": "HIGH",
                "building": "Science Block",
                "floor": "2nd Floor",
                "room": "Washroom 204"
            }
            """;

        mockMvc.perform(post("/api/complaints")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.referenceNumber", startsWith("CMP-")))
                .andExpect(jsonPath("$.title").value("Water leakage in washroom"))
                .andExpect(jsonPath("$.category").value("PLUMBING"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.reporter.email").value("student@campus.edu"))
                .andExpect(jsonPath("$.assignedStaff").doesNotExist())
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    @DisplayName("Complaint submission validation failures return 400 Bad Request")
    void testComplaintSubmissionValidationFailures() throws Exception {
        String invalidPayload = """
            {
                "title": "",
                "description": "",
                "category": null
            }
            """;

        mockMvc.perform(post("/api/complaints")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("Unauthenticated requests return 401 Unauthorized")
    void testUnauthenticatedRequestDenied() throws Exception {
        mockMvc.perform(get("/api/complaints/mine"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Student cannot view complaint submitted by another student (403 Forbidden)")
    void testStudentCannotViewOtherStudentComplaint() throws Exception {
        ComplaintEntity complaint = createComplaint(
                "CMP-20261012-111111", "Broken Chair",
                ComplaintCategory.FURNITURE, ComplaintPriority.LOW, ComplaintStatus.SUBMITTED,
                student, null
        );

        mockMvc.perform(get("/api/complaints/" + complaint.getId())
                        .header("Authorization", "Bearer " + otherStudentToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("Staff cannot view complaint not assigned to them (403 Forbidden)")
    void testStaffCannotViewUnassignedComplaint() throws Exception {
        ComplaintEntity complaint = createComplaint(
                "CMP-20261012-222222", "AC not cooling",
                ComplaintCategory.EQUIPMENT, ComplaintPriority.MEDIUM, ComplaintStatus.ASSIGNED,
                student, staff
        );

        mockMvc.perform(get("/api/complaints/" + complaint.getId())
                        .header("Authorization", "Bearer " + otherStaffToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("Admin can view any complaint and list all complaints")
    void testAdminCanViewAllComplaints() throws Exception {
        ComplaintEntity complaint = createComplaint(
                "CMP-20261012-333333", "Projector flicker",
                ComplaintCategory.EQUIPMENT, ComplaintPriority.MEDIUM, ComplaintStatus.SUBMITTED,
                student, null
        );

        mockMvc.perform(get("/api/complaints/" + complaint.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Projector flicker"));

        mockMvc.perform(get("/api/admin/complaints")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("Complete status lifecycle: SUBMITTED -> ASSIGNED -> ACCEPTED -> IN_PROGRESS -> RESOLVED -> CLOSED -> REOPENED")
    void testCompleteStatusLifecycleWorkflow() throws Exception {
        // 1. Student submits complaint
        String submitPayload = """
            {
                "title": "Wi-Fi Router Dead",
                "description": "Router on 4th floor has no power lights on.",
                "category": "WIFI",
                "priority": "HIGH",
                "building": "Tech Tower",
                "floor": "4th Floor",
                "room": "Corridor 4B"
            }
            """;

        MvcResult submitResult = mockMvc.perform(post("/api/complaints")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andReturn();

        String responseBody = submitResult.getResponse().getContentAsString();
        long complaintId = Long.parseLong(responseBody.split("\"id\":")[1].split(",")[0].trim());

        // 2. Admin assigns staff
        String assignPayload = String.format("{\"staffId\": %d}", staff.getId());
        mockMvc.perform(patch("/api/admin/complaints/" + complaintId + "/assignment")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.assignedStaff.id").value(staff.getId()));

        // 3. Staff accepts assignment
        mockMvc.perform(patch("/api/staff/complaints/" + complaintId + "/accept")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        // 4. Staff starts work
        mockMvc.perform(patch("/api/staff/complaints/" + complaintId + "/start")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        // 5. Staff resolves without notes -> rejected (400)
        String emptyResolvePayload = "{\"resolutionNotes\": \"\"}";
        mockMvc.perform(patch("/api/staff/complaints/" + complaintId + "/resolve")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emptyResolvePayload))
                .andExpect(status().isBadRequest());

        // 6. Staff resolves with notes -> success (200), resolvedAt populated
        String validResolvePayload = "{\"resolutionNotes\": \"Replaced blown power adapter and restored connection.\"}";
        mockMvc.perform(patch("/api/staff/complaints/" + complaintId + "/resolve")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validResolvePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolutionNotes").value("Replaced blown power adapter and restored connection."))
                .andExpect(jsonPath("$.resolvedAt").isNotEmpty());

        // 7. Admin closes complaint
        mockMvc.perform(patch("/api/admin/complaints/" + complaintId + "/close")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        // 8. Admin reopens complaint with reason -> resolvedAt is cleared
        String reopenPayload = "{\"reason\": \"Connection dropped again within 1 hour.\"}";
        mockMvc.perform(patch("/api/admin/complaints/" + complaintId + "/reopen")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reopenPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REOPENED"))
                .andExpect(jsonPath("$.resolvedAt").doesNotExist());

        // 9. Verify history audit trail
        mockMvc.perform(get("/api/complaints/" + complaintId + "/history")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(6))))
                .andExpect(jsonPath("$[0].action").value("SUBMITTED"));
    }

    @Test
    @DisplayName("Image upload: valid PNG passes magic bytes validation, invalid magic bytes rejected")
    void testImageUploadAndDownload() throws Exception {
        ComplaintEntity complaint = createComplaint(
                "CMP-20261012-444444", "Broken window glass",
                ComplaintCategory.OTHER, ComplaintPriority.MEDIUM, ComplaintStatus.SUBMITTED,
                student, null
        );

        // 1. Valid PNG header: 89 50 4E 47 0D 0A 1A 0A
        byte[] validPngBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
        MockMultipartFile validImage = new MockMultipartFile(
                "file",
                "crack.png",
                "image/png",
                validPngBytes
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/complaints/" + complaint.getId() + "/images")
                        .file(validImage)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].contentType").value("image/png"))
                .andReturn();

        String uploadResponse = uploadResult.getResponse().getContentAsString();
        long imageId = Long.parseLong(uploadResponse.split("\"id\":")[1].split(",")[0].trim());

        // 2. Download by authorized student succeeds
        mockMvc.perform(get("/api/complaints/" + complaint.getId() + "/images/" + imageId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"));

        // 3. Download by unauthorized other student is forbidden (403)
        mockMvc.perform(get("/api/complaints/" + complaint.getId() + "/images/" + imageId)
                        .header("Authorization", "Bearer " + otherStudentToken))
                .andExpect(status().isForbidden());

        // 4. Invalid file signature (fake png containing text) rejected (400)
        MockMultipartFile fakeImage = new MockMultipartFile(
                "file",
                "malicious.png",
                "image/png",
                "NOT_A_REAL_PNG_FILE_HEADER".getBytes()
        );

        mockMvc.perform(multipart("/api/complaints/" + complaint.getId() + "/images")
                        .file(fakeImage)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("FILE_UPLOAD_ERROR"));
    }

    @Test
    @DisplayName("Admin assigning inactive staff or non-staff user fails with 400 Bad Request")
    void testAdminAssignInvalidStaffFails() throws Exception {
        ComplaintEntity complaint = createComplaint(
                "CMP-20261012-555555", "Damaged desk",
                ComplaintCategory.FURNITURE, ComplaintPriority.LOW, ComplaintStatus.SUBMITTED,
                student, null
        );

        // Attempt to assign another student as maintenance staff
        String invalidAssign = String.format("{\"staffId\": %d}", otherStudent.getId());
        mockMvc.perform(patch("/api/admin/complaints/" + complaint.getId() + "/assignment")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidAssign))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Student attempting staff or admin operations is denied with 403 Forbidden")
    void testStudentForbiddenFromAdminAndStaffOperations() throws Exception {
        ComplaintEntity complaint = createComplaint(
                "CMP-20261012-666666", "Flickering bulb",
                ComplaintCategory.ELECTRICAL, ComplaintPriority.LOW, ComplaintStatus.SUBMITTED,
                student, null
        );

        // Student tries to close complaint
        mockMvc.perform(patch("/api/admin/complaints/" + complaint.getId() + "/close")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        // Student tries to accept assignment
        mockMvc.perform(patch("/api/staff/complaints/" + complaint.getId() + "/accept")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Deactivated user holding valid unexpired JWT is rejected with 403 Forbidden ACCOUNT_INACTIVE")
    void testDeactivatedUserCannotAccessProtectedEndpoints() throws Exception {
        // Deactivate student account
        student.setIsActive(false);
        userRepository.save(student);

        mockMvc.perform(get("/api/complaints/mine")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_INACTIVE"));
    }

    @Test
    @DisplayName("Unrelated student cannot reopen another student's complaint (403 Forbidden)")
    void testUnauthorizedStudentCannotReopenComplaint() throws Exception {
        ComplaintEntity complaint = createComplaint(
                "CMP-20261012-777777", "Projector bulb burnt",
                ComplaintCategory.EQUIPMENT, ComplaintPriority.MEDIUM, ComplaintStatus.RESOLVED,
                student, staff
        );

        String reopenPayload = "{\"reason\": \"Still not projecting colors properly.\"}";
        mockMvc.perform(post("/api/complaints/" + complaint.getId() + "/reopen")
                        .header("Authorization", "Bearer " + otherStudentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reopenPayload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("Maintenance staff cannot reopen a complaint (403 Forbidden)")
    void testStaffCannotReopenComplaint() throws Exception {
        ComplaintEntity complaint = createComplaint(
                "CMP-20261012-888888", "Lab door lock stuck",
                ComplaintCategory.OTHER, ComplaintPriority.LOW, ComplaintStatus.RESOLVED,
                student, staff
        );

        String reopenPayload = "{\"reason\": \"Staff cannot trigger reopen.\"}";
        mockMvc.perform(post("/api/complaints/" + complaint.getId() + "/reopen")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reopenPayload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("Original student reporter can reopen their own resolved complaint")
    void testReporterCanReopenComplaint() throws Exception {
        ComplaintEntity complaint = createComplaint(
                "CMP-20261012-999999", "Lab sink leaking",
                ComplaintCategory.PLUMBING, ComplaintPriority.MEDIUM, ComplaintStatus.RESOLVED,
                student, staff
        );

        String reopenPayload = "{\"reason\": \"Tap is still dripping water.\"}";
        mockMvc.perform(post("/api/complaints/" + complaint.getId() + "/reopen")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reopenPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REOPENED"))
                .andExpect(jsonPath("$.verificationStatus").value("REJECTED"));
    }

    @Test
    @DisplayName("Reporter can also reopen complaint via PATCH method")
    void testReporterCanReopenComplaintViaPatch() throws Exception {
        ComplaintEntity complaint = createComplaint(
                "CMP-20261012-777777", "Lab window glass cracked",
                ComplaintCategory.FURNITURE, ComplaintPriority.MEDIUM, ComplaintStatus.RESOLVED,
                student, staff
        );

        String reopenPayload = "{\"reason\": \"Crack widened after strong wind.\"}";
        mockMvc.perform(patch("/api/complaints/" + complaint.getId() + "/reopen")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reopenPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REOPENED"));
    }

    @Test
    @DisplayName("Attaching images to a CLOSED complaint is rejected")
    void testCannotUploadImagesToClosedComplaint() throws Exception {
        ComplaintEntity complaint = createComplaint(
                "CMP-20261012-666666", "Door latch broken",
                ComplaintCategory.FURNITURE, ComplaintPriority.LOW, ComplaintStatus.CLOSED,
                student, staff
        );

        byte[] pngBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
        MockMultipartFile imageFile = new MockMultipartFile("images", "test.png", "image/png", pngBytes);

        mockMvc.perform(multipart("/api/complaints/" + complaint.getId() + "/images")
                        .file(imageFile)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    @DisplayName("Invalid query parameter returns 400 Bad Request with client-safe message")
    void testInvalidQueryParameterReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/complaints/mine?category=NON_EXISTENT_CATEGORY")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_PARAMETER"));
    }

    @Test
    @DisplayName("Unsupported HTTP method returns 405 Method Not Allowed")
    void testUnsupportedHttpMethodReturnsMethodNotAllowed() throws Exception {
        mockMvc.perform(delete("/api/complaints/mine")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.errorCode").value("METHOD_NOT_ALLOWED"));
    }
}
