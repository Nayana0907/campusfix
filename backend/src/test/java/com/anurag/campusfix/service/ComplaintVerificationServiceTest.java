package com.anurag.campusfix.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.multipart.MultipartFile;

import com.anurag.campusfix.dto.request.AdminVerificationOverrideRequest;
import com.anurag.campusfix.dto.request.ResolutionRequest;
import com.anurag.campusfix.dto.request.VerificationRequest;
import com.anurag.campusfix.dto.response.ResolutionResponse;
import com.anurag.campusfix.dto.response.VerificationHistoryResponse;
import com.anurag.campusfix.dto.response.VerificationResponse;
import com.anurag.campusfix.entity.ComplaintCategory;
import com.anurag.campusfix.entity.ComplaintEntity;
import com.anurag.campusfix.entity.ComplaintHistoryEntity;
import com.anurag.campusfix.entity.ComplaintPriority;
import com.anurag.campusfix.entity.ComplaintStatus;
import com.anurag.campusfix.entity.ComplaintVerificationEntity;
import com.anurag.campusfix.entity.Role;
import com.anurag.campusfix.entity.UserEntity;
import com.anurag.campusfix.entity.VerificationStatus;
import com.anurag.campusfix.event.ComplaintVerificationDomainEvent;
import com.anurag.campusfix.exception.InvalidStatusTransitionException;
import com.anurag.campusfix.exception.UnauthorizedComplaintAccessException;
import com.anurag.campusfix.exception.VerificationConflictException;
import com.anurag.campusfix.repository.ComplaintHistoryRepository;
import com.anurag.campusfix.repository.ComplaintRepository;
import com.anurag.campusfix.repository.ComplaintVerificationRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComplaintVerificationServiceTest {

    @Mock
    private ComplaintRepository complaintRepository;

    @Mock
    private ComplaintVerificationRepository verificationRepository;

    @Mock
    private ComplaintHistoryRepository historyRepository;

    @Mock
    private ComplaintFileStorageService fileStorageService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ComplaintVerificationService verificationService;

    private UserEntity studentReporter;
    private UserEntity otherStudent;
    private UserEntity assignedStaff;
    private UserEntity unassignedStaff;
    private UserEntity adminUser;
    private ComplaintEntity sampleComplaint;

    @BeforeEach
    void setUp() {
        studentReporter = new UserEntity();
        studentReporter.setId(10L);
        studentReporter.setName("Alice Reporter");
        studentReporter.setEmail("alice@campus.edu");
        studentReporter.setRole(Role.STUDENT);
        studentReporter.setIsActive(true);

        otherStudent = new UserEntity();
        otherStudent.setId(11L);
        otherStudent.setName("Bob Other");
        otherStudent.setEmail("bob@campus.edu");
        otherStudent.setRole(Role.STUDENT);
        otherStudent.setIsActive(true);

        assignedStaff = new UserEntity();
        assignedStaff.setId(20L);
        assignedStaff.setName("Charlie Plumber");
        assignedStaff.setEmail("charlie@campus.edu");
        assignedStaff.setRole(Role.MAINTENANCE_STAFF);
        assignedStaff.setIsActive(true);

        unassignedStaff = new UserEntity();
        unassignedStaff.setId(21L);
        unassignedStaff.setName("Dan Electrician");
        unassignedStaff.setEmail("dan@campus.edu");
        unassignedStaff.setRole(Role.MAINTENANCE_STAFF);
        unassignedStaff.setIsActive(true);

        adminUser = new UserEntity();
        adminUser.setId(30L);
        adminUser.setName("Diana Admin");
        adminUser.setEmail("diana@campus.edu");
        adminUser.setRole(Role.ADMIN);
        adminUser.setIsActive(true);

        sampleComplaint = new ComplaintEntity();
        sampleComplaint.setId(100L);
        sampleComplaint.setReferenceNumber("CMP-20261012-ABC123");
        sampleComplaint.setTitle("Leaking tap in lab 3");
        sampleComplaint.setDescription("Water dripping continuously under sink");
        sampleComplaint.setCategory(ComplaintCategory.PLUMBING);
        sampleComplaint.setPriority(ComplaintPriority.MEDIUM);
        sampleComplaint.setStatus(ComplaintStatus.IN_PROGRESS);
        sampleComplaint.setReporter(studentReporter);
        sampleComplaint.setAssignedStaff(assignedStaff);
        sampleComplaint.setCreatedAt(LocalDateTime.now().minusDays(1));
    }

    @Test
    @DisplayName("submitResolution: Assigned staff can successfully resolve an in-progress complaint")
    void submitResolution_Success_ByAssignedStaff() {
        ResolutionRequest request = new ResolutionRequest("Replaced washer and tightened pipe fittings.");

        when(complaintRepository.findById(100L)).thenReturn(Optional.of(sampleComplaint));
        when(complaintRepository.saveAndFlush(any(ComplaintEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(verificationRepository.findTopByComplaintIdOrderByCycleNumberDesc(100L))
                .thenReturn(Optional.empty());

        ResolutionResponse response = verificationService.submitResolution(100L, request, null, assignedStaff);

        assertNotNull(response);
        assertEquals(ComplaintStatus.RESOLVED, sampleComplaint.getStatus());
        assertEquals(VerificationStatus.PENDING, sampleComplaint.getVerificationStatus());
        assertEquals("Replaced washer and tightened pipe fittings.", sampleComplaint.getResolutionNotes());
        assertNotNull(sampleComplaint.getResolvedAt());
        assertEquals(1, sampleComplaint.getVerificationCycles().size());

        verify(historyRepository).save(any(ComplaintHistoryEntity.class));
        verify(eventPublisher).publishEvent(any(ComplaintVerificationDomainEvent.class));
    }

    @Test
    @DisplayName("submitResolution: Admin can resolve complaint on behalf of maintenance team")
    void submitResolution_Success_ByAdmin() {
        ResolutionRequest request = new ResolutionRequest("Admin resolved: New faucet installed.");

        when(complaintRepository.findById(100L)).thenReturn(Optional.of(sampleComplaint));
        when(complaintRepository.saveAndFlush(any(ComplaintEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResolutionResponse response = verificationService.submitResolution(100L, request, null, adminUser);

        assertNotNull(response);
        assertEquals(ComplaintStatus.RESOLVED, sampleComplaint.getStatus());
        assertEquals(VerificationStatus.PENDING, sampleComplaint.getVerificationStatus());
        verify(eventPublisher).publishEvent(any(ComplaintVerificationDomainEvent.class));
    }

    @Test
    @DisplayName("submitResolution: Unassigned staff is rejected with 403 Forbidden")
    void submitResolution_Throws_WhenStaffNotAssigned() {
        ResolutionRequest request = new ResolutionRequest("I tried to fix this.");
        when(complaintRepository.findById(100L)).thenReturn(Optional.of(sampleComplaint));

        assertThrows(UnauthorizedComplaintAccessException.class, () ->
                verificationService.submitResolution(100L, request, null, unassignedStaff));

        verify(complaintRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("submitResolution: Student cannot resolve complaint")
    void submitResolution_Throws_WhenStudentAttempts() {
        ResolutionRequest request = new ResolutionRequest("Student resolution.");
        when(complaintRepository.findById(100L)).thenReturn(Optional.of(sampleComplaint));

        assertThrows(UnauthorizedComplaintAccessException.class, () ->
                verificationService.submitResolution(100L, request, null, studentReporter));
    }

    @Test
    @DisplayName("submitResolution: Complaint not in progress or reopened cannot be resolved")
    void submitResolution_Throws_WhenStatusInvalid() {
        sampleComplaint.setStatus(ComplaintStatus.SUBMITTED);
        ResolutionRequest request = new ResolutionRequest("Notes.");
        when(complaintRepository.findById(100L)).thenReturn(Optional.of(sampleComplaint));

        assertThrows(InvalidStatusTransitionException.class, () ->
                verificationService.submitResolution(100L, request, null, assignedStaff));
    }

    @Test
    @DisplayName("submitResolution: Empty or blank resolution notes rejected")
    void submitResolution_Throws_WhenNotesBlank() {
        ResolutionRequest request = new ResolutionRequest("   ");
        when(complaintRepository.findById(100L)).thenReturn(Optional.of(sampleComplaint));

        assertThrows(IllegalArgumentException.class, () ->
                verificationService.submitResolution(100L, request, null, assignedStaff));
    }

    @Test
    @DisplayName("verifyResolution: Reporter confirms repair -> CLOSED and VERIFIED")
    void verifyResolution_Confirm_Success() {
        sampleComplaint.setStatus(ComplaintStatus.RESOLVED);
        sampleComplaint.setVerificationStatus(VerificationStatus.PENDING);
        sampleComplaint.setResolvedAt(LocalDateTime.now().minusHours(2));
        sampleComplaint.setResolutionNotes("Fixed properly");

        ComplaintVerificationEntity cycle = new ComplaintVerificationEntity(
                sampleComplaint, 1, sampleComplaint.getResolvedAt(), "Fixed properly", null);
        cycle.setVerificationStatus(VerificationStatus.PENDING);

        when(complaintRepository.findById(100L)).thenReturn(Optional.of(sampleComplaint));
        when(verificationRepository.findTopByComplaintIdOrderByCycleNumberDesc(100L)).thenReturn(Optional.of(cycle));
        when(complaintRepository.saveAndFlush(any(ComplaintEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VerificationRequest request = new VerificationRequest(VerificationStatus.VERIFIED, "Confirmed, tap is completely dry.");

        VerificationResponse response = verificationService.verifyResolution(100L, request, studentReporter);

        assertNotNull(response);
        assertEquals(ComplaintStatus.CLOSED, sampleComplaint.getStatus());
        assertEquals(VerificationStatus.VERIFIED, sampleComplaint.getVerificationStatus());
        assertEquals(studentReporter, sampleComplaint.getVerifiedBy());
        assertNotNull(sampleComplaint.getVerifiedAt());
        assertEquals("Confirmed, tap is completely dry.", sampleComplaint.getVerificationComment());

        assertEquals(VerificationStatus.VERIFIED, cycle.getVerificationStatus());
        assertEquals(studentReporter, cycle.getVerifiedBy());

        verify(historyRepository).save(any(ComplaintHistoryEntity.class));
        verify(eventPublisher).publishEvent(any(ComplaintVerificationDomainEvent.class));
    }

    @Test
    @DisplayName("verifyResolution: Reporter rejects repair -> REOPENED and REJECTED, reason mandatory")
    void verifyResolution_Reject_Success() {
        sampleComplaint.setStatus(ComplaintStatus.RESOLVED);
        sampleComplaint.setVerificationStatus(VerificationStatus.PENDING);
        sampleComplaint.setResolvedAt(LocalDateTime.now().minusHours(2));
        sampleComplaint.setResolutionNotes("Fixed properly");

        ComplaintVerificationEntity cycle = new ComplaintVerificationEntity(
                sampleComplaint, 1, sampleComplaint.getResolvedAt(), "Fixed properly", null);
        cycle.setVerificationStatus(VerificationStatus.PENDING);

        when(complaintRepository.findById(100L)).thenReturn(Optional.of(sampleComplaint));
        when(verificationRepository.findTopByComplaintIdOrderByCycleNumberDesc(100L)).thenReturn(Optional.of(cycle));
        when(complaintRepository.saveAndFlush(any(ComplaintEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VerificationRequest request = new VerificationRequest(VerificationStatus.REJECTED, "Tap is still dripping heavily under the sink.");

        VerificationResponse response = verificationService.verifyResolution(100L, request, studentReporter);

        assertNotNull(response);
        assertEquals(ComplaintStatus.REOPENED, sampleComplaint.getStatus());
        assertNull(sampleComplaint.getResolvedAt()); // Cleared for new cycle
        assertEquals(VerificationStatus.REJECTED, sampleComplaint.getVerificationStatus());
        assertEquals("Tap is still dripping heavily under the sink.", sampleComplaint.getVerificationComment());

        assertEquals(VerificationStatus.REJECTED, cycle.getVerificationStatus());
        assertEquals("Tap is still dripping heavily under the sink.", cycle.getVerificationComment());

        verify(historyRepository).save(any(ComplaintHistoryEntity.class));
        verify(eventPublisher).publishEvent(any(ComplaintVerificationDomainEvent.class));
    }

    @Test
    @DisplayName("verifyResolution: Assigned staff cannot verify their own repair resolution")
    void verifyResolution_Throws_WhenStaffAttemptsSelfVerification() {
        sampleComplaint.setStatus(ComplaintStatus.RESOLVED);
        sampleComplaint.setVerificationStatus(VerificationStatus.PENDING);

        ComplaintVerificationEntity cycle = new ComplaintVerificationEntity(
                sampleComplaint, 1, LocalDateTime.now(), "Notes", null);
        cycle.setVerificationStatus(VerificationStatus.PENDING);

        when(complaintRepository.findById(100L)).thenReturn(Optional.of(sampleComplaint));
        when(verificationRepository.findTopByComplaintIdOrderByCycleNumberDesc(100L)).thenReturn(Optional.of(cycle));

        VerificationRequest request = new VerificationRequest(VerificationStatus.VERIFIED, "Looks good to me");

        assertThrows(UnauthorizedComplaintAccessException.class, () ->
                verificationService.verifyResolution(100L, request, assignedStaff));
    }

    @Test
    @DisplayName("verifyResolution: Other student cannot verify complaint")
    void verifyResolution_Throws_WhenOtherStudentAttempts() {
        sampleComplaint.setStatus(ComplaintStatus.RESOLVED);
        sampleComplaint.setVerificationStatus(VerificationStatus.PENDING);

        ComplaintVerificationEntity cycle = new ComplaintVerificationEntity(
                sampleComplaint, 1, LocalDateTime.now(), "Notes", null);
        cycle.setVerificationStatus(VerificationStatus.PENDING);

        when(complaintRepository.findById(100L)).thenReturn(Optional.of(sampleComplaint));
        when(verificationRepository.findTopByComplaintIdOrderByCycleNumberDesc(100L)).thenReturn(Optional.of(cycle));

        VerificationRequest request = new VerificationRequest(VerificationStatus.VERIFIED, "Confirmed");

        assertThrows(UnauthorizedComplaintAccessException.class, () ->
                verificationService.verifyResolution(100L, request, otherStudent));
    }

    @Test
    @DisplayName("verifyResolution: Complaint not RESOLVED cannot be verified")
    void verifyResolution_Throws_WhenComplaintNotResolved() {
        sampleComplaint.setStatus(ComplaintStatus.IN_PROGRESS);

        when(complaintRepository.findById(100L)).thenReturn(Optional.of(sampleComplaint));

        VerificationRequest request = new VerificationRequest(VerificationStatus.VERIFIED, "Confirmed");

        assertThrows(InvalidStatusTransitionException.class, () ->
                verificationService.verifyResolution(100L, request, studentReporter));
    }

    @Test
    @DisplayName("verifyResolution: Re-verifying an already completed cycle throws 409 Conflict")
    void verifyResolution_Throws_WhenCycleAlreadyVerified() {
        sampleComplaint.setStatus(ComplaintStatus.RESOLVED);
        sampleComplaint.setVerificationStatus(VerificationStatus.VERIFIED);

        ComplaintVerificationEntity cycle = new ComplaintVerificationEntity(
                sampleComplaint, 1, LocalDateTime.now().minusDays(1), "Notes", null);
        cycle.setVerificationStatus(VerificationStatus.VERIFIED);

        when(complaintRepository.findById(100L)).thenReturn(Optional.of(sampleComplaint));
        when(verificationRepository.findTopByComplaintIdOrderByCycleNumberDesc(100L)).thenReturn(Optional.of(cycle));

        VerificationRequest request = new VerificationRequest(VerificationStatus.VERIFIED, "Re-confirming");

        assertThrows(VerificationConflictException.class, () ->
                verificationService.verifyResolution(100L, request, studentReporter));
    }

    @Test
    @DisplayName("verifyResolution: Rejecting without nonblank reason throws IllegalArgumentException")
    void verifyResolution_Throws_WhenRejectReasonBlank() {
        sampleComplaint.setStatus(ComplaintStatus.RESOLVED);
        sampleComplaint.setVerificationStatus(VerificationStatus.PENDING);

        ComplaintVerificationEntity cycle = new ComplaintVerificationEntity(
                sampleComplaint, 1, LocalDateTime.now(), "Notes", null);
        cycle.setVerificationStatus(VerificationStatus.PENDING);

        when(complaintRepository.findById(100L)).thenReturn(Optional.of(sampleComplaint));
        when(verificationRepository.findTopByComplaintIdOrderByCycleNumberDesc(100L)).thenReturn(Optional.of(cycle));

        VerificationRequest request = new VerificationRequest(VerificationStatus.REJECTED, "   ");

        assertThrows(IllegalArgumentException.class, () ->
                verificationService.verifyResolution(100L, request, studentReporter));
    }

    @Test
    @DisplayName("adminOverride: Denied when disabled by policy")
    void adminOverride_Throws_WhenDisabled() {
        verificationService.setAdminOverrideEnabled(false);

        AdminVerificationOverrideRequest request = new AdminVerificationOverrideRequest(
                VerificationStatus.VERIFIED, "Admin closing after phone verification");

        assertThrows(UnauthorizedComplaintAccessException.class, () ->
                verificationService.adminOverride(100L, request, adminUser));
    }

    @Test
    @DisplayName("adminOverride: Denied before configured timeout has elapsed")
    void adminOverride_Throws_BeforeTimeout() {
        verificationService.setAdminOverrideEnabled(true);
        verificationService.setAdminOverrideDelayHours(72);

        sampleComplaint.setStatus(ComplaintStatus.RESOLVED);
        sampleComplaint.setVerificationStatus(VerificationStatus.PENDING);
        sampleComplaint.setResolvedAt(LocalDateTime.now().minusHours(24)); // Only 24h elapsed, requires 72h

        ComplaintVerificationEntity cycle = new ComplaintVerificationEntity(
                sampleComplaint, 1, sampleComplaint.getResolvedAt(), "Notes", null);
        cycle.setVerificationStatus(VerificationStatus.PENDING);

        when(complaintRepository.findById(100L)).thenReturn(Optional.of(sampleComplaint));
        when(verificationRepository.findTopByComplaintIdOrderByCycleNumberDesc(100L)).thenReturn(Optional.of(cycle));

        AdminVerificationOverrideRequest request = new AdminVerificationOverrideRequest(
                VerificationStatus.VERIFIED, "Overriding early");

        assertThrows(IllegalStateException.class, () ->
                verificationService.adminOverride(100L, request, adminUser));
    }

    @Test
    @DisplayName("adminOverride: Permitted after timeout has elapsed with required reason")
    void adminOverride_Success_AfterTimeout() {
        verificationService.setAdminOverrideEnabled(true);
        verificationService.setAdminOverrideDelayHours(48);

        sampleComplaint.setStatus(ComplaintStatus.RESOLVED);
        sampleComplaint.setVerificationStatus(VerificationStatus.PENDING);
        sampleComplaint.setResolvedAt(LocalDateTime.now().minusHours(50)); // 50h elapsed > 48h

        ComplaintVerificationEntity cycle = new ComplaintVerificationEntity(
                sampleComplaint, 1, sampleComplaint.getResolvedAt(), "Notes", null);
        cycle.setVerificationStatus(VerificationStatus.PENDING);

        when(complaintRepository.findById(100L)).thenReturn(Optional.of(sampleComplaint));
        when(verificationRepository.findTopByComplaintIdOrderByCycleNumberDesc(100L)).thenReturn(Optional.of(cycle));
        when(complaintRepository.saveAndFlush(any(ComplaintEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminVerificationOverrideRequest request = new AdminVerificationOverrideRequest(
                VerificationStatus.VERIFIED, "Contacted student via phone; student confirmed repair is complete.");

        VerificationResponse response = verificationService.adminOverride(100L, request, adminUser);

        assertNotNull(response);
        assertEquals(ComplaintStatus.CLOSED, sampleComplaint.getStatus());
        assertEquals(VerificationStatus.VERIFIED, sampleComplaint.getVerificationStatus());
        assertTrue(response.isOverriddenByAdmin());
        assertEquals(adminUser.getName(), response.getVerifiedBy().getName());
        verify(historyRepository).save(any(ComplaintHistoryEntity.class));
    }

    @Test
    @DisplayName("getVerificationHistory: Returns full cycle progression for authorized reporter")
    void getVerificationHistory_Success() {
        ComplaintVerificationEntity cycle1 = new ComplaintVerificationEntity(
                sampleComplaint, 1, LocalDateTime.now().minusDays(2), "Initial fix attempt", null);
        cycle1.setVerificationStatus(VerificationStatus.REJECTED);
        cycle1.setVerificationComment("Still leaking");

        ComplaintVerificationEntity cycle2 = new ComplaintVerificationEntity(
                sampleComplaint, 2, LocalDateTime.now().minusDays(1), "Replaced pipe section", null);
        cycle2.setVerificationStatus(VerificationStatus.VERIFIED);
        cycle2.setVerificationComment("All dry now");

        when(complaintRepository.findById(100L)).thenReturn(Optional.of(sampleComplaint));
        when(verificationRepository.findByComplaintIdOrderByCycleNumberAsc(100L))
                .thenReturn(List.of(cycle1, cycle2));

        List<VerificationHistoryResponse> history = verificationService.getVerificationHistory(100L, studentReporter);

        assertEquals(2, history.size());
        assertEquals(1, history.get(0).getCycleNumber());
        assertEquals(VerificationStatus.REJECTED, history.get(0).getVerificationStatus());
        assertEquals(2, history.get(1).getCycleNumber());
        assertEquals(VerificationStatus.VERIFIED, history.get(1).getVerificationStatus());
    }

    @Test
    @DisplayName("getVerificationHistory: Other student is denied access with 403")
    void getVerificationHistory_Throws_ForUnauthorizedUser() {
        when(complaintRepository.findById(100L)).thenReturn(Optional.of(sampleComplaint));

        assertThrows(UnauthorizedComplaintAccessException.class, () ->
                verificationService.getVerificationHistory(100L, otherStudent));
    }
}
