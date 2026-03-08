package com.hola.rate.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 레이트 코드 웹 페이지 컨트롤러
 */
@Controller
@RequestMapping("/admin/rate-codes")
public class RateCodeViewController {

    @GetMapping
    public String list(Model model) {
        model.addAttribute("currentURI", "/admin/rate-codes");
        return "rate-code/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("currentURI", "/admin/rate-codes");
        return "rate-code/form";
    }

    @GetMapping("/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("rateCodeId", id);
        model.addAttribute("currentURI", "/admin/rate-codes");
        return "rate-code/form";
    }
}
