package com.hola.reservation.booking.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.hotel.entity.Property;
import com.hola.hotel.entity.ReservationChannel;
import com.hola.hotel.repository.ReservationChannelRepository;
import com.hola.rate.entity.RateCode;
import com.hola.rate.entity.DayUseRate;
import com.hola.rate.repository.DayUseRateRepository;
import com.hola.rate.repository.RateCodeRepository;
import com.hola.reservation.booking.dto.request.BookingCreateRequest;
import com.hola.reservation.booking.dto.response.BookingConfirmationResponse;
import com.hola.reservation.booking.dto.response.BookingValidationResult;
import com.hola.reservation.booking.entity.BookingAuditLog;
import com.hola.reservation.booking.gateway.*;
import com.hola.reservation.booking.repository.BookingAuditLogRepository;
import com.hola.reservation.dto.request.PaymentProcessRequest;
import com.hola.reservation.entity.DailyCharge;
import com.hola.reservation.entity.MasterReservation;
import com.hola.reservation.entity.ReservationServiceItem;
import com.hola.reservation.entity.SubReservation;
import com.hola.reservation.repository.DailyChargeRepository;
import com.hola.reservation.repository.MasterReservationRepository;
import com.hola.reservation.repository.ReservationServiceItemRepository;
import com.hola.reservation.repository.SubReservationRepository;
import com.hola.reservation.service.PriceCalculationService;
import com.hola.reservation.service.ReservationNumberGenerator;
import com.hola.reservation.service.ReservationPaymentService;
import com.hola.reservation.service.RoomAvailabilityService;
import com.hola.room.entity.PaidServiceOption;
import com.hola.room.repository.PaidServiceOptionRepository;
import com.hola.common.enums.StayType;
import com.hola.reservation.vo.DayUseTimeSlot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 부킹엔진 예약 생성 서비스 구현
 * - 예약 검증, 예약 생성, PG 결제 기반 예약 생성
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingCreationServiceImpl implements BookingCreationService {

    private final BookingHelper helper;
    private final RateCodeRepository rateCodeRepository;
    private final DayUseRateRepository dayUseRateRepository;
    private final ReservationChannelRepository reservationChannelRepository;
    private final RoomAvailabilityService roomAvailabilityService;
    private final PriceCalculationService priceCalculationService;
    private final MasterReservationRepository masterReservationRepository;
    private final SubReservationRepository subReservationRepository;
    private final DailyChargeRepository dailyChargeRepository;
    private final ReservationNumberGenerator reservationNumberGenerator;
    private final ReservationPaymentService reservationPaymentService;
    private final PaymentGateway paymentGateway;
    private final BookingAuditLogRepository bookingAuditLogRepository;
    private final ObjectMapper objectMapper;
    private final com.hola.reservation.service.RateIncludedServiceHelper rateIncludedServiceHelper;
    private final ReservationServiceItemRepository reservationServiceItemRepository;
    private final PaidServiceOptionRepository paidServiceOptionRepository;
    private final com.hola.room.service.InventoryService inventoryService;

    @Override
    @Transactional(readOnly = true)
    public BookingValidationResult validateBookingRequest(String propertyCode, BookingCreateRequest request) {
        // 1. 이용약관 동의 확인
        if (!request.isAgreedTerms()) {
            throw new HolaException(ErrorCode.BOOKING_TERMS_NOT_AGREED);
        }

        // 2. 멱등성 체크
        var existingLog = bookingAuditLogRepository
                .findByIdempotencyKeyAndEventType(request.getIdempotencyKey(), "BOOKING_CREATED");
        if (existingLog.isPresent()) {
            throw new HolaException(ErrorCode.BOOKING_DUPLICATE_REQUEST);
        }

        // 3. 프로퍼티 조회 + WEBSITE 채널 조회
        Property property = helper.findPropertyByCode(propertyCode);
        ReservationChannel websiteChannel = reservationChannelRepository
                .findByPropertyIdAndChannelCode(property.getId(), "WEBSITE")
                .orElse(null);

        // 4. 객실 선택별 날짜/가용성/가격 재검증
        return doValidateRooms(request, property, websiteChannel);
    }

    @Override
    @Transactional
    public BookingConfirmationResponse createBooking(String propertyCode, BookingCreateRequest request,
                                                      String clientIp, String userAgent) {
        // 1~4. 검증
        if (!request.isAgreedTerms()) {
            throw new HolaException(ErrorCode.BOOKING_TERMS_NOT_AGREED);
        }

        var existingLog = bookingAuditLogRepository
                .findByIdempotencyKeyAndEventType(request.getIdempotencyKey(), "BOOKING_CREATED");
        if (existingLog.isPresent()) {
            Long existingReservationId = existingLog.get().getMasterReservationId();
            log.info("멱등성 중복 요청 감지: idempotencyKey={}", request.getIdempotencyKey());
            MasterReservation existingMaster = masterReservationRepository.findById(existingReservationId)
                    .orElseThrow(() -> new HolaException(ErrorCode.RESERVATION_NOT_FOUND));
            return helper.buildConfirmationResponse(existingMaster);
        }

        Property property = helper.findPropertyByCode(propertyCode);
        ReservationChannel websiteChannel = reservationChannelRepository
                .findByPropertyIdAndChannelCode(property.getId(), "WEBSITE")
                .orElse(null);

        BookingValidationResult validation = doValidateRooms(request, property, websiteChannel);

        // 5. Mock 결제 처리 (CASH 또는 테스트 환경)
        BookingCreateRequest.PaymentInfo paymentInfo = request.getPayment();
        PaymentResult paymentResult = paymentGateway.authorize(PaymentRequest.builder()
                .orderId(request.getIdempotencyKey())
                .amount(validation.getGrandTotal())
                .currency("KRW")
                .paymentMethod(paymentInfo.getMethod())
                .cardNumber(paymentInfo.getCardNumber())
                .expiryDate(paymentInfo.getExpiryDate())
                .cvv(paymentInfo.getCvv())
                .customerName(request.getGuest().getGuestNameKo())
                .customerEmail(request.getGuest().getEmail())
                .build());

        if (!paymentResult.isSuccess()) {
            throw new HolaException(ErrorCode.BOOKING_PAYMENT_FAILED,
                    paymentResult.getErrorMessage());
        }

        // 6~11. 예약 생성 + 결제 기록
        return createBookingInternal(request, validation, paymentResult, clientIp, userAgent);
    }

    @Override
    @Transactional
    public BookingConfirmationResponse createBookingWithPaymentResult(String propertyCode, BookingCreateRequest request,
                                                                       PaymentResult paymentResult,
                                                                       String clientIp, String userAgent) {
        // 멱등성 체크
        var existingLog = bookingAuditLogRepository
                .findByIdempotencyKeyAndEventType(request.getIdempotencyKey(), "BOOKING_CREATED");
        if (existingLog.isPresent()) {
            Long existingReservationId = existingLog.get().getMasterReservationId();
            log.info("멱등성 중복 요청 감지(PG): idempotencyKey={}", request.getIdempotencyKey());
            MasterReservation existingMaster = masterReservationRepository.findById(existingReservationId)
                    .orElseThrow(() -> new HolaException(ErrorCode.RESERVATION_NOT_FOUND));
            return helper.buildConfirmationResponse(existingMaster);
        }

        Property property = helper.findPropertyByCode(propertyCode);
        ReservationChannel websiteChannel = reservationChannelRepository
                .findByPropertyIdAndChannelCode(property.getId(), "WEBSITE")
                .orElse(null);

        BookingValidationResult validation = doValidateRooms(request, property, websiteChannel);

        return createBookingInternal(request, validation, paymentResult, clientIp, userAgent);
    }

    // ===== Private Helper Methods =====

    /**
     * 객실 검증 + 가격 계산 (Steps 3-4 공통)
     */
    private BookingValidationResult doValidateRooms(BookingCreateRequest request,
                                                     Property property,
                                                     ReservationChannel websiteChannel) {
        List<BookingCreateRequest.RoomSelection> rooms = request.getRooms();
        LocalDate earliestCheckIn = null;
        LocalDate latestCheckOut = null;
        BigDecimal grandTotal = BigDecimal.ZERO;

        List<List<DailyCharge>> roomDailyChargesList = new ArrayList<>();
        List<RateCode> roomRateCodes = new ArrayList<>();

        for (BookingCreateRequest.RoomSelection room : rooms) {
            helper.validateDateRange(room.getCheckIn(), room.getCheckOut());
            helper.validateSameDayCutoff(property, room.getCheckIn());
            helper.validateRateCodeStayDays(room.getRateCodeId(), room.getCheckIn(), room.getCheckOut());

            int available = roomAvailabilityService.getAvailableRoomCount(
                    room.getRoomTypeId(), room.getCheckIn(), room.getCheckOut());
            if (available <= 0) {
                throw new HolaException(ErrorCode.BOOKING_NO_AVAILABILITY);
            }

            RateCode roomRateCode = rateCodeRepository.findById(room.getRateCodeId())
                    .orElseThrow(() -> new HolaException(ErrorCode.RATE_CODE_NOT_FOUND));

            List<DailyCharge> dailyCharges;
            if (roomRateCode.isDayUse()) {
                List<DayUseRate> dayUseRates = dayUseRateRepository
                        .findByRateCodeIdAndUseYnTrueOrderBySortOrderAsc(room.getRateCodeId());
                if (dayUseRates.isEmpty()) {
                    throw new HolaException(ErrorCode.DAY_USE_RATE_NOT_FOUND);
                }
                DayUseRate rate = dayUseRates.get(0);
                dailyCharges = List.of(helper.buildDayUseDailyCharge(
                        rate.getSupplyPrice(), property, room.getCheckIn()));
            } else {
                dailyCharges = priceCalculationService.calculateDailyCharges(
                        room.getRateCodeId(), property,
                        room.getCheckIn(), room.getCheckOut(),
                        room.getAdults(),
                        room.getChildren() != null ? room.getChildren() : 0,
                        null);
            }
            roomDailyChargesList.add(dailyCharges);
            roomRateCodes.add(roomRateCode);

            BigDecimal roomTotal = dailyCharges.stream()
                    .map(DailyCharge::getTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            grandTotal = grandTotal.add(roomTotal);

            if (earliestCheckIn == null || room.getCheckIn().isBefore(earliestCheckIn)) {
                earliestCheckIn = room.getCheckIn();
            }
            if (latestCheckOut == null || room.getCheckOut().isAfter(latestCheckOut)) {
                latestCheckOut = room.getCheckOut();
            }
        }

        return BookingValidationResult.builder()
                .property(property)
                .websiteChannel(websiteChannel)
                .earliestCheckIn(earliestCheckIn)
                .latestCheckOut(latestCheckOut)
                .grandTotal(grandTotal)
                .roomDailyChargesList(roomDailyChargesList)
                .roomRateCodes(roomRateCodes)
                .build();
    }

    /**
     * 예약 생성 내부 로직 (Steps 6-11 공통)
     */
    private BookingConfirmationResponse createBookingInternal(BookingCreateRequest request,
                                                               BookingValidationResult validation,
                                                               PaymentResult paymentResult,
                                                               String clientIp, String userAgent) {
        Property property = validation.getProperty();
        List<BookingCreateRequest.RoomSelection> rooms = request.getRooms();

        // 6. 예약번호 + 확인번호 생성
        String masterReservationNo = reservationNumberGenerator.generateMasterReservationNo(property);
        String confirmationNo = reservationNumberGenerator.generateConfirmationNo();

        // 7. MasterReservation 생성
        BookingCreateRequest.GuestInfo guest = request.getGuest();
        MasterReservation master = MasterReservation.builder()
                .property(property)
                .masterReservationNo(masterReservationNo)
                .confirmationNo(confirmationNo)
                .reservationStatus("RESERVED")
                .masterCheckIn(validation.getEarliestCheckIn())
                .masterCheckOut(validation.getLatestCheckOut())
                .guestNameKo(guest.getGuestNameKo())
                .guestFirstNameEn(guest.getGuestFirstNameEn())
                .guestLastNameEn(guest.getGuestLastNameEn())
                .phoneCountryCode(guest.getPhoneCountryCode())
                .phoneNumber(guest.getPhoneNumber())
                .email(guest.getEmail())
                .nationality(guest.getNationality())
                .rateCodeId(rooms.get(0).getRateCodeId())
                .reservationChannelId(validation.getWebsiteChannel() != null ? validation.getWebsiteChannel().getId() : null)
                .build();
        master = masterReservationRepository.save(master);

        // 8. SubReservation + DailyCharge 생성 (비관적 락으로 가용성 최종 검증)
        for (int i = 0; i < rooms.size(); i++) {
            BookingCreateRequest.RoomSelection room = rooms.get(i);

            int lockedAvailable = roomAvailabilityService.getAvailableRoomCountWithLock(
                    room.getRoomTypeId(), room.getCheckIn(), room.getCheckOut());
            if (lockedAvailable <= 0) {
                throw new HolaException(ErrorCode.BOOKING_NO_AVAILABILITY);
            }

            String subNo = reservationNumberGenerator.generateSubReservationNo(masterReservationNo, i + 1);

            RateCode cachedRateCode = validation.getRoomRateCodes().get(i);
            StayType stayType = cachedRateCode.isDayUse() ? StayType.DAY_USE : StayType.OVERNIGHT;
            DayUseTimeSlot timeSlot = null;
            if (cachedRateCode.isDayUse()) {
                Integer hours = null;
                List<DayUseRate> duRates = dayUseRateRepository
                        .findByRateCodeIdAndUseYnTrueOrderBySortOrderAsc(room.getRateCodeId());
                if (!duRates.isEmpty()) hours = duRates.get(0).getDurationHours();
                timeSlot = DayUseTimeSlot.from(property, hours);
            }

            SubReservation sub = SubReservation.builder()
                    .masterReservation(master)
                    .subReservationNo(subNo)
                    .roomReservationStatus("RESERVED")
                    .roomTypeId(room.getRoomTypeId())
                    .adults(room.getAdults())
                    .children(room.getChildren() != null ? room.getChildren() : 0)
                    .checkIn(room.getCheckIn())
                    .checkOut(room.getCheckOut())
                    .stayType(stayType)
                    .dayUseStartTime(timeSlot != null ? timeSlot.startTime() : null)
                    .dayUseEndTime(timeSlot != null ? timeSlot.endTime() : null)
                    .build();
            sub = subReservationRepository.save(sub);
            final SubReservation savedSub = sub;

            List<DailyCharge> dailyCharges = validation.getRoomDailyChargesList().get(i);
            List<DailyCharge> chargesToSave = dailyCharges.stream()
                    .map(dc -> DailyCharge.builder()
                            .subReservation(savedSub)
                            .chargeDate(dc.getChargeDate())
                            .supplyPrice(dc.getSupplyPrice())
                            .tax(dc.getTax())
                            .serviceCharge(dc.getServiceCharge())
                            .total(dc.getTotal())
                            .build())
                    .toList();
            dailyChargeRepository.saveAll(chargesToSave);

            rateIncludedServiceHelper.addRateIncludedServices(sub, room.getRateCodeId());

            if (room.getServices() != null && !room.getServices().isEmpty()) {
                for (BookingCreateRequest.ServiceSelection sel : room.getServices()) {
                    PaidServiceOption option = paidServiceOptionRepository.findById(sel.getServiceOptionId())
                            .orElse(null);
                    if (option == null || !Boolean.TRUE.equals(option.getUseYn())) continue;

                    int qty = sel.getQuantity() != null ? sel.getQuantity() : 1;
                    BigDecimal unitPrice = option.getVatIncludedPrice();
                    BigDecimal tax = option.getTaxAmount().multiply(BigDecimal.valueOf(qty));
                    BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(qty));

                    String applicableNights = option.getApplicableNights();

                    if ("ALL_NIGHTS".equals(applicableNights)) {
                        // 매 숙박일마다 1건 (체크인 ~ 체크아웃 전일)
                        LocalDate date = sub.getCheckIn();
                        while (date.isBefore(sub.getCheckOut())) {
                            reservationServiceItemRepository.save(ReservationServiceItem.builder()
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
                        reservationServiceItemRepository.save(ReservationServiceItem.builder()
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
                        // NOT_APPLICABLE: 날짜 없이 1건
                        reservationServiceItemRepository.save(ReservationServiceItem.builder()
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

                    if (option.getInventoryItemId() != null) {
                        boolean reserved = inventoryService.reserveInventory(
                                option.getInventoryItemId(), room.getCheckIn(), room.getCheckOut(), qty);
                        if (!reserved) {
                            throw new HolaException(ErrorCode.INVENTORY_NOT_AVAILABLE);
                        }
                    }
                }
            }
        }

        // 9. 결제 정보 생성 (PG 필드 포함, 첫 번째 Leg에 귀속)
        reservationPaymentService.recalculatePayment(master.getId());
        String paymentMethod = request.getPayment() != null ? request.getPayment().getMethod() : "CARD";
        String memo = "부킹엔진 결제 - 승인번호: " + paymentResult.getApprovalNo();
        // 첫 번째 서브예약 ID 조회 (부킹엔진 PG 결제는 첫 번째 Leg에 귀속)
        Long firstSubId = subReservationRepository.findByMasterReservationId(master.getId())
                .stream().findFirst().map(SubReservation::getId).orElse(null);
        PaymentProcessRequest pgPaymentRequest = new PaymentProcessRequest(paymentMethod, validation.getGrandTotal(), memo);
        pgPaymentRequest.setSubReservationId(firstSubId);
        reservationPaymentService.processPaymentWithPgResult(master.getProperty().getId(), master.getId(),
                pgPaymentRequest, paymentResult);

        // 10. BookingAuditLog 기록
        saveAuditLog(master.getId(), confirmationNo, "BOOKING_CREATED",
                "WEBSITE", request, null, clientIp, userAgent, request.getIdempotencyKey());

        log.info("부킹엔진 예약 생성 완료: confirmationNo={}, masterNo={}, 총액={}",
                confirmationNo, masterReservationNo, validation.getGrandTotal());

        // 11. 응답 빌드
        return helper.buildConfirmationResponse(master);
    }

    /**
     * 부킹 감사 로그 저장
     */
    private void saveAuditLog(Long masterReservationId, String confirmationNo,
                               String eventType, String channel,
                               Object requestPayload, Object responsePayload,
                               String clientIp, String userAgent, String idempotencyKey) {
        try {
            BookingAuditLog auditLog = BookingAuditLog.builder()
                    .masterReservationId(masterReservationId)
                    .confirmationNo(confirmationNo)
                    .eventType(eventType)
                    .channel(channel)
                    .requestPayload(objectMapper.writeValueAsString(requestPayload))
                    .responsePayload(responsePayload != null ? objectMapper.writeValueAsString(responsePayload) : null)
                    .clientIp(clientIp)
                    .userAgent(userAgent)
                    .idempotencyKey(idempotencyKey)
                    .build();
            bookingAuditLogRepository.save(auditLog);
        } catch (JsonProcessingException e) {
            log.warn("부킹 감사 로그 JSON 직렬화 실패: {}", e.getMessage());
        }
    }
}
