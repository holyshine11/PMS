package com.hola.hotel.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 취소 정책 웹 페이지 컨트롤러
 */
@Controller
@RequestMapping("/admin/cancellation-policies")
public class CancellationPolicyViewController {

    @GetMapping
    public String list(Model model) {
        model.addAttribute("currentURI", "/admin/cancellation-policies");
        return "property/cancellation-policy";
    }
}
