package com.anurag.campusfix.dto.response;

import java.time.LocalDateTime;
import com.anurag.campusfix.entity.NotificationEntity;

public class NotificationResponse {

    private Long id;
    private Long complaintId;
    private String referenceNumber;
    private String eventType;
    private String subject;
    private String message;
    private Boolean isRead;
    private LocalDateTime createdAt;

    public NotificationResponse() {
    }

    public NotificationResponse(Long id, Long complaintId, String referenceNumber, String eventType,
                                String subject, String message, Boolean isRead, LocalDateTime createdAt) {
        this.id = id;
        this.complaintId = complaintId;
        this.referenceNumber = referenceNumber;
        this.eventType = eventType;
        this.subject = subject;
        this.message = message;
        this.isRead = isRead;
        this.createdAt = createdAt;
    }

    public static NotificationResponse fromEntity(NotificationEntity entity) {
        if (entity == null) {
            return null;
        }
        return new NotificationResponse(
                entity.getId(),
                entity.getComplaintId(),
                entity.getReferenceNumber(),
                entity.getEventType(),
                entity.getSubject(),
                entity.getMessage(),
                entity.getIsRead(),
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

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
