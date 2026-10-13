package com.anurag.campusfix.controller;

import java.time.LocalDateTime;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.anurag.campusfix.dto.request.ComplaintAssignmentRequest;
import com.anurag.campusfix.dto.request.ComplaintPriorityUpdateRequest;
import com.anurag.campusfix.dto.request.ComplaintReopenRequest;
import com.anurag.campusfix.dto.response.ComplaintResponse;
import com.anurag.campusfix.dto.response.PagedResponse;
import com.anurag.campusfix.entity.ComplaintCategory;
import com.anurag.campusfix.entity.ComplaintPriority;
import com.anurag.campusfix.entity.ComplaintStatus;
import com.anurag.campusfix.entity.UserEntity;
import com.anurag.campusfix.exception.AccountInactiveException;
import com.anurag.campusfix.exception.ResourceNotFoundException;
import com.anurag.campusfix.repository.UserRepository;
import com.anurag.campusfix.dto.request.AdminVerificationOverrideRequest;
import com.anurag.campusfix.dto.response.VerificationResponse;
import com.anurag.campusfix.service.ComplaintService;
import com.anurag.campusfix.service.ComplaintVerificationService;
import org.springframework.web.bind.annotation.PostMapping;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/complaints")
@PreAuthorize("hasRole('ADMIN')")
public class AdminComplaintController {

    private final ComplaintService complaintService;
    private final ComplaintVerificationService verificationService;
    private final UserRepository userRepository;

    public AdminComplaintController(
            ComplaintService complaintService,
            ComplaintVerificationService verificationService,
            UserRepository userRepository) {
        this.complaintService = complaintService;
        this.verificationService = verificationService;
        this.userRepository = userRepository;
    }

    /**
     * Admin: List all complaints with comprehensive search, filters, sorting, and pagination.
     */
    @GetMapping
    public ResponseEntity<PagedResponse<ComplaintResponse>> getAllComplaints(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ComplaintCategory category,
            @RequestParam(required = false) ComplaintPriority priority,
            @RequestParam(required = false) ComplaintStatus status,
            @RequestParam(required = false) String building,
            @RequestParam(required = false) String floor,
            @RequestParam(required = false) String room,
            @RequestParam(required = false) Long reporterId,
            @RequestParam(required = false) Long staffId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Pageable pageable = createSafePageable(page, size, sortBy, direction);
        PagedResponse<ComplaintResponse> response = complaintService.getAllComplaintsForAdmin(
                search, category, priority, status, building, floor, room,
                reporterId, staffId, startDate, endDate, pageable
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Admin: Retrieve all active maintenance technicians for staff allocation.
     */
    @GetMapping("/staff")
    public ResponseEntity<java.util.List<com.anurag.campusfix.dto.response.UserResponse>> getActiveStaff() {
        java.util.List<com.anurag.campusfix.dto.response.UserResponse> staff = userRepository
                .findByRoleAndIsActiveTrue(com.anurag.campusfix.entity.Role.MAINTENANCE_STAFF)
                .stream()
                .map(com.anurag.campusfix.dto.response.UserResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(staff);
    }

    /**
     * Admin: Assign or reassign eligible maintenance staff to a complaint.
     */
    @PatchMapping("/{id}/assignment")
    public ResponseEntity<ComplaintResponse> assignStaff(
            @PathVariable Long id,
            @Valid @RequestBody ComplaintAssignmentRequest request,
            Authentication authentication) {

        UserEntity admin = resolveCurrentUser(authentication);
        ComplaintResponse response = complaintService.assignStaff(id, request, admin);
        return ResponseEntity.ok(response);
    }

    /**
     * Admin: Update complaint priority.
     */
    @PatchMapping("/{id}/priority")
    public ResponseEntity<ComplaintResponse> updatePriority(
            @PathVariable Long id,
            @Valid @RequestBody ComplaintPriorityUpdateRequest request,
            Authentication authentication) {

        UserEntity admin = resolveCurrentUser(authentication);
        ComplaintResponse response = complaintService.updatePriority(id, request, admin);
        return ResponseEntity.ok(response);
    }

    /**
     * Admin: Close a resolved complaint.
     */
    @PatchMapping("/{id}/close")
    public ResponseEntity<ComplaintResponse> closeComplaint(
            @PathVariable Long id,
            Authentication authentication) {

        UserEntity admin = resolveCurrentUser(authentication);
        ComplaintResponse response = complaintService.closeComplaint(id, admin);
        return ResponseEntity.ok(response);
    }

    /**
     * Admin: Reopen a closed or resolved complaint with mandatory reason.
     */
    @PatchMapping("/{id}/reopen")
    public ResponseEntity<ComplaintResponse> reopenComplaint(
            @PathVariable Long id,
            @Valid @RequestBody ComplaintReopenRequest request,
            Authentication authentication) {

        UserEntity admin = resolveCurrentUser(authentication);
        ComplaintResponse response = complaintService.reopenComplaint(id, request, admin);
        return ResponseEntity.ok(response);
    }

    /**
     * Admin: Execute verification override following timeout
     */
    @PostMapping("/{id}/verification/override")
    public ResponseEntity<VerificationResponse> overrideVerification(
            @PathVariable Long id,
            @Valid @RequestBody AdminVerificationOverrideRequest request,
            Authentication authentication) {

        UserEntity admin = resolveCurrentUser(authentication);
        VerificationResponse response = verificationService.adminOverride(id, request, admin);
        return ResponseEntity.ok(response);
    }

    private UserEntity resolveCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated admin not found: " + email));
        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new AccountInactiveException("Account is inactive. Please contact support.");
        }
        return user;
    }

    private Pageable createSafePageable(int page, int size, String sortBy, String direction) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);

        String safeSort = switch (sortBy) {
            case "id", "referenceNumber", "title", "category", "priority", "status", "updatedAt", "building" -> sortBy;
            default -> "createdAt";
        };

        Sort.Direction sortDir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(safePage, safeSize, Sort.by(sortDir, safeSort));
    }
}
