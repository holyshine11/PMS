package com.hola.rate.repository;

import com.hola.rate.entity.RatePricing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 요금 정보 Repository
 */
public interface RatePricingRepository extends JpaRepository<RatePricing, Long> {

    List<RatePricing> findAllByRateCodeIdOrderByIdAsc(Long rateCodeId);

    @Modifying
    @Query("DELETE FROM RatePricing r WHERE r.rateCodeId = :rateCodeId")
    void deleteAllByRateCodeId(@Param("rateCodeId") Long rateCodeId);
}
