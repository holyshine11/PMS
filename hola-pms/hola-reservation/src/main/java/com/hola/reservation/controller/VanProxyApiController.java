package com.hola.reservation.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.common.security.AccessControlService;
import com.hola.hotel.entity.Workstation;
import com.hola.hotel.service.WorkstationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * VAN KPSP 프록시 API
 * 브라우저 → PMS 백엔드 → KPSP 프록시 (CORS 우회)
 */
@Slf4j
@Tag(name = "VAN 프록시", description = "KPSP 단말기 프록시 API (CORS 우회)")
@RestController
@RequestMapping("/api/v1/properties/{propertyId}/van/proxy")
@RequiredArgsConstructor
public class VanProxyApiController {

    private final WorkstationService workstationService;
    private final AccessControlService accessControlService;
    private final RestTemplate vanRestTemplate;

    /** 카드 결제 프록시 */
    @Operation(summary = "VAN 카드결제 (KPSP 프록시)")
    @PostMapping("/sales/card")
    public ResponseEntity<String> proxyCardSale(@PathVariable Long propertyId,
                                                 @RequestParam Long workstationId,
                                                 @RequestBody String body) {
        return proxyToKpsp(propertyId, workstationId, "/sales/card", body);
    }

    /** 현금 결제 프록시 */
    @Operation(summary = "VAN 현금결제 (KPSP 프록시)")
    @PostMapping("/sales/cash")
    public ResponseEntity<String> proxyCashSale(@PathVariable Long propertyId,
                                                 @RequestParam Long workstationId,
                                                 @RequestBody String body) {
        return proxyToKpsp(propertyId, workstationId, "/sales/cash", body);
    }

    /** 카드 취소 프록시 */
    @Operation(summary = "VAN 카드취소 (KPSP 프록시)")
    @PostMapping("/refund/card")
    public ResponseEntity<String> proxyCardRefund(@PathVariable Long propertyId,
                                                    @RequestParam Long workstationId,
                                                    @RequestBody String body) {
        return proxyToKpsp(propertyId, workstationId, "/refund/device/manual/card", body);
    }

    /** 현금 취소 프록시 */
    @Operation(summary = "VAN 현금취소 (KPSP 프록시)")
    @PostMapping("/refund/cash")
    public ResponseEntity<String> proxyCashRefund(@PathVariable Long propertyId,
                                                    @RequestParam Long workstationId,
                                                    @RequestBody String body) {
        return proxyToKpsp(propertyId, workstationId, "/refund/device/manual/cash", body);
    }

    private ResponseEntity<String> proxyToKpsp(Long propertyId, Long workstationId,
                                                 String kpspPath, String body) {
        accessControlService.validatePropertyAccess(propertyId);
        Workstation ws = workstationService.findById(workstationId);

        String kpspUrl = "http://" + ws.getKpspHost() + ":" + ws.getKpspPort() + kpspPath;
        log.info("KPSP 프록시: {} → {}", kpspPath, kpspUrl);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = vanRestTemplate.exchange(
                    kpspUrl, HttpMethod.POST, request, String.class);

            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            log.error("KPSP 프록시 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("{\"success\":false,\"respCode\":\"9999\",\"respMessage\":\"단말기에 연결할 수 없습니다: " + e.getMessage() + "\"}");
        }
    }
}
