package com.hola.hotel.service;

import com.hola.hotel.dto.request.HkAttendanceRequest;
import com.hola.hotel.dto.request.HkSectionRequest;
import com.hola.hotel.dto.response.HkAttendanceResponse;
import com.hola.hotel.dto.response.HkDayOffResponse;
import com.hola.hotel.dto.response.HkMonthlyAttendanceResponse;
import com.hola.hotel.dto.response.HkSectionResponse;

import java.time.LocalDate;
import java.util.List;

/**
 * 하우스키핑 배정 관리 서비스 (구역, 출근부, 자동 배정)
 */
public interface HkAssignmentService {

    // === 구역 관리 ===
    List<HkSectionResponse> getSections(Long propertyId);
    HkSectionResponse getSection(Long sectionId);
    HkSectionResponse createSection(Long propertyId, HkSectionRequest request);
    HkSectionResponse updateSection(Long sectionId, HkSectionRequest request);
    void deleteSection(Long sectionId);

    // === 출근부 ===
    HkAttendanceResponse getAttendance(Long propertyId, LocalDate date);
    void saveAttendance(Long propertyId, HkAttendanceRequest request);

    // === 자동 배정 ===
    /** 구역 기반 자동 배정: 가용 인력 → 구역 매핑 → 크레딧 균등 폴백 */
    int autoAssign(Long propertyId, LocalDate date);

    // === 모바일 근태 ===
    /** 출근 처리 */
    void clockIn(Long propertyId, Long housekeeperId);
    /** 퇴근 처리 */
    void clockOut(Long propertyId, Long housekeeperId);
    /** 내 근태 상태 조회 */
    HkAttendanceResponse.AttendanceEntry getMyAttendanceStatus(Long propertyId, Long housekeeperId);

    // === 근태관리 캘린더 ===
    /** 월별 근태 현황 조회 */
    HkMonthlyAttendanceResponse getMonthlyAttendance(Long propertyId, int year, int month);
    /** 단일 근태 수정 */
    void updateSingleAttendance(Long attendanceId, String attendanceStatus, String clockInAt, String clockOutAt);

    // === 휴무일 관리 ===
    /** 월별 휴무일 조회 (전체 하우스키퍼) */
    List<HkDayOffResponse> getMonthlyDayOffs(Long propertyId, int year, int month);
    /** 휴무일 등록 (PMS: 즉시 APPROVED, 모바일: PENDING) */
    HkDayOffResponse createDayOff(Long propertyId, Long housekeeperId, LocalDate date, String note, boolean autoApprove);
    /** 휴무일 삭제 */
    void deleteDayOff(Long dayOffId);
    /** 휴무일 승인 */
    void approveDayOff(Long dayOffId);
    /** 휴무일 거절 */
    void rejectDayOff(Long dayOffId);
}
