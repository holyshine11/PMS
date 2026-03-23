package com.hola.rate.entity;

import com.hola.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

/**
 * Dayuse 시간별 요금 엔티티
 */
@Entity
@Table(name = "rt_day_use_rate")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DayUseRate extends BaseEntity {

    @Column(name = "rate_code_id", nullable = false)
    private Long rateCodeId;

    @Column(name = "duration_hours", nullable = false)
    private Integer durationHours;

    @Column(name = "supply_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal supplyPrice;

    @Column(name = "description", length = 200)
    private String description;

    public void update(Integer durationHours, BigDecimal supplyPrice, String description) {
        this.durationHours = durationHours;
        this.supplyPrice = supplyPrice;
        this.description = description;
    }
}
