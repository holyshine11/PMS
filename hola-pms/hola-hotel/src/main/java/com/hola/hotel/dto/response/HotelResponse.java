package com.hola.hotel.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotelResponse {

    private Long id;
    private String hotelCode;
    private String hotelName;
    private String representativeName;
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
    private String description;
    private Integer sortOrder;
    private Boolean useYn;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
