package com.hola.reservation.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.common.security.AccessControlService;
import com.hola.reservation.dto.response.FrontDeskOperationResponse;
import com.hola.reservation.service.FrontDeskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 프론트데스크 운영현황 REST API
 */
@Tag(name = "프론트데스크", description = "체크인/체크아웃, 도착/출발/인하우스 조회 API")
@RestController
@RequestMapping("/api/v1/properties/{propertyId}/front-desk")
@RequiredArgsConstructor
public class FrontDeskApiController {

    private final FrontDeskService frontDeskService;
    private final AccessControlService accessControlService;

    /**
     * 운영 요약 (도착/투숙/출발 건수)
     */
    @Operation(summary = "프론트데스크 요약", description = "도착/출발/인하우스 건수 요약")
    @GetMapping("/summary")
    public HolaResponse<Map<String, Long>> getSummary(@PathVariable Long propertyId) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(frontDeskService.getSummary(propertyId));
    }

    /**
     * 전체 운영현황 목록 (모든 상태 포함)
     */
    @Operation(summary = "전체 운영 목록", description = "도착+출발+인하우스 통합 조회")
    @GetMapping("/all")
    public HolaResponse<List<FrontDeskOperationResponse>> getAllOperations(@PathVariable Long propertyId) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(frontDeskService.getAllOperations(propertyId));
    }

    /**
     * 도착 예정 목록
     */
    @Operation(summary = "도착 예약 목록", description = "오늘 체크인 예정 예약 조회")
    @GetMapping("/arrivals")
    public HolaResponse<List<FrontDeskOperationResponse>> getArrivals(@PathVariable Long propertyId) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(frontDeskService.getArrivals(propertyId));
    }

    /**
     * 투숙중 목록
     */
    @Operation(summary = "인하우스 목록", description = "현재 투숙 중인 예약 조회")
    @GetMapping("/in-house")
    public HolaResponse<List<FrontDeskOperationResponse>> getInHouse(@PathVariable Long propertyId) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(frontDeskService.getInHouse(propertyId));
    }

    /**
     * 출발 예정 목록
     */
    @Operation(summary = "출발 예약 목록", description = "오늘 체크아웃 예정 예약 조회")
    @GetMapping("/departures")
    public HolaResponse<List<FrontDeskOperationResponse>> getDepartures(@PathVariable Long propertyId) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(frontDeskService.getDepartures(propertyId));
    }
}
