package com.hola.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 대시보드 컨트롤러
 */
@Controller
public class DashboardController {

    @GetMapping({"/", "/admin/dashboard"})
    public String dashboard() {
        return "dashboard";
    }
}
