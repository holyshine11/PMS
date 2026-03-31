package com.hola.reservation.booking.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 프로퍼티 기본정보 응답 (게스트용)
 */
@Getter
@Builder
public class PropertyInfoResponse {

    private final Long propertyId;
    private final String propertyCode;
    private final String propertyName;
    private final String hotelName;
    private final String propertyType;
    private final Integer starRating;
    private final String checkInTime;
    private final String checkOutTime;
    private final String phone;
    private final String email;
    private final String address;
    private final String addressDetail;
    private final String logoPath;
    private final Boolean sameDayBookingEnabled;
    private final Integer sameDayCutoffTime;
}
