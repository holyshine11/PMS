package com.hola.hotel.dto.response;

import lombok.*;

import java.math.BigDecimal;

/**
 * 청소 정책 응답 (오버라이드 여부 포함)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HkCleaningPolicyResponse {

    private Long id;
    private Long propertyId;
    private Long roomTypeId;
    private String roomTypeName;
    private String roomTypeCode;
    private Boolean stayoverEnabled;
    private Integer stayoverFrequency;
    private Boolean turndownEnabled;
    private BigDecimal stayoverCredit;
    private BigDecimal turndownCredit;
    private String stayoverPriority;
    private String dndPolicy;
    private Integer dndMaxSkipDays;
    private String note;
    private boolean overridden;
}
