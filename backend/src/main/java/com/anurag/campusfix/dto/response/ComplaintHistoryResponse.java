package com.anurag.campusfix.dto.response;

import java.time.LocalDateTime;
import com.anurag.campusfix.entity.ComplaintHistoryEntity;
import com.anurag.campusfix.entity.ComplaintStatus;
import com.anurag.campusfix.entity.Role;

public class ComplaintHistoryResponse {

    private Long id;
    private Long complaintId;
    private String action;
    private ComplaintStatus previousStatus;
    private ComplaintStatus newStatus;
    private Long actorId;
    private String actorName;
    private Role actorRole;
    private String description;
    private LocalDateTime timestamp;

    public ComplaintHistoryResponse() {
    }

    public static ComplaintHistoryResponse fromEntity(ComplaintHistoryEntity entity) {
        if (entity == null) {
            return null;
        }
        ComplaintHistoryResponse res = new ComplaintHistoryResponse();
        res.setId(entity.getId());
        if (entity.getComplaint() != null) {
            res.setComplaintId(entity.getComplaint().getId());
        }
        res.setAction(entity.getAction());
        res.setPreviousStatus(entity.getPreviousStatus());
        res.setNewStatus(entity.getNewStatus());
        if (entity.getActor() != null) {
            res.setActorId(entity.getActor().getId());
        }
        res.setActorName(entity.getActorName());
        res.setActorRole(entity.getActorRole());
        res.setDescription(entity.getDescription());
        res.setTimestamp(entity.getTimestamp());
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

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public ComplaintStatus getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(ComplaintStatus previousStatus) {
        this.previousStatus = previousStatus;
    }

    public ComplaintStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(ComplaintStatus newStatus) {
        this.newStatus = newStatus;
    }

    public Long getActorId() {
        return actorId;
    }

    public void setActorId(Long actorId) {
        this.actorId = actorId;
    }

    public String getActorName() {
        return actorName;
    }

    public void setActorName(String actorName) {
        this.actorName = actorName;
    }

    public Role getActorRole() {
        return actorRole;
    }

    public void setActorRole(Role actorRole) {
        this.actorRole = actorRole;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
