package com.hola.reservation.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationChangeLogResponse {

    private Long id;
    private Long subReservationId;
    private String changeCategory;
    private String changeType;
    private String fieldName;
    private String oldValue;
    private String newValue;
    private String description;
    private String createdBy;
    private LocalDateTime createdAt;
}
