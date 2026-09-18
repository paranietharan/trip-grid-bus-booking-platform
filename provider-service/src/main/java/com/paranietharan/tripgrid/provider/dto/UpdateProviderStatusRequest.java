package com.paranietharan.tripgrid.provider.dto;

import com.paranietharan.tripgrid.provider.entity.ProviderStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateProviderStatusRequest {

    @NotNull(message = "Status is required")
    private ProviderStatus status;

    public UpdateProviderStatusRequest() {
    }

    public UpdateProviderStatusRequest(ProviderStatus status) {
        this.status = status;
    }

    public ProviderStatus getStatus() {
        return status;
    }

    public void setStatus(ProviderStatus status) {
        this.status = status;
    }
}
