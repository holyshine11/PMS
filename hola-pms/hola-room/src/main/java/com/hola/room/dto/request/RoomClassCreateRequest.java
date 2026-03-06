package com.hola.room.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 객실 클래스 등록 요청 DTO
 */
@Getter
@NoArgsConstructor
public class RoomClassCreateRequest {

    @NotBlank(message = "객실 클래스명을 입력해주세요.")
    @Size(max = 200, message = "객실 클래스명은 200자 이하입니다.")
    private String roomClassName;

    @NotBlank(message = "객실 클래스 코드를 입력해주세요.")
    @Size(max = 50, message = "객실 클래스 코드는 50자 이하입니다.")
    private String roomClassCode;

    @Size(max = 2000, message = "설명은 2000자 이하입니다.")
    private String description;

    private Boolean useYn;
    private Integer sortOrder;
}
