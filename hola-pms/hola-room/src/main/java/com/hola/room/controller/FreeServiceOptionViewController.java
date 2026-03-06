package com.hola.room.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 무료 서비스 옵션 웹 페이지 컨트롤러
 */
@Controller
@RequestMapping("/admin/free-service-options")
public class FreeServiceOptionViewController {

    @GetMapping
    public String list(Model model) {
        model.addAttribute("currentURI", "/admin/free-service-options");
        return "free-service-option/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("currentURI", "/admin/free-service-options");
        return "free-service-option/form";
    }

    @GetMapping("/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("freeServiceOptionId", id);
        model.addAttribute("currentURI", "/admin/free-service-options");
        return "free-service-option/form";
    }
}
