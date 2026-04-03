package com.hola.reservation.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.common.security.AccessControlService;
import com.hola.reservation.dto.request.PaymentAdjustmentRequest;
import com.hola.reservation.dto.request.PaymentProcessRequest;
import com.hola.reservation.dto.request.VanResultPayload;
import com.hola.reservation.dto.response.PaymentAdjustmentResponse;
import com.hola.reservation.dto.response.PaymentSummaryResponse;
import com.hola.reservation.dto.response.VanCancelInfoResponse;
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

    /** PG 환불 재시도 (PG_REFUND_FAILED 상태 거래의 PG 취소 재처리) */
    @Operation(summary = "PG 환불 재시도", description = "PG_REFUND_FAILED 상태 거래의 PG 환불 재시도")
    @PostMapping("/transactions/{transactionId}/retry-refund")
    public HolaResponse<PaymentSummaryResponse> retryPgRefund(@PathVariable Long propertyId,
                                                                @PathVariable Long reservationId,
                                                                @PathVariable Long transactionId) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(paymentService.retryPgRefund(propertyId, reservationId, transactionId));
    }

    /** 개별 PG 결제 건 취소 (예약 유지, 해당 결제만 KICC 취소) */
    @Operation(summary = "PG 결제 취소", description = "개별 PG 결제 건을 KICC를 통해 취소. 예약은 유지됨")
    @PostMapping("/transactions/{transactionId}/pg-cancel")
    public HolaResponse<PaymentSummaryResponse> cancelPgTransaction(@PathVariable Long propertyId,
                                                                      @PathVariable Long reservationId,
                                                                      @PathVariable Long transactionId) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(paymentService.cancelPgTransaction(propertyId, reservationId, transactionId));
    }

    /** VAN 취소에 필요한 원거래 정보 조회 */
    @Operation(summary = "VAN 취소 정보 조회", description = "VAN 취소에 필요한 원거래 authCode, rrn, amount 조회")
    @GetMapping("/transactions/{transactionId}/van-cancel-info")
    public HolaResponse<VanCancelInfoResponse> getVanCancelInfo(@PathVariable Long propertyId,
                                                                  @PathVariable Long reservationId,
                                                                  @PathVariable Long transactionId) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(paymentService.getVanCancelInfo(propertyId, reservationId, transactionId));
    }

    /** VAN 취소 결과 저장 */
    @Operation(summary = "VAN 취소 결과 저장", description = "브라우저가 KPSP 취소 후 결과를 백엔드에 전달")
    @PostMapping("/transactions/{transactionId}/van-cancel")
    public HolaResponse<PaymentSummaryResponse> processVanCancel(@PathVariable Long propertyId,
                                                                   @PathVariable Long reservationId,
                                                                   @PathVariable Long transactionId,
                                                                   @RequestBody VanResultPayload cancelResult) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(paymentService.processVanCancel(propertyId, reservationId, transactionId, cancelResult));
    }

    /** VAN 취소 수동 확인 (KPSP 취소 실패 시 관리자가 단말기 확인 후 수동 처리) */
    @Operation(summary = "VAN 취소 수동 확인", description = "KPSP 취소 실패 시 관리자가 단말기 영수증 확인 후 수동으로 REFUND 기록")
    @PostMapping("/transactions/{transactionId}/van-cancel-manual")
    public HolaResponse<PaymentSummaryResponse> processVanCancelManual(@PathVariable Long propertyId,
                                                                        @PathVariable Long reservationId,
                                                                        @PathVariable Long transactionId,
                                                                        @RequestBody VanResultPayload manualPayload) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(paymentService.processVanCancelManual(propertyId, reservationId, transactionId, manualPayload));
    }

    /** VAN 시퀀스 번호 발급 */
    @Operation(summary = "VAN 시퀀스 번호 발급", description = "KPSP 호출 전 고유 시퀀스 번호 생성")
    @GetMapping("/next-van-sequence")
    public HolaResponse<String> getNextVanSequence(@PathVariable Long propertyId,
                                                     @PathVariable Long reservationId,
                                                     @RequestParam Long workstationId) {
        accessControlService.validatePropertyAccess(propertyId);
        return HolaResponse.success(paymentService.generateVanSequenceNo(workstationId));
    }
}
