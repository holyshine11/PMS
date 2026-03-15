package com.hola.reservation.booking.repository;

import com.hola.reservation.booking.entity.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

/**
 * 환율 Repository
 */
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    /**
     * 특정 통화의 최신 환율 조회 (적용일 기준 가장 가까운 과거 환율)
     */
    @Query("SELECT e FROM ExchangeRate e " +
           "WHERE e.currencyCode = :currency AND e.rateDate <= :date AND e.active = true " +
           "ORDER BY e.rateDate DESC LIMIT 1")
    Optional<ExchangeRate> findLatestRate(@Param("currency") String currency,
                                          @Param("date") LocalDate date);
}
