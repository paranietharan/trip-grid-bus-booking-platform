package com.paranietharan.tripgrid.provider.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paranietharan.tripgrid.provider.config.SecurityConfig;
import com.paranietharan.tripgrid.provider.dto.CreateTripRequest;
import com.paranietharan.tripgrid.provider.dto.TripResponse;
import com.paranietharan.tripgrid.provider.entity.Role;
import com.paranietharan.tripgrid.provider.entity.TripStatus;
import com.paranietharan.tripgrid.provider.security.JwtAuthenticationFilter;
import com.paranietharan.tripgrid.provider.security.JwtService;
import com.paranietharan.tripgrid.provider.security.UserPrincipal;
import com.paranietharan.tripgrid.provider.service.TripService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TripController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class TripControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TripService tripService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    @DisplayName("GET /api/trips/search should be publicly accessible without authentication")
    void shouldAllowPublicSearch() throws Exception {
        UUID tripId = UUID.randomUUID();
        UUID providerId = UUID.randomUUID();
        UUID busId = UUID.randomUUID();
        UUID routeId = UUID.randomUUID();
        Instant dep = Instant.now().plus(1, ChronoUnit.DAYS);
        Instant arr = dep.plus(3, ChronoUnit.HOURS);

        TripResponse trip = new TripResponse(tripId, providerId, busId, routeId, dep, arr, new BigDecimal("2500.00"), "LKR", TripStatus.SCHEDULED, Instant.now(), Instant.now());
        when(tripService.searchTrips(any(), any(), any(), any(), any(), any())).thenReturn(List.of(trip));

        mockMvc.perform(get("/api/trips/search?origin=Colombo&destination=Kandy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(tripId.toString()))
                .andExpect(jsonPath("$[0].currency").value("LKR"));
    }

    @Test
    @DisplayName("POST /api/trips should create trip when authenticated as PROVIDER_ADMIN")
    void shouldCreateTripWhenProviderAdmin() throws Exception {
        UUID tripId = UUID.randomUUID();
        UUID providerId = UUID.randomUUID();
        UUID busId = UUID.randomUUID();
        UUID routeId = UUID.randomUUID();
        Instant dep = Instant.now().plus(1, ChronoUnit.DAYS);
        Instant arr = dep.plus(3, ChronoUnit.HOURS);

        CreateTripRequest request = new CreateTripRequest(busId, routeId, dep, arr, new BigDecimal("2500.00"), "LKR");
        TripResponse trip = new TripResponse(tripId, providerId, busId, routeId, dep, arr, new BigDecimal("2500.00"), "LKR", TripStatus.SCHEDULED, Instant.now(), Instant.now());
        UserPrincipal providerAdmin = new UserPrincipal(UUID.randomUUID(), "provider@tripgrid.com", Role.PROVIDER_ADMIN, providerId.toString());

        when(tripService.createTrip(any(CreateTripRequest.class))).thenReturn(trip);

        mockMvc.perform(post("/api/trips")
                        .with(user(providerAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(tripId.toString()))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));
    }
}
