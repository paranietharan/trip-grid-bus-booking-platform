package com.paranietharan.tripgrid.provider.service;

import com.paranietharan.tripgrid.provider.dto.*;
import com.paranietharan.tripgrid.provider.entity.Route;
import com.paranietharan.tripgrid.provider.entity.RouteStatus;
import com.paranietharan.tripgrid.provider.exception.ConflictException;
import com.paranietharan.tripgrid.provider.exception.ResourceNotFoundException;
import com.paranietharan.tripgrid.provider.repository.RouteRepository;
import com.paranietharan.tripgrid.provider.security.TenantSecurityService;
import com.paranietharan.tripgrid.provider.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class RouteService {

    private static final Logger log = LoggerFactory.getLogger(RouteService.class);

    private final RouteRepository routeRepository;
    private final TenantSecurityService tenantSecurityService;

    public RouteService(
            RouteRepository routeRepository,
            TenantSecurityService tenantSecurityService) {
        this.routeRepository = routeRepository;
        this.tenantSecurityService = tenantSecurityService;
    }

    @Transactional
    public RouteResponse createRoute(CreateRouteRequest request) {
        UUID providerId = tenantSecurityService.getRequiredProviderId();
        String origin = request.getOrigin().trim();
        String destination = request.getDestination().trim();

        if (origin.equalsIgnoreCase(destination)) {
            throw new ConflictException("Route origin and destination cannot be identical");
        }

        if (routeRepository.existsByProviderIdAndOriginIgnoreCaseAndDestinationIgnoreCase(providerId, origin, destination)) {
            throw new ConflictException("Route from " + origin + " to " + destination + " already exists for this provider");
        }

        Route route = new Route();
        route.setProviderId(providerId);
        route.setOrigin(origin);
        route.setDestination(destination);
        route.setDistance(request.getDistance());
        route.setEstimatedDurationMinutes(request.getEstimatedDurationMinutes());
        route.setStatus(RouteStatus.ACTIVE);

        Route saved = routeRepository.save(route);
        log.info("Created route id: {} ({} -> {}) for provider: {}", saved.getId(), origin, destination, providerId);
        return RouteResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<RouteResponse> getAllRoutes() {
        UserPrincipal principal = tenantSecurityService.getAuthenticatedPrincipal();
        if (principal.isSuperAdmin()) {
            return routeRepository.findAll()
                    .stream()
                    .map(RouteResponse::fromEntity)
                    .toList();
        }

        UUID providerId = tenantSecurityService.resolveProviderIdForPrincipal(principal);
        return routeRepository.findByProviderId(providerId)
                .stream()
                .map(RouteResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public RouteResponse getRouteById(UUID routeId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found with id: " + routeId));

        tenantSecurityService.verifyProviderAccess(route.getProviderId());
        return RouteResponse.fromEntity(route);
    }

    @Transactional
    public RouteResponse updateRoute(UUID routeId, UpdateRouteRequest request) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found with id: " + routeId));

        tenantSecurityService.verifyProviderAccess(route.getProviderId());

        String origin = request.getOrigin().trim();
        String destination = request.getDestination().trim();

        if (origin.equalsIgnoreCase(destination)) {
            throw new ConflictException("Route origin and destination cannot be identical");
        }

        if (routeRepository.existsByProviderIdAndOriginIgnoreCaseAndDestinationIgnoreCaseAndIdNot(route.getProviderId(), origin, destination, routeId)) {
            throw new ConflictException("Another route from " + origin + " to " + destination + " already exists");
        }

        route.setOrigin(origin);
        route.setDestination(destination);
        route.setDistance(request.getDistance());
        route.setEstimatedDurationMinutes(request.getEstimatedDurationMinutes());
        route.setUpdatedAt(Instant.now());

        Route updated = routeRepository.save(route);
        log.info("Updated route id: {}", updated.getId());
        return RouteResponse.fromEntity(updated);
    }

    @Transactional
    public RouteResponse updateRouteStatus(UUID routeId, UpdateRouteStatusRequest request) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found with id: " + routeId));

        tenantSecurityService.verifyProviderAccess(route.getProviderId());

        route.setStatus(request.getStatus());
        route.setUpdatedAt(Instant.now());

        Route updated = routeRepository.save(route);
        log.info("Updated route status id: {} to {}", routeId, request.getStatus());
        return RouteResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteRoute(UUID routeId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found with id: " + routeId));

        tenantSecurityService.verifyProviderAccess(route.getProviderId());
        routeRepository.delete(route);
        log.info("Deleted route id: {}", routeId);
    }
}
