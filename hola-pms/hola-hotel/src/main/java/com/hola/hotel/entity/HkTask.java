package com.hola.hotel.entity;

import com.hola.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 하우스키핑 작업 엔티티
 */
@Entity
@Table(name = "hk_task")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class HkTask extends BaseEntity {

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "room_number_id", nullable = false)
    private Long roomNumberId;

    @Column(name = "task_sheet_id")
    private Long taskSheetId;

    // 작업 정보
    @Column(name = "task_type", nullable = false, length = 20)
    private String taskType;

    @Column(name = "task_date", nullable = false)
    private LocalDate taskDate;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "priority", length = 10)
    @Builder.Default
    private String priority = "NORMAL";

    @Column(name = "credit", precision = 3, scale = 1)
    private BigDecimal credit;

    // 배정
    @Column(name = "assigned_to")
    private Long assignedTo;

    @Column(name = "assigned_by")
    private Long assignedBy;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    // 시간 추적
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "paused_at")
    private LocalDateTime pausedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "inspected_at")
    private LocalDateTime inspectedAt;

    @Column(name = "inspected_by")
    private Long inspectedBy;

    @Column(name = "estimated_end")
    private LocalDateTime estimatedEnd;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    // 예약 연계
    @Column(name = "reservation_id")
    private Long reservationId;

    @Column(name = "next_checkin_at")
    private LocalDateTime nextCheckinAt;

    // 메모
    @Column(name = "note", length = 500)
    private String note;

    // DND 추적
    @Column(name = "dnd_skipped")
    @Builder.Default
    private Boolean dndSkipped = false;

    @Column(name = "dnd_skip_count")
    @Builder.Default
    private Integer dndSkipCount = 0;

    @Column(name = "scheduled_time", length = 5)
    private String scheduledTime;

    // === 상태 전이 메서드 ===

    /**
     * 작업 배정
     */
    public void assign(Long assignedTo, Long assignedBy) {
        this.assignedTo = assignedTo;
        this.assignedBy = assignedBy;
        this.assignedAt = LocalDateTime.now();
    }

    /**
     * 배정 해제
     */
    public void unassign() {
        this.assignedTo = null;
        this.assignedBy = null;
        this.assignedAt = null;
    }

    /**
     * 작업 시작
     */
    public void start() {
        this.status = "IN_PROGRESS";
        this.startedAt = LocalDateTime.now();
    }

    /**
     * 일시 중단
     */
    public void pause() {
        this.status = "PAUSED";
        this.pausedAt = LocalDateTime.now();
    }

    /**
     * 작업 완료
     */
    public void complete() {
        this.status = "COMPLETED";
        this.completedAt = LocalDateTime.now();
        if (this.startedAt != null) {
            this.durationMinutes = (int) ChronoUnit.MINUTES.between(this.startedAt, this.completedAt);
        }
    }

    /**
     * 검수 완료
     */
    public void inspect(Long inspectedBy) {
        this.status = "INSPECTED";
        this.inspectedAt = LocalDateTime.now();
        this.inspectedBy = inspectedBy;
    }

    /**
     * 취소
     */
    public void cancel() {
        this.status = "CANCELLED";
    }

    /**
     * 작업 시트 배정
     */
    public void assignToSheet(Long taskSheetId) {
        this.taskSheetId = taskSheetId;
    }

    /**
     * 작업 정보 수정
     */
    public void update(String taskType, String priority, BigDecimal credit, String note) {
        this.taskType = taskType;
        this.priority = priority;
        this.credit = credit;
        this.note = note;
    }

    /**
     * 우선순위 변경
     */
    public void changePriority(String priority) {
        this.priority = priority;
    }

    /**
     * 완료 예상 시간 입력
     */
    public void updateEstimatedEnd(LocalDateTime estimatedEnd) {
        this.estimatedEnd = estimatedEnd;
    }
}
