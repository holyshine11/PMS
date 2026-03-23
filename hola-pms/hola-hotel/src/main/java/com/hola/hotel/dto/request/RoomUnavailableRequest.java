package com.hola.hotel.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * OOO/OOS 등록/수정 요청 DTO
 */
@Getter
@NoArgsConstructor
public class RoomUnavailableRequest {
    @NotNull(message = "객실 번호는 필수입니다.")
    private Long roomNumberId;

    @NotBlank(message = "사용불가 유형은 필수입니다.")
    private String unavailableType;  // OOO, OOS

    private String reasonCode;
    private String reasonDetail;

    @NotNull(message = "시작일은 필수입니다.")
    private LocalDate fromDate;

    @NotNull(message = "종료일은 필수입니다.")
    private LocalDate throughDate;

    private String returnStatus;     // CLEAN, DIRTY
}
