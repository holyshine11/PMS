package com.hola.rate.entity;

import com.hola.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;

/**
 * 레이트 코드 엔티티
 */
@Entity
@Table(name = "rt_rate_code")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateCode extends BaseEntity {

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "rate_code", nullable = false, length = 50)
    private String rateCode;

    @Column(name = "rate_name_ko", nullable = false, length = 200)
    private String rateNameKo;

    @Column(name = "rate_name_en", length = 200)
    private String rateNameEn;

    @Column(name = "rate_category", nullable = false, length = 20)
    private String rateCategory;

    @Column(name = "market_code_id")
    private Long marketCodeId;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Column(name = "sale_start_date", nullable = false)
    private LocalDate saleStartDate;

    @Column(name = "sale_end_date", nullable = false)
    private LocalDate saleEndDate;

    @Column(name = "min_stay_days", nullable = false)
    private Integer minStayDays;

    @Column(name = "max_stay_days", nullable = false)
    private Integer maxStayDays;

    // 숙박유형: OVERNIGHT(숙박), DAY_USE(데이유즈)
    @Column(name = "stay_type", nullable = false, length = 20)
    @Builder.Default
    private String stayType = "OVERNIGHT";

    /**
     * Dayuse 여부 판단 헬퍼
     */
    public boolean isDayUse() {
        return "DAY_USE".equals(this.stayType);
    }

    /**
     * 레이트 코드 정보 수정
     */
    public void update(String rateNameKo, String rateNameEn, String rateCategory,
                       Long marketCodeId, String currency,
                       LocalDate saleStartDate, LocalDate saleEndDate,
                       Integer minStayDays, Integer maxStayDays,
                       String stayType) {
        this.rateNameKo = rateNameKo;
        this.rateNameEn = rateNameEn;
        this.rateCategory = rateCategory;
        this.marketCodeId = marketCodeId;
        this.currency = currency;
        this.saleStartDate = saleStartDate;
        this.saleEndDate = saleEndDate;
        this.minStayDays = minStayDays;
        this.maxStayDays = maxStayDays;
        this.stayType = stayType != null ? stayType : "OVERNIGHT";
    }

}
