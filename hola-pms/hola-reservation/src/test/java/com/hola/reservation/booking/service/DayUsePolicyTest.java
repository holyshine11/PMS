package com.hola.reservation.booking.service;

import com.hola.common.enums.StayType;
import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.hotel.entity.Property;
import com.hola.rate.dto.response.RateCodeListResponse;
import com.hola.rate.entity.RateCode;
import com.hola.rate.repository.DayUseRateRepository;
import com.hola.rate.repository.RateCodeRepository;
import com.hola.rate.service.RateCodeService;
import com.hola.reservation.booking.dto.request.BookingCreateRequest;
import com.hola.reservation.booking.dto.response.BookingValidationResult;
import com.hola.reservation.service.RoomAvailabilityService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Dayuse 정책 검증 단위 테스트
 * - BookingSearchServiceImpl: dayUseEnabled 필터링 전달 검증
 * - BookingCreationServiceImpl: Dayuse 비허용 시 조기 차단 검증
 */
@DisplayName("Dayuse 정책 검증")
@ExtendWith(MockitoExtension.class)
class DayUsePolicyTest {

    @Nested
    @DisplayName("BookingSearchServiceImpl - dayUseEnabled 필터링")
    class SearchFilterTest {

        @InjectMocks
        private BookingSearchServiceImpl searchService;

        @Mock private BookingHelper helper;
        @Mock private RateCodeService rateCodeService;
        @Mock private com.hola.room.repository.RoomTypeRepository roomTypeRepository;
        @Mock private com.hola.room.repository.RoomClassRepository roomClassRepository;
        @Mock private com.hola.room.repository.RoomTypeFreeServiceRepository roomTypeFreeServiceRepository;
        @Mock private com.hola.room.repository.FreeServiceOptionRepository freeServiceOptionRepository;
        @Mock private com.hola.room.repository.PaidServiceOptionRepository paidServiceOptionRepository;
        @Mock private com.hola.rate.repository.RateCodeRepository rateCodeRepository;
        @Mock private com.hola.rate.repository.RateCodeRoomTypeRepository rateCodeRoomTypeRepository;
        @Mock private com.hola.rate.repository.RateCodePaidServiceRepository rateCodePaidServiceRepository;
        @Mock private com.hola.rate.repository.PromotionCodeRepository promotionCodeRepository;
        @Mock private RoomAvailabilityService roomAvailabilityService;
        @Mock private com.hola.reservation.service.PriceCalculationService priceCalculationService;
        @Mock private com.hola.reservation.repository.MasterReservationRepository masterReservationRepository;
        @Mock private DayUseRateRepository dayUseRateRepository;
        @Mock private com.hola.hotel.repository.PropertyImageRepository propertyImageRepository;
        @Mock private com.hola.hotel.repository.PropertyTermsRepository propertyTermsRepository;
        @Mock private com.hola.hotel.repository.CancellationFeeRepository cancellationFeeRepository;

        @Test
        @DisplayName("프로퍼티 dayUseEnabled=false → getAvailableRateCodes에 false 전달")
        void dayUseDisabled_filtersFalse() {
            // given
            Property property = Property.builder()
                    .dayUseEnabled(false)
                    .dayUseStartTime("10:00")
                    .dayUseEndTime("20:00")
                    .dayUseDefaultHours(5)
                    .build();
            ReflectionTestUtils.setField(property, "id", 1L);

            when(helper.findPropertyByCode("GMP")).thenReturn(property);
            when(roomTypeRepository.findAllByPropertyIdOrderBySortOrderAscRoomTypeCodeAsc(1L))
                    .thenReturn(List.of());
            when(rateCodeService.getAvailableRateCodes(eq(1L), any(), any(), eq(false)))
                    .thenReturn(List.of());

            // when
            var request = new com.hola.reservation.booking.dto.request.BookingSearchRequest();
            request.setCheckIn(LocalDate.now().plusDays(10));
            request.setCheckOut(LocalDate.now().plusDays(11));
            request.setAdults(2);
            searchService.searchAvailability("GMP", request);

            // then: dayUseEnabled=false가 전달되었는지 검증
            verify(rateCodeService).getAvailableRateCodes(eq(1L), any(), any(), eq(false));
        }

        @Test
        @DisplayName("프로퍼티 dayUseEnabled=true → getAvailableRateCodes에 true 전달")
        void dayUseEnabled_filtersTrue() {
            // given
            Property property = Property.builder()
                    .dayUseEnabled(true)
                    .dayUseStartTime("10:00")
                    .dayUseEndTime("20:00")
                    .dayUseDefaultHours(5)
                    .build();
            ReflectionTestUtils.setField(property, "id", 1L);

            when(helper.findPropertyByCode("GMP")).thenReturn(property);
            when(roomTypeRepository.findAllByPropertyIdOrderBySortOrderAscRoomTypeCodeAsc(1L))
                    .thenReturn(List.of());
            when(rateCodeService.getAvailableRateCodes(eq(1L), any(), any(), eq(true)))
                    .thenReturn(List.of());

            // when
            var request = new com.hola.reservation.booking.dto.request.BookingSearchRequest();
            request.setCheckIn(LocalDate.now().plusDays(10));
            request.setCheckOut(LocalDate.now().plusDays(11));
            request.setAdults(2);
            searchService.searchAvailability("GMP", request);

            // then: dayUseEnabled=true가 전달되었는지 검증
            verify(rateCodeService).getAvailableRateCodes(eq(1L), any(), any(), eq(true));
        }

        @Test
        @DisplayName("프로퍼티 dayUseEnabled=null (미설정) → false로 처리")
        void dayUseNull_filtersFalse() {
            // given: dayUseEnabled를 명시하지 않은 프로퍼티 (기본값 false)
            Property property = Property.builder()
                    .dayUseStartTime("10:00")
                    .dayUseEndTime("20:00")
                    .dayUseDefaultHours(5)
                    .build();
            ReflectionTestUtils.setField(property, "id", 1L);

            when(helper.findPropertyByCode("GMP")).thenReturn(property);
            when(roomTypeRepository.findAllByPropertyIdOrderBySortOrderAscRoomTypeCodeAsc(1L))
                    .thenReturn(List.of());
            when(rateCodeService.getAvailableRateCodes(eq(1L), any(), any(), eq(false)))
                    .thenReturn(List.of());

            // when
            var request = new com.hola.reservation.booking.dto.request.BookingSearchRequest();
            request.setCheckIn(LocalDate.now().plusDays(10));
            request.setCheckOut(LocalDate.now().plusDays(11));
            request.setAdults(2);
            searchService.searchAvailability("GMP", request);

            // then: null은 Boolean.TRUE.equals()에서 false → false 전달
            verify(rateCodeService).getAvailableRateCodes(eq(1L), any(), any(), eq(false));
        }
    }

    @Nested
    @DisplayName("BookingCreationServiceImpl - Dayuse 비허용 시 조기 차단")
    class CreationBlockTest {

        @InjectMocks
        private BookingCreationServiceImpl creationService;

        @Mock private BookingHelper helper;
        @Mock private RateCodeRepository rateCodeRepository;
        @Mock private DayUseRateRepository dayUseRateRepository;
        @Mock private RoomAvailabilityService roomAvailabilityService;
        @Mock private com.hola.reservation.service.PriceCalculationService priceCalculationService;
        @Mock private com.hola.reservation.service.ReservationNumberGenerator numberGenerator;
        @Mock private com.hola.reservation.service.ReservationPaymentService paymentService;
        @Mock private com.hola.reservation.repository.MasterReservationRepository masterReservationRepository;
        @Mock private com.hola.reservation.repository.SubReservationRepository subReservationRepository;
        @Mock private com.hola.reservation.repository.DailyChargeRepository dailyChargeRepository;
        @Mock private com.hola.reservation.repository.ReservationServiceItemRepository reservationServiceItemRepository;
        @Mock private com.hola.room.repository.PaidServiceOptionRepository paidServiceOptionRepository;
        @Mock private com.hola.hotel.repository.ReservationChannelRepository reservationChannelRepository;
        @Mock private com.hola.reservation.booking.repository.BookingAuditLogRepository bookingAuditLogRepository;
        @Mock private com.hola.reservation.booking.gateway.PaymentGateway paymentGateway;
        @Mock private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

        @Test
        @DisplayName("Dayuse 비허용 프로퍼티에서 Dayuse 레이트로 예약 시 DAY_USE_NOT_ENABLED 에러")
        void dayUseDisabled_throwsNotEnabled() {
            // given
            Property property = Property.builder()
                    .dayUseEnabled(false)
                    .dayUseStartTime("10:00")
                    .dayUseEndTime("20:00")
                    .dayUseDefaultHours(5)
                    .build();
            ReflectionTestUtils.setField(property, "id", 1L);

            // Dayuse 레이트코드 목 설정
            RateCode dayUseRate = RateCode.builder()
                    .stayType(StayType.DAY_USE)
                    .build();
            ReflectionTestUtils.setField(dayUseRate, "id", 100L);

            when(helper.findPropertyByCode("GMP")).thenReturn(property);
            when(rateCodeRepository.findById(100L)).thenReturn(Optional.of(dayUseRate));

            // 채널 목
            var channel = com.hola.hotel.entity.ReservationChannel.builder()
                    .channelCode("WEBSITE")
                    .channelName("웹사이트")
                    .build();
            when(reservationChannelRepository.findByPropertyIdAndChannelCode(1L, "WEBSITE"))
                    .thenReturn(Optional.of(channel));

            // 멱등성 키 체크 목
            when(bookingAuditLogRepository.findByIdempotencyKeyAndEventType(any(), eq("BOOKING_CREATED")))
                    .thenReturn(Optional.empty());

            // 가용 객실 있음 → DAY_USE_NOT_ENABLED 체크까지 도달
            when(roomAvailabilityService.getAvailableRoomCount(any(), any(), any()))
                    .thenReturn(5);

            // 예약 요청 구성
            BookingCreateRequest request = new BookingCreateRequest();
            request.setIdempotencyKey("test-key-001");
            request.setAgreedTerms(true);

            BookingCreateRequest.RoomSelection room = new BookingCreateRequest.RoomSelection();
            room.setCheckIn(LocalDate.now().plusDays(10));
            room.setCheckOut(LocalDate.now().plusDays(11));
            room.setRoomTypeId(1L);
            room.setRateCodeId(100L);
            room.setAdults(2);
            request.setRooms(List.of(room));

            BookingCreateRequest.GuestInfo guest = new BookingCreateRequest.GuestInfo();
            guest.setGuestNameKo("테스트");
            guest.setPhoneNumber("01012345678");
            guest.setEmail("test@test.com");
            request.setGuest(guest);

            BookingCreateRequest.PaymentInfo payment = new BookingCreateRequest.PaymentInfo();
            payment.setMethod("CARD");
            request.setPayment(payment);

            // when/then: DAY_USE_NOT_ENABLED 에러 발생
            assertThatThrownBy(() -> creationService.validateBookingRequest("GMP", request))
                    .isInstanceOf(HolaException.class)
                    .satisfies(e -> {
                        HolaException he = (HolaException) e;
                        assertThat(he.getErrorCode()).isEqualTo(ErrorCode.DAY_USE_NOT_ENABLED);
                    });
        }
    }
}
