package com.hola.rate.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/promotion-codes")
public class PromotionCodeViewController {

    @GetMapping
    public String list(Model model) {
        model.addAttribute("currentURI", "/admin/promotion-codes");
        return "promotion-code/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("currentURI", "/admin/promotion-codes");
        return "promotion-code/form";
    }

    @GetMapping("/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("promotionCodeId", id);
        model.addAttribute("currentURI", "/admin/promotion-codes");
        return "promotion-code/form";
    }
}
