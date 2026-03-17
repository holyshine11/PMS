package com.hola.reservation.repository;

import com.hola.reservation.entity.DailyCharge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 일별 요금 Repository
 */
public interface DailyChargeRepository extends JpaRepository<DailyCharge, Long> {

    List<DailyCharge> findBySubReservationId(Long subReservationId);

    @Modifying
    @Query("DELETE FROM DailyCharge d WHERE d.subReservation.id = :subReservationId")
    void deleteAllBySubReservationId(@Param("subReservationId") Long subReservationId);

    /**
     * 특정 프로퍼티, 특정 날짜의 매출 합계 (취소/노쇼 제외)
     */
    @Query("SELECT COALESCE(SUM(d.total), 0) FROM DailyCharge d " +
           "WHERE d.subReservation.masterReservation.property.id = :propertyId " +
           "AND d.chargeDate = :chargeDate " +
           "AND d.subReservation.roomReservationStatus NOT IN ('CANCELED', 'NO_SHOW')")
    BigDecimal sumRevenueByPropertyAndDate(@Param("propertyId") Long propertyId,
                                           @Param("chargeDate") LocalDate chargeDate);
}
