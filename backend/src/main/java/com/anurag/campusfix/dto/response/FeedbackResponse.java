package com.anurag.campusfix.dto.response;

import java.time.LocalDateTime;
import com.anurag.campusfix.entity.FeedbackEntity;

public class FeedbackResponse {

    private Long id;
    private Long complaintId;
    private Integer rating;
    private String comment;
    private String studentName;
    private Long staffId;
    private String staffName;
    private LocalDateTime createdAt;

    public FeedbackResponse() {
    }

    public FeedbackResponse(Long id, Long complaintId, Integer rating, String comment,
                            String studentName, Long staffId, String staffName, LocalDateTime createdAt) {
        this.id = id;
        this.complaintId = complaintId;
        this.rating = rating;
        this.comment = comment;
        this.studentName = studentName;
        this.staffId = staffId;
        this.staffName = staffName;
        this.createdAt = createdAt;
    }

    public static FeedbackResponse fromEntity(FeedbackEntity entity) {
        if (entity == null) {
            return null;
        }
        return new FeedbackResponse(
                entity.getId(),
                entity.getComplaint() != null ? entity.getComplaint().getId() : null,
                entity.getRating(),
                entity.getComment(),
                entity.getStudent() != null ? entity.getStudent().getName() : null,
                entity.getStaff() != null ? entity.getStaff().getId() : null,
                entity.getStaff() != null ? entity.getStaff().getName() : null,
                entity.getCreatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getComplaintId() {
        return complaintId;
    }

    public void setComplaintId(Long complaintId) {
        this.complaintId = complaintId;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public Long getStaffId() {
        return staffId;
    }

    public void setStaffId(Long staffId) {
        this.staffId = staffId;
    }

    public String getStaffName() {
        return staffName;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
