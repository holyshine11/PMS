package com.hola.reservation.booking.controller;

import com.hola.reservation.booking.dto.BookingResponse;
import com.hola.reservation.booking.dto.request.BookingCreateRequest;
import com.hola.reservation.booking.dto.request.BookingModifyRequest;
import com.hola.reservation.booking.dto.request.BookingSearchRequest;
import com.hola.reservation.booking.dto.request.CancelBookingRequest;
import com.hola.reservation.booking.dto.request.PriceCheckRequest;
import com.hola.reservation.booking.dto.response.*;
import com.hola.reservation.booking.service.BookingService;
import com.hola.reservation.booking.service.CardBinValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 부킹엔진 공개 API
 * - 인증 불필요 (permitAll) → Phase 1에서 API Key 인증으로 전환 예정
 * - 게스트 예약 플로우 전체 제공
 * - 산하정보통신 API 형식 호환 (BookingResponse)
 */
@Tag(name = "Booking Engine", description = "부킹엔진 공개 API (게스트용)")
@RestController
@RequestMapping("/api/v1/booking")
@RequiredArgsConstructor
public class BookingApiController {

    private final BookingService bookingService;
    private final CardBinValidationService cardBinValidationService;

    /**
     * 프로퍼티 기본정보 조회
     */
    @Operation(summary = "프로퍼티 정보 조회", description = "호텔 프로퍼티 기본정보 (이름, 주소, 체크인/아웃 시간 등)")
    @GetMapping("/properties/{propertyCode}")
    public BookingResponse<PropertyInfoResponse> getPropertyInfo(
            @PathVariable String propertyCode) {
        return BookingResponse.success(bookingService.getPropertyInfo(propertyCode));
    }

    /**
     * 캘린더 조회 (판매 가능 날짜 목록)
     */
    @Operation(summary = "캘린더 조회", description = "판매 가능 날짜 목록 (체크인/체크아웃 분리 모드 지원)")
    @GetMapping("/properties/{propertyCode}/calendar")
    public BookingResponse<CalendarResponse> getCalendar(
            @PathVariable String propertyCode,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(defaultValue = "all") String type) {
        return BookingResponse.success(bookingService.getCalendar(propertyCode, startDate, endDate, type));
    }

    /**
     * 객실 상세 조회
     */
    @Operation(summary = "객실 상세", description = "객실 시설, 최대인원, 어메니티 등 상세 정보")
    @GetMapping("/properties/{propertyCode}/rooms/{roomTypeId}")
    public BookingResponse<RoomDetailResponse> getRoomDetail(
            @PathVariable String propertyCode,
            @PathVariable Long roomTypeId) {
        return BookingResponse.success(bookingService.getRoomDetail(propertyCode, roomTypeId));
    }

    /**
     * 패키지(레이트플랜) 목록 조회
     */
    @Operation(summary = "패키지 목록", description = "조건별 예약 가능 레이트플랜 목록 (최저가 순)")
    @GetMapping("/properties/{propertyCode}/rate-plans")
    public BookingResponse<List<RatePlanListResponse>> getRatePlans(
            @PathVariable String propertyCode,
            @RequestParam LocalDate checkIn,
            @RequestParam LocalDate checkOut,
            @RequestParam(required = false, defaultValue = "2") Integer adults,
            @RequestParam(required = false, defaultValue = "0") Integer children,
            @RequestParam(required = false) String promotionCode) {
        return BookingResponse.success(bookingService.getRatePlans(
                propertyCode, checkIn, checkOut, adults, children, promotionCode));
    }

    /**
     * 패키지(레이트플랜) 상세 조회
     */
    @Operation(summary = "패키지 상세", description = "취소정책, 노쇼정책, 요금 상세 등")
    @GetMapping("/properties/{propertyCode}/rate-plans/{ratePlanId}")
    public BookingResponse<RatePlanDetailResponse> getRatePlanDetail(
            @PathVariable String propertyCode,
            @PathVariable Long ratePlanId) {
        return BookingResponse.success(bookingService.getRatePlanDetail(propertyCode, ratePlanId));
    }

    /**
     * 이용약관 조회
     */
    @Operation(summary = "이용약관 조회", description = "예약 약관, 개인정보 처리방침 등")
    @GetMapping("/properties/{propertyCode}/terms")
    public BookingResponse<List<PropertyTermsResponse>> getTerms(
            @PathVariable String propertyCode) {
        return BookingResponse.success(bookingService.getTerms(propertyCode));
    }

    /**
     * 숙소 이미지 목록 조회
     */
    @Operation(summary = "숙소 이미지", description = "프로퍼티 전체 이미지 (숙소/시설/외관)")
    @GetMapping("/properties/{propertyCode}/images")
    public BookingResponse<List<PropertyImageResponse>> getPropertyImages(
            @PathVariable String propertyCode) {
        return BookingResponse.success(bookingService.getPropertyImages(propertyCode));
    }

    /**
     * 객실타입 이미지 목록 조회
     */
    @Operation(summary = "객실타입 이미지", description = "특정 객실타입의 이미지 목록")
    @GetMapping("/properties/{propertyCode}/rooms/{roomTypeId}/images")
    public BookingResponse<List<PropertyImageResponse>> getRoomTypeImages(
            @PathVariable String propertyCode,
            @PathVariable Long roomTypeId) {
        return BookingResponse.success(bookingService.getRoomTypeImages(propertyCode, roomTypeId));
    }

    /**
     * 유료 서비스 목록 조회
     */
    @Operation(summary = "유료 서비스 목록", description = "예약 시 선택 가능한 유료 서비스(조식, 스파 등) 목록")
    @GetMapping("/properties/{propertyCode}/add-on-services")
    public BookingResponse<List<AddOnServiceResponse>> getAddOnServices(
            @PathVariable String propertyCode) {
        return BookingResponse.success(bookingService.getAddOnServices(propertyCode));
    }

    /**
     * 프로모션 코드 검증
     */
    @Operation(summary = "프로모션 코드 검증", description = "프로모션 코드 유효성 + 할인 정보 미리보기")
    @GetMapping("/properties/{propertyCode}/promotions/validate")
    public BookingResponse<PromotionValidationResponse> validatePromotionCode(
            @PathVariable String propertyCode,
            @RequestParam String code,
            @RequestParam(required = false) LocalDate checkIn,
            @RequestParam(required = false) LocalDate checkOut) {
        return BookingResponse.success(bookingService.validatePromotionCode(propertyCode, code, checkIn, checkOut));
    }

    /**
     * 가용 객실 검색 (날짜/인원 기반)
     */
    @Operation(summary = "가용 객실 검색", description = "날짜/인원 기반 예약 가능 객실타입 + 요금 조회")
    @GetMapping("/properties/{propertyCode}/availability")
    public BookingResponse<List<AvailableRoomTypeResponse>> searchAvailability(
            @PathVariable String propertyCode,
            @Valid @ModelAttribute BookingSearchRequest request) {
        return BookingResponse.success(bookingService.searchAvailability(propertyCode, request));
    }

    /**
     * 요금 상세 조회
     */
    @Operation(summary = "요금 상세 조회", description = "선택한 객실/레이트의 일자별 상세 요금 확인")
    @PostMapping("/properties/{propertyCode}/price-check")
    public BookingResponse<PriceCheckResponse> calculatePrice(
            @PathVariable String propertyCode,
            @Valid @RequestBody PriceCheckRequest request) {
        return BookingResponse.success(bookingService.calculatePrice(propertyCode, request));
    }

    /**
     * 예약 생성 + 결제 처리
     */
    @Operation(summary = "예약 생성", description = "게스트 예약 생성 + 결제 처리")
    @PostMapping("/properties/{propertyCode}/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse<BookingConfirmationResponse> createBooking(
            @PathVariable String propertyCode,
            @Valid @RequestBody BookingCreateRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");
        return BookingResponse.success(bookingService.createBooking(propertyCode, request, clientIp, userAgent));
    }

    /**
     * 내 예약 조회 (이메일 + 영문 성으로 검색)
     */
    @Operation(summary = "내 예약 조회", description = "이메일 + 영문 성(Last Name)으로 예약 목록 검색")
    @GetMapping("/reservations/lookup")
    public BookingResponse<List<BookingLookupResponse>> lookupReservations(
            @RequestParam String email,
            @RequestParam String lastName) {
        return BookingResponse.success(bookingService.lookupReservations(email, lastName));
    }

    /**
     * 예약 확인 조회
     */
    @Operation(summary = "예약 확인 조회", description = "확인번호 + 이메일/전화로 예약 내역 조회")
    @GetMapping("/confirmation/{confirmationNo}")
    public BookingResponse<BookingConfirmationResponse> getConfirmation(
            @PathVariable String confirmationNo,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone) {
        String verificationValue = email != null ? email : phone;
        return BookingResponse.success(bookingService.getConfirmation(confirmationNo, verificationValue));
    }

    /**
     * 예약 수정 (날짜/인원 변경)
     */
    @Operation(summary = "예약 수정", description = "날짜/인원 변경 + 차액 계산 (이메일 본인인증)")
    @PutMapping("/reservations/{confirmationNo}/modify")
    public BookingResponse<BookingModifyResponse> modifyBooking(
            @PathVariable String confirmationNo,
            @Valid @RequestBody BookingModifyRequest request) {
        return BookingResponse.success(bookingService.modifyBooking(confirmationNo, request));
    }

    /**
     * 취소 수수료 미리보기
     */
    @Operation(summary = "취소 수수료 미리보기", description = "확인번호 + 이메일 또는 전화번호로 취소 시 수수료 확인")
    @GetMapping("/reservations/{confirmationNo}/cancel-fee")
    public BookingResponse<CancelFeePreviewResponse> getCancelFeePreview(
            @PathVariable String confirmationNo,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone) {
        String verificationValue = email != null ? email : phone;
        return BookingResponse.success(bookingService.getCancelFeePreview(confirmationNo, verificationValue));
    }

    /**
     * 게스트 자가 취소
     */
    @Operation(summary = "게스트 자가 취소", description = "확인번호 + 이메일 또는 전화번호 검증 후 예약 취소")
    @PostMapping("/reservations/{confirmationNo}/cancel")
    public ResponseEntity<BookingResponse<CancelBookingResponse>> cancelBooking(
            @PathVariable String confirmationNo,
            @Valid @RequestBody CancelBookingRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");
        String verificationValue = request.getEmail() != null ? request.getEmail() : request.getPhone();
        return ResponseEntity.ok(BookingResponse.success(
                bookingService.cancelBooking(confirmationNo, verificationValue, clientIp, userAgent)));
    }

    /**
     * 객실별 적용가능 레이트플랜 조회 (역방향)
     */
    @Operation(summary = "객실별 레이트플랜", description = "특정 객실타입에 적용 가능한 레이트플랜 목록 (최저가 순)")
    @GetMapping("/properties/{propertyCode}/rooms/{roomTypeId}/rate-plans")
    public BookingResponse<List<RatePlanListResponse>> getRatePlansByRoomType(
            @PathVariable String propertyCode,
            @PathVariable Long roomTypeId,
            @RequestParam LocalDate checkIn,
            @RequestParam LocalDate checkOut,
            @RequestParam(required = false, defaultValue = "2") Integer adults,
            @RequestParam(required = false, defaultValue = "0") Integer children) {
        return BookingResponse.success(bookingService.getRatePlansByRoomType(
                propertyCode, roomTypeId, checkIn, checkOut, adults, children));
    }

    /**
     * Card BIN 검증
     */
    @Operation(summary = "Card BIN 검증", description = "카드 BIN(앞 6자리)으로 네트워크 판별 및 지원 여부 확인")
    @GetMapping("/card-bin/validate")
    public BookingResponse<CardBinValidationResponse> validateCardBin(
            @RequestParam String bin) {
        return BookingResponse.success(cardBinValidationService.validate(bin));
    }
}
