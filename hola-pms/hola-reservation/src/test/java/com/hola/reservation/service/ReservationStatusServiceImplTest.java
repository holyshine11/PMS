package com.hola.reservation.service;

import com.hola.common.enums.StayType;
import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.hotel.entity.Property;
import com.hola.hotel.repository.RoomNumberRepository;
import com.hola.reservation.dto.request.ReservationStatusRequest;
import com.hola.reservation.entity.MasterReservation;
import com.hola.reservation.entity.ReservationPayment;
import com.hola.reservation.entity.SubReservation;
import com.hola.reservation.repository.DailyChargeRepository;
import com.hola.reservation.repository.MasterReservationRepository;
import com.hola.reservation.repository.PaymentTransactionRepository;
import com.hola.reservation.repository.ReservationPaymentRepository;
import com.hola.reservation.repository.ReservationServiceItemRepository;
import com.hola.reservation.repository.SubReservationRepository;
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
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ReservationStatusServiceImpl 단위 테스트
 *
 * 테스트 범위:
 * - 얼리 체크인 요금 보존/자동계산/Dayuse 0원
 * - 레이트 체크아웃 요금 보존/자동계산 (Leg 단위 / 일괄)
 * - 체크아웃 후 결제 재계산 호출 확인
 * - 유효하지 않은 상태 전이 거부
 */
@ExtendWith(MockitoExtension.class)
class ReservationStatusServiceImplTest {

    @Mock private ReservationFinder finder;
    @Mock private MasterReservationRepository masterReservationRepository;
    @Mock private SubReservationRepository subReservationRepository;
    @Mock private DailyChargeRepository dailyChargeRepository;
    @Mock private ReservationPaymentRepository reservationPaymentRepository;
    @Mock private PaymentTransactionRepository paymentTransactionRepository;
    @Mock private ReservationServiceItemRepository serviceItemRepository;
    @Mock private RoomNumberRepository roomNumberRepository;
    @Mock private RoomTypeRepository roomTypeRepository;
    @Mock private PaidServiceOptionRepository paidServiceOptionRepository;
    @Mock private ReservationPaymentService paymentService;
    @Mock private EarlyLateCheckService earlyLateCheckService;
    @Mock private com.hola.reservation.booking.service.CancellationPolicyService cancellationPolicyService;
    @Mock private com.hola.room.service.InventoryService inventoryService;
    @Mock private com.hola.hotel.service.HousekeepingService housekeepingService;
    @Mock private ReservationChangeLogService changeLogService;

    @InjectMocks
    private ReservationStatusServiceImpl statusService;

    // 공통 상수
    private static final Long PROPERTY_ID = 1L;
    private static final Long MASTER_ID = 100L;
    private static final Long SUB_ID = 200L;
    private static final LocalDate CHECK_IN = LocalDate.of(2026, 6, 1);
    private static final LocalDate CHECK_OUT = LocalDate.of(2026, 6, 3);

    private Property property;

    @BeforeEach
    void setUp() {
        property = Property.builder()
                .propertyCode("GMP")
                .propertyName("그랜드 호텔")
                .checkInTime("15:00")
                .checkOutTime("11:00")
                .build();
        setId(property, PROPERTY_ID);

        // changeLogService 기본 스텁 (예외 불필요)
        lenient().doNothing().when(changeLogService).logStatusChange(any(), any(), any(), any());
    }

    // ──────────────────────────────────────────────
    // 헬퍼 메서드
    // ──────────────────────────────────────────────

    private MasterReservation createMaster(String status) {
        MasterReservation master = MasterReservation.builder()
                .property(property)
                .masterReservationNo("GMP260601-0001")
                .confirmationNo("HK4F29XP")
                .reservationStatus(status)
                .masterCheckIn(CHECK_IN)
                .masterCheckOut(CHECK_OUT)
                .guestNameKo("홍길동")
                .rateCodeId(10L)
                .isOtaManaged(false)
                .subReservations(new ArrayList<>())
                .build();
        setId(master, MASTER_ID);
        return master;
    }

    /**
     * 숙박(OVERNIGHT) SubReservation 생성
     */
    private SubReservation createSub(MasterReservation master, Long id, String status,
                                     BigDecimal earlyFee, BigDecimal lateFee) {
        SubReservation sub = SubReservation.builder()
                .masterReservation(master)
                .subReservationNo("GMP260601-0001-01")
                .roomReservationStatus(status)
                .roomTypeId(1L)
                .roomNumberId(1L)
                .checkIn(CHECK_IN)
                .checkOut(CHECK_OUT)
                .stayType(StayType.OVERNIGHT)
                .earlyCheckIn(earlyFee != null && earlyFee.compareTo(BigDecimal.ZERO) > 0)
                .lateCheckOut(lateFee != null && lateFee.compareTo(BigDecimal.ZERO) > 0)
                .earlyCheckInFee(earlyFee != null ? earlyFee : BigDecimal.ZERO)
                .lateCheckOutFee(lateFee != null ? lateFee : BigDecimal.ZERO)
                .guests(new ArrayList<>())
                .dailyCharges(new ArrayList<>())
                .services(new ArrayList<>())
                .build();
        setId(sub, id);
        master.getSubReservations().add(sub);
        return sub;
    }

    /**
     * Dayuse SubReservation 생성
     */
    private SubReservation createDayUseSub(MasterReservation master, Long id, String status) {
        SubReservation sub = SubReservation.builder()
                .masterReservation(master)
                .subReservationNo("GMP260601-0001-01")
                .roomReservationStatus(status)
                .roomTypeId(1L)
                .roomNumberId(1L)
                .checkIn(CHECK_IN)
                .checkOut(CHECK_IN) // Dayuse: 당일 체크인·체크아웃
                .stayType(StayType.DAY_USE)
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
     * Leg 단위 체크아웃 테스트에서 잔액 검증 통과를 위한 결제 스텁
     */
    private void stubZeroBalancePayment(Long masterId) {
        ReservationPayment payment = ReservationPayment.builder()
                .grandTotal(BigDecimal.ZERO)
                .totalPaidAmount(BigDecimal.ZERO)
                .refundAmount(BigDecimal.ZERO)
                .cancelFeeAmount(BigDecimal.ZERO)
                .build();
        lenient().when(reservationPaymentRepository.findByMasterReservationId(masterId))
                .thenReturn(Optional.of(payment));
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
    // 1. INHOUSE 전환 — 얼리 체크인 요금
    // ══════════════════════════════════════════════

    @Nested
    @DisplayName("INHOUSE 전환 — 얼리 체크인 요금")
    class EarlyCheckInFeeTests {

        @Test
        @DisplayName("사전 등록된 얼리 체크인 요금이 있으면 earlyLateCheckService를 호출하지 않고 기존 값을 사용한다")
        void inhouse_preservesPreRegisteredEarlyFee() {
            // given
            BigDecimal preRegisteredFee = new BigDecimal("30000");
            MasterReservation master = createMaster("RESERVED");
            SubReservation sub = createSub(master, SUB_ID, "RESERVED", preRegisteredFee, BigDecimal.ZERO);

            when(finder.findMasterById(MASTER_ID, PROPERTY_ID)).thenReturn(master);
            when(finder.findSubAndValidateOwnership(SUB_ID, master)).thenReturn(sub);
            // roomNumber 조회 시 null 반환 → checkIn() 스킵
            when(roomNumberRepository.findById(anyLong())).thenReturn(Optional.empty());
            lenient().doNothing().when(paymentService).recalculatePayment(anyLong());

            ReservationStatusRequest request = ReservationStatusRequest.builder()
                    .newStatus("INHOUSE")
                    .subReservationId(SUB_ID)
                    .build();

            // when
            statusService.changeStatus(MASTER_ID, PROPERTY_ID, request);

            // then
            verify(earlyLateCheckService, never()).calculateEarlyCheckInFee(any(), any());
            assertThat(sub.getActualCheckInTime()).isNotNull();
            // earlyCheckInFee 는 이미 등록된 값이 그대로 유지됨
            assertThat(sub.getEarlyCheckInFee()).isEqualByComparingTo(preRegisteredFee);
        }

        @Test
        @DisplayName("earlyCheckInFee가 0이면 earlyLateCheckService.calculateEarlyCheckInFee()를 호출하여 자동 계산한다")
        void inhouse_calculatesEarlyFeeWhenNotRegistered() {
            // given
            BigDecimal calculatedFee = new BigDecimal("20000");
            MasterReservation master = createMaster("RESERVED");
            SubReservation sub = createSub(master, SUB_ID, "RESERVED", BigDecimal.ZERO, BigDecimal.ZERO);

            when(finder.findMasterById(MASTER_ID, PROPERTY_ID)).thenReturn(master);
            when(finder.findSubAndValidateOwnership(SUB_ID, master)).thenReturn(sub);
            when(earlyLateCheckService.calculateEarlyCheckInFee(eq(sub), any())).thenReturn(calculatedFee);
            when(roomNumberRepository.findById(anyLong())).thenReturn(Optional.empty());
            lenient().doNothing().when(paymentService).recalculatePayment(anyLong());

            ReservationStatusRequest request = ReservationStatusRequest.builder()
                    .newStatus("INHOUSE")
                    .subReservationId(SUB_ID)
                    .build();

            // when
            statusService.changeStatus(MASTER_ID, PROPERTY_ID, request);

            // then
            verify(earlyLateCheckService).calculateEarlyCheckInFee(eq(sub), any());
            assertThat(sub.getActualCheckInTime()).isNotNull();
            assertThat(sub.getEarlyCheckInFee()).isEqualByComparingTo(calculatedFee);
        }

        @Test
        @DisplayName("Dayuse 예약은 INHOUSE 전환 시 얼리 체크인 요금을 0원으로 처리한다")
        void inhouse_dayuseGetsZeroEarlyFee() {
            // given
            MasterReservation master = createMaster("RESERVED");
            SubReservation dayUseSub = createDayUseSub(master, SUB_ID, "RESERVED");

            when(finder.findMasterById(MASTER_ID, PROPERTY_ID)).thenReturn(master);
            when(finder.findSubAndValidateOwnership(SUB_ID, master)).thenReturn(dayUseSub);
            when(roomNumberRepository.findById(anyLong())).thenReturn(Optional.empty());
            lenient().doNothing().when(paymentService).recalculatePayment(anyLong());

            ReservationStatusRequest request = ReservationStatusRequest.builder()
                    .newStatus("INHOUSE")
                    .subReservationId(SUB_ID)
                    .build();

            // when
            statusService.changeStatus(MASTER_ID, PROPERTY_ID, request);

            // then
            verify(earlyLateCheckService, never()).calculateEarlyCheckInFee(any(), any());
            assertThat(dayUseSub.getActualCheckInTime()).isNotNull();
            // Dayuse는 earlyCheckInFee = 0 이므로 recordCheckIn에서 earlyCheckIn이 true로 바뀌지 않음
            assertThat(dayUseSub.getEarlyCheckInFee()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    // ══════════════════════════════════════════════
    // 2. CHECKED_OUT 전환(Leg 단위) — 레이트 체크아웃 요금
    // ══════════════════════════════════════════════

    @Nested
    @DisplayName("CHECKED_OUT 전환(Leg 단위) — 레이트 체크아웃 요금")
    class LegCheckoutFeeTests {

        @Test
        @DisplayName("사전 등록된 레이트 체크아웃 요금이 있으면 earlyLateCheckService를 호출하지 않고 기존 값을 사용한다")
        void checkout_preservesPreRegisteredLateFee() {
            // given
            BigDecimal preRegisteredFee = new BigDecimal("50000");
            MasterReservation master = createMaster("INHOUSE");
            SubReservation sub = createSub(master, SUB_ID, "INHOUSE", BigDecimal.ZERO, preRegisteredFee);

            when(finder.findMasterById(MASTER_ID, PROPERTY_ID)).thenReturn(master);
            when(finder.findSubAndValidateOwnership(SUB_ID, master)).thenReturn(sub);
            stubZeroBalancePayment(MASTER_ID);
            lenient().doNothing().when(paymentService).recalculatePayment(anyLong());
            when(roomNumberRepository.findById(anyLong())).thenReturn(Optional.empty());

            ReservationStatusRequest request = ReservationStatusRequest.builder()
                    .newStatus("CHECKED_OUT")
                    .subReservationId(SUB_ID)
                    .build();

            // when
            statusService.changeStatus(MASTER_ID, PROPERTY_ID, request);

            // then
            verify(earlyLateCheckService, never()).calculateLateCheckOutFee(any(), any());
            assertThat(sub.getActualCheckOutTime()).isNotNull();
            assertThat(sub.getLateCheckOutFee()).isEqualByComparingTo(preRegisteredFee);
        }

        @Test
        @DisplayName("lateCheckOutFee가 0이면 earlyLateCheckService.calculateLateCheckOutFee()를 호출하여 자동 계산한다")
        void checkout_calculatesLateFeeWhenNotRegistered() {
            // given
            BigDecimal calculatedFee = new BigDecimal("40000");
            MasterReservation master = createMaster("INHOUSE");
            SubReservation sub = createSub(master, SUB_ID, "INHOUSE", BigDecimal.ZERO, BigDecimal.ZERO);

            when(finder.findMasterById(MASTER_ID, PROPERTY_ID)).thenReturn(master);
            when(finder.findSubAndValidateOwnership(SUB_ID, master)).thenReturn(sub);
            when(earlyLateCheckService.calculateLateCheckOutFee(eq(sub), any())).thenReturn(calculatedFee);
            stubZeroBalancePayment(MASTER_ID);
            lenient().doNothing().when(paymentService).recalculatePayment(anyLong());
            when(roomNumberRepository.findById(anyLong())).thenReturn(Optional.empty());

            ReservationStatusRequest request = ReservationStatusRequest.builder()
                    .newStatus("CHECKED_OUT")
                    .subReservationId(SUB_ID)
                    .build();

            // when
            statusService.changeStatus(MASTER_ID, PROPERTY_ID, request);

            // then
            verify(earlyLateCheckService).calculateLateCheckOutFee(eq(sub), any());
            assertThat(sub.getActualCheckOutTime()).isNotNull();
            assertThat(sub.getLateCheckOutFee()).isEqualByComparingTo(calculatedFee);
        }

        @Test
        @DisplayName("Leg 단위 체크아웃 시 paymentService.recalculatePayment()가 호출된다")
        void checkout_callsRecalculatePayment() {
            // given
            MasterReservation master = createMaster("INHOUSE");
            SubReservation sub = createSub(master, SUB_ID, "INHOUSE", BigDecimal.ZERO, BigDecimal.ZERO);

            when(finder.findMasterById(MASTER_ID, PROPERTY_ID)).thenReturn(master);
            when(finder.findSubAndValidateOwnership(SUB_ID, master)).thenReturn(sub);
            when(earlyLateCheckService.calculateLateCheckOutFee(any(), any())).thenReturn(BigDecimal.ZERO);
            stubZeroBalancePayment(MASTER_ID);
            when(roomNumberRepository.findById(anyLong())).thenReturn(Optional.empty());

            ReservationStatusRequest request = ReservationStatusRequest.builder()
                    .newStatus("CHECKED_OUT")
                    .subReservationId(SUB_ID)
                    .build();

            // when
            statusService.changeStatus(MASTER_ID, PROPERTY_ID, request);

            // then
            // Leg 체크아웃 시 recalculatePayment 최소 1회 호출 (changeStatus 내 명시적 호출)
            verify(paymentService, atLeastOnce()).recalculatePayment(MASTER_ID);
        }

        @Test
        @DisplayName("Dayuse Leg 체크아웃 시 레이트 요금을 0원으로 처리하고 earlyLateCheckService를 호출하지 않는다")
        void checkout_dayuseGetsZeroLateFee() {
            // given
            MasterReservation master = createMaster("INHOUSE");
            SubReservation dayUseSub = createDayUseSub(master, SUB_ID, "INHOUSE");

            when(finder.findMasterById(MASTER_ID, PROPERTY_ID)).thenReturn(master);
            when(finder.findSubAndValidateOwnership(SUB_ID, master)).thenReturn(dayUseSub);
            stubZeroBalancePayment(MASTER_ID);
            lenient().doNothing().when(paymentService).recalculatePayment(anyLong());
            when(roomNumberRepository.findById(anyLong())).thenReturn(Optional.empty());

            ReservationStatusRequest request = ReservationStatusRequest.builder()
                    .newStatus("CHECKED_OUT")
                    .subReservationId(SUB_ID)
                    .build();

            // when
            statusService.changeStatus(MASTER_ID, PROPERTY_ID, request);

            // then
            verify(earlyLateCheckService, never()).calculateLateCheckOutFee(any(), any());
            assertThat(dayUseSub.getActualCheckOutTime()).isNotNull();
            assertThat(dayUseSub.getLateCheckOutFee()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    // ══════════════════════════════════════════════
    // 3. 전체 일괄 CHECKED_OUT — 레이트 체크아웃 요금
    // ══════════════════════════════════════════════

    @Nested
    @DisplayName("전체 일괄 CHECKED_OUT — 레이트 체크아웃 요금")
    class BulkCheckoutFeeTests {

        @Test
        @DisplayName("일괄 체크아웃 시 INHOUSE 상태인 Leg에만 레이트 요금을 자동 계산하여 적용한다")
        void bulkCheckout_appliesLateFeeToAllInhouseLegs() {
            // given
            BigDecimal calculatedFee = new BigDecimal("30000");
            MasterReservation master = createMaster("INHOUSE");
            // Leg1: INHOUSE, lateCheckOutFee=0 → 계산 필요
            SubReservation sub1 = createSub(master, SUB_ID, "INHOUSE", BigDecimal.ZERO, BigDecimal.ZERO);
            // Leg2: INHOUSE, lateCheckOutFee=0 → 계산 필요
            SubReservation sub2 = createSub(master, 201L, "INHOUSE", BigDecimal.ZERO, BigDecimal.ZERO);

            when(finder.findMasterById(MASTER_ID, PROPERTY_ID)).thenReturn(master);
            when(earlyLateCheckService.calculateLateCheckOutFee(any(), any())).thenReturn(calculatedFee);
            stubZeroBalancePayment(MASTER_ID);
            lenient().doNothing().when(paymentService).recalculatePayment(anyLong());
            when(roomNumberRepository.findById(anyLong())).thenReturn(Optional.empty());

            ReservationStatusRequest request = ReservationStatusRequest.builder()
                    .newStatus("CHECKED_OUT")
                    .subReservationId(null) // 전체 일괄
                    .build();

            // when
            statusService.changeStatus(MASTER_ID, PROPERTY_ID, request);

            // then
            verify(earlyLateCheckService, times(2)).calculateLateCheckOutFee(any(), any());
            assertThat(sub1.getActualCheckOutTime()).isNotNull();
            assertThat(sub2.getActualCheckOutTime()).isNotNull();
        }

        @Test
        @DisplayName("일괄 체크아웃 시 사전 등록된 레이트 체크아웃 요금이 있는 Leg은 재계산하지 않는다")
        void bulkCheckout_preservesPreRegisteredLateFeePerLeg() {
            // given
            BigDecimal preRegisteredFee = new BigDecimal("50000");
            BigDecimal calculatedFee = new BigDecimal("30000");
            MasterReservation master = createMaster("INHOUSE");
            // Leg1: INHOUSE, 사전 등록된 lateCheckOutFee
            SubReservation sub1 = createSub(master, SUB_ID, "INHOUSE", BigDecimal.ZERO, preRegisteredFee);
            // Leg2: INHOUSE, lateCheckOutFee=0 → 계산 필요
            SubReservation sub2 = createSub(master, 201L, "INHOUSE", BigDecimal.ZERO, BigDecimal.ZERO);

            when(finder.findMasterById(MASTER_ID, PROPERTY_ID)).thenReturn(master);
            when(earlyLateCheckService.calculateLateCheckOutFee(eq(sub2), any())).thenReturn(calculatedFee);
            stubZeroBalancePayment(MASTER_ID);
            lenient().doNothing().when(paymentService).recalculatePayment(anyLong());
            when(roomNumberRepository.findById(anyLong())).thenReturn(Optional.empty());

            ReservationStatusRequest request = ReservationStatusRequest.builder()
                    .newStatus("CHECKED_OUT")
                    .subReservationId(null) // 전체 일괄
                    .build();

            // when
            statusService.changeStatus(MASTER_ID, PROPERTY_ID, request);

            // then
            // sub1은 사전 등록 요금이 있어 earlyLateCheckService 호출 없이 기존 값 유지
            verify(earlyLateCheckService, times(1)).calculateLateCheckOutFee(any(), any());
            assertThat(sub1.getLateCheckOutFee()).isEqualByComparingTo(preRegisteredFee);
            assertThat(sub2.getLateCheckOutFee()).isEqualByComparingTo(calculatedFee);
        }

        @Test
        @DisplayName("일괄 체크아웃 후 paymentService.recalculatePayment()가 호출된다")
        void bulkCheckout_callsRecalculatePayment() {
            // given
            MasterReservation master = createMaster("INHOUSE");
            SubReservation sub = createSub(master, SUB_ID, "INHOUSE", BigDecimal.ZERO, BigDecimal.ZERO);

            when(finder.findMasterById(MASTER_ID, PROPERTY_ID)).thenReturn(master);
            when(earlyLateCheckService.calculateLateCheckOutFee(any(), any())).thenReturn(BigDecimal.ZERO);
            stubZeroBalancePayment(MASTER_ID);
            when(roomNumberRepository.findById(anyLong())).thenReturn(Optional.empty());

            ReservationStatusRequest request = ReservationStatusRequest.builder()
                    .newStatus("CHECKED_OUT")
                    .subReservationId(null)
                    .build();

            // when
            statusService.changeStatus(MASTER_ID, PROPERTY_ID, request);

            // then
            verify(paymentService, atLeastOnce()).recalculatePayment(MASTER_ID);
        }
    }

    // ══════════════════════════════════════════════
    // 4. 유효하지 않은 상태 전이 거부
    // ══════════════════════════════════════════════

    @Nested
    @DisplayName("유효하지 않은 상태 전이 거부")
    class InvalidStatusTransitionTests {

        @Test
        @DisplayName("CHECKED_OUT → INHOUSE 전환은 허용되지 않는다")
        void checkedOut_cannotTransitionToInhouse() {
            // given
            MasterReservation master = createMaster("CHECKED_OUT");
            SubReservation sub = createSub(master, SUB_ID, "CHECKED_OUT", BigDecimal.ZERO, BigDecimal.ZERO);

            when(finder.findMasterById(MASTER_ID, PROPERTY_ID)).thenReturn(master);
            when(finder.findSubAndValidateOwnership(SUB_ID, master)).thenReturn(sub);

            ReservationStatusRequest request = ReservationStatusRequest.builder()
                    .newStatus("INHOUSE")
                    .subReservationId(SUB_ID)
                    .build();

            // when / then
            assertThatThrownBy(() -> statusService.changeStatus(MASTER_ID, PROPERTY_ID, request))
                    .isInstanceOf(HolaException.class)
                    .satisfies(ex -> assertThat(((HolaException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.RESERVATION_STATUS_CHANGE_NOT_ALLOWED));
        }

        @Test
        @DisplayName("CANCELED → INHOUSE 전환은 허용되지 않는다")
        void canceled_cannotTransitionToInhouse() {
            // given
            MasterReservation master = createMaster("CANCELED");
            SubReservation sub = createSub(master, SUB_ID, "CANCELED", BigDecimal.ZERO, BigDecimal.ZERO);

            when(finder.findMasterById(MASTER_ID, PROPERTY_ID)).thenReturn(master);
            when(finder.findSubAndValidateOwnership(SUB_ID, master)).thenReturn(sub);

            ReservationStatusRequest request = ReservationStatusRequest.builder()
                    .newStatus("INHOUSE")
                    .subReservationId(SUB_ID)
                    .build();

            // when / then
            assertThatThrownBy(() -> statusService.changeStatus(MASTER_ID, PROPERTY_ID, request))
                    .isInstanceOf(HolaException.class)
                    .satisfies(ex -> assertThat(((HolaException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.RESERVATION_STATUS_CHANGE_NOT_ALLOWED));
        }

        @Test
        @DisplayName("NO_SHOW → RESERVED 전환은 허용되지 않는다")
        void noShow_cannotTransitionToReserved() {
            // given
            MasterReservation master = createMaster("NO_SHOW");
            SubReservation sub = createSub(master, SUB_ID, "NO_SHOW", BigDecimal.ZERO, BigDecimal.ZERO);

            when(finder.findMasterById(MASTER_ID, PROPERTY_ID)).thenReturn(master);
            when(finder.findSubAndValidateOwnership(SUB_ID, master)).thenReturn(sub);

            ReservationStatusRequest request = ReservationStatusRequest.builder()
                    .newStatus("RESERVED")
                    .subReservationId(SUB_ID)
                    .build();

            // when / then
            assertThatThrownBy(() -> statusService.changeStatus(MASTER_ID, PROPERTY_ID, request))
                    .isInstanceOf(HolaException.class)
                    .satisfies(ex -> assertThat(((HolaException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.RESERVATION_STATUS_CHANGE_NOT_ALLOWED));
        }

        @Test
        @DisplayName("INHOUSE → RESERVED 전환은 허용되지 않는다")
        void inhouse_cannotTransitionToReserved() {
            // given
            MasterReservation master = createMaster("INHOUSE");
            SubReservation sub = createSub(master, SUB_ID, "INHOUSE", BigDecimal.ZERO, BigDecimal.ZERO);

            when(finder.findMasterById(MASTER_ID, PROPERTY_ID)).thenReturn(master);
            when(finder.findSubAndValidateOwnership(SUB_ID, master)).thenReturn(sub);

            ReservationStatusRequest request = ReservationStatusRequest.builder()
                    .newStatus("RESERVED")
                    .subReservationId(SUB_ID)
                    .build();

            // when / then
            assertThatThrownBy(() -> statusService.changeStatus(MASTER_ID, PROPERTY_ID, request))
                    .isInstanceOf(HolaException.class)
                    .satisfies(ex -> assertThat(((HolaException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.RESERVATION_STATUS_CHANGE_NOT_ALLOWED));
        }

        @Test
        @DisplayName("전체 일괄 전환에서 CHECKED_OUT Leg만 있으면 statusChanged가 false → 예외 발생")
        void bulkChange_noEligibleLeg_throwsException() {
            // given: 이미 모두 CHECKED_OUT인 Leg만 존재
            MasterReservation master = createMaster("CHECKED_OUT");
            createSub(master, SUB_ID, "CHECKED_OUT", BigDecimal.ZERO, BigDecimal.ZERO);

            when(finder.findMasterById(MASTER_ID, PROPERTY_ID)).thenReturn(master);

            ReservationStatusRequest request = ReservationStatusRequest.builder()
                    .newStatus("CHECKED_OUT")
                    .subReservationId(null)
                    .build();

            // when / then
            // CHECKED_OUT → CHECKED_OUT: 허용되지 않는 전이이므로 statusChanged = false → 예외
            assertThatThrownBy(() -> statusService.changeStatus(MASTER_ID, PROPERTY_ID, request))
                    .isInstanceOf(HolaException.class)
                    .satisfies(ex -> assertThat(((HolaException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.RESERVATION_STATUS_CHANGE_NOT_ALLOWED));
        }
    }

    // ══════════════════════════════════════════════
    // 5. 체크아웃 잔액 검증
    // ══════════════════════════════════════════════

    @Nested
    @DisplayName("체크아웃 잔액 검증")
    class CheckoutBalanceValidationTests {

        @Test
        @DisplayName("미결제 잔액이 있으면 CHECKED_OUT 전환 시 예외가 발생한다")
        void checkout_withUnpaidBalance_throwsException() {
            // given
            MasterReservation master = createMaster("INHOUSE");
            SubReservation sub = createSub(master, SUB_ID, "INHOUSE", BigDecimal.ZERO, BigDecimal.ZERO);

            when(finder.findMasterById(MASTER_ID, PROPERTY_ID)).thenReturn(master);
            when(finder.findSubAndValidateOwnership(SUB_ID, master)).thenReturn(sub);
            when(earlyLateCheckService.calculateLateCheckOutFee(any(), any())).thenReturn(BigDecimal.ZERO);
            lenient().doNothing().when(paymentService).recalculatePayment(anyLong());

            // grandTotal > totalPaidAmount → 미결제 잔액 있음
            ReservationPayment payment = ReservationPayment.builder()
                    .grandTotal(new BigDecimal("100000"))
                    .totalPaidAmount(new BigDecimal("50000"))
                    .refundAmount(BigDecimal.ZERO)
                    .cancelFeeAmount(BigDecimal.ZERO)
                    .build();
            when(reservationPaymentRepository.findByMasterReservationId(MASTER_ID))
                    .thenReturn(Optional.of(payment));

            ReservationStatusRequest request = ReservationStatusRequest.builder()
                    .newStatus("CHECKED_OUT")
                    .subReservationId(SUB_ID)
                    .build();

            // when / then
            assertThatThrownBy(() -> statusService.changeStatus(MASTER_ID, PROPERTY_ID, request))
                    .isInstanceOf(HolaException.class)
                    .satisfies(ex -> assertThat(((HolaException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.CHECKOUT_OUTSTANDING_BALANCE));
        }

        @Test
        @DisplayName("결제가 완료된 경우 CHECKED_OUT 전환에 성공한다")
        void checkout_withFullPayment_succeeds() {
            // given
            MasterReservation master = createMaster("INHOUSE");
            SubReservation sub = createSub(master, SUB_ID, "INHOUSE", BigDecimal.ZERO, BigDecimal.ZERO);

            when(finder.findMasterById(MASTER_ID, PROPERTY_ID)).thenReturn(master);
            when(finder.findSubAndValidateOwnership(SUB_ID, master)).thenReturn(sub);
            when(earlyLateCheckService.calculateLateCheckOutFee(any(), any())).thenReturn(BigDecimal.ZERO);
            lenient().doNothing().when(paymentService).recalculatePayment(anyLong());

            // grandTotal == totalPaidAmount → 잔액 없음
            ReservationPayment payment = ReservationPayment.builder()
                    .grandTotal(new BigDecimal("100000"))
                    .totalPaidAmount(new BigDecimal("100000"))
                    .refundAmount(BigDecimal.ZERO)
                    .cancelFeeAmount(BigDecimal.ZERO)
                    .build();
            when(reservationPaymentRepository.findByMasterReservationId(MASTER_ID))
                    .thenReturn(Optional.of(payment));
            when(roomNumberRepository.findById(anyLong())).thenReturn(Optional.empty());

            ReservationStatusRequest request = ReservationStatusRequest.builder()
                    .newStatus("CHECKED_OUT")
                    .subReservationId(SUB_ID)
                    .build();

            // when / then (예외가 발생하지 않아야 함)
            statusService.changeStatus(MASTER_ID, PROPERTY_ID, request);

            assertThat(sub.getActualCheckOutTime()).isNotNull();
            assertThat(sub.getRoomReservationStatus()).isEqualTo("CHECKED_OUT");
        }
    }
}
