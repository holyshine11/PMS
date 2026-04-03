package com.hola.hotel.service;

import com.hola.common.auth.entity.AdminUser;
import com.hola.common.auth.repository.AdminUserRepository;
import com.hola.common.auth.repository.AdminUserPropertyRepository;
import com.hola.common.security.AccessControlService;
import com.hola.hotel.dto.response.HkDashboardResponse;
import com.hola.hotel.entity.HkDailyAttendance;
import com.hola.hotel.entity.HkTask;
import com.hola.hotel.repository.HkDailyAttendanceRepository;
import com.hola.hotel.repository.HkTaskRepository;
import com.hola.hotel.repository.RoomNumberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 하우스키핑 대시보드 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HkDashboardServiceImpl implements HkDashboardService {

    private final HkTaskRepository hkTaskRepository;
    private final HkDailyAttendanceRepository hkDailyAttendanceRepository;
    private final RoomNumberRepository roomNumberRepository;
    private final AdminUserRepository adminUserRepository;
    private final AdminUserPropertyRepository adminUserPropertyRepository;
    private final AccessControlService accessControlService;

    @Override
    public HkDashboardResponse getDashboard(Long propertyId, LocalDate date) {
        accessControlService.validatePropertyAccess(propertyId);
        LocalDate targetDate = date != null ? date : LocalDate.now();

        // 상태별 카운트
        List<Object[]> statusCounts = hkTaskRepository.countByPropertyIdAndTaskDateGroupByStatus(propertyId, targetDate);
        Map<String, Long> countMap = new HashMap<>();
        long total = 0;
        for (Object[] row : statusCounts) {
            String status = (String) row[0];
            Long count = (Long) row[1];
            countMap.put(status, count);
            total += count;
        }

        int pending = countMap.getOrDefault("PENDING", 0L).intValue();
        int inProgress = countMap.getOrDefault("IN_PROGRESS", 0L).intValue();
        int completed = countMap.getOrDefault("COMPLETED", 0L).intValue();
        int inspected = countMap.getOrDefault("INSPECTED", 0L).intValue();
        int cancelled = countMap.getOrDefault("CANCELLED", 0L).intValue();
        int doneCount = completed + inspected;
        double completionRate = total > 0 ? (doneCount * 100.0 / total) : 0;

        // 하우스키퍼별 집계
        List<HkDashboardResponse.HousekeeperSummary> summaries = buildHousekeeperSummaries(propertyId, targetDate);

        // 미배정 작업 수 (CANCELLED, INSPECTED 제외)
        List<HkTask> allTasks = hkTaskRepository.findByPropertyIdAndTaskDate(propertyId, targetDate);
        int unassigned = (int) allTasks.stream()
                .filter(t -> t.getAssignedTo() == null)
                .filter(t -> !"CANCELLED".equals(t.getStatus()) && !"INSPECTED".equals(t.getStatus()))
                .count();

        // 객실 상태 요약
        HkDashboardResponse.RoomStatusSummary roomStatus = buildRoomStatusSummary(propertyId);

        return HkDashboardResponse.builder()
                .totalTasks((int) total)
                .pendingTasks(pending)
                .inProgressTasks(inProgress)
                .completedTasks(completed)
                .inspectedTasks(inspected)
                .cancelledTasks(cancelled)
                .unassignedTasks(unassigned)
                .completionRate(Math.round(completionRate * 10) / 10.0)
                .housekeeperSummaries(summaries)
                .roomStatusSummary(roomStatus)
                .build();
    }

    @Override
    public List<HkDashboardResponse.HousekeeperSummary> getHousekeepers(Long propertyId) {
        accessControlService.validatePropertyAccess(propertyId);

        // 프로퍼티에 매핑된 HOUSEKEEPER + SUPERVISOR 조회 (배치 조회로 N+1 방지)
        List<Long> userIds = adminUserPropertyRepository.findAdminUserIdsByPropertyId(propertyId);
        if (userIds.isEmpty()) return Collections.emptyList();

        return adminUserRepository.findAllById(userIds).stream()
                .filter(u -> "HOUSEKEEPER".equals(u.getRole()) || "HOUSEKEEPING_SUPERVISOR".equals(u.getRole()))
                .map(u -> HkDashboardResponse.HousekeeperSummary.builder()
                        .userId(u.getId())
                        .userName(u.getUserName())
                        .build())
                .collect(Collectors.toList());
    }

    // === Private 헬퍼 ===

    /**
     * 하우스키퍼별 대시보드 집계
     */
    private List<HkDashboardResponse.HousekeeperSummary> buildHousekeeperSummaries(Long propertyId, LocalDate date) {
        // 하우스키퍼별 상태/크레딧 집계
        List<Object[]> grouped = hkTaskRepository.countByPropertyIdAndTaskDateGroupByAssignedToAndStatus(propertyId, date);
        // 하우스키퍼별 평균 소요시간
        List<Object[]> avgDurations = hkTaskRepository.avgDurationByPropertyIdAndTaskDateGroupByAssignedTo(propertyId, date);

        Map<Long, Double> avgDurationMap = new HashMap<>();
        for (Object[] row : avgDurations) {
            Long userId = (Long) row[0];
            Double avg = row[1] != null ? ((Number) row[1]).doubleValue() : null;
            avgDurationMap.put(userId, avg);
        }

        // userId별로 그룹화
        Map<Long, Map<String, Object>> userMap = new LinkedHashMap<>();
        for (Object[] row : grouped) {
            Long userId = (Long) row[0];
            String status = (String) row[1];
            Long count = (Long) row[2];
            BigDecimal credit = row[3] != null ? new BigDecimal(row[3].toString()) : BigDecimal.ZERO;

            userMap.computeIfAbsent(userId, k -> new HashMap<>());
            Map<String, Object> m = userMap.get(userId);
            m.put("status_" + status, count.intValue());
            BigDecimal existing = (BigDecimal) m.getOrDefault("credits", BigDecimal.ZERO);
            m.put("credits", existing.add(credit));
        }

        // 오늘 출근부 조회 -> 근태 상태 매핑
        List<HkDailyAttendance> attendances = hkDailyAttendanceRepository
                .findByPropertyIdAndAttendanceDate(propertyId, date);
        Map<Long, HkDailyAttendance> attMap = attendances.stream()
                .collect(Collectors.toMap(HkDailyAttendance::getHousekeeperId, a -> a, (a, b) -> a));

        // 사용자 이름 배치 조회 (N+1 방지)
        Map<Long, String> userNameMap = adminUserRepository.findAllById(userMap.keySet()).stream()
                .collect(Collectors.toMap(AdminUser::getId, AdminUser::getUserName));

        List<HkDashboardResponse.HousekeeperSummary> summaries = new ArrayList<>();
        for (Map.Entry<Long, Map<String, Object>> entry : userMap.entrySet()) {
            Long userId = entry.getKey();
            Map<String, Object> m = entry.getValue();
            HkDailyAttendance att = attMap.get(userId);

            summaries.add(HkDashboardResponse.HousekeeperSummary.builder()
                    .userId(userId)
                    .userName(userNameMap.get(userId))
                    .pendingCount((Integer) m.getOrDefault("status_PENDING", 0))
                    .inProgressCount((Integer) m.getOrDefault("status_IN_PROGRESS", 0))
                    .completedCount(
                            (Integer) m.getOrDefault("status_COMPLETED", 0) +
                            (Integer) m.getOrDefault("status_INSPECTED", 0))
                    .totalCredits((BigDecimal) m.getOrDefault("credits", BigDecimal.ZERO))
                    .avgDurationMinutes(avgDurationMap.get(userId))
                    .attendanceStatus(att != null ? att.getAttendanceStatus() : "BEFORE_WORK")
                    .clockInAt(att != null ? att.getClockInAt() : null)
                    .clockOutAt(att != null ? att.getClockOutAt() : null)
                    .build());
        }

        return summaries;
    }

    /**
     * 객실 상태 요약 집계 (대시보드용)
     */
    private HkDashboardResponse.RoomStatusSummary buildRoomStatusSummary(Long propertyId) {
        long vc = roomNumberRepository.countByPropertyIdAndHkStatusAndFoStatus(propertyId, "CLEAN", "VACANT");
        long vd = roomNumberRepository.countByPropertyIdAndHkStatusAndFoStatus(propertyId, "DIRTY", "VACANT");
        long oc = roomNumberRepository.countByPropertyIdAndHkStatusAndFoStatus(propertyId, "CLEAN", "OCCUPIED");
        long od = roomNumberRepository.countByPropertyIdAndHkStatusAndFoStatus(propertyId, "DIRTY", "OCCUPIED");
        long ooo = roomNumberRepository.countByPropertyIdAndHkStatus(propertyId, "OOO");
        long oos = roomNumberRepository.countByPropertyIdAndHkStatus(propertyId, "OOS");
        long dnd = roomNumberRepository.countByPropertyIdAndHkStatus(propertyId, "DND");
        long total = roomNumberRepository.countByPropertyId(propertyId);

        return HkDashboardResponse.RoomStatusSummary.builder()
                .totalRooms((int) total)
                .vacantClean((int) vc)
                .vacantDirty((int) vd)
                .occupiedClean((int) oc)
                .occupiedDirty((int) od)
                .ooo((int) ooo)
                .oos((int) oos)
                .dnd((int) dnd)
                .build();
    }
}
