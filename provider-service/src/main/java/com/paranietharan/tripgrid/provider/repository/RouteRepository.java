package com.paranietharan.tripgrid.provider.repository;

import com.paranietharan.tripgrid.provider.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RouteRepository extends JpaRepository<Route, UUID> {
    List<Route> findByProviderId(UUID providerId);
    Optional<Route> findByIdAndProviderId(UUID id, UUID providerId);
    boolean existsByProviderIdAndOriginIgnoreCaseAndDestinationIgnoreCase(UUID providerId, String origin, String destination);
    boolean existsByProviderIdAndOriginIgnoreCaseAndDestinationIgnoreCaseAndIdNot(UUID providerId, String origin, String destination, UUID id);
}
