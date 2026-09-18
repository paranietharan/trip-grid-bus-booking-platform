package com.paranietharan.tripgrid.provider.security;

import com.paranietharan.tripgrid.provider.entity.Role;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private JwtService jwtService;
    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, "tripgrid-auth-service");
        this.secretKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Should validate correct JWT token and parse principal details")
    void shouldValidateAndParseToken() {
        UUID userId = UUID.randomUUID();
        String token = Jwts.builder()
                .subject(userId.toString())
                .claim("email", "provider@tripgrid.com")
                .claim("role", "PROVIDER_ADMIN")
                .claim("tenant_id", "tenant-123")
                .issuer("tripgrid-auth-service")
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .signWith(secretKey)
                .compact();

        assertThat(jwtService.validateToken(token)).isTrue();

        UserPrincipal principal = jwtService.parseUserPrincipal(token);
        assertThat(principal.getUserId()).isEqualTo(userId);
        assertThat(principal.getEmail()).isEqualTo("provider@tripgrid.com");
        assertThat(principal.getRole()).isEqualTo(Role.PROVIDER_ADMIN);
        assertThat(principal.getTenantId()).isEqualTo("tenant-123");
        assertThat(principal.isProviderAdmin()).isTrue();
        assertThat(principal.isSuperAdmin()).isFalse();
    }

    @Test
    @DisplayName("Should return false for expired token")
    void shouldRejectExpiredToken() {
        UUID userId = UUID.randomUUID();
        String expiredToken = Jwts.builder()
                .subject(userId.toString())
                .claim("email", "user@tripgrid.com")
                .claim("role", "CUSTOMER")
                .issuer("tripgrid-auth-service")
                .issuedAt(Date.from(Instant.now().minus(2, ChronoUnit.HOURS)))
                .expiration(Date.from(Instant.now().minus(1, ChronoUnit.HOURS)))
                .signWith(secretKey)
                .compact();

        assertThat(jwtService.validateToken(expiredToken)).isFalse();
    }

    @Test
    @DisplayName("Should return false for invalid signature or malformed token")
    void shouldRejectMalformedToken() {
        assertThat(jwtService.validateToken("invalid.malformed.token")).isFalse();
    }
}
