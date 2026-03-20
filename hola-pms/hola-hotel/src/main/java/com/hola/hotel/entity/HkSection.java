package com.hola.hotel.entity;

import com.hola.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 하우스키핑 구역 엔티티
 */
@Entity
@Table(name = "hk_section")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class HkSection extends BaseEntity {

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "section_name", nullable = false, length = 50)
    private String sectionName;

    @Column(name = "section_code", length = 20)
    private String sectionCode;

    @Column(name = "max_credits", precision = 5, scale = 1)
    private BigDecimal maxCredits;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "section_id")
    @Builder.Default
    private List<HkSectionFloor> floors = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "section_id")
    @Builder.Default
    private List<HkSectionHousekeeper> housekeepers = new ArrayList<>();

    public void update(String sectionName, String sectionCode, BigDecimal maxCredits) {
        this.sectionName = sectionName;
        this.sectionCode = sectionCode;
        this.maxCredits = maxCredits;
    }

    /**
     * 층 매핑 교체
     */
    public void replaceFloors(List<HkSectionFloor> newFloors) {
        this.floors.clear();
        if (newFloors != null) {
            this.floors.addAll(newFloors);
        }
    }

    /**
     * 담당자 매핑 교체
     */
    public void replaceHousekeepers(List<HkSectionHousekeeper> newHousekeepers) {
        this.housekeepers.clear();
        if (newHousekeepers != null) {
            this.housekeepers.addAll(newHousekeepers);
        }
    }
}
