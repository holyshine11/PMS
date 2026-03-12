package com.hola.reservation.repository;

import com.hola.reservation.entity.MasterReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 마스터 예약 Repository
 */
public interface MasterReservationRepository extends JpaRepository<MasterReservation, Long> {

    List<MasterReservation> findByPropertyIdAndReservationStatus(Long propertyId, String reservationStatus);

    Optional<MasterReservation> findByMasterReservationNo(String masterReservationNo);

    Optional<MasterReservation> findByConfirmationNo(String confirmationNo);

    boolean existsByConfirmationNo(String confirmationNo);

    List<MasterReservation> findByPropertyIdOrderByReservationDateDesc(Long propertyId);

    /**
     * 예약 리스트: 날짜 범위 + 상태 필터 (DB 레벨)
     * 날짜/상태 조건은 모두 선택적 (null이면 필터 안 함)
     */
    @Query("SELECT m FROM MasterReservation m " +
           "WHERE m.property.id = :propertyId " +
           "AND (:status IS NULL OR m.reservationStatus = :status) " +
           "AND (:checkInFrom IS NULL OR m.masterCheckIn >= :checkInFrom) " +
           "AND (:checkInTo IS NULL OR m.masterCheckIn <= :checkInTo) " +
           "ORDER BY m.reservationDate DESC")
    List<MasterReservation> findByPropertyIdWithFilters(
            @Param("propertyId") Long propertyId,
            @Param("status") String status,
            @Param("checkInFrom") LocalDate checkInFrom,
            @Param("checkInTo") LocalDate checkInTo);

    /**
     * 캘린더뷰: 기간 내 체류 기간이 겹치는 예약 조회
     * (masterCheckIn < endDate AND masterCheckOut > startDate)
     */
    @Query("SELECT m FROM MasterReservation m " +
           "WHERE m.property.id = :propertyId " +
           "AND m.masterCheckIn < :endDate " +
           "AND m.masterCheckOut > :startDate " +
           "ORDER BY m.masterCheckIn ASC, m.guestNameKo ASC")
    List<MasterReservation> findByPropertyIdAndDateRange(
            @Param("propertyId") Long propertyId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
