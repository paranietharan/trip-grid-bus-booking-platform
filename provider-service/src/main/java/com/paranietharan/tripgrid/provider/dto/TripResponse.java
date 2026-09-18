package com.paranietharan.tripgrid.provider.dto;

import com.paranietharan.tripgrid.provider.entity.Trip;
import com.paranietharan.tripgrid.provider.entity.TripStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class TripResponse {

    private UUID id;
    private UUID providerId;
    private UUID busId;
    private UUID routeId;
    private Instant departureTime;
    private Instant arrivalTime;
    private BigDecimal price;
    private String currency;
    private TripStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public TripResponse() {
    }

    public TripResponse(UUID id, UUID providerId, UUID busId, UUID routeId, Instant departureTime,
                        Instant arrivalTime, BigDecimal price, String currency, TripStatus status,
                        Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.providerId = providerId;
        this.busId = busId;
        this.routeId = routeId;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.price = price;
        this.currency = currency;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static TripResponse fromEntity(Trip trip) {
        return new TripResponse(
                trip.getId(),
                trip.getProviderId(),
                trip.getBusId(),
                trip.getRouteId(),
                trip.getDepartureTime(),
                trip.getArrivalTime(),
                trip.getPrice(),
                trip.getCurrency(),
                trip.getStatus(),
                trip.getCreatedAt(),
                trip.getUpdatedAt()
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

    public UUID getBusId() {
        return busId;
    }

    public void setBusId(UUID busId) {
        this.busId = busId;
    }

    public UUID getRouteId() {
        return routeId;
    }

    public void setRouteId(UUID routeId) {
        this.routeId = routeId;
    }

    public Instant getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(Instant departureTime) {
        this.departureTime = departureTime;
    }

    public Instant getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(Instant arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public TripStatus getStatus() {
        return status;
    }

    public void setStatus(TripStatus status) {
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
