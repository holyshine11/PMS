package com.hola.room.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class InventoryItemCreateRequest {

    @NotBlank @Size(max = 30)
    private String itemCode;

    @NotBlank @Size(max = 200)
    private String itemNameKo;

    @Size(max = 200)
    private String itemNameEn;

    @NotBlank
    private String itemType;            // EXTRA_BED, CRIB, ROLLAWAY, EQUIPMENT

    @NotBlank
    private String managementType;      // INTERNAL / EXTERNAL

    @Size(max = 50)
    private String externalSystemCode;

    @Min(0)
    private Integer totalQuantity;
}
