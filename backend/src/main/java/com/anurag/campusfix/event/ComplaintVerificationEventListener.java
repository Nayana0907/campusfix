package com.anurag.campusfix.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.anurag.campusfix.service.notification.NotificationService;

@Component
public class ComplaintVerificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(ComplaintVerificationEventListener.class);

    private final NotificationService notificationService;

    public ComplaintVerificationEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onComplaintVerificationEvent(ComplaintVerificationDomainEvent event) {
        try {
            log.debug("Dispatching after-commit verification notification for complaint reference: {}", event.getReferenceNumber());
            notificationService.sendNotification(event);
        } catch (Exception ex) {
            log.error("Failed to dispatch notification for event {}: {}", event.getEventType(), ex.getMessage(), ex);
        }
    }
}
