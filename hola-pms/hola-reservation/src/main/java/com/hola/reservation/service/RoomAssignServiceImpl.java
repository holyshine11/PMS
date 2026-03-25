package com.hola.reservation.service;

import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.common.util.NameMaskingUtil;
import com.hola.hotel.entity.Floor;
import com.hola.hotel.entity.Property;
import com.hola.hotel.entity.RoomNumber;
import com.hola.hotel.entity.RoomUnavailable;
import com.hola.hotel.repository.FloorRepository;
import com.hola.hotel.repository.PropertyRepository;
import com.hola.hotel.repository.RoomNumberRepository;
import com.hola.hotel.repository.RoomUnavailableRepository;
import com.hola.rate.entity.RateCode;
import com.hola.rate.entity.RateCodeRoomType;
import com.hola.rate.repository.RateCodeRepository;
import com.hola.rate.repository.RateCodeRoomTypeRepository;
import com.hola.reservation.booking.dto.response.*;
import com.hola.reservation.dto.response.RoomNumberAvailabilityResponse;
import com.hola.reservation.entity.DailyCharge;
import com.hola.reservation.entity.SubReservation;
import com.hola.reservation.repository.SubReservationRepository;
import com.hola.reservation.vo.DayUseTimeSlot;
import com.hola.room.entity.RoomClass;
import com.hola.room.entity.RoomType;
import com.hola.room.entity.RoomTypeFloor;
import com.hola.room.repository.RoomClassRepository;
import com.hola.room.repository.RoomTypeFloorRepository;
import com.hola.room.repository.RoomTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 객실 배정 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomAssignServiceImpl implements RoomAssignService {

    private final PropertyRepository propertyRepository;
    private final RateCodeRepository rateCodeRepository;
    private final RateCodeRoomTypeRepository rateCodeRoomTypeRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomClassRepository roomClassRepository;
    private final RoomTypeFloorRepository roomTypeFloorRepository;
    private final FloorRepository floorRepository;
    private final RoomNumberRepository roomNumberRepository;
    private final SubReservationRepository subReservationRepository;
    private final PriceCalculationService priceCalculationService;
    private final RoomUnavailableRepository roomUnavailableRepository;
    private final RoomAvailabilityService roomAvailabilityService;

    /** 해제된(비활성) 예약 상태 */
    private static final List<String> RELEASED_STATUSES = List.of("CANCELED", "NO_SHOW", "CHECKED_OUT");

    /** 객실 준비 안 됨 상태 (배정 불가) */
    private static final List<String> NOT_READY_STATUSES = List.of("OOO", "OOS", "DIRTY", "PICKUP");

    @Override
    public RoomAssignAvailabilityResponse getAvailability(
            Long propertyId, Long roomTypeId, Long rateCodeId,
            LocalDate checkIn, LocalDate checkOut,
            int adults, int children, Long excludeSubId) {

        // 1. 프로퍼티 조회 → 세금/봉사료율
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new HolaException(ErrorCode.PROPERTY_NOT_FOUND));

        // 2. 레이트코드 조회 → 통화
        RateCode rateCode = rateCodeRepository.findById(rateCodeId)
                .orElseThrow(() -> new HolaException(ErrorCode.RATE_CODE_NOT_FOUND));

        // 3. 현재 레그 요금 계산 (rate pricing 미설정 시 안전하게 0 처리)
        // hasPricingCoverage 먼저 확인하여 HolaException에 의한 트랜잭션 rollback-only 방지
        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        BigDecimal currentLegTotalPrice = BigDecimal.ZERO;
        BigDecimal currentAvgNightly = BigDecimal.ZERO;

        if (priceCalculationService.hasPricingCoverage(rateCodeId, checkIn, checkOut)) {
            try {
                List<DailyCharge> currentCharges = priceCalculationService.calculateDailyCharges(
                        rateCodeId, property, checkIn, checkOut, adults, children, null);
                currentLegTotalPrice = currentCharges.stream()
                        .map(DailyCharge::getTotal)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                currentAvgNightly = nights > 0
                        ? currentLegTotalPrice.divide(BigDecimal.valueOf(nights), 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
            } catch (Exception e) {
                log.warn("현재 레그 요금 계산 실패 (rateCodeId={}): {}", rateCodeId, e.getMessage());
            }
        }

        // 4. 레이트코드에 매핑된 객실타입 ID 조회 (추천 기준)
        Set<Long> rateCodeMappedRoomTypeIds = rateCodeRoomTypeRepository.findAllByRateCodeId(rateCodeId).stream()
                .map(RateCodeRoomType::getRoomTypeId)
                .collect(Collectors.toSet());

        // 5. 프로퍼티 전체 RoomType 조회
        List<RoomType> roomTypes = roomTypeRepository.findAllByPropertyIdOrderBySortOrderAscRoomTypeCodeAsc(propertyId);
        if (roomTypes.isEmpty()) {
            return RoomAssignAvailabilityResponse.builder()
                    .currentLegTotalPrice(currentLegTotalPrice)
                    .currentAvgNightly(currentAvgNightly)
                    .nights(nights)
                    .currency(rateCode.getCurrency())
                    .roomTypeGroups(Collections.emptyList())
                    .build();
        }

        // RoomClass 벌크 조회 → Map 캐싱
        List<Long> roomClassIds = roomTypes.stream()
                .map(RoomType::getRoomClassId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, RoomClass> roomClassMap = roomClassRepository.findAllById(roomClassIds).stream()
                .collect(Collectors.toMap(RoomClass::getId, rc -> rc));

        // 5. 전체 roomTypeId로 RoomTypeFloor 벌크 조회
        List<Long> allRoomTypeIds = roomTypes.stream()
                .map(RoomType::getId)
                .collect(Collectors.toList());
        List<RoomTypeFloor> allRoomTypeFloors = roomTypeFloorRepository.findAllByRoomTypeIdIn(allRoomTypeIds);

        // roomTypeId별 그룹핑
        Map<Long, List<RoomTypeFloor>> rtfByRoomType = allRoomTypeFloors.stream()
                .collect(Collectors.groupingBy(RoomTypeFloor::getRoomTypeId));

        // 6. 모든 roomNumberId, floorId 수집 → 벌크 조회
        Set<Long> allRoomNumberIds = allRoomTypeFloors.stream()
                .map(RoomTypeFloor::getRoomNumberId)
                .collect(Collectors.toSet());
        Set<Long> allFloorIds = allRoomTypeFloors.stream()
                .map(RoomTypeFloor::getFloorId)
                .collect(Collectors.toSet());

        // 벌크 충돌 조회 1회 (N+1 방지)
        List<SubReservation> allConflicts = allRoomNumberIds.isEmpty()
                ? Collections.emptyList()
                : subReservationRepository.findConflictsByRoomNumberIds(
                        new ArrayList<>(allRoomNumberIds), checkIn, checkOut, RELEASED_STATUSES);

        // 현재 예약이 Dayuse인지 확인 (시간 슬롯 필터링용)
        SubReservation currentSub = excludeSubId != null
                ? subReservationRepository.findById(excludeSubId).orElse(null) : null;
        DayUseTimeSlot timeSlot = currentSub != null ? currentSub.getDayUseTimeSlot() : null;

        // 자기 자신 제외 + Dayuse 시간 슬롯 필터링 + roomNumberId별 그룹핑
        List<SubReservation> filteredConflicts = allConflicts.stream()
                .filter(sub -> excludeSubId == null || !sub.getId().equals(excludeSubId))
                .toList();

        if (timeSlot != null) {
            filteredConflicts = roomAvailabilityService.filterDayUseTimeConflicts(
                    filteredConflicts, checkIn, timeSlot);
        }

        Map<Long, List<SubReservation>> conflictMap = filteredConflicts.stream()
                .filter(sub -> sub.getRoomNumberId() != null)
                .collect(Collectors.groupingBy(SubReservation::getRoomNumberId));

        // Floor, RoomNumber 벌크 조회 → Map 캐싱
        Map<Long, Floor> floorMap = allFloorIds.isEmpty()
                ? Collections.emptyMap()
                : floorRepository.findAllById(allFloorIds).stream()
                        .collect(Collectors.toMap(Floor::getId, f -> f));
        Map<Long, RoomNumber> roomNumberMap = allRoomNumberIds.isEmpty()
                ? Collections.emptyMap()
                : roomNumberRepository.findAllById(allRoomNumberIds).stream()
                        .collect(Collectors.toMap(RoomNumber::getId, rn -> rn));

        // 7. 각 roomType별 응답 조립
        List<RoomTypeGroupResponse> groups = new ArrayList<>();

        for (RoomType rt : roomTypes) {
            // 레이트코드에 매핑된 객실타입이면 추천
            boolean isRecommended = rateCodeMappedRoomTypeIds.contains(rt.getId());
            RoomClass roomClass = roomClassMap.get(rt.getRoomClassId());

            // 해당 roomType에 적합한 레이트코드 탐색 후 요금 계산
            // hasPricingCoverage 먼저 확인하여 트랜잭션 rollback-only 방지
            BigDecimal typeTotalPrice = null;
            BigDecimal typeAvgNightly = null;
            BigDecimal priceDiff = null;
            List<DailyChargeDetail> dailyChargeDetails = null;

            Long typeRateCodeId = findRateCodeForRoomType(rt.getId(), rateCodeId);
            if (priceCalculationService.hasPricingCoverage(typeRateCodeId, checkIn, checkOut)) {
                try {
                    List<DailyCharge> typeCharges = priceCalculationService.calculateDailyCharges(
                            typeRateCodeId, property, checkIn, checkOut, adults, children, null);
                    typeTotalPrice = typeCharges.stream()
                            .map(DailyCharge::getTotal)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    typeAvgNightly = nights > 0
                            ? typeTotalPrice.divide(BigDecimal.valueOf(nights), 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                    priceDiff = typeTotalPrice.subtract(currentLegTotalPrice);

                    // 일자별 요금 상세
                    dailyChargeDetails = typeCharges.stream()
                            .map(dc -> DailyChargeDetail.builder()
                                    .chargeDate(dc.getChargeDate())
                                    .supplyPrice(dc.getSupplyPrice())
                                    .tax(dc.getTax())
                                    .serviceCharge(dc.getServiceCharge())
                                    .total(dc.getTotal())
                                    .build())
                            .collect(Collectors.toList());
                } catch (Exception e) {
                    log.debug("객실타입 {} 요금 계산 실패: {}", rt.getRoomTypeCode(), e.getMessage());
                }
            }

            // 층별 그룹핑
            List<RoomTypeFloor> rtFloors = rtfByRoomType.getOrDefault(rt.getId(), Collections.emptyList());
            Map<Long, List<RoomTypeFloor>> byFloor = rtFloors.stream()
                    .collect(Collectors.groupingBy(RoomTypeFloor::getFloorId));

            List<FloorGroupResponse> floorGroups = new ArrayList<>();
            for (Map.Entry<Long, List<RoomTypeFloor>> entry : byFloor.entrySet()) {
                Long floorId = entry.getKey();
                List<RoomTypeFloor> floorRtfs = entry.getValue();
                Floor floor = floorMap.get(floorId);
                if (floor == null) continue;

                List<RoomAvailabilityItem> roomItems = new ArrayList<>();
                int availableCount = 0;

                for (RoomTypeFloor rtf : floorRtfs) {
                    RoomNumber rn = roomNumberMap.get(rtf.getRoomNumberId());
                    if (rn == null) continue;

                    List<SubReservation> conflicts = conflictMap.getOrDefault(rn.getId(), List.of());
                    String hkStatus = rn.getHkStatus();
                    boolean isNotReady = NOT_READY_STATUSES.contains(hkStatus);
                    boolean isAvailable = conflicts.isEmpty() && !isNotReady;

                    String unavailableType = isNotReady ? hkStatus : null;
                    RoomAvailabilityItem.RoomAvailabilityItemBuilder itemBuilder = RoomAvailabilityItem.builder()
                            .roomNumberId(rn.getId())
                            .roomNumber(rn.getRoomNumber())
                            .descriptionKo(rn.getDescriptionKo())
                            .available(isAvailable)
                            .unavailableType(unavailableType);

                    if (!isAvailable && !conflicts.isEmpty()) {
                        SubReservation conflict = conflicts.get(0);
                        String guestName = conflict.getMasterReservation().getGuestNameKo();
                        String maskedName = (guestName != null && !guestName.isBlank())
                                ? NameMaskingUtil.maskKoreanName(guestName) : "-";

                        itemBuilder
                                .conflictReservationId(conflict.getMasterReservation().getId())
                                .conflictReservationNumber(conflict.getMasterReservation().getMasterReservationNo())
                                .conflictGuestName(maskedName)
                                .conflictCheckIn(conflict.getCheckIn().toString())
                                .conflictCheckOut(conflict.getCheckOut().toString())
                                .conflictStatus(conflict.getRoomReservationStatus());
                    }

                    if (isAvailable) availableCount++;
                    roomItems.add(itemBuilder.build());
                }

                // 가용 객실 우선 정렬, 같은 그룹 내 호수번호순
                roomItems.sort((a, b) -> {
                    if (a.isAvailable() != b.isAvailable()) return a.isAvailable() ? -1 : 1;
                    return a.getRoomNumber().compareTo(b.getRoomNumber());
                });

                int floorNum;
                try {
                    floorNum = Integer.parseInt(floor.getFloorNumber());
                } catch (NumberFormatException e) {
                    floorNum = 0;
                }

                floorGroups.add(FloorGroupResponse.builder()
                        .floorId(floorId)
                        .floorNumber(floorNum)
                        .floorName(floor.getFloorName())
                        .totalRooms(roomItems.size())
                        .availableRooms(availableCount)
                        .rooms(roomItems)
                        .build());
            }

            // 층번호 순 정렬
            floorGroups.sort(Comparator.comparingInt(FloorGroupResponse::getFloorNumber));

            groups.add(RoomTypeGroupResponse.builder()
                    .roomTypeId(rt.getId())
                    .roomTypeCode(rt.getRoomTypeCode())
                    .roomTypeDescription(rt.getDescription())
                    .roomClassName(roomClass != null ? roomClass.getRoomClassName() : null)
                    .maxAdults(rt.getMaxAdults() != null ? rt.getMaxAdults() : 0)
                    .maxChildren(rt.getMaxChildren() != null ? rt.getMaxChildren() : 0)
                    .roomSize(rt.getRoomSize())
                    .recommended(isRecommended)
                    .totalPrice(typeTotalPrice)
                    .avgNightly(typeAvgNightly)
                    .priceDiff(priceDiff)
                    .dailyCharges(dailyChargeDetails)
                    .floors(floorGroups)
                    .build());
        }

        // 8. recommended=true 그룹 먼저, 나머지는 코드순
        groups.sort((a, b) -> {
            if (a.isRecommended() != b.isRecommended()) return a.isRecommended() ? -1 : 1;
            return a.getRoomTypeCode().compareTo(b.getRoomTypeCode());
        });

        return RoomAssignAvailabilityResponse.builder()
                .currentLegTotalPrice(currentLegTotalPrice)
                .currentAvgNightly(currentAvgNightly)
                .nights(nights)
                .currency(rateCode.getCurrency())
                .roomTypeGroups(groups)
                .build();
    }

    /**
     * 대상 객실타입에 적용 가능한 레이트코드 탐색
     * 1순위: 현재 레이트코드가 대상 타입에도 매핑되어 있으면 그대로 사용
     * 2순위: 대상 객실타입에 매핑된 첫 번째 활성 레이트코드
     * 3순위: 현재 레이트코드 (폴백)
     */
    private Long findRateCodeForRoomType(Long targetRoomTypeId, Long currentRateCodeId) {
        List<RateCodeRoomType> mappings = rateCodeRoomTypeRepository.findAllByRoomTypeId(targetRoomTypeId);
        if (mappings.isEmpty()) {
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
                return rc.getId();
            }
        }

        // 3순위: 폴백
        return currentRateCodeId;
    }

    @Override
    public List<RoomNumberAvailabilityResponse> getFloorRoomAvailability(
            Long floorId, LocalDate checkIn, LocalDate checkOut, Long excludeSubId) {

        List<Long> roomNumberIds = roomTypeFloorRepository.findDistinctRoomNumberIdsByFloorId(floorId);
        if (roomNumberIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<RoomNumber> rooms = roomNumberRepository.findAllById(roomNumberIds);

        // 벌크 충돌 조회 (N+1 방지)
        List<SubReservation> allConflicts = subReservationRepository
                .findConflictsByRoomNumberIds(roomNumberIds, checkIn, checkOut, RELEASED_STATUSES);

        // 현재 예약이 Dayuse인지 확인 (시간 슬롯 필터링용)
        SubReservation currentSub = excludeSubId != null
                ? subReservationRepository.findById(excludeSubId).orElse(null) : null;
        DayUseTimeSlot floorSlot = currentSub != null ? currentSub.getDayUseTimeSlot() : null;

        List<SubReservation> filteredConflicts = allConflicts.stream()
                .filter(sub -> excludeSubId == null || !sub.getId().equals(excludeSubId))
                .toList();

        if (floorSlot != null) {
            filteredConflicts = roomAvailabilityService.filterDayUseTimeConflicts(
                    filteredConflicts, checkIn, floorSlot);
        }

        Map<Long, List<SubReservation>> conflictMap = filteredConflicts.stream()
                .filter(sub -> sub.getRoomNumberId() != null)
                .collect(Collectors.groupingBy(SubReservation::getRoomNumberId));

        List<RoomNumberAvailabilityResponse> responses = new ArrayList<>();
        for (RoomNumber room : rooms) {
            List<SubReservation> conflicts = conflictMap.getOrDefault(room.getId(), List.of());
            String hkStatus = room.getHkStatus();
            boolean isNotReady = "OOO".equals(hkStatus) || "OOS".equals(hkStatus)
                    || "DIRTY".equals(hkStatus) || "PICKUP".equals(hkStatus);

            if (conflicts.isEmpty() && !isNotReady) {
                responses.add(RoomNumberAvailabilityResponse.builder()
                        .id(room.getId())
                        .roomNumber(room.getRoomNumber())
                        .descriptionKo(room.getDescriptionKo())
                        .available(true)
                        .build());
            } else if (isNotReady && conflicts.isEmpty()) {
                // 준비 안 된 객실 (OOO/OOS/DIRTY/PICKUP — 예약 충돌은 없지만 사용 불가)
                responses.add(RoomNumberAvailabilityResponse.builder()
                        .id(room.getId())
                        .roomNumber(room.getRoomNumber())
                        .descriptionKo(room.getDescriptionKo())
                        .available(false)
                        .unavailableType(hkStatus)
                        .build());
            } else {
                SubReservation conflict = conflicts.get(0);
                String guestName = conflict.getMasterReservation().getGuestNameKo();
                String maskedName = (guestName != null && !guestName.isBlank())
                        ? NameMaskingUtil.maskKoreanName(guestName) : "-";

                responses.add(RoomNumberAvailabilityResponse.builder()
                        .id(room.getId())
                        .roomNumber(room.getRoomNumber())
                        .descriptionKo(room.getDescriptionKo())
                        .available(false)
                        .conflictReservationNo(conflict.getMasterReservation().getMasterReservationNo())
                        .conflictGuestName(maskedName)
                        .conflictCheckIn(conflict.getCheckIn())
                        .conflictCheckOut(conflict.getCheckOut())
                        .build());
            }
        }

        // 가용 → 불가 순, 같은 그룹 내 호수번호순
        responses.sort((a, b) -> {
            if (a.isAvailable() != b.isAvailable()) return a.isAvailable() ? -1 : 1;
            return a.getRoomNumber().compareTo(b.getRoomNumber());
        });

        return responses;
    }
}
