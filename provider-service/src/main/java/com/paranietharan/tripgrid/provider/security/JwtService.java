package com.paranietharan.tripgrid.provider.security;

import com.paranietharan.tripgrid.provider.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey signingKey;
    private final String issuer;

    public JwtService(
            @Value("${tripgrid.jwt.secret}") String secret,
            @Value("${tripgrid.jwt.issuer:tripgrid-auth-service}") String issuer) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .requireIssuer(issuer)
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT expired: {}", e.getMessage());
        } catch (MalformedJwtException | UnsupportedJwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
        } catch (SecurityException e) {
            log.warn("JWT signature verification failed: {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("JWT claim validation failed: {}", e.getMessage());
        }
        return false;
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .requireIssuer(issuer)
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UserPrincipal parseUserPrincipal(String token) {
        Claims claims = extractAllClaims(token);
        String sub = claims.getSubject();
        UUID userId = UUID.fromString(sub);
        String email = claims.get("email", String.class);
        String roleStr = claims.get("role", String.class);
        String tenantId = claims.get("tenant_id", String.class);

        Role role = Role.CUSTOMER;
        if (roleStr != null) {
            try {
                role = Role.valueOf(roleStr);
            } catch (IllegalArgumentException ignored) {
            }
        }

        return new UserPrincipal(userId, email, role, tenantId);
    }
}
