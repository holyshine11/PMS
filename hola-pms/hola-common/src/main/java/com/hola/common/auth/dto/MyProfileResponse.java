package com.hola.common.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 내 프로필 조회 응답 DTO
 */
@Getter
@Builder
@AllArgsConstructor
public class MyProfileResponse {

    private String loginId;
    private String memberNumber;
    private String accountType;
    private String userName;
    private String email;
    private String phoneCountryCode;
    private String phone;
    private String mobileCountryCode;
    private String mobile;
    private String department;
    private String position;
    private String roleName;
    private String createdAt;
}
