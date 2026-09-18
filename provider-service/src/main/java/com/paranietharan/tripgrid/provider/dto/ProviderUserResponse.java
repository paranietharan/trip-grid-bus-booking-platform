package com.paranietharan.tripgrid.provider.dto;

import com.paranietharan.tripgrid.provider.entity.ProviderUser;
import com.paranietharan.tripgrid.provider.entity.Role;

import java.time.Instant;
import java.util.UUID;

public class ProviderUserResponse {

    private UUID id;
    private UUID providerId;
    private UUID userId;
    private Role role;
    private Instant createdAt;
    private Instant updatedAt;

    public ProviderUserResponse() {
    }

    public ProviderUserResponse(UUID id, UUID providerId, UUID userId, Role role, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.providerId = providerId;
        this.userId = userId;
        this.role = role;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ProviderUserResponse fromEntity(ProviderUser providerUser) {
        return new ProviderUserResponse(
                providerUser.getId(),
                providerUser.getProviderId(),
                providerUser.getUserId(),
                providerUser.getRole(),
                providerUser.getCreatedAt(),
                providerUser.getUpdatedAt()
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getProviderId() {
        return providerId;
    }

    public void setProviderId(UUID providerId) {
        this.providerId = providerId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
