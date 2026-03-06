package com.hola.room.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 객실 타입 - 유료 서비스 옵션 매핑 엔티티
 */
@Entity
@Table(name = "rm_room_type_paid_service",
        uniqueConstraints = @UniqueConstraint(columnNames = {"room_type_id", "paid_service_option_id"}))
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomTypePaidService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_type_id", nullable = false)
    private Long roomTypeId;

    @Column(name = "paid_service_option_id", nullable = false)
    private Long paidServiceOptionId;

    @Column(name = "quantity", nullable = false)
    @Builder.Default
    private Integer quantity = 1;
}
