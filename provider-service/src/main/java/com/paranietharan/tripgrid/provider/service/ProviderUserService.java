package com.paranietharan.tripgrid.provider.service;

import com.paranietharan.tripgrid.provider.dto.AddProviderUserRequest;
import com.paranietharan.tripgrid.provider.dto.ProviderUserResponse;
import com.paranietharan.tripgrid.provider.dto.UpdateProviderUserRoleRequest;
import com.paranietharan.tripgrid.provider.entity.ProviderUser;
import com.paranietharan.tripgrid.provider.entity.Role;
import com.paranietharan.tripgrid.provider.exception.BadRequestException;
import com.paranietharan.tripgrid.provider.exception.ConflictException;
import com.paranietharan.tripgrid.provider.exception.ResourceNotFoundException;
import com.paranietharan.tripgrid.provider.repository.ProviderRepository;
import com.paranietharan.tripgrid.provider.repository.ProviderUserRepository;
import com.paranietharan.tripgrid.provider.security.TenantSecurityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ProviderUserService {

    private static final Logger log = LoggerFactory.getLogger(ProviderUserService.class);

    private final ProviderUserRepository providerUserRepository;
    private final ProviderRepository providerRepository;
    private final TenantSecurityService tenantSecurityService;

    public ProviderUserService(
            ProviderUserRepository providerUserRepository,
            ProviderRepository providerRepository,
            TenantSecurityService tenantSecurityService) {
        this.providerUserRepository = providerUserRepository;
        this.providerRepository = providerRepository;
        this.tenantSecurityService = tenantSecurityService;
    }

    @Transactional
    public ProviderUserResponse addProviderUser(AddProviderUserRequest request) {
        UUID providerId = tenantSecurityService.getRequiredProviderId();
        return assignUserToProviderInternal(providerId, request);
    }

    @Transactional
    public ProviderUserResponse addUserToProvider(UUID providerId, AddProviderUserRequest request) {
        if (!providerRepository.existsById(providerId)) {
            throw new ResourceNotFoundException("Provider not found with id: " + providerId);
        }
        return assignUserToProviderInternal(providerId, request);
    }

    private ProviderUserResponse assignUserToProviderInternal(UUID providerId, AddProviderUserRequest request) {
        if (request.getRole() != Role.PROVIDER_ADMIN && request.getRole() != Role.PROVIDER_STAFF) {
            throw new BadRequestException("Role must be either PROVIDER_ADMIN or PROVIDER_STAFF");
        }

        if (providerUserRepository.existsByProviderIdAndUserId(providerId, request.getUserId())) {
            throw new ConflictException("User " + request.getUserId() + " is already assigned to this provider");
        }

        ProviderUser providerUser = new ProviderUser();
        providerUser.setProviderId(providerId);
        providerUser.setUserId(request.getUserId());
        providerUser.setRole(request.getRole());

        ProviderUser saved = providerUserRepository.save(providerUser);
        log.info("Assigned user: {} to provider: {} with role: {}", saved.getUserId(), providerId, saved.getRole());
        return ProviderUserResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<ProviderUserResponse> getCurrentProviderUsers() {
        UUID providerId = tenantSecurityService.getRequiredProviderId();
        return providerUserRepository.findByProviderId(providerId)
                .stream()
                .map(ProviderUserResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProviderUserResponse getProviderUser(UUID userId) {
        UUID providerId = tenantSecurityService.getRequiredProviderId();
        ProviderUser providerUser = providerUserRepository.findByProviderIdAndUserId(providerId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not associated with this provider: " + userId));

        return ProviderUserResponse.fromEntity(providerUser);
    }

    @Transactional
    public ProviderUserResponse updateProviderUserRole(UUID userId, UpdateProviderUserRoleRequest request) {
        UUID providerId = tenantSecurityService.getRequiredProviderId();

        if (request.getRole() != Role.PROVIDER_ADMIN && request.getRole() != Role.PROVIDER_STAFF) {
            throw new BadRequestException("Role must be either PROVIDER_ADMIN or PROVIDER_STAFF");
        }

        ProviderUser providerUser = providerUserRepository.findByProviderIdAndUserId(providerId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not associated with this provider: " + userId));

        providerUser.setRole(request.getRole());
        providerUser.setUpdatedAt(Instant.now());

        ProviderUser updated = providerUserRepository.save(providerUser);
        log.info("Updated role for user: {} in provider: {} to {}", userId, providerId, request.getRole());
        return ProviderUserResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteProviderUser(UUID userId) {
        UUID providerId = tenantSecurityService.getRequiredProviderId();
        if (!providerUserRepository.existsByProviderIdAndUserId(providerId, userId)) {
            throw new ResourceNotFoundException("User not associated with this provider: " + userId);
        }

        providerUserRepository.deleteByProviderIdAndUserId(providerId, userId);
        log.info("Removed user: {} from provider: {}", userId, providerId);
    }
}
