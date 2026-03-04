package com.hola.hotel.repository;

import com.hola.hotel.entity.MarketCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MarketCodeRepository extends JpaRepository<MarketCode, Long> {

    List<MarketCode> findAllByPropertyIdOrderBySortOrderAsc(Long propertyId);

    boolean existsByPropertyIdAndMarketCode(Long propertyId, String marketCode);

    long countByPropertyId(Long propertyId);
}
