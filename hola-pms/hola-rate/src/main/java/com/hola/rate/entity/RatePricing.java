package com.hola.rate.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 요금 정보 엔티티
 */
@Entity
@Table(name = "rt_rate_pricing")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatePricing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rate_code_id", nullable = false)
    private Long rateCodeId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "day_mon", nullable = false)
    @Builder.Default
    private Boolean dayMon = true;

    @Column(name = "day_tue", nullable = false)
    @Builder.Default
    private Boolean dayTue = true;

    @Column(name = "day_wed", nullable = false)
    @Builder.Default
    private Boolean dayWed = true;

    @Column(name = "day_thu", nullable = false)
    @Builder.Default
    private Boolean dayThu = true;

    @Column(name = "day_fri", nullable = false)
    @Builder.Default
    private Boolean dayFri = true;

    @Column(name = "day_sat", nullable = false)
    @Builder.Default
    private Boolean daySat = true;

    @Column(name = "day_sun", nullable = false)
    @Builder.Default
    private Boolean daySun = true;

    @Column(name = "currency", nullable = false, length = 10)
    @Builder.Default
    private String currency = "KRW";

    @Column(name = "base_supply_price", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal baseSupplyPrice = BigDecimal.ZERO;

    @Column(name = "base_tax", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal baseTax = BigDecimal.ZERO;

    @Column(name = "base_total", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal baseTotal = BigDecimal.ZERO;

    // Down/Up sale 설정 (요금 행별)
    @Column(name = "down_up_sign", length = 1)
    private String downUpSign;

    @Column(name = "down_up_value", precision = 15, scale = 2)
    private BigDecimal downUpValue;

    @Column(name = "down_up_unit", length = 10)
    private String downUpUnit;

    @Column(name = "rounding_decimal_point")
    @Builder.Default
    private Integer roundingDecimalPoint = 0;

    @Column(name = "rounding_digits")
    @Builder.Default
    private Integer roundingDigits = 0;

    @Column(name = "rounding_method", length = 20)
    private String roundingMethod;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "rate_pricing_id", insertable = false, updatable = false)
    @Builder.Default
    private List<RatePricingPerson> persons = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 요금 정보 수정
     */
    public void update(LocalDate startDate, LocalDate endDate,
                       Boolean dayMon, Boolean dayTue, Boolean dayWed, Boolean dayThu,
                       Boolean dayFri, Boolean daySat, Boolean daySun,
                       String currency, BigDecimal baseSupplyPrice, BigDecimal baseTax, BigDecimal baseTotal) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.dayMon = dayMon;
        this.dayTue = dayTue;
        this.dayWed = dayWed;
        this.dayThu = dayThu;
        this.dayFri = dayFri;
        this.daySat = daySat;
        this.daySun = daySun;
        this.currency = currency;
        this.baseSupplyPrice = baseSupplyPrice;
        this.baseTax = baseTax;
        this.baseTotal = baseTotal;
    }
}
