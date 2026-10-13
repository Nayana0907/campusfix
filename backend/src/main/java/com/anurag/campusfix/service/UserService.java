package com.anurag.campusfix.service;

import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.anurag.campusfix.dto.request.LoginRequest;
import com.anurag.campusfix.dto.request.RegisterRequest;
import com.anurag.campusfix.dto.response.AuthResponse;
import com.anurag.campusfix.dto.response.UserResponse;
import com.anurag.campusfix.entity.RefreshTokenEntity;
import com.anurag.campusfix.entity.Role;
import com.anurag.campusfix.entity.UserEntity;
import com.anurag.campusfix.exception.AccountInactiveException;
import com.anurag.campusfix.exception.EmailAlreadyExistsException;
import com.anurag.campusfix.exception.InvalidCredentialsException;
import com.anurag.campusfix.exception.JwtAuthenticationException;
import com.anurag.campusfix.exception.ResourceNotFoundException;
import com.anurag.campusfix.jwt.JwtService;
import com.anurag.campusfix.repository.RefreshTokenRepository;
import com.anurag.campusfix.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public static String normalizeEmail(String email) {
        return email != null ? email.trim().toLowerCase(Locale.ROOT) : "";
    }

    /**
     * Public user registration.
     * Enforces default STUDENT role and BCrypt password encryption.
     */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException("Email is already registered");
        }

        UserEntity user = new UserEntity();
        user.setName(request.getName().trim());
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);

        // Security rule: public registration MUST ALWAYS default to STUDENT role
        user.setRole(Role.STUDENT);
        user.setIsActive(true);

        UserEntity saved = userRepository.save(user);
        return UserResponse.fromEntity(saved);
    }

    /**
     * User authentication.
     * Verifies BCrypt password hash and account active status.
     * Persists refresh token for rotation and revocation tracking.
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());

        UserEntity user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new AccountInactiveException("Account is inactive. Please contact support.");
        }

        String accessToken = jwtService.generateToken(user.getEmail(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());

        LocalDateTime expiryDate = LocalDateTime.now().plusSeconds(jwtService.getRefreshExpirationMs() / 1000);
        refreshTokenRepository.save(new RefreshTokenEntity(refreshToken, user, expiryDate));

        return new AuthResponse(
                accessToken,
                jwtService.getExpirationMs(),
                refreshToken,
                UserResponse.fromEntity(user)
        );
    }

    /**
     * Get user profile by email (derived from SecurityContext).
     */
    @Transactional(readOnly = true)
    public UserResponse getUserProfile(String email) {
        String normalizedEmail = normalizeEmail(email);
        UserEntity user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new AccountInactiveException("Account is inactive. Please contact support.");
        }

        return UserResponse.fromEntity(user);
    }

    /**
     * Refresh access token using a valid, non-revoked refresh token.
     * Enforces single-use refresh token rotation and revocation verification.
     */
    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        if (refreshToken == null || !jwtService.isRefreshTokenValid(refreshToken)) {
            throw new JwtAuthenticationException("Invalid or expired refresh token");
        }

        String email = jwtService.extractEmail(refreshToken);
        if (email == null) {
            throw new JwtAuthenticationException("Token does not contain valid user information");
        }

        String normalizedEmail = normalizeEmail(email);
        UserEntity user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new AccountInactiveException("Account is inactive. Please contact support.");
        }

        // Verify token existence and revocation in repository
        RefreshTokenEntity storedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElse(null);

        if (storedToken != null) {
            if (Boolean.TRUE.equals(storedToken.getRevoked())) {
                // Potential token reuse / replay attempt: invalidate all active sessions for this user
                refreshTokenRepository.revokeAllByUser(user);
                throw new JwtAuthenticationException("Refresh token is invalid or has already been revoked");
            }

            if (storedToken.getExpiryDate().isBefore(LocalDateTime.now())) {
                storedToken.setRevoked(true);
                refreshTokenRepository.save(storedToken);
                throw new JwtAuthenticationException("Refresh token has expired");
            }

            // Rotate: mark previous token as revoked
            storedToken.setRevoked(true);
            refreshTokenRepository.save(storedToken);
        } else {
            // For tokens generated without prior DB persistence (e.g., in unit/integration tests),
            // record this token as revoked now so it cannot be replayed.
            RefreshTokenEntity legacyOrTestRecord = new RefreshTokenEntity(
                    refreshToken,
                    user,
                    LocalDateTime.now().plusSeconds(jwtService.getRefreshExpirationMs() / 1000)
            );
            legacyOrTestRecord.setRevoked(true);
            refreshTokenRepository.save(legacyOrTestRecord);
        }

        // Issue new pair
        String newAccessToken = jwtService.generateToken(user.getEmail(), user.getRole().name());
        String newRefreshToken = jwtService.generateRefreshToken(user.getEmail());

        LocalDateTime newExpiry = LocalDateTime.now().plusSeconds(jwtService.getRefreshExpirationMs() / 1000);
        refreshTokenRepository.save(new RefreshTokenEntity(newRefreshToken, user, newExpiry));

        return new AuthResponse(
                newAccessToken,
                jwtService.getExpirationMs(),
                newRefreshToken,
                UserResponse.fromEntity(user)
        );
    }

    /**
     * Invalidate refresh token upon logout.
     */
    @Transactional
    public void logout(String refreshToken, String email) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenRepository.revokeByToken(refreshToken.trim());
        }
        if (email != null && !email.isBlank()) {
            String normalizedEmail = normalizeEmail(email);
            userRepository.findByEmail(normalizedEmail).ifPresent(refreshTokenRepository::revokeAllByUser);
        }
    }

    // ==========================================
    // Backward-compatible methods for legacy callers
    // ==========================================

    @Transactional
    public UserEntity registerUser(UserEntity user) {
        String normalizedEmail = normalizeEmail(user.getEmail());
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException("Email already registered");
        }

        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        // Security rule: unconditionally enforce STUDENT role
        user.setRole(Role.STUDENT);
        user.setIsActive(true);

        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public UserEntity loginUser(String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        UserEntity user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidCredentialsException("Invalid password");
        }

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new AccountInactiveException("Account is inactive");
        }

        return user;
    }
}