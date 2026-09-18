package com.paranietharan.tripgrid.provider.security;

import com.paranietharan.tripgrid.provider.entity.Provider;
import com.paranietharan.tripgrid.provider.entity.ProviderStatus;
import com.paranietharan.tripgrid.provider.entity.ProviderUser;
import com.paranietharan.tripgrid.provider.entity.Role;
import com.paranietharan.tripgrid.provider.exception.ForbiddenException;
import com.paranietharan.tripgrid.provider.repository.ProviderRepository;
import com.paranietharan.tripgrid.provider.repository.ProviderUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantSecurityServiceTest {

    @Mock
    private ProviderUserRepository providerUserRepository;

    @Mock
    private ProviderRepository providerRepository;

    @InjectMocks
    private TenantSecurityService tenantSecurityService;

    private UUID userId;
    private UUID providerId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        providerId = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should resolve providerId for PROVIDER_ADMIN via provider_users mapping")
    void shouldResolveProviderIdForProviderAdmin() {
        UserPrincipal principal = new UserPrincipal(userId, "provider@tripgrid.com", Role.PROVIDER_ADMIN, null);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        ProviderUser providerUser = new ProviderUser(UUID.randomUUID(), providerId, userId, Role.PROVIDER_ADMIN, Instant.now(), Instant.now());
        when(providerUserRepository.findByUserId(userId)).thenReturn(List.of(providerUser));

        UUID resolved = tenantSecurityService.getRequiredProviderId();
        assertThat(resolved).isEqualTo(providerId);
    }

    @Test
    @DisplayName("Should resolve providerId by email match when not yet in provider_users")
    void shouldResolveProviderIdByEmailMatch() {
        UserPrincipal principal = new UserPrincipal(userId, "provider@tripgrid.com", Role.PROVIDER_ADMIN, null);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        Provider provider = new Provider(providerId, "Express Lines", "provider@tripgrid.com", "+94771234567", "Colombo", ProviderStatus.ACTIVE, Instant.now(), Instant.now());
        when(providerUserRepository.findByUserId(userId)).thenReturn(Collections.emptyList());
        when(providerRepository.findByEmailIgnoreCase("provider@tripgrid.com")).thenReturn(Optional.of(provider));
        when(providerUserRepository.existsByProviderIdAndUserId(providerId, userId)).thenReturn(false);

        UUID resolved = tenantSecurityService.getRequiredProviderId();
        assertThat(resolved).isEqualTo(providerId);
    }

    @Test
    @DisplayName("Should permit SUPER_ADMIN access across any provider")
    void shouldPermitSuperAdminAccess() {
        UserPrincipal principal = new UserPrincipal(userId, "admin@tripgrid.com", Role.SUPER_ADMIN, null);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        // SUPER_ADMIN verifyProviderAccess should not throw exception
        tenantSecurityService.verifyProviderAccess(providerId);
    }

    @Test
    @DisplayName("Should reject PROVIDER_ADMIN accessing another provider's resources")
    void shouldRejectCrossTenantAccessForProviderAdmin() {
        UserPrincipal principal = new UserPrincipal(userId, "provider@tripgrid.com", Role.PROVIDER_ADMIN, null);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        ProviderUser providerUser = new ProviderUser(UUID.randomUUID(), providerId, userId, Role.PROVIDER_ADMIN, Instant.now(), Instant.now());
        when(providerUserRepository.findByUserId(userId)).thenReturn(List.of(providerUser));

        UUID anotherProviderId = UUID.randomUUID();
        assertThatThrownBy(() -> tenantSecurityService.verifyProviderAccess(anotherProviderId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("another provider tenant");
    }
}
