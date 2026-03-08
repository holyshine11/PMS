package com.hola.rate.mapper;

import com.hola.rate.dto.response.PromotionCodeListResponse;
import com.hola.rate.dto.response.PromotionCodeResponse;
import com.hola.rate.entity.PromotionCode;
import org.springframework.stereotype.Component;

@Component
public class PromotionCodeMapper {
    public PromotionCodeResponse toResponse(PromotionCode entity, String rateCodeName) {
        return PromotionCodeResponse.builder()
                .id(entity.getId())
                .propertyId(entity.getPropertyId())
                .rateCodeId(entity.getRateCodeId())
                .rateCode(rateCodeName)
                .promotionCode(entity.getPromotionCode())
                .promotionStartDate(entity.getPromotionStartDate())
                .promotionEndDate(entity.getPromotionEndDate())
                .descriptionKo(entity.getDescriptionKo())
                .descriptionEn(entity.getDescriptionEn())
                .promotionType(entity.getPromotionType())
                .useYn(entity.getUseYn())
                .sortOrder(entity.getSortOrder())
                .downUpSign(entity.getDownUpSign())
                .downUpValue(entity.getDownUpValue())
                .downUpUnit(entity.getDownUpUnit())
                .roundingDecimalPoint(entity.getRoundingDecimalPoint())
                .roundingDigits(entity.getRoundingDigits())
                .roundingMethod(entity.getRoundingMethod())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public PromotionCodeListResponse toListResponse(PromotionCode entity, String rateCodeName) {
        return PromotionCodeListResponse.builder()
                .id(entity.getId())
                .propertyId(entity.getPropertyId())
                .promotionCode(entity.getPromotionCode())
                .rateCode(rateCodeName)
                .promotionType(entity.getPromotionType())
                .useYn(entity.getUseYn())
                .promotionStartDate(entity.getPromotionStartDate())
                .promotionEndDate(entity.getPromotionEndDate())
                .build();
    }
}
