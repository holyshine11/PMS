package com.hola.rate.repository;

import com.hola.rate.entity.RateCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 레이트 코드 Repository
 */
public interface RateCodeRepository extends JpaRepository<RateCode, Long> {

    List<RateCode> findAllByPropertyIdOrderBySortOrderAscRateCodeAsc(Long propertyId);

    boolean existsByPropertyIdAndRateCode(Long propertyId, String rateCode);
}
