package com.hola.rate.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 레이트 코드 - 객실 타입 매핑 엔티티
 */
@Entity
@Table(name = "rt_rate_code_room_type",
        uniqueConstraints = @UniqueConstraint(columnNames = {"rate_code_id", "room_type_id"}))
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateCodeRoomType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rate_code_id", nullable = false)
    private Long rateCodeId;

    @Column(name = "room_type_id", nullable = false)
    private Long roomTypeId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
