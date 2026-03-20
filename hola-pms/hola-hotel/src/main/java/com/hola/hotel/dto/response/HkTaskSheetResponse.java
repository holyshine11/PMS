package com.hola.hotel.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 하우스키핑 작업 시트 응답
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HkTaskSheetResponse {

    private Long id;
    private Long propertyId;
    private String sheetName;
    private LocalDate sheetDate;
    private Long assignedTo;
    private String assignedToName;
    private Integer totalRooms;
    private BigDecimal totalCredits;
    private Integer completedRooms;
}
