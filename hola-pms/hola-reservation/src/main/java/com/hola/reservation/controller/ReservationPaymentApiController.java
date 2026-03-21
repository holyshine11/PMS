package com.hola.reservation.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.common.security.AccessControlService;
import com.hola.reservation.dto.request.PaymentAdjustmentRequest;
import com.hola.reservation.dto.request.PaymentProcessRequest;
import com.hola.reservation.dto.response.PaymentAdjustmentResponse;
import com.hola.reservation.dto.response.PaymentSummaryResponse;
import com.hola.reservation.service.ReservationPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * 예약 결제 REST API 컨트롤러
 */
@Tag(name = "예약 결제", description = "예약 결제 조회, 결제 처리, 금액 조정 API")
@RestController
@RequestMapping("/api/v1/properties/{propertyId}/reservations/{reservationId}/payment")
@RequiredArgsConstructor
public class ReservationPaymentApiController {

    private final ReservationPaymentService paymentService;
    private final AccessControlService accessControlService;

    /** 결제 정보 조회 */
    @Operation(summary = "결제 정보 조회", description = "예약 결제 요약 (총액, 결제액, 잔액)")
    @GetMapping
    public HolaResponse<PaymentSummaryResponse> getPaymentSummary(@PathVariable Long propertyId,
                                                                    @PathVariable Long reservationId) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(paymentService.getPaymentSummary(propertyId, reservationId));
    }

    /** 결제 처리 (카드/현금, 부분결제 지원) */
    @Operation(summary = "결제 처리", description = "카드/현금 결제 처리 (부분결제 지원)")
    @PostMapping("/transactions")
    @ResponseStatus(HttpStatus.CREATED)
    public HolaResponse<PaymentSummaryResponse> processPayment(@PathVariable Long propertyId,
                                                                 @PathVariable Long reservationId,
                                                                 @Valid @RequestBody PaymentProcessRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(paymentService.processPayment(propertyId, reservationId, request));
    }

    /** 금액 조정 추가 */
    @Operation(summary = "금액 조정 추가", description = "할인/추가요금 등 결제 금액 조정")
    @PostMapping("/adjustments")
    @ResponseStatus(HttpStatus.CREATED)
    public HolaResponse<PaymentAdjustmentResponse> addAdjustment(@PathVariable Long propertyId,
                                                                   @PathVariable Long reservationId,
                                                                   @Valid @RequestBody PaymentAdjustmentRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(paymentService.addAdjustment(propertyId, reservationId, request));
    }
}
