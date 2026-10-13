package com.anurag.campusfix.dto.response;

import java.time.LocalDateTime;
import com.anurag.campusfix.entity.ComplaintVerificationEntity;
import com.anurag.campusfix.entity.VerificationStatus;

public class VerificationHistoryResponse {

    private Long id;
    private Long complaintId;
    private Integer cycleNumber;
    private LocalDateTime resolvedAt;
    private String resolutionNotes;
    private String afterImageUrl;
    private VerificationStatus verificationStatus;
    private UserResponse verifiedBy;
    private LocalDateTime verifiedAt;
    private String verificationComment;
    private boolean isOverriddenByAdmin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public VerificationHistoryResponse() {
    }

    public static VerificationHistoryResponse fromEntity(ComplaintVerificationEntity entity) {
        if (entity == null) {
            return null;
        }
        VerificationHistoryResponse res = new VerificationHistoryResponse();
        res.setId(entity.getId());
        if (entity.getComplaint() != null) {
            res.setComplaintId(entity.getComplaint().getId());
            if (entity.getAfterImageStorageKey() != null) {
                res.setAfterImageUrl("/api/complaints/" + entity.getComplaint().getId() + "/verification/after-image");
            }
        }
        res.setCycleNumber(entity.getCycleNumber());
        res.setResolvedAt(entity.getResolvedAt());
        res.setResolutionNotes(entity.getResolutionNotes());
        res.setVerificationStatus(entity.getVerificationStatus());
        res.setVerifiedBy(UserResponse.fromEntity(entity.getVerifiedBy()));
        res.setVerifiedAt(entity.getVerifiedAt());
        res.setVerificationComment(entity.getVerificationComment());
        res.setOverriddenByAdmin(entity.isOverriddenByAdmin());
        res.setCreatedAt(entity.getCreatedAt());
        res.setUpdatedAt(entity.getUpdatedAt());
        return res;
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

    public Integer getCycleNumber() {
        return cycleNumber;
    }

    public void setCycleNumber(Integer cycleNumber) {
        this.cycleNumber = cycleNumber;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }

    public String getAfterImageUrl() {
        return afterImageUrl;
    }

    public void setAfterImageUrl(String afterImageUrl) {
        this.afterImageUrl = afterImageUrl;
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public UserResponse getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(UserResponse verifiedBy) {
        this.verifiedBy = verifiedBy;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public String getVerificationComment() {
        return verificationComment;
    }

    public void setVerificationComment(String verificationComment) {
        this.verificationComment = verificationComment;
    }

    public boolean isOverriddenByAdmin() {
        return isOverriddenByAdmin;
    }

    public void setOverriddenByAdmin(boolean overriddenByAdmin) {
        isOverriddenByAdmin = overriddenByAdmin;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
