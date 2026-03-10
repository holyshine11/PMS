package com.hola.common.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 내 프로필 수정 요청 DTO
 */
@Getter
@NoArgsConstructor
public class MyProfileUpdateRequest {

    @NotBlank(message = "담당자명을 입력해주세요.")
    @Size(max = 100, message = "담당자명은 100자 이하입니다.")
    private String userName;

    @Size(max = 200, message = "이메일은 200자 이하입니다.")
    private String email;

    @Size(max = 10, message = "국가코드는 10자 이하입니다.")
    private String phoneCountryCode;

    @Size(max = 20, message = "연락처는 20자 이하입니다.")
    private String phone;

    @Size(max = 10, message = "국가코드는 10자 이하입니다.")
    private String mobileCountryCode;

    @Size(max = 20, message = "휴대폰 번호는 20자 이하입니다.")
    private String mobile;

    @Size(max = 100, message = "부서명은 100자 이하입니다.")
    private String department;

    @Size(max = 100, message = "직급/직책은 100자 이하입니다.")
    private String position;
}
