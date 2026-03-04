package com.hola.hotel.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 프로퍼티 관리자 목록 응답 DTO (마스킹 적용, 5컬럼)
 */
@Getter
@Builder
@AllArgsConstructor
public class PropertyAdminListResponse {

    private Long id;
    private String loginId;          // 마스킹: 앞5자 + *****
    private String userName;         // 마스킹: 성 + * + 끝자
    private Boolean useYn;
    private String createdAt;        // 회원가입일
}
