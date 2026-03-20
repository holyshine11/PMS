package com.hola.hotel.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Room Rack 개별 객실 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomRackItemResponse {

    private Long roomNumberId;
    private String roomNumber;
    private String hkStatus;       // CLEAN, DIRTY, OOO, OOS
    private String foStatus;       // VACANT, OCCUPIED
    private String statusCode;     // VC, VD, OC, OD, OOO, OOS
    private String roomTypeName;   // 투숙 중이면 객실타입
    private String guestName;      // 투숙 중이면 투숙객명
    private LocalDate checkOut;    // 투숙 중이면 체크아웃 예정일
    private Long reservationId;    // 투숙 중이면 마스터예약 ID
    private String hkMemo;

    // HK 작업 오버레이 (Phase 3)
    private String hkTaskStatus;       // HK 작업 상태 (PENDING, IN_PROGRESS, COMPLETED 등)
    private String hkAssigneeName;     // 배정된 하우스키퍼 이름
    private String hkTaskStartedAt;    // 작업 시작 시간 (HH:mm)
}
