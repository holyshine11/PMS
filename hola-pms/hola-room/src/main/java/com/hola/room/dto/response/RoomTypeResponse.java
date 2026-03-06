package com.hola.room.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 객실 타입 상세 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomTypeResponse {

    private Long id;
    private Long propertyId;
    private Long roomClassId;
    private String roomClassCode;
    private String roomClassName;
    private String roomTypeCode;
    private String description;
    private BigDecimal roomSize;
    private String features;
    private Integer maxAdults;
    private Integer maxChildren;
    private Boolean extraBedYn;
    private Integer sortOrder;
    private Boolean useYn;
    private Long roomCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 층/호수 매핑 정보 */
    private List<FloorRoomData> floors;

    /** 무료 서비스 옵션 매핑 */
    private List<ServiceOptionInfo> freeServiceOptions;

    /** 유료 서비스 옵션 매핑 */
    private List<ServiceOptionInfo> paidServiceOptions;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FloorRoomData {
        private Long floorId;
        private List<Long> roomNumberIds;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceOptionInfo {
        private Long id;
        private String serviceOptionCode;
        private String serviceNameKo;
        private String serviceType;
        private Integer quantity;
    }
}
