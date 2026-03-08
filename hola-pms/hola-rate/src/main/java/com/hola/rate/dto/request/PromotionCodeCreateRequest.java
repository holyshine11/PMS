package com.hola.rate.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PromotionCodeCreateRequest {
    @NotNull private Long rateCodeId;
    @NotBlank private String promotionCode;
    @NotNull private LocalDate promotionStartDate;
    @NotNull private LocalDate promotionEndDate;
    @NotBlank private String descriptionKo;
    private String descriptionEn;
    @NotBlank private String promotionType; // COMPANY, PROMOTION, OTA
    private Boolean useYn;
    private Integer sortOrder;
    // Down/Up sale
    private String downUpSign;
    private BigDecimal downUpValue;
    private String downUpUnit;
    private Integer roundingDecimalPoint;
    private Integer roundingDigits;
    private String roundingMethod;
}
