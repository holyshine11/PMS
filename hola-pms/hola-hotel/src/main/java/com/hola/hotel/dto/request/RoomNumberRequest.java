package com.hola.hotel.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RoomNumberRequest {

    @NotBlank(message = "호수를 입력해주세요.")
    private String roomNumber;

    private String descriptionKo;
    private String descriptionEn;
    private Boolean useYn;
    private Integer sortOrder;
}
