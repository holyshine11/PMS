package com.hola.reservation.service;

import com.hola.common.util.NameMaskingUtil;
import com.hola.common.auth.entity.AdminUser;
import com.hola.common.auth.repository.AdminUserRepository;
import com.hola.hotel.dto.response.RoomRackFloorGroupResponse;
import com.hola.hotel.dto.response.RoomRackItemResponse;
import com.hola.hotel.entity.HkTask;
import com.hola.hotel.repository.HkTaskRepository;
import com.hola.hotel.service.RoomStatusService;
import com.hola.reservation.entity.SubReservation;
import com.hola.reservation.repository.SubReservationRepository;
import com.hola.room.entity.RoomType;
import com.hola.room.entity.RoomTypeFloor;
import com.hola.room.repository.RoomTypeFloorRepository;
import com.hola.room.repository.RoomTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Room Rack 비즈니스 서비스 구현
 * - hotel + reservation + HK 데이터 합성
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomRackServiceImpl implements RoomRackService {

    private final RoomStatusService roomStatusService;
    private final SubReservationRepository subReservationRepository;
    private final RoomTypeFloorRepository roomTypeFloorRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final HkTaskRepository hkTaskRepository;
    private final AdminUserRepository adminUserRepository;

    @Override
    public List<RoomRackFloorGroupResponse> getRoomRack(Long propertyId) {
        // 1. 객실 기본 정보 + 상태
        List<RoomRackItemResponse> items = roomStatusService.getRoomRackItems(propertyId);

        // 2. 객실타입 매핑 (roomNumberId -> roomTypeCode)
        Map<Long, String> roomTypeNameMap = buildRoomTypeNameMap(propertyId);

        // 3. 투숙중 예약 정보 매핑 (roomNumberId -> SubReservation)
        LocalDate today = LocalDate.now();
        List<SubReservation> inHouseSubs = subReservationRepository.findInHouse(propertyId, today);
        Map<Long, SubReservation> occupiedRooms = new HashMap<>();
        for (SubReservation sub : inHouseSubs) {
            if (sub.getRoomNumberId() != null) {
                occupiedRooms.put(sub.getRoomNumberId(), sub);
            }
        }

        // 4. HK 작업 정보 매핑 (roomNumberId -> HkTask)
        Map<Long, HkTask> hkTaskMap = buildHkTaskMap(propertyId, today);

        // 5. HK 담당자명 배치 조회 (N+1 방지)
        Map<Long, String> assigneeNameMap = buildAssigneeNameMap(hkTaskMap);

        // 6. 객실타입 + 투숙객 + HK 작업 정보 주입
        List<RoomRackItemResponse> enrichedItems = new ArrayList<>();
        for (RoomRackItemResponse item : items) {
            String typeName = roomTypeNameMap.getOrDefault(item.getRoomNumberId(), null);
            SubReservation sub = occupiedRooms.get(item.getRoomNumberId());
            HkTask hkTask = hkTaskMap.get(item.getRoomNumberId());

            RoomRackItemResponse.RoomRackItemResponseBuilder builder = RoomRackItemResponse.builder()
                    .roomNumberId(item.getRoomNumberId())
                    .roomNumber(item.getRoomNumber())
                    .hkStatus(item.getHkStatus())
                    .foStatus(item.getFoStatus())
                    .statusCode(item.getStatusCode())
                    .roomTypeName(typeName)
                    .hkMemo(item.getHkMemo());

            if (sub != null && "OCCUPIED".equals(item.getFoStatus())) {
                builder.guestName(NameMaskingUtil.maskKoreanName(sub.getMasterReservation().getGuestNameKo()))
                       .checkOut(sub.getCheckOut())
                       .reservationId(sub.getMasterReservation().getId());
            } else if (sub == null && "OCCUPIED".equals(item.getFoStatus())) {
                // 고아 OC: foStatus=OCCUPIED인데 매칭 투숙 예약 없음 (체크아웃 미처리)
                builder.orphanOccupied(true);
            }

            // HK 작업 오버레이
            if (hkTask != null && !"CANCELLED".equals(hkTask.getStatus())) {
                builder.hkTaskStatus(hkTask.getStatus());
                if (hkTask.getAssignedTo() != null) {
                    builder.hkAssigneeId(hkTask.getAssignedTo());
                    builder.hkAssigneeName(assigneeNameMap.get(hkTask.getAssignedTo()));
                }
                if (hkTask.getStartedAt() != null) {
                    builder.hkTaskStartedAt(hkTask.getStartedAt().toLocalTime().toString().substring(0, 5));
                }
            }

            enrichedItems.add(builder.build());
        }

        // 7. 층별 그룹핑 (호수 앞자리 기준)
        Map<String, List<RoomRackItemResponse>> floorGroups = new LinkedHashMap<>();
        for (RoomRackItemResponse item : enrichedItems) {
            String floorLabel = extractFloor(item.getRoomNumber());
            floorGroups.computeIfAbsent(floorLabel, k -> new ArrayList<>()).add(item);
        }

        return floorGroups.entrySet().stream()
                .map(e -> RoomRackFloorGroupResponse.builder()
                        .floorLabel(e.getKey())
                        .rooms(e.getValue())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public RoomRackItemResponse getRoomRackItem(Long propertyId, Long roomNumberId) {
        List<RoomRackItemResponse> items = roomStatusService.getRoomRackItems(propertyId);
        RoomRackItemResponse item = items.stream()
                .filter(i -> i.getRoomNumberId().equals(roomNumberId))
                .findFirst().orElse(null);

        if (item == null) {
            return null;
        }

        // 객실타입 매핑
        Map<Long, String> roomTypeNameMap = buildRoomTypeNameMap(propertyId);
        String typeName = roomTypeNameMap.getOrDefault(roomNumberId, null);

        // 투숙객 정보 조회
        LocalDate today = LocalDate.now();
        List<SubReservation> inHouseSubs = subReservationRepository.findInHouse(propertyId, today);
        for (SubReservation sub : inHouseSubs) {
            if (roomNumberId.equals(sub.getRoomNumberId())) {
                return RoomRackItemResponse.builder()
                        .roomNumberId(item.getRoomNumberId())
                        .roomNumber(item.getRoomNumber())
                        .hkStatus(item.getHkStatus())
                        .foStatus(item.getFoStatus())
                        .statusCode(item.getStatusCode())
                        .roomTypeName(typeName)
                        .guestName(NameMaskingUtil.maskKoreanName(sub.getMasterReservation().getGuestNameKo()))
                        .checkOut(sub.getCheckOut())
                        .reservationId(sub.getMasterReservation().getId())
                        .hkMemo(item.getHkMemo())
                        .build();
            }
        }

        // 고아 OC 감지
        boolean orphan = "OCCUPIED".equals(item.getFoStatus());
        return RoomRackItemResponse.builder()
                .roomNumberId(item.getRoomNumberId())
                .roomNumber(item.getRoomNumber())
                .hkStatus(item.getHkStatus())
                .foStatus(item.getFoStatus())
                .statusCode(item.getStatusCode())
                .roomTypeName(typeName)
                .hkMemo(item.getHkMemo())
                .orphanOccupied(orphan ? true : null)
                .build();
    }

    /**
     * 오늘 HK 작업 매핑 (roomNumberId -> 가장 최근 작업)
     */
    private Map<Long, HkTask> buildHkTaskMap(Long propertyId, LocalDate date) {
        List<HkTask> tasks = hkTaskRepository.findByPropertyIdAndTaskDate(propertyId, date);
        Map<Long, HkTask> map = new HashMap<>();
        for (HkTask task : tasks) {
            // 같은 객실에 여러 작업이 있으면 가장 최근 것 사용
            HkTask existing = map.get(task.getRoomNumberId());
            if (existing == null || task.getId() > existing.getId()) {
                map.put(task.getRoomNumberId(), task);
            }
        }
        return map;
    }

    /**
     * HK 태스크 담당자 ID -> 이름 배치 조회 (N+1 방지)
     */
    private Map<Long, String> buildAssigneeNameMap(Map<Long, HkTask> hkTaskMap) {
        Set<Long> assigneeIds = hkTaskMap.values().stream()
                .filter(t -> t.getAssignedTo() != null && !"CANCELLED".equals(t.getStatus()))
                .map(HkTask::getAssignedTo)
                .collect(Collectors.toSet());
        if (assigneeIds.isEmpty()) return Collections.emptyMap();

        return adminUserRepository.findAllById(assigneeIds).stream()
                .collect(Collectors.toMap(AdminUser::getId, AdminUser::getUserName));
    }

    /**
     * 프로퍼티 내 roomNumberId -> roomTypeCode 매핑 빌드
     */
    private Map<Long, String> buildRoomTypeNameMap(Long propertyId) {
        List<RoomTypeFloor> mappings = roomTypeFloorRepository.findAllByPropertyId(propertyId);
        List<RoomType> roomTypes = roomTypeRepository.findAllByPropertyIdOrderBySortOrderAscRoomTypeCodeAsc(propertyId);
        Map<Long, String> typeIdToCode = new HashMap<>();
        for (RoomType rt : roomTypes) {
            typeIdToCode.put(rt.getId(), rt.getRoomTypeCode());
        }
        Map<Long, String> result = new HashMap<>();
        for (RoomTypeFloor rtf : mappings) {
            String code = typeIdToCode.get(rtf.getRoomTypeId());
            if (code != null) {
                result.put(rtf.getRoomNumberId(), code);
            }
        }
        return result;
    }

    /**
     * 호수번호에서 층 추출 (예: "101"->"1F", "B101"->"B1", "1203"->"12F")
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
