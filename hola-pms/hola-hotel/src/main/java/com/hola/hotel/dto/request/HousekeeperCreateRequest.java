package com.hola.hotel.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 하우스키퍼 담당자 등록 요청
 */
@Getter
@NoArgsConstructor
public class HousekeeperCreateRequest {

    @NotBlank(message = "아이디를 입력해주세요.")
    private String loginId;

    @NotBlank(message = "담당자명을 입력해주세요.")
    private String userName;

    private String email;
    private String phone;
    private String department;
    private String position;

    /** 역할: HOUSEKEEPER(청소 담당) 또는 HOUSEKEEPING_SUPERVISOR(감독자) */
    private String role;
}
