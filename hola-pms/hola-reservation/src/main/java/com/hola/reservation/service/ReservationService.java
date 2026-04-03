package com.hola.reservation.service;

import com.hola.reservation.dto.request.ReservationCreateRequest;
import com.hola.reservation.dto.request.ReservationUpdateRequest;
import com.hola.reservation.dto.response.ReservationDetailResponse;
import com.hola.reservation.dto.response.ReservationListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

/**
 * 예약 CRUD 서비스 인터페이스
 */
public interface ReservationService {

    /** 예약 리스트 조회 (프로퍼티별) */
    List<ReservationListResponse> getList(Long propertyId, String status, LocalDate checkInFrom,
                                           LocalDate checkInTo, String keyword);

    /** 예약 리스트 조회 (페이징) */
    Page<ReservationListResponse> getList(Long propertyId, String status, LocalDate checkInFrom,
                                           LocalDate checkInTo, String keyword, Pageable pageable);

    /** 예약 상세 조회 */
    ReservationDetailResponse getById(Long id, Long propertyId);

    /** 예약 등록 */
    ReservationDetailResponse create(Long propertyId, ReservationCreateRequest request);

    /** 예약 수정 */
    ReservationDetailResponse update(Long id, Long propertyId, ReservationUpdateRequest request);

    /** 예약 삭제 - SUPER_ADMIN 전용, CHECKED_OUT 상태만 */
    void deleteReservation(Long id, Long propertyId);

    /** 객실 가용성 조회 */
    int checkAvailability(Long propertyId, Long roomTypeId, LocalDate checkIn, LocalDate checkOut);
}
