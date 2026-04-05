package com.hola.reservation.service;

import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.hotel.entity.Property;
import com.hola.rate.entity.RatePricing;
import com.hola.rate.entity.RatePricingPerson;
import com.hola.rate.repository.RatePricingRepository;
import com.hola.reservation.entity.DailyCharge;
import com.hola.reservation.entity.SubReservation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 가격 자동 계산 엔진
 *
 * 계산 흐름:
 * 1. RateCode → RatePricing 목록 조회 (해당 레이트코드의 요금표)
 * 2. 체크인 ~ (체크아웃-1) 각 날짜에 대해:
 *    a. 요일 판단 (월~일)
 *    b. 해당 요일이 적용되는 RatePricing 찾기 (dayMon~daySun flag)
 *    c. 기본 요금 = baseSupplyPrice
 *    d. 인원 추가 요금 = RatePricingPerson에서 ADULT/CHILD별 추가분 합산
 *    e. 소계 = 기본 + 인원추가
 *    f. 봉사료 = 소계 × Property.serviceChargeRate
 *    g. 세금 = (소계 + 봉사료) × Property.taxRate  ← 봉사료에도 VAT 적용 (업계 표준)
 *    h. 합계 = 소계 + 봉사료 + 세금
 * 3. DailyCharge 리스트 반환
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PriceCalculationService {

    private final RatePricingRepository ratePricingRepository;

    /**
     * 일별 요금 계산
     *
     * @param rateCodeId  레이트코드 ID
     * @param property    프로퍼티 (세금/봉사료율 참조)
     * @param checkIn     체크인일
     * @param checkOut    체크아웃일
     * @param adults      성인 수
     * @param children    아동 수
     * @param subReservation 서브예약 엔티티 (DailyCharge 연결용, nullable)
     * @return 일별 요금 리스트
     */
    public List<DailyCharge> calculateDailyCharges(Long rateCodeId, Property property,
                                                    LocalDate checkIn, LocalDate checkOut,
                                                    int adults, int children,
                                                    SubReservation subReservation) {
        // 레이트코드의 요금표 조회
        List<RatePricing> pricingList = ratePricingRepository.findAllByRateCodeIdOrderByIdAsc(rateCodeId);

        List<DailyCharge> dailyCharges = new ArrayList<>();

        // 체크인일 ~ 체크아웃 전일까지
        LocalDate date = checkIn;
        while (date.isBefore(checkOut)) {
            DailyCharge charge = calculateForDate(date, pricingList, property, adults, children, subReservation);
            dailyCharges.add(charge);
            date = date.plusDays(1);
        }

        log.debug("일별 요금 계산 완료: rateCodeId={}, {}~{}, {}박, 총액={}",
                rateCodeId, checkIn, checkOut, dailyCharges.size(),
                dailyCharges.stream().map(DailyCharge::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add));

        return dailyCharges;
    }

    /**
     * 특정 날짜의 요금 계산
     */
    private DailyCharge calculateForDate(LocalDate date, List<RatePricing> pricingList,
                                          Property property, int adults, int children,
                                          SubReservation subReservation) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();

        // 해당 날짜(기간+요일)에 적용되는 요금표 찾기
        RatePricing pricing = findPricingForDate(pricingList, date);

        BigDecimal supplyPrice;
        if (pricing != null) {
            // 기본 요금
            BigDecimal basePrice = pricing.getBaseSupplyPrice();

            // 인원 추가 요금
            BigDecimal personExtra = calculatePersonExtra(pricing, adults, children);

            supplyPrice = basePrice.add(personExtra);
        } else {
            // 적용 가능한 요금표가 없으면 예외 발생 (0원 요금 방지)
            log.error("{}({})에 적용 가능한 요금표 없음 — 예약 생성 차단", date, dayOfWeek);
            throw new HolaException(ErrorCode.RESERVATION_RATE_NOT_APPLICABLE);
        }

        // 봉사료/세금 계산 (공통 유틸 사용)
        TaxCalculationResult taxResult = calculateTaxAndServiceCharge(supplyPrice, property);
        BigDecimal serviceCharge = taxResult.serviceCharge();
        BigDecimal tax = taxResult.tax();
        BigDecimal total = taxResult.total();

        return DailyCharge.builder()
                .subReservation(subReservation)
                .chargeDate(date)
                .supplyPrice(supplyPrice)
                .tax(tax)
                .serviceCharge(serviceCharge)
                .total(total)
                .build();
    }

    /**
     * 요금 커버리지 검증: 체크인~체크아웃 전일까지 모든 날짜에 매칭되는 요금행이 있는지 확인
     * 커버되지 않는 날짜가 있으면 HolaException 발생
     */
    public void validatePricingCoverage(Long rateCodeId, LocalDate checkIn, LocalDate checkOut) {
        List<RatePricing> pricingList = ratePricingRepository.findAllByRateCodeIdOrderByIdAsc(rateCodeId);

        if (pricingList.isEmpty()) {
            throw new HolaException(ErrorCode.RESERVATION_RATE_NOT_APPLICABLE,
                    "레이트코드에 설정된 요금 정보가 없습니다.");
        }

        LocalDate date = checkIn;
        while (date.isBefore(checkOut)) {
            RatePricing pricing = findPricingForDate(pricingList, date);
            if (pricing == null) {
                String dayName = switch (date.getDayOfWeek()) {
                    case MONDAY -> "월"; case TUESDAY -> "화"; case WEDNESDAY -> "수";
                    case THURSDAY -> "목"; case FRIDAY -> "금"; case SATURDAY -> "토"; case SUNDAY -> "일";
                };
                throw new HolaException(ErrorCode.RESERVATION_RATE_NOT_APPLICABLE,
                        date + "(" + dayName + ")에 적용 가능한 요금 설정이 없습니다. 레이트코드의 요금정보를 확인해주세요.");
            }
            date = date.plusDays(1);
        }
    }

    /**
     * 요금 커버리지 여부 확인 (예외 미발생, boolean 반환)
     * 트랜잭션 rollback 마킹 없이 안전하게 호출 가능
     */
    public boolean hasPricingCoverage(Long rateCodeId, LocalDate checkIn, LocalDate checkOut) {
        List<RatePricing> pricingList = ratePricingRepository.findAllByRateCodeIdOrderByIdAsc(rateCodeId);
        if (pricingList.isEmpty()) {
            return false;
        }
        LocalDate date = checkIn;
        while (date.isBefore(checkOut)) {
            RatePricing pricing = findPricingForDate(pricingList, date);
            if (pricing == null) {
                return false;
            }
            date = date.plusDays(1);
        }
        return true;
    }

    /**
     * 날짜(기간+요일)에 맞는 요금표 찾기
     * - 기간 중복 시 가장 좁은 기간(구체적 요금)을 우선 선택
     * - Boolean null 안전 처리 (getDayXxx() null → false 취급)
     */
    private RatePricing findPricingForDate(List<RatePricing> pricingList, LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        RatePricing bestMatch = null;
        long bestSpan = Long.MAX_VALUE;

        for (RatePricing pricing : pricingList) {
            // 기간 매칭
            if (date.isBefore(pricing.getStartDate()) || date.isAfter(pricing.getEndDate())) {
                continue;
            }
            // 요일 매칭 (Boolean null 안전)
            boolean matches = switch (dayOfWeek) {
                case MONDAY -> Boolean.TRUE.equals(pricing.getDayMon());
                case TUESDAY -> Boolean.TRUE.equals(pricing.getDayTue());
                case WEDNESDAY -> Boolean.TRUE.equals(pricing.getDayWed());
                case THURSDAY -> Boolean.TRUE.equals(pricing.getDayThu());
                case FRIDAY -> Boolean.TRUE.equals(pricing.getDayFri());
                case SATURDAY -> Boolean.TRUE.equals(pricing.getDaySat());
                case SUNDAY -> Boolean.TRUE.equals(pricing.getDaySun());
            };
            if (matches) {
                // 기간 폭이 좁을수록 구체적 요금 → 우선 선택
                long span = java.time.temporal.ChronoUnit.DAYS.between(
                        pricing.getStartDate(), pricing.getEndDate());
                if (span < bestSpan) {
                    bestSpan = span;
                    bestMatch = pricing;
                }
            }
        }
        return bestMatch;
    }

    /**
     * 인원 추가 요금 계산
     * - 기본 요금은 1인 기준, 추가 인원은 RatePricingPerson에서 조회
     * - adults > 1이면 ADULT 추가분, children > 0이면 CHILD 추가분
     */
    private BigDecimal calculatePersonExtra(RatePricing pricing, int adults, int children) {
        BigDecimal extra = BigDecimal.ZERO;
        List<RatePricingPerson> persons = pricing.getPersons();

        if (persons == null || persons.isEmpty()) {
            return extra;
        }

        // 성인 추가 (2번째 성인부터)
        for (int seq = 2; seq <= adults; seq++) {
            final int targetSeq = seq;
            BigDecimal adultExtra = persons.stream()
                    .filter(p -> "ADULT".equals(p.getPersonType()) && p.getPersonSeq() == targetSeq)
                    .map(RatePricingPerson::getSupplyPrice)
                    .findFirst()
                    .orElse(null);
            if (adultExtra == null) {
                log.warn("인원 추가 요금 미정의: ADULT seq={}, rateCodePricingId={} → 0원 처리",
                        targetSeq, pricing.getId());
                adultExtra = BigDecimal.ZERO;
            }
            extra = extra.add(adultExtra);
        }

        // 아동 추가
        for (int seq = 1; seq <= children; seq++) {
            final int targetSeq = seq;
            BigDecimal childExtra = persons.stream()
                    .filter(p -> "CHILD".equals(p.getPersonType()) && p.getPersonSeq() == targetSeq)
                    .map(RatePricingPerson::getSupplyPrice)
                    .findFirst()
                    .orElse(null);
            if (childExtra == null) {
                log.warn("인원 추가 요금 미정의: CHILD seq={}, rateCodePricingId={} → 0원 처리",
                        targetSeq, pricing.getId());
                childExtra = BigDecimal.ZERO;
            }
            extra = extra.add(childExtra);
        }

        return extra;
    }

    // ─── 세금/봉사료 계산 공통 유틸 (Dayuse/숙박 공용) ───

    /**
     * 공급가에 Property 봉사료/세금을 적용한 결과 반환
     * 숙박(일별 계산)과 Dayuse(고정가 계산) 모두에서 사용 가능
     */
    public TaxCalculationResult calculateTaxAndServiceCharge(BigDecimal supplyPrice, Property property) {
        // 봉사료 계산: FIXED(정액) vs PERCENTAGE(정률)
        BigDecimal serviceCharge;
        if ("FIXED".equals(property.getServiceChargeType())) {
            // 정액: 설정된 금액을 그대로 사용
            serviceCharge = nullToZero(property.getServiceChargeAmount());
        } else {
            // 정률(기본): 공급가 × 봉사료율 / 100
            serviceCharge = supplyPrice
                    .multiply(nullToZero(property.getServiceChargeRate()))
                    .divide(BigDecimal.valueOf(100),
                            getRoundingScale(property.getServiceChargeDecimalPlaces()),
                            parseRoundingMode(property.getServiceChargeRoundingMethod()));
        }

        BigDecimal tax = supplyPrice.add(serviceCharge)
                .multiply(nullToZero(property.getTaxRate()))
                .divide(BigDecimal.valueOf(100),
                        getRoundingScale(property.getTaxDecimalPlaces()),
                        parseRoundingMode(property.getTaxRoundingMethod()));

        BigDecimal total = supplyPrice.add(serviceCharge).add(tax);

        return new TaxCalculationResult(supplyPrice, serviceCharge, tax, total);
    }

    /**
     * 세금/봉사료 계산 결과
     */
    public record TaxCalculationResult(
            BigDecimal supplyPrice,
            BigDecimal serviceCharge,
            BigDecimal tax,
            BigDecimal total
    ) {}

    /**
     * 반올림 방법 변환 (3곳에서 중복 사용되던 로직을 통합)
     */
    public static RoundingMode parseRoundingMode(String method) {
        if (method == null) {
            return RoundingMode.HALF_UP;
        }
        return switch (method) {
            case "ROUND_UP" -> RoundingMode.UP;
            case "ROUND_DOWN" -> RoundingMode.DOWN;
            case "ROUND_FLOOR" -> RoundingMode.FLOOR;
            case "ROUND_CEILING" -> RoundingMode.CEILING;
            default -> RoundingMode.HALF_UP;
        };
    }

    // ─── private 헬퍼 ───

    private int getRoundingScale(Integer decimalPlaces) {
        return decimalPlaces != null ? decimalPlaces : 0;
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
