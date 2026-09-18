package com.paranietharan.tripgrid.provider.controller;

import com.paranietharan.tripgrid.provider.dto.CreateSeatRequest;
import com.paranietharan.tripgrid.provider.dto.SeatResponse;
import com.paranietharan.tripgrid.provider.dto.UpdateSeatRequest;
import com.paranietharan.tripgrid.provider.dto.UpdateSeatStatusRequest;
import com.paranietharan.tripgrid.provider.service.SeatService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/buses/{busId}/seats")
public class SeatController {

    private final SeatService seatService;

    public SeatController(SeatService seatService) {
        this.seatService = seatService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PROVIDER_ADMIN', 'PROVIDER_STAFF')")
    public ResponseEntity<List<SeatResponse>> getSeatsByBusId(@PathVariable UUID busId) {
        return ResponseEntity.ok(seatService.getSeatsByBusId(busId));
    }

    @PostMapping
    @PreAuthorize("hasRole('PROVIDER_ADMIN')")
    public ResponseEntity<SeatResponse> createSeat(
            @PathVariable UUID busId,
            @Valid @RequestBody CreateSeatRequest request) {
        SeatResponse response = seatService.createSeat(busId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{seatId}")
    @PreAuthorize("hasRole('PROVIDER_ADMIN')")
    public ResponseEntity<SeatResponse> updateSeat(
            @PathVariable UUID busId,
            @PathVariable UUID seatId,
            @Valid @RequestBody UpdateSeatRequest request) {
        return ResponseEntity.ok(seatService.updateSeat(busId, seatId, request));
    }

    @PatchMapping("/{seatId}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PROVIDER_ADMIN', 'PROVIDER_STAFF')")
    public ResponseEntity<SeatResponse> updateSeatStatus(
            @PathVariable UUID busId,
            @PathVariable UUID seatId,
            @Valid @RequestBody UpdateSeatStatusRequest request) {
        return ResponseEntity.ok(seatService.updateSeatStatus(busId, seatId, request));
    }
}
