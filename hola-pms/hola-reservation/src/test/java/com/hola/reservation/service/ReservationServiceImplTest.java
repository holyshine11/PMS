package com.hola.reservation.service;

import com.hola.common.auth.entity.AdminUser;
import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.common.security.AccessControlService;
import com.hola.hotel.entity.Property;
import com.hola.hotel.repository.FloorRepository;
import com.hola.hotel.repository.MarketCodeRepository;
import com.hola.hotel.repository.PropertyRepository;
import com.hola.hotel.repository.RoomNumberRepository;
import com.hola.rate.entity.RateCode;
import com.hola.rate.repository.RateCodeRepository;
import com.hola.reservation.booking.service.CancellationPolicyService;
import com.hola.reservation.booking.service.CancellationPolicyService.CancelFeeResult;
import com.hola.reservation.dto.request.*;
import com.hola.reservation.dto.response.*;
import com.hola.reservation.entity.*;
import com.hola.reservation.mapper.ReservationMapper;
import com.hola.reservation.repository.*;
import com.hola.room.repository.PaidServiceOptionRepository;
import com.hola.room.repository.RoomTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ReservationServiceImpl 단위 테스트
 *
 * 테스트 범위:
 * - CREATE (7): 정상 생성, 과거 날짜, 날짜 역전, 레이트코드 미선택, 만료, 숙박일수 위반, 객실 충돌
 * - UPDATE (5): 정상, CHECKED_OUT 불가, OTA 제한, 레이트/날짜 재검증, 서브레그 충돌
 * - STATUS CHANGE (8): 정상 전이 3개, 무효 전이, CANCELED 스킵, 얼리/레이트 요금, NO_SHOW 환불
 * - CANCEL (4): RESERVED 취소, CHECK_IN 취소, INHOUSE 불가, REFUND 거래 생성
 * - DELETE (3): SUPER_ADMIN 정상, 비권한, 비CHECKED_OUT
 * - FILTER/LIST (3): 상태, 날짜 범위, 키워드
 * - LEGS (5): 추가, 수정+충돌, 삭제 RESERVED만, 날짜 동기화, CANCELED 상태 삭제 불가
 */
@ExtendWith(MockitoExtension.class)
class ReservationServiceImplTest {

    @Mock private MasterReservationRepository masterReservationRepository;
    @Mock private SubReservationRepository subReservationRepository;
    @Mock private ReservationGuestRepository reservationGuestRepository;
    @Mock private DailyChargeRepository dailyChargeRepository;
    @Mock private ReservationDepositRepository reservationDepositRepository;
    @Mock private ReservationMemoRepository reservationMemoRepository;
    @Mock private PropertyRepository propertyRepository;
    @Mock private RateCodeRepository rateCodeRepository;
    @Mock private MarketCodeRepository marketCodeRepository;
    @Mock private FloorRepository floorRepository;
    @Mock private RoomNumberRepository roomNumberRepository;
    @Mock private RoomTypeRepository roomTypeRepository;
    @Mock private PaidServiceOptionRepository paidServiceOptionRepository;
    @Mock private ReservationServiceItemRepository serviceItemRepository;
    @Mock private ReservationMapper reservationMapper;
    @Mock private ReservationNumberGenerator numberGenerator;
    @Mock private RoomAvailabilityService availabilityService;
    @Mock private PriceCalculationService priceCalculationService;
    @Mock private EarlyLateCheckService earlyLateCheckService;
    @Mock private ReservationPaymentService paymentService;
    @Mock private AccessControlService accessControlService;
    @Mock private CancellationPolicyService cancellationPolicyService;
    @Mock private ReservationPaymentRepository reservationPaymentRepository;
    @Mock private PaymentTransactionRepository paymentTransactionRepository;

    @InjectMocks
    private ReservationServiceImpl reservationService;

    // 공통 테스트 상수
    private static final Long PROPERTY_ID = 1L;
    private static final Long MASTER_ID = 100L;
    private static final Long SUB_ID = 200L;
    private static final Long RATE_CODE_ID = 10L;
    private static final LocalDate CHECK_IN = LocalDate.of(2026, 6, 1);
    private static final LocalDate CHECK_OUT = LocalDate.of(2026, 6, 3);

    private Property property;
    private RateCode rateCode;

    @BeforeEach
    void setUp() {
        property = Property.builder()
                .propertyCode("GMP")
                .propertyName("그랜드 호텔")
                .build();

        rateCode = RateCode.builder()
                .saleStartDate(LocalDate.of(2026, 1, 1))
                .saleEndDate(LocalDate.of(2026, 12, 31))
                .minStayDays(1)
                .maxStayDays(30)
                .build();
    }

    // ──────────────────────────────────────────────
    // 헬퍼 메서드
    // ──────────────────────────────────────────────

    /**
     * 기본 MasterReservation 생성 (리플렉션으로 id 세팅)
     */
    private MasterReservation createMaster(String status) {
        MasterReservation master = MasterReservation.builder()
                .property(property)
                .masterReservationNo("GMP260601-0001")
                .confirmationNo("HK4F29XP")
                .reservationStatus(status)
                .masterCheckIn(CHECK_IN)
                .masterCheckOut(CHECK_OUT)
                .guestNameKo("홍길동")
                .rateCodeId(RATE_CODE_ID)
                .isOtaManaged(false)
                .subReservations(new ArrayList<>())
                .build();
        setId(master, MASTER_ID);
        setId(property, PROPERTY_ID);
        return master;
    }

    /**
     * SubReservation 생성
     */
    private SubReservation createSub(MasterReservation master, Long id, String status) {
        SubReservation sub = SubReservation.builder()
                .masterReservation(master)
                .subReservationNo("GMP260601-0001-01")
                .roomReservationStatus(status)
                .roomTypeId(1L)
                .floorId(1L)
                .roomNumberId(1L)
                .adults(2)
                .children(0)
                .checkIn(CHECK_IN)
                .checkOut(CHECK_OUT)
                .earlyCheckIn(false)
                .lateCheckOut(false)
                .earlyCheckInFee(BigDecimal.ZERO)
                .lateCheckOutFee(BigDecimal.ZERO)
                .guests(new ArrayList<>())
                .dailyCharges(new ArrayList<>())
                .services(new ArrayList<>())
                .build();
        setId(sub, id);
        master.getSubReservations().add(sub);
        return sub;
    }

    /**
     * 기본 생성 요청 DTO 빌드
     */
    private ReservationCreateRequest createRequest() {
        return ReservationCreateRequest.builder()
                .masterCheckIn(CHECK_IN)
                .masterCheckOut(CHECK_OUT)
                .guestNameKo("홍길동")
                .rateCodeId(RATE_CODE_ID)
                .subReservations(List.of(
                        SubReservationRequest.builder()
                                .roomTypeId(1L)
                                .floorId(1L)
                                .roomNumberId(1L)
                                .adults(2)
                                .children(0)
                                .checkIn(CHECK_IN)
                                .checkOut(CHECK_OUT)
                                .build()
                ))
                .build();
    }

    /**
     * 기본 수정 요청 DTO 빌드
     */
    private ReservationUpdateRequest updateRequest() {
        return ReservationUpdateRequest.builder()
                .masterCheckIn(CHECK_IN)
                .masterCheckOut(CHECK_OUT)
                .guestNameKo("홍길동")
                .rateCodeId(RATE_CODE_ID)
                .subReservations(List.of(
                        SubReservationRequest.builder()
                                .id(SUB_ID)
                                .roomTypeId(1L)
                                .floorId(1L)
                                .roomNumberId(1L)
                                .adults(2)
                                .children(0)
                                .checkIn(CHECK_IN)
                                .checkOut(CHECK_OUT)
                                .build()
                ))
                .build();
    }

    /**
     * getById() 호출에 필요한 공통 스터빙
     */
    private void stubGetById(MasterReservation master) {
        ReservationDetailResponse detailResponse = ReservationDetailResponse.builder()
                .id(master.getId())
                .propertyId(PROPERTY_ID)
                .masterReservationNo(master.getMasterReservationNo())
                .reservationStatus(master.getReservationStatus())
                .masterCheckIn(master.getMasterCheckIn())
                .masterCheckOut(master.getMasterCheckOut())
                .rateCodeId(master.getRateCodeId())
                .subReservations(Collections.emptyList())
                .build();

        lenient().when(masterReservationRepository.findById(master.getId()))
                .thenReturn(Optional.of(master));
        lenient().when(reservationMapper.toReservationDetailResponse(master))
                .thenReturn(detailResponse);
        lenient().when(reservationDepositRepository.findByMasterReservationId(master.getId()))
                .thenReturn(Collections.emptyList());
        lenient().when(reservationMemoRepository.findByMasterReservationIdOrderByCreatedAtDesc(master.getId()))
                .thenReturn(Collections.emptyList());
        lenient().when(paymentService.getPaymentSummary(master.getId()))
                .thenReturn(PaymentSummaryResponse.builder().build());
    }

    /**
     * 리플렉션으로 BaseEntity.id 설정
     */
    private void setId(Object entity, Long id) {
        try {
            var field = findField(entity.getClass(), "id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException("ID 설정 실패", e);
        }
    }

    private java.lang.reflect.Field findField(Class<?> clazz, String name) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new RuntimeException("필드를 찾을 수 없음: " + name);
    }

    // ══════════════════════════════════════════════
    // CREATE 테스트 (7개)
    // ══════════════════════════════════════════════
    @Nested
    @DisplayName("예약 생성 (create)")
    class CreateTests {

        @Test
        @DisplayName("정상 예약 생성 - 마스터 + 서브 + 결제 재계산")
        void create_정상() {
            // given
            ReservationCreateRequest request = createRequest();
            MasterReservation savedMaster = createMaster("RESERVED");

            when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(property));
            when(rateCodeRepository.findById(RATE_CODE_ID)).thenReturn(Optional.of(rateCode));
            when(numberGenerator.generateMasterReservationNo(property)).thenReturn("GMP260601-0001");
            when(numberGenerator.generateConfirmationNo()).thenReturn("HK4F29XP");
            when(masterReservationRepository.save(any(MasterReservation.class))).thenReturn(savedMaster);
            when(numberGenerator.generateSubReservationNo(anyString(), anyInt())).thenReturn("GMP260601-0001-01");
            when(subReservationRepository.save(any(SubReservation.class)))
                    .thenReturn(createSub(savedMaster, SUB_ID, "RESERVED"));
            stubGetById(savedMaster);

            // when
            ReservationDetailResponse result = reservationService.create(PROPERTY_ID, request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getMasterReservationNo()).isEqualTo("GMP260601-0001");
            verify(masterReservationRepository).save(any(MasterReservation.class));
            verify(paymentService).recalculatePayment(MASTER_ID);
        }

        @Test
        @DisplayName("과거 체크인 날짜 - RESERVATION_CHECKIN_PAST_DATE")
        void create_과거체크인() {
            // given
            ReservationCreateRequest request = ReservationCreateRequest.builder()
                    .masterCheckIn(LocalDate.of(2025, 1, 1))
                    .masterCheckOut(LocalDate.of(2025, 1, 3))
                    .rateCodeId(RATE_CODE_ID)
                    .subReservations(List.of(SubReservationRequest.builder()
                            .checkIn(LocalDate.of(2025, 1, 1))
                            .checkOut(LocalDate.of(2025, 1, 3))
                            .build()))
                    .build();
            when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(property));

            // when & then
            assertThatThrownBy(() -> reservationService.create(PROPERTY_ID, request))
                    .isInstanceOf(HolaException.class)
                    .satisfies(ex -> assertThat(((HolaException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.RESERVATION_CHECKIN_PAST_DATE));
        }

        @Test
        @DisplayName("체크아웃이 체크인 이전 - SUB_RESERVATION_DATE_INVALID")
        void create_날짜역전() {
            // given
            ReservationCreateRequest request = ReservationCreateRequest.builder()
                    .masterCheckIn(CHECK_IN)
                    .masterCheckOut(CHECK_IN.minusDays(1))
                    .rateCodeId(RATE_CODE_ID)
                    .subReservations(List.of(SubReservationRequest.builder()
                            .checkIn(CHECK_IN)
                            .checkOut(CHECK_IN.minusDays(1))
                            .build()))
                    .build();
            when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(property));

            // when & then
            assertThatThrownBy(() -> reservationService.create(PROPERTY_ID, request))
                    .isInstanceOf(HolaException.class)
                    .satisfies(ex -> assertThat(((HolaException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.SUB_RESERVATION_DATE_INVALID));
        }

        @Test
        @DisplayName("레이트코드 미선택 - RESERVATION_RATE_REQUIRED")
        void create_레이트코드없음() {
            // given
            ReservationCreateRequest request = ReservationCreateRequest.builder()
                    .masterCheckIn(CHECK_IN)
                    .masterCheckOut(CHECK_OUT)
                    .rateCodeId(null)
                    .subReservations(List.of(SubReservationRequest.builder()
                            .checkIn(CHECK_IN)
                            .checkOut(CHECK_OUT)
                            .build()))
                    .build();
            when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(property));

            // when & then
            assertThatThrownBy(() -> reservationService.create(PROPERTY_ID, request))
                    .isInstanceOf(HolaException.class)
                    .satisfies(ex -> assertThat(((HolaException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.RESERVATION_RATE_REQUIRED));
        }

        @Test
        @DisplayName("레이트코드 판매기간 만료 - RESERVATION_RATE_EXPIRED")
        void create_레이트만료() {
            // given
            RateCode expiredRate = RateCode.builder()
                    .saleStartDate(LocalDate.of(2025, 1, 1))
                    .saleEndDate(LocalDate.of(2025, 12, 31))
                    .minStayDays(1)
                    .maxStayDays(30)
                    .build();
            ReservationCreateRequest request = createRequest();

            when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(property));
            when(rateCodeRepository.findById(RATE_CODE_ID)).thenReturn(Optional.of(expiredRate));

            // when & then
            assertThatThrownBy(() -> reservationService.create(PROPERTY_ID, request))
                    .isInstanceOf(HolaException.class)
                    .satisfies(ex -> assertThat(((HolaException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.RESERVATION_RATE_EXPIRED));
        }

        @Test
        @DisplayName("최소 숙박일수 위반 - RESERVATION_STAY_DAYS_VIOLATION")
        void create_숙박일수위반() {
            // given
            RateCode minStayRate = RateCode.builder()
                    .saleStartDate(LocalDate.of(2026, 1, 1))
                    .saleEndDate(LocalDate.of(2026, 12, 31))
                    .minStayDays(5)
                    .maxStayDays(30)
                    .build();
            // 2박 예약 (최소 5박 요구)
            ReservationCreateRequest request = createRequest();

            when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(property));
            when(rateCodeRepository.findById(RATE_CODE_ID)).thenReturn(Optional.of(minStayRate));

            // when & then
            assertThatThrownBy(() -> reservationService.create(PROPERTY_ID, request))
                    .isInstanceOf(HolaException.class)
                    .satisfies(ex -> assertThat(((HolaException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.RESERVATION_STAY_DAYS_VIOLATION));
        }

        @Test
        @DisplayName("객실 충돌 - SUB_RESERVATION_ROOM_CONFLICT")
        void create_객실충돌() {
            // given
            ReservationCreateRequest request = createRequest();

            when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(property));
            when(rateCodeRepository.findById(RATE_CODE_ID)).thenReturn(Optional.of(rateCode));
            when(numberGenerator.generateMasterReservationNo(property)).thenReturn("GMP260601-0001");
            when(numberGenerator.generateConfirmationNo()).thenReturn("HK4F29XP");
            MasterReservation savedMaster = createMaster("RESERVED");
            when(masterReservationRepository.save(any(MasterReservation.class))).thenReturn(savedMaster);
            // 객실 충돌 발생
            when(availabilityService.hasRoomConflict(eq(1L), eq(CHECK_IN), eq(CHECK_OUT), isNull()))
                    .thenReturn(true);

            // when & then
            assertThatThrownBy(() -> reservationService.create(PROPERTY_ID, request))
                    .isInstanceOf(HolaException.class)
                    .satisfies(ex -> assertThat(((HolaException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.SUB_RESERVATION_ROOM_CONFLICT));
        }

        @Test
        @DisplayName("프로퍼티 미존재 - PROPERTY_NOT_FOUND")
        void create_프로퍼티없음() {
            // given
            ReservationCreateRequest request = createRequest();
            when(propertyRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> reservationService.create(999L, request))
                    .isInstanceOf(HolaException.class)
                    .satisfies(ex -> assertThat(((HolaException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.PROPERTY_NOT_FOUND));
        }
    }

    // ══════════════════════════════════════════════
    // UPDATE 테스트 (5개)
    // ══════════════════════════════════════════════
    @Nested
    @DisplayName("예약 수정 (update)")
    class UpdateTests {

        @Test
        @DisplayName("정상 예약 수정 - 마스터 정보 업데이트")
        void update_정상() {
            // given
            MasterReservation master = createMaster("RESERVED");
            SubReservation sub = createSub(master, SUB_ID, "RESERVED");
            ReservationUpdateRequest request = updateRequest();

            when(masterReservationRepository.findById(MASTER_ID)).thenReturn(Optional.of(master));
            when(subReservationRepository.findById(SUB_ID)).thenReturn(Optional.of(sub));
            when(availabilityService.hasRoomConflict(eq(1L), any(), any(), eq(SUB_ID))).thenReturn(false);
            stubGetById(master);

            // when
            ReservationDetailResponse result = reservationService.update(MASTER_ID, PROPERTY_ID, request);

            // then
            assertThat(result).isNotNull();
            verify(paymentService).recalculatePayment(MASTER_ID);
        }

        @Test
        @DisplayName("CHECKED_OUT 상태 수정 불가 - RESERVATION_MODIFY_NOT_ALLOWED")
        void update_체크아웃상태() {
            // given
            MasterReservation master = createMaster("CHECKED_OUT");
            ReservationUpdateRequest request = updateRequest();
            when(masterReservationRepository.findById(MASTER_ID)).thenReturn(Optional.of(master));

            // when & then
            assertThatThrownBy(() -> reservationService.update(MASTER_ID, PROPERTY_ID, request))
                    .isInstanceOf(HolaException.class)
                    .satisfies(ex -> assertThat(((HolaException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.RESERVATION_MODIFY_NOT_ALLOWED));
        }

        @Test
        @DisplayName("CANCELED 상태 수정 불가 - RESERVATION_MODIFY_NOT_ALLOWED")
        void update_취소상태() {
            // given
            MasterReservation master = createMaster("CANCELED");
            ReservationUpdateRequest request = updateRequest();
            when(masterReservationRepository.findById(MASTER_ID)).thenReturn(Optional.of(master));

            // when & then
            assertThatThrownBy(() -> reservationService.update(MASTER_ID, PROPERTY_ID, request))
                    .isInstanceOf(HolaException.class)
                    .satisfies(ex -> assertThat(((HolaException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.RESERVATION_MODIFY_NOT_ALLOWED));
        }

        @Test
        @DisplayName("OTA 예약 수정 제한 - RESERVATION_OTA_EDIT_RESTRICTED")
        void update_OTA제한() {
            // given
            MasterReservation master = MasterReservation.builder()
                    .property(property)
                    .masterReservationNo("GMP260601-0001")
                    .confirmationNo("HK4F29XP")
                    .reservationStatus("RESERVED")
                    .masterCheckIn(CHECK_IN)
                    .masterCheckOut(CHECK_OUT)
                    .rateCodeId(RATE_CODE_ID)
                    .isOtaManaged(true)
                    .subReservations(new ArrayList<>())
                    .build();
            setId(master, MASTER_ID);
            setId(property, PROPERTY_ID);

            ReservationUpdateRequest request = updateRequest();
            when(masterReservationRepository.findById(MASTER_ID)).thenReturn(Optional.of(master));

            // when & then
            assertThatThrownBy(() -> reservationService.update(MASTER_ID, PROPERTY_ID, request))
                    .isInstanceOf(HolaException.class)
                    .satisfies(ex -> assertThat(((HolaException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.RESERVATION_OTA_EDIT_RESTRICTED));
        }

        @Test
        @DisplayName("레이트코드 변경 시 재검증 수행")
        void update_레이트코드변경_재검증() {
            // given
            MasterReservation master = createMaster("RESERVED");
            SubReservation sub = createSub(master, SUB_ID, "RESERVED");

            Long newRateCodeId = 20L;
            ReservationUpdateRequest request = ReservationUpdateRequest.builder()
                    .masterCheckIn(CHECK_IN)
                    .masterCheckOut(CHECK_OUT)
                    .guestNameKo("홍길동")
                    .rateCodeId(newRateCodeId)
                    .subReservations(List.of(SubReservationRequest.builder()
                            .id(SUB_ID)
                            .roomTypeId(1L)
                            .roomNumberId(1L)
                            .checkIn(CHECK_IN)
                            .checkOut(CHECK_OUT)
                            .build()))
                    .build();

            when(masterReservationRepository.findById(MASTER_ID)).thenReturn(Optional.of(master));
            when(rateCodeRepository.findById(newRateCodeId)).thenReturn(Optional.of(rateCode));
            when(subReservationRepository.findById(SUB_ID)).thenReturn(Optional.of(sub));
            when(availabilityService.hasRoomConflict(eq(1L), any(), any(), eq(SUB_ID))).thenReturn(false);
            stubGetById(master);

            // when
            reservationService.update(MASTER_ID, PROPERTY_ID, request);

            // then - 레이트코드 변경이므로 validateRateCode 호출됨
            verify(rateCodeRepository).findById(newRateCodeId);
        }
    }

    // ══════════════════════════════════════════════
    // STATUS CHANGE 테스트 (8개)
    // ══════════════════════════════════════════════
    @Nested
    @DisplayName("예약 상태 변경 (changeStatus)")
    class StatusChangeTests {

        @Test
        @DisplayName("RESERVED → CHECK_IN 정상 전이")
        void changeStatus_RESERVED_to_CHECK_IN() {
            // given
            MasterReservation master = createMaster("RESERVED");
            SubReservation sub = createSub(master, SUB_ID, "RESERVED");
            ReservationStatusRequest request = ReservationStatusRequest.builder()
                    .newStatus("CHECK_IN").build();

            when(masterReservationRepository.findById(MASTER_ID)).thenReturn(Optional.of(master));
            when(earlyLateCheckService.calculateEarlyCheckInFee(eq(sub), any(LocalDateTime.class)))
                    .thenReturn(BigDecimal.ZERO);

            // when
            reservationService.changeStatus(MASTER_ID, PROPERTY_ID, request);

            // then
            assertThat(master.getReservationStatus()).isEqualTo("CHECK_IN");
            assertThat(sub.getRoomReservationStatus()).isEqualTo("CHECK_IN");
            verify(paymentService).recalculatePayment(MASTER_ID);
        }

        @Test
        @DisplayName("CHECK_IN → INHOUSE 정상 전이")
        void changeStatus_CHECK_IN_to_INHOUSE() {
            // given
            MasterReservation master = createMaster("CHECK_IN");
            SubReservation sub = createSub(master, SUB_ID, "CHECK_IN");
            ReservationStatusRequest request = ReservationStatusRequest.builder()
                    .newStatus("INHOUSE").build();

            when(masterReservationRepository.findById(MASTER_ID)).thenReturn(Optional.of(master));

            // when
            reservationService.changeStatus(MASTER_ID, PROPERTY_ID, request);

            // then
            assertThat(master.getReservationStatus()).isEqualTo("INHOUSE");
            assertThat(sub.getRoomReservationStatus()).isEqualTo("INHOUSE");
        }

        @Test
        @DisplayName("INHOUSE → CHECKED_OUT 정상 전이 + 레이트체크아웃 요금")
        void changeStatus_INHOUSE_to_CHECKED_OUT() {
            // given
            MasterReservation master = createMaster("INHOUSE");
            SubReservation sub = createSub(master, SUB_ID, "INHOUSE");
            ReservationStatusRequest request = ReservationStatusRequest.builder()
                    .newStatus("CHECKED_OUT").build();
            BigDecimal lateFee = new BigDecimal("30000");

            when(masterReservationRepository.findById(MASTER_ID)).thenReturn(Optional.of(master));
            when(earlyLateCheckService.calculateLateCheckOutFee(eq(sub), any(LocalDateTime.class)))
                    .thenReturn(lateFee);

            // when
            reservationService.changeStatus(MASTER_ID, PROPERTY_ID, request);

            // then
            assertThat(master.getReservationStatus()).isEqualTo("CHECKED_OUT");
            assertThat(sub.getRoomReservationStatus()).isEqualTo("CHECKED_OUT");
            verify(paymentService).recalculatePayment(MASTER_ID);
        }

        @Test
        @DisplayName("RESERVED → CHECKED_OUT 무효 전이 - RESERVATION_STATUS_CHANGE_NOT_ALLOWED")
        void changeStatus_무효전이() {
            // given
            MasterReservation master = createMaster("RESERVED");
            createSub(master, SUB_ID, "RESERVED");
            ReservationStatusRequest request = ReservationStatusRequest.builder()
                    .newStatus("CHECKED_OUT").build();

            when(masterReservationRepository.findById(MASTER_ID)).thenReturn(Optional.of(master));

            // when & then
            assertThatThrownBy(() -> reservationService.changeStatus(MASTER_ID, PROPERTY_ID, request))
                    .isInstanceOf(HolaException.class)
                    .satisfies(ex -> assertThat(((HolaException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.RESERVATION_STATUS_CHANGE_NOT_ALLOWED));
        }

        @Test
        @DisplayName("CANCELED 서브예약은 상태 변경 스킵")
        void changeStatus_CANCELED서브_스킵() {
            // given
            MasterReservation master = createMaster("RESERVED");
            SubReservation activeSub = createSub(master, SUB_ID, "RESERVED");
            SubReservation canceledSub = createSub(master, 201L, "CANCELED");
            ReservationStatusRequest request = ReservationStatusRequest.builder()
                    .newStatus("CHECK_IN").build();

            when(masterReservationRepository.findById(MASTER_ID)).thenReturn(Optional.of(master));
            when(earlyLateCheckService.calculateEarlyCheckInFee(eq(activeSub), any(LocalDateTime.class)))
                    .thenReturn(BigDecimal.ZERO);

            // when
            reservationService.changeStatus(MASTER_ID, PROPERTY_ID, request);

            // then
            assertThat(activeSub.getRoomReservationStatus()).isEqualTo("CHECK_IN");
            assertThat(canceledSub.getRoomReservationStatus()).isEqualTo("CANCELED"); // 변경 안 됨
        }

        @Test
        @DisplayName("CHECK_IN 시 얼리 체크인 요금 계산")
        void changeStatus_얼리체크인요금() {
            // given
            MasterReservation master = createMaster("RESERVED");
            SubReservation sub = createSub(master, SUB_ID, "RESERVED");
            ReservationStatusRequest request = ReservationStatusRequest.builder()
                    .newStatus("CHECK_IN").build();
            BigDecimal earlyFee = new BigDecimal("25000");

            when(masterReservationRepository.findById(MASTER_ID)).thenReturn(Optional.of(master));
            when(earlyLateCheckService.calculateEarlyCheckInFee(eq(sub), any(LocalDateTime.class)))
                    .thenReturn(earlyFee);

            // when
            reservationService.changeStatus(MASTER_ID, PROPERTY_ID, request);

            // then
            verify(earlyLateCheckService).calculateEarlyCheckInFee(eq(sub), any(LocalDateTime.class));
            verify(paymentService).recalculatePayment(MASTER_ID);
        }

        @Test
        @DisplayName("CHECKED_OUT 시 레이트 체크아웃 요금 계산")
        void changeStatus_레이트체크아웃요금() {
            // given
            MasterReservation master = createMaster("INHOUSE");
            SubReservation sub = createSub(master, SUB_ID, "INHOUSE");
            ReservationStatusRequest request = ReservationStatusRequest.builder()
                    .newStatus("CHECKED_OUT").build();
            BigDecimal lateFee = new BigDecimal("50000");

            when(masterReservationRepository.findById(MASTER_ID)).thenReturn(Optional.of(master));
            when(earlyLateCheckService.calculateLateCheckOutFee(eq(sub), any(LocalDateTime.class)))
                    .thenReturn(lateFee);

            // when
            reservationService.changeStatus(MASTER_ID, PROPERTY_ID, request);

            // then
            verify(earlyLateCheckService).calculateLateCheckOutFee(eq(sub), any(LocalDateTime.class));
            verify(paymentService).recalculatePayment(MASTER_ID);
        }

        @Test
        @DisplayName("NO_SHOW 처리 - 취소 수수료 + REFUND 거래 생성")
        void changeStatus_NO_SHOW_환불() {
            // given
            MasterReservation master = createMaster("RESERVED");
            SubReservation sub = createSub(master, SUB_ID, "RESERVED");
            ReservationStatusRequest request = ReservationStatusRequest.builder()
                    .newStatus("NO_SHOW").build();

            // 결제 정보 설정
            ReservationPayment payment = ReservationPayment.builder()
                    .masterReservation(master)
                    .totalPaidAmount(new BigDecimal("200000"))
                    .paymentMethod("CARD")
                    .build();

            // 1박 공급가
            DailyCharge charge = DailyCharge.builder()
                    .supplyPrice(new BigDecimal("100000"))
                    .build();

            CancelFeeResult cancelResult = new CancelFeeResult(
                    new BigDecimal("100000"), new BigDecimal("100"), "노쇼 수수료 100%");

            when(masterReservationRepository.findById(MASTER_ID)).thenReturn(Optional.of(master));
            when(subReservationRepository.findByMasterReservationId(MASTER_ID))
                    .thenReturn(List.of(sub));
            when(dailyChargeRepository.findBySubReservationId(SUB_ID))
                    .thenReturn(List.of(charge));
            when(cancellationPolicyService.calculateCancelFee(
                    eq(PROPERTY_ID), eq(CHECK_IN), eq(new BigDecimal("100000")), eq(true)))
                    .thenReturn(cancelResult);
            when(reservationPaymentRepository.findByMasterReservationId(MASTER_ID))
                    .thenReturn(Optional.of(payment));
            when(paymentTransactionRepository.findByMasterReservationIdOrderByTransactionSeqAsc(MASTER_ID))
                    .thenReturn(Collections.emptyList());

            // when
            reservationService.changeStatus(MASTER_ID, PROPERTY_ID, request);

            // then
            assertThat(master.getReservationStatus()).isEqualTo("NO_SHOW");
            verify(paymentTransactionRepository).save(argThat(txn ->
                    "REFUND".equals(txn.getTransactionType())
                            && txn.getAmount().compareTo(new BigDecimal("100000")) == 0
                            && "COMPLETED".equals(txn.getTransactionStatus())));
        }
    }

    // ══════════════════════════════════════════════
    // CANCEL 테스트 (4개)
    // ══════════════════════════════════════════════
    @Nested
    @DisplayName("예약 취소 (cancel)")
    class CancelTests {

        @Test
        @DisplayName("RESERVED 상태 취소 - 모든 서브 CANCELED 처리")
        void cancel_RESERVED() {
            // given
            MasterReservation master = createMaster("RESERVED");
            SubReservation sub = createSub(master, SUB_ID, "RESERVED");

            CancelFeeResult cancelResult = new CancelFeeResult(
                    BigDecimal.ZERO, BigDecimal.ZERO, "무료 취소");

            when(masterReservationRepository.findById(MASTER_ID)).thenReturn(Optional.of(master));
            when(subReservationRepository.findByMasterReservationId(MASTER_ID))
                    .thenReturn(List.of(sub));
            when(dailyChargeRepository.findBySubReservationId(SUB_ID))
                    .thenReturn(Collections.emptyList());
            when(cancellationPolicyService.calculateCancelFee(
                    eq(PROPERTY_ID), eq(CHECK_IN), any(BigDecimal.class)))
                    .thenReturn(cancelResult);
            when(reservationPaymentRepository.findByMasterReservationId(MASTER_ID))
                    .thenReturn(Optional.empty());

            // when
            reservationService.cancel(MASTER_ID, PROPERTY_ID);

            // then
            assertThat(master.getReservationStatus()).isEqualTo("CANCELED");
            assertThat(sub.getRoomReservationStatus()).isEqualTo("CANCELED");
        }

        @Test
        @DisplayName("CHECK_IN 상태 취소 가능")
        void cancel_CHECK_IN() {
            // given
            MasterReservation master = createMaster("CHECK_IN");
            SubReservation sub = createSub(master, SUB_ID, "CHECK_IN");

            CancelFeeResult cancelResult = new CancelFeeResult(
                    new BigDecimal("50000"), new BigDecimal("50"), "당일 취소 수수료 50%");

            when(masterReservationRepository.findById(MASTER_ID)).thenReturn(Optional.of(master));
            when(subReservationRepository.findByMasterReservationId(MASTER_ID))
                    .thenReturn(List.of(sub));
            when(dailyChargeRepository.findBySubReservationId(SUB_ID))
                    .thenReturn(Collections.emptyList());
            when(cancellationPolicyService.calculateCancelFee(
                    eq(PROPERTY_ID), eq(CHECK_IN), any(BigDecimal.class)))
                    .thenReturn(cancelResult);
            when(reservationPaymentRepository.findByMasterReservationId(MASTER_ID))
                    .thenReturn(Optional.empty());

            // when
            reservationService.cancel(MASTER_ID, PROPERTY_ID);

            // then
            assertThat(master.getReservationStatus()).isEqualTo("CANCELED");
        }

        @Test
        @DisplayName("INHOUSE 상태 취소 불가 - RESERVATION_STATUS_CHANGE_NOT_ALLOWED")
        void cancel_INHOUSE_불가() {
            // given
            MasterReservation master = createMaster("INHOUSE");
            when(masterReservationRepository.findById(MASTER_ID)).thenReturn(Optional.of(master));

            // when & then
            assertThatThrownBy(() -> reservationService.cancel(MASTER_ID, PROPERTY_ID))
                    .isInstanceOf(HolaException.class)
                    .satisfies(ex -> assertThat(((HolaException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.RESERVATION_STATUS_CHANGE_NOT_ALLOWED));
        }

        @Test
        @DisplayName("취소 시 환불 금액 > 0이면 REFUND 거래 생성")
        void cancel_REFUND_거래생성() {
            // given
            MasterReservation master = createMaster("RESERVED");
            SubReservation sub = createSub(master, SUB_ID, "RESERVED");

            ReservationPayment payment = ReservationPayment.builder()
                    .masterReservation(master)
                    .totalPaidAmount(new BigDecimal("200000"))
                    .paymentMethod("CARD")
                    .build();

            DailyCharge charge = DailyCharge.builder()
                    .supplyPrice(new BigDecimal("100000"))
                    .build();

            CancelFeeResult cancelResult = new CancelFeeResult(
                    new BigDecimal("50000"), new BigDecimal("50"), "취소 수수료 50%");

            when(masterReservationRepository.findById(MASTER_ID)).thenReturn(Optional.of(master));
            when(subReservationRepository.findByMasterReservationId(MASTER_ID))
                    .thenReturn(List.of(sub));
            when(dailyChargeRepository.findBySubReservationId(SUB_ID))
                    .thenReturn(List.of(charge));
            when(cancellationPolicyService.calculateCancelFee(
                    eq(PROPERTY_ID), eq(CHECK_IN), eq(new BigDecimal("100000"))))
                    .thenReturn(cancelResult);
            when(reservationPaymentRepository.findByMasterReservationId(MASTER_ID))
                    .thenReturn(Optional.of(payment));
            when(paymentTransactionRepository.findByMasterReservationIdOrderByTransactionSeqAsc(MASTER_ID))
                    .thenReturn(Collections.emptyList());

            // when
            reservationService.cancel(MASTER_ID, PROPERTY_ID);

            // then - 환불금액: 200000 - 50000 = 150000
            verify(paymentTransactionRepository).save(argThat(txn ->
                    "REFUND".equals(txn.getTransactionType())
                            && txn.getAmount().compareTo(new BigDecimal("150000")) == 0));
        }
    }

    // ══════════════════════════════════════════════
    // DELETE 테스트 (3개)
    // ══════════════════════════════════════════════
    @Nested
    @DisplayName("예약 삭제 (deleteReservation)")
    class DeleteTests {

        @Test
        @DisplayName("SUPER_ADMIN + CHECKED_OUT - 정상 삭제")
        void delete_정상() {
            // given
            MasterReservation master = createMaster("CHECKED_OUT");
            SubReservation sub = createSub(master, SUB_ID, "CHECKED_OUT");
            AdminUser superAdmin = AdminUser.builder()
                    .loginId("admin")
                    .role("SUPER_ADMIN")
                    .build();

            when(accessControlService.getCurrentUser()).thenReturn(superAdmin);
            when(masterReservationRepository.findById(MASTER_ID)).thenReturn(Optional.of(master));

            // when
            reservationService.deleteReservation(MASTER_ID, PROPERTY_ID);

            // then - soft delete 확인
            assertThat(master.isDeleted()).isTrue();
            assertThat(sub.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("비SUPER_ADMIN - RESERVATION_DELETE_UNAUTHORIZED")
        void delete_권한없음() {
            // given
            AdminUser hotelAdmin = AdminUser.builder()
                    .loginId("hotel_admin")
                    .role("HOTEL_ADMIN")
                    .build();

            when(accessControlService.getCurrentUser()).thenReturn(hotelAdmin);

            // when & then
            assertThatThrownBy(() -> reservationService.deleteReservation(MASTER_ID, PROPERTY_ID))
                    .isInstanceOf(HolaException.class)
                    .satisfies(ex -> assertThat(((HolaException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.RESERVATION_DELETE_UNAUTHORIZED));
        }

        @Test
        @DisplayName("RESERVED 상태 삭제 불가 - RESERVATION_DELETE_NOT_ALLOWED")
        void delete_상태불가() {
            // given
            MasterReservation master = createMaster("RESERVED");
            AdminUser superAdmin = AdminUser.builder()
                    .loginId("admin")
                    .role("SUPER_ADMIN")
                    .build();

            when(accessControlService.getCurrentUser()).thenReturn(superAdmin);
            when(masterReservationRepository.findById(MASTER_ID)).thenReturn(Optional.of(master));

            // when & then
            assertThatThrownBy(() -> reservationService.deleteReservation(MASTER_ID, PROPERTY_ID))
                    .isInstanceOf(HolaException.class)
                    .satisfies(ex -> assertThat(((HolaException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.RESERVATION_DELETE_NOT_ALLOWED));
        }
    }

    // ══════════════════════════════════════════════
    // FILTER/LIST 테스트 (3개)
    // ══════════════════════════════════════════════
    @Nested
    @DisplayName("예약 리스트 조회 (getList)")
    class ListTests {

        @Test
        @DisplayName("상태별 필터링 - RESERVED만 조회")
        void getList_상태필터() {
            // given
            MasterReservation reserved = createMaster("RESERVED");
            MasterReservation checkedOut = MasterReservation.builder()
                    .property(property)
                    .masterReservationNo("GMP260601-0002")
                    .confirmationNo("AB1234CD")
                    .reservationStatus("CHECKED_OUT")
                    .masterCheckIn(CHECK_IN)
                    .masterCheckOut(CHECK_OUT)
                    .isOtaManaged(false)
                    .subReservations(new ArrayList<>())
                    .build();
            setId(checkedOut, 101L);

            when(masterReservationRepository.findByPropertyIdOrderByReservationDateDesc(PROPERTY_ID))
                    .thenReturn(List.of(reserved, checkedOut));

            ReservationListResponse listResponse = ReservationListResponse.builder()
                    .id(MASTER_ID)
                    .masterReservationNo("GMP260601-0001")
                    .reservationStatus("RESERVED")
                    .build();
            when(reservationMapper.toReservationListResponse(reserved)).thenReturn(listResponse);

            // when
            List<ReservationListResponse> result = reservationService.getList(
                    PROPERTY_ID, "RESERVED", null, null, null);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getReservationStatus()).isEqualTo("RESERVED");
        }

        @Test
        @DisplayName("날짜 범위 필터링")
        void getList_날짜범위() {
            // given
            MasterReservation inRange = createMaster("RESERVED");
            // masterCheckIn = 2026-06-01 (범위 안)

            MasterReservation outOfRange = MasterReservation.builder()
                    .property(property)
                    .masterReservationNo("GMP260701-0001")
                    .confirmationNo("EF5678GH")
                    .reservationStatus("RESERVED")
                    .masterCheckIn(LocalDate.of(2026, 7, 1))
                    .masterCheckOut(LocalDate.of(2026, 7, 3))
                    .isOtaManaged(false)
                    .subReservations(new ArrayList<>())
                    .build();
            setId(outOfRange, 102L);

            when(masterReservationRepository.findByPropertyIdOrderByReservationDateDesc(PROPERTY_ID))
                    .thenReturn(List.of(inRange, outOfRange));

            ReservationListResponse listResponse = ReservationListResponse.builder()
                    .id(MASTER_ID)
                    .masterReservationNo("GMP260601-0001")
                    .build();
            when(reservationMapper.toReservationListResponse(inRange)).thenReturn(listResponse);

            // when - 6월만 조회
            List<ReservationListResponse> result = reservationService.getList(
                    PROPERTY_ID, null,
                    LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30),
                    null);

            // then
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("키워드 필터링 - 예약번호/게스트명/전화번호")
        void getList_키워드() {
            // given
            MasterReservation master = createMaster("RESERVED");

            when(masterReservationRepository.findByPropertyIdOrderByReservationDateDesc(PROPERTY_ID))
                    .thenReturn(List.of(master));

            ReservationListResponse listResponse = ReservationListResponse.builder()
                    .id(MASTER_ID)
                    .masterReservationNo("GMP260601-0001")
                    .guestNameKo("홍길동")
                    .build();
            when(reservationMapper.toReservationListResponse(master)).thenReturn(listResponse);

            // when - 게스트명으로 검색
            List<ReservationListResponse> result = reservationService.getList(
                    PROPERTY_ID, null, null, null, "홍길동");

            // then
            assertThat(result).hasSize(1);
        }
    }

    // ══════════════════════════════════════════════
    // LEGS 테스트 (5개)
    // ══════════════════════════════════════════════
    @Nested
    @DisplayName("서브 예약 레그 (addLeg/updateLeg/deleteLeg)")
    class LegTests {

        @Test
        @DisplayName("레그 추가 - 정상")
        void addLeg_정상() {
            // given
            MasterReservation master = createMaster("RESERVED");
            SubReservationRequest request = SubReservationRequest.builder()
                    .roomTypeId(2L)
                    .floorId(2L)
                    .roomNumberId(2L)
                    .adults(1)
                    .children(0)
                    .checkIn(CHECK_IN)
                    .checkOut(CHECK_OUT)
                    .build();

            SubReservation newSub = SubReservation.builder()
                    .masterReservation(master)
                    .subReservationNo("GMP260601-0001-02")
                    .roomReservationStatus("RESERVED")
                    .roomTypeId(2L)
                    .checkIn(CHECK_IN)
                    .checkOut(CHECK_OUT)
                    .adults(1)
                    .children(0)
                    .earlyCheckIn(false)
                    .lateCheckOut(false)
                    .earlyCheckInFee(BigDecimal.ZERO)
                    .lateCheckOutFee(BigDecimal.ZERO)
                    .guests(new ArrayList<>())
                    .dailyCharges(new ArrayList<>())
                    .services(new ArrayList<>())
                    .build();
            setId(newSub, 202L);

            SubReservationResponse subResponse = SubReservationResponse.builder()
                    .id(202L)
                    .subReservationNo("GMP260601-0001-02")
                    .build();

            when(masterReservationRepository.findById(MASTER_ID)).thenReturn(Optional.of(master));
            when(subReservationRepository.countAllIncludingDeleted(MASTER_ID)).thenReturn(0);
            when(numberGenerator.generateSubReservationNo("GMP260601-0001", 1))
                    .thenReturn("GMP260601-0001-02");
            when(availabilityService.hasRoomConflict(eq(2L), any(), any(), isNull())).thenReturn(false);
            when(subReservationRepository.save(any(SubReservation.class))).thenReturn(newSub);
            when(reservationMapper.toSubReservationResponse(any(SubReservation.class))).thenReturn(subResponse);

            // when
            SubReservationResponse result = reservationService.addLeg(MASTER_ID, PROPERTY_ID, request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getSubReservationNo()).isEqualTo("GMP260601-0001-02");
            verify(paymentService).recalculatePayment(MASTER_ID);
        }

        @Test
        @DisplayName("레그 수정 시 객실 충돌 - SUB_RESERVATION_ROOM_CONFLICT")
        void updateLeg_충돌() {
            // given
            MasterReservation master = createMaster("RESERVED");
            SubReservation sub = createSub(master, SUB_ID, "RESERVED");

            SubReservationRequest request = SubReservationRequest.builder()
                    .roomTypeId(1L)
                    .floorId(1L)
                    .roomNumberId(5L) // 다른 객실로 변경
                    .adults(2)
                    .checkIn(CHECK_IN)
                    .checkOut(CHECK_OUT)
                    .build();

            when(masterReservationRepository.findById(MASTER_ID)).thenReturn(Optional.of(master));
            when(subReservationRepository.findById(SUB_ID)).thenReturn(Optional.of(sub));
            when(availabilityService.hasRoomConflict(eq(5L), any(), any(), eq(SUB_ID))).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> reservationService.updateLeg(MASTER_ID, PROPERTY_ID, SUB_ID, request))
                    .isInstanceOf(HolaException.class)
                    .satisfies(ex -> assertThat(((HolaException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.SUB_RESERVATION_ROOM_CONFLICT));
        }

        @Test
        @DisplayName("레그 삭제 - RESERVED 상태만 가능")
        void deleteLeg_RESERVED_정상() {
            // given
            MasterReservation master = createMaster("RESERVED");
            SubReservation sub = createSub(master, SUB_ID, "RESERVED");

            when(masterReservationRepository.findById(MASTER_ID)).thenReturn(Optional.of(master));
            when(subReservationRepository.findById(SUB_ID)).thenReturn(Optional.of(sub));

            // when
            reservationService.deleteLeg(MASTER_ID, PROPERTY_ID, SUB_ID);

            // then
            assertThat(sub.getRoomReservationStatus()).isEqualTo("CANCELED");
            assertThat(sub.isDeleted()).isTrue();
            verify(paymentService).recalculatePayment(MASTER_ID);
        }

        @Test
        @DisplayName("INHOUSE 상태 레그 삭제 불가 - RESERVATION_STATUS_CHANGE_NOT_ALLOWED")
        void deleteLeg_INHOUSE_불가() {
            // given
            MasterReservation master = createMaster("INHOUSE");
            SubReservation sub = createSub(master, SUB_ID, "INHOUSE");

            when(masterReservationRepository.findById(MASTER_ID)).thenReturn(Optional.of(master));

            // when & then - 마스터가 IMMUTABLE 상태이므로 validateModifiable에서 에러
            // INHOUSE는 IMMUTABLE_STATUSES에 포함되지 않지만 서브 상태가 RESERVED가 아니면 삭제 불가
            // INHOUSE 마스터는 수정 가능하므로 서브 상태 검사에서 걸림
            when(subReservationRepository.findById(SUB_ID)).thenReturn(Optional.of(sub));

            assertThatThrownBy(() -> reservationService.deleteLeg(MASTER_ID, PROPERTY_ID, SUB_ID))
                    .isInstanceOf(HolaException.class)
                    .satisfies(ex -> assertThat(((HolaException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.RESERVATION_STATUS_CHANGE_NOT_ALLOWED));
        }

        @Test
        @DisplayName("CHECKED_OUT 마스터의 레그 추가 불가 - RESERVATION_MODIFY_NOT_ALLOWED")
        void addLeg_수정불가상태() {
            // given
            MasterReservation master = createMaster("CHECKED_OUT");
            SubReservationRequest request = SubReservationRequest.builder()
                    .roomTypeId(2L)
                    .checkIn(CHECK_IN)
                    .checkOut(CHECK_OUT)
                    .build();

            when(masterReservationRepository.findById(MASTER_ID)).thenReturn(Optional.of(master));

            // when & then
            assertThatThrownBy(() -> reservationService.addLeg(MASTER_ID, PROPERTY_ID, request))
                    .isInstanceOf(HolaException.class)
                    .satisfies(ex -> assertThat(((HolaException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.RESERVATION_MODIFY_NOT_ALLOWED));
        }
    }

    // ══════════════════════════════════════════════
    // 기타 테스트
    // ══════════════════════════════════════════════
    @Nested
    @DisplayName("기타 기능")
    class MiscTests {

        @Test
        @DisplayName("예약 상세 조회 (getById) - 정상")
        void getById_정상() {
            // given
            MasterReservation master = createMaster("RESERVED");
            SubReservation sub = createSub(master, SUB_ID, "RESERVED");

            ReservationDetailResponse detailResponse = ReservationDetailResponse.builder()
                    .id(MASTER_ID)
                    .propertyId(PROPERTY_ID)
                    .masterReservationNo("GMP260601-0001")
                    .reservationStatus("RESERVED")
                    .masterCheckIn(CHECK_IN)
                    .masterCheckOut(CHECK_OUT)
                    .rateCodeId(RATE_CODE_ID)
                    .subReservations(Collections.emptyList())
                    .build();

            when(masterReservationRepository.findById(MASTER_ID)).thenReturn(Optional.of(master));
            when(reservationMapper.toReservationDetailResponse(master)).thenReturn(detailResponse);
            when(reservationDepositRepository.findByMasterReservationId(MASTER_ID))
                    .thenReturn(Collections.emptyList());
            when(reservationMemoRepository.findByMasterReservationIdOrderByCreatedAtDesc(MASTER_ID))
                    .thenReturn(Collections.emptyList());
            when(paymentService.getPaymentSummary(MASTER_ID))
                    .thenReturn(PaymentSummaryResponse.builder().build());

            // when
            ReservationDetailResponse result = reservationService.getById(MASTER_ID, PROPERTY_ID);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(MASTER_ID);
            assertThat(result.getReservationStatus()).isEqualTo("RESERVED");
        }

        @Test
        @DisplayName("예약 미존재 시 - RESERVATION_NOT_FOUND")
        void getById_미존재() {
            // given
            when(masterReservationRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> reservationService.getById(999L, PROPERTY_ID))
                    .isInstanceOf(HolaException.class)
                    .satisfies(ex -> assertThat(((HolaException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.RESERVATION_NOT_FOUND));
        }

        @Test
        @DisplayName("프로퍼티 불일치 시 - RESERVATION_NOT_FOUND")
        void getById_프로퍼티불일치() {
            // given
            MasterReservation master = createMaster("RESERVED");
            when(masterReservationRepository.findById(MASTER_ID)).thenReturn(Optional.of(master));

            // when & then - 다른 프로퍼티 ID로 조회
            assertThatThrownBy(() -> reservationService.getById(MASTER_ID, 999L))
                    .isInstanceOf(HolaException.class)
                    .satisfies(ex -> assertThat(((HolaException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.RESERVATION_NOT_FOUND));
        }

        @Test
        @DisplayName("최대 숙박일수 초과 시 - RESERVATION_STAY_DAYS_VIOLATION")
        void create_최대숙박일수초과() {
            // given
            RateCode maxStayRate = RateCode.builder()
                    .saleStartDate(LocalDate.of(2026, 1, 1))
                    .saleEndDate(LocalDate.of(2026, 12, 31))
                    .minStayDays(1)
                    .maxStayDays(2)
                    .build();

            // 5박 예약 시도 (최대 2박)
            ReservationCreateRequest request = ReservationCreateRequest.builder()
                    .masterCheckIn(CHECK_IN)
                    .masterCheckOut(CHECK_IN.plusDays(5))
                    .rateCodeId(RATE_CODE_ID)
                    .subReservations(List.of(SubReservationRequest.builder()
                            .checkIn(CHECK_IN)
                            .checkOut(CHECK_IN.plusDays(5))
                            .build()))
                    .build();

            when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(property));
            when(rateCodeRepository.findById(RATE_CODE_ID)).thenReturn(Optional.of(maxStayRate));

            // when & then
            assertThatThrownBy(() -> reservationService.create(PROPERTY_ID, request))
                    .isInstanceOf(HolaException.class)
                    .satisfies(ex -> assertThat(((HolaException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.RESERVATION_STAY_DAYS_VIOLATION));
        }

        @Test
        @DisplayName("NO_SHOW 상태 수정 불가 - RESERVATION_MODIFY_NOT_ALLOWED")
        void update_NO_SHOW상태() {
            // given
            MasterReservation master = createMaster("NO_SHOW");
            ReservationUpdateRequest request = updateRequest();
            when(masterReservationRepository.findById(MASTER_ID)).thenReturn(Optional.of(master));

            // when & then
            assertThatThrownBy(() -> reservationService.update(MASTER_ID, PROPERTY_ID, request))
                    .isInstanceOf(HolaException.class)
                    .satisfies(ex -> assertThat(((HolaException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.RESERVATION_MODIFY_NOT_ALLOWED));
        }
    }
}
