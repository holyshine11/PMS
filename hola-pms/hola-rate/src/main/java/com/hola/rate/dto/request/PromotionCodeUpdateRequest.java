package com.hola.rate.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PromotionCodeUpdateRequest {
    @NotNull private Long rateCodeId;
    @NotNull private LocalDate promotionStartDate;
    @NotNull private LocalDate promotionEndDate;
    @NotBlank private String descriptionKo;
    private String descriptionEn;
    @NotBlank private String promotionType;
    private Boolean useYn;
    private Integer sortOrder;
    private String downUpSign;
    private BigDecimal downUpValue;
    private String downUpUnit;
    private Integer roundingDecimalPoint;
    private Integer roundingDigits;
    private String roundingMethod;
}
