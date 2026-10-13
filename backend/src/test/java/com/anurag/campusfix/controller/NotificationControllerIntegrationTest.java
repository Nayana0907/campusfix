package com.anurag.campusfix.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.anurag.campusfix.entity.NotificationEntity;
import com.anurag.campusfix.entity.Role;
import com.anurag.campusfix.entity.UserEntity;
import com.anurag.campusfix.jwt.JwtService;
import com.anurag.campusfix.repository.NotificationRepository;
import com.anurag.campusfix.repository.UserRepository;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class NotificationControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;
    private UserEntity studentUser;
    private UserEntity otherUser;
    private String studentToken;
    private String otherToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        studentUser = userRepository.save(new UserEntity(
                "Notif Student",
                "notifstudent@campusfix.edu",
                passwordEncoder.encode("Password123"),
                "9876543210",
                Role.STUDENT
        ));

        otherUser = userRepository.save(new UserEntity(
                "Other Student",
                "otherstudent@campusfix.edu",
                passwordEncoder.encode("Password123"),
                "9876543211",
                Role.STUDENT
        ));

        studentToken = jwtService.generateToken(studentUser.getEmail(), studentUser.getRole().name());
        otherToken = jwtService.generateToken(otherUser.getEmail(), otherUser.getRole().name());
    }

    @AfterEach
    void tearDown() {
        notificationRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("GET /api/notifications should require authentication")
    void testGetNotificationsUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/notifications should return only current user notifications")
    void testGetMyNotifications() throws Exception {
        notificationRepository.save(new NotificationEntity(
                studentUser,
                101L,
                "CMP-20261013-001",
                "COMPLAINT_ASSIGNED",
                "Staff Assigned",
                "Technician assigned to your ticket"
        ));

        notificationRepository.save(new NotificationEntity(
                otherUser,
                102L,
                "CMP-20261013-002",
                "COMPLAINT_RESOLVED",
                "Issue Resolved",
                "Other user notification"
        ));

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1))
                .andExpect(jsonPath("$.notifications", hasSize(1)))
                .andExpect(jsonPath("$.notifications[0].referenceNumber").value("CMP-20261013-001"))
                .andExpect(jsonPath("$.notifications[0].isRead").value(false));
    }

    @Test
    @DisplayName("PATCH /api/notifications/{id}/read should mark notification as read")
    void testMarkNotificationAsRead() throws Exception {
        NotificationEntity notif = notificationRepository.save(new NotificationEntity(
                studentUser,
                101L,
                "CMP-20261013-001",
                "COMPLAINT_ASSIGNED",
                "Staff Assigned",
                "Technician assigned"
        ));

        mockMvc.perform(patch("/api/notifications/" + notif.getId() + "/read")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("PATCH /api/notifications/{id}/read on another user's notification should return 404")
    void testCannotMarkOtherUserNotificationAsRead() throws Exception {
        NotificationEntity notif = notificationRepository.save(new NotificationEntity(
                studentUser,
                101L,
                "CMP-20261013-001",
                "COMPLAINT_ASSIGNED",
                "Staff Assigned",
                "Technician assigned"
        ));

        mockMvc.perform(patch("/api/notifications/" + notif.getId() + "/read")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /api/notifications/read-all should mark all user notifications as read")
    void testMarkAllAsRead() throws Exception {
        notificationRepository.save(new NotificationEntity(
                studentUser,
                101L,
                "CMP-20261013-001",
                "COMPLAINT_ASSIGNED",
                "Subject 1",
                "Msg 1"
        ));
        notificationRepository.save(new NotificationEntity(
                studentUser,
                102L,
                "CMP-20261013-002",
                "COMPLAINT_RESOLVED",
                "Subject 2",
                "Msg 2"
        ));

        mockMvc.perform(patch("/api/notifications/read-all")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0))
                .andExpect(jsonPath("$.notifications", hasSize(2)))
                .andExpect(jsonPath("$.notifications[0].isRead").value(true))
                .andExpect(jsonPath("$.notifications[1].isRead").value(true));
    }
}
