package com.anurag.campusfix.controller;

import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.anurag.campusfix.dto.response.NotificationResponse;
import com.anurag.campusfix.entity.UserEntity;
import com.anurag.campusfix.exception.AccountInactiveException;
import com.anurag.campusfix.exception.ResourceNotFoundException;
import com.anurag.campusfix.repository.UserRepository;
import com.anurag.campusfix.service.notification.NotificationService;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public NotificationController(NotificationService notificationService, UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    /**
     * List notifications for currently authenticated user with unread counter.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getMyNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        UserEntity currentUser = resolveCurrentUser(authentication);
        List<NotificationResponse> notifications = notificationService.getMyNotifications(currentUser, page, size);
        long unreadCount = notificationService.getUnreadCount(currentUser);

        return ResponseEntity.ok(Map.of(
                "notifications", notifications,
                "unreadCount", unreadCount
        ));
    }

    /**
     * Mark a single notification as read.
     */
    @PatchMapping("/{id:[0-9]+}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(
            @PathVariable Long id,
            Authentication authentication) {

        UserEntity currentUser = resolveCurrentUser(authentication);
        notificationService.markAsRead(id, currentUser);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Notification marked as read"
        ));
    }

    /**
     * Mark all notifications for currently authenticated user as read.
     */
    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead(
            Authentication authentication) {

        UserEntity currentUser = resolveCurrentUser(authentication);
        notificationService.markAllAsRead(currentUser);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "All notifications marked as read"
        ));
    }

    private UserEntity resolveCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + email));
        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new AccountInactiveException("Account is inactive. Please contact support.");
        }
        return user;
    }
}
