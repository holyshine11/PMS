package com.hola.rate.repository;

import com.hola.rate.entity.RatePricingPerson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 인원별 추가 요금 Repository
 */
public interface RatePricingPersonRepository extends JpaRepository<RatePricingPerson, Long> {

    @Modifying
    @Query("DELETE FROM RatePricingPerson p WHERE p.ratePricingId = :ratePricingId")
    void deleteAllByRatePricingId(@Param("ratePricingId") Long ratePricingId);
}
