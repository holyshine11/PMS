package com.hola.hotel.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 프로퍼티 관리자 권한 Thymeleaf 페이지
 */
@Controller
@RequestMapping("/admin/roles/property-admins")
@RequiredArgsConstructor
public class PropertyRoleViewController {

    @GetMapping
    public String list() {
        return "property-role/list";
    }

    @GetMapping("/new")
    public String createForm() {
        return "property-role/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("roleId", id);
        return "property-role/form";
    }
}
