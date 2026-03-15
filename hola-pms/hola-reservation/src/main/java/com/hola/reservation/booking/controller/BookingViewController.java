package com.hola.reservation.booking.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 부킹엔진 게스트 페이지 View 컨트롤러
 * - 인증 불필요 (SecurityConfig에서 /booking/** permitAll)
 * - Admin 레이아웃과 분리된 게스트 전용 레이아웃 사용
 */
@Controller
@RequestMapping("/booking")
public class BookingViewController {

    /**
     * 예약 검색 페이지 (체크인/아웃, 인원 입력)
     */
    @GetMapping("/{propertyCode}")
    public String searchPage(@PathVariable String propertyCode, Model model) {
        model.addAttribute("propertyCode", propertyCode);
        return "booking/search";
    }

    /**
     * 객실 선택 페이지 (가용 객실 + 요금 표시)
     */
    @GetMapping("/{propertyCode}/rooms")
    public String roomsPage(@PathVariable String propertyCode,
                            @RequestParam String checkIn,
                            @RequestParam String checkOut,
                            @RequestParam Integer adults,
                            @RequestParam(defaultValue = "0") Integer children,
                            Model model) {
        model.addAttribute("propertyCode", propertyCode);
        model.addAttribute("checkIn", checkIn);
        model.addAttribute("checkOut", checkOut);
        model.addAttribute("adults", adults);
        model.addAttribute("children", children);
        return "booking/rooms";
    }

    /**
     * 예약 정보 입력 + Mock 결제 페이지
     */
    @GetMapping("/{propertyCode}/checkout")
    public String checkoutPage(@PathVariable String propertyCode, Model model) {
        model.addAttribute("propertyCode", propertyCode);
        return "booking/checkout";
    }

    /**
     * 예약 확인 페이지
     */
    @GetMapping("/{propertyCode}/confirmation/{confirmationNo}")
    public String confirmationPage(@PathVariable String propertyCode,
                                   @PathVariable String confirmationNo,
                                   Model model) {
        model.addAttribute("propertyCode", propertyCode);
        model.addAttribute("confirmationNo", confirmationNo);
        return "booking/confirmation";
    }

    /**
     * 예약 취소 페이지
     */
    @GetMapping("/{propertyCode}/cancel/{confirmationNo}")
    public String cancellationPage(@PathVariable String propertyCode,
                                   @PathVariable String confirmationNo,
                                   Model model) {
        model.addAttribute("propertyCode", propertyCode);
        model.addAttribute("confirmationNo", confirmationNo);
        return "booking/cancellation";
    }
}
