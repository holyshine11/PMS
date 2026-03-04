package com.hola.hotel.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 프로퍼티 관리자 Thymeleaf 페이지
 */
@Controller
@RequestMapping("/admin/members/property-admins")
@RequiredArgsConstructor
public class PropertyAdminViewController {

    @GetMapping
    public String list() {
        return "property-admin/list";
    }

    /** 등록 페이지 */
    @GetMapping("/new")
    public String createForm() {
        return "property-admin/form";
    }

    /** 수정/상세 페이지 */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("adminId", id);
        return "property-admin/form";
    }
}
