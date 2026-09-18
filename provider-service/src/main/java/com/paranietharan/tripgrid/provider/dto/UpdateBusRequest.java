package com.paranietharan.tripgrid.provider.dto;

import com.paranietharan.tripgrid.provider.entity.BusType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UpdateBusRequest {

    @NotBlank(message = "Bus name is required")
    @Size(max = 100, message = "Bus name must not exceed 100 characters")
    private String name;

    @NotNull(message = "Bus type is required")
    private BusType busType;

    @NotNull(message = "Seat count is required")
    @Min(value = 1, message = "Seat count must be at least 1")
    @Max(value = 100, message = "Seat count cannot exceed 100")
    private Integer seatCount;

    public UpdateBusRequest() {
    }

    public UpdateBusRequest(String name, BusType busType, Integer seatCount) {
        this.name = name;
        this.busType = busType;
        this.seatCount = seatCount;
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

    public Integer getSeatCount() {
        return seatCount;
    }

    public void setSeatCount(Integer seatCount) {
        this.seatCount = seatCount;
    }
}
