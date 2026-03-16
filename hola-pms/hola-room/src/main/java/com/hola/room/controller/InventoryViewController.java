package com.hola.room.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 재고 관리 웹 페이지 컨트롤러
 */
@Controller
@RequestMapping("/admin/inventory-items")
public class InventoryViewController {

    @GetMapping
    public String list(Model model) {
        model.addAttribute("currentURI", "/admin/inventory-items");
        return "inventory-item/list";
    }
}
