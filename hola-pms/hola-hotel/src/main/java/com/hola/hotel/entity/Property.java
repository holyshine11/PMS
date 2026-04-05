package com.hola.hotel.entity;

import com.hola.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 프로퍼티 엔티티
 */
@Entity
@Table(name = "htl_property", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"hotel_id", "property_code"})
})
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Property extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @Column(name = "property_code", nullable = false, length = 20)
    private String propertyCode;

    @Column(name = "property_name", nullable = false, length = 200)
    private String propertyName;

    @Column(name = "property_type", length = 20)
    private String propertyType;

    @Column(name = "star_rating", length = 10)
    private String starRating;

    @Column(name = "check_in_time", length = 10)
    @Builder.Default
    private String checkInTime = "15:00";

    @Column(name = "check_out_time", length = 10)
    @Builder.Default
    private String checkOutTime = "11:00";

    @Column(name = "total_rooms")
    @Builder.Default
    private Integer totalRooms = 0;

    @Column(name = "timezone", length = 50)
    @Builder.Default
    private String timezone = "Asia/Seoul";

    @Column(name = "representative_name", length = 50)
    private String representativeName;

    @Column(name = "representative_name_en", length = 100)
    private String representativeNameEn;

    @Column(name = "country_code", length = 10)
    @Builder.Default
    private String countryCode = "+82";

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 200)
    private String email;

    @Column(name = "business_number", length = 20)
    private String businessNumber;

    @Column(name = "introduction", columnDefinition = "TEXT")
    private String introduction;

    @Column(name = "zip_code", length = 10)
    private String zipCode;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "address_detail", length = 500)
    private String addressDetail;

    @Column(name = "address_en", length = 500)
    private String addressEn;

    @Column(name = "address_detail_en", length = 500)
    private String addressDetailEn;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "biz_license_path", length = 500)
    private String bizLicensePath;

    @Column(name = "logo_path", length = 500)
    private String logoPath;

    // ─── TAX/봉사료 ────────────────────────────
    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal taxRate = BigDecimal.ZERO;

    @Column(name = "tax_decimal_places", nullable = false)
    @Builder.Default
    private Integer taxDecimalPlaces = 0;

    @Column(name = "tax_rounding_method", length = 20)
    private String taxRoundingMethod;

    @Column(name = "service_charge_rate", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal serviceChargeRate = BigDecimal.ZERO;

    @Column(name = "service_charge_decimal_places", nullable = false)
    @Builder.Default
    private Integer serviceChargeDecimalPlaces = 0;

    @Column(name = "service_charge_rounding_method", length = 20)
    private String serviceChargeRoundingMethod;

    @Column(name = "service_charge_type", nullable = false, length = 20)
    @Builder.Default
    private String serviceChargeType = "PERCENTAGE";

    @Column(name = "service_charge_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal serviceChargeAmount = BigDecimal.ZERO;

    // ─── Dayuse 설정 ────────────────────────────
    @Column(name = "day_use_enabled", nullable = false)
    @Builder.Default
    private Boolean dayUseEnabled = false;

    @Column(name = "day_use_start_time", length = 10)
    @Builder.Default
    private String dayUseStartTime = "10:00";

    @Column(name = "day_use_end_time", length = 10)
    @Builder.Default
    private String dayUseEndTime = "20:00";

    @Column(name = "day_use_default_hours")
    @Builder.Default
    private Integer dayUseDefaultHours = 5;

    // ─── 당일예약 마감 설정 ────────────────────────────
    @Column(name = "same_day_booking_enabled", nullable = false)
    @Builder.Default
    private Boolean sameDayBookingEnabled = true;

    @Column(name = "same_day_cutoff_time", nullable = false)
    @Builder.Default
    private Integer sameDayCutoffTime = 1080;

    @Column(name = "walk_in_override", nullable = false)
    @Builder.Default
    private Boolean walkInOverride = true;

    @OneToMany(mappedBy = "property", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Floor> floors = new ArrayList<>();

    @OneToMany(mappedBy = "property", fetch = FetchType.LAZY)
    @Builder.Default
    private List<RoomNumber> roomNumbers = new ArrayList<>();

    @OneToMany(mappedBy = "property", fetch = FetchType.LAZY)
    @Builder.Default
    private List<MarketCode> marketCodes = new ArrayList<>();

    public void update(String propertyName, String propertyType, String starRating,
                       String checkInTime, String checkOutTime, Integer totalRooms,
                       String timezone, String representativeName, String representativeNameEn,
                       String countryCode, String phone, String email, String businessNumber,
                       String introduction, String zipCode, String address, String addressDetail,
                       String addressEn, String addressDetailEn, String description,
                       String bizLicensePath, String logoPath) {
        this.propertyName = propertyName;
        this.propertyType = propertyType;
        this.starRating = starRating;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
        this.totalRooms = totalRooms;
        this.timezone = timezone;
        this.representativeName = representativeName;
        this.representativeNameEn = representativeNameEn;
        this.countryCode = countryCode;
        this.phone = phone;
        this.email = email;
        this.businessNumber = businessNumber;
        this.introduction = introduction;
        this.zipCode = zipCode;
        this.address = address;
        this.addressDetail = addressDetail;
        this.addressEn = addressEn;
        this.addressDetailEn = addressDetailEn;
        this.description = description;
        this.bizLicensePath = bizLicensePath;
        this.logoPath = logoPath;
    }

    /** Dayuse 설정 수정 */
    public void updateDayUse(Boolean dayUseEnabled, String dayUseStartTime,
                              String dayUseEndTime, Integer dayUseDefaultHours) {
        this.dayUseEnabled = dayUseEnabled != null ? dayUseEnabled : false;
        this.dayUseStartTime = dayUseStartTime != null ? dayUseStartTime : "10:00";
        this.dayUseEndTime = dayUseEndTime != null ? dayUseEndTime : "20:00";
        this.dayUseDefaultHours = dayUseDefaultHours != null ? dayUseDefaultHours : 5;
    }

    /** 당일예약 마감 설정 수정 */
    public void updateBookingCutoff(Boolean sameDayBookingEnabled, Integer sameDayCutoffTime,
                                     Boolean walkInOverride) {
        this.sameDayBookingEnabled = sameDayBookingEnabled != null ? sameDayBookingEnabled : true;
        this.sameDayCutoffTime = sameDayCutoffTime != null ? sameDayCutoffTime : 1080;
        this.walkInOverride = walkInOverride != null ? walkInOverride : true;
    }

    /** TAX/봉사료 정보 수정 */
    public void updateTaxServiceCharge(BigDecimal taxRate, Integer taxDecimalPlaces, String taxRoundingMethod,
                                       String serviceChargeType, BigDecimal serviceChargeRate,
                                       Integer serviceChargeDecimalPlaces, String serviceChargeRoundingMethod,
                                       BigDecimal serviceChargeAmount) {
        this.taxRate = taxRate;
        this.taxDecimalPlaces = taxDecimalPlaces;
        this.taxRoundingMethod = taxRoundingMethod;
        this.serviceChargeType = serviceChargeType != null ? serviceChargeType : "PERCENTAGE";
        this.serviceChargeRate = serviceChargeRate;
        this.serviceChargeDecimalPlaces = serviceChargeDecimalPlaces;
        this.serviceChargeRoundingMethod = serviceChargeRoundingMethod;
        this.serviceChargeAmount = serviceChargeAmount != null ? serviceChargeAmount : BigDecimal.ZERO;
    }
}
