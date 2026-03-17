package com.hola.hotel.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Room Rack 층별 그룹 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomRackFloorGroupResponse {

    private String floorLabel;     // 층 라벨 (예: "1F", "2F", "B1")
    private List<RoomRackItemResponse> rooms;
}
