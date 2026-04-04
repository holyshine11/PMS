package com.hola.reservation.vo;

import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.hotel.entity.Property;

import java.time.Duration;
import java.time.LocalTime;

/**
 * Dayuse 이용 시간 슬롯 Value Object
 * startTime/endTime을 캡슐화하여 시간 관련 로직을 중앙화
 */
public record DayUseTimeSlot(LocalTime startTime, LocalTime endTime) {

    /**
     * Property 운영설정 + 이용시간으로 생성
     * 운영 종료시간을 초과하면 예외 발생
     */
    public static DayUseTimeSlot from(Property property, Integer durationHours) {
        int hours = durationHours != null ? durationHours : property.getDayUseDefaultHours();
        LocalTime start = LocalTime.parse(property.getDayUseStartTime());
        LocalTime end = start.plusHours(hours);

        // 운영 종료시간 초과 검증
        LocalTime operationEnd = LocalTime.parse(property.getDayUseEndTime());
        if (end.isAfter(operationEnd)) {
            throw new HolaException(ErrorCode.DAY_USE_EXCEEDS_OPERATION_HOURS);
        }

        return new DayUseTimeSlot(start, end);
    }

    /**
     * nullable 필드에서 안전하게 생성 (null이면 null 반환)
     */
    public static DayUseTimeSlot ofNullable(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null) return null;
        return new DayUseTimeSlot(startTime, endTime);
    }

    /**
     * 이용 시간(시간 단위)
     */
    public int durationHours() {
        return (int) Duration.between(startTime, endTime).toHours();
    }

    /**
     * 시간 겹침 여부: A.start < B.end AND A.end > B.start
     */
    public boolean overlapsWith(DayUseTimeSlot other) {
        if (other == null) return true; // 상대 시간 정보 없으면 안전하게 충돌 처리
        return this.startTime.isBefore(other.endTime) && this.endTime.isAfter(other.startTime);
    }
}
