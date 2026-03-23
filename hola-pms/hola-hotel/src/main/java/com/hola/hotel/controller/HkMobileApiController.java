package com.hola.hotel.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.common.auth.entity.AdminUser;
import com.hola.common.auth.repository.AdminUserRepository;
import com.hola.common.util.NameMaskingUtil;
import com.hola.hotel.dto.request.HkTaskIssueCreateRequest;
import com.hola.hotel.dto.request.HousekeeperUpdateRequest;
import com.hola.hotel.dto.response.*;
import com.hola.hotel.entity.HkDayOff;
import com.hola.hotel.entity.HkDailyAttendance;
import com.hola.hotel.repository.HkDayOffRepository;
import com.hola.hotel.repository.HkDailyAttendanceRepository;
import com.hola.hotel.service.HkAssignmentService;
import com.hola.hotel.service.HousekeeperService;
import com.hola.hotel.service.HousekeepingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 하우스키핑 모바일웹 REST API
 * HOUSEKEEPER/HOUSEKEEPING_SUPERVISOR 전용
 */
@Tag(name = "하우스키핑 모바일")
@RestController
@RequestMapping("/api/v1/properties/{propertyId}/hk-mobile")
@RequiredArgsConstructor
public class HkMobileApiController {

    private final HousekeepingService housekeepingService;
    private final HkAssignmentService hkAssignmentService;
    private final HousekeeperService housekeeperService;
    private final AdminUserRepository adminUserRepository;
    private final HkDayOffRepository hkDayOffRepository;
    private final HkDailyAttendanceRepository hkDailyAttendanceRepository;
    private final PasswordEncoder passwordEncoder;

    // === 내 작업 ===

    @Operation(summary = "내 작업 목록", description = "모바일 하우스키퍼 배정 작업 목록")
    @GetMapping("/my-tasks")
    public ResponseEntity<HolaResponse<List<HkTaskResponse>>> getMyTasks(
            @PathVariable Long propertyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpSession session) {
        Long userId = getSessionUserId(session);
        return ResponseEntity.ok(HolaResponse.success(housekeepingService.getMyTasks(userId, date)));
    }

    @Operation(summary = "작업 상세", description = "개별 작업 상세 조회")
    @GetMapping("/my-tasks/{taskId}")
    public ResponseEntity<HolaResponse<HkTaskResponse>> getMyTask(
            @PathVariable Long propertyId,
            @PathVariable Long taskId,
            HttpSession session) {
        // 본인 배정 작업만 조회 가능 (SUPERVISOR는 전체 가능)
        validateTaskOwnership(taskId, session);
        return ResponseEntity.ok(HolaResponse.success(housekeepingService.getTask(taskId)));
    }

    @Operation(summary = "작업 시작", description = "청소 작업 시작 (미배정 작업은 시작한 사용자에게 자동 배정)")
    @PutMapping("/my-tasks/{taskId}/start")
    public ResponseEntity<HolaResponse<Void>> startTask(
            @PathVariable Long propertyId,
            @PathVariable Long taskId,
            HttpSession session) {
        Long userId = getSessionUserId(session);
        String role = (String) session.getAttribute("hkUserRole");

        HkTaskResponse task = housekeepingService.getTask(taskId);

        // HOUSEKEEPER: 본인 배정 작업만 시작 가능 / SUPERVISOR: 전체 가능 (미배정 포함)
        if (!"HOUSEKEEPING_SUPERVISOR".equals(role)) {
            if (!userId.equals(task.getAssignedTo())) {
                throw new HolaException(ErrorCode.HK_TASK_ACCESS_DENIED);
            }
        }

        // 미배정 작업은 시작한 사용자에게 자동 배정
        housekeepingService.startTask(taskId, userId);
        return ResponseEntity.ok(HolaResponse.success());
    }

    @Operation(summary = "작업 일시중지", description = "청소 작업 일시중지")
    @PutMapping("/my-tasks/{taskId}/pause")
    public ResponseEntity<HolaResponse<Void>> pauseTask(
            @PathVariable Long propertyId,
            @PathVariable Long taskId,
            HttpSession session) {
        validateTaskOwnership(taskId, session);
        housekeepingService.pauseTask(taskId);
        return ResponseEntity.ok(HolaResponse.success());
    }

    @Operation(summary = "작업 완료", description = "청소 작업 완료 처리")
    @PutMapping("/my-tasks/{taskId}/complete")
    public ResponseEntity<HolaResponse<Void>> completeTask(
            @PathVariable Long propertyId,
            @PathVariable Long taskId,
            HttpSession session) {
        validateTaskOwnership(taskId, session);
        housekeepingService.completeTask(taskId);
        return ResponseEntity.ok(HolaResponse.success());
    }

    @Operation(summary = "예상 시간 수정", description = "작업 예상 완료 시간 수정")
    @PutMapping("/my-tasks/{taskId}/estimate")
    public ResponseEntity<HolaResponse<Void>> updateEstimate(
            @PathVariable Long propertyId,
            @PathVariable Long taskId,
            @RequestBody Map<String, String> body) {
        // 추후 구현 - estimatedEnd 업데이트
        return ResponseEntity.ok(HolaResponse.success());
    }

    // === 이슈/메모 ===

    @Operation(summary = "작업 이슈 목록", description = "작업 관련 이슈 조회")
    @GetMapping("/my-tasks/{taskId}/issues")
    public ResponseEntity<HolaResponse<List<HkTaskIssueResponse>>> getTaskIssues(
            @PathVariable Long propertyId,
            @PathVariable Long taskId,
            HttpSession session) {
        // 본인 배정 작업의 이슈만 조회 가능
        validateTaskOwnership(taskId, session);
        return ResponseEntity.ok(HolaResponse.success(housekeepingService.getTaskIssues(taskId)));
    }

    @Operation(summary = "이슈 등록", description = "작업 중 이슈 등록")
    @PostMapping("/my-tasks/{taskId}/issues")
    public ResponseEntity<HolaResponse<HkTaskIssueResponse>> createIssue(
            @PathVariable Long propertyId,
            @PathVariable Long taskId,
            @Valid @RequestBody HkTaskIssueCreateRequest request,
            HttpSession session) {
        // 본인 배정 작업에만 이슈 등록 가능
        validateTaskOwnership(taskId, session);
        HkTaskResponse task = housekeepingService.getTask(taskId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(HolaResponse.success(
                        housekeepingService.createIssue(taskId, propertyId, task.getRoomNumberId(), request)));
    }

    // === 일일 요약 ===

    @Operation(summary = "내 작업 요약", description = "모바일 하우스키퍼 작업 요약")
    @GetMapping("/my-summary")
    public ResponseEntity<HolaResponse<HkMobileSummaryResponse>> getMySummary(
            @PathVariable Long propertyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpSession session) {
        Long userId = getSessionUserId(session);
        return ResponseEntity.ok(HolaResponse.success(housekeepingService.getMySummary(userId, date)));
    }

    // === 휴무일 ===

    @Operation(summary = "내 휴무 목록", description = "휴무 신청 목록 조회")
    @GetMapping("/my-dayoffs")
    public ResponseEntity<HolaResponse<List<HkDayOffResponse>>> getMyDayOffs(
            @PathVariable Long propertyId,
            @RequestParam int year, @RequestParam int month,
            HttpSession session) {
        java.time.YearMonth ym = java.time.YearMonth.of(year, month);
        List<HkDayOff> dayOffs = hkDayOffRepository
                .findByPropertyIdAndDayOffDateBetween(propertyId, ym.atDay(1), ym.atEndOfMonth());
        List<HkDayOffResponse> responses = dayOffs.stream().map(d -> {
            AdminUser user = adminUserRepository.findById(d.getHousekeeperId()).orElse(null);
            return HkDayOffResponse.builder()
                    .id(d.getId()).propertyId(d.getPropertyId())
                    .housekeeperId(d.getHousekeeperId())
                    .userName(user != null ? NameMaskingUtil.maskKoreanName(user.getUserName()) : null)
                    .dayOffDate(d.getDayOffDate()).dayOffType(d.getDayOffType())
                    .status(d.getStatus()).note(d.getNote())
                    .createdAt(d.getCreatedAt()).build();
        }).collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(HolaResponse.success(responses));
    }

    @Operation(summary = "휴무 신청", description = "휴무일 신청")
    @Transactional
    @PostMapping("/my-dayoffs")
    public ResponseEntity<HolaResponse<HkDayOffResponse>> createMyDayOff(
            @PathVariable Long propertyId,
            @RequestBody Map<String, String> body,
            HttpSession session) {
        Long userId = getSessionUserId(session);
        LocalDate date = LocalDate.parse(body.get("date"));
        String note = body.get("note");
        // 출근 상태 검증: 이미 출근(WORKING/LEFT)한 날짜는 휴무 신청 불가
        hkDailyAttendanceRepository
                .findByPropertyIdAndAttendanceDateAndHousekeeperId(propertyId, date, userId)
                .ifPresent(att -> {
                    if ("WORKING".equals(att.getAttendanceStatus()) || "LEFT".equals(att.getAttendanceStatus())) {
                        throw new HolaException(ErrorCode.HK_DAYOFF_CONFLICT_WORKING);
                    }
                });
        // 중복 체크
        if (hkDayOffRepository.findByPropertyIdAndHousekeeperIdAndDayOffDate(propertyId, userId, date).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(HolaResponse.error("HOLA-0004", "이미 등록된 휴무일입니다."));
        }
        HkDayOff dayOff = HkDayOff.builder()
                .propertyId(propertyId).housekeeperId(userId).dayOffDate(date)
                .dayOffType("REQUESTED").status("PENDING").note(note)
                .createdBy(String.valueOf(userId)).build();
        HkDayOff saved = hkDayOffRepository.save(dayOff);
        AdminUser user = adminUserRepository.findById(userId).orElse(null);
        HkDayOffResponse response = HkDayOffResponse.builder()
                .id(saved.getId()).propertyId(propertyId).housekeeperId(userId)
                .userName(user != null ? NameMaskingUtil.maskKoreanName(user.getUserName()) : null)
                .dayOffDate(date).status("PENDING").build();
        return ResponseEntity.status(HttpStatus.CREATED).body(HolaResponse.success(response));
    }

    @Operation(summary = "휴무 신청 취소", description = "휴무 신청 철회")
    @Transactional
    @DeleteMapping("/my-dayoffs/{dayOffId}")
    public ResponseEntity<HolaResponse<Void>> deleteMyDayOff(
            @PathVariable Long propertyId, @PathVariable Long dayOffId,
            HttpSession session) {
        Long userId = getSessionUserId(session);
        // 본인의 휴무만 삭제 가능
        HkDayOff dayOff = hkDayOffRepository.findById(dayOffId)
                .orElseThrow(() -> new HolaException(ErrorCode.HK_DAYOFF_NOT_FOUND));
        if (!userId.equals(dayOff.getHousekeeperId())) {
            throw new HolaException(ErrorCode.HK_TASK_ACCESS_DENIED);
        }
        hkDayOffRepository.deleteById(dayOffId);
        return ResponseEntity.ok(HolaResponse.success());
    }

    // === 프로필 ===

    @Operation(summary = "내 정보 조회", description = "모바일 프로필 조회")
    @GetMapping("/my-profile")
    public ResponseEntity<HolaResponse<HousekeeperResponse>> getMyProfile(
            @PathVariable Long propertyId,
            HttpSession session) {
        Long userId = getSessionUserId(session);
        AdminUser user = adminUserRepository.findById(userId)
                .orElseThrow(() -> new HolaException(ErrorCode.ADMIN_NOT_FOUND));
        String roleLabel = "HOUSEKEEPING_SUPERVISOR".equals(user.getRole()) ? "감독자" : "청소 담당";
        HousekeeperResponse response = HousekeeperResponse.builder()
                .id(user.getId()).loginId(user.getLoginId()).userName(user.getUserName())
                .email(user.getEmail()).phone(user.getPhone()).department(user.getDepartment())
                .position(user.getPosition()).role(user.getRole()).roleLabel(roleLabel)
                .useYn(user.getUseYn()).build();
        return ResponseEntity.ok(HolaResponse.success(response));
    }

    @Operation(summary = "내 정보 수정", description = "모바일 프로필 수정")
    @Transactional
    @PutMapping("/my-profile")
    public ResponseEntity<HolaResponse<Void>> updateMyProfile(
            @PathVariable Long propertyId,
            @RequestBody Map<String, String> body,
            HttpSession session) {
        Long userId = getSessionUserId(session);
        AdminUser user = adminUserRepository.findById(userId)
                .orElseThrow(() -> new HolaException(ErrorCode.ADMIN_NOT_FOUND));
        // 모바일: 연락처/이메일만 수정 가능
        user.updateProfile(user.getUserName(), body.get("email"), body.get("phone"),
                user.getMobileCountryCode(), user.getMobile(), user.getPhoneCountryCode(),
                user.getDepartment(), user.getPosition(), null, null, user.getUseYn());
        return ResponseEntity.ok(HolaResponse.success());
    }

    @Operation(summary = "비밀번호 변경", description = "모바일 비밀번호 변경")
    @Transactional
    @PutMapping("/my-profile/change-password")
    public ResponseEntity<HolaResponse<Void>> changeMyPassword(
            @PathVariable Long propertyId,
            @RequestBody Map<String, String> body,
            HttpSession session) {
        Long userId = getSessionUserId(session);
        String currentPassword = body.get("currentPassword");
        String newPassword = body.get("newPassword");

        AdminUser user = adminUserRepository.findById(userId)
                .orElseThrow(() -> new HolaException(ErrorCode.ADMIN_NOT_FOUND));

        // 현재 비밀번호 검증
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPassword())) {
            return ResponseEntity.badRequest()
                    .body(HolaResponse.error("HOLA-0801", "현재 비밀번호가 일치하지 않습니다."));
        }

        if (newPassword == null || newPassword.length() < 10) {
            return ResponseEntity.badRequest()
                    .body(HolaResponse.error("HOLA-0802", "비밀번호는 10자 이상이어야 합니다."));
        }
        user.resetPassword(passwordEncoder.encode(newPassword));
        return ResponseEntity.ok(HolaResponse.success());
    }

    // === 근태 ===

    @Operation(summary = "내 근태", description = "오늘 출퇴근 현황")
    @GetMapping("/my-attendance")
    public ResponseEntity<HolaResponse<HkAttendanceResponse.AttendanceEntry>> getMyAttendance(
            @PathVariable Long propertyId,
            HttpSession session) {
        Long userId = getSessionUserId(session);
        return ResponseEntity.ok(HolaResponse.success(
                hkAssignmentService.getMyAttendanceStatus(propertyId, userId)));
    }

    @Operation(summary = "출근", description = "하우스키퍼 출근 처리")
    @PostMapping("/clock-in")
    public ResponseEntity<HolaResponse<Void>> clockIn(
            @PathVariable Long propertyId,
            HttpSession session) {
        Long userId = getSessionUserId(session);
        hkAssignmentService.clockIn(propertyId, userId);
        return ResponseEntity.ok(HolaResponse.success());
    }

    @Operation(summary = "퇴근", description = "하우스키퍼 퇴근 처리")
    @PostMapping("/clock-out")
    public ResponseEntity<HolaResponse<Void>> clockOut(
            @PathVariable Long propertyId,
            HttpSession session) {
        Long userId = getSessionUserId(session);
        hkAssignmentService.clockOut(propertyId, userId);
        return ResponseEntity.ok(HolaResponse.success());
    }

    // === 헬퍼 ===

    private Long getSessionUserId(HttpSession session) {
        Long userId = (Long) session.getAttribute("hkUserId");
        if (userId == null) {
            throw new HolaException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }

    /**
     * HOUSEKEEPER는 본인 배정 작업만 처리 가능
     * SUPERVISOR는 전체 처리 가능
     */
    private void validateTaskOwnership(Long taskId, HttpSession session) {
        String role = (String) session.getAttribute("hkUserRole");
        if ("HOUSEKEEPING_SUPERVISOR".equals(role)) return;

        Long userId = getSessionUserId(session);
        HkTaskResponse task = housekeepingService.getTask(taskId);
        if (!userId.equals(task.getAssignedTo())) {
            throw new HolaException(ErrorCode.HK_TASK_ACCESS_DENIED);
        }
    }
}
