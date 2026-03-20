package com.hola.hotel.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 하우스키핑 휴무일 응답
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HkDayOffResponse {

    private Long id;
    private Long propertyId;
    private Long housekeeperId;
    private String userName;       // 마스킹된 이름
    private LocalDate dayOffDate;
    private String dayOffType;     // REGULAR, REQUESTED, APPROVED
    private String status;         // PENDING, APPROVED, REJECTED
    private String note;
    private String approvedBy;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
}
