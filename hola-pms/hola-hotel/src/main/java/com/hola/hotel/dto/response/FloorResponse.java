package com.hola.hotel.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FloorResponse {

    private Long id;
    private Long propertyId;
    private String floorNumber;
    private String floorName;
    private String descriptionKo;
    private String descriptionEn;
    private Integer sortOrder;
    private Boolean useYn;
    private LocalDateTime updatedAt;
}
