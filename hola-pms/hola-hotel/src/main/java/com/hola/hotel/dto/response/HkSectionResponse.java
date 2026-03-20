package com.hola.hotel.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 하우스키핑 구역 응답 DTO
 */
@Getter
@Builder
public class HkSectionResponse {

    private Long id;
    private String sectionName;
    private String sectionCode;
    private BigDecimal maxCredits;
    private List<FloorInfo> floors;
    private List<HousekeeperInfo> housekeepers;

    @Getter
    @Builder
    public static class FloorInfo {
        private Long id;
        private String floorNumber;
        private String floorName;
    }

    @Getter
    @Builder
    public static class HousekeeperInfo {
        private Long id;
        private String userName;
        private Boolean isPrimary;
    }
}
