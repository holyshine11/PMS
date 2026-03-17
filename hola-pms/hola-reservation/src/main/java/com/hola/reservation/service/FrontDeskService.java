package com.hola.reservation.service;

import com.hola.reservation.dto.response.FrontDeskArrivalResponse;
import com.hola.reservation.dto.response.FrontDeskDepartureResponse;
import com.hola.reservation.dto.response.FrontDeskInHouseResponse;

import java.util.List;
import java.util.Map;

/**
 * 프론트데스크 서비스 인터페이스
 */
public interface FrontDeskService {

    /** 오늘 도착 예정 목록 */
    List<FrontDeskArrivalResponse> getArrivals(Long propertyId);

    /** 현재 투숙중 목록 */
    List<FrontDeskInHouseResponse> getInHouse(Long propertyId);

    /** 오늘 출발 예정 목록 */
    List<FrontDeskDepartureResponse> getDepartures(Long propertyId);

    /** 도착/투숙/출발 카운트 요약 */
    Map<String, Long> getSummary(Long propertyId);
}
