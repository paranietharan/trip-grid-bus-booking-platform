package com.paranietharan.tripgrid.provider.dto;

import com.paranietharan.tripgrid.provider.entity.Role;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class AddProviderUserRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotNull(message = "Role is required")
    private Role role;

    public AddProviderUserRequest() {
    }

    public AddProviderUserRequest(UUID userId, Role role) {
        this.userId = userId;
        this.role = role;
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
}
