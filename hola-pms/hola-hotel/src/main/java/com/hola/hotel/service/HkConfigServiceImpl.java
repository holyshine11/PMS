package com.hola.hotel.service;

import com.hola.common.security.AccessControlService;
import com.hola.hotel.dto.request.HkConfigUpdateRequest;
import com.hola.hotel.dto.response.HkConfigResponse;
import com.hola.hotel.dto.response.ResolvedCleaningPolicy;
import com.hola.hotel.entity.HkConfig;
import com.hola.hotel.entity.HkTask;
import com.hola.hotel.entity.RoomNumber;
import com.hola.hotel.mapper.HkTaskMapper;
import com.hola.hotel.repository.HkConfigRepository;
import com.hola.hotel.repository.HkTaskRepository;
import com.hola.hotel.repository.RoomNumberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 하우스키핑 설정 및 자동화 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HkConfigServiceImpl implements HkConfigService {

    private final HkConfigRepository hkConfigRepository;
    private final HkTaskRepository hkTaskRepository;
    private final RoomNumberRepository roomNumberRepository;
    private final AccessControlService accessControlService;
    private final HkTaskMapper hkTaskMapper;
    private final HkCleaningPolicyService cleaningPolicyService;
    private final HkHelper hkHelper;

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
        hkHelper.applyRushPriority(task, propertyId);

        hkTaskRepository.save(task);
        log.info("HK 자동 작업 생성 (체크아웃): propertyId={}, roomNumberId={}, priority={}",
                propertyId, roomNumberId, task.getPriority());
    }

    @Override
    @Transactional
    public int generateDailyTasks(Long propertyId, LocalDate date) {
        // 인가 검증은 호출자(API Controller)에서 수행 -- 스케줄러/내부 서비스에서도 호출 가능
        LocalDate targetDate = date != null ? date : LocalDate.now();

        HkConfig config = hkConfigRepository.findByPropertyId(propertyId).orElse(null);
        BigDecimal checkoutCredit = config != null ? config.getDefaultCheckoutCredit() : new BigDecimal("1.0");
        BigDecimal stayoverCredit = config != null ? config.getDefaultStayoverCredit() : new BigDecimal("0.5");

        int createdCount = 0;

        // 1) VD(빈방+청소필요) 객실 -> CHECKOUT 타입 작업 생성
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
            hkHelper.applyRushPriority(task, propertyId);
            hkTaskRepository.save(task);
            createdCount++;
        }

        // 2) OD(투숙중+청소필요) 객실 -> STAYOVER 타입 작업 생성
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
        // OC 객실 -> DIRTY 전환
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

        log.info("OC->OD 전환: propertyId={}, 전환={}건, DND 일수증가={}건",
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
                hkHelper.applyRushPriority(task, propertyId);
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
}
