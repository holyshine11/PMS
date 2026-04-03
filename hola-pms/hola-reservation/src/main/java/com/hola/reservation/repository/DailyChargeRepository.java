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

    /**
     * Dayuse 매출 합계 (취소/노쇼 제외)
     */
    @Query("SELECT COALESCE(SUM(d.total), 0) FROM DailyCharge d " +
           "WHERE d.subReservation.masterReservation.property.id = :propertyId " +
           "AND d.chargeDate = :chargeDate " +
           "AND d.subReservation.stayType = 'DAY_USE' " +
           "AND d.subReservation.roomReservationStatus NOT IN ('CANCELED', 'NO_SHOW')")
    BigDecimal sumDayUseRevenueByPropertyAndDate(@Param("propertyId") Long propertyId,
                                                  @Param("chargeDate") LocalDate chargeDate);

    // === 대시보드 벌크 집계 쿼리 (N+1 방지) ===

    /**
     * 전체 프로퍼티 매출 합계 벌크 조회 (GROUP BY propertyId)
     * 반환: Object[] = {propertyId (Long), revenue (BigDecimal)}
     */
    @Query("SELECT d.subReservation.masterReservation.property.id, COALESCE(SUM(d.total), 0) " +
           "FROM DailyCharge d " +
           "WHERE d.chargeDate = :chargeDate " +
           "AND d.subReservation.roomReservationStatus NOT IN ('CANCELED', 'NO_SHOW') " +
           "GROUP BY d.subReservation.masterReservation.property.id")
    List<Object[]> sumRevenueBulkByDate(@Param("chargeDate") LocalDate chargeDate);

    /**
     * 전체 프로퍼티 Dayuse 매출 합계 벌크 조회 (GROUP BY propertyId)
     * 반환: Object[] = {propertyId (Long), revenue (BigDecimal)}
     */
    @Query("SELECT d.subReservation.masterReservation.property.id, COALESCE(SUM(d.total), 0) " +
           "FROM DailyCharge d " +
           "WHERE d.chargeDate = :chargeDate " +
           "AND d.subReservation.stayType = 'DAY_USE' " +
           "AND d.subReservation.roomReservationStatus NOT IN ('CANCELED', 'NO_SHOW') " +
           "GROUP BY d.subReservation.masterReservation.property.id")
    List<Object[]> sumDayUseRevenueBulkByDate(@Param("chargeDate") LocalDate chargeDate);

    /**
     * 특정 프로퍼티의 날짜 범위 매출 합계 (GROUP BY chargeDate)
     * 반환: Object[] = {chargeDate (LocalDate), revenue (BigDecimal)}
     */
    @Query("SELECT d.chargeDate, COALESCE(SUM(d.total), 0) " +
           "FROM DailyCharge d " +
           "WHERE d.subReservation.masterReservation.property.id = :propertyId " +
           "AND d.chargeDate BETWEEN :startDate AND :endDate " +
           "AND d.subReservation.roomReservationStatus NOT IN ('CANCELED', 'NO_SHOW') " +
           "GROUP BY d.chargeDate")
    List<Object[]> sumRevenueByPropertyAndDateRange(@Param("propertyId") Long propertyId,
                                                     @Param("startDate") LocalDate startDate,
                                                     @Param("endDate") LocalDate endDate);
}
