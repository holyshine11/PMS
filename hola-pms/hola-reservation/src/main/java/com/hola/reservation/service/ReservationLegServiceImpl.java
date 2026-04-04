package com.hola.reservation.service;

import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.hotel.entity.RoomUnavailable;
import com.hola.hotel.repository.RoomUnavailableRepository;
import com.hola.reservation.dto.request.SubReservationRequest;
import com.hola.reservation.dto.response.SubReservationResponse;
import com.hola.reservation.entity.MasterReservation;
import com.hola.reservation.entity.SubReservation;
import com.hola.reservation.mapper.ReservationMapper;
import com.hola.reservation.repository.SubReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 서브 예약(Leg) 관리 서비스 구현 — 추가/수정/삭제
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationLegServiceImpl implements ReservationLegService {

    private final ReservationFinder finder;
    private final SubReservationCreator subCreator;
    private final SubReservationRepository subReservationRepository;
    private final RoomUnavailableRepository roomUnavailableRepository;
    private final ReservationMapper reservationMapper;
    private final ReservationPaymentService paymentService;
    private final RoomAvailabilityService availabilityService;
    private final RateIncludedServiceHelper rateIncludedServiceHelper;
    private final ReservationChangeLogService changeLogService;
    private final EarlyLateCheckService earlyLateCheckService;

    @Override
    @Transactional
    public SubReservationResponse addLeg(Long reservationId, Long propertyId, SubReservationRequest request) {
        MasterReservation master = finder.findMasterById(reservationId, propertyId);
        finder.validateModifiable(master);

        // 레이트코드 판매기간/숙박일수 검증
        subCreator.validateRateCode(master.getRateCodeId(), request.getCheckIn(), request.getCheckOut());

        // 소프트삭제 포함 전체 수 기준 채번
        int legSeq = subReservationRepository.countAllIncludingDeleted(master.getId()) + 1;

        SubReservation sub = subCreator.create(master, request, legSeq, master.getProperty());

        // 마스터 체크인/체크아웃 동기화
        syncMasterDates(master);

        // 결제 금액 재계산
        paymentService.recalculatePayment(master.getId());

        log.info("서브예약 추가: master={}, leg={}", master.getMasterReservationNo(), sub.getSubReservationNo());
        try {
            changeLogService.log(master.getId(), sub.getId(), "ROOM", "ADD_LEG",
                    null, null, sub.getSubReservationNo(),
                    "객실 레그 추가: " + sub.getSubReservationNo());
        } catch (Exception e) {
            log.error("변경이력 기록 실패: {}", e.getMessage());
        }
        return reservationMapper.toSubReservationResponse(sub);
    }

    @Override
    @Transactional
    public SubReservationResponse updateLeg(Long reservationId, Long propertyId, Long legId, SubReservationRequest request) {
        MasterReservation master = finder.findMasterById(reservationId, propertyId);
        finder.validateModifiable(master);

        SubReservation sub = finder.findSubAndValidateOwnership(legId, master);

        subCreator.validateDates(request.getCheckIn(), request.getCheckOut());

        // 객실 충돌 검사
        if (request.getRoomNumberId() != null) {
            if (availabilityService.hasRoomConflict(request.getRoomNumberId(),
                    request.getCheckIn(), request.getCheckOut(), sub.getId(),
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

        sub.update(request.getRoomTypeId(), request.getFloorId(), request.getRoomNumberId(),
                request.getAdults() != null ? request.getAdults() : 1,
                request.getChildren() != null ? request.getChildren() : 0,
                request.getCheckIn(), request.getCheckOut(),
                request.getEarlyCheckIn() != null ? request.getEarlyCheckIn() : false,
                request.getLateCheckOut() != null ? request.getLateCheckOut() : false);

        subCreator.updateGuests(sub, request.getGuests());
        subCreator.recalculateDailyCharges(sub, master.getProperty());

        // 마스터 날짜 동기화
        syncMasterDates(master);

        // 결제 금액 재계산
        paymentService.recalculatePayment(master.getId());

        log.info("서브예약 수정: {}", sub.getSubReservationNo());
        return reservationMapper.toSubReservationResponse(sub);
    }

    @Override
    @Transactional
    public void deleteLeg(Long reservationId, Long propertyId, Long legId) {
        MasterReservation master = finder.findMasterById(reservationId, propertyId);
        finder.validateModifiable(master);

        SubReservation sub = finder.findSubAndValidateOwnership(legId, master);

        // 마지막 남은 레그 삭제 불가
        long activeLegCount = master.getSubReservations().stream()
                .filter(s -> s.getDeletedAt() == null)
                .count();
        if (activeLegCount <= 1) {
            throw new HolaException(ErrorCode.SUB_RESERVATION_LAST_LEG);
        }

        sub.softDelete();

        // 마스터 날짜 동기화
        syncMasterDates(master);

        // 결제 금액 재계산
        paymentService.recalculatePayment(master.getId());

        log.info("서브예약 삭제: {}", sub.getSubReservationNo());
        try {
            changeLogService.log(master.getId(), sub.getId(), "ROOM", "REMOVE_LEG",
                    null, sub.getSubReservationNo(), null,
                    "객실 레그 삭제: " + sub.getSubReservationNo());
        } catch (Exception e) {
            log.error("변경이력 기록 실패: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public java.math.BigDecimal registerEarlyLateFee(Long reservationId, Long propertyId, Long legId,
                                                      String policyType, int policyIndex) {
        MasterReservation master = finder.findMasterById(reservationId, propertyId);
        SubReservation sub = finder.findSubAndValidateOwnership(legId, master);

        java.math.BigDecimal fee = earlyLateCheckService.calculateFeeByPolicyIndex(sub, policyType, policyIndex);

        if ("EARLY_CHECKIN".equals(policyType)) {
            sub.registerEarlyCheckInFee(fee);
        } else {
            sub.registerLateCheckOutFee(fee);
        }

        subReservationRepository.flush();
        paymentService.recalculatePayment(master.getId());

        log.info("얼리/레이트 요금 등록: leg={}, type={}, fee={}", sub.getSubReservationNo(), policyType, fee);
        try {
            String label = "EARLY_CHECKIN".equals(policyType) ? "얼리체크인" : "레이트체크아웃";
            changeLogService.log(master.getId(), sub.getId(), "ROOM", "UPDATE",
                    "EARLY_CHECKIN".equals(policyType) ? "earlyCheckIn" : "lateCheckOut",
                    "미사용", "사용 (₩" + fee.stripTrailingZeros().toPlainString() + ")",
                    sub.getSubReservationNo() + " " + label + " 설정: ₩" + fee.stripTrailingZeros().toPlainString());
        } catch (Exception e) {
            log.error("변경이력 기록 실패: {}", e.getMessage());
        }
        return fee;
    }

    @Override
    @Transactional
    public void removeEarlyLateFee(Long reservationId, Long propertyId, Long legId, String policyType) {
        MasterReservation master = finder.findMasterById(reservationId, propertyId);
        SubReservation sub = finder.findSubAndValidateOwnership(legId, master);

        if ("EARLY_CHECKIN".equals(policyType)) {
            sub.clearEarlyCheckInFee();
        } else {
            sub.clearLateCheckOutFee();
        }

        subReservationRepository.flush();
        paymentService.recalculatePayment(master.getId());

        log.info("얼리/레이트 요금 해제: leg={}, type={}", sub.getSubReservationNo(), policyType);
        try {
            String label = "EARLY_CHECKIN".equals(policyType) ? "얼리체크인" : "레이트체크아웃";
            changeLogService.log(master.getId(), sub.getId(), "ROOM", "UPDATE",
                    "EARLY_CHECKIN".equals(policyType) ? "earlyCheckIn" : "lateCheckOut",
                    "사용", "미사용",
                    sub.getSubReservationNo() + " " + label + " 해제");
        } catch (Exception e) {
            log.error("변경이력 기록 실패: {}", e.getMessage());
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

        java.time.LocalDate earliestCheckIn = activeSubs.stream()
                .map(SubReservation::getCheckIn)
                .min(java.time.LocalDate::compareTo)
                .orElse(master.getMasterCheckIn());

        java.time.LocalDate latestCheckOut = activeSubs.stream()
                .map(SubReservation::getCheckOut)
                .max(java.time.LocalDate::compareTo)
                .orElse(master.getMasterCheckOut());

        master.syncDates(earliestCheckIn, latestCheckOut);
    }
}
