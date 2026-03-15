package com.hola.reservation.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 거래 이력 엔티티 (경량 - BaseEntity 미상속)
 */
@Entity
@Table(name = "rsv_payment_transaction")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "master_reservation_id", nullable = false)
    private Long masterReservationId;

    @Column(name = "transaction_seq", nullable = false)
    private Integer transactionSeq;

    /** 거래 유형: PAYMENT, REFUND, CANCEL_FEE */
    @Column(name = "transaction_type", nullable = false, length = 20)
    @Builder.Default
    private String transactionType = "PAYMENT";

    /** 결제 수단: CARD, CASH */
    @Column(name = "payment_method", nullable = false, length = 20)
    private String paymentMethod;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 10)
    @Builder.Default
    private String currency = "KRW";

    @Column(name = "transaction_status", nullable = false, length = 20)
    @Builder.Default
    private String transactionStatus = "COMPLETED";

    /** VAN 승인번호 (추후 연동) */
    @Column(name = "approval_no", length = 50)
    private String approvalNo;

    @Column(name = "memo", columnDefinition = "TEXT")
    private String memo;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;
}
