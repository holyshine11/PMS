package com.hola.reservation.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomUpgradeHistoryResponse {

    private Long id;
    private Long subReservationId;
    private Long fromRoomTypeId;
    private String fromRoomTypeName;
    private Long toRoomTypeId;
    private String toRoomTypeName;
    private LocalDateTime upgradedAt;
    private String upgradeType;
    private BigDecimal priceDifference;
    private String reason;
    private String createdBy;
    private LocalDateTime createdAt;
}
