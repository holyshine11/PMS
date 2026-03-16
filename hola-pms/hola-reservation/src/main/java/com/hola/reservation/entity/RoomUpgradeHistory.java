package com.hola.reservation.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 객실 업그레이드 이력 (경량 엔티티)
 */
@Entity
@Table(name = "rsv_room_upgrade_history")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RoomUpgradeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sub_reservation_id", nullable = false)
    private Long subReservationId;

    @Column(name = "from_room_type_id", nullable = false)
    private Long fromRoomTypeId;

    @Column(name = "to_room_type_id", nullable = false)
    private Long toRoomTypeId;

    @Column(name = "upgraded_at", nullable = false)
    private LocalDateTime upgradedAt;

    // COMPLIMENTARY / PAID / UPSELL
    @Column(name = "upgrade_type", nullable = false, length = 20)
    private String upgradeType;

    // 잔여 숙박일 기준 총 차액
    @Column(name = "price_difference", precision = 15, scale = 2)
    private BigDecimal priceDifference;

    @Column(name = "reason", length = 500)
    private String reason;

    @CreatedBy
    @Column(name = "created_by", length = 50)
    private String createdBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
