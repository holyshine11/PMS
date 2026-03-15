package com.hola.common.auth.controller;

import com.hola.common.auth.dto.LoginRequest;
import com.hola.common.auth.dto.LoginResponse;
import com.hola.common.auth.service.AuthService;
import com.hola.common.dto.HolaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 REST API
 */
@Tag(name = "인증", description = "로그인/로그아웃 인증 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthApiController {

    private final AuthService authService;

    @Operation(summary = "로그인", description = "아이디/비밀번호로 JWT 토큰 발급")
    @PostMapping("/login")
    public ResponseEntity<HolaResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(HolaResponse.success(response));
    }
}
