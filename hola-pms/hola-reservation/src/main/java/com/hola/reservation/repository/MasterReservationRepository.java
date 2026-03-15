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
}
