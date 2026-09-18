package com.paranietharan.tripgrid.auth.service;

import com.paranietharan.tripgrid.auth.dto.*;
import com.paranietharan.tripgrid.auth.entity.RefreshToken;
import com.paranietharan.tripgrid.auth.entity.Role;
import com.paranietharan.tripgrid.auth.entity.User;
import com.paranietharan.tripgrid.auth.entity.UserStatus;
import com.paranietharan.tripgrid.auth.exception.AccountNotVerifiedException;
import com.paranietharan.tripgrid.auth.exception.BadRequestException;
import com.paranietharan.tripgrid.auth.exception.ConflictException;
import com.paranietharan.tripgrid.auth.exception.InvalidTokenException;
import com.paranietharan.tripgrid.auth.exception.ResourceNotFoundException;
import com.paranietharan.tripgrid.auth.messaging.AuthEventPublisher;
import com.paranietharan.tripgrid.auth.messaging.event.EmailVerificationRequestedEvent;
import com.paranietharan.tripgrid.auth.messaging.event.UserRegisteredEvent;
import com.paranietharan.tripgrid.auth.repository.RefreshTokenRepository;
import com.paranietharan.tripgrid.auth.repository.UserRepository;
import com.paranietharan.tripgrid.auth.security.JwtService;
import com.paranietharan.tripgrid.auth.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final VerificationCodeService verificationCodeService;
    private final AuthEventPublisher eventPublisher;
    private final long refreshTokenExpirationMs;
    private final SecureRandom secureRandom;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            VerificationCodeService verificationCodeService,
            AuthEventPublisher eventPublisher,
            @Value("${tripgrid.jwt.refresh-token-expiration-ms:604800000}") long refreshTokenExpirationMs) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.verificationCodeService = verificationCodeService;
        this.eventPublisher = eventPublisher;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
        this.secureRandom = new SecureRandom();
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with this email already exists");
        }

        // Public registration always forces CUSTOMER role to prevent privilege escalation.
        // Elevated roles (SUPER_ADMIN, PROVIDER_ADMIN, PROVIDER_STAFF) must be provisioned via admin flows.
        User user = new User();
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setEmail(email);
        user.setPhoneNumber(request.getPhoneNumber().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.CUSTOMER);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(false);
        user.setTenantId(null);

        User savedUser = userRepository.save(user);
        log.info("User registered with id: {}, role: {}", savedUser.getId(), savedUser.getRole());

        // Generate verification code and store securely in Redis with TTL
        String verificationCode = verificationCodeService.generateAndStoreCode(email);

        // Publish event to RabbitMQ for asynchronous processing by Email Service
        eventPublisher.publishUserRegistered(new UserRegisteredEvent(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getFirstName(),
                verificationCode
        ));

        return UserResponse.fromEntity(savedUser);
    }

    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        if (user.isEmailVerified()) {
            throw new BadRequestException("Email is already verified");
        }

        boolean isValid = verificationCodeService.verifyCode(email, request.getCode());
        if (!isValid) {
            throw new BadRequestException("Invalid or expired verification code");
        }

        user.setEmailVerified(true);
        userRepository.save(user);
        log.info("Email verified successfully for user id: {}", user.getId());
    }

    public void resendVerification(ResendVerificationRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        // Rate limiting check
        verificationCodeService.checkAndIncrementRateLimit(email);

        Optional<User> optionalUser = userRepository.findByEmailIgnoreCase(email);
        if (optionalUser.isEmpty()) {
            // Silently return to prevent user enumeration
            log.debug("Resend verification requested for non-existent email: {}", email);
            return;
        }

        User user = optionalUser.get();
        if (user.isEmailVerified()) {
            throw new BadRequestException("Email is already verified");
        }

        String newCode = verificationCodeService.generateAndStoreCode(email);
        eventPublisher.publishVerificationRequested(new EmailVerificationRequestedEvent(
                user.getEmail(),
                user.getFirstName(),
                newCode
        ));
        log.info("Resent verification code event published for user id: {}", user.getId());
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        if (!user.isEmailVerified()) {
            throw new AccountNotVerifiedException("Email is not verified. Please verify your email before logging in.");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new DisabledException("User account is inactive or suspended");
        }

        UserPrincipal principal = UserPrincipal.create(user);
        String accessToken = jwtService.generateAccessToken(principal);

        // Generate and persist refresh token
        String rawRefreshToken = generateSecureToken();
        String tokenHash = hashToken(rawRefreshToken);
        Instant expiresAt = Instant.now().plusMillis(refreshTokenExpirationMs);

        RefreshToken refreshToken = new RefreshToken(
                UUID.randomUUID(),
                user,
                tokenHash,
                expiresAt,
                false,
                Instant.now()
        );
        refreshTokenRepository.save(refreshToken);

        log.info("User logged in successfully: {}", user.getId());
        return new AuthResponse(
                accessToken,
                rawRefreshToken,
                jwtService.getAccessTokenExpirationMs(),
                UserResponse.fromEntity(user)
        );
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String tokenHash = hashToken(request.getRefreshToken().trim());

        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        if (!storedToken.isActive()) {
            throw new InvalidTokenException("Refresh token is expired or revoked");
        }

        User user = storedToken.getUser();
        if (user.getStatus() != UserStatus.ACTIVE || !user.isEmailVerified()) {
            throw new InvalidTokenException("User account is not eligible for token refresh");
        }

        // Revoke the used refresh token (Token Rotation)
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        // Generate new access token & new refresh token
        UserPrincipal principal = UserPrincipal.create(user);
        String newAccessToken = jwtService.generateAccessToken(principal);

        String newRawRefreshToken = generateSecureToken();
        String newTokenHash = hashToken(newRawRefreshToken);
        Instant expiresAt = Instant.now().plusMillis(refreshTokenExpirationMs);

        RefreshToken newRefreshToken = new RefreshToken(
                UUID.randomUUID(),
                user,
                newTokenHash,
                expiresAt,
                false,
                Instant.now()
        );
        refreshTokenRepository.save(newRefreshToken);

        log.info("Token refreshed for user id: {}", user.getId());
        return new AuthResponse(
                newAccessToken,
                newRawRefreshToken,
                jwtService.getAccessTokenExpirationMs(),
                UserResponse.fromEntity(user)
        );
    }

    @Transactional
    public void logout(LogoutRequest request) {
        if (request.getRefreshToken() != null && !request.getRefreshToken().isBlank()) {
            String tokenHash = hashToken(request.getRefreshToken().trim());
            refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
                log.info("Revoked refresh token on logout for user id: {}", token.getUser().getId());
            });
        }
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return UserResponse.fromEntity(user);
    }

    private String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
