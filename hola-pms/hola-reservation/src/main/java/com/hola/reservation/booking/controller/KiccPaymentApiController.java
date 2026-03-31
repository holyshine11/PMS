package com.hola.reservation.booking.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.reservation.booking.dto.request.BookingCreateRequest;
import com.hola.reservation.booking.dto.response.BookingConfirmationResponse;
import com.hola.reservation.booking.dto.response.BookingValidationResult;
import com.hola.reservation.booking.gateway.ApproveAfterAuthRequest;
import com.hola.reservation.booking.gateway.PaymentGateway;
import com.hola.reservation.booking.gateway.PaymentResult;
import com.hola.reservation.booking.gateway.RegisterRequest;
import com.hola.reservation.booking.gateway.RegisterResult;
import com.hola.reservation.booking.gateway.CancelPaymentRequest;
import com.hola.reservation.booking.pg.kicc.dto.KiccBookingTempData;
import com.hola.reservation.booking.service.BookingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * KICC 결제 플로우 API 컨트롤러
 * - /api/v1/booking/payment/register : 거래등록 + Redis 임시 저장
 * - /api/v1/booking/payment/return   : KICC returnUrl 콜백 (승인 + 예약 생성)
 * - /api/v1/booking/payment/result   : 결제 결과 조회 (폴링용)
 *
 * 보안: BookingSecurityConfig (Order 0) 에 의해 /api/v1/booking/** → STATELESS + permitAll
 */
@Slf4j
@Controller
@RequestMapping("/api/v1/booking/payment")
@RequiredArgsConstructor
public class KiccPaymentApiController {

    private static final String REDIS_KEY_PREFIX = "kicc:booking:";
    private static final Duration REDIS_TTL = Duration.ofMinutes(30);
    private static final String RESULT_KEY_PREFIX = "kicc:result:";
    private static final Duration RESULT_TTL = Duration.ofMinutes(10);

    private final BookingService bookingService;
    private final PaymentGateway paymentGateway;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 거래등록 — KICC 결제창 URL 반환
     */
    @PostMapping("/register")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> register(
            @RequestParam String propertyCode,
            @Valid @RequestBody BookingCreateRequest request,
            HttpServletRequest httpRequest) {

        String clientIp = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");

        // 1. 부킹 검증 (Steps 1-4)
        BookingValidationResult validation = bookingService.validateBookingRequest(propertyCode, request);

        // 2. 주문번호 생성
        String shopOrderNo = "HOLA-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();

        // 3. Redis 임시 저장
        KiccBookingTempData tempData = KiccBookingTempData.builder()
                .propertyCode(propertyCode)
                .bookingRequest(request)
                .grandTotal(validation.getGrandTotal())
                .shopOrderNo(shopOrderNo)
                .clientIp(clientIp)
                .userAgent(userAgent)
                .build();

        try {
            String json = objectMapper.writeValueAsString(tempData);
            redisTemplate.opsForValue().set(REDIS_KEY_PREFIX + shopOrderNo, json, REDIS_TTL);
        } catch (JsonProcessingException e) {
            log.error("[KICC] 임시 데이터 직렬화 실패", e);
            throw new HolaException(ErrorCode.PG_REGISTER_FAILED, "결제 데이터 저장 실패");
        }

        // 4. 상품명 생성
        String goodsName = buildGoodsName(request);

        // 5. KICC 거래등록
        String deviceType = isMobile(userAgent) ? "mobile" : "pc";
        RegisterResult result = paymentGateway.registerTransaction(RegisterRequest.builder()
                .orderId(shopOrderNo)
                .amount(validation.getGrandTotal())
                .goodsName(goodsName)
                .customerName(request.getGuest().getGuestNameKo())
                .customerPhone(request.getGuest().getPhoneNumber())
                .customerEmail(request.getGuest().getEmail())
                .paymentMethod("CARD")
                .deviceType(deviceType)
                .build());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("authPageUrl", result.getAuthPageUrl());
        response.put("shopOrderNo", shopOrderNo);
        response.put("amount", validation.getGrandTotal());

        log.info("[KICC] 거래등록 완료 - shopOrderNo: {}, amount: {}", shopOrderNo, validation.getGrandTotal());
        return ResponseEntity.ok(response);
    }

    /**
     * KICC returnUrl 콜백 — 결제 인증 결과 수신 + 승인 + 예약 생성
     * KICC가 브라우저를 통해 POST로 호출 (결제창 팝업 또는 리다이렉트 내)
     */
    @PostMapping("/return")
    public String paymentReturn(
            @RequestParam String resCd,
            @RequestParam String shopOrderNo,
            @RequestParam(required = false) String authorizationId,
            @RequestParam(required = false) String resMsg,
            Model model) {

        log.info("[KICC] returnUrl 콜백 - resCd: {}, shopOrderNo: {}, authorizationId: {}",
                resCd, shopOrderNo, authorizationId);

        // 1. KICC 인증 실패 체크
        if (!"0000".equals(resCd)) {
            log.warn("[KICC] 결제 인증 실패 - resCd: {}, resMsg: {}", resCd, resMsg);
            model.addAttribute("success", false);
            model.addAttribute("errorMessage", resMsg != null ? resMsg : "결제 인증에 실패했습니다.");
            model.addAttribute("shopOrderNo", shopOrderNo);
            cleanupRedis(shopOrderNo);
            return "booking/payment-return";
        }

        // 2. Redis에서 임시 데이터 복원
        KiccBookingTempData tempData = restoreTempData(shopOrderNo);
        if (tempData == null) {
            model.addAttribute("success", false);
            model.addAttribute("errorMessage", "결제 세션이 만료되었습니다. 다시 시도해주세요.");
            return "booking/payment-return";
        }

        PaymentResult paymentResult = null;
        try {
            // 3. KICC 결제 승인
            paymentResult = paymentGateway.approveAfterAuth(ApproveAfterAuthRequest.builder()
                    .authorizationId(authorizationId)
                    .orderId(shopOrderNo)
                    .expectedAmount(tempData.getGrandTotal())
                    .build());

            // 4. 예약 생성
            BookingConfirmationResponse confirmation = bookingService.createBookingWithPaymentResult(
                    tempData.getPropertyCode(),
                    tempData.getBookingRequest(),
                    paymentResult,
                    tempData.getClientIp(),
                    tempData.getUserAgent());

            // 5. 결과 Redis 저장 (프론트엔드 폴링용 — 전체 confirmation 데이터 포함)
            saveResult(shopOrderNo, true, confirmation);

            // 6. 결과 페이지 반환
            model.addAttribute("success", true);
            model.addAttribute("confirmationNo", confirmation.getConfirmationNo());
            model.addAttribute("propertyCode", tempData.getPropertyCode());
            model.addAttribute("shopOrderNo", shopOrderNo);

            log.info("[KICC] 결제+예약 완료 - shopOrderNo: {}, confirmationNo: {}",
                    shopOrderNo, confirmation.getConfirmationNo());

        } catch (Exception e) {
            log.error("[KICC] 결제승인 또는 예약 생성 실패 - shopOrderNo: {}", shopOrderNo, e);

            // 결제 승인은 성공했으나 예약 생성 실패 시 → PG 자동 취소 (환불)
            if (paymentResult != null && paymentResult.isSuccess() && paymentResult.getPgCno() != null) {
                log.warn("[KICC] 결제 승인 후 예약 실패 → 자동 환불 시도 - pgCno: {}", paymentResult.getPgCno());
                try {
                    paymentGateway.cancelPayment(CancelPaymentRequest.builder()
                            .pgCno(paymentResult.getPgCno())
                            .cancelType("FULL")
                            .reason("예약 생성 실패에 따른 자동 환불")
                            .build());
                    log.info("[KICC] 자동 환불 성공 - pgCno: {}", paymentResult.getPgCno());
                } catch (Exception cancelEx) {
                    log.error("[KICC] *** 자동 환불 실패 — 수동 환불 필요 *** pgCno: {}, shopOrderNo: {}, amount: {}",
                            paymentResult.getPgCno(), shopOrderNo, paymentResult.getAmount(), cancelEx);
                }
            }

            model.addAttribute("success", false);
            model.addAttribute("errorMessage", "결제 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
            saveResult(shopOrderNo, false, null);
        } finally {
            cleanupRedis(shopOrderNo);
        }

        return "booking/payment-return";
    }

    /**
     * 결제 결과 조회 (폴링용 — 모바일 리다이렉트 후 결과 확인)
     */
    @GetMapping("/result")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getResult(@RequestParam String shopOrderNo) {
        String json = redisTemplate.opsForValue().get(RESULT_KEY_PREFIX + shopOrderNo);
        if (json == null) {
            Map<String, Object> pending = Map.of("status", "PENDING");
            return ResponseEntity.ok(pending);
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = objectMapper.readValue(json, Map.class);
            return ResponseEntity.ok(result);
        } catch (JsonProcessingException e) {
            return ResponseEntity.ok(Map.of("status", "ERROR"));
        }
    }

    // === 내부 헬퍼 ===

    private KiccBookingTempData restoreTempData(String shopOrderNo) {
        String json = redisTemplate.opsForValue().get(REDIS_KEY_PREFIX + shopOrderNo);
        if (json == null) {
            log.warn("[KICC] Redis 임시 데이터 없음 (만료?) - shopOrderNo: {}", shopOrderNo);
            return null;
        }
        try {
            return objectMapper.readValue(json, KiccBookingTempData.class);
        } catch (JsonProcessingException e) {
            log.error("[KICC] 임시 데이터 역직렬화 실패", e);
            return null;
        }
    }

    private void cleanupRedis(String shopOrderNo) {
        redisTemplate.delete(REDIS_KEY_PREFIX + shopOrderNo);
    }

    private void saveResult(String shopOrderNo, boolean success, BookingConfirmationResponse confirmation) {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("status", success ? "SUCCESS" : "FAILED");
            result.put("success", success);
            if (confirmation != null) {
                result.put("confirmationNo", confirmation.getConfirmationNo());
                // 전체 confirmation 데이터 포함 (프론트엔드 sessionStorage 저장용)
                result.put("confirmation", confirmation);
            }
            String json = objectMapper.writeValueAsString(result);
            redisTemplate.opsForValue().set(RESULT_KEY_PREFIX + shopOrderNo, json, RESULT_TTL);
        } catch (JsonProcessingException e) {
            log.warn("[KICC] 결과 저장 실패", e);
        }
    }

    private String buildGoodsName(BookingCreateRequest request) {
        if (request.getRooms() == null || request.getRooms().isEmpty()) {
            return "호텔 숙박";
        }
        int roomCount = request.getRooms().size();
        return roomCount > 1 ? "호텔 숙박 " + roomCount + "건" : "호텔 숙박";
    }

    private boolean isMobile(String userAgent) {
        if (userAgent == null) return false;
        String ua = userAgent.toLowerCase();
        return ua.contains("mobile") || ua.contains("android") || ua.contains("iphone");
    }
}
