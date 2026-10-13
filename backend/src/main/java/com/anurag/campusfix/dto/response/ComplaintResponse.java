package com.anurag.campusfix.dto.response;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.anurag.campusfix.entity.ComplaintCategory;
import com.anurag.campusfix.entity.ComplaintEntity;
import com.anurag.campusfix.entity.ComplaintPriority;
import com.anurag.campusfix.entity.ComplaintStatus;

public class ComplaintResponse {

    private Long id;
    private String referenceNumber;
    private String title;
    private String description;
    private ComplaintCategory category;
    private ComplaintPriority priority;
    private ComplaintStatus status;
    private String building;
    private String floor;
    private String room;
    private UserResponse reporter;
    private UserResponse assignedStaff;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
    private String resolutionNotes;
    private com.anurag.campusfix.entity.VerificationStatus verificationStatus;
    private LocalDateTime verifiedAt;
    private UserResponse verifiedBy;
    private String verificationComment;
    private String afterImageUrl;
    private List<ComplaintImageResponse> images = new ArrayList<>();

    public ComplaintResponse() {
    }

    public static ComplaintResponse fromEntity(ComplaintEntity entity) {
        if (entity == null) {
            return null;
        }
        ComplaintResponse res = new ComplaintResponse();
        res.setId(entity.getId());
        res.setReferenceNumber(entity.getReferenceNumber());
        res.setTitle(entity.getTitle());
        res.setDescription(entity.getDescription());
        res.setCategory(entity.getCategory());
        res.setPriority(entity.getPriority());
        res.setStatus(entity.getStatus());
        res.setBuilding(entity.getBuilding());
        res.setFloor(entity.getFloor());
        res.setRoom(entity.getRoom());
        res.setReporter(UserResponse.fromEntity(entity.getReporter()));
        res.setAssignedStaff(UserResponse.fromEntity(entity.getAssignedStaff()));
        res.setCreatedAt(entity.getCreatedAt());
        res.setUpdatedAt(entity.getUpdatedAt());
        res.setResolvedAt(entity.getResolvedAt());
        res.setResolutionNotes(entity.getResolutionNotes());
        res.setVerificationStatus(entity.getVerificationStatus());
        res.setVerifiedAt(entity.getVerifiedAt());
        res.setVerifiedBy(UserResponse.fromEntity(entity.getVerifiedBy()));
        res.setVerificationComment(entity.getVerificationComment());
        if (entity.getAfterImageStorageKey() != null) {
            res.setAfterImageUrl("/api/complaints/" + entity.getId() + "/verification/after-image");
        }

        if (entity.getImages() != null && !entity.getImages().isEmpty()) {
            res.setImages(entity.getImages().stream()
                    .map(ComplaintImageResponse::fromEntity)
                    .toList());
        }
        return res;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ComplaintCategory getCategory() {
        return category;
    }

    public void setCategory(ComplaintCategory category) {
        this.category = category;
    }

    public ComplaintPriority getPriority() {
        return priority;
    }

    public void setPriority(ComplaintPriority priority) {
        this.priority = priority;
    }

    public ComplaintStatus getStatus() {
        return status;
    }

    public void setStatus(ComplaintStatus status) {
        this.status = status;
    }

    public String getBuilding() {
        return building;
    }

    public void setBuilding(String building) {
        this.building = building;
    }

    public String getFloor() {
        return floor;
    }

    public void setFloor(String floor) {
        this.floor = floor;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public UserResponse getReporter() {
        return reporter;
    }

    public void setReporter(UserResponse reporter) {
        this.reporter = reporter;
    }

    public UserResponse getAssignedStaff() {
        return assignedStaff;
    }

    public void setAssignedStaff(UserResponse assignedStaff) {
        this.assignedStaff = assignedStaff;
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

    public List<ComplaintImageResponse> getImages() {
        return images;
    }

    public void setImages(List<ComplaintImageResponse> images) {
        this.images = images;
    }

    public com.anurag.campusfix.entity.VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(com.anurag.campusfix.entity.VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public UserResponse getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(UserResponse verifiedBy) {
        this.verifiedBy = verifiedBy;
    }

    public String getVerificationComment() {
        return verificationComment;
    }

    public void setVerificationComment(String verificationComment) {
        this.verificationComment = verificationComment;
    }

    public String getAfterImageUrl() {
        return afterImageUrl;
    }

    public void setAfterImageUrl(String afterImageUrl) {
        this.afterImageUrl = afterImageUrl;
    }
}
