package com.hola.hotel.entity;

import com.hola.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

/**
 * 프로퍼티/객실 이미지 엔티티
 */
@Entity
@Table(name = "htl_property_image")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PropertyImage extends BaseEntity {

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    /** PROPERTY, ROOM_TYPE, FACILITY, EXTERIOR */
    @Column(name = "image_type", nullable = false, length = 20)
    private String imageType;

    /** 객실타입 ID 등 (image_type에 따라) */
    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "image_path", nullable = false, length = 500)
    private String imagePath;

    @Column(name = "image_name", length = 200)
    private String imageName;

    @Column(name = "alt_text", length = 300)
    private String altText;
}
