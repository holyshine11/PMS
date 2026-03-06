package com.hola.room.entity;

import com.hola.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

/**
 * 객실 클래스(그룹) 엔티티
 */
@Entity
@Table(name = "rm_room_class",
        uniqueConstraints = @UniqueConstraint(columnNames = {"property_id", "room_class_code"}))
@SQLRestriction("deleted_at IS NULL")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomClass extends BaseEntity {

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "room_class_code", nullable = false, length = 50)
    private String roomClassCode;

    @Column(name = "room_class_name", nullable = false, length = 200)
    private String roomClassName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * 객실 클래스 정보 수정
     */
    public void update(String roomClassName, String description) {
        this.roomClassName = roomClassName;
        this.description = description;
    }
}
