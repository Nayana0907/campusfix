package com.anurag.campusfix.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.anurag.campusfix.dto.request.ComplaintAssignmentRequest;
import com.anurag.campusfix.dto.request.ComplaintCreateRequest;
import com.anurag.campusfix.dto.request.ComplaintPriorityUpdateRequest;
import com.anurag.campusfix.dto.request.ComplaintReopenRequest;
import com.anurag.campusfix.dto.request.ComplaintResolutionRequest;
import com.anurag.campusfix.dto.response.ComplaintResponse;
import com.anurag.campusfix.entity.ComplaintCategory;
import com.anurag.campusfix.entity.ComplaintEntity;
import com.anurag.campusfix.entity.ComplaintHistoryEntity;
import com.anurag.campusfix.entity.ComplaintPriority;
import com.anurag.campusfix.entity.ComplaintStatus;
import com.anurag.campusfix.entity.Role;
import com.anurag.campusfix.entity.UserEntity;
import com.anurag.campusfix.exception.InvalidStatusTransitionException;
import com.anurag.campusfix.exception.UnauthorizedComplaintAccessException;
import com.anurag.campusfix.exception.UserException;
import com.anurag.campusfix.repository.ComplaintHistoryRepository;
import com.anurag.campusfix.repository.ComplaintImageRepository;
import com.anurag.campusfix.repository.ComplaintRepository;
import com.anurag.campusfix.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComplaintServiceTest {

    @Mock
    private ComplaintRepository complaintRepository;

    @Mock
    private ComplaintHistoryRepository historyRepository;

    @Mock
    private ComplaintImageRepository imageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ComplaintFileStorageService fileStorageService;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ComplaintService complaintService;

    private UserEntity studentUser;
    private UserEntity otherStudentUser;
    private UserEntity staffUser;
    private UserEntity adminUser;
    private ComplaintEntity sampleComplaint;

    @BeforeEach
    void setUp() {
        studentUser = new UserEntity();
        studentUser.setId(1L);
        studentUser.setName("Alice Student");
        studentUser.setEmail("student@campus.edu");
        studentUser.setRole(Role.STUDENT);
        studentUser.setIsActive(true);

        otherStudentUser = new UserEntity();
        otherStudentUser.setId(2L);
        otherStudentUser.setName("Bob Student");
        otherStudentUser.setEmail("other@campus.edu");
        otherStudentUser.setRole(Role.STUDENT);
        otherStudentUser.setIsActive(true);

        staffUser = new UserEntity();
        staffUser.setId(3L);
        staffUser.setName("Charlie Staff");
        staffUser.setEmail("staff@campus.edu");
        staffUser.setRole(Role.MAINTENANCE_STAFF);
        staffUser.setIsActive(true);

        adminUser = new UserEntity();
        adminUser.setId(4L);
        adminUser.setName("Diana Admin");
        adminUser.setEmail("admin@campus.edu");
        adminUser.setRole(Role.ADMIN);
        adminUser.setIsActive(true);

        sampleComplaint = new ComplaintEntity();
        sampleComplaint.setId(100L);
        sampleComplaint.setReferenceNumber("CMP-20261012-ABC123");
        sampleComplaint.setTitle("Broken switchboard");
        sampleComplaint.setDescription("Switchboard in lab 302 is sparking");
        sampleComplaint.setCategory(ComplaintCategory.ELECTRICAL);
        sampleComplaint.setPriority(ComplaintPriority.HIGH);
        sampleComplaint.setStatus(ComplaintStatus.SUBMITTED);
        sampleComplaint.setBuilding("Academic Block A");
        sampleComplaint.setFloor("3rd Floor");
        sampleComplaint.setRoom("Lab 302");
        sampleComplaint.setReporter(studentUser);
        sampleComplaint.setCreatedAt(LocalDateTime.now().minusHours(2));
        sampleComplaint.setUpdatedAt(LocalDateTime.now().minusHours(2));
    }

    @Test
    @DisplayName("Submit complaint successfully sets initial status to SUBMITTED and generates reference")
    void testSubmitComplaintSuccess() {
        ComplaintCreateRequest request = new ComplaintCreateRequest();
        request.setTitle("Broken switchboard");
        request.setDescription("Switchboard in lab 302 is sparking");
        request.setCategory(ComplaintCategory.ELECTRICAL);
        request.setPriority(ComplaintPriority.HIGH);
        request.setBuilding("Academic Block A");
        request.setFloor("3rd Floor");
        request.setRoom("Lab 302");

        when(complaintRepository.existsByReferenceNumber(anyString())).thenReturn(false);
        when(complaintRepository.save(any(ComplaintEntity.class))).thenAnswer(invocation -> {
            ComplaintEntity entity = invocation.getArgument(0);
            entity.setId(100L);
            return entity;
        });

        ComplaintResponse response = complaintService.submitComplaint(request, null, studentUser);

        assertNotNull(response);
        assertEquals(ComplaintStatus.SUBMITTED, response.getStatus());
        assertEquals("Broken switchboard", response.getTitle());
        assertTrue(response.getReferenceNumber().startsWith("CMP-"));
        verify(historyRepository, times(1)).save(any(ComplaintHistoryEntity.class));
    }

    @Test
    @DisplayName("Admin assigns maintenance staff successfully")
    void testAssignStaffSuccess() {
        when(complaintRepository.findById(100L)).thenReturn(Optional.of(sampleComplaint));
        when(userRepository.findById(3L)).thenReturn(Optional.of(staffUser));
        when(complaintRepository.save(any(ComplaintEntity.class))).thenAnswer(i -> i.getArgument(0));

        ComplaintAssignmentRequest request = new ComplaintAssignmentRequest();
        request.setStaffId(3L);

        ComplaintResponse response = complaintService.assignStaff(100L, request, adminUser);

        assertEquals(ComplaintStatus.ASSIGNED, response.getStatus());
        assertEquals(3L, response.getAssignedStaff().getId());
        verify(historyRepository, times(1)).save(any(ComplaintHistoryEntity.class));
    }

    @Test
    @DisplayName("Admin assigning non-staff user throws UserException")
    void testAssignNonStaffThrows() {
        when(complaintRepository.findById(100L)).thenReturn(Optional.of(sampleComplaint));
        when(userRepository.findById(2L)).thenReturn(Optional.of(otherStudentUser));

        ComplaintAssignmentRequest request = new ComplaintAssignmentRequest();
        request.setStaffId(2L);

        assertThrows(UserException.class, () ->
                complaintService.assignStaff(100L, request, adminUser));
    }

    @Test
    @DisplayName("Assigned staff accepts complaint successfully")
    void testStaffAcceptsComplaint() {
        sampleComplaint.setStatus(ComplaintStatus.ASSIGNED);
        sampleComplaint.setAssignedStaff(staffUser);

        when(complaintRepository.findById(100L)).thenReturn(Optional.of(sampleComplaint));
        when(complaintRepository.save(any(ComplaintEntity.class))).thenAnswer(i -> i.getArgument(0));

        ComplaintResponse response = complaintService.acceptAssignment(100L, staffUser);

        assertEquals(ComplaintStatus.ACCEPTED, response.getStatus());
        verify(historyRepository, times(1)).save(any(ComplaintHistoryEntity.class));
    }

    @Test
    @DisplayName("Unassigned staff cannot accept complaint")
    void testUnassignedStaffCannotAccept() {
        sampleComplaint.setStatus(ComplaintStatus.ASSIGNED);
        UserEntity differentStaff = new UserEntity();
        differentStaff.setId(99L);
        differentStaff.setRole(Role.MAINTENANCE_STAFF);
        sampleComplaint.setAssignedStaff(differentStaff);

        when(complaintRepository.findById(100L)).thenReturn(Optional.of(sampleComplaint));

        assertThrows(UnauthorizedComplaintAccessException.class, () ->
                complaintService.acceptAssignment(100L, staffUser));
    }

    @Test
    @DisplayName("Staff starting work transitions status to IN_PROGRESS")
    void testStaffStartsWork() {
        sampleComplaint.setStatus(ComplaintStatus.ACCEPTED);
        sampleComplaint.setAssignedStaff(staffUser);

        when(complaintRepository.findById(100L)).thenReturn(Optional.of(sampleComplaint));
        when(complaintRepository.save(any(ComplaintEntity.class))).thenAnswer(i -> i.getArgument(0));

        ComplaintResponse response = complaintService.startWork(100L, staffUser);

        assertEquals(ComplaintStatus.IN_PROGRESS, response.getStatus());
    }

    @Test
    @DisplayName("Staff resolves complaint with notes and records resolvedAt")
    void testStaffResolvesComplaint() {
        sampleComplaint.setStatus(ComplaintStatus.IN_PROGRESS);
        sampleComplaint.setAssignedStaff(staffUser);

        when(complaintRepository.findById(100L)).thenReturn(Optional.of(sampleComplaint));
        when(complaintRepository.save(any(ComplaintEntity.class))).thenAnswer(i -> i.getArgument(0));

        ComplaintResolutionRequest request = new ComplaintResolutionRequest();
        request.setResolutionNotes("Replaced the spark plug and secured terminals.");

        ComplaintResponse response = complaintService.resolveComplaint(100L, request, staffUser);

        assertEquals(ComplaintStatus.RESOLVED, response.getStatus());
        assertEquals("Replaced the spark plug and secured terminals.", response.getResolutionNotes());
        assertNotNull(response.getResolvedAt());
    }

    @Test
    @DisplayName("Resolving complaint without notes throws IllegalArgumentException")
    void testResolveComplaintBlankNotesThrows() {
        sampleComplaint.setStatus(ComplaintStatus.IN_PROGRESS);
        sampleComplaint.setAssignedStaff(staffUser);

        when(complaintRepository.findById(100L)).thenReturn(Optional.of(sampleComplaint));

        ComplaintResolutionRequest request = new ComplaintResolutionRequest();
        request.setResolutionNotes("   ");

        assertThrows(IllegalArgumentException.class, () ->
                complaintService.resolveComplaint(100L, request, staffUser));
    }

    @Test
    @DisplayName("Admin closes resolved complaint")
    void testAdminClosesComplaint() {
        sampleComplaint.setStatus(ComplaintStatus.RESOLVED);
        sampleComplaint.setAssignedStaff(staffUser);
        sampleComplaint.setResolvedAt(LocalDateTime.now().minusHours(1));

        when(complaintRepository.findById(100L)).thenReturn(Optional.of(sampleComplaint));
        when(complaintRepository.save(any(ComplaintEntity.class))).thenAnswer(i -> i.getArgument(0));

        ComplaintResponse response = complaintService.closeComplaint(100L, adminUser);

        assertEquals(ComplaintStatus.CLOSED, response.getStatus());
    }

    @Test
    @DisplayName("Admin reopens complaint with reason and clears resolvedAt")
    void testAdminReopensComplaint() {
        sampleComplaint.setStatus(ComplaintStatus.RESOLVED);
        sampleComplaint.setAssignedStaff(staffUser);
        sampleComplaint.setResolvedAt(LocalDateTime.now().minusHours(1));

        when(complaintRepository.findById(100L)).thenReturn(Optional.of(sampleComplaint));
        when(complaintRepository.save(any(ComplaintEntity.class))).thenAnswer(i -> i.getArgument(0));

        ComplaintReopenRequest request = new ComplaintReopenRequest();
        request.setReason("Sparking restarted when heavy load was connected.");

        ComplaintResponse response = complaintService.reopenComplaint(100L, request, adminUser);

        assertEquals(ComplaintStatus.REOPENED, response.getStatus());
        assertNull(response.getResolvedAt());
    }

    @Test
    @DisplayName("Invalid status transition throws InvalidStatusTransitionException")
    void testInvalidStatusTransitionThrows() {
        sampleComplaint.setStatus(ComplaintStatus.SUBMITTED);
        when(complaintRepository.findById(100L)).thenReturn(Optional.of(sampleComplaint));

        assertThrows(InvalidStatusTransitionException.class, () ->
                complaintService.closeComplaint(100L, adminUser));
    }

    @Test
    @DisplayName("Student cannot view complaint filed by another student")
    void testStudentCannotViewOtherComplaint() {
        when(complaintRepository.findById(100L)).thenReturn(Optional.of(sampleComplaint));

        assertThrows(UnauthorizedComplaintAccessException.class, () ->
                complaintService.getComplaintDetails(100L, otherStudentUser));
    }

    @Test
    @DisplayName("Reporter can reopen their own resolved complaint")
    void testReporterReopensOwnComplaint() {
        sampleComplaint.setStatus(ComplaintStatus.RESOLVED);
        sampleComplaint.setAssignedStaff(staffUser);
        sampleComplaint.setResolvedAt(LocalDateTime.now().minusHours(1));

        when(complaintRepository.findById(100L)).thenReturn(Optional.of(sampleComplaint));
        when(complaintRepository.save(any(ComplaintEntity.class))).thenAnswer(i -> i.getArgument(0));

        ComplaintReopenRequest request = new ComplaintReopenRequest();
        request.setReason("Still not functioning as expected.");

        ComplaintResponse response = complaintService.reopenComplaint(100L, request, studentUser);

        assertEquals(ComplaintStatus.REOPENED, response.getStatus());
        assertNull(response.getResolvedAt());
    }

    @Test
    @DisplayName("Unrelated student or staff cannot reopen complaint")
    void testUnauthorizedUserCannotReopenComplaint() {
        sampleComplaint.setStatus(ComplaintStatus.RESOLVED);
        when(complaintRepository.findById(100L)).thenReturn(Optional.of(sampleComplaint));

        ComplaintReopenRequest request = new ComplaintReopenRequest();
        request.setReason("Reopening someone else's complaint.");

        assertThrows(UnauthorizedComplaintAccessException.class, () ->
                complaintService.reopenComplaint(100L, request, otherStudentUser));

        assertThrows(UnauthorizedComplaintAccessException.class, () ->
                complaintService.reopenComplaint(100L, request, staffUser));
    }

    @Test
    @DisplayName("Non-admin cannot directly close a complaint")
    void testNonAdminCannotCloseComplaint() {
        assertThrows(UnauthorizedComplaintAccessException.class, () ->
                complaintService.closeComplaint(100L, studentUser));

        assertThrows(UnauthorizedComplaintAccessException.class, () ->
                complaintService.closeComplaint(100L, staffUser));
    }

    @Test
    @DisplayName("Cannot attach images to a CLOSED complaint")
    void testCannotAddImagesToClosedComplaint() {
        sampleComplaint.setStatus(ComplaintStatus.CLOSED);
        when(complaintRepository.findById(100L)).thenReturn(Optional.of(sampleComplaint));

        assertThrows(InvalidStatusTransitionException.class, () ->
                complaintService.addImages(100L, Collections.emptyList(), studentUser));
    }

    @Test
    @DisplayName("Staff can resolve a REOPENED complaint directly")
    void testStaffResolvesReopenedComplaint() {
        sampleComplaint.setStatus(ComplaintStatus.REOPENED);
        sampleComplaint.setAssignedStaff(staffUser);

        when(complaintRepository.findById(100L)).thenReturn(Optional.of(sampleComplaint));
        when(complaintRepository.save(any(ComplaintEntity.class))).thenAnswer(i -> i.getArgument(0));

        ComplaintResolutionRequest request = new ComplaintResolutionRequest();
        request.setResolutionNotes("Fixed remaining issue after reopening.");

        ComplaintResponse response = complaintService.resolveComplaint(100L, request, staffUser);

        assertEquals(ComplaintStatus.RESOLVED, response.getStatus());
        assertEquals("Fixed remaining issue after reopening.", response.getResolutionNotes());
    }
}
