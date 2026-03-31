package com.hola.reservation.service;

import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.common.security.AccessControlService;
import com.hola.common.util.NameMaskingUtil;
import com.hola.hotel.entity.Floor;
import com.hola.hotel.entity.Property;
import com.hola.hotel.entity.RoomNumber;
import com.hola.hotel.repository.FloorRepository;
import com.hola.hotel.repository.MarketCodeRepository;
import com.hola.hotel.repository.PropertyRepository;
import com.hola.hotel.repository.RoomNumberRepository;
import com.hola.hotel.repository.RoomUnavailableRepository;
import com.hola.hotel.entity.RoomUnavailable;
import com.hola.rate.repository.RateCodeRepository;
import com.hola.room.entity.PaidServiceOption;
import com.hola.room.entity.RoomType;
import com.hola.room.entity.RoomTypeFloor;
import com.hola.room.repository.PaidServiceOptionRepository;
import com.hola.room.repository.RoomTypeFloorRepository;
import com.hola.room.repository.RoomTypeRepository;
import com.hola.reservation.dto.request.*;
import com.hola.reservation.dto.response.*;
import com.hola.reservation.entity.*;
import com.hola.reservation.mapper.ReservationMapper;
import com.hola.reservation.repository.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hola.common.enums.StayType;
import com.hola.rate.entity.RateCode;
import com.hola.rate.entity.DayUseRate;
import com.hola.rate.repository.DayUseRateRepository;
import com.hola.reservation.vo.DayUseTimeSlot;

import java.math.BigDecimal;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 예약 관리 서비스 구현
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationServiceImpl implements ReservationService {

    private final MasterReservationRepository masterReservationRepository;
    private final SubReservationRepository subReservationRepository;
    private final ReservationGuestRepository reservationGuestRepository;
    private final DailyChargeRepository dailyChargeRepository;
    private final ReservationDepositRepository reservationDepositRepository;
    private final ReservationMemoRepository reservationMemoRepository;
    private final PropertyRepository propertyRepository;
    private final RateCodeRepository rateCodeRepository;
    private final MarketCodeRepository marketCodeRepository;
    private final FloorRepository floorRepository;
    private final RoomNumberRepository roomNumberRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomTypeFloorRepository roomTypeFloorRepository;
    private final PaidServiceOptionRepository paidServiceOptionRepository;
    private final ReservationServiceItemRepository serviceItemRepository;
    private final ReservationMapper reservationMapper;
    private final ReservationNumberGenerator numberGenerator;
    private final RoomAvailabilityService availabilityService;
    private final PriceCalculationService priceCalculationService;
    private final EarlyLateCheckService earlyLateCheckService;
    private final ReservationPaymentService paymentService;
    private final AccessControlService accessControlService;
    private final RoomUnavailableRepository roomUnavailableRepository;
    private final com.hola.reservation.booking.service.CancellationPolicyService cancellationPolicyService;
    private final com.hola.room.service.InventoryService inventoryService;
    private final ReservationPaymentRepository reservationPaymentRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final RateIncludedServiceHelper rateIncludedServiceHelper;
    private final DayUseRateRepository dayUseRateRepository;
    private final EntityManager entityManager;
    private final com.hola.hotel.service.HousekeepingService housekeepingService;

    // 허용되는 상태 전이 매트릭스
    // 체크인 액션 → 바로 INHOUSE (CHECK_IN 중간 상태 제거, 업계 표준)
    private static final Map<String, Set<String>> STATUS_TRANSITIONS = Map.of(
            "RESERVED", Set.of("INHOUSE", "CANCELED", "NO_SHOW"),
            "CHECK_IN", Set.of("INHOUSE", "CANCELED"),  // 하위 호환: 기존 CHECK_IN 데이터 → INHOUSE 전이 허용
            "INHOUSE", Set.of("CHECKED_OUT"),
            "CHECKED_OUT", Set.of(),
            "CANCELED", Set.of(),
            "NO_SHOW", Set.of()
    );

    // 수정 불가 상태
    private static final Set<String> IMMUTABLE_STATUSES = Set.of("CHECKED_OUT", "CANCELED", "NO_SHOW");

    @Override
    public List<ReservationListResponse> getList(Long propertyId, String status, LocalDate checkInFrom,
                                                   LocalDate checkInTo, String keyword) {
        // 전체 조회 후 Java 필터링 (Hibernate 6 + PostgreSQL null 파라미터 타입 추론 이슈 회피)
        List<MasterReservation> reservations = masterReservationRepository
                .findByPropertyIdOrderByReservationDateDesc(propertyId);

        return reservations.stream()
                .filter(r -> filterByStatus(r, status))
                .filter(r -> filterByDateRange(r, checkInFrom, checkInTo))
                .filter(r -> filterByKeyword(r, keyword))
                .map(reservationMapper::toReservationListResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ReservationDetailResponse getById(Long id, Long propertyId) {
        MasterReservation master = findMasterById(id, propertyId);
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
                .stayType(isDayUseRateCode(response.getRateCodeId()) ? "DAY_USE" : "OVERNIGHT")
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

    /**
     * 서브 예약의 층/호수/객실타입/서비스명을 벌크 resolve
     */
    private List<SubReservationResponse> resolveSubReservationNames(List<SubReservationResponse> subs) {
        if (subs == null || subs.isEmpty()) return subs;

        // ID 수집
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

        // 벌크 조회
        Map<Long, String> floorMap = floorIds.isEmpty() ? Map.of()
                : floorRepository.findAllById(floorIds).stream()
                    .collect(Collectors.toMap(Floor::getId, f -> f.getFloorNumber() + (f.getFloorName() != null ? " | " + f.getFloorName() : "")));
        Map<Long, String> roomNumberMap = roomNumberIds.isEmpty() ? Map.of()
                : roomNumberRepository.findAllById(roomNumberIds).stream()
                    .collect(Collectors.toMap(RoomNumber::getId, RoomNumber::getRoomNumber));
        Map<Long, String> roomTypeMap = roomTypeIds.isEmpty() ? Map.of()
                : roomTypeRepository.findAllById(roomTypeIds).stream()
                    .collect(Collectors.toMap(RoomType::getId, RoomType::getRoomTypeCode));
        Map<Long, String> serviceOptionNameMap = serviceOptionIds.isEmpty() ? Map.of()
                : paidServiceOptionRepository.findAllById(serviceOptionIds).stream()
                    .collect(Collectors.toMap(PaidServiceOption::getId, PaidServiceOption::getServiceNameKo));

        // 이름이 포함된 새 SubReservationResponse 생성
        // toBuilder 패턴: 기존 필드 자동 유지, 이름만 resolve (새 필드 추가 시 누락 방지)
        return subs.stream().map(sub -> {
            // 서비스 항목에 서비스명 매핑
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

    @Override
    @Transactional
    public ReservationDetailResponse create(Long propertyId, ReservationCreateRequest request) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new HolaException(ErrorCode.PROPERTY_NOT_FOUND));

        // 레이트코드 필수 검증
        if (request.getRateCodeId() == null) {
            throw new HolaException(ErrorCode.RESERVATION_RATE_REQUIRED);
        }

        // Dayuse 여부 판별 (날짜 검증 전에 확인)
        boolean isDayUse = isDayUseRateCode(request.getRateCodeId());

        // 체크인/체크아웃 유효성 (dayuse는 같은 날짜 허용)
        validateDates(request.getMasterCheckIn(), request.getMasterCheckOut(), isDayUse);

        // 신규 예약은 과거 날짜 체크인 불가
        if (request.getMasterCheckIn().isBefore(LocalDate.now())) {
            throw new HolaException(ErrorCode.RESERVATION_CHECKIN_PAST_DATE);
        }

        // 레이트코드 판매기간/숙박일수 검증
        validateRateCode(request.getRateCodeId(), request.getMasterCheckIn(), request.getMasterCheckOut());

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
                createSubReservation(master, subRequest, legSeq++, property);
            }
        }

        // 결제 금액 계산 (일별 요금 기반)
        paymentService.recalculatePayment(master.getId());

        // L1 캐시 클리어하여 getById에서 서브예약 포함한 완전한 데이터 조회
        entityManager.flush();
        entityManager.clear();

        return getById(master.getId(), propertyId);
    }

    @Override
    @Transactional
    public ReservationDetailResponse update(Long id, Long propertyId, ReservationUpdateRequest request) {
        MasterReservation master = findMasterById(id, propertyId);

        // 수정 불가 상태 검사
        validateModifiable(master);

        // OTA 수정 제한 검사
        if (Boolean.TRUE.equals(master.getIsOtaManaged())) {
            throw new HolaException(ErrorCode.RESERVATION_OTA_EDIT_RESTRICTED);
        }

        // Dayuse 여부 판별
        boolean isDayUse = isDayUseRateCode(master.getRateCodeId());
        validateDates(request.getMasterCheckIn(), request.getMasterCheckOut(), isDayUse);

        // 수정 시에도 체크인 날짜가 과거인지 검증 (기존 체크인이 미래였다면 과거로 변경 불가)
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
                validateRateCode(request.getRateCodeId(), request.getMasterCheckIn(), request.getMasterCheckOut());
            }
        }

        // rateCodeId null이면 기존 값 유지 (null로 덮어쓰기 방지)
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
                    SubReservation sub = findSubAndValidateOwnership(subReq.getId(), master);
                    validateDates(subReq.getCheckIn(), subReq.getCheckOut());

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

                    updateGuests(sub, subReq.getGuests());
                    recalculateDailyCharges(sub, property);

                    // 레이트코드 변경 시 포함 서비스 갱신
                    if (rateChanged) {
                        rateIncludedServiceHelper.refreshRateIncludedServices(sub, request.getRateCodeId());
                    }
                } else {
                    // 신규 레그 추가 (소프트삭제 포함 전체 수 기준 채번)
                    int legSeq = subReservationRepository.countAllIncludingDeleted(master.getId()) + 1;
                    createSubReservation(master, subReq, legSeq, property);
                }
            }
            syncMasterDates(master);
        }

        // 결제 금액 재계산 (일별 요금 변경 반영)
        paymentService.recalculatePayment(master.getId());

        log.info("마스터 예약 수정: {}", master.getMasterReservationNo());
        return getById(id, propertyId);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminCancelPreviewResponse getCancelPreview(Long id, Long propertyId, boolean noShow, Long subReservationId) {
        MasterReservation master = findMasterById(id, propertyId);

        // 취소/노쇼 가능 상태 확인
        String currentStatus = master.getReservationStatus();
        if (!"RESERVED".equals(currentStatus) && !"CHECK_IN".equals(currentStatus)
                && !"INHOUSE".equals(currentStatus)) {
            throw new HolaException(ErrorCode.RESERVATION_STATUS_CHANGE_NOT_ALLOWED);
        }

        // 1박 요금 조회: Leg 지정 시 해당 Leg 기준, 아니면 마스터(원본) 기준
        BigDecimal firstNightSupply;
        if (subReservationId != null) {
            firstNightSupply = getFirstNightTotalForSub(subReservationId);
        } else {
            firstNightSupply = getFirstNightTotal(master.getId());
        }

        // 취소 수수료 계산 (노쇼 여부 전달)
        var cancelResult = cancellationPolicyService.calculateCancelFee(
                propertyId, master.getMasterCheckIn(), firstNightSupply, noShow);

        // 결제 정보 조회: Leg 단위 취소 시 해당 Leg의 결제 현황 사용
        BigDecimal totalPaid = BigDecimal.ZERO;
        BigDecimal grandTotal = BigDecimal.ZERO;
        BigDecimal unpaidBalance = BigDecimal.ZERO;
        String targetSubNo = null;

        if (subReservationId != null) {
            // Leg 단위: 해당 Leg의 결제 현황으로 계산
            List<LegPaymentInfo> legPayments = paymentService.calculatePerLegPayments(master.getId());
            LegPaymentInfo legInfo = legPayments.stream()
                    .filter(lp -> lp.getSubReservationId().equals(subReservationId))
                    .findFirst().orElse(null);
            if (legInfo != null) {
                grandTotal = legInfo.getLegTotal();
                totalPaid = legInfo.getLegPaid().subtract(legInfo.getLegRefunded());
                unpaidBalance = legInfo.getLegRemaining();
                targetSubNo = legInfo.getSubReservationNo();
            }
        } else {
            // 전체 취소: Master 결제 현황
            ReservationPayment payment = reservationPaymentRepository
                    .findByMasterReservationId(master.getId()).orElse(null);
            if (payment != null) {
                if (payment.getTotalPaidAmount() != null) totalPaid = payment.getTotalPaidAmount();
                if (payment.getGrandTotal() != null) grandTotal = payment.getGrandTotal();
                BigDecimal refund = payment.getRefundAmount() != null ? payment.getRefundAmount() : BigDecimal.ZERO;
                totalPaid = totalPaid.subtract(refund);
            }
            unpaidBalance = grandTotal.subtract(totalPaid).max(BigDecimal.ZERO);
        }

        BigDecimal cancelFee = cancelResult.feeAmount();
        BigDecimal refundAmt = totalPaid.subtract(cancelFee).max(BigDecimal.ZERO);
        BigDecimal outstandingCancelFee = cancelFee.subtract(totalPaid).max(BigDecimal.ZERO);

        // 결제 트랜잭션 조회 + 환불 분배 계산
        List<PaymentTransaction> txns = paymentTransactionRepository
                .findByMasterReservationIdOrderByTransactionSeqAsc(master.getId());

        // Leg 단위 취소 시 해당 Leg의 결제만 필터링
        final Long targetSubId = subReservationId;
        List<PaymentTransaction> paymentTxns = txns.stream()
                .filter(t -> "PAYMENT".equals(t.getTransactionType()))
                .filter(t -> targetSubId == null || targetSubId.equals(t.getSubReservationId()))
                .toList();

        boolean isPgPayment = false;
        String pgCardNo = null;
        String pgIssuerName = null;

        // PG / 비-PG 결제 분리
        List<PaymentTransaction> pgPayments = paymentTxns.stream()
                .filter(t -> t.getPgCno() != null).toList();
        List<PaymentTransaction> nonPgPayments = paymentTxns.stream()
                .filter(t -> t.getPgCno() == null).toList();

        if (!pgPayments.isEmpty()) {
            isPgPayment = true;
            pgCardNo = pgPayments.get(0).getPgCardNo();
            pgIssuerName = pgPayments.get(0).getPgIssuerName();
        }

        // 환불 분배 내역 계산
        List<AdminCancelPreviewResponse.RefundBreakdown> breakdowns = new ArrayList<>();
        BigDecimal pgRefundTotal = BigDecimal.ZERO;
        BigDecimal nonPgRefundTotal = BigDecimal.ZERO;
        String nonPgMethodResult = null;

        if (refundAmt.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal pgPaidTotal = pgPayments.stream()
                    .map(PaymentTransaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal nonPgPaidTotal = nonPgPayments.stream()
                    .map(PaymentTransaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

            pgRefundTotal = refundAmt.min(pgPaidTotal);
            nonPgRefundTotal = refundAmt.subtract(pgRefundTotal);

            if (pgRefundTotal.compareTo(BigDecimal.ZERO) > 0) {
                breakdowns.add(AdminCancelPreviewResponse.RefundBreakdown.builder()
                        .paymentMethod("CARD")
                        .paidAmount(pgPaidTotal)
                        .refundAmount(pgRefundTotal)
                        .pgRefund(true)
                        .cardInfo((pgIssuerName != null ? pgIssuerName + " " : "") + (pgCardNo != null ? pgCardNo : ""))
                        .build());
            }
            if (nonPgRefundTotal.compareTo(BigDecimal.ZERO) > 0) {
                nonPgMethodResult = nonPgPayments.isEmpty() ? "CASH"
                        : nonPgPayments.get(nonPgPayments.size() - 1).getPaymentMethod();
                breakdowns.add(AdminCancelPreviewResponse.RefundBreakdown.builder()
                        .paymentMethod(nonPgMethodResult)
                        .paidAmount(nonPgPaidTotal)
                        .refundAmount(nonPgRefundTotal)
                        .pgRefund(false)
                        .build());
            }
        }

        return AdminCancelPreviewResponse.builder()
                .reservationId(master.getId())
                .masterReservationNo(master.getMasterReservationNo())
                .subReservationNo(targetSubNo)
                .guestNameKo(master.getGuestNameKo())
                .checkIn(master.getMasterCheckIn().toString())
                .checkOut(master.getMasterCheckOut().toString())
                .reservationStatus(currentStatus)
                .firstNightTotal(firstNightSupply)
                .cancelFeeAmount(cancelFee)
                .cancelFeePercent(cancelResult.feePercent())
                .totalPaidAmount(totalPaid)
                .refundAmount(refundAmt)
                .outstandingCancelFee(outstandingCancelFee)
                .grandTotal(grandTotal)
                .unpaidBalance(unpaidBalance)
                .policyDescription(cancelResult.policyDescription())
                .pgPayment(isPgPayment)
                .pgCardNo(pgCardNo)
                .pgIssuerName(pgIssuerName)
                .pgRefundAmount(pgRefundTotal)
                .nonPgRefundAmount(nonPgRefundTotal)
                .nonPgRefundMethod(nonPgMethodResult)
                .refundBreakdowns(breakdowns)
                .build();
    }

    @Override
    @Transactional
    public void cancel(Long id, Long propertyId) {
        MasterReservation master = findMasterById(id, propertyId);

        // RESERVED / CHECK_IN 상태에서 취소 가능
        String currentStatus = master.getReservationStatus();
        if (!"RESERVED".equals(currentStatus) && !"CHECK_IN".equals(currentStatus)) {
            throw new HolaException(ErrorCode.RESERVATION_STATUS_CHANGE_NOT_ALLOWED);
        }

        // 미결제 잔액 검증 + 취소 수수료 검증
        validateUnpaidBalance(master);

        // 취소 수수료 계산
        BigDecimal firstNightSupply = getFirstNightTotal(master.getId());
        var cancelResult = cancellationPolicyService.calculateCancelFee(
                propertyId, master.getMasterCheckIn(), firstNightSupply);

        // 결제 정보 업데이트 (취소 수수료 + 환불 금액)
        ReservationPayment payment = reservationPaymentRepository
                .findByMasterReservationId(master.getId()).orElse(null);
        if (payment != null) {
            BigDecimal totalPaid = payment.getTotalPaidAmount() != null ? payment.getTotalPaidAmount() : BigDecimal.ZERO;
            BigDecimal existingRefund = payment.getRefundAmount() != null ? payment.getRefundAmount() : BigDecimal.ZERO;
            BigDecimal existingCancelFee = payment.getCancelFeeAmount() != null ? payment.getCancelFeeAmount() : BigDecimal.ZERO;
            // 순결제액: 활성 예약에 할당된 금액 (이전 환불 + 이전 취소수수료 차감)
            BigDecimal netPaid = totalPaid.subtract(existingRefund).subtract(existingCancelFee);
            BigDecimal cancelFee = cancelResult.feeAmount();

            // 취소 수수료 미결제 검증: 수수료 > 순결제액이면 차단
            if (cancelFee.compareTo(BigDecimal.ZERO) > 0 && netPaid.compareTo(cancelFee) < 0) {
                throw new HolaException(ErrorCode.CANCEL_FEE_UNPAID);
            }

            BigDecimal refundAmt = netPaid.subtract(cancelFee).max(BigDecimal.ZERO);

            payment.updateCancelRefund(cancelFee, refundAmt);

            // 환불 거래 기록 (OTA 예약은 PG 환불 차단 — OTA 채널에서 환불 처리)
            if (refundAmt.compareTo(BigDecimal.ZERO) > 0 || cancelFee.compareTo(BigDecimal.ZERO) > 0) {
                String memo = cancelResult.policyDescription() + " / 취소 환불 (수수료: " + cancelFee + "원)";
                if (Boolean.TRUE.equals(master.getIsOtaManaged())) {
                    memo += " [OTA 예약 — PG 환불은 OTA 채널에서 처리 필요]";
                }
                paymentService.processRefundWithPg(master.getId(),
                        Boolean.TRUE.equals(master.getIsOtaManaged()) ? BigDecimal.ZERO : refundAmt,
                        cancelFee, memo);
            }

            // 취소 후 REFUNDED 상태로 확정 (grandTotal은 원본 유지 — 감사 추적용)
            payment.setPaymentStatusRefunded();
        }

        // 서비스 재고 복원
        for (SubReservation sub : master.getSubReservations()) {
            releaseServiceItemInventory(sub);
        }

        // 상태 변경
        master.updateStatus("CANCELED");
        for (SubReservation sub : master.getSubReservations()) {
            sub.updateStatus("CANCELED");
        }

        log.info("예약 취소: {}, OTA={}, 취소수수료: {}, 정책: {}",
                master.getMasterReservationNo(), master.getIsOtaManaged(),
                cancelResult.feeAmount(), cancelResult.policyDescription());
    }

    /**
     * 첫 번째 서브예약의 1박 공급가 조회
     */
    /**
     * 취소/노쇼 수수료 기준 1박 총액 조회
     * 원본 1박 총액(originalFirstNightTotal)이 보존되어 있으면 우선 사용 (업그레이드 후에도 원래 요금 기준)
     * 없으면 현재 DailyCharge에서 조회 (하위호환)
     */
    private BigDecimal getFirstNightTotal(Long masterReservationId) {
        // 1. 원본 1박 총액 우선 (업그레이드 전 요금)
        ReservationPayment payment = reservationPaymentRepository
                .findByMasterReservationId(masterReservationId).orElse(null);
        if (payment != null && payment.getOriginalFirstNightTotal() != null
                && payment.getOriginalFirstNightTotal().compareTo(BigDecimal.ZERO) > 0) {
            return payment.getOriginalFirstNightTotal();
        }

        // 2. 하위호환: DailyCharge에서 조회
        List<SubReservation> subs = subReservationRepository.findByMasterReservationId(masterReservationId);
        if (subs.isEmpty()) return BigDecimal.ZERO;

        SubReservation firstSub = subs.get(0);
        List<DailyCharge> charges = dailyChargeRepository.findBySubReservationId(firstSub.getId());
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

    /**
     * 특정 Leg(SubReservation)의 1박 총액 조회
     * Leg 단위 취소 수수료 기준: DailyCharge 1박 + 유료 업그레이드 차액(1박분)
     *
     * 정책 근거:
     * - 무료 업그레이드: DailyCharge만 (호텔 주도 → 고객 불이익 불가)
     * - 유료 업그레이드: DailyCharge + 업그레이드 차액/숙박일수 (고객 합의 총요금 기준)
     * - 참조: .planning/cancellation-policy.md
     */
    private BigDecimal getFirstNightTotalForSub(Long subReservationId) {
        // 1. DailyCharge 기반 1박 총액
        List<DailyCharge> charges = dailyChargeRepository.findBySubReservationId(subReservationId);
        if (charges.isEmpty()) return BigDecimal.ZERO;

        DailyCharge first = charges.get(0);
        BigDecimal firstNight;
        if (first.getTotal() != null) {
            firstNight = first.getTotal();
        } else {
            BigDecimal supply = first.getSupplyPrice() != null ? first.getSupplyPrice() : BigDecimal.ZERO;
            BigDecimal tax = first.getTax() != null ? first.getTax() : BigDecimal.ZERO;
            BigDecimal svc = first.getServiceCharge() != null ? first.getServiceCharge() : BigDecimal.ZERO;
            firstNight = supply.add(tax).add(svc);
        }

        // 2. 유료 업그레이드 차액의 1박분 추가
        //    업그레이드 차액(ReservationServiceItem)은 전체 숙박에 대한 총액이므로 숙박일수로 나눔
        int nights = charges.size();
        if (nights > 0) {
            List<ReservationServiceItem> services = serviceItemRepository.findBySubReservationId(subReservationId);
            // TC:1020(Room Upgrade) 차액 합산 — upgradeType=PAID인 항목만
            // 업그레이드 서비스는 transactionCodeId가 설정되어 있고 serviceOptionId가 null
            BigDecimal upgradeTotal = services.stream()
                    .filter(s -> "PAID".equals(s.getServiceType()) && s.getServiceOptionId() == null)
                    .map(ReservationServiceItem::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (upgradeTotal.compareTo(BigDecimal.ZERO) > 0) {
                // 업그레이드 차액의 1박분 = 총 차액 / 숙박일수
                BigDecimal upgradePerNight = upgradeTotal.divide(
                        BigDecimal.valueOf(nights), 0, java.math.RoundingMode.HALF_UP);
                firstNight = firstNight.add(upgradePerNight);
            }
        }

        return firstNight;
    }

    @Override
    @Transactional
    public void deleteReservation(Long id, Long propertyId) {
        // 슈퍼어드민 권한 검증
        if (!accessControlService.getCurrentUser().isSuperAdmin()) {
            throw new HolaException(ErrorCode.RESERVATION_DELETE_UNAUTHORIZED);
        }

        MasterReservation master = findMasterById(id, propertyId);

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
    @Transactional
    public void changeStatus(Long id, Long propertyId, ReservationStatusRequest request) {
        MasterReservation master = findMasterById(id, propertyId);
        String newStatus = request.getNewStatus();
        boolean statusChanged = false;

        if (request.getSubReservationId() != null) {
            // ── Leg 단위 상태 변경 ──
            SubReservation targetSub = findSubAndValidateOwnership(request.getSubReservationId(), master);
            String currentLegStatus = targetSub.getRoomReservationStatus();

            // Leg 상태 전이 검증
            Set<String> allowed = STATUS_TRANSITIONS.getOrDefault(currentLegStatus, Set.of());
            if (!allowed.contains(newStatus)) {
                throw new HolaException(ErrorCode.RESERVATION_STATUS_CHANGE_NOT_ALLOWED);
            }

            // 체크인 전제조건: 객실 배정 + 청소상태 확인 (RESERVED→INHOUSE 직행)
            if ("INHOUSE".equals(newStatus) && "RESERVED".equals(currentLegStatus)) {
                validateCheckInPrerequisites(targetSub);
            }

            // 체크아웃 전제조건: 마지막 활성 Leg인 경우에만 결제 잔액 검증
            if ("CHECKED_OUT".equals(newStatus) && isLastActiveLeg(targetSub, master)) {
                validateCheckOutBalance(master);
            }

            // 취소/노쇼 수수료 미결제 검증 (Leg 단위도 마스터 기준으로 수수료 산정)
            if ("CANCELED".equals(newStatus) || "NO_SHOW".equals(newStatus)) {
                // 노쇼: 체크인 날짜가 오늘 이후인 경우 처리 불가 (미도착이 확정되지 않음)
                if ("NO_SHOW".equals(newStatus) && master.getMasterCheckIn().isAfter(LocalDate.now())) {
                    throw new HolaException(ErrorCode.RESERVATION_STATUS_CHANGE_NOT_ALLOWED);
                }
                validateCancelFeePayment(master, propertyId, "NO_SHOW".equals(newStatus));

                // 미결제 잔액 검증: 개별 Leg 취소 시 해당 Leg의 결제 상태만 확인
                if ("CANCELED".equals(newStatus)) {
                    validateLegUnpaidBalance(master, targetSub);
                }
            }

            applyStatusChange(targetSub, newStatus);
            statusChanged = true;

            // 취소/노쇼 시 서비스 재고 복원
            if ("CANCELED".equals(newStatus) || "NO_SHOW".equals(newStatus)) {
                releaseServiceItemInventory(targetSub);
            }

            log.info("Leg 상태 변경: {} → {} (서브예약: {})", currentLegStatus, newStatus, targetSub.getSubReservationNo());

        } else {
            // ── 전체 Leg 일괄 변경 (하위 호환) ──
            // 노쇼/취소는 전체 적용
            if ("NO_SHOW".equals(newStatus) || "CANCELED".equals(newStatus)) {
                // 마스터 레벨 전이 검증
                Set<String> masterAllowed = STATUS_TRANSITIONS.getOrDefault(master.getReservationStatus(), Set.of());
                if (!masterAllowed.contains(newStatus)) {
                    throw new HolaException(ErrorCode.RESERVATION_STATUS_CHANGE_NOT_ALLOWED);
                }
                // 노쇼: 체크인 날짜가 오늘 이후인 경우 처리 불가 (미도착이 확정되지 않음)
                if ("NO_SHOW".equals(newStatus) && master.getMasterCheckIn().isAfter(LocalDate.now())) {
                    throw new HolaException(ErrorCode.RESERVATION_STATUS_CHANGE_NOT_ALLOWED);
                }
                // 취소/노쇼 수수료 미결제 검증
                validateCancelFeePayment(master, propertyId, "NO_SHOW".equals(newStatus));

                // 미결제 잔액 검증: grandTotal > totalPaid이면 취소 차단
                if ("CANCELED".equals(newStatus)) {
                    validateUnpaidBalance(master);
                }

                for (SubReservation sub : master.getSubReservations()) {
                    if (!"CANCELED".equals(sub.getRoomReservationStatus())
                            && !"NO_SHOW".equals(sub.getRoomReservationStatus())
                            && !"CHECKED_OUT".equals(sub.getRoomReservationStatus())) {
                        sub.updateStatus(newStatus);
                        // 취소/노쇼 시 서비스 재고 복원
                        releaseServiceItemInventory(sub);
                    }
                }
                statusChanged = true;
            } else {
                // 체크인/투숙중/체크아웃: 전이 가능한 Leg만 변경
                if ("INHOUSE".equals(newStatus)) {
                    // 전체 체크인: 모든 RESERVED Leg의 전제조건 검증
                    for (SubReservation sub : master.getSubReservations()) {
                        if ("RESERVED".equals(sub.getRoomReservationStatus())) {
                            validateCheckInPrerequisites(sub);
                        }
                    }
                }
                if ("CHECKED_OUT".equals(newStatus)) {
                    validateCheckOutBalance(master);
                }

                for (SubReservation sub : master.getSubReservations()) {
                    if ("CANCELED".equals(sub.getRoomReservationStatus())
                            || "NO_SHOW".equals(sub.getRoomReservationStatus())) continue;
                    Set<String> allowed = STATUS_TRANSITIONS.getOrDefault(sub.getRoomReservationStatus(), Set.of());
                    if (allowed.contains(newStatus)) {
                        applyStatusChange(sub, newStatus);
                        statusChanged = true;
                    }
                }
            }
        }

        if (!statusChanged) {
            throw new HolaException(ErrorCode.RESERVATION_STATUS_CHANGE_NOT_ALLOWED);
        }

        // 마스터 상태 자동 도출
        String derivedStatus = deriveMasterStatus(master.getSubReservations());
        String previousMasterStatus = master.getReservationStatus();
        master.updateStatus(derivedStatus);

        // 얼리/레이트 요금 발생 시 결제 재계산
        if ("INHOUSE".equals(newStatus) || "CHECKED_OUT".equals(newStatus)) {
            paymentService.recalculatePayment(master.getId());
        }

        // 노쇼 처리 시 수수료 계산 + REFUND 거래 기록
        if ("NO_SHOW".equals(newStatus)) {
            processNoShow(master, propertyId);
        }

        // 취소 처리 시 수수료 계산 + REFUND 거래 기록 (마스터가 CANCELED 로 확정된 경우만)
        // 부분 Leg 취소 시에는 derivedStatus가 RESERVED로 남아 있으므로 중복 호출 방지
        if ("CANCELED".equals(derivedStatus) && !"CANCELED".equals(previousMasterStatus)) {
            processCancel(master, propertyId);
        }

        // 부분 Leg 취소 시 해당 Leg 결제분 환불 (마스터는 CANCELED가 아닌 경우)
        if ("CANCELED".equals(newStatus) && !"CANCELED".equals(derivedStatus)) {
            // 취소 대상 Leg 특정
            SubReservation canceledTarget = null;
            if (request.getSubReservationId() != null) {
                canceledTarget = findSubAndValidateOwnership(request.getSubReservationId(), master);
            }
            processPartialLegCancelRefund(master, canceledTarget);
        }

        log.info("예약 상태 변경: {} → {} (마스터: {}, 예약번호: {})",
                previousMasterStatus, derivedStatus, newStatus, master.getMasterReservationNo());
    }

    /**
     * 개별 Leg 상태 변경 + 부수효과 (체크인/아웃 시 객실 상태 변경 등)
     */
    private void applyStatusChange(SubReservation sub, String newStatus) {
        sub.updateStatus(newStatus);
        LocalDateTime now = LocalDateTime.now();

        // INHOUSE: 체크인 부수효과 (RESERVED→INHOUSE 또는 CHECK_IN→INHOUSE)
        if ("INHOUSE".equals(newStatus) && sub.getActualCheckInTime() == null) {
            // Dayuse는 얼리 체크인 요금 해당 없음
            BigDecimal earlyFee = sub.isDayUse() ? BigDecimal.ZERO
                    : earlyLateCheckService.calculateEarlyCheckInFee(sub, now);
            sub.recordCheckIn(now, earlyFee);
            if (sub.getRoomNumberId() != null) {
                com.hola.hotel.entity.RoomNumber room = roomNumberRepository.findById(sub.getRoomNumberId()).orElse(null);
                if (room != null) room.checkIn();
            }
        }

        if ("CHECKED_OUT".equals(newStatus)) {
            // Dayuse는 레이트 체크아웃 요금 해당 없음
            BigDecimal lateFee = sub.isDayUse() ? BigDecimal.ZERO
                    : earlyLateCheckService.calculateLateCheckOutFee(sub, now);
            sub.recordCheckOut(now, lateFee);
            if (sub.getRoomNumberId() != null) {
                com.hola.hotel.entity.RoomNumber room = roomNumberRepository.findById(sub.getRoomNumberId()).orElse(null);
                if (room != null) {
                    room.checkOut();
                    // HK Task 자동 생성 (설정에서 autoCreateCheckout = true인 경우)
                    try {
                        housekeepingService.createTaskOnCheckout(
                                sub.getMasterReservation().getProperty().getId(),
                                sub.getRoomNumberId(),
                                sub.getMasterReservation().getId());
                    } catch (Exception e) {
                        log.warn("HK 자동 작업 생성 실패: roomId={}, {}", sub.getRoomNumberId(), e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * 마스터 상태 자동 도출 (Leg 상태 조합 기반)
     *
     * 규칙:
     * - 활성 Leg(CANCELED/NO_SHOW 제외) 중 가장 진행된 상태를 마스터 상태로 설정
     * - 전부 CHECKED_OUT → CHECKED_OUT
     * - 하나라도 INHOUSE → INHOUSE
     * - 전부 CHECKED_OUT → CHECKED_OUT
     * - 전부 CANCELED → CANCELED
     * - CHECK_IN은 하위 호환 처리 (INHOUSE와 동급)
     */
    private String deriveMasterStatus(List<SubReservation> subs) {
        List<String> activeStatuses = subs.stream()
                .filter(s -> !"CANCELED".equals(s.getRoomReservationStatus())
                          && !"NO_SHOW".equals(s.getRoomReservationStatus()))
                .map(SubReservation::getRoomReservationStatus)
                .toList();

        if (activeStatuses.isEmpty()) {
            boolean allCanceled = subs.stream()
                    .allMatch(s -> "CANCELED".equals(s.getRoomReservationStatus()));
            return allCanceled ? "CANCELED" : "NO_SHOW";
        }

        // CHECK_IN은 하위 호환 — INHOUSE와 동급 취급
        if (activeStatuses.contains("INHOUSE") || activeStatuses.contains("CHECK_IN")) return "INHOUSE";
        if (activeStatuses.stream().allMatch("CHECKED_OUT"::equals)) return "CHECKED_OUT";
        return "RESERVED";
    }

    /**
     * 체크인 전제조건 검증 (개별 Leg 단위)
     */
    private void validateCheckInPrerequisites(SubReservation sub) {
        if (sub.getRoomNumberId() == null) {
            throw new HolaException(ErrorCode.FD_ROOM_ASSIGN_REQUIRED);
        }
        com.hola.hotel.entity.RoomNumber room = roomNumberRepository.findById(sub.getRoomNumberId()).orElse(null);
        if (room != null) {
            String hkStatus = room.getHkStatus();
            if ("OOO".equals(hkStatus) || "OOS".equals(hkStatus)) {
                throw new HolaException(ErrorCode.FD_ROOM_OUT_OF_ORDER);
            }
            // 청소 미완료 객실 체크인 차단 (CLEAN 또는 INSPECTED만 허용)
            if ("DIRTY".equals(hkStatus) || "PICKUP".equals(hkStatus)) {
                throw new HolaException(ErrorCode.FD_ROOM_NOT_CLEAN);
            }
        }
    }

    /**
     * 체크아웃 잔액 검증
     */
    private void validateCheckOutBalance(MasterReservation master) {
        ReservationPayment payment = reservationPaymentRepository
                .findByMasterReservationId(master.getId()).orElse(null);
        if (payment != null) {
            BigDecimal grandTotal = payment.getGrandTotal() != null ? payment.getGrandTotal() : BigDecimal.ZERO;
            BigDecimal totalPaid = payment.getTotalPaidAmount() != null ? payment.getTotalPaidAmount() : BigDecimal.ZERO;
            BigDecimal remaining = grandTotal.subtract(totalPaid);
            if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                throw new HolaException(ErrorCode.CHECKOUT_OUTSTANDING_BALANCE);
            }
        } else {
            // 결제 레코드가 없더라도 DailyCharge 합계가 있으면 미결제 상태로 판단
            BigDecimal totalCharge = master.getSubReservations().stream()
                    .flatMap(sub -> dailyChargeRepository.findBySubReservationId(sub.getId()).stream())
                    .map(dc -> dc.getTotal() != null ? dc.getTotal() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (totalCharge.compareTo(BigDecimal.ZERO) > 0) {
                throw new HolaException(ErrorCode.CHECKOUT_OUTSTANDING_BALANCE);
            }
        }
    }

    /**
     * 취소/노쇼 수수료 미결제 검증
     * 정책에 의한 수수료가 기결제 금액보다 크면 상태 변경 차단
     */
    private void validateCancelFeePayment(MasterReservation master, Long propertyId, boolean isNoShow) {
        BigDecimal firstNightSupply = getFirstNightTotal(master.getId());
        var cancelResult = cancellationPolicyService.calculateCancelFee(
                propertyId, master.getMasterCheckIn(), firstNightSupply, isNoShow);
        BigDecimal cancelFee = cancelResult.feeAmount();
        if (cancelFee.compareTo(BigDecimal.ZERO) <= 0) return;

        ReservationPayment payment = reservationPaymentRepository
                .findByMasterReservationId(master.getId()).orElse(null);
        BigDecimal totalPaid = BigDecimal.ZERO;
        if (payment != null && payment.getTotalPaidAmount() != null) {
            totalPaid = payment.getTotalPaidAmount();
        }
        if (totalPaid.compareTo(cancelFee) < 0) {
            throw new HolaException(ErrorCode.CANCEL_FEE_UNPAID);
        }
    }

    /**
     * 미결제 잔액 검증 (Master 전체) — grandTotal > 순결제액이면 취소 차단
     * 전체 취소(cancel(), 전체 상태변경) 경로에서 사용
     */
    private void validateUnpaidBalance(MasterReservation master) {
        ReservationPayment payment = reservationPaymentRepository
                .findByMasterReservationId(master.getId()).orElse(null);
        if (payment == null) return;

        BigDecimal grandTotal = payment.getGrandTotal() != null ? payment.getGrandTotal() : BigDecimal.ZERO;
        BigDecimal totalPaid = payment.getTotalPaidAmount() != null ? payment.getTotalPaidAmount() : BigDecimal.ZERO;
        BigDecimal refund = payment.getRefundAmount() != null ? payment.getRefundAmount() : BigDecimal.ZERO;
        BigDecimal cancelFee = payment.getCancelFeeAmount() != null ? payment.getCancelFeeAmount() : BigDecimal.ZERO;
        BigDecimal netPaid = totalPaid.subtract(refund).subtract(cancelFee);

        if (grandTotal.compareTo(netPaid) > 0) {
            BigDecimal unpaid = grandTotal.subtract(netPaid);
            throw new HolaException(ErrorCode.CANCEL_UNPAID_BALANCE,
                    "미결제 잔액 " + unpaid.setScale(0, java.math.RoundingMode.DOWN) + "원을 먼저 결제해주세요");
        }
    }

    /**
     * Leg별 미결제 잔액 검증 — 해당 Leg의 요금 vs 해당 Leg에 귀속된 결제를 비교
     * 개별 Leg 취소 경로에서 사용 (다른 Leg 미결제로 차단하지 않음)
     */
    private void validateLegUnpaidBalance(MasterReservation master, SubReservation targetSub) {
        List<LegPaymentInfo> legPayments = paymentService.calculatePerLegPayments(master.getId());
        LegPaymentInfo legInfo = legPayments.stream()
                .filter(lp -> lp.getSubReservationId().equals(targetSub.getId()))
                .findFirst().orElse(null);
        if (legInfo == null) return;

        if (legInfo.getLegRemaining().compareTo(BigDecimal.ZERO) > 0) {
            throw new HolaException(ErrorCode.CANCEL_UNPAID_BALANCE,
                    "Leg #" + getLegIndex(master, targetSub) + " 미결제 잔액 "
                    + legInfo.getLegRemaining().setScale(0, java.math.RoundingMode.DOWN) + "원을 먼저 결제해주세요");
        }
    }

    /**
     * 마지막 활성 Leg 여부 판별 (체크아웃 잔액 검증 필요 여부)
     */
    private boolean isLastActiveLeg(SubReservation targetSub, MasterReservation master) {
        return master.getSubReservations().stream()
                .filter(s -> !s.getId().equals(targetSub.getId()))
                .filter(s -> !"CANCELED".equals(s.getRoomReservationStatus())
                          && !"CHECKED_OUT".equals(s.getRoomReservationStatus())
                          && !"NO_SHOW".equals(s.getRoomReservationStatus()))
                .findAny().isEmpty();
    }

    /**
     * 서비스 항목 재고 복원 (PAID 서비스 중 inventoryItemId가 있는 항목)
     */
    private void releaseServiceItemInventory(SubReservation sub) {
        for (var svcItem : sub.getServices()) {
            if ("PAID".equals(svcItem.getServiceType()) && svcItem.getServiceOptionId() != null) {
                paidServiceOptionRepository.findById(svcItem.getServiceOptionId())
                        .filter(opt -> opt.getInventoryItemId() != null)
                        .ifPresent(opt -> inventoryService.releaseInventory(
                                opt.getInventoryItemId(), sub.getCheckIn(), sub.getCheckOut(),
                                svcItem.getQuantity()));
            }
        }
    }

    /**
     * 취소 처리 — 수수료 계산 + 환불 거래 기록
     * changeStatus() 경로에서 마스터가 CANCELED 로 확정되는 시점에 호출.
     * cancel() 직접 호출 경로는 이미 처리되므로 이 메서드는 changeStatus 전용.
     */
    private void processCancel(MasterReservation master, Long propertyId) {
        // Leg 단위 환불: 아직 환불되지 않은 Leg 각각에 대해 개별 처리
        // (이미 부분 취소로 환불된 Leg는 기존 환불 거래가 있으므로 스킵됨)
        List<SubReservation> canceledLegs = master.getSubReservations().stream()
                .filter(s -> "CANCELED".equals(s.getRoomReservationStatus()))
                .toList();

        for (SubReservation leg : canceledLegs) {
            Long subId = leg.getId();

            // 이미 환불 거래가 있는 Leg는 건너뜀 (부분 취소로 이미 처리됨)
            boolean alreadyRefunded = paymentTransactionRepository
                    .findBySubReservationIdOrderByTransactionSeqAsc(subId).stream()
                    .anyMatch(t -> "REFUND".equals(t.getTransactionType()));
            if (alreadyRefunded) continue;

            processPartialLegCancelRefund(master, leg);
        }

        // 최종 결제 상태 갱신
        ReservationPayment payment = reservationPaymentRepository
                .findByMasterReservationId(master.getId()).orElse(null);
        if (payment != null) {
            payment.setPaymentStatusRefunded();
        }

        log.info("취소 처리(changeStatus 경로): {}, OTA={}, legs={}",
                master.getMasterReservationNo(), master.getIsOtaManaged(), canceledLegs.size());
    }

    /**
     * 노쇼 처리 — 수수료 계산 + 환불 거래 기록
     */
    private void processNoShow(MasterReservation master, Long propertyId) {
        // Leg 단위 노쇼 환불: 각 Leg별 결제수단에 맞게 개별 처리
        List<SubReservation> noShowLegs = master.getSubReservations().stream()
                .filter(s -> "NO_SHOW".equals(s.getRoomReservationStatus()))
                .toList();

        for (SubReservation leg : noShowLegs) {
            Long subId = leg.getId();

            // 이미 환불 거래가 있는 Leg는 건너뜀
            boolean alreadyRefunded = paymentTransactionRepository
                    .findBySubReservationIdOrderByTransactionSeqAsc(subId).stream()
                    .anyMatch(t -> "REFUND".equals(t.getTransactionType()));
            if (alreadyRefunded) continue;

            // Leg별 노쇼 수수료 계산
            BigDecimal firstNightForSub = getFirstNightTotalForSub(subId);
            var cancelResult = cancellationPolicyService.calculateCancelFee(
                    propertyId, leg.getCheckIn(), firstNightForSub, true);

            // Leg에 귀속된 결제액
            BigDecimal legPaid = paymentTransactionRepository
                    .findBySubReservationIdOrderByTransactionSeqAsc(subId).stream()
                    .filter(t -> "PAYMENT".equals(t.getTransactionType()))
                    .map(PaymentTransaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal cancelFee = cancelResult.feeAmount().min(legPaid);
            BigDecimal refundAmt = legPaid.subtract(cancelFee).max(BigDecimal.ZERO);

            ReservationPayment payment = reservationPaymentRepository
                    .findByMasterReservationId(master.getId()).orElse(null);
            if (payment != null) {
                payment.updateCancelRefund(cancelFee, refundAmt);
            }

            // RoomType 라벨
            String roomTypeLabel = "객실";
            if (leg.getRoomTypeId() != null) {
                roomTypeLabel = roomTypeRepository.findById(leg.getRoomTypeId())
                        .map(RoomType::getRoomTypeCode).orElse("객실");
            }
            String legLabel = "Leg #" + getLegIndex(master, leg) + " - " + roomTypeLabel;

            if (refundAmt.compareTo(BigDecimal.ZERO) > 0 || cancelFee.compareTo(BigDecimal.ZERO) > 0) {
                String memo = cancelResult.policyDescription() + " / " + legLabel + " 노쇼 환불";
                if (cancelFee.compareTo(BigDecimal.ZERO) > 0) {
                    memo += " (수수료: " + cancelFee.setScale(0, java.math.RoundingMode.DOWN) + "원)";
                }
                if (Boolean.TRUE.equals(master.getIsOtaManaged())) {
                    memo += " [OTA 예약 — PG 환불은 OTA 채널에서 처리 필요]";
                }
                paymentService.processRefundForLeg(master.getId(), subId,
                        Boolean.TRUE.equals(master.getIsOtaManaged()) ? BigDecimal.ZERO : refundAmt,
                        cancelFee, memo);
            }
        }

        // 최종 결제 상태 갱신
        ReservationPayment payment = reservationPaymentRepository
                .findByMasterReservationId(master.getId()).orElse(null);
        if (payment != null) {
            payment.setPaymentStatusRefunded();
        }

        log.info("노쇼 처리: {}, OTA={}, legs={}",
                master.getMasterReservationNo(), master.getIsOtaManaged(), noShowLegs.size());
    }

    /**
     * 부분 Leg 취소 시 해당 Leg에 귀속된 결제분 환불 처리
     * 취소된 Leg의 결제 거래만 대상으로 환불 (PG 자동, 비PG MANUAL_CONFIRMED)
     *
     * @param canceledTarget 취소 대상 Leg (null이면 전체 일괄 취소 경로에서 호출)
     */
    private void processPartialLegCancelRefund(MasterReservation master, SubReservation canceledTarget) {
        // grandTotal 재계산 (취소된 Leg 제외)
        paymentService.recalculatePayment(master.getId());

        // 취소 대상 Leg 특정 (명시적으로 전달받거나, 전체 취소 경로인 경우 방금 취소된 Leg 검색)
        SubReservation canceledLeg = canceledTarget;
        if (canceledLeg == null) {
            // 전체 일괄 취소 경로: 가장 마지막에 취소된 Leg (방어 코드)
            canceledLeg = master.getSubReservations().stream()
                    .filter(s -> "CANCELED".equals(s.getRoomReservationStatus()))
                    .reduce((first, second) -> second)
                    .orElse(null);
        }

        if (canceledLeg == null) return;

        Long subId = canceledLeg.getId();
        Long propertyId = master.getProperty().getId();

        // 해당 Leg에 귀속된 결제 거래 조회
        List<PaymentTransaction> legPaymentTxns = paymentTransactionRepository
                .findBySubReservationIdOrderByTransactionSeqAsc(subId).stream()
                .filter(t -> "PAYMENT".equals(t.getTransactionType()))
                .toList();

        // 해당 Leg에 귀속된 기존 환불 누적액
        BigDecimal alreadyRefunded = paymentTransactionRepository
                .findBySubReservationIdOrderByTransactionSeqAsc(subId).stream()
                .filter(t -> "REFUND".equals(t.getTransactionType()))
                .map(PaymentTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal legPaid = legPaymentTxns.stream()
                .map(PaymentTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 환불 가능액 = Leg 결제 - 기환불
        BigDecimal refundable = legPaid.subtract(alreadyRefunded).max(BigDecimal.ZERO);

        if (refundable.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("부분 Leg 취소: {}의 환불 대상 결제건 없음", canceledLeg.getSubReservationNo());
            return;
        }

        // Leg 단위 취소 수수료 계산
        BigDecimal firstNightForSub = getFirstNightTotalForSub(subId);
        var cancelResult = cancellationPolicyService.calculateCancelFee(
                propertyId, canceledLeg.getCheckIn(), firstNightForSub);
        BigDecimal cancelFee = cancelResult.feeAmount().min(refundable); // 수수료가 환불액 초과하지 않도록
        BigDecimal refundAmt = refundable.subtract(cancelFee).max(BigDecimal.ZERO);

        // RoomType 코드 조회 (메모용)
        String roomTypeLabel = "객실";
        if (canceledLeg.getRoomTypeId() != null) {
            roomTypeLabel = roomTypeRepository.findById(canceledLeg.getRoomTypeId())
                    .map(RoomType::getRoomTypeCode)
                    .orElse("객실");
        }
        String legLabel = "Leg #" + getLegIndex(master, canceledLeg) + " - " + roomTypeLabel;

        // ReservationPayment에 취소 수수료 + 환불 금액 누적
        ReservationPayment payment = reservationPaymentRepository
                .findByMasterReservationId(master.getId()).orElse(null);
        if (payment != null) {
            payment.updateCancelRefund(cancelFee, refundAmt);
        }

        // Leg 단위 환불 거래 기록
        if (refundAmt.compareTo(BigDecimal.ZERO) > 0 || cancelFee.compareTo(BigDecimal.ZERO) > 0) {
            String memo = legLabel + " 취소 환불";
            if (cancelFee.compareTo(BigDecimal.ZERO) > 0) {
                memo += " (수수료: " + cancelFee.setScale(0, java.math.RoundingMode.DOWN) + "원)";
            }
            if (Boolean.TRUE.equals(master.getIsOtaManaged())) {
                memo += " [OTA 예약 — PG 환불은 OTA 채널에서 처리 필요]";
            }
            paymentService.processRefundForLeg(master.getId(), subId,
                    Boolean.TRUE.equals(master.getIsOtaManaged()) ? BigDecimal.ZERO : refundAmt,
                    cancelFee, memo);
        }

        if (payment != null) {
            payment.updatePaymentStatus();
        }

        log.info("부분 Leg 취소 환불: {}, Leg={}, 환불={}원, 수수료={}원",
                master.getMasterReservationNo(), canceledLeg.getSubReservationNo(), refundAmt, cancelFee);
    }

    /**
     * Leg 순번 조회 (1-based)
     */
    private int getLegIndex(MasterReservation master, SubReservation target) {
        List<SubReservation> subs = master.getSubReservations();
        for (int i = 0; i < subs.size(); i++) {
            if (subs.get(i).getId().equals(target.getId())) return i + 1;
        }
        return 0;
    }

    @Override
    @Transactional
    public SubReservationResponse addLeg(Long reservationId, Long propertyId, SubReservationRequest request) {
        MasterReservation master = findMasterById(reservationId, propertyId);

        // 수정 불가 상태 검사
        validateModifiable(master);

        // OTA 예약은 객실 추가 불가 (OTA 시스템과 불일치 방지)
        if (Boolean.TRUE.equals(master.getIsOtaManaged())) {
            throw new HolaException(ErrorCode.RESERVATION_OTA_EDIT_RESTRICTED);
        }

        Property property = master.getProperty();

        int legSeq = subReservationRepository.countAllIncludingDeleted(master.getId()) + 1;
        SubReservation sub = createSubReservation(master, request, legSeq, property);

        // 새 Leg를 마스터의 컬렉션에 추가 (syncMasterDates가 새 Leg 포함하여 계산하도록)
        master.getSubReservations().add(sub);

        // 마스터 체크인/체크아웃 자동 동기화
        syncMasterDates(master);

        // 결제 금액 재계산
        paymentService.recalculatePayment(master.getId());

        return reservationMapper.toSubReservationResponse(sub);
    }

    @Override
    @Transactional
    public SubReservationResponse updateLeg(Long reservationId, Long propertyId, Long legId, SubReservationRequest request) {
        MasterReservation master = findMasterById(reservationId, propertyId);

        // 수정 불가 상태 검사
        validateModifiable(master);

        // OTA 예약은 객실 수정 불가 (OTA 시스템과 불일치 방지)
        if (Boolean.TRUE.equals(master.getIsOtaManaged())) {
            throw new HolaException(ErrorCode.RESERVATION_OTA_EDIT_RESTRICTED);
        }

        // 서브예약 소속 마스터 검증
        SubReservation sub = findSubAndValidateOwnership(legId, master);

        validateDates(request.getCheckIn(), request.getCheckOut());

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

        // L1 객실 충돌 검사 (자기 자신 제외, Dayuse 시간 슬롯 인식)
        if (request.getRoomNumberId() != null) {
            if (availabilityService.hasRoomConflict(request.getRoomNumberId(),
                    request.getCheckIn(), request.getCheckOut(), legId,
                    sub.getStayType(), sub.getDayUseTimeSlot())) {
                throw new HolaException(ErrorCode.SUB_RESERVATION_ROOM_CONFLICT);
            }
        }

        // OOO/OOS 기간 체크
        if (request.getRoomNumberId() != null) {
            List<RoomUnavailable> unavailable = roomUnavailableRepository.findOverlapping(
                    request.getRoomNumberId(), request.getCheckIn(), request.getCheckOut());
            if (!unavailable.isEmpty()) {
                throw new HolaException(ErrorCode.ROOM_UNAVAILABLE_FOR_RESERVATION);
            }
        }

        // Dayuse인 경우 checkOut = checkIn + 1 자동 보정
        LocalDate effectiveCheckOut = request.getCheckOut();
        if (sub.isDayUse() && request.getCheckIn() != null) {
            effectiveCheckOut = request.getCheckIn().plusDays(1);
        }

        sub.update(request.getRoomTypeId(), request.getFloorId(), request.getRoomNumberId(),
                request.getAdults() != null ? request.getAdults() : 1,
                request.getChildren() != null ? request.getChildren() : 0,
                request.getCheckIn(), effectiveCheckOut,
                request.getEarlyCheckIn() != null ? request.getEarlyCheckIn() : false,
                request.getLateCheckOut() != null ? request.getLateCheckOut() : false);

        // 투숙객 갱신
        updateGuests(sub, request.getGuests());

        // 일별 요금 재계산
        recalculateDailyCharges(sub, sub.getMasterReservation().getProperty());

        // 마스터 체크인/체크아웃 동기화
        syncMasterDates(sub.getMasterReservation());

        // 결제 금액 재계산 (객실타입/날짜 변경 시 grandTotal 갱신 + paymentStatus 재판단)
        paymentService.recalculatePayment(master.getId());

        log.info("서브 예약 수정: {}", sub.getSubReservationNo());
        return reservationMapper.toSubReservationResponse(sub);
    }

    @Override
    @Transactional
    public void deleteLeg(Long reservationId, Long propertyId, Long legId) {
        MasterReservation master = findMasterById(reservationId, propertyId);

        // 수정 불가 상태 검사
        validateModifiable(master);

        // OTA 예약은 객실 삭제 불가 (OTA 시스템과 불일치 방지)
        if (Boolean.TRUE.equals(master.getIsOtaManaged())) {
            throw new HolaException(ErrorCode.RESERVATION_OTA_EDIT_RESTRICTED);
        }

        // 서브예약 소속 마스터 검증
        SubReservation sub = findSubAndValidateOwnership(legId, master);

        // RESERVED 상태만 개별 삭제 가능
        if (!"RESERVED".equals(sub.getRoomReservationStatus())) {
            throw new HolaException(ErrorCode.RESERVATION_STATUS_CHANGE_NOT_ALLOWED);
        }

        // 마지막 활성 Leg는 삭제 불가 (최소 1개 필수)
        long activeCount = master.getSubReservations().stream()
                .filter(s -> !"CANCELED".equals(s.getRoomReservationStatus()))
                .count();
        if (activeCount <= 1) {
            throw new HolaException(ErrorCode.SUB_RESERVATION_LAST_LEG);
        }

        sub.updateStatus("CANCELED");
        sub.softDelete();

        syncMasterDates(master);

        // 결제 금액 재계산
        paymentService.recalculatePayment(master.getId());

        log.info("서브 예약 삭제(취소): {}", sub.getSubReservationNo());
    }

    @Override
    public int checkAvailability(Long propertyId, Long roomTypeId, LocalDate checkIn, LocalDate checkOut) {
        return availabilityService.getAvailableRoomCount(roomTypeId, checkIn, checkOut);
    }

    @Override
    public List<ReservationMemoResponse> getMemos(Long reservationId, Long propertyId) {
        findMasterById(reservationId, propertyId);
        return reservationMemoRepository.findByMasterReservationIdOrderByCreatedAtDesc(reservationId)
                .stream()
                .map(reservationMapper::toReservationMemoResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ReservationMemoResponse addMemo(Long reservationId, Long propertyId, String content) {
        findMasterById(reservationId, propertyId);
        ReservationMemo memo = ReservationMemo.builder()
                .masterReservationId(reservationId)
                .content(content)
                .build();
        memo = reservationMemoRepository.save(memo);
        log.info("예약 메모 등록: reservationId={}", reservationId);
        return reservationMapper.toReservationMemoResponse(memo);
    }

    @Override
    @Transactional
    public ReservationDepositResponse addDeposit(Long reservationId, Long propertyId, ReservationDepositRequest request) {
        MasterReservation master = findMasterById(reservationId, propertyId);
        ReservationDeposit deposit = ReservationDeposit.builder()
                .masterReservation(master)
                .depositMethod(request.getDepositMethod())
                .cardCompany(request.getCardCompany())
                .cardNumberEncrypted(request.getCardNumberEncrypted())
                .cardCvcEncrypted(request.getCardCvcEncrypted())
                .cardExpiryDate(request.getCardExpiryDate())
                .cardPasswordEncrypted(request.getCardPasswordEncrypted())
                .currency(request.getCurrency() != null ? request.getCurrency() : "KRW")
                .amount(request.getAmount())
                .build();
        deposit = reservationDepositRepository.save(deposit);
        log.info("예치금 등록: reservationId={}, 금액={}", reservationId, request.getAmount());
        return reservationMapper.toReservationDepositResponse(deposit);
    }

    @Override
    @Transactional
    public ReservationDepositResponse updateDeposit(Long reservationId, Long propertyId, Long depositId, ReservationDepositRequest request) {
        findMasterById(reservationId, propertyId);
        ReservationDeposit deposit = reservationDepositRepository.findById(depositId)
                .orElseThrow(() -> new HolaException(ErrorCode.DEPOSIT_NOT_FOUND));
        // 예치금이 해당 예약에 속하는지 검증
        if (!deposit.getMasterReservation().getId().equals(reservationId)) {
            throw new HolaException(ErrorCode.DEPOSIT_NOT_FOUND);
        }
        deposit.update(request.getDepositMethod(), request.getCardCompany(),
                request.getCardNumberEncrypted(), request.getCardCvcEncrypted(),
                request.getCardExpiryDate(), request.getCardPasswordEncrypted(),
                request.getCurrency(), request.getAmount());
        log.info("예치금 수정: depositId={}", depositId);
        return reservationMapper.toReservationDepositResponse(deposit);
    }

    // ─── 내부 헬퍼 메서드 ──────────────────────────

    /**
     * 마스터 예약 조회 + 프로퍼티 소속 검증
     */
    private MasterReservation findMasterById(Long id, Long propertyId) {
        MasterReservation master = masterReservationRepository.findById(id)
                .orElseThrow(() -> new HolaException(ErrorCode.RESERVATION_NOT_FOUND));
        if (!master.getProperty().getId().equals(propertyId)) {
            throw new HolaException(ErrorCode.RESERVATION_NOT_FOUND);
        }
        return master;
    }

    /**
     * 서브예약 조회 + 마스터 소속 검증
     */
    private SubReservation findSubAndValidateOwnership(Long subId, MasterReservation master) {
        SubReservation sub = subReservationRepository.findById(subId)
                .orElseThrow(() -> new HolaException(ErrorCode.SUB_RESERVATION_NOT_FOUND));
        if (!sub.getMasterReservation().getId().equals(master.getId())) {
            throw new HolaException(ErrorCode.SUB_RESERVATION_MASTER_MISMATCH);
        }
        return sub;
    }

    /**
     * 수정 불가 상태 검증 (CHECKED_OUT, CANCELED, NO_SHOW)
     */
    private void validateModifiable(MasterReservation master) {
        if (IMMUTABLE_STATUSES.contains(master.getReservationStatus())) {
            throw new HolaException(ErrorCode.RESERVATION_MODIFY_NOT_ALLOWED);
        }
    }

    private void validateDates(LocalDate checkIn, LocalDate checkOut) {
        validateDates(checkIn, checkOut, false);
    }

    /**
     * 날짜 유효성 검증 (dayuse 허용 시 같은 날짜 OK)
     */
    private void validateDates(LocalDate checkIn, LocalDate checkOut, boolean allowSameDay) {
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
     * 레이트코드 판매기간 + 숙박일수 검증
     */
    private void validateRateCode(Long rateCodeId, LocalDate checkIn, LocalDate checkOut) {
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
     * 서브 예약 생성 + 투숙객 + 일별요금
     */
    private SubReservation createSubReservation(MasterReservation master, SubReservationRequest request,
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

        // 서브 예약 생성 — 마스터 상태가 이미 진행 중이면 새 Leg도 동일 상태로 시작
        String masterStatus = master.getReservationStatus();
        String legStatus = "RESERVED";
        if ("CHECK_IN".equals(masterStatus) || "INHOUSE".equals(masterStatus)) {
            legStatus = masterStatus;
        }

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
     * 투숙객 목록 갱신 (삭제 후 재등록)
     */
    private void updateGuests(SubReservation sub, List<ReservationGuestRequest> guestRequests) {
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
    private void recalculateDailyCharges(SubReservation sub, Property property) {
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
     * 마스터 체크인/체크아웃 동기화 (서브 중 최초 체크인, 최종 체크아웃)
     */
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
     * 상태 필터
     */
    private boolean filterByStatus(MasterReservation r, String status) {
        if (status == null || status.isBlank()) return true;
        return status.equals(r.getReservationStatus());
    }

    /**
     * 날짜 범위 필터 (체크인일 기준)
     */
    private boolean filterByDateRange(MasterReservation r, LocalDate checkInFrom, LocalDate checkInTo) {
        if (checkInFrom != null && r.getMasterCheckIn().isBefore(checkInFrom)) return false;
        if (checkInTo != null && r.getMasterCheckIn().isAfter(checkInTo)) return false;
        return true;
    }

    /**
     * 키워드 필터 (예약번호, 예약자명, 전화번호)
     */
    private boolean filterByKeyword(MasterReservation r, String keyword) {
        if (keyword == null || keyword.isBlank()) return true;
        String kw = keyword.toLowerCase();
        return (r.getMasterReservationNo() != null && r.getMasterReservationNo().toLowerCase().contains(kw))
                || (r.getGuestNameKo() != null && r.getGuestNameKo().contains(kw))
                || (r.getPhoneNumber() != null && r.getPhoneNumber().contains(kw))
                || (r.getConfirmationNo() != null && r.getConfirmationNo().toLowerCase().contains(kw));
    }

    // ─── 유료 서비스 추가/삭제 ──────────────────────────

    @Override
    @Transactional
    public ReservationServiceResponse addService(Long masterReservationId, Long subReservationId,
                                                  Long propertyId, ReservationServiceRequest request) {
        MasterReservation master = findMasterById(masterReservationId, propertyId);
        validateModifiable(master);

        SubReservation sub = findSubAndValidateOwnership(subReservationId, master);

        // 서비스 일자가 체크인~체크아웃 전일 범위 내인지 검증 (체크아웃 당일은 객실 사용일이 아님)
        if (request.getServiceDate() != null) {
            if (request.getServiceDate().isBefore(sub.getCheckIn())
                    || !request.getServiceDate().isBefore(sub.getCheckOut())) {
                throw new HolaException(ErrorCode.SERVICE_DATE_OUT_OF_RANGE);
            }
        }

        PaidServiceOption option = paidServiceOptionRepository.findById(request.getServiceOptionId())
                .orElseThrow(() -> new HolaException(ErrorCode.PAID_SERVICE_OPTION_NOT_FOUND));

        int qty = request.getQuantity() != null ? request.getQuantity() : 1;
        BigDecimal unitPrice = option.getVatIncludedPrice();
        BigDecimal tax = option.getTaxAmount().multiply(BigDecimal.valueOf(qty));
        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(qty));

        ReservationServiceItem serviceItem = ReservationServiceItem.builder()
                .subReservation(sub)
                .serviceType("PAID")
                .serviceOptionId(option.getId())
                .transactionCodeId(option.getTransactionCodeId())
                .serviceDate(request.getServiceDate())
                .quantity(qty)
                .unitPrice(unitPrice)
                .tax(tax)
                .totalPrice(totalPrice)
                .build();

        serviceItem = serviceItemRepository.save(serviceItem);

        // 결제 금액 재계산
        paymentService.recalculatePayment(master.getId());

        log.info("유료 서비스 추가: reservationId={}, legId={}, serviceOptionId={}", masterReservationId, subReservationId, option.getId());

        return ReservationServiceResponse.builder()
                .id(serviceItem.getId())
                .serviceType(serviceItem.getServiceType())
                .serviceOptionId(serviceItem.getServiceOptionId())
                .serviceName(option.getServiceNameKo())
                .serviceDate(serviceItem.getServiceDate())
                .quantity(serviceItem.getQuantity())
                .unitPrice(serviceItem.getUnitPrice())
                .tax(serviceItem.getTax())
                .totalPrice(serviceItem.getTotalPrice())
                .build();
    }

    @Override
    @Transactional
    public void removeService(Long masterReservationId, Long subReservationId, Long serviceId, Long propertyId) {
        MasterReservation master = findMasterById(masterReservationId, propertyId);
        validateModifiable(master);

        ReservationServiceItem serviceItem = serviceItemRepository.findById(serviceId)
                .orElseThrow(() -> new HolaException(ErrorCode.RESERVATION_SERVICE_NOT_FOUND));

        // 서브예약 소속 검증
        if (!serviceItem.getSubReservation().getId().equals(subReservationId)) {
            throw new HolaException(ErrorCode.RESERVATION_SERVICE_MISMATCH);
        }

        serviceItemRepository.deleteById(serviceId);

        // 결제 금액 재계산
        paymentService.recalculatePayment(master.getId());

        log.info("유료 서비스 삭제: reservationId={}, legId={}, serviceId={}", masterReservationId, subReservationId, serviceId);
    }

    // ─── 캘린더뷰 ──────────────────────────

    @Override
    public Map<String, List<ReservationCalendarResponse>> getCalendarData(
            Long propertyId, LocalDate startDate, LocalDate endDate,
            String status, String keyword) {

        List<MasterReservation> reservations = masterReservationRepository
                .findByPropertyIdAndDateRange(propertyId, startDate, endDate);

        // Java 필터링 (상태, 키워드)
        List<MasterReservation> filtered = reservations.stream()
                .filter(r -> status == null || status.isEmpty() || status.equals(r.getReservationStatus()))
                .filter(r -> filterByKeyword(r, keyword))
                .collect(Collectors.toList());

        // Floor/RoomNumber/RoomType ID 수집 → 벌크 조회 (N+1 방지)
        Set<Long> floorIds = new HashSet<>();
        Set<Long> roomNumberIds = new HashSet<>();
        Set<Long> roomTypeIds = new HashSet<>();

        for (MasterReservation m : filtered) {
            for (SubReservation sub : m.getSubReservations()) {
                if (sub.getFloorId() != null) floorIds.add(sub.getFloorId());
                if (sub.getRoomNumberId() != null) roomNumberIds.add(sub.getRoomNumberId());
                if (sub.getRoomTypeId() != null) roomTypeIds.add(sub.getRoomTypeId());
            }
        }

        Map<Long, String> floorMap = floorIds.isEmpty() ? Map.of()
                : floorRepository.findAllById(floorIds).stream()
                    .collect(Collectors.toMap(Floor::getId, Floor::getFloorNumber));

        Map<Long, String> roomNumberMap = roomNumberIds.isEmpty() ? Map.of()
                : roomNumberRepository.findAllById(roomNumberIds).stream()
                    .collect(Collectors.toMap(RoomNumber::getId, RoomNumber::getRoomNumber));

        Map<Long, String> roomTypeMap = roomTypeIds.isEmpty() ? Map.of()
                : roomTypeRepository.findAllById(roomTypeIds).stream()
                    .collect(Collectors.toMap(RoomType::getId, RoomType::getRoomTypeCode));

        // 날짜별 그룹핑
        Map<String, List<ReservationCalendarResponse>> result = new LinkedHashMap<>();

        for (MasterReservation m : filtered) {
            ReservationCalendarResponse dto = toCalendarResponse(m, floorMap, roomNumberMap, roomTypeMap);

            // Dayuse: 체크인 당일만 매핑 (체크아웃일은 인벤토리용 +1일)
            LocalDate effectiveCheckOut = "DAY_USE".equals(dto.getStayType())
                    ? m.getMasterCheckIn() : m.getMasterCheckOut();

            // 체류 기간 내 각 날짜에 매핑
            LocalDate cursor = m.getMasterCheckIn().isBefore(startDate) ? startDate : m.getMasterCheckIn();
            LocalDate until = effectiveCheckOut.isAfter(endDate) ? endDate : effectiveCheckOut;

            // 체크아웃일 포함 (Gantt 바가 체크아웃일까지 표시)
            while (!cursor.isAfter(until)) {
                String dateKey = cursor.toString();
                result.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(dto);
                cursor = cursor.plusDays(1);
            }
        }

        return result;
    }

    /**
     * 캘린더뷰용 DTO 변환 (이름 마스킹 + 층/호수 조합)
     */
    private ReservationCalendarResponse toCalendarResponse(
            MasterReservation master,
            Map<Long, String> floorMap,
            Map<Long, String> roomNumberMap,
            Map<Long, String> roomTypeMap) {

        // 첫 번째 활성 서브 예약 기준 객실/숙박유형 정보
        String roomInfo = null;
        String roomTypeName = null;
        String stayType = null;
        List<SubReservation> subs = master.getSubReservations();
        if (subs != null && !subs.isEmpty()) {
            SubReservation firstSub = subs.stream()
                    .filter(s -> !"CANCELED".equals(s.getRoomReservationStatus()))
                    .findFirst()
                    .orElse(subs.get(0));

            // 층+호수: 12F-1201
            String floor = firstSub.getFloorId() != null ? floorMap.get(firstSub.getFloorId()) : null;
            String room = firstSub.getRoomNumberId() != null ? roomNumberMap.get(firstSub.getRoomNumberId()) : null;
            if (floor != null && room != null) {
                roomInfo = floor + "-" + room;
            } else if (room != null) {
                roomInfo = room;
            }

            roomTypeName = firstSub.getRoomTypeId() != null ? roomTypeMap.get(firstSub.getRoomTypeId()) : null;
            if (firstSub.getStayType() != null) {
                stayType = firstSub.getStayType().name();
            }
        }

        // 이름: 국문 우선, 없으면 영문 폴백 (OTA 예약 대응)
        String displayName = master.getGuestNameKo();
        if (displayName == null || displayName.isBlank()) {
            StringBuilder en = new StringBuilder();
            if (master.getGuestLastNameEn() != null) en.append(master.getGuestLastNameEn());
            if (master.getGuestFirstNameEn() != null) {
                if (en.length() > 0) en.append(" ");
                en.append(master.getGuestFirstNameEn());
            }
            if (en.length() > 0) displayName = en.toString();
        }
        String maskedName = (displayName != null && !displayName.isBlank())
                ? NameMaskingUtil.maskKoreanName(displayName) : null;

        return ReservationCalendarResponse.builder()
                .id(master.getId())
                .masterReservationNo(master.getMasterReservationNo())
                .reservationStatus(master.getReservationStatus())
                .masterCheckIn(master.getMasterCheckIn())
                .masterCheckOut(master.getMasterCheckOut())
                .guestNameMasked(maskedName)
                .roomInfo(roomInfo)
                .roomTypeName(roomTypeName)
                .stayType(stayType)
                .build();
    }

    // ─── 타임라인뷰 ──────────────────────────

    @Override
    public ReservationTimelineResponse getTimelineData(
            Long propertyId, LocalDate startDate, LocalDate endDate,
            String status, String keyword) {

        // 1. 예약 조회 + 필터링 (캘린더뷰와 동일 로직)
        List<MasterReservation> reservations = masterReservationRepository
                .findByPropertyIdAndDateRange(propertyId, startDate, endDate);

        List<MasterReservation> filtered = reservations.stream()
                .filter(r -> status == null || status.isEmpty() || status.equals(r.getReservationStatus()))
                .filter(r -> filterByKeyword(r, keyword))
                .collect(Collectors.toList());

        // 2. 프로퍼티 전체 객실 매핑 정보 조회 (RoomTypeFloor → roomNumberId → floorId, roomTypeId)
        List<RoomTypeFloor> allMappings = roomTypeFloorRepository.findAllByPropertyId(propertyId);

        // roomNumberId → RoomTypeFloor 매핑 (1객실 = 1매핑 가정, 중복 시 첫 번째 사용)
        Map<Long, RoomTypeFloor> roomMappingMap = new LinkedHashMap<>();
        for (RoomTypeFloor rtf : allMappings) {
            roomMappingMap.putIfAbsent(rtf.getRoomNumberId(), rtf);
        }

        // 3. 벌크 ID 조회 (Floor, RoomNumber, RoomType)
        Set<Long> allFloorIds = allMappings.stream()
                .map(RoomTypeFloor::getFloorId).collect(Collectors.toSet());
        Set<Long> allRoomTypeIds = allMappings.stream()
                .map(RoomTypeFloor::getRoomTypeId).collect(Collectors.toSet());
        Set<Long> allRoomNumberIds = roomMappingMap.keySet();

        Map<Long, String> floorMap = allFloorIds.isEmpty() ? Map.of()
                : floorRepository.findAllById(allFloorIds).stream()
                    .collect(Collectors.toMap(Floor::getId, Floor::getFloorNumber));

        Map<Long, RoomNumber> roomNumberEntityMap = allRoomNumberIds.isEmpty() ? Map.of()
                : roomNumberRepository.findAllById(allRoomNumberIds).stream()
                    .collect(Collectors.toMap(RoomNumber::getId, Function.identity()));

        Map<Long, String> roomTypeMap = allRoomTypeIds.isEmpty() ? Map.of()
                : roomTypeRepository.findAllById(allRoomTypeIds).stream()
                    .collect(Collectors.toMap(RoomType::getId, RoomType::getRoomTypeCode));

        // 4. 예약을 roomNumberId 기준 그룹핑
        Map<Long, List<MasterReservation>> roomReservationMap = new LinkedHashMap<>();
        List<MasterReservation> unassignedList = new ArrayList<>();

        for (MasterReservation m : filtered) {
            Long assignedRoomId = getAssignedRoomNumberId(m);
            if (assignedRoomId != null) {
                roomReservationMap.computeIfAbsent(assignedRoomId, k -> new ArrayList<>()).add(m);
            } else {
                unassignedList.add(m);
            }
        }

        // 5. 객실 벌크 조회용 맵 (서브예약 기반 floor/room/type 정보 - 캘린더와 동일)
        Set<Long> subFloorIds = new HashSet<>(allFloorIds);
        Set<Long> subRoomNumberIds = new HashSet<>(allRoomNumberIds);
        Set<Long> subRoomTypeIds = new HashSet<>(allRoomTypeIds);
        for (MasterReservation m : filtered) {
            for (SubReservation sub : m.getSubReservations()) {
                if (sub.getFloorId() != null) subFloorIds.add(sub.getFloorId());
                if (sub.getRoomNumberId() != null) subRoomNumberIds.add(sub.getRoomNumberId());
                if (sub.getRoomTypeId() != null) subRoomTypeIds.add(sub.getRoomTypeId());
            }
        }
        // floorMap/roomTypeMap 보강 (서브예약에서 추가된 ID)
        if (subFloorIds.size() > allFloorIds.size()) {
            Set<Long> extraFloorIds = new HashSet<>(subFloorIds);
            extraFloorIds.removeAll(allFloorIds);
            floorRepository.findAllById(extraFloorIds)
                    .forEach(f -> floorMap.put(f.getId(), f.getFloorNumber()));
        }
        Map<Long, String> subRoomNumberMap = subRoomNumberIds.isEmpty() ? Map.of()
                : roomNumberRepository.findAllById(subRoomNumberIds).stream()
                    .collect(Collectors.toMap(RoomNumber::getId, RoomNumber::getRoomNumber));
        if (subRoomTypeIds.size() > allRoomTypeIds.size()) {
            Set<Long> extraTypeIds = new HashSet<>(subRoomTypeIds);
            extraTypeIds.removeAll(allRoomTypeIds);
            roomTypeRepository.findAllById(extraTypeIds)
                    .forEach(rt -> roomTypeMap.put(rt.getId(), rt.getRoomTypeCode()));
        }

        // 6. TimelineRoom 목록 생성 (층 내림차순 → 호수 오름차순)
        List<ReservationTimelineResponse.TimelineRoom> timelineRooms = new ArrayList<>();

        for (Map.Entry<Long, RoomTypeFloor> entry : roomMappingMap.entrySet()) {
            Long roomId = entry.getKey();
            RoomTypeFloor rtf = entry.getValue();

            RoomNumber rn = roomNumberEntityMap.get(roomId);
            if (rn == null || Boolean.FALSE.equals(rn.getUseYn())) continue;

            String floorName = floorMap.getOrDefault(rtf.getFloorId(), "");
            String roomTypeName = roomTypeMap.getOrDefault(rtf.getRoomTypeId(), "");

            // 해당 객실의 예약 목록
            List<MasterReservation> roomReservations = roomReservationMap.getOrDefault(roomId, List.of());
            List<ReservationCalendarResponse> calDtos = roomReservations.stream()
                    .map(m -> toCalendarResponse(m, floorMap, subRoomNumberMap, roomTypeMap))
                    .collect(Collectors.toList());

            timelineRooms.add(ReservationTimelineResponse.TimelineRoom.builder()
                    .roomId(roomId)
                    .roomNumber(rn.getRoomNumber())
                    .floorName(floorName)
                    .roomTypeName(roomTypeName)
                    .reservations(calDtos)
                    .build());
        }

        // 정렬: 층 자연 오름차순(1F→2F→10F) → 호수 자연 오름차순
        timelineRooms.sort((a, b) -> {
            int floorCmp = naturalCompare(a.getFloorName(), b.getFloorName());
            if (floorCmp != 0) return floorCmp;
            return naturalCompare(a.getRoomNumber(), b.getRoomNumber());
        });

        // 미배정 예약 변환
        List<ReservationCalendarResponse> unassignedDtos = unassignedList.stream()
                .map(m -> toCalendarResponse(m, floorMap, subRoomNumberMap, roomTypeMap))
                .collect(Collectors.toList());

        return ReservationTimelineResponse.builder()
                .rooms(timelineRooms)
                .unassigned(unassignedDtos)
                .build();
    }

    /**
     * 마스터 예약의 배정된 roomNumberId 추출 (첫 활성 서브예약 기준)
     */
    private Long getAssignedRoomNumberId(MasterReservation master) {
        if (master.getSubReservations() == null) return null;
        return master.getSubReservations().stream()
                .filter(s -> !"CANCELED".equals(s.getRoomReservationStatus()))
                .map(SubReservation::getRoomNumberId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    // ─── Dayuse 헬퍼 메서드 ──────────────────────────

    /**
     * 레이트코드가 Dayuse인지 확인
     */
    /**
     * 자연 정렬: 문자열 내 숫자를 숫자값으로 비교 (1F < 2F < 10F, 101 < 102 < 1001)
     */
    private static int naturalCompare(String a, String b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        int ia = 0, ib = 0;
        while (ia < a.length() && ib < b.length()) {
            char ca = a.charAt(ia), cb = b.charAt(ib);
            if (Character.isDigit(ca) && Character.isDigit(cb)) {
                long na = 0, nb = 0;
                while (ia < a.length() && Character.isDigit(a.charAt(ia))) {
                    na = na * 10 + (a.charAt(ia++) - '0');
                }
                while (ib < b.length() && Character.isDigit(b.charAt(ib))) {
                    nb = nb * 10 + (b.charAt(ib++) - '0');
                }
                if (na != nb) return Long.compare(na, nb);
            } else {
                if (ca != cb) return Character.compare(ca, cb);
                ia++;
                ib++;
            }
        }
        return Integer.compare(a.length(), b.length());
    }

    private boolean isDayUseRateCode(Long rateCodeId) {
        if (rateCodeId == null) return false;
        RateCode rc = rateCodeRepository.findById(rateCodeId).orElse(null);
        return rc != null && rc.isDayUse();
    }

    /**
     * 레이트코드 기반 stayType 자동 결정
     */
    private StayType resolveStayType(String requestStayType, Long rateCodeId) {
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
