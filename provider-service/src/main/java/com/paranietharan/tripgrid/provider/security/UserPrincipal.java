package com.paranietharan.tripgrid.provider.security;

import com.paranietharan.tripgrid.provider.entity.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class UserPrincipal implements UserDetails {

    private final UUID userId;
    private final String email;
    private final Role role;
    private final String tenantId;

    public UserPrincipal(UUID userId, String email, Role role, String tenantId) {
        this.userId = userId;
        this.email = email;
        this.role = role;
        this.tenantId = tenantId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }

    public String getTenantId() {
        return tenantId;
    }

    public boolean isSuperAdmin() {
        return this.role == Role.SUPER_ADMIN;
    }

    public boolean isProviderAdmin() {
        return this.role == Role.PROVIDER_ADMIN;
    }

    public boolean isProviderStaff() {
        return this.role == Role.PROVIDER_STAFF;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return null; // Provider service does not manage passwords
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
