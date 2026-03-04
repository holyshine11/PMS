package com.hola.hotel.entity;

import com.hola.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

/**
 * 층 엔티티
 */
@Entity
@Table(name = "htl_floor", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"property_id", "floor_number"})
})
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Floor extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(name = "floor_number", nullable = false, length = 20)
    private String floorNumber;

    @Column(name = "floor_name", length = 50)
    private String floorName;

    @Column(name = "description_ko", columnDefinition = "TEXT")
    private String descriptionKo;

    @Column(name = "description_en", columnDefinition = "TEXT")
    private String descriptionEn;

    public void update(String floorNumber, String floorName, String descriptionKo, String descriptionEn) {
        this.floorNumber = floorNumber;
        this.floorName = floorName;
        this.descriptionKo = descriptionKo;
        this.descriptionEn = descriptionEn;
    }
}
