package com.paranietharan.tripgrid.provider.repository;

import com.paranietharan.tripgrid.provider.entity.Bus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BusRepository extends JpaRepository<Bus, UUID> {
    List<Bus> findByProviderId(UUID providerId);
    Optional<Bus> findByIdAndProviderId(UUID id, UUID providerId);
    boolean existsByProviderIdAndRegistrationNumberIgnoreCase(UUID providerId, String registrationNumber);
    boolean existsByProviderIdAndRegistrationNumberIgnoreCaseAndIdNot(UUID providerId, String registrationNumber, UUID id);
}
