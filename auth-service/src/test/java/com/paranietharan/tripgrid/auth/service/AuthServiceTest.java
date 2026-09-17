package com.paranietharan.tripgrid.auth.service;

import com.paranietharan.tripgrid.auth.dto.*;
import com.paranietharan.tripgrid.auth.entity.RefreshToken;
import com.paranietharan.tripgrid.auth.entity.Role;
import com.paranietharan.tripgrid.auth.entity.User;
import com.paranietharan.tripgrid.auth.entity.UserStatus;
import com.paranietharan.tripgrid.auth.exception.AccountNotVerifiedException;
import com.paranietharan.tripgrid.auth.exception.BadRequestException;
import com.paranietharan.tripgrid.auth.exception.ConflictException;
import com.paranietharan.tripgrid.auth.messaging.AuthEventPublisher;
import com.paranietharan.tripgrid.auth.messaging.event.UserRegisteredEvent;
import com.paranietharan.tripgrid.auth.repository.RefreshTokenRepository;
import com.paranietharan.tripgrid.auth.repository.UserRepository;
import com.paranietharan.tripgrid.auth.security.JwtService;
import com.paranietharan.tripgrid.auth.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private VerificationCodeService verificationCodeService;

    @Mock
    private AuthEventPublisher eventPublisher;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                refreshTokenRepository,
                passwordEncoder,
                jwtService,
                verificationCodeService,
                eventPublisher,
                604800000L // 7 days
        );
    }

    @Test
    @DisplayName("Should register new user successfully and publish UserRegisteredEvent")
    void shouldRegisterUserSuccessfully() {
        RegisterRequest request = new RegisterRequest(
                "John",
                "Doe",
                "john@example.com",
                "+94771234567",
                "SecurePassword123!"
        );

        when(userRepository.existsByEmailIgnoreCase("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded_pass");
        when(verificationCodeService.generateAndStoreCode("john@example.com")).thenReturn("123456");

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        UserResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("John", response.getFirstName());
        assertEquals("john@example.com", response.getEmail());
        assertEquals(Role.CUSTOMER, response.getRole());
        assertFalse(response.isEmailVerified());

        verify(eventPublisher).publishUserRegistered(any(UserRegisteredEvent.class));
    }

    @Test
    @DisplayName("Should throw ConflictException when registering with duplicate email")
    void shouldThrowConflictExceptionForDuplicateEmail() {
        RegisterRequest request = new RegisterRequest(
                "John",
                "Doe",
                "john@example.com",
                "+94771234567",
                "SecurePassword123!"
        );

        when(userRepository.existsByEmailIgnoreCase("john@example.com")).thenReturn(true);

        assertThrows(ConflictException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
        verify(eventPublisher, never()).publishUserRegistered(any());
    }

    @Test
    @DisplayName("Should verify email successfully when code is valid")
    void shouldVerifyEmailSuccessfully() {
        VerifyEmailRequest request = new VerifyEmailRequest("john@example.com", "123456");
        User user = new User(UUID.randomUUID(), "John", "Doe", "john@example.com", "+94771234567", "pass", Role.CUSTOMER, UserStatus.ACTIVE, false, null, Instant.now(), Instant.now());

        when(userRepository.findByEmailIgnoreCase("john@example.com")).thenReturn(Optional.of(user));
        when(verificationCodeService.verifyCode("john@example.com", "123456")).thenReturn(true);

        authService.verifyEmail(request);

        assertTrue(user.isEmailVerified());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Should throw BadRequestException when verifying with invalid code")
    void shouldThrowWhenVerifyingWithInvalidCode() {
        VerifyEmailRequest request = new VerifyEmailRequest("john@example.com", "000000");
        User user = new User(UUID.randomUUID(), "John", "Doe", "john@example.com", "+94771234567", "pass", Role.CUSTOMER, UserStatus.ACTIVE, false, null, Instant.now(), Instant.now());

        when(userRepository.findByEmailIgnoreCase("john@example.com")).thenReturn(Optional.of(user));
        when(verificationCodeService.verifyCode("john@example.com", "000000")).thenReturn(false);

        assertThrows(BadRequestException.class, () -> authService.verifyEmail(request));
        assertFalse(user.isEmailVerified());
    }

    @Test
    @DisplayName("Should login successfully and return access and refresh tokens")
    void shouldLoginSuccessfully() {
        LoginRequest request = new LoginRequest("john@example.com", "SecurePassword123!");
        User user = new User(UUID.randomUUID(), "John", "Doe", "john@example.com", "+94771234567", "encoded_pass", Role.CUSTOMER, UserStatus.ACTIVE, true, null, Instant.now(), Instant.now());

        when(userRepository.findByEmailIgnoreCase("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("SecurePassword123!", "encoded_pass")).thenReturn(true);
        when(jwtService.generateAccessToken(any(UserPrincipal.class))).thenReturn("access_token_jwt");
        when(jwtService.getAccessTokenExpirationMs()).thenReturn(900000L);

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("access_token_jwt", response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertEquals("john@example.com", response.getUser().getEmail());

        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Should block login if email is not verified")
    void shouldRejectLoginForUnverifiedEmail() {
        LoginRequest request = new LoginRequest("john@example.com", "SecurePassword123!");
        User user = new User(UUID.randomUUID(), "John", "Doe", "john@example.com", "+94771234567", "encoded_pass", Role.CUSTOMER, UserStatus.ACTIVE, false, null, Instant.now(), Instant.now());

        when(userRepository.findByEmailIgnoreCase("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("SecurePassword123!", "encoded_pass")).thenReturn(true);

        assertThrows(AccountNotVerifiedException.class, () -> authService.login(request));
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should block login if password is incorrect")
    void shouldRejectLoginForWrongPassword() {
        LoginRequest request = new LoginRequest("john@example.com", "WrongPassword!");
        User user = new User(UUID.randomUUID(), "John", "Doe", "john@example.com", "+94771234567", "encoded_pass", Role.CUSTOMER, UserStatus.ACTIVE, true, null, Instant.now(), Instant.now());

        when(userRepository.findByEmailIgnoreCase("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword!", "encoded_pass")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Should refresh tokens and rotate refresh token")
    void shouldRefreshTokens() throws Exception {
        String rawToken = "raw_refresh_token_string";
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String tokenHash = HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));

        User user = new User(UUID.randomUUID(), "John", "Doe", "john@example.com", "+94771234567", "pass", Role.CUSTOMER, UserStatus.ACTIVE, true, null, Instant.now(), Instant.now());
        RefreshToken oldToken = new RefreshToken(UUID.randomUUID(), user, tokenHash, Instant.now().plusSeconds(3600), false, Instant.now());

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(oldToken));
        when(jwtService.generateAccessToken(any(UserPrincipal.class))).thenReturn("new_access_jwt");
        when(jwtService.getAccessTokenExpirationMs()).thenReturn(900000L);

        RefreshTokenRequest request = new RefreshTokenRequest(rawToken);
        AuthResponse response = authService.refreshToken(request);

        assertNotNull(response);
        assertEquals("new_access_jwt", response.getAccessToken());
        assertNotEquals(rawToken, response.getRefreshToken());
        assertTrue(oldToken.isRevoked());

        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Should revoke refresh token on logout")
    void shouldRevokeRefreshTokenOnLogout() throws Exception {
        String rawToken = "raw_refresh_token_to_revoke";
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String tokenHash = HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));

        User user = new User(UUID.randomUUID(), "John", "Doe", "john@example.com", "+94771234567", "pass", Role.CUSTOMER, UserStatus.ACTIVE, true, null, Instant.now(), Instant.now());
        RefreshToken storedToken = new RefreshToken(UUID.randomUUID(), user, tokenHash, Instant.now().plusSeconds(3600), false, Instant.now());

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(storedToken));

        authService.logout(new LogoutRequest(rawToken));

        assertTrue(storedToken.isRevoked());
        verify(refreshTokenRepository).save(storedToken);
    }
}
