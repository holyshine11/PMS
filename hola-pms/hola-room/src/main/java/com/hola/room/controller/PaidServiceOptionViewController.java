package com.hola.room.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 유료 서비스 옵션 웹 페이지 컨트롤러
 */
@Controller
@RequestMapping("/admin/paid-service-options")
public class PaidServiceOptionViewController {

    @GetMapping
    public String list(Model model) {
        model.addAttribute("currentURI", "/admin/paid-service-options");
        return "paid-service-option/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("currentURI", "/admin/paid-service-options");
        return "paid-service-option/form";
    }

    @GetMapping("/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("paidServiceOptionId", id);
        model.addAttribute("currentURI", "/admin/paid-service-options");
        return "paid-service-option/form";
    }
}
