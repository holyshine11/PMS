package com.hola.reservation.service;

import com.hola.common.enums.StayType;
import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.hotel.entity.Property;
import com.hola.hotel.entity.RoomUnavailable;
import com.hola.hotel.repository.RoomUnavailableRepository;
import com.hola.rate.entity.DayUseRate;
import com.hola.rate.entity.RateCode;
import com.hola.rate.repository.DayUseRateRepository;
import com.hola.rate.repository.RateCodeRepository;
import com.hola.reservation.dto.request.ReservationGuestRequest;
import com.hola.reservation.dto.request.ServiceSelectionRequest;
import com.hola.reservation.dto.request.SubReservationRequest;
import com.hola.reservation.entity.*;
import com.hola.reservation.mapper.ReservationMapper;
import com.hola.reservation.repository.*;
import com.hola.reservation.vo.DayUseTimeSlot;
import com.hola.room.entity.PaidServiceOption;
import com.hola.room.entity.RoomType;
import com.hola.room.repository.PaidServiceOptionRepository;
import com.hola.room.repository.RoomTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 서브 예약 생성 팩토리 — CRUD/Leg 서비스에서 공유
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubReservationCreator {

    private final SubReservationRepository subReservationRepository;
    private final ReservationGuestRepository reservationGuestRepository;
    private final DailyChargeRepository dailyChargeRepository;
    private final ReservationServiceItemRepository serviceItemRepository;
    private final RateCodeRepository rateCodeRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final PaidServiceOptionRepository paidServiceOptionRepository;
    private final RoomUnavailableRepository roomUnavailableRepository;
    private final DayUseRateRepository dayUseRateRepository;
    private final ReservationMapper reservationMapper;
    private final ReservationNumberGenerator numberGenerator;
    private final RoomAvailabilityService availabilityService;
    private final PriceCalculationService priceCalculationService;
    private final RateIncludedServiceHelper rateIncludedServiceHelper;
    private final com.hola.room.service.InventoryService inventoryService;

    /**
     * 서브 예약 생성 + 투숙객 + 일별요금
     */
    public SubReservation create(MasterReservation master, SubReservationRequest request,
                                  int legSeq, Property property) {
        // Dayuse 자동 감지: 레이트코드의 stayType으로 결정
        StayType stayType = resolveStayType(request.getStayType(), master.getRateCodeId());
        LocalDate effectiveCheckOut = request.getCheckOut();
        DayUseTimeSlot timeSlot = null;

        if (stayType.isDayUse()) {
            if (!Boolean.TRUE.equals(property.getDayUseEnabled())) {
                throw new HolaException(ErrorCode.DAY_USE_NOT_ENABLED);
            }
            effectiveCheckOut = request.getCheckIn().plusDays(1);

            // 이용시간 결정: 요청값 → 기존 활성 Dayuse Leg → 프로퍼티 기본값
            Integer durationHours = request.getDayUseDurationHours();
            if (durationHours == null) {
                durationHours = master.getSubReservations().stream()
                        .filter(s -> s.isDayUse() && !"CANCELED".equals(s.getRoomReservationStatus()))
                        .findFirst()
                        .map(s -> s.getDayUseTimeSlot() != null ? s.getDayUseTimeSlot().durationHours() : null)
                        .orElse(null);
            }
            timeSlot = DayUseTimeSlot.from(property, durationHours);
        }

        validateDates(request.getCheckIn(), effectiveCheckOut);

        // 서브예약도 과거 날짜 체크인 불가
        if (request.getCheckIn().isBefore(LocalDate.now())) {
            throw new HolaException(ErrorCode.RESERVATION_CHECKIN_PAST_DATE);
        }

        // 객실타입 최대 수용 인원 검증
        if (request.getRoomTypeId() != null) {
            RoomType roomType = roomTypeRepository.findById(request.getRoomTypeId()).orElse(null);
            if (roomType != null) {
                int adults = request.getAdults() != null ? request.getAdults() : 1;
                int children = request.getChildren() != null ? request.getChildren() : 0;
                if (adults > roomType.getMaxAdults() || children > roomType.getMaxChildren()) {
                    throw new HolaException(ErrorCode.SUB_RESERVATION_OCCUPANCY_EXCEEDED);
                }
            }
        }

        // L1 객실 충돌 검사 (비관적 락, Dayuse 시간 슬롯 인식)
        if (request.getRoomNumberId() != null) {
            if (availabilityService.hasRoomConflictWithLock(request.getRoomNumberId(),
                    request.getCheckIn(), effectiveCheckOut, null,
                    stayType, timeSlot)) {
                throw new HolaException(ErrorCode.SUB_RESERVATION_ROOM_CONFLICT);
            }
        }

        // OOO/OOS 기간 체크 (roomNumberId가 있을 때)
        if (request.getRoomNumberId() != null) {
            List<RoomUnavailable> unavailable = roomUnavailableRepository.findOverlapping(
                    request.getRoomNumberId(), request.getCheckIn(), effectiveCheckOut);
            if (!unavailable.isEmpty()) {
                throw new HolaException(ErrorCode.ROOM_UNAVAILABLE_FOR_RESERVATION);
            }
        }

        // L2 타입별 가용성 비관적 락 검증 (호수 미배정 시)
        if (request.getRoomNumberId() == null && request.getRoomTypeId() != null) {
            int available = availabilityService.getAvailableRoomCountWithLock(
                    request.getRoomTypeId(), request.getCheckIn(), effectiveCheckOut);
            if (available <= 0) {
                log.warn("L2 타입별 가용 객실 부족: roomTypeId={}, 잔여={}", request.getRoomTypeId(), available);
            }
        }

        // 서브 예약 생성 — 새 Leg는 항상 RESERVED로 시작 (체크인 전)
        String legStatus = "RESERVED";

        String subNo = numberGenerator.generateSubReservationNo(master.getMasterReservationNo(), legSeq);
        SubReservation sub = SubReservation.builder()
                .masterReservation(master)
                .subReservationNo(subNo)
                .roomReservationStatus(legStatus)
                .stayType(stayType)
                .dayUseStartTime(timeSlot != null ? timeSlot.startTime() : null)
                .dayUseEndTime(timeSlot != null ? timeSlot.endTime() : null)
                .roomTypeId(request.getRoomTypeId())
                .floorId(request.getFloorId())
                .roomNumberId(request.getRoomNumberId())
                .adults(request.getAdults() != null ? request.getAdults() : 1)
                .children(request.getChildren() != null ? request.getChildren() : 0)
                .checkIn(request.getCheckIn())
                .checkOut(effectiveCheckOut)
                .earlyCheckIn(request.getEarlyCheckIn() != null ? request.getEarlyCheckIn() : false)
                .lateCheckOut(request.getLateCheckOut() != null ? request.getLateCheckOut() : false)
                .build();

        sub = subReservationRepository.save(sub);

        // 투숙객 등록
        if (request.getGuests() != null) {
            for (ReservationGuestRequest guestReq : request.getGuests()) {
                ReservationGuest guest = reservationMapper.toReservationGuestEntity(guestReq, sub);
                reservationGuestRepository.save(guest);
            }
        }

        // 일별 요금 계산 및 저장
        if (master.getRateCodeId() != null) {
            recalculateDailyCharges(sub, property);
        }

        // 레이트코드 포함 서비스 자동 추가
        rateIncludedServiceHelper.addRateIncludedServices(sub, master.getRateCodeId());

        // 예약 시 선택한 유료 서비스(Add-on) 추가
        if (request.getServices() != null && !request.getServices().isEmpty()) {
            addSelectedServices(sub, request.getServices());
        }

        return sub;
    }

    /**
     * 투숙객 목록 갱신 (삭제 후 재등록)
     */
    public void updateGuests(SubReservation sub, List<ReservationGuestRequest> guestRequests) {
        if (guestRequests == null) return;

        // orphanRemoval=true 컬렉션 → collection.clear() + flush() 방식 사용 (JPQL DELETE 금지)
        sub.getGuests().clear();
        reservationGuestRepository.flush();

        for (ReservationGuestRequest guestReq : guestRequests) {
            ReservationGuest guest = reservationMapper.toReservationGuestEntity(guestReq, sub);
            reservationGuestRepository.save(guest);
        }
    }

    /**
     * 일별 요금 재계산
     */
    public void recalculateDailyCharges(SubReservation sub, Property property) {
        // orphanRemoval=true 컬렉션 → collection.clear() + flush() 방식 사용 (JPQL DELETE 금지)
        sub.getDailyCharges().clear();
        dailyChargeRepository.flush();

        Long rateCodeId = sub.getMasterReservation().getRateCodeId();
        if (rateCodeId == null) return;

        if (sub.isDayUse()) {
            // Dayuse 전용 요금 조회
            DailyCharge charge = calculateDayUseCharge(sub, rateCodeId, property);
            dailyChargeRepository.save(charge);
        } else {
            // 기존 숙박 요금 (변경 없음)
            List<DailyCharge> charges = priceCalculationService.calculateDailyCharges(
                    rateCodeId, property,
                    sub.getCheckIn(), sub.getCheckOut(),
                    sub.getAdults(), sub.getChildren(), sub);
            dailyChargeRepository.saveAll(charges);
        }
    }

    /**
     * 레이트코드 판매기간 + 숙박일수 검증
     */
    public void validateRateCode(Long rateCodeId, LocalDate checkIn, LocalDate checkOut) {
        RateCode rateCode = rateCodeRepository.findById(rateCodeId)
                .orElseThrow(() -> new HolaException(ErrorCode.RATE_CODE_NOT_FOUND));

        // 판매기간 검증: 체크인이 판매기간 내에 있어야 함
        if (checkIn.isBefore(rateCode.getSaleStartDate()) || checkIn.isAfter(rateCode.getSaleEndDate())) {
            throw new HolaException(ErrorCode.RESERVATION_RATE_EXPIRED);
        }

        // Dayuse 레이트코드는 숙박일수/요금커버리지 검증 스킵 (DayUseRate 기반 요금 적용)
        if (rateCode.isDayUse()) {
            return;
        }

        // 숙박일수 검증 (Overnight만)
        long stayDays = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (rateCode.getMinStayDays() != null && stayDays < rateCode.getMinStayDays()) {
            throw new HolaException(ErrorCode.RESERVATION_STAY_DAYS_VIOLATION);
        }
        if (rateCode.getMaxStayDays() != null && rateCode.getMaxStayDays() > 0 && stayDays > rateCode.getMaxStayDays()) {
            throw new HolaException(ErrorCode.RESERVATION_STAY_DAYS_VIOLATION);
        }

        // 요금 커버리지 검증 (Overnight만)
        priceCalculationService.validatePricingCoverage(rateCodeId, checkIn, checkOut);
    }

    /**
     * 날짜 유효성 검증
     */
    public void validateDates(LocalDate checkIn, LocalDate checkOut) {
        validateDates(checkIn, checkOut, false);
    }

    /**
     * 날짜 유효성 검증 (dayuse 허용 시 같은 날짜 OK)
     */
    public void validateDates(LocalDate checkIn, LocalDate checkOut, boolean allowSameDay) {
        if (checkIn == null || checkOut == null) return;
        if (allowSameDay) {
            if (checkOut.isBefore(checkIn)) {
                throw new HolaException(ErrorCode.SUB_RESERVATION_DATE_INVALID);
            }
        } else {
            if (!checkOut.isAfter(checkIn)) {
                throw new HolaException(ErrorCode.SUB_RESERVATION_DATE_INVALID);
            }
        }
    }

    /**
     * 레이트코드가 Dayuse인지 확인
     */
    public boolean isDayUseRateCode(Long rateCodeId) {
        if (rateCodeId == null) return false;
        RateCode rc = rateCodeRepository.findById(rateCodeId).orElse(null);
        return rc != null && rc.isDayUse();
    }

    /**
     * 레이트코드 기반 stayType 자동 결정
     */
    public StayType resolveStayType(String requestStayType, Long rateCodeId) {
        if (requestStayType != null) {
            try { return StayType.valueOf(requestStayType); }
            catch (IllegalArgumentException e) { return StayType.OVERNIGHT; }
        }
        if (rateCodeId != null) {
            RateCode rc = rateCodeRepository.findById(rateCodeId).orElse(null);
            if (rc != null && rc.isDayUse()) return StayType.DAY_USE;
        }
        return StayType.OVERNIGHT;
    }

    // ─── private ──────────────────────────

    /**
     * 예약 시 선택한 유료 서비스를 PAID 타입으로 추가
     */
    private void addSelectedServices(SubReservation sub, List<ServiceSelectionRequest> services) {
        List<Long> serviceIds = services.stream()
                .map(ServiceSelectionRequest::getServiceOptionId)
                .collect(Collectors.toList());

        Map<Long, PaidServiceOption> optionMap = paidServiceOptionRepository.findAllById(serviceIds).stream()
                .collect(Collectors.toMap(PaidServiceOption::getId, java.util.function.Function.identity()));

        for (ServiceSelectionRequest sel : services) {
            PaidServiceOption option = optionMap.get(sel.getServiceOptionId());
            if (option == null) continue;

            int qty = sel.getQuantity() != null ? sel.getQuantity() : 1;
            BigDecimal unitPrice = option.getVatIncludedPrice();
            BigDecimal tax = option.getTaxAmount().multiply(BigDecimal.valueOf(qty));
            BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(qty));

            String applicableNights = option.getApplicableNights();
            // 재고 아이템 연결 시 재고 차감
            if (option.getInventoryItemId() != null) {
                boolean reserved = inventoryService.reserveInventory(
                        option.getInventoryItemId(), sub.getCheckIn(), sub.getCheckOut(), qty);
                if (!reserved) {
                    log.warn("재고 부족 - 서비스 스킵: serviceId={}, itemId={}", option.getId(), option.getInventoryItemId());
                    continue;
                }
            }

            if ("ALL_NIGHTS".equals(applicableNights)) {
                LocalDate date = sub.getCheckIn();
                while (date.isBefore(sub.getCheckOut())) {
                    serviceItemRepository.save(ReservationServiceItem.builder()
                            .subReservation(sub)
                            .serviceType("PAID")
                            .serviceOptionId(option.getId())
                            .transactionCodeId(option.getTransactionCodeId())
                            .serviceDate(date)
                            .quantity(qty)
                            .unitPrice(unitPrice)
                            .tax(tax)
                            .totalPrice(total)
                            .build());
                    date = date.plusDays(1);
                }
            } else if ("FIRST_NIGHT_ONLY".equals(applicableNights)) {
                serviceItemRepository.save(ReservationServiceItem.builder()
                        .subReservation(sub)
                        .serviceType("PAID")
                        .serviceOptionId(option.getId())
                        .transactionCodeId(option.getTransactionCodeId())
                        .serviceDate(sub.getCheckIn())
                        .quantity(qty)
                        .unitPrice(unitPrice)
                        .tax(tax)
                        .totalPrice(total)
                        .build());
            } else {
                serviceItemRepository.save(ReservationServiceItem.builder()
                        .subReservation(sub)
                        .serviceType("PAID")
                        .serviceOptionId(option.getId())
                        .transactionCodeId(option.getTransactionCodeId())
                        .serviceDate(null)
                        .quantity(qty)
                        .unitPrice(unitPrice)
                        .tax(tax)
                        .totalPrice(total)
                        .build());
            }
        }
    }

    /**
     * Dayuse 요금 계산 — DayUseRate 테이블에서 이용시간에 맞는 요금 조회
     */
    private DailyCharge calculateDayUseCharge(SubReservation sub, Long rateCodeId, Property property) {
        int hours = sub.getDayUseTimeSlot().durationHours();

        DayUseRate rate = dayUseRateRepository.findByRateCodeIdAndDurationHoursAndUseYnTrue(rateCodeId, hours)
                .orElseThrow(() -> new HolaException(ErrorCode.DAY_USE_RATE_NOT_FOUND));

        // PriceCalculationService 공통 유틸 사용
        var r = priceCalculationService.calculateTaxAndServiceCharge(rate.getSupplyPrice(), property);

        return DailyCharge.builder()
                .subReservation(sub)
                .chargeDate(sub.getCheckIn())
                .supplyPrice(r.supplyPrice())
                .serviceCharge(r.serviceCharge())
                .tax(r.tax())
                .total(r.total())
                .build();
    }
}
