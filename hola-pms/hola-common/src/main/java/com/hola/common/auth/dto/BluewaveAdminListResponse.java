package com.hola.common.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 블루웨이브 관리자 목록 응답 DTO (마스킹 적용)
 */
@Getter
@Builder
@AllArgsConstructor
public class BluewaveAdminListResponse {

    private Long id;
    private String loginId;          // 마스킹: 앞5자 + *****
    private String userName;         // 마스킹: 성 + * + 끝자
    private String accountType;      // "블루웨이브 관리자"
    private Boolean useYn;
    private String createdAt;        // 회원가입일
}
