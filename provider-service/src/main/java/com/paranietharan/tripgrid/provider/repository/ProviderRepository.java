package com.paranietharan.tripgrid.provider.repository;

import com.paranietharan.tripgrid.provider.entity.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProviderRepository extends JpaRepository<Provider, UUID> {
    boolean existsByEmailIgnoreCase(String email);
    Optional<Provider> findByEmailIgnoreCase(String email);
}
