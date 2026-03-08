package com.hola.rate.entity;

import com.hola.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 프로모션 코드 엔티티
 */
@Entity
@Table(name = "rt_promotion_code")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionCode extends BaseEntity {

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "rate_code_id", nullable = false)
    private Long rateCodeId;

    @Column(name = "promotion_code", nullable = false, length = 50)
    private String promotionCode;

    @Column(name = "promotion_start_date", nullable = false)
    private LocalDate promotionStartDate;

    @Column(name = "promotion_end_date", nullable = false)
    private LocalDate promotionEndDate;

    @Column(name = "description_ko", length = 500)
    private String descriptionKo;

    @Column(name = "description_en", length = 500)
    private String descriptionEn;

    @Column(name = "promotion_type", nullable = false, length = 20)
    private String promotionType;

    @Column(name = "down_up_sign", length = 1)
    private String downUpSign;

    @Column(name = "down_up_value", precision = 15, scale = 2)
    private BigDecimal downUpValue;

    @Column(name = "down_up_unit", length = 10)
    private String downUpUnit;

    @Builder.Default
    @Column(name = "rounding_decimal_point")
    private Integer roundingDecimalPoint = 0;

    @Builder.Default
    @Column(name = "rounding_digits")
    private Integer roundingDigits = 0;

    @Column(name = "rounding_method", length = 20)
    private String roundingMethod;

    /**
     * 프로모션 코드 정보 수정
     */
    public void update(Long rateCodeId, LocalDate promotionStartDate, LocalDate promotionEndDate,
                       String descriptionKo, String descriptionEn, String promotionType,
                       String downUpSign, BigDecimal downUpValue, String downUpUnit,
                       Integer roundingDecimalPoint, Integer roundingDigits, String roundingMethod) {
        this.rateCodeId = rateCodeId;
        this.promotionStartDate = promotionStartDate;
        this.promotionEndDate = promotionEndDate;
        this.descriptionKo = descriptionKo;
        this.descriptionEn = descriptionEn;
        this.promotionType = promotionType;
        this.downUpSign = downUpSign;
        this.downUpValue = downUpValue;
        this.downUpUnit = downUpUnit;
        this.roundingDecimalPoint = roundingDecimalPoint;
        this.roundingDigits = roundingDigits;
        this.roundingMethod = roundingMethod;
    }

}
