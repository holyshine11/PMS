package com.hola.rate.repository;

import com.hola.rate.entity.DayUseRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Dayuse 요금 Repository
 */
public interface DayUseRateRepository extends JpaRepository<DayUseRate, Long> {

    List<DayUseRate> findByRateCodeIdAndUseYnTrueOrderBySortOrderAsc(Long rateCodeId);

    Optional<DayUseRate> findByRateCodeIdAndDurationHoursAndUseYnTrue(Long rateCodeId, Integer durationHours);
}
