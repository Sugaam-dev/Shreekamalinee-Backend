package com.pmrgsolution.features.address.repository;

import com.pmrgsolution.features.address.entity.ShippingAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShippingAddressRepository extends JpaRepository<ShippingAddress, UUID> {
    List<ShippingAddress> findByUserId(UUID userId);
    Optional<ShippingAddress> findByIdAndUserId(UUID id, UUID userId);
    Optional<ShippingAddress> findByUserIdAndIsDefaultTrue(UUID userId);
}