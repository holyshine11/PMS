package com.hola.room.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 무료 서비스 옵션 수정 요청 DTO
 */
@Getter
@NoArgsConstructor
public class FreeServiceOptionUpdateRequest {

    @NotBlank(message = "서비스명(국문)을 입력해주세요.")
    @Size(max = 200, message = "서비스명은 200자 이하입니다.")
    private String serviceNameKo;

    @Size(max = 200, message = "서비스명(영문)은 200자 이하입니다.")
    private String serviceNameEn;

    @NotBlank(message = "서비스 옵션 유형을 선택해주세요.")
    private String serviceType;

    @NotBlank(message = "적용 박수를 선택해주세요.")
    private String applicableNights;

    @NotNull(message = "수량을 입력해주세요.")
    @Min(value = 1, message = "수량은 1 이상이어야 합니다.")
    private Integer quantity;

    @NotBlank(message = "수량 단위를 선택해주세요.")
    private String quantityUnit;

    private Boolean useYn;
}
