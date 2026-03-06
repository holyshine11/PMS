package com.hola.room.entity;

import com.hola.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

/**
 * 무료 서비스 옵션 엔티티
 */
@Entity
@Table(name = "rm_free_service_option",
        uniqueConstraints = @UniqueConstraint(columnNames = {"property_id", "service_option_code"}))
@SQLRestriction("deleted_at IS NULL")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FreeServiceOption extends BaseEntity {

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "service_option_code", nullable = false, length = 50)
    private String serviceOptionCode;

    @Column(name = "service_name_ko", nullable = false, length = 200)
    private String serviceNameKo;

    @Column(name = "service_name_en", length = 200)
    private String serviceNameEn;

    /** 서비스 옵션 유형: BED, VIEW, ROOM_AMENITY, BREAKFAST */
    @Column(name = "service_type", nullable = false, length = 20)
    private String serviceType;

    /** 적용 박수: FIRST_NIGHT_ONLY, ALL_NIGHTS, NOT_APPLICABLE */
    @Column(name = "applicable_nights", nullable = false, length = 20)
    private String applicableNights;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /** 수량 단위: EA, SET, TIME, SERVICE */
    @Column(name = "quantity_unit", nullable = false, length = 10)
    private String quantityUnit;

    /**
     * 무료 서비스 옵션 정보 수정
     */
    public void update(String serviceNameKo, String serviceNameEn, String serviceType,
                       String applicableNights, Integer quantity, String quantityUnit) {
        this.serviceNameKo = serviceNameKo;
        this.serviceNameEn = serviceNameEn;
        this.serviceType = serviceType;
        this.applicableNights = applicableNights;
        this.quantity = quantity;
        this.quantityUnit = quantityUnit;
    }
}
