package com.hola.reservation.service;

import com.hola.reservation.entity.SubReservation;
import com.hola.reservation.repository.SubReservationRepository;
import com.hola.room.repository.RoomTypeFloorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * 객실 가용성 서비스
 *
 * L1: 호수 중복 방지 - 동일 floor+roomNumber에 겹치는 기간 예약 차단
 * L2: 타입별 잔여 확인 - roomType에 등록된 총 호수 vs 예약 건수 비교
 * L3: 오버부킹 경고 - L2 초과 시 경고만 (관리자 판단)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomAvailabilityService {

    private final SubReservationRepository subReservationRepository;
    private final RoomTypeFloorRepository roomTypeFloorRepository;

    // 가용 상태 (해제됨)
    private static final List<String> RELEASED_STATUSES = Arrays.asList(
            "CHECKED_OUT", "CANCELED", "NO_SHOW");

    /**
     * L1: 특정 객실(roomNumberId)에 겹치는 기간의 예약이 있는지 확인
     * 날짜 겹침 판단: checkIn <= X < checkOut (체크아웃일 당일 미포함)
     *
     * @param roomNumberId 호수 ID
     * @param checkIn      체크인 일자
     * @param checkOut     체크아웃 일자
     * @param excludeSubId 제외할 서브예약 ID (수정 시 자기자신 제외, null이면 미적용)
     * @return 충돌 여부 (true = 충돌 있음)
     */
    public boolean hasRoomConflict(Long roomNumberId, LocalDate checkIn, LocalDate checkOut, Long excludeSubId) {
        if (roomNumberId == null) {
            return false; // 호수 미배정 상태면 충돌 없음
        }

        List<SubReservation> conflicts = subReservationRepository
                .findByRoomNumberIdAndCheckInLessThanAndCheckOutGreaterThanAndRoomReservationStatusNotIn(
                        roomNumberId, checkOut, checkIn, RELEASED_STATUSES);

        // 자기 자신 제외
        if (excludeSubId != null) {
            conflicts = conflicts.stream()
                    .filter(sub -> !sub.getId().equals(excludeSubId))
                    .toList();
        }

        if (!conflicts.isEmpty()) {
            log.warn("L1 객실 충돌 감지: roomNumberId={}, checkIn={}, checkOut={}, 충돌건수={}",
                    roomNumberId, checkIn, checkOut, conflicts.size());
        }

        return !conflicts.isEmpty();
    }

    /**
     * L2: 객실 타입별 잔여 객실 수 계산
     *
     * @param roomTypeId 객실 타입 ID
     * @param checkIn    체크인 일자
     * @param checkOut   체크아웃 일자
     * @return 잔여 객실 수 (음수이면 오버부킹 상태)
     */
    public int getAvailableRoomCount(Long roomTypeId, LocalDate checkIn, LocalDate checkOut) {
        return getAvailableRoomCount(roomTypeId, checkIn, checkOut, null);
    }

    /**
     * L2: 객실 타입별 잔여 객실 수 계산 (특정 서브예약 제외)
     *
     * @param roomTypeId    객실 타입 ID
     * @param checkIn       체크인 일자
     * @param checkOut      체크아웃 일자
     * @param excludeSubIds 제외할 서브예약 ID 목록 (수정 시 자기 자신 제외, null이면 미적용)
     * @return 잔여 객실 수 (음수이면 오버부킹 상태)
     */
    public int getAvailableRoomCount(Long roomTypeId, LocalDate checkIn, LocalDate checkOut, List<Long> excludeSubIds) {
        // 총 등록 객실 수 (rm_room_type_floor 기준)
        long totalRooms = roomTypeFloorRepository.countByRoomTypeId(roomTypeId);

        // 활성 예약 수 (해당 기간 + 해당 타입)
        long activeReservations = countActiveReservationsByRoomType(roomTypeId, checkIn, checkOut);

        // 제외 대상 차감
        if (excludeSubIds != null && !excludeSubIds.isEmpty()) {
            activeReservations = Math.max(0, activeReservations - excludeSubIds.size());
        }

        int available = (int) (totalRooms - activeReservations);
        log.debug("L2 가용성: roomTypeId={}, 총객실={}, 활성예약={}, 제외={}, 잔여={}",
                roomTypeId, totalRooms, activeReservations,
                excludeSubIds != null ? excludeSubIds.size() : 0, available);
        return available;
    }

    /**
     * L2 + L3: 오버부킹 경고 필요 여부 확인
     *
     * @return true = 잔여 없음 (오버부킹 경고 대상)
     */
    public boolean isOverbookingWarning(Long roomTypeId, LocalDate checkIn, LocalDate checkOut) {
        return getAvailableRoomCount(roomTypeId, checkIn, checkOut) <= 0;
    }

    // ─── 동시성 보호 메서드 (비관적 락) ──────────────────────────

    /**
     * L1 비관적 락: 특정 호수에 대해 FOR UPDATE 락을 걸고 충돌 검사
     * SubReservation save 직전에 호출하여 TOCTOU 레이스 컨디션 방지
     *
     * @param roomNumberId 호수 ID (null이면 검사 생략)
     * @param checkIn      체크인 일자
     * @param checkOut     체크아웃 일자
     * @param excludeSubId 제외할 서브예약 ID (수정 시, null이면 미적용)
     * @return 충돌 여부 (true = 충돌 있음)
     */
    @Transactional
    public boolean hasRoomConflictWithLock(Long roomNumberId, LocalDate checkIn, LocalDate checkOut, Long excludeSubId) {
        if (roomNumberId == null) {
            return false;
        }

        List<SubReservation> conflicts = subReservationRepository
                .findConflictsWithLock(roomNumberId, checkIn, checkOut, RELEASED_STATUSES);

        if (excludeSubId != null) {
            conflicts = conflicts.stream()
                    .filter(sub -> !sub.getId().equals(excludeSubId))
                    .toList();
        }

        if (!conflicts.isEmpty()) {
            log.warn("L1 비관적 락 충돌 감지: roomNumberId={}, checkIn={}, checkOut={}, 충돌건수={}",
                    roomNumberId, checkIn, checkOut, conflicts.size());
        }
        return !conflicts.isEmpty();
    }

    /**
     * L2 비관적 락: 객실 타입별 잔여 수를 FOR UPDATE 락으로 확인
     * SubReservation save 직전에 호출하여 동시 예약 시 오버부킹 방지
     *
     * @param roomTypeId 객실 타입 ID
     * @param checkIn    체크인 일자
     * @param checkOut   체크아웃 일자
     * @return 잔여 객실 수
     */
    @Transactional
    public int getAvailableRoomCountWithLock(Long roomTypeId, LocalDate checkIn, LocalDate checkOut) {
        long totalRooms = roomTypeFloorRepository.countByRoomTypeId(roomTypeId);

        // 비관적 락으로 활성 예약 조회 (다른 트랜잭션의 INSERT 완료 대기)
        long activeReservations = subReservationRepository
                .findActiveByRoomTypeWithLock(roomTypeId, checkIn, checkOut, RELEASED_STATUSES)
                .size();

        int available = (int) (totalRooms - activeReservations);
        log.debug("L2 비관적 락 가용성: roomTypeId={}, 총객실={}, 활성예약={}, 잔여={}",
                roomTypeId, totalRooms, activeReservations, available);
        return available;
    }

    // ─── 내부 헬퍼 ──────────────────────────

    /**
     * 해당 객실타입 + 기간에 활성 예약(서브) 수 계산
     */
    private long countActiveReservationsByRoomType(Long roomTypeId, LocalDate checkIn, LocalDate checkOut) {
        // 기간 겹침 + 해당 타입 + 활성 상태인 서브예약 수
        return subReservationRepository.countByRoomTypeIdAndCheckInLessThanAndCheckOutGreaterThanAndRoomReservationStatusNotIn(
                roomTypeId, checkOut, checkIn, RELEASED_STATUSES);
    }
}
