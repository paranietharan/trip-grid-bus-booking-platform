package com.paranietharan.tripgrid.provider.dto;

import com.paranietharan.tripgrid.provider.entity.Provider;
import com.paranietharan.tripgrid.provider.entity.ProviderStatus;

import java.time.Instant;
import java.util.UUID;

public class ProviderResponse {

    private UUID id;
    private String name;
    private String email;
    private String phoneNumber;
    private String address;
    private ProviderStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public ProviderResponse() {
    }

    public ProviderResponse(UUID id, String name, String email, String phoneNumber, String address,
                            ProviderStatus status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ProviderResponse fromEntity(Provider provider) {
        return new ProviderResponse(
                provider.getId(),
                provider.getName(),
                provider.getEmail(),
                provider.getPhoneNumber(),
                provider.getAddress(),
                provider.getStatus(),
                provider.getCreatedAt(),
                provider.getUpdatedAt()
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public ProviderStatus getStatus() {
        return status;
    }

    public void setStatus(ProviderStatus status) {
        this.status = status;
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
