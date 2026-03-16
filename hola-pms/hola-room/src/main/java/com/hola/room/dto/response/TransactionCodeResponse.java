package com.hola.room.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionCodeResponse {

    private Long id;
    private Long propertyId;
    private Long transactionGroupId;
    private String transactionGroupNameKo;  // 소속 그룹명
    private String mainGroupNameKo;         // 상위 메인 그룹명
    private String transactionCode;
    private String codeNameKo;
    private String codeNameEn;
    private String revenueCategory;
    private String codeType;
    private Integer sortOrder;
    private Boolean useYn;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
