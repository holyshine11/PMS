package com.hola.room.mapper;

import com.hola.room.dto.response.RoomTypeListResponse;
import com.hola.room.dto.response.RoomTypeResponse;
import com.hola.room.entity.RoomClass;
import com.hola.room.entity.RoomType;
import com.hola.room.entity.RoomTypeFloor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 객실 타입 Entity <-> DTO 변환
 */
@Component
public class RoomTypeMapper {

    /**
     * 리스트 응답 변환
     */
    public RoomTypeListResponse toListResponse(RoomType roomType, RoomClass roomClass, long roomCount) {
        return RoomTypeListResponse.builder()
                .id(roomType.getId())
                .roomTypeCode(roomType.getRoomTypeCode())
                .roomClassCode(roomClass != null ? roomClass.getRoomClassCode() : null)
                .roomClassName(roomClass != null ? roomClass.getRoomClassName() : null)
                .roomSize(roomType.getRoomSize())
                .maxAdults(roomType.getMaxAdults())
                .maxChildren(roomType.getMaxChildren())
                .description(roomType.getDescription())
                .roomCount(roomCount)
                .useYn(roomType.getUseYn())
                .updatedAt(roomType.getUpdatedAt())
                .build();
    }

    /**
     * 상세 응답 변환
     */
    public RoomTypeResponse toResponse(RoomType roomType, RoomClass roomClass,
                                       List<RoomTypeFloor> floorMappings, long roomCount,
                                       List<RoomTypeResponse.ServiceOptionInfo> freeServiceOptions,
                                       List<RoomTypeResponse.ServiceOptionInfo> paidServiceOptions) {
        // 층/호수 매핑 → floorId별 그룹핑
        List<RoomTypeResponse.FloorRoomData> floors = groupFloorData(floorMappings);

        return RoomTypeResponse.builder()
                .id(roomType.getId())
                .propertyId(roomType.getPropertyId())
                .roomClassId(roomType.getRoomClassId())
                .roomClassCode(roomClass != null ? roomClass.getRoomClassCode() : null)
                .roomClassName(roomClass != null ? roomClass.getRoomClassName() : null)
                .roomTypeCode(roomType.getRoomTypeCode())
                .description(roomType.getDescription())
                .roomSize(roomType.getRoomSize())
                .features(roomType.getFeatures())
                .maxAdults(roomType.getMaxAdults())
                .maxChildren(roomType.getMaxChildren())
                .extraBedYn(roomType.getExtraBedYn())
                .sortOrder(roomType.getSortOrder())
                .useYn(roomType.getUseYn())
                .roomCount(roomCount)
                .createdAt(roomType.getCreatedAt())
                .updatedAt(roomType.getUpdatedAt())
                .floors(floors)
                .freeServiceOptions(freeServiceOptions)
                .paidServiceOptions(paidServiceOptions)
                .build();
    }

    /**
     * floorId별로 roomNumberId 그룹핑
     */
    private List<RoomTypeResponse.FloorRoomData> groupFloorData(List<RoomTypeFloor> mappings) {
        if (mappings == null || mappings.isEmpty()) {
            return List.of();
        }

        Map<Long, List<Long>> floorMap = new LinkedHashMap<>();
        for (RoomTypeFloor m : mappings) {
            floorMap.computeIfAbsent(m.getFloorId(), k -> new ArrayList<>()).add(m.getRoomNumberId());
        }

        return floorMap.entrySet().stream()
                .map(e -> RoomTypeResponse.FloorRoomData.builder()
                        .floorId(e.getKey())
                        .roomNumberIds(e.getValue())
                        .build())
                .toList();
    }
}
