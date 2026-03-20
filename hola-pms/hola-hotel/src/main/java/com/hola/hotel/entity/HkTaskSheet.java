package com.hola.hotel.entity;

import com.hola.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 하우스키핑 작업 시트 엔티티
 */
@Entity
@Table(name = "hk_task_sheet")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class HkTaskSheet extends BaseEntity {

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "sheet_name", nullable = false, length = 100)
    private String sheetName;

    @Column(name = "sheet_date", nullable = false)
    private LocalDate sheetDate;

    @Column(name = "assigned_to")
    private Long assignedTo;

    @Column(name = "total_rooms")
    @Builder.Default
    private Integer totalRooms = 0;

    @Column(name = "total_credits", precision = 5, scale = 1)
    @Builder.Default
    private BigDecimal totalCredits = BigDecimal.ZERO;

    @Column(name = "completed_rooms")
    @Builder.Default
    private Integer completedRooms = 0;

    /**
     * 시트 정보 수정
     */
    public void update(String sheetName, Long assignedTo) {
        this.sheetName = sheetName;
        this.assignedTo = assignedTo;
    }

    /**
     * 집계 갱신
     */
    public void updateStats(int totalRooms, BigDecimal totalCredits, int completedRooms) {
        this.totalRooms = totalRooms;
        this.totalCredits = totalCredits;
        this.completedRooms = completedRooms;
    }
}
