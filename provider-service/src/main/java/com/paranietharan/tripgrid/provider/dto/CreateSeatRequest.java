package com.paranietharan.tripgrid.provider.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateSeatRequest {

    @NotBlank(message = "Seat number is required")
    @Size(max = 10, message = "Seat number must not exceed 10 characters")
    private String seatNumber;

    @NotNull(message = "Row number is required")
    @Min(value = 1, message = "Row number must be at least 1")
    private Integer rowNumber;

    @NotNull(message = "Column number is required")
    @Min(value = 1, message = "Column number must be at least 1")
    private Integer columnNumber;

    public CreateSeatRequest() {
    }

    public CreateSeatRequest(String seatNumber, Integer rowNumber, Integer columnNumber) {
        this.seatNumber = seatNumber;
        this.rowNumber = rowNumber;
        this.columnNumber = columnNumber;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public Integer getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(Integer rowNumber) {
        this.rowNumber = rowNumber;
    }

    public Integer getColumnNumber() {
        return columnNumber;
    }

    public void setColumnNumber(Integer columnNumber) {
        this.columnNumber = columnNumber;
    }
}
