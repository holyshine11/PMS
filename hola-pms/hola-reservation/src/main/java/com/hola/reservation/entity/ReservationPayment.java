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
    private String paymentStatus = "PENDING";

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
     * 결제 처리 (상태 → COMPLETED, 결제일시 기록)
     */
    public void processPayment(String method) {
        this.paymentStatus = "COMPLETED";
        this.paymentMethod = method;
        this.paymentDate = LocalDateTime.now();
    }
}
