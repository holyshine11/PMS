package com.hola.reservation.booking.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.hotel.entity.Hotel;
import com.hola.hotel.entity.Property;
import com.hola.hotel.entity.PropertyImage;
import com.hola.hotel.entity.PropertyTerms;
import com.hola.hotel.entity.ReservationChannel;
import com.hola.hotel.repository.PropertyImageRepository;
import com.hola.hotel.repository.PropertyTermsRepository;
import com.hola.hotel.repository.PropertyRepository;
import com.hola.hotel.repository.ReservationChannelRepository;
import com.hola.rate.dto.response.RateCodeListResponse;
import com.hola.rate.entity.RateCode;
import com.hola.rate.entity.RateCodePaidService;
import com.hola.rate.entity.RateCodeRoomType;
import com.hola.rate.entity.PromotionCode;
import com.hola.rate.entity.DayUseRate;
import com.hola.rate.repository.DayUseRateRepository;
import com.hola.rate.repository.RateCodePaidServiceRepository;
import com.hola.rate.repository.PromotionCodeRepository;
import com.hola.rate.repository.RateCodeRepository;
import com.hola.rate.repository.RateCodeRoomTypeRepository;
import com.hola.rate.service.RateCodeService;
import com.hola.reservation.booking.dto.request.BookingCreateRequest;
import com.hola.reservation.booking.dto.request.BookingModifyRequest;
import com.hola.reservation.booking.dto.request.BookingSearchRequest;
import com.hola.reservation.booking.dto.request.PriceCheckRequest;
import com.hola.reservation.booking.dto.response.*;
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
import com.hola.room.entity.FreeServiceOption;
import com.hola.room.entity.PaidServiceOption;
import com.hola.room.entity.RoomClass;
import com.hola.room.entity.RoomType;
import com.hola.room.entity.RoomTypeFreeService;
import com.hola.room.repository.FreeServiceOptionRepository;
import com.hola.room.repository.PaidServiceOptionRepository;
import com.hola.room.repository.RoomClassRepository;
import com.hola.room.repository.RoomTypeFreeServiceRepository;
import com.hola.room.repository.RoomTypeRepository;
import com.hola.common.enums.StayType;
import com.hola.reservation.vo.DayUseTimeSlot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 부킹엔진 서비스 구현
 * - 게스트 예약 플로우: 프로퍼티 조회 → 객실 검색 → 요금 확인 → 예약 생성
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingServiceImpl implements BookingService {

    private final PropertyRepository propertyRepository;
    private final PropertyImageRepository propertyImageRepository;
    private final PropertyTermsRepository propertyTermsRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomClassRepository roomClassRepository;
    private final RoomTypeFreeServiceRepository roomTypeFreeServiceRepository;
    private final FreeServiceOptionRepository freeServiceOptionRepository;
    private final PaidServiceOptionRepository paidServiceOptionRepository;
    private final RateCodeService rateCodeService;
    private final RateCodeRepository rateCodeRepository;
    private final RateCodeRoomTypeRepository rateCodeRoomTypeRepository;
    private final PromotionCodeRepository promotionCodeRepository;
    private final RoomAvailabilityService roomAvailabilityService;
    private final PriceCalculationService priceCalculationService;
    private final MasterReservationRepository masterReservationRepository;
    private final SubReservationRepository subReservationRepository;
    private final DailyChargeRepository dailyChargeRepository;
    private final ReservationChannelRepository reservationChannelRepository;
    private final ReservationNumberGenerator reservationNumberGenerator;
    private final ReservationPaymentService reservationPaymentService;
    private final PaymentGateway paymentGateway;
    private final BookingAuditLogRepository bookingAuditLogRepository;
    private final ObjectMapper objectMapper;
    private final com.hola.reservation.service.RateIncludedServiceHelper rateIncludedServiceHelper;
    private final ReservationServiceItemRepository reservationServiceItemRepository;
    private final RateCodePaidServiceRepository rateCodePaidServiceRepository;
    private final DayUseRateRepository dayUseRateRepository;
    private final CancellationPolicyService cancellationPolicyService;
    private final com.hola.hotel.repository.CancellationFeeRepository cancellationFeeRepository;
    private final com.hola.reservation.repository.ReservationPaymentRepository reservationPaymentRepository;
    private final com.hola.reservation.repository.PaymentTransactionRepository paymentTransactionRepository;
    private final com.hola.room.service.InventoryService inventoryService;

    /** 최대 숙박 가능 일수 */
    private static final int MAX_STAY_NIGHTS = 30;

    @Override
    public PropertyInfoResponse getPropertyInfo(String propertyCode) {
        Property property = findPropertyByCode(propertyCode);
        Hotel hotel = property.getHotel();

        // starRating 문자열 → 숫자 변환 (파싱 실패 시 null)
        Integer starRating = parseStarRating(property.getStarRating());

        return PropertyInfoResponse.builder()
                .propertyId(property.getId())
                .propertyCode(property.getPropertyCode())
                .propertyName(property.getPropertyName())
                .hotelName(hotel != null ? hotel.getHotelName() : null)
                .propertyType(property.getPropertyType())
                .starRating(starRating)
                .checkInTime(property.getCheckInTime())
                .checkOutTime(property.getCheckOutTime())
                .phone(property.getPhone())
                .email(property.getEmail())
                .address(property.getAddress())
                .addressDetail(property.getAddressDetail())
                .logoPath(property.getLogoPath())
                .build();
    }

    @Override
    public List<AvailableRoomTypeResponse> searchAvailability(String propertyCode, BookingSearchRequest request) {
        // 날짜 유효성 검증
        validateDateRange(request.getCheckIn(), request.getCheckOut());

        Property property = findPropertyByCode(propertyCode);
        Long propertyId = property.getId();

        // 프로퍼티의 활성 객실타입 전체 조회
        List<RoomType> roomTypes = roomTypeRepository.findAllByPropertyIdOrderBySortOrderAscRoomTypeCodeAsc(propertyId);

        // 인원 조건 필터링
        List<RoomType> filteredRoomTypes = roomTypes.stream()
                .filter(rt -> Boolean.TRUE.equals(rt.getUseYn()))
                .filter(rt -> request.getAdults() <= rt.getMaxAdults())
                .filter(rt -> request.getChildren() == null || request.getChildren() == 0
                        || request.getChildren() <= rt.getMaxChildren())
                .toList();

        // 해당 기간에 적용 가능한 레이트코드 목록 조회
        List<RateCodeListResponse> availableRateCodes = rateCodeService.getAvailableRateCodes(
                propertyId, request.getCheckIn(), request.getCheckOut());

        List<AvailableRoomTypeResponse> result = new ArrayList<>();

        for (RoomType roomType : filteredRoomTypes) {
            // 가용 객실 수 확인
            int availableCount = roomAvailabilityService.getAvailableRoomCount(
                    roomType.getId(), request.getCheckIn(), request.getCheckOut());

            if (availableCount <= 0) {
                continue;
            }

            // 해당 객실타입에 매핑된 레이트코드 필터링
            List<AvailableRoomTypeResponse.RateOption> rateOptions = buildRateOptions(
                    roomType.getId(), availableRateCodes, property, request);

            if (rateOptions.isEmpty()) {
                continue;
            }

            // 무료 서비스 목록 조회
            List<AvailableRoomTypeResponse.ServiceInfo> freeServices = buildFreeServices(roomType.getId());

            // 객실 클래스명 조회
            String roomClassName = getRoomClassName(roomType.getRoomClassId());

            result.add(AvailableRoomTypeResponse.builder()
                    .roomTypeId(roomType.getId())
                    .roomTypeCode(roomType.getRoomTypeCode())
                    .roomClassName(roomClassName)
                    .description(roomType.getDescription())
                    .roomSize(roomType.getRoomSize())
                    .features(roomType.getFeatures())
                    .maxAdults(roomType.getMaxAdults())
                    .maxChildren(roomType.getMaxChildren())
                    .availableCount(availableCount)
                    .rateOptions(rateOptions)
                    .freeServices(freeServices)
                    .build());
        }

        log.info("객실 가용성 검색: propertyCode={}, {}~{}, 성인={}, 아동={}, 결과={}건",
                propertyCode, request.getCheckIn(), request.getCheckOut(),
                request.getAdults(), request.getChildren(), result.size());

        return result;
    }

    @Override
    public PriceCheckResponse calculatePrice(String propertyCode, PriceCheckRequest request) {
        // 날짜 유효성 검증
        validateDateRange(request.getCheckIn(), request.getCheckOut());

        Property property = findPropertyByCode(propertyCode);

        // 객실타입 존재 및 프로퍼티 소속 검증
        RoomType roomType = roomTypeRepository.findById(request.getRoomTypeId())
                .orElseThrow(() -> new HolaException(ErrorCode.ROOM_TYPE_NOT_FOUND));

        if (!roomType.getPropertyId().equals(property.getId())) {
            throw new HolaException(ErrorCode.ROOM_TYPE_NOT_FOUND,
                    "해당 프로퍼티에 속하지 않는 객실 타입입니다.");
        }

        // 레이트코드-객실타입 매핑 검증
        List<RateCodeRoomType> mappings = rateCodeRoomTypeRepository.findAllByRateCodeId(request.getRateCodeId());
        boolean isMapped = mappings.stream()
                .anyMatch(m -> m.getRoomTypeId().equals(request.getRoomTypeId()));

        if (!isMapped) {
            throw new HolaException(ErrorCode.BOOKING_RATE_NOT_AVAILABLE,
                    "해당 객실 타입에 적용 가능한 레이트코드가 아닙니다.");
        }

        // Dayuse 레이트코드 여부 확인
        RateCode rateCode = rateCodeRepository.findById(request.getRateCodeId())
                .orElseThrow(() -> new HolaException(ErrorCode.RATE_CODE_NOT_FOUND));

        List<AvailableRoomTypeResponse.DailyPrice> dailyPrices;
        BigDecimal totalSupply, totalTax, totalServiceCharge, grandTotal;

        if (rateCode.isDayUse()) {
            // Dayuse: DayUseRate 테이블에서 요금 조회 + Property 봉사료/세금 적용
            List<DayUseRate> dayUseRates = dayUseRateRepository
                    .findByRateCodeIdAndUseYnTrueOrderBySortOrderAsc(request.getRateCodeId());
            if (dayUseRates.isEmpty()) {
                throw new HolaException(ErrorCode.DAY_USE_RATE_NOT_FOUND);
            }
            DayUseRate rate = dayUseRates.get(0);
            AvailableRoomTypeResponse.DailyPrice dp = buildDayUseDailyPrice(
                    rate.getSupplyPrice(), property, request.getCheckIn());
            totalSupply = dp.getSupplyPrice();
            totalServiceCharge = dp.getServiceCharge();
            totalTax = dp.getTax();
            grandTotal = dp.getTotal();
            dailyPrices = List.of(dp);
        } else {
            // 숙박: 기존 일별 요금 계산
            List<DailyCharge> dailyCharges = priceCalculationService.calculateDailyCharges(
                    request.getRateCodeId(), property,
                    request.getCheckIn(), request.getCheckOut(),
                    request.getAdults(),
                    request.getChildren() != null ? request.getChildren() : 0,
                    null);

            dailyPrices = dailyCharges.stream()
                    .map(dc -> AvailableRoomTypeResponse.DailyPrice.builder()
                            .date(dc.getChargeDate().toString())
                            .supplyPrice(dc.getSupplyPrice())
                            .tax(dc.getTax())
                            .serviceCharge(dc.getServiceCharge())
                            .total(dc.getTotal())
                            .build())
                    .toList();

            totalSupply = dailyCharges.stream()
                    .map(DailyCharge::getSupplyPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
            totalTax = dailyCharges.stream()
                    .map(DailyCharge::getTax).reduce(BigDecimal.ZERO, BigDecimal::add);
            totalServiceCharge = dailyCharges.stream()
                    .map(DailyCharge::getServiceCharge).reduce(BigDecimal.ZERO, BigDecimal::add);
            grandTotal = dailyCharges.stream()
                    .map(DailyCharge::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        // 레이트코드 정보 조회 (이름, 통화)
        RateCodeListResponse rateInfo = findRateCodeInfo(request.getRateCodeId());

        // 객실클래스명 조회 (roomTypeName 대용)
        String roomClassName = getRoomClassName(roomType.getRoomClassId());

        return PriceCheckResponse.builder()
                .roomTypeId(roomType.getId())
                .roomTypeName(roomClassName + " - " + roomType.getRoomTypeCode())
                .rateCodeId(request.getRateCodeId())
                .rateNameKo(rateInfo != null ? rateInfo.getRateNameKo() : null)
                .currency(rateInfo != null ? rateInfo.getCurrency() : "KRW")
                .dailyCharges(dailyPrices)
                .totalSupply(totalSupply)
                .totalTax(totalTax)
                .totalServiceCharge(totalServiceCharge)
                .grandTotal(grandTotal)
                .build();
    }

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
        Property property = findPropertyByCode(propertyCode);
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
            return buildConfirmationResponse(existingMaster);
        }

        Property property = findPropertyByCode(propertyCode);
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
            return buildConfirmationResponse(existingMaster);
        }

        Property property = findPropertyByCode(propertyCode);
        ReservationChannel websiteChannel = reservationChannelRepository
                .findByPropertyIdAndChannelCode(property.getId(), "WEBSITE")
                .orElse(null);

        BookingValidationResult validation = doValidateRooms(request, property, websiteChannel);

        return createBookingInternal(request, validation, paymentResult, clientIp, userAgent);
    }

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
            validateDateRange(room.getCheckIn(), room.getCheckOut());
            validateRateCodeStayDays(room.getRateCodeId(), room.getCheckIn(), room.getCheckOut());

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
                dailyCharges = List.of(buildDayUseDailyCharge(
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

                    reservationServiceItemRepository.save(ReservationServiceItem.builder()
                            .subReservation(sub)
                            .serviceType("PAID")
                            .serviceOptionId(option.getId())
                            .serviceDate(room.getCheckIn())
                            .quantity(qty)
                            .unitPrice(unitPrice)
                            .tax(tax)
                            .totalPrice(total)
                            .transactionCodeId(option.getTransactionCodeId())
                            .build());

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
        return buildConfirmationResponse(master);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingConfirmationResponse getConfirmation(String confirmationNo, String verificationValue) {
        // 1. 확인번호로 예약 조회
        MasterReservation master = masterReservationRepository.findByConfirmationNo(confirmationNo)
                .orElseThrow(() -> new HolaException(ErrorCode.BOOKING_CONFIRMATION_NOT_FOUND));

        // 2. 2차 검증 (이메일 또는 전화번호) — 값이 없으면 검증 생략 (데모)
        if (verificationValue != null && !verificationValue.isBlank()) {
            boolean verified = verificationValue.equalsIgnoreCase(master.getEmail())
                    || verificationValue.equals(master.getPhoneNumber());
            if (!verified) {
                throw new HolaException(ErrorCode.BOOKING_GUEST_VERIFICATION_FAILED);
            }
        }

        return buildConfirmationResponse(master);
    }

    // ===== Private Helper Methods =====

    /**
     * 프로퍼티 코드로 프로퍼티 조회
     */
    private Property findPropertyByCode(String propertyCode) {
        return propertyRepository.findByPropertyCodeAndUseYnTrue(propertyCode)
                .orElseThrow(() -> new HolaException(ErrorCode.BOOKING_PROPERTY_NOT_FOUND));
    }

    /**
     * 날짜 범위 유효성 검증
     * - 체크아웃 > 체크인
     * - 체크인이 과거 날짜가 아닌지
     * - 최대 30박 초과 여부
     */
    /**
     * 레이트코드 숙박일수(min/max) 검증
     */
    private void validateRateCodeStayDays(Long rateCodeId, LocalDate checkIn, LocalDate checkOut) {
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

    private void validateDateRange(LocalDate checkIn, LocalDate checkOut) {
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
     * 객실타입에 매핑된 레이트코드 기반으로 요금 옵션 목록 생성
     */
    private List<AvailableRoomTypeResponse.RateOption> buildRateOptions(
            Long roomTypeId,
            List<RateCodeListResponse> availableRateCodes,
            Property property,
            BookingSearchRequest request) {

        List<AvailableRoomTypeResponse.RateOption> rateOptions = new ArrayList<>();

        // 벌크 조회: 모든 레이트코드의 객실타입 매핑 + 포함 서비스 (N+1 방지)
        List<Long> rateCodeIds = availableRateCodes.stream().map(RateCodeListResponse::getId).toList();
        Map<Long, Set<Long>> rateToRoomTypeMap = rateCodeRoomTypeRepository.findAllByRateCodeIdIn(rateCodeIds).stream()
                .collect(Collectors.groupingBy(RateCodeRoomType::getRateCodeId,
                        Collectors.mapping(RateCodeRoomType::getRoomTypeId, Collectors.toSet())));
        Map<Long, List<RateCodePaidService>> rateToPaidServiceMap = rateCodePaidServiceRepository
                .findAllByRateCodeIdIn(rateCodeIds).stream()
                .collect(Collectors.groupingBy(RateCodePaidService::getRateCodeId));

        // 포함 서비스 옵션 벌크 조회
        Set<Long> allPaidSvcIds = rateToPaidServiceMap.values().stream()
                .flatMap(List::stream)
                .map(RateCodePaidService::getPaidServiceOptionId)
                .collect(Collectors.toSet());
        Map<Long, PaidServiceOption> paidSvcMap = allPaidSvcIds.isEmpty() ? Map.of()
                : paidServiceOptionRepository.findAllById(allPaidSvcIds).stream()
                        .collect(Collectors.toMap(PaidServiceOption::getId, s -> s));

        for (RateCodeListResponse rc : availableRateCodes) {
            // 해당 레이트코드가 이 객실타입에 매핑되어 있는지 확인
            Set<Long> mappedRoomTypeIds = rateToRoomTypeMap.getOrDefault(rc.getId(), Set.of());
            if (!mappedRoomTypeIds.contains(roomTypeId)) {
                continue;
            }

            try {
                BigDecimal totalAmount;
                List<AvailableRoomTypeResponse.DailyPrice> dailyPrices;

                // Dayuse 레이트코드: DayUseRate에서 요금 조회 (PriceCalculationService 사용 안 함)
                DayUseRate dayUseRate = null;
                if ("DAY_USE".equals(rc.getStayType())) {
                    List<DayUseRate> dayUseRates = dayUseRateRepository
                            .findByRateCodeIdAndUseYnTrueOrderBySortOrderAsc(rc.getId());
                    if (dayUseRates.isEmpty()) continue;
                    dayUseRate = dayUseRates.get(0);
                    AvailableRoomTypeResponse.DailyPrice dp = buildDayUseDailyPrice(
                            dayUseRate.getSupplyPrice(), property, request.getCheckIn());
                    totalAmount = dp.getTotal();
                    dailyPrices = List.of(dp);
                } else {
                    // 숙박: 기존 일별 요금 계산
                    List<DailyCharge> dailyCharges = priceCalculationService.calculateDailyCharges(
                            rc.getId(), property,
                            request.getCheckIn(), request.getCheckOut(),
                            request.getAdults(),
                            request.getChildren() != null ? request.getChildren() : 0,
                            null);

                    totalAmount = dailyCharges.stream()
                            .map(DailyCharge::getTotal)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    dailyPrices = dailyCharges.stream()
                            .map(dc -> AvailableRoomTypeResponse.DailyPrice.builder()
                                    .date(dc.getChargeDate().toString())
                                    .supplyPrice(dc.getSupplyPrice())
                                    .tax(dc.getTax())
                                    .serviceCharge(dc.getServiceCharge())
                                    .total(dc.getTotal())
                                    .build())
                            .toList();
                }

                // 포함 서비스 조회 (벌크 캐시에서)
                List<RateCodePaidService> serviceMappings = rateToPaidServiceMap.getOrDefault(rc.getId(), List.of());
                List<AvailableRoomTypeResponse.IncludedServiceInfo> includedServices = serviceMappings.stream()
                        .map(m -> paidSvcMap.get(m.getPaidServiceOptionId()))
                        .filter(s -> s != null && Boolean.TRUE.equals(s.getUseYn()))
                        .map(s -> AvailableRoomTypeResponse.IncludedServiceInfo.builder()
                                .serviceOptionId(s.getId())
                                .nameKo(s.getServiceNameKo())
                                .type(s.getServiceType())
                                .applicableNights(s.getApplicableNights())
                                .build())
                        .toList();

                // Dayuse 이용시간 추출 (위에서 조회한 dayUseRate 재사용)
                Integer durationHours = dayUseRate != null ? dayUseRate.getDurationHours() : null;

                rateOptions.add(AvailableRoomTypeResponse.RateOption.builder()
                        .rateCodeId(rc.getId())
                        .rateCode(rc.getRateCode())
                        .rateNameKo(rc.getRateNameKo())
                        .currency(rc.getCurrency())
                        .totalAmount(totalAmount)
                        .dailyPrices(dailyPrices)
                        .includedServices(includedServices)
                        .stayType(rc.getStayType())
                        .dayUseDurationHours(durationHours)
                        .build());

            } catch (HolaException e) {
                log.debug("레이트코드 요금 계산 실패: rateCodeId={}, roomTypeId={}, error={}",
                        rc.getId(), roomTypeId, e.getMessage());
            }
        }

        return rateOptions;
    }

    /**
     * 객실타입의 무료 서비스 목록 조회
     */
    private List<AvailableRoomTypeResponse.ServiceInfo> buildFreeServices(Long roomTypeId) {
        List<RoomTypeFreeService> mappings = roomTypeFreeServiceRepository.findAllByRoomTypeId(roomTypeId);
        if (mappings.isEmpty()) return List.of();

        // 벌크 조회 (N+1 방지)
        List<Long> svcIds = mappings.stream().map(RoomTypeFreeService::getFreeServiceOptionId).toList();
        Map<Long, FreeServiceOption> svcMap = freeServiceOptionRepository.findAllById(svcIds).stream()
                .collect(Collectors.toMap(FreeServiceOption::getId, s -> s));

        return mappings.stream()
                .map(m -> {
                    FreeServiceOption opt = svcMap.get(m.getFreeServiceOptionId());
                    if (opt == null) return null;
                    return AvailableRoomTypeResponse.ServiceInfo.builder()
                            .nameKo(opt.getServiceNameKo())
                            .type(opt.getServiceType())
                            .quantity(m.getQuantity())
                            .build();
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    /**
     * 객실 클래스명 조회
     */
    private String getRoomClassName(Long roomClassId) {
        if (roomClassId == null) return null;
        return roomClassRepository.findById(roomClassId)
                .map(RoomClass::getRoomClassName)
                .orElse(null);
    }

    /**
     * starRating 문자열 → 숫자 변환
     */
    private Integer parseStarRating(String starRating) {
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
    private RateCodeListResponse findRateCodeInfo(Long rateCodeId) {
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
     * MasterReservation → BookingConfirmationResponse 변환
     */
    private BookingConfirmationResponse buildConfirmationResponse(MasterReservation master) {
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
    private String buildPolicyDescription(com.hola.hotel.entity.CancellationFee fee) {
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
     * 부킹 감사 로그 저장
     */
    @Override
    public CancelFeePreviewResponse getCancelFeePreview(String confirmationNo, String email) {
        MasterReservation master = findAndVerifyReservation(confirmationNo, email);

        // 취소 가능 상태 확인
        String status = master.getReservationStatus();
        if ("CANCELED".equals(status)) {
            throw new HolaException(ErrorCode.BOOKING_ALREADY_CANCELED);
        }
        if (!"RESERVED".equals(status) && !"CHECK_IN".equals(status)) {
            throw new HolaException(ErrorCode.BOOKING_CANCEL_NOT_ALLOWED);
        }

        // 1박 요금 조회
        BigDecimal firstNightAmount = getFirstNightTotal(master.getId());

        // 취소 수수료 계산
        Long propertyId = master.getProperty().getId();
        var cancelResult = cancellationPolicyService.calculateCancelFee(
                propertyId, master.getMasterCheckIn(), firstNightAmount);

        // 총 결제액 조회
        BigDecimal totalPaid = BigDecimal.ZERO;
        var payment = reservationPaymentRepository.findByMasterReservationId(master.getId()).orElse(null);
        if (payment != null) {
            totalPaid = payment.getTotalPaidAmount() != null ? payment.getTotalPaidAmount() : BigDecimal.ZERO;
        }
        BigDecimal refundAmt = totalPaid.subtract(cancelResult.feeAmount()).max(BigDecimal.ZERO);

        // PG 결제 정보 조회
        boolean isPgPayment = false;
        String pgCardNo = null;
        String pgIssuerName = null;
        var txns = paymentTransactionRepository
                .findByMasterReservationIdOrderByTransactionSeqAsc(master.getId());
        var pgTxn = txns.stream()
                .filter(t -> "PAYMENT".equals(t.getTransactionType()) && t.getPgCno() != null)
                .findFirst().orElse(null);
        if (pgTxn != null) {
            isPgPayment = true;
            pgCardNo = pgTxn.getPgCardNo();
            pgIssuerName = pgTxn.getPgIssuerName();
        }

        return CancelFeePreviewResponse.builder()
                .confirmationNo(confirmationNo)
                .reservationStatus(status)
                .guestNameKo(master.getGuestNameKo())
                .checkIn(master.getMasterCheckIn().toString())
                .checkOut(master.getMasterCheckOut().toString())
                .firstNightAmount(firstNightAmount)
                .cancelFeeAmount(cancelResult.feeAmount())
                .cancelFeePercent(cancelResult.feePercent())
                .totalPaidAmount(totalPaid)
                .refundAmount(refundAmt)
                .policyDescription(cancelResult.policyDescription())
                .pgPayment(isPgPayment)
                .pgCardNo(pgCardNo)
                .pgIssuerName(pgIssuerName)
                .build();
    }

    @Override
    @Transactional
    public CancelBookingResponse cancelBooking(String confirmationNo, String email,
                                                String clientIp, String userAgent) {
        MasterReservation master = findAndVerifyReservation(confirmationNo, email);

        // 취소 가능 상태 확인
        String status = master.getReservationStatus();
        if ("CANCELED".equals(status)) {
            throw new HolaException(ErrorCode.BOOKING_ALREADY_CANCELED);
        }
        if (!"RESERVED".equals(status) && !"CHECK_IN".equals(status)) {
            throw new HolaException(ErrorCode.BOOKING_CANCEL_NOT_ALLOWED);
        }

        // 취소 수수료 계산
        BigDecimal firstNightAmount = getFirstNightTotal(master.getId());
        Long propertyId = master.getProperty().getId();
        var cancelResult = cancellationPolicyService.calculateCancelFee(
                propertyId, master.getMasterCheckIn(), firstNightAmount);

        // 결제 정보 업데이트
        BigDecimal cancelFee = cancelResult.feeAmount();
        BigDecimal refundAmt = BigDecimal.ZERO;
        Boolean pgRefundSuccess = null;
        String pgRefundApprovalNo = null;
        var payment = reservationPaymentRepository.findByMasterReservationId(master.getId()).orElse(null);
        if (payment != null) {
            BigDecimal totalPaid = payment.getTotalPaidAmount() != null ? payment.getTotalPaidAmount() : BigDecimal.ZERO;
            BigDecimal existingRefund = payment.getRefundAmount() != null ? payment.getRefundAmount() : BigDecimal.ZERO;
            BigDecimal existingCancelFee = payment.getCancelFeeAmount() != null ? payment.getCancelFeeAmount() : BigDecimal.ZERO;
            BigDecimal netPaid = totalPaid.subtract(existingRefund).subtract(existingCancelFee);
            refundAmt = netPaid.subtract(cancelFee).max(BigDecimal.ZERO);
            payment.updateCancelRefund(cancelFee, refundAmt);

            // PG 환불 포함 REFUND 거래 기록
            if (refundAmt.compareTo(BigDecimal.ZERO) > 0 || cancelFee.compareTo(BigDecimal.ZERO) > 0) {
                var refundTxn = reservationPaymentService.processRefundWithPg(
                        master.getId(), refundAmt, cancelFee,
                        "게스트 자가 취소 환불 (수수료: " + cancelFee + "원)");
                // PG 환불 결과 저장 (응답 DTO에 반영)
                if (refundTxn != null && refundTxn.getPgCno() != null) {
                    pgRefundSuccess = "COMPLETED".equals(refundTxn.getTransactionStatus());
                    pgRefundApprovalNo = refundTxn.getPgApprovalNo();
                }
            }

            // 취소 후 REFUNDED 상태로 확정 (grandTotal은 원본 유지 — 감사 추적용)
            payment.setPaymentStatusRefunded();
        }

        // 서비스 항목 재고 복원
        for (SubReservation sub : master.getSubReservations()) {
            List<ReservationServiceItem> serviceItems = reservationServiceItemRepository
                    .findBySubReservationId(sub.getId());
            for (ReservationServiceItem item : serviceItems) {
                if (item.getServiceOptionId() != null) {
                    paidServiceOptionRepository.findById(item.getServiceOptionId()).ifPresent(option -> {
                        if (option.getInventoryItemId() != null) {
                            inventoryService.releaseInventory(
                                    option.getInventoryItemId(), sub.getCheckIn(), sub.getCheckOut(),
                                    item.getQuantity());
                        }
                    });
                }
            }
        }

        // 상태 변경
        master.updateStatus("CANCELED");
        for (SubReservation sub : master.getSubReservations()) {
            sub.updateStatus("CANCELED");
        }

        // 감사 로그 기록 (STEP 6)
        java.util.Map<String, Object> auditData = new java.util.HashMap<>();
        auditData.put("verificationValue", email != null ? email : "");
        auditData.put("cancelFee", cancelFee);
        auditData.put("refund", refundAmt);
        saveAuditLog(master.getId(), confirmationNo, "BOOKING_CANCELED", "WEBSITE",
                auditData, null, clientIp, userAgent, null);

        log.info("게스트 자가 취소: confirmationNo={}, cancelFee={}, refund={}",
                confirmationNo, cancelFee, refundAmt);

        return CancelBookingResponse.builder()
                .confirmationNo(confirmationNo)
                .status("CANCELED")
                .cancelFeeAmount(cancelFee)
                .refundAmount(refundAmt)
                .pgRefundSuccess(pgRefundSuccess)
                .pgRefundApprovalNo(pgRefundApprovalNo)
                .build();
    }

    @Override
    public List<BookingLookupResponse> lookupReservations(String email, String lastName) {
        if (email == null || email.isBlank() || lastName == null || lastName.isBlank()) {
            throw new HolaException(ErrorCode.BOOKING_GUEST_VERIFICATION_FAILED,
                    "이메일과 영문 성(Last Name)이 필요합니다.");
        }

        List<MasterReservation> reservations = masterReservationRepository.findByEmailAndLastName(email, lastName);
        if (reservations.isEmpty()) {
            throw new HolaException(ErrorCode.BOOKING_CONFIRMATION_NOT_FOUND,
                    "일치하는 예약을 찾을 수 없습니다.");
        }

        return reservations.stream()
                .map(master -> {
                    // 영문 이름 조합
                    String nameEn = buildEnglishName(master);

                    // 총 결제금액 조회
                    BigDecimal totalAmount = BigDecimal.ZERO;
                    var payment = reservationPaymentRepository.findByMasterReservationId(master.getId()).orElse(null);
                    if (payment != null && payment.getTotalPaidAmount() != null) {
                        totalAmount = payment.getTotalPaidAmount();
                    }

                    Property property = master.getProperty();
                    return BookingLookupResponse.builder()
                            .confirmationNo(master.getConfirmationNo())
                            .reservationStatus(master.getReservationStatus())
                            .guestNameKo(master.getGuestNameKo())
                            .guestNameEn(nameEn)
                            .propertyName(property.getPropertyName())
                            .propertyCode(property.getPropertyCode())
                            .checkIn(master.getMasterCheckIn())
                            .checkOut(master.getMasterCheckOut())
                            .roomCount(master.getSubReservations().size())
                            .totalAmount(totalAmount)
                            .currency("KRW")
                            .createdAt(master.getCreatedAt())
                            .build();
                })
                .toList();
    }

    @Override
    @Transactional
    public BookingModifyResponse modifyBooking(String confirmationNo, BookingModifyRequest request) {
        // 1. 이메일 검증 + 예약 조회
        MasterReservation master = findAndVerifyReservation(confirmationNo, request.getEmail());

        // 2. 수정 가능한 상태인지 확인 (RESERVED만 수정 가능)
        if (!"RESERVED".equals(master.getReservationStatus())) {
            throw new HolaException(ErrorCode.BOOKING_CANCEL_NOT_ALLOWED,
                    "예약 대기(RESERVED) 상태에서만 수정이 가능합니다.");
        }

        // 3. 날짜 유효성 검증
        validateDateRange(request.getCheckIn(), request.getCheckOut());

        Property property = master.getProperty();

        // 4. 기존 총액 계산
        BigDecimal previousAmount = BigDecimal.ZERO;
        var existingPayment = reservationPaymentRepository.findByMasterReservationId(master.getId()).orElse(null);
        if (existingPayment != null && existingPayment.getTotalPaidAmount() != null) {
            previousAmount = existingPayment.getTotalPaidAmount();
        }

        // 5. 서브예약 순회하며 날짜/인원 변경 + 요금 재계산
        BigDecimal newTotalAmount = BigDecimal.ZERO;
        for (SubReservation sub : master.getSubReservations()) {
            int newAdults = request.getAdults() != null ? request.getAdults() : sub.getAdults();
            int newChildren = request.getChildren() != null ? request.getChildren() : sub.getChildren();

            // 가용성 확인 (자기 자신 제외하여 날짜 변경 시 false negative 방지)
            int availableCount = roomAvailabilityService.getAvailableRoomCount(
                    sub.getRoomTypeId(), request.getCheckIn(), request.getCheckOut(),
                    List.of(sub.getId()));
            if (availableCount <= 0) {
                throw new HolaException(ErrorCode.BOOKING_NO_AVAILABILITY);
            }

            // 서브예약 날짜/인원 업데이트
            sub.update(sub.getRoomTypeId(), sub.getFloorId(), sub.getRoomNumberId(),
                    newAdults, newChildren, request.getCheckIn(), request.getCheckOut(),
                    sub.getEarlyCheckIn(), sub.getLateCheckOut());

            // 기존 일별 요금 삭제 후 재계산 (orphanRemoval=true이므로 clear()+flush() 패턴 사용)
            sub.getDailyCharges().clear();
            dailyChargeRepository.flush();

            // Dayuse 레이트코드 분기
            RateCode modifyRateCode = rateCodeRepository.findById(master.getRateCodeId()).orElse(null);
            List<DailyCharge> newCharges;
            if (modifyRateCode != null && modifyRateCode.isDayUse()) {
                // Dayuse: DayUseRate 기반 요금 (봉사료/세금 포함)
                List<DayUseRate> dayUseRates = dayUseRateRepository
                        .findByRateCodeIdAndUseYnTrueOrderBySortOrderAsc(master.getRateCodeId());
                if (dayUseRates.isEmpty()) {
                    throw new HolaException(ErrorCode.DAY_USE_RATE_NOT_FOUND);
                }
                DailyCharge dc = buildDayUseDailyCharge(
                        dayUseRates.get(0).getSupplyPrice(), property, request.getCheckIn());
                newCharges = List.of(DailyCharge.builder()
                        .subReservation(sub)
                        .chargeDate(dc.getChargeDate())
                        .supplyPrice(dc.getSupplyPrice())
                        .serviceCharge(dc.getServiceCharge())
                        .tax(dc.getTax())
                        .total(dc.getTotal())
                        .build());
            } else {
                // 숙박: 기존 일별 요금 계산
                newCharges = priceCalculationService.calculateDailyCharges(
                        master.getRateCodeId(), property,
                        request.getCheckIn(), request.getCheckOut(),
                        newAdults, newChildren, sub);
            }
            sub.getDailyCharges().addAll(newCharges);

            BigDecimal subTotal = newCharges.stream()
                    .map(DailyCharge::getTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            newTotalAmount = newTotalAmount.add(subTotal);
        }

        // 6. 마스터 예약 날짜 동기화
        master.syncDates(request.getCheckIn(), request.getCheckOut());

        // 게스트 정보 변경 (제공된 경우만)
        if (request.getGuestNameKo() != null) {
            master.update(request.getCheckIn(), request.getCheckOut(),
                    request.getGuestNameKo(),
                    master.getGuestFirstNameEn(), master.getGuestMiddleNameEn(), master.getGuestLastNameEn(),
                    master.getPhoneCountryCode(),
                    request.getPhoneNumber() != null ? request.getPhoneNumber() : master.getPhoneNumber(),
                    master.getEmail(),
                    master.getBirthDate(), master.getGender(), master.getNationality(),
                    master.getRateCodeId(), master.getMarketCodeId(), master.getReservationChannelId(),
                    master.getPromotionType(), master.getPromotionCode(),
                    master.getOtaReservationNo(), master.getIsOtaManaged(),
                    request.getCustomerRequest() != null ? request.getCustomerRequest() : master.getCustomerRequest());
        }

        // 7. 차액 계산
        BigDecimal priceDifference = newTotalAmount.subtract(previousAmount);
        String message;
        if (priceDifference.compareTo(BigDecimal.ZERO) > 0) {
            message = "추가 결제가 필요합니다: " + priceDifference.toPlainString() + "원";
        } else if (priceDifference.compareTo(BigDecimal.ZERO) < 0) {
            message = "차액 환불 예정: " + priceDifference.abs().toPlainString() + "원";
        } else {
            message = "요금 변동 없이 예약이 수정되었습니다.";
        }

        log.info("예약 수정 완료: confirmationNo={}, 이전={}, 신규={}, 차액={}",
                confirmationNo, previousAmount, newTotalAmount, priceDifference);

        // 결제 정보 재계산
        reservationPaymentService.recalculatePayment(master.getId());

        return BookingModifyResponse.builder()
                .confirmationNo(confirmationNo)
                .status("MODIFIED")
                .checkIn(request.getCheckIn())
                .checkOut(request.getCheckOut())
                .adults(master.getSubReservations().isEmpty() ? 0 : master.getSubReservations().get(0).getAdults())
                .children(master.getSubReservations().isEmpty() ? 0 : master.getSubReservations().get(0).getChildren())
                .previousAmount(previousAmount)
                .newAmount(newTotalAmount)
                .priceDifference(priceDifference)
                .message(message)
                .build();
    }

    @Override
    public List<PropertyImageResponse> getPropertyImages(String propertyCode) {
        Property property = findPropertyByCode(propertyCode);
        return propertyImageRepository
                .findAllByPropertyIdOrderByImageTypeAscSortOrderAsc(property.getId())
                .stream()
                .filter(img -> Boolean.TRUE.equals(img.getUseYn()))
                .map(this::toImageResponse)
                .toList();
    }

    @Override
    public List<PropertyImageResponse> getRoomTypeImages(String propertyCode, Long roomTypeId) {
        Property property = findPropertyByCode(propertyCode);
        return propertyImageRepository
                .findAllByPropertyIdAndImageTypeAndReferenceIdOrderBySortOrderAsc(
                        property.getId(), "ROOM_TYPE", roomTypeId)
                .stream()
                .filter(img -> Boolean.TRUE.equals(img.getUseYn()))
                .map(this::toImageResponse)
                .toList();
    }

    private PropertyImageResponse toImageResponse(PropertyImage img) {
        return PropertyImageResponse.builder()
                .imageId(img.getId())
                .imageType(img.getImageType())
                .imagePath(img.getImagePath())
                .imageName(img.getImageName())
                .altText(img.getAltText())
                .sortOrder(img.getSortOrder())
                .build();
    }

    @Override
    public List<PropertyTermsResponse> getTerms(String propertyCode) {
        Property property = findPropertyByCode(propertyCode);
        return propertyTermsRepository
                .findAllByPropertyIdOrderBySortOrderAsc(property.getId())
                .stream()
                .filter(t -> Boolean.TRUE.equals(t.getUseYn()))
                .map(t -> PropertyTermsResponse.builder()
                        .termsId(t.getId())
                        .termsType(t.getTermsType())
                        .titleKo(t.getTitleKo())
                        .titleEn(t.getTitleEn())
                        .contentKo(t.getContentKo())
                        .contentEn(t.getContentEn())
                        .version(t.getVersion())
                        .required(Boolean.TRUE.equals(t.getRequired()))
                        .build())
                .toList();
    }

    @Override
    public List<AddOnServiceResponse> getAddOnServices(String propertyCode) {
        Property property = findPropertyByCode(propertyCode);

        List<PaidServiceOption> paidServices = paidServiceOptionRepository
                .findAllByPropertyIdOrderBySortOrderAscServiceNameKoAsc(property.getId());

        return paidServices.stream()
                .filter(s -> Boolean.TRUE.equals(s.getUseYn()))
                .map(s -> AddOnServiceResponse.builder()
                        .serviceId(s.getId())
                        .serviceCode(s.getServiceOptionCode())
                        .serviceNameKo(s.getServiceNameKo())
                        .serviceNameEn(s.getServiceNameEn())
                        .serviceType(s.getServiceType())
                        .applicableNights(s.getApplicableNights())
                        .currencyCode(s.getCurrencyCode())
                        .price(s.getVatIncludedPrice())
                        .supplyPrice(s.getSupplyPrice())
                        .taxAmount(s.getTaxAmount())
                        .quantity(s.getQuantity())
                        .quantityUnit(s.getQuantityUnit())
                        .build())
                .toList();
    }

    @Override
    public PromotionValidationResponse validatePromotionCode(String propertyCode, String code,
                                                              LocalDate checkIn, LocalDate checkOut) {
        Property property = findPropertyByCode(propertyCode);

        // 프로모션 코드 조회
        List<PromotionCode> promotions = promotionCodeRepository
                .findAllByPropertyIdOrderBySortOrderAscPromotionCodeAsc(property.getId());

        PromotionCode matched = promotions.stream()
                .filter(p -> code.equalsIgnoreCase(p.getPromotionCode()))
                .findFirst()
                .orElse(null);

        if (matched == null) {
            return PromotionValidationResponse.builder()
                    .promotionCode(code)
                    .valid(false)
                    .invalidReason("존재하지 않는 프로모션 코드입니다.")
                    .build();
        }

        // 사용 여부 확인
        if (!Boolean.TRUE.equals(matched.getUseYn())) {
            return PromotionValidationResponse.builder()
                    .promotionCode(code)
                    .valid(false)
                    .invalidReason("비활성화된 프로모션 코드입니다.")
                    .build();
        }

        // 기간 유효성 확인
        LocalDate today = LocalDate.now();
        LocalDate targetDate = checkIn != null ? checkIn : today;

        if (targetDate.isBefore(matched.getPromotionStartDate()) || targetDate.isAfter(matched.getPromotionEndDate())) {
            return PromotionValidationResponse.builder()
                    .promotionCode(code)
                    .valid(false)
                    .promotionType(matched.getPromotionType())
                    .startDate(matched.getPromotionStartDate())
                    .endDate(matched.getPromotionEndDate())
                    .invalidReason("프로모션 적용 기간이 아닙니다. (" +
                            matched.getPromotionStartDate() + " ~ " + matched.getPromotionEndDate() + ")")
                    .build();
        }

        return PromotionValidationResponse.builder()
                .promotionCode(code)
                .valid(true)
                .promotionType(matched.getPromotionType())
                .descriptionKo(matched.getDescriptionKo())
                .descriptionEn(matched.getDescriptionEn())
                .downUpSign(matched.getDownUpSign())
                .downUpValue(matched.getDownUpValue())
                .downUpUnit(matched.getDownUpUnit())
                .startDate(matched.getPromotionStartDate())
                .endDate(matched.getPromotionEndDate())
                .build();
    }

    /**
     * 영문 이름 조합 (FirstName + MiddleName + LastName)
     */
    private String buildEnglishName(MasterReservation master) {
        StringBuilder sb = new StringBuilder();
        if (master.getGuestFirstNameEn() != null) sb.append(master.getGuestFirstNameEn());
        if (master.getGuestMiddleNameEn() != null) sb.append(" ").append(master.getGuestMiddleNameEn());
        if (master.getGuestLastNameEn() != null) sb.append(" ").append(master.getGuestLastNameEn());
        return sb.toString().trim();
    }

    /**
     * 확인번호 + 이메일로 예약 조회 및 검증
     */
    private MasterReservation findAndVerifyReservation(String confirmationNo, String verificationValue) {
        MasterReservation master = masterReservationRepository.findByConfirmationNo(confirmationNo)
                .orElseThrow(() -> new HolaException(ErrorCode.BOOKING_CONFIRMATION_NOT_FOUND));

        // 이메일 또는 전화번호 검증 — 값이 없으면 검증 생략 (데모)
        if (verificationValue != null && !verificationValue.isBlank()) {
            boolean verified = verificationValue.equalsIgnoreCase(master.getEmail())
                    || verificationValue.equals(master.getPhoneNumber());
            if (!verified) {
                throw new HolaException(ErrorCode.BOOKING_GUEST_VERIFICATION_FAILED);
            }
        }

        return master;
    }

    /**
     * 취소/노쇼 수수료 기준 1박 총액 조회
     * 원본 1박 총액(originalFirstNightTotal) 우선, 없으면 DailyCharge에서 조회 (하위호환)
     */
    private BigDecimal getFirstNightTotal(Long masterReservationId) {
        // 1. 원본 1박 총액 우선 (업그레이드 전 요금)
        var payment = reservationPaymentRepository
                .findByMasterReservationId(masterReservationId).orElse(null);
        if (payment != null && payment.getOriginalFirstNightTotal() != null
                && payment.getOriginalFirstNightTotal().compareTo(BigDecimal.ZERO) > 0) {
            return payment.getOriginalFirstNightTotal();
        }

        // 2. 하위호환: DailyCharge에서 조회
        List<SubReservation> subs = subReservationRepository.findByMasterReservationId(masterReservationId);
        if (subs.isEmpty()) return BigDecimal.ZERO;

        List<DailyCharge> charges = dailyChargeRepository.findBySubReservationId(subs.get(0).getId());
        if (charges.isEmpty()) return BigDecimal.ZERO;

        DailyCharge first = charges.get(0);
        if (first.getTotal() != null) {
            return first.getTotal();
        }
        BigDecimal supply = first.getSupplyPrice() != null ? first.getSupplyPrice() : BigDecimal.ZERO;
        BigDecimal tax = first.getTax() != null ? first.getTax() : BigDecimal.ZERO;
        BigDecimal svc = first.getServiceCharge() != null ? first.getServiceCharge() : BigDecimal.ZERO;
        return supply.add(tax).add(svc);
    }

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

    // ─── 패키지(레이트플랜) 목록 API (산하 2.4 대응) ───

    @Override
    public List<RatePlanListResponse> getRatePlans(String propertyCode, LocalDate checkIn, LocalDate checkOut,
                                                    Integer adults, Integer children, String promotionCode) {
        Property property = findPropertyByCode(propertyCode);
        Long propertyId = property.getId();

        // 기본값 설정
        final int adultsCount = (adults != null) ? adults : 2;
        final int childrenCount = (children != null) ? children : 0;

        // 해당 기간 적용 가능 레이트코드 조회
        List<RateCodeListResponse> availableRateCodes = rateCodeService.getAvailableRateCodes(
                propertyId, checkIn, checkOut);

        // 프로모션 코드 필터링 (있으면)
        if (promotionCode != null && !promotionCode.isBlank()) {
            availableRateCodes = availableRateCodes.stream()
                    .filter(rc -> promotionCode.equalsIgnoreCase(rc.getRateCode()))
                    .toList();
        }

        // 활성 객실타입 조회
        List<RoomType> roomTypes = roomTypeRepository.findAllByPropertyIdOrderBySortOrderAscRoomTypeCodeAsc(propertyId)
                .stream()
                .filter(rt -> Boolean.TRUE.equals(rt.getUseYn()))
                .filter(rt -> adultsCount <= rt.getMaxAdults())
                .toList();

        List<RatePlanListResponse> result = new ArrayList<>();

        for (RateCodeListResponse rc : availableRateCodes) {
            // 레이트코드에 매핑된 객실타입 목록
            List<RateCodeRoomType> mappings = rateCodeRoomTypeRepository.findAllByRateCodeId(rc.getId());
            Set<Long> mappedRoomTypeIds = mappings.stream()
                    .map(RateCodeRoomType::getRoomTypeId)
                    .collect(Collectors.toSet());

            List<RatePlanListResponse.RoomTypeInfo> roomTypeInfos = new ArrayList<>();
            Long minPrice = null;

            for (RoomType roomType : roomTypes) {
                if (!mappedRoomTypeIds.contains(roomType.getId())) continue;

                // 가용성 확인
                int available = roomAvailabilityService.getAvailableRoomCount(
                        roomType.getId(), checkIn, checkOut);
                if (available <= 0) continue;

                // 1박 요금 계산
                Long pricePerNight = null;
                try {
                    List<DailyCharge> charges = priceCalculationService.calculateDailyCharges(
                            rc.getId(), property, checkIn, checkOut, adultsCount, childrenCount, null);
                    if (!charges.isEmpty()) {
                        pricePerNight = charges.get(0).getTotal().longValue();
                        if (minPrice == null || pricePerNight < minPrice) {
                            minPrice = pricePerNight;
                        }
                    }
                } catch (Exception e) {
                    // 요금 계산 실패 시 무시
                }

                String roomClassName = getRoomClassName(roomType.getRoomClassId());

                roomTypeInfos.add(RatePlanListResponse.RoomTypeInfo.builder()
                        .roomTypeId(roomType.getId())
                        .roomTypeCode(roomType.getRoomTypeCode())
                        .roomClassName(roomClassName)
                        .pricePerNight(pricePerNight)
                        .build());
            }

            if (roomTypeInfos.isEmpty()) continue;

            // 레이트코드 상세 조회
            RateCode rateCode = rateCodeRepository.findById(rc.getId()).orElse(null);

            result.add(RatePlanListResponse.builder()
                    .ratePlanId(rc.getId())
                    .rateCode(rc.getRateCode())
                    .ratePlanName(rc.getRateNameKo())
                    .ratePlanNameEn(rateCode != null ? rateCode.getRateNameEn() : null)
                    .category(rc.getRateCategory())
                    .currency(rc.getCurrency())
                    .minPrice(minPrice)
                    .minStayDays(rateCode != null ? rateCode.getMinStayDays() : null)
                    .maxStayDays(rateCode != null ? rateCode.getMaxStayDays() : null)
                    .roomTypeCount(roomTypeInfos.size())
                    .roomTypes(roomTypeInfos)
                    .build());
        }

        // 최저가 순 정렬
        result.sort((a, b) -> {
            if (a.getMinPrice() == null) return 1;
            if (b.getMinPrice() == null) return -1;
            return Long.compare(a.getMinPrice(), b.getMinPrice());
        });

        return result;
    }

    // ─── 객실 상세 API (산하 2.8 대응) ───

    @Override
    public RoomDetailResponse getRoomDetail(String propertyCode, Long roomTypeId) {
        Property property = findPropertyByCode(propertyCode);

        RoomType roomType = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new HolaException(ErrorCode.ROOM_TYPE_NOT_FOUND));

        // 프로퍼티 소속 검증
        if (!roomType.getPropertyId().equals(property.getId())) {
            throw new HolaException(ErrorCode.ROOM_TYPE_NOT_FOUND);
        }

        // 객실 클래스 정보
        RoomClass roomClass = roomClassRepository.findById(roomType.getRoomClassId()).orElse(null);

        // 무료 서비스(어메니티) 조회
        List<RoomTypeFreeService> freeServiceMappings =
                roomTypeFreeServiceRepository.findAllByRoomTypeId(roomTypeId);
        List<Long> freeServiceIds = freeServiceMappings.stream()
                .map(RoomTypeFreeService::getFreeServiceOptionId)
                .toList();
        List<FreeServiceOption> freeServices = freeServiceIds.isEmpty()
                ? List.of()
                : freeServiceOptionRepository.findAllById(freeServiceIds);

        List<RoomDetailResponse.AmenityInfo> amenities = freeServices.stream()
                .map(fs -> RoomDetailResponse.AmenityInfo.builder()
                        .serviceId(fs.getId())
                        .serviceName(fs.getServiceNameKo())
                        .serviceCategory(fs.getServiceType())
                        .build())
                .toList();

        // 시설 목록 분리
        List<String> featureList = roomType.getFeatures() != null
                ? java.util.Arrays.stream(roomType.getFeatures().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList()
                : List.of();

        // 현재 가용 객실 수
        int totalRoomCount = roomAvailabilityService.getAvailableRoomCount(
                roomTypeId, LocalDate.now(), LocalDate.now().plusDays(1));

        return RoomDetailResponse.builder()
                .roomTypeId(roomType.getId())
                .roomTypeCode(roomType.getRoomTypeCode())
                .roomClassName(roomClass != null ? roomClass.getRoomClassName() : null)
                .roomClassDescription(roomClass != null ? roomClass.getDescription() : null)
                .description(roomType.getDescription())
                .roomSize(roomType.getRoomSize())
                .features(roomType.getFeatures())
                .featureList(featureList)
                .maxAdults(roomType.getMaxAdults())
                .maxChildren(roomType.getMaxChildren())
                .extraBedYn(roomType.getExtraBedYn())
                .totalRoomCount((int) totalRoomCount)
                .amenities(amenities)
                .build();
    }

    // ─── 패키지(레이트플랜) 상세 API (산하 2.5 대응) ───

    @Override
    public RatePlanDetailResponse getRatePlanDetail(String propertyCode, Long ratePlanId) {
        Property property = findPropertyByCode(propertyCode);

        RateCode rateCode = rateCodeRepository.findById(ratePlanId)
                .orElseThrow(() -> new HolaException(ErrorCode.RATE_CODE_NOT_FOUND));

        // 프로퍼티 소속 검증
        if (!rateCode.getPropertyId().equals(property.getId())) {
            throw new HolaException(ErrorCode.RATE_CODE_NOT_FOUND);
        }

        // 취소 정책 조회
        List<com.hola.hotel.entity.CancellationFee> fees =
                cancellationFeeRepository.findAllByPropertyIdOrderBySortOrder(property.getId());

        List<RatePlanDetailResponse.CancellationPolicyInfo> cancellationPolicies = fees.stream()
                .filter(f -> !"NO_SHOW".equals(f.getCheckinBasis()))
                .map(f -> RatePlanDetailResponse.CancellationPolicyInfo.builder()
                        .basis(f.getCheckinBasis())
                        .daysBefore(f.getDaysBefore())
                        .feeAmount(f.getFeeAmount())
                        .feeType(f.getFeeType())
                        .build())
                .toList();

        // 노쇼 정책 조회
        RatePlanDetailResponse.NoShowPolicyInfo noShowPolicy = fees.stream()
                .filter(f -> "NO_SHOW".equals(f.getCheckinBasis()))
                .findFirst()
                .map(f -> RatePlanDetailResponse.NoShowPolicyInfo.builder()
                        .feeAmount(f.getFeeAmount())
                        .feeType(f.getFeeType())
                        .build())
                .orElse(null);

        return RatePlanDetailResponse.builder()
                .ratePlanId(rateCode.getId())
                .rateCode(rateCode.getRateCode())
                .ratePlanName(rateCode.getRateNameKo())
                .ratePlanNameEn(rateCode.getRateNameEn())
                .category(rateCode.getRateCategory())
                .currency(rateCode.getCurrency())
                .saleStartDate(rateCode.getSaleStartDate())
                .saleEndDate(rateCode.getSaleEndDate())
                .minStayDays(rateCode.getMinStayDays())
                .maxStayDays(rateCode.getMaxStayDays())
                .cancellationPolicies(cancellationPolicies)
                .noShowPolicy(noShowPolicy)
                .build();
    }

    // ─── 캘린더 API (산하 2.2 + 2.3 통합) ───

    @Override
    public CalendarResponse getCalendar(String propertyCode, LocalDate startDate, LocalDate endDate, String type) {
        Property property = findPropertyByCode(propertyCode);
        Long propertyId = property.getId();

        // 기본값: 오늘부터 90일
        if (startDate == null) startDate = LocalDate.now();
        if (endDate == null) endDate = startDate.plusDays(90);

        // 프로퍼티의 활성 객실타입 조회
        List<RoomType> roomTypes = roomTypeRepository.findAllByPropertyIdOrderBySortOrderAscRoomTypeCodeAsc(propertyId)
                .stream()
                .filter(rt -> Boolean.TRUE.equals(rt.getUseYn()))
                .toList();

        // 해당 기간에 적용 가능한 레이트코드 조회
        List<RateCodeListResponse> availableRateCodes = rateCodeService.getAvailableRateCodes(
                propertyId, startDate, endDate);

        List<CalendarResponse.DateAvailability> dates = new ArrayList<>();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            int availableRoomTypeCount = 0;
            Long minPrice = null;

            for (RoomType roomType : roomTypes) {
                // 1박 기준 가용성 확인
                int available = roomAvailabilityService.getAvailableRoomCount(
                        roomType.getId(), date, date.plusDays(1));
                if (available <= 0) continue;

                availableRoomTypeCount++;

                // 해당 객실타입의 최저 요금 계산
                for (RateCodeListResponse rc : availableRateCodes) {
                    // 레이트코드-객실타입 매핑 확인
                    boolean mapped = rateCodeRoomTypeRepository.findAllByRateCodeId(rc.getId())
                            .stream()
                            .anyMatch(m -> m.getRoomTypeId().equals(roomType.getId()));
                    if (!mapped) continue;

                    try {
                        List<com.hola.reservation.entity.DailyCharge> charges =
                                priceCalculationService.calculateDailyCharges(
                                        rc.getId(), property, date, date.plusDays(1), 2, 0, null);
                        if (!charges.isEmpty()) {
                            long priceVal = charges.get(0).getTotal().longValue();
                            if (minPrice == null || priceVal < minPrice) {
                                minPrice = priceVal;
                            }
                        }
                    } catch (Exception e) {
                        // 요금 계산 실패 시 무시 (해당 날짜 요금표 없을 수 있음)
                    }
                }
            }

            boolean checkInAvailable = availableRoomTypeCount > 0;
            // 체크아웃 가능 = 전날 체크인 가능 (첫날은 false)
            boolean checkOutAvailable = !date.equals(startDate) && availableRoomTypeCount > 0;

            // type 필터링
            if ("checkin".equalsIgnoreCase(type) && !checkInAvailable) continue;
            if ("checkout".equalsIgnoreCase(type) && !checkOutAvailable) continue;

            dates.add(CalendarResponse.DateAvailability.builder()
                    .date(date)
                    .checkInAvailable(checkInAvailable)
                    .checkOutAvailable(checkOutAvailable)
                    .availableRoomTypes(availableRoomTypeCount)
                    .minPrice(minPrice)
                    .build());
        }

        return CalendarResponse.builder()
                .propertyCode(propertyCode)
                .startDate(startDate)
                .endDate(endDate)
                .dates(dates)
                .build();
    }

    // ─── 객실별 적용가능 레이트플랜 역방향 조회 ───

    @Override
    public List<RatePlanListResponse> getRatePlansByRoomType(String propertyCode, Long roomTypeId,
                                                              LocalDate checkIn, LocalDate checkOut,
                                                              Integer adults, Integer children) {
        Property property = findPropertyByCode(propertyCode);

        // 객실타입 존재 + 소속 검증
        RoomType roomType = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new HolaException(ErrorCode.ROOM_TYPE_NOT_FOUND));
        if (!roomType.getPropertyId().equals(property.getId())) {
            throw new HolaException(ErrorCode.ROOM_TYPE_NOT_FOUND);
        }

        final int adultsCount = (adults != null) ? adults : 2;
        final int childrenCount = (children != null) ? children : 0;

        // 인원수 초과 시 빈 목록
        if (adultsCount > roomType.getMaxAdults()) {
            return List.of();
        }

        // 해당 roomType에 매핑된 rateCodeId 목록
        Set<Long> mappedRateCodeIds = rateCodeRoomTypeRepository.findAllByRoomTypeId(roomTypeId)
                .stream()
                .map(RateCodeRoomType::getRateCodeId)
                .collect(Collectors.toSet());

        if (mappedRateCodeIds.isEmpty()) {
            return List.of();
        }

        // 기간 유효 레이트코드 조회
        List<RateCodeListResponse> availableRateCodes = rateCodeService.getAvailableRateCodes(
                property.getId(), checkIn, checkOut);

        // 교집합 필터 (해당 roomType에 매핑된 것만)
        availableRateCodes = availableRateCodes.stream()
                .filter(rc -> mappedRateCodeIds.contains(rc.getId()))
                .toList();

        // 가용성 확인
        int available = roomAvailabilityService.getAvailableRoomCount(roomTypeId, checkIn, checkOut);
        if (available <= 0) {
            return List.of();
        }

        String roomClassName = getRoomClassName(roomType.getRoomClassId());
        List<RatePlanListResponse> result = new ArrayList<>();

        for (RateCodeListResponse rc : availableRateCodes) {
            // 1박 요금 계산
            Long pricePerNight = null;
            try {
                List<DailyCharge> charges = priceCalculationService.calculateDailyCharges(
                        rc.getId(), property, checkIn, checkOut, adultsCount, childrenCount, null);
                if (!charges.isEmpty()) {
                    pricePerNight = charges.get(0).getTotal().longValue();
                }
            } catch (Exception e) {
                // 요금 계산 실패 시 무시
            }

            RateCode rateCode = rateCodeRepository.findById(rc.getId()).orElse(null);

            RatePlanListResponse.RoomTypeInfo roomTypeInfo = RatePlanListResponse.RoomTypeInfo.builder()
                    .roomTypeId(roomType.getId())
                    .roomTypeCode(roomType.getRoomTypeCode())
                    .roomClassName(roomClassName)
                    .pricePerNight(pricePerNight)
                    .build();

            result.add(RatePlanListResponse.builder()
                    .ratePlanId(rc.getId())
                    .rateCode(rc.getRateCode())
                    .ratePlanName(rc.getRateNameKo())
                    .ratePlanNameEn(rateCode != null ? rateCode.getRateNameEn() : null)
                    .category(rc.getRateCategory())
                    .currency(rc.getCurrency())
                    .minPrice(pricePerNight)
                    .minStayDays(rateCode != null ? rateCode.getMinStayDays() : null)
                    .maxStayDays(rateCode != null ? rateCode.getMaxStayDays() : null)
                    .roomTypeCount(1)
                    .roomTypes(List.of(roomTypeInfo))
                    .build());
        }

        // 최저가 순 정렬
        result.sort((a, b) -> {
            if (a.getMinPrice() == null) return 1;
            if (b.getMinPrice() == null) return -1;
            return Long.compare(a.getMinPrice(), b.getMinPrice());
        });

        return result;
    }

    // ─── Dayuse 세금/봉사료 계산 헬퍼 (PriceCalculationService 위임) ───

    private AvailableRoomTypeResponse.DailyPrice buildDayUseDailyPrice(
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

    private DailyCharge buildDayUseDailyCharge(BigDecimal supplyPrice, Property property, LocalDate date) {
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
