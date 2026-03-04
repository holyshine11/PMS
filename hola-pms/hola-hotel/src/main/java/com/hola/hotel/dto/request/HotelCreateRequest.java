package com.hola.hotel.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class HotelCreateRequest {

    @NotBlank(message = "호텔명을 입력해주세요.")
    @Size(max = 200, message = "호텔명은 200자 이하입니다.")
    private String hotelName;

    @Size(max = 50, message = "대표자명은 50자 이하입니다.")
    private String representativeName;

    @Size(max = 100, message = "영문 대표자명은 100자 이하입니다.")
    private String representativeNameEn;

    private String countryCode;
    private String phone;
    private String email;
    private String zipCode;
    private String address;
    private String addressDetail;
    private String addressEn;
    private String addressDetailEn;
    private String introduction;
    private Boolean useYn;
    private Integer sortOrder;
}
