package com.hola.reservation.service;

import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.common.util.NameMaskingUtil;
import com.hola.hotel.entity.Floor;
import com.hola.hotel.entity.Property;
import com.hola.hotel.entity.RoomNumber;
import com.hola.hotel.repository.FloorRepository;
import com.hola.hotel.repository.MarketCodeRepository;
import com.hola.hotel.repository.PropertyRepository;
import com.hola.hotel.repository.RoomNumberRepository;
import com.hola.rate.repository.RateCodeRepository;
import com.hola.room.entity.RoomType;
import com.hola.room.repository.RoomTypeRepository;
import com.hola.reservation.dto.request.*;
import com.hola.reservation.dto.response.*;
import com.hola.reservation.entity.*;
import com.hola.reservation.mapper.ReservationMapper;
import com.hola.reservation.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private final ReservationMapper reservationMapper;
    private final ReservationNumberGenerator numberGenerator;
    private final RoomAvailabilityService availabilityService;
    private final PriceCalculationService priceCalculationService;
    private final EarlyLateCheckService earlyLateCheckService;
    private final ReservationPaymentService paymentService;

    // 허용되는 상태 전이 매트릭스
    private static final Map<String, Set<String>> STATUS_TRANSITIONS = Map.of(
            "RESERVED", Set.of("CHECK_IN", "CANCELED", "NO_SHOW"),
            "CHECK_IN", Set.of("INHOUSE", "CANCELED"),
            "INHOUSE", Set.of("CHECKED_OUT"),
            "CHECKED_OUT", Set.of(),
            "CANCELED", Set.of(),
            "NO_SHOW", Set.of()
    );

    @Override
    public List<ReservationListResponse> getList(Long propertyId, String status, LocalDate checkInFrom,
                                                   LocalDate checkInTo, String keyword) {
        List<MasterReservation> reservations;

        if (status != null && !status.isEmpty()) {
            reservations = masterReservationRepository
                    .findByPropertyIdAndReservationStatus(propertyId, status);
        } else {
            reservations = masterReservationRepository.findByPropertyIdOrderByReservationDateDesc(propertyId);
        }

        // Java 필터링 (JPQL null LIKE 이슈 회피)
        return reservations.stream()
                .filter(r -> filterByDateRange(r, checkInFrom, checkInTo))
                .filter(r -> filterByKeyword(r, keyword))
                .map(reservationMapper::toReservationListResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ReservationDetailResponse getById(Long id) {
        MasterReservation master = findMasterById(id);
        ReservationDetailResponse response = reservationMapper.toReservationDetailResponse(master);

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
                .subReservations(response.getSubReservations())
                .deposits(depositResponses)
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

        // 체크인/체크아웃 유효성
        validateDates(request.getMasterCheckIn(), request.getMasterCheckOut());

        // 마스터 예약 생성
        MasterReservation master = reservationMapper.toMasterReservationEntity(request, property);

        // 예약번호 + 확인번호 생성
        String reservationNo = numberGenerator.generateMasterReservationNo(property);
        String confirmationNo = numberGenerator.generateConfirmationNo();
        master.updateStatus("RESERVED"); // 초기 상태
        // Builder로 생성된 엔티티에 번호를 직접 세팅할 수 없으므로, 별도 처리
        master = MasterReservation.builder()
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

        return getById(master.getId());
    }

    @Override
    @Transactional
    public ReservationDetailResponse update(Long id, ReservationUpdateRequest request) {
        MasterReservation master = findMasterById(id);

        // OTA 수정 제한 검사
        if (Boolean.TRUE.equals(master.getIsOtaManaged())) {
            throw new HolaException(ErrorCode.RESERVATION_OTA_EDIT_RESTRICTED);
        }

        validateDates(request.getMasterCheckIn(), request.getMasterCheckOut());

        master.update(
                request.getMasterCheckIn(), request.getMasterCheckOut(),
                request.getGuestNameKo(), request.getGuestFirstNameEn(),
                request.getGuestMiddleNameEn(), request.getGuestLastNameEn(),
                request.getPhoneCountryCode(), request.getPhoneNumber(),
                request.getEmail(), request.getBirthDate(), request.getGender(),
                request.getNationality(), request.getRateCodeId(),
                request.getMarketCodeId(), request.getReservationChannelId(),
                request.getPromotionType(), request.getPromotionCode(),
                request.getOtaReservationNo(), request.getIsOtaManaged(),
                request.getCustomerRequest()
        );

        log.info("마스터 예약 수정: {}", master.getMasterReservationNo());
        return getById(id);
    }

    @Override
    @Transactional
    public void cancel(Long id) {
        MasterReservation master = findMasterById(id);

        // RESERVED 상태만 취소 가능
        if (!"RESERVED".equals(master.getReservationStatus())) {
            throw new HolaException(ErrorCode.RESERVATION_STATUS_CHANGE_NOT_ALLOWED);
        }

        master.updateStatus("CANCELED");

        // 모든 서브 예약도 취소
        for (SubReservation sub : master.getSubReservations()) {
            sub.updateStatus("CANCELED");
        }

        log.info("예약 취소: {}", master.getMasterReservationNo());
    }

    @Override
    @Transactional
    public void changeStatus(Long id, ReservationStatusRequest request) {
        MasterReservation master = findMasterById(id);
        String currentStatus = master.getReservationStatus();
        String newStatus = request.getNewStatus();

        // 상태 전이 검증
        Set<String> allowedTransitions = STATUS_TRANSITIONS.getOrDefault(currentStatus, Set.of());
        if (!allowedTransitions.contains(newStatus)) {
            throw new HolaException(ErrorCode.RESERVATION_STATUS_CHANGE_NOT_ALLOWED);
        }

        master.updateStatus(newStatus);
        LocalDateTime now = LocalDateTime.now();

        // 서브 예약 동기화 (마스터와 동일 상태로 변경)
        for (SubReservation sub : master.getSubReservations()) {
            // CANCELED 서브는 건너뜀 (부분취소된 경우)
            if (!"CANCELED".equals(sub.getRoomReservationStatus())) {
                sub.updateStatus(newStatus);

                // 체크인 시 실제 시각 및 얼리 체크인 요금 기록
                if ("CHECK_IN".equals(newStatus)) {
                    BigDecimal earlyFee = earlyLateCheckService.calculateEarlyCheckInFee(sub, now);
                    sub.recordCheckIn(now, earlyFee);
                }

                // 체크아웃 시 실제 시각 및 레이트 체크아웃 요금 기록
                if ("CHECKED_OUT".equals(newStatus)) {
                    BigDecimal lateFee = earlyLateCheckService.calculateLateCheckOutFee(sub, now);
                    sub.recordCheckOut(now, lateFee);
                }
            }
        }

        // 얼리/레이트 요금 발생 시 결제 재계산
        if ("CHECK_IN".equals(newStatus) || "CHECKED_OUT".equals(newStatus)) {
            paymentService.recalculatePayment(master.getId());
        }

        log.info("예약 상태 변경: {} → {} (예약번호: {})", currentStatus, newStatus, master.getMasterReservationNo());
    }

    @Override
    @Transactional
    public SubReservationResponse addLeg(Long reservationId, SubReservationRequest request) {
        MasterReservation master = findMasterById(reservationId);
        Property property = master.getProperty();

        int legSeq = master.getSubReservations().size() + 1;
        SubReservation sub = createSubReservation(master, request, legSeq, property);

        // 마스터 체크인/체크아웃 자동 동기화
        syncMasterDates(master);

        return reservationMapper.toSubReservationResponse(sub);
    }

    @Override
    @Transactional
    public SubReservationResponse updateLeg(Long reservationId, Long legId, SubReservationRequest request) {
        findMasterById(reservationId);
        SubReservation sub = subReservationRepository.findById(legId)
                .orElseThrow(() -> new HolaException(ErrorCode.SUB_RESERVATION_NOT_FOUND));

        validateDates(request.getCheckIn(), request.getCheckOut());

        // L1 객실 충돌 검사 (자기 자신 제외)
        if (request.getRoomNumberId() != null) {
            if (availabilityService.hasRoomConflict(request.getRoomNumberId(),
                    request.getCheckIn(), request.getCheckOut(), legId)) {
                throw new HolaException(ErrorCode.SUB_RESERVATION_ROOM_CONFLICT);
            }
        }

        sub.update(request.getRoomTypeId(), request.getFloorId(), request.getRoomNumberId(),
                request.getAdults() != null ? request.getAdults() : 1,
                request.getChildren() != null ? request.getChildren() : 0,
                request.getCheckIn(), request.getCheckOut(),
                request.getEarlyCheckIn() != null ? request.getEarlyCheckIn() : false,
                request.getLateCheckOut() != null ? request.getLateCheckOut() : false);

        // 투숙객 갱신
        updateGuests(sub, request.getGuests());

        // 일별 요금 재계산
        recalculateDailyCharges(sub, sub.getMasterReservation().getProperty());

        // 마스터 체크인/체크아웃 동기화
        syncMasterDates(sub.getMasterReservation());

        log.info("서브 예약 수정: {}", sub.getSubReservationNo());
        return reservationMapper.toSubReservationResponse(sub);
    }

    @Override
    @Transactional
    public void deleteLeg(Long reservationId, Long legId) {
        MasterReservation master = findMasterById(reservationId);
        SubReservation sub = subReservationRepository.findById(legId)
                .orElseThrow(() -> new HolaException(ErrorCode.SUB_RESERVATION_NOT_FOUND));

        // RESERVED 상태만 개별 삭제 가능
        if (!"RESERVED".equals(sub.getRoomReservationStatus())) {
            throw new HolaException(ErrorCode.RESERVATION_STATUS_CHANGE_NOT_ALLOWED);
        }

        sub.updateStatus("CANCELED");
        sub.softDelete();

        syncMasterDates(master);
        log.info("서브 예약 삭제(취소): {}", sub.getSubReservationNo());
    }

    @Override
    public int checkAvailability(Long propertyId, Long roomTypeId, LocalDate checkIn, LocalDate checkOut) {
        return availabilityService.getAvailableRoomCount(roomTypeId, checkIn, checkOut);
    }

    @Override
    public List<ReservationMemoResponse> getMemos(Long reservationId) {
        findMasterById(reservationId);
        return reservationMemoRepository.findByMasterReservationIdOrderByCreatedAtDesc(reservationId)
                .stream()
                .map(reservationMapper::toReservationMemoResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ReservationMemoResponse addMemo(Long reservationId, String content) {
        findMasterById(reservationId);
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
    public ReservationDepositResponse addDeposit(Long reservationId, ReservationDepositRequest request) {
        MasterReservation master = findMasterById(reservationId);
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
    public ReservationDepositResponse updateDeposit(Long reservationId, Long depositId, ReservationDepositRequest request) {
        findMasterById(reservationId);
        ReservationDeposit deposit = reservationDepositRepository.findById(depositId)
                .orElseThrow(() -> new HolaException(ErrorCode.RESERVATION_NOT_FOUND));
        deposit.update(request.getDepositMethod(), request.getCardCompany(),
                request.getCardNumberEncrypted(), request.getCardCvcEncrypted(),
                request.getCardExpiryDate(), request.getCardPasswordEncrypted(),
                request.getCurrency(), request.getAmount());
        log.info("예치금 수정: depositId={}", depositId);
        return reservationMapper.toReservationDepositResponse(deposit);
    }

    // ─── 내부 헬퍼 메서드 ──────────────────────────

    private MasterReservation findMasterById(Long id) {
        return masterReservationRepository.findById(id)
                .orElseThrow(() -> new HolaException(ErrorCode.RESERVATION_NOT_FOUND));
    }

    private void validateDates(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn != null && checkOut != null && !checkOut.isAfter(checkIn)) {
            throw new HolaException(ErrorCode.SUB_RESERVATION_DATE_INVALID);
        }
    }

    /**
     * 서브 예약 생성 + 투숙객 + 일별요금
     */
    private SubReservation createSubReservation(MasterReservation master, SubReservationRequest request,
                                                 int legSeq, Property property) {
        validateDates(request.getCheckIn(), request.getCheckOut());

        // L1 객실 충돌 검사
        if (request.getRoomNumberId() != null) {
            if (availabilityService.hasRoomConflict(request.getRoomNumberId(),
                    request.getCheckIn(), request.getCheckOut(), null)) {
                throw new HolaException(ErrorCode.SUB_RESERVATION_ROOM_CONFLICT);
            }
        }

        // 서브 예약 생성
        String subNo = numberGenerator.generateSubReservationNo(master.getMasterReservationNo(), legSeq);
        SubReservation sub = reservationMapper.toSubReservationEntity(request, master);
        sub = SubReservation.builder()
                .masterReservation(master)
                .subReservationNo(subNo)
                .roomReservationStatus("RESERVED")
                .roomTypeId(request.getRoomTypeId())
                .floorId(request.getFloorId())
                .roomNumberId(request.getRoomNumberId())
                .adults(request.getAdults() != null ? request.getAdults() : 1)
                .children(request.getChildren() != null ? request.getChildren() : 0)
                .checkIn(request.getCheckIn())
                .checkOut(request.getCheckOut())
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

        return sub;
    }

    /**
     * 투숙객 목록 갱신 (삭제 후 재등록)
     */
    private void updateGuests(SubReservation sub, List<ReservationGuestRequest> guestRequests) {
        if (guestRequests == null) return;

        reservationGuestRepository.deleteAllBySubReservationId(sub.getId());
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
        dailyChargeRepository.deleteAllBySubReservationId(sub.getId());
        dailyChargeRepository.flush();

        Long rateCodeId = sub.getMasterReservation().getRateCodeId();
        if (rateCodeId == null) return;

        List<DailyCharge> charges = priceCalculationService.calculateDailyCharges(
                rateCodeId, property,
                sub.getCheckIn(), sub.getCheckOut(),
                sub.getAdults(), sub.getChildren(), sub);

        dailyChargeRepository.saveAll(charges);
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

        master.update(earliestCheckIn, latestCheckOut,
                master.getGuestNameKo(), master.getGuestFirstNameEn(),
                master.getGuestMiddleNameEn(), master.getGuestLastNameEn(),
                master.getPhoneCountryCode(), master.getPhoneNumber(),
                master.getEmail(), master.getBirthDate(), master.getGender(),
                master.getNationality(), master.getRateCodeId(),
                master.getMarketCodeId(), master.getReservationChannelId(),
                master.getPromotionType(), master.getPromotionCode(),
                master.getOtaReservationNo(), master.getIsOtaManaged(),
                master.getCustomerRequest());
    }

    /**
     * 날짜 범위 필터
     */
    private boolean filterByDateRange(MasterReservation r, LocalDate from, LocalDate to) {
        if (from == null && to == null) return true;
        LocalDate checkIn = r.getMasterCheckIn();
        if (from != null && checkIn.isBefore(from)) return false;
        if (to != null && checkIn.isAfter(to)) return false;
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

            // 체류 기간 내 각 날짜에 매핑
            LocalDate cursor = m.getMasterCheckIn().isBefore(startDate) ? startDate : m.getMasterCheckIn();
            LocalDate until = m.getMasterCheckOut().isAfter(endDate) ? endDate : m.getMasterCheckOut();

            while (!cursor.isAfter(until.minusDays(1))) {
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

        // 첫 번째 활성 서브 예약 기준 객실 정보
        String roomInfo = null;
        String roomTypeName = null;
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
        }

        return ReservationCalendarResponse.builder()
                .id(master.getId())
                .masterReservationNo(master.getMasterReservationNo())
                .reservationStatus(master.getReservationStatus())
                .masterCheckIn(master.getMasterCheckIn())
                .masterCheckOut(master.getMasterCheckOut())
                .guestNameMasked(NameMaskingUtil.maskKoreanName(master.getGuestNameKo()))
                .roomInfo(roomInfo)
                .roomTypeName(roomTypeName)
                .build();
    }
}
