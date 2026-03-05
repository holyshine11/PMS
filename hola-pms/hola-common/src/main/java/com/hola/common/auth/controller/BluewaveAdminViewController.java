package com.hola.common.auth.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 블루웨이브 관리자 Thymeleaf 페이지
 */
@Controller
@RequestMapping("/admin/members/bluewave-admins")
@RequiredArgsConstructor
public class BluewaveAdminViewController {

    @GetMapping
    public String list() {
        return "bluewave-admin/list";
    }

    /** 등록 페이지 */
    @GetMapping("/new")
    public String createForm() {
        return "bluewave-admin/form";
    }

    /** 수정/상세 페이지 */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("adminId", id);
        return "bluewave-admin/form";
    }
}
