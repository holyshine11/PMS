package com.hola.reservation.entity;

import com.hola.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
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

    @Column(name = "early_check_in", nullable = false)
    @Builder.Default
    private Boolean earlyCheckIn = false;

    @Column(name = "late_check_out", nullable = false)
    @Builder.Default
    private Boolean lateCheckOut = false;

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
     * 객실 예약 상태 변경
     */
    public void updateStatus(String newStatus) {
        this.roomReservationStatus = newStatus;
    }
}
