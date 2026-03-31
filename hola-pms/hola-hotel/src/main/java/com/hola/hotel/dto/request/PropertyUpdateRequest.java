package com.hola.hotel.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PropertyUpdateRequest {

    @NotBlank(message = "프로퍼티명을 입력해주세요.")
    @Size(max = 200, message = "프로퍼티명은 200자 이하입니다.")
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
    // Dayuse 설정
    private Boolean dayUseEnabled;
    private String dayUseStartTime;
    private String dayUseEndTime;
    private Integer dayUseDefaultHours;
    // 당일예약 마감 설정
    private Boolean sameDayBookingEnabled;
    private Integer sameDayCutoffTime;
    private Boolean walkInOverride;

    private Boolean useYn;
    private Integer sortOrder;
}
