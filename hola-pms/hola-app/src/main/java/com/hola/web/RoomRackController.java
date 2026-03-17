package com.hola.web;

import com.hola.common.dto.HolaResponse;
import com.hola.common.security.AccessControlService;
import com.hola.hotel.dto.response.RoomRackFloorGroupResponse;
import com.hola.hotel.dto.response.RoomRackItemResponse;
import com.hola.hotel.service.RoomStatusService;
import com.hola.reservation.entity.SubReservation;
import com.hola.reservation.repository.SubReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Room Rack REST API (hola-app에서 hotel + reservation 데이터 합성)
 */
@RestController
@RequestMapping("/api/v1/properties/{propertyId}/room-rack")
@RequiredArgsConstructor
public class RoomRackController {

    private final RoomStatusService roomStatusService;
    private final SubReservationRepository subReservationRepository;
    private final AccessControlService accessControlService;

    /**
     * Room Rack 전체 조회 (층별 그룹핑)
     */
    @GetMapping
    public HolaResponse<List<RoomRackFloorGroupResponse>> getRoomRack(@PathVariable Long propertyId) {
        accessControlService.validatePropertyAccess(propertyId);

        // 1. 객실 기본 정보 + 상태
        List<RoomRackItemResponse> items = roomStatusService.getRoomRackItems(propertyId);

        // 2. 투숙중 예약 정보 매핑 (roomNumberId → SubReservation)
        LocalDate today = LocalDate.now();
        List<SubReservation> inHouseSubs = subReservationRepository.findInHouse(propertyId, today);
        Map<Long, SubReservation> occupiedRooms = new HashMap<>();
        for (SubReservation sub : inHouseSubs) {
            if (sub.getRoomNumberId() != null) {
                occupiedRooms.put(sub.getRoomNumberId(), sub);
            }
        }

        // 3. 투숙객 정보 주입 (FO=OCCUPIED인 객실만)
        List<RoomRackItemResponse> enrichedItems = new ArrayList<>();
        for (RoomRackItemResponse item : items) {
            SubReservation sub = occupiedRooms.get(item.getRoomNumberId());
            if (sub != null && "OCCUPIED".equals(item.getFoStatus())) {
                enrichedItems.add(RoomRackItemResponse.builder()
                        .roomNumberId(item.getRoomNumberId())
                        .roomNumber(item.getRoomNumber())
                        .hkStatus(item.getHkStatus())
                        .foStatus(item.getFoStatus())
                        .statusCode(item.getStatusCode())
                        .guestName(sub.getMasterReservation().getGuestNameKo())
                        .checkOut(sub.getCheckOut())
                        .reservationId(sub.getMasterReservation().getId())
                        .hkMemo(item.getHkMemo())
                        .build());
            } else {
                enrichedItems.add(item);
            }
        }

        // 4. 층별 그룹핑 (호수 앞자리 기준)
        Map<String, List<RoomRackItemResponse>> floorGroups = new LinkedHashMap<>();
        for (RoomRackItemResponse item : enrichedItems) {
            String floorLabel = extractFloor(item.getRoomNumber());
            floorGroups.computeIfAbsent(floorLabel, k -> new ArrayList<>()).add(item);
        }

        List<RoomRackFloorGroupResponse> result = floorGroups.entrySet().stream()
                .map(e -> RoomRackFloorGroupResponse.builder()
                        .floorLabel(e.getKey())
                        .rooms(e.getValue())
                        .build())
                .collect(Collectors.toList());

        return HolaResponse.success(result);
    }

    /**
     * 개별 객실 상세
     */
    @GetMapping("/{roomNumberId}")
    public HolaResponse<RoomRackItemResponse> getRoomRackItem(
            @PathVariable Long propertyId, @PathVariable Long roomNumberId) {
        accessControlService.validatePropertyAccess(propertyId);

        List<RoomRackItemResponse> items = roomStatusService.getRoomRackItems(propertyId);
        RoomRackItemResponse item = items.stream()
                .filter(i -> i.getRoomNumberId().equals(roomNumberId))
                .findFirst().orElse(null);

        if (item == null) {
            return HolaResponse.success(null);
        }

        // 투숙객 정보 조회
        LocalDate today = LocalDate.now();
        List<SubReservation> inHouseSubs = subReservationRepository.findInHouse(propertyId, today);
        for (SubReservation sub : inHouseSubs) {
            if (roomNumberId.equals(sub.getRoomNumberId())) {
                return HolaResponse.success(RoomRackItemResponse.builder()
                        .roomNumberId(item.getRoomNumberId())
                        .roomNumber(item.getRoomNumber())
                        .hkStatus(item.getHkStatus())
                        .foStatus(item.getFoStatus())
                        .statusCode(item.getStatusCode())
                        .guestName(sub.getMasterReservation().getGuestNameKo())
                        .checkOut(sub.getCheckOut())
                        .reservationId(sub.getMasterReservation().getId())
                        .hkMemo(item.getHkMemo())
                        .build());
            }
        }

        return HolaResponse.success(item);
    }

    /**
     * 호수번호에서 층 추출 (예: "101"→"1F", "B101"→"B1", "1203"→"12F")
     */
    private String extractFloor(String roomNumber) {
        if (roomNumber == null || roomNumber.isEmpty()) return "?";

        // "B"로 시작하면 지하층
        if (roomNumber.toUpperCase().startsWith("B")) {
            String digits = roomNumber.substring(1).replaceAll("[^0-9]", "");
            if (digits.length() >= 1) {
                return "B" + digits.charAt(0);
            }
            return "B1";
        }

        // 숫자로 시작: 3자리=앞1자리, 4자리=앞2자리
        String digits = roomNumber.replaceAll("[^0-9]", "");
        if (digits.length() >= 4) {
            return digits.substring(0, 2) + "F";
        } else if (digits.length() >= 3) {
            return digits.substring(0, 1) + "F";
        } else if (digits.length() >= 2) {
            return digits.substring(0, 1) + "F";
        }
        return "1F";
    }
}
