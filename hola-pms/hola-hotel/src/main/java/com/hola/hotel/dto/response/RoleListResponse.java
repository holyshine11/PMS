package com.hola.hotel.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 권한 목록 응답 DTO
 */
@Getter
@Builder
@AllArgsConstructor
public class RoleListResponse {

    private Long id;
    private String roleName;
    private Long hotelId;
    private String hotelName;
    private Long propertyId;
    private String propertyName;
    private Boolean useYn;
    private String updatedAt;
}
