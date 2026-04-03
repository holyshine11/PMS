package com.hola.hotel.service;

import com.hola.common.auth.entity.AdminUser;
import com.hola.common.auth.repository.AdminUserRepository;
import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.common.security.AccessControlService;
import com.hola.common.util.NameMaskingUtil;
import com.hola.hotel.dto.response.HkTaskResponse;
import com.hola.hotel.entity.HkConfig;
import com.hola.hotel.entity.HkTask;
import com.hola.hotel.entity.HkTaskLog;
import com.hola.hotel.entity.RoomNumber;
import com.hola.hotel.mapper.HkTaskMapper;
import com.hola.hotel.repository.HkConfigRepository;
import com.hola.hotel.repository.HkTaskLogRepository;
import com.hola.hotel.repository.HkTaskRepository;
import com.hola.hotel.repository.RoomNumberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 하우스키핑 서비스 공통 헬퍼
 * 여러 HK 서비스(HkTaskService, HkDashboardService, HkConfigService)에서 공유하는 유틸리티 메서드
 */
@Component
@RequiredArgsConstructor
public class HkHelper {

    private final HkTaskRepository hkTaskRepository;
    private final HkTaskLogRepository hkTaskLogRepository;
    private final HkConfigRepository hkConfigRepository;
    private final RoomNumberRepository roomNumberRepository;
    private final AdminUserRepository adminUserRepository;
    private final AccessControlService accessControlService;
    private final HkTaskMapper hkTaskMapper;

    /**
     * 작업 ID로 HkTask 조회 (없으면 예외)
     */
    public HkTask findTaskById(Long taskId) {
        return hkTaskRepository.findById(taskId)
                .orElseThrow(() -> new HolaException(ErrorCode.HK_TASK_NOT_FOUND));
    }

    /**
     * 사용자 ID로 이름 조회
     */
    public String getUserName(Long userId) {
        return adminUserRepository.findById(userId)
                .map(AdminUser::getUserName)
                .orElse(null);
    }

    /**
     * 작업 상태 변경 로그 기록
     */
    public void logStatusChange(Long taskId, String fromStatus, String toStatus, String note) {
        HkTaskLog log = HkTaskLog.builder()
                .taskId(taskId)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .changedBy(accessControlService.getCurrentLoginId())
                .note(note)
                .build();
        hkTaskLogRepository.save(log);
    }

    /**
     * Rush(긴급) 자동 판정
     * 다음 체크인 예정 시간까지 남은 시간이 설정값(rushThresholdMinutes) 이하이면
     * 우선순위를 자동으로 RUSH(긴급)로 변경합니다.
     */
    public void applyRushPriority(HkTask task, Long propertyId) {
        // 이미 사용자가 RUSH로 지정했으면 스킵
        if ("RUSH".equals(task.getPriority())) return;

        // 다음 체크인 시간이 없으면 판정 불가
        if (task.getNextCheckinAt() == null) return;

        HkConfig config = hkConfigRepository.findByPropertyId(propertyId).orElse(null);
        int threshold = config != null ? config.getRushThresholdMinutes() : 120;

        long minutesUntilCheckin = Duration.between(
                LocalDateTime.now(), task.getNextCheckinAt()).toMinutes();

        if (minutesUntilCheckin >= 0 && minutesUntilCheckin <= threshold) {
            task.changePriority("RUSH");
        }
    }

    /**
     * 작업 유형별 기본 크레딧 조회
     */
    public BigDecimal getDefaultCreditByType(Long propertyId, String taskType) {
        HkConfig config = hkConfigRepository.findByPropertyId(propertyId).orElse(null);
        if (config == null) return new BigDecimal("1.0");

        switch (taskType) {
            case "CHECKOUT": return config.getDefaultCheckoutCredit();
            case "STAYOVER": return config.getDefaultStayoverCredit();
            case "TURNDOWN": return config.getDefaultTurndownCredit();
            case "DEEP_CLEAN": return config.getDefaultDeepCleanCredit();
            case "TOUCH_UP": return config.getDefaultTouchUpCredit();
            default: return new BigDecimal("1.0");
        }
    }

    /**
     * HkTask -> HkTaskResponse 변환 (단건 조회 - 상세 조회 등에서 사용)
     */
    public HkTaskResponse toResponseWithDetails(HkTask task) {
        RoomNumber room = roomNumberRepository.findById(task.getRoomNumberId()).orElse(null);
        String roomNum = room != null ? room.getRoomNumber() : null;

        String assignedToName = task.getAssignedTo() != null ? getUserName(task.getAssignedTo()) : null;
        String inspectedByName = task.getInspectedBy() != null ? getUserName(task.getInspectedBy()) : null;

        return hkTaskMapper.toResponse(task, roomNum, null, null, assignedToName, inspectedByName);
    }

    /**
     * HkTask 리스트 -> HkTaskResponse 리스트 변환 (배치 조회로 N+1 방지)
     */
    public List<HkTaskResponse> toResponseListWithDetails(List<HkTask> tasks) {
        if (tasks.isEmpty()) return Collections.emptyList();

        // 필요한 ID 수집
        Set<Long> roomIds = tasks.stream().map(HkTask::getRoomNumberId).collect(Collectors.toSet());
        Set<Long> userIds = new HashSet<>();
        tasks.forEach(t -> {
            if (t.getAssignedTo() != null) userIds.add(t.getAssignedTo());
            if (t.getInspectedBy() != null) userIds.add(t.getInspectedBy());
        });

        // 배치 조회
        Map<Long, RoomNumber> roomMap = roomNumberRepository.findAllById(roomIds).stream()
                .collect(Collectors.toMap(RoomNumber::getId, r -> r));
        Map<Long, AdminUser> userMap = userIds.isEmpty() ? Collections.emptyMap()
                : adminUserRepository.findAllById(userIds).stream()
                    .collect(Collectors.toMap(AdminUser::getId, u -> u));

        return tasks.stream().map(task -> {
            RoomNumber room = roomMap.get(task.getRoomNumberId());
            String roomNum = room != null ? room.getRoomNumber() : null;
            String assignedToName = task.getAssignedTo() != null
                    ? Optional.ofNullable(userMap.get(task.getAssignedTo())).map(AdminUser::getUserName).orElse(null)
                    : null;
            String inspectedByName = task.getInspectedBy() != null
                    ? Optional.ofNullable(userMap.get(task.getInspectedBy())).map(AdminUser::getUserName).orElse(null)
                    : null;
            return hkTaskMapper.toResponse(task, roomNum, null, null, assignedToName, inspectedByName);
        }).collect(Collectors.toList());
    }

    /** createdBy -> 마스킹된 이름 변환 (모바일: userId 숫자, PMS: loginId 문자열) */
    public String resolveCreatedByName(String createdBy) {
        if (createdBy == null || createdBy.isBlank()) return null;
        AdminUser creator = null;
        // 숫자면 userId로 조회 (모바일 세션)
        try {
            Long userId = Long.parseLong(createdBy);
            creator = adminUserRepository.findById(userId).orElse(null);
        } catch (NumberFormatException e) {
            // 문자열이면 loginId로 조회 (PMS 세션)
            creator = adminUserRepository.findByLoginIdAndDeletedAtIsNull(createdBy).orElse(null);
        }
        return creator != null ? NameMaskingUtil.maskKoreanName(creator.getUserName()) : null;
    }
}
