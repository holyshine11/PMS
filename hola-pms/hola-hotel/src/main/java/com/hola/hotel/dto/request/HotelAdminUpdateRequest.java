package com.hola.hotel.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class HotelAdminUpdateRequest {

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

    @NotEmpty(message = "프로퍼티를 1개 이상 선택해주세요.")
    private List<Long> propertyIds;
}
