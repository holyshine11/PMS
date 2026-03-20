package com.hola.hotel.repository;

import com.hola.hotel.entity.HkConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 하우스키핑 설정 Repository
 */
public interface HkConfigRepository extends JpaRepository<HkConfig, Long> {

    Optional<HkConfig> findByPropertyId(Long propertyId);

    boolean existsByPropertyId(Long propertyId);
}
