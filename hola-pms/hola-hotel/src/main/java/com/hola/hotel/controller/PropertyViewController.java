package com.hola.hotel.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 프로퍼티 Thymeleaf 페이지 (기존 경로 리디렉트)
 */
@Controller
@RequiredArgsConstructor
public class PropertyViewController {

    /** 기존 경로 호환 - 리디렉트 */
    @GetMapping("/admin/hotels/{hotelId}/properties/{id}")
    public String redirectToEdit(@PathVariable Long hotelId, @PathVariable Long id) {
        return "redirect:/admin/properties/" + id + "/edit";
    }
}
