package com.hola.reservation.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.common.security.AccessControlService;
import com.hola.hotel.dto.response.RoomNumberResponse;
import com.hola.hotel.mapper.HotelMapper;
import com.hola.hotel.repository.RoomNumberRepository;
import com.hola.reservation.booking.dto.response.RoomAssignAvailabilityResponse;
import com.hola.reservation.dto.response.RoomNumberAvailabilityResponse;
import com.hola.reservation.service.RoomAssignService;
import com.hola.room.repository.RoomTypeFloorRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 층/호수 배정용 API (예약 상세 페이지에서 사용)
 */
@Tag(name = "객실 배정", description = "층/호수 기반 객실 배정용 API")
@RestController
@RequestMapping("/api/v1/properties/{propertyId}")
@RequiredArgsConstructor
public class RoomAssignApiController {

    private final RoomTypeFloorRepository roomTypeFloorRepository;
    private final RoomNumberRepository roomNumberRepository;
    private final HotelMapper hotelMapper;
    private final AccessControlService accessControlService;
    private final RoomAssignService roomAssignService;

    /** 특정 층에 매핑된 호수 목록 조회 (기본) */
    @Operation(summary = "층별 호수 목록 조회", description = "특정 층에 매핑된 호수(객실번호) 목록")
    @GetMapping("/floors/{floorId}/room-numbers")
    public HolaResponse<List<RoomNumberResponse>> getRoomNumbersByFloor(
            @PathVariable Long propertyId,
            @PathVariable Long floorId) {
        accessControlService.validatePropertyAccess(propertyId);

        List<Long> roomNumberIds = roomTypeFloorRepository.findDistinctRoomNumberIdsByFloorId(floorId);
        if (roomNumberIds.isEmpty()) {
            return HolaResponse.success(Collections.emptyList());
        }

        List<RoomNumberResponse> responses = roomNumberRepository.findAllById(roomNumberIds).stream()
                .map(hotelMapper::toResponse)
                .collect(Collectors.toList());

        return HolaResponse.success(responses);
    }

    /** 특정 층에 매핑된 호수 목록 + 가용성 조회 */
    @Operation(summary = "층별 호수 가용성 조회", description = "호수별 가용성 + 충돌 예약 정보 포함")
    @GetMapping("/floors/{floorId}/room-numbers/availability")
    public HolaResponse<List<RoomNumberAvailabilityResponse>> getRoomNumbersWithAvailability(
            @PathVariable Long propertyId,
            @PathVariable Long floorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(required = false) Long excludeSubId) {
        accessControlService.validatePropertyAccess(propertyId);

        return HolaResponse.success(
                roomAssignService.getFloorRoomAvailability(floorId, checkIn, checkOut, excludeSubId));
    }

    /** 객실 배정 가용성 조회 (전체 객실타입 + 층 + 호수 + 요금 비교) */
    @Operation(summary = "객실 배정 가용성 조회", description = "전체 객실타입별 층/호수 가용성과 요금 비교 정보")
    @GetMapping("/room-assign/availability")
    public HolaResponse<RoomAssignAvailabilityResponse> getAvailability(
            @PathVariable Long propertyId,
            @RequestParam Long roomTypeId,
            @RequestParam Long rateCodeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam int adults,
            @RequestParam int children,
            @RequestParam(required = false) Long excludeSubId) {
        accessControlService.validatePropertyAccess(propertyId);

        return HolaResponse.success(
                roomAssignService.getAvailability(
                        propertyId, roomTypeId, rateCodeId,
                        checkIn, checkOut, adults, children, excludeSubId));
    }
}
