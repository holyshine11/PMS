package com.hola.room.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TransactionCodeCreateRequest {

    @NotNull
    private Long transactionGroupId;

    @NotBlank @Size(max = 10)
    private String transactionCode;

    @NotBlank @Size(max = 200)
    private String codeNameKo;

    @Size(max = 200)
    private String codeNameEn;

    @NotBlank
    private String revenueCategory;     // LODGING, FOOD_BEVERAGE, MISC, TAX, NON_REVENUE

    @NotBlank
    private String codeType;            // CHARGE / PAYMENT

    private Integer sortOrder;
}
