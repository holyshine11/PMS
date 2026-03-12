package com.hola.reservation.service;

import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.reservation.dto.request.PaymentAdjustmentRequest;
import com.hola.reservation.dto.response.PaymentAdjustmentResponse;
import com.hola.reservation.dto.response.PaymentSummaryResponse;
import com.hola.reservation.entity.*;
import com.hola.reservation.mapper.ReservationMapper;
import com.hola.reservation.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private final SubReservationRepository subReservationRepository;
    private final DailyChargeRepository dailyChargeRepository;
    private final ReservationServiceItemRepository serviceItemRepository;
    private final ReservationMapper reservationMapper;

    @Override
    public PaymentSummaryResponse getPaymentSummary(Long reservationId) {
        MasterReservation master = findMasterById(reservationId);

        ReservationPayment payment = paymentRepository
                .findByMasterReservationId(reservationId)
                .orElse(null);

        // 결제 정보가 없으면 빈 응답
        if (payment == null) {
            return PaymentSummaryResponse.builder()
                    .paymentStatus("PENDING")
                    .totalRoomAmount(BigDecimal.ZERO)
                    .totalServiceAmount(BigDecimal.ZERO)
                    .totalServiceChargeAmount(BigDecimal.ZERO)
                    .totalAdjustmentAmount(BigDecimal.ZERO)
                    .totalEarlyLateFee(BigDecimal.ZERO)
                    .grandTotal(BigDecimal.ZERO)
                    .build();
        }

        List<PaymentAdjustment> adjustments = adjustmentRepository
                .findByMasterReservationIdOrderByAdjustmentSeqAsc(reservationId);

        return reservationMapper.toPaymentSummaryResponse(payment, adjustments);
    }

    @Override
    @Transactional
    public PaymentSummaryResponse processPayment(Long reservationId) {
        MasterReservation master = findMasterById(reservationId);

        // OTA 결제 불가
        if (Boolean.TRUE.equals(master.getIsOtaManaged())) {
            throw new HolaException(ErrorCode.RESERVATION_OTA_EDIT_RESTRICTED);
        }

        ReservationPayment payment = getOrCreatePayment(master);

        if ("COMPLETED".equals(payment.getPaymentStatus())) {
            throw new HolaException(ErrorCode.RESERVATION_PAYMENT_ALREADY_COMPLETED);
        }

        // 금액 재계산 후 더미 결제 처리
        recalculateAmounts(payment, reservationId);
        payment.processPayment("CREDIT_CARD"); // 더미: 카드결제

        log.info("결제 처리 (더미): reservationId={}, 금액={}", reservationId, payment.getGrandTotal());

        List<PaymentAdjustment> adjustments = adjustmentRepository
                .findByMasterReservationIdOrderByAdjustmentSeqAsc(reservationId);
        return reservationMapper.toPaymentSummaryResponse(payment, adjustments);
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

        // COMPLETED 결제는 조정 불가
        ReservationPayment existingPayment = paymentRepository
                .findByMasterReservationId(reservationId).orElse(null);
        if (existingPayment != null && "COMPLETED".equals(existingPayment.getPaymentStatus())) {
            throw new HolaException(ErrorCode.RESERVATION_PAYMENT_ALREADY_COMPLETED);
        }

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

        // 완료된 결제는 재계산하지 않음
        if ("COMPLETED".equals(payment.getPaymentStatus())) {
            log.info("결제 완료 상태 — 재계산 스킵: reservationId={}", reservationId);
            return;
        }

        recalculateAmounts(payment, reservationId);
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
                                .paymentStatus("PENDING")
                                .totalRoomAmount(BigDecimal.ZERO)
                                .totalServiceAmount(BigDecimal.ZERO)
                                .totalServiceChargeAmount(BigDecimal.ZERO)
                                .totalAdjustmentAmount(BigDecimal.ZERO)
                                .grandTotal(BigDecimal.ZERO)
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

        payment.updateAmounts(totalRoom, totalService, totalServiceCharge, totalAdjustment, grandTotal);
        payment.updateEarlyLateFee(totalEarlyLateFee);
    }
}
