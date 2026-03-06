package com.hola.room.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 객실 타입 리스트 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomTypeListResponse {

    private Long id;
    private String roomTypeCode;
    private String roomClassCode;
    private String roomClassName;
    private Long roomCount;
    private Boolean useYn;
    private LocalDateTime updatedAt;
}
