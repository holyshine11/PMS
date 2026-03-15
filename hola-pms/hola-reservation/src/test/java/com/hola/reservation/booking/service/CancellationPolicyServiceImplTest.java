package com.hola.reservation.booking.service;

import com.hola.hotel.entity.CancellationFee;
import com.hola.hotel.entity.Property;
import com.hola.hotel.repository.CancellationFeeRepository;
import com.hola.reservation.booking.service.CancellationPolicyService.CancelFeeResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DisplayName("CancellationPolicyService - 취소 수수료 계산")
@ExtendWith(MockitoExtension.class)
class CancellationPolicyServiceImplTest {

    @InjectMocks
    private CancellationPolicyServiceImpl service;

    @Mock
    private CancellationFeeRepository cancellationFeeRepository;

    private static final Long PROPERTY_ID = 1L;
    private static final BigDecimal FIRST_NIGHT = new BigDecimal("100000");

    /**
     * CancellationFee 빌더 (테스트용)
     */
    private CancellationFee createPolicy(String checkinBasis, Integer daysBefore,
                                          String feeType, BigDecimal feeAmount, int sortOrder) {
        CancellationFee fee = CancellationFee.builder()
                .property(Property.builder().build())
                .checkinBasis(checkinBasis)
                .daysBefore(daysBefore)
                .feeType(feeType)
                .feeAmount(feeAmount)
                .build();
        fee.changeSortOrder(sortOrder);
        return fee;
    }

    @Nested
    @DisplayName("DATE 기준 수수료")
    class DateBased {

        @Test
        @DisplayName("체크인 1일 전 취소 - PERCENTAGE 50% 적용")
        void calculateCancelFee_1dayBefore_percentage50_correct() {
            // daysBefore=3(30%), daysBefore=1(50%)
            List<CancellationFee> policies = List.of(
                    createPolicy("DATE", 1, "PERCENTAGE", new BigDecimal("50"), 1),
                    createPolicy("DATE", 3, "PERCENTAGE", new BigDecimal("30"), 2)
            );
            when(cancellationFeeRepository.findAllByPropertyIdOrderBySortOrder(PROPERTY_ID))
                    .thenReturn(policies);

            // 내일 체크인 → remainingDays=1
            LocalDate checkIn = LocalDate.now().plusDays(1);
            CancelFeeResult result = service.calculateCancelFee(PROPERTY_ID, checkIn, FIRST_NIGHT);

            // daysBefore ASC 정렬 → daysBefore=1 먼저, remainingDays(1) <= daysBefore(1) 매칭
            assertThat(result.feeAmount()).isEqualByComparingTo(new BigDecimal("50000"));
            assertThat(result.feePercent()).isEqualByComparingTo(new BigDecimal("50"));
        }

        @Test
        @DisplayName("체크인 7일 전 취소 - 모든 daysBefore 초과 → 무료 취소")
        void calculateCancelFee_7daysBefore_freeCancellation() {
            List<CancellationFee> policies = List.of(
                    createPolicy("DATE", 3, "PERCENTAGE", new BigDecimal("30"), 1),
                    createPolicy("DATE", 1, "PERCENTAGE", new BigDecimal("50"), 2)
            );
            when(cancellationFeeRepository.findAllByPropertyIdOrderBySortOrder(PROPERTY_ID))
                    .thenReturn(policies);

            // 7일 후 체크인 → remainingDays=7, 모든 daysBefore(1,3) 초과
            LocalDate checkIn = LocalDate.now().plusDays(7);
            CancelFeeResult result = service.calculateCancelFee(PROPERTY_ID, checkIn, FIRST_NIGHT);

            assertThat(result.feeAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("체크인 당일 취소 - remainingDays=0, daysBefore=0 정책 매칭")
        void calculateCancelFee_sameDay_maxFee() {
            List<CancellationFee> policies = List.of(
                    createPolicy("DATE", 0, "PERCENTAGE", new BigDecimal("100"), 1),
                    createPolicy("DATE", 3, "PERCENTAGE", new BigDecimal("30"), 2)
            );
            when(cancellationFeeRepository.findAllByPropertyIdOrderBySortOrder(PROPERTY_ID))
                    .thenReturn(policies);

            LocalDate checkIn = LocalDate.now();
            CancelFeeResult result = service.calculateCancelFee(PROPERTY_ID, checkIn, FIRST_NIGHT);

            // 100% 수수료
            assertThat(result.feeAmount()).isEqualByComparingTo(FIRST_NIGHT);
        }

        @Test
        @DisplayName("FIXED_KRW 타입 - 고정 금액 수수료")
        void calculateCancelFee_fixedKrw_returnsFixedAmount() {
            List<CancellationFee> policies = List.of(
                    createPolicy("DATE", 3, "FIXED_KRW", new BigDecimal("50000"), 1)
            );
            when(cancellationFeeRepository.findAllByPropertyIdOrderBySortOrder(PROPERTY_ID))
                    .thenReturn(policies);

            LocalDate checkIn = LocalDate.now().plusDays(2);
            CancelFeeResult result = service.calculateCancelFee(PROPERTY_ID, checkIn, FIRST_NIGHT);

            assertThat(result.feeAmount()).isEqualByComparingTo(new BigDecimal("50000"));
            // 역산: 50000 * 100 / 100000 = 50%
            assertThat(result.feePercent()).isEqualByComparingTo(new BigDecimal("50"));
        }

        @Test
        @DisplayName("정책 미설정 - 무료 취소 처리 (0원)")
        void calculateCancelFee_noPolicies_freeCancellation() {
            when(cancellationFeeRepository.findAllByPropertyIdOrderBySortOrder(PROPERTY_ID))
                    .thenReturn(List.of());

            LocalDate checkIn = LocalDate.now().plusDays(1);
            CancelFeeResult result = service.calculateCancelFee(PROPERTY_ID, checkIn, FIRST_NIGHT);

            assertThat(result.feeAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.policyDescription()).contains("무료 취소");
        }

        @Test
        @DisplayName("미지원 feeType (FIXED_USD) - 0원 처리")
        void calculateCancelFee_unsupportedFeeType_zeroFee() {
            List<CancellationFee> policies = List.of(
                    createPolicy("DATE", 3, "FIXED_USD", new BigDecimal("100"), 1)
            );
            when(cancellationFeeRepository.findAllByPropertyIdOrderBySortOrder(PROPERTY_ID))
                    .thenReturn(policies);

            LocalDate checkIn = LocalDate.now().plusDays(1);
            CancelFeeResult result = service.calculateCancelFee(PROPERTY_ID, checkIn, FIRST_NIGHT);

            assertThat(result.feeAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("NOSHOW 우선 처리")
    class NoShow {

        @Test
        @DisplayName("노쇼 + NOSHOW 정책 존재 - NOSHOW 정책 우선 적용")
        void calculateCancelFee_noShow_noShowPolicyApplied() {
            List<CancellationFee> policies = List.of(
                    createPolicy("DATE", 1, "PERCENTAGE", new BigDecimal("50"), 1),
                    createPolicy("NOSHOW", null, "PERCENTAGE", new BigDecimal("100"), 2)
            );
            when(cancellationFeeRepository.findAllByPropertyIdOrderBySortOrder(PROPERTY_ID))
                    .thenReturn(policies);

            LocalDate checkIn = LocalDate.now().plusDays(1);
            CancelFeeResult result = service.calculateCancelFee(PROPERTY_ID, checkIn, FIRST_NIGHT, true);

            // NOSHOW 정책: 100% 적용
            assertThat(result.feeAmount()).isEqualByComparingTo(FIRST_NIGHT);
        }

        @Test
        @DisplayName("노쇼 + NOSHOW 정책 미설정 - DATE 기준으로 폴백")
        void calculateCancelFee_noShow_noNoShowPolicy_fallbackToDate() {
            List<CancellationFee> policies = List.of(
                    createPolicy("DATE", 1, "PERCENTAGE", new BigDecimal("50"), 1)
            );
            when(cancellationFeeRepository.findAllByPropertyIdOrderBySortOrder(PROPERTY_ID))
                    .thenReturn(policies);

            LocalDate checkIn = LocalDate.now().plusDays(1);
            CancelFeeResult result = service.calculateCancelFee(PROPERTY_ID, checkIn, FIRST_NIGHT, true);

            // DATE 정책으로 폴백: 50%
            assertThat(result.feeAmount()).isEqualByComparingTo(new BigDecimal("50000"));
        }

        @Test
        @DisplayName("일반 취소 + NOSHOW 정책 존재 - NOSHOW 무시, DATE 기준 적용")
        void calculateCancelFee_normalCancel_ignoresNoShowPolicy() {
            List<CancellationFee> policies = List.of(
                    createPolicy("DATE", 1, "PERCENTAGE", new BigDecimal("30"), 1),
                    createPolicy("NOSHOW", null, "PERCENTAGE", new BigDecimal("100"), 2)
            );
            when(cancellationFeeRepository.findAllByPropertyIdOrderBySortOrder(PROPERTY_ID))
                    .thenReturn(policies);

            LocalDate checkIn = LocalDate.now().plusDays(1);
            CancelFeeResult result = service.calculateCancelFee(PROPERTY_ID, checkIn, FIRST_NIGHT, false);

            // isNoShow=false → NOSHOW 무시, DATE 30% 적용
            assertThat(result.feeAmount()).isEqualByComparingTo(new BigDecimal("30000"));
        }
    }

    @Nested
    @DisplayName("경계값")
    class EdgeCases {

        @Test
        @DisplayName("remainingDays 음수 (체크인 지남) - 0으로 보정")
        void calculateCancelFee_pastCheckIn_remainingDaysZero() {
            List<CancellationFee> policies = List.of(
                    createPolicy("DATE", 0, "PERCENTAGE", new BigDecimal("100"), 1)
            );
            when(cancellationFeeRepository.findAllByPropertyIdOrderBySortOrder(PROPERTY_ID))
                    .thenReturn(policies);

            // 어제 체크인 → remainingDays 음수 → 0으로 보정
            LocalDate checkIn = LocalDate.now().minusDays(1);
            CancelFeeResult result = service.calculateCancelFee(PROPERTY_ID, checkIn, FIRST_NIGHT);

            // remainingDays=0 <= daysBefore=0 → 매칭
            assertThat(result.feeAmount()).isEqualByComparingTo(FIRST_NIGHT);
        }

        @Test
        @DisplayName("firstNightSupplyPrice null - PERCENTAGE 0원 결과")
        void calculateCancelFee_nullFirstNight_zeroFee() {
            List<CancellationFee> policies = List.of(
                    createPolicy("DATE", 3, "PERCENTAGE", new BigDecimal("50"), 1)
            );
            when(cancellationFeeRepository.findAllByPropertyIdOrderBySortOrder(PROPERTY_ID))
                    .thenReturn(policies);

            LocalDate checkIn = LocalDate.now().plusDays(1);
            CancelFeeResult result = service.calculateCancelFee(PROPERTY_ID, checkIn, null);

            assertThat(result.feeAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("firstNightSupplyPrice 0 - PERCENTAGE 0원, FIXED_KRW feePercent 0")
        void calculateCancelFee_zeroFirstNight_zeroPercentFee() {
            List<CancellationFee> policies = List.of(
                    createPolicy("DATE", 3, "FIXED_KRW", new BigDecimal("50000"), 1)
            );
            when(cancellationFeeRepository.findAllByPropertyIdOrderBySortOrder(PROPERTY_ID))
                    .thenReturn(policies);

            LocalDate checkIn = LocalDate.now().plusDays(1);
            CancelFeeResult result = service.calculateCancelFee(PROPERTY_ID, checkIn, BigDecimal.ZERO);

            // FIXED_KRW: 금액은 50000, 퍼센트 역산 시 firstNight=0이므로 0
            assertThat(result.feeAmount()).isEqualByComparingTo(new BigDecimal("50000"));
            assertThat(result.feePercent()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("PERCENTAGE 반올림 - HALF_UP 적용 확인")
        void calculateCancelFee_percentageRounding_halfUp() {
            List<CancellationFee> policies = List.of(
                    createPolicy("DATE", 3, "PERCENTAGE", new BigDecimal("33"), 1)
            );
            when(cancellationFeeRepository.findAllByPropertyIdOrderBySortOrder(PROPERTY_ID))
                    .thenReturn(policies);

            LocalDate checkIn = LocalDate.now().plusDays(1);
            BigDecimal firstNight = new BigDecimal("100000");
            CancelFeeResult result = service.calculateCancelFee(PROPERTY_ID, checkIn, firstNight);

            // 100000 * 33 / 100 = 33000 (정확히 나눠짐)
            assertThat(result.feeAmount()).isEqualByComparingTo(new BigDecimal("33000"));
        }

        @Test
        @DisplayName("2-arg 오버로드 호출 - isNoShow=false로 위임")
        void calculateCancelFee_twoArgOverload_delegatesWithFalse() {
            List<CancellationFee> policies = List.of(
                    createPolicy("DATE", 3, "PERCENTAGE", new BigDecimal("30"), 1),
                    createPolicy("NOSHOW", null, "PERCENTAGE", new BigDecimal("100"), 2)
            );
            when(cancellationFeeRepository.findAllByPropertyIdOrderBySortOrder(PROPERTY_ID))
                    .thenReturn(policies);

            LocalDate checkIn = LocalDate.now().plusDays(1);
            // 2-arg 호출 → isNoShow=false → NOSHOW 정책 무시
            CancelFeeResult result = service.calculateCancelFee(PROPERTY_ID, checkIn, FIRST_NIGHT);

            assertThat(result.feeAmount()).isEqualByComparingTo(new BigDecimal("30000"));
        }
    }
}
