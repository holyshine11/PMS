package com.hola.hotel.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FloorRequest {

    @NotBlank(message = "층 코드를 입력해주세요.")
    @Size(max = 20, message = "층 코드는 20자 이하입니다.")
    private String floorNumber;

    private String floorName;
    private String descriptionKo;
    private String descriptionEn;
    private Boolean useYn;
    private Integer sortOrder;
}
