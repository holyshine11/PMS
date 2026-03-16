package com.hola.room.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TransactionCodeUpdateRequest {

    @NotNull
    private Long transactionGroupId;

    @NotBlank @Size(max = 200)
    private String codeNameKo;

    @Size(max = 200)
    private String codeNameEn;

    @NotBlank
    private String revenueCategory;

    private Integer sortOrder;

    private Boolean useYn;
}
