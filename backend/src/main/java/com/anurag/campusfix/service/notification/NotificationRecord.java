package com.anurag.campusfix.service.notification;

import java.time.LocalDateTime;

public record NotificationRecord(
        Long complaintId,
        String referenceNumber,
        Long recipientId,
        String recipientEmail,
        String recipientName,
        String eventType,
        String subject,
        String message,
        LocalDateTime sentAt
) {
}
