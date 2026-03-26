package com.hola.hotel.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 청소 정책 오버라이드 요청
 */
@Getter
@NoArgsConstructor
public class HkCleaningPolicyRequest {

    private Long roomTypeId;
    private Boolean stayoverEnabled;
    private Integer stayoverFrequency;
    private Boolean turndownEnabled;
    private BigDecimal stayoverCredit;
    private BigDecimal turndownCredit;
    private String stayoverPriority;
    private String dndPolicy;
    private Integer dndMaxSkipDays;
    private String note;
}
