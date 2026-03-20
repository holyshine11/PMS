package com.hola.hotel.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 하우스키퍼 담당자 수정 요청
 */
@Getter
@Setter
@NoArgsConstructor
public class HousekeeperUpdateRequest {

    private String userName;
    private String email;
    private String phone;
    private String department;
    private String position;

    /** 역할: HOUSEKEEPER(청소 담당) 또는 HOUSEKEEPING_SUPERVISOR(감독자) */
    private String role;
    private Boolean useYn;
}
