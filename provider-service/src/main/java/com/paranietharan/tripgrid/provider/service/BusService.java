package com.paranietharan.tripgrid.provider.service;

import com.paranietharan.tripgrid.provider.dto.*;
import com.paranietharan.tripgrid.provider.entity.*;
import com.paranietharan.tripgrid.provider.exception.ConflictException;
import com.paranietharan.tripgrid.provider.exception.ResourceNotFoundException;
import com.paranietharan.tripgrid.provider.repository.BusRepository;
import com.paranietharan.tripgrid.provider.repository.SeatRepository;
import com.paranietharan.tripgrid.provider.security.TenantSecurityService;
import com.paranietharan.tripgrid.provider.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class BusService {

    private static final Logger log = LoggerFactory.getLogger(BusService.class);

    private final BusRepository busRepository;
    private final SeatRepository seatRepository;
    private final TenantSecurityService tenantSecurityService;

    public BusService(
            BusRepository busRepository,
            SeatRepository seatRepository,
            TenantSecurityService tenantSecurityService) {
        this.busRepository = busRepository;
        this.seatRepository = seatRepository;
        this.tenantSecurityService = tenantSecurityService;
    }

    @Transactional
    public BusResponse createBus(CreateBusRequest request) {
        UUID providerId = tenantSecurityService.getRequiredProviderId();
        String regNumber = request.getRegistrationNumber().trim().toUpperCase();

        if (busRepository.existsByProviderIdAndRegistrationNumberIgnoreCase(providerId, regNumber)) {
            throw new ConflictException("Bus with registration number " + regNumber + " already exists for this provider");
        }

        Bus bus = new Bus();
        bus.setProviderId(providerId);
        bus.setRegistrationNumber(regNumber);
        bus.setName(request.getName().trim());
        bus.setBusType(request.getBusType());
        bus.setSeatCount(request.getSeatCount());
        bus.setStatus(BusStatus.ACTIVE);

        Bus savedBus = busRepository.save(bus);
        log.info("Created bus id: {}, reg: {} for provider: {}", savedBus.getId(), regNumber, providerId);

        // Generate physical seats
        generateSeatsForBus(savedBus.getId(), request.getSeatCount());

        return BusResponse.fromEntity(savedBus);
    }

    @Transactional(readOnly = true)
    public List<BusResponse> getAllBuses() {
        UserPrincipal principal = tenantSecurityService.getAuthenticatedPrincipal();
        if (principal.isSuperAdmin()) {
            return busRepository.findAll()
                    .stream()
                    .map(BusResponse::fromEntity)
                    .toList();
        }

        UUID providerId = tenantSecurityService.resolveProviderIdForPrincipal(principal);
        return busRepository.findByProviderId(providerId)
                .stream()
                .map(BusResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public BusResponse getBusById(UUID busId) {
        Bus bus = busRepository.findById(busId)
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found with id: " + busId));

        tenantSecurityService.verifyProviderAccess(bus.getProviderId());
        return BusResponse.fromEntity(bus);
    }

    @Transactional
    public BusResponse updateBus(UUID busId, UpdateBusRequest request) {
        Bus bus = busRepository.findById(busId)
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found with id: " + busId));

        tenantSecurityService.verifyProviderAccess(bus.getProviderId());

        bus.setName(request.getName().trim());
        bus.setBusType(request.getBusType());

        if (bus.getSeatCount() != request.getSeatCount()) {
            bus.setSeatCount(request.getSeatCount());
            // Regenerate seats
            seatRepository.deleteByBusId(busId);
            generateSeatsForBus(busId, request.getSeatCount());
        }

        bus.setUpdatedAt(Instant.now());
        Bus updated = busRepository.save(bus);
        log.info("Updated bus id: {}", updated.getId());
        return BusResponse.fromEntity(updated);
    }

    @Transactional
    public BusResponse updateBusStatus(UUID busId, UpdateBusStatusRequest request) {
        Bus bus = busRepository.findById(busId)
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found with id: " + busId));

        tenantSecurityService.verifyProviderAccess(bus.getProviderId());

        bus.setStatus(request.getStatus());
        bus.setUpdatedAt(Instant.now());
        Bus updated = busRepository.save(bus);
        log.info("Updated bus status id: {} to {}", busId, request.getStatus());
        return BusResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteBus(UUID busId) {
        Bus bus = busRepository.findById(busId)
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found with id: " + busId));

        tenantSecurityService.verifyProviderAccess(bus.getProviderId());
        busRepository.delete(bus);
        log.info("Deleted bus id: {}", busId);
    }

    public void generateSeatsForBus(UUID busId, int seatCount) {
        List<Seat> seats = new ArrayList<>();
        char[] columnLetters = {'A', 'B', 'C', 'D'};
        int currentSeatIndex = 0;
        int rowNumber = 1;

        while (currentSeatIndex < seatCount) {
            for (int col = 0; col < 4 && currentSeatIndex < seatCount; col++) {
                String seatNumber = rowNumber + String.valueOf(columnLetters[col]);
                Seat seat = new Seat(
                        UUID.randomUUID(),
                        busId,
                        seatNumber,
                        rowNumber,
                        col + 1,
                        SeatStatus.ACTIVE,
                        Instant.now()
                );
                seats.add(seat);
                currentSeatIndex++;
            }
            rowNumber++;
        }

        seatRepository.saveAll(seats);
        log.info("Generated {} seats for bus: {}", seats.size(), busId);
    }
}
