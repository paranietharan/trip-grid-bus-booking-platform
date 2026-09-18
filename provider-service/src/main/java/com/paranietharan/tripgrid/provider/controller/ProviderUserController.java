package com.paranietharan.tripgrid.provider.controller;

import com.paranietharan.tripgrid.provider.dto.AddProviderUserRequest;
import com.paranietharan.tripgrid.provider.dto.ProviderUserResponse;
import com.paranietharan.tripgrid.provider.dto.UpdateProviderUserRoleRequest;
import com.paranietharan.tripgrid.provider.service.ProviderUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/providers/me/users")
@PreAuthorize("hasRole('PROVIDER_ADMIN')")
public class ProviderUserController {

    private final ProviderUserService providerUserService;

    public ProviderUserController(ProviderUserService providerUserService) {
        this.providerUserService = providerUserService;
    }

    @PostMapping
    public ResponseEntity<ProviderUserResponse> addProviderUser(@Valid @RequestBody AddProviderUserRequest request) {
        ProviderUserResponse response = providerUserService.addProviderUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ProviderUserResponse>> getCurrentProviderUsers() {
        return ResponseEntity.ok(providerUserService.getCurrentProviderUsers());
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ProviderUserResponse> getProviderUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(providerUserService.getProviderUser(userId));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<ProviderUserResponse> updateProviderUserRole(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateProviderUserRoleRequest request) {
        return ResponseEntity.ok(providerUserService.updateProviderUserRole(userId, request));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteProviderUser(@PathVariable UUID userId) {
        providerUserService.deleteProviderUser(userId);
        return ResponseEntity.noContent().build();
    }
}
