package com.hola.hotel.service;

import com.hola.hotel.dto.request.HkConfigUpdateRequest;
import com.hola.hotel.dto.response.HkConfigResponse;

import java.time.LocalDate;
import java.util.Map;

/**
 * 하우스키핑 설정 및 자동화 서비스 인터페이스
 * 설정 관리, 자동 작업 생성(체크아웃/일일/스테이오버), DND 처리
 */
public interface HkConfigService {

    // === 설정 ===
    HkConfigResponse getConfig(Long propertyId);
    HkConfigResponse updateConfig(Long propertyId, HkConfigUpdateRequest request);

    // === 자동화 ===
    void createTaskOnCheckout(Long propertyId, Long roomNumberId, Long reservationId);

    /** DIRTY 상태 객실을 스캔하여 미생성 작업을 일괄 생성. 생성된 작업 수 반환 */
    int generateDailyTasks(Long propertyId, LocalDate date);

    // === 스테이오버 자동화 ===

    /** OC->OD 일괄 전환 (매일 새벽 실행). SecurityContext 불필요. 전환된 객실 수 반환 */
    int transitionOccupiedRoomsToDirty(Long propertyId);

    /** 정책 기반 스테이오버 작업 자동 생성. SecurityContext 불필요. 생성된 작업 수 반환 */
    int generateStayoverTasks(Long propertyId, LocalDate date);

    /** DND 객실 정책 기반 처리. SecurityContext 불필요. 처리 결과 반환 */
    Map<String, Integer> processDndRooms(Long propertyId, LocalDate date);
}
