package com.paranietharan.tripgrid.provider.service;

import com.paranietharan.tripgrid.provider.dto.AddProviderUserRequest;
import com.paranietharan.tripgrid.provider.dto.ProviderUserResponse;
import com.paranietharan.tripgrid.provider.dto.UpdateProviderUserRoleRequest;
import com.paranietharan.tripgrid.provider.entity.ProviderUser;
import com.paranietharan.tripgrid.provider.entity.Role;
import com.paranietharan.tripgrid.provider.exception.ConflictException;
import com.paranietharan.tripgrid.provider.exception.ResourceNotFoundException;
import com.paranietharan.tripgrid.provider.repository.ProviderUserRepository;
import com.paranietharan.tripgrid.provider.security.TenantSecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProviderUserServiceTest {

    @Mock
    private ProviderUserRepository providerUserRepository;

    @Mock
    private TenantSecurityService tenantSecurityService;

    @InjectMocks
    private ProviderUserService providerUserService;

    private UUID providerId;
    private UUID userId;
    private ProviderUser providerUser;

    @BeforeEach
    void setUp() {
        providerId = UUID.randomUUID();
        userId = UUID.randomUUID();
        providerUser = new ProviderUser(UUID.randomUUID(), providerId, userId, Role.PROVIDER_STAFF, Instant.now(), Instant.now());
    }

    @Test
    @DisplayName("Should add user to provider")
    void shouldAddProviderUser() {
        AddProviderUserRequest request = new AddProviderUserRequest(userId, Role.PROVIDER_STAFF);

        when(tenantSecurityService.getRequiredProviderId()).thenReturn(providerId);
        when(providerUserRepository.existsByProviderIdAndUserId(providerId, userId)).thenReturn(false);
        when(providerUserRepository.save(any(ProviderUser.class))).thenReturn(providerUser);

        ProviderUserResponse response = providerUserService.addProviderUser(request);

        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getRole()).isEqualTo(Role.PROVIDER_STAFF);
    }

    @Test
    @DisplayName("Should throw ConflictException if user is already assigned to this provider")
    void shouldThrowConflictWhenUserAlreadyAssigned() {
        AddProviderUserRequest request = new AddProviderUserRequest(userId, Role.PROVIDER_STAFF);

        when(tenantSecurityService.getRequiredProviderId()).thenReturn(providerId);
        when(providerUserRepository.existsByProviderIdAndUserId(providerId, userId)).thenReturn(true);

        assertThatThrownBy(() -> providerUserService.addProviderUser(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already assigned");
    }

    @Test
    @DisplayName("Should update user role")
    void shouldUpdateUserRole() {
        UpdateProviderUserRoleRequest request = new UpdateProviderUserRoleRequest(Role.PROVIDER_ADMIN);

        when(tenantSecurityService.getRequiredProviderId()).thenReturn(providerId);
        when(providerUserRepository.findByProviderIdAndUserId(providerId, userId)).thenReturn(Optional.of(providerUser));
        when(providerUserRepository.save(any(ProviderUser.class))).thenReturn(providerUser);

        ProviderUserResponse response = providerUserService.updateProviderUserRole(userId, request);

        assertThat(response).isNotNull();
        assertThat(providerUser.getRole()).isEqualTo(Role.PROVIDER_ADMIN);
    }

    @Test
    @DisplayName("Should delete provider user")
    void shouldDeleteProviderUser() {
        when(tenantSecurityService.getRequiredProviderId()).thenReturn(providerId);
        when(providerUserRepository.existsByProviderIdAndUserId(providerId, userId)).thenReturn(true);

        providerUserService.deleteProviderUser(userId);

        verify(providerUserRepository).deleteByProviderIdAndUserId(providerId, userId);
    }
}
