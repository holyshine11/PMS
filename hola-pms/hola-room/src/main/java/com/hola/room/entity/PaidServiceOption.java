package com.hola.room.entity;

import com.hola.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

/**
 * 유료 서비스 옵션 엔티티
 */
@Entity
@Table(name = "rm_paid_service_option",
        uniqueConstraints = @UniqueConstraint(columnNames = {"property_id", "service_option_code"}))
@SQLRestriction("deleted_at IS NULL")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaidServiceOption extends BaseEntity {

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "service_option_code", nullable = false, length = 50)
    private String serviceOptionCode;

    @Column(name = "service_name_ko", nullable = false, length = 200)
    private String serviceNameKo;

    @Column(name = "service_name_en", length = 200)
    private String serviceNameEn;

    /** 서비스 옵션 유형: ROOM_AMENITY, BREAKFAST, ROOM_SERVICE */
    @Column(name = "service_type", nullable = false, length = 20)
    private String serviceType;

    /** 적용 박수: FIRST_NIGHT_ONLY, ALL_NIGHTS, NOT_APPLICABLE */
    @Column(name = "applicable_nights", nullable = false, length = 20)
    private String applicableNights;

    /** 통화: KRW, USD */
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    /** 부가세 포함여부 */
    @Column(name = "vat_included", nullable = false)
    @Builder.Default
    private Boolean vatIncluded = true;

    /** 세율 (%) */
    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal taxRate = BigDecimal.ZERO;

    /** 공급가 */
    @Column(name = "supply_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal supplyPrice;

    /** TAX 금액 (자동 계산) */
    @Column(name = "tax_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal taxAmount;

    /** VAT 포함 가격 (공급가 + TAX) */
    @Column(name = "vat_included_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal vatIncludedPrice;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /** 수량 단위: EA, SET, TIME, SERVICE */
    @Column(name = "quantity_unit", nullable = false, length = 10)
    private String quantityUnit;

    /** 관리자 메모 */
    @Column(name = "admin_memo", columnDefinition = "TEXT")
    private String adminMemo;

    /**
     * 유료 서비스 옵션 정보 수정
     */
    public void update(String serviceNameKo, String serviceNameEn, String serviceType,
                       String applicableNights, String currencyCode,
                       Boolean vatIncluded, BigDecimal taxRate,
                       BigDecimal supplyPrice, BigDecimal taxAmount, BigDecimal vatIncludedPrice,
                       Integer quantity, String quantityUnit, String adminMemo) {
        this.serviceNameKo = serviceNameKo;
        this.serviceNameEn = serviceNameEn;
        this.serviceType = serviceType;
        this.applicableNights = applicableNights;
        this.currencyCode = currencyCode;
        this.vatIncluded = vatIncluded;
        this.taxRate = taxRate;
        this.supplyPrice = supplyPrice;
        this.taxAmount = taxAmount;
        this.vatIncludedPrice = vatIncludedPrice;
        this.quantity = quantity;
        this.quantityUnit = quantityUnit;
        this.adminMemo = adminMemo;
    }
}
