package com.hola.reservation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RoomUpgradeRequest {

    @NotNull
    private Long toRoomTypeId;

    @NotBlank
    private String upgradeType;         // COMPLIMENTARY / PAID / UPSELL

    @Size(max = 500)
    private String reason;
}
