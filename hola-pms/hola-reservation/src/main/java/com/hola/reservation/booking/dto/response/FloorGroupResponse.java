package com.hola.reservation.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 층별 그룹 응답 DTO
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FloorGroupResponse {

    private Long floorId;
    private int floorNumber;
    private String floorName;
    private int totalRooms;
    private int availableRooms;

    /** 해당 층 객실 목록 */
    private List<RoomAvailabilityItem> rooms;
}
