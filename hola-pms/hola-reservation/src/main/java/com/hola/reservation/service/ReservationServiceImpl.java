package com.hola.reservation.service;

import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.common.security.AccessControlService;
import com.hola.hotel.entity.Property;
import com.hola.hotel.entity.RoomUnavailable;
import com.hola.hotel.repository.MarketCodeRepository;
import com.hola.hotel.repository.PropertyRepository;
import com.hola.hotel.repository.RoomUnavailableRepository;
import com.hola.room.entity.RoomType;
import com.hola.room.repository.RoomTypeRepository;
import com.hola.rate.entity.RateCode;
import com.hola.rate.repository.RateCodeRepository;
import com.hola.reservation.dto.request.ReservationCreateRequest;
import com.hola.reservation.dto.request.ReservationUpdateRequest;
import com.hola.reservation.dto.request.SubReservationRequest;
import com.hola.reservation.dto.response.PaymentSummaryResponse;
import com.hola.reservation.dto.response.RateChangePreviewResponse;
import com.hola.reservation.dto.response.ReservationDepositResponse;
import com.hola.reservation.dto.response.ReservationDetailResponse;
import com.hola.reservation.dto.response.ReservationListResponse;
import com.hola.reservation.dto.response.ReservationMemoResponse;
import com.hola.reservation.dto.response.ReservationServiceResponse;
import com.hola.reservation.dto.response.SubReservationResponse;
import com.hola.reservation.entity.DailyCharge;
import com.hola.reservation.entity.MasterReservation;
import com.hola.reservation.entity.ReservationDeposit;
import com.hola.reservation.entity.ReservationMemo;
import com.hola.reservation.entity.SubReservation;
import com.hola.reservation.mapper.ReservationMapper;
import com.hola.reservation.repository.MasterReservationRepository;
import com.hola.reservation.repository.MasterReservationSpecification;
import com.hola.reservation.repository.ReservationDepositRepository;
import com.hola.reservation.repository.ReservationMemoRepository;
import com.hola.reservation.repository.SubReservationRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 예약 CRUD 서비스 구현
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationServiceImpl implements ReservationService {

    private final ReservationFinder finder;
    private final SubReservationCreator subCreator;
    private final MasterReservationRepository masterReservationRepository;
    private final SubReservationRepository subReservationRepository;
    private final ReservationDepositRepository reservationDepositRepository;
    private final ReservationMemoRepository reservationMemoRepository;
    private final PropertyRepository propertyRepository;
    private final RateCodeRepository rateCodeRepository;
    private final MarketCodeRepository marketCodeRepository;
    private final RoomUnavailableRepository roomUnavailableRepository;
    private final RoomInfoResolver roomInfoResolver;
    private final ReservationMapper reservationMapper;
    private final ReservationNumberGenerator numberGenerator;
    private final RoomAvailabilityService availabilityService;
    private final ReservationPaymentService paymentService;
    private final PriceCalculationService priceCalculationService;
    private final AccessControlService accessControlService;
    private final RateIncludedServiceHelper rateIncludedServiceHelper;
    private final RoomTypeRepository roomTypeRepository;
    private final EntityManager entityManager;
    private final ReservationChangeLogService changeLogService;
    private final EarlyLateCheckService earlyLateCheckService;

    @Override
    public List<ReservationListResponse> getList(Long propertyId, String status, LocalDate checkInFrom,
                                                   LocalDate checkInTo, String keyword) {
        // Specification 기반 DB 쿼리 (Hibernate 6 + PostgreSQL null 파라미터 타입 추론 이슈 회피)
        Specification<MasterReservation> spec = MasterReservationSpecification.search(
                propertyId, status, checkInFrom, checkInTo, keyword);
        Sort sort = Sort.by(Sort.Direction.DESC, "reservationDate");

        List<MasterReservation> reservations = masterReservationRepository.findAll(spec, sort);
        return reservations.stream()
                .map(reservationMapper::toReservationListResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Page<ReservationListResponse> getList(Long propertyId, String status, LocalDate checkInFrom,
                                                  LocalDate checkInTo, String keyword, Pageable pageable) {
        // Specification + Pageable로 DB 레벨 페이징
        Specification<MasterReservation> spec = MasterReservationSpecification.search(
                propertyId, status, checkInFrom, checkInTo, keyword);

        Page<MasterReservation> page = masterReservationRepository.findAll(spec, pageable);
        List<ReservationListResponse> content = page.getContent().stream()
                .map(reservationMapper::toReservationListResponse)
                .collect(Collectors.toList());
        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    @Override
    public ReservationDetailResponse getById(Long id, Long propertyId) {
        MasterReservation master = finder.findMasterById(id, propertyId);
        Property property = master.getProperty();
        ReservationDetailResponse response = reservationMapper.toReservationDetailResponse(master);

        // 서브 예약의 층/호수/객실타입 이름 벌크 resolve
        List<SubReservationResponse> resolvedSubs = resolveSubReservationNames(response.getSubReservations());

        // 보증금 목록
        List<ReservationDeposit> deposits = reservationDepositRepository
                .findByMasterReservationId(id);
        List<ReservationDepositResponse> depositResponses = deposits.stream()
                .map(reservationMapper::toReservationDepositResponse)
                .collect(Collectors.toList());

        // 메모 목록
        List<ReservationMemo> memos = reservationMemoRepository
                .findByMasterReservationIdOrderByCreatedAtDesc(id);
        List<ReservationMemoResponse> memoResponses = memos.stream()
                .map(reservationMapper::toReservationMemoResponse)
                .collect(Collectors.toList());

        // 결제 정보
        PaymentSummaryResponse paymentSummary = paymentService.getPaymentSummary(propertyId, id);

        return ReservationDetailResponse.builder()
                .id(response.getId())
                .propertyId(response.getPropertyId())
                .masterReservationNo(response.getMasterReservationNo())
                .confirmationNo(response.getConfirmationNo())
                .reservationStatus(response.getReservationStatus())
                .masterCheckIn(response.getMasterCheckIn())
                .masterCheckOut(response.getMasterCheckOut())
                .reservationDate(response.getReservationDate())
                .guestNameKo(response.getGuestNameKo())
                .guestFirstNameEn(response.getGuestFirstNameEn())
                .guestMiddleNameEn(response.getGuestMiddleNameEn())
                .guestLastNameEn(response.getGuestLastNameEn())
                .phoneCountryCode(response.getPhoneCountryCode())
                .phoneNumber(response.getPhoneNumber())
                .email(response.getEmail())
                .birthDate(response.getBirthDate())
                .gender(response.getGender())
                .nationality(response.getNationality())
                .rateCodeId(response.getRateCodeId())
                .stayType(subCreator.isDayUseRateCode(response.getRateCodeId()) ? "DAY_USE" : "OVERNIGHT")
                .rateCodeName(response.getRateCodeId() != null ?
                    rateCodeRepository.findById(response.getRateCodeId())
                        .map(rc -> rc.getRateNameKo()).orElse(null) : null)
                .marketCodeId(response.getMarketCodeId())
                .marketCodeName(response.getMarketCodeId() != null ?
                    marketCodeRepository.findById(response.getMarketCodeId())
                        .map(mc -> mc.getMarketName()).orElse(null) : null)
                .reservationChannelId(response.getReservationChannelId())
                .promotionType(response.getPromotionType())
                .promotionCode(response.getPromotionCode())
                .otaReservationNo(response.getOtaReservationNo())
                .isOtaManaged(response.getIsOtaManaged())
                .customerRequest(response.getCustomerRequest())
                .subReservations(resolvedSubs)
                .deposits(depositResponses)
                .payment(paymentSummary)
                .memos(memoResponses)
                .propertyCheckInTime(property.getCheckInTime())
                .propertyCheckOutTime(property.getCheckOutTime())
                .createdAt(response.getCreatedAt())
                .updatedAt(response.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public ReservationDetailResponse create(Long propertyId, ReservationCreateRequest request) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new HolaException(ErrorCode.PROPERTY_NOT_FOUND));

        // 체크아웃이 체크인보다 앞서는 경우
        if (request.getMasterCheckIn() != null && request.getMasterCheckOut() != null
                && request.getMasterCheckOut().isBefore(request.getMasterCheckIn())) {
            throw new HolaException(ErrorCode.SUB_RESERVATION_DATE_INVALID);
        }

        // 레이트코드 필수 검증
        if (request.getRateCodeId() == null) {
            throw new HolaException(ErrorCode.RESERVATION_RATE_REQUIRED);
        }

        // Dayuse 여부 판별
        boolean isDayUse = subCreator.isDayUseRateCode(request.getRateCodeId());

        // 체크인/체크아웃 유효성
        subCreator.validateDates(request.getMasterCheckIn(), request.getMasterCheckOut(), isDayUse);

        // 신규 예약은 과거 날짜 체크인 불가
        if (request.getMasterCheckIn().isBefore(LocalDate.now())) {
            throw new HolaException(ErrorCode.RESERVATION_CHECKIN_PAST_DATE);
        }

        // 당일 예약 마감시간 검증
        if (request.getMasterCheckIn().isEqual(LocalDate.now())) {
            if (!Boolean.TRUE.equals(property.getSameDayBookingEnabled())) {
                throw new HolaException(ErrorCode.BOOKING_SAME_DAY_DISABLED);
            }
            if (!Boolean.TRUE.equals(property.getWalkInOverride())) {
                LocalTime now = LocalTime.now();
                int currentMinutes = now.getHour() * 60 + now.getMinute();
                if (currentMinutes >= property.getSameDayCutoffTime()) {
                    throw new HolaException(ErrorCode.BOOKING_SAME_DAY_CUTOFF);
                }
            }
        }

        // 레이트코드 판매기간/숙박일수 검증
        subCreator.validateRateCode(request.getRateCodeId(), request.getMasterCheckIn(), request.getMasterCheckOut());

        // 예약번호 + 확인번호 생성
        String reservationNo = numberGenerator.generateMasterReservationNo(property);
        String confirmationNo = numberGenerator.generateConfirmationNo();

        // 마스터 예약 생성
        MasterReservation master = MasterReservation.builder()
                .property(property)
                .masterReservationNo(reservationNo)
                .confirmationNo(confirmationNo)
                .reservationStatus("RESERVED")
                .masterCheckIn(request.getMasterCheckIn())
                .masterCheckOut(request.getMasterCheckOut())
                .guestNameKo(request.getGuestNameKo())
                .guestFirstNameEn(request.getGuestFirstNameEn())
                .guestMiddleNameEn(request.getGuestMiddleNameEn())
                .guestLastNameEn(request.getGuestLastNameEn())
                .phoneCountryCode(request.getPhoneCountryCode())
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .birthDate(request.getBirthDate())
                .gender(request.getGender())
                .nationality(request.getNationality())
                .rateCodeId(request.getRateCodeId())
                .marketCodeId(request.getMarketCodeId())
                .reservationChannelId(request.getReservationChannelId())
                .promotionType(request.getPromotionType())
                .promotionCode(request.getPromotionCode())
                .otaReservationNo(request.getOtaReservationNo())
                .isOtaManaged(request.getIsOtaManaged() != null ? request.getIsOtaManaged() : false)
                .customerRequest(request.getCustomerRequest())
                .build();

        master = masterReservationRepository.save(master);
        log.info("마스터 예약 생성: {}", reservationNo);

        // 서브 예약 생성
        if (request.getSubReservations() != null) {
            int legSeq = 1;
            for (SubReservationRequest subRequest : request.getSubReservations()) {
                subCreator.create(master, subRequest, legSeq++, property);
            }
        }

        // 결제 금액 계산
        paymentService.recalculatePayment(master.getId());

        // L1 캐시 클리어
        entityManager.flush();
        entityManager.clear();

        return getById(master.getId(), propertyId);
    }

    @Override
    @Transactional
    public ReservationDetailResponse update(Long id, Long propertyId, ReservationUpdateRequest request) {
        MasterReservation master = finder.findMasterById(id, propertyId);
        finder.validateModifiable(master);

        // OTA 수정 제한 검사
        if (Boolean.TRUE.equals(master.getIsOtaManaged())) {
            throw new HolaException(ErrorCode.RESERVATION_OTA_EDIT_RESTRICTED);
        }

        // Dayuse 여부 판별
        boolean isDayUse = subCreator.isDayUseRateCode(master.getRateCodeId());
        subCreator.validateDates(request.getMasterCheckIn(), request.getMasterCheckOut(), isDayUse);

        // 수정 시에도 체크인 날짜가 과거인지 검증
        if (request.getMasterCheckIn().isBefore(LocalDate.now())
                && !request.getMasterCheckIn().equals(master.getMasterCheckIn())) {
            throw new HolaException(ErrorCode.RESERVATION_CHECKIN_PAST_DATE);
        }

        // 레이트코드 또는 날짜가 실제 변경된 경우에만 재검증
        boolean rateChanged = false;
        if (request.getRateCodeId() != null) {
            rateChanged = !request.getRateCodeId().equals(master.getRateCodeId());
            boolean datesChanged = !request.getMasterCheckIn().equals(master.getMasterCheckIn())
                    || !request.getMasterCheckOut().equals(master.getMasterCheckOut());
            if (rateChanged || datesChanged) {
                subCreator.validateRateCode(request.getRateCodeId(), request.getMasterCheckIn(), request.getMasterCheckOut());
            }
        }

        Long effectiveRateCodeId = request.getRateCodeId() != null
                ? request.getRateCodeId() : master.getRateCodeId();

        // 변경이력용 이전 값 캡처 — 마스터 전체
        LocalDate prevCheckIn = master.getMasterCheckIn();
        LocalDate prevCheckOut = master.getMasterCheckOut();
        Long prevRateCodeId = master.getRateCodeId();
        Long prevMarketCodeId = master.getMarketCodeId();
        String prevGuestNameKo = master.getGuestNameKo();
        String prevGuestFirstNameEn = master.getGuestFirstNameEn();
        String prevGuestMiddleNameEn = master.getGuestMiddleNameEn();
        String prevGuestLastNameEn = master.getGuestLastNameEn();
        String prevPhoneCountryCode = master.getPhoneCountryCode();
        String prevPhoneNumber = master.getPhoneNumber();
        String prevEmail = master.getEmail();
        LocalDate prevBirthDate = master.getBirthDate();
        String prevGender = master.getGender();
        String prevNationality = master.getNationality();
        String prevPromotionType = master.getPromotionType();
        String prevPromotionCode = master.getPromotionCode();
        String prevOtaReservationNo = master.getOtaReservationNo();
        String prevCustomerRequest = master.getCustomerRequest();

        master.update(
                request.getMasterCheckIn(), request.getMasterCheckOut(),
                request.getGuestNameKo(), request.getGuestFirstNameEn(),
                request.getGuestMiddleNameEn(), request.getGuestLastNameEn(),
                request.getPhoneCountryCode(), request.getPhoneNumber(),
                request.getEmail(), request.getBirthDate(), request.getGender(),
                request.getNationality(), effectiveRateCodeId,
                request.getMarketCodeId(), request.getReservationChannelId(),
                request.getPromotionType(), request.getPromotionCode(),
                request.getOtaReservationNo(), request.getIsOtaManaged(),
                request.getCustomerRequest()
        );

        // 서브 예약(객실 레그) 업데이트
        if (request.getSubReservations() != null) {
            Property property = master.getProperty();
            for (SubReservationRequest subReq : request.getSubReservations()) {
                if (subReq.getId() != null) {
                    // 기존 레그 수정
                    SubReservation sub = finder.findSubAndValidateOwnership(subReq.getId(), master);
                    subCreator.validateDates(subReq.getCheckIn(), subReq.getCheckOut());

                    if (subReq.getRoomNumberId() != null) {
                        if (availabilityService.hasRoomConflict(subReq.getRoomNumberId(),
                                subReq.getCheckIn(), subReq.getCheckOut(), sub.getId(),
                                sub.getStayType(), sub.getDayUseTimeSlot())) {
                            throw new HolaException(ErrorCode.SUB_RESERVATION_ROOM_CONFLICT);
                        }
                    }

                    // OOO/OOS 기간 체크
                    if (subReq.getRoomNumberId() != null) {
                        List<RoomUnavailable> unavailable = roomUnavailableRepository.findOverlapping(
                                subReq.getRoomNumberId(), subReq.getCheckIn(), subReq.getCheckOut());
                        if (!unavailable.isEmpty()) {
                            throw new HolaException(ErrorCode.ROOM_UNAVAILABLE_FOR_RESERVATION);
                        }
                    }

                    // 변경이력용 이전 값 캡처 — Leg 레벨
                    LocalDate legPrevCheckIn = sub.getCheckIn();
                    LocalDate legPrevCheckOut = sub.getCheckOut();
                    int legPrevAdults = sub.getAdults();
                    int legPrevChildren = sub.getChildren();
                    Long legPrevRoomTypeId = sub.getRoomTypeId();
                    Long legPrevFloorId = sub.getFloorId();
                    Long legPrevRoomNumberId = sub.getRoomNumberId();
                    Boolean legPrevEarlyCheckIn = sub.getEarlyCheckIn();
                    Boolean legPrevLateCheckOut = sub.getLateCheckOut();

                    sub.update(subReq.getRoomTypeId(), subReq.getFloorId(), subReq.getRoomNumberId(),
                            subReq.getAdults() != null ? subReq.getAdults() : 1,
                            subReq.getChildren() != null ? subReq.getChildren() : 0,
                            subReq.getCheckIn(), subReq.getCheckOut(),
                            subReq.getEarlyCheckIn() != null ? subReq.getEarlyCheckIn() : false,
                            subReq.getLateCheckOut() != null ? subReq.getLateCheckOut() : false);

                    subCreator.updateGuests(sub, subReq.getGuests());

                    // Leg 변경이력 기록
                    try {
                        Long mid = master.getId();
                        Long sid = sub.getId();
                        String legLabel = sub.getSubReservationNo();
                        changeLogService.logFieldChange(mid, sid, "ROOM", "checkIn",
                                legPrevCheckIn, subReq.getCheckIn(), legLabel + " 체크인");
                        changeLogService.logFieldChange(mid, sid, "ROOM", "checkOut",
                                legPrevCheckOut, subReq.getCheckOut(), legLabel + " 체크아웃");
                        changeLogService.logFieldChange(mid, sid, "ROOM", "adults",
                                legPrevAdults, subReq.getAdults() != null ? subReq.getAdults() : 1, legLabel + " 성인");
                        changeLogService.logFieldChange(mid, sid, "ROOM", "children",
                                legPrevChildren, subReq.getChildren() != null ? subReq.getChildren() : 0, legLabel + " 아동");
                        // 객실타입 변경
                        if (!java.util.Objects.equals(legPrevRoomTypeId, subReq.getRoomTypeId())) {
                            String prevRtName = legPrevRoomTypeId != null ? roomTypeRepository.findById(legPrevRoomTypeId).map(RoomType::getRoomTypeCode).orElse(String.valueOf(legPrevRoomTypeId)) : null;
                            String newRtName = subReq.getRoomTypeId() != null ? roomTypeRepository.findById(subReq.getRoomTypeId()).map(RoomType::getRoomTypeCode).orElse(String.valueOf(subReq.getRoomTypeId())) : null;
                            changeLogService.logFieldChange(mid, sid, "ROOM", "roomTypeId",
                                    prevRtName, newRtName, legLabel + " 객실타입");
                        }
                        // 객실 배정 변경
                        if (!java.util.Objects.equals(legPrevRoomNumberId, subReq.getRoomNumberId())) {
                            String prevRoomName = resolveRoomDisplay(legPrevFloorId, legPrevRoomNumberId);
                            String newRoomName = resolveRoomDisplay(subReq.getFloorId(), subReq.getRoomNumberId());
                            changeLogService.logFieldChange(mid, sid, "ROOM", "roomNumberId",
                                    prevRoomName, newRoomName, legLabel + " 객실배정");
                        }
                        changeLogService.logFieldChange(mid, sid, "ROOM", "earlyCheckIn",
                                legPrevEarlyCheckIn, subReq.getEarlyCheckIn() != null ? subReq.getEarlyCheckIn() : false, legLabel + " 얼리체크인");
                        changeLogService.logFieldChange(mid, sid, "ROOM", "lateCheckOut",
                                legPrevLateCheckOut, subReq.getLateCheckOut() != null ? subReq.getLateCheckOut() : false, legLabel + " 레이트체크아웃");
                    } catch (Exception e) {
                        log.error("Leg 변경이력 기록 실패: {}", e.getMessage());
                    }

                    // 얼리/레이트 변경 시 fee 자동 등록/해제 (저장 버튼 통합 처리)
                    boolean earlyChanged = !java.util.Objects.equals(legPrevEarlyCheckIn, sub.getEarlyCheckIn());
                    boolean lateChanged = !java.util.Objects.equals(legPrevLateCheckOut, sub.getLateCheckOut());
                    if (earlyChanged) {
                        if (Boolean.TRUE.equals(sub.getEarlyCheckIn()) && sub.getEarlyCheckInFee() == null) {
                            try {
                                java.math.BigDecimal fee = earlyLateCheckService.calculateFeeByPolicyIndex(sub, "EARLY_CHECKIN", 0);
                                sub.registerEarlyCheckInFee(fee);
                            } catch (Exception e) {
                                log.warn("얼리체크인 요금 자동 등록 실패 (정책 없음): {}", e.getMessage());
                            }
                        } else if (Boolean.FALSE.equals(sub.getEarlyCheckIn())) {
                            sub.clearEarlyCheckInFee();
                        }
                    }
                    if (lateChanged) {
                        if (Boolean.TRUE.equals(sub.getLateCheckOut()) && sub.getLateCheckOutFee() == null) {
                            try {
                                java.math.BigDecimal fee = earlyLateCheckService.calculateFeeByPolicyIndex(sub, "LATE_CHECKOUT", 0);
                                sub.registerLateCheckOutFee(fee);
                            } catch (Exception e) {
                                log.warn("레이트체크아웃 요금 자동 등록 실패 (정책 없음): {}", e.getMessage());
                            }
                        } else if (Boolean.FALSE.equals(sub.getLateCheckOut())) {
                            sub.clearLateCheckOutFee();
                        }
                    }

                    // 레이트코드/날짜/인원 변경 시에만 요금 재계산
                    boolean datesOrGuestsChanged = !subReq.getCheckIn().equals(legPrevCheckIn)
                            || !subReq.getCheckOut().equals(legPrevCheckOut)
                            || sub.getAdults() != legPrevAdults
                            || sub.getChildren() != legPrevChildren;
                    if (rateChanged || datesOrGuestsChanged) {
                        subCreator.recalculateDailyCharges(sub, property);
                    }

                    // 레이트코드 변경 시 포함 서비스 갱신
                    if (rateChanged) {
                        rateIncludedServiceHelper.refreshRateIncludedServices(sub, request.getRateCodeId());
                    }
                } else {
                    // 신규 레그 추가
                    int legSeq = subReservationRepository.countAllIncludingDeleted(master.getId()) + 1;
                    SubReservation newSub = subCreator.create(master, subReq, legSeq, property);
                    try {
                        changeLogService.log(master.getId(), newSub.getId(), "ROOM", "ADD_LEG",
                                null, null, newSub.getSubReservationNo(),
                                "객실 레그 추가: " + newSub.getSubReservationNo());
                    } catch (Exception e) {
                        log.error("Leg 추가 변경이력 기록 실패: {}", e.getMessage());
                    }
                }
            }
            syncMasterDates(master);
        }

        // 결제 금액 재계산
        paymentService.recalculatePayment(master.getId());

        // 변경이력 기록 — 마스터 레벨 전체 필드
        try {
            Long mid = master.getId();
            // 날짜
            changeLogService.logFieldChange(mid, null, "RESERVATION", "masterCheckIn",
                    prevCheckIn, request.getMasterCheckIn(), "체크인");
            changeLogService.logFieldChange(mid, null, "RESERVATION", "masterCheckOut",
                    prevCheckOut, request.getMasterCheckOut(), "체크아웃");
            // 레이트코드/마켓코드
            String prevRateName = prevRateCodeId != null ? rateCodeRepository.findById(prevRateCodeId).map(rc -> rc.getRateNameKo()).orElse(String.valueOf(prevRateCodeId)) : null;
            String newRateName = effectiveRateCodeId != null ? rateCodeRepository.findById(effectiveRateCodeId).map(rc -> rc.getRateNameKo()).orElse(String.valueOf(effectiveRateCodeId)) : null;
            changeLogService.logFieldChange(mid, null, "RATE", "rateCodeId",
                    prevRateName, newRateName, "레이트코드");
            String prevMarketName = prevMarketCodeId != null ? marketCodeRepository.findById(prevMarketCodeId).map(mc -> mc.getMarketName()).orElse(String.valueOf(prevMarketCodeId)) : null;
            String newMarketName = request.getMarketCodeId() != null ? marketCodeRepository.findById(request.getMarketCodeId()).map(mc -> mc.getMarketName()).orElse(String.valueOf(request.getMarketCodeId())) : null;
            changeLogService.logFieldChange(mid, null, "RESERVATION", "marketCodeId",
                    prevMarketName, newMarketName, "마켓코드");
            // 게스트 정보
            changeLogService.logFieldChange(mid, null, "RESERVATION", "guestNameKo",
                    prevGuestNameKo, request.getGuestNameKo(), "투숙객명");
            changeLogService.logFieldChange(mid, null, "RESERVATION", "guestFirstNameEn",
                    prevGuestFirstNameEn, request.getGuestFirstNameEn(), "영문 이름(First)");
            changeLogService.logFieldChange(mid, null, "RESERVATION", "guestMiddleNameEn",
                    prevGuestMiddleNameEn, request.getGuestMiddleNameEn(), "영문 이름(Middle)");
            changeLogService.logFieldChange(mid, null, "RESERVATION", "guestLastNameEn",
                    prevGuestLastNameEn, request.getGuestLastNameEn(), "영문 성(Last)");
            changeLogService.logFieldChange(mid, null, "RESERVATION", "phoneNumber",
                    (prevPhoneCountryCode != null ? prevPhoneCountryCode + " " : "") + prevPhoneNumber,
                    (request.getPhoneCountryCode() != null ? request.getPhoneCountryCode() + " " : "") + request.getPhoneNumber(),
                    "전화번호");
            changeLogService.logFieldChange(mid, null, "RESERVATION", "email",
                    prevEmail, request.getEmail(), "이메일");
            changeLogService.logFieldChange(mid, null, "RESERVATION", "birthDate",
                    prevBirthDate, request.getBirthDate(), "생년월일");
            changeLogService.logFieldChange(mid, null, "RESERVATION", "gender",
                    prevGender, request.getGender(), "성별");
            changeLogService.logFieldChange(mid, null, "RESERVATION", "nationality",
                    prevNationality, request.getNationality(), "국적");
            // 프로모션/OTA
            changeLogService.logFieldChange(mid, null, "RESERVATION", "promotionType",
                    prevPromotionType, request.getPromotionType(), "프로모션 유형");
            changeLogService.logFieldChange(mid, null, "RESERVATION", "promotionCode",
                    prevPromotionCode, request.getPromotionCode(), "프로모션 코드");
            changeLogService.logFieldChange(mid, null, "RESERVATION", "otaReservationNo",
                    prevOtaReservationNo, request.getOtaReservationNo(), "OTA 예약번호");
            // 고객 요청사항
            changeLogService.logFieldChange(mid, null, "RESERVATION", "customerRequest",
                    prevCustomerRequest, request.getCustomerRequest(), "고객 요청사항");
        } catch (Exception e) {
            log.error("변경이력 기록 실패: {}", e.getMessage());
        }

        log.info("마스터 예약 수정: {}", master.getMasterReservationNo());
        return getById(id, propertyId);
    }

    @Override
    @Transactional
    public void deleteReservation(Long id, Long propertyId) {
        // 슈퍼어드민 권한 검증
        if (!accessControlService.getCurrentUser().isSuperAdmin()) {
            throw new HolaException(ErrorCode.RESERVATION_DELETE_UNAUTHORIZED);
        }

        MasterReservation master = finder.findMasterById(id, propertyId);

        // CHECKED_OUT 상태만 삭제 가능
        if (!"CHECKED_OUT".equals(master.getReservationStatus())) {
            throw new HolaException(ErrorCode.RESERVATION_DELETE_NOT_ALLOWED);
        }

        // 서브 예약 soft delete
        for (SubReservation sub : master.getSubReservations()) {
            sub.softDelete();
        }

        // 마스터 예약 soft delete
        master.softDelete();

        log.info("예약 삭제(soft): {} by SUPER_ADMIN", master.getMasterReservationNo());
    }

    @Override
    public int checkAvailability(Long propertyId, Long roomTypeId, LocalDate checkIn, LocalDate checkOut) {
        return availabilityService.getAvailableRoomCount(roomTypeId, checkIn, checkOut);
    }

    @Override
    public RateChangePreviewResponse previewRateChange(Long reservationId, Long propertyId, Long newRateCodeId) {
        MasterReservation master = finder.findMasterById(reservationId, propertyId);
        Property property = master.getProperty();

        // 현재/새 레이트코드 정보 조회
        Long currentRateCodeId = master.getRateCodeId();
        RateCode currentRateCode = currentRateCodeId != null
                ? rateCodeRepository.findById(currentRateCodeId).orElse(null) : null;
        RateCode newRateCode = rateCodeRepository.findById(newRateCodeId)
                .orElseThrow(() -> new HolaException(ErrorCode.RATE_CODE_NOT_FOUND));

        // 활성 서브 예약만 대상
        List<SubReservation> activeSubs = master.getSubReservations().stream()
                .filter(s -> !"CANCELED".equals(s.getRoomReservationStatus())
                        && !"NO_SHOW".equals(s.getRoomReservationStatus()))
                .toList();

        // Leg별 요금 비교
        BigDecimal totalCurrent = BigDecimal.ZERO;
        BigDecimal totalNew = BigDecimal.ZERO;
        List<RateChangePreviewResponse.LegPreview> legPreviews = new ArrayList<>();

        // 객실타입명 resolve
        Set<Long> roomTypeIds = activeSubs.stream()
                .map(SubReservation::getRoomTypeId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, String> roomTypeMap = roomInfoResolver.resolveRoomTypeCodes(roomTypeIds);

        for (SubReservation sub : activeSubs) {
            // 현재 요금 합계
            BigDecimal currentCharge = sub.getDailyCharges().stream()
                    .map(DailyCharge::getTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 새 레이트코드로 계산 (저장하지 않음)
            BigDecimal newCharge;
            try {
                List<DailyCharge> newCharges = priceCalculationService.calculateDailyCharges(
                        newRateCodeId, property,
                        sub.getCheckIn(), sub.getCheckOut(),
                        sub.getAdults(), sub.getChildren(), null);
                newCharge = newCharges.stream()
                        .map(DailyCharge::getTotal)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            } catch (Exception e) {
                // 요금 계산 실패 시 (커버리지 미달 등) 현재 요금 유지
                log.warn("레이트코드 변경 미리보기 요금 계산 실패: subId={}, newRateCodeId={}", sub.getId(), newRateCodeId, e);
                newCharge = currentCharge;
            }

            totalCurrent = totalCurrent.add(currentCharge);
            totalNew = totalNew.add(newCharge);

            legPreviews.add(RateChangePreviewResponse.LegPreview.builder()
                    .legId(sub.getId())
                    .roomTypeName(sub.getRoomTypeId() != null ? roomTypeMap.get(sub.getRoomTypeId()) : null)
                    .currentCharge(currentCharge)
                    .newCharge(newCharge)
                    .difference(newCharge.subtract(currentCharge))
                    .build());
        }

        return RateChangePreviewResponse.builder()
                .currentRateCodeId(currentRateCodeId)
                .currentRateCodeName(currentRateCode != null
                        ? currentRateCode.getRateCode() + " - " + currentRateCode.getRateNameKo() : null)
                .newRateCodeId(newRateCodeId)
                .newRateCodeName(newRateCode.getRateCode() + " - " + newRateCode.getRateNameKo())
                .currentTotal(totalCurrent)
                .newTotal(totalNew)
                .difference(totalNew.subtract(totalCurrent))
                .legs(legPreviews)
                .build();
    }

    // ─── private helpers ──────────────────────────

    /**
     * 서브 예약의 층/호수/객실타입/서비스명을 벌크 resolve
     */
    private List<SubReservationResponse> resolveSubReservationNames(List<SubReservationResponse> subs) {
        if (subs == null || subs.isEmpty()) return subs;

        Set<Long> floorIds = new HashSet<>();
        Set<Long> roomNumberIds = new HashSet<>();
        Set<Long> roomTypeIds = new HashSet<>();
        Set<Long> serviceOptionIds = new HashSet<>();
        for (SubReservationResponse sub : subs) {
            if (sub.getFloorId() != null) floorIds.add(sub.getFloorId());
            if (sub.getRoomNumberId() != null) roomNumberIds.add(sub.getRoomNumberId());
            if (sub.getRoomTypeId() != null) roomTypeIds.add(sub.getRoomTypeId());
            if (sub.getServices() != null) {
                for (ReservationServiceResponse svc : sub.getServices()) {
                    if (svc.getServiceOptionId() != null) serviceOptionIds.add(svc.getServiceOptionId());
                }
            }
        }

        Map<Long, String> floorMap = roomInfoResolver.resolveFloorNames(floorIds);
        Map<Long, String> roomNumberMap = roomInfoResolver.resolveRoomNumbers(roomNumberIds);
        Map<Long, String> roomTypeMap = roomInfoResolver.resolveRoomTypeCodes(roomTypeIds);
        Map<Long, String> serviceOptionNameMap = roomInfoResolver.resolveServiceOptionNames(serviceOptionIds);

        return subs.stream().map(sub -> {
            List<ReservationServiceResponse> resolvedServices = sub.getServices() != null
                    ? sub.getServices().stream().map(svc -> ReservationServiceResponse.builder()
                        .id(svc.getId())
                        .serviceType(svc.getServiceType())
                        .serviceOptionId(svc.getServiceOptionId())
                        .serviceName(svc.getServiceOptionId() != null ? serviceOptionNameMap.get(svc.getServiceOptionId()) : null)
                        .serviceDate(svc.getServiceDate())
                        .quantity(svc.getQuantity())
                        .unitPrice(svc.getUnitPrice())
                        .tax(svc.getTax())
                        .totalPrice(svc.getTotalPrice())
                        .build()).collect(Collectors.toList())
                    : Collections.emptyList();

            return sub.toBuilder()
                .roomTypeName(sub.getRoomTypeId() != null ? roomTypeMap.get(sub.getRoomTypeId()) : null)
                .floorName(sub.getFloorId() != null ? floorMap.get(sub.getFloorId()) : null)
                .roomNumber(sub.getRoomNumberId() != null ? roomNumberMap.get(sub.getRoomNumberId()) : null)
                .services(resolvedServices)
                .build();
        }).collect(Collectors.toList());
    }

    private void syncMasterDates(MasterReservation master) {
        List<SubReservation> activeSubs = master.getSubReservations().stream()
                .filter(s -> !"CANCELED".equals(s.getRoomReservationStatus()))
                .toList();

        if (activeSubs.isEmpty()) return;

        LocalDate earliestCheckIn = activeSubs.stream()
                .map(SubReservation::getCheckIn)
                .min(LocalDate::compareTo)
                .orElse(master.getMasterCheckIn());

        LocalDate latestCheckOut = activeSubs.stream()
                .map(SubReservation::getCheckOut)
                .max(LocalDate::compareTo)
                .orElse(master.getMasterCheckOut());

        master.syncDates(earliestCheckIn, latestCheckOut);
    }

    /**
     * 층/호수 ID → "12층 / 1201" 표시용 문자열 변환
     */
    private String resolveRoomDisplay(Long floorId, Long roomNumberId) {
        if (floorId == null && roomNumberId == null) return null;
        Map<Long, String> floorMap = floorId != null
                ? roomInfoResolver.resolveFloorNames(Set.of(floorId)) : Collections.emptyMap();
        Map<Long, String> roomMap = roomNumberId != null
                ? roomInfoResolver.resolveRoomNumbers(Set.of(roomNumberId)) : Collections.emptyMap();
        String floor = floorId != null ? floorMap.get(floorId) : null;
        String room = roomNumberId != null ? roomMap.get(roomNumberId) : null;
        if (floor != null && room != null) return floor + " / " + room;
        if (room != null) return room;
        return floor;
    }
}
