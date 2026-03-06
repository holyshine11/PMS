package com.hola.room.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 무료 서비스 옵션 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FreeServiceOptionResponse {

    private Long id;
    private Long propertyId;
    private String serviceOptionCode;
    private String serviceNameKo;
    private String serviceNameEn;
    private String serviceType;
    private String applicableNights;
    private Integer quantity;
    private String quantityUnit;
    private Boolean useYn;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
