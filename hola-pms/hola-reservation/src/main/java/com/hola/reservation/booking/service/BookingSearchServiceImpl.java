package com.hola.reservation.booking.service;

import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.hotel.entity.Hotel;
import com.hola.hotel.entity.Property;
import com.hola.hotel.entity.PropertyImage;
import com.hola.hotel.repository.PropertyImageRepository;
import com.hola.hotel.repository.PropertyTermsRepository;
import com.hola.rate.dto.response.RateCodeListResponse;
import com.hola.rate.entity.RateCode;
import com.hola.rate.entity.RateCodePaidService;
import com.hola.rate.entity.RateCodeRoomType;
import com.hola.rate.entity.DayUseRate;
import com.hola.rate.entity.PromotionCode;
import com.hola.rate.repository.DayUseRateRepository;
import com.hola.rate.repository.RateCodePaidServiceRepository;
import com.hola.rate.repository.PromotionCodeRepository;
import com.hola.rate.repository.RateCodeRepository;
import com.hola.rate.repository.RateCodeRoomTypeRepository;
import com.hola.rate.service.RateCodeService;
import com.hola.reservation.booking.dto.request.BookingSearchRequest;
import com.hola.reservation.booking.dto.request.PriceCheckRequest;
import com.hola.reservation.booking.dto.response.*;
import com.hola.reservation.entity.DailyCharge;
import com.hola.reservation.entity.MasterReservation;
import com.hola.reservation.repository.MasterReservationRepository;
import com.hola.reservation.service.PriceCalculationService;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 부킹엔진 검색/조회 서비스 구현
 * - 프로퍼티 정보, 객실 가용성 검색, 요금 확인, 캘린더 등
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingSearchServiceImpl implements BookingSearchService {

    private final BookingHelper helper;
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
    private final RateCodePaidServiceRepository rateCodePaidServiceRepository;
    private final PromotionCodeRepository promotionCodeRepository;
    private final RoomAvailabilityService roomAvailabilityService;
    private final PriceCalculationService priceCalculationService;
    private final MasterReservationRepository masterReservationRepository;
    private final DayUseRateRepository dayUseRateRepository;
    private final com.hola.hotel.repository.CancellationFeeRepository cancellationFeeRepository;

    @Override
    public PropertyInfoResponse getPropertyInfo(String propertyCode) {
        Property property = helper.findPropertyByCode(propertyCode);
        Hotel hotel = property.getHotel();

        // starRating 문자열 -> 숫자 변환 (파싱 실패 시 null)
        Integer starRating = helper.parseStarRating(property.getStarRating());

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
                .sameDayBookingEnabled(property.getSameDayBookingEnabled())
                .sameDayCutoffTime(property.getSameDayCutoffTime())
                .build();
    }

    @Override
    public List<AvailableRoomTypeResponse> searchAvailability(String propertyCode, BookingSearchRequest request) {
        // 날짜 유효성 검증
        helper.validateDateRange(request.getCheckIn(), request.getCheckOut());

        Property property = helper.findPropertyByCode(propertyCode);

        // 당일 예약 마감시간 검증
        helper.validateSameDayCutoff(property, request.getCheckIn());

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
            String roomClassName = helper.getRoomClassName(roomType.getRoomClassId());

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
        helper.validateDateRange(request.getCheckIn(), request.getCheckOut());

        Property property = helper.findPropertyByCode(propertyCode);

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
            AvailableRoomTypeResponse.DailyPrice dp = helper.buildDayUseDailyPrice(
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
        RateCodeListResponse rateInfo = helper.findRateCodeInfo(request.getRateCodeId());

        // 객실클래스명 조회 (roomTypeName 대용)
        String roomClassName = helper.getRoomClassName(roomType.getRoomClassId());

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
    public BookingConfirmationResponse getConfirmation(String confirmationNo, String verificationValue) {
        // 1. 확인번호로 예약 조회
        MasterReservation master = masterReservationRepository.findByConfirmationNo(confirmationNo)
                .orElseThrow(() -> new HolaException(ErrorCode.BOOKING_CONFIRMATION_NOT_FOUND));

        // 2. 2차 검증 (이메일 또는 전화번호) -- 값이 없으면 검증 생략 (데모)
        if (verificationValue != null && !verificationValue.isBlank()) {
            boolean verified = verificationValue.equalsIgnoreCase(master.getEmail())
                    || verificationValue.equals(master.getPhoneNumber());
            if (!verified) {
                throw new HolaException(ErrorCode.BOOKING_GUEST_VERIFICATION_FAILED);
            }
        }

        return helper.buildConfirmationResponse(master);
    }

    @Override
    public List<PropertyImageResponse> getPropertyImages(String propertyCode) {
        Property property = helper.findPropertyByCode(propertyCode);
        return propertyImageRepository
                .findAllByPropertyIdOrderByImageTypeAscSortOrderAsc(property.getId())
                .stream()
                .filter(img -> Boolean.TRUE.equals(img.getUseYn()))
                .map(this::toImageResponse)
                .toList();
    }

    @Override
    public List<PropertyImageResponse> getRoomTypeImages(String propertyCode, Long roomTypeId) {
        Property property = helper.findPropertyByCode(propertyCode);
        return propertyImageRepository
                .findAllByPropertyIdAndImageTypeAndReferenceIdOrderBySortOrderAsc(
                        property.getId(), "ROOM_TYPE", roomTypeId)
                .stream()
                .filter(img -> Boolean.TRUE.equals(img.getUseYn()))
                .map(this::toImageResponse)
                .toList();
    }

    @Override
    public List<PropertyTermsResponse> getTerms(String propertyCode) {
        Property property = helper.findPropertyByCode(propertyCode);
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
        Property property = helper.findPropertyByCode(propertyCode);

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
        Property property = helper.findPropertyByCode(propertyCode);

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

    @Override
    public List<RatePlanListResponse> getRatePlans(String propertyCode, LocalDate checkIn, LocalDate checkOut,
                                                    Integer adults, Integer children, String promotionCode) {
        Property property = helper.findPropertyByCode(propertyCode);
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

                String roomClassName = helper.getRoomClassName(roomType.getRoomClassId());

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

    @Override
    public RoomDetailResponse getRoomDetail(String propertyCode, Long roomTypeId) {
        Property property = helper.findPropertyByCode(propertyCode);

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

    @Override
    public RatePlanDetailResponse getRatePlanDetail(String propertyCode, Long ratePlanId) {
        Property property = helper.findPropertyByCode(propertyCode);

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

    @Override
    public CalendarResponse getCalendar(String propertyCode, LocalDate startDate, LocalDate endDate, String type) {
        Property property = helper.findPropertyByCode(propertyCode);
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

    @Override
    public List<RatePlanListResponse> getRatePlansByRoomType(String propertyCode, Long roomTypeId,
                                                              LocalDate checkIn, LocalDate checkOut,
                                                              Integer adults, Integer children) {
        Property property = helper.findPropertyByCode(propertyCode);

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

        String roomClassName = helper.getRoomClassName(roomType.getRoomClassId());
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

    // ===== Private Helper Methods =====

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
                    AvailableRoomTypeResponse.DailyPrice dp = helper.buildDayUseDailyPrice(
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
}
