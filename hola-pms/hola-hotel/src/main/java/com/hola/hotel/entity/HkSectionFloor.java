package com.hola.hotel.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 구역-층 매핑 엔티티
 */
@Entity
@Table(name = "hk_section_floor",
       uniqueConstraints = @UniqueConstraint(columnNames = {"section_id", "floor_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class HkSectionFloor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "section_id", insertable = false, updatable = false)
    private Long sectionId;

    @Column(name = "floor_id", nullable = false)
    private Long floorId;
}
