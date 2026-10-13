package com.anurag.campusfix.controller;

import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.anurag.campusfix.dto.request.ComplaintCreateRequest;
import com.anurag.campusfix.dto.response.ComplaintHistoryResponse;
import com.anurag.campusfix.dto.response.ComplaintImageResponse;
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
@RequestMapping("/api/complaints")
public class ComplaintController {

    private final ComplaintService complaintService;
    private final UserRepository userRepository;

    public ComplaintController(ComplaintService complaintService, UserRepository userRepository) {
        this.complaintService = complaintService;
        this.userRepository = userRepository;
    }

    /**
     * Submit a new complaint (JSON request body).
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ComplaintResponse> submitComplaintJson(
            @Valid @RequestBody ComplaintCreateRequest request,
            Authentication authentication) {

        UserEntity currentUser = resolveCurrentUser(authentication);
        ComplaintResponse response = complaintService.submitComplaint(request, null, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Submit a new complaint with optional multipart image attachments.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ComplaintResponse> submitComplaintMultipart(
            @Valid @RequestPart("complaint") ComplaintCreateRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestPart(value = "image", required = false) MultipartFile image,
            Authentication authentication) {

        List<MultipartFile> targetFiles = new java.util.ArrayList<>();
        if (images != null) {
            targetFiles.addAll(images);
        }
        if (file != null) {
            targetFiles.add(file);
        }
        if (image != null) {
            targetFiles.add(image);
        }

        UserEntity currentUser = resolveCurrentUser(authentication);
        ComplaintResponse response = complaintService.submitComplaint(
                request, targetFiles.isEmpty() ? null : targetFiles, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * List complaints reported by currently authenticated student.
     */
    @GetMapping("/mine")
    public ResponseEntity<PagedResponse<ComplaintResponse>> getMyComplaints(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ComplaintCategory category,
            @RequestParam(required = false) ComplaintPriority priority,
            @RequestParam(required = false) ComplaintStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            Authentication authentication) {

        UserEntity currentUser = resolveCurrentUser(authentication);
        Pageable pageable = createSafePageable(page, size, sortBy, direction);
        PagedResponse<ComplaintResponse> response = complaintService.getMyComplaints(
                currentUser, search, category, priority, status, pageable
        );
        return ResponseEntity.ok(response);
    }

    /**
     * View complaint details by numeric ID.
     */
    @GetMapping("/{id:[0-9]+}")
    public ResponseEntity<ComplaintResponse> getComplaintById(
            @PathVariable Long id,
            Authentication authentication) {

        UserEntity currentUser = resolveCurrentUser(authentication);
        ComplaintResponse response = complaintService.getComplaintDetails(id, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * View complaint details by human-readable reference number (e.g. CMP-20261012-ABC123).
     */
    @GetMapping("/ref/{referenceNumber}")
    public ResponseEntity<ComplaintResponse> getComplaintByReference(
            @PathVariable String referenceNumber,
            Authentication authentication) {

        UserEntity currentUser = resolveCurrentUser(authentication);
        ComplaintResponse response = complaintService.getComplaintByReference(referenceNumber, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * View chronological audit history for a complaint.
     */
    @GetMapping("/{id:[0-9]+}/history")
    public ResponseEntity<List<ComplaintHistoryResponse>> getComplaintHistory(
            @PathVariable Long id,
            Authentication authentication) {

        UserEntity currentUser = resolveCurrentUser(authentication);
        List<ComplaintHistoryResponse> history = complaintService.getComplaintHistory(id, currentUser);
        return ResponseEntity.ok(history);
    }

    /**
     * Attach image files to an existing complaint.
     */
    @PostMapping(value = "/{id:[0-9]+}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<ComplaintImageResponse>> uploadImages(
            @PathVariable Long id,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "image", required = false) MultipartFile image,
            Authentication authentication) {

        List<MultipartFile> targetFiles = new java.util.ArrayList<>();
        if (images != null) {
            targetFiles.addAll(images);
        }
        if (file != null) {
            targetFiles.add(file);
        }
        if (image != null) {
            targetFiles.add(image);
        }
        if (targetFiles.isEmpty()) {
            throw new com.anurag.campusfix.exception.FileUploadValidationException("At least one image file must be provided");
        }

        UserEntity currentUser = resolveCurrentUser(authentication);
        List<ComplaintImageResponse> response = complaintService.addImages(id, targetFiles, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * View / download authorized image attachment.
     */
    @GetMapping("/{complaintId:[0-9]+}/images/{imageId:[0-9]+}")
    public ResponseEntity<Resource> getImage(
            @PathVariable Long complaintId,
            @PathVariable Long imageId,
            Authentication authentication) {

        UserEntity currentUser = resolveCurrentUser(authentication);
        var result = complaintService.getImageResource(complaintId, imageId, currentUser);

        String contentType = (result.contentType() != null && !result.contentType().isBlank())
                ? result.contentType()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + result.filename() + "\"")
                .body(result.resource());
    }

    private UserEntity resolveCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + email));
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
