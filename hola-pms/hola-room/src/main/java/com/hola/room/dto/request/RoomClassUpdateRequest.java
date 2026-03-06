package com.hola.room.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 객실 클래스 수정 요청 DTO
 */
@Getter
@NoArgsConstructor
public class RoomClassUpdateRequest {

    @NotBlank(message = "객실 클래스명을 입력해주세요.")
    @Size(max = 200, message = "객실 클래스명은 200자 이하입니다.")
    private String roomClassName;

    @Size(max = 2000, message = "설명은 2000자 이하입니다.")
    private String description;

    private Boolean useYn;
    private Integer sortOrder;
}
