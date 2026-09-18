package com.paranietharan.tripgrid.provider.service;

import com.paranietharan.tripgrid.provider.dto.CreateProviderRequest;
import com.paranietharan.tripgrid.provider.dto.ProviderResponse;
import com.paranietharan.tripgrid.provider.dto.UpdateProviderRequest;
import com.paranietharan.tripgrid.provider.dto.UpdateProviderStatusRequest;
import com.paranietharan.tripgrid.provider.entity.Provider;
import com.paranietharan.tripgrid.provider.entity.ProviderStatus;
import com.paranietharan.tripgrid.provider.entity.Role;
import com.paranietharan.tripgrid.provider.exception.ConflictException;
import com.paranietharan.tripgrid.provider.exception.ForbiddenException;
import com.paranietharan.tripgrid.provider.repository.ProviderRepository;
import com.paranietharan.tripgrid.provider.repository.ProviderUserRepository;
import com.paranietharan.tripgrid.provider.security.TenantSecurityService;
import com.paranietharan.tripgrid.provider.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProviderServiceTest {

    @Mock
    private ProviderRepository providerRepository;

    @Mock
    private ProviderUserRepository providerUserRepository;

    @Mock
    private TenantSecurityService tenantSecurityService;

    @InjectMocks
    private ProviderService providerService;

    private Provider sampleProvider;
    private UUID providerId;

    @BeforeEach
    void setUp() {
        providerId = UUID.randomUUID();
        sampleProvider = new Provider(
                providerId,
                "Express Lines",
                "contact@expresslines.com",
                "+94771234567",
                "Colombo",
                ProviderStatus.ACTIVE,
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    @DisplayName("Should successfully create a new provider")
    void shouldCreateProvider() {
        CreateProviderRequest request = new CreateProviderRequest(
                "Express Lines",
                "contact@expresslines.com",
                "+94771234567",
                "Colombo"
        );

        when(providerRepository.existsByEmailIgnoreCase(any())).thenReturn(false);
        when(providerRepository.save(any(Provider.class))).thenReturn(sampleProvider);

        ProviderResponse response = providerService.createProvider(request);

        assertThat(response.getId()).isEqualTo(providerId);
        assertThat(response.getName()).isEqualTo("Express Lines");
        assertThat(response.getStatus()).isEqualTo(ProviderStatus.ACTIVE);
        verify(providerRepository, times(1)).save(any(Provider.class));
    }

    @Test
    @DisplayName("Should register new provider and auto-link user as PROVIDER_ADMIN")
    void shouldRegisterProviderAndAutoLink() {
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(userId, "admin@expresslines.com", Role.PROVIDER_ADMIN, null);
        when(tenantSecurityService.getAuthenticatedPrincipal()).thenReturn(principal);
        when(providerRepository.existsByEmailIgnoreCase(any())).thenReturn(false);
        when(providerRepository.save(any(Provider.class))).thenReturn(sampleProvider);

        CreateProviderRequest request = new CreateProviderRequest(
                "Express Lines",
                "contact@expresslines.com",
                "+94771234567",
                "Colombo"
        );

        ProviderResponse response = providerService.registerProvider(request);
        assertThat(response.getId()).isEqualTo(providerId);
        verify(providerUserRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Should throw ConflictException when creating provider with duplicate email")
    void shouldThrowConflictWhenEmailExists() {
        CreateProviderRequest request = new CreateProviderRequest(
                "Express Lines",
                "contact@expresslines.com",
                "+94771234567",
                "Colombo"
        );

        when(providerRepository.existsByEmailIgnoreCase("contact@expresslines.com")).thenReturn(true);

        assertThatThrownBy(() -> providerService.createProvider(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Should get provider by ID when tenant access is verified")
    void shouldGetProviderById() {
        when(providerRepository.findById(providerId)).thenReturn(Optional.of(sampleProvider));
        doNothing().when(tenantSecurityService).verifyProviderAccess(providerId);

        ProviderResponse response = providerService.getProviderById(providerId);
        assertThat(response.getId()).isEqualTo(providerId);
        verify(tenantSecurityService).verifyProviderAccess(providerId);
    }

    @Test
    @DisplayName("Should throw ForbiddenException when accessing another provider's details")
    void shouldThrowForbiddenWhenCrossTenantAccess() {
        doThrow(new ForbiddenException("Access denied")).when(tenantSecurityService).verifyProviderAccess(providerId);

        assertThatThrownBy(() -> providerService.getProviderById(providerId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Should update provider status by SUPER_ADMIN")
    void shouldUpdateProviderStatus() {
        when(providerRepository.findById(providerId)).thenReturn(Optional.of(sampleProvider));
        when(providerRepository.save(any(Provider.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProviderStatusRequest request = new UpdateProviderStatusRequest(ProviderStatus.SUSPENDED);
        ProviderResponse response = providerService.updateProviderStatus(providerId, request);

        assertThat(response.getStatus()).isEqualTo(ProviderStatus.SUSPENDED);
        verify(providerRepository).save(any(Provider.class));
    }
}
