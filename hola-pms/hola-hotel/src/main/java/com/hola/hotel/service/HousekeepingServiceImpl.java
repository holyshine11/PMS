package com.hola.hotel.service;

import com.hola.common.auth.entity.AdminUser;
import com.hola.common.auth.repository.AdminUserRepository;
import com.hola.common.auth.repository.AdminUserPropertyRepository;
import com.hola.common.exception.ErrorCode;
import com.hola.common.util.NameMaskingUtil;
import com.hola.common.exception.HolaException;
import com.hola.common.security.AccessControlService;
import com.hola.hotel.dto.request.*;
import com.hola.hotel.dto.response.*;
import com.hola.hotel.entity.*;
import com.hola.hotel.mapper.HkTaskMapper;
import com.hola.hotel.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 하우스키핑 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HousekeepingServiceImpl implements HousekeepingService {

    private final HkTaskRepository hkTaskRepository;
    private final HkTaskSheetRepository hkTaskSheetRepository;
    private final HkTaskLogRepository hkTaskLogRepository;
    private final HkTaskIssueRepository hkTaskIssueRepository;
    private final HkConfigRepository hkConfigRepository;
    private final HkDailyAttendanceRepository hkDailyAttendanceRepository;
    private final RoomNumberRepository roomNumberRepository;
    private final AdminUserRepository adminUserRepository;
    private final AdminUserPropertyRepository adminUserPropertyRepository;
    private final AccessControlService accessControlService;
    private final HkTaskMapper hkTaskMapper;
    private final HkCleaningPolicyService cleaningPolicyService;

    // === 대시보드 ===

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

    // === 작업 관리 ===

    @Override
    public List<HkTaskResponse> getTasks(Long propertyId, LocalDate date, String status,
                                         Long assignedTo, String taskType) {
        accessControlService.validatePropertyAccess(propertyId);
        LocalDate targetDate = date != null ? date : LocalDate.now();

        List<HkTask> tasks = hkTaskRepository.findByPropertyIdAndTaskDate(propertyId, targetDate);

        // Java stream 필터링 (JPQL null 파라미터 금지)
        List<HkTask> filtered = tasks.stream()
                .filter(t -> status == null || t.getStatus().equals(status))
                .filter(t -> assignedTo == null || assignedTo.equals(t.getAssignedTo()))
                .filter(t -> taskType == null || t.getTaskType().equals(taskType))
                .collect(Collectors.toList());

        return toResponseListWithDetails(filtered);
    }

    @Override
    public HkTaskResponse getTask(Long taskId) {
        HkTask task = findTaskById(taskId);
        return toResponseWithDetails(task);
    }

    @Override
    @Transactional
    public HkTaskResponse createTask(Long propertyId, HkTaskCreateRequest request) {
        accessControlService.validatePropertyAccess(propertyId);

        // 객실 존재 확인
        RoomNumber room = roomNumberRepository.findById(request.getRoomNumberId())
                .orElseThrow(() -> new HolaException(ErrorCode.ROOM_NUMBER_NOT_FOUND));

        HkTask task = hkTaskMapper.toEntity(request, propertyId);

        // 타입별 기본 크레딧 적용 (사용자가 지정하지 않은 경우 설정값 사용)
        if (task.getCredit() == null) {
            task.update(task.getTaskType(), task.getPriority(),
                    getDefaultCreditByType(propertyId, task.getTaskType()), task.getNote());
        }

        // Rush 자동 판정: 다음 체크인까지 남은 시간이 기준 이하이면 긴급 처리
        applyRushPriority(task, propertyId);

        // 배정 처리
        if (request.getAssignedTo() != null) {
            AdminUser currentUser = accessControlService.getCurrentUser();
            task.assign(request.getAssignedTo(), currentUser.getId());
        }

        HkTask saved = hkTaskRepository.save(task);
        logStatusChange(saved.getId(), null, "PENDING", null);

        log.info("HK 작업 생성: taskId={}, roomNumber={}, type={}, priority={}",
                saved.getId(), room.getRoomNumber(), request.getTaskType(), saved.getPriority());
        return toResponseWithDetails(saved);
    }

    @Override
    @Transactional
    public HkTaskResponse updateTask(Long taskId, HkTaskUpdateRequest request) {
        HkTask task = findTaskById(taskId);

        task.update(
                request.getTaskType() != null ? request.getTaskType() : task.getTaskType(),
                request.getPriority() != null ? request.getPriority() : task.getPriority(),
                request.getCredit() != null ? request.getCredit() : task.getCredit(),
                request.getNote() != null ? request.getNote() : task.getNote()
        );

        if (request.getAssignedTo() != null && !request.getAssignedTo().equals(task.getAssignedTo())) {
            AdminUser currentUser = accessControlService.getCurrentUser();
            task.assign(request.getAssignedTo(), currentUser.getId());
        }

        return toResponseWithDetails(task);
    }

    @Override
    @Transactional
    public void assignTask(Long taskId, Long assignedTo) {
        HkTask task = findTaskById(taskId);
        AdminUser currentUser = accessControlService.getCurrentUser();
        task.assign(assignedTo, currentUser.getId());
        log.info("HK 작업 배정: taskId={}, assignedTo={}", taskId, assignedTo);
    }

    @Override
    @Transactional
    public void unassignTask(Long taskId) {
        HkTask task = findTaskById(taskId);
        task.unassign();
        log.info("HK 작업 배정 해제: taskId={}", taskId);
    }

    @Override
    @Transactional
    public void batchAssignTasks(Long propertyId, HkBatchAssignRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        AdminUser currentUser = accessControlService.getCurrentUser();

        for (Long taskId : request.getTaskIds()) {
            HkTask task = findTaskById(taskId);
            if (request.getAssignedTo() != null) {
                task.assign(request.getAssignedTo(), currentUser.getId());
            } else {
                // assignedTo가 null이면 배정 해제
                task.unassign();
            }
        }
        log.info("HK 일괄 배정: taskIds={}, assignedTo={}", request.getTaskIds(), request.getAssignedTo());
    }

    @Override
    @Transactional
    public void inspectTask(Long taskId) {
        HkTask task = findTaskById(taskId);
        if (!"COMPLETED".equals(task.getStatus())) {
            throw new HolaException(ErrorCode.HK_TASK_STATUS_CHANGE_NOT_ALLOWED);
        }

        AdminUser currentUser = accessControlService.getCurrentUser();
        String prevStatus = task.getStatus();
        task.inspect(currentUser.getId());
        logStatusChange(taskId, prevStatus, "INSPECTED", null);

        // Room hkStatus → INSPECTED → CLEAN 자동 전환
        RoomNumber room = roomNumberRepository.findById(task.getRoomNumberId())
                .orElseThrow(() -> new HolaException(ErrorCode.ROOM_NUMBER_NOT_FOUND));
        room.updateHkStatus("CLEAN", null);

        log.info("HK 검수 완료: taskId={}, roomNumber={}", taskId, room.getRoomNumber());
    }

    @Override
    @Transactional
    public void cancelTask(Long taskId) {
        HkTask task = findTaskById(taskId);
        String prevStatus = task.getStatus();

        // COMPLETED/INSPECTED 상태는 취소 불가 (이미 객실 상태가 변경됨)
        if ("COMPLETED".equals(prevStatus) || "INSPECTED".equals(prevStatus)) {
            throw new HolaException(ErrorCode.HK_TASK_STATUS_CHANGE_NOT_ALLOWED,
                    "완료/검수된 태스크는 취소할 수 없습니다. 현재 상태: " + prevStatus);
        }
        if ("CANCELLED".equals(prevStatus)) {
            throw new HolaException(ErrorCode.HK_TASK_STATUS_CHANGE_NOT_ALLOWED,
                    "이미 취소된 태스크입니다.");
        }

        task.cancel();
        logStatusChange(taskId, prevStatus, "CANCELLED", null);
    }

    // === 작업 상태 변경 (모바일) ===

    @Override
    @Transactional
    public void startTask(Long taskId) {
        startTask(taskId, null);
    }

    @Override
    @Transactional
    public void startTask(Long taskId, Long startedByUserId) {
        HkTask task = findTaskById(taskId);
        // 미배정 작업 시작 시 해당 사용자에게 자동 배정
        if (task.getAssignedTo() == null && startedByUserId != null) {
            task.assign(startedByUserId, startedByUserId);
            log.info("HK 작업 시작 시 자동 배정: taskId={}, userId={}", taskId, startedByUserId);
        }
        if (!"PENDING".equals(task.getStatus()) && !"PAUSED".equals(task.getStatus())) {
            throw new HolaException(ErrorCode.HK_TASK_STATUS_CHANGE_NOT_ALLOWED);
        }
        String prevStatus = task.getStatus();
        task.start();
        logStatusChange(taskId, prevStatus, "IN_PROGRESS", null);
    }

    @Override
    @Transactional
    public void pauseTask(Long taskId) {
        HkTask task = findTaskById(taskId);
        if (!"IN_PROGRESS".equals(task.getStatus())) {
            throw new HolaException(ErrorCode.HK_TASK_STATUS_CHANGE_NOT_ALLOWED);
        }
        String prevStatus = task.getStatus();
        task.pause();
        logStatusChange(taskId, prevStatus, "PAUSED", null);
    }

    @Override
    @Transactional
    public void completeTask(Long taskId) {
        HkTask task = findTaskById(taskId);
        if (!"IN_PROGRESS".equals(task.getStatus())) {
            throw new HolaException(ErrorCode.HK_TASK_STATUS_CHANGE_NOT_ALLOWED);
        }
        String prevStatus = task.getStatus();
        task.complete();
        logStatusChange(taskId, prevStatus, "COMPLETED", null);

        // Room hkStatus 변경
        RoomNumber room = roomNumberRepository.findById(task.getRoomNumberId())
                .orElseThrow(() -> new HolaException(ErrorCode.ROOM_NUMBER_NOT_FOUND));

        // 검수 필수 여부 확인
        HkConfig config = hkConfigRepository.findByPropertyId(task.getPropertyId()).orElse(null);
        boolean inspectionRequired = config != null && Boolean.TRUE.equals(config.getInspectionRequired());

        if (inspectionRequired) {
            // 검수 필수 → PICKUP 상태 (감독자 검수 대기)
            room.updateHkStatus("PICKUP", null);
        } else {
            // 검수 생략 → 즉시 CLEAN
            room.updateHkStatus("CLEAN", null);
        }

        log.info("HK 작업 완료: taskId={}, roomNumber={}, duration={}분",
                taskId, room.getRoomNumber(), task.getDurationMinutes());
    }

    // === 작업 시트 ===

    @Override
    public List<HkTaskSheetResponse> getTaskSheets(Long propertyId, LocalDate date) {
        accessControlService.validatePropertyAccess(propertyId);
        LocalDate targetDate = date != null ? date : LocalDate.now();
        List<HkTaskSheet> sheets = hkTaskSheetRepository.findByPropertyIdAndSheetDateOrderBySortOrder(propertyId, targetDate);

        return sheets.stream()
                .map(s -> {
                    String name = s.getAssignedTo() != null ? getUserName(s.getAssignedTo()) : null;
                    return hkTaskMapper.toResponse(s, name);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public HkTaskSheetResponse generateTaskSheet(Long propertyId, LocalDate date,
                                                  Long assignedTo, String sheetName) {
        accessControlService.validatePropertyAccess(propertyId);

        HkTaskSheet sheet = HkTaskSheet.builder()
                .propertyId(propertyId)
                .sheetName(sheetName != null ? sheetName : "Sheet")
                .sheetDate(date != null ? date : LocalDate.now())
                .assignedTo(assignedTo)
                .build();

        HkTaskSheet saved = hkTaskSheetRepository.save(sheet);
        String name = assignedTo != null ? getUserName(assignedTo) : null;
        return hkTaskMapper.toResponse(saved, name);
    }

    @Override
    @Transactional
    public void deleteTaskSheet(Long sheetId) {
        HkTaskSheet sheet = hkTaskSheetRepository.findById(sheetId)
                .orElseThrow(() -> new HolaException(ErrorCode.HK_TASK_SHEET_NOT_FOUND));
        sheet.softDelete();
    }

    // === 작업 시트: 크레딧 균등 재분배 ===

    @Override
    @Transactional
    public void redistributeTaskSheets(Long propertyId, LocalDate date) {
        accessControlService.validatePropertyAccess(propertyId);
        LocalDate targetDate = date != null ? date : LocalDate.now();

        // 오늘 작업 시트 목록 (하우스키퍼가 배정된 시트만)
        List<HkTaskSheet> sheets = hkTaskSheetRepository.findByPropertyIdAndSheetDate(propertyId, targetDate)
                .stream().filter(s -> s.getAssignedTo() != null).collect(Collectors.toList());

        if (sheets.size() < 2) return; // 2명 미만이면 재분배 불필요

        // 재분배 대상: PENDING/PAUSED 상태 작업만 (진행중/완료 작업은 배정 유지)
        List<HkTask> unassignedTasks = hkTaskRepository.findByPropertyIdAndTaskDate(propertyId, targetDate)
                .stream()
                .filter(t -> "PENDING".equals(t.getStatus()) || "PAUSED".equals(t.getStatus()))
                .collect(Collectors.toList());

        if (unassignedTasks.isEmpty()) return;

        // 크레딧 내림차순 정렬 (큰 작업부터 배정 → 균형 잡기 쉬움)
        unassignedTasks.sort((a, b) -> b.getCredit().compareTo(a.getCredit()));

        // 시트별 현재 크레딧 합계 초기화
        BigDecimal[] sheetCredits = new BigDecimal[sheets.size()];
        for (int i = 0; i < sheets.size(); i++) {
            sheetCredits[i] = BigDecimal.ZERO;
        }

        // 탐욕 알고리즘: 현재 크레딧이 가장 적은 시트에 작업 배정
        for (HkTask task : unassignedTasks) {
            int minIdx = 0;
            for (int i = 1; i < sheetCredits.length; i++) {
                if (sheetCredits[i].compareTo(sheetCredits[minIdx]) < 0) {
                    minIdx = i;
                }
            }
            HkTaskSheet targetSheet = sheets.get(minIdx);
            task.assignToSheet(targetSheet.getId());
            task.assign(targetSheet.getAssignedTo(), null);
            sheetCredits[minIdx] = sheetCredits[minIdx].add(task.getCredit());
        }

        // 시트 집계 갱신
        for (int i = 0; i < sheets.size(); i++) {
            HkTaskSheet sheet = sheets.get(i);
            List<HkTask> sheetTasks = unassignedTasks.stream()
                    .filter(t -> sheet.getId().equals(t.getTaskSheetId()))
                    .collect(Collectors.toList());
            int completedCount = (int) sheetTasks.stream()
                    .filter(t -> "COMPLETED".equals(t.getStatus()) || "INSPECTED".equals(t.getStatus()))
                    .count();
            sheet.updateStats(sheetTasks.size(), sheetCredits[i], completedCount);
        }

        log.info("크레딧 균등 재분배 완료: propertyId={}, date={}, 시트={}개, 작업={}건",
                propertyId, targetDate, sheets.size(), unassignedTasks.size());
    }

    // === 이슈/메모 ===

    @Override
    @Transactional
    public HkTaskIssueResponse createIssue(Long taskId, Long propertyId, Long roomNumberId,
                                            HkTaskIssueCreateRequest request) {
        HkTaskIssue issue = HkTaskIssue.builder()
                .taskId(taskId)
                .propertyId(propertyId)
                .roomNumberId(roomNumberId)
                .issueType(request.getIssueType())
                .description(request.getDescription())
                .imagePath(request.getImagePath())
                .build();

        HkTaskIssue saved = hkTaskIssueRepository.save(issue);
        RoomNumber room = roomNumberRepository.findById(roomNumberId)
                .orElseThrow(() -> new HolaException(ErrorCode.ROOM_NUMBER_NOT_FOUND));
        return hkTaskMapper.toResponse(saved, room.getRoomNumber());
    }

    @Override
    public List<HkTaskIssueResponse> getTaskIssues(Long taskId) {
        List<HkTaskIssue> issues = hkTaskIssueRepository.findByTaskIdOrderByCreatedAtDesc(taskId);
        return issues.stream()
                .map(i -> {
                    RoomNumber room = roomNumberRepository.findById(i.getRoomNumberId()).orElse(null);
                    String roomNum = room != null ? room.getRoomNumber() : null;
                    // createdBy → userName 조회 → 마스킹
                    // PMS: loginId (문자열), 모바일: userId (숫자 문자열)
                    String createdByName = resolveCreatedByName(i.getCreatedBy());
                    return hkTaskMapper.toResponse(i, roomNum, createdByName);
                })
                .collect(Collectors.toList());
    }

    // === 설정 ===

    @Override
    public HkConfigResponse getConfig(Long propertyId) {
        accessControlService.validatePropertyAccess(propertyId);
        HkConfig config = hkConfigRepository.findByPropertyId(propertyId)
                .orElse(null);

        if (config == null) {
            // 설정 없으면 기본값 반환
            return HkConfigResponse.builder()
                    .propertyId(propertyId)
                    .inspectionRequired(false)
                    .autoCreateCheckout(true)
                    .autoCreateStayover(false)
                    .defaultCheckoutCredit(new BigDecimal("1.0"))
                    .defaultStayoverCredit(new BigDecimal("0.5"))
                    .defaultTurndownCredit(new BigDecimal("0.3"))
                    .defaultDeepCleanCredit(new BigDecimal("2.0"))
                    .defaultTouchUpCredit(new BigDecimal("0.3"))
                    .rushThresholdMinutes(120)
                    .stayoverEnabled(false)
                    .stayoverFrequency(1)
                    .turndownEnabled(false)
                    .dndPolicy("SKIP")
                    .dndMaxSkipDays(3)
                    .dailyTaskGenTime("06:00")
                    .odTransitionTime("05:00")
                    .build();
        }
        return hkTaskMapper.toResponse(config);
    }

    @Override
    @Transactional
    public HkConfigResponse updateConfig(Long propertyId, HkConfigUpdateRequest request) {
        accessControlService.validatePropertyAccess(propertyId);

        HkConfig config = hkConfigRepository.findByPropertyId(propertyId)
                .orElseGet(() -> {
                    HkConfig newConfig = HkConfig.builder()
                            .propertyId(propertyId)
                            .build();
                    return hkConfigRepository.save(newConfig);
                });

        config.update(
                request.getInspectionRequired(),
                request.getAutoCreateCheckout(),
                request.getAutoCreateStayover(),
                request.getDefaultCheckoutCredit(),
                request.getDefaultStayoverCredit(),
                request.getDefaultTurndownCredit(),
                request.getDefaultDeepCleanCredit(),
                request.getDefaultTouchUpCredit(),
                request.getRushThresholdMinutes(),
                request.getStayoverEnabled(),
                request.getStayoverFrequency(),
                request.getTurndownEnabled(),
                request.getDndPolicy(),
                request.getDndMaxSkipDays(),
                request.getDailyTaskGenTime(),
                request.getOdTransitionTime()
        );

        return hkTaskMapper.toResponse(config);
    }

    // === 하우스키퍼 목록 ===

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

    // === 자동화 ===

    @Override
    @Transactional
    public void createTaskOnCheckout(Long propertyId, Long roomNumberId, Long reservationId) {
        // 설정 확인
        HkConfig config = hkConfigRepository.findByPropertyId(propertyId).orElse(null);
        if (config != null && !Boolean.TRUE.equals(config.getAutoCreateCheckout())) {
            return; // 자동 생성 비활성
        }

        // 같은 객실에 오늘 이미 활성 작업이 있으면 스킵 (취소된 작업은 제외)
        if (hkTaskRepository.existsActiveTaskByRoomNumberIdAndTaskDate(roomNumberId, LocalDate.now())) {
            return;
        }

        BigDecimal credit = config != null ? config.getDefaultCheckoutCredit() : new BigDecimal("1.0");

        HkTask task = HkTask.builder()
                .propertyId(propertyId)
                .roomNumberId(roomNumberId)
                .taskType("CHECKOUT")
                .taskDate(LocalDate.now())
                .priority("NORMAL")
                .credit(credit)
                .reservationId(reservationId)
                .build();

        // Rush 자동 판정 적용
        applyRushPriority(task, propertyId);

        hkTaskRepository.save(task);
        log.info("HK 자동 작업 생성 (체크아웃): propertyId={}, roomNumberId={}, priority={}",
                propertyId, roomNumberId, task.getPriority());
    }

    @Override
    @Transactional
    public int generateDailyTasks(Long propertyId, LocalDate date) {
        // 인가 검증은 호출자(API Controller)에서 수행 — 스케줄러/내부 서비스에서도 호출 가능
        LocalDate targetDate = date != null ? date : LocalDate.now();

        HkConfig config = hkConfigRepository.findByPropertyId(propertyId).orElse(null);
        BigDecimal checkoutCredit = config != null ? config.getDefaultCheckoutCredit() : new BigDecimal("1.0");
        BigDecimal stayoverCredit = config != null ? config.getDefaultStayoverCredit() : new BigDecimal("0.5");

        int createdCount = 0;

        // 1) VD(빈방+청소필요) 객실 → CHECKOUT 타입 작업 생성
        List<RoomNumber> vacantDirtyRooms = roomNumberRepository.findVacantDirtyRooms(propertyId);
        for (RoomNumber room : vacantDirtyRooms) {
            if (hkTaskRepository.existsActiveTaskByRoomNumberIdAndTaskDate(room.getId(), targetDate)) {
                continue; // 이미 오늘 활성 작업이 있으면 스킵 (취소된 작업 제외)
            }
            HkTask task = HkTask.builder()
                    .propertyId(propertyId)
                    .roomNumberId(room.getId())
                    .taskType("CHECKOUT")
                    .taskDate(targetDate)
                    .priority("NORMAL")
                    .credit(checkoutCredit)
                    .build();
            applyRushPriority(task, propertyId);
            hkTaskRepository.save(task);
            createdCount++;
        }

        // 2) OD(투숙중+청소필요) 객실 → STAYOVER 타입 작업 생성
        List<RoomNumber> occupiedDirtyRooms = roomNumberRepository.findOccupiedDirtyRooms(propertyId);
        for (RoomNumber room : occupiedDirtyRooms) {
            if (hkTaskRepository.existsActiveTaskByRoomNumberIdAndTaskDate(room.getId(), targetDate)) {
                continue; // 이미 오늘 활성 작업이 있으면 스킵 (취소된 작업 제외)
            }
            HkTask task = HkTask.builder()
                    .propertyId(propertyId)
                    .roomNumberId(room.getId())
                    .taskType("STAYOVER")
                    .taskDate(targetDate)
                    .priority("NORMAL")
                    .credit(stayoverCredit)
                    .build();
            hkTaskRepository.save(task);
            createdCount++;
        }

        log.info("HK 일일 작업 생성: propertyId={}, date={}, 생성={}건 (VD={}, OD={})",
                propertyId, targetDate, createdCount, vacantDirtyRooms.size(), occupiedDirtyRooms.size());
        return createdCount;
    }

    // === 스테이오버 자동화 ===

    @Override
    @Transactional
    public int transitionOccupiedRoomsToDirty(Long propertyId) {
        // OC 객실 → DIRTY 전환
        List<RoomNumber> ocRooms = roomNumberRepository.findOccupiedCleanRooms(propertyId);
        for (RoomNumber room : ocRooms) {
            room.updateHkStatus("DIRTY", null);
        }

        // DND 객실: 연속 일수 증가
        List<RoomNumber> dndRooms = roomNumberRepository.findByPropertyIdAndHkStatusOrderByRoomNumberAsc(
                propertyId, "DND");
        for (RoomNumber room : dndRooms) {
            if ("OCCUPIED".equals(room.getFoStatus())) {
                room.incrementDndDays();
            }
        }

        log.info("OC→OD 전환: propertyId={}, 전환={}건, DND 일수증가={}건",
                 propertyId, ocRooms.size(), dndRooms.size());
        return ocRooms.size();
    }

    @Override
    @Transactional
    public int generateStayoverTasks(Long propertyId, LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();

        // OD 객실 + roomTypeId 조회
        List<Object[]> odRooms = roomNumberRepository.findOccupiedDirtyRoomsWithRoomTypeId(propertyId);
        int created = 0;

        for (Object[] row : odRooms) {
            Long roomNumberId = ((Number) row[0]).longValue();
            Long roomTypeId = row[1] != null ? ((Number) row[1]).longValue() : null;

            // 정책 해석
            ResolvedCleaningPolicy policy = cleaningPolicyService.resolvePolicy(
                    propertyId, roomTypeId != null ? roomTypeId : 0L);

            if (!policy.isStayoverEnabled()) continue;

            // 이미 오늘 활성 작업이 있으면 스킵
            if (hkTaskRepository.existsActiveTaskByRoomNumberIdAndTaskDate(roomNumberId, targetDate)) continue;

            // frequency만큼 작업 생성
            for (int i = 0; i < policy.getStayoverFrequency(); i++) {
                String scheduledTime = calculateScheduledTime(i, policy.getStayoverFrequency());
                HkTask task = HkTask.builder()
                        .propertyId(propertyId)
                        .roomNumberId(roomNumberId)
                        .taskType("STAYOVER")
                        .taskDate(targetDate)
                        .priority(policy.getStayoverPriority())
                        .credit(policy.getStayoverCredit())
                        .scheduledTime(scheduledTime)
                        .build();
                applyRushPriority(task, propertyId);
                hkTaskRepository.save(task);
                created++;
            }
        }

        log.info("스테이오버 작업 생성: propertyId={}, date={}, 생성={}건", propertyId, targetDate, created);
        return created;
    }

    @Override
    @Transactional
    public Map<String, Integer> processDndRooms(Long propertyId, LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        List<RoomNumber> dndRooms = roomNumberRepository.findByPropertyIdAndHkStatusOrderByRoomNumberAsc(
                propertyId, "DND");
        int skipped = 0, retried = 0, forced = 0;

        for (RoomNumber room : dndRooms) {
            if (!"OCCUPIED".equals(room.getFoStatus())) continue;

            Long roomTypeId = roomNumberRepository.findRoomTypeIdByRoomNumberId(room.getId());
            ResolvedCleaningPolicy policy = cleaningPolicyService.resolvePolicy(
                    propertyId, roomTypeId != null ? roomTypeId : 0L);

            String dndPolicy = policy.getDndPolicy();
            if (dndPolicy == null) dndPolicy = "SKIP";

            switch (dndPolicy) {
                case "SKIP":
                    skipped++;
                    break;

                case "RETRY_AFTERNOON":
                    if (!hkTaskRepository.existsActiveTaskByRoomNumberIdAndTaskDate(room.getId(), targetDate)) {
                        HkTask task = HkTask.builder()
                                .propertyId(propertyId)
                                .roomNumberId(room.getId())
                                .taskType("STAYOVER")
                                .taskDate(targetDate)
                                .priority("NORMAL")
                                .credit(policy.getStayoverCredit())
                                .scheduledTime("14:00")
                                .dndSkipped(true)
                                .dndSkipCount(room.getConsecutiveDndDays() != null ? room.getConsecutiveDndDays() : 0)
                                .note("DND 오후 재시도")
                                .build();
                        hkTaskRepository.save(task);
                    }
                    retried++;
                    break;

                case "FORCE_AFTER_DAYS":
                    int maxDays = policy.getDndMaxSkipDays();
                    if (room.getConsecutiveDndDays() != null && room.getConsecutiveDndDays() >= maxDays) {
                        room.clearDnd();
                        if (!hkTaskRepository.existsActiveTaskByRoomNumberIdAndTaskDate(room.getId(), targetDate)) {
                            HkTask task = HkTask.builder()
                                    .propertyId(propertyId)
                                    .roomNumberId(room.getId())
                                    .taskType("STAYOVER")
                                    .taskDate(targetDate)
                                    .priority("HIGH")
                                    .credit(policy.getStayoverCredit())
                                    .scheduledTime("10:00")
                                    .note("DND " + maxDays + "일 초과 강제 청소")
                                    .build();
                            hkTaskRepository.save(task);
                        }
                        forced++;
                    } else {
                        skipped++;
                    }
                    break;

                default:
                    skipped++;
                    break;
            }
        }

        log.info("DND 처리: propertyId={}, 스킵={}, 재시도={}, 강제={}",
                 propertyId, skipped, retried, forced);
        return Map.of("skipped", skipped, "retried", retried, "forced", forced);
    }

    /**
     * 시간대 분배: frequency별 청소 시작 시간 계산
     */
    private String calculateScheduledTime(int index, int total) {
        if (total <= 1) return "10:00";
        int startHour = 9;
        int endHour = 18;
        int gap = (endHour - startHour) / total;
        int hour = startHour + (gap * index);
        return String.format("%02d:00", hour);
    }

    // === 이력 조회 ===

    @Override
    public List<HkTaskResponse> getHistory(Long propertyId, LocalDate from, LocalDate to, Long assignedTo) {
        accessControlService.validatePropertyAccess(propertyId);
        LocalDate fromDate = from != null ? from : LocalDate.now().minusDays(7);
        LocalDate toDate = to != null ? to : LocalDate.now();

        // 범위 쿼리 1회로 조회 (날짜별 반복 쿼리 제거)
        List<HkTask> allTasks = hkTaskRepository
                .findByPropertyIdAndTaskDateBetweenOrderByTaskDateDescCreatedAtDesc(propertyId, fromDate, toDate);

        List<HkTask> filtered = allTasks.stream()
                .filter(t -> assignedTo == null || assignedTo.equals(t.getAssignedTo()))
                .collect(Collectors.toList());

        return toResponseListWithDetails(filtered);
    }

    // === 모바일: 내 작업 ===

    @Override
    public List<HkTaskResponse> getMyTasks(Long userId, LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        List<HkTask> tasks = hkTaskRepository.findByAssignedToAndTaskDate(userId, targetDate);
        return toResponseListWithDetails(tasks);
    }

    @Override
    public HkMobileSummaryResponse getMySummary(Long userId, LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        List<HkTask> tasks = hkTaskRepository.findByAssignedToAndTaskDate(userId, targetDate);

        int total = tasks.size();
        int completed = (int) tasks.stream()
                .filter(t -> "COMPLETED".equals(t.getStatus()) || "INSPECTED".equals(t.getStatus()))
                .count();
        double rate = total > 0 ? (completed * 100.0 / total) : 0;

        BigDecimal totalCredits = tasks.stream()
                .filter(t -> "COMPLETED".equals(t.getStatus()) || "INSPECTED".equals(t.getStatus()))
                .map(HkTask::getCredit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Double avgDuration = tasks.stream()
                .filter(t -> t.getDurationMinutes() != null)
                .mapToInt(HkTask::getDurationMinutes)
                .average()
                .orElse(0);

        return HkMobileSummaryResponse.builder()
                .totalTasks(total)
                .completedTasks(completed)
                .completionRate(Math.round(rate * 10) / 10.0)
                .totalCredits(totalCredits)
                .avgDurationMinutes(avgDuration > 0 ? avgDuration : null)
                .build();
    }

    // === Private 헬퍼 ===

    /**
     * Rush(긴급) 자동 판정
     * 다음 체크인 예정 시간까지 남은 시간이 설정값(rushThresholdMinutes) 이하이면
     * 우선순위를 자동으로 RUSH(긴급)로 변경합니다.
     * 예: 설정이 120분이고, 다음 체크인이 2시간 이내이면 → 긴급 처리
     */
    private void applyRushPriority(HkTask task, Long propertyId) {
        // 이미 사용자가 RUSH로 지정했으면 스킵
        if ("RUSH".equals(task.getPriority())) return;

        // 다음 체크인 시간이 없으면 판정 불가
        if (task.getNextCheckinAt() == null) return;

        HkConfig config = hkConfigRepository.findByPropertyId(propertyId).orElse(null);
        int threshold = config != null ? config.getRushThresholdMinutes() : 120;

        long minutesUntilCheckin = java.time.Duration.between(
                LocalDateTime.now(), task.getNextCheckinAt()).toMinutes();

        if (minutesUntilCheckin >= 0 && minutesUntilCheckin <= threshold) {
            task.changePriority("RUSH");
        }
    }

    /**
     * 작업 유형별 기본 크레딧 조회
     * 설정에서 유형별 기준값을 가져옵니다.
     * (예: Checkout=1.0, Stayover=0.5, Deep Clean=2.0)
     */
    private BigDecimal getDefaultCreditByType(Long propertyId, String taskType) {
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

    private HkTask findTaskById(Long taskId) {
        return hkTaskRepository.findById(taskId)
                .orElseThrow(() -> new HolaException(ErrorCode.HK_TASK_NOT_FOUND));
    }

    /** createdBy → 마스킹된 이름 변환 (모바일: userId 숫자, PMS: loginId 문자열) */
    private String resolveCreatedByName(String createdBy) {
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

    private String getUserName(Long userId) {
        return adminUserRepository.findById(userId)
                .map(AdminUser::getUserName)
                .orElse(null);
    }

    private void logStatusChange(Long taskId, String fromStatus, String toStatus, String note) {
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
     * HkTask → HkTaskResponse 변환 (단건 조회 - 상세 조회 등에서 사용)
     */
    private HkTaskResponse toResponseWithDetails(HkTask task) {
        RoomNumber room = roomNumberRepository.findById(task.getRoomNumberId()).orElse(null);
        String roomNum = room != null ? room.getRoomNumber() : null;

        String assignedToName = task.getAssignedTo() != null ? getUserName(task.getAssignedTo()) : null;
        String inspectedByName = task.getInspectedBy() != null ? getUserName(task.getInspectedBy()) : null;

        return hkTaskMapper.toResponse(task, roomNum, null, null, assignedToName, inspectedByName);
    }

    /**
     * HkTask 리스트 → HkTaskResponse 리스트 변환 (배치 조회로 N+1 방지)
     * 객실/사용자 정보를 한 번에 조회하여 캐시 맵으로 활용
     */
    private List<HkTaskResponse> toResponseListWithDetails(List<HkTask> tasks) {
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

        // 오늘 출근부 조회 → 근태 상태 매핑
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
