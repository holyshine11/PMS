package com.hola.hotel.service;

import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.common.security.AccessControlService;
import com.hola.hotel.dto.request.HkCleaningPolicyRequest;
import com.hola.hotel.dto.response.HkCleaningPolicyResponse;
import com.hola.hotel.dto.response.ResolvedCleaningPolicy;
import com.hola.hotel.entity.HkCleaningPolicy;
import com.hola.hotel.entity.HkConfig;
import com.hola.hotel.repository.HkCleaningPolicyRepository;
import com.hola.hotel.repository.HkConfigRepository;
import com.hola.hotel.repository.RoomNumberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 청소 정책 서비스 구현체
 * - 프로퍼티 기본값(HkConfig) + 룸타입 오버라이드(HkCleaningPolicy) 병합 엔진
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HkCleaningPolicyServiceImpl implements HkCleaningPolicyService {

    private final HkCleaningPolicyRepository hkCleaningPolicyRepository;
    private final HkConfigRepository hkConfigRepository;
    private final RoomNumberRepository roomNumberRepository;
    private final AccessControlService accessControlService;

    // === 정책 해석 ===

    @Override
    public ResolvedCleaningPolicy resolvePolicy(Long propertyId, Long roomTypeId) {
        // accessControl 체크 없음: 스케줄러에서 SecurityContext 없이 호출됨
        // 호출자(API Controller)가 이미 인가 검증을 수행함

        HkConfig config = getOrCreateConfig(propertyId);
        HkCleaningPolicy override = hkCleaningPolicyRepository
                .findByPropertyIdAndRoomTypeId(propertyId, roomTypeId)
                .orElse(null);

        boolean overridden = override != null;

        return ResolvedCleaningPolicy.builder()
                .stayoverEnabled(pick(override, HkCleaningPolicy::getStayoverEnabled, config.getStayoverEnabled()))
                .stayoverFrequency(pick(override, HkCleaningPolicy::getStayoverFrequency, config.getStayoverFrequency()))
                .turndownEnabled(pick(override, HkCleaningPolicy::getTurndownEnabled, config.getTurndownEnabled()))
                .stayoverCredit(pick(override, HkCleaningPolicy::getStayoverCredit, config.getDefaultStayoverCredit()))
                .turndownCredit(pick(override, HkCleaningPolicy::getTurndownCredit, config.getDefaultTurndownCredit()))
                .stayoverPriority(pick(override, HkCleaningPolicy::getStayoverPriority, "NORMAL"))
                .dndPolicy(pick(override, HkCleaningPolicy::getDndPolicy, config.getDndPolicy()))
                .dndMaxSkipDays(pick(override, HkCleaningPolicy::getDndMaxSkipDays, config.getDndMaxSkipDays()))
                .overridden(overridden)
                .build();
    }

    // === 목록 조회 ===

    @Override
    public List<HkCleaningPolicyResponse> getAllPolicies(Long propertyId) {
        accessControlService.validatePropertyAccess(propertyId);

        // 프로퍼티의 모든 룸타입 조회 (크로스 모듈 네이티브 쿼리)
        List<Object[]> roomTypes = roomNumberRepository.findRoomTypesByPropertyId(propertyId);

        // 기존 오버라이드를 Map으로 변환
        Map<Long, HkCleaningPolicy> overrideMap = hkCleaningPolicyRepository
                .findByPropertyIdOrderBySortOrder(propertyId)
                .stream()
                .collect(Collectors.toMap(HkCleaningPolicy::getRoomTypeId, Function.identity()));

        return roomTypes.stream()
                .map(row -> {
                    Long roomTypeId = ((Number) row[0]).longValue();
                    String roomTypeCode = (String) row[1];
                    String description = (String) row[2];
                    HkCleaningPolicy override = overrideMap.get(roomTypeId);

                    return buildResponse(roomTypeId, roomTypeCode, description, override);
                })
                .collect(Collectors.toList());
    }

    // === 생성/수정 ===

    @Override
    @Transactional
    public HkCleaningPolicyResponse createOrUpdate(Long propertyId, HkCleaningPolicyRequest request) {
        accessControlService.validatePropertyAccess(propertyId);

        Long roomTypeId = request.getRoomTypeId();

        // 룸타입명/코드 조회
        Object[] roomTypeInfo = findRoomTypeInfo(propertyId, roomTypeId);
        String roomTypeCode = (String) roomTypeInfo[1];
        String description = (String) roomTypeInfo[2];

        HkCleaningPolicy policy = hkCleaningPolicyRepository
                .findByPropertyIdAndRoomTypeId(propertyId, roomTypeId)
                .map(existing -> {
                    // 기존 오버라이드 수정
                    existing.update(
                            request.getStayoverEnabled(),
                            request.getStayoverFrequency(),
                            request.getTurndownEnabled(),
                            request.getStayoverCredit(),
                            request.getTurndownCredit(),
                            request.getStayoverPriority(),
                            request.getDndPolicy(),
                            request.getDndMaxSkipDays(),
                            request.getNote()
                    );
                    return existing;
                })
                .orElseGet(() -> {
                    // 신규 오버라이드 생성
                    HkCleaningPolicy newPolicy = HkCleaningPolicy.builder()
                            .propertyId(propertyId)
                            .roomTypeId(roomTypeId)
                            .stayoverEnabled(request.getStayoverEnabled())
                            .stayoverFrequency(request.getStayoverFrequency())
                            .turndownEnabled(request.getTurndownEnabled())
                            .stayoverCredit(request.getStayoverCredit())
                            .turndownCredit(request.getTurndownCredit())
                            .stayoverPriority(request.getStayoverPriority())
                            .dndPolicy(request.getDndPolicy())
                            .dndMaxSkipDays(request.getDndMaxSkipDays())
                            .note(request.getNote())
                            .build();
                    return hkCleaningPolicyRepository.save(newPolicy);
                });

        return buildResponse(roomTypeId, roomTypeCode, description, policy);
    }

    // === 삭제 ===

    @Override
    @Transactional
    public void deletePolicy(Long propertyId, Long roomTypeId) {
        accessControlService.validatePropertyAccess(propertyId);

        HkCleaningPolicy policy = hkCleaningPolicyRepository
                .findByPropertyIdAndRoomTypeId(propertyId, roomTypeId)
                .orElseThrow(() -> new HolaException(ErrorCode.HK_CLEANING_POLICY_NOT_FOUND));

        policy.softDelete();
    }

    // === Private 헬퍼 ===

    /**
     * 오버라이드 필드 선택: override 값이 non-null이면 사용, 아니면 fallback
     */
    private <T> T pick(HkCleaningPolicy override, Function<HkCleaningPolicy, T> getter, T fallback) {
        if (override == null) {
            return fallback;
        }
        T value = getter.apply(override);
        return value != null ? value : fallback;
    }

    /**
     * HkConfig 조회 (없으면 기본값으로 생성)
     */
    private HkConfig getOrCreateConfig(Long propertyId) {
        return hkConfigRepository.findByPropertyId(propertyId)
                .orElseGet(() -> {
                    HkConfig newConfig = HkConfig.builder()
                            .propertyId(propertyId)
                            .build();
                    return hkConfigRepository.save(newConfig);
                });
    }

    /**
     * 룸타입 정보 조회 (네이티브 쿼리 결과에서 매칭)
     */
    private Object[] findRoomTypeInfo(Long propertyId, Long roomTypeId) {
        List<Object[]> roomTypes = roomNumberRepository.findRoomTypesByPropertyId(propertyId);
        return roomTypes.stream()
                .filter(row -> ((Number) row[0]).longValue() == roomTypeId.longValue())
                .findFirst()
                .orElseThrow(() -> new HolaException(ErrorCode.ROOM_TYPE_NOT_FOUND));
    }

    /**
     * 응답 DTO 빌드
     */
    private HkCleaningPolicyResponse buildResponse(Long roomTypeId, String roomTypeCode,
                                                     String description, HkCleaningPolicy override) {
        boolean overridden = override != null;
        return HkCleaningPolicyResponse.builder()
                .id(overridden ? override.getId() : null)
                .propertyId(overridden ? override.getPropertyId() : null)
                .roomTypeId(roomTypeId)
                .roomTypeName(description)
                .roomTypeCode(roomTypeCode)
                .stayoverEnabled(overridden ? override.getStayoverEnabled() : null)
                .stayoverFrequency(overridden ? override.getStayoverFrequency() : null)
                .turndownEnabled(overridden ? override.getTurndownEnabled() : null)
                .stayoverCredit(overridden ? override.getStayoverCredit() : null)
                .turndownCredit(overridden ? override.getTurndownCredit() : null)
                .stayoverPriority(overridden ? override.getStayoverPriority() : null)
                .dndPolicy(overridden ? override.getDndPolicy() : null)
                .dndMaxSkipDays(overridden ? override.getDndMaxSkipDays() : null)
                .note(overridden ? override.getNote() : null)
                .overridden(overridden)
                .build();
    }
}
