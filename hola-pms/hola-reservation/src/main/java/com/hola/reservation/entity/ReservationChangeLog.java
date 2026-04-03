package com.hola.reservation.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 예약 변경 이력 (append-only 경량 엔티티)
 */
@Entity
@Table(name = "rsv_reservation_change_log")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ReservationChangeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "master_reservation_id", nullable = false)
    private Long masterReservationId;

    @Column(name = "sub_reservation_id")
    private Long subReservationId;

    @Column(name = "change_category", nullable = false, length = 30)
    private String changeCategory;

    @Column(name = "change_type", nullable = false, length = 50)
    private String changeType;

    @Column(name = "field_name", length = 50)
    private String fieldName;

    @Column(name = "old_value", length = 500)
    private String oldValue;

    @Column(name = "new_value", length = 500)
    private String newValue;

    @Column(name = "description", length = 500)
    private String description;

    @CreatedBy
    @Column(name = "created_by", length = 50)
    private String createdBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
