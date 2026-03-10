package com.hola.hotel.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 얼리/레이트 정책 웹 페이지 컨트롤러
 */
@Controller
@RequestMapping("/admin/early-late-policies")
public class EarlyLateFeePolicyViewController {

    @GetMapping
    public String list(Model model) {
        model.addAttribute("currentURI", "/admin/early-late-policies");
        return "property/early-late-policy";
    }
}
