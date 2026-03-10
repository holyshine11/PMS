package com.hola.hotel.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 얼리체크인/레이트체크아웃 요금 정책 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EarlyLateFeePolicyResponse {

    private Long id;
    private Long propertyId;
    private String policyType;
    private String timeFrom;
    private String timeTo;
    private String feeType;
    private BigDecimal feeValue;
    private String description;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
