package com.hola.reservation.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.common.security.AccessControlService;
import com.hola.common.util.NameMaskingUtil;
import com.hola.hotel.dto.response.RoomNumberResponse;
import com.hola.hotel.entity.RoomNumber;
import com.hola.hotel.mapper.HotelMapper;
import com.hola.hotel.repository.RoomNumberRepository;
import com.hola.reservation.dto.response.RoomNumberAvailabilityResponse;
import com.hola.reservation.entity.SubReservation;
import com.hola.reservation.repository.SubReservationRepository;
import com.hola.room.repository.RoomTypeFloorRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 층/호수 배정용 API (예약 상세 페이지에서 사용)
 */
@Tag(name = "객실 배정", description = "층/호수 기반 객실 배정용 API")
@RestController
@RequestMapping("/api/v1/properties/{propertyId}/floors")
@RequiredArgsConstructor
public class RoomAssignApiController {

    private final RoomTypeFloorRepository roomTypeFloorRepository;
    private final RoomNumberRepository roomNumberRepository;
    private final SubReservationRepository subReservationRepository;
    private final HotelMapper hotelMapper;
    private final AccessControlService accessControlService;

    private static final List<String> RELEASED_STATUSES = List.of("CANCELED", "NO_SHOW", "CHECKED_OUT");

    /** 특정 층에 매핑된 호수 목록 조회 (기본) */
    @Operation(summary = "층별 호수 목록 조회", description = "특정 층에 매핑된 호수(객실번호) 목록")
    @GetMapping("/{floorId}/room-numbers")
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
    @Transactional(readOnly = true)
    @GetMapping("/{floorId}/room-numbers/availability")
    public HolaResponse<List<RoomNumberAvailabilityResponse>> getRoomNumbersWithAvailability(
            @PathVariable Long propertyId,
            @PathVariable Long floorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(required = false) Long excludeSubId) {
        accessControlService.validatePropertyAccess(propertyId);

        List<Long> roomNumberIds = roomTypeFloorRepository.findDistinctRoomNumberIdsByFloorId(floorId);
        if (roomNumberIds.isEmpty()) {
            return HolaResponse.success(Collections.emptyList());
        }

        List<RoomNumber> rooms = roomNumberRepository.findAllById(roomNumberIds);

        // 벌크 충돌 조회 (N+1 방지: 1회 쿼리로 모든 호수의 충돌 예약 조회)
        List<SubReservation> allConflicts = subReservationRepository
                .findConflictsByRoomNumberIds(roomNumberIds, checkIn, checkOut, RELEASED_STATUSES);

        // 자기 자신 제외 + roomNumberId별 그룹핑
        Map<Long, List<SubReservation>> conflictMap = allConflicts.stream()
                .filter(sub -> excludeSubId == null || !sub.getId().equals(excludeSubId))
                .collect(Collectors.groupingBy(SubReservation::getRoomNumberId));

        // 응답 생성
        List<RoomNumberAvailabilityResponse> responses = new ArrayList<>();
        for (RoomNumber room : rooms) {
            List<SubReservation> conflicts = conflictMap.getOrDefault(room.getId(), List.of());

            if (conflicts.isEmpty()) {
                responses.add(RoomNumberAvailabilityResponse.builder()
                        .id(room.getId())
                        .roomNumber(room.getRoomNumber())
                        .descriptionKo(room.getDescriptionKo())
                        .available(true)
                        .build());
            } else {
                // 첫 번째 충돌 예약 정보
                SubReservation conflict = conflicts.get(0);
                String guestName = conflict.getMasterReservation().getGuestNameKo();
                String maskedName = (guestName != null && !guestName.isBlank())
                        ? NameMaskingUtil.maskKoreanName(guestName) : "-";

                responses.add(RoomNumberAvailabilityResponse.builder()
                        .id(room.getId())
                        .roomNumber(room.getRoomNumber())
                        .descriptionKo(room.getDescriptionKo())
                        .available(false)
                        .conflictReservationNo(conflict.getMasterReservation().getMasterReservationNo())
                        .conflictGuestName(maskedName)
                        .conflictCheckIn(conflict.getCheckIn())
                        .conflictCheckOut(conflict.getCheckOut())
                        .build());
            }
        }

        // 가용 → 불가 순, 같은 그룹 내 호수번호순
        responses.sort((a, b) -> {
            if (a.isAvailable() != b.isAvailable()) return a.isAvailable() ? -1 : 1;
            return a.getRoomNumber().compareTo(b.getRoomNumber());
        });

        return HolaResponse.success(responses);
    }
}
