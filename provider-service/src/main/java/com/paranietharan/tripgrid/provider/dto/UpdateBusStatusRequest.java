package com.paranietharan.tripgrid.provider.dto;

import com.paranietharan.tripgrid.provider.entity.BusStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateBusStatusRequest {

    @NotNull(message = "Bus status is required")
    private BusStatus status;

    public UpdateBusStatusRequest() {
    }

    public UpdateBusStatusRequest(BusStatus status) {
        this.status = status;
    }

    public BusStatus getStatus() {
        return status;
    }

    public void setStatus(BusStatus status) {
        this.status = status;
    }
}
