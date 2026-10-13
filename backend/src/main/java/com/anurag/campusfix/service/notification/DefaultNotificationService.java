package com.anurag.campusfix.service.notification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.anurag.campusfix.dto.response.NotificationResponse;
import com.anurag.campusfix.entity.NotificationEntity;
import com.anurag.campusfix.entity.UserEntity;
import com.anurag.campusfix.event.ComplaintVerificationDomainEvent;
import com.anurag.campusfix.exception.ResourceNotFoundException;
import com.anurag.campusfix.repository.NotificationRepository;
import com.anurag.campusfix.repository.UserRepository;

@Service
public class DefaultNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(DefaultNotificationService.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final List<NotificationRecord> notificationRecords = new CopyOnWriteArrayList<>();

    public DefaultNotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void sendNotification(ComplaintVerificationDomainEvent event) {
        if (event == null || event.getRecipientId() == null) {
            return;
        }

        // Deduplication safeguard: avoid duplicate notifications sent within 5 seconds for same event and complaint
        LocalDateTime fiveSecondsAgo = LocalDateTime.now().minusSeconds(5);
        if (notificationRepository.existsByRecipientIdAndComplaintIdAndEventTypeAndCreatedAtAfter(
                event.getRecipientId(), event.getComplaintId(), event.getEventType(), fiveSecondsAgo)) {
            log.debug("Skipping duplicate notification for recipient {} on complaint {}",
                    event.getRecipientId(), event.getReferenceNumber());
            return;
        }

        UserEntity recipient = userRepository.findById(event.getRecipientId()).orElse(null);
        if (recipient != null) {
            NotificationEntity entity = new NotificationEntity(
                    recipient,
                    event.getComplaintId(),
                    event.getReferenceNumber(),
                    event.getEventType(),
                    event.getSubject(),
                    event.getMessage()
            );
            notificationRepository.save(entity);
        }

        NotificationRecord record = new NotificationRecord(
                event.getComplaintId(),
                event.getReferenceNumber(),
                event.getRecipientId(),
                event.getRecipientEmail(),
                event.getRecipientName(),
                event.getEventType(),
                event.getSubject(),
                event.getMessage(),
                LocalDateTime.now()
        );

        notificationRecords.add(record);

        log.info("[NOTIFICATION] Event='{}' To='{}' <{}> Subject='{}' Message='{}' (Complaint #{}: {})",
                event.getEventType(),
                event.getRecipientName(),
                event.getRecipientEmail(),
                event.getSubject(),
                event.getMessage(),
                event.getComplaintId(),
                event.getReferenceNumber());
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(UserEntity user, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        PageRequest pageRequest = PageRequest.of(safePage, safeSize);

        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(user.getId(), pageRequest)
                .stream()
                .map(NotificationResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(UserEntity user) {
        return notificationRepository.countByRecipientIdAndIsReadFalse(user.getId());
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, UserEntity user) {
        NotificationEntity notification = notificationRepository.findByIdAndRecipientId(notificationId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with ID: " + notificationId));
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(UserEntity user) {
        notificationRepository.markAllAsReadByRecipientId(user.getId());
    }

    @Override
    public List<NotificationRecord> getRecentNotifications() {
        return Collections.unmodifiableList(new ArrayList<>(notificationRecords));
    }

    @Override
    public void clearNotifications() {
        notificationRecords.clear();
    }
}
