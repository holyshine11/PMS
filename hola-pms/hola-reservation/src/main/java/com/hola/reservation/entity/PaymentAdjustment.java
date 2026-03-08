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
 * 결제 조정 엔티티 (경량 - BaseEntity 미상속)
 */
@Entity
@Table(name = "rsv_payment_adjustment")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PaymentAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "master_reservation_id", nullable = false)
    private Long masterReservationId;

    @Column(name = "adjustment_seq", nullable = false)
    private Integer adjustmentSeq;

    @Column(name = "currency", nullable = false, length = 10)
    @Builder.Default
    private String currency = "KRW";

    @Column(name = "adjustment_sign", nullable = false, length = 1)
    @Builder.Default
    private String adjustmentSign = "+";

    @Column(name = "supply_price", precision = 15, scale = 2)
    private BigDecimal supplyPrice;

    @Column(name = "tax", precision = 15, scale = 2)
    private BigDecimal tax;

    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

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
