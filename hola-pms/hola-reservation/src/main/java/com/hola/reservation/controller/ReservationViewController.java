package com.hola.reservation.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 예약 관리 View 컨트롤러
 */
@Controller
@RequestMapping("/admin/reservations")
public class ReservationViewController {

    /** 예약 리스트 (카드뷰/테이블뷰) */
    @GetMapping
    public String list() {
        return "reservation/list";
    }

    /** 예약 등록 폼 */
    @GetMapping("/new")
    public String createForm() {
        return "reservation/form";
    }

    /** 예약 상세/수정 */
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id) {
        return "reservation/detail";
    }
}
