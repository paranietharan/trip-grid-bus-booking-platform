package com.paranietharan.tripgrid.provider.repository;

import com.paranietharan.tripgrid.provider.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SeatRepository extends JpaRepository<Seat, UUID> {
    List<Seat> findByBusIdOrderByRowNumberAscColumnNumberAsc(UUID busId);
    Optional<Seat> findByIdAndBusId(UUID id, UUID busId);
    boolean existsByBusIdAndSeatNumberIgnoreCase(UUID busId, String seatNumber);
    void deleteByBusId(UUID busId);
}
