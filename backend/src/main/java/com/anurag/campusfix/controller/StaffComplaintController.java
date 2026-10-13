package com.anurag.campusfix.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

import com.anurag.campusfix.dto.request.ComplaintResolutionRequest;
import com.anurag.campusfix.dto.response.ComplaintResponse;
import com.anurag.campusfix.dto.response.PagedResponse;
import com.anurag.campusfix.entity.ComplaintCategory;
import com.anurag.campusfix.entity.ComplaintPriority;
import com.anurag.campusfix.entity.ComplaintStatus;
import com.anurag.campusfix.entity.UserEntity;
import com.anurag.campusfix.exception.AccountInactiveException;
import com.anurag.campusfix.exception.ResourceNotFoundException;
import com.anurag.campusfix.repository.UserRepository;
import com.anurag.campusfix.service.ComplaintService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/staff/complaints")
@PreAuthorize("hasRole('MAINTENANCE_STAFF')")
public class StaffComplaintController {

    private final ComplaintService complaintService;
    private final UserRepository userRepository;

    public StaffComplaintController(ComplaintService complaintService, UserRepository userRepository) {
        this.complaintService = complaintService;
        this.userRepository = userRepository;
    }

    /**
     * Maintenance Staff: List complaints assigned to the authenticated staff member.
     */
    @GetMapping
    public ResponseEntity<PagedResponse<ComplaintResponse>> getAssignedComplaints(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ComplaintCategory category,
            @RequestParam(required = false) ComplaintPriority priority,
            @RequestParam(required = false) ComplaintStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            Authentication authentication) {

        UserEntity currentStaff = resolveCurrentUser(authentication);
        Pageable pageable = createSafePageable(page, size, sortBy, direction);
        PagedResponse<ComplaintResponse> response = complaintService.getAssignedComplaints(
                currentStaff, search, category, priority, status, pageable
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Maintenance Staff: Accept an assigned complaint.
     */
    @PatchMapping("/{id}/accept")
    public ResponseEntity<ComplaintResponse> acceptAssignment(
            @PathVariable Long id,
            Authentication authentication) {

        UserEntity currentStaff = resolveCurrentUser(authentication);
        ComplaintResponse response = complaintService.acceptAssignment(id, currentStaff);
        return ResponseEntity.ok(response);
    }

    /**
     * Maintenance Staff: Start work on an accepted or reopened complaint.
     */
    @PatchMapping("/{id}/start")
    public ResponseEntity<ComplaintResponse> startWork(
            @PathVariable Long id,
            Authentication authentication) {

        UserEntity currentStaff = resolveCurrentUser(authentication);
        ComplaintResponse response = complaintService.startWork(id, currentStaff);
        return ResponseEntity.ok(response);
    }

    /**
     * Maintenance Staff: Submit resolution notes and mark work resolved.
     */
    @PatchMapping("/{id}/resolve")
    public ResponseEntity<ComplaintResponse> resolveComplaint(
            @PathVariable Long id,
            @Valid @RequestBody ComplaintResolutionRequest request,
            Authentication authentication) {

        UserEntity currentStaff = resolveCurrentUser(authentication);
        ComplaintResponse response = complaintService.resolveComplaint(id, request, currentStaff);
        return ResponseEntity.ok(response);
    }

    private UserEntity resolveCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated staff member not found: " + email));
        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new AccountInactiveException("Account is inactive. Please contact support.");
        }
        return user;
    }

    private Pageable createSafePageable(int page, int size, String sortBy, String direction) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);

        String safeSort = switch (sortBy) {
            case "id", "referenceNumber", "title", "category", "priority", "status", "updatedAt" -> sortBy;
            default -> "createdAt";
        };

        Sort.Direction sortDir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(safePage, safeSize, Sort.by(sortDir, safeSort));
    }
}
