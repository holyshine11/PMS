package com.hola.reservation.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 프론트데스크 뷰 컨트롤러
 */
@Controller
@RequestMapping("/admin/front-desk")
public class FrontDeskViewController {

    /**
     * 운영현황 페이지 (도착/투숙/출발 탭 통합)
     */
    @GetMapping("/operations")
    public String operations() {
        return "front-desk/operations";
    }

    /**
     * OOO/OOS 관리 페이지
     */
    @GetMapping("/room-unavailable")
    public String roomUnavailable() {
        return "front-desk/room-unavailable";
    }
}
