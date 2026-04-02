package com.hola.reservation.service;

import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.common.security.AccessControlService;
import com.hola.hotel.entity.Property;
import com.hola.hotel.entity.RoomUnavailable;
import com.hola.hotel.repository.MarketCodeRepository;
import com.hola.hotel.repository.PropertyRepository;
import com.hola.hotel.repository.RoomUnavailableRepository;
import com.hola.rate.repository.RateCodeRepository;
import com.hola.reservation.dto.request.ReservationCreateRequest;
import com.hola.reservation.dto.request.ReservationUpdateRequest;
import com.hola.reservation.dto.request.SubReservationRequest;
import com.hola.reservation.dto.response.*;
import com.hola.reservation.entity.*;
import com.hola.reservation.mapper.ReservationMapper;
import com.hola.reservation.repository.*;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
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
    private final AccessControlService accessControlService;
    private final RateIncludedServiceHelper rateIncludedServiceHelper;
    private final EntityManager entityManager;

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

                    sub.update(subReq.getRoomTypeId(), subReq.getFloorId(), subReq.getRoomNumberId(),
                            subReq.getAdults() != null ? subReq.getAdults() : 1,
                            subReq.getChildren() != null ? subReq.getChildren() : 0,
                            subReq.getCheckIn(), subReq.getCheckOut(),
                            subReq.getEarlyCheckIn() != null ? subReq.getEarlyCheckIn() : false,
                            subReq.getLateCheckOut() != null ? subReq.getLateCheckOut() : false);

                    subCreator.updateGuests(sub, subReq.getGuests());
                    subCreator.recalculateDailyCharges(sub, property);

                    // 레이트코드 변경 시 포함 서비스 갱신
                    if (rateChanged) {
                        rateIncludedServiceHelper.refreshRateIncludedServices(sub, request.getRateCodeId());
                    }
                } else {
                    // 신규 레그 추가
                    int legSeq = subReservationRepository.countAllIncludingDeleted(master.getId()) + 1;
                    subCreator.create(master, subReq, legSeq, property);
                }
            }
            syncMasterDates(master);
        }

        // 결제 금액 재계산
        paymentService.recalculatePayment(master.getId());

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

}
