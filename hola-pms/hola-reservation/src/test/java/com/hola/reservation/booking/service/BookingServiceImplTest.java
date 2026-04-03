package com.hola.reservation.booking.service;

import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.hotel.entity.Hotel;
import com.hola.hotel.entity.Property;
import com.hola.reservation.booking.dto.request.BookingCreateRequest;
import com.hola.reservation.booking.dto.request.BookingSearchRequest;
import com.hola.reservation.booking.dto.request.PriceCheckRequest;
import com.hola.reservation.booking.dto.response.*;
import com.hola.reservation.booking.gateway.PaymentResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * BookingServiceImpl 단위 테스트 (Facade 위임 검증)
 * <p>
 * BookingServiceImpl은 BookingSearchService, BookingCreationService,
 * BookingManagementService로 위임하는 Facade.
 * 각 서비스의 상세 비즈니스 로직 테스트는 해당 구현체 테스트에서 수행.
 * </p>
 */
@DisplayName("BookingServiceImpl - 부킹엔진 서비스 (Facade)")
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("deprecation")
class BookingServiceImplTest {

    @InjectMocks
    private BookingServiceImpl bookingService;

    @Mock private BookingSearchService searchService;
    @Mock private BookingCreationService creationService;
    @Mock private BookingManagementService managementService;

    // 공통 테스트 데이터
    private static final String PROPERTY_CODE = "GMP";
    private static final Long PROPERTY_ID = 1L;
    private static final Long ROOM_TYPE_ID = 10L;
    private static final Long RATE_CODE_ID = 100L;
    private static final String CONFIRMATION_NO = "HK4F29XP";
    private static final String GUEST_EMAIL = "guest@test.com";
    private static final String GUEST_PHONE = "01012345678";

    // ===== 1. Search 위임 검증 =====

    @Nested
    @DisplayName("1. Search 위임")
    class SearchDelegation {

        @Test
        @DisplayName("getPropertyInfo 위임")
        void getPropertyInfo_delegates() {
            PropertyInfoResponse expected = PropertyInfoResponse.builder()
                    .propertyId(PROPERTY_ID)
                    .propertyCode(PROPERTY_CODE)
                    .propertyName("올라 강남")
                    .build();
            when(searchService.getPropertyInfo(PROPERTY_CODE)).thenReturn(expected);

            PropertyInfoResponse result = bookingService.getPropertyInfo(PROPERTY_CODE);

            assertThat(result).isEqualTo(expected);
            verify(searchService).getPropertyInfo(PROPERTY_CODE);
        }

        @Test
        @DisplayName("getPropertyInfo 예외 전파")
        void getPropertyInfo_propagatesException() {
            when(searchService.getPropertyInfo("INVALID"))
                    .thenThrow(new HolaException(ErrorCode.BOOKING_PROPERTY_NOT_FOUND));

            assertThatThrownBy(() -> bookingService.getPropertyInfo("INVALID"))
                    .isInstanceOf(HolaException.class)
                    .extracting(e -> ((HolaException) e).getErrorCode())
                    .isEqualTo(ErrorCode.BOOKING_PROPERTY_NOT_FOUND);
        }

        @Test
        @DisplayName("searchAvailability 위임")
        void searchAvailability_delegates() {
            BookingSearchRequest req = new BookingSearchRequest();
            req.setCheckIn(LocalDate.now().plusDays(3));
            req.setCheckOut(LocalDate.now().plusDays(5));
            req.setAdults(2);

            List<AvailableRoomTypeResponse> expected = List.of();
            when(searchService.searchAvailability(PROPERTY_CODE, req)).thenReturn(expected);

            List<AvailableRoomTypeResponse> result = bookingService.searchAvailability(PROPERTY_CODE, req);

            assertThat(result).isEqualTo(expected);
            verify(searchService).searchAvailability(PROPERTY_CODE, req);
        }

        @Test
        @DisplayName("calculatePrice 위임")
        void calculatePrice_delegates() {
            PriceCheckRequest req = new PriceCheckRequest();
            req.setRoomTypeId(ROOM_TYPE_ID);
            req.setRateCodeId(RATE_CODE_ID);

            PriceCheckResponse expected = PriceCheckResponse.builder()
                    .grandTotal(new BigDecimal("230000"))
                    .build();
            when(searchService.calculatePrice(PROPERTY_CODE, req)).thenReturn(expected);

            PriceCheckResponse result = bookingService.calculatePrice(PROPERTY_CODE, req);

            assertThat(result).isEqualTo(expected);
            verify(searchService).calculatePrice(PROPERTY_CODE, req);
        }

        @Test
        @DisplayName("getConfirmation 위임")
        void getConfirmation_delegates() {
            BookingConfirmationResponse expected = BookingConfirmationResponse.builder()
                    .confirmationNo(CONFIRMATION_NO)
                    .build();
            when(searchService.getConfirmation(CONFIRMATION_NO, GUEST_EMAIL)).thenReturn(expected);

            BookingConfirmationResponse result = bookingService.getConfirmation(CONFIRMATION_NO, GUEST_EMAIL);

            assertThat(result.getConfirmationNo()).isEqualTo(CONFIRMATION_NO);
            verify(searchService).getConfirmation(CONFIRMATION_NO, GUEST_EMAIL);
        }

        @Test
        @DisplayName("getCalendar 위임")
        void getCalendar_delegates() {
            LocalDate start = LocalDate.now();
            LocalDate end = start.plusDays(30);
            CalendarResponse expected = CalendarResponse.builder()
                    .propertyCode(PROPERTY_CODE)
                    .build();
            when(searchService.getCalendar(PROPERTY_CODE, start, end, "all")).thenReturn(expected);

            CalendarResponse result = bookingService.getCalendar(PROPERTY_CODE, start, end, "all");

            assertThat(result).isEqualTo(expected);
            verify(searchService).getCalendar(PROPERTY_CODE, start, end, "all");
        }

        @Test
        @DisplayName("getRatePlans 위임")
        void getRatePlans_delegates() {
            LocalDate checkIn = LocalDate.now().plusDays(3);
            LocalDate checkOut = LocalDate.now().plusDays(5);
            List<RatePlanListResponse> expected = List.of();
            when(searchService.getRatePlans(PROPERTY_CODE, checkIn, checkOut, 2, 0, null))
                    .thenReturn(expected);

            List<RatePlanListResponse> result = bookingService.getRatePlans(
                    PROPERTY_CODE, checkIn, checkOut, 2, 0, null);

            assertThat(result).isEqualTo(expected);
            verify(searchService).getRatePlans(PROPERTY_CODE, checkIn, checkOut, 2, 0, null);
        }
    }

    // ===== 2. Creation 위임 검증 =====

    @Nested
    @DisplayName("2. Creation 위임")
    class CreationDelegation {

        @Test
        @DisplayName("createBooking 위임")
        void createBooking_delegates() {
            BookingCreateRequest req = new BookingCreateRequest();
            BookingConfirmationResponse expected = BookingConfirmationResponse.builder()
                    .confirmationNo(CONFIRMATION_NO)
                    .reservationStatus("RESERVED")
                    .build();
            when(creationService.createBooking(PROPERTY_CODE, req, "127.0.0.1", "TestAgent"))
                    .thenReturn(expected);

            BookingConfirmationResponse result = bookingService.createBooking(
                    PROPERTY_CODE, req, "127.0.0.1", "TestAgent");

            assertThat(result.getConfirmationNo()).isEqualTo(CONFIRMATION_NO);
            verify(creationService).createBooking(PROPERTY_CODE, req, "127.0.0.1", "TestAgent");
        }

        @Test
        @DisplayName("createBooking 약관 미동의 예외 전파")
        void createBooking_termsNotAgreed() {
            BookingCreateRequest req = new BookingCreateRequest();
            when(creationService.createBooking(eq(PROPERTY_CODE), eq(req), any(), any()))
                    .thenThrow(new HolaException(ErrorCode.BOOKING_TERMS_NOT_AGREED));

            assertThatThrownBy(() -> bookingService.createBooking(PROPERTY_CODE, req, "127.0.0.1", "TestAgent"))
                    .isInstanceOf(HolaException.class)
                    .extracting(e -> ((HolaException) e).getErrorCode())
                    .isEqualTo(ErrorCode.BOOKING_TERMS_NOT_AGREED);
        }

        @Test
        @DisplayName("validateBookingRequest 위임")
        void validateBookingRequest_delegates() {
            BookingCreateRequest req = new BookingCreateRequest();
            BookingValidationResult expected = BookingValidationResult.builder()
                    .grandTotal(new BigDecimal("230000"))
                    .build();
            when(creationService.validateBookingRequest(PROPERTY_CODE, req)).thenReturn(expected);

            BookingValidationResult result = bookingService.validateBookingRequest(PROPERTY_CODE, req);

            assertThat(result.getGrandTotal()).isEqualByComparingTo(new BigDecimal("230000"));
            verify(creationService).validateBookingRequest(PROPERTY_CODE, req);
        }

        @Test
        @DisplayName("createBookingWithPaymentResult 위임")
        void createBookingWithPaymentResult_delegates() {
            BookingCreateRequest req = new BookingCreateRequest();
            PaymentResult paymentResult = PaymentResult.success("MOCK-123", "MOCK", new BigDecimal("230000"));
            BookingConfirmationResponse expected = BookingConfirmationResponse.builder()
                    .confirmationNo(CONFIRMATION_NO)
                    .build();
            when(creationService.createBookingWithPaymentResult(
                    PROPERTY_CODE, req, paymentResult, "127.0.0.1", "TestAgent"))
                    .thenReturn(expected);

            BookingConfirmationResponse result = bookingService.createBookingWithPaymentResult(
                    PROPERTY_CODE, req, paymentResult, "127.0.0.1", "TestAgent");

            assertThat(result.getConfirmationNo()).isEqualTo(CONFIRMATION_NO);
            verify(creationService).createBookingWithPaymentResult(
                    PROPERTY_CODE, req, paymentResult, "127.0.0.1", "TestAgent");
        }
    }

    // ===== 3. Management 위임 검증 =====

    @Nested
    @DisplayName("3. Management 위임")
    class ManagementDelegation {

        @Test
        @DisplayName("cancelBooking 위임")
        void cancelBooking_delegates() {
            CancelBookingResponse expected = CancelBookingResponse.builder()
                    .confirmationNo(CONFIRMATION_NO)
                    .status("CANCELED")
                    .cancelFeeAmount(new BigDecimal("50000"))
                    .refundAmount(new BigDecimal("180000"))
                    .build();
            when(managementService.cancelBooking(CONFIRMATION_NO, GUEST_EMAIL, "127.0.0.1", "TestAgent"))
                    .thenReturn(expected);

            CancelBookingResponse result = bookingService.cancelBooking(
                    CONFIRMATION_NO, GUEST_EMAIL, "127.0.0.1", "TestAgent");

            assertThat(result.getStatus()).isEqualTo("CANCELED");
            assertThat(result.getCancelFeeAmount()).isEqualByComparingTo(new BigDecimal("50000"));
            verify(managementService).cancelBooking(CONFIRMATION_NO, GUEST_EMAIL, "127.0.0.1", "TestAgent");
        }

        @Test
        @DisplayName("cancelBooking 이미 취소된 예약 예외 전파")
        void cancelBooking_alreadyCanceled() {
            when(managementService.cancelBooking(eq(CONFIRMATION_NO), eq(GUEST_EMAIL), any(), any()))
                    .thenThrow(new HolaException(ErrorCode.BOOKING_ALREADY_CANCELED));

            assertThatThrownBy(() -> bookingService.cancelBooking(
                    CONFIRMATION_NO, GUEST_EMAIL, "127.0.0.1", "TestAgent"))
                    .isInstanceOf(HolaException.class)
                    .extracting(e -> ((HolaException) e).getErrorCode())
                    .isEqualTo(ErrorCode.BOOKING_ALREADY_CANCELED);
        }

        @Test
        @DisplayName("getCancelFeePreview 위임")
        void getCancelFeePreview_delegates() {
            CancelFeePreviewResponse expected = CancelFeePreviewResponse.builder()
                    .confirmationNo(CONFIRMATION_NO)
                    .cancelFeeAmount(new BigDecimal("50000"))
                    .build();
            when(managementService.getCancelFeePreview(CONFIRMATION_NO, GUEST_EMAIL)).thenReturn(expected);

            CancelFeePreviewResponse result = bookingService.getCancelFeePreview(CONFIRMATION_NO, GUEST_EMAIL);

            assertThat(result.getCancelFeeAmount()).isEqualByComparingTo(new BigDecimal("50000"));
            verify(managementService).getCancelFeePreview(CONFIRMATION_NO, GUEST_EMAIL);
        }

        @Test
        @DisplayName("lookupReservations 위임")
        void lookupReservations_delegates() {
            List<BookingLookupResponse> expected = List.of(BookingLookupResponse.builder()
                    .confirmationNo(CONFIRMATION_NO)
                    .build());
            when(managementService.lookupReservations(GUEST_EMAIL, "Hong")).thenReturn(expected);

            List<BookingLookupResponse> result = bookingService.lookupReservations(GUEST_EMAIL, "Hong");

            assertThat(result).hasSize(1);
            verify(managementService).lookupReservations(GUEST_EMAIL, "Hong");
        }
    }
}
