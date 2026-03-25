package com.hola.hotel.service;

import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.common.security.AccessControlService;
import com.hola.hotel.dto.response.RoomRackItemResponse;
import com.hola.hotel.entity.HkTask;
import com.hola.hotel.entity.RoomNumber;
import com.hola.hotel.repository.HkConfigRepository;
import com.hola.hotel.repository.HkTaskRepository;
import com.hola.hotel.repository.RoomNumberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 객실 상태 관리 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomStatusServiceImpl implements RoomStatusService {

    private final RoomNumberRepository roomNumberRepository;
    private final HkTaskRepository hkTaskRepository;
    private final HkConfigRepository hkConfigRepository;
    private final AccessControlService accessControlService;

    @Override
    @Transactional
    public void updateRoomStatus(Long roomNumberId, Long propertyId, String hkStatus, String foStatus, String memo) {
        updateRoomStatus(roomNumberId, propertyId, hkStatus, foStatus, memo, null);
    }

    @Override
    @Transactional
    public void updateRoomStatus(Long roomNumberId, Long propertyId, String hkStatus, String foStatus, String memo, Long assigneeId) {
        RoomNumber room = roomNumberRepository.findById(roomNumberId)
                .orElseThrow(() -> new HolaException(ErrorCode.ROOM_NUMBER_NOT_FOUND));

        // propertyId 소속 검증 (IDOR 방지)
        if (!room.getProperty().getId().equals(propertyId)) {
            throw new HolaException(ErrorCode.FORBIDDEN);
        }

        if (hkStatus != null) {
            room.updateHkStatus(hkStatus, memo);
        }
        if (foStatus != null) {
            if ("VACANT".equals(foStatus)) {
                room.checkOut();
            } else if ("OCCUPIED".equals(foStatus)) {
                room.checkIn();
            }
        }

        // 담당자 배정: VD 상태이고 assigneeId가 넘어온 경우만
        if (assigneeId != null && "DIRTY".equals(hkStatus)) {
            assignHousekeeperToRoom(roomNumberId, propertyId, assigneeId);
        }
    }

    /**
     * VD 객실에 하우스키퍼 담당자 배정
     * - 활성 HkTask가 있으면: PENDING 상태만 재배정, 그 외 상태는 무시
     * - 활성 HkTask가 없으면: 신규 생성 + 배정
     */
    private void assignHousekeeperToRoom(Long roomNumberId, Long propertyId, Long assigneeId) {
        LocalDate today = LocalDate.now();
        Long currentUserId = accessControlService.getCurrentUser().getId();

        List<HkTask> activeTasks = hkTaskRepository.findActiveTasksByRoomNumberIdAndTaskDate(roomNumberId, today);

        if (!activeTasks.isEmpty()) {
            HkTask latestTask = activeTasks.get(0); // 최신순 정렬
            if ("PENDING".equals(latestTask.getStatus())) {
                // PENDING 작업만 재배정 허용
                latestTask.assign(assigneeId, currentUserId);
                log.info("VD 담당자 재배정: roomNumberId={}, taskId={}, assigneeId={}", roomNumberId, latestTask.getId(), assigneeId);
            } else {
                // IN_PROGRESS/COMPLETED/INSPECTED → 재배정 차단 (로그만)
                log.info("VD 담당자 배정 스킵: roomNumberId={}, taskId={}, 현재 상태={} (진행중 작업은 재배정 불가)",
                        roomNumberId, latestTask.getId(), latestTask.getStatus());
            }
        } else {
            // 활성 작업 없음 → 신규 HkTask 생성 + 배정
            BigDecimal credit = hkConfigRepository.findByPropertyId(propertyId)
                    .map(config -> config.getDefaultCheckoutCredit())
                    .orElse(new BigDecimal("1.0"));

            HkTask task = HkTask.builder()
                    .propertyId(propertyId)
                    .roomNumberId(roomNumberId)
                    .taskType("CHECKOUT")
                    .taskDate(today)
                    .priority("NORMAL")
                    .credit(credit)
                    .build();
            task.assign(assigneeId, currentUserId);
            hkTaskRepository.save(task);
            log.info("VD 담당자 배정 (신규 작업): roomNumberId={}, assigneeId={}, credit={}", roomNumberId, assigneeId, credit);
        }
    }

    @Override
    public Map<String, Long> getStatusSummary(Long propertyId) {
        Map<String, Long> summary = new LinkedHashMap<>();
        summary.put("VC", roomNumberRepository.countByPropertyIdAndHkStatusAndFoStatus(propertyId, "CLEAN", "VACANT"));
        summary.put("VD", roomNumberRepository.countByPropertyIdAndHkStatusAndFoStatus(propertyId, "DIRTY", "VACANT"));
        summary.put("OC", roomNumberRepository.countByPropertyIdAndHkStatusAndFoStatus(propertyId, "CLEAN", "OCCUPIED"));
        summary.put("OD", roomNumberRepository.countByPropertyIdAndHkStatusAndFoStatus(propertyId, "DIRTY", "OCCUPIED"));
        summary.put("OOO", roomNumberRepository.countByPropertyIdAndHkStatus(propertyId, "OOO"));
        summary.put("OOS", roomNumberRepository.countByPropertyIdAndHkStatus(propertyId, "OOS"));
        return summary;
    }

    @Override
    public List<RoomRackItemResponse> getRoomRackItems(Long propertyId) {
        List<RoomNumber> rooms = roomNumberRepository.findAllByPropertyIdOrderBySortOrderAscRoomNumberAsc(propertyId);
        List<RoomRackItemResponse> items = new ArrayList<>();

        for (RoomNumber room : rooms) {
            String statusCode = RoomStatusService.calcStatusCode(room.getHkStatus(), room.getFoStatus());
            items.add(RoomRackItemResponse.builder()
                    .roomNumberId(room.getId())
                    .roomNumber(room.getRoomNumber())
                    .hkStatus(room.getHkStatus())
                    .foStatus(room.getFoStatus())
                    .statusCode(statusCode)
                    .hkMemo(room.getHkMemo())
                    .build());
        }
        return items;
    }
}
