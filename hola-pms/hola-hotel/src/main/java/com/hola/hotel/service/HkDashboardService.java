package com.hola.hotel.service;

import com.hola.hotel.dto.response.HkDashboardResponse;

import java.time.LocalDate;
import java.util.List;

/**
 * 하우스키핑 대시보드 서비스 인터페이스
 */
public interface HkDashboardService {

    /** 대시보드 데이터 조회 (상태별 카운트, 하우스키퍼별 집계, 객실 상태 요약) */
    HkDashboardResponse getDashboard(Long propertyId, LocalDate date);

    /** 프로퍼티에 배정된 하우스키퍼 + 감독자 목록 */
    List<HkDashboardResponse.HousekeeperSummary> getHousekeepers(Long propertyId);
}
