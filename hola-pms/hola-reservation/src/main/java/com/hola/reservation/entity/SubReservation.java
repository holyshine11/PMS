package com.hola.reservation.entity;

import com.hola.common.entity.BaseEntity;
import com.hola.common.enums.StayType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import com.hola.reservation.vo.DayUseTimeSlot;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 서브 예약 엔티티 (객실별 예약 단위)
 */
@Entity
@Table(name = "rsv_sub_reservation")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SubReservation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_reservation_id", nullable = false)
    private MasterReservation masterReservation;

    @Column(name = "sub_reservation_no", nullable = false, unique = true, length = 25)
    private String subReservationNo;

    @Version
    @Column(name = "version")
    @Builder.Default
    private Long version = 0L;

    @Column(name = "room_reservation_status", nullable = false, length = 20)
    @Builder.Default
    private String roomReservationStatus = "RESERVED";

    // 다른 모듈 FK (@Column만)
    @Column(name = "room_type_id")
    private Long roomTypeId;

    @Column(name = "floor_id")
    private Long floorId;

    @Column(name = "room_number_id")
    private Long roomNumberId;

    @Column(name = "adults", nullable = false)
    @Builder.Default
    private Integer adults = 1;

    @Column(name = "children", nullable = false)
    @Builder.Default
    private Integer children = 0;

    @Column(name = "check_in", nullable = false)
    private LocalDate checkIn;

    @Column(name = "check_out", nullable = false)
    private LocalDate checkOut;

    // 숙박유형: OVERNIGHT(숙박), DAY_USE(데이유즈)
    @Enumerated(EnumType.STRING)
    @Column(name = "stay_type", nullable = false, length = 20)
    @Builder.Default
    private StayType stayType = StayType.OVERNIGHT;

    // Dayuse 이용 시간 범위
    @Column(name = "day_use_start_time")
    private LocalTime dayUseStartTime;

    @Column(name = "day_use_end_time")
    private LocalTime dayUseEndTime;

    // 예상 도착/출발 시간
    @Column(name = "eta")
    private LocalTime eta;

    @Column(name = "etd")
    private LocalTime etd;

    @Column(name = "early_check_in", nullable = false)
    @Builder.Default
    private Boolean earlyCheckIn = false;

    @Column(name = "late_check_out", nullable = false)
    @Builder.Default
    private Boolean lateCheckOut = false;

    // 실제 체크인/체크아웃 시각
    @Column(name = "actual_check_in_time")
    private LocalDateTime actualCheckInTime;

    @Column(name = "actual_check_out_time")
    private LocalDateTime actualCheckOutTime;

    // 얼리 체크인 / 레이트 체크아웃 요금
    @Column(name = "early_check_in_fee", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal earlyCheckInFee = BigDecimal.ZERO;

    @Column(name = "late_check_out_fee", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal lateCheckOutFee = BigDecimal.ZERO;

    // 동반 투숙객
    @OneToMany(mappedBy = "subReservation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ReservationGuest> guests = new ArrayList<>();

    // 일별 요금
    @OneToMany(mappedBy = "subReservation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DailyCharge> dailyCharges = new ArrayList<>();

    // 서비스 항목
    @OneToMany(mappedBy = "subReservation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ReservationServiceItem> services = new ArrayList<>();

    /**
     * 서브 예약 정보 수정
     */
    public void update(Long roomTypeId, Long floorId, Long roomNumberId,
                       Integer adults, Integer children,
                       LocalDate checkIn, LocalDate checkOut,
                       Boolean earlyCheckIn, Boolean lateCheckOut) {
        this.roomTypeId = roomTypeId;
        this.floorId = floorId;
        this.roomNumberId = roomNumberId;
        this.adults = adults;
        this.children = children;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.earlyCheckIn = earlyCheckIn;
        this.lateCheckOut = lateCheckOut;
    }

    /**
     * Dayuse 전용 필드 갱신 (stayType/시간)
     */
    public void updateDayUseInfo(StayType stayType, java.time.LocalTime dayUseStartTime, java.time.LocalTime dayUseEndTime) {
        this.stayType = stayType;
        this.dayUseStartTime = dayUseStartTime;
        this.dayUseEndTime = dayUseEndTime;
    }

    /**
     * 객실타입 변경 (업그레이드용) - 기존 배정 초기화
     */
    public void changeRoomType(Long newRoomTypeId) {
        this.roomTypeId = newRoomTypeId;
        this.roomNumberId = null;
        this.floorId = null;
    }

    /**
     * 객실 예약 상태 변경
     */
    public void updateStatus(String newStatus) {
        this.roomReservationStatus = newStatus;
    }

    /**
     * 실제 체크인 시각 및 얼리 체크인 요금 기록
     */
    public void recordCheckIn(LocalDateTime checkInTime, BigDecimal earlyFee) {
        this.actualCheckInTime = checkInTime;
        if (earlyFee != null && earlyFee.compareTo(BigDecimal.ZERO) > 0) {
            this.earlyCheckIn = true;
            this.earlyCheckInFee = earlyFee;
        }
    }

    /**
     * Dayuse 여부 판단 헬퍼
     */
    public boolean isDayUse() {
        return this.stayType != null && this.stayType.isDayUse();
    }

    /**
     * Dayuse 시간 슬롯 VO 반환 (숙박이면 null)
     */
    public DayUseTimeSlot getDayUseTimeSlot() {
        return DayUseTimeSlot.ofNullable(this.dayUseStartTime, this.dayUseEndTime);
    }

    /**
     * 실제 체크아웃 시각 및 레이트 체크아웃 요금 기록
     */
    public void recordCheckOut(LocalDateTime checkOutTime, BigDecimal lateFee) {
        this.actualCheckOutTime = checkOutTime;
        if (lateFee != null && lateFee.compareTo(BigDecimal.ZERO) > 0) {
            this.lateCheckOut = true;
            this.lateCheckOutFee = lateFee;
        }
    }

    /**
     * 얼리 체크인 요금 등록 (시간대 선택 시 즉시 확정)
     */
    public void registerEarlyCheckInFee(BigDecimal fee) {
        this.earlyCheckIn = true;
        this.earlyCheckInFee = fee;
    }

    /**
     * 얼리 체크인 요금 해제
     */
    public void clearEarlyCheckInFee() {
        this.earlyCheckIn = false;
        this.earlyCheckInFee = BigDecimal.ZERO;
    }

    /**
     * 레이트 체크아웃 요금 등록 (시간대 선택 시 즉시 확정)
     */
    public void registerLateCheckOutFee(BigDecimal fee) {
        this.lateCheckOut = true;
        this.lateCheckOutFee = fee;
    }

    /**
     * 레이트 체크아웃 요금 해제
     */
    public void clearLateCheckOutFee() {
        this.lateCheckOut = false;
        this.lateCheckOutFee = BigDecimal.ZERO;
    }
}
