package com.hola.reservation.booking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.hotel.entity.CancellationFee;
import com.hola.hotel.entity.Hotel;
import com.hola.hotel.entity.Property;
import com.hola.hotel.entity.ReservationChannel;
import com.hola.hotel.repository.CancellationFeeRepository;
import com.hola.hotel.repository.PropertyRepository;
import com.hola.hotel.repository.ReservationChannelRepository;
import com.hola.rate.dto.response.RateCodeListResponse;
import com.hola.rate.dto.response.RateCodeResponse;
import com.hola.rate.entity.RateCode;
import com.hola.rate.entity.RateCodeRoomType;
import com.hola.rate.repository.RateCodeRepository;
import com.hola.rate.repository.RateCodeRoomTypeRepository;
import com.hola.rate.service.RateCodeService;
import com.hola.reservation.booking.dto.request.BookingCreateRequest;
import com.hola.reservation.booking.dto.request.BookingSearchRequest;
import com.hola.reservation.booking.dto.request.PriceCheckRequest;
import com.hola.reservation.booking.dto.response.*;
import com.hola.reservation.booking.entity.BookingAuditLog;
import com.hola.reservation.booking.gateway.PaymentGateway;
import com.hola.reservation.booking.gateway.PaymentResult;
import com.hola.reservation.booking.repository.BookingAuditLogRepository;
import com.hola.reservation.entity.DailyCharge;
import com.hola.reservation.entity.MasterReservation;
import com.hola.reservation.entity.PaymentTransaction;
import com.hola.reservation.entity.ReservationPayment;
import com.hola.reservation.entity.SubReservation;
import com.hola.reservation.dto.response.PaymentSummaryResponse;
import com.hola.reservation.dto.response.PaymentTransactionResponse;
import com.hola.reservation.repository.DailyChargeRepository;
import com.hola.reservation.repository.MasterReservationRepository;
import com.hola.reservation.repository.PaymentTransactionRepository;
import com.hola.reservation.repository.ReservationPaymentRepository;
import com.hola.reservation.repository.ReservationServiceItemRepository;
import com.hola.reservation.repository.SubReservationRepository;
import com.hola.rate.repository.RateCodePaidServiceRepository;
import com.hola.reservation.service.PriceCalculationService;
import com.hola.reservation.service.ReservationNumberGenerator;
import com.hola.reservation.service.ReservationPaymentService;
import com.hola.reservation.service.RoomAvailabilityService;
import com.hola.room.entity.RoomClass;
import com.hola.room.entity.RoomType;
import com.hola.room.repository.FreeServiceOptionRepository;
import com.hola.room.repository.PaidServiceOptionRepository;
import com.hola.room.repository.RoomClassRepository;
import com.hola.room.repository.RoomTypeFreeServiceRepository;
import com.hola.room.repository.RoomTypeRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * BookingServiceImpl 단위 테스트
 * - Mockito 기반, 외부 의존성 전부 Mock
 */
@DisplayName("BookingServiceImpl - 부킹엔진 서비스")
@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @InjectMocks
    private BookingServiceImpl bookingService;

    @Mock private PropertyRepository propertyRepository;
    @Mock private RoomTypeRepository roomTypeRepository;
    @Mock private RoomClassRepository roomClassRepository;
    @Mock private RoomTypeFreeServiceRepository roomTypeFreeServiceRepository;
    @Mock private FreeServiceOptionRepository freeServiceOptionRepository;
    @Mock private RateCodeService rateCodeService;
    @Mock private RateCodeRepository rateCodeRepository;
    @Mock private RateCodeRoomTypeRepository rateCodeRoomTypeRepository;
    @Mock private RoomAvailabilityService roomAvailabilityService;
    @Mock private PriceCalculationService priceCalculationService;
    @Mock private MasterReservationRepository masterReservationRepository;
    @Mock private SubReservationRepository subReservationRepository;
    @Mock private DailyChargeRepository dailyChargeRepository;
    @Mock private ReservationChannelRepository reservationChannelRepository;
    @Mock private ReservationNumberGenerator reservationNumberGenerator;
    @Mock private ReservationPaymentService reservationPaymentService;
    @Mock private PaymentGateway paymentGateway;
    @Mock private BookingAuditLogRepository bookingAuditLogRepository;
    @Mock private ObjectMapper objectMapper;
    @Mock private com.hola.reservation.service.RateIncludedServiceHelper rateIncludedServiceHelper;
    @Mock private ReservationServiceItemRepository reservationServiceItemRepository;
    @Mock private RateCodePaidServiceRepository rateCodePaidServiceRepository;
    @Mock private com.hola.rate.repository.DayUseRateRepository dayUseRateRepository;
    @Mock private CancellationPolicyService cancellationPolicyService;
    @Mock private CancellationFeeRepository cancellationFeeRepository;
    @Mock private ReservationPaymentRepository reservationPaymentRepository;
    @Mock private PaymentTransactionRepository paymentTransactionRepository;
    @Mock private com.hola.room.service.InventoryService inventoryService;
    @Mock private com.hola.hotel.repository.PropertyImageRepository propertyImageRepository;
    @Mock private com.hola.hotel.repository.PropertyTermsRepository propertyTermsRepository;
    @Mock private PaidServiceOptionRepository paidServiceOptionRepository;
    @Mock private com.hola.rate.repository.PromotionCodeRepository promotionCodeRepository;

    // 공통 테스트 데이터
    private static final String PROPERTY_CODE = "GMP";
    private static final Long PROPERTY_ID = 1L;
    private static final Long ROOM_TYPE_ID = 10L;
    private static final Long RATE_CODE_ID = 100L;
    private static final String CONFIRMATION_NO = "HK4F29XP";
    private static final String GUEST_EMAIL = "guest@test.com";
    private static final String GUEST_PHONE = "01012345678";

    private Property property;
    private Hotel hotel;

    @BeforeEach
    void setUp() {
        hotel = Hotel.builder()
                .hotelCode("HOLA")
                .hotelName("올라호텔")
                .build();
        ReflectionTestUtils.setField(hotel, "id", 1L);

        property = Property.builder()
                .hotel(hotel)
                .propertyCode(PROPERTY_CODE)
                .propertyName("올라 강남")
                .propertyType("HOTEL")
                .starRating("5")
                .checkInTime("15:00")
                .checkOutTime("11:00")
                .phone("02-1234-5678")
                .email("info@hola.com")
                .address("서울시 강남구")
                .build();
        ReflectionTestUtils.setField(property, "id", PROPERTY_ID);

        // Phase 1에서 추가된 벌크 조회 메서드 기본 스텁 (NPE 방지)
        lenient().when(rateCodeRoomTypeRepository.findAllByRateCodeIdIn(any()))
                .thenReturn(Collections.emptyList());
        lenient().when(rateCodePaidServiceRepository.findAllByRateCodeIdIn(any()))
                .thenReturn(Collections.emptyList());
        lenient().when(reservationServiceItemRepository.findBySubReservationId(any()))
                .thenReturn(Collections.emptyList());
    }

    // ===== 헬퍼 메서드 =====

    /** 테스트용 RoomType 생성 */
    private RoomType createRoomType(Long id, int maxAdults, int maxChildren, boolean useYn) {
        RoomType rt = RoomType.builder()
                .propertyId(PROPERTY_ID)
                .roomClassId(1L)
                .roomTypeCode("DLX")
                .maxAdults(maxAdults)
                .maxChildren(maxChildren)
                .build();
        ReflectionTestUtils.setField(rt, "id", id);
        ReflectionTestUtils.setField(rt, "useYn", useYn);
        return rt;
    }

    /** 테스트용 DailyCharge 생성 */
    private DailyCharge createDailyCharge(LocalDate date, BigDecimal supply, BigDecimal tax,
                                          BigDecimal svcCharge, BigDecimal total) {
        return DailyCharge.builder()
                .chargeDate(date)
                .supplyPrice(supply)
                .tax(tax)
                .serviceCharge(svcCharge)
                .total(total)
                .build();
    }

    /** 테스트용 BookingSearchRequest 생성 */
    private BookingSearchRequest createSearchRequest(LocalDate checkIn, LocalDate checkOut,
                                                     int adults, Integer children) {
        BookingSearchRequest req = new BookingSearchRequest();
        req.setCheckIn(checkIn);
        req.setCheckOut(checkOut);
        req.setAdults(adults);
        req.setChildren(children);
        return req;
    }

    /** 테스트용 BookingCreateRequest 생성 */
    private BookingCreateRequest createBookingRequest(LocalDate checkIn, LocalDate checkOut,
                                                      boolean agreedTerms) {
        BookingCreateRequest req = new BookingCreateRequest();
        req.setIdempotencyKey("idem-key-001");
        req.setAgreedTerms(agreedTerms);

        BookingCreateRequest.GuestInfo guest = new BookingCreateRequest.GuestInfo();
        guest.setGuestNameKo("홍길동");
        guest.setPhoneNumber(GUEST_PHONE);
        guest.setEmail(GUEST_EMAIL);
        req.setGuest(guest);

        BookingCreateRequest.RoomSelection room = new BookingCreateRequest.RoomSelection();
        room.setRoomTypeId(ROOM_TYPE_ID);
        room.setRateCodeId(RATE_CODE_ID);
        room.setCheckIn(checkIn);
        room.setCheckOut(checkOut);
        room.setAdults(2);
        room.setChildren(0);
        req.setRooms(List.of(room));

        BookingCreateRequest.PaymentInfo pay = new BookingCreateRequest.PaymentInfo();
        pay.setMethod("CARD");
        pay.setCardNumber("4111111111111111");
        pay.setExpiryDate("12/28");
        pay.setCvv("123");
        req.setPayment(pay);

        return req;
    }

    /** 테스트용 MasterReservation 생성 */
    private MasterReservation createMasterReservation(String status, String email) {
        MasterReservation master = MasterReservation.builder()
                .property(property)
                .masterReservationNo("GMP260315-0001")
                .confirmationNo(CONFIRMATION_NO)
                .reservationStatus(status)
                .masterCheckIn(LocalDate.now().plusDays(7))
                .masterCheckOut(LocalDate.now().plusDays(9))
                .guestNameKo("홍길동")
                .phoneNumber(GUEST_PHONE)
                .email(email)
                .build();
        ReflectionTestUtils.setField(master, "id", 1L);
        ReflectionTestUtils.setField(master, "createdAt", LocalDateTime.now());
        return master;
    }

    // ===== 1. 프로퍼티 정보 조회 =====

    @Nested
    @DisplayName("1. 프로퍼티 정보 조회")
    class GetPropertyInfo {

        @Test
        @DisplayName("정상 조회 - 프로퍼티 정보 반환")
        void getPropertyInfo_success() {
            when(propertyRepository.findByPropertyCodeAndUseYnTrue(PROPERTY_CODE))
                    .thenReturn(Optional.of(property));

            PropertyInfoResponse result = bookingService.getPropertyInfo(PROPERTY_CODE);

            assertThat(result).isNotNull();
            assertThat(result.getPropertyId()).isEqualTo(PROPERTY_ID);
            assertThat(result.getPropertyCode()).isEqualTo(PROPERTY_CODE);
            assertThat(result.getPropertyName()).isEqualTo("올라 강남");
            assertThat(result.getHotelName()).isEqualTo("올라호텔");
            assertThat(result.getStarRating()).isEqualTo(5);
            assertThat(result.getCheckInTime()).isEqualTo("15:00");
        }

        @Test
        @DisplayName("존재하지 않는 프로퍼티 코드 - BOOKING_PROPERTY_NOT_FOUND")
        void getPropertyInfo_notFound() {
            when(propertyRepository.findByPropertyCodeAndUseYnTrue("INVALID"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookingService.getPropertyInfo("INVALID"))
                    .isInstanceOf(HolaException.class)
                    .extracting(e -> ((HolaException) e).getErrorCode())
                    .isEqualTo(ErrorCode.BOOKING_PROPERTY_NOT_FOUND);
        }
    }

    // ===== 2. 객실 검색 =====

    @Nested
    @DisplayName("2. 객실 가용성 검색")
    class SearchAvailability {

        private LocalDate checkIn;
        private LocalDate checkOut;

        @BeforeEach
        void setUp() {
            checkIn = LocalDate.now().plusDays(3);
            checkOut = LocalDate.now().plusDays(5);
        }

        @Test
        @DisplayName("인원 조건 필터링 - maxAdults 미달 객실 제외")
        void searchAvailability_filterByOccupancy() {
            // 성인 3명 요청 → maxAdults=2인 객실 제외, maxAdults=4인 객실만 반환
            RoomType smallRoom = createRoomType(10L, 2, 1, true);
            RoomType largeRoom = createRoomType(11L, 4, 2, true);

            when(propertyRepository.findByPropertyCodeAndUseYnTrue(PROPERTY_CODE))
                    .thenReturn(Optional.of(property));
            when(roomTypeRepository.findAllByPropertyIdOrderBySortOrderAscRoomTypeCodeAsc(PROPERTY_ID))
                    .thenReturn(List.of(smallRoom, largeRoom));
            when(rateCodeService.getAvailableRateCodes(eq(PROPERTY_ID), any(), any()))
                    .thenReturn(List.of());
            when(roomAvailabilityService.getAvailableRoomCount(eq(11L), any(), any()))
                    .thenReturn(3);

            BookingSearchRequest req = createSearchRequest(checkIn, checkOut, 3, 0);
            List<AvailableRoomTypeResponse> result = bookingService.searchAvailability(PROPERTY_CODE, req);

            // largeRoom은 레이트 옵션이 없으므로 결과에 포함 안 됨 (rateOptions empty → skip)
            // 핵심: getAvailableRoomCount가 smallRoom(id=10)에 대해 호출되지 않음
            verify(roomAvailabilityService, never()).getAvailableRoomCount(eq(10L), any(), any());
        }

        @Test
        @DisplayName("가용 객실 0 - 해당 객실타입 결과에서 제외")
        void searchAvailability_excludeUnavailable() {
            RoomType roomType = createRoomType(ROOM_TYPE_ID, 2, 1, true);

            when(propertyRepository.findByPropertyCodeAndUseYnTrue(PROPERTY_CODE))
                    .thenReturn(Optional.of(property));
            when(roomTypeRepository.findAllByPropertyIdOrderBySortOrderAscRoomTypeCodeAsc(PROPERTY_ID))
                    .thenReturn(List.of(roomType));
            when(rateCodeService.getAvailableRateCodes(eq(PROPERTY_ID), any(), any()))
                    .thenReturn(List.of());
            when(roomAvailabilityService.getAvailableRoomCount(eq(ROOM_TYPE_ID), any(), any()))
                    .thenReturn(0);

            BookingSearchRequest req = createSearchRequest(checkIn, checkOut, 2, 0);
            List<AvailableRoomTypeResponse> result = bookingService.searchAvailability(PROPERTY_CODE, req);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("매핑된 레이트코드 없음 - 결과에서 제외")
        void searchAvailability_excludeNoRate() {
            RoomType roomType = createRoomType(ROOM_TYPE_ID, 2, 1, true);
            RateCodeListResponse rateCode = RateCodeListResponse.builder()
                    .id(RATE_CODE_ID)
                    .rateCode("BAR")
                    .rateNameKo("기본요금")
                    .currency("KRW")
                    .build();

            when(propertyRepository.findByPropertyCodeAndUseYnTrue(PROPERTY_CODE))
                    .thenReturn(Optional.of(property));
            when(roomTypeRepository.findAllByPropertyIdOrderBySortOrderAscRoomTypeCodeAsc(PROPERTY_ID))
                    .thenReturn(List.of(roomType));
            when(rateCodeService.getAvailableRateCodes(eq(PROPERTY_ID), any(), any()))
                    .thenReturn(List.of(rateCode));
            when(roomAvailabilityService.getAvailableRoomCount(eq(ROOM_TYPE_ID), any(), any()))
                    .thenReturn(5);
            // 레이트코드-객실타입 매핑 없음 (벌크 조회)
            when(rateCodeRoomTypeRepository.findAllByRateCodeIdIn(any()))
                    .thenReturn(Collections.emptyList());

            BookingSearchRequest req = createSearchRequest(checkIn, checkOut, 2, 0);
            List<AvailableRoomTypeResponse> result = bookingService.searchAvailability(PROPERTY_CODE, req);

            // rateOptions가 비어있으면 결과에서 제외
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("요금 계산 실패 시 해당 레이트 옵션 건너뜀")
        void searchAvailability_skipCalcFailure() {
            RoomType roomType = createRoomType(ROOM_TYPE_ID, 2, 1, true);
            RateCodeListResponse rateCode = RateCodeListResponse.builder()
                    .id(RATE_CODE_ID)
                    .rateCode("BAR")
                    .rateNameKo("기본요금")
                    .currency("KRW")
                    .build();
            RateCodeRoomType mapping = RateCodeRoomType.builder()
                    .rateCodeId(RATE_CODE_ID)
                    .roomTypeId(ROOM_TYPE_ID)
                    .build();

            when(propertyRepository.findByPropertyCodeAndUseYnTrue(PROPERTY_CODE))
                    .thenReturn(Optional.of(property));
            when(roomTypeRepository.findAllByPropertyIdOrderBySortOrderAscRoomTypeCodeAsc(PROPERTY_ID))
                    .thenReturn(List.of(roomType));
            when(rateCodeService.getAvailableRateCodes(eq(PROPERTY_ID), any(), any()))
                    .thenReturn(List.of(rateCode));
            when(roomAvailabilityService.getAvailableRoomCount(eq(ROOM_TYPE_ID), any(), any()))
                    .thenReturn(5);
            // 벌크 조회로 매핑 반환
            when(rateCodeRoomTypeRepository.findAllByRateCodeIdIn(any()))
                    .thenReturn(List.of(mapping));
            // 요금 계산 시 예외 발생
            when(priceCalculationService.calculateDailyCharges(eq(RATE_CODE_ID), eq(property),
                    any(), any(), anyInt(), anyInt(), isNull()))
                    .thenThrow(new HolaException(ErrorCode.RESERVATION_RATE_NOT_APPLICABLE));

            BookingSearchRequest req = createSearchRequest(checkIn, checkOut, 2, 0);
            List<AvailableRoomTypeResponse> result = bookingService.searchAvailability(PROPERTY_CODE, req);

            // 요금 계산 실패 → rateOptions 비어있음 → 결과에서 제외
            assertThat(result).isEmpty();
        }
    }

    // ===== 3. 날짜 유효성 검증 =====

    @Nested
    @DisplayName("3. 날짜 유효성 검증")
    class DateValidation {

        @Test
        @DisplayName("유효한 날짜 범위 - 정상 처리")
        void validateDateRange_valid() {
            LocalDate checkIn = LocalDate.now().plusDays(1);
            LocalDate checkOut = LocalDate.now().plusDays(3);

            when(propertyRepository.findByPropertyCodeAndUseYnTrue(PROPERTY_CODE))
                    .thenReturn(Optional.of(property));
            when(roomTypeRepository.findAllByPropertyIdOrderBySortOrderAscRoomTypeCodeAsc(PROPERTY_ID))
                    .thenReturn(Collections.emptyList());
            when(rateCodeService.getAvailableRateCodes(eq(PROPERTY_ID), any(), any()))
                    .thenReturn(Collections.emptyList());

            BookingSearchRequest req = createSearchRequest(checkIn, checkOut, 2, 0);

            // 날짜 검증 통과 (빈 결과이지만 예외 없음)
            List<AvailableRoomTypeResponse> result = bookingService.searchAvailability(PROPERTY_CODE, req);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("체크인/체크아웃 역전 - BOOKING_INVALID_DATE_RANGE")
        void validateDateRange_reversed() {
            LocalDate checkIn = LocalDate.now().plusDays(5);
            LocalDate checkOut = LocalDate.now().plusDays(3);

            BookingSearchRequest req = createSearchRequest(checkIn, checkOut, 2, 0);

            assertThatThrownBy(() -> bookingService.searchAvailability(PROPERTY_CODE, req))
                    .isInstanceOf(HolaException.class)
                    .extracting(e -> ((HolaException) e).getErrorCode())
                    .isEqualTo(ErrorCode.BOOKING_INVALID_DATE_RANGE);
        }

        @Test
        @DisplayName("과거 체크인 날짜 - BOOKING_INVALID_DATE_RANGE")
        void validateDateRange_pastCheckIn() {
            LocalDate checkIn = LocalDate.now().minusDays(1);
            LocalDate checkOut = LocalDate.now().plusDays(2);

            BookingSearchRequest req = createSearchRequest(checkIn, checkOut, 2, 0);

            assertThatThrownBy(() -> bookingService.searchAvailability(PROPERTY_CODE, req))
                    .isInstanceOf(HolaException.class)
                    .extracting(e -> ((HolaException) e).getErrorCode())
                    .isEqualTo(ErrorCode.BOOKING_INVALID_DATE_RANGE);
        }

        @Test
        @DisplayName("31박 초과 - BOOKING_MAX_STAY_EXCEEDED")
        void validateDateRange_over30Nights() {
            LocalDate checkIn = LocalDate.now().plusDays(1);
            LocalDate checkOut = checkIn.plusDays(31);

            BookingSearchRequest req = createSearchRequest(checkIn, checkOut, 2, 0);

            assertThatThrownBy(() -> bookingService.searchAvailability(PROPERTY_CODE, req))
                    .isInstanceOf(HolaException.class)
                    .extracting(e -> ((HolaException) e).getErrorCode())
                    .isEqualTo(ErrorCode.BOOKING_MAX_STAY_EXCEEDED);
        }

        @Test
        @DisplayName("정확히 30박 - 정상 처리")
        void validateDateRange_exactly30Nights() {
            LocalDate checkIn = LocalDate.now().plusDays(1);
            LocalDate checkOut = checkIn.plusDays(30);

            when(propertyRepository.findByPropertyCodeAndUseYnTrue(PROPERTY_CODE))
                    .thenReturn(Optional.of(property));
            when(roomTypeRepository.findAllByPropertyIdOrderBySortOrderAscRoomTypeCodeAsc(PROPERTY_ID))
                    .thenReturn(Collections.emptyList());
            when(rateCodeService.getAvailableRateCodes(eq(PROPERTY_ID), any(), any()))
                    .thenReturn(Collections.emptyList());

            BookingSearchRequest req = createSearchRequest(checkIn, checkOut, 2, 0);

            // 30박은 MAX_STAY_NIGHTS 이하이므로 정상
            List<AvailableRoomTypeResponse> result = bookingService.searchAvailability(PROPERTY_CODE, req);
            assertThat(result).isNotNull();
        }
    }

    // ===== 4. 요금/가격 계산 =====

    @Nested
    @DisplayName("4. 요금/가격 계산")
    class RatePrice {

        @Test
        @DisplayName("일별 요금 합산 정상 계산")
        void calculatePrice_dailySum() {
            LocalDate checkIn = LocalDate.now().plusDays(3);
            LocalDate checkOut = LocalDate.now().plusDays(5);

            PriceCheckRequest req = new PriceCheckRequest();
            req.setRoomTypeId(ROOM_TYPE_ID);
            req.setRateCodeId(RATE_CODE_ID);
            req.setCheckIn(checkIn);
            req.setCheckOut(checkOut);
            req.setAdults(2);
            req.setChildren(0);

            RoomType roomType = createRoomType(ROOM_TYPE_ID, 2, 1, true);
            RateCodeRoomType mapping = RateCodeRoomType.builder()
                    .rateCodeId(RATE_CODE_ID)
                    .roomTypeId(ROOM_TYPE_ID)
                    .build();

            DailyCharge dc1 = createDailyCharge(checkIn, new BigDecimal("100000"),
                    new BigDecimal("10000"), new BigDecimal("5000"), new BigDecimal("115000"));
            DailyCharge dc2 = createDailyCharge(checkIn.plusDays(1), new BigDecimal("120000"),
                    new BigDecimal("12000"), new BigDecimal("6000"), new BigDecimal("138000"));

            RoomClass roomClass = RoomClass.builder()
                    .propertyId(PROPERTY_ID)
                    .roomClassCode("DLX")
                    .roomClassName("디럭스")
                    .build();

            // Dayuse 여부 확인용 레이트코드 (Overnight)
            RateCode overnightRate = RateCode.builder()
                    .propertyId(PROPERTY_ID).rateCode("BAR").rateNameKo("기본요금")
                    .currency("KRW").minStayDays(1).maxStayDays(30).build();
            ReflectionTestUtils.setField(overnightRate, "id", RATE_CODE_ID);

            when(propertyRepository.findByPropertyCodeAndUseYnTrue(PROPERTY_CODE))
                    .thenReturn(Optional.of(property));
            when(roomTypeRepository.findById(ROOM_TYPE_ID))
                    .thenReturn(Optional.of(roomType));
            when(rateCodeRoomTypeRepository.findAllByRateCodeId(RATE_CODE_ID))
                    .thenReturn(List.of(mapping));
            when(rateCodeRepository.findById(RATE_CODE_ID))
                    .thenReturn(Optional.of(overnightRate));
            when(priceCalculationService.calculateDailyCharges(eq(RATE_CODE_ID), eq(property),
                    eq(checkIn), eq(checkOut), eq(2), eq(0), isNull()))
                    .thenReturn(List.of(dc1, dc2));
            when(roomClassRepository.findById(1L))
                    .thenReturn(Optional.of(roomClass));
            when(rateCodeService.getRateCode(RATE_CODE_ID))
                    .thenReturn(RateCodeResponse.builder()
                            .id(RATE_CODE_ID).rateCode("BAR").rateNameKo("기본요금").currency("KRW").build());

            PriceCheckResponse result = bookingService.calculatePrice(PROPERTY_CODE, req);

            assertThat(result.getGrandTotal()).isEqualByComparingTo(new BigDecimal("253000"));
            assertThat(result.getTotalSupply()).isEqualByComparingTo(new BigDecimal("220000"));
            assertThat(result.getTotalTax()).isEqualByComparingTo(new BigDecimal("22000"));
            assertThat(result.getTotalServiceCharge()).isEqualByComparingTo(new BigDecimal("11000"));
            assertThat(result.getDailyCharges()).hasSize(2);
        }

        @Test
        @DisplayName("매핑되지 않은 레이트코드 - BOOKING_RATE_NOT_AVAILABLE")
        void calculatePrice_unmappedRate() {
            LocalDate checkIn = LocalDate.now().plusDays(3);
            LocalDate checkOut = LocalDate.now().plusDays(5);

            PriceCheckRequest req = new PriceCheckRequest();
            req.setRoomTypeId(ROOM_TYPE_ID);
            req.setRateCodeId(RATE_CODE_ID);
            req.setCheckIn(checkIn);
            req.setCheckOut(checkOut);
            req.setAdults(2);
            req.setChildren(0);

            RoomType roomType = createRoomType(ROOM_TYPE_ID, 2, 1, true);

            when(propertyRepository.findByPropertyCodeAndUseYnTrue(PROPERTY_CODE))
                    .thenReturn(Optional.of(property));
            when(roomTypeRepository.findById(ROOM_TYPE_ID))
                    .thenReturn(Optional.of(roomType));
            // 매핑 목록에 해당 roomTypeId 없음
            RateCodeRoomType otherMapping = RateCodeRoomType.builder()
                    .rateCodeId(RATE_CODE_ID)
                    .roomTypeId(999L) // 다른 객실타입
                    .build();
            when(rateCodeRoomTypeRepository.findAllByRateCodeId(RATE_CODE_ID))
                    .thenReturn(List.of(otherMapping));

            assertThatThrownBy(() -> bookingService.calculatePrice(PROPERTY_CODE, req))
                    .isInstanceOf(HolaException.class)
                    .extracting(e -> ((HolaException) e).getErrorCode())
                    .isEqualTo(ErrorCode.BOOKING_RATE_NOT_AVAILABLE);
        }
    }

    // ===== 5. 예약 생성 =====

    @Nested
    @DisplayName("5. 예약 생성")
    class CreateBooking {

        private LocalDate checkIn;
        private LocalDate checkOut;

        @BeforeEach
        void setUp() {
            checkIn = LocalDate.now().plusDays(3);
            checkOut = LocalDate.now().plusDays(5);
        }

        @Test
        @DisplayName("정상 예약 생성 - 전체 플로우")
        void createBooking_fullFlow() throws Exception {
            BookingCreateRequest req = createBookingRequest(checkIn, checkOut, true);

            RateCode rateCode = RateCode.builder()
                    .propertyId(PROPERTY_ID)
                    .rateCode("BAR")
                    .rateNameKo("기본요금")
                    .currency("KRW")
                    .minStayDays(1)
                    .maxStayDays(30)
                    .build();
            ReflectionTestUtils.setField(rateCode, "id", RATE_CODE_ID);

            DailyCharge dc1 = createDailyCharge(checkIn, new BigDecimal("100000"),
                    new BigDecimal("10000"), new BigDecimal("5000"), new BigDecimal("115000"));
            DailyCharge dc2 = createDailyCharge(checkIn.plusDays(1), new BigDecimal("100000"),
                    new BigDecimal("10000"), new BigDecimal("5000"), new BigDecimal("115000"));

            MasterReservation savedMaster = createMasterReservation("RESERVED", GUEST_EMAIL);
            SubReservation savedSub = SubReservation.builder()
                    .masterReservation(savedMaster)
                    .subReservationNo("GMP260315-0001-01")
                    .roomTypeId(ROOM_TYPE_ID)
                    .adults(2)
                    .children(0)
                    .checkIn(checkIn)
                    .checkOut(checkOut)
                    .build();
            ReflectionTestUtils.setField(savedSub, "id", 1L);

            // 스텁 설정
            when(bookingAuditLogRepository.findByIdempotencyKeyAndEventType(any(), eq("BOOKING_CREATED")))
                    .thenReturn(Optional.empty());
            when(propertyRepository.findByPropertyCodeAndUseYnTrue(PROPERTY_CODE))
                    .thenReturn(Optional.of(property));
            when(reservationChannelRepository.findByPropertyIdAndChannelCode(PROPERTY_ID, "WEBSITE"))
                    .thenReturn(Optional.of(ReservationChannel.builder()
                            .property(property).channelCode("WEBSITE").channelName("웹사이트")
                            .channelType("DIRECT").build()));
            when(rateCodeRepository.findById(RATE_CODE_ID))
                    .thenReturn(Optional.of(rateCode));
            when(roomAvailabilityService.getAvailableRoomCount(eq(ROOM_TYPE_ID), any(), any()))
                    .thenReturn(5);
            when(roomAvailabilityService.getAvailableRoomCountWithLock(eq(ROOM_TYPE_ID), any(), any()))
                    .thenReturn(5);
            when(priceCalculationService.calculateDailyCharges(eq(RATE_CODE_ID), eq(property),
                    any(), any(), anyInt(), anyInt(), isNull()))
                    .thenReturn(List.of(dc1, dc2));
            when(paymentGateway.authorize(any()))
                    .thenReturn(PaymentResult.success("MOCK-12345678", "MOCK", new BigDecimal("230000")));
            when(reservationNumberGenerator.generateMasterReservationNo(property))
                    .thenReturn("GMP260315-0001");
            when(reservationNumberGenerator.generateConfirmationNo())
                    .thenReturn(CONFIRMATION_NO);
            when(reservationNumberGenerator.generateSubReservationNo("GMP260315-0001", 1))
                    .thenReturn("GMP260315-0001-01");
            when(masterReservationRepository.save(any(MasterReservation.class)))
                    .thenReturn(savedMaster);
            when(subReservationRepository.save(any(SubReservation.class)))
                    .thenReturn(savedSub);
            when(dailyChargeRepository.saveAll(anyList()))
                    .thenAnswer(inv -> inv.getArgument(0));

            // buildConfirmationResponse용 스텁
            when(subReservationRepository.findByMasterReservationId(1L))
                    .thenReturn(List.of(savedSub));
            when(dailyChargeRepository.findBySubReservationId(1L))
                    .thenReturn(List.of(dc1, dc2));
            when(roomTypeRepository.findById(ROOM_TYPE_ID))
                    .thenReturn(Optional.of(createRoomType(ROOM_TYPE_ID, 2, 1, true)));
            when(roomClassRepository.findById(1L))
                    .thenReturn(Optional.of(RoomClass.builder()
                            .propertyId(PROPERTY_ID).roomClassCode("DLX").roomClassName("디럭스").build()));
            when(reservationPaymentService.getPaymentSummary(PROPERTY_ID, 1L))
                    .thenReturn(PaymentSummaryResponse.builder()
                            .paymentStatus("PAID")
                            .transactions(List.of(PaymentTransactionResponse.builder()
                                    .paymentMethod("CARD")
                                    .memo("부킹엔진 결제 - 승인번호: MOCK-12345678")
                                    .build()))
                            .build());
            when(cancellationFeeRepository.findAllByPropertyIdOrderBySortOrder(PROPERTY_ID))
                    .thenReturn(Collections.emptyList());
            when(objectMapper.writeValueAsString(any()))
                    .thenReturn("{}");

            BookingConfirmationResponse result = bookingService.createBooking(PROPERTY_CODE, req,
                    "127.0.0.1", "TestAgent");

            assertThat(result).isNotNull();
            assertThat(result.getConfirmationNo()).isEqualTo(CONFIRMATION_NO);
            assertThat(result.getReservationStatus()).isEqualTo("RESERVED");
            assertThat(result.getGuestNameKo()).isEqualTo("홍길동");
            assertThat(result.getRooms()).hasSize(1);

            verify(masterReservationRepository).save(any(MasterReservation.class));
            verify(subReservationRepository).save(any(SubReservation.class));
            verify(reservationPaymentService).recalculatePayment(1L);
            verify(bookingAuditLogRepository).save(any(BookingAuditLog.class));
        }

        @Test
        @DisplayName("이용약관 미동의 - BOOKING_TERMS_NOT_AGREED")
        void createBooking_termsNotAgreed() {
            BookingCreateRequest req = createBookingRequest(checkIn, checkOut, false);

            assertThatThrownBy(() -> bookingService.createBooking(PROPERTY_CODE, req, "127.0.0.1", "TestAgent"))
                    .isInstanceOf(HolaException.class)
                    .extracting(e -> ((HolaException) e).getErrorCode())
                    .isEqualTo(ErrorCode.BOOKING_TERMS_NOT_AGREED);
        }

        @Test
        @DisplayName("멱등성 중복 요청 - 기존 확인 결과 반환")
        void createBooking_idempotentDuplicate() {
            BookingCreateRequest req = createBookingRequest(checkIn, checkOut, true);

            MasterReservation existingMaster = createMasterReservation("RESERVED", GUEST_EMAIL);
            SubReservation existingSub = SubReservation.builder()
                    .masterReservation(existingMaster)
                    .subReservationNo("GMP260315-0001-01")
                    .roomTypeId(ROOM_TYPE_ID)
                    .adults(2).children(0)
                    .checkIn(checkIn).checkOut(checkOut)
                    .build();
            ReflectionTestUtils.setField(existingSub, "id", 1L);

            BookingAuditLog existingLog = BookingAuditLog.builder()
                    .masterReservationId(1L)
                    .confirmationNo(CONFIRMATION_NO)
                    .eventType("BOOKING_CREATED")
                    .idempotencyKey("idem-key-001")
                    .build();

            when(bookingAuditLogRepository.findByIdempotencyKeyAndEventType("idem-key-001", "BOOKING_CREATED"))
                    .thenReturn(Optional.of(existingLog));
            when(masterReservationRepository.findById(1L))
                    .thenReturn(Optional.of(existingMaster));
            when(subReservationRepository.findByMasterReservationId(1L))
                    .thenReturn(List.of(existingSub));
            when(dailyChargeRepository.findBySubReservationId(1L))
                    .thenReturn(Collections.emptyList());
            when(roomTypeRepository.findById(ROOM_TYPE_ID))
                    .thenReturn(Optional.of(createRoomType(ROOM_TYPE_ID, 2, 1, true)));
            when(roomClassRepository.findById(1L))
                    .thenReturn(Optional.of(RoomClass.builder()
                            .propertyId(PROPERTY_ID).roomClassCode("DLX").roomClassName("디럭스").build()));
            when(reservationPaymentService.getPaymentSummary(PROPERTY_ID, 1L))
                    .thenReturn(PaymentSummaryResponse.builder()
                            .paymentStatus("PAID")
                            .transactions(Collections.emptyList())
                            .build());
            when(cancellationFeeRepository.findAllByPropertyIdOrderBySortOrder(PROPERTY_ID))
                    .thenReturn(Collections.emptyList());

            BookingConfirmationResponse result = bookingService.createBooking(PROPERTY_CODE, req,
                    "127.0.0.1", "TestAgent");

            assertThat(result.getConfirmationNo()).isEqualTo(CONFIRMATION_NO);
            // 새 예약 생성 없음 확인
            verify(masterReservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("가용 객실 없음 - BOOKING_NO_AVAILABILITY")
        void createBooking_noAvailability() {
            BookingCreateRequest req = createBookingRequest(checkIn, checkOut, true);

            RateCode rateCode = RateCode.builder()
                    .propertyId(PROPERTY_ID).rateCode("BAR").rateNameKo("기본요금")
                    .currency("KRW").minStayDays(1).maxStayDays(30).build();
            ReflectionTestUtils.setField(rateCode, "id", RATE_CODE_ID);

            when(bookingAuditLogRepository.findByIdempotencyKeyAndEventType(any(), eq("BOOKING_CREATED")))
                    .thenReturn(Optional.empty());
            when(propertyRepository.findByPropertyCodeAndUseYnTrue(PROPERTY_CODE))
                    .thenReturn(Optional.of(property));
            when(reservationChannelRepository.findByPropertyIdAndChannelCode(PROPERTY_ID, "WEBSITE"))
                    .thenReturn(Optional.empty());
            when(rateCodeRepository.findById(RATE_CODE_ID))
                    .thenReturn(Optional.of(rateCode));
            when(roomAvailabilityService.getAvailableRoomCount(eq(ROOM_TYPE_ID), any(), any()))
                    .thenReturn(0);

            assertThatThrownBy(() -> bookingService.createBooking(PROPERTY_CODE, req, "127.0.0.1", "TestAgent"))
                    .isInstanceOf(HolaException.class)
                    .extracting(e -> ((HolaException) e).getErrorCode())
                    .isEqualTo(ErrorCode.BOOKING_NO_AVAILABILITY);
        }

        @Test
        @DisplayName("숙박일수 범위 위반 - RESERVATION_STAY_DAYS_VIOLATION")
        void createBooking_stayDaysViolation() {
            // 2박 요청, 최소 3박 레이트코드
            BookingCreateRequest req = createBookingRequest(checkIn, checkOut, true);

            RateCode rateCode = RateCode.builder()
                    .propertyId(PROPERTY_ID).rateCode("LONG")
                    .rateNameKo("장기체류요금").currency("KRW")
                    .minStayDays(3).maxStayDays(30).build();
            ReflectionTestUtils.setField(rateCode, "id", RATE_CODE_ID);

            when(bookingAuditLogRepository.findByIdempotencyKeyAndEventType(any(), eq("BOOKING_CREATED")))
                    .thenReturn(Optional.empty());
            when(propertyRepository.findByPropertyCodeAndUseYnTrue(PROPERTY_CODE))
                    .thenReturn(Optional.of(property));
            when(reservationChannelRepository.findByPropertyIdAndChannelCode(PROPERTY_ID, "WEBSITE"))
                    .thenReturn(Optional.empty());
            when(rateCodeRepository.findById(RATE_CODE_ID))
                    .thenReturn(Optional.of(rateCode));

            assertThatThrownBy(() -> bookingService.createBooking(PROPERTY_CODE, req, "127.0.0.1", "TestAgent"))
                    .isInstanceOf(HolaException.class)
                    .extracting(e -> ((HolaException) e).getErrorCode())
                    .isEqualTo(ErrorCode.RESERVATION_STAY_DAYS_VIOLATION);
        }
    }

    // ===== 6. 예약 확인 조회 =====

    @Nested
    @DisplayName("6. 예약 확인 조회")
    class GetConfirmation {

        @Test
        @DisplayName("이메일 또는 전화번호 일치 - 정상 반환")
        void getConfirmation_emailAndPhoneMatch() {
            MasterReservation master = createMasterReservation("RESERVED", GUEST_EMAIL);
            SubReservation sub = SubReservation.builder()
                    .masterReservation(master)
                    .subReservationNo("GMP260315-0001-01")
                    .roomTypeId(ROOM_TYPE_ID)
                    .adults(2).children(0)
                    .checkIn(LocalDate.now().plusDays(7))
                    .checkOut(LocalDate.now().plusDays(9))
                    .build();
            ReflectionTestUtils.setField(sub, "id", 1L);

            when(masterReservationRepository.findByConfirmationNo(CONFIRMATION_NO))
                    .thenReturn(Optional.of(master));
            when(subReservationRepository.findByMasterReservationId(1L))
                    .thenReturn(List.of(sub));
            when(dailyChargeRepository.findBySubReservationId(1L))
                    .thenReturn(Collections.emptyList());
            when(roomTypeRepository.findById(ROOM_TYPE_ID))
                    .thenReturn(Optional.of(createRoomType(ROOM_TYPE_ID, 2, 1, true)));
            when(roomClassRepository.findById(1L))
                    .thenReturn(Optional.of(RoomClass.builder()
                            .propertyId(PROPERTY_ID).roomClassCode("DLX").roomClassName("디럭스").build()));
            when(reservationPaymentService.getPaymentSummary(PROPERTY_ID, 1L))
                    .thenReturn(PaymentSummaryResponse.builder()
                            .paymentStatus("PAID")
                            .transactions(Collections.emptyList())
                            .build());
            when(cancellationFeeRepository.findAllByPropertyIdOrderBySortOrder(PROPERTY_ID))
                    .thenReturn(Collections.emptyList());

            // 이메일로 검증
            BookingConfirmationResponse result = bookingService.getConfirmation(CONFIRMATION_NO, GUEST_EMAIL);
            assertThat(result).isNotNull();
            assertThat(result.getConfirmationNo()).isEqualTo(CONFIRMATION_NO);

            // 전화번호로 검증
            BookingConfirmationResponse result2 = bookingService.getConfirmation(CONFIRMATION_NO, GUEST_PHONE);
            assertThat(result2).isNotNull();
        }

        @Test
        @DisplayName("존재하지 않는 확인번호 - BOOKING_CONFIRMATION_NOT_FOUND")
        void getConfirmation_wrongConfirmationNo() {
            when(masterReservationRepository.findByConfirmationNo("WRONG123"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookingService.getConfirmation("WRONG123", GUEST_EMAIL))
                    .isInstanceOf(HolaException.class)
                    .extracting(e -> ((HolaException) e).getErrorCode())
                    .isEqualTo(ErrorCode.BOOKING_CONFIRMATION_NOT_FOUND);
        }

        @Test
        @DisplayName("검증값 불일치 - BOOKING_GUEST_VERIFICATION_FAILED")
        void getConfirmation_verificationMismatch() {
            MasterReservation master = createMasterReservation("RESERVED", GUEST_EMAIL);

            when(masterReservationRepository.findByConfirmationNo(CONFIRMATION_NO))
                    .thenReturn(Optional.of(master));

            assertThatThrownBy(() -> bookingService.getConfirmation(CONFIRMATION_NO, "wrong@test.com"))
                    .isInstanceOf(HolaException.class)
                    .extracting(e -> ((HolaException) e).getErrorCode())
                    .isEqualTo(ErrorCode.BOOKING_GUEST_VERIFICATION_FAILED);
        }
    }

    // ===== 7. 게스트 자가 취소 =====

    @Nested
    @DisplayName("7. 게스트 자가 취소")
    class CancelBooking {

        @Test
        @DisplayName("정상 취소 - 수수료 차감 후 환불")
        void cancelBooking_normalWithFeeAndRefund() throws Exception {
            MasterReservation master = createMasterReservation("RESERVED", GUEST_EMAIL);
            // subReservations 리스트에 직접 추가 (updateStatus 호출용)
            SubReservation sub = SubReservation.builder()
                    .masterReservation(master)
                    .subReservationNo("GMP260315-0001-01")
                    .roomTypeId(ROOM_TYPE_ID)
                    .adults(2).children(0)
                    .checkIn(LocalDate.now().plusDays(7))
                    .checkOut(LocalDate.now().plusDays(9))
                    .build();
            ReflectionTestUtils.setField(sub, "id", 1L);
            master.getSubReservations().add(sub);

            ReservationPayment payment = ReservationPayment.builder()
                    .masterReservation(master)
                    .totalPaidAmount(new BigDecimal("230000"))
                    .paymentMethod("CARD")
                    .build();

            when(masterReservationRepository.findByConfirmationNo(CONFIRMATION_NO))
                    .thenReturn(Optional.of(master));
            when(subReservationRepository.findByMasterReservationId(1L))
                    .thenReturn(List.of(sub));
            when(dailyChargeRepository.findBySubReservationId(1L))
                    .thenReturn(List.of(createDailyCharge(LocalDate.now().plusDays(7),
                            new BigDecimal("100000"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("100000"))));
            when(cancellationPolicyService.calculateCancelFee(eq(PROPERTY_ID), any(), any()))
                    .thenReturn(new CancellationPolicyService.CancelFeeResult(
                            new BigDecimal("50000"), new BigDecimal("50"), "체크인 7일 이내: 1박 요금의 50% 부과"));
            when(reservationPaymentRepository.findByMasterReservationId(1L))
                    .thenReturn(Optional.of(payment));
            when(objectMapper.writeValueAsString(any()))
                    .thenReturn("{}");

            CancelBookingResponse result = bookingService.cancelBooking(
                    CONFIRMATION_NO, GUEST_EMAIL, "127.0.0.1", "TestAgent");

            assertThat(result.getStatus()).isEqualTo("CANCELED");
            assertThat(result.getCancelFeeAmount()).isEqualByComparingTo(new BigDecimal("50000"));
            assertThat(result.getRefundAmount()).isEqualByComparingTo(new BigDecimal("180000"));

            // 마스터/서브 예약 상태 변경 검증
            assertThat(master.getReservationStatus()).isEqualTo("CANCELED");
            assertThat(sub.getRoomReservationStatus()).isEqualTo("CANCELED");
            // 환불 거래 기록 검증 — processRefundWithPg 호출
            verify(reservationPaymentService).processRefundWithPg(
                    eq(1L),
                    argThat(amt -> amt.compareTo(new BigDecimal("180000")) == 0),
                    argThat(fee -> fee.compareTo(new BigDecimal("50000")) == 0),
                    anyString());
        }

        @Test
        @DisplayName("이미 취소된 예약 - BOOKING_ALREADY_CANCELED")
        void cancelBooking_alreadyCanceled() {
            MasterReservation master = createMasterReservation("CANCELED", GUEST_EMAIL);

            when(masterReservationRepository.findByConfirmationNo(CONFIRMATION_NO))
                    .thenReturn(Optional.of(master));

            assertThatThrownBy(() -> bookingService.cancelBooking(
                    CONFIRMATION_NO, GUEST_EMAIL, "127.0.0.1", "TestAgent"))
                    .isInstanceOf(HolaException.class)
                    .extracting(e -> ((HolaException) e).getErrorCode())
                    .isEqualTo(ErrorCode.BOOKING_ALREADY_CANCELED);
        }

        @Test
        @DisplayName("INHOUSE 상태 취소 불가 - BOOKING_CANCEL_NOT_ALLOWED")
        void cancelBooking_inhouseNotAllowed() {
            MasterReservation master = createMasterReservation("INHOUSE", GUEST_EMAIL);

            when(masterReservationRepository.findByConfirmationNo(CONFIRMATION_NO))
                    .thenReturn(Optional.of(master));

            assertThatThrownBy(() -> bookingService.cancelBooking(
                    CONFIRMATION_NO, GUEST_EMAIL, "127.0.0.1", "TestAgent"))
                    .isInstanceOf(HolaException.class)
                    .extracting(e -> ((HolaException) e).getErrorCode())
                    .isEqualTo(ErrorCode.BOOKING_CANCEL_NOT_ALLOWED);
        }

        @Test
        @DisplayName("이메일 불일치 - BOOKING_GUEST_VERIFICATION_FAILED")
        void cancelBooking_emailMismatch() {
            MasterReservation master = createMasterReservation("RESERVED", GUEST_EMAIL);

            when(masterReservationRepository.findByConfirmationNo(CONFIRMATION_NO))
                    .thenReturn(Optional.of(master));

            assertThatThrownBy(() -> bookingService.cancelBooking(
                    CONFIRMATION_NO, "wrong@test.com", "127.0.0.1", "TestAgent"))
                    .isInstanceOf(HolaException.class)
                    .extracting(e -> ((HolaException) e).getErrorCode())
                    .isEqualTo(ErrorCode.BOOKING_GUEST_VERIFICATION_FAILED);
        }

        @Test
        @DisplayName("무료 취소 - 수수료 0원, 전액 환불")
        void cancelBooking_freeCancellation() throws Exception {
            MasterReservation master = createMasterReservation("RESERVED", GUEST_EMAIL);
            SubReservation sub = SubReservation.builder()
                    .masterReservation(master)
                    .subReservationNo("GMP260315-0001-01")
                    .roomTypeId(ROOM_TYPE_ID)
                    .adults(2).children(0)
                    .checkIn(LocalDate.now().plusDays(7))
                    .checkOut(LocalDate.now().plusDays(9))
                    .build();
            ReflectionTestUtils.setField(sub, "id", 1L);
            master.getSubReservations().add(sub);

            ReservationPayment payment = ReservationPayment.builder()
                    .masterReservation(master)
                    .totalPaidAmount(new BigDecimal("200000"))
                    .paymentMethod("CARD")
                    .build();

            when(masterReservationRepository.findByConfirmationNo(CONFIRMATION_NO))
                    .thenReturn(Optional.of(master));
            when(subReservationRepository.findByMasterReservationId(1L))
                    .thenReturn(List.of(sub));
            when(dailyChargeRepository.findBySubReservationId(1L))
                    .thenReturn(List.of(createDailyCharge(LocalDate.now().plusDays(7),
                            new BigDecimal("100000"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("100000"))));
            // 무료 취소 정책
            when(cancellationPolicyService.calculateCancelFee(eq(PROPERTY_ID), any(), any()))
                    .thenReturn(new CancellationPolicyService.CancelFeeResult(
                            BigDecimal.ZERO, BigDecimal.ZERO, "체크인 7일 이전: 무료 취소"));
            when(reservationPaymentRepository.findByMasterReservationId(1L))
                    .thenReturn(Optional.of(payment));
            when(objectMapper.writeValueAsString(any()))
                    .thenReturn("{}");

            CancelBookingResponse result = bookingService.cancelBooking(
                    CONFIRMATION_NO, GUEST_EMAIL, "127.0.0.1", "TestAgent");

            assertThat(result.getStatus()).isEqualTo("CANCELED");
            assertThat(result.getCancelFeeAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getRefundAmount()).isEqualByComparingTo(new BigDecimal("200000"));

            // 전액 환불 — processRefundWithPg 호출
            verify(reservationPaymentService).processRefundWithPg(
                    eq(1L),
                    argThat(amt -> amt.compareTo(new BigDecimal("200000")) == 0),
                    argThat(fee -> fee.compareTo(BigDecimal.ZERO) == 0),
                    anyString());
        }
    }
}
