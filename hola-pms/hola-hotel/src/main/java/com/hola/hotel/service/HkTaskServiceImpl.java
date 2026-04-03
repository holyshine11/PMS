package com.hola.hotel.service;

import com.hola.common.auth.entity.AdminUser;
import com.hola.common.exception.ErrorCode;
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
import java.util.*;
import java.util.stream.Collectors;

/**
 * 하우스키핑 작업 관리 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HkTaskServiceImpl implements HkTaskService {

    private final HkTaskRepository hkTaskRepository;
    private final HkTaskSheetRepository hkTaskSheetRepository;
    private final HkTaskIssueRepository hkTaskIssueRepository;
    private final HkConfigRepository hkConfigRepository;
    private final RoomNumberRepository roomNumberRepository;
    private final AccessControlService accessControlService;
    private final HkTaskMapper hkTaskMapper;
    private final HkHelper hkHelper;

    // === 작업 CRUD ===

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

        return hkHelper.toResponseListWithDetails(filtered);
    }

    @Override
    public HkTaskResponse getTask(Long taskId) {
        HkTask task = hkHelper.findTaskById(taskId);
        return hkHelper.toResponseWithDetails(task);
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
                    hkHelper.getDefaultCreditByType(propertyId, task.getTaskType()), task.getNote());
        }

        // Rush 자동 판정: 다음 체크인까지 남은 시간이 기준 이하이면 긴급 처리
        hkHelper.applyRushPriority(task, propertyId);

        // 배정 처리
        if (request.getAssignedTo() != null) {
            AdminUser currentUser = accessControlService.getCurrentUser();
            task.assign(request.getAssignedTo(), currentUser.getId());
        }

        HkTask saved = hkTaskRepository.save(task);
        hkHelper.logStatusChange(saved.getId(), null, "PENDING", null);

        log.info("HK 작업 생성: taskId={}, roomNumber={}, type={}, priority={}",
                saved.getId(), room.getRoomNumber(), request.getTaskType(), saved.getPriority());
        return hkHelper.toResponseWithDetails(saved);
    }

    @Override
    @Transactional
    public HkTaskResponse updateTask(Long taskId, HkTaskUpdateRequest request) {
        HkTask task = hkHelper.findTaskById(taskId);

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

        return hkHelper.toResponseWithDetails(task);
    }

    // === 작업 배정 ===

    @Override
    @Transactional
    public void assignTask(Long taskId, Long assignedTo) {
        HkTask task = hkHelper.findTaskById(taskId);
        AdminUser currentUser = accessControlService.getCurrentUser();
        task.assign(assignedTo, currentUser.getId());
        log.info("HK 작업 배정: taskId={}, assignedTo={}", taskId, assignedTo);
    }

    @Override
    @Transactional
    public void unassignTask(Long taskId) {
        HkTask task = hkHelper.findTaskById(taskId);
        task.unassign();
        log.info("HK 작업 배정 해제: taskId={}", taskId);
    }

    @Override
    @Transactional
    public void batchAssignTasks(Long propertyId, HkBatchAssignRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        AdminUser currentUser = accessControlService.getCurrentUser();

        for (Long taskId : request.getTaskIds()) {
            HkTask task = hkHelper.findTaskById(taskId);
            if (request.getAssignedTo() != null) {
                task.assign(request.getAssignedTo(), currentUser.getId());
            } else {
                // assignedTo가 null이면 배정 해제
                task.unassign();
            }
        }
        log.info("HK 일괄 배정: taskIds={}, assignedTo={}", request.getTaskIds(), request.getAssignedTo());
    }

    // === 작업 상태 변경 ===

    @Override
    @Transactional
    public void inspectTask(Long taskId) {
        HkTask task = hkHelper.findTaskById(taskId);
        if (!"COMPLETED".equals(task.getStatus())) {
            throw new HolaException(ErrorCode.HK_TASK_STATUS_CHANGE_NOT_ALLOWED);
        }

        AdminUser currentUser = accessControlService.getCurrentUser();
        String prevStatus = task.getStatus();
        task.inspect(currentUser.getId());
        hkHelper.logStatusChange(taskId, prevStatus, "INSPECTED", null);

        // Room hkStatus -> INSPECTED -> CLEAN 자동 전환
        RoomNumber room = roomNumberRepository.findById(task.getRoomNumberId())
                .orElseThrow(() -> new HolaException(ErrorCode.ROOM_NUMBER_NOT_FOUND));
        room.updateHkStatus("CLEAN", null);

        log.info("HK 검수 완료: taskId={}, roomNumber={}", taskId, room.getRoomNumber());
    }

    @Override
    @Transactional
    public void cancelTask(Long taskId) {
        HkTask task = hkHelper.findTaskById(taskId);
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
        hkHelper.logStatusChange(taskId, prevStatus, "CANCELLED", null);
    }

    @Override
    @Transactional
    public void startTask(Long taskId) {
        startTask(taskId, null);
    }

    @Override
    @Transactional
    public void startTask(Long taskId, Long startedByUserId) {
        HkTask task = hkHelper.findTaskById(taskId);
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
        hkHelper.logStatusChange(taskId, prevStatus, "IN_PROGRESS", null);
    }

    @Override
    @Transactional
    public void pauseTask(Long taskId) {
        HkTask task = hkHelper.findTaskById(taskId);
        if (!"IN_PROGRESS".equals(task.getStatus())) {
            throw new HolaException(ErrorCode.HK_TASK_STATUS_CHANGE_NOT_ALLOWED);
        }
        String prevStatus = task.getStatus();
        task.pause();
        hkHelper.logStatusChange(taskId, prevStatus, "PAUSED", null);
    }

    @Override
    @Transactional
    public void completeTask(Long taskId) {
        HkTask task = hkHelper.findTaskById(taskId);
        if (!"IN_PROGRESS".equals(task.getStatus())) {
            throw new HolaException(ErrorCode.HK_TASK_STATUS_CHANGE_NOT_ALLOWED);
        }
        String prevStatus = task.getStatus();
        task.complete();
        hkHelper.logStatusChange(taskId, prevStatus, "COMPLETED", null);

        // Room hkStatus 변경
        RoomNumber room = roomNumberRepository.findById(task.getRoomNumberId())
                .orElseThrow(() -> new HolaException(ErrorCode.ROOM_NUMBER_NOT_FOUND));

        // 검수 필수 여부 확인
        HkConfig config = hkConfigRepository.findByPropertyId(task.getPropertyId()).orElse(null);
        boolean inspectionRequired = config != null && Boolean.TRUE.equals(config.getInspectionRequired());

        if (inspectionRequired) {
            // 검수 필수 -> PICKUP 상태 (감독자 검수 대기)
            room.updateHkStatus("PICKUP", null);
        } else {
            // 검수 생략 -> 즉시 CLEAN
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
                    String name = s.getAssignedTo() != null ? hkHelper.getUserName(s.getAssignedTo()) : null;
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
        String name = assignedTo != null ? hkHelper.getUserName(assignedTo) : null;
        return hkTaskMapper.toResponse(saved, name);
    }

    @Override
    @Transactional
    public void deleteTaskSheet(Long sheetId) {
        HkTaskSheet sheet = hkTaskSheetRepository.findById(sheetId)
                .orElseThrow(() -> new HolaException(ErrorCode.HK_TASK_SHEET_NOT_FOUND));
        sheet.softDelete();
    }

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

        // 크레딧 내림차순 정렬 (큰 작업부터 배정 -> 균형 잡기 쉬움)
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
                    // createdBy -> userName 조회 -> 마스킹
                    String createdByName = hkHelper.resolveCreatedByName(i.getCreatedBy());
                    return hkTaskMapper.toResponse(i, roomNum, createdByName);
                })
                .collect(Collectors.toList());
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

        return hkHelper.toResponseListWithDetails(filtered);
    }

    // === 모바일: 내 작업 ===

    @Override
    public List<HkTaskResponse> getMyTasks(Long userId, LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        List<HkTask> tasks = hkTaskRepository.findByAssignedToAndTaskDate(userId, targetDate);
        return hkHelper.toResponseListWithDetails(tasks);
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
}
