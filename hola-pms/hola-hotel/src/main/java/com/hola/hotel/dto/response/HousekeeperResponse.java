package com.hola.hotel.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 하우스키퍼 담당자 응답
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HousekeeperResponse {

    private Long id;
    private String loginId;
    private String userName;
    private String email;
    private String phone;
    private String department;
    private String position;
    private String role;         // HOUSEKEEPER 또는 HOUSEKEEPING_SUPERVISOR
    private String roleLabel;    // "청소 담당" 또는 "감독자"
    private Boolean useYn;
    private LocalDateTime createdAt;
    private String sectionName;  // 소속 구역명
}
