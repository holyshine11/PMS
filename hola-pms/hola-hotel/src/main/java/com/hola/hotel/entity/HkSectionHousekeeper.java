package com.hola.hotel.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 구역 기본 담당자 매핑 엔티티
 */
@Entity
@Table(name = "hk_section_housekeeper",
       uniqueConstraints = @UniqueConstraint(columnNames = {"section_id", "housekeeper_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class HkSectionHousekeeper {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "section_id", insertable = false, updatable = false)
    private Long sectionId;

    @Column(name = "housekeeper_id", nullable = false)
    private Long housekeeperId;

    @Column(name = "is_primary")
    @Builder.Default
    private Boolean isPrimary = true;
}
