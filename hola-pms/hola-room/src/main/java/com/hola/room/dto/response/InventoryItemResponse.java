package com.hola.room.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItemResponse {

    private Long id;
    private Long propertyId;
    private String itemCode;
    private String itemNameKo;
    private String itemNameEn;
    private String itemType;
    private String managementType;
    private String externalSystemCode;
    private Integer totalQuantity;
    private Integer sortOrder;
    private Boolean useYn;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
