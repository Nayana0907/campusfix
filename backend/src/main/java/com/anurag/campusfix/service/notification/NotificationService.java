package com.anurag.campusfix.service.notification;

import java.util.List;
import com.anurag.campusfix.dto.response.NotificationResponse;
import com.anurag.campusfix.entity.UserEntity;
import com.anurag.campusfix.event.ComplaintVerificationDomainEvent;

public interface NotificationService {

    void sendNotification(ComplaintVerificationDomainEvent event);

    List<NotificationRecord> getRecentNotifications();

    void clearNotifications();

    List<NotificationResponse> getMyNotifications(UserEntity user, int page, int size);

    long getUnreadCount(UserEntity user);

    void markAsRead(Long notificationId, UserEntity user);

    void markAllAsRead(UserEntity user);
}
