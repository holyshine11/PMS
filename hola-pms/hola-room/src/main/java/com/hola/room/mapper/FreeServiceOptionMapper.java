package com.hola.room.mapper;

import com.hola.room.dto.response.FreeServiceOptionResponse;
import com.hola.room.entity.FreeServiceOption;
import org.springframework.stereotype.Component;

/**
 * 무료 서비스 옵션 Entity <-> DTO 변환
 */
@Component
public class FreeServiceOptionMapper {

    public FreeServiceOptionResponse toResponse(FreeServiceOption entity) {
        return FreeServiceOptionResponse.builder()
                .id(entity.getId())
                .propertyId(entity.getPropertyId())
                .serviceOptionCode(entity.getServiceOptionCode())
                .serviceNameKo(entity.getServiceNameKo())
                .serviceNameEn(entity.getServiceNameEn())
                .serviceType(entity.getServiceType())
                .applicableNights(entity.getApplicableNights())
                .quantity(entity.getQuantity())
                .quantityUnit(entity.getQuantityUnit())
                .useYn(entity.getUseYn())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
