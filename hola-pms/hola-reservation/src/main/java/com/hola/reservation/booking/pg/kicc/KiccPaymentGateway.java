package com.hola.reservation.booking.pg.kicc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.reservation.booking.gateway.*;
import com.hola.reservation.booking.pg.kicc.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * KICC 이지페이 결제 게이트웨이 구현체
 * - 3단계 플로우: 거래등록 → 결제창 → 승인
 * - 테스트 환경 제외, 로컬/운영에서 Primary Bean
 */
@Slf4j
@Component
@Primary
@Profile("!test")
@RequiredArgsConstructor
public class KiccPaymentGateway implements PaymentGateway {

    private static final String GATEWAY_ID = "KICC";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final KiccApiClient apiClient;
    private final KiccProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * 레거시 동기식 승인 — KICC는 3단계 플로우만 지원
     * BookingServiceImpl에서 호출하는 기존 메서드 — Phase 2에서 리팩토링 예정
     */
    @Override
    public PaymentResult authorize(PaymentRequest request) {
        // KICC는 3단계 비동기 플로우이므로 동기식 authorize 미지원
        // Phase 2에서 BookingServiceImpl이 registerTransaction → approveAfterAuth로 전환
        throw new UnsupportedOperationException(
                "KICC는 동기식 authorize를 지원하지 않습니다. registerTransaction() → approveAfterAuth() 사용");
    }

    /**
     * 레거시 취소 — cancelPayment(CancelPaymentRequest) 사용 권장
     */
    @Override
    public PaymentResult cancel(String approvalNo) {
        log.warn("[KICC] 레거시 cancel() 호출 — pgCno 기반 cancelPayment() 사용 권장");
        return PaymentResult.failure(GATEWAY_ID, "UNSUPPORTED", "cancelPayment(CancelPaymentRequest) 사용 필요");
    }

    @Override
    public String getGatewayId() {
        return GATEWAY_ID;
    }

    // === PG 3단계 플로우 ===

    /**
     * Step 1: 거래등록 — KICC 결제창 URL 반환
     */
    @Override
    public RegisterResult registerTransaction(RegisterRequest request) {
        String payMethodTypeCode = "BILLING".equals(request.getPaymentMethod()) ? "81" : "11";
        long amount = "BILLING".equals(request.getPaymentMethod()) ? 0L : request.getAmount().longValue();

        KiccRegisterRequest kiccRequest = KiccRegisterRequest.builder()
                .mallId(properties.getMallId())
                .shopOrderNo(request.getOrderId())
                .amount(amount)
                .payMethodTypeCode(payMethodTypeCode)
                .currency("00")
                .returnUrl(buildReturnUrl(payMethodTypeCode))
                .deviceTypeCode(request.getDeviceType())
                .clientTypeCode("00")
                .orderInfo(KiccRegisterRequest.OrderInfo.builder()
                        .goodsName(sanitizeGoodsName(request.getGoodsName()))
                        .customerInfo(KiccRegisterRequest.OrderInfo.CustomerInfo.builder()
                                .customerName(request.getCustomerName())
                                .customerContactNo(request.getCustomerPhone())
                                .customerEmail(request.getCustomerEmail())
                                .build())
                        .build())
                .payMethodInfo("81".equals(payMethodTypeCode)
                        ? KiccRegisterRequest.PayMethodInfo.builder()
                                .billKeyMethodInfo(KiccRegisterRequest.PayMethodInfo.BillKeyMethodInfo.builder()
                                        .certType(properties.getBillingCertType())
                                        .build())
                                .build()
                        : null)
                .build();

        KiccRegisterResponse response = apiClient.registerTransaction(kiccRequest);
        return RegisterResult.success(response.getAuthPageUrl(), request.getOrderId());
    }

    /**
     * Step 3: 인증 후 결제승인 — authorizationId로 최종 승인
     */
    @Override
    public PaymentResult approveAfterAuth(ApproveAfterAuthRequest request) {
        String shopTransactionId = UUID.randomUUID().toString();
        String today = LocalDate.now().format(DATE_FMT);

        KiccApprovalRequest kiccRequest = KiccApprovalRequest.builder()
                .mallId(properties.getMallId())
                .shopTransactionId(shopTransactionId)
                .authorizationId(request.getAuthorizationId())
                .shopOrderNo(request.getOrderId())
                .approvalReqDate(today)
                .build();

        KiccApprovalResponse response = apiClient.approvePayment(kiccRequest);

        // 금액 검증: 응답금액과 요청금액 비교
        if (request.getExpectedAmount() != null) {
            BigDecimal approvedAmount = BigDecimal.valueOf(response.getAmount());
            if (request.getExpectedAmount().compareTo(approvedAmount) != 0) {
                log.error("[KICC] 금액 불일치 - 요청: {}, 승인: {}. 즉시 취소 진행",
                        request.getExpectedAmount(), approvedAmount);
                // 금액 불일치 시 즉시 취소
                cancelByPgCno(response.getPgCno(), shopTransactionId);
                throw new HolaException(ErrorCode.PG_AMOUNT_MISMATCH);
            }
        }

        // HMAC 무결성 검증
        if (response.getMsgAuthValue() != null) {
            boolean valid = KiccHmacUtils.verifyApproval(
                    response.getMsgAuthValue(),
                    properties.getSecretKey(),
                    response.getPgCno(),
                    response.getAmount(),
                    response.getTransactionDate());
            if (!valid) {
                // 테스트 환경(T로 시작하는 상점ID)에서는 시크릿키 불일치 가능 → 경고만 로그
                if (properties.getMallId().startsWith("T")) {
                    log.warn("[KICC] 테스트 환경 HMAC 검증 실패 (무시) - pgCno: {}, mallId: {}",
                            response.getPgCno(), properties.getMallId());
                } else {
                    log.error("[KICC] 메시지 인증값 검증 실패 - pgCno: {}", response.getPgCno());
                    cancelByPgCno(response.getPgCno(), shopTransactionId);
                    throw new HolaException(ErrorCode.PG_AUTH_VERIFY_FAILED);
                }
            }
        }

        return buildPaymentResult(response, shopTransactionId);
    }

    /**
     * PG 결제 취소 (전체/부분)
     */
    @Override
    public PaymentResult cancelPayment(CancelPaymentRequest request) {
        String shopTransactionId = UUID.randomUUID().toString();
        String today = LocalDate.now().format(DATE_FMT);

        // 취소 유형 결정
        String reviseTypeCode = request.isPartialCancel() ? "32" : "40";

        // HMAC 생성
        String msgAuthValue = KiccHmacUtils.generateForRevise(
                properties.getSecretKey(), request.getPgCno(), shopTransactionId);

        KiccReviseRequest kiccRequest = KiccReviseRequest.builder()
                .mallId(properties.getMallId())
                .shopTransactionId(shopTransactionId)
                .pgCno(request.getPgCno())
                .reviseTypeCode(reviseTypeCode)
                .cancelReqDate(today)
                .msgAuthValue(msgAuthValue)
                .reviseMessage(request.getReason())
                .amount(request.isPartialCancel() ? request.getCancelAmount().longValue() : null)
                .remainAmount(request.isPartialCancel() ? request.getRemainAmount().longValue() : null)
                .build();

        KiccReviseResponse response = apiClient.revisePayment(kiccRequest);

        return PaymentResult.builder()
                .success(true)
                .gatewayId(GATEWAY_ID)
                .pgProvider(GATEWAY_ID)
                .pgCno(response.getCancelPgCno())
                .amount(response.getCancelAmount() != null ? BigDecimal.valueOf(response.getCancelAmount()) : null)
                .approvalNo(response.getReviseInfo() != null ? response.getReviseInfo().getApprovalNo() : null)
                .pgStatusCode(response.getStatusCode())
                .pgTransactionId(shopTransactionId)
                .processedAt(java.time.LocalDateTime.now())
                .build();
    }

    // === 내부 헬퍼 ===

    /** returnUrl 생성 */
    private String buildReturnUrl(String payMethodTypeCode) {
        String path = "81".equals(payMethodTypeCode)
                ? "/api/v1/booking/payment/billkey/return"
                : "/api/v1/booking/payment/return";
        return properties.getReturnBaseUrl() + path;
    }

    /** 상품명 50Byte 제한 + 특수문자 제거 */
    private String sanitizeGoodsName(String name) {
        if (name == null) return "숙박요금";
        // KICC 금지 특수문자 제거
        String sanitized = name.replaceAll("['\";|<>\\\\]", "");
        // 50Byte 제한 (한글 3Byte 기준)
        byte[] bytes = sanitized.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (bytes.length > 50) {
            sanitized = new String(bytes, 0, 50, java.nio.charset.StandardCharsets.UTF_8);
        }
        return sanitized;
    }

    /** 금액 불일치/HMAC 실패 시 즉시 취소 */
    private void cancelByPgCno(String pgCno, String shopTransactionId) {
        try {
            String msgAuthValue = KiccHmacUtils.generateForRevise(
                    properties.getSecretKey(), pgCno, shopTransactionId);
            KiccReviseRequest cancelRequest = KiccReviseRequest.builder()
                    .mallId(properties.getMallId())
                    .shopTransactionId(UUID.randomUUID().toString())
                    .pgCno(pgCno)
                    .reviseTypeCode("40")
                    .cancelReqDate(LocalDate.now().format(DATE_FMT))
                    .msgAuthValue(msgAuthValue)
                    .reviseMessage("금액 불일치/인증 실패 자동 취소")
                    .build();
            apiClient.revisePayment(cancelRequest);
            log.info("[KICC] 자동 취소 성공 - pgCno: {}", pgCno);
        } catch (Exception e) {
            log.error("[KICC] 자동 취소 실패 - pgCno: {}, error: {}", pgCno, e.getMessage());
        }
    }

    /** KICC 승인 응답 → PaymentResult 변환 */
    private PaymentResult buildPaymentResult(KiccApprovalResponse response, String shopTransactionId) {
        String rawResponse = null;
        try {
            rawResponse = objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            log.warn("[KICC] 응답 직렬화 실패", e);
        }

        PaymentResult.PaymentResultBuilder builder = PaymentResult.builder()
                .success(true)
                .gatewayId(GATEWAY_ID)
                .pgProvider(GATEWAY_ID)
                .pgCno(response.getPgCno())
                .amount(BigDecimal.valueOf(response.getAmount()))
                .pgStatusCode(response.getStatusCode())
                .pgApprovalDate(response.getTransactionDate())
                .pgTransactionId(shopTransactionId)
                .pgRawResponse(rawResponse)
                .processedAt(java.time.LocalDateTime.now());

        // 카드 정보
        if (response.getPaymentInfo() != null) {
            builder.approvalNo(response.getPaymentInfo().getApprovalNo())
                    .pgApprovalNo(response.getPaymentInfo().getApprovalNo());

            if (response.getPaymentInfo().getCardInfo() != null) {
                KiccCardInfo card = response.getPaymentInfo().getCardInfo();
                builder.pgCardNo(card.getCardMaskNo() != null ? card.getCardMaskNo() : card.getCardNo())
                        .pgIssuerName(card.getIssuerNm())
                        .pgAcquirerName(card.getAcquirerNm())
                        .installmentMonth(card.getInstallmentMonth())
                        .pgCardType(card.getCardType());
            }
        }

        return builder.build();
    }
}
