package com.hola.hotel.entity;

import com.hola.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

/**
 * 취소 수수료 정책 엔티티
 */
@Entity
@Table(name = "htl_cancellation_fee")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CancellationFee extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(name = "checkin_basis", nullable = false, length = 10)
    private String checkinBasis;

    @Column(name = "days_before")
    private Integer daysBefore;

    @Column(name = "fee_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal feeAmount;

    @Column(name = "fee_type", nullable = false, length = 20)
    private String feeType;
}
