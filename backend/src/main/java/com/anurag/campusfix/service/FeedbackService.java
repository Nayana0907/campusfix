package com.anurag.campusfix.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.anurag.campusfix.dto.request.FeedbackRequest;
import com.anurag.campusfix.dto.response.FeedbackResponse;
import com.anurag.campusfix.entity.ComplaintEntity;
import com.anurag.campusfix.entity.ComplaintStatus;
import com.anurag.campusfix.entity.FeedbackEntity;
import com.anurag.campusfix.entity.Role;
import com.anurag.campusfix.entity.UserEntity;
import com.anurag.campusfix.exception.InvalidStatusTransitionException;
import com.anurag.campusfix.exception.ResourceNotFoundException;
import com.anurag.campusfix.exception.UnauthorizedComplaintAccessException;
import com.anurag.campusfix.repository.ComplaintRepository;
import com.anurag.campusfix.repository.FeedbackRepository;

@Service
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final ComplaintRepository complaintRepository;

    public FeedbackService(FeedbackRepository feedbackRepository, ComplaintRepository complaintRepository) {
        this.feedbackRepository = feedbackRepository;
        this.complaintRepository = complaintRepository;
    }

    /**
     * Submit rating and feedback for a resolved or closed complaint.
     * Enforces reporter ownership and single-submission invariant.
     */
    @Transactional
    public FeedbackResponse submitFeedback(Long complaintId, FeedbackRequest request, UserEntity student) {
        ComplaintEntity complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with ID: " + complaintId));

        if (complaint.getReporter() == null || !complaint.getReporter().getId().equals(student.getId())) {
            throw new UnauthorizedComplaintAccessException("Only the original student reporter can submit feedback for this complaint");
        }

        if (complaint.getStatus() != ComplaintStatus.RESOLVED && complaint.getStatus() != ComplaintStatus.CLOSED) {
            throw new InvalidStatusTransitionException(
                    "Feedback can only be provided after resolution. Current status: " + complaint.getStatus());
        }

        if (feedbackRepository.existsByComplaintId(complaintId)) {
            throw new IllegalStateException("Feedback has already been submitted for this complaint");
        }

        String comment = request.getComment() != null ? request.getComment().trim() : null;
        FeedbackEntity feedback = new FeedbackEntity(
                complaint,
                student,
                complaint.getAssignedStaff(),
                request.getRating(),
                comment
        );

        FeedbackEntity saved = feedbackRepository.save(feedback);
        return FeedbackResponse.fromEntity(saved);
    }

    /**
     * Retrieve feedback for a complaint if authorized.
     */
    @Transactional(readOnly = true)
    public FeedbackResponse getFeedback(Long complaintId, UserEntity currentUser) {
        ComplaintEntity complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with ID: " + complaintId));

        boolean isReporter = complaint.getReporter() != null && complaint.getReporter().getId().equals(currentUser.getId());
        boolean isStaff = complaint.getAssignedStaff() != null && complaint.getAssignedStaff().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;

        if (!isReporter && !isStaff && !isAdmin) {
            throw new UnauthorizedComplaintAccessException("You are not authorized to view feedback for this complaint");
        }

        return feedbackRepository.findByComplaintId(complaintId)
                .map(FeedbackResponse::fromEntity)
                .orElse(null);
    }
}
