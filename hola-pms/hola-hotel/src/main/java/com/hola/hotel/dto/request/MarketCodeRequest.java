package com.hola.hotel.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MarketCodeRequest {

    @NotBlank(message = "마켓코드를 입력해주세요.")
    private String marketCode;

    @NotBlank(message = "마켓코드명을 입력해주세요.")
    private String marketName;

    private String descriptionKo;
    private String descriptionEn;
    private Boolean useYn;
    private Integer sortOrder;
}
