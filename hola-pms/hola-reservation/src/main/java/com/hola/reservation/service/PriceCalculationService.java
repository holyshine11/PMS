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
 *    f. 세금 = 소계 × Property.taxRate
 *    g. 봉사료 = 소계 × Property.serviceChargeRate
 *    h. 합계 = 소계 + 세금 + 봉사료
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

        // 세금 계산 (프로퍼티 세율 기반)
        BigDecimal taxRate = property.getTaxRate() != null ? property.getTaxRate() : BigDecimal.ZERO;
        BigDecimal tax = supplyPrice.multiply(taxRate)
                .divide(BigDecimal.valueOf(100), getRoundingScale(property.getTaxDecimalPlaces()),
                        getRoundingMode(property.getTaxRoundingMethod()));

        // 봉사료 계산
        BigDecimal serviceChargeRate = property.getServiceChargeRate() != null ?
                property.getServiceChargeRate() : BigDecimal.ZERO;
        BigDecimal serviceCharge = supplyPrice.multiply(serviceChargeRate)
                .divide(BigDecimal.valueOf(100), getRoundingScale(property.getServiceChargeDecimalPlaces()),
                        getRoundingMode(property.getServiceChargeRoundingMethod()));

        BigDecimal total = supplyPrice.add(tax).add(serviceCharge);

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
     */
    private RatePricing findPricingForDate(List<RatePricing> pricingList, LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        for (RatePricing pricing : pricingList) {
            // 기간 매칭
            if (date.isBefore(pricing.getStartDate()) || date.isAfter(pricing.getEndDate())) {
                continue;
            }
            // 요일 매칭
            boolean matches = switch (dayOfWeek) {
                case MONDAY -> pricing.getDayMon();
                case TUESDAY -> pricing.getDayTue();
                case WEDNESDAY -> pricing.getDayWed();
                case THURSDAY -> pricing.getDayThu();
                case FRIDAY -> pricing.getDayFri();
                case SATURDAY -> pricing.getDaySat();
                case SUNDAY -> pricing.getDaySun();
            };
            if (matches) {
                return pricing;
            }
        }
        return null;
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
                    .orElse(BigDecimal.ZERO);
            extra = extra.add(adultExtra);
        }

        // 아동 추가
        for (int seq = 1; seq <= children; seq++) {
            final int targetSeq = seq;
            BigDecimal childExtra = persons.stream()
                    .filter(p -> "CHILD".equals(p.getPersonType()) && p.getPersonSeq() == targetSeq)
                    .map(RatePricingPerson::getSupplyPrice)
                    .findFirst()
                    .orElse(BigDecimal.ZERO);
            extra = extra.add(childExtra);
        }

        return extra;
    }

    /**
     * 반올림 자릿수
     */
    private int getRoundingScale(Integer decimalPlaces) {
        return decimalPlaces != null ? decimalPlaces : 0;
    }

    /**
     * 반올림 방법 변환
     */
    private RoundingMode getRoundingMode(String method) {
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
}
