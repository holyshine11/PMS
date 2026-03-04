package com.hola.hotel.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertySettlementResponse {

    private Long id;
    private Long propertyId;
    private String countryType;
    private String accountNumber;
    private String bankName;
    private String bankCode;
    private String accountHolder;
    private String routingNumber;
    private String swiftCode;
    private String settlementDay;
    private String bankBookPath;
    private Integer sortOrder;
    private Boolean useYn;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
