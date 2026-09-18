package com.paranietharan.tripgrid.provider.repository;

import com.paranietharan.tripgrid.provider.entity.Trip;
import com.paranietharan.tripgrid.provider.entity.TripStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TripRepository extends JpaRepository<Trip, UUID> {

    List<Trip> findByProviderId(UUID providerId);

    Optional<Trip> findByIdAndProviderId(UUID id, UUID providerId);

    List<Trip> findByBusId(UUID busId);

    @Query("""
        SELECT t FROM Trip t
        WHERE t.busId = :busId
          AND t.status NOT IN ('CANCELLED', 'COMPLETED')
          AND t.departureTime < :arrivalTime
          AND t.arrivalTime > :departureTime
    """)
    List<Trip> findOverlappingTrips(
            @Param("busId") UUID busId,
            @Param("departureTime") Instant departureTime,
            @Param("arrivalTime") Instant arrivalTime
    );

    @Query("""
        SELECT t FROM Trip t
        WHERE t.busId = :busId
          AND t.id <> :excludeTripId
          AND t.status NOT IN ('CANCELLED', 'COMPLETED')
          AND t.departureTime < :arrivalTime
          AND t.arrivalTime > :departureTime
    """)
    List<Trip> findOverlappingTripsExcludingId(
            @Param("busId") UUID busId,
            @Param("departureTime") Instant departureTime,
            @Param("arrivalTime") Instant arrivalTime,
            @Param("excludeTripId") UUID excludeTripId
    );

    @Query("""
        SELECT t FROM Trip t
        JOIN Route r ON t.routeId = r.id
        WHERE t.status = 'SCHEDULED'
          AND (:providerId IS NULL OR t.providerId = :providerId)
          AND (:origin IS NULL OR LOWER(r.origin) = LOWER(:origin))
          AND (:destination IS NULL OR LOWER(r.destination) = LOWER(:destination))
          AND (:fromTime IS NULL OR t.departureTime >= :fromTime)
          AND (:toTime IS NULL OR t.departureTime <= :toTime)
          AND (:minPrice IS NULL OR t.price >= :minPrice)
          AND (:maxPrice IS NULL OR t.price <= :maxPrice)
        ORDER BY t.departureTime ASC
    """)
    List<Trip> searchTrips(
            @Param("origin") String origin,
            @Param("destination") String destination,
            @Param("fromTime") Instant fromTime,
            @Param("toTime") Instant toTime,
            @Param("providerId") UUID providerId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice
    );
}
