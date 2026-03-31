package com.hola.reservation.service;

import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.reservation.dto.request.RoomUpgradeRequest;
import com.hola.reservation.dto.response.DailyChargeDiff;
import com.hola.reservation.dto.response.RoomUpgradeHistoryResponse;
import com.hola.reservation.dto.response.UpgradeAvailableTypeResponse;
import com.hola.reservation.dto.response.UpgradePreviewResponse;
import com.hola.reservation.entity.DailyCharge;
import com.hola.reservation.entity.ReservationServiceItem;
import com.hola.reservation.entity.RoomUpgradeHistory;
import com.hola.reservation.entity.SubReservation;
import com.hola.reservation.repository.DailyChargeRepository;
import com.hola.reservation.repository.ReservationServiceItemRepository;
import com.hola.reservation.repository.RoomUpgradeHistoryRepository;
import com.hola.reservation.repository.SubReservationRepository;
import com.hola.rate.entity.RateCode;
import com.hola.rate.entity.RateCodeRoomType;
import com.hola.rate.repository.RateCodeRepository;
import com.hola.rate.repository.RateCodeRoomTypeRepository;
import com.hola.room.entity.RoomType;
import com.hola.room.entity.TransactionCode;
import com.hola.room.repository.RoomTypeRepository;
import com.hola.room.repository.TransactionCodeRepository;
import com.hola.reservation.service.RoomAvailabilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomUpgradeServiceImpl implements RoomUpgradeService {

    private final SubReservationRepository subReservationRepository;
    private final DailyChargeRepository dailyChargeRepository;
    private final RoomUpgradeHistoryRepository upgradeHistoryRepository;
    private final ReservationServiceItemRepository serviceItemRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final TransactionCodeRepository transactionCodeRepository;
    private final RateCodeRoomTypeRepository rateCodeRoomTypeRepository;
    private final RateCodeRepository rateCodeRepository;
    private final PriceCalculationService priceCalculationService;
    private final RoomAvailabilityService roomAvailabilityService;

    // 업그레이드 가능 상태
    private static final Set<String> UPGRADEABLE_STATUSES = Set.of("RESERVED", "CHECK_IN", "INHOUSE");

    @Override
    public List<UpgradeAvailableTypeResponse> getAvailableTypes(Long subReservationId) {
        SubReservation sub = findSubReservation(subReservationId);
        Long propertyId = sub.getMasterReservation().getProperty().getId();
        Long currentRoomTypeId = sub.getRoomTypeId();
        Long currentRateCodeId = sub.getMasterReservation().getRateCodeId();
        LocalDate checkIn = sub.getCheckIn();
        LocalDate checkOut = sub.getCheckOut();

        // 같은 프로퍼티의 전체 객실타입 중 현재 타입 제외 + 요금 적용 가능한 타입만
        return roomTypeRepository.findAllByPropertyIdOrderBySortOrderAscRoomTypeCodeAsc(propertyId)
                .stream()
                .filter(rt -> !rt.getId().equals(currentRoomTypeId))
                .filter(rt -> Boolean.TRUE.equals(rt.getUseYn()))
                .filter(rt -> hasApplicableRate(rt.getId(), currentRateCodeId, checkIn, checkOut))
                .map(rt -> UpgradeAvailableTypeResponse.builder()
                        .roomTypeId(rt.getId())
                        .roomTypeCode(rt.getRoomTypeCode())
                        .description(rt.getDescription())
                        .maxAdults(rt.getMaxAdults())
                        .maxChildren(rt.getMaxChildren())
                        .build())
                .toList();
    }

    @Override
    public UpgradePreviewResponse previewUpgrade(Long subReservationId, Long toRoomTypeId) {
        SubReservation sub = findSubReservation(subReservationId);
        validateUpgradeAllowed(sub, toRoomTypeId);

        RoomType fromType = findRoomType(sub.getRoomTypeId());
        RoomType toType = findRoomType(toRoomTypeId);

        // 전체 숙박 기간 기준 차액 계산 (체크인~체크아웃)
        LocalDate calcFrom = sub.getCheckIn();
        LocalDate calcTo = sub.getCheckOut();
        int totalNights = (int) (calcTo.toEpochDay() - calcFrom.toEpochDay());
        if (totalNights <= 0) totalNights = 0;

        // 현재 DailyCharge 합산 (전체 기간)
        List<DailyCharge> currentCharges = dailyChargeRepository.findBySubReservationId(subReservationId);
        BigDecimal currentTotal = currentCharges.stream()
                .map(DailyCharge::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 현재 일자별 요금 맵 구성
        Map<LocalDate, BigDecimal> currentChargeMap = currentCharges.stream()
                .collect(Collectors.toMap(DailyCharge::getChargeDate, DailyCharge::getTotal,
                        (a, b) -> a));  // 중복 일자 시 첫 번째 사용

        // 대상 객실타입에 적용 가능한 레이트코드 탐색
        Long currentRateCodeId = sub.getMasterReservation().getRateCodeId();
        Long targetRateCodeId = findRateCodeForRoomType(toRoomTypeId, currentRateCodeId);

        // 새 객실타입 요금 계산 (전체 기간, 대상 레이트코드 사용)
        BigDecimal newTotal = BigDecimal.ZERO;
        List<DailyCharge> newCharges = List.of();
        try {
            newCharges = priceCalculationService.calculateDailyCharges(
                    targetRateCodeId, sub.getMasterReservation().getProperty(),
                    calcFrom, calcTo, sub.getAdults(), sub.getChildren(), sub);
            newTotal = newCharges.stream()
                    .map(DailyCharge::getTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } catch (Exception e) {
            log.warn("업그레이드 미리보기 요금 계산 실패 (targetRateCodeId={}): {}", targetRateCodeId, e.getMessage());
        }

        BigDecimal diff = newTotal.subtract(currentTotal);

        // 일자별 차액 상세
        List<DailyChargeDiff> dailyDiffs = newCharges.stream()
                .map(nc -> {
                    BigDecimal curCharge = currentChargeMap.getOrDefault(nc.getChargeDate(), BigDecimal.ZERO);
                    return DailyChargeDiff.builder()
                            .chargeDate(nc.getChargeDate())
                            .currentCharge(curCharge)
                            .newCharge(nc.getTotal())
                            .difference(nc.getTotal().subtract(curCharge))
                            .build();
                })
                .toList();

        return UpgradePreviewResponse.builder()
                .fromRoomTypeId(sub.getRoomTypeId())
                .fromRoomTypeName(fromType.getRoomTypeCode())
                .toRoomTypeId(toRoomTypeId)
                .toRoomTypeName(toType.getRoomTypeCode())
                .currentTotalCharge(currentTotal)
                .newTotalCharge(newTotal)
                .priceDifference(diff)
                .remainingNights(totalNights)
                .dailyDiffs(dailyDiffs)
                .build();
    }

    @Override
    @Transactional
    public RoomUpgradeHistoryResponse executeUpgrade(Long subReservationId, RoomUpgradeRequest request) {
        SubReservation sub = findSubReservation(subReservationId);
        validateUpgradeAllowed(sub, request.getToRoomTypeId());

        Long fromRoomTypeId = sub.getRoomTypeId();
        Long toRoomTypeId = request.getToRoomTypeId();

        RoomType fromType = findRoomType(fromRoomTypeId);
        RoomType toType = findRoomType(toRoomTypeId);

        // 차액 계산
        UpgradePreviewResponse preview = previewUpgrade(subReservationId, toRoomTypeId);
        BigDecimal priceDiff = preview.getPriceDifference();

        // COMPLIMENTARY인 경우 차액 0 처리
        if ("COMPLIMENTARY".equals(request.getUpgradeType())) {
            priceDiff = BigDecimal.ZERO;
        }

        // SubReservation roomTypeId 업데이트 (객실 타입만 변경, DailyCharge는 원본 유지)
        // Opera 방식: 예약 시점 요금 유지, 업그레이드 차액은 서비스 항목으로만 반영
        sub.changeRoomType(toRoomTypeId);

        // PAID/UPSELL인 경우 차액을 ReservationServiceItem으로 자동 부과
        if (("PAID".equals(request.getUpgradeType()) || "UPSELL".equals(request.getUpgradeType()))
                && priceDiff.compareTo(BigDecimal.ZERO) > 0) {
            // TC:1020 (Room Upgrade) 코드 조회 시도
            Long tcId = findUpgradeTransactionCodeId(sub.getMasterReservation().getProperty().getId());
            serviceItemRepository.save(ReservationServiceItem.builder()
                    .subReservation(sub)
                    .serviceType("PAID")
                    .transactionCodeId(tcId)
                    .serviceDate(LocalDate.now())
                    .quantity(1)
                    .unitPrice(priceDiff)
                    .tax(BigDecimal.ZERO)
                    .totalPrice(priceDiff)
                    .postingStatus("POSTED")
                    .build());
            log.info("업그레이드 차액 부과: subRes={}, amount={}", subReservationId, priceDiff);
        }

        // 이력 기록
        RoomUpgradeHistory history = RoomUpgradeHistory.builder()
                .subReservationId(subReservationId)
                .fromRoomTypeId(fromRoomTypeId)
                .toRoomTypeId(toRoomTypeId)
                .upgradedAt(LocalDateTime.now())
                .upgradeType(request.getUpgradeType())
                .priceDifference(priceDiff)
                .reason(request.getReason())
                .build();

        RoomUpgradeHistory saved = upgradeHistoryRepository.save(history);
        log.info("객실 업그레이드 실행: subRes={}, {} → {}, type={}, diff={}",
                subReservationId, fromType.getRoomTypeCode(), toType.getRoomTypeCode(),
                request.getUpgradeType(), priceDiff);

        return toHistoryResponse(saved, fromType, toType);
    }

    @Override
    public List<RoomUpgradeHistoryResponse> getUpgradeHistory(Long subReservationId) {
        List<RoomUpgradeHistory> histories = upgradeHistoryRepository
                .findAllBySubReservationIdOrderByUpgradedAtDesc(subReservationId);

        // 객실타입명 매핑
        Set<Long> typeIds = histories.stream()
                .flatMap(h -> java.util.stream.Stream.of(h.getFromRoomTypeId(), h.getToRoomTypeId()))
                .collect(Collectors.toSet());
        Map<Long, RoomType> typeMap = roomTypeRepository.findAllById(typeIds)
                .stream()
                .collect(Collectors.toMap(RoomType::getId, Function.identity()));

        return histories.stream()
                .map(h -> toHistoryResponse(h, typeMap.get(h.getFromRoomTypeId()), typeMap.get(h.getToRoomTypeId())))
                .toList();
    }

    // ========== 내부 헬퍼 ==========

    /**
     * 대상 객실타입에 해당 기간 동안 적용 가능한 요금이 있는지 확인
     * (예외 미발생 — 트랜잭션 rollback 마킹 방지)
     */
    private boolean hasApplicableRate(Long targetRoomTypeId, Long currentRateCodeId,
                                       LocalDate checkIn, LocalDate checkOut) {
        Long rateCodeId = findRateCodeForRoomType(targetRoomTypeId, currentRateCodeId);
        boolean covered = priceCalculationService.hasPricingCoverage(rateCodeId, checkIn, checkOut);
        if (!covered) {
            log.debug("업그레이드 대상 제외: roomTypeId={}, 해당 기간 요금 미설정", targetRoomTypeId);
        }
        return covered;
    }

    private SubReservation findSubReservation(Long id) {
        return subReservationRepository.findById(id)
                .orElseThrow(() -> new HolaException(ErrorCode.SUB_RESERVATION_NOT_FOUND));
    }

    private RoomType findRoomType(Long id) {
        return roomTypeRepository.findById(id)
                .orElseThrow(() -> new HolaException(ErrorCode.ROOM_TYPE_NOT_FOUND));
    }

    private void validateUpgradeAllowed(SubReservation sub, Long toRoomTypeId) {
        if (!UPGRADEABLE_STATUSES.contains(sub.getRoomReservationStatus())) {
            throw new HolaException(ErrorCode.UPGRADE_NOT_ALLOWED);
        }
        if (sub.getRoomTypeId().equals(toRoomTypeId)) {
            throw new HolaException(ErrorCode.UPGRADE_SAME_ROOM_TYPE);
        }
        // 대상 객실타입 가용성 검증 (자기 자신 제외)
        int available = roomAvailabilityService.getAvailableRoomCount(
                toRoomTypeId, sub.getCheckIn(), sub.getCheckOut(), List.of(sub.getId()));
        if (available <= 0) {
            throw new HolaException(ErrorCode.UPGRADE_ROOM_TYPE_NOT_AVAILABLE);
        }
    }

    /**
     * 대상 객실타입에 적용 가능한 레이트코드 탐색
     * 1순위: 현재 예약의 레이트코드가 대상 타입에도 매핑되어 있으면 그대로 사용
     * 2순위: 대상 객실타입에 매핑된 첫 번째 활성 레이트코드
     * 3순위: 현재 레이트코드 (폴백)
     */
    private Long findRateCodeForRoomType(Long targetRoomTypeId, Long currentRateCodeId) {
        // 대상 객실타입에 매핑된 레이트코드 목록
        List<RateCodeRoomType> mappings = rateCodeRoomTypeRepository.findAllByRoomTypeId(targetRoomTypeId);

        if (mappings.isEmpty()) {
            log.warn("대상 객실타입에 매핑된 레이트코드 없음: roomTypeId={}, 현재 레이트코드 사용", targetRoomTypeId);
            return currentRateCodeId;
        }

        // 1순위: 현재 레이트코드가 대상 타입에도 매핑되어 있는지
        boolean currentMapped = mappings.stream()
                .anyMatch(m -> m.getRateCodeId().equals(currentRateCodeId));
        if (currentMapped) {
            return currentRateCodeId;
        }

        // 2순위: 대상 타입의 첫 번째 활성 레이트코드
        for (RateCodeRoomType mapping : mappings) {
            RateCode rc = rateCodeRepository.findById(mapping.getRateCodeId()).orElse(null);
            if (rc != null && Boolean.TRUE.equals(rc.getUseYn())) {
                log.info("업그레이드 대상 레이트코드 변경: {} → {} (roomTypeId={})",
                        currentRateCodeId, rc.getId(), targetRoomTypeId);
                return rc.getId();
            }
        }

        // 3순위: 폴백
        return currentRateCodeId;
    }

    /**
     * 프로퍼티에서 Room Upgrade 트랜잭션 코드 ID 조회 (코드: 1020)
     */
    private Long findUpgradeTransactionCodeId(Long propertyId) {
        return transactionCodeRepository
                .findAllByPropertyIdOrderBySortOrderAscTransactionCodeAsc(propertyId)
                .stream()
                .filter(tc -> "1020".equals(tc.getTransactionCode()))
                .map(TransactionCode::getId)
                .findFirst()
                .orElse(null);
    }

    private RoomUpgradeHistoryResponse toHistoryResponse(RoomUpgradeHistory h, RoomType from, RoomType to) {
        return RoomUpgradeHistoryResponse.builder()
                .id(h.getId())
                .subReservationId(h.getSubReservationId())
                .fromRoomTypeId(h.getFromRoomTypeId())
                .fromRoomTypeName(from != null ? from.getRoomTypeCode() : null)
                .toRoomTypeId(h.getToRoomTypeId())
                .toRoomTypeName(to != null ? to.getRoomTypeCode() : null)
                .upgradedAt(h.getUpgradedAt())
                .upgradeType(h.getUpgradeType())
                .priceDifference(h.getPriceDifference())
                .reason(h.getReason())
                .createdBy(h.getCreatedBy())
                .createdAt(h.getCreatedAt())
                .build();
    }
}
