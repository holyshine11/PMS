package com.hola.reservation.repository;

import com.hola.reservation.entity.MasterReservation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 마스터 예약 Repository
 */
public interface MasterReservationRepository extends JpaRepository<MasterReservation, Long>,
        JpaSpecificationExecutor<MasterReservation> {

    List<MasterReservation> findByPropertyIdAndReservationStatus(Long propertyId, String reservationStatus);

    Optional<MasterReservation> findByMasterReservationNo(String masterReservationNo);

    Optional<MasterReservation> findByConfirmationNo(String confirmationNo);

    boolean existsByConfirmationNo(String confirmationNo);

    List<MasterReservation> findByPropertyIdOrderByReservationDateDesc(Long propertyId);

    /**
     * 이메일 + 성(lastName) 기반 예약 검색 (부킹엔진 내 예약 조회용)
     */
    @Query("SELECT m FROM MasterReservation m " +
           "JOIN FETCH m.property p " +
           "WHERE LOWER(m.email) = LOWER(:email) " +
           "AND LOWER(m.guestLastNameEn) = LOWER(:lastName) " +
           "ORDER BY m.reservationDate DESC")
    List<MasterReservation> findByEmailAndLastName(
            @Param("email") String email,
            @Param("lastName") String lastName);

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

    /**
     * 마스터 예약 + 서브 예약 즉시 로딩 (상세 조회용, N+1 방지)
     * DISTINCT로 JOIN에 의한 중복 행 방지
     */
    @Query("SELECT DISTINCT m FROM MasterReservation m " +
           "LEFT JOIN FETCH m.subReservations " +
           "WHERE m.id = :id")
    Optional<MasterReservation> findByIdWithSubReservations(@Param("id") Long id);

    /**
     * 캘린더/타임라인뷰: 기간 내 예약 + 서브 예약 즉시 로딩 (N+1 방지)
     * @EntityGraph 사용으로 서브쿼리 방식 fetch (카르테시안 곱 방지)
     */
    @EntityGraph(attributePaths = {"subReservations"})
    @Query("SELECT m FROM MasterReservation m " +
           "WHERE m.property.id = :propertyId " +
           "AND m.masterCheckIn < :endDate " +
           "AND m.masterCheckOut > :startDate " +
           "ORDER BY m.masterCheckIn ASC, m.guestNameKo ASC")
    List<MasterReservation> findByPropertyIdAndDateRangeWithSubs(
            @Param("propertyId") Long propertyId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
