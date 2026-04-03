package com.hola.reservation.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpgradePreviewResponse {

    private Long fromRoomTypeId;
    private String fromRoomTypeName;
    private Long toRoomTypeId;
    private String toRoomTypeName;
    private BigDecimal currentTotalCharge;      // 현재 잔여 숙박 총액
    private BigDecimal newTotalCharge;          // 업그레이드 후 잔여 숙박 총액
    private BigDecimal priceDifference;         // 차액
    private Integer remainingNights;            // 잔여 숙박일
    private List<DailyChargeDiff> dailyDiffs;   // 일자별 차액 상세
    private String currentRateCodeName;         // 현재 레이트코드명
    private Long targetRateCodeId;              // 대상 레이트코드 ID
    private String targetRateCodeName;          // 대상 레이트코드명
    private boolean rateCodeChanged;            // 레이트코드 변경 여부
    private List<RateCodeOption> availableRateCodes;  // 대상 객실타입에 매핑된 레이트코드 후보

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RateCodeOption {
        private Long id;
        private String rateCode;
        private String rateNameKo;
        private boolean current;         // 현재 예약의 레이트코드 여부
        private boolean recommended;     // 자동 선택된 추천 레이트코드 여부
    }
}
