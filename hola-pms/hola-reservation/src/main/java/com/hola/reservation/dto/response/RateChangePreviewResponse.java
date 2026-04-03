package com.hola.reservation.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 레이트코드 변경 시 요금 미리보기 응답
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateChangePreviewResponse {

    private Long currentRateCodeId;
    private String currentRateCodeName;
    private Long newRateCodeId;
    private String newRateCodeName;
    private BigDecimal currentTotal;       // 현재 전체 요금 합계
    private BigDecimal newTotal;           // 변경 후 예상 요금 합계
    private BigDecimal difference;         // 차액 (newTotal - currentTotal)
    private List<LegPreview> legs;         // Leg별 미리보기

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LegPreview {
        private Long legId;
        private String roomTypeName;
        private BigDecimal currentCharge;
        private BigDecimal newCharge;
        private BigDecimal difference;
    }
}
