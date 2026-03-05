package com.hola.common.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 블루웨이브 관리자 상세 응답 DTO (원본, 수정 폼용)
 */
@Getter
@Builder
@AllArgsConstructor
public class BluewaveAdminResponse {

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
    private String accountType;
    private Boolean useYn;
    private String createdAt;
}
