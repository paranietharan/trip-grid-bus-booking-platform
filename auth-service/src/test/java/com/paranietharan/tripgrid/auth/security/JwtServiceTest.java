package com.paranietharan.tripgrid.auth.security;

import com.paranietharan.tripgrid.auth.entity.Role;
import com.paranietharan.tripgrid.auth.entity.User;
import com.paranietharan.tripgrid.auth.entity.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private static final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long EXPIRATION_MS = 60000; // 1 minute
    private static final String ISSUER = "tripgrid-test";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, EXPIRATION_MS, ISSUER);
    }

    @Test
    @DisplayName("Should generate valid JWT token with claims and extract them correctly")
    void shouldGenerateAndValidateToken() {
        UUID userId = UUID.randomUUID();
        User user = new User(
                userId,
                "John",
                "Doe",
                "john.doe@example.com",
                "+94771234567",
                "hashedpassword",
                Role.CUSTOMER,
                UserStatus.ACTIVE,
                true,
                null,
                Instant.now(),
                Instant.now()
        );
        UserPrincipal principal = UserPrincipal.create(user);

        String token = jwtService.generateAccessToken(principal);

        assertNotNull(token);
        assertTrue(jwtService.validateToken(token));
        assertEquals(userId, jwtService.extractUserId(token));
        assertEquals("john.doe@example.com", jwtService.extractEmail(token));
        assertEquals(Role.CUSTOMER.name(), jwtService.extractRole(token));
        assertNull(jwtService.extractTenantId(token));
    }

    @Test
    @DisplayName("Should include tenant_id in JWT claims for PROVIDER_ADMIN")
    void shouldIncludeTenantIdForProviderAdmin() {
        UUID userId = UUID.randomUUID();
        User user = new User(
                userId,
                "Provider",
                "Admin",
                "admin@provider.com",
                "+94771234568",
                "hashedpassword",
                Role.PROVIDER_ADMIN,
                UserStatus.ACTIVE,
                true,
                "tenant-123",
                Instant.now(),
                Instant.now()
        );
        UserPrincipal principal = UserPrincipal.create(user);

        String token = jwtService.generateAccessToken(principal);

        assertNotNull(token);
        assertTrue(jwtService.validateToken(token));
        assertEquals(Role.PROVIDER_ADMIN.name(), jwtService.extractRole(token));
        assertEquals("tenant-123", jwtService.extractTenantId(token));
    }

    @Test
    @DisplayName("Should return false for expired JWT token")
    void shouldReturnFalseForExpiredToken() {
        JwtService shortLivedJwtService = new JwtService(SECRET, -1000, ISSUER); // expired 1 sec ago
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "Jane", "Doe", "jane@example.com", "+94771234569", "pass", Role.CUSTOMER, UserStatus.ACTIVE, true, null, Instant.now(), Instant.now());
        UserPrincipal principal = UserPrincipal.create(user);

        String expiredToken = shortLivedJwtService.generateAccessToken(principal);

        assertFalse(jwtService.validateToken(expiredToken));
    }

    @Test
    @DisplayName("Should return false for malformed JWT token")
    void shouldReturnFalseForMalformedToken() {
        assertFalse(jwtService.validateToken("invalid.malformed.token"));
    }
}
