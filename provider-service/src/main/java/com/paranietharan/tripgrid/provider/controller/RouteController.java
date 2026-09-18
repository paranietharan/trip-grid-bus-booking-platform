package com.paranietharan.tripgrid.provider.controller;

import com.paranietharan.tripgrid.provider.dto.*;
import com.paranietharan.tripgrid.provider.service.RouteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/routes")
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @PostMapping
    @PreAuthorize("hasRole('PROVIDER_ADMIN')")
    public ResponseEntity<RouteResponse> createRoute(@Valid @RequestBody CreateRouteRequest request) {
        RouteResponse response = routeService.createRoute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PROVIDER_ADMIN', 'PROVIDER_STAFF')")
    public ResponseEntity<List<RouteResponse>> getAllRoutes() {
        return ResponseEntity.ok(routeService.getAllRoutes());
    }

    @GetMapping("/{routeId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PROVIDER_ADMIN', 'PROVIDER_STAFF')")
    public ResponseEntity<RouteResponse> getRouteById(@PathVariable UUID routeId) {
        return ResponseEntity.ok(routeService.getRouteById(routeId));
    }

    @PutMapping("/{routeId}")
    @PreAuthorize("hasRole('PROVIDER_ADMIN')")
    public ResponseEntity<RouteResponse> updateRoute(
            @PathVariable UUID routeId,
            @Valid @RequestBody UpdateRouteRequest request) {
        return ResponseEntity.ok(routeService.updateRoute(routeId, request));
    }

    @PatchMapping("/{routeId}/status")
    @PreAuthorize("hasRole('PROVIDER_ADMIN')")
    public ResponseEntity<RouteResponse> updateRouteStatus(
            @PathVariable UUID routeId,
            @Valid @RequestBody UpdateRouteStatusRequest request) {
        return ResponseEntity.ok(routeService.updateRouteStatus(routeId, request));
    }

    @DeleteMapping("/{routeId}")
    @PreAuthorize("hasRole('PROVIDER_ADMIN')")
    public ResponseEntity<Void> deleteRoute(@PathVariable UUID routeId) {
        routeService.deleteRoute(routeId);
        return ResponseEntity.noContent().build();
    }
}
