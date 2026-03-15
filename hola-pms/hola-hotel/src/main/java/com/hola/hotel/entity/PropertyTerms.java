package com.hola.hotel.entity;

import com.hola.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

/**
 * 프로퍼티 이용약관 엔티티
 */
@Entity
@Table(name = "htl_property_terms")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PropertyTerms extends BaseEntity {

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    /** BOOKING, PRIVACY, CANCELLATION, HOUSE_RULES */
    @Column(name = "terms_type", nullable = false, length = 30)
    private String termsType;

    @Column(name = "title_ko", nullable = false, length = 200)
    private String titleKo;

    @Column(name = "title_en", length = 200)
    private String titleEn;

    @Column(name = "content_ko", nullable = false, columnDefinition = "TEXT")
    private String contentKo;

    @Column(name = "content_en", columnDefinition = "TEXT")
    private String contentEn;

    @Column(name = "version", length = 20)
    @Builder.Default
    private String version = "1.0";

    /** 필수 동의 여부 */
    @Column(name = "required")
    @Builder.Default
    private Boolean required = true;
}
