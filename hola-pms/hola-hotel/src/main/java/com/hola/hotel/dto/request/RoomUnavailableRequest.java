package com.hola.hotel.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * OOO/OOS 등록/수정 요청 DTO
 */
@Getter
@NoArgsConstructor
public class RoomUnavailableRequest {
    private Long roomNumberId;
    private String unavailableType;  // OOO, OOS
    private String reasonCode;
    private String reasonDetail;
    private LocalDate fromDate;
    private LocalDate throughDate;
    private String returnStatus;     // CLEAN, DIRTY
}
