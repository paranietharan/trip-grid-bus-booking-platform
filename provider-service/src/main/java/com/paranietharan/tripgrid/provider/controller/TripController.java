package com.paranietharan.tripgrid.provider.controller;

import com.paranietharan.tripgrid.provider.dto.*;
import com.paranietharan.tripgrid.provider.service.TripService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PROVIDER_ADMIN', 'PROVIDER_STAFF')")
    public ResponseEntity<TripResponse> createTrip(@Valid @RequestBody CreateTripRequest request) {
        TripResponse response = tripService.createTrip(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PROVIDER_ADMIN', 'PROVIDER_STAFF')")
    public ResponseEntity<List<TripResponse>> getAllTrips() {
        return ResponseEntity.ok(tripService.getAllTrips());
    }

    @GetMapping("/{tripId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PROVIDER_ADMIN', 'PROVIDER_STAFF')")
    public ResponseEntity<TripResponse> getTripById(@PathVariable UUID tripId) {
        return ResponseEntity.ok(tripService.getTripById(tripId));
    }

    @PutMapping("/{tripId}")
    @PreAuthorize("hasAnyRole('PROVIDER_ADMIN', 'PROVIDER_STAFF')")
    public ResponseEntity<TripResponse> updateTrip(
            @PathVariable UUID tripId,
            @Valid @RequestBody UpdateTripRequest request) {
        return ResponseEntity.ok(tripService.updateTrip(tripId, request));
    }

    @PatchMapping("/{tripId}/status")
    @PreAuthorize("hasAnyRole('PROVIDER_ADMIN', 'PROVIDER_STAFF')")
    public ResponseEntity<TripResponse> updateTripStatus(
            @PathVariable UUID tripId,
            @Valid @RequestBody UpdateTripStatusRequest request) {
        return ResponseEntity.ok(tripService.updateTripStatus(tripId, request));
    }

    @DeleteMapping("/{tripId}")
    @PreAuthorize("hasRole('PROVIDER_ADMIN')")
    public ResponseEntity<Void> deleteTrip(@PathVariable UUID tripId) {
        tripService.deleteTrip(tripId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<TripResponse>> searchTrips(
            @RequestParam(required = false) String origin,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate departureDate,
            @RequestParam(required = false) UUID providerId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice) {

        return ResponseEntity.ok(tripService.searchTrips(origin, destination, departureDate, providerId, minPrice, maxPrice));
    }
}
