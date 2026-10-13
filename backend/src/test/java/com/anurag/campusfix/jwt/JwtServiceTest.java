package com.anurag.campusfix.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private static final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 900000L, 604800000L);
    }

    @Test
    @DisplayName("Should generate a valid JWT token with email and role")
    void testGenerateAndValidateToken() {
        String token = jwtService.generateToken("student@campusfix.edu", "STUDENT");

        assertNotNull(token);
        assertTrue(jwtService.isTokenValid(token));
        assertEquals("student@campusfix.edu", jwtService.extractEmail(token));
        assertEquals("STUDENT", jwtService.extractRole(token));
    }

    @Test
    @DisplayName("Should reject invalid or tampered token")
    void testInvalidToken() {
        assertFalse(jwtService.isTokenValid("invalid.jwt.token"));
        assertFalse(jwtService.isTokenValid(""));
        assertFalse(jwtService.isTokenValid(null));
    }

    @Test
    @DisplayName("Should generate and validate refresh token")
    void testRefreshToken() {
        String refreshToken = jwtService.generateRefreshToken("student@campusfix.edu");

        assertNotNull(refreshToken);
        assertTrue(jwtService.isRefreshTokenValid(refreshToken));
        assertEquals("student@campusfix.edu", jwtService.extractEmail(refreshToken));
    }

    @Test
    @DisplayName("Access token should not be considered a valid refresh token")
    void testAccessTokenIsNotRefreshToken() {
        String accessToken = jwtService.generateToken("student@campusfix.edu", "STUDENT");
        assertFalse(jwtService.isRefreshTokenValid(accessToken));
    }

    @Test
    @DisplayName("Expired token should be rejected")
    void testExpiredToken() {
        JwtService shortLivedJwtService = new JwtService(SECRET, -1000L, -1000L);
        String expiredToken = shortLivedJwtService.generateToken("user@campusfix.edu", "STUDENT");

        assertFalse(shortLivedJwtService.isTokenValid(expiredToken));
    }
}
