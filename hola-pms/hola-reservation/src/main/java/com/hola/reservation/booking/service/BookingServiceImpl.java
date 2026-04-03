package com.hola.reservation.booking.service;

import com.hola.reservation.booking.dto.request.BookingCreateRequest;
import com.hola.reservation.booking.dto.request.BookingModifyRequest;
import com.hola.reservation.booking.dto.request.BookingSearchRequest;
import com.hola.reservation.booking.dto.request.PriceCheckRequest;
import com.hola.reservation.booking.dto.response.*;
import com.hola.reservation.booking.gateway.PaymentResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 부킹엔진 서비스 통합 구현 (하위호환 Facade)
 * <p>
 * 실제 로직은 {@link BookingSearchServiceImpl}, {@link BookingCreationServiceImpl},
 * {@link BookingManagementServiceImpl}에 위임.
 * 신규 코드는 분리된 서비스를 직접 주입하여 사용할 것.
 * </p>
 * @deprecated 분리된 서비스를 직접 주입하여 사용 권장
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Deprecated
@SuppressWarnings("deprecation")
public class BookingServiceImpl implements BookingService {

    private final BookingSearchService searchService;
    private final BookingCreationService creationService;
    private final BookingManagementService managementService;

    // === Search 위임 ===

    @Override
    public PropertyInfoResponse getPropertyInfo(String propertyCode) {
        return searchService.getPropertyInfo(propertyCode);
    }

    @Override
    public CalendarResponse getCalendar(String propertyCode, LocalDate startDate, LocalDate endDate, String type) {
        return searchService.getCalendar(propertyCode, startDate, endDate, type);
    }

    @Override
    public List<RatePlanListResponse> getRatePlans(String propertyCode, LocalDate checkIn, LocalDate checkOut,
                                                    Integer adults, Integer children, String promotionCode) {
        return searchService.getRatePlans(propertyCode, checkIn, checkOut, adults, children, promotionCode);
    }

    @Override
    public RoomDetailResponse getRoomDetail(String propertyCode, Long roomTypeId) {
        return searchService.getRoomDetail(propertyCode, roomTypeId);
    }

    @Override
    public RatePlanDetailResponse getRatePlanDetail(String propertyCode, Long ratePlanId) {
        return searchService.getRatePlanDetail(propertyCode, ratePlanId);
    }

    @Override
    public List<AvailableRoomTypeResponse> searchAvailability(String propertyCode, BookingSearchRequest request) {
        return searchService.searchAvailability(propertyCode, request);
    }

    @Override
    public PriceCheckResponse calculatePrice(String propertyCode, PriceCheckRequest request) {
        return searchService.calculatePrice(propertyCode, request);
    }

    @Override
    public BookingConfirmationResponse getConfirmation(String confirmationNo, String verificationValue) {
        return searchService.getConfirmation(confirmationNo, verificationValue);
    }

    @Override
    public List<PropertyImageResponse> getPropertyImages(String propertyCode) {
        return searchService.getPropertyImages(propertyCode);
    }

    @Override
    public List<PropertyImageResponse> getRoomTypeImages(String propertyCode, Long roomTypeId) {
        return searchService.getRoomTypeImages(propertyCode, roomTypeId);
    }

    @Override
    public List<PropertyTermsResponse> getTerms(String propertyCode) {
        return searchService.getTerms(propertyCode);
    }

    @Override
    public List<AddOnServiceResponse> getAddOnServices(String propertyCode) {
        return searchService.getAddOnServices(propertyCode);
    }

    @Override
    public PromotionValidationResponse validatePromotionCode(String propertyCode, String code,
                                                              LocalDate checkIn, LocalDate checkOut) {
        return searchService.validatePromotionCode(propertyCode, code, checkIn, checkOut);
    }

    @Override
    public List<RatePlanListResponse> getRatePlansByRoomType(String propertyCode, Long roomTypeId,
                                                              LocalDate checkIn, LocalDate checkOut,
                                                              Integer adults, Integer children) {
        return searchService.getRatePlansByRoomType(propertyCode, roomTypeId, checkIn, checkOut, adults, children);
    }

    // === Creation 위임 ===

    @Override
    @Transactional(readOnly = true)
    public BookingValidationResult validateBookingRequest(String propertyCode, BookingCreateRequest request) {
        return creationService.validateBookingRequest(propertyCode, request);
    }

    @Override
    @Transactional
    public BookingConfirmationResponse createBooking(String propertyCode, BookingCreateRequest request,
                                                      String clientIp, String userAgent) {
        return creationService.createBooking(propertyCode, request, clientIp, userAgent);
    }

    @Override
    @Transactional
    public BookingConfirmationResponse createBookingWithPaymentResult(String propertyCode, BookingCreateRequest request,
                                                                       PaymentResult paymentResult,
                                                                       String clientIp, String userAgent) {
        return creationService.createBookingWithPaymentResult(propertyCode, request, paymentResult, clientIp, userAgent);
    }

    // === Management 위임 ===

    @Override
    public CancelFeePreviewResponse getCancelFeePreview(String confirmationNo, String email) {
        return managementService.getCancelFeePreview(confirmationNo, email);
    }

    @Override
    @Transactional
    public CancelBookingResponse cancelBooking(String confirmationNo, String email, String clientIp, String userAgent) {
        return managementService.cancelBooking(confirmationNo, email, clientIp, userAgent);
    }

    @Override
    public List<BookingLookupResponse> lookupReservations(String email, String lastName) {
        return managementService.lookupReservations(email, lastName);
    }

    @Override
    @Transactional
    public BookingModifyResponse modifyBooking(String confirmationNo, BookingModifyRequest request) {
        return managementService.modifyBooking(confirmationNo, request);
    }
}
