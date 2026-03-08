package com.hola.rate.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 레이트 코드 - 유료 서비스 옵션 매핑 엔티티
 */
@Entity
@Table(name = "rt_rate_code_paid_service",
        uniqueConstraints = @UniqueConstraint(columnNames = {"rate_code_id", "paid_service_option_id"}))
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateCodePaidService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rate_code_id", nullable = false)
    private Long rateCodeId;

    @Column(name = "paid_service_option_id", nullable = false)
    private Long paidServiceOptionId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
