package com.hola.room.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class InventoryItemUpdateRequest {

    @NotBlank @Size(max = 200)
    private String itemNameKo;

    @Size(max = 200)
    private String itemNameEn;

    @NotBlank
    private String itemType;

    @NotBlank
    private String managementType;

    @Size(max = 50)
    private String externalSystemCode;

    @Min(0)
    private Integer totalQuantity;

    private Boolean useYn;
}
