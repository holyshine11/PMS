package com.hola.reservation.booking.service;

import com.hola.reservation.booking.dto.request.BookingCreateRequest;
import com.hola.reservation.booking.dto.request.BookingModifyRequest;
import com.hola.reservation.booking.dto.request.BookingSearchRequest;
import com.hola.reservation.booking.dto.request.PriceCheckRequest;
import com.hola.reservation.booking.dto.response.*;
import com.hola.reservation.booking.gateway.PaymentResult;

import java.time.LocalDate;
import java.util.List;

/**
 * 부킹엔진 서비스 인터페이스
 * - 게스트 예약 플로우 전체 오케스트레이션
 */
public interface BookingService {

    /**
     * 프로퍼티 기본정보 조회 (호텔명, 주소, 체크인/아웃 시간 등)
     */
    PropertyInfoResponse getPropertyInfo(String propertyCode);

    /**
     * 캘린더 조회 (판매 가능 날짜, 체크인/아웃 분리)
     * @param type checkin, checkout, all
     */
    CalendarResponse getCalendar(String propertyCode, LocalDate startDate, LocalDate endDate, String type);

    /**
     * 패키지(레이트플랜) 목록 조회 (산하 2.4 대응)
     */
    List<RatePlanListResponse> getRatePlans(String propertyCode, LocalDate checkIn, LocalDate checkOut,
                                            Integer adults, Integer children, String promotionCode);

    /**
     * 객실 상세 조회 (산하 2.8 대응)
     */
    RoomDetailResponse getRoomDetail(String propertyCode, Long roomTypeId);

    /**
     * 패키지(레이트플랜) 상세 조회 (산하 2.5 대응)
     */
    RatePlanDetailResponse getRatePlanDetail(String propertyCode, Long ratePlanId);

    /**
     * 가용 객실 검색 (날짜/인원 기반 객실타입 + 요금 조회)
     */
    List<AvailableRoomTypeResponse> searchAvailability(String propertyCode, BookingSearchRequest request);

    /**
     * 선택 객실/레이트 상세 요금 조회
     */
    PriceCheckResponse calculatePrice(String propertyCode, PriceCheckRequest request);

    /**
     * 예약 생성 + Mock 결제 처리 (CASH 또는 Mock 환경)
     */
    BookingConfirmationResponse createBooking(String propertyCode, BookingCreateRequest request,
                                              String clientIp, String userAgent);

    /**
     * 부킹 요청 검증 (Steps 1-4: 약관/멱등성/가용성/가격 재계산)
     * - KICC 결제 플로우에서 거래등록 전 사전 검증용
     */
    BookingValidationResult validateBookingRequest(String propertyCode, BookingCreateRequest request);

    /**
     * PG 결제 결과 기반 예약 생성 (KICC 3단계 플로우용)
     * - PG 승인 완료 후 예약 + 결제 내역 저장
     */
    BookingConfirmationResponse createBookingWithPaymentResult(String propertyCode, BookingCreateRequest request,
                                                                PaymentResult paymentResult,
                                                                String clientIp, String userAgent);

    /**
     * 예약 확인 조회 (확인번호 + 2차 검증)
     */
    BookingConfirmationResponse getConfirmation(String confirmationNo, String verificationValue);

    /**
     * 취소 수수료 미리보기 (확인번호 + 이메일 검증)
     */
    CancelFeePreviewResponse getCancelFeePreview(String confirmationNo, String email);

    /**
     * 게스트 자가 취소 (확인번호 + 이메일 검증)
     */
    CancelBookingResponse cancelBooking(String confirmationNo, String email, String clientIp, String userAgent);

    /**
     * 내 예약 조회 (이메일 + 성 기반 검색)
     */
    List<BookingLookupResponse> lookupReservations(String email, String lastName);

    /**
     * 게스트 자가 예약 수정 (날짜/인원 변경 + 차액 계산)
     */
    BookingModifyResponse modifyBooking(String confirmationNo, BookingModifyRequest request);

    /**
     * 프로모션 코드 유효성 검증
     */
    PromotionValidationResponse validatePromotionCode(String propertyCode, String code,
                                                       LocalDate checkIn, LocalDate checkOut);

    /**
     * 유료 서비스 목록 조회 (부킹엔진용)
     */
    List<AddOnServiceResponse> getAddOnServices(String propertyCode);

    /**
     * 숙소 이미지 목록 조회
     */
    List<PropertyImageResponse> getPropertyImages(String propertyCode);

    /**
     * 객실타입 이미지 목록 조회
     */
    List<PropertyImageResponse> getRoomTypeImages(String propertyCode, Long roomTypeId);

    /**
     * 이용약관 목록 조회
     */
    List<PropertyTermsResponse> getTerms(String propertyCode);

    /**
     * 객실별 적용가능 레이트플랜 조회 (역방향)
     */
    List<RatePlanListResponse> getRatePlansByRoomType(String propertyCode, Long roomTypeId,
                                                      LocalDate checkIn, LocalDate checkOut,
                                                      Integer adults, Integer children);
}
