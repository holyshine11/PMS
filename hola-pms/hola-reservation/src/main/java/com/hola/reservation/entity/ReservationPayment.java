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
     * 결제 상태 자동 판단: totalPaidAmount vs grandTotal 비교
     * - grandTotal <= 0: 결제 불필요 (조정으로 0 이하)
     * - totalPaid > grandTotal: 초과결제 (환불 필요)
     * - totalPaid == grandTotal: 결제완료
     * - totalPaid > 0: 부분결제
     * - 그 외: 미결제
     */
    public void updatePaymentStatus() {
        BigDecimal gt = this.grandTotal != null ? this.grandTotal : BigDecimal.ZERO;
        BigDecimal paid = this.totalPaidAmount != null ? this.totalPaidAmount : BigDecimal.ZERO;

        // 최종 합계가 0 이하 → 결제 불필요
        if (gt.compareTo(BigDecimal.ZERO) <= 0) {
            this.paymentStatus = "PAID";
            return;
        }

        int cmp = paid.compareTo(gt);
        if (cmp > 0) {
            // 초과결제 (조정으로 grandTotal 감소) → 환불 필요
            this.paymentStatus = "OVERPAID";
        } else if (cmp == 0) {
            this.paymentStatus = "PAID";
        } else if (paid.compareTo(BigDecimal.ZERO) > 0) {
            this.paymentStatus = "PARTIAL";
        } else {
            this.paymentStatus = "UNPAID";
        }
    }
}
