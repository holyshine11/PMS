package com.hola.reservation.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.common.security.AccessControlService;
import com.hola.reservation.dto.response.FrontDeskArrivalResponse;
import com.hola.reservation.dto.response.FrontDeskDepartureResponse;
import com.hola.reservation.dto.response.FrontDeskInHouseResponse;
import com.hola.reservation.service.FrontDeskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 프론트데스크 REST API
 */
@RestController
@RequestMapping("/api/v1/properties/{propertyId}/front-desk")
@RequiredArgsConstructor
public class FrontDeskApiController {

    private final FrontDeskService frontDeskService;
    private final AccessControlService accessControlService;

    /**
     * 오늘 도착 예정 목록
     */
    @GetMapping("/arrivals")
    public HolaResponse<List<FrontDeskArrivalResponse>> getArrivals(@PathVariable Long propertyId) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(frontDeskService.getArrivals(propertyId));
    }

    /**
     * 현재 투숙중 목록
     */
    @GetMapping("/in-house")
    public HolaResponse<List<FrontDeskInHouseResponse>> getInHouse(@PathVariable Long propertyId) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(frontDeskService.getInHouse(propertyId));
    }

    /**
     * 오늘 출발 예정 목록
     */
    @GetMapping("/departures")
    public HolaResponse<List<FrontDeskDepartureResponse>> getDepartures(@PathVariable Long propertyId) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(frontDeskService.getDepartures(propertyId));
    }

    /**
     * 도착/투숙/출발 카운트 요약
     */
    @GetMapping("/summary")
    public HolaResponse<Map<String, Long>> getSummary(@PathVariable Long propertyId) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(frontDeskService.getSummary(propertyId));
    }
}
