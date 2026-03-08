package com.hola.hotel.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ReservationChannelResponse {
    private Long id;
    private Long propertyId;
    private String channelCode;
    private String channelName;
    private String channelType;
    private String descriptionKo;
    private String descriptionEn;
    private Integer sortOrder;
    private Boolean useYn;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
