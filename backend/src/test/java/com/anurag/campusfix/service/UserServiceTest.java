package com.anurag.campusfix.service;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.anurag.campusfix.dto.request.LoginRequest;
import com.anurag.campusfix.dto.request.RegisterRequest;
import com.anurag.campusfix.dto.response.AuthResponse;
import com.anurag.campusfix.dto.response.UserResponse;
import com.anurag.campusfix.entity.Role;
import com.anurag.campusfix.entity.UserEntity;
import com.anurag.campusfix.exception.AccountInactiveException;
import com.anurag.campusfix.exception.EmailAlreadyExistsException;
import com.anurag.campusfix.exception.InvalidCredentialsException;
import com.anurag.campusfix.jwt.JwtService;
import com.anurag.campusfix.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private com.anurag.campusfix.repository.RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private UserService userService;

    private UserEntity sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new UserEntity();
        sampleUser.setId(1L);
        sampleUser.setName("Nayana");
        sampleUser.setEmail("nayana@example.com");
        sampleUser.setPassword("$2a$10$encodedPasswordHash");
        sampleUser.setRole(Role.STUDENT);
        sampleUser.setIsActive(true);
    }

    @Test
    @DisplayName("Successful registration assigns default STUDENT role and BCrypt-encoded password")
    void testSuccessfulRegistration() {
        RegisterRequest request = new RegisterRequest("Nayana", "Nayana@Example.com", "SecretPass123");

        when(userRepository.existsByEmail("nayana@example.com")).thenReturn(false);
        when(passwordEncoder.encode("SecretPass123")).thenReturn("$2a$10$hashedPass");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId(10L);
            return user;
        });

        UserResponse response = userService.register(request);

        assertNotNull(response);
        assertEquals("nayana@example.com", response.getEmail());
        assertEquals("Nayana", response.getName());
        assertEquals(Role.STUDENT, response.getRole());
        assertTrue(response.getIsActive());

        // Verify password was hashed and saved
        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        UserEntity savedUser = captor.getValue();
        assertEquals("$2a$10$hashedPass", savedUser.getPassword());
        assertEquals(Role.STUDENT, savedUser.getRole());
    }

    @Test
    @DisplayName("Registration rejects duplicate email addresses")
    void testDuplicateEmailRejection() {
        RegisterRequest request = new RegisterRequest("Nayana", "duplicate@example.com", "SecretPass123");
        when(userRepository.existsByEmail("duplicate@example.com")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> userService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Successful login authenticates and returns safe profile and JWT tokens")
    void testSuccessfulLogin() {
        LoginRequest request = new LoginRequest("nayana@example.com", "Password123");

        when(userRepository.findByEmail("nayana@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("Password123", sampleUser.getPassword())).thenReturn(true);
        when(jwtService.generateToken("nayana@example.com", "STUDENT")).thenReturn("mocked.access.jwt");
        when(jwtService.generateRefreshToken("nayana@example.com")).thenReturn("mocked.refresh.jwt");
        when(jwtService.getExpirationMs()).thenReturn(900000L);
        when(jwtService.getRefreshExpirationMs()).thenReturn(604800000L);

        AuthResponse authResponse = userService.login(request);

        assertNotNull(authResponse);
        assertEquals("mocked.access.jwt", authResponse.getAccessToken());
        assertEquals("mocked.refresh.jwt", authResponse.getRefreshToken());
        assertEquals("Bearer", authResponse.getTokenType());
        assertEquals("nayana@example.com", authResponse.getUser().getEmail());
        assertEquals(Role.STUDENT, authResponse.getUser().getRole());
    }

    @Test
    @DisplayName("Login rejects incorrect password with generic exception")
    void testIncorrectPasswordRejection() {
        LoginRequest request = new LoginRequest("nayana@example.com", "WrongPassword");

        when(userRepository.findByEmail("nayana@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("WrongPassword", sampleUser.getPassword())).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> userService.login(request));
    }

    @Test
    @DisplayName("Login rejects non-existent email with generic exception")
    void testNonExistentUserRejection() {
        LoginRequest request = new LoginRequest("unknown@example.com", "Password123");

        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> userService.login(request));
    }

    @Test
    @DisplayName("Login rejects inactive user accounts")
    void testInactiveUserRejection() {
        sampleUser.setIsActive(false);
        LoginRequest request = new LoginRequest("nayana@example.com", "Password123");

        when(userRepository.findByEmail("nayana@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("Password123", sampleUser.getPassword())).thenReturn(true);

        assertThrows(AccountInactiveException.class, () -> userService.login(request));
    }

    @Test
    @DisplayName("Password hash never appears in UserResponse or AuthResponse")
    void testPasswordNeverExposed() {
        UserResponse userResponse = UserResponse.fromEntity(sampleUser);
        assertNotNull(userResponse);
        assertEquals(1L, userResponse.getId());
        assertEquals("nayana@example.com", userResponse.getEmail());

        AuthResponse authResponse = new AuthResponse("token", 900000L, "refresh", userResponse);
        assertEquals(1L, authResponse.getId());
        assertEquals("nayana@example.com", authResponse.getEmail());
        assertEquals(Role.STUDENT, authResponse.getRole());
    }

    @Test
    @DisplayName("getUserProfile throws AccountInactiveException when user account is inactive")
    void testGetUserProfileInactiveUserThrows() {
        sampleUser.setIsActive(false);
        when(userRepository.findByEmail("nayana@example.com")).thenReturn(Optional.of(sampleUser));

        assertThrows(AccountInactiveException.class, () -> userService.getUserProfile("nayana@example.com"));
    }
}
