package com.hola.room.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 트랜잭션 코드 관리 웹 페이지 컨트롤러
 */
@Controller
@RequestMapping("/admin/transaction-codes")
public class TransactionCodeViewController {

    @GetMapping
    public String list(Model model) {
        model.addAttribute("currentURI", "/admin/transaction-codes");
        return "transaction-code/list";
    }
}
