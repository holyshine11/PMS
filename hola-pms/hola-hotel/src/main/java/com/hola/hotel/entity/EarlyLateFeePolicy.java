package com.hola.hotel.entity;

import com.hola.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

/**
 * 얼리 체크인 / 레이트 체크아웃 요금 정책 엔티티
 */
@Entity
@Table(name = "htl_early_late_fee_policy")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EarlyLateFeePolicy extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    // 정책 유형: EARLY_CHECKIN, LATE_CHECKOUT
    @Column(name = "policy_type", nullable = false, length = 20)
    private String policyType;

    // 시작 시간
    @Column(name = "time_from", nullable = false, length = 10)
    private String timeFrom;

    // 종료 시간
    @Column(name = "time_to", nullable = false, length = 10)
    private String timeTo;

    // 요금 유형: PERCENT, FIXED
    @Column(name = "fee_type", nullable = false, length = 10)
    private String feeType;

    // 요금 값 (비율이면 %, 고정이면 원)
    @Column(name = "fee_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal feeValue;

    // 설명
    @Column(name = "description", length = 200)
    private String description;
}
