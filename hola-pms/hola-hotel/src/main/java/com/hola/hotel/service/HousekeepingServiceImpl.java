package com.hola.hotel.service;

import com.hola.hotel.dto.request.*;
import com.hola.hotel.dto.response.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 하우스키핑 서비스 구현체 (Facade)
 *
 * @deprecated 이 클래스는 하위 호환을 위한 위임 파사드입니다.
 * 실제 비즈니스 로직은 {@link HkDashboardServiceImpl}, {@link HkTaskServiceImpl}, {@link HkConfigServiceImpl}에 있습니다.
 * 신규 코드는 개별 서비스를 직접 주입하세요.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Deprecated
public class HousekeepingServiceImpl implements HousekeepingService {

    private final HkDashboardService hkDashboardService;
    private final HkTaskService hkTaskService;
    private final HkConfigService hkConfigService;

    // === 대시보드 ===

    @Override
    public HkDashboardResponse getDashboard(Long propertyId, LocalDate date) {
        return hkDashboardService.getDashboard(propertyId, date);
    }

    // === 작업 관리 ===

    @Override
    public List<HkTaskResponse> getTasks(Long propertyId, LocalDate date, String status,
                                         Long assignedTo, String taskType) {
        return hkTaskService.getTasks(propertyId, date, status, assignedTo, taskType);
    }

    @Override
    public HkTaskResponse getTask(Long taskId) {
        return hkTaskService.getTask(taskId);
    }

    @Override
    @Transactional
    public HkTaskResponse createTask(Long propertyId, HkTaskCreateRequest request) {
        return hkTaskService.createTask(propertyId, request);
    }

    @Override
    @Transactional
    public HkTaskResponse updateTask(Long taskId, HkTaskUpdateRequest request) {
        return hkTaskService.updateTask(taskId, request);
    }

    @Override
    @Transactional
    public void assignTask(Long taskId, Long assignedTo) {
        hkTaskService.assignTask(taskId, assignedTo);
    }

    @Override
    @Transactional
    public void unassignTask(Long taskId) {
        hkTaskService.unassignTask(taskId);
    }

    @Override
    @Transactional
    public void batchAssignTasks(Long propertyId, HkBatchAssignRequest request) {
        hkTaskService.batchAssignTasks(propertyId, request);
    }

    @Override
    @Transactional
    public void inspectTask(Long taskId) {
        hkTaskService.inspectTask(taskId);
    }

    @Override
    @Transactional
    public void cancelTask(Long taskId) {
        hkTaskService.cancelTask(taskId);
    }

    // === 작업 상태 변경 (모바일) ===

    @Override
    @Transactional
    public void startTask(Long taskId) {
        hkTaskService.startTask(taskId);
    }

    @Override
    @Transactional
    public void startTask(Long taskId, Long startedByUserId) {
        hkTaskService.startTask(taskId, startedByUserId);
    }

    @Override
    @Transactional
    public void pauseTask(Long taskId) {
        hkTaskService.pauseTask(taskId);
    }

    @Override
    @Transactional
    public void completeTask(Long taskId) {
        hkTaskService.completeTask(taskId);
    }

    // === 작업 시트 ===

    @Override
    public List<HkTaskSheetResponse> getTaskSheets(Long propertyId, LocalDate date) {
        return hkTaskService.getTaskSheets(propertyId, date);
    }

    @Override
    @Transactional
    public HkTaskSheetResponse generateTaskSheet(Long propertyId, LocalDate date,
                                                  Long assignedTo, String sheetName) {
        return hkTaskService.generateTaskSheet(propertyId, date, assignedTo, sheetName);
    }

    @Override
    @Transactional
    public void deleteTaskSheet(Long sheetId) {
        hkTaskService.deleteTaskSheet(sheetId);
    }

    // === 이슈/메모 ===

    @Override
    @Transactional
    public HkTaskIssueResponse createIssue(Long taskId, Long propertyId, Long roomNumberId,
                                            HkTaskIssueCreateRequest request) {
        return hkTaskService.createIssue(taskId, propertyId, roomNumberId, request);
    }

    @Override
    public List<HkTaskIssueResponse> getTaskIssues(Long taskId) {
        return hkTaskService.getTaskIssues(taskId);
    }

    // === 설정 ===

    @Override
    public HkConfigResponse getConfig(Long propertyId) {
        return hkConfigService.getConfig(propertyId);
    }

    @Override
    @Transactional
    public HkConfigResponse updateConfig(Long propertyId, HkConfigUpdateRequest request) {
        return hkConfigService.updateConfig(propertyId, request);
    }

    // === 하우스키퍼 목록 ===

    @Override
    public List<HkDashboardResponse.HousekeeperSummary> getHousekeepers(Long propertyId) {
        return hkDashboardService.getHousekeepers(propertyId);
    }

    // === 작업 시트: 크레딧 균등 재분배 ===

    @Override
    @Transactional
    public void redistributeTaskSheets(Long propertyId, LocalDate date) {
        hkTaskService.redistributeTaskSheets(propertyId, date);
    }

    // === 자동화 ===

    @Override
    @Transactional
    public void createTaskOnCheckout(Long propertyId, Long roomNumberId, Long reservationId) {
        hkConfigService.createTaskOnCheckout(propertyId, roomNumberId, reservationId);
    }

    @Override
    @Transactional
    public int generateDailyTasks(Long propertyId, LocalDate date) {
        return hkConfigService.generateDailyTasks(propertyId, date);
    }

    // === 스테이오버 자동화 ===

    @Override
    @Transactional
    public int transitionOccupiedRoomsToDirty(Long propertyId) {
        return hkConfigService.transitionOccupiedRoomsToDirty(propertyId);
    }

    @Override
    @Transactional
    public int generateStayoverTasks(Long propertyId, LocalDate date) {
        return hkConfigService.generateStayoverTasks(propertyId, date);
    }

    @Override
    @Transactional
    public Map<String, Integer> processDndRooms(Long propertyId, LocalDate date) {
        return hkConfigService.processDndRooms(propertyId, date);
    }

    // === 이력 조회 ===

    @Override
    public List<HkTaskResponse> getHistory(Long propertyId, LocalDate from, LocalDate to, Long assignedTo) {
        return hkTaskService.getHistory(propertyId, from, to, assignedTo);
    }

    // === 모바일: 내 작업 ===

    @Override
    public List<HkTaskResponse> getMyTasks(Long userId, LocalDate date) {
        return hkTaskService.getMyTasks(userId, date);
    }

    @Override
    public HkMobileSummaryResponse getMySummary(Long userId, LocalDate date) {
        return hkTaskService.getMySummary(userId, date);
    }
}
