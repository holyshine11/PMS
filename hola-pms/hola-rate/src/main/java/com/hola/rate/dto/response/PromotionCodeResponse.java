package com.hola.rate.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Builder @NoArgsConstructor @AllArgsConstructor
public class PromotionCodeResponse {
    private Long id;
    private Long propertyId;
    private Long rateCodeId;
    private String rateCode;       // rt_rate_code.rate_code 조회
    private String promotionCode;
    private LocalDate promotionStartDate;
    private LocalDate promotionEndDate;
    private String descriptionKo;
    private String descriptionEn;
    private String promotionType;
    private Boolean useYn;
    private Integer sortOrder;
    private String downUpSign;
    private BigDecimal downUpValue;
    private String downUpUnit;
    private Integer roundingDecimalPoint;
    private Integer roundingDigits;
    private String roundingMethod;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
