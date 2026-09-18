package com.paranietharan.tripgrid.provider.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "seats", uniqueConstraints = {
        @UniqueConstraint(name = "uq_seat_bus_num", columnNames = {"bus_id", "seat_number"})
})
public class Seat {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "bus_id", nullable = false)
    private UUID busId;

    @Column(name = "seat_number", nullable = false, length = 10)
    private String seatNumber;

    @Column(name = "row_number", nullable = false)
    private int rowNumber;

    @Column(name = "column_number", nullable = false)
    private int columnNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private SeatStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Seat() {
    }

    public Seat(UUID id, UUID busId, String seatNumber, int rowNumber, int columnNumber,
                SeatStatus status, Instant createdAt) {
        this.id = id;
        this.busId = busId;
        this.seatNumber = seatNumber;
        this.rowNumber = rowNumber;
        this.columnNumber = columnNumber;
        this.status = status;
        this.createdAt = createdAt;
    }

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.status == null) {
            this.status = SeatStatus.ACTIVE;
        }
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
        this.seatNumber = seatNumber != null ? seatNumber.trim().toUpperCase() : null;
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
