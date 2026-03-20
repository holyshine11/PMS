package com.hola.hotel.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 하우스키핑 일일 출근부 엔티티
 */
@Entity
@Table(name = "hk_daily_attendance",
       uniqueConstraints = @UniqueConstraint(columnNames = {"property_id", "attendance_date", "housekeeper_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class HkDailyAttendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version")
    private Long version; // 낙관적 잠금: 동시 출퇴근 Race Condition 방지

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "housekeeper_id", nullable = false)
    private Long housekeeperId;

    @Column(name = "is_available")
    @Builder.Default
    private Boolean isAvailable = true;

    @Column(name = "shift_type", length = 10)
    @Builder.Default
    private String shiftType = "DAY";

    @Column(name = "note", length = 200)
    private String note;

    @Column(name = "clock_in_at")
    private LocalDateTime clockInAt;

    @Column(name = "clock_out_at")
    private LocalDateTime clockOutAt;

    @Column(name = "attendance_status", length = 20)
    @Builder.Default
    private String attendanceStatus = "BEFORE_WORK";

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    public void update(Boolean isAvailable, String shiftType, String note) {
        this.isAvailable = isAvailable;
        this.shiftType = shiftType;
        this.note = note;
        // isAvailable 변경 시 attendanceStatus 동기화 (이미 출근/퇴근한 경우는 유지)
        if (!Boolean.TRUE.equals(isAvailable) && "BEFORE_WORK".equals(this.attendanceStatus)) {
            this.attendanceStatus = "DAY_OFF";
        } else if (Boolean.TRUE.equals(isAvailable) && "DAY_OFF".equals(this.attendanceStatus)) {
            this.attendanceStatus = "BEFORE_WORK";
        }
        this.updatedAt = LocalDateTime.now();
    }

    /** 출근 처리 */
    public void clockIn() {
        this.clockInAt = LocalDateTime.now();
        this.clockOutAt = null; // 이전 퇴근 시각 초기화 (PMS 리셋 후 재출근 시 잔존 방지)
        this.attendanceStatus = "WORKING";
        this.isAvailable = true;
        this.updatedAt = LocalDateTime.now();
    }

    /** 퇴근 처리 */
    public void clockOut() {
        this.clockOutAt = LocalDateTime.now();
        this.attendanceStatus = "LEFT";
        this.updatedAt = LocalDateTime.now();
    }

    /** 휴무 처리 */
    public void markDayOff() {
        this.attendanceStatus = "DAY_OFF";
        this.isAvailable = false;
        this.updatedAt = LocalDateTime.now();
    }

    /** 관리자 편집: 상태 + 시간 직접 설정 */
    public void updateAttendance(String attendanceStatus, LocalDateTime clockInAt, LocalDateTime clockOutAt) {
        this.attendanceStatus = attendanceStatus;
        this.clockInAt = clockInAt;
        this.clockOutAt = clockOutAt;
        this.isAvailable = !"DAY_OFF".equals(attendanceStatus);
        this.updatedAt = LocalDateTime.now();
    }
}
