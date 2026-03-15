package com.hola.reservation.booking.service;

import com.hola.reservation.booking.dto.request.BookingCreateRequest;
import com.hola.reservation.booking.dto.request.BookingSearchRequest;
import com.hola.reservation.booking.dto.request.PriceCheckRequest;
import com.hola.reservation.booking.dto.response.*;

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
     * 가용 객실 검색 (날짜/인원 기반 객실타입 + 요금 조회)
     */
    List<AvailableRoomTypeResponse> searchAvailability(String propertyCode, BookingSearchRequest request);

    /**
     * 선택 객실/레이트 상세 요금 조회
     */
    PriceCheckResponse calculatePrice(String propertyCode, PriceCheckRequest request);

    /**
     * 예약 생성 + Mock 결제 처리
     */
    BookingConfirmationResponse createBooking(String propertyCode, BookingCreateRequest request,
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
}
