package com.hola.reservation.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 객실 배정 가용성 최상위 응답 DTO
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoomAssignAvailabilityResponse {

    /** 현재 레그 총 요금 */
    private BigDecimal currentLegTotalPrice;

    /** 1박 평균 */
    private BigDecimal currentAvgNightly;

    /** 숙박일수 */
    private int nights;

    /** 통화 */
    private String currency;

    /** 객실타입별 그룹 목록 */
    private List<RoomTypeGroupResponse> roomTypeGroups;
}
