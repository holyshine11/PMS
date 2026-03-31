package com.hola.hotel.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyResponse {

    private Long id;
    private Long hotelId;
    private String hotelName;
    private String propertyCode;
    private String propertyName;
    private String propertyType;
    private String starRating;
    private String checkInTime;
    private String checkOutTime;
    private Integer totalRooms;
    private String timezone;
    private String representativeName;
    private String representativeNameEn;
    private String countryCode;
    private String phone;
    private String email;
    private String businessNumber;
    private String introduction;
    private String zipCode;
    private String address;
    private String addressDetail;
    private String addressEn;
    private String addressDetailEn;
    private String description;
    private String bizLicensePath;
    private String logoPath;
    private BigDecimal taxRate;
    private Integer taxDecimalPlaces;
    private String taxRoundingMethod;
    private BigDecimal serviceChargeRate;
    private Integer serviceChargeDecimalPlaces;
    private String serviceChargeRoundingMethod;
    // Dayuse 설정
    private Boolean dayUseEnabled;
    private String dayUseStartTime;
    private String dayUseEndTime;
    private Integer dayUseDefaultHours;
    // 당일예약 마감 설정
    private Boolean sameDayBookingEnabled;
    private Integer sameDayCutoffTime;
    private Boolean walkInOverride;
    private Integer sortOrder;
    private Boolean useYn;
    private long floorCount;
    private long roomCount;
    private long marketCodeCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
