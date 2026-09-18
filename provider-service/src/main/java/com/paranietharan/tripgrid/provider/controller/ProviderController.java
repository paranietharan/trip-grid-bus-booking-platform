package com.paranietharan.tripgrid.provider.controller;

import com.paranietharan.tripgrid.provider.dto.*;
import com.paranietharan.tripgrid.provider.service.ProviderService;
import com.paranietharan.tripgrid.provider.service.ProviderUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/providers")
public class ProviderController {

    private final ProviderService providerService;
    private final ProviderUserService providerUserService;

    public ProviderController(
            ProviderService providerService,
            ProviderUserService providerUserService) {
        this.providerService = providerService;
        this.providerUserService = providerUserService;
    }

    /**
     * Self-service registration for providers.
     * Registers a new provider company and immediately associates the authenticated user as PROVIDER_ADMIN.
     */
    @PostMapping("/register")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProviderResponse> registerProvider(@Valid @RequestBody CreateProviderRequest request) {
        ProviderResponse response = providerService.registerProvider(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ProviderResponse> createProvider(@Valid @RequestBody CreateProviderRequest request) {
        ProviderResponse response = providerService.createProvider(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<ProviderResponse>> getAllProviders() {
        return ResponseEntity.ok(providerService.getAllProviders());
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('PROVIDER_ADMIN', 'PROVIDER_STAFF')")
    public ResponseEntity<ProviderResponse> getCurrentProvider() {
        return ResponseEntity.ok(providerService.getCurrentProvider());
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('PROVIDER_ADMIN')")
    public ResponseEntity<ProviderResponse> updateCurrentProvider(@Valid @RequestBody UpdateProviderRequest request) {
        return ResponseEntity.ok(providerService.updateCurrentProvider(request));
    }

    @GetMapping("/{providerId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PROVIDER_ADMIN', 'PROVIDER_STAFF')")
    public ResponseEntity<ProviderResponse> getProviderById(@PathVariable UUID providerId) {
        return ResponseEntity.ok(providerService.getProviderById(providerId));
    }

    @PutMapping("/{providerId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PROVIDER_ADMIN')")
    public ResponseEntity<ProviderResponse> updateProvider(
            @PathVariable UUID providerId,
            @Valid @RequestBody UpdateProviderRequest request) {
        return ResponseEntity.ok(providerService.updateProvider(providerId, request));
    }

    @PatchMapping("/{providerId}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ProviderResponse> updateProviderStatus(
            @PathVariable UUID providerId,
            @Valid @RequestBody UpdateProviderStatusRequest request) {
        return ResponseEntity.ok(providerService.updateProviderStatus(providerId, request));
    }

    /**
     * SUPER_ADMIN assigning any user directly to a specific provider.
     */
    @PostMapping("/{providerId}/users")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ProviderUserResponse> assignUserToProvider(
            @PathVariable UUID providerId,
            @Valid @RequestBody AddProviderUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(providerUserService.addUserToProvider(providerId, request));
    }
}
