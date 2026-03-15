package com.hola.reservation.service;

import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.reservation.dto.request.PaymentAdjustmentRequest;
import com.hola.reservation.dto.request.PaymentProcessRequest;
import com.hola.reservation.dto.response.PaymentAdjustmentResponse;
import com.hola.reservation.dto.response.PaymentSummaryResponse;
import com.hola.reservation.entity.*;
import com.hola.reservation.mapper.ReservationMapper;
import com.hola.reservation.repository.*;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * 예약 결제 서비스 구현
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationPaymentServiceImpl implements ReservationPaymentService {

    private final MasterReservationRepository masterReservationRepository;
    private final ReservationPaymentRepository paymentRepository;
    private final PaymentAdjustmentRepository adjustmentRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final SubReservationRepository subReservationRepository;
    private final DailyChargeRepository dailyChargeRepository;
    private final ReservationServiceItemRepository serviceItemRepository;
    private final ReservationMapper reservationMapper;

    @Override
    public PaymentSummaryResponse getPaymentSummary(Long reservationId) {
        findMasterById(reservationId);

        ReservationPayment payment = paymentRepository
                .findByMasterReservationId(reservationId)
                .orElse(null);

        // 결제 정보가 없으면 빈 응답
        if (payment == null) {
            return PaymentSummaryResponse.builder()
                    .paymentStatus("UNPAID")
                    .totalRoomAmount(BigDecimal.ZERO)
                    .totalServiceAmount(BigDecimal.ZERO)
                    .totalServiceChargeAmount(BigDecimal.ZERO)
                    .totalAdjustmentAmount(BigDecimal.ZERO)
                    .totalEarlyLateFee(BigDecimal.ZERO)
                    .grandTotal(BigDecimal.ZERO)
                    .totalPaidAmount(BigDecimal.ZERO)
                    .remainingAmount(BigDecimal.ZERO)
                    .transactions(Collections.emptyList())
                    .build();
        }

        List<PaymentAdjustment> adjustments = adjustmentRepository
                .findByMasterReservationIdOrderByAdjustmentSeqAsc(reservationId);
        List<PaymentTransaction> transactions = transactionRepository
                .findByMasterReservationIdOrderByTransactionSeqAsc(reservationId);

        return reservationMapper.toPaymentSummaryResponse(payment, adjustments, transactions);
    }

    @Override
    @Transactional
    public PaymentSummaryResponse processPayment(Long reservationId, PaymentProcessRequest request) {
        MasterReservation master = findMasterById(reservationId);

        // 예약 상태 체크
        String reservationStatus = master.getReservationStatus();
        if ("CHECKED_OUT".equals(reservationStatus) || "CANCELED".equals(reservationStatus) || "NO_SHOW".equals(reservationStatus)) {
            throw new HolaException(ErrorCode.RESERVATION_MODIFY_NOT_ALLOWED);
        }

        ReservationPayment payment = getOrCreatePayment(master);

        // 이미 전액 결제 완료 또는 초과결제
        String paymentStatus = payment.getPaymentStatus();
        if ("PAID".equals(paymentStatus) || "OVERPAID".equals(paymentStatus)) {
            throw new HolaException(ErrorCode.RESERVATION_PAYMENT_ALREADY_COMPLETED);
        }

        // 금액 재계산 (grandTotal 최신화)
        recalculateAmounts(payment, reservationId);

        // 잔액 계산
        BigDecimal grandTotal = payment.getGrandTotal() != null ? payment.getGrandTotal() : BigDecimal.ZERO;
        BigDecimal totalPaid = payment.getTotalPaidAmount() != null ? payment.getTotalPaidAmount() : BigDecimal.ZERO;
        BigDecimal remaining = grandTotal.subtract(totalPaid);

        // 잔액이 0 이하면 결제 불필요
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            throw new HolaException(ErrorCode.RESERVATION_PAYMENT_ALREADY_COMPLETED);
        }

        // 결제 금액 결정
        BigDecimal payAmount;
        if (request.getAmount() == null) {
            payAmount = remaining; // 잔액 전액
        } else {
            payAmount = request.getAmount();
        }

        // 금액 검증
        if (payAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new HolaException(ErrorCode.RESERVATION_PAYMENT_AMOUNT_INVALID);
        }
        if (payAmount.compareTo(remaining) > 0) {
            throw new HolaException(ErrorCode.RESERVATION_PAYMENT_AMOUNT_EXCEEDED);
        }

        // 거래 시퀀스 번호 부여
        List<PaymentTransaction> existingTxns = transactionRepository
                .findByMasterReservationIdOrderByTransactionSeqAsc(reservationId);
        int nextSeq = existingTxns.isEmpty() ? 1 : existingTxns.get(existingTxns.size() - 1).getTransactionSeq() + 1;

        // PaymentTransaction 생성
        PaymentTransaction transaction = PaymentTransaction.builder()
                .masterReservationId(reservationId)
                .transactionSeq(nextSeq)
                .paymentMethod(request.getPaymentMethod())
                .amount(payAmount)
                .memo(request.getMemo())
                .build();
        transactionRepository.save(transaction);

        // 결제 누적 + 상태 자동 판단
        payment.addPaidAmount(payAmount);

        log.info("결제 처리: reservationId={}, method={}, amount={}, totalPaid={}",
                reservationId, request.getPaymentMethod(), payAmount, payment.getTotalPaidAmount());

        List<PaymentAdjustment> adjustments = adjustmentRepository
                .findByMasterReservationIdOrderByAdjustmentSeqAsc(reservationId);
        List<PaymentTransaction> transactions = transactionRepository
                .findByMasterReservationIdOrderByTransactionSeqAsc(reservationId);
        return reservationMapper.toPaymentSummaryResponse(payment, adjustments, transactions);
    }

    @Override
    @Transactional
    public PaymentAdjustmentResponse addAdjustment(Long reservationId, PaymentAdjustmentRequest request) {
        MasterReservation master = findMasterById(reservationId);

        // 완료/취소 상태 예약은 금액 조정 불가
        String status = master.getReservationStatus();
        if ("CHECKED_OUT".equals(status) || "CANCELED".equals(status) || "NO_SHOW".equals(status)) {
            throw new HolaException(ErrorCode.RESERVATION_PAYMENT_MODIFY_NOT_ALLOWED);
        }

        // PAID 결제도 조정 가능 (grandTotal 재계산 후 상태 재판단)
        ReservationPayment existingPayment = paymentRepository
                .findByMasterReservationId(reservationId).orElse(null);

        // 시퀀스 번호 자동 부여
        List<PaymentAdjustment> existing = adjustmentRepository
                .findByMasterReservationIdOrderByAdjustmentSeqAsc(reservationId);
        int nextSeq = existing.isEmpty() ? 1 : existing.get(existing.size() - 1).getAdjustmentSeq() + 1;

        PaymentAdjustment adjustment = PaymentAdjustment.builder()
                .masterReservationId(reservationId)
                .adjustmentSeq(nextSeq)
                .currency("KRW")
                .adjustmentSign(request.getAdjustmentSign())
                .supplyPrice(request.getSupplyPrice())
                .tax(request.getTax())
                .totalAmount(request.getTotalAmount())
                .comment(request.getComment())
                .build();

        adjustment = adjustmentRepository.save(adjustment);
        log.info("금액 조정 등록: reservationId={}, seq={}, {}{}",
                reservationId, nextSeq, request.getAdjustmentSign(), request.getTotalAmount());

        // 결제 금액 재계산
        recalculatePayment(reservationId);

        return reservationMapper.toPaymentAdjustmentResponse(adjustment);
    }

    @Override
    @Transactional
    public void recalculatePayment(Long reservationId) {
        MasterReservation master = findMasterById(reservationId);
        ReservationPayment payment = getOrCreatePayment(master);

        // grandTotal 재계산 (PAID 상태에서도 재계산하되 totalPaidAmount 유지)
        recalculateAmounts(payment, reservationId);

        // 결제 상태 재판단
        payment.updatePaymentStatus();
    }

    // ─── 내부 헬퍼 ──────────────────────────

    private MasterReservation findMasterById(Long id) {
        return masterReservationRepository.findById(id)
                .orElseThrow(() -> new HolaException(ErrorCode.RESERVATION_NOT_FOUND));
    }

    /**
     * 결제 정보 조회 또는 신규 생성
     */
    private ReservationPayment getOrCreatePayment(MasterReservation master) {
        return paymentRepository.findByMasterReservationId(master.getId())
                .orElseGet(() -> paymentRepository.save(
                        ReservationPayment.builder()
                                .masterReservation(master)
                                .paymentStatus("UNPAID")
                                .totalRoomAmount(BigDecimal.ZERO)
                                .totalServiceAmount(BigDecimal.ZERO)
                                .totalServiceChargeAmount(BigDecimal.ZERO)
                                .totalAdjustmentAmount(BigDecimal.ZERO)
                                .grandTotal(BigDecimal.ZERO)
                                .totalPaidAmount(BigDecimal.ZERO)
                                .build()
                ));
    }

    /**
     * 금액 재계산
     */
    private void recalculateAmounts(ReservationPayment payment, Long reservationId) {
        // 1. 객실 요금 합계 (모든 서브의 DailyCharge 합산) + 얼리/레이트 요금
        List<SubReservation> subs = subReservationRepository.findByMasterReservationId(reservationId);
        BigDecimal totalRoom = BigDecimal.ZERO;
        BigDecimal totalServiceCharge = BigDecimal.ZERO;
        BigDecimal totalEarlyLateFee = BigDecimal.ZERO;

        for (SubReservation sub : subs) {
            if ("CANCELED".equals(sub.getRoomReservationStatus())) continue;
            List<DailyCharge> charges = dailyChargeRepository.findBySubReservationId(sub.getId());
            for (DailyCharge charge : charges) {
                totalRoom = totalRoom.add(charge.getSupplyPrice()).add(charge.getTax());
                totalServiceCharge = totalServiceCharge.add(charge.getServiceCharge());
            }
            // 얼리 체크인 / 레이트 체크아웃 요금 합산
            if (sub.getEarlyCheckInFee() != null) {
                totalEarlyLateFee = totalEarlyLateFee.add(sub.getEarlyCheckInFee());
            }
            if (sub.getLateCheckOutFee() != null) {
                totalEarlyLateFee = totalEarlyLateFee.add(sub.getLateCheckOutFee());
            }
        }

        // 2. 서비스 요금 합계
        BigDecimal totalService = BigDecimal.ZERO;
        for (SubReservation sub : subs) {
            if ("CANCELED".equals(sub.getRoomReservationStatus())) continue;
            List<ReservationServiceItem> services = serviceItemRepository.findBySubReservationId(sub.getId());
            for (ReservationServiceItem svc : services) {
                totalService = totalService.add(svc.getTotalPrice());
            }
        }

        // 3. 조정 금액 합계
        BigDecimal totalAdjustment = BigDecimal.ZERO;
        List<PaymentAdjustment> adjustments = adjustmentRepository
                .findByMasterReservationIdOrderByAdjustmentSeqAsc(reservationId);
        for (PaymentAdjustment adj : adjustments) {
            if ("+".equals(adj.getAdjustmentSign())) {
                totalAdjustment = totalAdjustment.add(adj.getTotalAmount());
            } else {
                totalAdjustment = totalAdjustment.subtract(adj.getTotalAmount());
            }
        }

        // 4. 최종 합계 (얼리/레이트 요금 포함)
        BigDecimal grandTotal = totalRoom.add(totalService).add(totalServiceCharge)
                .add(totalAdjustment).add(totalEarlyLateFee);

        // 음수 하한 적용
        if (grandTotal.compareTo(BigDecimal.ZERO) < 0) {
            grandTotal = BigDecimal.ZERO;
        }

        payment.updateAmounts(totalRoom, totalService, totalServiceCharge, totalAdjustment, grandTotal);
        payment.updateEarlyLateFee(totalEarlyLateFee);
    }
}
