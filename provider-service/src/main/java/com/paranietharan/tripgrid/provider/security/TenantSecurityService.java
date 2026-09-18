package com.paranietharan.tripgrid.provider.security;

import com.paranietharan.tripgrid.provider.entity.Provider;
import com.paranietharan.tripgrid.provider.entity.ProviderUser;
import com.paranietharan.tripgrid.provider.entity.Role;
import com.paranietharan.tripgrid.provider.exception.ForbiddenException;
import com.paranietharan.tripgrid.provider.repository.ProviderRepository;
import com.paranietharan.tripgrid.provider.repository.ProviderUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TenantSecurityService {

    private final ProviderUserRepository providerUserRepository;
    private final ProviderRepository providerRepository;

    public TenantSecurityService(
            ProviderUserRepository providerUserRepository,
            ProviderRepository providerRepository) {
        this.providerUserRepository = providerUserRepository;
        this.providerRepository = providerRepository;
    }

    public UserPrincipal getAuthenticatedPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ForbiddenException("Authentication principal is missing or invalid");
        }
        return principal;
    }

    /**
     * Resolves the providerId for the currently authenticated user.
     * If user is SUPER_ADMIN, returns error for /me endpoints (super admin must manage target provider explicitly).
     * If user is PROVIDER_ADMIN or PROVIDER_STAFF, returns the assigned provider's UUID.
     */
    public UUID getRequiredProviderId() {
        UserPrincipal principal = getAuthenticatedPrincipal();

        if (principal.isSuperAdmin()) {
            throw new ForbiddenException("Super admin must specify target provider explicitly");
        }

        return resolveProviderIdForPrincipal(principal);
    }

    /**
     * Resolves providerId for a non-super-admin principal or validates tenant match.
     */
    public UUID resolveProviderIdForPrincipal(UserPrincipal principal) {
        // 1. Check provider_users table first by userId
        List<ProviderUser> providerUsers = providerUserRepository.findByUserId(principal.getUserId());
        if (!providerUsers.isEmpty()) {
            return providerUsers.get(0).getProviderId();
        }

        // 2. Check if user's email matches an existing registered provider (e.g. primary account or seeded user)
        if (principal.getEmail() != null && !principal.getEmail().isBlank()) {
            Optional<Provider> providerByEmail = providerRepository.findByEmailIgnoreCase(principal.getEmail().trim().toLowerCase());
            if (providerByEmail.isPresent()) {
                Provider provider = providerByEmail.get();
                if (!providerUserRepository.existsByProviderIdAndUserId(provider.getId(), principal.getUserId())) {
                    ProviderUser pu = new ProviderUser();
                    pu.setProviderId(provider.getId());
                    pu.setUserId(principal.getUserId());
                    pu.setRole(principal.getRole() != null ? principal.getRole() : Role.PROVIDER_ADMIN);
                    pu.setCreatedAt(Instant.now());
                    try {
                        providerUserRepository.save(pu);
                    } catch (Exception ignored) {
                        // ignore potential race condition
                    }
                }
                return provider.getId();
            }
        }

        // 3. Check if tenantId in JWT is a valid UUID matching a provider
        if (principal.getTenantId() != null && !principal.getTenantId().isBlank()) {
            try {
                UUID tenantUuid = UUID.fromString(principal.getTenantId());
                if (providerRepository.existsById(tenantUuid)) {
                    return tenantUuid;
                }
            } catch (IllegalArgumentException ignored) {
            }
        }

        throw new ForbiddenException("Authenticated user is not linked to any active provider organization");
    }

    /**
     * Verifies that the authenticated user has rights to access/mutate resources of the target providerId.
     */
    public void verifyProviderAccess(UUID targetProviderId) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        if (principal.isSuperAdmin()) {
            return; // SUPER_ADMIN has cross-tenant access
        }

        UUID userProviderId = resolveProviderIdForPrincipal(principal);
        if (!userProviderId.equals(targetProviderId)) {
            throw new ForbiddenException("Access denied: Resource belongs to another provider tenant");
        }
    }
}
