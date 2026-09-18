package com.paranietharan.tripgrid.provider.service;

import com.paranietharan.tripgrid.provider.dto.*;
import com.paranietharan.tripgrid.provider.entity.Provider;
import com.paranietharan.tripgrid.provider.entity.ProviderStatus;
import com.paranietharan.tripgrid.provider.entity.ProviderUser;
import com.paranietharan.tripgrid.provider.entity.Role;
import com.paranietharan.tripgrid.provider.exception.ConflictException;
import com.paranietharan.tripgrid.provider.exception.ResourceNotFoundException;
import com.paranietharan.tripgrid.provider.repository.ProviderRepository;
import com.paranietharan.tripgrid.provider.repository.ProviderUserRepository;
import com.paranietharan.tripgrid.provider.security.TenantSecurityService;
import com.paranietharan.tripgrid.provider.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ProviderService {

    private static final Logger log = LoggerFactory.getLogger(ProviderService.class);

    private final ProviderRepository providerRepository;
    private final ProviderUserRepository providerUserRepository;
    private final TenantSecurityService tenantSecurityService;

    public ProviderService(
            ProviderRepository providerRepository,
            ProviderUserRepository providerUserRepository,
            TenantSecurityService tenantSecurityService) {
        this.providerRepository = providerRepository;
        this.providerUserRepository = providerUserRepository;
        this.tenantSecurityService = tenantSecurityService;
    }

    @Transactional
    public ProviderResponse createProvider(CreateProviderRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (providerRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Provider with email " + email + " already exists");
        }

        Provider provider = new Provider();
        provider.setName(request.getName().trim());
        provider.setEmail(email);
        provider.setPhoneNumber(request.getPhoneNumber().trim());
        provider.setAddress(request.getAddress() != null ? request.getAddress().trim() : null);
        provider.setStatus(ProviderStatus.ACTIVE);

        Provider saved = providerRepository.save(provider);
        log.info("Created new bus provider with id: {}, name: {}", saved.getId(), saved.getName());
        return ProviderResponse.fromEntity(saved);
    }

    @Transactional
    public ProviderResponse registerProvider(CreateProviderRequest request) {
        UserPrincipal principal = tenantSecurityService.getAuthenticatedPrincipal();

        String email = request.getEmail().trim().toLowerCase();
        if (providerRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Provider with email " + email + " already exists");
        }

        Provider provider = new Provider();
        provider.setName(request.getName().trim());
        provider.setEmail(email);
        provider.setPhoneNumber(request.getPhoneNumber().trim());
        provider.setAddress(request.getAddress() != null ? request.getAddress().trim() : null);
        provider.setStatus(ProviderStatus.ACTIVE);

        Provider saved = providerRepository.save(provider);

        // Link authenticated user to newly registered provider as PROVIDER_ADMIN
        ProviderUser providerUser = new ProviderUser();
        providerUser.setProviderId(saved.getId());
        providerUser.setUserId(principal.getUserId());
        providerUser.setRole(Role.PROVIDER_ADMIN);
        providerUser.setCreatedAt(Instant.now());
        providerUserRepository.save(providerUser);

        log.info("Registered provider company: {} ({}) and linked user: {}", saved.getName(), saved.getId(), principal.getUserId());
        return ProviderResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<ProviderResponse> getAllProviders() {
        return providerRepository.findAll()
                .stream()
                .map(ProviderResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProviderResponse getProviderById(UUID providerId) {
        tenantSecurityService.verifyProviderAccess(providerId);

        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found with id: " + providerId));

        return ProviderResponse.fromEntity(provider);
    }

    @Transactional(readOnly = true)
    public ProviderResponse getCurrentProvider() {
        UUID providerId = tenantSecurityService.getRequiredProviderId();
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found with id: " + providerId));

        return ProviderResponse.fromEntity(provider);
    }

    @Transactional
    public ProviderResponse updateProvider(UUID providerId, UpdateProviderRequest request) {
        tenantSecurityService.verifyProviderAccess(providerId);

        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found with id: " + providerId));

        provider.setName(request.getName().trim());
        provider.setPhoneNumber(request.getPhoneNumber().trim());
        provider.setAddress(request.getAddress() != null ? request.getAddress().trim() : null);
        provider.setUpdatedAt(Instant.now());

        Provider updated = providerRepository.save(provider);
        log.info("Updated provider id: {}", updated.getId());
        return ProviderResponse.fromEntity(updated);
    }

    @Transactional
    public ProviderResponse updateCurrentProvider(UpdateProviderRequest request) {
        UUID providerId = tenantSecurityService.getRequiredProviderId();
        return updateProvider(providerId, request);
    }

    @Transactional
    public ProviderResponse updateProviderStatus(UUID providerId, UpdateProviderStatusRequest request) {
        // Only SUPER_ADMIN allowed
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found with id: " + providerId));

        provider.setStatus(request.getStatus());
        provider.setUpdatedAt(Instant.now());

        Provider updated = providerRepository.save(provider);
        log.info("Updated provider status id: {} to {}", updated.getId(), updated.getStatus());
        return ProviderResponse.fromEntity(updated);
    }
}
