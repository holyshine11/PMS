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
 * 부킹엔진 서비스 통합 인터페이스 (하위호환 유지)
 * <p>
 * 실제 로직은 {@link BookingSearchService}, {@link BookingCreationService},
 * {@link BookingManagementService}로 분리됨.
 * 신규 코드는 분리된 서비스를 직접 주입하여 사용할 것.
 * </p>
 * @deprecated 분리된 BookingSearchService / BookingCreationService / BookingManagementService 사용 권장
 */
@Deprecated
public interface BookingService {

    // === Search (BookingSearchService) ===

    PropertyInfoResponse getPropertyInfo(String propertyCode);

    CalendarResponse getCalendar(String propertyCode, LocalDate startDate, LocalDate endDate, String type);

    List<RatePlanListResponse> getRatePlans(String propertyCode, LocalDate checkIn, LocalDate checkOut,
                                            Integer adults, Integer children, String promotionCode);

    RoomDetailResponse getRoomDetail(String propertyCode, Long roomTypeId);

    RatePlanDetailResponse getRatePlanDetail(String propertyCode, Long ratePlanId);

    List<AvailableRoomTypeResponse> searchAvailability(String propertyCode, BookingSearchRequest request);

    PriceCheckResponse calculatePrice(String propertyCode, PriceCheckRequest request);

    BookingConfirmationResponse getConfirmation(String confirmationNo, String verificationValue);

    List<PropertyImageResponse> getPropertyImages(String propertyCode);

    List<PropertyImageResponse> getRoomTypeImages(String propertyCode, Long roomTypeId);

    List<PropertyTermsResponse> getTerms(String propertyCode);

    List<AddOnServiceResponse> getAddOnServices(String propertyCode);

    PromotionValidationResponse validatePromotionCode(String propertyCode, String code,
                                                       LocalDate checkIn, LocalDate checkOut);

    List<RatePlanListResponse> getRatePlansByRoomType(String propertyCode, Long roomTypeId,
                                                      LocalDate checkIn, LocalDate checkOut,
                                                      Integer adults, Integer children);

    // === Creation (BookingCreationService) ===

    BookingValidationResult validateBookingRequest(String propertyCode, BookingCreateRequest request);

    BookingConfirmationResponse createBooking(String propertyCode, BookingCreateRequest request,
                                              String clientIp, String userAgent);

    BookingConfirmationResponse createBookingWithPaymentResult(String propertyCode, BookingCreateRequest request,
                                                                PaymentResult paymentResult,
                                                                String clientIp, String userAgent);

    // === Management (BookingManagementService) ===

    CancelFeePreviewResponse getCancelFeePreview(String confirmationNo, String email);

    CancelBookingResponse cancelBooking(String confirmationNo, String email, String clientIp, String userAgent);

    List<BookingLookupResponse> lookupReservations(String email, String lastName);

    BookingModifyResponse modifyBooking(String confirmationNo, BookingModifyRequest request);
}
