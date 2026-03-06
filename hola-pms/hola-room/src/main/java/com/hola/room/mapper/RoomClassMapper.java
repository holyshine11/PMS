package com.hola.room.mapper;

import com.hola.room.dto.response.RoomClassResponse;
import com.hola.room.entity.RoomClass;
import org.springframework.stereotype.Component;

/**
 * 객실 클래스 Entity <-> DTO 변환
 */
@Component
public class RoomClassMapper {

    public RoomClassResponse toResponse(RoomClass roomClass) {
        return RoomClassResponse.builder()
                .id(roomClass.getId())
                .propertyId(roomClass.getPropertyId())
                .roomClassCode(roomClass.getRoomClassCode())
                .roomClassName(roomClass.getRoomClassName())
                .description(roomClass.getDescription())
                .sortOrder(roomClass.getSortOrder())
                .useYn(roomClass.getUseYn())
                .createdAt(roomClass.getCreatedAt())
                .updatedAt(roomClass.getUpdatedAt())
                .build();
    }
}
