package com.hola.hotel.service;

import com.hola.hotel.dto.request.*;
import com.hola.hotel.dto.response.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 하우스키핑 작업 관리 서비스 인터페이스
 * Task CRUD, 상태 변경, 작업 시트, 이슈/메모, 이력 조회, 모바일 작업
 */
public interface HkTaskService {

    // === 작업 CRUD ===
    List<HkTaskResponse> getTasks(Long propertyId, LocalDate date, String status, Long assignedTo, String taskType);
    HkTaskResponse getTask(Long taskId);
    HkTaskResponse createTask(Long propertyId, HkTaskCreateRequest request);
    HkTaskResponse updateTask(Long taskId, HkTaskUpdateRequest request);

    // === 작업 배정 ===
    void assignTask(Long taskId, Long assignedTo);
    void unassignTask(Long taskId);
    void batchAssignTasks(Long propertyId, HkBatchAssignRequest request);

    // === 작업 상태 변경 ===
    void inspectTask(Long taskId);
    void cancelTask(Long taskId);
    void startTask(Long taskId);
    /** 미배정 작업 시작 시 startedByUserId에게 자동 배정 */
    void startTask(Long taskId, Long startedByUserId);
    void pauseTask(Long taskId);
    void completeTask(Long taskId);

    // === 작업 시트 ===
    List<HkTaskSheetResponse> getTaskSheets(Long propertyId, LocalDate date);
    HkTaskSheetResponse generateTaskSheet(Long propertyId, LocalDate date, Long assignedTo, String sheetName);
    void deleteTaskSheet(Long sheetId);
    /** 하우스키퍼 간 작업량(크레딧)을 균등하게 재분배 */
    void redistributeTaskSheets(Long propertyId, LocalDate date);

    // === 이슈/메모 ===
    HkTaskIssueResponse createIssue(Long taskId, Long propertyId, Long roomNumberId, HkTaskIssueCreateRequest request);
    List<HkTaskIssueResponse> getTaskIssues(Long taskId);

    // === 이력 조회 ===
    List<HkTaskResponse> getHistory(Long propertyId, LocalDate from, LocalDate to, Long assignedTo);

    // === 모바일: 내 작업 ===
    List<HkTaskResponse> getMyTasks(Long userId, LocalDate date);
    HkMobileSummaryResponse getMySummary(Long userId, LocalDate date);
}
