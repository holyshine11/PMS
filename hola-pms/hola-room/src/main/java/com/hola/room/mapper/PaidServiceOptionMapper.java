package com.hola.room.mapper;

import com.hola.room.dto.response.PaidServiceOptionResponse;
import com.hola.room.entity.PaidServiceOption;
import org.springframework.stereotype.Component;

/**
 * 유료 서비스 옵션 Entity <-> DTO 변환
 */
@Component
public class PaidServiceOptionMapper {

    public PaidServiceOptionResponse toResponse(PaidServiceOption entity) {
        return PaidServiceOptionResponse.builder()
                .id(entity.getId())
                .propertyId(entity.getPropertyId())
                .serviceOptionCode(entity.getServiceOptionCode())
                .serviceNameKo(entity.getServiceNameKo())
                .serviceNameEn(entity.getServiceNameEn())
                .serviceType(entity.getServiceType())
                .applicableNights(entity.getApplicableNights())
                .currencyCode(entity.getCurrencyCode())
                .vatIncluded(entity.getVatIncluded())
                .taxRate(entity.getTaxRate())
                .supplyPrice(entity.getSupplyPrice())
                .taxAmount(entity.getTaxAmount())
                .vatIncludedPrice(entity.getVatIncludedPrice())
                .quantity(entity.getQuantity())
                .quantityUnit(entity.getQuantityUnit())
                .adminMemo(entity.getAdminMemo())
                .useYn(entity.getUseYn())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
