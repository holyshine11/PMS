package com.hola.hotel.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 하우스키핑 관리자 View 컨트롤러
 */
@Controller
@RequestMapping("/admin/housekeeping")
public class HousekeepingViewController {

    @GetMapping("/dashboard")
    public String dashboard() {
        return "housekeeping/dashboard";
    }

    @GetMapping("/tasks")
    public String tasks() {
        return "housekeeping/tasks";
    }

    @GetMapping("/board")
    public String board() {
        return "housekeeping/board";
    }

    @GetMapping("/history")
    public String history() {
        return "housekeeping/history";
    }

    @GetMapping("/dayoff")
    public String dayoff() {
        return "housekeeping/dayoff";
    }

    @GetMapping("/attendance")
    public String attendance() {
        return "housekeeping/attendance";
    }

    @GetMapping("/settings")
    public String settings() {
        return "housekeeping/settings";
    }

    @GetMapping("/staff")
    public String staff() {
        return "housekeeping/staff";
    }

    @GetMapping("/staff/new")
    public String staffNew(Model model) {
        model.addAttribute("isSuperAdmin", isSuperAdmin());
        return "housekeeping/staff-form";
    }

    @GetMapping("/staff/{id}/edit")
    public String staffEdit(@org.springframework.web.bind.annotation.PathVariable Long id, Model model) {
        model.addAttribute("staffId", id);
        model.addAttribute("isSuperAdmin", isSuperAdmin());
        return "housekeeping/staff-form";
    }

    /** 현재 사용자가 SUPER_ADMIN인지 확인 */
    private boolean isSuperAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
    }
}
