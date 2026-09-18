package com.paranietharan.tripgrid.provider.dto;

import com.paranietharan.tripgrid.provider.entity.TripStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateTripStatusRequest {

    @NotNull(message = "Trip status is required")
    private TripStatus status;

    public UpdateTripStatusRequest() {
    }

    public UpdateTripStatusRequest(TripStatus status) {
        this.status = status;
    }

    public TripStatus getStatus() {
        return status;
    }

    public void setStatus(TripStatus status) {
        this.status = status;
    }
}
