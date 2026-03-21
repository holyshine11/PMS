package com.hola.integration.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hola.common.auth.entity.AdminUser;
import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.common.security.AccessControlService;
import com.hola.reservation.dto.request.PaymentAdjustmentRequest;
import com.hola.reservation.dto.request.PaymentProcessRequest;
import com.hola.reservation.dto.response.PaymentAdjustmentResponse;
import com.hola.reservation.dto.response.PaymentSummaryResponse;
import com.hola.reservation.dto.response.PaymentTransactionResponse;
import com.hola.reservation.service.ReservationPaymentService;
import com.hola.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 결제 API 통합 테스트
 * - ReservationPaymentApiController 엔드포인트 검증
 * - AccessControlService Mock으로 인가 바이패스
 * - ReservationPaymentService Mock으로 비즈니스 로직 제어
 */
@DisplayName("결제 API 통합 테스트")
class PaymentApiIntegrationTest extends BaseIntegrationTest {

    private static final String BASE_URL = "/api/v1/properties/{propertyId}/reservations/{reservationId}/payment";
    private static final Long PROPERTY_ID = 1L;
    private static final Long RESERVATION_ID = 100L;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccessControlService accessControlService;

    @MockBean
    private ReservationPaymentService paymentService;

    @BeforeEach
    void setUp() {
        AdminUser mockAdmin = AdminUser.builder()
                .loginId("admin")
                .role("SUPER_ADMIN")
                .userName("관리자")
                .password("encoded")
                .build();
        when(accessControlService.getCurrentUser()).thenReturn(mockAdmin);
    }

    // 결제 요약 응답 빌더 헬퍼
    private PaymentSummaryResponse buildSummary(String status, BigDecimal grandTotal,
                                                  BigDecimal paid, BigDecimal remaining) {
        return PaymentSummaryResponse.builder()
                .id(1L)
                .paymentStatus(status)
                .totalRoomAmount(new BigDecimal("300000"))
                .totalServiceAmount(BigDecimal.ZERO)
                .totalServiceChargeAmount(BigDecimal.ZERO)
                .totalAdjustmentAmount(BigDecimal.ZERO)
                .totalEarlyLateFee(BigDecimal.ZERO)
                .grandTotal(grandTotal)
                .totalPaidAmount(paid)
                .cancelFeeAmount(BigDecimal.ZERO)
                .refundAmount(BigDecimal.ZERO)
                .remainingAmount(remaining)
                .adjustments(List.of())
                .transactions(List.of())
                .build();
    }

    @Nested
    @DisplayName("결제 → 조정 → 전체 흐름")
    class PaymentFlow {

        @Test
        @DisplayName("결제 → 조정 → 재조회 전체 흐름")
        void fullPaymentFlow() throws Exception {
            // 1) 결제 처리
            PaymentSummaryResponse afterPayment = buildSummary(
                    "PARTIAL", new BigDecimal("300000"),
                    new BigDecimal("100000"), new BigDecimal("200000"));
            when(paymentService.processPayment(eq(PROPERTY_ID), eq(RESERVATION_ID), any(PaymentProcessRequest.class)))
                    .thenReturn(afterPayment);

            PaymentProcessRequest payReq = new PaymentProcessRequest("CARD", new BigDecimal("100000"), null);
            mockMvc.perform(post(BASE_URL + "/transactions", PROPERTY_ID, RESERVATION_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payReq)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.paymentStatus").value("PARTIAL"))
                    .andExpect(jsonPath("$.data.totalPaidAmount").value(100000));

            // 2) 조정 추가
            PaymentAdjustmentResponse adjResp = PaymentAdjustmentResponse.builder()
                    .id(1L)
                    .adjustmentSeq(1)
                    .adjustmentSign("+")
                    .supplyPrice(new BigDecimal("10000"))
                    .tax(new BigDecimal("1000"))
                    .totalAmount(new BigDecimal("11000"))
                    .comment("할증")
                    .createdAt(LocalDateTime.now())
                    .createdBy("admin")
                    .build();
            when(paymentService.addAdjustment(eq(PROPERTY_ID), eq(RESERVATION_ID), any(PaymentAdjustmentRequest.class)))
                    .thenReturn(adjResp);

            PaymentAdjustmentRequest adjReq = PaymentAdjustmentRequest.builder()
                    .adjustmentSign("+")
                    .supplyPrice(new BigDecimal("10000"))
                    .tax(new BigDecimal("1000"))
                    .totalAmount(new BigDecimal("11000"))
                    .comment("할증")
                    .build();
            mockMvc.perform(post(BASE_URL + "/adjustments", PROPERTY_ID, RESERVATION_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(adjReq)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.adjustmentSign").value("+"))
                    .andExpect(jsonPath("$.data.totalAmount").value(11000));

            // 3) 결제 정보 재조회
            PaymentSummaryResponse finalSummary = buildSummary(
                    "PARTIAL", new BigDecimal("311000"),
                    new BigDecimal("100000"), new BigDecimal("211000"));
            when(paymentService.getPaymentSummary(PROPERTY_ID, RESERVATION_ID)).thenReturn(finalSummary);

            mockMvc.perform(get(BASE_URL, PROPERTY_ID, RESERVATION_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.grandTotal").value(311000))
                    .andExpect(jsonPath("$.data.remainingAmount").value(211000));
        }
    }

    @Nested
    @DisplayName("결제 처리")
    class ProcessPayment {

        @Test
        @DisplayName("전액 결제 후 추가 결제 시 400")
        void processPayment_alreadyPaid_400() throws Exception {
            when(paymentService.processPayment(eq(PROPERTY_ID), eq(RESERVATION_ID), any(PaymentProcessRequest.class)))
                    .thenThrow(new HolaException(ErrorCode.RESERVATION_PAYMENT_ALREADY_COMPLETED));

            PaymentProcessRequest req = new PaymentProcessRequest("CARD", new BigDecimal("100000"), null);
            mockMvc.perform(post(BASE_URL + "/transactions", PROPERTY_ID, RESERVATION_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("전액 결제 → PAID 상태")
        void processPayment_fullAmount_paid() throws Exception {
            PaymentSummaryResponse resp = buildSummary(
                    "PAID", new BigDecimal("300000"),
                    new BigDecimal("300000"), BigDecimal.ZERO);
            when(paymentService.processPayment(eq(PROPERTY_ID), eq(RESERVATION_ID), any(PaymentProcessRequest.class)))
                    .thenReturn(resp);

            PaymentProcessRequest req = new PaymentProcessRequest("CASH", null, "전액 현금 결제");
            mockMvc.perform(post(BASE_URL + "/transactions", PROPERTY_ID, RESERVATION_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.paymentStatus").value("PAID"))
                    .andExpect(jsonPath("$.data.remainingAmount").value(0));
        }
    }

    @Nested
    @DisplayName("결제 조정")
    class Adjustment {

        @Test
        @DisplayName("할인 조정 → 잔액 감소")
        void addAdjustment_discount_reducesRemaining() throws Exception {
            PaymentAdjustmentResponse resp = PaymentAdjustmentResponse.builder()
                    .id(2L)
                    .adjustmentSeq(1)
                    .adjustmentSign("-")
                    .supplyPrice(new BigDecimal("20000"))
                    .tax(BigDecimal.ZERO)
                    .totalAmount(new BigDecimal("20000"))
                    .comment("회원 할인")
                    .createdAt(LocalDateTime.now())
                    .createdBy("admin")
                    .build();
            when(paymentService.addAdjustment(eq(PROPERTY_ID), eq(RESERVATION_ID), any(PaymentAdjustmentRequest.class)))
                    .thenReturn(resp);

            PaymentAdjustmentRequest req = PaymentAdjustmentRequest.builder()
                    .adjustmentSign("-")
                    .supplyPrice(new BigDecimal("20000"))
                    .tax(BigDecimal.ZERO)
                    .totalAmount(new BigDecimal("20000"))
                    .comment("회원 할인")
                    .build();
            mockMvc.perform(post(BASE_URL + "/adjustments", PROPERTY_ID, RESERVATION_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.adjustmentSign").value("-"))
                    .andExpect(jsonPath("$.data.comment").value("회원 할인"));
        }
    }

    @Nested
    @DisplayName("거래 이력")
    class TransactionHistory {

        @Test
        @DisplayName("결제 이력 시퀀스 순서 확인")
        void getPaymentSummary_transactionsOrdered() throws Exception {
            PaymentTransactionResponse tx1 = PaymentTransactionResponse.builder()
                    .transactionSeq(1)
                    .transactionType("PAYMENT")
                    .paymentMethod("CARD")
                    .amount(new BigDecimal("100000"))
                    .transactionStatus("COMPLETED")
                    .build();
            PaymentTransactionResponse tx2 = PaymentTransactionResponse.builder()
                    .transactionSeq(2)
                    .transactionType("PAYMENT")
                    .paymentMethod("CASH")
                    .amount(new BigDecimal("200000"))
                    .transactionStatus("COMPLETED")
                    .build();

            PaymentSummaryResponse resp = PaymentSummaryResponse.builder()
                    .id(1L)
                    .paymentStatus("PAID")
                    .totalRoomAmount(new BigDecimal("300000"))
                    .totalServiceAmount(BigDecimal.ZERO)
                    .totalServiceChargeAmount(BigDecimal.ZERO)
                    .totalAdjustmentAmount(BigDecimal.ZERO)
                    .totalEarlyLateFee(BigDecimal.ZERO)
                    .grandTotal(new BigDecimal("300000"))
                    .totalPaidAmount(new BigDecimal("300000"))
                    .cancelFeeAmount(BigDecimal.ZERO)
                    .refundAmount(BigDecimal.ZERO)
                    .remainingAmount(BigDecimal.ZERO)
                    .adjustments(List.of())
                    .transactions(List.of(tx1, tx2))
                    .build();
            when(paymentService.getPaymentSummary(PROPERTY_ID, RESERVATION_ID)).thenReturn(resp);

            mockMvc.perform(get(BASE_URL, PROPERTY_ID, RESERVATION_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.transactions", hasSize(2)))
                    .andExpect(jsonPath("$.data.transactions[0].transactionSeq").value(1))
                    .andExpect(jsonPath("$.data.transactions[1].transactionSeq").value(2))
                    .andExpect(jsonPath("$.data.transactions[0].paymentMethod").value("CARD"))
                    .andExpect(jsonPath("$.data.transactions[1].paymentMethod").value("CASH"));
        }

        @Test
        @DisplayName("취소 시 REFUND 거래 자동 생성 확인")
        void getPaymentSummary_cancelCreatesRefund() throws Exception {
            PaymentTransactionResponse payTx = PaymentTransactionResponse.builder()
                    .transactionSeq(1)
                    .transactionType("PAYMENT")
                    .paymentMethod("CARD")
                    .amount(new BigDecimal("300000"))
                    .transactionStatus("COMPLETED")
                    .build();
            PaymentTransactionResponse refundTx = PaymentTransactionResponse.builder()
                    .transactionSeq(2)
                    .transactionType("REFUND")
                    .paymentMethod("CARD")
                    .amount(new BigDecimal("270000"))
                    .transactionStatus("COMPLETED")
                    .build();

            PaymentSummaryResponse resp = PaymentSummaryResponse.builder()
                    .id(1L)
                    .paymentStatus("PAID")
                    .totalRoomAmount(new BigDecimal("300000"))
                    .totalServiceAmount(BigDecimal.ZERO)
                    .totalServiceChargeAmount(BigDecimal.ZERO)
                    .totalAdjustmentAmount(BigDecimal.ZERO)
                    .totalEarlyLateFee(BigDecimal.ZERO)
                    .grandTotal(new BigDecimal("300000"))
                    .totalPaidAmount(new BigDecimal("300000"))
                    .cancelFeeAmount(new BigDecimal("30000"))
                    .refundAmount(new BigDecimal("270000"))
                    .remainingAmount(BigDecimal.ZERO)
                    .adjustments(List.of())
                    .transactions(List.of(payTx, refundTx))
                    .build();
            when(paymentService.getPaymentSummary(PROPERTY_ID, RESERVATION_ID)).thenReturn(resp);

            mockMvc.perform(get(BASE_URL, PROPERTY_ID, RESERVATION_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.cancelFeeAmount").value(30000))
                    .andExpect(jsonPath("$.data.refundAmount").value(270000))
                    .andExpect(jsonPath("$.data.transactions[1].transactionType").value("REFUND"));
        }
    }
}
