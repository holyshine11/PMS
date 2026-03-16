package com.hola.room.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 객실 타입 - 유료 서비스 옵션 매핑 엔티티
 * Phase 2: 가격 오버라이드, 최대 수량, 가용 여부 추가
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

    // === Phase 2 확장 필드 ===

    /** 객실타입별 가격 오버라이드 (null이면 기본가 사용) */
    @Column(name = "override_price", precision = 15, scale = 2)
    private BigDecimal overridePrice;

    /** 객실타입별 최대 수량 (null이면 무제한) */
    @Column(name = "max_quantity")
    private Integer maxQuantity;

    /** 해당 객실타입 가용 여부 */
    @Column(name = "available", nullable = false)
    @Builder.Default
    private Boolean available = true;
}
