package com.anurag.campusfix.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.anurag.campusfix.entity.Role;
import com.anurag.campusfix.entity.UserEntity;
import com.anurag.campusfix.jwt.JwtService;
import com.anurag.campusfix.repository.UserRepository;

import jakarta.servlet.http.Cookie;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class AuthControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.anurag.campusfix.repository.ComplaintVerificationRepository complaintVerificationRepository;

    @Autowired
    private com.anurag.campusfix.repository.ComplaintHistoryRepository complaintHistoryRepository;

    @Autowired
    private com.anurag.campusfix.repository.ComplaintImageRepository complaintImageRepository;

    @Autowired
    private com.anurag.campusfix.repository.ComplaintRepository complaintRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        complaintVerificationRepository.deleteAll();
        complaintImageRepository.deleteAll();
        complaintHistoryRepository.deleteAll();
        complaintRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/auth/register should register a user and return 201 Created without exposing password")
    void testRegisterSuccess() throws Exception {
        String requestJson = """
            {
                "name": "Nayana",
                "email": "nayana@example.com",
                "password": "StrongPassword123"
            }
            """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Nayana"))
                .andExpect(jsonPath("$.email").value("nayana@example.com"))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/auth/register should reject duplicate email with 409 Conflict")
    void testRegisterDuplicateEmail() throws Exception {
        String requestJson = """
            {
                "name": "Nayana",
                "email": "duplicate@example.com",
                "password": "StrongPassword123"
            }
            """;

        // First registration
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated());

        // Duplicate registration
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("POST /api/auth/register should fail validation for invalid inputs")
    void testRegisterValidationFailure() throws Exception {
        String invalidJson = """
            {
                "name": "",
                "email": "not-an-email",
                "password": "123"
            }
            """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    @DisplayName("Public registration cannot assign ADMIN role")
    void testPublicRegistrationCannotAssignAdmin() throws Exception {
        String adminAttemptJson = """
            {
                "name": "Admin Sneak",
                "email": "admin.sneak@example.com",
                "password": "StrongPassword123",
                "role": "ADMIN"
            }
            """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(adminAttemptJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    @DisplayName("Public registration cannot assign MAINTENANCE_STAFF role")
    void testPublicRegistrationCannotAssignMaintenanceStaff() throws Exception {
        String staffAttemptJson = """
            {
                "name": "Staff Sneak",
                "email": "staff.sneak@example.com",
                "password": "StrongPassword123",
                "role": "MAINTENANCE_STAFF"
            }
            """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(staffAttemptJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    @DisplayName("POST /api/auth/login should return access token and profile on valid credentials")
    void testLoginSuccess() throws Exception {
        String registerJson = """
            {
                "name": "Login Tester",
                "email": "login.test@example.com",
                "password": "CorrectPassword123"
            }
            """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isCreated());

        String loginJson = """
            {
                "email": "login.test@example.com",
                "password": "CorrectPassword123"
            }
            """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("login.test@example.com"))
                .andExpect(jsonPath("$.user.role").value("STUDENT"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(cookie().exists("refreshToken"));
    }

    @Test
    @DisplayName("POST /api/auth/login should reject incorrect password with 401")
    void testLoginInvalidPassword() throws Exception {
        String registerJson = """
            {
                "name": "User One",
                "email": "user1@example.com",
                "password": "CorrectPassword123"
            }
            """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isCreated());

        String badLoginJson = """
            {
                "email": "user1@example.com",
                "password": "WrongPassword999"
            }
            """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badLoginJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("POST /api/auth/login should reject inactive user with 403 Forbidden")
    void testLoginInactiveUser() throws Exception {
        UserEntity inactiveUser = new UserEntity(
                "Inactive Guy",
                "inactive@example.com",
                passwordEncoder.encode("Password123"),
                null,
                Role.STUDENT
        );
        inactiveUser.setIsActive(false);
        userRepository.save(inactiveUser);

        String loginJson = """
            {
                "email": "inactive@example.com",
                "password": "Password123"
            }
            """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_INACTIVE"));
    }

    @Test
    @DisplayName("Protected endpoint GET /api/auth/me should reject unauthenticated requests with 401")
    void testProtectedEndpointUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Protected endpoint GET /api/auth/me should reject invalid JWT with 401")
    void testProtectedEndpointInvalidJwt() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer invalid.bogus.jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Protected endpoint GET /api/auth/me should succeed with valid JWT")
    void testProtectedEndpointValidJwt() throws Exception {
        String registerJson = """
            {
                "name": "Valid User",
                "email": "valid.user@example.com",
                "password": "StrongPassword123"
            }
            """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isCreated());

        String token = jwtService.generateToken("valid.user@example.com", "STUDENT");

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Valid User"))
                .andExpect(jsonPath("$.email").value("valid.user@example.com"))
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    @DisplayName("POST /api/auth/refresh should issue new access token when valid refresh token is passed")
    void testRefreshTokenSuccess() throws Exception {
        UserEntity user = new UserEntity(
                "Refresher",
                "refresh@example.com",
                passwordEncoder.encode("Password123"),
                null,
                Role.STUDENT
        );
        userRepository.save(user);

        String refreshToken = jwtService.generateRefreshToken("refresh@example.com");

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refreshToken", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("refresh@example.com"));
    }

    @Test
    @DisplayName("POST /api/auth/logout should clear refresh cookie")
    void testLogoutClearsCookie() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(cookie().maxAge("refreshToken", 0));
    }

    @Test
    @DisplayName("GET /api/health should return 200 OK and healthy status")
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.service").value("campusfix-backend"))
                .andExpect(jsonPath("$.status").value("healthy"))
                .andExpect(jsonPath("$.database").value("connected"));
    }

    @Test
    @DisplayName("CORS preflight request on /api/auth/login should allow configured origin")
    void testCorsPreflight() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }
}
