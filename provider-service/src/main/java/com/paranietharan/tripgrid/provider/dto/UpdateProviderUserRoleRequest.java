package com.paranietharan.tripgrid.provider.dto;

import com.paranietharan.tripgrid.provider.entity.Role;
import jakarta.validation.constraints.NotNull;

public class UpdateProviderUserRoleRequest {

    @NotNull(message = "Role is required")
    private Role role;

    public UpdateProviderUserRoleRequest() {
    }

    public UpdateProviderUserRoleRequest(Role role) {
        this.role = role;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
