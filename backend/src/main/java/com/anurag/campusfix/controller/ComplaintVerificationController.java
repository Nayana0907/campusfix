package com.anurag.campusfix.controller;

import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

import com.anurag.campusfix.dto.request.ComplaintReopenRequest;
import com.anurag.campusfix.dto.request.ResolutionRequest;
import com.anurag.campusfix.dto.request.VerificationRequest;
import com.anurag.campusfix.dto.response.ComplaintResponse;
import com.anurag.campusfix.dto.response.ResolutionResponse;
import com.anurag.campusfix.dto.response.VerificationHistoryResponse;
import com.anurag.campusfix.dto.response.VerificationResponse;
import com.anurag.campusfix.entity.UserEntity;
import com.anurag.campusfix.exception.AccountInactiveException;
import com.anurag.campusfix.exception.ResourceNotFoundException;
import com.anurag.campusfix.repository.UserRepository;
import com.anurag.campusfix.service.ComplaintService;
import com.anurag.campusfix.service.ComplaintVerificationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/complaints/{complaintId:[0-9]+}")
public class ComplaintVerificationController {

    private final ComplaintVerificationService verificationService;
    private final ComplaintService complaintService;
    private final com.anurag.campusfix.service.FeedbackService feedbackService;
    private final UserRepository userRepository;

    public ComplaintVerificationController(
            ComplaintVerificationService verificationService,
            ComplaintService complaintService,
            com.anurag.campusfix.service.FeedbackService feedbackService,
            UserRepository userRepository) {
        this.verificationService = verificationService;
        this.complaintService = complaintService;
        this.feedbackService = feedbackService;
        this.userRepository = userRepository;
    }

    /**
     * Submit resolution with notes (JSON body)
     */
    @PostMapping(value = "/resolution", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResolutionResponse> submitResolutionJson(
            @PathVariable Long complaintId,
            @Valid @RequestBody ResolutionRequest request,
            Authentication authentication) {

        UserEntity currentUser = resolveCurrentUser(authentication);
        ResolutionResponse response = verificationService.submitResolution(complaintId, request, null, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Submit resolution with notes and optional after-repair image (Multipart)
     */
    @PostMapping(value = "/resolution", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResolutionResponse> submitResolutionMultipart(
            @PathVariable Long complaintId,
            @RequestParam("resolutionNotes") String resolutionNotes,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "afterImage", required = false) MultipartFile afterImage,
            @RequestParam(value = "image", required = false) MultipartFile image,
            Authentication authentication) {

        MultipartFile imageToAttach = file != null ? file : (afterImage != null ? afterImage : image);
        ResolutionRequest request = new ResolutionRequest(resolutionNotes);
        UserEntity currentUser = resolveCurrentUser(authentication);

        ResolutionResponse response = verificationService.submitResolution(complaintId, request, imageToAttach, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Verify resolution: Authorized reporter confirms (VERIFIED -> CLOSED) or rejects (REJECTED -> REOPENED)
     */
    @PostMapping("/verification")
    public ResponseEntity<VerificationResponse> verifyResolution(
            @PathVariable Long complaintId,
            @Valid @RequestBody VerificationRequest request,
            Authentication authentication) {

        UserEntity currentUser = resolveCurrentUser(authentication);
        VerificationResponse response = verificationService.verifyResolution(complaintId, request, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieve the current / active verification state
     */
    @GetMapping("/verification")
    public ResponseEntity<VerificationResponse> getCurrentVerification(
            @PathVariable Long complaintId,
            Authentication authentication) {

        UserEntity currentUser = resolveCurrentUser(authentication);
        VerificationResponse response = verificationService.getCurrentVerification(complaintId, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieve full verification cycle history
     */
    @GetMapping("/verification-history")
    public ResponseEntity<List<VerificationHistoryResponse>> getVerificationHistory(
            @PathVariable Long complaintId,
            Authentication authentication) {

        UserEntity currentUser = resolveCurrentUser(authentication);
        List<VerificationHistoryResponse> response = verificationService.getVerificationHistory(complaintId, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Download or stream the after-repair image
     */
    @GetMapping("/verification/after-image")
    public ResponseEntity<Resource> getAfterImage(
            @PathVariable Long complaintId,
            Authentication authentication) {

        UserEntity currentUser = resolveCurrentUser(authentication);
        Resource resource = verificationService.getAfterImageResource(complaintId, currentUser);

        String filename = resource.getFilename();
        String contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        if (filename != null) {
            String lower = filename.toLowerCase(java.util.Locale.ROOT);
            if (lower.endsWith(".png")) {
                contentType = MediaType.IMAGE_PNG_VALUE;
            } else if (lower.endsWith(".webp")) {
                contentType = "image/webp";
            } else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
                contentType = MediaType.IMAGE_JPEG_VALUE;
            }
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"after-repair-" + complaintId + "\"")
                .body(resource);
    }

    /**
     * Explicit reopen endpoint for authorized actors (supports POST and PATCH)
     */
    @org.springframework.web.bind.annotation.RequestMapping(
            value = "/reopen",
            method = {org.springframework.web.bind.annotation.RequestMethod.POST, org.springframework.web.bind.annotation.RequestMethod.PATCH})
    public ResponseEntity<ComplaintResponse> reopenComplaint(
            @PathVariable Long complaintId,
            @Valid @RequestBody ComplaintReopenRequest request,
            Authentication authentication) {

        UserEntity currentUser = resolveCurrentUser(authentication);
        ComplaintResponse response = complaintService.reopenComplaint(complaintId, request, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Submit feedback and rating after complaint resolution.
     */
    @PostMapping("/feedback")
    public ResponseEntity<com.anurag.campusfix.dto.response.FeedbackResponse> submitFeedback(
            @PathVariable Long complaintId,
            @Valid @RequestBody com.anurag.campusfix.dto.request.FeedbackRequest request,
            Authentication authentication) {

        UserEntity currentUser = resolveCurrentUser(authentication);
        com.anurag.campusfix.dto.response.FeedbackResponse response = feedbackService.submitFeedback(complaintId, request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieve feedback for a complaint if authorized.
     */
    @GetMapping("/feedback")
    public ResponseEntity<com.anurag.campusfix.dto.response.FeedbackResponse> getFeedback(
            @PathVariable Long complaintId,
            Authentication authentication) {

        UserEntity currentUser = resolveCurrentUser(authentication);
        com.anurag.campusfix.dto.response.FeedbackResponse response = feedbackService.getFeedback(complaintId, currentUser);
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
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
}
