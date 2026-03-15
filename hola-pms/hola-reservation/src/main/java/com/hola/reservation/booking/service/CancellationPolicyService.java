package com.hola.reservation.booking.service;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 취소 수수료 계산 서비스
 */
public interface CancellationPolicyService {

    /**
     * 취소 수수료 계산
     * @param propertyId 프로퍼티 ID
     * @param checkInDate 체크인 날짜
     * @param firstNightSupplyPrice 1박 공급가 (PERCENTAGE 계산 기준)
     * @return 계산 결과
     */
    CancelFeeResult calculateCancelFee(Long propertyId, LocalDate checkInDate, BigDecimal firstNightSupplyPrice);

    /**
     * 취소 수수료 계산 (노쇼 지원)
     * @param isNoShow true이면 NOSHOW 정책 우선 적용
     */
    CancelFeeResult calculateCancelFee(Long propertyId, LocalDate checkInDate, BigDecimal firstNightSupplyPrice, boolean isNoShow);

    /**
     * 취소 수수료 계산 결과
     */
    record CancelFeeResult(
            BigDecimal feeAmount,
            BigDecimal feePercent,
            String policyDescription
    ) {}
}
