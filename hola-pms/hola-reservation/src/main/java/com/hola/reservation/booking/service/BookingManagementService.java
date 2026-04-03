package com.hola.reservation.booking.service;

import com.hola.reservation.booking.dto.request.BookingModifyRequest;
import com.hola.reservation.booking.dto.response.*;

import java.util.List;

/**
 * 부킹엔진 예약 관리 서비스 인터페이스
 * - 취소 수수료 조회, 예약 취소, 예약 조회, 예약 수정
 */
public interface BookingManagementService {

    CancelFeePreviewResponse getCancelFeePreview(String confirmationNo, String email);

    CancelBookingResponse cancelBooking(String confirmationNo, String email,
                                         String clientIp, String userAgent);

    List<BookingLookupResponse> lookupReservations(String email, String lastName);

    BookingModifyResponse modifyBooking(String confirmationNo, BookingModifyRequest request);
}
