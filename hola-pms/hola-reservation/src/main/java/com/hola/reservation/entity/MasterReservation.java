package com.hola.reservation.entity;

import com.hola.common.entity.BaseEntity;
import com.hola.hotel.entity.Property;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 마스터 예약 엔티티
 */
@Entity
@Table(name = "rsv_master_reservation")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MasterReservation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(name = "master_reservation_no", nullable = false, unique = true, length = 20)
    private String masterReservationNo;

    @Column(name = "confirmation_no", nullable = false, unique = true, length = 10)
    private String confirmationNo;

    @Column(name = "reservation_status", nullable = false, length = 20)
    @Builder.Default
    private String reservationStatus = "RESERVED";

    @Column(name = "master_check_in", nullable = false)
    private LocalDate masterCheckIn;

    @Column(name = "master_check_out", nullable = false)
    private LocalDate masterCheckOut;

    @Column(name = "reservation_date", nullable = false)
    @Builder.Default
    private LocalDateTime reservationDate = LocalDateTime.now();

    // 게스트 정보
    @Column(name = "guest_name_ko", length = 100)
    private String guestNameKo;

    @Column(name = "guest_first_name_en", length = 100)
    private String guestFirstNameEn;

    @Column(name = "guest_middle_name_en", length = 100)
    private String guestMiddleNameEn;

    @Column(name = "guest_last_name_en", length = 100)
    private String guestLastNameEn;

    @Column(name = "phone_country_code", length = 10)
    private String phoneCountryCode;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "email", length = 200)
    private String email;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "gender", length = 10)
    private String gender;

    @Column(name = "nationality", length = 10)
    private String nationality;

    // 다른 모듈 FK (Long 타입, @Column만 사용)
    @Column(name = "rate_code_id")
    private Long rateCodeId;

    @Column(name = "market_code_id")
    private Long marketCodeId;

    @Column(name = "reservation_channel_id")
    private Long reservationChannelId;

    // 프로모션/OTA 정보
    @Column(name = "promotion_type", length = 20)
    private String promotionType;

    @Column(name = "promotion_code", length = 50)
    private String promotionCode;

    @Column(name = "ota_reservation_no", length = 50)
    private String otaReservationNo;

    @Column(name = "is_ota_managed", nullable = false)
    @Builder.Default
    private Boolean isOtaManaged = false;

    @Column(name = "customer_request", columnDefinition = "TEXT")
    private String customerRequest;

    // 서브 예약 목록 (ID 순 정렬 보장)
    @OneToMany(mappedBy = "masterReservation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    @Builder.Default
    private List<SubReservation> subReservations = new ArrayList<>();

    /**
     * 게스트 정보 + 기본 정보 업데이트
     */
    public void update(LocalDate masterCheckIn, LocalDate masterCheckOut,
                       String guestNameKo, String guestFirstNameEn, String guestMiddleNameEn, String guestLastNameEn,
                       String phoneCountryCode, String phoneNumber, String email,
                       LocalDate birthDate, String gender, String nationality,
                       Long rateCodeId, Long marketCodeId, Long reservationChannelId,
                       String promotionType, String promotionCode,
                       String otaReservationNo, Boolean isOtaManaged,
                       String customerRequest) {
        this.masterCheckIn = masterCheckIn;
        this.masterCheckOut = masterCheckOut;
        this.guestNameKo = guestNameKo;
        this.guestFirstNameEn = guestFirstNameEn;
        this.guestMiddleNameEn = guestMiddleNameEn;
        this.guestLastNameEn = guestLastNameEn;
        this.phoneCountryCode = phoneCountryCode;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.birthDate = birthDate;
        this.gender = gender;
        this.nationality = nationality;
        this.rateCodeId = rateCodeId;
        this.marketCodeId = marketCodeId;
        this.reservationChannelId = reservationChannelId;
        this.promotionType = promotionType;
        this.promotionCode = promotionCode;
        this.otaReservationNo = otaReservationNo;
        this.isOtaManaged = isOtaManaged != null ? isOtaManaged : false;
        this.customerRequest = customerRequest;
    }

    /**
     * 예약 상태 변경
     */
    public void updateStatus(String newStatus) {
        this.reservationStatus = newStatus;
    }
}
