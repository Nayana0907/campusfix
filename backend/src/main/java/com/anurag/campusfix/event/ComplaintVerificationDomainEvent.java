package com.anurag.campusfix.event;

public class ComplaintVerificationDomainEvent {

    private final Long complaintId;
    private final String referenceNumber;
    private final Long recipientId;
    private final String recipientEmail;
    private final String recipientName;
    private final String eventType;
    private final String subject;
    private final String message;

    public ComplaintVerificationDomainEvent(Long complaintId, String referenceNumber, Long recipientId,
                                            String recipientEmail, String recipientName, String eventType,
                                            String subject, String message) {
        this.complaintId = complaintId;
        this.referenceNumber = referenceNumber;
        this.recipientId = recipientId;
        this.recipientEmail = recipientEmail;
        this.recipientName = recipientName;
        this.eventType = eventType;
        this.subject = subject;
        this.message = message;
    }

    public Long getComplaintId() {
        return complaintId;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public Long getRecipientId() {
        return recipientId;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public String getEventType() {
        return eventType;
    }

    public String getSubject() {
        return subject;
    }

    public String getMessage() {
        return message;
    }
}
