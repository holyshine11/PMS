package com.hola.hotel.controller;

import com.hola.common.auth.entity.AdminUser;
import com.hola.common.auth.entity.AdminUserProperty;
import com.hola.common.auth.repository.AdminUserPropertyRepository;
import com.hola.common.auth.repository.AdminUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;



/**
 * 하우스키핑 모바일웹 View 컨트롤러
 */
@Controller
@RequestMapping("/m/housekeeping")
@RequiredArgsConstructor
public class HkMobileViewController {

    private final AdminUserRepository adminUserRepository;
    private final AdminUserPropertyRepository adminUserPropertyRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/login")
    public String loginPage() {
        return "mobile/housekeeping/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password,
                        HttpServletRequest request) {
        // 사용자 조회
        AdminUser user = adminUserRepository.findByLoginIdAndDeletedAtIsNull(username).orElse(null);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            return "redirect:/m/housekeeping/login?error";
        }

        // HOUSEKEEPER 또는 HOUSEKEEPING_SUPERVISOR만 허용
        if (!"HOUSEKEEPER".equals(user.getRole()) && !"HOUSEKEEPING_SUPERVISOR".equals(user.getRole())) {
            return "redirect:/m/housekeeping/login?error";
        }

        // 계정 잠금 체크
        if (Boolean.TRUE.equals(user.getAccountLocked()) || !Boolean.TRUE.equals(user.getUseYn())) {
            return "redirect:/m/housekeeping/login?error";
        }

        // 모바일 전용 세션에만 저장 (Admin SecurityContext 건드리지 않음)
        HttpSession session = request.getSession(true);
        session.setAttribute("hkUserId", user.getId());
        session.setAttribute("hkUserName", user.getUserName());
        session.setAttribute("hkUserRole", user.getRole());

        // 매핑된 프로퍼티 ID 저장 (첫 번째 프로퍼티 사용)
        java.util.List<AdminUserProperty> props = adminUserPropertyRepository.findByAdminUserId(user.getId());
        if (!props.isEmpty()) {
            session.setAttribute("hkPropertyId", props.get(0).getPropertyId());
        }

        return "redirect:/m/housekeeping/tasks";
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request) {
        // 모바일 세션 속성만 제거 (Admin 세션 유지)
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute("hkUserId");
            session.removeAttribute("hkUserName");
            session.removeAttribute("hkUserRole");
            session.removeAttribute("hkPropertyId");
        }
        return "redirect:/m/housekeeping/login?logout";
    }

    @GetMapping("/tasks")
    public String tasks(Model model, HttpServletRequest request) {
        // 세션 만료 시 모바일 로그인으로 리다이렉트
        if (!isHkSessionValid(request)) {
            return "redirect:/m/housekeeping/login";
        }
        model.addAttribute("activeTab", "tasks");
        HttpSession session = request.getSession(false);
        model.addAttribute("propertyId", session.getAttribute("hkPropertyId"));
        model.addAttribute("userName", session.getAttribute("hkUserName"));
        return "mobile/housekeeping/tasks";
    }

    @GetMapping("/dayoff")
    public String dayoff(Model model, HttpServletRequest request) {
        if (!isHkSessionValid(request)) {
            return "redirect:/m/housekeeping/login";
        }
        model.addAttribute("activeTab", "dayoff");
        HttpSession session = request.getSession(false);
        model.addAttribute("propertyId", session.getAttribute("hkPropertyId"));
        model.addAttribute("userId", session.getAttribute("hkUserId"));
        return "mobile/housekeeping/dayoff";
    }

    @GetMapping("/profile")
    public String profile(Model model, HttpServletRequest request) {
        if (!isHkSessionValid(request)) {
            return "redirect:/m/housekeeping/login";
        }
        model.addAttribute("activeTab", "profile");
        HttpSession session = request.getSession(false);
        model.addAttribute("propertyId", session.getAttribute("hkPropertyId"));
        model.addAttribute("userId", session.getAttribute("hkUserId"));
        return "mobile/housekeeping/profile";
    }

    @GetMapping("/summary")
    public String summary(Model model, HttpServletRequest request) {
        if (!isHkSessionValid(request)) {
            return "redirect:/m/housekeeping/login";
        }
        model.addAttribute("activeTab", "summary");
        HttpSession session = request.getSession(false);
        model.addAttribute("propertyId", session.getAttribute("hkPropertyId"));
        return "mobile/housekeeping/summary";
    }

    /** 모바일 세션 유효성 체크 */
    private boolean isHkSessionValid(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session != null && session.getAttribute("hkUserId") != null;
    }
}
