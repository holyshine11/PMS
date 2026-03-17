package com.hola.reservation.service;

import com.hola.reservation.dto.response.DashboardOperationResponse;
import com.hola.reservation.dto.response.DashboardPickupResponse;
import com.hola.reservation.dto.response.DashboardPropertyKpiResponse;

import java.util.List;

/**
 * 대시보드 서비스 인터페이스
 */
public interface DashboardService {

    /** 특정 프로퍼티 KPI 조회 (OCC%, ADR, RevPAR) */
    DashboardPropertyKpiResponse getPropertyKpi(Long propertyId);

    /** 특정 프로퍼티 운영현황 (도착/투숙/출발 카운트) */
    DashboardOperationResponse getOperation(Long propertyId);

    /** 특정 프로퍼티 7일 예약 추이 */
    DashboardPickupResponse getPickup(Long propertyId);

    /** 전체 프로퍼티 KPI 목록 (SUPER_ADMIN용 랭킹) */
    List<DashboardPropertyKpiResponse> getAllPropertyKpis();
}
