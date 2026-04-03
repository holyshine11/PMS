package com.hola.reservation.booking.service;

import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.hotel.entity.Property;
import com.hola.hotel.repository.PropertyRepository;
import com.hola.rate.dto.response.RateCodeListResponse;
import com.hola.rate.entity.RateCode;
import com.hola.rate.repository.RateCodeRepository;
import com.hola.rate.service.RateCodeService;
import com.hola.reservation.booking.dto.response.AvailableRoomTypeResponse;
import com.hola.reservation.booking.dto.response.BookingConfirmationResponse;
import com.hola.reservation.entity.DailyCharge;
import com.hola.reservation.entity.MasterReservation;
import com.hola.reservation.entity.ReservationServiceItem;
import com.hola.reservation.entity.SubReservation;
import com.hola.reservation.repository.DailyChargeRepository;
import com.hola.reservation.repository.SubReservationRepository;
import com.hola.reservation.repository.ReservationServiceItemRepository;
import com.hola.reservation.service.PriceCalculationService;
import com.hola.reservation.service.ReservationPaymentService;
import com.hola.reservation.vo.DayUseTimeSlot;
import com.hola.room.entity.PaidServiceOption;
import com.hola.room.entity.RoomClass;
import com.hola.room.entity.RoomType;
import com.hola.room.repository.PaidServiceOptionRepository;
import com.hola.room.repository.RoomClassRepository;
import com.hola.room.repository.RoomTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 부킹엔진 공통 헬퍼
 * - 여러 부킹 서비스에서 공유하는 조회/검증/변환 메서드
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingHelper {

    private final PropertyRepository propertyRepository;
    private final RoomClassRepository roomClassRepository;
    private final RateCodeRepository rateCodeRepository;
    private final RateCodeService rateCodeService;
    private final SubReservationRepository subReservationRepository;
    private final DailyChargeRepository dailyChargeRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final PaidServiceOptionRepository paidServiceOptionRepository;
    private final ReservationServiceItemRepository reservationServiceItemRepository;
    private final ReservationPaymentService reservationPaymentService;
    private final PriceCalculationService priceCalculationService;
    private final com.hola.hotel.repository.CancellationFeeRepository cancellationFeeRepository;

    /** 최대 숙박 가능 일수 */
    private static final int MAX_STAY_NIGHTS = 30;

    /**
     * 프로퍼티 코드로 프로퍼티 조회
     */
    public Property findPropertyByCode(String propertyCode) {
        return propertyRepository.findByPropertyCodeAndUseYnTrue(propertyCode)
                .orElseThrow(() -> new HolaException(ErrorCode.BOOKING_PROPERTY_NOT_FOUND));
    }

    /**
     * 날짜 범위 유효성 검증
     * - 체크아웃 > 체크인
     * - 체크인이 과거 날짜가 아닌지
     * - 최대 30박 초과 여부
     */
    public void validateDateRange(LocalDate checkIn, LocalDate checkOut) {
        if (checkOut == null || checkIn == null || !checkOut.isAfter(checkIn)) {
            throw new HolaException(ErrorCode.BOOKING_INVALID_DATE_RANGE);
        }
        if (checkIn.isBefore(LocalDate.now())) {
            throw new HolaException(ErrorCode.BOOKING_INVALID_DATE_RANGE,
                    "체크인 날짜는 오늘 또는 이후여야 합니다.");
        }
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights > MAX_STAY_NIGHTS) {
            throw new HolaException(ErrorCode.BOOKING_MAX_STAY_EXCEEDED);
        }
    }

    /**
     * 당일 예약 마감시간 검증 (부킹엔진 전용)
     */
    public void validateSameDayCutoff(Property property, LocalDate checkIn) {
        if (!checkIn.isEqual(LocalDate.now())) {
            return;
        }
        if (!Boolean.TRUE.equals(property.getSameDayBookingEnabled())) {
            throw new HolaException(ErrorCode.BOOKING_SAME_DAY_DISABLED);
        }
        LocalTime now = LocalTime.now();
        int currentMinutes = now.getHour() * 60 + now.getMinute();
        if (currentMinutes >= property.getSameDayCutoffTime()) {
            throw new HolaException(ErrorCode.BOOKING_SAME_DAY_CUTOFF);
        }
    }

    /**
     * 레이트코드 숙박일수(min/max) 검증
     */
    public void validateRateCodeStayDays(Long rateCodeId, LocalDate checkIn, LocalDate checkOut) {
        RateCode rateCode = rateCodeRepository.findById(rateCodeId)
                .orElseThrow(() -> new HolaException(ErrorCode.RATE_CODE_NOT_FOUND));

        long stayDays = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (rateCode.getMinStayDays() != null && stayDays < rateCode.getMinStayDays()) {
            throw new HolaException(ErrorCode.RESERVATION_STAY_DAYS_VIOLATION);
        }
        if (rateCode.getMaxStayDays() != null && rateCode.getMaxStayDays() > 0 && stayDays > rateCode.getMaxStayDays()) {
            throw new HolaException(ErrorCode.RESERVATION_STAY_DAYS_VIOLATION);
        }
    }

    /**
     * 객실 클래스명 조회
     */
    public String getRoomClassName(Long roomClassId) {
        if (roomClassId == null) return null;
        return roomClassRepository.findById(roomClassId)
                .map(RoomClass::getRoomClassName)
                .orElse(null);
    }

    /**
     * starRating 문자열 -> 숫자 변환
     */
    public Integer parseStarRating(String starRating) {
        if (starRating == null || starRating.isBlank()) return null;
        try {
            return Integer.parseInt(starRating.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 레이트코드 정보 조회 (RateCodeService를 통해)
     */
    public RateCodeListResponse findRateCodeInfo(Long rateCodeId) {
        try {
            var response = rateCodeService.getRateCode(rateCodeId);
            return RateCodeListResponse.builder()
                    .id(response.getId())
                    .rateCode(response.getRateCode())
                    .rateNameKo(response.getRateNameKo())
                    .currency(response.getCurrency())
                    .build();
        } catch (HolaException e) {
            log.warn("레이트코드 조회 실패: rateCodeId={}", rateCodeId);
            return null;
        }
    }

    /**
     * MasterReservation -> BookingConfirmationResponse 변환
     */
    public BookingConfirmationResponse buildConfirmationResponse(MasterReservation master) {
        Property property = master.getProperty();

        // 서브예약 목록 조회
        List<SubReservation> subs = subReservationRepository.findByMasterReservationId(master.getId());

        // 총액 계산
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<BookingConfirmationResponse.RoomDetail> roomDetails = new ArrayList<>();

        for (SubReservation sub : subs) {
            List<DailyCharge> charges = dailyChargeRepository.findBySubReservationId(sub.getId());

            BigDecimal roomTotal = charges.stream()
                    .map(DailyCharge::getTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            totalAmount = totalAmount.add(roomTotal);

            // 일별 요금 DTO 변환
            List<AvailableRoomTypeResponse.DailyPrice> dailyPrices = charges.stream()
                    .map(dc -> AvailableRoomTypeResponse.DailyPrice.builder()
                            .date(dc.getChargeDate().toString())
                            .supplyPrice(dc.getSupplyPrice())
                            .tax(dc.getTax())
                            .serviceCharge(dc.getServiceCharge())
                            .total(dc.getTotal())
                            .build())
                    .toList();

            String roomTypeName = getRoomClassName(
                    roomTypeRepository.findById(sub.getRoomTypeId())
                            .map(RoomType::getRoomClassId).orElse(null));
            boolean isDayUseSub = sub.isDayUse();
            int nights = isDayUseSub ? 0 : (int) ChronoUnit.DAYS.between(sub.getCheckIn(), sub.getCheckOut());
            // Dayuse 이용시간 계산
            Integer dayUseDurationHours = null;
            DayUseTimeSlot confirmSlot = sub.getDayUseTimeSlot();
            if (isDayUseSub && confirmSlot != null) {
                dayUseDurationHours = confirmSlot.durationHours();
            }

            // 서비스 항목 조회
            List<ReservationServiceItem> serviceItems = reservationServiceItemRepository
                    .findBySubReservationId(sub.getId());
            List<BookingConfirmationResponse.ServiceDetail> serviceDetails = new ArrayList<>();
            if (!serviceItems.isEmpty()) {
                // 서비스명 벌크 조회
                Set<Long> svcIds = serviceItems.stream()
                        .map(ReservationServiceItem::getServiceOptionId)
                        .filter(id -> id != null)
                        .collect(Collectors.toSet());
                java.util.Map<Long, String> svcNameMap = svcIds.isEmpty() ? java.util.Map.of()
                        : paidServiceOptionRepository.findAllById(svcIds).stream()
                                .collect(Collectors.toMap(PaidServiceOption::getId, PaidServiceOption::getServiceNameKo));

                for (ReservationServiceItem si : serviceItems) {
                    serviceDetails.add(BookingConfirmationResponse.ServiceDetail.builder()
                            .serviceName(si.getServiceOptionId() != null
                                    ? svcNameMap.getOrDefault(si.getServiceOptionId(), "서비스 #" + si.getServiceOptionId())
                                    : "기타")
                            .serviceType(si.getServiceType())
                            .quantity(si.getQuantity())
                            .unitPrice(si.getUnitPrice())
                            .totalPrice(si.getTotalPrice())
                            .serviceDate(si.getServiceDate())
                            .build());
                }
            }

            roomDetails.add(BookingConfirmationResponse.RoomDetail.builder()
                    .roomTypeName(roomTypeName)
                    .checkIn(sub.getCheckIn())
                    .checkOut(sub.getCheckOut())
                    .adults(sub.getAdults())
                    .children(sub.getChildren() != null ? sub.getChildren() : 0)
                    .nights(nights)
                    .roomTotal(roomTotal)
                    .dailyCharges(dailyPrices)
                    .services(serviceDetails)
                    .stayType(sub.getStayType() != null ? sub.getStayType().name() : "OVERNIGHT")
                    .dayUseDurationHours(dayUseDurationHours)
                    .build());
        }

        // 결제 정보 조회
        var paymentSummary = reservationPaymentService.getPaymentSummary(
                master.getProperty().getId(), master.getId());
        String approvalNo = null;
        LocalDateTime paymentDate = null;
        String cardMaskNo = null;
        if (paymentSummary.getTransactions() != null && !paymentSummary.getTransactions().isEmpty()) {
            var firstTxn = paymentSummary.getTransactions().get(0);
            paymentDate = firstTxn.getCreatedAt();
            cardMaskNo = firstTxn.getPgCardNo();
            approvalNo = firstTxn.getMemo();
            // 승인번호는 메모에서 추출 (부킹엔진 결제 - 승인번호: MOCK-XXXXXXXX)
            if (approvalNo != null && approvalNo.contains("승인번호: ")) {
                approvalNo = approvalNo.substring(approvalNo.indexOf("승인번호: ") + 6);
            }
        }

        // 취소 정책 조회
        List<BookingConfirmationResponse.CancellationPolicyInfo> policyInfos =
                cancellationFeeRepository.findAllByPropertyIdOrderBySortOrder(property.getId())
                        .stream()
                        .map(fee -> BookingConfirmationResponse.CancellationPolicyInfo.builder()
                                .description(buildPolicyDescription(fee))
                                .build())
                        .toList();

        return BookingConfirmationResponse.builder()
                .confirmationNo(master.getConfirmationNo())
                .masterReservationNo(master.getMasterReservationNo())
                .reservationStatus(master.getReservationStatus())
                .guestNameKo(master.getGuestNameKo())
                .totalAmount(totalAmount)
                .currency("KRW")
                .paymentStatus(paymentSummary.getPaymentStatus())
                .paymentMethod(paymentSummary.getTransactions() != null && !paymentSummary.getTransactions().isEmpty()
                        ? paymentSummary.getTransactions().get(0).getPaymentMethod() : null)
                .approvalNo(approvalNo)
                .paymentDate(paymentDate)
                .cardMaskNo(cardMaskNo)
                .rooms(roomDetails)
                .cancellationPolicies(policyInfos)
                .propertyName(property.getPropertyName())
                .propertyAddress(property.getAddress())
                .checkInTime(property.getCheckInTime())
                .checkOutTime(property.getCheckOutTime())
                .propertyPhone(property.getPhone())
                .createdAt(master.getCreatedAt())
                .build();
    }

    /**
     * 취소 정책 자연어 설명 생성
     */
    public String buildPolicyDescription(com.hola.hotel.entity.CancellationFee fee) {
        String unit = "PERCENTAGE".equals(fee.getFeeType()) ? "%" :
                "FIXED_KRW".equals(fee.getFeeType()) ? "원" : "USD";
        String amountStr = fee.getFeeAmount().stripTrailingZeros().toPlainString();

        if ("NOSHOW".equals(fee.getCheckinBasis())) {
            return "노쇼 시: 1박 요금의 " + amountStr + unit + " 부과";
        }
        if (fee.getFeeAmount().compareTo(BigDecimal.ZERO) == 0) {
            return "체크인 " + fee.getDaysBefore() + "일 이내: 무료 취소";
        }
        return "체크인 " + fee.getDaysBefore() + "일 이내: 1박 요금의 " + amountStr + unit + " 부과";
    }

    /**
     * Dayuse 세금/봉사료 포함 DailyPrice 빌드 (PriceCalculationService 위임)
     */
    public AvailableRoomTypeResponse.DailyPrice buildDayUseDailyPrice(
            BigDecimal supplyPrice, Property property, LocalDate date) {
        var r = priceCalculationService.calculateTaxAndServiceCharge(supplyPrice, property);
        return AvailableRoomTypeResponse.DailyPrice.builder()
                .date(date.toString())
                .supplyPrice(r.supplyPrice())
                .serviceCharge(r.serviceCharge())
                .tax(r.tax())
                .total(r.total())
                .build();
    }

    /**
     * Dayuse 세금/봉사료 포함 DailyCharge 빌드 (PriceCalculationService 위임)
     */
    public DailyCharge buildDayUseDailyCharge(BigDecimal supplyPrice, Property property, LocalDate date) {
        var r = priceCalculationService.calculateTaxAndServiceCharge(supplyPrice, property);
        return DailyCharge.builder()
                .chargeDate(date)
                .supplyPrice(r.supplyPrice())
                .serviceCharge(r.serviceCharge())
                .tax(r.tax())
                .total(r.total())
                .build();
    }
}
