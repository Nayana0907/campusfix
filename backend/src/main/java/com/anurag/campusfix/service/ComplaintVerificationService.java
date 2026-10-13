package com.anurag.campusfix.service;

import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.anurag.campusfix.dto.request.AdminVerificationOverrideRequest;
import com.anurag.campusfix.dto.request.ResolutionRequest;
import com.anurag.campusfix.dto.request.VerificationRequest;
import com.anurag.campusfix.dto.response.ResolutionResponse;
import com.anurag.campusfix.dto.response.VerificationHistoryResponse;
import com.anurag.campusfix.dto.response.VerificationResponse;
import com.anurag.campusfix.entity.ComplaintEntity;
import com.anurag.campusfix.entity.ComplaintHistoryEntity;
import com.anurag.campusfix.entity.ComplaintStatus;
import com.anurag.campusfix.entity.ComplaintVerificationEntity;
import com.anurag.campusfix.entity.Role;
import com.anurag.campusfix.entity.UserEntity;
import com.anurag.campusfix.entity.VerificationStatus;
import com.anurag.campusfix.event.ComplaintVerificationDomainEvent;
import com.anurag.campusfix.exception.InvalidStatusTransitionException;
import com.anurag.campusfix.exception.ResourceNotFoundException;
import com.anurag.campusfix.exception.UnauthorizedComplaintAccessException;
import com.anurag.campusfix.exception.VerificationConflictException;
import com.anurag.campusfix.repository.ComplaintHistoryRepository;
import com.anurag.campusfix.repository.ComplaintRepository;
import com.anurag.campusfix.repository.ComplaintVerificationRepository;

@Service
public class ComplaintVerificationService {

    private static final Logger log = LoggerFactory.getLogger(ComplaintVerificationService.class);

    private final ComplaintRepository complaintRepository;
    private final ComplaintVerificationRepository verificationRepository;
    private final ComplaintHistoryRepository historyRepository;
    private final ComplaintFileStorageService fileStorageService;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.verification.timeout-hours:72}")
    private long timeoutHours;

    @Value("${app.verification.admin-override-enabled:false}")
    private boolean adminOverrideEnabled;

    @Value("${app.verification.admin-override-delay-hours:72}")
    private long adminOverrideDelayHours;

    public ComplaintVerificationService(
            ComplaintRepository complaintRepository,
            ComplaintVerificationRepository verificationRepository,
            ComplaintHistoryRepository historyRepository,
            ComplaintFileStorageService fileStorageService,
            ApplicationEventPublisher eventPublisher) {
        this.complaintRepository = complaintRepository;
        this.verificationRepository = verificationRepository;
        this.historyRepository = historyRepository;
        this.fileStorageService = fileStorageService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Submit a resolution for an assigned complaint by maintenance staff or admin.
     * Transitions complaint operational status to RESOLVED and verificationStatus to PENDING.
     * Creates a new immutable verification cycle.
     */
    @Transactional
    public ResolutionResponse submitResolution(Long complaintId, ResolutionRequest request,
                                               MultipartFile afterImageFile, UserEntity actor) {
        ComplaintEntity complaint = findComplaintOrThrow(complaintId);

        // Authorization check: Assigned maintenance staff or ADMIN
        if (actor.getRole() == Role.MAINTENANCE_STAFF) {
            if (complaint.getAssignedStaff() == null || !complaint.getAssignedStaff().getId().equals(actor.getId())) {
                throw new UnauthorizedComplaintAccessException("You are not the assigned maintenance staff for this complaint");
            }
        } else if (actor.getRole() != Role.ADMIN) {
            throw new UnauthorizedComplaintAccessException("Only assigned maintenance staff or administrators can resolve a complaint");
        }

        // Operational status validation
        if (complaint.getStatus() != ComplaintStatus.IN_PROGRESS && complaint.getStatus() != ComplaintStatus.REOPENED) {
            throw new InvalidStatusTransitionException(
                    "Complaint can only be resolved when status is IN_PROGRESS or REOPENED. Current status: " + complaint.getStatus());
        }

        if (request == null || request.getResolutionNotes() == null || request.getResolutionNotes().isBlank()) {
            throw new IllegalArgumentException("Resolution notes are required to resolve a complaint");
        }

        String notes = request.getResolutionNotes().trim();

        // Process optional after-repair image
        String afterImageStorageKey = complaint.getAfterImageStorageKey();
        if (afterImageFile != null && !afterImageFile.isEmpty()) {
            var stored = fileStorageService.storeFile(afterImageFile);
            afterImageStorageKey = stored.storageKey();
        }

        LocalDateTime now = LocalDateTime.now();
        ComplaintStatus previousStatus = complaint.getStatus();

        // Update main complaint entity
        complaint.setStatus(ComplaintStatus.RESOLVED);
        complaint.setResolvedAt(now);
        complaint.setResolutionNotes(notes);
        complaint.setVerificationStatus(VerificationStatus.PENDING);
        complaint.setVerifiedAt(null);
        complaint.setVerifiedBy(null);
        complaint.setVerificationComment(null);
        complaint.setAfterImageStorageKey(afterImageStorageKey);

        // Determine cycle number
        int nextCycle = complaint.getVerificationCycles().size() + 1;

        ComplaintVerificationEntity cycle = new ComplaintVerificationEntity(
                complaint, nextCycle, now, notes, afterImageStorageKey
        );
        complaint.addVerificationCycle(cycle);

        ComplaintEntity savedComplaint = complaintRepository.saveAndFlush(complaint);
        ComplaintVerificationEntity savedCycle = verificationRepository.findTopByComplaintIdOrderByCycleNumberDesc(savedComplaint.getId())
                .orElse(cycle);

        // Persist history audit record
        ComplaintHistoryEntity history = new ComplaintHistoryEntity(
                savedComplaint,
                "RESOLVED",
                previousStatus,
                ComplaintStatus.RESOLVED,
                actor,
                String.format("Resolution cycle #%d submitted by %s: %s (Verification PENDING)",
                        nextCycle, actor.getName(), notes)
        );
        historyRepository.save(history);

        // Publish domain event for after-commit notification to reporter
        if (eventPublisher != null && savedComplaint.getReporter() != null) {
            eventPublisher.publishEvent(new ComplaintVerificationDomainEvent(
                    savedComplaint.getId(),
                    savedComplaint.getReferenceNumber(),
                    savedComplaint.getReporter().getId(),
                    savedComplaint.getReporter().getEmail(),
                    savedComplaint.getReporter().getName(),
                    "VERIFICATION_PENDING",
                    "Action Required: Verify Resolution for Complaint #" + savedComplaint.getReferenceNumber(),
                    "Work on complaint '" + savedComplaint.getTitle() + "' has been completed by maintenance staff. "
                            + "Please review the repair notes and confirm or reject the resolution."
            ));
        }

        return ResolutionResponse.from(savedComplaint, savedCycle);
    }

    /**
     * Authorized reporter confirms (VERIFIED -> CLOSED) or rejects (REJECTED -> REOPENED) a pending resolution.
     */
    @Transactional
    public VerificationResponse verifyResolution(Long complaintId, VerificationRequest request, UserEntity verifier) {
        ComplaintEntity complaint = findComplaintOrThrow(complaintId);

        // Check if an active cycle exists and has already been completed
        ComplaintVerificationEntity activeCycle = verificationRepository.findTopByComplaintIdOrderByCycleNumberDesc(complaintId)
                .orElse(null);

        if (activeCycle != null && (complaint.getVerificationStatus() != VerificationStatus.PENDING || activeCycle.getVerificationStatus() != VerificationStatus.PENDING)) {
            throw new VerificationConflictException(
                    String.format("Verification cycle #%d has already been completed with status %s. Cannot re-verify.",
                            activeCycle.getCycleNumber(), activeCycle.getVerificationStatus()));
        }

        // Must be in operational RESOLVED state
        if (complaint.getStatus() != ComplaintStatus.RESOLVED) {
            throw new InvalidStatusTransitionException(
                    "Only RESOLVED complaints can be verified. Current operational status: " + complaint.getStatus());
        }

        if (activeCycle == null) {
            throw new ResourceNotFoundException("No verification cycle found for complaint ID: " + complaintId);
        }

        // Separation of concerns: Resolver cannot verify their own work
        if (complaint.getAssignedStaff() != null && verifier.getId().equals(complaint.getAssignedStaff().getId())) {
            throw new UnauthorizedComplaintAccessException("Maintenance staff cannot verify their own repair resolution");
        }

        // Verifier must be the original reporter or admin
        boolean isReporter = complaint.getReporter() != null && complaint.getReporter().getId().equals(verifier.getId());
        boolean isAdmin = verifier.getRole() == Role.ADMIN;

        if (!isReporter && !isAdmin) {
            throw new UnauthorizedComplaintAccessException("Only the original reporter or an authorized administrator can verify this complaint");
        }

        // Validate decision
        if (request == null || request.getDecision() == null) {
            throw new IllegalArgumentException("Verification decision is required");
        }

        if (request.getDecision() == VerificationStatus.PENDING) {
            throw new IllegalArgumentException("Decision cannot be PENDING; must be VERIFIED or REJECTED");
        }

        LocalDateTime now = LocalDateTime.now();

        if (request.getDecision() == VerificationStatus.VERIFIED) {
            // CONFIRM REPAIR -> CLOSED
            String comment = (request.getComment() != null && !request.getComment().isBlank())
                    ? request.getComment().trim() : null;

            complaint.setStatus(ComplaintStatus.CLOSED);
            complaint.setVerificationStatus(VerificationStatus.VERIFIED);
            complaint.setVerifiedAt(now);
            complaint.setVerifiedBy(verifier);
            complaint.setVerificationComment(comment);

            activeCycle.setVerificationStatus(VerificationStatus.VERIFIED);
            activeCycle.setVerifiedAt(now);
            activeCycle.setVerifiedBy(verifier);
            activeCycle.setVerificationComment(comment);

            verificationRepository.save(activeCycle);
            complaintRepository.saveAndFlush(complaint);

            String auditDesc = String.format("Repair verified in cycle #%d and complaint CLOSED by %s%s",
                    activeCycle.getCycleNumber(), verifier.getName(),
                    comment != null ? ": " + comment : "");
            ComplaintHistoryEntity history = new ComplaintHistoryEntity(
                    complaint, "VERIFIED", ComplaintStatus.RESOLVED, ComplaintStatus.CLOSED, verifier, auditDesc
            );
            historyRepository.save(history);

            // Notify reporter and assigned staff
            if (eventPublisher != null && complaint.getReporter() != null) {
                eventPublisher.publishEvent(new ComplaintVerificationDomainEvent(
                        complaint.getId(),
                        complaint.getReferenceNumber(),
                        complaint.getReporter().getId(),
                        complaint.getReporter().getEmail(),
                        complaint.getReporter().getName(),
                        "VERIFICATION_CONFIRMED",
                        "Complaint #" + complaint.getReferenceNumber() + " Verified and Closed",
                        "Thank you. Your complaint has been verified and marked as CLOSED."
                ));
            }

            return VerificationResponse.from(complaint, activeCycle);

        } else {
            // REJECT REPAIR -> REOPENED
            if (request.getComment() == null || request.getComment().isBlank()) {
                throw new IllegalArgumentException("A rejection reason is required when rejecting a repair resolution");
            }

            String reason = request.getComment().trim();

            complaint.setStatus(ComplaintStatus.REOPENED);
            complaint.setResolvedAt(null); // Clear operational resolvedAt so staff must resolve again
            complaint.setVerificationStatus(VerificationStatus.REJECTED);
            complaint.setVerifiedAt(now);
            complaint.setVerifiedBy(verifier);
            complaint.setVerificationComment(reason);

            activeCycle.setVerificationStatus(VerificationStatus.REJECTED);
            activeCycle.setVerifiedAt(now);
            activeCycle.setVerifiedBy(verifier);
            activeCycle.setVerificationComment(reason);

            verificationRepository.save(activeCycle);
            complaintRepository.saveAndFlush(complaint);

            String auditDesc = String.format("Repair REJECTED in cycle #%d by %s: %s (Complaint REOPENED)",
                    activeCycle.getCycleNumber(), verifier.getName(), reason);
            ComplaintHistoryEntity history = new ComplaintHistoryEntity(
                    complaint, "REJECTED", ComplaintStatus.RESOLVED, ComplaintStatus.REOPENED, verifier, auditDesc
            );
            historyRepository.save(history);

            // Notify staff if assigned
            if (complaint.getAssignedStaff() != null) {
                eventPublisher.publishEvent(new ComplaintVerificationDomainEvent(
                        complaint.getId(),
                        complaint.getReferenceNumber(),
                        complaint.getAssignedStaff().getId(),
                        complaint.getAssignedStaff().getEmail(),
                        complaint.getAssignedStaff().getName(),
                        "VERIFICATION_REJECTED",
                        "Resolution Rejected for Complaint #" + complaint.getReferenceNumber(),
                        "The reporter has rejected the resolution with reason: '" + reason + "'. The complaint is now REOPENED."
                ));
            }

            return VerificationResponse.from(complaint, activeCycle);
        }
    }

    /**
     * Admin Override: Permits administrator to verify on behalf of reporter only when
     * admin-override is enabled and the configured timeout has elapsed.
     */
    @Transactional
    public VerificationResponse adminOverride(Long complaintId, AdminVerificationOverrideRequest request, UserEntity admin) {
        if (admin.getRole() != Role.ADMIN) {
            throw new UnauthorizedComplaintAccessException("Only administrators can execute verification override");
        }

        if (!adminOverrideEnabled) {
            throw new UnauthorizedComplaintAccessException("Admin override is disabled by system policy");
        }

        ComplaintEntity complaint = findComplaintOrThrow(complaintId);

        if (complaint.getStatus() != ComplaintStatus.RESOLVED) {
            throw new InvalidStatusTransitionException("Only RESOLVED complaints can be overridden. Current status: " + complaint.getStatus());
        }

        ComplaintVerificationEntity activeCycle = verificationRepository.findTopByComplaintIdOrderByCycleNumberDesc(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("No verification cycle found for complaint ID: " + complaintId));

        if (complaint.getVerificationStatus() != VerificationStatus.PENDING || activeCycle.getVerificationStatus() != VerificationStatus.PENDING) {
            throw new VerificationConflictException("Cannot override a cycle that is not in PENDING status");
        }

        // Verify that timeout has elapsed
        LocalDateTime deadline = complaint.getResolvedAt() != null
                ? complaint.getResolvedAt().plusHours(adminOverrideDelayHours)
                : LocalDateTime.now().minusSeconds(1);

        if (LocalDateTime.now().isBefore(deadline)) {
            throw new IllegalStateException(String.format(
                    "Admin override can only be performed after timeout of %d hours has elapsed. Override eligible after: %s",
                    adminOverrideDelayHours, deadline));
        }

        if (request == null || request.getReason() == null || request.getReason().isBlank()) {
            throw new IllegalArgumentException("An explicit reason is required to execute an admin verification override");
        }

        String reason = "ADMIN OVERRIDE: " + request.getReason().trim();
        LocalDateTime now = LocalDateTime.now();

        activeCycle.setOverriddenByAdmin(true);
        activeCycle.setVerifiedBy(admin);
        activeCycle.setVerifiedAt(now);
        activeCycle.setVerificationComment(reason);

        complaint.setVerifiedBy(admin);
        complaint.setVerifiedAt(now);
        complaint.setVerificationComment(reason);

        if (request.getDecision() == VerificationStatus.VERIFIED) {
            complaint.setStatus(ComplaintStatus.CLOSED);
            complaint.setVerificationStatus(VerificationStatus.VERIFIED);
            activeCycle.setVerificationStatus(VerificationStatus.VERIFIED);

            ComplaintHistoryEntity history = new ComplaintHistoryEntity(
                    complaint, "ADMIN_OVERRIDE_VERIFIED", ComplaintStatus.RESOLVED, ComplaintStatus.CLOSED,
                    admin, "Admin override executed by " + admin.getName() + ": " + reason
            );
            historyRepository.save(history);
        } else {
            complaint.setStatus(ComplaintStatus.REOPENED);
            complaint.setResolvedAt(null);
            complaint.setVerificationStatus(VerificationStatus.REJECTED);
            activeCycle.setVerificationStatus(VerificationStatus.REJECTED);

            ComplaintHistoryEntity history = new ComplaintHistoryEntity(
                    complaint, "ADMIN_OVERRIDE_REJECTED", ComplaintStatus.RESOLVED, ComplaintStatus.REOPENED,
                    admin, "Admin override rejection by " + admin.getName() + ": " + reason
            );
            historyRepository.save(history);
        }

        verificationRepository.save(activeCycle);
        complaintRepository.saveAndFlush(complaint);

        // Notify reporter of override
        if (eventPublisher != null && complaint.getReporter() != null) {
            eventPublisher.publishEvent(new ComplaintVerificationDomainEvent(
                    complaint.getId(),
                    complaint.getReferenceNumber(),
                    complaint.getReporter().getId(),
                    complaint.getReporter().getEmail(),
                    complaint.getReporter().getName(),
                    "ADMIN_OVERRIDE",
                    "Administrator Verification Override for Complaint #" + complaint.getReferenceNumber(),
                    "An administrator has verified the complaint on your behalf following timeout: " + reason
            ));
        }

        return VerificationResponse.from(complaint, activeCycle);
    }

    /**
     * Retrieve the active or latest verification cycle for a complaint.
     */
    @Transactional(readOnly = true)
    public VerificationResponse getCurrentVerification(Long complaintId, UserEntity currentUser) {
        ComplaintEntity complaint = findComplaintOrThrow(complaintId);
        enforceViewAccess(complaint, currentUser);

        ComplaintVerificationEntity cycle = verificationRepository.findTopByComplaintIdOrderByCycleNumberDesc(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("No verification data available for complaint ID: " + complaintId));

        return VerificationResponse.from(complaint, cycle);
    }

    /**
     * Retrieve the complete audit history of verification cycles for a complaint.
     */
    @Transactional(readOnly = true)
    public List<VerificationHistoryResponse> getVerificationHistory(Long complaintId, UserEntity currentUser) {
        ComplaintEntity complaint = findComplaintOrThrow(complaintId);
        enforceViewAccess(complaint, currentUser);

        List<ComplaintVerificationEntity> cycles = verificationRepository.findByComplaintIdOrderByCycleNumberAsc(complaintId);
        return cycles.stream()
                .map(VerificationHistoryResponse::fromEntity)
                .toList();
    }

    /**
     * Load the after-repair image resource with record-level authorization.
     */
    @Transactional(readOnly = true)
    public Resource getAfterImageResource(Long complaintId, UserEntity currentUser) {
        ComplaintEntity complaint = findComplaintOrThrow(complaintId);
        enforceViewAccess(complaint, currentUser);

        if (complaint.getAfterImageStorageKey() == null) {
            throw new ResourceNotFoundException("No after-repair image attached to complaint ID: " + complaintId);
        }

        return fileStorageService.loadAsResource(complaint.getAfterImageStorageKey());
    }

    public void setAdminOverrideEnabled(boolean adminOverrideEnabled) {
        this.adminOverrideEnabled = adminOverrideEnabled;
    }

    public void setAdminOverrideDelayHours(long adminOverrideDelayHours) {
        this.adminOverrideDelayHours = adminOverrideDelayHours;
    }

    private ComplaintEntity findComplaintOrThrow(Long id) {
        return complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with ID: " + id));
    }

    private void enforceViewAccess(ComplaintEntity complaint, UserEntity user) {
        if (user.getRole() == Role.ADMIN) {
            return;
        }
        if (user.getRole() == Role.STUDENT && complaint.getReporter() != null
                && complaint.getReporter().getId().equals(user.getId())) {
            return;
        }
        if (user.getRole() == Role.MAINTENANCE_STAFF && complaint.getAssignedStaff() != null
                && complaint.getAssignedStaff().getId().equals(user.getId())) {
            return;
        }
        throw new UnauthorizedComplaintAccessException("You do not have permission to view verification details for this complaint");
    }
}
