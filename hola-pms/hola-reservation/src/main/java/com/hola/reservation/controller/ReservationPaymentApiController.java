package com.hola.reservation.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.common.security.AccessControlService;
import com.hola.reservation.dto.request.PaymentAdjustmentRequest;
import com.hola.reservation.dto.request.PaymentProcessRequest;
import com.hola.reservation.dto.response.PaymentAdjustmentResponse;
import com.hola.reservation.dto.response.PaymentSummaryResponse;
import com.hola.reservation.service.ReservationPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * 예약 결제 REST API 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/properties/{propertyId}/reservations/{reservationId}/payment")
@RequiredArgsConstructor
public class ReservationPaymentApiController {

    private final ReservationPaymentService paymentService;
    private final AccessControlService accessControlService;

    /** 결제 정보 조회 */
    @GetMapping
    public HolaResponse<PaymentSummaryResponse> getPaymentSummary(@PathVariable Long propertyId,
                                                                    @PathVariable Long reservationId) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(paymentService.getPaymentSummary(reservationId));
    }

    /** 결제 처리 (카드/현금, 부분결제 지원) */
    @PostMapping("/transactions")
    @ResponseStatus(HttpStatus.CREATED)
    public HolaResponse<PaymentSummaryResponse> processPayment(@PathVariable Long propertyId,
                                                                 @PathVariable Long reservationId,
                                                                 @RequestBody PaymentProcessRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(paymentService.processPayment(reservationId, request));
    }

    /** 금액 조정 추가 */
    @PostMapping("/adjustments")
    @ResponseStatus(HttpStatus.CREATED)
    public HolaResponse<PaymentAdjustmentResponse> addAdjustment(@PathVariable Long propertyId,
                                                                   @PathVariable Long reservationId,
                                                                   @RequestBody PaymentAdjustmentRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(paymentService.addAdjustment(reservationId, request));
    }
}
