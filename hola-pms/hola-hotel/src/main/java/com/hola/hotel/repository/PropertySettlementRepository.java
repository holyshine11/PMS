package com.hola.hotel.repository;

import com.hola.hotel.entity.PropertySettlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PropertySettlementRepository extends JpaRepository<PropertySettlement, Long> {

    List<PropertySettlement> findAllByPropertyId(Long propertyId);

    Optional<PropertySettlement> findByPropertyIdAndCountryType(Long propertyId, String countryType);
}
