package com.hola.hotel.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 하우스키핑 작업 응답
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HkTaskResponse {

    private Long id;
    private Long propertyId;
    private Long roomNumberId;
    private String roomNumber;
    private String floorNumber;
    private String roomTypeName;
    private Long taskSheetId;

    private String taskType;
    private LocalDate taskDate;
    private String status;
    private String priority;
    private BigDecimal credit;

    // 배정
    private Long assignedTo;
    private String assignedToName;
    private Long assignedBy;
    private LocalDateTime assignedAt;

    // 시간 추적
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime inspectedAt;
    private String inspectedByName;
    private LocalDateTime estimatedEnd;
    private Integer durationMinutes;

    // 예약 연계
    private Long reservationId;
    private LocalDateTime nextCheckinAt;

    private String note;
    private LocalDateTime createdAt;
}
