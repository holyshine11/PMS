package com.hola.rate.service;

import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.rate.dto.request.RateCodeCreateRequest;
import com.hola.rate.dto.request.RateCodeUpdateRequest;
import com.hola.rate.dto.request.RatePricingRequest;
import com.hola.rate.dto.response.RateCodeListResponse;
import com.hola.rate.dto.response.RateCodeResponse;
import com.hola.rate.dto.response.RatePricingResponse;
import com.hola.rate.entity.RateCode;
import com.hola.rate.entity.RateCodeRoomType;
import com.hola.rate.entity.RatePricing;
import com.hola.rate.entity.RatePricingPerson;
import com.hola.rate.mapper.RateCodeMapper;
import com.hola.rate.entity.RateCodePaidService;
import com.hola.rate.repository.RateCodePaidServiceRepository;
import com.hola.rate.repository.RateCodeRepository;
import com.hola.rate.repository.RateCodeRoomTypeRepository;
import com.hola.rate.repository.RatePricingPersonRepository;
import com.hola.rate.repository.RatePricingRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 레이트 코드 서비스 구현
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RateCodeServiceImpl implements RateCodeService {

    private final RateCodeRepository rateCodeRepository;
    private final RateCodeRoomTypeRepository rateCodeRoomTypeRepository;
    private final RateCodePaidServiceRepository rateCodePaidServiceRepository;
    private final RatePricingRepository ratePricingRepository;
    private final RatePricingPersonRepository ratePricingPersonRepository;
    private final RateCodeMapper rateCodeMapper;
    private final EntityManager entityManager;

    @Override
    public List<RateCodeListResponse> getRateCodes(Long propertyId) {
        List<RateCode> rateCodes = rateCodeRepository.findAllByPropertyIdOrderBySortOrderAscRateCodeAsc(propertyId);
        return rateCodes.stream()
                .map(rc -> {
                    String marketCodeName = getMarketCodeName(rc.getMarketCodeId());
                    long roomTypeCount = rateCodeRoomTypeRepository.countByRateCodeId(rc.getId());
                    return rateCodeMapper.toListResponse(rc, marketCodeName, roomTypeCount);
                })
                .toList();
    }

    @Override
    public List<RateCodeListResponse> getAvailableRateCodes(Long propertyId, LocalDate checkIn, LocalDate checkOut) {
        List<RateCode> rateCodes = rateCodeRepository.findAllByPropertyIdOrderBySortOrderAscRateCodeAsc(propertyId);
        return rateCodes.stream()
                .filter(rc -> Boolean.TRUE.equals(rc.getUseYn()))
                .filter(rc -> rc.getSaleStartDate() != null && rc.getSaleEndDate() != null)
                .filter(rc -> !checkIn.isBefore(rc.getSaleStartDate()) && !checkIn.isAfter(rc.getSaleEndDate()))
                // 숙박일수(min/max) 범위 필터링
                .filter(rc -> {
                    long stayDays = java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
                    if (rc.getMinStayDays() != null && stayDays < rc.getMinStayDays()) return false;
                    if (rc.getMaxStayDays() != null && rc.getMaxStayDays() > 0 && stayDays > rc.getMaxStayDays()) return false;
                    return true;
                })
                .filter(rc -> {
                    // Dayuse 레이트코드는 DayUseRate 사용 → RatePricing 커버리지 스킵
                    if (rc.isDayUse()) return true;
                    // 요금행이 체크인~체크아웃 전일까지 모든 날짜를 커버하는지 확인
                    List<RatePricing> pricings = ratePricingRepository.findAllByRateCodeIdOrderByIdAsc(rc.getId());
                    if (pricings.isEmpty()) return false;
                    LocalDate date = checkIn;
                    while (date.isBefore(checkOut)) {
                        if (!hasPricingForDate(pricings, date)) return false;
                        date = date.plusDays(1);
                    }
                    return true;
                })
                .map(rc -> {
                    String marketCodeName = getMarketCodeName(rc.getMarketCodeId());
                    long roomTypeCount = rateCodeRoomTypeRepository.countByRateCodeId(rc.getId());
                    return rateCodeMapper.toListResponse(rc, marketCodeName, roomTypeCount);
                })
                .toList();
    }

    /**
     * 특정 날짜에 매칭되는 요금행이 있는지 확인 (기간+요일)
     */
    private boolean hasPricingForDate(List<RatePricing> pricings, LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        for (RatePricing p : pricings) {
            if (date.isBefore(p.getStartDate()) || date.isAfter(p.getEndDate())) continue;
            boolean matches = switch (dayOfWeek) {
                case MONDAY -> Boolean.TRUE.equals(p.getDayMon());
                case TUESDAY -> Boolean.TRUE.equals(p.getDayTue());
                case WEDNESDAY -> Boolean.TRUE.equals(p.getDayWed());
                case THURSDAY -> Boolean.TRUE.equals(p.getDayThu());
                case FRIDAY -> Boolean.TRUE.equals(p.getDayFri());
                case SATURDAY -> Boolean.TRUE.equals(p.getDaySat());
                case SUNDAY -> Boolean.TRUE.equals(p.getDaySun());
            };
            if (matches) return true;
        }
        return false;
    }

    @Override
    public RateCodeResponse getRateCode(Long id) {
        RateCode rateCode = findById(id);
        String marketCodeName = getMarketCodeName(rateCode.getMarketCodeId());
        List<RateCodeRoomType> roomTypeMappings = rateCodeRoomTypeRepository.findAllByRateCodeId(id);
        return rateCodeMapper.toResponse(rateCode, marketCodeName, roomTypeMappings);
    }

    @Override
    @Transactional
    public RateCodeResponse createRateCode(Long propertyId, RateCodeCreateRequest request) {
        // 코드 중복 확인
        if (rateCodeRepository.existsByPropertyIdAndRateCode(propertyId, request.getRateCode())) {
            throw new HolaException(ErrorCode.RATE_CODE_DUPLICATE);
        }

        // 판매기간 검증
        validateSalePeriod(request.getSaleStartDate(), request.getSaleEndDate());
        // 숙박일수 검증
        validateStayDays(request.getMinStayDays(), request.getMaxStayDays());
        // 숙박유형 검증
        String stayType = validateAndNormalizeStayType(request.getStayType());

        RateCode rateCode = RateCode.builder()
                .propertyId(propertyId)
                .rateCode(request.getRateCode())
                .rateNameKo(request.getRateNameKo())
                .rateNameEn(request.getRateNameEn())
                .rateCategory(request.getRateCategory())
                .marketCodeId(request.getMarketCodeId())
                .currency(request.getCurrency())
                .saleStartDate(request.getSaleStartDate())
                .saleEndDate(request.getSaleEndDate())
                .minStayDays(request.getMinStayDays())
                .maxStayDays(request.getMaxStayDays())
                .stayType(stayType)
                .build();

        // 사용여부 설정
        if (request.getUseYn() != null && !request.getUseYn()) {
            rateCode.deactivate();
        }
        // 정렬순서 설정
        if (request.getSortOrder() != null) {
            rateCode.changeSortOrder(request.getSortOrder());
        }

        RateCode saved = rateCodeRepository.save(rateCode);

        // 객실 타입 매핑 저장
        saveRoomTypeMappings(saved.getId(), request.getRoomTypeIds());

        log.info("레이트 코드 생성: {} ({}) - 프로퍼티: {}", saved.getRateNameKo(), saved.getRateCode(), propertyId);

        String marketCodeName = getMarketCodeName(saved.getMarketCodeId());
        List<RateCodeRoomType> mappings = rateCodeRoomTypeRepository.findAllByRateCodeId(saved.getId());
        return rateCodeMapper.toResponse(saved, marketCodeName, mappings);
    }

    @Override
    @Transactional
    public RateCodeResponse updateRateCode(Long id, RateCodeUpdateRequest request) {
        RateCode rateCode = findById(id);

        // 판매기간 검증
        validateSalePeriod(request.getSaleStartDate(), request.getSaleEndDate());
        // 숙박일수 검증
        validateStayDays(request.getMinStayDays(), request.getMaxStayDays());
        // 숙박유형 검증
        String stayType = validateAndNormalizeStayType(request.getStayType());

        rateCode.update(
                request.getRateNameKo(),
                request.getRateNameEn(),
                request.getRateCategory(),
                request.getMarketCodeId(),
                request.getCurrency(),
                request.getSaleStartDate(),
                request.getSaleEndDate(),
                request.getMinStayDays(),
                request.getMaxStayDays(),
                stayType
        );

        // 사용여부 토글
        if (request.getUseYn() != null) {
            if (request.getUseYn()) {
                rateCode.activate();
            } else {
                rateCode.deactivate();
            }
        }
        // 정렬순서
        if (request.getSortOrder() != null) {
            rateCode.changeSortOrder(request.getSortOrder());
        }

        // 객실 타입 매핑: 전체 삭제 후 재등록
        rateCodeRoomTypeRepository.deleteAllByRateCodeId(id);
        saveRoomTypeMappings(id, request.getRoomTypeIds());

        log.info("레이트 코드 수정: {} ({})", rateCode.getRateNameKo(), rateCode.getRateCode());

        String marketCodeName = getMarketCodeName(rateCode.getMarketCodeId());
        List<RateCodeRoomType> mappings = rateCodeRoomTypeRepository.findAllByRateCodeId(id);
        return rateCodeMapper.toResponse(rateCode, marketCodeName, mappings);
    }

    @Override
    @Transactional
    public void deleteRateCode(Long id) {
        RateCode rateCode = findById(id);
        // 매핑 데이터 삭제
        rateCodeRoomTypeRepository.deleteAllByRateCodeId(id);
        rateCodePaidServiceRepository.deleteAllByRateCodeId(id);
        rateCode.softDelete();
        log.info("레이트 코드 삭제: {} ({})", rateCode.getRateNameKo(), rateCode.getRateCode());
    }

    @Override
    public boolean existsRateCode(Long propertyId, String rateCode) {
        return rateCodeRepository.existsByPropertyIdAndRateCode(propertyId, rateCode);
    }

    private RateCode findById(Long id) {
        return rateCodeRepository.findById(id)
                .orElseThrow(() -> new HolaException(ErrorCode.RATE_CODE_NOT_FOUND));
    }

    private void validateSalePeriod(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new HolaException(ErrorCode.RATE_INVALID_SALE_PERIOD);
        }
    }

    private void validateStayDays(Integer minDays, Integer maxDays) {
        if (minDays != null && maxDays != null && maxDays < minDays) {
            throw new HolaException(ErrorCode.RATE_INVALID_STAY_DAYS);
        }
    }

    private static final java.util.Set<String> VALID_STAY_TYPES = java.util.Set.of("OVERNIGHT", "DAY_USE");

    /**
     * 숙박유형 검증 및 정규화 (null → OVERNIGHT, 유효하지 않은 값 → 에러)
     */
    private String validateAndNormalizeStayType(String stayType) {
        if (stayType == null || stayType.isBlank()) return "OVERNIGHT";
        if (!VALID_STAY_TYPES.contains(stayType)) {
            throw new HolaException(ErrorCode.RATE_INVALID_STAY_TYPE);
        }
        return stayType;
    }

    private void saveRoomTypeMappings(Long rateCodeId, List<Long> roomTypeIds) {
        if (roomTypeIds == null || roomTypeIds.isEmpty()) return;

        List<RateCodeRoomType> mappings = roomTypeIds.stream()
                .map(roomTypeId -> RateCodeRoomType.builder()
                        .rateCodeId(rateCodeId)
                        .roomTypeId(roomTypeId)
                        .build())
                .toList();
        rateCodeRoomTypeRepository.saveAll(mappings);
    }

    // ===== 요금정보 CRUD =====

    @Override
    public RatePricingResponse getRatePricing(Long rateCodeId) {
        findById(rateCodeId); // 존재 확인
        List<RatePricing> pricings = ratePricingRepository.findAllByRateCodeIdOrderByIdAsc(rateCodeId);
        return buildPricingResponse(pricings);
    }

    @Override
    @Transactional
    public RatePricingResponse saveRatePricing(Long rateCodeId, RatePricingRequest request) {
        RateCode rateCode = findById(rateCodeId);

        // 요금 설정 기간 검증
        if (request.getPricingRows() != null) {
            validatePricingPeriods(request.getPricingRows(), rateCode.getSaleStartDate(), rateCode.getSaleEndDate());
        }

        // 기존 요금 행 전체 삭제 후 재생성
        List<RatePricing> existingPricings = ratePricingRepository.findAllByRateCodeIdOrderByIdAsc(rateCodeId);
        for (RatePricing p : existingPricings) {
            ratePricingPersonRepository.deleteAllByRatePricingId(p.getId());
        }
        ratePricingRepository.deleteAllByRateCodeId(rateCodeId);
        entityManager.flush();

        // 새 요금 행 저장
        List<RatePricing> savedPricings = new ArrayList<>();
        if (request.getPricingRows() != null) {
            for (RatePricingRequest.PricingRow row : request.getPricingRows()) {
                RatePricing pricing = RatePricing.builder()
                        .rateCodeId(rateCodeId)
                        .startDate(row.getStartDate())
                        .endDate(row.getEndDate())
                        .dayMon(row.getDayMon() != null ? row.getDayMon() : true)
                        .dayTue(row.getDayTue() != null ? row.getDayTue() : true)
                        .dayWed(row.getDayWed() != null ? row.getDayWed() : true)
                        .dayThu(row.getDayThu() != null ? row.getDayThu() : true)
                        .dayFri(row.getDayFri() != null ? row.getDayFri() : true)
                        .daySat(row.getDaySat() != null ? row.getDaySat() : true)
                        .daySun(row.getDaySun() != null ? row.getDaySun() : true)
                        .currency(row.getCurrency() != null ? row.getCurrency() : "KRW")
                        .baseSupplyPrice(row.getBaseSupplyPrice() != null ? row.getBaseSupplyPrice() : BigDecimal.ZERO)
                        .baseTax(row.getBaseTax() != null ? row.getBaseTax() : BigDecimal.ZERO)
                        .baseTotal(row.getBaseTotal() != null ? row.getBaseTotal() : BigDecimal.ZERO)
                        .downUpSign(row.getDownUpSign())
                        .downUpValue(row.getDownUpValue())
                        .downUpUnit(row.getDownUpUnit())
                        .roundingDecimalPoint(row.getRoundingDecimalPoint() != null ? row.getRoundingDecimalPoint() : 0)
                        .roundingDigits(row.getRoundingDigits() != null ? row.getRoundingDigits() : 0)
                        .roundingMethod(row.getRoundingMethod())
                        .build();
                RatePricing saved = ratePricingRepository.save(pricing);

                // 인원별 추가 요금 저장
                if (row.getPersons() != null) {
                    for (RatePricingRequest.PersonPrice pp : row.getPersons()) {
                        if (pp.getSupplyPrice() != null && pp.getSupplyPrice().compareTo(BigDecimal.ZERO) > 0) {
                            RatePricingPerson person = RatePricingPerson.builder()
                                    .ratePricingId(saved.getId())
                                    .personType(pp.getPersonType())
                                    .personSeq(pp.getPersonSeq())
                                    .supplyPrice(pp.getSupplyPrice() != null ? pp.getSupplyPrice() : BigDecimal.ZERO)
                                    .tax(pp.getTax() != null ? pp.getTax() : BigDecimal.ZERO)
                                    .totalPrice(pp.getTotalPrice() != null ? pp.getTotalPrice() : BigDecimal.ZERO)
                                    .build();
                            ratePricingPersonRepository.save(person);
                        }
                    }
                }
                savedPricings.add(saved);
            }
        }

        log.info("요금정보 저장: 레이트코드 ID={}, 요금행 {}건", rateCodeId, savedPricings.size());

        // 저장 후 재조회하여 응답
        List<RatePricing> result = ratePricingRepository.findAllByRateCodeIdOrderByIdAsc(rateCodeId);
        return buildPricingResponse(result);
    }

    @Override
    @Transactional
    public void deleteRatePricingRow(Long rateCodeId, Long pricingId) {
        findById(rateCodeId); // 존재 확인
        ratePricingPersonRepository.deleteAllByRatePricingId(pricingId);
        ratePricingRepository.deleteById(pricingId);
        log.info("요금 행 삭제: 레이트코드 ID={}, 요금행 ID={}", rateCodeId, pricingId);
    }

    private RatePricingResponse buildPricingResponse(List<RatePricing> pricings) {
        List<RatePricingResponse.PricingRowResponse> rows = pricings.stream()
                .map(p -> {
                    List<RatePricingResponse.PersonPriceResponse> persons = p.getPersons().stream()
                            .map(pp -> RatePricingResponse.PersonPriceResponse.builder()
                                    .personType(pp.getPersonType())
                                    .personSeq(pp.getPersonSeq())
                                    .supplyPrice(pp.getSupplyPrice())
                                    .tax(pp.getTax())
                                    .totalPrice(pp.getTotalPrice())
                                    .build())
                            .toList();
                    return RatePricingResponse.PricingRowResponse.builder()
                            .id(p.getId())
                            .startDate(p.getStartDate())
                            .endDate(p.getEndDate())
                            .dayMon(p.getDayMon())
                            .dayTue(p.getDayTue())
                            .dayWed(p.getDayWed())
                            .dayThu(p.getDayThu())
                            .dayFri(p.getDayFri())
                            .daySat(p.getDaySat())
                            .daySun(p.getDaySun())
                            .currency(p.getCurrency())
                            .baseSupplyPrice(p.getBaseSupplyPrice())
                            .baseTax(p.getBaseTax())
                            .baseTotal(p.getBaseTotal())
                            .downUpSign(p.getDownUpSign())
                            .downUpValue(p.getDownUpValue())
                            .downUpUnit(p.getDownUpUnit())
                            .roundingDecimalPoint(p.getRoundingDecimalPoint())
                            .roundingDigits(p.getRoundingDigits())
                            .roundingMethod(p.getRoundingMethod())
                            .persons(persons)
                            .build();
                })
                .toList();

        return RatePricingResponse.builder()
                .pricingRows(rows)
                .build();
    }

    // ===== 옵션요금 CRUD =====

    @Override
    public List<Long> getOptionPricing(Long rateCodeId) {
        findById(rateCodeId);
        return rateCodePaidServiceRepository.findAllByRateCodeId(rateCodeId).stream()
                .map(RateCodePaidService::getPaidServiceOptionId)
                .toList();
    }

    @Override
    @Transactional
    public List<Long> saveOptionPricing(Long rateCodeId, List<Long> paidServiceOptionIds) {
        findById(rateCodeId);

        // 전체 삭제 후 재등록
        rateCodePaidServiceRepository.deleteAllByRateCodeId(rateCodeId);
        entityManager.flush();

        if (paidServiceOptionIds != null && !paidServiceOptionIds.isEmpty()) {
            List<RateCodePaidService> mappings = paidServiceOptionIds.stream()
                    .map(optionId -> RateCodePaidService.builder()
                            .rateCodeId(rateCodeId)
                            .paidServiceOptionId(optionId)
                            .build())
                    .toList();
            rateCodePaidServiceRepository.saveAll(mappings);
        }

        log.info("옵션요금 저장: 레이트코드 ID={}, 옵션 {}건", rateCodeId,
                paidServiceOptionIds != null ? paidServiceOptionIds.size() : 0);

        return rateCodePaidServiceRepository.findAllByRateCodeId(rateCodeId).stream()
                .map(RateCodePaidService::getPaidServiceOptionId)
                .toList();
    }

    /**
     * 요금 설정 기간 검증 (필수/역전/판매기간 종속/기간 중첩)
     */
    private void validatePricingPeriods(List<RatePricingRequest.PricingRow> rows,
                                         LocalDate saleStartDate, LocalDate saleEndDate) {
        for (RatePricingRequest.PricingRow row : rows) {
            // 필수 검증
            if (row.getStartDate() == null || row.getEndDate() == null) {
                throw new HolaException(ErrorCode.RATE_PRICING_PERIOD_REQUIRED);
            }
            // 역전 검증
            if (row.getEndDate().isBefore(row.getStartDate())) {
                throw new HolaException(ErrorCode.RATE_INVALID_PRICING_PERIOD);
            }
            // 판매기간 종속 검증
            if (saleStartDate != null && saleEndDate != null) {
                if (row.getStartDate().isBefore(saleStartDate) || row.getEndDate().isAfter(saleEndDate)) {
                    throw new HolaException(ErrorCode.RATE_PRICING_PERIOD_OUT_OF_SALE);
                }
            }
        }

        // 기간 중첩 검증 (모든 행 쌍 비교)
        for (int i = 0; i < rows.size(); i++) {
            for (int j = i + 1; j < rows.size(); j++) {
                RatePricingRequest.PricingRow a = rows.get(i);
                RatePricingRequest.PricingRow b = rows.get(j);
                // 기간이 겹치는지 확인: !(a.end < b.start || b.end < a.start)
                boolean overlaps = !a.getEndDate().isBefore(b.getStartDate())
                        && !b.getEndDate().isBefore(a.getStartDate());
                if (overlaps) {
                    // 기간이 겹치는 경우, 요일도 겹치는지 확인
                    if (hasDayOverlap(a, b)) {
                        throw new HolaException(ErrorCode.RATE_PRICING_PERIOD_OVERLAP);
                    }
                }
            }
        }
    }

    /**
     * 두 요금행의 요일 중복 여부 확인
     */
    private boolean hasDayOverlap(RatePricingRequest.PricingRow a, RatePricingRequest.PricingRow b) {
        if (Boolean.TRUE.equals(a.getDayMon()) && Boolean.TRUE.equals(b.getDayMon())) return true;
        if (Boolean.TRUE.equals(a.getDayTue()) && Boolean.TRUE.equals(b.getDayTue())) return true;
        if (Boolean.TRUE.equals(a.getDayWed()) && Boolean.TRUE.equals(b.getDayWed())) return true;
        if (Boolean.TRUE.equals(a.getDayThu()) && Boolean.TRUE.equals(b.getDayThu())) return true;
        if (Boolean.TRUE.equals(a.getDayFri()) && Boolean.TRUE.equals(b.getDayFri())) return true;
        if (Boolean.TRUE.equals(a.getDaySat()) && Boolean.TRUE.equals(b.getDaySat())) return true;
        if (Boolean.TRUE.equals(a.getDaySun()) && Boolean.TRUE.equals(b.getDaySun())) return true;
        return false;
    }

    /**
     * 마켓코드명 조회 (타 모듈 테이블 직접 접근 - Native Query)
     */
    private String getMarketCodeName(Long marketCodeId) {
        if (marketCodeId == null) return null;
        try {
            Object result = entityManager
                    .createNativeQuery("SELECT market_name FROM htl_market_code WHERE id = :id AND deleted_at IS NULL")
                    .setParameter("id", marketCodeId)
                    .getSingleResult();
            return result != null ? result.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
