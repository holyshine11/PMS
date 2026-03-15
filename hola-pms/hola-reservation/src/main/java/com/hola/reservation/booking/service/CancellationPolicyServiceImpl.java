package com.hola.reservation.booking.service;

import com.hola.hotel.entity.CancellationFee;
import com.hola.hotel.repository.CancellationFeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

/**
 * 취소 수수료 계산 서비스 구현
 *
 * CancellationFee(htl_cancellation_fee) 정책 기반으로
 * 체크인까지 남은 일수에 따른 취소 수수료를 계산한다.
 *
 * 매칭 알고리즘:
 * 1. remainingDays = checkInDate - today
 * 2. DATE 기준 정책 조회 후 daysBefore ASC 정렬
 * 3. remainingDays <= daysBefore 조건에 매칭되는 첫 정책 적용
 * 4. 매칭 없으면 수수료 0 (무료 취소)
 * 5. isNoShow=true 시 NOSHOW 정책 우선 적용, 없으면 DATE 기준 폴백
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CancellationPolicyServiceImpl implements CancellationPolicyService {

    private final CancellationFeeRepository cancellationFeeRepository;

    @Override
    public CancelFeeResult calculateCancelFee(Long propertyId, LocalDate checkInDate, BigDecimal firstNightSupplyPrice) {
        return calculateCancelFee(propertyId, checkInDate, firstNightSupplyPrice, false);
    }

    @Override
    public CancelFeeResult calculateCancelFee(Long propertyId, LocalDate checkInDate,
                                                BigDecimal firstNightSupplyPrice, boolean isNoShow) {
        List<CancellationFee> allPolicies = cancellationFeeRepository
                .findAllByPropertyIdOrderBySortOrder(propertyId);

        // 노쇼인 경우 NOSHOW 정책 우선 적용
        if (isNoShow) {
            List<CancellationFee> noShowPolicies = allPolicies.stream()
                    .filter(p -> "NOSHOW".equals(p.getCheckinBasis()))
                    .toList();

            if (!noShowPolicies.isEmpty()) {
                CancellationFee matched = noShowPolicies.get(0);
                return calculateFeeFromPolicy(matched, firstNightSupplyPrice, propertyId, "노쇼");
            }
            // NOSHOW 정책 미설정 시 DATE 기준으로 폴백
            log.info("NOSHOW 정책 미설정: propertyId={}, DATE 기준으로 폴백", propertyId);
        }

        // DATE 기준 정책 매칭
        long remainingDays = ChronoUnit.DAYS.between(LocalDate.now(), checkInDate);
        if (remainingDays < 0) {
            remainingDays = 0;
        }

        List<CancellationFee> datePolicies = allPolicies.stream()
                .filter(p -> "DATE".equals(p.getCheckinBasis()))
                .sorted(Comparator.comparingInt(p -> p.getDaysBefore() != null ? p.getDaysBefore() : 0))
                .toList();

        if (datePolicies.isEmpty()) {
            log.info("취소 정책 미설정: propertyId={}, 무료 취소 처리", propertyId);
            return new CancelFeeResult(BigDecimal.ZERO, BigDecimal.ZERO, "취소 정책 미설정 - 무료 취소");
        }

        // 남은 일수 이하인 가장 가까운 정책 매칭 (daysBefore ASC → 첫 매칭)
        CancellationFee matched = null;
        for (CancellationFee policy : datePolicies) {
            int daysBefore = policy.getDaysBefore() != null ? policy.getDaysBefore() : 0;
            if (remainingDays <= daysBefore) {
                matched = policy;
                break;
            }
        }

        // 매칭 정책 없으면 무료 취소
        if (matched == null) {
            String desc = String.format("체크인 %d일 전 - 무료 취소 구간", remainingDays);
            log.info("취소 수수료 계산: propertyId={}, remainingDays={}, 무료 취소", propertyId, remainingDays);
            return new CancelFeeResult(BigDecimal.ZERO, BigDecimal.ZERO, desc);
        }

        String context = String.format("체크인 %d일 전", matched.getDaysBefore() != null ? matched.getDaysBefore() : 0);
        return calculateFeeFromPolicy(matched, firstNightSupplyPrice, propertyId, context);
    }

    /**
     * 매칭된 정책으로 수수료 금액 계산
     */
    private CancelFeeResult calculateFeeFromPolicy(CancellationFee matched, BigDecimal firstNightSupplyPrice,
                                                     Long propertyId, String contextLabel) {
        BigDecimal feeAmount;
        BigDecimal feePercent;
        String feeType = matched.getFeeType();
        BigDecimal policyFeeAmount = matched.getFeeAmount();

        if ("PERCENTAGE".equals(feeType)) {
            feePercent = policyFeeAmount;
            BigDecimal baseAmount = firstNightSupplyPrice != null ? firstNightSupplyPrice : BigDecimal.ZERO;
            feeAmount = baseAmount.multiply(policyFeeAmount).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        } else if ("FIXED_KRW".equals(feeType)) {
            feeAmount = policyFeeAmount;
            if (firstNightSupplyPrice != null && firstNightSupplyPrice.compareTo(BigDecimal.ZERO) > 0) {
                feePercent = feeAmount.multiply(BigDecimal.valueOf(100))
                        .divide(firstNightSupplyPrice, 0, RoundingMode.HALF_UP);
            } else {
                feePercent = BigDecimal.ZERO;
            }
        } else {
            // FIXED_USD 등 미지원 → 무료 처리
            feeAmount = BigDecimal.ZERO;
            feePercent = BigDecimal.ZERO;
        }

        String desc = String.format("%s - %s %s%%",
                contextLabel, feeType, policyFeeAmount.stripTrailingZeros().toPlainString());

        log.info("취소 수수료 계산: propertyId={}, context={}, feeType={}, feeAmount={}",
                propertyId, contextLabel, feeType, feeAmount);

        return new CancelFeeResult(feeAmount, feePercent, desc);
    }
}
