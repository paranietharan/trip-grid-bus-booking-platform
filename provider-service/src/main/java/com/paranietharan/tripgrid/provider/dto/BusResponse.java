package com.paranietharan.tripgrid.provider.dto;

import com.paranietharan.tripgrid.provider.entity.Bus;
import com.paranietharan.tripgrid.provider.entity.BusStatus;
import com.paranietharan.tripgrid.provider.entity.BusType;

import java.time.Instant;
import java.util.UUID;

public class BusResponse {

    private UUID id;
    private UUID providerId;
    private String registrationNumber;
    private String name;
    private BusType busType;
    private int seatCount;
    private BusStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public BusResponse() {
    }

    public BusResponse(UUID id, UUID providerId, String registrationNumber, String name,
                       BusType busType, int seatCount, BusStatus status,
                       Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.providerId = providerId;
        this.registrationNumber = registrationNumber;
        this.name = name;
        this.busType = busType;
        this.seatCount = seatCount;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static BusResponse fromEntity(Bus bus) {
        return new BusResponse(
                bus.getId(),
                bus.getProviderId(),
                bus.getRegistrationNumber(),
                bus.getName(),
                bus.getBusType(),
                bus.getSeatCount(),
                bus.getStatus(),
                bus.getCreatedAt(),
                bus.getUpdatedAt()
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

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BusType getBusType() {
        return busType;
    }

    public void setBusType(BusType busType) {
        this.busType = busType;
    }

    public int getSeatCount() {
        return seatCount;
    }

    public void setSeatCount(int seatCount) {
        this.seatCount = seatCount;
    }

    public BusStatus getStatus() {
        return status;
    }

    public void setStatus(BusStatus status) {
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
