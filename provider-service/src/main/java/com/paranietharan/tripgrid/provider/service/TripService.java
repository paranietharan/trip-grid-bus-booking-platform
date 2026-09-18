package com.paranietharan.tripgrid.provider.service;

import com.paranietharan.tripgrid.provider.dto.*;
import com.paranietharan.tripgrid.provider.entity.*;
import com.paranietharan.tripgrid.provider.exception.BadRequestException;
import com.paranietharan.tripgrid.provider.exception.ForbiddenException;
import com.paranietharan.tripgrid.provider.exception.ResourceNotFoundException;
import com.paranietharan.tripgrid.provider.exception.ScheduleConflictException;
import com.paranietharan.tripgrid.provider.repository.BusRepository;
import com.paranietharan.tripgrid.provider.repository.RouteRepository;
import com.paranietharan.tripgrid.provider.repository.TripRepository;
import com.paranietharan.tripgrid.provider.security.TenantSecurityService;
import com.paranietharan.tripgrid.provider.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class TripService {

    private static final Logger log = LoggerFactory.getLogger(TripService.class);

    private final TripRepository tripRepository;
    private final BusRepository busRepository;
    private final RouteRepository routeRepository;
    private final TenantSecurityService tenantSecurityService;

    public TripService(
            TripRepository tripRepository,
            BusRepository busRepository,
            RouteRepository routeRepository,
            TenantSecurityService tenantSecurityService) {
        this.tripRepository = tripRepository;
        this.busRepository = busRepository;
        this.routeRepository = routeRepository;
        this.tenantSecurityService = tenantSecurityService;
    }

    @Transactional
    public TripResponse createTrip(CreateTripRequest request) {
        UUID providerId = tenantSecurityService.getRequiredProviderId();

        validateTripTimes(request.getDepartureTime(), request.getArrivalTime());

        // Acquire row-level pessimistic write lock on the Bus row to serialize concurrent scheduling transactions for the same bus
        Bus bus = busRepository.findByIdForUpdate(request.getBusId())
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found with id: " + request.getBusId()));

        if (!bus.getProviderId().equals(providerId)) {
            throw new ForbiddenException("Cannot create trip: Bus belongs to another provider");
        }

        if (bus.getStatus() != BusStatus.ACTIVE) {
            throw new BadRequestException("Cannot assign inactive or maintenance bus to a trip");
        }

        Route route = routeRepository.findById(request.getRouteId())
                .orElseThrow(() -> new ResourceNotFoundException("Route not found with id: " + request.getRouteId()));

        if (!route.getProviderId().equals(providerId)) {
            throw new ForbiddenException("Cannot create trip: Route belongs to another provider");
        }

        if (route.getStatus() != RouteStatus.ACTIVE) {
            throw new BadRequestException("Cannot assign inactive route to a trip");
        }

        // Validate schedule conflicts
        List<Trip> conflicting = tripRepository.findOverlappingTrips(
                request.getBusId(),
                request.getDepartureTime(),
                request.getArrivalTime()
        );

        if (!conflicting.isEmpty()) {
            throw new ScheduleConflictException(
                    "Bus " + bus.getRegistrationNumber() + " is already assigned to a conflicting trip between "
                            + request.getDepartureTime() + " and " + request.getArrivalTime()
            );
        }

        Trip trip = new Trip();
        trip.setProviderId(providerId);
        trip.setBusId(request.getBusId());
        trip.setRouteId(request.getRouteId());
        trip.setDepartureTime(request.getDepartureTime());
        trip.setArrivalTime(request.getArrivalTime());
        trip.setPrice(request.getPrice());
        trip.setCurrency(request.getCurrency() != null ? request.getCurrency().trim().toUpperCase() : "LKR");
        trip.setStatus(TripStatus.SCHEDULED);

        Trip saved = tripRepository.save(trip);
        log.info("Created scheduled trip id: {} for provider: {}", saved.getId(), providerId);
        return TripResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<TripResponse> getAllTrips() {
        UserPrincipal principal = tenantSecurityService.getAuthenticatedPrincipal();
        if (principal.isSuperAdmin()) {
            return tripRepository.findAll()
                    .stream()
                    .map(TripResponse::fromEntity)
                    .toList();
        }

        UUID providerId = tenantSecurityService.resolveProviderIdForPrincipal(principal);
        return tripRepository.findByProviderId(providerId)
                .stream()
                .map(TripResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public TripResponse getTripById(UUID tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));

        tenantSecurityService.verifyProviderAccess(trip.getProviderId());
        return TripResponse.fromEntity(trip);
    }

    @Transactional
    public TripResponse updateTrip(UUID tripId, UpdateTripRequest request) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));

        tenantSecurityService.verifyProviderAccess(trip.getProviderId());

        validateTripTimes(request.getDepartureTime(), request.getArrivalTime());

        // Acquire row-level pessimistic write lock on the Bus row to serialize concurrent scheduling transactions for the same bus
        Bus bus = busRepository.findByIdForUpdate(request.getBusId())
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found with id: " + request.getBusId()));

        if (!bus.getProviderId().equals(trip.getProviderId())) {
            throw new ForbiddenException("Cannot update trip: Bus belongs to another provider");
        }

        if (bus.getStatus() != BusStatus.ACTIVE) {
            throw new BadRequestException("Cannot assign inactive or maintenance bus to a trip");
        }

        Route route = routeRepository.findById(request.getRouteId())
                .orElseThrow(() -> new ResourceNotFoundException("Route not found with id: " + request.getRouteId()));

        if (!route.getProviderId().equals(trip.getProviderId())) {
            throw new ForbiddenException("Cannot update trip: Route belongs to another provider");
        }

        if (route.getStatus() != RouteStatus.ACTIVE) {
            throw new BadRequestException("Cannot assign inactive route to a trip");
        }

        // Validate schedule conflicts excluding this trip
        List<Trip> conflicting = tripRepository.findOverlappingTripsExcludingId(
                request.getBusId(),
                request.getDepartureTime(),
                request.getArrivalTime(),
                tripId
        );

        if (!conflicting.isEmpty()) {
            throw new ScheduleConflictException(
                    "Bus " + bus.getRegistrationNumber() + " is already assigned to a conflicting trip between "
                            + request.getDepartureTime() + " and " + request.getArrivalTime()
            );
        }

        trip.setBusId(request.getBusId());
        trip.setRouteId(request.getRouteId());
        trip.setDepartureTime(request.getDepartureTime());
        trip.setArrivalTime(request.getArrivalTime());
        trip.setPrice(request.getPrice());
        if (request.getCurrency() != null) {
            trip.setCurrency(request.getCurrency().trim().toUpperCase());
        }
        trip.setUpdatedAt(Instant.now());

        Trip updated = tripRepository.save(trip);
        log.info("Updated trip id: {}", updated.getId());
        return TripResponse.fromEntity(updated);
    }

    @Transactional
    public TripResponse updateTripStatus(UUID tripId, UpdateTripStatusRequest request) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));

        tenantSecurityService.verifyProviderAccess(trip.getProviderId());

        trip.setStatus(request.getStatus());
        trip.setUpdatedAt(Instant.now());

        Trip updated = tripRepository.save(trip);
        log.info("Updated trip status id: {} to {}", tripId, request.getStatus());
        return TripResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteTrip(UUID tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));

        tenantSecurityService.verifyProviderAccess(trip.getProviderId());
        tripRepository.delete(trip);
        log.info("Deleted trip id: {}", tripId);
    }

    @Transactional(readOnly = true)
    public List<TripResponse> searchTrips(String origin, String destination, LocalDate departureDate,
                                          UUID providerId, BigDecimal minPrice, BigDecimal maxPrice) {
        Instant fromTime = null;
        Instant toTime = null;

        if (departureDate != null) {
            fromTime = departureDate.atStartOfDay().toInstant(ZoneOffset.UTC);
            // Use exclusive upper bound for the start of next UTC day
            toTime = departureDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        }

        return tripRepository.searchTrips(origin, destination, fromTime, toTime, providerId, minPrice, maxPrice)
                .stream()
                .map(TripResponse::fromEntity)
                .toList();
    }

    private void validateTripTimes(Instant departureTime, Instant arrivalTime) {
        if (departureTime == null || arrivalTime == null) {
            throw new BadRequestException("Departure time and arrival time are required");
        }
        if (!departureTime.isBefore(arrivalTime)) {
            throw new BadRequestException("Departure time must be strictly before arrival time");
        }
    }
}
