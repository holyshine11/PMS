package com.hola.reservation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 결제 처리 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentProcessRequest {

    /** 결제 수단: CARD, CASH (필수) */
    @NotBlank(message = "결제 수단은 필수입니다")
    private String paymentMethod;

    /** 결제 금액 (null이면 잔액 전액) */
    @Positive(message = "결제 금액은 0보다 커야 합니다")
    private BigDecimal amount;

    /** 메모 (선택) */
    private String memo;

    /** Leg별 결제 추적용 SubReservation ID (선택 — null이면 마스터 레벨) */
    @Setter
    private Long subReservationId;

    /** 결제 채널: "VAN" 또는 null(수동결제) */
    private String paymentChannel;

    /** 워크스테이션 ID (VAN 결제 시) */
    private Long workstationId;

    /** VAN 결제 결과 (브라우저가 KPSP에서 받은 응답) */
    private VanResultPayload vanResult;

    /** KPSP 원본 응답 JSON (감사 추적용, vanResult와 별도로 원본 보관) */
    private String vanRawJson;

    /** 기존 3-arg 생성자 호환용 */
    public PaymentProcessRequest(String paymentMethod, BigDecimal amount, String memo) {
        this.paymentMethod = paymentMethod;
        this.amount = amount;
        this.memo = memo;
        this.subReservationId = null;
    }
}
