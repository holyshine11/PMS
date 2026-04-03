package com.hola.reservation.booking.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.reservation.booking.dto.BookingResponse;
import com.hola.reservation.booking.dto.request.BookingCreateRequest;
import com.hola.reservation.booking.dto.response.EasyPayCardResponse;
import com.hola.reservation.booking.service.EasyPayCardService;
import com.hola.reservation.booking.service.EasyPayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 간편결제(빌키) API 컨트롤러
 * - /api/v1/booking/easy-pay/** : 카드 등록/삭제/조회/결제
 * - 보안: BookingSecurityConfig (Order 0) -> /api/v1/booking/** -> permitAll
 */
@Slf4j
@Controller
@RequestMapping("/api/v1/booking/easy-pay")
@RequiredArgsConstructor
public class EasyPayApiController {

    private final EasyPayCardService easyPayCardService;
    private final EasyPayService easyPayService;

    // ========================================
    // 카드 목록 조회
    // ========================================

    /**
     * 이메일로 등록된 간편결제 카드 목록 조회
     */
    @GetMapping("/cards")
    @ResponseBody
    public ResponseEntity<BookingResponse<List<EasyPayCardResponse>>> getCards(@RequestParam String email) {
        List<EasyPayCardResponse> cards = easyPayCardService.getCardsByEmail(email);
        return ResponseEntity.ok(BookingResponse.success(cards));
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
    // 빌키 등록 (KICC 인증창 -> 빌키 발급)
    // ========================================

    /**
     * 빌키 등록 - Step 1: 거래등록 -> KICC 인증창 URL 반환
     */
    @PostMapping("/register")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> registerBillkey(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {

        String email = request.get("email");
        String customerName = request.getOrDefault("customerName", "");
        String customerPhone = request.getOrDefault("customerPhone", "");
        String userAgent = httpRequest.getHeader("User-Agent");
        String deviceType = isMobile(userAgent) ? "mobile" : "pc";

        Map<String, Object> response = easyPayService.registerBillkey(email, customerName, customerPhone, deviceType);
        return ResponseEntity.ok(response);
    }

    /**
     * 빌키 등록 - Step 2: KICC returnUrl 콜백 -> 빌키 발급 + DB 저장
     */
    @PostMapping("/billkey-return")
    public String billkeyReturn(
            @RequestParam String resCd,
            @RequestParam String shopOrderNo,
            @RequestParam(required = false) String authorizationId,
            @RequestParam(required = false) String resMsg,
            Model model) {

        Map<String, Object> result = easyPayService.processBillkeyReturn(resCd, shopOrderNo, authorizationId, resMsg);

        model.addAttribute("success", result.get("success"));
        if (result.containsKey("errorMessage")) {
            model.addAttribute("errorMessage", result.get("errorMessage"));
        }
        if (result.containsKey("shopOrderNo")) {
            model.addAttribute("shopOrderNo", result.get("shopOrderNo"));
        }

        return "booking/billkey-return";
    }

    /**
     * 빌키 등록 결과 조회 (폴링용)
     */
    @GetMapping("/billkey-result")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getBillkeyResult(@RequestParam String shopOrderNo) {
        Map<String, Object> result = easyPayService.getBillkeyResult(shopOrderNo);
        return ResponseEntity.ok(result);
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

        Map<String, Object> response = easyPayService.payWithBillkey(propertyCode, cardId, request, clientIp, userAgent);
        return ResponseEntity.ok(response);
    }

    // === 내부 헬퍼 ===

    private boolean isMobile(String userAgent) {
        if (userAgent == null) return false;
        String ua = userAgent.toLowerCase();
        return ua.contains("mobile") || ua.contains("android") || ua.contains("iphone");
    }
}
