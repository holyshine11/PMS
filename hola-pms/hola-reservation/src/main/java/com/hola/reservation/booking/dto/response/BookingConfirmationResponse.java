package com.hola.reservation.booking.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 예약 확인 응답 (게스트용)
 */
@Getter
@Builder
public class BookingConfirmationResponse {

    private final String confirmationNo;
    private final String masterReservationNo;
    private final String reservationStatus;
    private final String guestNameKo;
    private final BigDecimal totalAmount;
    private final String currency;
    private final String paymentStatus;
    private final String paymentMethod;
    private final String approvalNo;

    /** 객실 예약 내역 */
    private final List<RoomDetail> rooms;

    /** 프로퍼티 정보 */
    private final String propertyName;
    private final String propertyAddress;
    private final String checkInTime;
    private final String checkOutTime;
    private final String propertyPhone;

    private final LocalDateTime createdAt;

    /** 취소 정책 정보 */
    private final List<CancellationPolicyInfo> cancellationPolicies;

    @Getter
    @Builder
    public static class CancellationPolicyInfo {
        private final String description;
    }

    @Getter
    @Builder
    public static class RoomDetail {
        private final String roomTypeName;
        private final LocalDate checkIn;
        private final LocalDate checkOut;
        private final int adults;
        private final int children;
        private final int nights;
        private final BigDecimal roomTotal;
        private final List<AvailableRoomTypeResponse.DailyPrice> dailyCharges;
        /** 포함/추가 서비스 내역 */
        private final List<ServiceDetail> services;
    }

    @Getter
    @Builder
    public static class ServiceDetail {
        private final String serviceName;
        private final String serviceType;
        private final int quantity;
        private final BigDecimal unitPrice;
        private final BigDecimal totalPrice;
        private final LocalDate serviceDate;
    }
}
