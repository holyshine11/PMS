package com.hola.rate.dto.response;

import lombok.*;
import java.time.LocalDate;

@Getter @Builder @NoArgsConstructor @AllArgsConstructor
public class PromotionCodeListResponse {
    private Long id;
    private Long propertyId;
    private String promotionCode;
    private String rateCode;          // 레이트 코드명 조회
    private String promotionType;
    private Boolean useYn;
    private LocalDate promotionStartDate;
    private LocalDate promotionEndDate;
}
