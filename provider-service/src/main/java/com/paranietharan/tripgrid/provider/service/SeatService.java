package com.paranietharan.tripgrid.provider.service;

import com.paranietharan.tripgrid.provider.dto.CreateSeatRequest;
import com.paranietharan.tripgrid.provider.dto.SeatResponse;
import com.paranietharan.tripgrid.provider.dto.UpdateSeatRequest;
import com.paranietharan.tripgrid.provider.dto.UpdateSeatStatusRequest;
import com.paranietharan.tripgrid.provider.entity.Bus;
import com.paranietharan.tripgrid.provider.entity.Seat;
import com.paranietharan.tripgrid.provider.entity.SeatStatus;
import com.paranietharan.tripgrid.provider.exception.ConflictException;
import com.paranietharan.tripgrid.provider.exception.ResourceNotFoundException;
import com.paranietharan.tripgrid.provider.repository.BusRepository;
import com.paranietharan.tripgrid.provider.repository.SeatRepository;
import com.paranietharan.tripgrid.provider.security.TenantSecurityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class SeatService {

    private static final Logger log = LoggerFactory.getLogger(SeatService.class);

    private final SeatRepository seatRepository;
    private final BusRepository busRepository;
    private final TenantSecurityService tenantSecurityService;

    public SeatService(
            SeatRepository seatRepository,
            BusRepository busRepository,
            TenantSecurityService tenantSecurityService) {
        this.seatRepository = seatRepository;
        this.busRepository = busRepository;
        this.tenantSecurityService = tenantSecurityService;
    }

    @Transactional(readOnly = true)
    public List<SeatResponse> getSeatsByBusId(UUID busId) {
        Bus bus = busRepository.findById(busId)
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found with id: " + busId));

        tenantSecurityService.verifyProviderAccess(bus.getProviderId());

        return seatRepository.findByBusIdOrderByRowNumberAscColumnNumberAsc(busId)
                .stream()
                .map(SeatResponse::fromEntity)
                .toList();
    }

    @Transactional
    public SeatResponse createSeat(UUID busId, CreateSeatRequest request) {
        Bus bus = busRepository.findById(busId)
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found with id: " + busId));

        tenantSecurityService.verifyProviderAccess(bus.getProviderId());

        String seatNumber = request.getSeatNumber().trim().toUpperCase();
        if (seatRepository.existsByBusIdAndSeatNumberIgnoreCase(busId, seatNumber)) {
            throw new ConflictException("Seat " + seatNumber + " already exists on bus " + busId);
        }

        Seat seat = new Seat();
        seat.setBusId(busId);
        seat.setSeatNumber(seatNumber);
        seat.setRowNumber(request.getRowNumber());
        seat.setColumnNumber(request.getColumnNumber());
        seat.setStatus(SeatStatus.ACTIVE);

        Seat saved = seatRepository.save(seat);
        log.info("Created seat: {} on bus: {}", seatNumber, busId);
        return SeatResponse.fromEntity(saved);
    }

    @Transactional
    public SeatResponse updateSeat(UUID busId, UUID seatId, UpdateSeatRequest request) {
        Bus bus = busRepository.findById(busId)
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found with id: " + busId));

        tenantSecurityService.verifyProviderAccess(bus.getProviderId());

        Seat seat = seatRepository.findByIdAndBusId(seatId, busId)
                .orElseThrow(() -> new ResourceNotFoundException("Seat not found with id: " + seatId + " on bus: " + busId));

        String seatNumber = request.getSeatNumber().trim().toUpperCase();
        if (!seat.getSeatNumber().equalsIgnoreCase(seatNumber) &&
                seatRepository.existsByBusIdAndSeatNumberIgnoreCase(busId, seatNumber)) {
            throw new ConflictException("Seat " + seatNumber + " already exists on bus " + busId);
        }

        seat.setSeatNumber(seatNumber);
        seat.setRowNumber(request.getRowNumber());
        seat.setColumnNumber(request.getColumnNumber());

        Seat updated = seatRepository.save(seat);
        log.info("Updated seat: {} on bus: {}", seatId, busId);
        return SeatResponse.fromEntity(updated);
    }

    @Transactional
    public SeatResponse updateSeatStatus(UUID busId, UUID seatId, UpdateSeatStatusRequest request) {
        Bus bus = busRepository.findById(busId)
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found with id: " + busId));

        tenantSecurityService.verifyProviderAccess(bus.getProviderId());

        Seat seat = seatRepository.findByIdAndBusId(seatId, busId)
                .orElseThrow(() -> new ResourceNotFoundException("Seat not found with id: " + seatId + " on bus: " + busId));

        seat.setStatus(request.getStatus());

        Seat updated = seatRepository.save(seat);
        log.info("Updated seat status: {} on bus: {} to {}", seatId, busId, request.getStatus());
        return SeatResponse.fromEntity(updated);
    }
}
