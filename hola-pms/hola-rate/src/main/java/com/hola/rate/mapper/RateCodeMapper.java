package com.hola.rate.mapper;

import com.hola.rate.dto.response.RateCodeListResponse;
import com.hola.rate.dto.response.RateCodeResponse;
import com.hola.rate.entity.RateCode;
import com.hola.rate.entity.RateCodeRoomType;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 레이트 코드 Entity <-> DTO 변환
 */
@Component
public class RateCodeMapper {

    public RateCodeResponse toResponse(RateCode rateCode, String marketCodeName,
                                       List<RateCodeRoomType> roomTypeMappings) {
        List<Long> roomTypeIds = roomTypeMappings.stream()
                .map(RateCodeRoomType::getRoomTypeId)
                .toList();

        return RateCodeResponse.builder()
                .id(rateCode.getId())
                .propertyId(rateCode.getPropertyId())
                .rateCode(rateCode.getRateCode())
                .rateNameKo(rateCode.getRateNameKo())
                .rateNameEn(rateCode.getRateNameEn())
                .rateCategory(rateCode.getRateCategory())
                .marketCodeId(rateCode.getMarketCodeId())
                .marketCodeName(marketCodeName)
                .currency(rateCode.getCurrency())
                .saleStartDate(rateCode.getSaleStartDate())
                .saleEndDate(rateCode.getSaleEndDate())
                .minStayDays(rateCode.getMinStayDays())
                .maxStayDays(rateCode.getMaxStayDays())
                .sortOrder(rateCode.getSortOrder())
                .useYn(rateCode.getUseYn())
                .createdAt(rateCode.getCreatedAt())
                .updatedAt(rateCode.getUpdatedAt())
                .roomTypeIds(roomTypeIds)
                .build();
    }

    public RateCodeListResponse toListResponse(RateCode rateCode, String marketCodeName,
                                               long roomTypeCount) {
        return RateCodeListResponse.builder()
                .id(rateCode.getId())
                .propertyId(rateCode.getPropertyId())
                .rateCode(rateCode.getRateCode())
                .rateNameKo(rateCode.getRateNameKo())
                .rateCategory(rateCode.getRateCategory())
                .marketCodeName(marketCodeName)
                .currency(rateCode.getCurrency())
                .saleStartDate(rateCode.getSaleStartDate())
                .saleEndDate(rateCode.getSaleEndDate())
                .roomTypeCount(roomTypeCount)
                .useYn(rateCode.getUseYn())
                .updatedAt(rateCode.getUpdatedAt())
                .build();
    }
}
