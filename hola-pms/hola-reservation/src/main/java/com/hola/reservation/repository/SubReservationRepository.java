package com.hola.reservation.repository;

import com.hola.reservation.entity.SubReservation;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
