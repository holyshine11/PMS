package com.hola.reservation.repository;

import com.hola.reservation.entity.SubReservation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
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
}
