package com.hola.reservation.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 객실타입별 그룹 응답 DTO
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoomTypeGroupResponse {

    private Long roomTypeId;
    private String roomTypeCode;
    private String roomTypeDescription;
    private String roomClassName;
    private int maxAdults;
    private int maxChildren;
    private BigDecimal roomSize;

    /** 현재 레그 roomType 일치 여부 */
    private boolean recommended;

    /** 해당 타입 요금 (null 가능 = 요금 미설정) */
    private BigDecimal totalPrice;

    /** 1박 평균 */
    private BigDecimal avgNightly;

    /** 현재 대비 차이 (+/-) */
    private BigDecimal priceDiff;

    /** 일자별 요금 상세 */
    private List<DailyChargeDetail> dailyCharges;

    /** 층별 그룹 목록 */
    private List<FloorGroupResponse> floors;
}
