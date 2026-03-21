package com.hola.reservation.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.common.security.AccessControlService;
import com.hola.reservation.dto.request.*;
import com.hola.reservation.dto.response.*;
import com.hola.reservation.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@Tag(name = "예약 관리", description = "예약 CRUD, 상태변경, 서브예약, 메모, 예치금, 유료서비스 API")
@RestController
@RequestMapping("/api/v1/properties/{propertyId}/reservations")
@RequiredArgsConstructor
public class ReservationApiController {

    private final ReservationService reservationService;
    private final AccessControlService accessControlService;

    /** 캘린더뷰: 기간 내 예약 날짜별 그룹핑 조회 */
    @Operation(summary = "캘린더뷰 데이터 조회", description = "기간 내 예약 날짜별 그룹핑 조회")
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
    @Operation(summary = "예약 목록 조회", description = "예약 리스트 (상태/체크인날짜/키워드 필터)")
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
    @Operation(summary = "예약 상세 조회", description = "예약 ID로 마스터/서브 예약 상세 정보 조회")
    @GetMapping("/{id}")
    public HolaResponse<ReservationDetailResponse> getById(@PathVariable Long propertyId,
                                                            @PathVariable Long id) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(reservationService.getById(id, propertyId));
    }

    /** 예약 등록 */
    @Operation(summary = "예약 등록", description = "새 예약 생성 (마스터 + 서브 예약)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HolaResponse<ReservationDetailResponse> create(@PathVariable Long propertyId,
                                                           @Valid @RequestBody ReservationCreateRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(reservationService.create(propertyId, request));
    }

    /** 예약 수정 */
    @Operation(summary = "예약 수정", description = "예약 기본정보 수정")
    @PutMapping("/{id}")
    public HolaResponse<ReservationDetailResponse> update(@PathVariable Long propertyId,
                                                           @PathVariable Long id,
                                                           @Valid @RequestBody ReservationUpdateRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(reservationService.update(id, propertyId, request));
    }

    /** 예약 취소/노쇼 수수료 미리보기 */
    @Operation(summary = "취소/노쇼 수수료 미리보기", description = "예약 취소 또는 노쇼 시 수수료 미리보기")
    @GetMapping("/{id}/cancel-preview")
    public HolaResponse<AdminCancelPreviewResponse> getCancelPreview(
            @PathVariable Long propertyId,
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "false") boolean noShow) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(reservationService.getCancelPreview(id, propertyId, noShow));
    }

    /** 예약 취소 */
    @Operation(summary = "예약 취소", description = "예약 취소 처리")
    @DeleteMapping("/{id}")
    public HolaResponse<Void> cancel(@PathVariable Long propertyId,
                                      @PathVariable Long id) {
        accessControlService.validatePropertyAccess(propertyId);
        reservationService.cancel(id, propertyId);
        return HolaResponse.success();
    }

    /** 예약 삭제 (SUPER_ADMIN 전용, CHECKED_OUT 상태만) */
    @Operation(summary = "예약 삭제", description = "예약 물리 삭제 (SUPER_ADMIN, CHECKED_OUT만)")
    @DeleteMapping("/{id}/delete")
    public HolaResponse<Void> deleteReservation(@PathVariable Long propertyId,
                                                 @PathVariable Long id) {
        accessControlService.validatePropertyAccess(propertyId);
        reservationService.deleteReservation(id, propertyId);
        return HolaResponse.success();
    }

    /** 예약 상태 변경 */
    @Operation(summary = "예약 상태 변경", description = "CHECK_IN, INHOUSE, CHECKED_OUT 등 상태 전환")
    @PutMapping("/{id}/status")
    public HolaResponse<Void> changeStatus(@PathVariable Long propertyId,
                                            @PathVariable Long id,
                                            @Valid @RequestBody ReservationStatusRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        reservationService.changeStatus(id, propertyId, request);
        return HolaResponse.success();
    }

    /** 서브 예약(객실 레그) 추가 */
    @Operation(summary = "서브 예약 추가", description = "마스터 예약에 서브 예약(객실 레그) 추가")
    @PostMapping("/{id}/legs")
    @ResponseStatus(HttpStatus.CREATED)
    public HolaResponse<SubReservationResponse> addLeg(@PathVariable Long propertyId,
                                                        @PathVariable Long id,
                                                        @Valid @RequestBody SubReservationRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(reservationService.addLeg(id, propertyId, request));
    }

    /** 서브 예약 수정 */
    @Operation(summary = "서브 예약 수정", description = "서브 예약 정보 수정")
    @PutMapping("/{id}/legs/{legId}")
    public HolaResponse<SubReservationResponse> updateLeg(@PathVariable Long propertyId,
                                                           @PathVariable Long id,
                                                           @PathVariable Long legId,
                                                           @Valid @RequestBody SubReservationRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(reservationService.updateLeg(id, propertyId, legId, request));
    }

    /** 서브 예약 삭제 */
    @Operation(summary = "서브 예약 삭제", description = "서브 예약 삭제")
    @DeleteMapping("/{id}/legs/{legId}")
    public HolaResponse<Void> deleteLeg(@PathVariable Long propertyId,
                                         @PathVariable Long id,
                                         @PathVariable Long legId) {
        accessControlService.validatePropertyAccess(propertyId);
        reservationService.deleteLeg(id, propertyId, legId);
        return HolaResponse.success();
    }

    /** 객실 가용성 조회 */
    @Operation(summary = "객실 가용성 조회", description = "객실타입/기간별 가용 객실 수 확인")
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
    @Operation(summary = "예약 메모 조회", description = "예약에 등록된 메모 목록")
    @GetMapping("/{id}/memos")
    public HolaResponse<List<ReservationMemoResponse>> getMemos(@PathVariable Long propertyId,
                                                                 @PathVariable Long id) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(reservationService.getMemos(id, propertyId));
    }

    /** 예약 메모 등록 */
    @Operation(summary = "예약 메모 등록", description = "예약에 메모 추가")
    @PostMapping("/{id}/memos")
    @ResponseStatus(HttpStatus.CREATED)
    public HolaResponse<ReservationMemoResponse> addMemo(@PathVariable Long propertyId,
                                                          @PathVariable Long id,
                                                          @RequestBody Map<String, String> body) {
        accessControlService.validatePropertyAccess(propertyId);
        String content = body.get("content");
        if (content == null || content.isBlank()) {
            throw new HolaException(ErrorCode.INVALID_INPUT);
        }
        return HolaResponse.success(reservationService.addMemo(id, propertyId, content));
    }

    /** 예치금 등록 */
    @Operation(summary = "예치금 등록", description = "예약에 예치금 추가")
    @PostMapping("/{id}/deposit")
    @ResponseStatus(HttpStatus.CREATED)
    public HolaResponse<ReservationDepositResponse> addDeposit(@PathVariable Long propertyId,
                                                                @PathVariable Long id,
                                                                @Valid @RequestBody ReservationDepositRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(reservationService.addDeposit(id, propertyId, request));
    }

    /** 유료 서비스 추가 */
    @Operation(summary = "유료 서비스 추가", description = "서브 예약에 유료 서비스 추가")
    @PostMapping("/{id}/legs/{legId}/services")
    @ResponseStatus(HttpStatus.CREATED)
    public HolaResponse<ReservationServiceResponse> addService(@PathVariable Long propertyId,
                                                                @PathVariable Long id,
                                                                @PathVariable Long legId,
                                                                @Valid @RequestBody ReservationServiceRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(reservationService.addService(id, legId, propertyId, request));
    }

    /** 유료 서비스 삭제 */
    @Operation(summary = "유료 서비스 삭제", description = "서브 예약의 유료 서비스 삭제")
    @DeleteMapping("/{id}/legs/{legId}/services/{serviceId}")
    public HolaResponse<Void> removeService(@PathVariable Long propertyId,
                                              @PathVariable Long id,
                                              @PathVariable Long legId,
                                              @PathVariable Long serviceId) {
        accessControlService.validatePropertyAccess(propertyId);
        reservationService.removeService(id, legId, serviceId, propertyId);
        return HolaResponse.success();
    }

    /** 예치금 수정 */
    @Operation(summary = "예치금 수정", description = "예치금 정보 수정")
    @PutMapping("/{id}/deposit/{depositId}")
    public HolaResponse<ReservationDepositResponse> updateDeposit(@PathVariable Long propertyId,
                                                                   @PathVariable Long id,
                                                                   @PathVariable Long depositId,
                                                                   @Valid @RequestBody ReservationDepositRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(reservationService.updateDeposit(id, propertyId, depositId, request));
    }
}
