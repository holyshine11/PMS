package com.hola.reservation.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.common.security.AccessControlService;
import com.hola.reservation.dto.request.*;
import com.hola.reservation.dto.response.*;
import com.hola.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 예약 관리 REST API 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/properties/{propertyId}/reservations")
@RequiredArgsConstructor
public class ReservationApiController {

    private final ReservationService reservationService;
    private final AccessControlService accessControlService;

    /** 캘린더뷰: 기간 내 예약 날짜별 그룹핑 조회 */
    @GetMapping("/calendar")
    public HolaResponse<Map<String, List<ReservationCalendarResponse>>> getCalendarData(
            @PathVariable Long propertyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(reservationService.getCalendarData(propertyId, startDate, endDate, status, keyword));
    }

    /** 예약 리스트 조회 */
    @GetMapping
    public HolaResponse<List<ReservationListResponse>> getList(
            @PathVariable Long propertyId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInTo,
            @RequestParam(required = false) String keyword) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(reservationService.getList(propertyId, status, checkInFrom, checkInTo, keyword));
    }

    /** 예약 상세 조회 */
    @GetMapping("/{id}")
    public HolaResponse<ReservationDetailResponse> getById(@PathVariable Long propertyId,
                                                            @PathVariable Long id) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(reservationService.getById(id));
    }

    /** 예약 등록 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HolaResponse<ReservationDetailResponse> create(@PathVariable Long propertyId,
                                                           @RequestBody ReservationCreateRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(reservationService.create(propertyId, request));
    }

    /** 예약 수정 */
    @PutMapping("/{id}")
    public HolaResponse<ReservationDetailResponse> update(@PathVariable Long propertyId,
                                                           @PathVariable Long id,
                                                           @RequestBody ReservationUpdateRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(reservationService.update(id, request));
    }

    /** 예약 취소 */
    @DeleteMapping("/{id}")
    public HolaResponse<Void> cancel(@PathVariable Long propertyId,
                                      @PathVariable Long id) {
        accessControlService.validatePropertyAccess(propertyId);
        reservationService.cancel(id);
        return HolaResponse.success();
    }

    /** 예약 상태 변경 */
    @PutMapping("/{id}/status")
    public HolaResponse<Void> changeStatus(@PathVariable Long propertyId,
                                            @PathVariable Long id,
                                            @RequestBody ReservationStatusRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        reservationService.changeStatus(id, request);
        return HolaResponse.success();
    }

    /** 서브 예약(객실 레그) 추가 */
    @PostMapping("/{id}/legs")
    @ResponseStatus(HttpStatus.CREATED)
    public HolaResponse<SubReservationResponse> addLeg(@PathVariable Long propertyId,
                                                        @PathVariable Long id,
                                                        @RequestBody SubReservationRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(reservationService.addLeg(id, request));
    }

    /** 서브 예약 수정 */
    @PutMapping("/{id}/legs/{legId}")
    public HolaResponse<SubReservationResponse> updateLeg(@PathVariable Long propertyId,
                                                           @PathVariable Long id,
                                                           @PathVariable Long legId,
                                                           @RequestBody SubReservationRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(reservationService.updateLeg(id, legId, request));
    }

    /** 서브 예약 삭제 */
    @DeleteMapping("/{id}/legs/{legId}")
    public HolaResponse<Void> deleteLeg(@PathVariable Long propertyId,
                                         @PathVariable Long id,
                                         @PathVariable Long legId) {
        accessControlService.validatePropertyAccess(propertyId);
        reservationService.deleteLeg(id, legId);
        return HolaResponse.success();
    }

    /** 객실 가용성 조회 */
    @GetMapping("/availability")
    public HolaResponse<Map<String, Object>> checkAvailability(
            @PathVariable Long propertyId,
            @RequestParam Long roomTypeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {
        accessControlService.validatePropertyAccess(propertyId);
        int available = reservationService.checkAvailability(propertyId, roomTypeId, checkIn, checkOut);
        return HolaResponse.success(Map.of(
                "roomTypeId", roomTypeId,
                "checkIn", checkIn.toString(),
                "checkOut", checkOut.toString(),
                "availableCount", available,
                "overbooking", available < 0
        ));
    }

    /** 예약 메모 조회 */
    @GetMapping("/{id}/memos")
    public HolaResponse<List<ReservationMemoResponse>> getMemos(@PathVariable Long propertyId,
                                                                 @PathVariable Long id) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(reservationService.getMemos(id));
    }

    /** 예약 메모 등록 */
    @PostMapping("/{id}/memos")
    @ResponseStatus(HttpStatus.CREATED)
    public HolaResponse<ReservationMemoResponse> addMemo(@PathVariable Long propertyId,
                                                          @PathVariable Long id,
                                                          @RequestBody Map<String, String> body) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(reservationService.addMemo(id, body.get("content")));
    }

    /** 예치금 등록 */
    @PostMapping("/{id}/deposit")
    @ResponseStatus(HttpStatus.CREATED)
    public HolaResponse<ReservationDepositResponse> addDeposit(@PathVariable Long propertyId,
                                                                @PathVariable Long id,
                                                                @RequestBody ReservationDepositRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(reservationService.addDeposit(id, request));
    }

    /** 예치금 수정 */
    @PutMapping("/{id}/deposit/{depositId}")
    public HolaResponse<ReservationDepositResponse> updateDeposit(@PathVariable Long propertyId,
                                                                   @PathVariable Long id,
                                                                   @PathVariable Long depositId,
                                                                   @RequestBody ReservationDepositRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(reservationService.updateDeposit(id, depositId, request));
    }
}
