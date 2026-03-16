package com.hola.reservation.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 예약 서비스 항목 엔티티 (경량 - BaseEntity 미상속)
 */
@Entity
@Table(name = "rsv_reservation_service")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ReservationServiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_reservation_id", nullable = false)
    private SubReservation subReservation;

    @Column(name = "service_type", nullable = false, length = 20)
    private String serviceType;

    @Column(name = "service_option_id")
    private Long serviceOptionId;

    @Column(name = "service_date")
    private LocalDate serviceDate;

    @Column(name = "quantity", nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    @Column(name = "unit_price", precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "tax", precision = 15, scale = 2)
    private BigDecimal tax;

    @Column(name = "total_price", precision = 15, scale = 2)
    private BigDecimal totalPrice;

    // Phase 5: 트랜잭션 코드 연결 + 포스팅 상태
    @Column(name = "transaction_code_id")
    private Long transactionCodeId;

    @Column(name = "posting_status", length = 10)
    @Builder.Default
    private String postingStatus = "POSTED";    // POSTED / PENDING / VOIDED

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
