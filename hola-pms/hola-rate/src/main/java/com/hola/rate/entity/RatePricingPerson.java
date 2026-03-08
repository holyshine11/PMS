package com.hola.rate.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 인원별 추가 요금 엔티티
 */
@Entity
@Table(name = "rt_rate_pricing_person",
        uniqueConstraints = @UniqueConstraint(columnNames = {"rate_pricing_id", "person_type", "person_seq"}))
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatePricingPerson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rate_pricing_id", nullable = false)
    private Long ratePricingId;

    @Column(name = "person_type", nullable = false, length = 10)
    private String personType;

    @Column(name = "person_seq", nullable = false)
    private Integer personSeq;

    @Column(name = "supply_price", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal supplyPrice = BigDecimal.ZERO;

    @Column(name = "tax", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal tax = BigDecimal.ZERO;

    @Column(name = "total_price", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalPrice = BigDecimal.ZERO;
}
