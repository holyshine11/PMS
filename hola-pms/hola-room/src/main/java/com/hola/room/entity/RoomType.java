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
 * 객실 타입 엔티티
 */
@Entity
@Table(name = "rm_room_type",
        uniqueConstraints = @UniqueConstraint(columnNames = {"property_id", "room_type_code"}))
@SQLRestriction("deleted_at IS NULL")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomType extends BaseEntity {

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "room_class_id", nullable = false)
    private Long roomClassId;

    @Column(name = "room_type_code", nullable = false, length = 50)
    private String roomTypeCode;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "room_size", precision = 10, scale = 2)
    private BigDecimal roomSize;

    @Column(name = "features", columnDefinition = "TEXT")
    private String features;

    @Column(name = "max_adults", nullable = false)
    private Integer maxAdults;

    @Column(name = "max_children", nullable = false)
    private Integer maxChildren;

    @Column(name = "extra_bed_yn")
    private Boolean extraBedYn;

    /**
     * 객실 타입 정보 수정
     */
    public void update(String description, BigDecimal roomSize,
                       String features, Integer maxAdults, Integer maxChildren, Boolean extraBedYn) {
        this.description = description;
        this.roomSize = roomSize;
        this.features = features;
        this.maxAdults = maxAdults;
        this.maxChildren = maxChildren;
        this.extraBedYn = extraBedYn;
    }
}
