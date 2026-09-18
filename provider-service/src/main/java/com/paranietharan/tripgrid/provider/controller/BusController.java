package com.paranietharan.tripgrid.provider.controller;

import com.paranietharan.tripgrid.provider.dto.BusResponse;
import com.paranietharan.tripgrid.provider.dto.CreateBusRequest;
import com.paranietharan.tripgrid.provider.dto.UpdateBusRequest;
import com.paranietharan.tripgrid.provider.dto.UpdateBusStatusRequest;
import com.paranietharan.tripgrid.provider.service.BusService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/buses")
public class BusController {

    private final BusService busService;

    public BusController(BusService busService) {
        this.busService = busService;
    }

    @PostMapping
    @PreAuthorize("hasRole('PROVIDER_ADMIN')")
    public ResponseEntity<BusResponse> createBus(@Valid @RequestBody CreateBusRequest request) {
        BusResponse response = busService.createBus(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PROVIDER_ADMIN', 'PROVIDER_STAFF')")
    public ResponseEntity<List<BusResponse>> getAllBuses() {
        return ResponseEntity.ok(busService.getAllBuses());
    }

    @GetMapping("/{busId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PROVIDER_ADMIN', 'PROVIDER_STAFF')")
    public ResponseEntity<BusResponse> getBusById(@PathVariable UUID busId) {
        return ResponseEntity.ok(busService.getBusById(busId));
    }

    @PutMapping("/{busId}")
    @PreAuthorize("hasRole('PROVIDER_ADMIN')")
    public ResponseEntity<BusResponse> updateBus(
            @PathVariable UUID busId,
            @Valid @RequestBody UpdateBusRequest request) {
        return ResponseEntity.ok(busService.updateBus(busId, request));
    }

    @PatchMapping("/{busId}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PROVIDER_ADMIN', 'PROVIDER_STAFF')")
    public ResponseEntity<BusResponse> updateBusStatus(
            @PathVariable UUID busId,
            @Valid @RequestBody UpdateBusStatusRequest request) {
        return ResponseEntity.ok(busService.updateBusStatus(busId, request));
    }

    @DeleteMapping("/{busId}")
    @PreAuthorize("hasRole('PROVIDER_ADMIN')")
    public ResponseEntity<Void> deleteBus(@PathVariable UUID busId) {
        busService.deleteBus(busId);
        return ResponseEntity.noContent().build();
    }
}
