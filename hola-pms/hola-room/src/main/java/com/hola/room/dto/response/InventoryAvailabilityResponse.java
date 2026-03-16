package com.hola.room.dto.response;

import lombok.*;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryAvailabilityResponse {

    private Long id;
    private LocalDate availabilityDate;
    private Integer availableCount;
    private Integer reservedCount;
    private Integer remainingCount;
}
