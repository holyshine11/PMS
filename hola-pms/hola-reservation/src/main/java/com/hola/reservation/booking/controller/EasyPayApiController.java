package com.hola.reservation.booking.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.common.dto.HolaResponse;
import com.hola.reservation.booking.dto.request.BookingCreateRequest;
import com.hola.reservation.booking.dto.response.BookingConfirmationResponse;
import com.hola.reservation.booking.dto.response.BookingValidationResult;
import com.hola.reservation.booking.dto.response.EasyPayCardResponse;
import com.hola.reservation.booking.gateway.PaymentResult;
import com.hola.reservation.booking.gateway.RegisterRequest;
import com.hola.reservation.booking.gateway.RegisterResult;
import com.hola.reservation.booking.gateway.ApproveAfterAuthRequest;
import com.hola.reservation.booking.gateway.PaymentGateway;
import com.hola.reservation.booking.entity.EasyPayCard;
import com.hola.reservation.booking.pg.kicc.KiccApiClient;
import com.hola.reservation.booking.pg.kicc.KiccProperties;
import org.springframework.beans.factory.annotation.Autowired;
import com.hola.reservation.booking.pg.kicc.dto.KiccBatchApprovalRequest;
import com.hola.reservation.booking.pg.kicc.dto.KiccApprovalResponse;
import com.hola.reservation.booking.repository.EasyPayCardRepository;
import com.hola.reservation.booking.service.BookingService;
import com.hola.reservation.booking.service.EasyPayCardService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 간편결제(빌키) API 컨트롤러
 * - /api/v1/booking/easy-pay/** : 카드 등록/삭제/조회/결제
 * - 보안: BookingSecurityConfig (Order 0) → /api/v1/booking/** → permitAll
 */
@Slf4j
@Controller
@RequestMapping("/api/v1/booking/easy-pay")
@RequiredArgsConstructor
public class EasyPayApiController {

    private static final String BILLKEY_REDIS_PREFIX = "kicc:billkey:";
    private static final String BILLKEY_RESULT_PREFIX = "kicc:billkey-result:";
    private static final Duration REDIS_TTL = Duration.ofMinutes(30);
    private static final Duration RESULT_TTL = Duration.ofMinutes(10);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final EasyPayCardService easyPayCardService;
    private final EasyPayCardRepository easyPayCardRepository;
    private final BookingService bookingService;
    private final PaymentGateway paymentGateway;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private KiccApiClient kiccApiClient;

    @Autowired(required = false)
    private KiccProperties kiccProperties;

    // ========================================
    // 카드 목록 조회
    // ========================================

    /**
     * 이메일로 등록된 간편결제 카드 목록 조회
     */
    @GetMapping("/cards")
    @ResponseBody
    public ResponseEntity<HolaResponse<List<EasyPayCardResponse>>> getCards(@RequestParam String email) {
        List<EasyPayCardResponse> cards = easyPayCardService.getCardsByEmail(email);
        return ResponseEntity.ok(HolaResponse.success(cards));
    }

    /**
     * 카드 등록 가능 여부 확인
     */
    @GetMapping("/cards/can-register")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> canRegister(@RequestParam String email) {
        boolean canRegister = easyPayCardService.canRegisterMore(email);
        long count = easyPayCardService.getCardsByEmail(email).size();
        return ResponseEntity.ok(Map.of("canRegister", canRegister, "currentCount", count, "maxCount", 5));
    }

    // ========================================
    // 빌키 등록 (KICC 인증창 → 빌키 발급)
    // ========================================

    /**
     * 빌키 등록 - Step 1: 거래등록 → KICC 인증창 URL 반환
     */
    @PostMapping("/register")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> registerBillkey(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {

        String email = request.get("email");
        String customerName = request.getOrDefault("customerName", "");
        String customerPhone = request.getOrDefault("customerPhone", "");

        if (email == null || email.isBlank()) {
            throw new HolaException(ErrorCode.EASY_PAY_CARD_EMAIL_MISMATCH, "이메일이 필요합니다.");
        }

        // 카드 수 제한 체크
        if (!easyPayCardService.canRegisterMore(email)) {
            throw new HolaException(ErrorCode.EASY_PAY_CARD_LIMIT_EXCEEDED);
        }

        // 주문번호 생성
        String shopOrderNo = "BILLKEY-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();

        // Redis에 이메일 임시 저장 (콜백에서 사용)
        try {
            Map<String, String> tempData = new HashMap<>();
            tempData.put("email", email);
            tempData.put("shopOrderNo", shopOrderNo);
            String json = objectMapper.writeValueAsString(tempData);
            redisTemplate.opsForValue().set(BILLKEY_REDIS_PREFIX + shopOrderNo, json, REDIS_TTL);
        } catch (JsonProcessingException e) {
            log.error("[간편결제] Redis 저장 실패", e);
            throw new HolaException(ErrorCode.PG_REGISTER_FAILED, "데이터 저장 실패");
        }

        // KICC 거래등록 (빌키 모드: paymentMethod="BILLING", amount=0)
        String userAgent = httpRequest.getHeader("User-Agent");
        String deviceType = isMobile(userAgent) ? "mobile" : "pc";

        RegisterResult result = paymentGateway.registerTransaction(RegisterRequest.builder()
                .orderId(shopOrderNo)
                .amount(BigDecimal.ZERO)
                .goodsName("간편결제 카드 등록")
                .customerName(customerName)
                .customerPhone(customerPhone)
                .customerEmail(email)
                .paymentMethod("BILLING")
                .deviceType(deviceType)
                .build());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("authPageUrl", result.getAuthPageUrl());
        response.put("shopOrderNo", shopOrderNo);

        log.info("[간편결제] 빌키 거래등록 완료 - shopOrderNo: {}, email: {}", shopOrderNo, email);
        return ResponseEntity.ok(response);
    }

    /**
     * 빌키 등록 - Step 2: KICC returnUrl 콜백 → 빌키 발급 + DB 저장
     */
    @PostMapping("/billkey-return")
    public String billkeyReturn(
            @RequestParam String resCd,
            @RequestParam String shopOrderNo,
            @RequestParam(required = false) String authorizationId,
            @RequestParam(required = false) String resMsg,
            Model model) {

        log.info("[간편결제] 빌키 콜백 - resCd: {}, shopOrderNo: {}, authorizationId: {}",
                resCd, shopOrderNo, authorizationId);

        // 1. KICC 인증 실패
        if (!"0000".equals(resCd)) {
            log.warn("[간편결제] 빌키 인증 실패 - resCd: {}, resMsg: {}", resCd, resMsg);
            model.addAttribute("success", false);
            model.addAttribute("errorMessage", resMsg != null ? resMsg : "카드 인증에 실패했습니다.");
            saveBillkeyResult(shopOrderNo, false, null);
            cleanupRedis(shopOrderNo);
            return "booking/billkey-return";
        }

        // 2. Redis에서 이메일 복원
        String email = restoreEmail(shopOrderNo);
        if (email == null) {
            model.addAttribute("success", false);
            model.addAttribute("errorMessage", "세션이 만료되었습니다. 다시 시도해주세요.");
            saveBillkeyResult(shopOrderNo, false, null);
            return "booking/billkey-return";
        }

        try {
            // 3. KICC 승인 (빌키 발급) — 직접 KiccApiClient 사용하여 빌키와 마스킹번호를 별도 추출
            if (kiccApiClient == null) {
                throw new HolaException(ErrorCode.PG_REGISTER_FAILED, "PG 클라이언트가 비활성화 상태입니다.");
            }
            if (kiccProperties == null) {
                throw new HolaException(ErrorCode.PG_REGISTER_FAILED, "PG 설정이 비활성화 상태입니다.");
            }
            String shopTransactionId = UUID.randomUUID().toString();
            String today = LocalDate.now().format(DATE_FMT);

            com.hola.reservation.booking.pg.kicc.dto.KiccApprovalRequest kiccApprovalRequest =
                    com.hola.reservation.booking.pg.kicc.dto.KiccApprovalRequest.builder()
                            .mallId(kiccProperties.getMallId())
                            .shopTransactionId(shopTransactionId)
                            .authorizationId(authorizationId)
                            .shopOrderNo(shopOrderNo)
                            .approvalReqDate(today)
                            .build();

            KiccApprovalResponse approvalResponse = kiccApiClient.approvePayment(kiccApprovalRequest);

            // 디버그: KICC 빌키 발급 응답 전체 로깅
            try {
                String rawJson = objectMapper.writeValueAsString(approvalResponse);
                log.info("[간편결제] KICC 빌키 발급 응답 전체: {}", rawJson);
            } catch (Exception logEx) {
                log.warn("[간편결제] 응답 로깅 실패", logEx);
            }

            // 4. 빌키 + 카드정보 추출
            String batchKey = null;
            String cardMaskNo = null;
            String issuerName = null;
            String cardType = null;
            String pgCno = approvalResponse.getPgCno();

            if (approvalResponse.getPaymentInfo() != null) {
                log.info("[간편결제] paymentInfo.approvalNo: {}", approvalResponse.getPaymentInfo().getApprovalNo());

                if (approvalResponse.getPaymentInfo().getCardInfo() != null) {
                    var cardInfo = approvalResponse.getPaymentInfo().getCardInfo();
                    log.info("[간편결제] cardInfo — cardNo: {}, cardMaskNo: {}, issuerCode: {}, issuerName: {}, acquirerCode: {}, acquirerName: {}, cardGubun: {}",
                            cardInfo.getCardNo(), cardInfo.getCardMaskNo(),
                            cardInfo.getIssuerCode(), cardInfo.getIssuerName(),
                            cardInfo.getAcquirerCode(), cardInfo.getAcquirerName(),
                            cardInfo.getCardGubun());

                    batchKey = cardInfo.getCardNo();
                    cardMaskNo = cardInfo.getCardMaskNo() != null ? cardInfo.getCardMaskNo() : cardInfo.getCardNo();

                    // 카드사명: issuerName → issuerCode 매핑 → acquirerName → acquirerCode 매핑 순서로 fallback
                    issuerName = cardInfo.getIssuerName();
                    if (issuerName == null || issuerName.isBlank()) {
                        issuerName = resolveIssuerName(cardInfo.getIssuerCode());
                    }
                    if (issuerName == null || issuerName.isBlank()) {
                        issuerName = cardInfo.getAcquirerName();
                    }
                    if (issuerName == null || issuerName.isBlank()) {
                        issuerName = resolveIssuerName(cardInfo.getAcquirerCode());
                    }
                    cardType = cardInfo.getCardType();
                } else {
                    log.warn("[간편결제] paymentInfo는 있지만 cardInfo가 null");
                }
            } else {
                log.warn("[간편결제] paymentInfo가 null — KICC 응답 구조 확인 필요");
            }

            if (batchKey == null || batchKey.isBlank()) {
                throw new HolaException(ErrorCode.EASY_PAY_BILLKEY_ISSUE_FAILED, "빌키 값이 응답에 없습니다.");
            }

            // 5. DB 저장
            EasyPayCardResponse savedCard = easyPayCardService.registerCard(
                    email, batchKey, cardMaskNo, issuerName, cardType, pgCno);

            // 6. 결과 Redis 저장
            saveBillkeyResult(shopOrderNo, true, savedCard);

            model.addAttribute("success", true);
            model.addAttribute("shopOrderNo", shopOrderNo);

            log.info("[간편결제] 빌키 발급+등록 완료 - email: {}, cardMaskNo: {}, issuer: {}",
                    email, cardMaskNo, issuerName);

        } catch (Exception e) {
            log.error("[간편결제] 빌키 발급 실패 - shopOrderNo: {}", shopOrderNo, e);
            model.addAttribute("success", false);
            model.addAttribute("errorMessage", "카드 등록 처리 중 오류가 발생했습니다.");
            saveBillkeyResult(shopOrderNo, false, null);
        } finally {
            cleanupRedis(shopOrderNo);
        }

        return "booking/billkey-return";
    }

    /**
     * 빌키 등록 결과 조회 (폴링용)
     */
    @GetMapping("/billkey-result")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getBillkeyResult(@RequestParam String shopOrderNo) {
        String json = redisTemplate.opsForValue().get(BILLKEY_RESULT_PREFIX + shopOrderNo);
        if (json == null) {
            return ResponseEntity.ok(Map.of("status", "PENDING"));
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = objectMapper.readValue(json, Map.class);
            return ResponseEntity.ok(result);
        } catch (JsonProcessingException e) {
            return ResponseEntity.ok(Map.of("status", "ERROR"));
        }
    }

    // ========================================
    // 카드 삭제
    // ========================================

    /**
     * 간편결제 카드 삭제
     */
    @DeleteMapping("/cards/{cardId}")
    @ResponseBody
    public ResponseEntity<HolaResponse<Void>> deleteCard(
            @PathVariable Long cardId,
            @RequestParam String email) {
        easyPayCardService.deleteCard(cardId, email);
        return ResponseEntity.ok(HolaResponse.success(null));
    }

    // ========================================
    // 빌키 결제 (간편결제로 예약 생성)
    // ========================================

    /**
     * 간편결제로 예약 생성 (빌키 결제)
     */
    @PostMapping("/pay")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> payWithBillkey(
            @RequestParam String propertyCode,
            @RequestParam Long cardId,
            @RequestBody BookingCreateRequest request,
            HttpServletRequest httpRequest) {

        String clientIp = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");
        String email = request.getGuest().getEmail();

        // 1. 부킹 검증
        BookingValidationResult validation = bookingService.validateBookingRequest(propertyCode, request);

        // 2. 카드 조회 및 소유권 확인
        List<EasyPayCardResponse> cards = easyPayCardService.getCardsByEmail(email);
        EasyPayCardResponse selectedCard = cards.stream()
                .filter(c -> c.getId().equals(cardId))
                .findFirst()
                .orElseThrow(() -> new HolaException(ErrorCode.EASY_PAY_CARD_NOT_FOUND));

        // 3. 빌키에서 실제 batchKey 조회
        EasyPayCard cardEntity = easyPayCardRepository.findById(cardId)
                .orElseThrow(() -> new HolaException(ErrorCode.EASY_PAY_CARD_NOT_FOUND));

        if (!cardEntity.getEmail().equalsIgnoreCase(email)) {
            throw new HolaException(ErrorCode.EASY_PAY_CARD_EMAIL_MISMATCH);
        }

        // 4. 주문번호 생성
        String shopOrderNo = "HOLA-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();

        try {
            // 5. KICC 빌키 결제 승인
            if (kiccApiClient == null) {
                throw new HolaException(ErrorCode.PG_REGISTER_FAILED, "PG 클라이언트가 비활성화 상태입니다.");
            }
            if (kiccProperties == null) {
                throw new HolaException(ErrorCode.PG_REGISTER_FAILED, "PG 설정이 비활성화 상태입니다.");
            }
            KiccBatchApprovalRequest batchRequest = KiccBatchApprovalRequest.builder()
                    .mallId(kiccProperties.getMallId())
                    .shopTransactionId(UUID.randomUUID().toString())
                    .shopOrderNo(shopOrderNo)
                    .approvalReqDate(LocalDate.now().format(DATE_FMT))
                    .amount(validation.getGrandTotal().longValue())
                    .orderInfo(KiccBatchApprovalRequest.OrderInfo.builder()
                            .goodsName(buildGoodsName(request))
                            .build())
                    .payMethodInfo(KiccBatchApprovalRequest.PayMethodInfo.builder()
                            .billKeyMethodInfo(KiccBatchApprovalRequest.PayMethodInfo.BillKeyMethodInfo.builder()
                                    .batchKey(cardEntity.getBatchKey())
                                    .build())
                            .cardMethodInfo(KiccBatchApprovalRequest.PayMethodInfo.CardMethodInfo.builder()
                                    .installmentMonth(0)
                                    .build())
                            .build())
                    .build();

            KiccApprovalResponse approvalResponse = kiccApiClient.approveBatchPayment(batchRequest);

            // 6. PaymentResult 변환 — cardEntity에서 카드사명/카드번호 직접 사용
            String pgApprovalNo = approvalResponse.getPaymentInfo() != null
                    ? approvalResponse.getPaymentInfo().getApprovalNo() : null;

            PaymentResult paymentResult = PaymentResult.builder()
                    .success(true)
                    .gatewayId("KICC")
                    .pgProvider("KICC")
                    .pgCno(approvalResponse.getPgCno())
                    .amount(BigDecimal.valueOf(approvalResponse.getAmount()))
                    .approvalNo(pgApprovalNo)
                    .pgApprovalNo(pgApprovalNo)
                    .pgStatusCode(approvalResponse.getStatusCode())
                    .pgTransactionId(batchRequest.getShopTransactionId())
                    .pgCardNo(cardEntity.getCardMaskNo())
                    .pgIssuerName(cardEntity.getIssuerName())
                    .pgCardType(cardEntity.getCardType())
                    .installmentMonth(0)
                    .processedAt(java.time.LocalDateTime.now())
                    .build();

            log.info("[간편결제] PaymentResult 생성 — pgIssuerName: {}, pgCardNo: {}, pgApprovalNo: {}",
                    cardEntity.getIssuerName(), cardEntity.getCardMaskNo(), pgApprovalNo);

            // 7. 예약 생성 (기존 메서드 재활용)
            BookingConfirmationResponse confirmation = bookingService.createBookingWithPaymentResult(
                    propertyCode, request, paymentResult, clientIp, userAgent);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("confirmationNo", confirmation.getConfirmationNo());
            response.put("confirmation", confirmation);

            log.info("[간편결제] 빌키 결제+예약 완료 - shopOrderNo: {}, confirmationNo: {}, amount: {}",
                    shopOrderNo, confirmation.getConfirmationNo(), validation.getGrandTotal());

            return ResponseEntity.ok(response);

        } catch (HolaException e) {
            throw e;
        } catch (Exception e) {
            log.error("[간편결제] 빌키 결제 실패 - shopOrderNo: {}, cardId: {}", shopOrderNo, cardId, e);
            throw new HolaException(ErrorCode.BOOKING_PAYMENT_FAILED, "간편결제 처리 중 오류가 발생했습니다.");
        }
    }

    // === 내부 헬퍼 ===

    private String restoreEmail(String shopOrderNo) {
        String json = redisTemplate.opsForValue().get(BILLKEY_REDIS_PREFIX + shopOrderNo);
        if (json == null) return null;
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> data = objectMapper.readValue(json, Map.class);
            return data.get("email");
        } catch (JsonProcessingException e) {
            log.error("[간편결제] Redis 데이터 역직렬화 실패", e);
            return null;
        }
    }

    private void cleanupRedis(String shopOrderNo) {
        redisTemplate.delete(BILLKEY_REDIS_PREFIX + shopOrderNo);
    }

    private void saveBillkeyResult(String shopOrderNo, boolean success, EasyPayCardResponse card) {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("status", success ? "SUCCESS" : "FAILED");
            result.put("success", success);
            if (card != null) {
                result.put("card", card);
            }
            String json = objectMapper.writeValueAsString(result);
            redisTemplate.opsForValue().set(BILLKEY_RESULT_PREFIX + shopOrderNo, json, RESULT_TTL);
        } catch (JsonProcessingException e) {
            log.warn("[간편결제] 결과 저장 실패", e);
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

    /** KICC issuerCd → 카드사명 매핑 (issuerNm이 비어있을 때 fallback) */
    private String resolveIssuerName(String issuerCd) {
        if (issuerCd == null) return null;
        return switch (issuerCd) {
            case "01", "001" -> "비씨카드";
            case "02", "002" -> "KB국민카드";
            case "03", "003" -> "하나카드";
            case "04", "004" -> "삼성카드";
            case "06", "006" -> "신한카드";
            case "07", "007" -> "현대카드";
            case "08", "008" -> "롯데카드";
            case "11", "011" -> "씨티카드";
            case "12", "012" -> "NH농협카드";
            case "13", "013" -> "수협카드";
            case "14", "014" -> "신협카드";
            case "15", "015" -> "우리카드";
            case "16", "016" -> "하나카드";
            case "21", "021" -> "광주카드";
            case "22", "022" -> "전북카드";
            case "23", "023" -> "제주카드";
            case "24", "024" -> "산은카드";
            case "25", "025" -> "해외VISA";
            case "26", "026" -> "해외MASTER";
            case "27", "027" -> "해외JCB";
            default -> "카드(" + issuerCd + ")";
        };
    }
}
