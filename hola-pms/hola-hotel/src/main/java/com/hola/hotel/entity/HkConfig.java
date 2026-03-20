package com.hola.hotel.entity;

import com.hola.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

/**
 * 하우스키핑 프로퍼티별 설정 엔티티
 */
@Entity
@Table(name = "hk_config")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class HkConfig extends BaseEntity {

    @Column(name = "property_id", nullable = false, unique = true)
    private Long propertyId;

    // 프로세스 설정
    @Column(name = "inspection_required")
    @Builder.Default
    private Boolean inspectionRequired = false;

    @Column(name = "auto_create_checkout")
    @Builder.Default
    private Boolean autoCreateCheckout = true;

    @Column(name = "auto_create_stayover")
    @Builder.Default
    private Boolean autoCreateStayover = false;

    // 크레딧 기본값
    @Column(name = "default_checkout_credit", precision = 3, scale = 1)
    @Builder.Default
    private BigDecimal defaultCheckoutCredit = new BigDecimal("1.0");

    @Column(name = "default_stayover_credit", precision = 3, scale = 1)
    @Builder.Default
    private BigDecimal defaultStayoverCredit = new BigDecimal("0.5");

    @Column(name = "default_turndown_credit", precision = 3, scale = 1)
    @Builder.Default
    private BigDecimal defaultTurndownCredit = new BigDecimal("0.3");

    @Column(name = "default_deep_clean_credit", precision = 3, scale = 1)
    @Builder.Default
    private BigDecimal defaultDeepCleanCredit = new BigDecimal("2.0");

    @Column(name = "default_touch_up_credit", precision = 3, scale = 1)
    @Builder.Default
    private BigDecimal defaultTouchUpCredit = new BigDecimal("0.3");

    // Rush 판정 기준
    @Column(name = "rush_threshold_minutes")
    @Builder.Default
    private Integer rushThresholdMinutes = 120;

    /**
     * 설정 수정
     */
    public void update(Boolean inspectionRequired, Boolean autoCreateCheckout, Boolean autoCreateStayover,
                       BigDecimal defaultCheckoutCredit, BigDecimal defaultStayoverCredit,
                       BigDecimal defaultTurndownCredit, BigDecimal defaultDeepCleanCredit,
                       BigDecimal defaultTouchUpCredit, Integer rushThresholdMinutes) {
        this.inspectionRequired = inspectionRequired;
        this.autoCreateCheckout = autoCreateCheckout;
        this.autoCreateStayover = autoCreateStayover;
        this.defaultCheckoutCredit = defaultCheckoutCredit;
        this.defaultStayoverCredit = defaultStayoverCredit;
        this.defaultTurndownCredit = defaultTurndownCredit;
        this.defaultDeepCleanCredit = defaultDeepCleanCredit;
        this.defaultTouchUpCredit = defaultTouchUpCredit;
        this.rushThresholdMinutes = rushThresholdMinutes;
    }
}
