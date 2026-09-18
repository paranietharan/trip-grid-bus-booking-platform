package com.paranietharan.tripgrid.provider.service;

import com.paranietharan.tripgrid.provider.dto.CreateTripRequest;
import com.paranietharan.tripgrid.provider.dto.TripResponse;
import com.paranietharan.tripgrid.provider.entity.*;
import com.paranietharan.tripgrid.provider.exception.BadRequestException;
import com.paranietharan.tripgrid.provider.exception.ForbiddenException;
import com.paranietharan.tripgrid.provider.exception.ScheduleConflictException;
import com.paranietharan.tripgrid.provider.repository.BusRepository;
import com.paranietharan.tripgrid.provider.repository.RouteRepository;
import com.paranietharan.tripgrid.provider.repository.TripRepository;
import com.paranietharan.tripgrid.provider.security.TenantSecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    @Mock
    private TripRepository tripRepository;

    @Mock
    private BusRepository busRepository;

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private TenantSecurityService tenantSecurityService;

    @InjectMocks
    private TripService tripService;

    private UUID providerId;
    private UUID otherProviderId;
    private UUID busId;
    private UUID routeId;
    private Bus bus;
    private Route route;
    private Instant departureTime;
    private Instant arrivalTime;

    @BeforeEach
    void setUp() {
        providerId = UUID.randomUUID();
        otherProviderId = UUID.randomUUID();
        busId = UUID.randomUUID();
        routeId = UUID.randomUUID();

        departureTime = Instant.now().plus(1, ChronoUnit.DAYS);
        arrivalTime = departureTime.plus(3, ChronoUnit.HOURS);

        bus = new Bus(busId, providerId, "WP-1234", "Express", BusType.LUXURY, 40, BusStatus.ACTIVE, Instant.now(), Instant.now());
        route = new Route(routeId, providerId, "Colombo", "Kandy", 115.0, 210, RouteStatus.ACTIVE, Instant.now(), Instant.now());
    }

    @Test
    @DisplayName("Should successfully create a trip when all conditions and schedule checks pass")
    void shouldCreateTripSuccessfully() {
        CreateTripRequest request = new CreateTripRequest(
                busId, routeId, departureTime, arrivalTime, new BigDecimal("2500.00"), "LKR"
        );

        when(tenantSecurityService.getRequiredProviderId()).thenReturn(providerId);
        when(busRepository.findByIdForUpdate(busId)).thenReturn(Optional.of(bus));
        when(routeRepository.findById(routeId)).thenReturn(Optional.of(route));
        when(tripRepository.findOverlappingTrips(busId, departureTime, arrivalTime)).thenReturn(Collections.emptyList());

        Trip savedTrip = new Trip(UUID.randomUUID(), providerId, busId, routeId, departureTime, arrivalTime,
                new BigDecimal("2500.00"), "LKR", TripStatus.SCHEDULED, Instant.now(), Instant.now());
        when(tripRepository.save(any(Trip.class))).thenReturn(savedTrip);

        TripResponse response = tripService.createTrip(request);

        assertThat(response).isNotNull();
        assertThat(response.getBusId()).isEqualTo(busId);
        assertThat(response.getRouteId()).isEqualTo(routeId);
        assertThat(response.getStatus()).isEqualTo(TripStatus.SCHEDULED);
        verify(tripRepository).save(any(Trip.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException when departure time is after arrival time")
    void shouldThrowBadRequestWhenInvalidTimes() {
        CreateTripRequest request = new CreateTripRequest(
                busId, routeId, arrivalTime, departureTime, new BigDecimal("2500.00"), "LKR"
        );

        when(tenantSecurityService.getRequiredProviderId()).thenReturn(providerId);

        assertThatThrownBy(() -> tripService.createTrip(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("before arrival time");
    }

    @Test
    @DisplayName("Should throw ForbiddenException when assigning bus from another provider")
    void shouldThrowForbiddenWhenBusBelongsToAnotherProvider() {
        Bus otherBus = new Bus(busId, otherProviderId, "WP-9999", "Other Express", BusType.LUXURY, 40, BusStatus.ACTIVE, Instant.now(), Instant.now());
        CreateTripRequest request = new CreateTripRequest(
                busId, routeId, departureTime, arrivalTime, new BigDecimal("2500.00"), "LKR"
        );

        when(tenantSecurityService.getRequiredProviderId()).thenReturn(providerId);
        when(busRepository.findByIdForUpdate(busId)).thenReturn(Optional.of(otherBus));

        assertThatThrownBy(() -> tripService.createTrip(request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Bus belongs to another provider");
    }

    @Test
    @DisplayName("Should throw ScheduleConflictException when bus is already scheduled for overlapping trip")
    void shouldThrowScheduleConflictWhenOverlapping() {
        CreateTripRequest request = new CreateTripRequest(
                busId, routeId, departureTime, arrivalTime, new BigDecimal("2500.00"), "LKR"
        );

        Trip conflictingTrip = new Trip(UUID.randomUUID(), providerId, busId, routeId, departureTime, arrivalTime,
                new BigDecimal("2500.00"), "LKR", TripStatus.SCHEDULED, Instant.now(), Instant.now());

        when(tenantSecurityService.getRequiredProviderId()).thenReturn(providerId);
        when(busRepository.findByIdForUpdate(busId)).thenReturn(Optional.of(bus));
        when(routeRepository.findById(routeId)).thenReturn(Optional.of(route));
        when(tripRepository.findOverlappingTrips(busId, departureTime, arrivalTime)).thenReturn(List.of(conflictingTrip));

        assertThatThrownBy(() -> tripService.createTrip(request))
                .isInstanceOf(ScheduleConflictException.class)
                .hasMessageContaining("conflicting trip");
    }
}
