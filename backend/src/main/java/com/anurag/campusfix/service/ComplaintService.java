package com.anurag.campusfix.service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.anurag.campusfix.event.ComplaintVerificationDomainEvent;

import com.anurag.campusfix.dto.request.ComplaintAssignmentRequest;
import com.anurag.campusfix.dto.request.ComplaintCreateRequest;
import com.anurag.campusfix.dto.request.ComplaintPriorityUpdateRequest;
import com.anurag.campusfix.dto.request.ComplaintReopenRequest;
import com.anurag.campusfix.dto.request.ComplaintResolutionRequest;
import com.anurag.campusfix.dto.response.ComplaintHistoryResponse;
import com.anurag.campusfix.dto.response.ComplaintImageResponse;
import com.anurag.campusfix.dto.response.ComplaintResponse;
import com.anurag.campusfix.dto.response.PagedResponse;
import com.anurag.campusfix.entity.ComplaintCategory;
import com.anurag.campusfix.entity.ComplaintEntity;
import com.anurag.campusfix.entity.ComplaintHistoryEntity;
import com.anurag.campusfix.entity.ComplaintImageEntity;
import com.anurag.campusfix.entity.ComplaintPriority;
import com.anurag.campusfix.entity.ComplaintStatus;
import com.anurag.campusfix.entity.ComplaintVerificationEntity;
import com.anurag.campusfix.entity.Role;
import com.anurag.campusfix.entity.UserEntity;
import com.anurag.campusfix.entity.VerificationStatus;
import com.anurag.campusfix.exception.FileUploadValidationException;
import com.anurag.campusfix.exception.InvalidStatusTransitionException;
import com.anurag.campusfix.exception.ResourceNotFoundException;
import com.anurag.campusfix.exception.UnauthorizedComplaintAccessException;
import com.anurag.campusfix.exception.UserException;
import com.anurag.campusfix.repository.ComplaintHistoryRepository;
import com.anurag.campusfix.repository.ComplaintImageRepository;
import com.anurag.campusfix.repository.ComplaintRepository;
import com.anurag.campusfix.repository.UserRepository;
import com.anurag.campusfix.service.spec.ComplaintSpecification;

@Service
public class ComplaintService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALPHANUM = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final ComplaintRepository complaintRepository;
    private final ComplaintHistoryRepository historyRepository;
    private final ComplaintImageRepository imageRepository;
    private final UserRepository userRepository;
    private final ComplaintFileStorageService fileStorageService;
    private final ApplicationEventPublisher eventPublisher;

    @org.springframework.beans.factory.annotation.Autowired
    public ComplaintService(
            ComplaintRepository complaintRepository,
            ComplaintHistoryRepository historyRepository,
            ComplaintImageRepository imageRepository,
            UserRepository userRepository,
            ComplaintFileStorageService fileStorageService,
            ApplicationEventPublisher eventPublisher) {
        this.complaintRepository = complaintRepository;
        this.historyRepository = historyRepository;
        this.imageRepository = imageRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
        this.eventPublisher = eventPublisher;
    }

    public ComplaintService(
            ComplaintRepository complaintRepository,
            ComplaintHistoryRepository historyRepository,
            ComplaintImageRepository imageRepository,
            UserRepository userRepository,
            ComplaintFileStorageService fileStorageService) {
        this(complaintRepository, historyRepository, imageRepository, userRepository, fileStorageService, null);
    }

    /**
     * Submit a new complaint by authenticated student.
     */
    @Transactional
    public ComplaintResponse submitComplaint(ComplaintCreateRequest request, List<MultipartFile> files, UserEntity reporter) {
        ComplaintEntity complaint = new ComplaintEntity();
        complaint.setReferenceNumber(generateUniqueReference());
        complaint.setTitle(safeTrim(request.getTitle()));
        complaint.setDescription(safeTrim(request.getDescription()));
        complaint.setCategory(request.getCategory());
        complaint.setPriority(request.getPriority() != null ? request.getPriority() : ComplaintPriority.MEDIUM);
        complaint.setStatus(ComplaintStatus.SUBMITTED);
        complaint.setBuilding(safeTrim(request.getBuilding()));
        complaint.setFloor(safeTrim(request.getFloor()));
        complaint.setRoom(safeTrim(request.getRoom()));
        complaint.setReporter(reporter);

        // Process attachments if present
        if (files != null && !files.isEmpty()) {
            validateAndAttachImages(complaint, files);
        }

        ComplaintEntity savedComplaint = complaintRepository.save(complaint);

        // Record history
        ComplaintHistoryEntity history = new ComplaintHistoryEntity(
                savedComplaint,
                "SUBMITTED",
                null,
                ComplaintStatus.SUBMITTED,
                reporter,
                "Complaint submitted by " + reporter.getName()
        );
        historyRepository.save(history);

        return ComplaintResponse.fromEntity(savedComplaint);
    }

    /**
     * Retrieve complaint details with record-level authorization.
     */
    @Transactional(readOnly = true)
    public ComplaintResponse getComplaintDetails(Long id, UserEntity currentUser) {
        ComplaintEntity complaint = findComplaintOrThrow(id);
        enforceViewAccess(complaint, currentUser);
        return ComplaintResponse.fromEntity(complaint);
    }

    /**
     * Retrieve complaint by reference number with record-level authorization.
     */
    @Transactional(readOnly = true)
    public ComplaintResponse getComplaintByReference(String referenceNumber, UserEntity currentUser) {
        ComplaintEntity complaint = complaintRepository.findByReferenceNumber(referenceNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with reference: " + referenceNumber));
        enforceViewAccess(complaint, currentUser);
        return ComplaintResponse.fromEntity(complaint);
    }

    /**
     * Retrieve complaint audit history.
     */
    @Transactional(readOnly = true)
    public List<ComplaintHistoryResponse> getComplaintHistory(Long id, UserEntity currentUser) {
        ComplaintEntity complaint = findComplaintOrThrow(id);
        enforceViewAccess(complaint, currentUser);

        List<ComplaintHistoryEntity> historyList = historyRepository.findByComplaintIdOrderByTimestampAsc(id);
        return historyList.stream()
                .map(ComplaintHistoryResponse::fromEntity)
                .toList();
    }

    /**
     * List complaints reported by the authenticated student.
     */
    @Transactional(readOnly = true)
    public PagedResponse<ComplaintResponse> getMyComplaints(
            UserEntity currentUser,
            String search,
            ComplaintCategory category,
            ComplaintPriority priority,
            ComplaintStatus status,
            Pageable pageable) {

        Specification<ComplaintEntity> spec = ComplaintSpecification.filterBy(
                search, category, priority, status, null, null, null,
                currentUser.getId(), null, null, null
        );

        Page<ComplaintEntity> page = complaintRepository.findAll(spec, pageable);
        return PagedResponse.fromPage(page.map(ComplaintResponse::fromEntity));
    }

    /**
     * List complaints assigned to the authenticated maintenance staff.
     */
    @Transactional(readOnly = true)
    public PagedResponse<ComplaintResponse> getAssignedComplaints(
            UserEntity currentUser,
            String search,
            ComplaintCategory category,
            ComplaintPriority priority,
            ComplaintStatus status,
            Pageable pageable) {

        Specification<ComplaintEntity> spec = ComplaintSpecification.filterBy(
                search, category, priority, status, null, null, null,
                null, currentUser.getId(), null, null
        );

        Page<ComplaintEntity> page = complaintRepository.findAll(spec, pageable);
        return PagedResponse.fromPage(page.map(ComplaintResponse::fromEntity));
    }

    /**
     * Admin: List all complaints with full search, filtering, and pagination.
     */
    @Transactional(readOnly = true)
    public PagedResponse<ComplaintResponse> getAllComplaintsForAdmin(
            String search,
            ComplaintCategory category,
            ComplaintPriority priority,
            ComplaintStatus status,
            String building,
            String floor,
            String room,
            Long reporterId,
            Long staffId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable) {

        Specification<ComplaintEntity> spec = ComplaintSpecification.filterBy(
                search, category, priority, status, building, floor, room,
                reporterId, staffId, startDate, endDate
        );

        Page<ComplaintEntity> page = complaintRepository.findAll(spec, pageable);
        return PagedResponse.fromPage(page.map(ComplaintResponse::fromEntity));
    }

    /**
     * Admin: Assign or reassign maintenance staff.
     */
    @Transactional
    public ComplaintResponse assignStaff(Long complaintId, ComplaintAssignmentRequest request, UserEntity admin) {
        ComplaintEntity complaint = findComplaintOrThrow(complaintId);

        if (complaint.getStatus() == ComplaintStatus.CLOSED) {
            throw new InvalidStatusTransitionException("Cannot assign staff to a CLOSED complaint. Reopen it first.");
        }

        UserEntity staff = userRepository.findById(request.getStaffId())
                .orElseThrow(() -> new ResourceNotFoundException("Staff member not found with ID: " + request.getStaffId()));

        if (staff.getRole() != Role.MAINTENANCE_STAFF) {
            throw new UserException("Assigned user must have the MAINTENANCE_STAFF role");
        }

        if (Boolean.FALSE.equals(staff.getIsActive())) {
            throw new UserException("Assigned staff account is inactive");
        }

        ComplaintStatus previousStatus = complaint.getStatus();
        String action = complaint.getAssignedStaff() == null ? "ASSIGNED" : "REASSIGNED";
        String previousStaffName = complaint.getAssignedStaff() != null ? complaint.getAssignedStaff().getName() : "None";

        complaint.setAssignedStaff(staff);
        complaint.setStatus(ComplaintStatus.ASSIGNED);

        ComplaintEntity saved = complaintRepository.save(complaint);

        String desc = String.format("Assigned to staff '%s' (previously: '%s')", staff.getName(), previousStaffName);
        ComplaintHistoryEntity history = new ComplaintHistoryEntity(
                saved, action, previousStatus, ComplaintStatus.ASSIGNED, admin, desc
        );
        historyRepository.save(history);

        if (eventPublisher != null && staff != null) {
            eventPublisher.publishEvent(new ComplaintVerificationDomainEvent(
                    saved.getId(),
                    saved.getReferenceNumber(),
                    staff.getId(),
                    staff.getEmail(),
                    staff.getName(),
                    "STAFF_ASSIGNED",
                    "Assignment: Complaint #" + saved.getReferenceNumber(),
                    "You have been assigned to maintenance complaint: " + saved.getTitle()
            ));
        }

        return ComplaintResponse.fromEntity(saved);
    }

    /**
     * Admin: Update complaint priority.
     */
    @Transactional
    public ComplaintResponse updatePriority(Long complaintId, ComplaintPriorityUpdateRequest request, UserEntity admin) {
        ComplaintEntity complaint = findComplaintOrThrow(complaintId);

        if (complaint.getStatus() == ComplaintStatus.CLOSED) {
            throw new InvalidStatusTransitionException("Cannot update priority of a CLOSED complaint.");
        }

        ComplaintPriority oldPriority = complaint.getPriority();
        complaint.setPriority(request.getPriority());

        ComplaintEntity saved = complaintRepository.save(complaint);

        ComplaintHistoryEntity history = new ComplaintHistoryEntity(
                saved, "PRIORITY_UPDATED", complaint.getStatus(), complaint.getStatus(),
                admin, String.format("Priority changed from %s to %s", oldPriority, request.getPriority())
        );
        historyRepository.save(history);

        return ComplaintResponse.fromEntity(saved);
    }

    /**
     * Staff: Accept assignment.
     */
    @Transactional
    public ComplaintResponse acceptAssignment(Long complaintId, UserEntity staff) {
        ComplaintEntity complaint = findComplaintOrThrow(complaintId);
        enforceAssignedStaff(complaint, staff);

        if (complaint.getStatus() != ComplaintStatus.ASSIGNED) {
            throw new InvalidStatusTransitionException(
                    "Complaint can only be accepted when status is ASSIGNED. Current status: " + complaint.getStatus());
        }

        complaint.setStatus(ComplaintStatus.ACCEPTED);
        ComplaintEntity saved = complaintRepository.save(complaint);

        ComplaintHistoryEntity history = new ComplaintHistoryEntity(
                saved, "ACCEPTED", ComplaintStatus.ASSIGNED, ComplaintStatus.ACCEPTED,
                staff, "Assignment accepted by staff " + staff.getName()
        );
        historyRepository.save(history);

        return ComplaintResponse.fromEntity(saved);
    }

    /**
     * Staff: Start work on complaint.
     */
    @Transactional
    public ComplaintResponse startWork(Long complaintId, UserEntity staff) {
        ComplaintEntity complaint = findComplaintOrThrow(complaintId);
        enforceAssignedStaff(complaint, staff);

        if (complaint.getStatus() != ComplaintStatus.ACCEPTED && complaint.getStatus() != ComplaintStatus.REOPENED) {
            throw new InvalidStatusTransitionException(
                    "Work can only be started from ACCEPTED or REOPENED status. Current status: " + complaint.getStatus());
        }

        ComplaintStatus previousStatus = complaint.getStatus();
        complaint.setStatus(ComplaintStatus.IN_PROGRESS);
        ComplaintEntity saved = complaintRepository.save(complaint);

        ComplaintHistoryEntity history = new ComplaintHistoryEntity(
                saved, "STARTED", previousStatus, ComplaintStatus.IN_PROGRESS,
                staff, "Work started by staff " + staff.getName()
        );
        historyRepository.save(history);

        return ComplaintResponse.fromEntity(saved);
    }

    /**
     * Staff: Resolve complaint with resolution notes.
     */
    @Transactional
    public ComplaintResponse resolveComplaint(Long complaintId, ComplaintResolutionRequest request, UserEntity staff) {
        ComplaintEntity complaint = findComplaintOrThrow(complaintId);
        enforceAssignedStaff(complaint, staff);

        if (complaint.getStatus() != ComplaintStatus.IN_PROGRESS && complaint.getStatus() != ComplaintStatus.REOPENED) {
            throw new InvalidStatusTransitionException(
                    "Complaint must be IN_PROGRESS or REOPENED before it can be resolved. Current status: " + complaint.getStatus());
        }

        if (request == null || request.getResolutionNotes() == null || request.getResolutionNotes().isBlank()) {
            throw new IllegalArgumentException("Resolution notes are required to resolve a complaint");
        }

        String notes = request.getResolutionNotes().trim();
        LocalDateTime now = LocalDateTime.now();
        ComplaintStatus previousStatus = complaint.getStatus();
        complaint.setStatus(ComplaintStatus.RESOLVED);
        complaint.setResolvedAt(now);
        complaint.setResolutionNotes(notes);
        complaint.setVerificationStatus(VerificationStatus.PENDING);
        complaint.setVerifiedAt(null);
        complaint.setVerifiedBy(null);
        complaint.setVerificationComment(null);

        int nextCycle = complaint.getVerificationCycles().size() + 1;
        ComplaintVerificationEntity cycle = new ComplaintVerificationEntity(
                complaint, nextCycle, now, notes, complaint.getAfterImageStorageKey()
        );
        complaint.addVerificationCycle(cycle);

        ComplaintEntity saved = complaintRepository.save(complaint);

        ComplaintHistoryEntity history = new ComplaintHistoryEntity(
                saved, "RESOLVED", previousStatus, ComplaintStatus.RESOLVED,
                staff, "Resolved by staff: " + notes + " (Verification PENDING)"
        );
        historyRepository.save(history);

        if (eventPublisher != null && saved.getReporter() != null) {
            eventPublisher.publishEvent(new ComplaintVerificationDomainEvent(
                    saved.getId(),
                    saved.getReferenceNumber(),
                    saved.getReporter().getId(),
                    saved.getReporter().getEmail(),
                    saved.getReporter().getName(),
                    "VERIFICATION_PENDING",
                    "Action Required: Verify Resolution for Complaint #" + saved.getReferenceNumber(),
                    "Work on complaint '" + saved.getTitle() + "' has been resolved by maintenance staff. Please review the repair notes and confirm or reject."
            ));
        }

        return ComplaintResponse.fromEntity(saved);
    }

    /**
     * Admin: Close resolved complaint.
     */
    @Transactional
    public ComplaintResponse closeComplaint(Long complaintId, UserEntity admin) {
        if (admin.getRole() != Role.ADMIN) {
            throw new UnauthorizedComplaintAccessException("Only administrators can directly close a complaint");
        }
        ComplaintEntity complaint = findComplaintOrThrow(complaintId);

        if (complaint.getStatus() != ComplaintStatus.RESOLVED) {
            throw new InvalidStatusTransitionException(
                    "Only RESOLVED complaints can be closed. Current status: " + complaint.getStatus());
        }

        LocalDateTime now = LocalDateTime.now();
        complaint.setStatus(ComplaintStatus.CLOSED);
        complaint.setVerificationStatus(VerificationStatus.VERIFIED);
        complaint.setVerifiedAt(now);
        complaint.setVerifiedBy(admin);

        // Synchronize active verification cycle if pending
        if (complaint.getVerificationCycles() != null && !complaint.getVerificationCycles().isEmpty()) {
            ComplaintVerificationEntity latestCycle = complaint.getVerificationCycles().get(complaint.getVerificationCycles().size() - 1);
            if (latestCycle.getVerificationStatus() == VerificationStatus.PENDING) {
                latestCycle.setVerificationStatus(VerificationStatus.VERIFIED);
                latestCycle.setVerifiedAt(now);
                latestCycle.setVerifiedBy(admin);
                latestCycle.setVerificationComment("Closed by admin");
            }
        }

        ComplaintEntity saved = complaintRepository.save(complaint);

        ComplaintHistoryEntity history = new ComplaintHistoryEntity(
                saved, "CLOSED", ComplaintStatus.RESOLVED, ComplaintStatus.CLOSED,
                admin, "Complaint closed by admin " + admin.getName()
        );
        historyRepository.save(history);

        if (eventPublisher != null && saved.getReporter() != null) {
            eventPublisher.publishEvent(new ComplaintVerificationDomainEvent(
                    saved.getId(),
                    saved.getReferenceNumber(),
                    saved.getReporter().getId(),
                    saved.getReporter().getEmail(),
                    saved.getReporter().getName(),
                    "COMPLAINT_CLOSED",
                    "Complaint #" + saved.getReferenceNumber() + " Closed",
                    "Your complaint has been closed by administration."
            ));
        }

        return ComplaintResponse.fromEntity(saved);
    }

    /**
     * Admin or Reporter: Reopen complaint with mandatory reason.
     */
    @Transactional
    public ComplaintResponse reopenComplaint(Long complaintId, ComplaintReopenRequest request, UserEntity actor) {
        ComplaintEntity complaint = findComplaintOrThrow(complaintId);

        // Security check: Only administrators or the original student reporter can reopen
        if (actor.getRole() != Role.ADMIN) {
            if (actor.getRole() != Role.STUDENT || complaint.getReporter() == null
                    || !complaint.getReporter().getId().equals(actor.getId())) {
                throw new UnauthorizedComplaintAccessException("Only the original reporter or an administrator can reopen this complaint");
            }
        }

        if (complaint.getStatus() != ComplaintStatus.RESOLVED && complaint.getStatus() != ComplaintStatus.CLOSED) {
            throw new InvalidStatusTransitionException(
                    "Only RESOLVED or CLOSED complaints can be reopened. Current status: " + complaint.getStatus());
        }

        if (request == null || request.getReason() == null || request.getReason().isBlank()) {
            throw new IllegalArgumentException("A valid reason is required to reopen a complaint");
        }

        ComplaintStatus previousStatus = complaint.getStatus();
        LocalDateTime now = LocalDateTime.now();
        String reason = request.getReason().trim();

        complaint.setStatus(ComplaintStatus.REOPENED);
        complaint.setResolvedAt(null); // Clear resolvedAt on reopen
        complaint.setVerificationStatus(VerificationStatus.REJECTED);
        complaint.setVerifiedAt(now);
        complaint.setVerifiedBy(actor);
        complaint.setVerificationComment(reason);

        // Synchronize active verification cycle if pending
        if (complaint.getVerificationCycles() != null && !complaint.getVerificationCycles().isEmpty()) {
            ComplaintVerificationEntity latestCycle = complaint.getVerificationCycles().get(complaint.getVerificationCycles().size() - 1);
            if (latestCycle.getVerificationStatus() == VerificationStatus.PENDING) {
                latestCycle.setVerificationStatus(VerificationStatus.REJECTED);
                latestCycle.setVerifiedAt(now);
                latestCycle.setVerifiedBy(actor);
                latestCycle.setVerificationComment(reason);
            }
        }

        ComplaintEntity saved = complaintRepository.save(complaint);

        String actorLabel = actor.getRole() == Role.ADMIN ? "admin " + actor.getName() : "reporter " + actor.getName();
        ComplaintHistoryEntity history = new ComplaintHistoryEntity(
                saved, "REOPENED", previousStatus, ComplaintStatus.REOPENED,
                actor, "Reopened by " + actorLabel + ": " + reason
        );
        historyRepository.save(history);

        if (eventPublisher != null && saved.getAssignedStaff() != null) {
            eventPublisher.publishEvent(new ComplaintVerificationDomainEvent(
                    saved.getId(),
                    saved.getReferenceNumber(),
                    saved.getAssignedStaff().getId(),
                    saved.getAssignedStaff().getEmail(),
                    saved.getAssignedStaff().getName(),
                    "COMPLAINT_REOPENED",
                    "Complaint #" + saved.getReferenceNumber() + " Reopened",
                    "Complaint has been reopened with reason: " + reason
            ));
        }

        return ComplaintResponse.fromEntity(saved);
    }

    /**
     * Attach additional images to an existing complaint.
     */
    @Transactional
    public List<ComplaintImageResponse> addImages(Long complaintId, List<MultipartFile> files, UserEntity currentUser) {
        ComplaintEntity complaint = findComplaintOrThrow(complaintId);
        enforceUpdateAccess(complaint, currentUser);

        if (complaint.getStatus() == ComplaintStatus.CLOSED) {
            throw new InvalidStatusTransitionException("Cannot attach images to a CLOSED complaint. Reopen it first.");
        }

        if (files == null || files.isEmpty()) {
            throw new FileUploadValidationException("No files were provided for upload");
        }

        int currentCount = complaint.getImages().size();
        if (currentCount + files.size() > fileStorageService.getMaxImagesPerComplaint()) {
            throw new FileUploadValidationException(
                    String.format("Cannot exceed maximum of %d images per complaint. Current: %d, New: %d",
                            fileStorageService.getMaxImagesPerComplaint(), currentCount, files.size()));
        }

        for (MultipartFile file : files) {
            var stored = fileStorageService.storeFile(file);
            ComplaintImageEntity img = new ComplaintImageEntity(
                    complaint, stored.storageKey(), stored.originalFilename(), stored.contentType(), stored.fileSize()
            );
            complaint.addImage(img);
        }

        ComplaintEntity saved = complaintRepository.saveAndFlush(complaint);

        return saved.getImages().stream()
                .map(ComplaintImageResponse::fromEntity)
                .toList();
    }

    /**
     * Retrieve image resource with permission check.
     */
    @Transactional(readOnly = true)
    public ImageResourceResult getImageResource(Long complaintId, Long imageId, UserEntity currentUser) {
        ComplaintEntity complaint = findComplaintOrThrow(complaintId);
        enforceViewAccess(complaint, currentUser);

        ComplaintImageEntity imageEntity = imageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image record not found with ID: " + imageId));

        if (!imageEntity.getComplaint().getId().equals(complaint.getId())) {
            throw new UnauthorizedComplaintAccessException("Image does not belong to this complaint");
        }

        Resource resource = fileStorageService.loadAsResource(imageEntity.getStorageKey());
        return new ImageResourceResult(resource, imageEntity.getContentType(), imageEntity.getOriginalFilename());
    }

    // ==========================================
    // Internal Helper and Security Methods
    // ==========================================

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
        throw new UnauthorizedComplaintAccessException("You do not have permission to view this complaint");
    }

    private void enforceAssignedStaff(ComplaintEntity complaint, UserEntity staff) {
        if (staff.getRole() != Role.MAINTENANCE_STAFF) {
            throw new UnauthorizedComplaintAccessException("Only maintenance staff can perform this operation");
        }
        if (complaint.getAssignedStaff() == null || !complaint.getAssignedStaff().getId().equals(staff.getId())) {
            throw new UnauthorizedComplaintAccessException("This complaint is not assigned to you");
        }
    }

    private void enforceUpdateAccess(ComplaintEntity complaint, UserEntity user) {
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
        throw new UnauthorizedComplaintAccessException("You do not have permission to modify attachments on this complaint");
    }

    private void validateAndAttachImages(ComplaintEntity complaint, List<MultipartFile> files) {
        if (files.size() > fileStorageService.getMaxImagesPerComplaint()) {
            throw new FileUploadValidationException(
                    "Maximum of " + fileStorageService.getMaxImagesPerComplaint() + " images allowed per complaint");
        }
        for (MultipartFile file : files) {
            var stored = fileStorageService.storeFile(file);
            ComplaintImageEntity img = new ComplaintImageEntity(
                    complaint, stored.storageKey(), stored.originalFilename(), stored.contentType(), stored.fileSize()
            );
            complaint.addImage(img);
        }
    }

    private String generateUniqueReference() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        for (int i = 0; i < 5; i++) {
            StringBuilder sb = new StringBuilder("CMP-").append(datePart).append("-");
            for (int j = 0; j < 6; j++) {
                sb.append(ALPHANUM.charAt(RANDOM.nextInt(ALPHANUM.length())));
            }
            String candidate = sb.toString();
            if (!complaintRepository.existsByReferenceNumber(candidate)) {
                return candidate;
            }
        }
        // Fallback using formatted timestamp
        return "CMP-" + datePart + "-" + String.format("%06d", Math.abs(System.currentTimeMillis() % 1000000));
    }

    private static String safeTrim(String value) {
        return value != null ? value.trim() : "";
    }

    public record ImageResourceResult(
            Resource resource,
            String contentType,
            String filename
    ) {}
}
