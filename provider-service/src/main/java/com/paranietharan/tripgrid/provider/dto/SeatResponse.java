package com.paranietharan.tripgrid.provider.dto;

import com.paranietharan.tripgrid.provider.entity.Seat;
import com.paranietharan.tripgrid.provider.entity.SeatStatus;

import java.time.Instant;
import java.util.UUID;

public class SeatResponse {

    private UUID id;
    private UUID busId;
    private String seatNumber;
    private int rowNumber;
    private int columnNumber;
    private SeatStatus status;
    private Instant createdAt;

    public SeatResponse() {
    }

    public SeatResponse(UUID id, UUID busId, String seatNumber, int rowNumber, int columnNumber,
                        SeatStatus status, Instant createdAt) {
        this.id = id;
        this.busId = busId;
        this.seatNumber = seatNumber;
        this.rowNumber = rowNumber;
        this.columnNumber = columnNumber;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static SeatResponse fromEntity(Seat seat) {
        return new SeatResponse(
                seat.getId(),
                seat.getBusId(),
                seat.getSeatNumber(),
                seat.getRowNumber(),
                seat.getColumnNumber(),
                seat.getStatus(),
                seat.getCreatedAt()
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getBusId() {
        return busId;
    }

    public void setBusId(UUID busId) {
        this.busId = busId;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(int rowNumber) {
        this.rowNumber = rowNumber;
    }

    public int getColumnNumber() {
        return columnNumber;
    }

    public void setColumnNumber(int columnNumber) {
        this.columnNumber = columnNumber;
    }

    public SeatStatus getStatus() {
        return status;
    }

    public void setStatus(SeatStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
