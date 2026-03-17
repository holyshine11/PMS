package com.hola.reservation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 프로퍼티 KPI 대시보드 응답 DTO
 * OCC%, ADR, RevPAR 등 핵심 지표
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardPropertyKpiResponse {

    private Long propertyId;
    private String propertyName;

    // 객실 현황
    private long totalRooms;        // 판매가능객실 수
    private long soldRooms;         // 판매객실 수 (INHOUSE + CHECK_IN)

    // 매출
    private BigDecimal totalRevenue; // 오늘 매출 합계

    // KPI 지표
    private BigDecimal occupancyRate; // OCC% = 판매객실/판매가능객실×100
    private BigDecimal adr;           // ADR = 총매출/판매객실
    private BigDecimal revPar;        // RevPAR = 총매출/판매가능객실
}
