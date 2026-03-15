package com.hola.reservation.service;

import com.hola.common.exception.HolaException;
import com.hola.hotel.entity.Property;
import com.hola.rate.entity.RatePricing;
import com.hola.rate.entity.RatePricingPerson;
import com.hola.rate.repository.RatePricingRepository;
import com.hola.reservation.entity.DailyCharge;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@DisplayName("PriceCalculationService - 가격 자동 계산 엔진")
@ExtendWith(MockitoExtension.class)
class PriceCalculationServiceTest {

    @InjectMocks
    private PriceCalculationService service;

    @Mock
    private RatePricingRepository ratePricingRepository;

    private static final Long RATE_CODE_ID = 1L;

    /**
     * 모든 요일 적용 + 기간 범위가 넓은 기본 요금표 생성
     */
    private RatePricing createAllDayPricing(BigDecimal basePrice) {
        return RatePricing.builder()
                .rateCodeId(RATE_CODE_ID)
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .baseSupplyPrice(basePrice)
                .dayMon(true).dayTue(true).dayWed(true).dayThu(true)
                .dayFri(true).daySat(true).daySun(true)
                .persons(new ArrayList<>())
                .build();
    }

    /**
     * 평일만 적용 요금표
     */
    private RatePricing createWeekdayPricing(BigDecimal basePrice) {
        return RatePricing.builder()
                .rateCodeId(RATE_CODE_ID)
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .baseSupplyPrice(basePrice)
                .dayMon(true).dayTue(true).dayWed(true).dayThu(true)
                .dayFri(true).daySat(false).daySun(false)
                .persons(new ArrayList<>())
                .build();
    }

    /**
     * 주말만 적용 요금표
     */
    private RatePricing createWeekendPricing(BigDecimal basePrice) {
        return RatePricing.builder()
                .rateCodeId(RATE_CODE_ID)
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .baseSupplyPrice(basePrice)
                .dayMon(false).dayTue(false).dayWed(false).dayThu(false)
                .dayFri(false).daySat(true).daySun(true)
                .persons(new ArrayList<>())
                .build();
    }

    /**
     * 세금/봉사료 설정이 있는 프로퍼티
     */
    private Property createProperty(BigDecimal taxRate, BigDecimal serviceChargeRate) {
        return Property.builder()
                .taxRate(taxRate)
                .taxDecimalPlaces(0)
                .taxRoundingMethod("HALF_UP")
                .serviceChargeRate(serviceChargeRate)
                .serviceChargeDecimalPlaces(0)
                .serviceChargeRoundingMethod("HALF_UP")
                .build();
    }

    private Property createPropertyNoTax() {
        return Property.builder().build();
    }

    @Nested
    @DisplayName("기본 요금 계산")
    class BasicCalculation {

        @Test
        @DisplayName("1박 기본 요금 계산 - 정상 케이스")
        void calculateDailyCharges_singleNight_returnsCorrectCharge() {
            RatePricing pricing = createAllDayPricing(new BigDecimal("100000"));
            when(ratePricingRepository.findAllByRateCodeIdOrderByIdAsc(RATE_CODE_ID))
                    .thenReturn(List.of(pricing));

            // 월요일 체크인 1박
            LocalDate checkIn = LocalDate.of(2026, 4, 6); // 월
            LocalDate checkOut = LocalDate.of(2026, 4, 7); // 화

            List<DailyCharge> charges = service.calculateDailyCharges(
                    RATE_CODE_ID, createPropertyNoTax(), checkIn, checkOut, 1, 0, null);

            assertThat(charges).hasSize(1);
            assertThat(charges.get(0).getSupplyPrice()).isEqualByComparingTo(new BigDecimal("100000"));
            assertThat(charges.get(0).getChargeDate()).isEqualTo(checkIn);
        }

        @Test
        @DisplayName("3박 일별 요금 계산 - 각 날짜별 독립 계산 확인")
        void calculateDailyCharges_multipleNights_returnsChargePerDay() {
            RatePricing pricing = createAllDayPricing(new BigDecimal("150000"));
            when(ratePricingRepository.findAllByRateCodeIdOrderByIdAsc(RATE_CODE_ID))
                    .thenReturn(List.of(pricing));

            LocalDate checkIn = LocalDate.of(2026, 4, 6);
            LocalDate checkOut = LocalDate.of(2026, 4, 9);

            List<DailyCharge> charges = service.calculateDailyCharges(
                    RATE_CODE_ID, createPropertyNoTax(), checkIn, checkOut, 1, 0, null);

            assertThat(charges).hasSize(3);
            charges.forEach(c ->
                    assertThat(c.getSupplyPrice()).isEqualByComparingTo(new BigDecimal("150000"))
            );
        }

        @Test
        @DisplayName("주중/주말 요금 차등 - 금/토 요금이 평일과 다른 경우")
        void calculateDailyCharges_weekendDifferentRate_appliesCorrectly() {
            RatePricing weekday = createWeekdayPricing(new BigDecimal("100000"));
            RatePricing weekend = createWeekendPricing(new BigDecimal("150000"));
            when(ratePricingRepository.findAllByRateCodeIdOrderByIdAsc(RATE_CODE_ID))
                    .thenReturn(List.of(weekday, weekend));

            // 금(4/10)→토(4/11)→일(4/12) 3박
            LocalDate checkIn = LocalDate.of(2026, 4, 10); // 금
            LocalDate checkOut = LocalDate.of(2026, 4, 13); // 월

            List<DailyCharge> charges = service.calculateDailyCharges(
                    RATE_CODE_ID, createPropertyNoTax(), checkIn, checkOut, 1, 0, null);

            assertThat(charges).hasSize(3);
            // 금(평일): 100000
            assertThat(charges.get(0).getSupplyPrice()).isEqualByComparingTo(new BigDecimal("100000"));
            // 토(주말): 150000
            assertThat(charges.get(1).getSupplyPrice()).isEqualByComparingTo(new BigDecimal("150000"));
            // 일(주말): 150000
            assertThat(charges.get(2).getSupplyPrice()).isEqualByComparingTo(new BigDecimal("150000"));
        }
    }

    @Nested
    @DisplayName("인원 추가 요금")
    class PersonExtra {

        @Test
        @DisplayName("성인 2명 - ADULT seq=2 추가 요금 적용")
        void calculateDailyCharges_twoAdults_addsAdultExtra() {
            RatePricing pricing = createAllDayPricing(new BigDecimal("100000"));
            pricing.getPersons().add(RatePricingPerson.builder()
                    .personType("ADULT").personSeq(2).supplyPrice(new BigDecimal("20000")).build());
            when(ratePricingRepository.findAllByRateCodeIdOrderByIdAsc(RATE_CODE_ID))
                    .thenReturn(List.of(pricing));

            LocalDate checkIn = LocalDate.of(2026, 4, 6);
            LocalDate checkOut = LocalDate.of(2026, 4, 7);

            List<DailyCharge> charges = service.calculateDailyCharges(
                    RATE_CODE_ID, createPropertyNoTax(), checkIn, checkOut, 2, 0, null);

            // 100000 + 20000 = 120000
            assertThat(charges.get(0).getSupplyPrice()).isEqualByComparingTo(new BigDecimal("120000"));
        }

        @Test
        @DisplayName("성인 3명 - ADULT seq=2,3 추가 요금 누적")
        void calculateDailyCharges_threeAdults_addsMultipleAdultExtras() {
            RatePricing pricing = createAllDayPricing(new BigDecimal("100000"));
            pricing.getPersons().add(RatePricingPerson.builder()
                    .personType("ADULT").personSeq(2).supplyPrice(new BigDecimal("20000")).build());
            pricing.getPersons().add(RatePricingPerson.builder()
                    .personType("ADULT").personSeq(3).supplyPrice(new BigDecimal("20000")).build());
            when(ratePricingRepository.findAllByRateCodeIdOrderByIdAsc(RATE_CODE_ID))
                    .thenReturn(List.of(pricing));

            LocalDate checkIn = LocalDate.of(2026, 4, 6);
            LocalDate checkOut = LocalDate.of(2026, 4, 7);

            List<DailyCharge> charges = service.calculateDailyCharges(
                    RATE_CODE_ID, createPropertyNoTax(), checkIn, checkOut, 3, 0, null);

            // 100000 + 20000 + 20000 = 140000
            assertThat(charges.get(0).getSupplyPrice()).isEqualByComparingTo(new BigDecimal("140000"));
        }

        @Test
        @DisplayName("아동 1명 - CHILD seq=1 추가 요금 적용")
        void calculateDailyCharges_oneChild_addsChildExtra() {
            RatePricing pricing = createAllDayPricing(new BigDecimal("100000"));
            pricing.getPersons().add(RatePricingPerson.builder()
                    .personType("CHILD").personSeq(1).supplyPrice(new BigDecimal("10000")).build());
            when(ratePricingRepository.findAllByRateCodeIdOrderByIdAsc(RATE_CODE_ID))
                    .thenReturn(List.of(pricing));

            LocalDate checkIn = LocalDate.of(2026, 4, 6);
            LocalDate checkOut = LocalDate.of(2026, 4, 7);

            List<DailyCharge> charges = service.calculateDailyCharges(
                    RATE_CODE_ID, createPropertyNoTax(), checkIn, checkOut, 1, 1, null);

            // 100000 + 10000 = 110000
            assertThat(charges.get(0).getSupplyPrice()).isEqualByComparingTo(new BigDecimal("110000"));
        }

        @Test
        @DisplayName("추가 인원 요금표 미설정 - 추가 요금 0원")
        void calculateDailyCharges_noPersonPricing_zeroExtra() {
            RatePricing pricing = createAllDayPricing(new BigDecimal("100000"));
            when(ratePricingRepository.findAllByRateCodeIdOrderByIdAsc(RATE_CODE_ID))
                    .thenReturn(List.of(pricing));

            LocalDate checkIn = LocalDate.of(2026, 4, 6);
            LocalDate checkOut = LocalDate.of(2026, 4, 7);

            List<DailyCharge> charges = service.calculateDailyCharges(
                    RATE_CODE_ID, createPropertyNoTax(), checkIn, checkOut, 3, 2, null);

            // 추가 요금 없이 기본 요금만
            assertThat(charges.get(0).getSupplyPrice()).isEqualByComparingTo(new BigDecimal("100000"));
        }
    }

    @Nested
    @DisplayName("세금/봉사료")
    class TaxAndServiceCharge {

        @Test
        @DisplayName("세금 10% 적용 - 공급가 * taxRate / 100")
        void calculateDailyCharges_withTaxRate_calculatesTax() {
            RatePricing pricing = createAllDayPricing(new BigDecimal("100000"));
            when(ratePricingRepository.findAllByRateCodeIdOrderByIdAsc(RATE_CODE_ID))
                    .thenReturn(List.of(pricing));

            Property property = createProperty(new BigDecimal("10"), BigDecimal.ZERO);

            LocalDate checkIn = LocalDate.of(2026, 4, 6);
            LocalDate checkOut = LocalDate.of(2026, 4, 7);

            List<DailyCharge> charges = service.calculateDailyCharges(
                    RATE_CODE_ID, property, checkIn, checkOut, 1, 0, null);

            assertThat(charges.get(0).getTax()).isEqualByComparingTo(new BigDecimal("10000"));
            assertThat(charges.get(0).getTotal()).isEqualByComparingTo(new BigDecimal("110000"));
        }

        @Test
        @DisplayName("봉사료 5% 적용 - 공급가 * serviceChargeRate / 100")
        void calculateDailyCharges_withServiceCharge_calculatesServiceCharge() {
            RatePricing pricing = createAllDayPricing(new BigDecimal("200000"));
            when(ratePricingRepository.findAllByRateCodeIdOrderByIdAsc(RATE_CODE_ID))
                    .thenReturn(List.of(pricing));

            Property property = createProperty(BigDecimal.ZERO, new BigDecimal("5"));

            LocalDate checkIn = LocalDate.of(2026, 4, 6);
            LocalDate checkOut = LocalDate.of(2026, 4, 7);

            List<DailyCharge> charges = service.calculateDailyCharges(
                    RATE_CODE_ID, property, checkIn, checkOut, 1, 0, null);

            assertThat(charges.get(0).getServiceCharge()).isEqualByComparingTo(new BigDecimal("10000"));
        }

        @Test
        @DisplayName("세금+봉사료 복합 - total = supply + tax + serviceCharge")
        void calculateDailyCharges_taxAndServiceCharge_correctTotal() {
            RatePricing pricing = createAllDayPricing(new BigDecimal("100000"));
            when(ratePricingRepository.findAllByRateCodeIdOrderByIdAsc(RATE_CODE_ID))
                    .thenReturn(List.of(pricing));

            Property property = createProperty(new BigDecimal("10"), new BigDecimal("5"));

            LocalDate checkIn = LocalDate.of(2026, 4, 6);
            LocalDate checkOut = LocalDate.of(2026, 4, 7);

            List<DailyCharge> charges = service.calculateDailyCharges(
                    RATE_CODE_ID, property, checkIn, checkOut, 1, 0, null);

            // supply=100000, tax=10000, service=5000, total=115000
            assertThat(charges.get(0).getSupplyPrice()).isEqualByComparingTo(new BigDecimal("100000"));
            assertThat(charges.get(0).getTax()).isEqualByComparingTo(new BigDecimal("10000"));
            assertThat(charges.get(0).getServiceCharge()).isEqualByComparingTo(new BigDecimal("5000"));
            assertThat(charges.get(0).getTotal()).isEqualByComparingTo(new BigDecimal("115000"));
        }

        @Test
        @DisplayName("세금율 null - 세금 0원 처리")
        void calculateDailyCharges_nullTaxRate_zeroTax() {
            RatePricing pricing = createAllDayPricing(new BigDecimal("100000"));
            when(ratePricingRepository.findAllByRateCodeIdOrderByIdAsc(RATE_CODE_ID))
                    .thenReturn(List.of(pricing));

            List<DailyCharge> charges = service.calculateDailyCharges(
                    RATE_CODE_ID, createPropertyNoTax(), LocalDate.of(2026, 4, 6),
                    LocalDate.of(2026, 4, 7), 1, 0, null);

            assertThat(charges.get(0).getTax()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("반올림 ROUND_DOWN - 소수점 내림 적용")
        void calculateDailyCharges_roundDown_appliesFloor() {
            RatePricing pricing = createAllDayPricing(new BigDecimal("99999"));
            when(ratePricingRepository.findAllByRateCodeIdOrderByIdAsc(RATE_CODE_ID))
                    .thenReturn(List.of(pricing));

            Property property = Property.builder()
                    .taxRate(new BigDecimal("10"))
                    .taxDecimalPlaces(0)
                    .taxRoundingMethod("ROUND_DOWN")
                    .build();

            List<DailyCharge> charges = service.calculateDailyCharges(
                    RATE_CODE_ID, property, LocalDate.of(2026, 4, 6),
                    LocalDate.of(2026, 4, 7), 1, 0, null);

            // 99999 * 10 / 100 = 9999.9 → ROUND_DOWN → 9999
            assertThat(charges.get(0).getTax()).isEqualByComparingTo(new BigDecimal("9999"));
        }
    }

    @Nested
    @DisplayName("에러 케이스")
    class ErrorCases {

        @Test
        @DisplayName("해당 날짜에 매칭 요금표 없음 - RESERVATION_RATE_NOT_APPLICABLE 예외")
        void calculateDailyCharges_noPricingForDate_throwsException() {
            // 기간 밖 요금표
            RatePricing pricing = RatePricing.builder()
                    .rateCodeId(RATE_CODE_ID)
                    .startDate(LocalDate.of(2026, 1, 1))
                    .endDate(LocalDate.of(2026, 3, 31))
                    .baseSupplyPrice(new BigDecimal("100000"))
                    .dayMon(true).dayTue(true).dayWed(true).dayThu(true)
                    .dayFri(true).daySat(true).daySun(true)
                    .persons(new ArrayList<>())
                    .build();
            when(ratePricingRepository.findAllByRateCodeIdOrderByIdAsc(RATE_CODE_ID))
                    .thenReturn(List.of(pricing));

            // 4월 체크인 → 3월까지 요금표만 있음
            assertThatThrownBy(() -> service.calculateDailyCharges(
                    RATE_CODE_ID, createPropertyNoTax(),
                    LocalDate.of(2026, 4, 6), LocalDate.of(2026, 4, 7), 1, 0, null))
                    .isInstanceOf(HolaException.class);
        }

        @Test
        @DisplayName("빈 요금표 목록 - 예외 발생")
        void calculateDailyCharges_emptyPricingList_throwsException() {
            when(ratePricingRepository.findAllByRateCodeIdOrderByIdAsc(RATE_CODE_ID))
                    .thenReturn(List.of());

            assertThatThrownBy(() -> service.calculateDailyCharges(
                    RATE_CODE_ID, createPropertyNoTax(),
                    LocalDate.of(2026, 4, 6), LocalDate.of(2026, 4, 7), 1, 0, null))
                    .isInstanceOf(HolaException.class);
        }
    }

    @Nested
    @DisplayName("커버리지 검증")
    class CoverageValidation {

        @Test
        @DisplayName("요금 커버리지 정상 - 모든 날짜 커버됨")
        void validatePricingCoverage_allDatesCovered_noException() {
            RatePricing pricing = createAllDayPricing(new BigDecimal("100000"));
            when(ratePricingRepository.findAllByRateCodeIdOrderByIdAsc(RATE_CODE_ID))
                    .thenReturn(List.of(pricing));

            // 예외 없이 정상 통과
            service.validatePricingCoverage(RATE_CODE_ID,
                    LocalDate.of(2026, 4, 6), LocalDate.of(2026, 4, 9));
        }

        @Test
        @DisplayName("빈 요금 목록 - 커버리지 검증 예외")
        void validatePricingCoverage_emptyPricing_throwsException() {
            when(ratePricingRepository.findAllByRateCodeIdOrderByIdAsc(RATE_CODE_ID))
                    .thenReturn(List.of());

            assertThatThrownBy(() -> service.validatePricingCoverage(RATE_CODE_ID,
                    LocalDate.of(2026, 4, 6), LocalDate.of(2026, 4, 9)))
                    .isInstanceOf(HolaException.class);
        }

        @Test
        @DisplayName("요일 미커버 - 토요일 false인 요금표로 토요일 포함 시 예외")
        void validatePricingCoverage_dayNotCovered_throwsException() {
            // 평일만 적용 요금표 (토/일 미커버)
            RatePricing weekdayOnly = createWeekdayPricing(new BigDecimal("100000"));
            when(ratePricingRepository.findAllByRateCodeIdOrderByIdAsc(RATE_CODE_ID))
                    .thenReturn(List.of(weekdayOnly));

            // 금(4/10) → 일(4/12) 2박, 토요일 미커버
            assertThatThrownBy(() -> service.validatePricingCoverage(RATE_CODE_ID,
                    LocalDate.of(2026, 4, 10), LocalDate.of(2026, 4, 12)))
                    .isInstanceOf(HolaException.class)
                    .hasMessageContaining("토");
        }
    }
}
