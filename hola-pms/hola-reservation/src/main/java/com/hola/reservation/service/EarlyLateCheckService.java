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
import java.util.ArrayList;
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

        boolean isActuallyEarly = actualTime.isBefore(standardCheckIn);
        boolean toggledOn = Boolean.TRUE.equals(sub.getEarlyCheckIn());

        // 토글 OFF이고 실제 얼리도 아니면 요금 없음
        if (!isActuallyEarly && !toggledOn) {
            return BigDecimal.ZERO;
        }

        // 해당 프로퍼티의 얼리 체크인 정책 조회
        List<EarlyLateFeePolicy> policies = policyRepository
                .findAllByPropertyIdAndPolicyTypeOrderBySortOrder(property.getId(), "EARLY_CHECKIN");

        if (policies.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // 시간대에 해당하는 정책 찾기 (실제 얼리면 시간 매칭, 토글만 ON이면 첫 정책)
        EarlyLateFeePolicy matchedPolicy = isActuallyEarly
                ? findMatchingPolicy(policies, actualTime)
                : policies.get(0);
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

        boolean isActuallyLate = actualTime.isAfter(standardCheckOut);
        boolean toggledOn = Boolean.TRUE.equals(sub.getLateCheckOut());

        // 토글 OFF이고 실제 레이트도 아니면 요금 없음
        if (!isActuallyLate && !toggledOn) {
            return BigDecimal.ZERO;
        }

        // 해당 프로퍼티의 레이트 체크아웃 정책 조회
        List<EarlyLateFeePolicy> policies = policyRepository
                .findAllByPropertyIdAndPolicyTypeOrderBySortOrder(property.getId(), "LATE_CHECKOUT");

        if (policies.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // 시간대에 해당하는 정책 찾기 (실제 레이트면 시간 매칭, 토글만 ON이면 첫 정책)
        EarlyLateFeePolicy matchedPolicy = isActuallyLate
                ? findMatchingPolicy(policies, actualTime)
                : policies.get(0);
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

    /**
     * 얼리 체크인 / 레이트 체크아웃 시간대별 예상 요금 조회
     * @param sub 서브예약 (DailyCharge 기반 기준가 산출)
     * @param policyType "EARLY_CHECKIN" 또는 "LATE_CHECKOUT"
     * @return 시간대별 예상 요금 리스트
     */
    public List<FeeEstimate> estimateFees(SubReservation sub, String policyType) {
        return estimateFees(sub, policyType, sub.getMasterReservation().getProperty());
    }

    /**
     * 얼리 체크인 / 레이트 체크아웃 시간대별 예상 요금 조회 (Property 직접 전달)
     */
    public List<FeeEstimate> estimateFees(SubReservation sub, String policyType, Property property) {
        List<EarlyLateFeePolicy> policies = policyRepository
                .findAllByPropertyIdAndPolicyTypeOrderBySortOrder(property.getId(), policyType);

        if (policies.isEmpty()) {
            return List.of();
        }

        // 기준가: 얼리=첫째 날, 레이트=마지막 날
        BigDecimal baseAmount = "EARLY_CHECKIN".equals(policyType)
                ? getBaseRoomRate(sub) : getLastNightRoomRate(sub);

        List<FeeEstimate> estimates = new ArrayList<>();
        for (EarlyLateFeePolicy policy : policies) {
            BigDecimal fee = calculateFee(policy, baseAmount);
            estimates.add(new FeeEstimate(
                    policy.getTimeFrom(), policy.getTimeTo(),
                    policy.getFeeType(), policy.getFeeValue(), fee,
                    policy.getDescription()));
        }
        return estimates;
    }

    /**
     * 사용자 선택 정책 인덱스 기반 요금 계산 (등록 시점 즉시 확정용)
     * @param sub 서브예약
     * @param policyType "EARLY_CHECKIN" 또는 "LATE_CHECKOUT"
     * @param policyIndex 정책 리스트 내 인덱스 (sortOrder 순)
     * @return 확정 요금
     */
    public BigDecimal calculateFeeByPolicyIndex(SubReservation sub, String policyType, int policyIndex) {
        Long propertyId = sub.getMasterReservation().getProperty().getId();
        List<EarlyLateFeePolicy> policies = policyRepository
                .findAllByPropertyIdAndPolicyTypeOrderBySortOrder(propertyId, policyType);

        if (policies.isEmpty() || policyIndex < 0 || policyIndex >= policies.size()) {
            return BigDecimal.ZERO;
        }

        EarlyLateFeePolicy policy = policies.get(policyIndex);
        BigDecimal baseAmount = "EARLY_CHECKIN".equals(policyType)
                ? getBaseRoomRate(sub) : getLastNightRoomRate(sub);
        return calculateFee(policy, baseAmount);
    }

    /**
     * 시간대별 예상 요금 DTO
     */
    public record FeeEstimate(
            String timeFrom,
            String timeTo,
            String feeType,
            BigDecimal feeValue,
            BigDecimal estimatedFee,
            String description
    ) {}
}
