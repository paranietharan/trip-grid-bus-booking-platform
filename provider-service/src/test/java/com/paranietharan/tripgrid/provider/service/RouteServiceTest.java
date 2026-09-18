package com.paranietharan.tripgrid.provider.service;

import com.paranietharan.tripgrid.provider.dto.CreateRouteRequest;
import com.paranietharan.tripgrid.provider.dto.RouteResponse;
import com.paranietharan.tripgrid.provider.entity.Route;
import com.paranietharan.tripgrid.provider.entity.RouteStatus;
import com.paranietharan.tripgrid.provider.exception.ConflictException;
import com.paranietharan.tripgrid.provider.exception.ForbiddenException;
import com.paranietharan.tripgrid.provider.repository.RouteRepository;
import com.paranietharan.tripgrid.provider.security.TenantSecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RouteServiceTest {

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private TenantSecurityService tenantSecurityService;

    @InjectMocks
    private RouteService routeService;

    private UUID providerId;
    private UUID routeId;
    private Route route;

    @BeforeEach
    void setUp() {
        providerId = UUID.randomUUID();
        routeId = UUID.randomUUID();
        route = new Route(routeId, providerId, "Colombo", "Kandy", 115.0, 210, RouteStatus.ACTIVE, Instant.now(), Instant.now());
    }

    @Test
    @DisplayName("Should successfully create a route")
    void shouldCreateRoute() {
        CreateRouteRequest request = new CreateRouteRequest("Colombo", "Kandy", 115.0, 210);

        when(tenantSecurityService.getRequiredProviderId()).thenReturn(providerId);
        when(routeRepository.existsByProviderIdAndOriginIgnoreCaseAndDestinationIgnoreCase(providerId, "Colombo", "Kandy")).thenReturn(false);
        when(routeRepository.save(any(Route.class))).thenReturn(route);

        RouteResponse response = routeService.createRoute(request);

        assertThat(response).isNotNull();
        assertThat(response.getOrigin()).isEqualTo("Colombo");
        assertThat(response.getDestination()).isEqualTo("Kandy");
        verify(routeRepository).save(any(Route.class));
    }

    @Test
    @DisplayName("Should throw ConflictException when origin and destination are identical")
    void shouldThrowConflictWhenOriginEqualsDestination() {
        CreateRouteRequest request = new CreateRouteRequest("Colombo", "Colombo", 10.0, 30);

        when(tenantSecurityService.getRequiredProviderId()).thenReturn(providerId);

        assertThatThrownBy(() -> routeService.createRoute(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("identical");
    }

    @Test
    @DisplayName("Should throw ForbiddenException on cross-tenant route access")
    void shouldThrowForbiddenOnCrossTenantAccess() {
        when(routeRepository.findById(routeId)).thenReturn(Optional.of(route));
        doThrow(new ForbiddenException("Access denied")).when(tenantSecurityService).verifyProviderAccess(providerId);

        assertThatThrownBy(() -> routeService.getRouteById(routeId))
                .isInstanceOf(ForbiddenException.class);
    }
}
