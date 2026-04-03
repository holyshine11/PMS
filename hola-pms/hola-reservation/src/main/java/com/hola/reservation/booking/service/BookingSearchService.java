package com.hola.reservation.booking.service;

import com.hola.reservation.booking.dto.request.BookingSearchRequest;
import com.hola.reservation.booking.dto.request.PriceCheckRequest;
import com.hola.reservation.booking.dto.response.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 부킹엔진 검색/조회 서비스 인터페이스
 * - 프로퍼티 정보, 객실 검색, 요금 확인, 캘린더 등
 */
public interface BookingSearchService {

    PropertyInfoResponse getPropertyInfo(String propertyCode);

    List<AvailableRoomTypeResponse> searchAvailability(String propertyCode, BookingSearchRequest request);

    PriceCheckResponse calculatePrice(String propertyCode, PriceCheckRequest request);

    BookingConfirmationResponse getConfirmation(String confirmationNo, String verificationValue);

    List<PropertyImageResponse> getPropertyImages(String propertyCode);

    List<PropertyImageResponse> getRoomTypeImages(String propertyCode, Long roomTypeId);

    List<PropertyTermsResponse> getTerms(String propertyCode);

    List<AddOnServiceResponse> getAddOnServices(String propertyCode);

    PromotionValidationResponse validatePromotionCode(String propertyCode, String code,
                                                       LocalDate checkIn, LocalDate checkOut);

    List<RatePlanListResponse> getRatePlans(String propertyCode, LocalDate checkIn, LocalDate checkOut,
                                            Integer adults, Integer children, String promotionCode);

    RoomDetailResponse getRoomDetail(String propertyCode, Long roomTypeId);

    RatePlanDetailResponse getRatePlanDetail(String propertyCode, Long ratePlanId);

    CalendarResponse getCalendar(String propertyCode, LocalDate startDate, LocalDate endDate, String type);

    List<RatePlanListResponse> getRatePlansByRoomType(String propertyCode, Long roomTypeId,
                                                      LocalDate checkIn, LocalDate checkOut,
                                                      Integer adults, Integer children);
}
