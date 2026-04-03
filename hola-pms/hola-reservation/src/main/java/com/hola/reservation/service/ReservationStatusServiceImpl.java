package com.hola.reservation.service;

import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.reservation.dto.request.ReservationStatusRequest;
import com.hola.reservation.dto.response.AdminCancelPreviewResponse;
import com.hola.reservation.dto.response.LegPaymentInfo;
import com.hola.reservation.entity.DailyCharge;
import com.hola.reservation.entity.MasterReservation;
import com.hola.reservation.entity.PaymentTransaction;
import com.hola.reservation.entity.ReservationPayment;
import com.hola.reservation.entity.ReservationServiceItem;
import com.hola.reservation.entity.SubReservation;
import com.hola.reservation.repository.DailyChargeRepository;
import com.hola.reservation.repository.MasterReservationRepository;
import com.hola.reservation.repository.PaymentTransactionRepository;
import com.hola.reservation.repository.ReservationPaymentRepository;
import com.hola.reservation.repository.ReservationServiceItemRepository;
import com.hola.reservation.repository.SubReservationRepository;
import com.hola.room.entity.PaidServiceOption;
import com.hola.room.entity.RoomType;
import com.hola.room.repository.PaidServiceOptionRepository;
import com.hola.room.repository.RoomTypeRepository;
import com.hola.hotel.repository.RoomNumberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 예약 상태 관리 서비스 구현 — 상태 전환, 취소, 노쇼 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationStatusServiceImpl implements ReservationStatusService {

    private final ReservationFinder finder;
    private final MasterReservationRepository masterReservationRepository;
    private final SubReservationRepository subReservationRepository;
    private final DailyChargeRepository dailyChargeRepository;
    private final ReservationPaymentRepository reservationPaymentRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final ReservationServiceItemRepository serviceItemRepository;
    private final RoomNumberRepository roomNumberRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final PaidServiceOptionRepository paidServiceOptionRepository;
    private final ReservationPaymentService paymentService;
    private final EarlyLateCheckService earlyLateCheckService;
    private final com.hola.reservation.booking.service.CancellationPolicyService cancellationPolicyService;
    private final com.hola.room.service.InventoryService inventoryService;
    private final com.hola.hotel.service.HousekeepingService housekeepingService;
    private final ReservationChangeLogService changeLogService;

    // 허용되는 상태 전이 매트릭스
    private static final Map<String, Set<String>> STATUS_TRANSITIONS = Map.of(
            "RESERVED", Set.of("INHOUSE", "CANCELED", "NO_SHOW"),
            "CHECK_IN", Set.of("INHOUSE", "CANCELED"),
            "INHOUSE", Set.of("CHECKED_OUT"),
            "CHECKED_OUT", Set.of(),
            "CANCELED", Set.of(),
            "NO_SHOW", Set.of()
    );

    @Override
    @Transactional
    public void changeStatus(Long id, Long propertyId, ReservationStatusRequest request) {
        MasterReservation master = finder.findMasterById(id, propertyId);
        String newStatus = request.getNewStatus();
        boolean statusChanged = false;

        if (request.getSubReservationId() != null) {
            // ── Leg 단위 상태 변경 ──
            SubReservation targetSub = finder.findSubAndValidateOwnership(request.getSubReservationId(), master);
            String currentLegStatus = targetSub.getRoomReservationStatus();

            Set<String> allowed = STATUS_TRANSITIONS.getOrDefault(currentLegStatus, Set.of());
            if (!allowed.contains(newStatus)) {
                throw new HolaException(ErrorCode.RESERVATION_STATUS_CHANGE_NOT_ALLOWED);
            }

            if ("INHOUSE".equals(newStatus) && "RESERVED".equals(currentLegStatus)) {
                validateCheckInPrerequisites(targetSub);
            }

            if ("CHECKED_OUT".equals(newStatus) && isLastActiveLeg(targetSub, master)) {
                validateCheckOutBalance(master);
            }

            if ("CANCELED".equals(newStatus) || "NO_SHOW".equals(newStatus)) {
                if ("NO_SHOW".equals(newStatus) && master.getMasterCheckIn().isAfter(LocalDate.now())) {
                    throw new HolaException(ErrorCode.RESERVATION_STATUS_CHANGE_NOT_ALLOWED);
                }
                validateCancelFeePayment(master, propertyId, "NO_SHOW".equals(newStatus));

                if ("CANCELED".equals(newStatus)) {
                    validateLegUnpaidBalance(master, targetSub);
                }
            }

            applyStatusChange(targetSub, newStatus);
            statusChanged = true;

            if ("CANCELED".equals(newStatus) || "NO_SHOW".equals(newStatus)) {
                releaseServiceItemInventory(targetSub);
            }

            log.info("Leg 상태 변경: {} → {} (서브예약: {})", currentLegStatus, newStatus, targetSub.getSubReservationNo());
            try {
                changeLogService.logStatusChange(master.getId(), targetSub.getId(), currentLegStatus, newStatus);
            } catch (Exception e) {
                log.error("변경이력 기록 실패: {}", e.getMessage());
            }

        } else {
            // ── 전체 Leg 일괄 변경 ──
            if ("NO_SHOW".equals(newStatus) || "CANCELED".equals(newStatus)) {
                Set<String> masterAllowed = STATUS_TRANSITIONS.getOrDefault(master.getReservationStatus(), Set.of());
                if (!masterAllowed.contains(newStatus)) {
                    throw new HolaException(ErrorCode.RESERVATION_STATUS_CHANGE_NOT_ALLOWED);
                }
                if ("NO_SHOW".equals(newStatus) && master.getMasterCheckIn().isAfter(LocalDate.now())) {
                    throw new HolaException(ErrorCode.RESERVATION_STATUS_CHANGE_NOT_ALLOWED);
                }
                validateCancelFeePayment(master, propertyId, "NO_SHOW".equals(newStatus));

                if ("CANCELED".equals(newStatus)) {
                    validateUnpaidBalance(master);
                }

                for (SubReservation sub : master.getSubReservations()) {
                    if (!"CANCELED".equals(sub.getRoomReservationStatus())
                            && !"NO_SHOW".equals(sub.getRoomReservationStatus())
                            && !"CHECKED_OUT".equals(sub.getRoomReservationStatus())) {
                        sub.updateStatus(newStatus);
                        releaseServiceItemInventory(sub);
                    }
                }
                statusChanged = true;
            } else {
                if ("INHOUSE".equals(newStatus)) {
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

        String derivedStatus = deriveMasterStatus(master.getSubReservations());
        String previousMasterStatus = master.getReservationStatus();
        master.updateStatus(derivedStatus);

        if (!previousMasterStatus.equals(derivedStatus)) {
            try {
                changeLogService.logStatusChange(master.getId(), null, previousMasterStatus, derivedStatus);
            } catch (Exception e) {
                log.error("변경이력 기록 실패: {}", e.getMessage());
            }
        }

        if ("INHOUSE".equals(newStatus) || "CHECKED_OUT".equals(newStatus)) {
            paymentService.recalculatePayment(master.getId());
        }

        if ("NO_SHOW".equals(newStatus)) {
            processNoShow(master, propertyId);
        }

        if ("CANCELED".equals(derivedStatus) && !"CANCELED".equals(previousMasterStatus)) {
            processCancel(master, propertyId);
        }

        if ("CANCELED".equals(newStatus) && !"CANCELED".equals(derivedStatus)) {
            SubReservation canceledTarget = null;
            if (request.getSubReservationId() != null) {
                canceledTarget = finder.findSubAndValidateOwnership(request.getSubReservationId(), master);
            }
            processPartialLegCancelRefund(master, canceledTarget);
        }

        log.info("예약 상태 변경: {} → {} (마스터: {}, 예약번호: {})",
                previousMasterStatus, derivedStatus, newStatus, master.getMasterReservationNo());
    }

    @Override
    @Transactional(readOnly = true)
    public AdminCancelPreviewResponse getCancelPreview(Long id, Long propertyId, boolean noShow, Long subReservationId) {
        MasterReservation master = finder.findMasterById(id, propertyId);

        String currentStatus = master.getReservationStatus();
        if (!"RESERVED".equals(currentStatus) && !"CHECK_IN".equals(currentStatus)
                && !"INHOUSE".equals(currentStatus)) {
            throw new HolaException(ErrorCode.RESERVATION_STATUS_CHANGE_NOT_ALLOWED);
        }

        BigDecimal firstNightSupply;
        if (subReservationId != null) {
            firstNightSupply = getFirstNightTotalForSub(subReservationId);
        } else {
            firstNightSupply = getFirstNightTotal(master.getId());
        }

        var cancelResult = cancellationPolicyService.calculateCancelFee(
                propertyId, master.getMasterCheckIn(), firstNightSupply, noShow);

        BigDecimal totalPaid = BigDecimal.ZERO;
        BigDecimal grandTotal = BigDecimal.ZERO;
        BigDecimal unpaidBalance = BigDecimal.ZERO;
        String targetSubNo = null;

        if (subReservationId != null) {
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

        List<PaymentTransaction> txns = paymentTransactionRepository
                .findByMasterReservationIdOrderByTransactionSeqAsc(master.getId());

        final Long targetSubId = subReservationId;
        List<PaymentTransaction> paymentTxns = txns.stream()
                .filter(t -> "PAYMENT".equals(t.getTransactionType()))
                .filter(t -> targetSubId == null || targetSubId.equals(t.getSubReservationId()))
                .toList();

        boolean isPgPayment = false;
        String pgCardNo = null;
        String pgIssuerName = null;

        List<PaymentTransaction> pgPayments = paymentTxns.stream()
                .filter(t -> t.getPgCno() != null).toList();
        List<PaymentTransaction> nonPgPayments = paymentTxns.stream()
                .filter(t -> t.getPgCno() == null).toList();

        if (!pgPayments.isEmpty()) {
            isPgPayment = true;
            pgCardNo = pgPayments.get(0).getPgCardNo();
            pgIssuerName = pgPayments.get(0).getPgIssuerName();
        }

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
        MasterReservation master = finder.findMasterById(id, propertyId);

        String currentStatus = master.getReservationStatus();
        if (!"RESERVED".equals(currentStatus) && !"CHECK_IN".equals(currentStatus)) {
            throw new HolaException(ErrorCode.RESERVATION_STATUS_CHANGE_NOT_ALLOWED);
        }

        validateUnpaidBalance(master);

        BigDecimal firstNightSupply = getFirstNightTotal(master.getId());
        var cancelResult = cancellationPolicyService.calculateCancelFee(
                propertyId, master.getMasterCheckIn(), firstNightSupply);

        ReservationPayment payment = reservationPaymentRepository
                .findByMasterReservationId(master.getId()).orElse(null);
        if (payment != null) {
            BigDecimal totalPaid = payment.getTotalPaidAmount() != null ? payment.getTotalPaidAmount() : BigDecimal.ZERO;
            BigDecimal existingRefund = payment.getRefundAmount() != null ? payment.getRefundAmount() : BigDecimal.ZERO;
            BigDecimal existingCancelFee = payment.getCancelFeeAmount() != null ? payment.getCancelFeeAmount() : BigDecimal.ZERO;
            BigDecimal netPaid = totalPaid.subtract(existingRefund).subtract(existingCancelFee);
            BigDecimal cancelFee = cancelResult.feeAmount();

            if (cancelFee.compareTo(BigDecimal.ZERO) > 0 && netPaid.compareTo(cancelFee) < 0) {
                throw new HolaException(ErrorCode.CANCEL_FEE_UNPAID);
            }

            BigDecimal refundAmt = netPaid.subtract(cancelFee).max(BigDecimal.ZERO);
            payment.updateCancelRefund(cancelFee, refundAmt);

            if (refundAmt.compareTo(BigDecimal.ZERO) > 0 || cancelFee.compareTo(BigDecimal.ZERO) > 0) {
                String memo = cancelResult.policyDescription() + " / 취소 환불 (수수료: " + cancelFee + "원)";
                if (Boolean.TRUE.equals(master.getIsOtaManaged())) {
                    memo += " [OTA 예약 — PG 환불은 OTA 채널에서 처리 필요]";
                }
                paymentService.processRefundWithPg(master.getId(),
                        Boolean.TRUE.equals(master.getIsOtaManaged()) ? BigDecimal.ZERO : refundAmt,
                        cancelFee, memo);
            }

            payment.setPaymentStatusRefunded();
        }

        for (SubReservation sub : master.getSubReservations()) {
            releaseServiceItemInventory(sub);
        }

        master.updateStatus("CANCELED");
        for (SubReservation sub : master.getSubReservations()) {
            sub.updateStatus("CANCELED");
        }

        log.info("예약 취소: {}, OTA={}, 취소수수료: {}, 정책: {}",
                master.getMasterReservationNo(), master.getIsOtaManaged(),
                cancelResult.feeAmount(), cancelResult.policyDescription());
    }

    // ─── private helpers ──────────────────────────

    private void applyStatusChange(SubReservation sub, String newStatus) {
        sub.updateStatus(newStatus);
        LocalDateTime now = LocalDateTime.now();

        if ("INHOUSE".equals(newStatus) && sub.getActualCheckInTime() == null) {
            BigDecimal earlyFee = sub.isDayUse() ? BigDecimal.ZERO
                    : earlyLateCheckService.calculateEarlyCheckInFee(sub, now);
            sub.recordCheckIn(now, earlyFee);
            if (sub.getRoomNumberId() != null) {
                com.hola.hotel.entity.RoomNumber room = roomNumberRepository.findById(sub.getRoomNumberId()).orElse(null);
                if (room != null) room.checkIn();
            }
        }

        if ("CHECKED_OUT".equals(newStatus)) {
            BigDecimal lateFee = sub.isDayUse() ? BigDecimal.ZERO
                    : earlyLateCheckService.calculateLateCheckOutFee(sub, now);
            sub.recordCheckOut(now, lateFee);
            if (sub.getRoomNumberId() != null) {
                com.hola.hotel.entity.RoomNumber room = roomNumberRepository.findById(sub.getRoomNumberId()).orElse(null);
                if (room != null) {
                    room.checkOut();
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

        if (activeStatuses.contains("INHOUSE") || activeStatuses.contains("CHECK_IN")) return "INHOUSE";
        if (activeStatuses.stream().allMatch("CHECKED_OUT"::equals)) return "CHECKED_OUT";
        return "RESERVED";
    }

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
            if ("DIRTY".equals(hkStatus) || "PICKUP".equals(hkStatus)) {
                throw new HolaException(ErrorCode.FD_ROOM_NOT_CLEAN);
            }
        }
    }

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
            BigDecimal totalCharge = master.getSubReservations().stream()
                    .flatMap(sub -> dailyChargeRepository.findBySubReservationId(sub.getId()).stream())
                    .map(dc -> dc.getTotal() != null ? dc.getTotal() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (totalCharge.compareTo(BigDecimal.ZERO) > 0) {
                throw new HolaException(ErrorCode.CHECKOUT_OUTSTANDING_BALANCE);
            }
        }
    }

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

    private boolean isLastActiveLeg(SubReservation targetSub, MasterReservation master) {
        return master.getSubReservations().stream()
                .filter(s -> !s.getId().equals(targetSub.getId()))
                .filter(s -> !"CANCELED".equals(s.getRoomReservationStatus())
                          && !"CHECKED_OUT".equals(s.getRoomReservationStatus())
                          && !"NO_SHOW".equals(s.getRoomReservationStatus()))
                .findAny().isEmpty();
    }

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

    private BigDecimal getFirstNightTotal(Long masterReservationId) {
        ReservationPayment payment = reservationPaymentRepository
                .findByMasterReservationId(masterReservationId).orElse(null);
        if (payment != null && payment.getOriginalFirstNightTotal() != null
                && payment.getOriginalFirstNightTotal().compareTo(BigDecimal.ZERO) > 0) {
            return payment.getOriginalFirstNightTotal();
        }

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

    private BigDecimal getFirstNightTotalForSub(Long subReservationId) {
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

        int nights = charges.size();
        if (nights > 0) {
            List<ReservationServiceItem> services = serviceItemRepository.findBySubReservationId(subReservationId);
            BigDecimal upgradeTotal = services.stream()
                    .filter(s -> "PAID".equals(s.getServiceType()) && s.getServiceOptionId() == null)
                    .map(ReservationServiceItem::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (upgradeTotal.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal upgradePerNight = upgradeTotal.divide(
                        BigDecimal.valueOf(nights), 0, java.math.RoundingMode.HALF_UP);
                firstNight = firstNight.add(upgradePerNight);
            }
        }

        return firstNight;
    }

    private void processCancel(MasterReservation master, Long propertyId) {
        List<SubReservation> canceledLegs = master.getSubReservations().stream()
                .filter(s -> "CANCELED".equals(s.getRoomReservationStatus()))
                .toList();

        for (SubReservation leg : canceledLegs) {
            Long subId = leg.getId();
            boolean alreadyRefunded = paymentTransactionRepository
                    .findBySubReservationIdOrderByTransactionSeqAsc(subId).stream()
                    .anyMatch(t -> "REFUND".equals(t.getTransactionType()));
            if (alreadyRefunded) continue;

            processPartialLegCancelRefund(master, leg);
        }

        ReservationPayment payment = reservationPaymentRepository
                .findByMasterReservationId(master.getId()).orElse(null);
        if (payment != null) {
            payment.setPaymentStatusRefunded();
        }

        log.info("취소 처리(changeStatus 경로): {}, OTA={}, legs={}",
                master.getMasterReservationNo(), master.getIsOtaManaged(), canceledLegs.size());
    }

    private void processNoShow(MasterReservation master, Long propertyId) {
        List<SubReservation> noShowLegs = master.getSubReservations().stream()
                .filter(s -> "NO_SHOW".equals(s.getRoomReservationStatus()))
                .toList();

        for (SubReservation leg : noShowLegs) {
            Long subId = leg.getId();
            boolean alreadyRefunded = paymentTransactionRepository
                    .findBySubReservationIdOrderByTransactionSeqAsc(subId).stream()
                    .anyMatch(t -> "REFUND".equals(t.getTransactionType()));
            if (alreadyRefunded) continue;

            BigDecimal firstNightForSub = getFirstNightTotalForSub(subId);
            var cancelResult = cancellationPolicyService.calculateCancelFee(
                    propertyId, leg.getCheckIn(), firstNightForSub, true);

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

        ReservationPayment payment = reservationPaymentRepository
                .findByMasterReservationId(master.getId()).orElse(null);
        if (payment != null) {
            payment.setPaymentStatusRefunded();
        }

        log.info("노쇼 처리: {}, OTA={}, legs={}",
                master.getMasterReservationNo(), master.getIsOtaManaged(), noShowLegs.size());
    }

    private void processPartialLegCancelRefund(MasterReservation master, SubReservation canceledTarget) {
        paymentService.recalculatePayment(master.getId());

        SubReservation canceledLeg = canceledTarget;
        if (canceledLeg == null) {
            canceledLeg = master.getSubReservations().stream()
                    .filter(s -> "CANCELED".equals(s.getRoomReservationStatus()))
                    .reduce((first, second) -> second)
                    .orElse(null);
        }

        if (canceledLeg == null) return;

        Long subId = canceledLeg.getId();
        Long propertyId = master.getProperty().getId();

        List<PaymentTransaction> legPaymentTxns = paymentTransactionRepository
                .findBySubReservationIdOrderByTransactionSeqAsc(subId).stream()
                .filter(t -> "PAYMENT".equals(t.getTransactionType()))
                .toList();

        BigDecimal alreadyRefunded = paymentTransactionRepository
                .findBySubReservationIdOrderByTransactionSeqAsc(subId).stream()
                .filter(t -> "REFUND".equals(t.getTransactionType()))
                .map(PaymentTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal legPaid = legPaymentTxns.stream()
                .map(PaymentTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal refundable = legPaid.subtract(alreadyRefunded).max(BigDecimal.ZERO);

        if (refundable.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("부분 Leg 취소: {}의 환불 대상 결제건 없음", canceledLeg.getSubReservationNo());
            return;
        }

        BigDecimal firstNightForSub = getFirstNightTotalForSub(subId);
        var cancelResult = cancellationPolicyService.calculateCancelFee(
                propertyId, canceledLeg.getCheckIn(), firstNightForSub);
        BigDecimal cancelFee = cancelResult.feeAmount().min(refundable);
        BigDecimal refundAmt = refundable.subtract(cancelFee).max(BigDecimal.ZERO);

        String roomTypeLabel = "객실";
        if (canceledLeg.getRoomTypeId() != null) {
            roomTypeLabel = roomTypeRepository.findById(canceledLeg.getRoomTypeId())
                    .map(RoomType::getRoomTypeCode)
                    .orElse("객실");
        }
        String legLabel = "Leg #" + getLegIndex(master, canceledLeg) + " - " + roomTypeLabel;

        ReservationPayment payment = reservationPaymentRepository
                .findByMasterReservationId(master.getId()).orElse(null);
        if (payment != null) {
            payment.updateCancelRefund(cancelFee, refundAmt);
        }

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

    private int getLegIndex(MasterReservation master, SubReservation target) {
        List<SubReservation> subs = master.getSubReservations();
        for (int i = 0; i < subs.size(); i++) {
            if (subs.get(i).getId().equals(target.getId())) return i + 1;
        }
        return 0;
    }
}
