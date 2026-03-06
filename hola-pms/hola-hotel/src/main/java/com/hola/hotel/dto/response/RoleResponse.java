package com.hola.hotel.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 권한 상세 응답 DTO
 */
@Getter
@Builder
@AllArgsConstructor
public class RoleResponse {

    private Long id;
    private String roleName;
    private Long hotelId;
    private String hotelName;
    private Long propertyId;
    private String propertyName;
    private Boolean useYn;
    private String updatedAt;
    private List<Long> menuIds;
}
