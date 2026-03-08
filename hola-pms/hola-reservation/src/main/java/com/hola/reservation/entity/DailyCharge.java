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
 * 일별 요금 엔티티 (경량 - BaseEntity 미상속)
 */
@Entity
@Table(name = "rsv_daily_charge", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"sub_reservation_id", "charge_date"})
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DailyCharge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_reservation_id", nullable = false)
    private SubReservation subReservation;

    @Column(name = "charge_date", nullable = false)
    private LocalDate chargeDate;

    @Column(name = "supply_price", precision = 15, scale = 2)
    private BigDecimal supplyPrice;

    @Column(name = "tax", precision = 15, scale = 2)
    private BigDecimal tax;

    @Column(name = "service_charge", precision = 15, scale = 2)
    private BigDecimal serviceCharge;

    @Column(name = "total", precision = 15, scale = 2)
    private BigDecimal total;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
