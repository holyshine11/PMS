package com.hola.hotel.service;

import com.hola.hotel.entity.HkConfig;
import com.hola.hotel.repository.HkConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 하우스키핑 스케줄러
 * 매 분 실행하여 프로퍼티별 설정 시각에 맞춰 작업 자동 생성
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HkSchedulerService {

    private final HkConfigRepository hkConfigRepository;
    private final HousekeepingService housekeepingService;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * 매 분 실행 — 프로퍼티별 설정 시각에 맞춰 처리
     * 프로퍼티마다 odTransitionTime / dailyTaskGenTime이 다를 수 있으므로
     * 1분 단위로 체크하여 해당 시각에 맞는 프로퍼티만 처리
     */
    @Scheduled(cron = "0 * * * * *")
    public void processHousekeepingSchedule() {
        String now = LocalTime.now().format(TIME_FMT);
        List<HkConfig> configs = hkConfigRepository.findAll();

        for (HkConfig config : configs) {
            try {
                processPropertySchedule(config, now);
            } catch (Exception e) {
                // 한 프로퍼티 오류가 다른 프로퍼티에 영향 미치지 않도록
                log.error("HK 스케줄러 오류: propertyId={}, time={}, error={}",
                        config.getPropertyId(), now, e.getMessage(), e);
            }
        }
    }

    private void processPropertySchedule(HkConfig config, String currentTime) {
        Long propertyId = config.getPropertyId();
        LocalDate today = LocalDate.now();

        // 1) OC→OD 전환 시각
        String odTime = config.getOdTransitionTime() != null ? config.getOdTransitionTime() : "05:00";
        if (currentTime.equals(odTime)) {
            int converted = housekeepingService.transitionOccupiedRoomsToDirty(propertyId);
            if (converted > 0) {
                log.info("[스케줄러] OC→OD: propertyId={}, {}건", propertyId, converted);
            }
        }

        // 2) 일일 작업 생성 시각
        String genTime = config.getDailyTaskGenTime() != null ? config.getDailyTaskGenTime() : "06:00";
        if (currentTime.equals(genTime)) {
            // 스테이오버 작업 생성
            if (Boolean.TRUE.equals(config.getStayoverEnabled())) {
                int stayover = housekeepingService.generateStayoverTasks(propertyId, today);
                if (stayover > 0) {
                    log.info("[스케줄러] 스테이오버: propertyId={}, {}건", propertyId, stayover);
                }
            }

            // 기존 일일 작업 (VD→CHECKOUT 포함)
            int daily = housekeepingService.generateDailyTasks(propertyId, today);
            if (daily > 0) {
                log.info("[스케줄러] 일일작업: propertyId={}, {}건", propertyId, daily);
            }

            // DND 처리
            housekeepingService.processDndRooms(propertyId, today);
        }
    }
}
