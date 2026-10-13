package com.anurag.campusfix.dto.response;

import java.time.LocalDateTime;
import com.anurag.campusfix.entity.ComplaintEntity;
import com.anurag.campusfix.entity.ComplaintStatus;
import com.anurag.campusfix.entity.ComplaintVerificationEntity;
import com.anurag.campusfix.entity.VerificationStatus;

public class VerificationResponse {

    private Long complaintId;
    private String referenceNumber;
    private ComplaintStatus complaintStatus;
    private VerificationStatus verificationStatus;
    private Integer cycleNumber;
    private UserResponse verifiedBy;
    private LocalDateTime verifiedAt;
    private String comment;
    private boolean isOverriddenByAdmin;

    public VerificationResponse() {
    }

    public static VerificationResponse from(ComplaintEntity complaint, ComplaintVerificationEntity cycle) {
        VerificationResponse res = new VerificationResponse();
        res.setComplaintId(complaint.getId());
        res.setReferenceNumber(complaint.getReferenceNumber());
        res.setComplaintStatus(complaint.getStatus());
        res.setVerificationStatus(complaint.getVerificationStatus());
        if (cycle != null) {
            res.setCycleNumber(cycle.getCycleNumber());
            res.setVerifiedBy(UserResponse.fromEntity(cycle.getVerifiedBy()));
            res.setVerifiedAt(cycle.getVerifiedAt());
            res.setComment(cycle.getVerificationComment());
            res.setOverriddenByAdmin(cycle.isOverriddenByAdmin());
        } else {
            res.setVerifiedBy(UserResponse.fromEntity(complaint.getVerifiedBy()));
            res.setVerifiedAt(complaint.getVerifiedAt());
            res.setComment(complaint.getVerificationComment());
        }
        return res;
    }

    public Long getComplaintId() {
        return complaintId;
    }

    public void setComplaintId(Long complaintId) {
        this.complaintId = complaintId;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public ComplaintStatus getComplaintStatus() {
        return complaintStatus;
    }

    public void setComplaintStatus(ComplaintStatus complaintStatus) {
        this.complaintStatus = complaintStatus;
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public Integer getCycleNumber() {
        return cycleNumber;
    }

    public void setCycleNumber(Integer cycleNumber) {
        this.cycleNumber = cycleNumber;
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

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public boolean isOverriddenByAdmin() {
        return isOverriddenByAdmin;
    }

    public void setOverriddenByAdmin(boolean overriddenByAdmin) {
        isOverriddenByAdmin = overriddenByAdmin;
    }
}
