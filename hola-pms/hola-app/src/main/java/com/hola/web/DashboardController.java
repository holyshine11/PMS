package com.hola.web;

import com.hola.common.dto.HolaResponse;
import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.common.security.AccessControlService;
import com.hola.reservation.dto.response.DashboardOperationResponse;
import com.hola.reservation.dto.response.DashboardPickupResponse;
import com.hola.reservation.dto.response.DashboardPropertyKpiResponse;
import com.hola.reservation.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * 대시보드 컨트롤러 (View + REST API)
 */
@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final AccessControlService accessControlService;

    // === View ===

    @GetMapping({"/", "/admin/dashboard"})
    public String dashboard() {
        return "dashboard";
    }

    // === REST API ===

    /**
     * 전체 프로퍼티 KPI (SUPER_ADMIN 전용 랭킹)
     */
    @GetMapping("/api/v1/dashboard")
    @ResponseBody
    public HolaResponse<List<DashboardPropertyKpiResponse>> getAllKpis() {
        // SUPER_ADMIN만 전체 프로퍼티 랭킹 조회 가능
        if (!accessControlService.getCurrentUser().isSuperAdmin()) {
            throw new HolaException(ErrorCode.FORBIDDEN);
        }
        return HolaResponse.success(dashboardService.getAllPropertyKpis());
    }

    /**
     * 특정 프로퍼티 KPI
     */
    @GetMapping("/api/v1/dashboard/property/{propertyId}")
    @ResponseBody
    public HolaResponse<DashboardPropertyKpiResponse> getPropertyKpi(
            @PathVariable Long propertyId) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(dashboardService.getPropertyKpi(propertyId));
    }

    /**
     * 특정 프로퍼티 운영현황
     */
    @GetMapping("/api/v1/dashboard/operation/{propertyId}")
    @ResponseBody
    public HolaResponse<DashboardOperationResponse> getOperation(
            @PathVariable Long propertyId) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(dashboardService.getOperation(propertyId));
    }

    /**
     * 특정 프로퍼티 7일 예약 추이
     */
    @GetMapping("/api/v1/dashboard/pickup/{propertyId}")
    @ResponseBody
    public HolaResponse<DashboardPickupResponse> getPickup(
            @PathVariable Long propertyId) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(dashboardService.getPickup(propertyId));
    }
}
