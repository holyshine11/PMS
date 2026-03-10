package com.hola.common.auth.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 내 프로필 Thymeleaf 페이지
 */
@Controller
@RequestMapping("/admin/my-profile")
public class MyProfileViewController {

    @GetMapping
    public String myProfileForm() {
        return "my-profile/form";
    }
}
