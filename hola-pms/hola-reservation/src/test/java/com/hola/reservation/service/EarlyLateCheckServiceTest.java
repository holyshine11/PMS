package com.hola.reservation.service;

import com.hola.hotel.entity.EarlyLateFeePolicy;
import com.hola.hotel.entity.Hotel;
import com.hola.hotel.entity.Property;
import com.hola.hotel.repository.EarlyLateFeePolicyRepository;
import com.hola.reservation.entity.DailyCharge;
import com.hola.reservation.entity.MasterReservation;
import com.hola.reservation.entity.SubReservation;
import com.hola.reservation.repository.DailyChargeRepository;
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
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 얼리 체크인 / 레이트 체크아웃 요금 계산 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EarlyLateCheckService")
class EarlyLateCheckServiceTest {

    @InjectMocks
    private EarlyLateCheckService earlyLateCheckService;

    @Mock
    private EarlyLateFeePolicyRepository policyRepository;

    @Mock
    private DailyChargeRepository dailyChargeRepository;

    private Property createProperty() {
        Hotel hotel = Hotel.builder().hotelCode("HTL00001").hotelName("테스트").build();
        return Property.builder()
                .hotel(hotel).propertyCode("GMP").propertyName("테스트")
                .checkInTime("15:00").checkOutTime("11:00")
                .taxRate(BigDecimal.ZERO).serviceChargeRate(BigDecimal.ZERO)
                .taxDecimalPlaces(0).serviceChargeDecimalPlaces(0)
                .build();
    }

    private SubReservation createSub(Property property) {
        MasterReservation master = MasterReservation.builder()
                .property(property)
                .masterCheckIn(LocalDate.of(2026, 3, 15))
                .masterCheckOut(LocalDate.of(2026, 3, 18))
                .guestNameKo("테스트")
                .isOtaManaged(false)
                .subReservations(new ArrayList<>())
                .build();
        return SubReservation.builder()
                .masterReservation(master)
                .checkIn(LocalDate.of(2026, 3, 15))
                .checkOut(LocalDate.of(2026, 3, 18))
                .earlyCheckInFee(BigDecimal.ZERO)
                .lateCheckOutFee(BigDecimal.ZERO)
                .build();
    }

    private EarlyLateFeePolicy createPolicy(Property property, String type,
                                              String from, String to,
                                              String feeType, BigDecimal value) {
        return EarlyLateFeePolicy.builder()
                .property(property).policyType(type)
                .timeFrom(from).timeTo(to)
                .feeType(feeType).feeValue(value)
                .build();
    }

    private SubReservation createSubWithToggle(Property property, boolean earlyOn, boolean lateOn) {
        MasterReservation master = MasterReservation.builder()
                .property(property)
                .masterCheckIn(LocalDate.of(2026, 3, 15))
                .masterCheckOut(LocalDate.of(2026, 3, 18))
                .guestNameKo("테스트")
                .isOtaManaged(false)
                .subReservations(new ArrayList<>())
                .build();
        return SubReservation.builder()
                .masterReservation(master)
                .checkIn(LocalDate.of(2026, 3, 15))
                .checkOut(LocalDate.of(2026, 3, 18))
                .earlyCheckIn(earlyOn)
                .lateCheckOut(lateOn)
                .earlyCheckInFee(BigDecimal.ZERO)
                .lateCheckOutFee(BigDecimal.ZERO)
                .build();
    }

    private EarlyLateFeePolicy createPolicyWithDesc(Property property, String type,
                                                      String from, String to,
                                                      String feeType, BigDecimal value, String desc) {
        return EarlyLateFeePolicy.builder()
                .property(property).policyType(type)
                .timeFrom(from).timeTo(to)
                .feeType(feeType).feeValue(value).description(desc)
                .build();
    }

    private void mockDailyCharge(BigDecimal supplyPrice) {
        DailyCharge charge = DailyCharge.builder()
                .chargeDate(LocalDate.of(2026, 3, 15))
                .supplyPrice(supplyPrice)
                .tax(BigDecimal.ZERO).serviceCharge(BigDecimal.ZERO).total(supplyPrice)
                .build();
        // 가변 리스트 반환 (sort() 호출 대응)
        when(dailyChargeRepository.findBySubReservationId(any())).thenReturn(new ArrayList<>(List.of(charge)));
    }

    private void mockMultipleDailyCharges(BigDecimal firstNight, BigDecimal lastNight) {
        List<DailyCharge> charges = new ArrayList<>(List.of(
                DailyCharge.builder()
                        .chargeDate(LocalDate.of(2026, 3, 15))
                        .supplyPrice(firstNight)
                        .tax(BigDecimal.ZERO).serviceCharge(BigDecimal.ZERO).total(firstNight)
                        .build(),
                DailyCharge.builder()
                        .chargeDate(LocalDate.of(2026, 3, 17))
                        .supplyPrice(lastNight)
                        .tax(BigDecimal.ZERO).serviceCharge(BigDecimal.ZERO).total(lastNight)
                        .build()
        ));
        when(dailyChargeRepository.findBySubReservationId(any())).thenReturn(charges);
    }

    @Nested
    @DisplayName("얼리 체크인")
    class EarlyCheckIn {

        @Test
        @DisplayName("표준 시간 이전 체크인 → PERCENT 요금 계산")
        void earlyCheckIn_beforeStandard_percentFee() {
            Property property = createProperty(); // checkIn 15:00
            SubReservation sub = createSub(property);
            LocalDateTime actualTime = LocalDateTime.of(2026, 3, 15, 12, 0); // 12:00

            when(policyRepository.findAllByPropertyIdAndPolicyTypeOrderBySortOrder(any(), eq("EARLY_CHECKIN")))
                    .thenReturn(List.of(createPolicy(property, "EARLY_CHECKIN", "10:00", "15:00", "PERCENT", new BigDecimal("50"))));
            mockDailyCharge(new BigDecimal("100000"));

            BigDecimal fee = earlyLateCheckService.calculateEarlyCheckInFee(sub, actualTime);
            assertThat(fee).isEqualByComparingTo("50000"); // 100000 * 50% = 50000
        }

        @Test
        @DisplayName("표준 시간 이후 체크인 → 0원")
        void earlyCheckIn_afterStandard_zero() {
            Property property = createProperty();
            SubReservation sub = createSub(property);
            LocalDateTime actualTime = LocalDateTime.of(2026, 3, 15, 16, 0); // 16:00

            BigDecimal fee = earlyLateCheckService.calculateEarlyCheckInFee(sub, actualTime);
            assertThat(fee).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("정책 미설정 → 0원")
        void earlyCheckIn_noPolicy_zero() {
            Property property = createProperty();
            SubReservation sub = createSub(property);
            LocalDateTime actualTime = LocalDateTime.of(2026, 3, 15, 12, 0);

            when(policyRepository.findAllByPropertyIdAndPolicyTypeOrderBySortOrder(any(), eq("EARLY_CHECKIN")))
                    .thenReturn(List.of());

            BigDecimal fee = earlyLateCheckService.calculateEarlyCheckInFee(sub, actualTime);
            assertThat(fee).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("FIXED 요금 타입")
        void earlyCheckIn_fixedFee() {
            Property property = createProperty();
            SubReservation sub = createSub(property);
            LocalDateTime actualTime = LocalDateTime.of(2026, 3, 15, 13, 0);

            when(policyRepository.findAllByPropertyIdAndPolicyTypeOrderBySortOrder(any(), eq("EARLY_CHECKIN")))
                    .thenReturn(List.of(createPolicy(property, "EARLY_CHECKIN", "10:00", "15:00", "FIXED", new BigDecimal("30000"))));
            mockDailyCharge(new BigDecimal("100000"));

            BigDecimal fee = earlyLateCheckService.calculateEarlyCheckInFee(sub, actualTime);
            assertThat(fee).isEqualByComparingTo("30000");
        }

        @Test
        @DisplayName("시간대 밖 → 0원 (매칭 정책 없음)")
        void earlyCheckIn_outsideTimeRange_zero() {
            Property property = createProperty();
            SubReservation sub = createSub(property);
            LocalDateTime actualTime = LocalDateTime.of(2026, 3, 15, 8, 0); // 08:00

            when(policyRepository.findAllByPropertyIdAndPolicyTypeOrderBySortOrder(any(), eq("EARLY_CHECKIN")))
                    .thenReturn(List.of(createPolicy(property, "EARLY_CHECKIN", "10:00", "15:00", "PERCENT", new BigDecimal("50"))));

            BigDecimal fee = earlyLateCheckService.calculateEarlyCheckInFee(sub, actualTime);
            assertThat(fee).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("복수 정책 중 시간대 매칭")
        void earlyCheckIn_multiplePolicies_matchCorrect() {
            Property property = createProperty();
            SubReservation sub = createSub(property);
            LocalDateTime actualTime = LocalDateTime.of(2026, 3, 15, 11, 0); // 11:00

            List<EarlyLateFeePolicy> policies = List.of(
                    createPolicy(property, "EARLY_CHECKIN", "06:00", "10:00", "PERCENT", new BigDecimal("100")),
                    createPolicy(property, "EARLY_CHECKIN", "10:00", "13:00", "PERCENT", new BigDecimal("50")),
                    createPolicy(property, "EARLY_CHECKIN", "13:00", "15:00", "PERCENT", new BigDecimal("30"))
            );
            when(policyRepository.findAllByPropertyIdAndPolicyTypeOrderBySortOrder(any(), eq("EARLY_CHECKIN")))
                    .thenReturn(policies);
            mockDailyCharge(new BigDecimal("100000"));

            BigDecimal fee = earlyLateCheckService.calculateEarlyCheckInFee(sub, actualTime);
            assertThat(fee).isEqualByComparingTo("50000"); // 10:00~13:00 → 50%
        }

        @Test
        @DisplayName("DailyCharge 없으면 기본 0원")
        void earlyCheckIn_noDailyCharge_zero() {
            Property property = createProperty();
            SubReservation sub = createSub(property);
            LocalDateTime actualTime = LocalDateTime.of(2026, 3, 15, 12, 0);

            when(policyRepository.findAllByPropertyIdAndPolicyTypeOrderBySortOrder(any(), eq("EARLY_CHECKIN")))
                    .thenReturn(List.of(createPolicy(property, "EARLY_CHECKIN", "10:00", "15:00", "PERCENT", new BigDecimal("50"))));
            when(dailyChargeRepository.findBySubReservationId(any())).thenReturn(List.of());

            BigDecimal fee = earlyLateCheckService.calculateEarlyCheckInFee(sub, actualTime);
            assertThat(fee).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("토글 기반 얼리/레이트 요금")
    class ToggleBasedFee {

        @Test
        @DisplayName("토글 ON + 표준시간 이후 체크인 → 첫 정책 적용")
        void earlyToggleOn_afterStandard_usesFirstPolicy() {
            Property property = createProperty(); // checkIn 15:00
            SubReservation sub = createSubWithToggle(property, true, false);
            LocalDateTime actualTime = LocalDateTime.of(2026, 3, 15, 16, 0); // 16:00 (얼리 아님)

            List<EarlyLateFeePolicy> policies = List.of(
                    createPolicy(property, "EARLY_CHECKIN", "06:00", "10:00", "PERCENT", new BigDecimal("100")),
                    createPolicy(property, "EARLY_CHECKIN", "10:00", "15:00", "PERCENT", new BigDecimal("50"))
            );
            when(policyRepository.findAllByPropertyIdAndPolicyTypeOrderBySortOrder(any(), eq("EARLY_CHECKIN")))
                    .thenReturn(policies);
            mockDailyCharge(new BigDecimal("100000"));

            BigDecimal fee = earlyLateCheckService.calculateEarlyCheckInFee(sub, actualTime);
            // 토글 ON → 첫 정책(100%) 적용
            assertThat(fee).isEqualByComparingTo("100000");
        }

        @Test
        @DisplayName("토글 OFF + 표준시간 이후 체크인 → 0원")
        void earlyToggleOff_afterStandard_zero() {
            Property property = createProperty();
            SubReservation sub = createSubWithToggle(property, false, false);
            LocalDateTime actualTime = LocalDateTime.of(2026, 3, 15, 16, 0);

            BigDecimal fee = earlyLateCheckService.calculateEarlyCheckInFee(sub, actualTime);
            assertThat(fee).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("레이트 토글 ON + 표준시간 이전 체크아웃 → 첫 정책 적용")
        void lateToggleOn_beforeStandard_usesFirstPolicy() {
            Property property = createProperty(); // checkOut 11:00
            SubReservation sub = createSubWithToggle(property, false, true);
            LocalDateTime actualTime = LocalDateTime.of(2026, 3, 18, 10, 0); // 10:00 (레이트 아님)

            List<EarlyLateFeePolicy> policies = List.of(
                    createPolicy(property, "LATE_CHECKOUT", "11:00", "13:00", "FIXED", new BigDecimal("30000")),
                    createPolicy(property, "LATE_CHECKOUT", "13:00", "15:00", "FIXED", new BigDecimal("50000"))
            );
            when(policyRepository.findAllByPropertyIdAndPolicyTypeOrderBySortOrder(any(), eq("LATE_CHECKOUT")))
                    .thenReturn(policies);
            mockDailyCharge(new BigDecimal("100000"));

            BigDecimal fee = earlyLateCheckService.calculateLateCheckOutFee(sub, actualTime);
            // 토글 ON → 첫 정책(30000) 적용
            assertThat(fee).isEqualByComparingTo("30000");
        }
    }

    @Nested
    @DisplayName("estimateFees - 시간대별 예상 요금 조회")
    class EstimateFees {

        @Test
        @DisplayName("정책 2개 → 시간대별 예상 요금 2건 반환")
        void estimateFees_twoPolicies() {
            Property property = createProperty();
            SubReservation sub = createSub(property);

            List<EarlyLateFeePolicy> policies = List.of(
                    createPolicyWithDesc(property, "EARLY_CHECKIN", "06:00", "10:00", "PERCENT", new BigDecimal("100"), "새벽"),
                    createPolicyWithDesc(property, "EARLY_CHECKIN", "10:00", "15:00", "PERCENT", new BigDecimal("50"), "오전")
            );
            when(policyRepository.findAllByPropertyIdAndPolicyTypeOrderBySortOrder(any(), eq("EARLY_CHECKIN")))
                    .thenReturn(policies);
            mockDailyCharge(new BigDecimal("100000"));

            List<EarlyLateCheckService.FeeEstimate> estimates =
                    earlyLateCheckService.estimateFees(sub, "EARLY_CHECKIN", property);

            assertThat(estimates).hasSize(2);
            assertThat(estimates.get(0).estimatedFee()).isEqualByComparingTo("100000");
            assertThat(estimates.get(0).timeFrom()).isEqualTo("06:00");
            assertThat(estimates.get(1).estimatedFee()).isEqualByComparingTo("50000");
            assertThat(estimates.get(1).description()).isEqualTo("오전");
        }

        @Test
        @DisplayName("정책 없음 → 빈 리스트")
        void estimateFees_noPolicies_empty() {
            Property property = createProperty();
            SubReservation sub = createSub(property);

            when(policyRepository.findAllByPropertyIdAndPolicyTypeOrderBySortOrder(any(), eq("LATE_CHECKOUT")))
                    .thenReturn(List.of());

            List<EarlyLateCheckService.FeeEstimate> estimates =
                    earlyLateCheckService.estimateFees(sub, "LATE_CHECKOUT", property);

            assertThat(estimates).isEmpty();
        }

        @Test
        @DisplayName("레이트 체크아웃 estimateFees → 마지막 날 기준가 사용")
        void estimateFees_lateCheckout_usesLastNightRate() {
            Property property = createProperty();
            SubReservation sub = createSub(property);

            List<EarlyLateFeePolicy> policies = List.of(
                    createPolicyWithDesc(property, "LATE_CHECKOUT", "11:00", "15:00", "PERCENT", new BigDecimal("50"), "오후")
            );
            when(policyRepository.findAllByPropertyIdAndPolicyTypeOrderBySortOrder(any(), eq("LATE_CHECKOUT")))
                    .thenReturn(policies);
            // 복수 DailyCharge → 마지막 날 기준
            mockMultipleDailyCharges(new BigDecimal("80000"), new BigDecimal("120000"));

            List<EarlyLateCheckService.FeeEstimate> estimates =
                    earlyLateCheckService.estimateFees(sub, "LATE_CHECKOUT", property);

            assertThat(estimates).hasSize(1);
            assertThat(estimates.get(0).estimatedFee()).isEqualByComparingTo("60000"); // 120000 * 50%
        }
    }

    @Nested
    @DisplayName("calculateFeeByPolicyIndex - 정책 인덱스 기반 요금")
    class CalculateFeeByPolicyIndex {

        @Test
        @DisplayName("인덱스 0 → 첫 번째 정책 요금")
        void index0_firstPolicy() {
            Property property = createProperty();
            SubReservation sub = createSub(property);

            List<EarlyLateFeePolicy> policies = List.of(
                    createPolicy(property, "EARLY_CHECKIN", "06:00", "10:00", "PERCENT", new BigDecimal("100")),
                    createPolicy(property, "EARLY_CHECKIN", "10:00", "15:00", "PERCENT", new BigDecimal("50"))
            );
            when(policyRepository.findAllByPropertyIdAndPolicyTypeOrderBySortOrder(any(), eq("EARLY_CHECKIN")))
                    .thenReturn(policies);
            mockDailyCharge(new BigDecimal("100000"));

            BigDecimal fee = earlyLateCheckService.calculateFeeByPolicyIndex(sub, "EARLY_CHECKIN", 0);
            assertThat(fee).isEqualByComparingTo("100000"); // 100%
        }

        @Test
        @DisplayName("인덱스 1 → 두 번째 정책 요금")
        void index1_secondPolicy() {
            Property property = createProperty();
            SubReservation sub = createSub(property);

            List<EarlyLateFeePolicy> policies = List.of(
                    createPolicy(property, "EARLY_CHECKIN", "06:00", "10:00", "PERCENT", new BigDecimal("100")),
                    createPolicy(property, "EARLY_CHECKIN", "10:00", "15:00", "PERCENT", new BigDecimal("50"))
            );
            when(policyRepository.findAllByPropertyIdAndPolicyTypeOrderBySortOrder(any(), eq("EARLY_CHECKIN")))
                    .thenReturn(policies);
            mockDailyCharge(new BigDecimal("100000"));

            BigDecimal fee = earlyLateCheckService.calculateFeeByPolicyIndex(sub, "EARLY_CHECKIN", 1);
            assertThat(fee).isEqualByComparingTo("50000"); // 50%
        }

        @Test
        @DisplayName("유효하지 않은 인덱스 → ZERO")
        void invalidIndex_zero() {
            Property property = createProperty();
            SubReservation sub = createSub(property);

            when(policyRepository.findAllByPropertyIdAndPolicyTypeOrderBySortOrder(any(), eq("EARLY_CHECKIN")))
                    .thenReturn(List.of(createPolicy(property, "EARLY_CHECKIN", "06:00", "15:00", "FIXED", new BigDecimal("30000"))));

            BigDecimal fee = earlyLateCheckService.calculateFeeByPolicyIndex(sub, "EARLY_CHECKIN", 5);
            assertThat(fee).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("음수 인덱스 → ZERO")
        void negativeIndex_zero() {
            Property property = createProperty();
            SubReservation sub = createSub(property);

            when(policyRepository.findAllByPropertyIdAndPolicyTypeOrderBySortOrder(any(), eq("EARLY_CHECKIN")))
                    .thenReturn(List.of(createPolicy(property, "EARLY_CHECKIN", "06:00", "15:00", "FIXED", new BigDecimal("30000"))));

            BigDecimal fee = earlyLateCheckService.calculateFeeByPolicyIndex(sub, "EARLY_CHECKIN", -1);
            assertThat(fee).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("정책 없음 → ZERO")
        void noPolicies_zero() {
            Property property = createProperty();
            SubReservation sub = createSub(property);

            when(policyRepository.findAllByPropertyIdAndPolicyTypeOrderBySortOrder(any(), eq("LATE_CHECKOUT")))
                    .thenReturn(List.of());

            BigDecimal fee = earlyLateCheckService.calculateFeeByPolicyIndex(sub, "LATE_CHECKOUT", 0);
            assertThat(fee).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("레이트 체크아웃")
    class LateCheckOut {

        @Test
        @DisplayName("표준 시간 이후 체크아웃 → PERCENT 요금 계산")
        void lateCheckOut_afterStandard_percentFee() {
            Property property = createProperty(); // checkOut 11:00
            SubReservation sub = createSub(property);
            LocalDateTime actualTime = LocalDateTime.of(2026, 3, 18, 13, 0); // 13:00

            when(policyRepository.findAllByPropertyIdAndPolicyTypeOrderBySortOrder(any(), eq("LATE_CHECKOUT")))
                    .thenReturn(List.of(createPolicy(property, "LATE_CHECKOUT", "11:00", "15:00", "PERCENT", new BigDecimal("50"))));
            mockDailyCharge(new BigDecimal("100000"));

            BigDecimal fee = earlyLateCheckService.calculateLateCheckOutFee(sub, actualTime);
            assertThat(fee).isEqualByComparingTo("50000");
        }

        @Test
        @DisplayName("표준 시간 이전 체크아웃 → 0원")
        void lateCheckOut_beforeStandard_zero() {
            Property property = createProperty();
            SubReservation sub = createSub(property);
            LocalDateTime actualTime = LocalDateTime.of(2026, 3, 18, 10, 0);

            BigDecimal fee = earlyLateCheckService.calculateLateCheckOutFee(sub, actualTime);
            assertThat(fee).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("정책 미설정 → 0원")
        void lateCheckOut_noPolicy_zero() {
            Property property = createProperty();
            SubReservation sub = createSub(property);
            LocalDateTime actualTime = LocalDateTime.of(2026, 3, 18, 14, 0);

            when(policyRepository.findAllByPropertyIdAndPolicyTypeOrderBySortOrder(any(), eq("LATE_CHECKOUT")))
                    .thenReturn(List.of());

            BigDecimal fee = earlyLateCheckService.calculateLateCheckOutFee(sub, actualTime);
            assertThat(fee).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("FIXED 타입 요금")
        void lateCheckOut_fixedFee() {
            Property property = createProperty();
            SubReservation sub = createSub(property);
            LocalDateTime actualTime = LocalDateTime.of(2026, 3, 18, 14, 0);

            when(policyRepository.findAllByPropertyIdAndPolicyTypeOrderBySortOrder(any(), eq("LATE_CHECKOUT")))
                    .thenReturn(List.of(createPolicy(property, "LATE_CHECKOUT", "11:00", "15:00", "FIXED", new BigDecimal("25000"))));
            mockDailyCharge(new BigDecimal("100000"));

            BigDecimal fee = earlyLateCheckService.calculateLateCheckOutFee(sub, actualTime);
            assertThat(fee).isEqualByComparingTo("25000");
        }

        @Test
        @DisplayName("HALF_UP 반올림 검증")
        void lateCheckOut_halfUpRounding() {
            Property property = createProperty();
            SubReservation sub = createSub(property);
            LocalDateTime actualTime = LocalDateTime.of(2026, 3, 18, 13, 0);

            when(policyRepository.findAllByPropertyIdAndPolicyTypeOrderBySortOrder(any(), eq("LATE_CHECKOUT")))
                    .thenReturn(List.of(createPolicy(property, "LATE_CHECKOUT", "11:00", "15:00", "PERCENT", new BigDecimal("33"))));
            mockDailyCharge(new BigDecimal("100000"));

            BigDecimal fee = earlyLateCheckService.calculateLateCheckOutFee(sub, actualTime);
            // 100000 * 33 / 100 = 33000 (정수)
            assertThat(fee).isEqualByComparingTo("33000");
        }

        @Test
        @DisplayName("정확히 표준 시간 → 0원 (isAfter이므로 같으면 레이트 아님)")
        void lateCheckOut_exactlyStandard_zero() {
            Property property = createProperty();
            SubReservation sub = createSub(property);
            LocalDateTime actualTime = LocalDateTime.of(2026, 3, 18, 11, 0);

            BigDecimal fee = earlyLateCheckService.calculateLateCheckOutFee(sub, actualTime);
            assertThat(fee).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }
}
