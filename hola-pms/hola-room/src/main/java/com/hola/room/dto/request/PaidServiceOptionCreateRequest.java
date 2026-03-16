package com.hola.room.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 유료 서비스 옵션 등록 요청 DTO
 */
@Getter
@NoArgsConstructor
public class PaidServiceOptionCreateRequest {

    @NotBlank(message = "서비스 옵션 코드명을 입력해주세요.")
    @Size(max = 50, message = "서비스 옵션 코드명은 50자 이하입니다.")
    private String serviceOptionCode;

    @NotBlank(message = "서비스명(국문)을 입력해주세요.")
    @Size(max = 200, message = "서비스명은 200자 이하입니다.")
    private String serviceNameKo;

    @Size(max = 200, message = "서비스명(영문)은 200자 이하입니다.")
    private String serviceNameEn;

    @NotBlank(message = "서비스 옵션 유형을 선택해주세요.")
    private String serviceType;

    @NotBlank(message = "적용 박수를 선택해주세요.")
    private String applicableNights;

    @NotBlank(message = "통화 구분을 선택해주세요.")
    private String currencyCode;

    private Boolean vatIncluded;

    @NotNull(message = "세율을 입력해주세요.")
    @DecimalMin(value = "0", message = "세율은 0 이상이어야 합니다.")
    @DecimalMax(value = "100", message = "세율은 100 이하여야 합니다.")
    private BigDecimal taxRate;

    @NotNull(message = "공급가를 입력해주세요.")
    @DecimalMin(value = "0", message = "공급가는 0 이상이어야 합니다.")
    private BigDecimal supplyPrice;

    @NotNull(message = "TAX 금액을 입력해주세요.")
    private BigDecimal taxAmount;

    @NotNull(message = "VAT 포함 가격을 입력해주세요.")
    private BigDecimal vatIncludedPrice;

    @NotNull(message = "수량을 입력해주세요.")
    @Min(value = 1, message = "수량은 1 이상이어야 합니다.")
    private Integer quantity;

    @NotBlank(message = "수량 단위를 선택해주세요.")
    private String quantityUnit;

    @Size(max = 2000, message = "관리자 메모는 2000자 이하입니다.")
    private String adminMemo;

    private Boolean useYn;

    // Phase 2 확장 필드
    private Long transactionCodeId;
    private String postingFrequency;
    private String packageScope;
    private Boolean sellSeparately;
    private Long inventoryItemId;
}
