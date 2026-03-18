package com.hola.reservation.service;

import com.hola.reservation.dto.response.FrontDeskOperationResponse;

import java.util.List;
import java.util.Map;

/**
 * 프론트데스크 운영현황 서비스
 */
public interface FrontDeskService {

    /**
     * 오늘 도착 예정 리스트
     */
    List<FrontDeskOperationResponse> getArrivals(Long propertyId);

    /**
     * 현재 투숙중 리스트
     */
    List<FrontDeskOperationResponse> getInHouse(Long propertyId);

    /**
     * 오늘 출발 예정 리스트
     */
    List<FrontDeskOperationResponse> getDepartures(Long propertyId);

    /**
     * 운영 요약 (도착/투숙/출발 건수)
     */
    Map<String, Long> getSummary(Long propertyId);
}
