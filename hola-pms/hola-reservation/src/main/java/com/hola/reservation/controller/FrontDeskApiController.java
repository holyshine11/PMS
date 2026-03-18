package com.hola.reservation.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.common.security.AccessControlService;
import com.hola.reservation.dto.response.FrontDeskOperationResponse;
import com.hola.reservation.service.FrontDeskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 프론트데스크 운영현황 REST API
 */
@RestController
@RequestMapping("/api/v1/properties/{propertyId}/front-desk")
@RequiredArgsConstructor
public class FrontDeskApiController {

    private final FrontDeskService frontDeskService;
    private final AccessControlService accessControlService;

    /**
     * 운영 요약 (도착/투숙/출발 건수)
     */
    @GetMapping("/summary")
    public HolaResponse<Map<String, Long>> getSummary(@PathVariable Long propertyId) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(frontDeskService.getSummary(propertyId));
    }

    /**
     * 전체 운영현황 목록 (모든 상태 포함)
     */
    @GetMapping("/all")
    public HolaResponse<List<FrontDeskOperationResponse>> getAllOperations(@PathVariable Long propertyId) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(frontDeskService.getAllOperations(propertyId));
    }

    /**
     * 도착 예정 목록
     */
    @GetMapping("/arrivals")
    public HolaResponse<List<FrontDeskOperationResponse>> getArrivals(@PathVariable Long propertyId) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(frontDeskService.getArrivals(propertyId));
    }

    /**
     * 투숙중 목록
     */
    @GetMapping("/in-house")
    public HolaResponse<List<FrontDeskOperationResponse>> getInHouse(@PathVariable Long propertyId) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(frontDeskService.getInHouse(propertyId));
    }

    /**
     * 출발 예정 목록
     */
    @GetMapping("/departures")
    public HolaResponse<List<FrontDeskOperationResponse>> getDepartures(@PathVariable Long propertyId) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(frontDeskService.getDepartures(propertyId));
    }
}
