package com.hola.room.mapper;

import com.hola.room.dto.response.PaidServiceOptionResponse;
import com.hola.room.entity.InventoryItem;
import com.hola.room.entity.PaidServiceOption;
import com.hola.room.entity.TransactionCode;
import org.springframework.stereotype.Component;

/**
 * 유료 서비스 옵션 Entity <-> DTO 변환
 */
@Component
public class PaidServiceOptionMapper {

    public PaidServiceOptionResponse toResponse(PaidServiceOption entity) {
        return toResponse(entity, null, null);
    }

    public PaidServiceOptionResponse toResponse(PaidServiceOption entity, TransactionCode tc) {
        return toResponse(entity, tc, null);
    }

    /**
     * TransactionCode + InventoryItem 조인 정보 포함 변환
     */
    public PaidServiceOptionResponse toResponse(PaidServiceOption entity, TransactionCode tc, InventoryItem inv) {
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
                // Phase 2 확장
                .transactionCodeId(entity.getTransactionCodeId())
                .transactionCodeName(tc != null ? tc.getCodeNameKo() : null)
                .transactionCodeValue(tc != null ? tc.getTransactionCode() : null)
                .postingFrequency(entity.getPostingFrequency())
                .packageScope(entity.getPackageScope())
                .sellSeparately(entity.getSellSeparately())
                .inventoryItemId(entity.getInventoryItemId())
                .inventoryItemName(inv != null ? inv.getItemNameKo() : null)
                .build();
    }
}
