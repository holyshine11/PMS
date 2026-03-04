package com.hola.hotel.controller;

import com.hola.hotel.service.HotelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 호텔 Thymeleaf 페이지
 */
@Controller
@RequestMapping("/admin/hotels")
@RequiredArgsConstructor
public class HotelViewController {

    private final HotelService hotelService;

    @GetMapping
    public String list() {
        return "hotel/list";
    }

    /** 호텔 등록 페이지 */
    @GetMapping("/new")
    public String createForm() {
        return "hotel/form";
    }

    /** 호텔 수정 페이지 */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("hotelId", id);
        return "hotel/form";
    }
}
