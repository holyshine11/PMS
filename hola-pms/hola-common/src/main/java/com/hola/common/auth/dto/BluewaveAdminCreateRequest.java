package com.hola.common.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 블루웨이브 관리자 등록 요청 DTO
 */
@Getter
@NoArgsConstructor
public class BluewaveAdminCreateRequest {

    @NotBlank(message = "아이디를 입력해주세요.")
    @Size(max = 50, message = "아이디는 50자 이하입니다.")
    private String loginId;

    @NotBlank(message = "담당자명을 입력해주세요.")
    @Size(max = 100, message = "담당자명은 100자 이하입니다.")
    private String userName;

    @NotBlank(message = "이메일을 입력해주세요.")
    @Size(max = 200, message = "이메일은 200자 이하입니다.")
    private String email;

    @NotBlank(message = "연락처를 입력해주세요.")
    @Size(max = 20, message = "연락처는 20자 이하입니다.")
    private String phone;

    private String phoneCountryCode;

    private String mobileCountryCode;

    @Size(max = 20, message = "휴대폰 번호는 20자 이하입니다.")
    private String mobile;

    @Size(max = 100, message = "부서명은 100자 이하입니다.")
    private String department;

    @Size(max = 100, message = "직급/직책은 100자 이하입니다.")
    private String position;

    @Size(max = 100, message = "권한명은 100자 이하입니다.")
    private String roleName;

    private Boolean useYn;
}
