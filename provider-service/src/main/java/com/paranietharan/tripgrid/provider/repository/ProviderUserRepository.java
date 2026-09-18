package com.paranietharan.tripgrid.provider.repository;

import com.paranietharan.tripgrid.provider.entity.ProviderUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProviderUserRepository extends JpaRepository<ProviderUser, UUID> {
    List<ProviderUser> findByProviderId(UUID providerId);
    Optional<ProviderUser> findByProviderIdAndUserId(UUID providerId, UUID userId);
    List<ProviderUser> findByUserId(UUID userId);
    boolean existsByProviderIdAndUserId(UUID providerId, UUID userId);
    void deleteByProviderIdAndUserId(UUID providerId, UUID userId);
}
