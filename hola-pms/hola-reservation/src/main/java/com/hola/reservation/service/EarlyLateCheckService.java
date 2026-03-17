package com.hola.reservation.service;

import com.hola.hotel.entity.EarlyLateFeePolicy;
import com.hola.hotel.entity.Property;
import com.hola.hotel.repository.EarlyLateFeePolicyRepository;
import com.hola.reservation.entity.DailyCharge;
import com.hola.reservation.entity.SubReservation;
import com.hola.reservation.repository.DailyChargeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

/**
 * 얼리 체크인 / 레이트 체크아웃 요금 계산 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EarlyLateCheckService {

    private final EarlyLateFeePolicyRepository policyRepository;
    private final DailyChargeRepository dailyChargeRepository;

    /**
     * 얼리 체크인 요금 계산
     * @param sub 서브예약
     * @param actualCheckInTime 실제 체크인 시각
     * @return 얼리 체크인 요금 (해당 없으면 ZERO)
     */
    public BigDecimal calculateEarlyCheckInFee(SubReservation sub, LocalDateTime actualCheckInTime) {
        Property property = sub.getMasterReservation().getProperty();
        // 체크인 시간 미설정 시 요금 없음
        if (property.getCheckInTime() == null || property.getCheckInTime().isBlank()) {
            return BigDecimal.ZERO;
        }
        LocalTime standardCheckIn = LocalTime.parse(property.getCheckInTime());
        LocalTime actualTime = actualCheckInTime.toLocalTime();

        // 표준 체크인 시간 이후이면 얼리 아님
        if (!actualTime.isBefore(standardCheckIn)) {
            return BigDecimal.ZERO;
        }

        // 해당 프로퍼티의 얼리 체크인 정책 조회
        List<EarlyLateFeePolicy> policies = policyRepository
                .findAllByPropertyIdAndPolicyTypeOrderBySortOrder(property.getId(), "EARLY_CHECKIN");

        if (policies.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // 시간대에 해당하는 정책 찾기
        EarlyLateFeePolicy matchedPolicy = findMatchingPolicy(policies, actualTime);
        if (matchedPolicy == null) {
            return BigDecimal.ZERO;
        }

        // 체크인 당일 DailyCharge 기준 요금 계산
        BigDecimal baseAmount = getBaseRoomRate(sub);
        return calculateFee(matchedPolicy, baseAmount);
    }

    /**
     * 레이트 체크아웃 요금 계산
     * @param sub 서브예약
     * @param actualCheckOutTime 실제 체크아웃 시각
     * @return 레이트 체크아웃 요금 (해당 없으면 ZERO)
     */
    public BigDecimal calculateLateCheckOutFee(SubReservation sub, LocalDateTime actualCheckOutTime) {
        Property property = sub.getMasterReservation().getProperty();
        // 체크아웃 시간 미설정 시 요금 없음
        if (property.getCheckOutTime() == null || property.getCheckOutTime().isBlank()) {
            return BigDecimal.ZERO;
        }
        LocalTime standardCheckOut = LocalTime.parse(property.getCheckOutTime());
        LocalTime actualTime = actualCheckOutTime.toLocalTime();

        // 표준 체크아웃 시간 이전이면 레이트 아님
        if (!actualTime.isAfter(standardCheckOut)) {
            return BigDecimal.ZERO;
        }

        // 해당 프로퍼티의 레이트 체크아웃 정책 조회
        List<EarlyLateFeePolicy> policies = policyRepository
                .findAllByPropertyIdAndPolicyTypeOrderBySortOrder(property.getId(), "LATE_CHECKOUT");

        if (policies.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // 시간대에 해당하는 정책 찾기
        EarlyLateFeePolicy matchedPolicy = findMatchingPolicy(policies, actualTime);
        if (matchedPolicy == null) {
            return BigDecimal.ZERO;
        }

        // 마지막 투숙일 DailyCharge 기준 요금 계산
        BigDecimal baseAmount = getLastNightRoomRate(sub);
        return calculateFee(matchedPolicy, baseAmount);
    }

    /**
     * 시간대에 맞는 정책 찾기
     */
    private EarlyLateFeePolicy findMatchingPolicy(List<EarlyLateFeePolicy> policies, LocalTime actualTime) {
        for (EarlyLateFeePolicy policy : policies) {
            LocalTime from = LocalTime.parse(policy.getTimeFrom());
            LocalTime to = LocalTime.parse(policy.getTimeTo());
            // from <= actualTime < to
            if (!actualTime.isBefore(from) && actualTime.isBefore(to)) {
                return policy;
            }
        }
        return null;
    }

    /**
     * 정책 기반 요금 계산
     */
    private BigDecimal calculateFee(EarlyLateFeePolicy policy, BigDecimal baseAmount) {
        if ("PERCENT".equals(policy.getFeeType())) {
            // 비율 계산: baseAmount * feeValue / 100
            return baseAmount.multiply(policy.getFeeValue())
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        } else {
            // 고정 금액
            return policy.getFeeValue();
        }
    }

    /**
     * 서브 예약의 기본 객실료 - 얼리 체크인용 (첫째 날 공급가 기준)
     */
    private BigDecimal getBaseRoomRate(SubReservation sub) {
        List<DailyCharge> charges = dailyChargeRepository.findBySubReservationId(sub.getId());
        if (charges.isEmpty()) {
            return BigDecimal.ZERO;
        }
        // chargeDate 기준 정렬하여 첫째 날 요금 반환
        charges.sort(Comparator.comparing(DailyCharge::getChargeDate));
        return charges.get(0).getSupplyPrice();
    }

    /**
     * 서브 예약의 기본 객실료 - 레이트 체크아웃용 (마지막 투숙일 공급가 기준)
     */
    private BigDecimal getLastNightRoomRate(SubReservation sub) {
        List<DailyCharge> charges = dailyChargeRepository.findBySubReservationId(sub.getId());
        if (charges.isEmpty()) {
            return BigDecimal.ZERO;
        }
        // chargeDate 기준 정렬하여 마지막 날 요금 반환
        charges.sort(Comparator.comparing(DailyCharge::getChargeDate));
        return charges.get(charges.size() - 1).getSupplyPrice();
    }
}
