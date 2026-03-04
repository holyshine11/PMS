package com.hola.common.auth.service;

import com.hola.common.auth.dto.LoginRequest;
import com.hola.common.auth.dto.LoginResponse;
import com.hola.common.auth.entity.AdminUser;
import com.hola.common.auth.repository.AdminUserRepository;
import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.common.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    /**
     * API 로그인 (JWT 발급)
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        AdminUser user = adminUserRepository.findByLoginIdAndDeletedAtIsNull(request.getLoginId())
                .orElseThrow(() -> new HolaException(ErrorCode.UNAUTHORIZED, "아이디 또는 비밀번호가 일치하지 않습니다."));

        if (user.getAccountLocked()) {
            throw new HolaException(ErrorCode.FORBIDDEN, "계정이 잠겨있습니다. 관리자에게 문의하세요.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            user.incrementLoginFailCount();
            throw new HolaException(ErrorCode.UNAUTHORIZED, "아이디 또는 비밀번호가 일치하지 않습니다.");
        }

        user.resetLoginFailCount();

        String accessToken = jwtProvider.generateAccessToken(user.getLoginId(), user.getRole());
        String refreshToken = jwtProvider.generateRefreshToken(user.getLoginId());

        log.info("로그인 성공: {}", user.getLoginId());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userName(user.getUserName())
                .role(user.getRole())
                .build();
    }
}
