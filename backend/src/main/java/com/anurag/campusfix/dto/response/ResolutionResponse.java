package com.anurag.campusfix.dto.response;

import java.time.LocalDateTime;
import com.anurag.campusfix.entity.ComplaintEntity;
import com.anurag.campusfix.entity.ComplaintStatus;
import com.anurag.campusfix.entity.ComplaintVerificationEntity;
import com.anurag.campusfix.entity.VerificationStatus;

public class ResolutionResponse {

    private Long complaintId;
    private String referenceNumber;
    private ComplaintStatus complaintStatus;
    private VerificationStatus verificationStatus;
    private Integer cycleNumber;
    private LocalDateTime resolvedAt;
    private String resolutionNotes;
    private String afterImageUrl;

    public ResolutionResponse() {
    }

    public static ResolutionResponse from(ComplaintEntity complaint, ComplaintVerificationEntity cycle) {
        ResolutionResponse res = new ResolutionResponse();
        res.setComplaintId(complaint.getId());
        res.setReferenceNumber(complaint.getReferenceNumber());
        res.setComplaintStatus(complaint.getStatus());
        res.setVerificationStatus(complaint.getVerificationStatus());
        if (cycle != null) {
            res.setCycleNumber(cycle.getCycleNumber());
            res.setResolvedAt(cycle.getResolvedAt());
            res.setResolutionNotes(cycle.getResolutionNotes());
            if (cycle.getAfterImageStorageKey() != null) {
                res.setAfterImageUrl("/api/complaints/" + complaint.getId() + "/verification/after-image");
            }
        } else {
            res.setResolvedAt(complaint.getResolvedAt());
            res.setResolutionNotes(complaint.getResolutionNotes());
            if (complaint.getAfterImageStorageKey() != null) {
                res.setAfterImageUrl("/api/complaints/" + complaint.getId() + "/verification/after-image");
            }
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
}
