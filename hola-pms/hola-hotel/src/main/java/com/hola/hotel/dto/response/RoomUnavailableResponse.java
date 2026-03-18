package com.hola.hotel.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * OOO/OOS 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomUnavailableResponse {
    private Long id;
    private Long roomNumberId;
    private String roomNumber;
    private String unavailableType;
    private String reasonCode;
    private String reasonDetail;
    private LocalDate fromDate;
    private LocalDate throughDate;
    private String returnStatus;
    private LocalDateTime createdAt;
    private String createdBy;
}
