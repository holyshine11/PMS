package com.hola.hotel.service;

import com.hola.hotel.dto.request.*;
import com.hola.hotel.dto.response.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 하우스키핑 서비스 인터페이스
 */
public interface HousekeepingService {

    // === 대시보드 ===
    HkDashboardResponse getDashboard(Long propertyId, LocalDate date);

    // === 작업 관리 ===
    List<HkTaskResponse> getTasks(Long propertyId, LocalDate date, String status, Long assignedTo, String taskType);
    HkTaskResponse getTask(Long taskId);
    HkTaskResponse createTask(Long propertyId, HkTaskCreateRequest request);
    HkTaskResponse updateTask(Long taskId, HkTaskUpdateRequest request);
    void assignTask(Long taskId, Long assignedTo);
    void unassignTask(Long taskId);
    void batchAssignTasks(Long propertyId, HkBatchAssignRequest request);
    void inspectTask(Long taskId);
    void cancelTask(Long taskId);

    // === 작업 상태 변경 (모바일) ===
    void startTask(Long taskId);
    /** 미배정 작업 시작 시 startedByUserId에게 자동 배정 */
    void startTask(Long taskId, Long startedByUserId);
    void pauseTask(Long taskId);
    void completeTask(Long taskId);

    // === 작업 시트 ===
    List<HkTaskSheetResponse> getTaskSheets(Long propertyId, LocalDate date);
    HkTaskSheetResponse generateTaskSheet(Long propertyId, LocalDate date, Long assignedTo, String sheetName);
    void deleteTaskSheet(Long sheetId);

    // === 이슈/메모 ===
    HkTaskIssueResponse createIssue(Long taskId, Long propertyId, Long roomNumberId, HkTaskIssueCreateRequest request);
    List<HkTaskIssueResponse> getTaskIssues(Long taskId);

    // === 설정 ===
    HkConfigResponse getConfig(Long propertyId);
    HkConfigResponse updateConfig(Long propertyId, HkConfigUpdateRequest request);

    // === 하우스키퍼 목록 ===
    List<HkDashboardResponse.HousekeeperSummary> getHousekeepers(Long propertyId);

    // === 작업 시트: 크레딧 균등 재분배 ===
    /** 하우스키퍼 간 작업량(크레딧)을 균등하게 재분배 */
    void redistributeTaskSheets(Long propertyId, LocalDate date);

    // === 자동화 ===
    void createTaskOnCheckout(Long propertyId, Long roomNumberId, Long reservationId);

    /** DIRTY 상태 객실을 스캔하여 미생성 작업을 일괄 생성. 생성된 작업 수 반환 */
    int generateDailyTasks(Long propertyId, LocalDate date);

    // === 스테이오버 자동화 ===

    /** OC→OD 일괄 전환 (매일 새벽 실행). SecurityContext 불필요. 전환된 객실 수 반환 */
    int transitionOccupiedRoomsToDirty(Long propertyId);

    /** 정책 기반 스테이오버 작업 자동 생성. SecurityContext 불필요. 생성된 작업 수 반환 */
    int generateStayoverTasks(Long propertyId, LocalDate date);

    /** DND 객실 정책 기반 처리. SecurityContext 불필요. 처리 결과 반환 */
    Map<String, Integer> processDndRooms(Long propertyId, LocalDate date);

    // === 이력 조회 ===
    List<HkTaskResponse> getHistory(Long propertyId, LocalDate from, LocalDate to, Long assignedTo);

    // === 모바일: 내 작업 ===
    List<HkTaskResponse> getMyTasks(Long userId, LocalDate date);
    HkMobileSummaryResponse getMySummary(Long userId, LocalDate date);
}
