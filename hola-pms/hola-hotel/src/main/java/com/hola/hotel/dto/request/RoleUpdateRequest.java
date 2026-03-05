package com.hola.hotel.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 권한 수정 요청 DTO
 */
@Getter
@NoArgsConstructor
public class RoleUpdateRequest {

    @NotBlank(message = "권한명을 입력해주세요.")
    @Size(max = 100, message = "권한명은 100자 이하입니다.")
    private String roleName;

    private Boolean useYn;

    private List<Long> menuIds;
}
