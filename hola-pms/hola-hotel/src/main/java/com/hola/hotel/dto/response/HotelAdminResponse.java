package com.hola.hotel.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 호텔 관리자 상세 응답 DTO (원본, 수정 폼용)
 */
@Getter
@Builder
@AllArgsConstructor
public class HotelAdminResponse {

    private Long id;
    private String memberNumber;
    private String loginId;
    private String userName;
    private String email;
    private String phone;
    private String phoneCountryCode;
    private String mobileCountryCode;
    private String mobile;
    private String department;
    private String position;
    private String roleName;
    private Long roleId;
    private String accountType;
    private Boolean useYn;
    private Long hotelId;
    private String hotelName;
    private List<Long> propertyIds;
    private String createdAt;
}
