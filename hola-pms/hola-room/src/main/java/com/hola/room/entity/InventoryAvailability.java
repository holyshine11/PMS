package com.hola.room.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 일자별 재고 가용성 (경량 엔티티)
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "rm_inventory_availability",
       uniqueConstraints = @UniqueConstraint(columnNames = {"inventory_item_id", "availability_date"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class InventoryAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inventory_item_id", nullable = false)
    private Long inventoryItemId;

    @Column(name = "availability_date", nullable = false)
    private LocalDate availabilityDate;

    @Column(name = "available_count", nullable = false)
    private Integer availableCount;

    @Column(name = "reserved_count", nullable = false)
    @Builder.Default
    private Integer reservedCount = 0;

    /** 재고 차감 (예약 시) */
    public boolean reserve(int qty) {
        if (getRemainingCount() < qty) return false;
        this.reservedCount += qty;
        return true;
    }

    /** 재고 복원 (취소 시) */
    public void release(int qty) {
        this.reservedCount = Math.max(0, this.reservedCount - qty);
    }

    /** 실제 가용 수량 */
    public int getRemainingCount() {
        return availableCount - reservedCount;
    }

    /** 가용 수량 벌크 설정 */
    public void setAvailableCount(int count) {
        this.availableCount = count;
    }

    // 감사 필드
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;
}
