package com.hola.hotel.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 호텔 관리자 권한 Thymeleaf 페이지
 */
@Controller
@RequestMapping("/admin/roles/hotel-admins")
@RequiredArgsConstructor
public class HotelRoleViewController {

    @GetMapping
    public String list() {
        return "hotel-role/list";
    }

    @GetMapping("/new")
    public String createForm() {
        return "hotel-role/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("roleId", id);
        return "hotel-role/form";
    }
}
