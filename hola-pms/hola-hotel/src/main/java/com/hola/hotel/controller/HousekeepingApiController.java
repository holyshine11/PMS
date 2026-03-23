package com.hola.hotel.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.common.security.AccessControlService;
import com.hola.hotel.dto.request.*;
import com.hola.hotel.dto.response.*;
import com.hola.hotel.service.HkAssignmentService;
import com.hola.hotel.service.HousekeepingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 하우스키핑 관리자 REST API
 */
@Tag(name = "하우스키핑 관리")
@RestController
@RequestMapping("/api/v1/properties/{propertyId}/housekeeping")
@RequiredArgsConstructor
public class HousekeepingApiController {

    private final HousekeepingService housekeepingService;
    private final HkAssignmentService hkAssignmentService;
    private final AccessControlService accessControlService;

    // === 대시보드 ===

    @Operation(summary = "대시보드 조회", description = "상태별 작업 카운트 및 하우스키퍼별 집계")
    @GetMapping("/dashboard")
    public ResponseEntity<HolaResponse<HkDashboardResponse>> getDashboard(
            @PathVariable Long propertyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(housekeepingService.getDashboard(propertyId, date)));
    }

    // === 작업 관리 ===

    @Operation(summary = "작업 목록 조회", description = "날짜/상태/유형/담당자별 필터링")
    @GetMapping("/tasks")
    public ResponseEntity<HolaResponse<List<HkTaskResponse>>> getTasks(
            @PathVariable Long propertyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long assignedTo,
            @RequestParam(required = false) String taskType) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(
                housekeepingService.getTasks(propertyId, date, status, assignedTo, taskType)));
    }

    @Operation(summary = "작업 상세 조회", description = "작업 ID로 단건 조회")
    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<HolaResponse<HkTaskResponse>> getTask(
            @PathVariable Long propertyId,
            @PathVariable Long taskId) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(housekeepingService.getTask(taskId)));
    }

    @Operation(summary = "작업 생성", description = "청소 작업 수동 생성 (객실, 유형, 우선순위, 담당자)")
    @PostMapping("/tasks")
    public ResponseEntity<HolaResponse<HkTaskResponse>> createTask(
            @PathVariable Long propertyId,
            @Valid @RequestBody HkTaskCreateRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(HolaResponse.success(housekeepingService.createTask(propertyId, request)));
    }

    @Operation(summary = "작업 수정", description = "작업 유형, 우선순위, 크레딧, 담당자, 메모 수정")
    @PutMapping("/tasks/{taskId}")
    public ResponseEntity<HolaResponse<HkTaskResponse>> updateTask(
            @PathVariable Long propertyId,
            @PathVariable Long taskId,
            @Valid @RequestBody HkTaskUpdateRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(housekeepingService.updateTask(taskId, request)));
    }

    @Operation(summary = "작업 배정", description = "특정 작업에 담당자 배정")
    @PutMapping("/tasks/{taskId}/assign")
    public ResponseEntity<HolaResponse<Void>> assignTask(
            @PathVariable Long propertyId,
            @PathVariable Long taskId,
            @RequestBody Map<String, Long> body) {
        accessControlService.validatePropertyAccess(propertyId);
        housekeepingService.assignTask(taskId, body.get("assignedTo"));
        return ResponseEntity.ok(HolaResponse.success());
    }

    @Operation(summary = "작업 배정 해제", description = "작업의 담당자 배정을 해제 (미배정 상태로)")
    @PutMapping("/tasks/{taskId}/unassign")
    public ResponseEntity<HolaResponse<Void>> unassignTask(
            @PathVariable Long propertyId,
            @PathVariable Long taskId) {
        accessControlService.validatePropertyAccess(propertyId);
        housekeepingService.unassignTask(taskId);
        return ResponseEntity.ok(HolaResponse.success());
    }

    @Operation(summary = "일괄 배정", description = "여러 작업을 한 담당자에게 일괄 배정 (assignedTo=null이면 일괄 해제)")
    @PutMapping("/tasks/batch-assign")
    public ResponseEntity<HolaResponse<Void>> batchAssignTasks(
            @PathVariable Long propertyId,
            @Valid @RequestBody HkBatchAssignRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        housekeepingService.batchAssignTasks(propertyId, request);
        return ResponseEntity.ok(HolaResponse.success());
    }

    @Operation(summary = "작업 검수", description = "감독자가 완료된 작업 검수 처리")
    @PutMapping("/tasks/{taskId}/inspect")
    public ResponseEntity<HolaResponse<Void>> inspectTask(
            @PathVariable Long propertyId,
            @PathVariable Long taskId) {
        accessControlService.validatePropertyAccess(propertyId);
        housekeepingService.inspectTask(taskId);
        return ResponseEntity.ok(HolaResponse.success());
    }

    @Operation(summary = "작업 취소", description = "작업 취소 (soft delete)")
    @DeleteMapping("/tasks/{taskId}")
    public ResponseEntity<HolaResponse<Void>> cancelTask(
            @PathVariable Long propertyId,
            @PathVariable Long taskId) {
        accessControlService.validatePropertyAccess(propertyId);
        housekeepingService.cancelTask(taskId);
        return ResponseEntity.ok(HolaResponse.success());
    }

    // === 작업 상태 변경 ===

    @Operation(summary = "작업 시작", description = "PENDING/PAUSED → IN_PROGRESS")
    @PutMapping("/tasks/{taskId}/start")
    public ResponseEntity<HolaResponse<Void>> startTask(
            @PathVariable Long propertyId,
            @PathVariable Long taskId) {
        accessControlService.validatePropertyAccess(propertyId);
        housekeepingService.startTask(taskId);
        return ResponseEntity.ok(HolaResponse.success());
    }

    @Operation(summary = "작업 일시중단", description = "IN_PROGRESS → PAUSED")
    @PutMapping("/tasks/{taskId}/pause")
    public ResponseEntity<HolaResponse<Void>> pauseTask(
            @PathVariable Long propertyId,
            @PathVariable Long taskId) {
        accessControlService.validatePropertyAccess(propertyId);
        housekeepingService.pauseTask(taskId);
        return ResponseEntity.ok(HolaResponse.success());
    }

    @Operation(summary = "작업 완료", description = "IN_PROGRESS → COMPLETED (검수 필수 시 PICKUP, 아니면 CLEAN)")
    @PutMapping("/tasks/{taskId}/complete")
    public ResponseEntity<HolaResponse<Void>> completeTask(
            @PathVariable Long propertyId,
            @PathVariable Long taskId) {
        accessControlService.validatePropertyAccess(propertyId);
        housekeepingService.completeTask(taskId);
        return ResponseEntity.ok(HolaResponse.success());
    }

    // === 작업 시트 ===

    @Operation(summary = "작업 시트 목록", description = "하우스키퍼별 일일 작업 시트 조회")
    @GetMapping("/task-sheets")
    public ResponseEntity<HolaResponse<List<HkTaskSheetResponse>>> getTaskSheets(
            @PathVariable Long propertyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(housekeepingService.getTaskSheets(propertyId, date)));
    }

    @Operation(summary = "작업 시트 생성", description = "하우스키퍼에게 일일 작업 시트 생성")
    @PostMapping("/task-sheets/generate")
    public ResponseEntity<HolaResponse<HkTaskSheetResponse>> generateTaskSheet(
            @PathVariable Long propertyId,
            @RequestBody Map<String, Object> body) {
        accessControlService.validatePropertyAccess(propertyId);
        LocalDate date = body.get("date") != null ? LocalDate.parse(body.get("date").toString()) : LocalDate.now();
        Long assignedTo = body.get("assignedTo") != null ? Long.valueOf(body.get("assignedTo").toString()) : null;
        String sheetName = body.get("sheetName") != null ? body.get("sheetName").toString() : null;

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(HolaResponse.success(housekeepingService.generateTaskSheet(propertyId, date, assignedTo, sheetName)));
    }

    @Operation(summary = "작업 시트 삭제", description = "작업 시트 삭제 (soft delete)")
    @DeleteMapping("/task-sheets/{sheetId}")
    public ResponseEntity<HolaResponse<Void>> deleteTaskSheet(
            @PathVariable Long propertyId,
            @PathVariable Long sheetId) {
        accessControlService.validatePropertyAccess(propertyId);
        housekeepingService.deleteTaskSheet(sheetId);
        return ResponseEntity.ok(HolaResponse.success());
    }

    @Operation(summary = "크레딧 균등 재분배", description = "하우스키퍼 간 작업량(크레딧)을 균등하게 재분배")
    @PostMapping("/task-sheets/redistribute")
    public ResponseEntity<HolaResponse<Void>> redistributeTaskSheets(
            @PathVariable Long propertyId,
            @RequestBody(required = false) Map<String, String> body) {
        accessControlService.validatePropertyAccess(propertyId);
        LocalDate date = (body != null && body.get("date") != null)
                ? LocalDate.parse(body.get("date")) : LocalDate.now();
        housekeepingService.redistributeTaskSheets(propertyId, date);
        return ResponseEntity.ok(HolaResponse.success());
    }

    // === 이슈/메모 ===

    @Operation(summary = "이슈 등록", description = "작업에 이슈/메모 등록 (유지보수, 비품부족, 분실물, 파손 등)")
    @PostMapping("/tasks/{taskId}/issues")
    public ResponseEntity<HolaResponse<HkTaskIssueResponse>> createIssue(
            @PathVariable Long propertyId,
            @PathVariable Long taskId,
            @Valid @RequestBody HkTaskIssueCreateRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        HkTaskResponse task = housekeepingService.getTask(taskId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(HolaResponse.success(
                        housekeepingService.createIssue(taskId, propertyId, task.getRoomNumberId(), request)));
    }

    @Operation(summary = "이슈 목록 조회", description = "작업에 등록된 이슈/메모 목록")
    @GetMapping("/tasks/{taskId}/issues")
    public ResponseEntity<HolaResponse<List<HkTaskIssueResponse>>> getTaskIssues(
            @PathVariable Long propertyId,
            @PathVariable Long taskId) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(housekeepingService.getTaskIssues(taskId)));
    }

    // === 설정 ===

    @Operation(summary = "설정 조회", description = "프로퍼티별 하우스키핑 설정 조회")
    @GetMapping("/config")
    public ResponseEntity<HolaResponse<HkConfigResponse>> getConfig(@PathVariable Long propertyId) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(housekeepingService.getConfig(propertyId)));
    }

    @Operation(summary = "설정 저장", description = "검수 필수, 자동 생성, 크레딧 기본값, Rush 기준 등 설정")
    @PutMapping("/config")
    public ResponseEntity<HolaResponse<HkConfigResponse>> updateConfig(
            @PathVariable Long propertyId,
            @Valid @RequestBody HkConfigUpdateRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(housekeepingService.updateConfig(propertyId, request)));
    }

    // === 하우스키퍼 목록 ===

    @Operation(summary = "하우스키퍼 목록", description = "프로퍼티에 배정된 하우스키퍼 + 감독자 목록")
    @GetMapping("/housekeepers")
    public ResponseEntity<HolaResponse<List<HkDashboardResponse.HousekeeperSummary>>> getHousekeepers(
            @PathVariable Long propertyId) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(housekeepingService.getHousekeepers(propertyId)));
    }

    // === 이력 조회 ===

    @Operation(summary = "이력 조회", description = "날짜 범위 + 담당자 필터로 작업 이력 조회")
    @GetMapping("/history")
    public ResponseEntity<HolaResponse<List<HkTaskResponse>>> getHistory(
            @PathVariable Long propertyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long assignedTo) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(
                housekeepingService.getHistory(propertyId, from, to, assignedTo)));
    }

    // === 근태관리 캘린더 ===

    @Operation(summary = "월별 근태 현황", description = "행=하우스키퍼, 열=일자별 근태 상태")
    @GetMapping("/attendance/monthly")
    public ResponseEntity<HolaResponse<HkMonthlyAttendanceResponse>> getMonthlyAttendance(
            @PathVariable Long propertyId,
            @RequestParam int year,
            @RequestParam int month) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(
                hkAssignmentService.getMonthlyAttendance(propertyId, year, month)));
    }

    @Operation(summary = "단일 근태 수정", description = "특정 하우스키퍼의 특정 날짜 근태 수정")
    @PutMapping("/attendance/{attendanceId}")
    public ResponseEntity<HolaResponse<Void>> updateSingleAttendance(
            @PathVariable Long propertyId,
            @PathVariable Long attendanceId,
            @RequestBody Map<String, String> body) {
        accessControlService.validatePropertyAccess(propertyId);
        String status = body.get("attendanceStatus");
        String clockIn = body.get("clockInAt");
        String clockOut = body.get("clockOutAt");
        hkAssignmentService.updateSingleAttendance(attendanceId, status, clockIn, clockOut);
        return ResponseEntity.ok(HolaResponse.success());
    }

    // === 휴무일 관리 ===

    @Operation(summary = "월별 휴무일 조회")
    @GetMapping("/day-offs")
    public ResponseEntity<HolaResponse<List<HkDayOffResponse>>> getMonthlyDayOffs(
            @PathVariable Long propertyId,
            @RequestParam int year, @RequestParam int month) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(
                hkAssignmentService.getMonthlyDayOffs(propertyId, year, month)));
    }

    @Operation(summary = "휴무일 등록 (PMS: 즉시 승인)")
    @PostMapping("/day-offs")
    public ResponseEntity<HolaResponse<HkDayOffResponse>> createDayOff(
            @PathVariable Long propertyId, @RequestBody Map<String, String> body) {
        accessControlService.validatePropertyAccess(propertyId);
        Long hkId = Long.parseLong(body.get("housekeeperId"));
        LocalDate date = LocalDate.parse(body.get("date"));
        String note = body.get("note");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(HolaResponse.success(
                        hkAssignmentService.createDayOff(propertyId, hkId, date, note, true)));
    }

    @Operation(summary = "휴무일 삭제")
    @DeleteMapping("/day-offs/{dayOffId}")
    public ResponseEntity<HolaResponse<Void>> deleteDayOff(
            @PathVariable Long propertyId, @PathVariable Long dayOffId) {
        accessControlService.validatePropertyAccess(propertyId);
        hkAssignmentService.deleteDayOff(dayOffId);
        return ResponseEntity.ok(HolaResponse.success());
    }

    @Operation(summary = "휴무일 승인")
    @PutMapping("/day-offs/{dayOffId}/approve")
    public ResponseEntity<HolaResponse<Void>> approveDayOff(
            @PathVariable Long propertyId, @PathVariable Long dayOffId) {
        accessControlService.validatePropertyAccess(propertyId);
        hkAssignmentService.approveDayOff(dayOffId);
        return ResponseEntity.ok(HolaResponse.success());
    }

    @Operation(summary = "휴무일 거절")
    @PutMapping("/day-offs/{dayOffId}/reject")
    public ResponseEntity<HolaResponse<Void>> rejectDayOff(
            @PathVariable Long propertyId, @PathVariable Long dayOffId) {
        accessControlService.validatePropertyAccess(propertyId);
        hkAssignmentService.rejectDayOff(dayOffId);
        return ResponseEntity.ok(HolaResponse.success());
    }

    // === 구역 관리 ===

    @Operation(summary = "구역 목록 조회", description = "프로퍼티의 하우스키핑 구역 목록 (층/담당자 포함)")
    @GetMapping("/sections")
    public ResponseEntity<HolaResponse<List<HkSectionResponse>>> getSections(@PathVariable Long propertyId) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(hkAssignmentService.getSections(propertyId)));
    }

    @Operation(summary = "구역 상세 조회", description = "구역 ID로 상세 정보 조회")
    @GetMapping("/sections/{sectionId}")
    public ResponseEntity<HolaResponse<HkSectionResponse>> getSection(
            @PathVariable Long propertyId, @PathVariable Long sectionId) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(hkAssignmentService.getSection(sectionId)));
    }

    @Operation(summary = "구역 생성", description = "구역 생성 (이름, 층 매핑, 기본 담당자)")
    @PostMapping("/sections")
    public ResponseEntity<HolaResponse<HkSectionResponse>> createSection(
            @PathVariable Long propertyId, @Valid @RequestBody HkSectionRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(HolaResponse.success(hkAssignmentService.createSection(propertyId, request)));
    }

    @Operation(summary = "구역 수정", description = "구역 정보 및 층/담당자 매핑 수정")
    @PutMapping("/sections/{sectionId}")
    public ResponseEntity<HolaResponse<HkSectionResponse>> updateSection(
            @PathVariable Long propertyId, @PathVariable Long sectionId,
            @Valid @RequestBody HkSectionRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(hkAssignmentService.updateSection(sectionId, request)));
    }

    @Operation(summary = "구역 삭제", description = "구역 삭제 (soft delete)")
    @DeleteMapping("/sections/{sectionId}")
    public ResponseEntity<HolaResponse<Void>> deleteSection(
            @PathVariable Long propertyId, @PathVariable Long sectionId) {
        accessControlService.validatePropertyAccess(propertyId);
        hkAssignmentService.deleteSection(sectionId);
        return ResponseEntity.ok(HolaResponse.success());
    }

    // === 출근부 ===

    @Operation(summary = "출근부 조회", description = "일일 출근부 조회 (미설정 시 전체 가용으로 표시)")
    @GetMapping("/attendance")
    public ResponseEntity<HolaResponse<HkAttendanceResponse>> getAttendance(
            @PathVariable Long propertyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(hkAssignmentService.getAttendance(propertyId, date)));
    }

    @Operation(summary = "출근부 저장", description = "일일 출근부 저장 (가용 여부, 근무 시간대)")
    @PostMapping("/attendance")
    public ResponseEntity<HolaResponse<Void>> saveAttendance(
            @PathVariable Long propertyId, @Valid @RequestBody HkAttendanceRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        hkAssignmentService.saveAttendance(propertyId, request);
        return ResponseEntity.ok(HolaResponse.success());
    }

    // === 일일 작업 생성 ===

    @Operation(summary = "일일 작업 생성", description = "DIRTY 상태 객실을 스캔하여 HK 작업 일괄 생성 (VD→CHECKOUT, OD→STAYOVER)")
    @PostMapping("/generate-daily-tasks")
    public ResponseEntity<HolaResponse<Map<String, Object>>> generateDailyTasks(
            @PathVariable Long propertyId,
            @RequestBody(required = false) Map<String, String> body) {
        accessControlService.validatePropertyAccess(propertyId);
        LocalDate date = (body != null && body.get("date") != null)
                ? LocalDate.parse(body.get("date")) : LocalDate.now();
        int count = housekeepingService.generateDailyTasks(propertyId, date);
        return ResponseEntity.ok(HolaResponse.success(Map.of("createdCount", count)));
    }

    // === 자동 배정 ===

    @Operation(summary = "자동 배정", description = "구역 기반 자동 배정 (가용 인력 → 구역 매핑 → 크레딧 균등 폴백)")
    @PostMapping("/auto-assign")
    public ResponseEntity<HolaResponse<Map<String, Object>>> autoAssign(
            @PathVariable Long propertyId,
            @RequestBody(required = false) Map<String, String> body) {
        accessControlService.validatePropertyAccess(propertyId);
        LocalDate date = (body != null && body.get("date") != null)
                ? LocalDate.parse(body.get("date")) : LocalDate.now();
        int count = hkAssignmentService.autoAssign(propertyId, date);
        return ResponseEntity.ok(HolaResponse.success(Map.of("assignedCount", count)));
    }
}
