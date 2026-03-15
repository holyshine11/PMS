package com.hola.reservation.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 객실 상세 응답 (산하 2.8 대응)
 * - 시설, 최대인원, 침대타입, 어메니티(무료서비스)
 */
@Getter
@Builder
@AllArgsConstructor
public class RoomDetailResponse {

    /** 객실타입 ID */
    private final Long roomTypeId;

    /** 객실타입 코드 */
    private final String roomTypeCode;

    /** 객실 클래스명 */
    private final String roomClassName;

    /** 객실 클래스 설명 */
    private final String roomClassDescription;

    /** 객실 설명 */
    private final String description;

    /** 객실 크기 (㎡) */
    private final BigDecimal roomSize;

    /** 시설/특징 (콤마 구분 문자열) */
    private final String features;

    /** 시설 목록 (분리) */
    private final List<String> featureList;

    /** 최대 성인 */
    private final Integer maxAdults;

    /** 최대 아동 */
    private final Integer maxChildren;

    /** 엑스트라 베드 가능 여부 */
    private final Boolean extraBedYn;

    /** 가용 객실 수 (현재 기준) */
    private final Integer totalRoomCount;

    /** 무료 서비스(어메니티) 목록 */
    private final List<AmenityInfo> amenities;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class AmenityInfo {
        private final Long serviceId;
        private final String serviceName;
        private final String serviceCategory;
    }
}
