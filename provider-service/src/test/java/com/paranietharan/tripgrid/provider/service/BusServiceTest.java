package com.paranietharan.tripgrid.provider.service;

import com.paranietharan.tripgrid.provider.dto.BusResponse;
import com.paranietharan.tripgrid.provider.dto.CreateBusRequest;
import com.paranietharan.tripgrid.provider.dto.UpdateBusRequest;
import com.paranietharan.tripgrid.provider.dto.UpdateBusStatusRequest;
import com.paranietharan.tripgrid.provider.entity.Bus;
import com.paranietharan.tripgrid.provider.entity.BusStatus;
import com.paranietharan.tripgrid.provider.entity.BusType;
import com.paranietharan.tripgrid.provider.entity.Role;
import com.paranietharan.tripgrid.provider.exception.ConflictException;
import com.paranietharan.tripgrid.provider.exception.ForbiddenException;
import com.paranietharan.tripgrid.provider.exception.ResourceNotFoundException;
import com.paranietharan.tripgrid.provider.repository.BusRepository;
import com.paranietharan.tripgrid.provider.repository.SeatRepository;
import com.paranietharan.tripgrid.provider.security.TenantSecurityService;
import com.paranietharan.tripgrid.provider.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BusServiceTest {

    @Mock
    private BusRepository busRepository;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private TenantSecurityService tenantSecurityService;

    @InjectMocks
    private BusService busService;

    private UUID providerId;
    private UUID busId;
    private Bus sampleBus;

    @BeforeEach
    void setUp() {
        providerId = UUID.randomUUID();
        busId = UUID.randomUUID();
        sampleBus = new Bus(
                busId,
                providerId,
                "WP-1234",
                "Super Express",
                BusType.LUXURY,
                40,
                BusStatus.ACTIVE,
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    @DisplayName("Should create bus and generate 40 seats automatically")
    void shouldCreateBusAndGenerateSeats() {
        CreateBusRequest request = new CreateBusRequest("WP-1234", "Super Express", BusType.LUXURY, 40);

        when(tenantSecurityService.getRequiredProviderId()).thenReturn(providerId);
        when(busRepository.existsByProviderIdAndRegistrationNumberIgnoreCase(providerId, "WP-1234")).thenReturn(false);
        when(busRepository.save(any(Bus.class))).thenReturn(sampleBus);

        BusResponse response = busService.createBus(request);

        assertThat(response.getId()).isEqualTo(busId);
        assertThat(response.getSeatCount()).isEqualTo(40);
        assertThat(response.getRegistrationNumber()).isEqualTo("WP-1234");

        verify(seatRepository).saveAll(argThat(seats -> {
            List<?> list = (List<?>) seats;
            return list.size() == 40;
        }));
    }

    @Test
    @DisplayName("Should throw ConflictException on duplicate bus registration number in same provider")
    void shouldThrowConflictOnDuplicateRegNumber() {
        CreateBusRequest request = new CreateBusRequest("WP-1234", "Super Express", BusType.LUXURY, 40);

        when(tenantSecurityService.getRequiredProviderId()).thenReturn(providerId);
        when(busRepository.existsByProviderIdAndRegistrationNumberIgnoreCase(providerId, "WP-1234")).thenReturn(true);

        assertThatThrownBy(() -> busService.createBus(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Should throw ForbiddenException when accessing another provider's bus")
    void shouldThrowForbiddenOnCrossTenantBusAccess() {
        when(busRepository.findById(busId)).thenReturn(Optional.of(sampleBus));
        doThrow(new ForbiddenException("Access denied")).when(tenantSecurityService).verifyProviderAccess(providerId);

        assertThatThrownBy(() -> busService.getBusById(busId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Should update bus and regenerate seats when seat count changes")
    void shouldUpdateBusAndRegenerateSeats() {
        UpdateBusRequest request = new UpdateBusRequest("Super Express Updated", BusType.VIP, 44);

        when(busRepository.findById(busId)).thenReturn(Optional.of(sampleBus));
        when(busRepository.save(any(Bus.class))).thenReturn(sampleBus);

        BusResponse response = busService.updateBus(busId, request);

        assertThat(response).isNotNull();
        verify(seatRepository).deleteByBusId(busId);
        verify(seatRepository).saveAll(argThat(seats -> ((List<?>) seats).size() == 44));
    }
}
