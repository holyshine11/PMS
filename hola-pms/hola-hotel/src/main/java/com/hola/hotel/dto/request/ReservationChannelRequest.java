package com.hola.hotel.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReservationChannelRequest {

    @NotBlank(message = "채널 코드는 필수입니다.")
    @Size(max = 20, message = "채널 코드는 20자 이내여야 합니다.")
    private String channelCode;

    @NotBlank(message = "채널명은 필수입니다.")
    @Size(max = 200, message = "채널명은 200자 이내여야 합니다.")
    private String channelName;

    @NotBlank(message = "채널 유형은 필수입니다.")
    private String channelType;

    private String descriptionKo;
    private String descriptionEn;
    private Integer sortOrder;
    private Boolean useYn;
}
