package com.hola.reservation.booking.service;

import com.hola.reservation.booking.dto.request.BookingCreateRequest;
import com.hola.reservation.booking.dto.response.BookingConfirmationResponse;
import com.hola.reservation.booking.dto.response.BookingValidationResult;
import com.hola.reservation.booking.gateway.PaymentResult;

/**
 * 부킹엔진 예약 생성 서비스 인터페이스
 * - 예약 검증, 예약 생성, PG 결제 기반 예약 생성
 */
public interface BookingCreationService {

    BookingValidationResult validateBookingRequest(String propertyCode, BookingCreateRequest request);

    BookingConfirmationResponse createBooking(String propertyCode, BookingCreateRequest request,
                                              String clientIp, String userAgent);

    BookingConfirmationResponse createBookingWithPaymentResult(String propertyCode, BookingCreateRequest request,
                                                                PaymentResult paymentResult,
                                                                String clientIp, String userAgent);
}
