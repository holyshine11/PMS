package com.hola.hotel.entity;

import com.hola.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

/**
 * 호수 엔티티
 */
@Entity
@Table(name = "htl_room_number", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"property_id", "room_number"})
})
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RoomNumber extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(name = "room_number", nullable = false, length = 20)
    private String roomNumber;

    @Column(name = "description_ko", columnDefinition = "TEXT")
    private String descriptionKo;

    @Column(name = "description_en", columnDefinition = "TEXT")
    private String descriptionEn;

    public void update(String roomNumber, String descriptionKo, String descriptionEn) {
        this.roomNumber = roomNumber;
        this.descriptionKo = descriptionKo;
        this.descriptionEn = descriptionEn;
    }
}
