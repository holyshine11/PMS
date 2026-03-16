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
}
