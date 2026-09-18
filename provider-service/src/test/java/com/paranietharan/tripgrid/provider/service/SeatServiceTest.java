package com.paranietharan.tripgrid.provider.service;

import com.paranietharan.tripgrid.provider.dto.CreateSeatRequest;
import com.paranietharan.tripgrid.provider.dto.SeatResponse;
import com.paranietharan.tripgrid.provider.dto.UpdateSeatStatusRequest;
import com.paranietharan.tripgrid.provider.entity.Bus;
import com.paranietharan.tripgrid.provider.entity.BusStatus;
import com.paranietharan.tripgrid.provider.entity.BusType;
import com.paranietharan.tripgrid.provider.entity.Seat;
import com.paranietharan.tripgrid.provider.entity.SeatStatus;
import com.paranietharan.tripgrid.provider.exception.ConflictException;
import com.paranietharan.tripgrid.provider.repository.BusRepository;
import com.paranietharan.tripgrid.provider.repository.SeatRepository;
import com.paranietharan.tripgrid.provider.security.TenantSecurityService;
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
class SeatServiceTest {

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private BusRepository busRepository;

    @Mock
    private TenantSecurityService tenantSecurityService;

    @InjectMocks
    private SeatService seatService;

    private UUID providerId;
    private UUID busId;
    private UUID seatId;
    private Bus bus;
    private Seat seat;

    @BeforeEach
    void setUp() {
        providerId = UUID.randomUUID();
        busId = UUID.randomUUID();
        seatId = UUID.randomUUID();
        bus = new Bus(busId, providerId, "WP-1234", "Express", BusType.LUXURY, 40, BusStatus.ACTIVE, Instant.now(), Instant.now());
        seat = new Seat(seatId, busId, "1A", 1, 1, SeatStatus.ACTIVE, Instant.now());
    }

    @Test
    @DisplayName("Should get seats for a bus")
    void shouldGetSeatsByBusId() {
        when(busRepository.findById(busId)).thenReturn(Optional.of(bus));
        when(seatRepository.findByBusIdOrderByRowNumberAscColumnNumberAsc(busId)).thenReturn(List.of(seat));

        List<SeatResponse> seats = seatService.getSeatsByBusId(busId);

        assertThat(seats).hasSize(1);
        assertThat(seats.get(0).getSeatNumber()).isEqualTo("1A");
        verify(tenantSecurityService).verifyProviderAccess(providerId);
    }

    @Test
    @DisplayName("Should create seat on bus")
    void shouldCreateSeat() {
        CreateSeatRequest request = new CreateSeatRequest("1B", 1, 2);

        when(busRepository.findById(busId)).thenReturn(Optional.of(bus));
        when(seatRepository.existsByBusIdAndSeatNumberIgnoreCase(busId, "1B")).thenReturn(false);
        Seat newSeat = new Seat(UUID.randomUUID(), busId, "1B", 1, 2, SeatStatus.ACTIVE, Instant.now());
        when(seatRepository.save(any(Seat.class))).thenReturn(newSeat);

        SeatResponse response = seatService.createSeat(busId, request);

        assertThat(response).isNotNull();
        assertThat(response.getSeatNumber()).isEqualTo("1B");
    }

    @Test
    @DisplayName("Should throw ConflictException on duplicate seat number on the same bus")
    void shouldThrowConflictWhenSeatExists() {
        CreateSeatRequest request = new CreateSeatRequest("1A", 1, 1);

        when(busRepository.findById(busId)).thenReturn(Optional.of(bus));
        when(seatRepository.existsByBusIdAndSeatNumberIgnoreCase(busId, "1A")).thenReturn(true);

        assertThatThrownBy(() -> seatService.createSeat(busId, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Should update seat status to DISABLED")
    void shouldUpdateSeatStatus() {
        when(busRepository.findById(busId)).thenReturn(Optional.of(bus));
        when(seatRepository.findByIdAndBusId(seatId, busId)).thenReturn(Optional.of(seat));
        when(seatRepository.save(any(Seat.class))).thenReturn(seat);

        UpdateSeatStatusRequest request = new UpdateSeatStatusRequest(SeatStatus.DISABLED);
        SeatResponse response = seatService.updateSeatStatus(busId, seatId, request);

        assertThat(response).isNotNull();
        assertThat(seat.getStatus()).isEqualTo(SeatStatus.DISABLED);
    }
}
