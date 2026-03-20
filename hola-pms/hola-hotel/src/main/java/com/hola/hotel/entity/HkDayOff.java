package com.hola.hotel.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 하우스키핑 휴무일 엔티티
 */
@Entity
@Table(name = "hk_day_off",
       uniqueConstraints = @UniqueConstraint(columnNames = {"property_id", "housekeeper_id", "day_off_date"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class HkDayOff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "housekeeper_id", nullable = false)
    private Long housekeeperId;

    @Column(name = "day_off_date", nullable = false)
    private LocalDate dayOffDate;

    /** REGULAR(정기), REQUESTED(요청), APPROVED(관리자 직접) */
    @Column(name = "day_off_type", nullable = false, length = 20)
    @Builder.Default
    private String dayOffType = "REQUESTED";

    /** PENDING(대기), APPROVED(승인), REJECTED(거절) */
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "note", length = 200)
    private String note;

    @Column(name = "approved_by", length = 50)
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "created_by", length = 50)
    private String createdBy;

    /** 승인 처리 */
    public void approve(String approvedBy) {
        this.status = "APPROVED";
        this.approvedBy = approvedBy;
        this.approvedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /** 거절 처리 */
    public void reject(String approvedBy) {
        this.status = "REJECTED";
        this.approvedBy = approvedBy;
        this.approvedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
