package com.hola.room.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 유료 서비스 옵션 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaidServiceOptionResponse {

    private Long id;
    private Long propertyId;
    private String serviceOptionCode;
    private String serviceNameKo;
    private String serviceNameEn;
    private String serviceType;
    private String applicableNights;
    private String currencyCode;
    private Boolean vatIncluded;
    private BigDecimal taxRate;
    private BigDecimal supplyPrice;
    private BigDecimal taxAmount;
    private BigDecimal vatIncludedPrice;
    private Integer quantity;
    private String quantityUnit;
    private String adminMemo;
    private Boolean useYn;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Phase 2 확장 필드
    private Long transactionCodeId;
    private String transactionCodeName;     // 조인: TransactionCode.codeNameKo
    private String transactionCodeValue;    // 조인: TransactionCode.transactionCode
    private String postingFrequency;
    private String packageScope;
    private Boolean sellSeparately;
    private Long inventoryItemId;
    private String inventoryItemName;
}
