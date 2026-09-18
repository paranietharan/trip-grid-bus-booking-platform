package com.paranietharan.tripgrid.provider.dto;

import com.paranietharan.tripgrid.provider.entity.RouteStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateRouteStatusRequest {

    @NotNull(message = "Status is required")
    private RouteStatus status;

    public UpdateRouteStatusRequest() {
    }

    public UpdateRouteStatusRequest(RouteStatus status) {
        this.status = status;
    }

    public RouteStatus getStatus() {
        return status;
    }

    public void setStatus(RouteStatus status) {
        this.status = status;
    }
}
