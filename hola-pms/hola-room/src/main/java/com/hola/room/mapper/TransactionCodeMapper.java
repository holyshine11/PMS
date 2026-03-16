package com.hola.room.mapper;

import com.hola.room.dto.response.TransactionCodeGroupResponse;
import com.hola.room.dto.response.TransactionCodeGroupTreeResponse;
import com.hola.room.dto.response.TransactionCodeResponse;
import com.hola.room.dto.response.TransactionCodeSelectorResponse;
import com.hola.room.entity.TransactionCode;
import com.hola.room.entity.TransactionCodeGroup;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class TransactionCodeMapper {

    public TransactionCodeGroupResponse toGroupResponse(TransactionCodeGroup entity) {
        return TransactionCodeGroupResponse.builder()
                .id(entity.getId())
                .propertyId(entity.getPropertyId())
                .groupCode(entity.getGroupCode())
                .groupNameKo(entity.getGroupNameKo())
                .groupNameEn(entity.getGroupNameEn())
                .groupType(entity.getGroupType())
                .parentGroupId(entity.getParentGroupId())
                .sortOrder(entity.getSortOrder())
                .useYn(entity.getUseYn())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public TransactionCodeGroupTreeResponse toGroupTreeResponse(
            TransactionCodeGroup entity,
            List<TransactionCodeGroupTreeResponse> children) {
        return TransactionCodeGroupTreeResponse.builder()
                .id(entity.getId())
                .groupCode(entity.getGroupCode())
                .groupNameKo(entity.getGroupNameKo())
                .groupNameEn(entity.getGroupNameEn())
                .groupType(entity.getGroupType())
                .sortOrder(entity.getSortOrder())
                .useYn(entity.getUseYn())
                .children(children)
                .build();
    }

    /**
     * TransactionCode → Response 변환
     * @param groupMap 그룹 ID → 그룹 엔티티 매핑 (그룹명 조인용)
     */
    public TransactionCodeResponse toCodeResponse(TransactionCode entity,
                                                   Map<Long, TransactionCodeGroup> groupMap) {
        TransactionCodeGroup subGroup = groupMap.get(entity.getTransactionGroupId());
        String subGroupName = subGroup != null ? subGroup.getGroupNameKo() : null;

        // 상위 MAIN 그룹명 조회
        String mainGroupName = null;
        if (subGroup != null && subGroup.getParentGroupId() != null) {
            TransactionCodeGroup mainGroup = groupMap.get(subGroup.getParentGroupId());
            mainGroupName = mainGroup != null ? mainGroup.getGroupNameKo() : null;
        }

        return TransactionCodeResponse.builder()
                .id(entity.getId())
                .propertyId(entity.getPropertyId())
                .transactionGroupId(entity.getTransactionGroupId())
                .transactionGroupNameKo(subGroupName)
                .mainGroupNameKo(mainGroupName)
                .transactionCode(entity.getTransactionCode())
                .codeNameKo(entity.getCodeNameKo())
                .codeNameEn(entity.getCodeNameEn())
                .revenueCategory(entity.getRevenueCategory())
                .codeType(entity.getCodeType())
                .sortOrder(entity.getSortOrder())
                .useYn(entity.getUseYn())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public TransactionCodeSelectorResponse toSelectorResponse(TransactionCode entity) {
        return TransactionCodeSelectorResponse.builder()
                .id(entity.getId())
                .transactionCode(entity.getTransactionCode())
                .codeNameKo(entity.getCodeNameKo())
                .revenueCategory(entity.getRevenueCategory())
                .build();
    }
}
