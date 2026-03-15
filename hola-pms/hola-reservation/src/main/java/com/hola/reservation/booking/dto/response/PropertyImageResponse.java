package com.hola.reservation.booking.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 숙소/객실 이미지 응답
 */
@Getter
@Builder
public class PropertyImageResponse {

    /** 이미지 ID */
    private final Long imageId;

    /** 이미지 유형 (PROPERTY, ROOM_TYPE, FACILITY, EXTERIOR) */
    private final String imageType;

    /** 이미지 URL */
    private final String imagePath;

    /** 이미지 파일명 */
    private final String imageName;

    /** 대체 텍스트 */
    private final String altText;

    /** 정렬 순서 */
    private final Integer sortOrder;
}
