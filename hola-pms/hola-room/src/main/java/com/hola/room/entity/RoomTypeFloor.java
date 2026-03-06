package com.hola.room.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 객실 타입 - 층/호수 매핑 엔티티
 */
@Entity
@Table(name = "rm_room_type_floor",
        uniqueConstraints = @UniqueConstraint(columnNames = {"room_type_id", "floor_id", "room_number_id"}))
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomTypeFloor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_type_id", nullable = false)
    private Long roomTypeId;

    @Column(name = "floor_id", nullable = false)
    private Long floorId;

    @Column(name = "room_number_id", nullable = false)
    private Long roomNumberId;
}
