package com.hola.hotel.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.common.security.AccessControlService;
import com.hola.hotel.entity.Workstation;
import com.hola.hotel.service.WorkstationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 워크스테이션 REST API 컨트롤러
 */
@Tag(name = "워크스테이션", description = "VAN 카드 단말기 워크스테이션 조회 API")
@RestController
@RequestMapping("/api/v1/properties/{propertyId}/workstations")
@RequiredArgsConstructor
public class WorkstationApiController {

    private final WorkstationService workstationService;
    private final AccessControlService accessControlService;

    @Operation(summary = "활성 워크스테이션 목록 조회")
    @GetMapping
    public HolaResponse<List<WorkstationResponse>> getWorkstations(@PathVariable Long propertyId) {
        accessControlService.validatePropertyAccess(propertyId);
        List<Workstation> workstations = workstationService.findActiveByProperty(propertyId);
        List<WorkstationResponse> responses = workstations.stream()
                .map(WorkstationResponse::from)
                .toList();
        return HolaResponse.success(responses);
    }

    /**
     * 워크스테이션 응답 DTO (내부 record)
     */
    public record WorkstationResponse(
            Long id, String wsNo, String wsName,
            String kpspHost, Integer kpspPort, String terminalId, String status
    ) {
        static WorkstationResponse from(Workstation ws) {
            return new WorkstationResponse(
                    ws.getId(), ws.getWsNo(), ws.getWsName(),
                    ws.getKpspHost(), ws.getKpspPort(), ws.getTerminalId(), ws.getStatus()
            );
        }
    }
}
