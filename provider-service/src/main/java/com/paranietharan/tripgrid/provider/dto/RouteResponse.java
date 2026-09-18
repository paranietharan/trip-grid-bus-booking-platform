package com.paranietharan.tripgrid.provider.dto;

import com.paranietharan.tripgrid.provider.entity.Route;
import com.paranietharan.tripgrid.provider.entity.RouteStatus;

import java.time.Instant;
import java.util.UUID;

public class RouteResponse {

    private UUID id;
    private UUID providerId;
    private String origin;
    private String destination;
    private double distance;
    private int estimatedDurationMinutes;
    private RouteStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public RouteResponse() {
    }

    public RouteResponse(UUID id, UUID providerId, String origin, String destination, double distance,
                         int estimatedDurationMinutes, RouteStatus status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.providerId = providerId;
        this.origin = origin;
        this.destination = destination;
        this.distance = distance;
        this.estimatedDurationMinutes = estimatedDurationMinutes;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static RouteResponse fromEntity(Route route) {
        return new RouteResponse(
                route.getId(),
                route.getProviderId(),
                route.getOrigin(),
                route.getDestination(),
                route.getDistance(),
                route.getEstimatedDurationMinutes(),
                route.getStatus(),
                route.getCreatedAt(),
                route.getUpdatedAt()
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

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public int getEstimatedDurationMinutes() {
        return estimatedDurationMinutes;
    }

    public void setEstimatedDurationMinutes(int estimatedDurationMinutes) {
        this.estimatedDurationMinutes = estimatedDurationMinutes;
    }

    public RouteStatus getStatus() {
        return status;
    }

    public void setStatus(RouteStatus status) {
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
