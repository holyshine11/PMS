package com.hola.reservation.entity;

import com.hola.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 예약 결제 엔티티
 */
@Entity
@Table(name = "rsv_reservation_payment")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ReservationPayment extends BaseEntity {

    /** 낙관적 락 (동시 결제 방지) */
    @Version
    @Column(name = "version")
    private Long version;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_reservation_id", nullable = false, unique = true)
    private MasterReservation masterReservation;

    @Column(name = "payment_status", nullable = false, length = 20)
    @Builder.Default
    private String paymentStatus = "UNPAID";

    @Column(name = "total_room_amount", precision = 15, scale = 2)
    private BigDecimal totalRoomAmount;

    @Column(name = "total_service_amount", precision = 15, scale = 2)
    private BigDecimal totalServiceAmount;

    @Column(name = "total_service_charge_amount", precision = 15, scale = 2)
    private BigDecimal totalServiceChargeAmount;

    @Column(name = "total_adjustment_amount", precision = 15, scale = 2)
    private BigDecimal totalAdjustmentAmount;

    @Column(name = "total_early_late_fee", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalEarlyLateFee = BigDecimal.ZERO;

    @Column(name = "grand_total", precision = 15, scale = 2)
    private BigDecimal grandTotal;

    @Column(name = "total_paid_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalPaidAmount = BigDecimal.ZERO;

    /** 취소 수수료 금액 */
    @Column(name = "cancel_fee_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal cancelFeeAmount = BigDecimal.ZERO;

    /** 환불 금액 */
    @Column(name = "refund_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal refundAmount = BigDecimal.ZERO;

    /** 최초 예약 시점 1박 총액 (취소 수수료 기준 — 업그레이드 후에도 변경 안 됨) */
    @Column(name = "original_first_night_total", precision = 15, scale = 2)
    private BigDecimal originalFirstNightTotal;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @Column(name = "payment_method", length = 20)
    private String paymentMethod;

    /**
     * 금액 정보 업데이트
     */
    public void updateAmounts(BigDecimal totalRoomAmount, BigDecimal totalServiceAmount,
                              BigDecimal totalServiceChargeAmount, BigDecimal totalAdjustmentAmount,
                              BigDecimal grandTotal) {
        this.totalRoomAmount = totalRoomAmount;
        this.totalServiceAmount = totalServiceAmount;
        this.totalServiceChargeAmount = totalServiceChargeAmount;
        this.totalAdjustmentAmount = totalAdjustmentAmount;
        this.grandTotal = grandTotal;
    }

    /**
     * 얼리/레이트 요금 업데이트
     */
    public void updateEarlyLateFee(BigDecimal totalEarlyLateFee) {
        this.totalEarlyLateFee = totalEarlyLateFee;
    }

    /**
     * 결제 금액 누적 + 상태 자동 판단
     */
    public void addPaidAmount(BigDecimal amount) {
        this.totalPaidAmount = this.totalPaidAmount.add(amount);
        this.paymentDate = LocalDateTime.now();
        updatePaymentStatus();
    }

    /**
     * 취소 수수료 및 환불 금액 누적 (부분 Leg 취소 → 전체 취소 시 이전 값에 합산)
     */
    public void updateCancelRefund(BigDecimal cancelFeeAmount, BigDecimal refundAmount) {
        BigDecimal existingFee = this.cancelFeeAmount != null ? this.cancelFeeAmount : BigDecimal.ZERO;
        BigDecimal existingRefund = this.refundAmount != null ? this.refundAmount : BigDecimal.ZERO;
        this.cancelFeeAmount = existingFee.add(cancelFeeAmount);
        this.refundAmount = existingRefund.add(refundAmount);
    }

    /**
     * 취소/노쇼 환불 처리 완료 후 결제 상태를 REFUNDED로 직접 설정
     * grandTotal은 원본 유지 (감사 추적용), paymentStatus만 변경
     * (일반 updatePaymentStatus()는 cancel 흐름을 인식하지 못하므로 별도 메서드 사용)
     */
    public void setPaymentStatusRefunded() {
        this.paymentStatus = "REFUNDED";
    }

    /**
     * 최초 예약 시점 1박 총액 저장 (최초 1회만 — null일 때만 설정)
     */
    public void saveOriginalFirstNightTotal(BigDecimal firstNightTotal) {
        if (this.originalFirstNightTotal == null && firstNightTotal != null
                && firstNightTotal.compareTo(BigDecimal.ZERO) > 0) {
            this.originalFirstNightTotal = firstNightTotal;
        }
    }

    /**
     * 결제 상태 자동 판단: 순결제액(totalPaid - refund) vs grandTotal 비교
     * - REFUNDED 상태는 취소/노쇼 전용이므로 자동 판단에서 보호 (덮어쓰지 않음)
     * - 순결제액 = totalPaidAmount - refundAmount (환불 차감 후 실제 보유 금액)
     * - grandTotal <= 0: 결제 불필요 (조정으로 0 이하)
     * - 순결제액 > grandTotal: 초과결제 (환불 필요)
     * - 순결제액 == grandTotal: 결제완료
     * - 순결제액 > 0: 부분결제
     * - 그 외: 미결제
     */
    public void updatePaymentStatus() {
        // REFUNDED 상태는 취소/노쇼 흐름에서 명시적으로 설정된 것이므로 보호
        if ("REFUNDED".equals(this.paymentStatus)) {
            return;
        }
        BigDecimal gt = this.grandTotal != null ? this.grandTotal : BigDecimal.ZERO;
        BigDecimal paid = this.totalPaidAmount != null ? this.totalPaidAmount : BigDecimal.ZERO;
        BigDecimal refund = this.refundAmount != null ? this.refundAmount : BigDecimal.ZERO;
        BigDecimal cancelFee = this.cancelFeeAmount != null ? this.cancelFeeAmount : BigDecimal.ZERO;
        // 순결제액: 총 결제 - 환불 - 취소수수료 (수수료는 패널티 수익이므로 활성 예약 결제에 미포함)
        BigDecimal netPaid = paid.subtract(refund).subtract(cancelFee);

        // grandTotal이 0 이하인 경우
        if (gt.compareTo(BigDecimal.ZERO) <= 0) {
            if (netPaid.compareTo(BigDecimal.ZERO) > 0) {
                // 이미 결제한 금액이 있으면 환불 필요 (조정으로 grandTotal이 0 이하로 내려간 경우)
                this.paymentStatus = "OVERPAID";
            } else {
                // 결제한 금액이 없으면 결제 불필요
                this.paymentStatus = "PAID";
            }
            return;
        }

        int cmp = netPaid.compareTo(gt);
        if (cmp > 0) {
            // 초과결제 (조정으로 grandTotal 감소) → 환불 필요
            this.paymentStatus = "OVERPAID";
        } else if (cmp == 0) {
            this.paymentStatus = "PAID";
        } else if (netPaid.compareTo(BigDecimal.ZERO) > 0) {
            this.paymentStatus = "PARTIAL";
        } else {
            this.paymentStatus = "UNPAID";
        }
    }
}
