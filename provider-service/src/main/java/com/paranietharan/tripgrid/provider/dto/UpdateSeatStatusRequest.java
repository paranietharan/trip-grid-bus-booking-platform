package com.paranietharan.tripgrid.provider.dto;

import com.paranietharan.tripgrid.provider.entity.SeatStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateSeatStatusRequest {

    @NotNull(message = "Status is required")
    private SeatStatus status;

    public UpdateSeatStatusRequest() {
    }

    public UpdateSeatStatusRequest(SeatStatus status) {
        this.status = status;
    }

    public SeatStatus getStatus() {
        return status;
    }

    public void setStatus(SeatStatus status) {
        this.status = status;
    }
}
