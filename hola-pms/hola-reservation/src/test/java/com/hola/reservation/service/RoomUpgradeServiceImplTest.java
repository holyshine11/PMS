package com.hola.reservation.service;

import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.hotel.entity.Hotel;
import com.hola.hotel.entity.Property;
import com.hola.rate.entity.RateCode;
import com.hola.rate.entity.RateCodeRoomType;
import com.hola.rate.repository.RateCodeRepository;
import com.hola.rate.repository.RateCodeRoomTypeRepository;
import com.hola.reservation.dto.request.RoomUpgradeRequest;
import com.hola.reservation.dto.response.RoomUpgradeHistoryResponse;
import com.hola.reservation.dto.response.UpgradePreviewResponse;
import com.hola.reservation.entity.DailyCharge;
import com.hola.reservation.entity.MasterReservation;
import com.hola.reservation.entity.RoomUpgradeHistory;
import com.hola.reservation.entity.SubReservation;
import com.hola.reservation.repository.DailyChargeRepository;
import com.hola.reservation.repository.ReservationServiceItemRepository;
import com.hola.reservation.repository.RoomUpgradeHistoryRepository;
import com.hola.reservation.repository.SubReservationRepository;
import com.hola.room.entity.RoomType;
import com.hola.room.entity.TransactionCode;
import com.hola.room.repository.RoomTypeRepository;
import com.hola.room.repository.TransactionCodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RoomUpgradeServiceImpl 단위 테스트
 *
 * 테스트 범위:
 * - previewUpgrade (selectedRateCodeId 지정 시 / 미지정 시 / availableRateCodes 빌드)
 * - executeUpgrade (paymentService.recalculatePayment 호출 검증)
 * - validateUpgradeAllowed (비업그레이드 상태 거부 / 동일 타입 거부)
 */
@ExtendWith(MockitoExtension.class)
class RoomUpgradeServiceImplTest {

    @Mock private SubReservationRepository subReservationRepository;
    @Mock private DailyChargeRepository dailyChargeRepository;
    @Mock private RoomUpgradeHistoryRepository upgradeHistoryRepository;
    @Mock private ReservationServiceItemRepository serviceItemRepository;
    @Mock private RoomTypeRepository roomTypeRepository;
    @Mock private TransactionCodeRepository transactionCodeRepository;
    @Mock private RateCodeRoomTypeRepository rateCodeRoomTypeRepository;
    @Mock private RateCodeRepository rateCodeRepository;
    @Mock private PriceCalculationService priceCalculationService;
    @Mock private ReservationChangeLogService changeLogService;
    @Mock private ReservationPaymentService paymentService;
    @Mock private RoomAvailabilityService roomAvailabilityService;

    @InjectMocks
    private RoomUpgradeServiceImpl roomUpgradeService;

    // 공통 테스트 상수
    private static final Long PROPERTY_ID   = 1L;
    private static final Long HOTEL_ID      = 10L;
    private static final Long MASTER_ID     = 100L;
    private static final Long SUB_ID        = 200L;
    private static final Long FROM_TYPE_ID  = 1L;
    private static final Long TO_TYPE_ID    = 2L;
    private static final Long RATE_CODE_ID  = 10L;
    private static final Long TARGET_RATE_ID = 20L;
    private static final LocalDate CHECK_IN  = LocalDate.of(2026, 6, 1);
    private static final LocalDate CHECK_OUT = LocalDate.of(2026, 6, 3);

    private Hotel hotel;
    private Property property;
    private MasterReservation master;
    private SubReservation sub;
    private RoomType fromType;
    private RoomType toType;

    @BeforeEach
    void setUp() {
        hotel = Hotel.builder()
                .hotelCode("GH")
                .hotelName("그랜드 호텔")
                .build();
        setId(hotel, HOTEL_ID);

        property = Property.builder()
                .hotel(hotel)
                .propertyCode("GMP")
                .propertyName("그랜드 호텔 메인")
                .checkInTime("15:00")
                .checkOutTime("11:00")
                .build();
        setId(property, PROPERTY_ID);

        master = MasterReservation.builder()
                .property(property)
                .masterReservationNo("GMP260601-0001")
                .confirmationNo("HK4F29XP")
                .reservationStatus("RESERVED")
                .masterCheckIn(CHECK_IN)
                .masterCheckOut(CHECK_OUT)
                .guestNameKo("홍길동")
                .rateCodeId(RATE_CODE_ID)
                .isOtaManaged(false)
                .subReservations(new ArrayList<>())
                .build();
        setId(master, MASTER_ID);

        sub = SubReservation.builder()
                .masterReservation(master)
                .subReservationNo("GMP260601-0001-01")
                .roomReservationStatus("RESERVED")
                .roomTypeId(FROM_TYPE_ID)
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
        setId(sub, SUB_ID);
        master.getSubReservations().add(sub);

        fromType = RoomType.builder()
                .propertyId(PROPERTY_ID)
                .roomClassId(1L)
                .roomTypeCode("STD")
                .description("스탠다드룸")
                .maxAdults(2)
                .maxChildren(1)
                .build();
        setId(fromType, FROM_TYPE_ID);

        toType = RoomType.builder()
                .propertyId(PROPERTY_ID)
                .roomClassId(1L)
                .roomTypeCode("DLX")
                .description("디럭스룸")
                .maxAdults(2)
                .maxChildren(1)
                .build();
        setId(toType, TO_TYPE_ID);
    }

    // ──────────────────────────────────────────────
    // 헬퍼: 리플렉션으로 BaseEntity.id / 일반 필드 세팅
    // ──────────────────────────────────────────────

    private static void setId(Object entity, Long id) {
        try {
            // BaseEntity 또는 직접 id 필드 탐색 (상위 클래스 포함)
            Class<?> clazz = entity.getClass();
            while (clazz != null) {
                try {
                    Field f = clazz.getDeclaredField("id");
                    f.setAccessible(true);
                    f.set(entity, id);
                    return;
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();
                }
            }
            throw new RuntimeException("id 필드를 찾을 수 없음: " + entity.getClass().getSimpleName());
        } catch (IllegalAccessException e) {
            throw new RuntimeException("id 필드 접근 실패", e);
        }
    }

    /** RoomUpgradeRequest 생성 헬퍼 */
    private RoomUpgradeRequest buildRequest(Long toRoomTypeId, String upgradeType) {
        try {
            RoomUpgradeRequest req = new RoomUpgradeRequest();
            setField(req, "toRoomTypeId", toRoomTypeId);
            setField(req, "upgradeType", upgradeType);
            setField(req, "reason", "테스트 업그레이드");
            return req;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void setField(Object obj, String fieldName, Object value) throws Exception {
        Field f = obj.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(obj, value);
    }

    /** DailyCharge 생성 헬퍼 */
    private DailyCharge charge(LocalDate date, BigDecimal total) {
        return DailyCharge.builder()
                .chargeDate(date)
                .supplyPrice(total)
                .tax(BigDecimal.ZERO)
                .serviceCharge(BigDecimal.ZERO)
                .total(total)
                .build();
    }

    /** 현재 DailyCharge 목록과 프리뷰 계산에 필요한 공통 스텁 */
    private void stubForPreview(List<DailyCharge> currentCharges, List<DailyCharge> newCharges) {
        when(subReservationRepository.findById(SUB_ID)).thenReturn(Optional.of(sub));
        when(roomTypeRepository.findById(FROM_TYPE_ID)).thenReturn(Optional.of(fromType));
        when(roomTypeRepository.findById(TO_TYPE_ID)).thenReturn(Optional.of(toType));
        when(roomAvailabilityService.getAvailableRoomCount(
                eq(TO_TYPE_ID), any(), any(), anyList())).thenReturn(5);
        when(dailyChargeRepository.findBySubReservationId(SUB_ID)).thenReturn(currentCharges);
        when(priceCalculationService.calculateDailyCharges(
                anyLong(), any(), any(), any(), anyInt(), anyInt(), any()))
                .thenReturn(newCharges);
    }

    /** RateCode 스텁: findById */
    private RateCode buildRateCode(Long id, String code, String name, boolean useYn) {
        RateCode rc = RateCode.builder()
                .propertyId(PROPERTY_ID)
                .rateCode(code)
                .rateNameKo(name)
                .rateCategory("RACK")
                .currency("KRW")
                .saleStartDate(LocalDate.of(2026, 1, 1))
                .saleEndDate(LocalDate.of(2026, 12, 31))
                .minStayDays(1)
                .maxStayDays(30)
                .build();
        setId(rc, id);
        // BaseEntity.useYn 세팅
        try {
            Class<?> c = rc.getClass().getSuperclass();
            Field f = c.getDeclaredField("useYn");
            f.setAccessible(true);
            f.set(rc, useYn);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return rc;
    }

    // ──────────────────────────────────────────────
    // previewUpgrade 테스트
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("previewUpgrade")
    class PreviewUpgradeTest {

        @Test
        @DisplayName("selectedRateCodeId가 지정된 경우 해당 레이트코드를 사용한다")
        void previewUpgrade_withSelectedRateCodeId_usesSelectedRate() {
            // given
            List<DailyCharge> currentCharges = List.of(
                    charge(CHECK_IN, BigDecimal.valueOf(100_000)),
                    charge(CHECK_IN.plusDays(1), BigDecimal.valueOf(100_000)));
            List<DailyCharge> newCharges = List.of(
                    charge(CHECK_IN, BigDecimal.valueOf(150_000)),
                    charge(CHECK_IN.plusDays(1), BigDecimal.valueOf(150_000)));

            stubForPreview(currentCharges, newCharges);

            // selectedRateCodeId 사용 → findRateCodeForRoomType 호출 안 됨
            // 현재 레이트코드명 조회 (rateCodeChanged=true 케이스)
            RateCode currentRc = buildRateCode(RATE_CODE_ID, "RACK", "기본 레이트", true);
            RateCode targetRc = buildRateCode(TARGET_RATE_ID, "DLX", "디럭스 레이트", true);
            when(rateCodeRepository.findById(RATE_CODE_ID)).thenReturn(Optional.of(currentRc));
            when(rateCodeRepository.findById(TARGET_RATE_ID)).thenReturn(Optional.of(targetRc));

            // buildRateCodeOptions 스텁
            RateCodeRoomType mapping = mock(RateCodeRoomType.class);
            when(mapping.getRateCodeId()).thenReturn(TARGET_RATE_ID);
            when(rateCodeRoomTypeRepository.findAllByRoomTypeId(TO_TYPE_ID))
                    .thenReturn(List.of(mapping));

            // when
            UpgradePreviewResponse response =
                    roomUpgradeService.previewUpgrade(SUB_ID, TO_TYPE_ID, TARGET_RATE_ID);

            // then
            assertThat(response.getTargetRateCodeId()).isEqualTo(TARGET_RATE_ID);
            assertThat(response.getTargetRateCodeName()).isEqualTo("디럭스 레이트");
            assertThat(response.isRateCodeChanged()).isTrue();
            assertThat(response.getCurrentTotalCharge()).isEqualByComparingTo(BigDecimal.valueOf(200_000));
            assertThat(response.getNewTotalCharge()).isEqualByComparingTo(BigDecimal.valueOf(300_000));
            assertThat(response.getPriceDifference()).isEqualByComparingTo(BigDecimal.valueOf(100_000));

            // findRateCodeForRoomType 경로(rateCodeRoomTypeRepository)는 preview에서 호출되지 않아야 함
            // (buildRateCodeOptions에서 한 번만 호출, findRateCodeForRoomType 경로는 불필요)
            verify(rateCodeRoomTypeRepository, times(1)).findAllByRoomTypeId(TO_TYPE_ID);
        }

        @Test
        @DisplayName("selectedRateCodeId 미지정 시 findRateCodeForRoomType 자동 탐색으로 폴백된다")
        void previewUpgrade_withoutSelectedRateCodeId_autoDetectsRate() {
            // given
            List<DailyCharge> currentCharges = List.of(
                    charge(CHECK_IN, BigDecimal.valueOf(100_000)));
            List<DailyCharge> newCharges = List.of(
                    charge(CHECK_IN, BigDecimal.valueOf(120_000)));

            stubForPreview(currentCharges, newCharges);

            // 현재 레이트코드가 대상 타입에도 매핑되어 있음 → 1순위 적용 (rate 불변)
            RateCodeRoomType mapping = mock(RateCodeRoomType.class);
            when(mapping.getRateCodeId()).thenReturn(RATE_CODE_ID);
            when(rateCodeRoomTypeRepository.findAllByRoomTypeId(TO_TYPE_ID))
                    .thenReturn(List.of(mapping));

            RateCode currentRc = buildRateCode(RATE_CODE_ID, "RACK", "기본 레이트", true);
            when(rateCodeRepository.findById(RATE_CODE_ID)).thenReturn(Optional.of(currentRc));

            // when — selectedRateCodeId=null (2-arg overload)
            UpgradePreviewResponse response =
                    roomUpgradeService.previewUpgrade(SUB_ID, TO_TYPE_ID);

            // then: 현재 레이트코드가 그대로 사용되고 rateCodeChanged=false
            assertThat(response.getTargetRateCodeId()).isEqualTo(RATE_CODE_ID);
            assertThat(response.isRateCodeChanged()).isFalse();
            assertThat(response.getFromRoomTypeId()).isEqualTo(FROM_TYPE_ID);
            assertThat(response.getToRoomTypeId()).isEqualTo(TO_TYPE_ID);
        }

        @Test
        @DisplayName("availableRateCodes 목록은 대상 객실타입에 매핑된 활성 레이트코드를 포함한다")
        void previewUpgrade_availableRateCodes_containsActiveMappedRateCodes() {
            // given
            List<DailyCharge> currentCharges = List.of(
                    charge(CHECK_IN, BigDecimal.valueOf(100_000)));
            List<DailyCharge> newCharges = List.of(
                    charge(CHECK_IN, BigDecimal.valueOf(130_000)));

            stubForPreview(currentCharges, newCharges);

            // 대상 타입에 2개 레이트코드 매핑 (1개 활성, 1개 비활성)
            RateCodeRoomType mappingActive = mock(RateCodeRoomType.class);
            when(mappingActive.getRateCodeId()).thenReturn(TARGET_RATE_ID);
            RateCodeRoomType mappingInactive = mock(RateCodeRoomType.class);
            when(mappingInactive.getRateCodeId()).thenReturn(99L);

            when(rateCodeRoomTypeRepository.findAllByRoomTypeId(TO_TYPE_ID))
                    .thenReturn(List.of(mappingActive, mappingInactive));

            RateCode activeRc = buildRateCode(TARGET_RATE_ID, "DLX", "디럭스 레이트", true);
            RateCode inactiveRc = buildRateCode(99L, "OLD", "구 레이트", false);

            when(rateCodeRepository.findById(RATE_CODE_ID))
                    .thenReturn(Optional.of(buildRateCode(RATE_CODE_ID, "RACK", "기본 레이트", true)));
            when(rateCodeRepository.findById(TARGET_RATE_ID)).thenReturn(Optional.of(activeRc));
            when(rateCodeRepository.findById(99L)).thenReturn(Optional.of(inactiveRc));

            // when: selectedRateCodeId = TARGET_RATE_ID (변경된 케이스)
            UpgradePreviewResponse response =
                    roomUpgradeService.previewUpgrade(SUB_ID, TO_TYPE_ID, TARGET_RATE_ID);

            // then: 활성 레이트코드만 포함
            assertThat(response.getAvailableRateCodes()).hasSize(1);
            UpgradePreviewResponse.RateCodeOption option = response.getAvailableRateCodes().get(0);
            assertThat(option.getId()).isEqualTo(TARGET_RATE_ID);
            assertThat(option.getRateCode()).isEqualTo("DLX");
            assertThat(option.getRateNameKo()).isEqualTo("디럭스 레이트");
            assertThat(option.isRecommended()).isTrue();   // targetRateCodeId와 일치
        }
    }

    // ──────────────────────────────────────────────
    // validateUpgradeAllowed 테스트
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("validateUpgradeAllowed")
    class ValidateUpgradeAllowedTest {

        @Test
        @DisplayName("CHECKED_OUT 상태이면 UPGRADE_NOT_ALLOWED 예외가 발생한다")
        void validateUpgradeAllowed_checkedOut_throwsUpgradeNotAllowed() {
            // given
            SubReservation checkedOutSub = SubReservation.builder()
                    .masterReservation(master)
                    .subReservationNo("GMP260601-0001-02")
                    .roomReservationStatus("CHECKED_OUT")
                    .roomTypeId(FROM_TYPE_ID)
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
            setId(checkedOutSub, SUB_ID);

            when(subReservationRepository.findById(SUB_ID)).thenReturn(Optional.of(checkedOutSub));

            // when & then
            assertThatThrownBy(() -> roomUpgradeService.previewUpgrade(SUB_ID, TO_TYPE_ID))
                    .isInstanceOf(HolaException.class)
                    .satisfies(ex -> assertThat(((HolaException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.UPGRADE_NOT_ALLOWED));
        }

        @Test
        @DisplayName("CANCELED 상태이면 UPGRADE_NOT_ALLOWED 예외가 발생한다")
        void validateUpgradeAllowed_canceled_throwsUpgradeNotAllowed() {
            // given
            SubReservation canceledSub = SubReservation.builder()
                    .masterReservation(master)
                    .subReservationNo("GMP260601-0001-03")
                    .roomReservationStatus("CANCELED")
                    .roomTypeId(FROM_TYPE_ID)
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
            setId(canceledSub, SUB_ID);

            when(subReservationRepository.findById(SUB_ID)).thenReturn(Optional.of(canceledSub));

            // when & then
            assertThatThrownBy(() -> roomUpgradeService.previewUpgrade(SUB_ID, TO_TYPE_ID))
                    .isInstanceOf(HolaException.class)
                    .satisfies(ex -> assertThat(((HolaException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.UPGRADE_NOT_ALLOWED));
        }

        @Test
        @DisplayName("현재 객실타입과 대상 타입이 동일하면 UPGRADE_SAME_ROOM_TYPE 예외가 발생한다")
        void validateUpgradeAllowed_sameRoomType_throwsUpgradeSameRoomType() {
            // given: status=RESERVED이므로 상태 체크는 통과, 동일 타입 체크에서 예외 발생
            when(subReservationRepository.findById(SUB_ID)).thenReturn(Optional.of(sub));

            // when & then: toRoomTypeId = FROM_TYPE_ID (동일 타입) — availability 체크 전에 예외
            assertThatThrownBy(() -> roomUpgradeService.previewUpgrade(SUB_ID, FROM_TYPE_ID))
                    .isInstanceOf(HolaException.class)
                    .satisfies(ex -> assertThat(((HolaException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.UPGRADE_SAME_ROOM_TYPE));
        }
    }

    // ──────────────────────────────────────────────
    // executeUpgrade 테스트
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("executeUpgrade")
    class ExecuteUpgradeTest {

        @Test
        @DisplayName("COMPLIMENTARY 업그레이드 후 paymentService.recalculatePayment가 호출된다")
        void executeUpgrade_complimentary_callsRecalculatePayment() {
            // given: previewUpgrade 내부 스텁 (executeUpgrade가 내부적으로 호출)
            List<DailyCharge> currentCharges = List.of(
                    charge(CHECK_IN, BigDecimal.valueOf(100_000)),
                    charge(CHECK_IN.plusDays(1), BigDecimal.valueOf(100_000)));
            List<DailyCharge> newCharges = List.of(
                    charge(CHECK_IN, BigDecimal.valueOf(150_000)),
                    charge(CHECK_IN.plusDays(1), BigDecimal.valueOf(150_000)));

            // executeUpgrade 내부에서 findSubReservation 두 번 호출
            when(subReservationRepository.findById(SUB_ID)).thenReturn(Optional.of(sub));
            when(roomTypeRepository.findById(FROM_TYPE_ID)).thenReturn(Optional.of(fromType));
            when(roomTypeRepository.findById(TO_TYPE_ID)).thenReturn(Optional.of(toType));
            when(roomAvailabilityService.getAvailableRoomCount(
                    eq(TO_TYPE_ID), any(), any(), anyList())).thenReturn(5);
            when(dailyChargeRepository.findBySubReservationId(SUB_ID)).thenReturn(currentCharges);
            when(priceCalculationService.calculateDailyCharges(
                    anyLong(), any(), any(), any(), anyInt(), anyInt(), any()))
                    .thenReturn(newCharges);

            // 현재/대상 레이트코드명 조회
            RateCode currentRc = buildRateCode(RATE_CODE_ID, "RACK", "기본 레이트", true);
            RateCodeRoomType mapping = mock(RateCodeRoomType.class);
            when(mapping.getRateCodeId()).thenReturn(RATE_CODE_ID);
            when(rateCodeRoomTypeRepository.findAllByRoomTypeId(TO_TYPE_ID))
                    .thenReturn(List.of(mapping));
            when(rateCodeRepository.findById(RATE_CODE_ID)).thenReturn(Optional.of(currentRc));

            // history save 스텁
            RoomUpgradeHistory savedHistory = RoomUpgradeHistory.builder()
                    .subReservationId(SUB_ID)
                    .fromRoomTypeId(FROM_TYPE_ID)
                    .toRoomTypeId(TO_TYPE_ID)
                    .upgradedAt(LocalDateTime.now())
                    .upgradeType("COMPLIMENTARY")
                    .priceDifference(BigDecimal.ZERO)
                    .reason("테스트 업그레이드")
                    .build();
            setId(savedHistory, 999L);
            when(upgradeHistoryRepository.save(any())).thenReturn(savedHistory);

            RoomUpgradeRequest request = buildRequest(TO_TYPE_ID, "COMPLIMENTARY");

            // when
            RoomUpgradeHistoryResponse response =
                    roomUpgradeService.executeUpgrade(SUB_ID, request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getUpgradeType()).isEqualTo("COMPLIMENTARY");
            assertThat(response.getPriceDifference()).isEqualByComparingTo(BigDecimal.ZERO);

            // 핵심 검증: paymentService.recalculatePayment가 masterId로 호출됐는지
            verify(paymentService, times(1)).recalculatePayment(MASTER_ID);
        }

        @Test
        @DisplayName("PAID 업그레이드 시 양수 차액이면 서비스 항목이 생성되고 recalculatePayment가 호출된다")
        void executeUpgrade_paid_createsServiceItemAndCallsRecalculate() {
            // given: 현재 100K, 업그레이드 후 150K → 차액 50K
            List<DailyCharge> currentCharges = List.of(
                    charge(CHECK_IN, BigDecimal.valueOf(100_000)));
            List<DailyCharge> newCharges = List.of(
                    charge(CHECK_IN, BigDecimal.valueOf(150_000)));

            when(subReservationRepository.findById(SUB_ID)).thenReturn(Optional.of(sub));
            when(roomTypeRepository.findById(FROM_TYPE_ID)).thenReturn(Optional.of(fromType));
            when(roomTypeRepository.findById(TO_TYPE_ID)).thenReturn(Optional.of(toType));
            when(roomAvailabilityService.getAvailableRoomCount(
                    eq(TO_TYPE_ID), any(), any(), anyList())).thenReturn(5);
            when(dailyChargeRepository.findBySubReservationId(SUB_ID)).thenReturn(currentCharges);
            when(priceCalculationService.calculateDailyCharges(
                    anyLong(), any(), any(), any(), anyInt(), anyInt(), any()))
                    .thenReturn(newCharges);

            RateCode currentRc = buildRateCode(RATE_CODE_ID, "RACK", "기본 레이트", true);
            RateCodeRoomType mapping = mock(RateCodeRoomType.class);
            when(mapping.getRateCodeId()).thenReturn(RATE_CODE_ID);
            when(rateCodeRoomTypeRepository.findAllByRoomTypeId(TO_TYPE_ID))
                    .thenReturn(List.of(mapping));
            when(rateCodeRepository.findById(RATE_CODE_ID)).thenReturn(Optional.of(currentRc));

            // TC 1010 스텁
            TransactionCode tc1010 = TransactionCode.builder()
                    .propertyId(PROPERTY_ID)
                    .transactionGroupId(1L)
                    .transactionCode("1010")
                    .codeNameKo("업그레이드 차액")
                    .revenueCategory("LODGING")
                    .codeType("CHARGE")
                    .build();
            setId(tc1010, 50L);
            when(transactionCodeRepository.findAllByPropertyIdOrderBySortOrderAscTransactionCodeAsc(PROPERTY_ID))
                    .thenReturn(List.of(tc1010));

            when(serviceItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            RoomUpgradeHistory savedHistory = RoomUpgradeHistory.builder()
                    .subReservationId(SUB_ID)
                    .fromRoomTypeId(FROM_TYPE_ID)
                    .toRoomTypeId(TO_TYPE_ID)
                    .upgradedAt(LocalDateTime.now())
                    .upgradeType("PAID")
                    .priceDifference(BigDecimal.valueOf(50_000))
                    .reason("테스트 업그레이드")
                    .build();
            setId(savedHistory, 1000L);
            when(upgradeHistoryRepository.save(any())).thenReturn(savedHistory);

            RoomUpgradeRequest request = buildRequest(TO_TYPE_ID, "PAID");

            // when
            RoomUpgradeHistoryResponse response =
                    roomUpgradeService.executeUpgrade(SUB_ID, request);

            // then
            assertThat(response.getPriceDifference()).isEqualByComparingTo(BigDecimal.valueOf(50_000));

            // 서비스 항목 저장 호출 확인
            verify(serviceItemRepository, times(1)).save(any());

            // recalculatePayment 반드시 호출
            verify(paymentService, times(1)).recalculatePayment(MASTER_ID);
        }

        @Test
        @DisplayName("업그레이드 실행 후 SubReservation의 roomTypeId가 대상 타입으로 변경된다")
        void executeUpgrade_changesRoomTypeOnSubReservation() {
            // given
            List<DailyCharge> currentCharges = List.of(
                    charge(CHECK_IN, BigDecimal.valueOf(100_000)));
            List<DailyCharge> newCharges = List.of(
                    charge(CHECK_IN, BigDecimal.valueOf(100_000)));  // 차액 없음

            when(subReservationRepository.findById(SUB_ID)).thenReturn(Optional.of(sub));
            when(roomTypeRepository.findById(FROM_TYPE_ID)).thenReturn(Optional.of(fromType));
            when(roomTypeRepository.findById(TO_TYPE_ID)).thenReturn(Optional.of(toType));
            when(roomAvailabilityService.getAvailableRoomCount(
                    eq(TO_TYPE_ID), any(), any(), anyList())).thenReturn(3);
            when(dailyChargeRepository.findBySubReservationId(SUB_ID)).thenReturn(currentCharges);
            when(priceCalculationService.calculateDailyCharges(
                    anyLong(), any(), any(), any(), anyInt(), anyInt(), any()))
                    .thenReturn(newCharges);

            RateCode currentRc = buildRateCode(RATE_CODE_ID, "RACK", "기본 레이트", true);
            RateCodeRoomType mapping = mock(RateCodeRoomType.class);
            when(mapping.getRateCodeId()).thenReturn(RATE_CODE_ID);
            when(rateCodeRoomTypeRepository.findAllByRoomTypeId(TO_TYPE_ID))
                    .thenReturn(List.of(mapping));
            when(rateCodeRepository.findById(RATE_CODE_ID)).thenReturn(Optional.of(currentRc));

            RoomUpgradeHistory savedHistory = RoomUpgradeHistory.builder()
                    .subReservationId(SUB_ID)
                    .fromRoomTypeId(FROM_TYPE_ID)
                    .toRoomTypeId(TO_TYPE_ID)
                    .upgradedAt(LocalDateTime.now())
                    .upgradeType("COMPLIMENTARY")
                    .priceDifference(BigDecimal.ZERO)
                    .reason(null)
                    .build();
            setId(savedHistory, 1001L);
            when(upgradeHistoryRepository.save(any())).thenReturn(savedHistory);

            RoomUpgradeRequest request = buildRequest(TO_TYPE_ID, "COMPLIMENTARY");

            // when
            roomUpgradeService.executeUpgrade(SUB_ID, request);

            // then: sub.changeRoomType(TO_TYPE_ID) 호출됐는지 — roomTypeId 확인
            assertThat(sub.getRoomTypeId()).isEqualTo(TO_TYPE_ID);
            assertThat(sub.getRoomNumberId()).isNull();   // 배정 초기화
            assertThat(sub.getFloorId()).isNull();         // 배정 초기화

            verify(paymentService, times(1)).recalculatePayment(MASTER_ID);
        }
    }
}
