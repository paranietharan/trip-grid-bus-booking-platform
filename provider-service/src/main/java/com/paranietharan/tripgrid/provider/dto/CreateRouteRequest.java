package com.paranietharan.tripgrid.provider.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateRouteRequest {

    @NotBlank(message = "Origin is required")
    @Size(max = 100, message = "Origin must not exceed 100 characters")
    private String origin;

    @NotBlank(message = "Destination is required")
    @Size(max = 100, message = "Destination must not exceed 100 characters")
    private String destination;

    @NotNull(message = "Distance is required")
    @DecimalMin(value = "0.1", message = "Distance must be greater than 0")
    private Double distance;

    @NotNull(message = "Estimated duration is required")
    @Min(value = 1, message = "Estimated duration must be at least 1 minute")
    private Integer estimatedDurationMinutes;

    public CreateRouteRequest() {
    }

    public CreateRouteRequest(String origin, String destination, Double distance, Integer estimatedDurationMinutes) {
        this.origin = origin;
        this.destination = destination;
        this.distance = distance;
        this.estimatedDurationMinutes = estimatedDurationMinutes;
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

    public Double getDistance() {
        return distance;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }

    public Integer getEstimatedDurationMinutes() {
        return estimatedDurationMinutes;
    }

    public void setEstimatedDurationMinutes(Integer estimatedDurationMinutes) {
        this.estimatedDurationMinutes = estimatedDurationMinutes;
    }
}
