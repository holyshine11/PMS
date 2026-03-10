package com.hola.common.auth.controller;

import com.hola.common.auth.dto.MyProfileResponse;
import com.hola.common.auth.dto.MyProfileUpdateRequest;
import com.hola.common.auth.dto.PasswordChangeRequest;
import com.hola.common.auth.entity.AdminUser;
import com.hola.common.auth.repository.AdminUserRepository;
import com.hola.common.dto.HolaResponse;
import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.common.security.AccessControlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;

/**
 * 내 프로필 REST API
 */
@RestController
@RequestMapping("/api/v1/my-profile")
@RequiredArgsConstructor
public class MyProfileApiController {

    private final AccessControlService accessControlService;
    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    /** 비밀번호 규칙: 영문+숫자+특수문자 */
    private static final String PASSWORD_PATTERN = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{10,20}$";

    /** 내 프로필 조회 */
    @GetMapping
    public ResponseEntity<HolaResponse<MyProfileResponse>> getMyProfile() {
        AdminUser user = accessControlService.getCurrentUser();
        MyProfileResponse response = toResponse(user);
        return ResponseEntity.ok(HolaResponse.success(response));
    }

    /** 내 프로필 수정 */
    @PutMapping
    public ResponseEntity<HolaResponse<MyProfileResponse>> updateMyProfile(
            @Valid @RequestBody MyProfileUpdateRequest request) {
        AdminUser user = accessControlService.getCurrentUser();
        user.updateMyProfile(
                request.getUserName(),
                request.getEmail(),
                request.getPhoneCountryCode(),
                request.getPhone(),
                request.getMobileCountryCode(),
                request.getMobile(),
                request.getDepartment(),
                request.getPosition()
        );
        adminUserRepository.save(user);
        return ResponseEntity.ok(HolaResponse.success(toResponse(user)));
    }

    /** 비밀번호 변경 */
    @PutMapping("/password")
    public ResponseEntity<HolaResponse<Void>> changePassword(
            @Valid @RequestBody PasswordChangeRequest request) {
        AdminUser user = accessControlService.getCurrentUser();

        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new HolaException(ErrorCode.PASSWORD_MISMATCH);
        }

        // 새 비밀번호 확인 일치
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new HolaException(ErrorCode.PASSWORD_CONFIRM_MISMATCH);
        }

        // 비밀번호 형식 검증 (영문+숫자+특수문자)
        if (!request.getNewPassword().matches(PASSWORD_PATTERN)) {
            throw new HolaException(ErrorCode.PASSWORD_INVALID_FORMAT);
        }

        user.changePassword(passwordEncoder.encode(request.getNewPassword()));
        adminUserRepository.save(user);
        return ResponseEntity.ok(HolaResponse.success());
    }

    private MyProfileResponse toResponse(AdminUser user) {
        return MyProfileResponse.builder()
                .loginId(user.getLoginId())
                .memberNumber(user.getMemberNumber())
                .accountType(user.getAccountType())
                .userName(user.getUserName())
                .email(user.getEmail())
                .phoneCountryCode(user.getPhoneCountryCode())
                .phone(user.getPhone())
                .mobileCountryCode(user.getMobileCountryCode())
                .mobile(user.getMobile())
                .department(user.getDepartment())
                .position(user.getPosition())
                .roleName(user.getRoleName())
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().format(DATE_FMT) : null)
                .build();
    }
}
