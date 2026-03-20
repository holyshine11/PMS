package com.hola.hotel.service;

import com.hola.common.auth.entity.AdminUser;
import com.hola.common.auth.repository.AdminUserRepository;
import com.hola.common.auth.repository.AdminUserPropertyRepository;
import com.hola.common.exception.ErrorCode;
import com.hola.common.util.NameMaskingUtil;
import com.hola.common.exception.HolaException;
import com.hola.common.security.AccessControlService;
import com.hola.hotel.dto.request.HkAttendanceRequest;
import com.hola.hotel.dto.request.HkSectionRequest;
import com.hola.hotel.dto.response.HkAttendanceResponse;
import com.hola.hotel.dto.response.HkDayOffResponse;
import com.hola.hotel.dto.response.HkMonthlyAttendanceResponse;
import com.hola.hotel.dto.response.HkSectionResponse;
import com.hola.hotel.entity.*;
import com.hola.hotel.repository.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 하우스키핑 배정 관리 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HkAssignmentServiceImpl implements HkAssignmentService {

    private final HkSectionRepository hkSectionRepository;
    private final HkDailyAttendanceRepository hkDailyAttendanceRepository;
    private final HkDayOffRepository hkDayOffRepository;
    private final HkTaskRepository hkTaskRepository;
    private final RoomNumberRepository roomNumberRepository;
    private final FloorRepository floorRepository;
    private final AdminUserRepository adminUserRepository;
    private final AdminUserPropertyRepository adminUserPropertyRepository;
    private final AccessControlService accessControlService;
    private final EntityManager entityManager;

    // === 구역 관리 ===

    @Override
    public List<HkSectionResponse> getSections(Long propertyId) {
        accessControlService.validatePropertyAccess(propertyId);
        List<HkSection> sections = hkSectionRepository.findByPropertyIdOrderBySortOrder(propertyId);
        return sections.stream().map(this::toSectionResponse).collect(Collectors.toList());
    }

    @Override
    public HkSectionResponse getSection(Long sectionId) {
        HkSection section = findSectionById(sectionId);
        return toSectionResponse(section);
    }

    @Override
    @Transactional
    public HkSectionResponse createSection(Long propertyId, HkSectionRequest request) {
        accessControlService.validatePropertyAccess(propertyId);

        HkSection section = HkSection.builder()
                .propertyId(propertyId)
                .sectionName(request.getSectionName())
                .sectionCode(request.getSectionCode())
                .maxCredits(request.getMaxCredits())
                .build();

        HkSection saved = hkSectionRepository.save(section);

        // 층 매핑
        if (request.getFloorIds() != null) {
            List<HkSectionFloor> floors = request.getFloorIds().stream()
                    .map(fid -> HkSectionFloor.builder().sectionId(saved.getId()).floorId(fid).build())
                    .collect(Collectors.toList());
            saved.replaceFloors(floors);
        }

        // 담당자 매핑
        if (request.getHousekeeperIds() != null) {
            List<HkSectionHousekeeper> hks = request.getHousekeeperIds().stream()
                    .map(hid -> HkSectionHousekeeper.builder().sectionId(saved.getId()).housekeeperId(hid).isPrimary(true).build())
                    .collect(Collectors.toList());
            saved.replaceHousekeepers(hks);
        }

        log.info("HK 구역 생성: id={}, name={}", saved.getId(), saved.getSectionName());
        return toSectionResponse(saved);
    }

    @Override
    @Transactional
    public HkSectionResponse updateSection(Long sectionId, HkSectionRequest request) {
        HkSection section = findSectionById(sectionId);

        section.update(
                request.getSectionName() != null ? request.getSectionName() : section.getSectionName(),
                request.getSectionCode(),
                request.getMaxCredits()
        );

        // 층 매핑 교체 (orphanRemoval 충돌 방지: clear → flush → addAll)
        if (request.getFloorIds() != null) {
            section.getFloors().clear();
            entityManager.flush();
            List<HkSectionFloor> floors = request.getFloorIds().stream()
                    .map(fid -> HkSectionFloor.builder().sectionId(sectionId).floorId(fid).build())
                    .collect(Collectors.toList());
            section.getFloors().addAll(floors);
        }

        // 담당자 매핑 교체
        if (request.getHousekeeperIds() != null) {
            section.getHousekeepers().clear();
            entityManager.flush();
            List<HkSectionHousekeeper> hks = request.getHousekeeperIds().stream()
                    .map(hid -> HkSectionHousekeeper.builder().sectionId(sectionId).housekeeperId(hid).isPrimary(true).build())
                    .collect(Collectors.toList());
            section.getHousekeepers().addAll(hks);
        }

        log.info("HK 구역 수정: id={}, name={}", section.getId(), section.getSectionName());
        return toSectionResponse(section);
    }

    @Override
    @Transactional
    public void deleteSection(Long sectionId) {
        HkSection section = findSectionById(sectionId);
        section.softDelete();
        log.info("HK 구역 삭제: id={}", sectionId);
    }

    // === 출근부 ===

    @Override
    public HkAttendanceResponse getAttendance(Long propertyId, LocalDate date) {
        accessControlService.validatePropertyAccess(propertyId);
        LocalDate targetDate = date != null ? date : LocalDate.now();

        // 프로퍼티 소속 하우스키퍼 전체 목록
        List<AdminUser> allHousekeepers = getPropertyHousekeepers(propertyId);

        // 기존 출근부 조회
        List<HkDailyAttendance> existing = hkDailyAttendanceRepository
                .findByPropertyIdAndAttendanceDate(propertyId, targetDate);
        Map<Long, HkDailyAttendance> attendanceMap = existing.stream()
                .collect(Collectors.toMap(HkDailyAttendance::getHousekeeperId, a -> a));

        // 구역 매핑 (하우스키퍼 → 구역명)
        Map<Long, String> hkSectionMap = buildHousekeeperSectionMap(propertyId);

        boolean hasAttendance = !existing.isEmpty();

        List<HkAttendanceResponse.AttendanceEntry> entries = allHousekeepers.stream()
                .map(u -> {
                    HkDailyAttendance att = attendanceMap.get(u.getId());
                    return HkAttendanceResponse.AttendanceEntry.builder()
                            .housekeeperId(u.getId())
                            .userName(u.getUserName())
                            .role(u.getRole())
                            .isAvailable(att != null ? att.getIsAvailable() : !hasAttendance)
                            .shiftType(att != null ? att.getShiftType() : "DAY")
                            .note(att != null ? att.getNote() : null)
                            .sectionName(hkSectionMap.get(u.getId()))
                            .attendanceStatus(att != null ? att.getAttendanceStatus() : "BEFORE_WORK")
                            .clockInAt(att != null ? att.getClockInAt() : null)
                            .clockOutAt(att != null ? att.getClockOutAt() : null)
                            .build();
                })
                .collect(Collectors.toList());

        int availableCount = (int) entries.stream().filter(HkAttendanceResponse.AttendanceEntry::getIsAvailable).count();

        return HkAttendanceResponse.builder()
                .date(targetDate)
                .totalCount(entries.size())
                .availableCount(availableCount)
                .entries(entries)
                .build();
    }

    @Override
    @Transactional
    public void saveAttendance(Long propertyId, HkAttendanceRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        LocalDate targetDate = request.getDate();

        // 기존 데이터 조회
        List<HkDailyAttendance> existing = hkDailyAttendanceRepository
                .findByPropertyIdAndAttendanceDate(propertyId, targetDate);
        Map<Long, HkDailyAttendance> existingMap = existing.stream()
                .collect(Collectors.toMap(HkDailyAttendance::getHousekeeperId, a -> a));

        String currentLoginId = accessControlService.getCurrentLoginId();

        for (HkAttendanceRequest.AttendanceEntry entry : request.getEntries()) {
            HkDailyAttendance att = existingMap.get(entry.getHousekeeperId());
            if (att != null) {
                // 기존 데이터 업데이트
                att.update(entry.getIsAvailable(), entry.getShiftType(), entry.getNote());
            } else {
                // 신규 생성
                HkDailyAttendance newAtt = HkDailyAttendance.builder()
                        .propertyId(propertyId)
                        .attendanceDate(targetDate)
                        .housekeeperId(entry.getHousekeeperId())
                        .isAvailable(entry.getIsAvailable())
                        .shiftType(entry.getShiftType() != null ? entry.getShiftType() : "DAY")
                        .note(entry.getNote())
                        .createdBy(currentLoginId)
                        .updatedBy(currentLoginId)
                        .build();
                hkDailyAttendanceRepository.save(newAtt);
            }
        }

        log.info("HK 출근부 저장: propertyId={}, date={}, entries={}", propertyId, targetDate, request.getEntries().size());
    }

    // === 자동 배정 ===

    @Override
    @Transactional
    public int autoAssign(Long propertyId, LocalDate date) {
        accessControlService.validatePropertyAccess(propertyId);
        LocalDate targetDate = date != null ? date : LocalDate.now();

        // 1) 가용 인력 조회
        List<Long> availableHkIds = getAvailableHousekeeperIds(propertyId, targetDate);
        if (availableHkIds.isEmpty()) {
            throw new HolaException(ErrorCode.HK_NO_AVAILABLE_HOUSEKEEPER);
        }

        // 2) 미배정 + 활성 작업 조회
        List<HkTask> allTasks = hkTaskRepository.findByPropertyIdAndTaskDate(propertyId, targetDate);
        List<HkTask> unassigned = allTasks.stream()
                .filter(t -> t.getAssignedTo() == null)
                .filter(t -> !"CANCELLED".equals(t.getStatus()) && !"INSPECTED".equals(t.getStatus()))
                .collect(Collectors.toList());

        if (unassigned.isEmpty()) {
            log.info("HK 자동 배정: 미배정 작업 없음 (propertyId={}, date={})", propertyId, targetDate);
            return 0;
        }

        // 3) 구역 기반 매핑 구축: floorId → section → 기본 담당자
        Map<Long, List<Long>> floorToHousekeepers = buildFloorToHousekeeperMap(propertyId, availableHkIds);

        // 4) 인력별 현재 크레딧 초기화 (이미 배정된 작업 반영)
        Map<Long, BigDecimal> creditMap = new LinkedHashMap<>();
        for (Long hkId : availableHkIds) {
            creditMap.put(hkId, BigDecimal.ZERO);
        }
        // 이미 배정된 작업의 크레딧 반영
        allTasks.stream()
                .filter(t -> t.getAssignedTo() != null && creditMap.containsKey(t.getAssignedTo()))
                .forEach(t -> creditMap.merge(t.getAssignedTo(), t.getCredit(), BigDecimal::add));

        AdminUser currentUser = accessControlService.getCurrentUser();
        int assignedCount = 0;

        // 5) 크레딧 내림차순 정렬 (큰 작업부터 배정)
        unassigned.sort((a, b) -> b.getCredit().compareTo(a.getCredit()));

        for (HkTask task : unassigned) {
            Long targetHk = null;

            // 구역 기반 배정 시도
            Long floorId = roomNumberRepository.findFloorIdByRoomNumberId(task.getRoomNumberId());
            if (floorId != null && floorToHousekeepers.containsKey(floorId)) {
                List<Long> sectionHks = floorToHousekeepers.get(floorId);
                // 구역 담당자 중 크레딧이 가장 적은 사람
                targetHk = sectionHks.stream()
                        .min(Comparator.comparing(id -> creditMap.getOrDefault(id, BigDecimal.ZERO)))
                        .orElse(null);
            }

            // 구역 매핑 없으면 전체 가용 인력 중 크레딧 최소
            if (targetHk == null) {
                targetHk = availableHkIds.stream()
                        .min(Comparator.comparing(id -> creditMap.getOrDefault(id, BigDecimal.ZERO)))
                        .orElse(null);
            }

            if (targetHk != null) {
                task.assign(targetHk, currentUser.getId());
                creditMap.merge(targetHk, task.getCredit(), BigDecimal::add);
                assignedCount++;
            }
        }

        log.info("HK 자동 배정 완료: propertyId={}, date={}, 배정={}건, 가용인력={}명",
                propertyId, targetDate, assignedCount, availableHkIds.size());
        return assignedCount;
    }

    // === 모바일 근태 ===

    @Override
    @Transactional
    public void clockIn(Long propertyId, Long housekeeperId) {
        LocalDate today = LocalDate.now();

        // 승인된 휴무일이 있으면 출근 불가
        hkDayOffRepository.findByPropertyIdAndHousekeeperIdAndDayOffDate(propertyId, housekeeperId, today)
                .ifPresent(dayOff -> {
                    if ("APPROVED".equals(dayOff.getStatus())) {
                        throw new HolaException(ErrorCode.HK_CLOCKIN_DAYOFF_CONFLICT);
                    }
                });

        // 출근부가 없으면 자동 생성 (PMS에서 출근부를 미리 저장하지 않은 경우)
        // 동시 요청 시 UNIQUE 제약조건 위반 방지: save 실패 시 재조회
        HkDailyAttendance att = hkDailyAttendanceRepository
                .findByPropertyIdAndAttendanceDateAndHousekeeperId(propertyId, today, housekeeperId)
                .orElseGet(() -> {
                    try {
                        HkDailyAttendance newAtt = HkDailyAttendance.builder()
                                .propertyId(propertyId)
                                .attendanceDate(today)
                                .housekeeperId(housekeeperId)
                                .isAvailable(true)
                                .shiftType("DAY")
                                .createdBy(String.valueOf(housekeeperId))
                                .updatedBy(String.valueOf(housekeeperId))
                                .build();
                        HkDailyAttendance saved = hkDailyAttendanceRepository.saveAndFlush(newAtt);
                        return saved;
                    } catch (org.springframework.dao.DataIntegrityViolationException e) {
                        // 동시 요청으로 이미 생성된 경우 재조회
                        return hkDailyAttendanceRepository
                                .findByPropertyIdAndAttendanceDateAndHousekeeperId(propertyId, today, housekeeperId)
                                .orElseThrow(() -> new HolaException(ErrorCode.HK_ATTENDANCE_NOT_FOUND));
                    }
                });

        if ("WORKING".equals(att.getAttendanceStatus())) {
            throw new HolaException(ErrorCode.HK_ALREADY_CLOCKED_IN);
        }
        if ("LEFT".equals(att.getAttendanceStatus())) {
            throw new HolaException(ErrorCode.HK_ALREADY_CLOCKED_IN);
        }
        // PMS에서 휴무(isAvailable=false) 또는 DAY_OFF로 설정한 경우 출근 불가
        if ("DAY_OFF".equals(att.getAttendanceStatus()) || !Boolean.TRUE.equals(att.getIsAvailable())) {
            throw new HolaException(ErrorCode.HK_CLOCKIN_DAYOFF_CONFLICT);
        }
        att.clockIn();
        log.info("HK 출근: propertyId={}, housekeeperId={}", propertyId, housekeeperId);
    }

    @Override
    @Transactional
    public void clockOut(Long propertyId, Long housekeeperId) {
        LocalDate today = LocalDate.now();
        HkDailyAttendance att = hkDailyAttendanceRepository
                .findByPropertyIdAndAttendanceDateAndHousekeeperId(propertyId, today, housekeeperId)
                .orElseThrow(() -> new HolaException(ErrorCode.HK_ATTENDANCE_NOT_FOUND));

        if (!"WORKING".equals(att.getAttendanceStatus())) {
            throw new HolaException(ErrorCode.HK_NOT_WORKING);
        }
        att.clockOut();
        log.info("HK 퇴근: propertyId={}, housekeeperId={}", propertyId, housekeeperId);
    }

    @Override
    public HkAttendanceResponse.AttendanceEntry getMyAttendanceStatus(Long propertyId, Long housekeeperId) {
        LocalDate today = LocalDate.now();
        AdminUser user = adminUserRepository.findById(housekeeperId)
                .orElseThrow(() -> new HolaException(ErrorCode.ADMIN_NOT_FOUND));

        HkDailyAttendance att = hkDailyAttendanceRepository
                .findByPropertyIdAndAttendanceDateAndHousekeeperId(propertyId, today, housekeeperId)
                .orElse(null);

        Map<Long, String> sectionMap = buildHousekeeperSectionMap(propertyId);

        // 출근부 미등록 시 기본적으로 가용(true)으로 표시하여 모바일 출근 버튼 활성화
        return HkAttendanceResponse.AttendanceEntry.builder()
                .housekeeperId(housekeeperId)
                .userName(user.getUserName())
                .role(user.getRole())
                .isAvailable(att != null ? att.getIsAvailable() : true)
                .shiftType(att != null ? att.getShiftType() : "DAY")
                .attendanceStatus(att != null ? att.getAttendanceStatus() : "BEFORE_WORK")
                .clockInAt(att != null ? att.getClockInAt() : null)
                .clockOutAt(att != null ? att.getClockOutAt() : null)
                .sectionName(sectionMap.get(housekeeperId))
                .build();
    }

    // === 근태관리 캘린더 ===

    @Override
    public HkMonthlyAttendanceResponse getMonthlyAttendance(Long propertyId, int year, int month) {
        accessControlService.validatePropertyAccess(propertyId);
        YearMonth ym = YearMonth.of(year, month);
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();
        int daysInMonth = ym.lengthOfMonth();

        // 프로퍼티 소속 하우스키퍼
        List<AdminUser> housekeepers = getPropertyHousekeepers(propertyId);

        // 월간 출근부 전체 조회
        List<HkDailyAttendance> attendances = hkDailyAttendanceRepository
                .findByPropertyIdAndAttendanceDateBetween(propertyId, startDate, endDate);

        // housekeeperId + day → attendance 매핑
        Map<String, HkDailyAttendance> attMap = new HashMap<>();
        for (HkDailyAttendance a : attendances) {
            attMap.put(a.getHousekeeperId() + "_" + a.getAttendanceDate().getDayOfMonth(), a);
        }

        List<HkMonthlyAttendanceResponse.HousekeeperRow> rows = housekeepers.stream()
                .map(u -> {
                    List<HkMonthlyAttendanceResponse.DayCell> days = new ArrayList<>();
                    for (int d = 1; d <= daysInMonth; d++) {
                        HkDailyAttendance att = attMap.get(u.getId() + "_" + d);
                        days.add(HkMonthlyAttendanceResponse.DayCell.builder()
                                .attendanceId(att != null ? att.getId() : null)
                                .day(d)
                                .attendanceStatus(att != null ? att.getAttendanceStatus() : null)
                                .isAvailable(att != null ? att.getIsAvailable() : null)
                                .shiftType(att != null ? att.getShiftType() : null)
                                .clockInAt(att != null ? att.getClockInAt() : null)
                                .clockOutAt(att != null ? att.getClockOutAt() : null)
                                .build());
                    }
                    return HkMonthlyAttendanceResponse.HousekeeperRow.builder()
                            .housekeeperId(u.getId())
                            .userName(u.getUserName())
                            .days(days)
                            .build();
                })
                .collect(Collectors.toList());

        return HkMonthlyAttendanceResponse.builder()
                .year(year)
                .month(month)
                .daysInMonth(daysInMonth)
                .rows(rows)
                .build();
    }

    @Override
    @Transactional
    public void updateSingleAttendance(Long attendanceId, String attendanceStatus,
                                        String clockInAtStr, String clockOutAtStr) {
        HkDailyAttendance att = hkDailyAttendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new HolaException(ErrorCode.HK_ATTENDANCE_NOT_FOUND));

        // 시간 파싱 (HH:mm 형식 → 해당 날짜의 LocalDateTime으로 변환)
        LocalDateTime clockIn = att.getClockInAt();
        LocalDateTime clockOut = att.getClockOutAt();
        if (clockInAtStr != null && !clockInAtStr.isEmpty()) {
            String[] parts = clockInAtStr.split(":");
            clockIn = att.getAttendanceDate().atTime(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        }
        if (clockOutAtStr != null && !clockOutAtStr.isEmpty()) {
            String[] parts = clockOutAtStr.split(":");
            clockOut = att.getAttendanceDate().atTime(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        }

        // 상태 + 시간 반영
        att.updateAttendance(attendanceStatus != null ? attendanceStatus : att.getAttendanceStatus(), clockIn, clockOut);

        log.info("HK 근태 단일 수정: attendanceId={}, status={}", attendanceId, attendanceStatus);
    }

    // === 휴무일 관리 ===

    @Override
    public List<HkDayOffResponse> getMonthlyDayOffs(Long propertyId, int year, int month) {
        accessControlService.validatePropertyAccess(propertyId);
        YearMonth ym = YearMonth.of(year, month);
        List<HkDayOff> dayOffs = hkDayOffRepository
                .findByPropertyIdAndDayOffDateBetween(propertyId, ym.atDay(1), ym.atEndOfMonth());
        return dayOffs.stream().map(this::toDayOffResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public HkDayOffResponse createDayOff(Long propertyId, Long housekeeperId, LocalDate date,
                                          String note, boolean autoApprove) {
        // 출근 상태 검증: 이미 출근(WORKING/LEFT)한 날짜는 휴무 등록 불가
        hkDailyAttendanceRepository
                .findByPropertyIdAndAttendanceDateAndHousekeeperId(propertyId, date, housekeeperId)
                .ifPresent(att -> {
                    if ("WORKING".equals(att.getAttendanceStatus()) || "LEFT".equals(att.getAttendanceStatus())) {
                        throw new HolaException(ErrorCode.HK_DAYOFF_CONFLICT_WORKING);
                    }
                });
        // 중복 체크
        if (hkDayOffRepository.findByPropertyIdAndHousekeeperIdAndDayOffDate(propertyId, housekeeperId, date).isPresent()) {
            throw new HolaException(ErrorCode.DUPLICATE_RESOURCE);
        }

        String currentLoginId = accessControlService.getCurrentLoginId();
        HkDayOff dayOff = HkDayOff.builder()
                .propertyId(propertyId)
                .housekeeperId(housekeeperId)
                .dayOffDate(date)
                .dayOffType(autoApprove ? "APPROVED" : "REQUESTED")
                .status(autoApprove ? "APPROVED" : "PENDING")
                .note(note)
                .createdBy(currentLoginId)
                .build();

        if (autoApprove) {
            dayOff.approve(currentLoginId);
        }

        HkDayOff saved = hkDayOffRepository.save(dayOff);
        log.info("HK 휴무일 등록: propertyId={}, hkId={}, date={}, autoApprove={}", propertyId, housekeeperId, date, autoApprove);
        return toDayOffResponse(saved);
    }

    @Override
    @Transactional
    public void deleteDayOff(Long dayOffId) {
        hkDayOffRepository.deleteById(dayOffId);
        log.info("HK 휴무일 삭제: id={}", dayOffId);
    }

    @Override
    @Transactional
    public void approveDayOff(Long dayOffId) {
        HkDayOff dayOff = hkDayOffRepository.findById(dayOffId)
                .orElseThrow(() -> new HolaException(ErrorCode.RESOURCE_NOT_FOUND));
        // 해당 날짜에 출근 기록(WORKING/LEFT)이 있으면 승인 불가
        hkDailyAttendanceRepository
                .findByPropertyIdAndAttendanceDateAndHousekeeperId(
                        dayOff.getPropertyId(), dayOff.getDayOffDate(), dayOff.getHousekeeperId())
                .ifPresent(att -> {
                    if ("WORKING".equals(att.getAttendanceStatus()) || "LEFT".equals(att.getAttendanceStatus())) {
                        throw new HolaException(ErrorCode.HK_DAYOFF_CONFLICT_APPROVE);
                    }
                });
        dayOff.approve(accessControlService.getCurrentLoginId());
        log.info("HK 휴무일 승인: id={}", dayOffId);
    }

    @Override
    @Transactional
    public void rejectDayOff(Long dayOffId) {
        HkDayOff dayOff = hkDayOffRepository.findById(dayOffId)
                .orElseThrow(() -> new HolaException(ErrorCode.RESOURCE_NOT_FOUND));
        dayOff.reject(accessControlService.getCurrentLoginId());
        log.info("HK 휴무일 거절: id={}", dayOffId);
    }

    private HkDayOffResponse toDayOffResponse(HkDayOff d) {
        AdminUser user = adminUserRepository.findById(d.getHousekeeperId()).orElse(null);
        return HkDayOffResponse.builder()
                .id(d.getId())
                .propertyId(d.getPropertyId())
                .housekeeperId(d.getHousekeeperId())
                .userName(user != null ? NameMaskingUtil.maskKoreanName(user.getUserName()) : null)
                .dayOffDate(d.getDayOffDate())
                .dayOffType(d.getDayOffType())
                .status(d.getStatus())
                .note(d.getNote())
                .approvedBy(d.getApprovedBy())
                .approvedAt(d.getApprovedAt())
                .createdAt(d.getCreatedAt())
                .build();
    }

    // === Private 헬퍼 ===

    private HkSection findSectionById(Long sectionId) {
        return hkSectionRepository.findById(sectionId)
                .orElseThrow(() -> new HolaException(ErrorCode.HK_SECTION_NOT_FOUND));
    }

    /**
     * 가용 하우스키퍼 ID 목록 조회
     * 출근부가 있으면 → 출근자만, 없으면 → 전체 하우스키퍼
     */
    private List<Long> getAvailableHousekeeperIds(Long propertyId, LocalDate date) {
        if (hkDailyAttendanceRepository.existsByPropertyIdAndAttendanceDate(propertyId, date)) {
            return hkDailyAttendanceRepository.findAvailableHousekeeperIds(propertyId, date);
        }
        // 출근부 미설정 → 전체 활성 하우스키퍼
        return getPropertyHousekeepers(propertyId).stream()
                .map(AdminUser::getId)
                .collect(Collectors.toList());
    }

    /**
     * 프로퍼티 소속 하우스키퍼 + 감독자 목록
     */
    private List<AdminUser> getPropertyHousekeepers(Long propertyId) {
        List<Long> userIds = adminUserPropertyRepository.findAdminUserIdsByPropertyId(propertyId);
        if (userIds.isEmpty()) return Collections.emptyList();

        // 배치 조회로 N+1 방지
        return adminUserRepository.findAllById(userIds).stream()
                .filter(u -> "HOUSEKEEPER".equals(u.getRole()) || "HOUSEKEEPING_SUPERVISOR".equals(u.getRole()))
                .filter(u -> Boolean.TRUE.equals(u.getUseYn()))
                .collect(Collectors.toList());
    }

    /**
     * floorId → 가용한 구역 담당자 목록 매핑
     */
    private Map<Long, List<Long>> buildFloorToHousekeeperMap(Long propertyId, List<Long> availableHkIds) {
        Set<Long> availableSet = new HashSet<>(availableHkIds);
        Map<Long, List<Long>> result = new HashMap<>();

        List<HkSection> sections = hkSectionRepository.findByPropertyIdOrderBySortOrder(propertyId);
        for (HkSection section : sections) {
            // 구역의 가용 담당자 필터링
            List<Long> sectionHks = section.getHousekeepers().stream()
                    .map(HkSectionHousekeeper::getHousekeeperId)
                    .filter(availableSet::contains)
                    .collect(Collectors.toList());

            if (!sectionHks.isEmpty()) {
                for (HkSectionFloor sf : section.getFloors()) {
                    result.put(sf.getFloorId(), sectionHks);
                }
            }
        }
        return result;
    }

    /**
     * 하우스키퍼 → 소속 구역명 매핑
     */
    private Map<Long, String> buildHousekeeperSectionMap(Long propertyId) {
        Map<Long, String> result = new HashMap<>();
        List<HkSection> sections = hkSectionRepository.findByPropertyIdOrderBySortOrder(propertyId);
        for (HkSection section : sections) {
            for (HkSectionHousekeeper shk : section.getHousekeepers()) {
                result.put(shk.getHousekeeperId(), section.getSectionName());
            }
        }
        return result;
    }

    /**
     * HkSection → HkSectionResponse 변환
     */
    private HkSectionResponse toSectionResponse(HkSection section) {
        List<HkSectionResponse.FloorInfo> floorInfos = section.getFloors().stream()
                .map(sf -> {
                    Floor floor = floorRepository.findById(sf.getFloorId()).orElse(null);
                    return HkSectionResponse.FloorInfo.builder()
                            .id(sf.getFloorId())
                            .floorNumber(floor != null ? floor.getFloorNumber() : null)
                            .floorName(floor != null ? floor.getFloorName() : null)
                            .build();
                })
                .collect(Collectors.toList());

        List<HkSectionResponse.HousekeeperInfo> hkInfos = section.getHousekeepers().stream()
                .map(shk -> {
                    AdminUser user = adminUserRepository.findById(shk.getHousekeeperId()).orElse(null);
                    return HkSectionResponse.HousekeeperInfo.builder()
                            .id(shk.getHousekeeperId())
                            .userName(user != null ? user.getUserName() : null)
                            .isPrimary(shk.getIsPrimary())
                            .build();
                })
                .collect(Collectors.toList());

        return HkSectionResponse.builder()
                .id(section.getId())
                .sectionName(section.getSectionName())
                .sectionCode(section.getSectionCode())
                .maxCredits(section.getMaxCredits())
                .floors(floorInfos)
                .housekeepers(hkInfos)
                .build();
    }
}
