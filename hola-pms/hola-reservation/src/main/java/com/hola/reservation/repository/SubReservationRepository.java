package com.hola.reservation.repository;

import com.hola.reservation.entity.SubReservation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 서브 예약 Repository
 */
public interface SubReservationRepository extends JpaRepository<SubReservation, Long> {

    List<SubReservation> findByMasterReservationId(Long masterReservationId);

    /**
     * 소프트 삭제 포함 전체 서브 예약 수 (번호 채번용)
     */
    @Query(value = "SELECT COUNT(*) FROM rsv_sub_reservation WHERE master_reservation_id = :masterId",
           nativeQuery = true)
    int countAllIncludingDeleted(@Param("masterId") Long masterId);

    /**
     * L1 객실 가용성: 특정 객실에 대해 겹치는 기간의 예약이 있는지 조회
     * (제외할 상태 목록을 지정하여 취소/노쇼 등 제외)
     */
    List<SubReservation> findByRoomNumberIdAndCheckInLessThanAndCheckOutGreaterThanAndRoomReservationStatusNotIn(
            Long roomNumberId, LocalDate checkOut, LocalDate checkIn, List<String> excludeStatuses);

    /**
     * L2 타입별 가용성: 해당 객실타입 + 겹치는 기간의 활성 예약 수
     */
    long countByRoomTypeIdAndCheckInLessThanAndCheckOutGreaterThanAndRoomReservationStatusNotIn(
            Long roomTypeId, LocalDate checkOut, LocalDate checkIn, List<String> excludeStatuses);

    /**
     * 벌크 가용성 조회: 여러 호수에 대해 겹치는 기간의 충돌 예약 한 번에 조회 (N+1 방지)
     */
    @Query("SELECT s FROM SubReservation s JOIN FETCH s.masterReservation " +
           "WHERE s.roomNumberId IN :roomNumberIds " +
           "AND s.checkIn < :checkOut AND s.checkOut > :checkIn " +
           "AND s.roomReservationStatus NOT IN :excludeStatuses")
    List<SubReservation> findConflictsByRoomNumberIds(
            @Param("roomNumberIds") List<Long> roomNumberIds,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut,
            @Param("excludeStatuses") List<String> excludeStatuses);

    /**
     * L1 비관적 락: 특정 객실+기간에 대해 FOR UPDATE 락을 걸고 충돌 예약 조회
     * 동시 예약 시 오버부킹 방지용
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SubReservation s " +
           "WHERE s.roomNumberId = :roomNumberId " +
           "AND s.checkIn < :checkOut AND s.checkOut > :checkIn " +
           "AND s.roomReservationStatus NOT IN :excludeStatuses")
    List<SubReservation> findConflictsWithLock(
            @Param("roomNumberId") Long roomNumberId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut,
            @Param("excludeStatuses") List<String> excludeStatuses);

    /**
     * L2 비관적 락: 해당 객실타입+기간의 활성 서브예약을 FOR UPDATE 락으로 조회 후 카운트
     * 동시 예약 시 타입별 오버부킹 방지용
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SubReservation s " +
           "WHERE s.roomTypeId = :roomTypeId " +
           "AND s.checkIn < :checkOut AND s.checkOut > :checkIn " +
           "AND s.roomReservationStatus NOT IN :excludeStatuses")
    List<SubReservation> findActiveByRoomTypeWithLock(
            @Param("roomTypeId") Long roomTypeId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut,
            @Param("excludeStatuses") List<String> excludeStatuses);

    // === 대시보드 집계 쿼리 ===

    /**
     * 오늘 투숙중 객실 수 (CHECK_IN 또는 INHOUSE 상태)
     */
    @Query("SELECT COUNT(s) FROM SubReservation s " +
           "WHERE s.masterReservation.property.id = :propertyId " +
           "AND s.roomReservationStatus IN ('CHECK_IN', 'INHOUSE') " +
           "AND s.checkIn <= :today AND s.checkOut > :today")
    long countSoldRooms(@Param("propertyId") Long propertyId, @Param("today") LocalDate today);

    /**
     * 오늘 도착 예정 (RESERVED 상태, checkIn = today)
     */
    @Query("SELECT COUNT(s) FROM SubReservation s " +
           "WHERE s.masterReservation.property.id = :propertyId " +
           "AND s.roomReservationStatus = 'RESERVED' " +
           "AND s.checkIn = :today")
    long countArrivals(@Param("propertyId") Long propertyId, @Param("today") LocalDate today);

    /**
     * 현재 투숙중 (CHECK_IN/INHOUSE, 체크아웃 날짜 >= 오늘)
     */
    @Query("SELECT COUNT(s) FROM SubReservation s " +
           "WHERE s.masterReservation.property.id = :propertyId " +
           "AND s.roomReservationStatus IN ('CHECK_IN', 'INHOUSE') " +
           "AND s.checkOut >= :today")
    long countInHouse(@Param("propertyId") Long propertyId, @Param("today") LocalDate today);

    /**
     * 오늘 출발 예정 (checkOut = today, CHECK_IN/INHOUSE)
     */
    @Query("SELECT COUNT(s) FROM SubReservation s " +
           "WHERE s.masterReservation.property.id = :propertyId " +
           "AND s.roomReservationStatus IN ('CHECK_IN', 'INHOUSE') " +
           "AND s.checkOut = :today")
    long countDepartures(@Param("propertyId") Long propertyId, @Param("today") LocalDate today);

    /**
     * 오늘 실제 체크인 완료
     */
    @Query("SELECT COUNT(s) FROM SubReservation s " +
           "WHERE s.masterReservation.property.id = :propertyId " +
           "AND s.roomReservationStatus IN ('CHECK_IN', 'INHOUSE', 'CHECKED_OUT') " +
           "AND s.actualCheckInTime >= :startOfDay AND s.actualCheckInTime < :endOfDay")
    long countCheckedInToday(@Param("propertyId") Long propertyId,
                             @Param("startOfDay") LocalDateTime startOfDay,
                             @Param("endOfDay") LocalDateTime endOfDay);

    /**
     * 오늘 실제 체크아웃 완료
     */
    @Query("SELECT COUNT(s) FROM SubReservation s " +
           "WHERE s.masterReservation.property.id = :propertyId " +
           "AND s.roomReservationStatus = 'CHECKED_OUT' " +
           "AND s.actualCheckOutTime >= :startOfDay AND s.actualCheckOutTime < :endOfDay")
    long countCheckedOutToday(@Param("propertyId") Long propertyId,
                              @Param("startOfDay") LocalDateTime startOfDay,
                              @Param("endOfDay") LocalDateTime endOfDay);

    /**
     * 7일 추이: 특정 날짜에 체류 중인 예약 수 (checkIn <= date AND checkOut > date)
     */
    @Query("SELECT COUNT(s) FROM SubReservation s " +
           "WHERE s.masterReservation.property.id = :propertyId " +
           "AND s.roomReservationStatus NOT IN ('CANCELED', 'NO_SHOW') " +
           "AND s.checkIn <= :date AND s.checkOut > :date")
    long countOccupiedByDate(@Param("propertyId") Long propertyId, @Param("date") LocalDate date);

    // === 프론트데스크 리스트 쿼리 ===

    /**
     * 오늘 도착 예정 리스트 (RESERVED, checkIn=today)
     */
    @Query("SELECT s FROM SubReservation s JOIN FETCH s.masterReservation m " +
           "WHERE m.property.id = :propertyId " +
           "AND s.roomReservationStatus = 'RESERVED' " +
           "AND s.checkIn = :today " +
           "ORDER BY m.guestNameKo ASC")
    List<SubReservation> findArrivals(@Param("propertyId") Long propertyId, @Param("today") LocalDate today);

    /**
     * 현재 투숙중 리스트 (CHECK_IN/INHOUSE, 체크아웃 날짜 >= 오늘)
     */
    @Query("SELECT s FROM SubReservation s JOIN FETCH s.masterReservation m " +
           "WHERE m.property.id = :propertyId " +
           "AND s.roomReservationStatus IN ('CHECK_IN', 'INHOUSE') " +
           "AND s.checkOut >= :today " +
           "ORDER BY s.checkOut ASC, m.guestNameKo ASC")
    List<SubReservation> findInHouse(@Param("propertyId") Long propertyId, @Param("today") LocalDate today);

    /**
     * 오늘 출발 예정 리스트 (checkOut=today, CHECK_IN/INHOUSE)
     */
    @Query("SELECT s FROM SubReservation s JOIN FETCH s.masterReservation m " +
           "WHERE m.property.id = :propertyId " +
           "AND s.roomReservationStatus IN ('CHECK_IN', 'INHOUSE') " +
           "AND s.checkOut = :today " +
           "ORDER BY m.guestNameKo ASC")
    List<SubReservation> findDepartures(@Param("propertyId") Long propertyId, @Param("today") LocalDate today);
}
