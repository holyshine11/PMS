package com.hola.reservation.service;

import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.reservation.booking.gateway.CancelPaymentRequest;
import com.hola.reservation.booking.gateway.PaymentGateway;
import com.hola.reservation.booking.gateway.PaymentResult;
import com.hola.reservation.dto.request.PaymentAdjustmentRequest;
import com.hola.reservation.dto.request.PaymentProcessRequest;
import com.hola.reservation.dto.request.VanResultPayload;
import com.hola.reservation.dto.response.LegPaymentInfo;
import com.hola.reservation.dto.response.PaymentAdjustmentResponse;
import com.hola.reservation.dto.response.PaymentSummaryResponse;
import com.hola.reservation.dto.response.VanCancelInfoResponse;
import com.hola.reservation.entity.*;
import com.hola.reservation.mapper.ReservationMapper;
import com.hola.reservation.repository.*;
import com.hola.hotel.entity.Workstation;
import com.hola.hotel.service.WorkstationService;
import com.hola.room.entity.RoomType;
import com.hola.room.repository.RoomTypeRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

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
    private final RoomTypeRepository roomTypeRepository;
    private final ReservationMapper reservationMapper;
    private final PaymentGateway paymentGateway;
    private final WorkstationService workstationService;
    private final ObjectMapper objectMapper;
    private final ReservationChangeLogService changeLogService;

    @Override
    public PaymentSummaryResponse getPaymentSummary(Long propertyId, Long reservationId) {
        MasterReservation master = findMasterById(reservationId);
        validateReservationProperty(master, propertyId);

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

        List<LegPaymentInfo> legPayments = calculatePerLegPayments(reservationId);
        return reservationMapper.toPaymentSummaryResponse(payment, adjustments, transactions, legPayments);
    }

    @Override
    @Transactional
    public PaymentSummaryResponse processPayment(Long propertyId, Long reservationId, PaymentProcessRequest request) {
        MasterReservation master = findMasterById(reservationId);
        validateReservationProperty(master, propertyId);

        // 예약 상태 체크
        String reservationStatus = master.getReservationStatus();
        if ("CHECKED_OUT".equals(reservationStatus) || "CANCELED".equals(reservationStatus) || "NO_SHOW".equals(reservationStatus)) {
            throw new HolaException(ErrorCode.RESERVATION_MODIFY_NOT_ALLOWED);
        }

        ReservationPayment payment = getOrCreatePayment(master);

        // 금액 재계산 (grandTotal 최신화) — stale paymentStatus 체크 전에 먼저 재계산하여
        // 룸 업그레이드/서비스 추가 후 grandTotal이 증가한 경우를 정확히 반영
        recalculateAmounts(payment, reservationId);

        // 완납/과납 상태 사전 검증
        String paymentStatus = payment.getPaymentStatus();
        if ("PAID".equals(paymentStatus) || "OVERPAID".equals(paymentStatus)) {
            throw new HolaException(ErrorCode.RESERVATION_PAYMENT_ALREADY_COMPLETED);
        }

        // 잔액 계산: Leg 지정 시 해당 Leg 잔액, 아니면 Master 잔액
        BigDecimal remaining;
        if (request.getSubReservationId() != null) {
            // Per-Leg 결제: 해당 Leg의 잔액으로 검증
            List<LegPaymentInfo> legPayments = calculatePerLegPayments(reservationId);
            remaining = legPayments.stream()
                    .filter(lp -> lp.getSubReservationId().equals(request.getSubReservationId()))
                    .map(LegPaymentInfo::getLegRemaining)
                    .findFirst().orElse(BigDecimal.ZERO);
        } else {
            // 전체 결제: Master 잔액 (processPaymentWithPgResult와 동일 공식)
            BigDecimal grandTotal = payment.getGrandTotal() != null ? payment.getGrandTotal() : BigDecimal.ZERO;
            BigDecimal totalPaid = payment.getTotalPaidAmount() != null ? payment.getTotalPaidAmount() : BigDecimal.ZERO;
            BigDecimal refund = payment.getRefundAmount() != null ? payment.getRefundAmount() : BigDecimal.ZERO;
            BigDecimal cancelFee = payment.getCancelFeeAmount() != null ? payment.getCancelFeeAmount() : BigDecimal.ZERO;
            BigDecimal netPaid = totalPaid.subtract(refund).subtract(cancelFee);
            remaining = grandTotal.subtract(netPaid);
        }

        // 잔액이 0 이하면 결제 불필요
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            throw new HolaException(ErrorCode.RESERVATION_PAYMENT_ALREADY_COMPLETED);
        }

        // 결제 금액 결정
        BigDecimal payAmount;
        if (request.getAmount() == null) {
            payAmount = remaining;
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

        // subReservationId 결정: 요청에 없으면 싱글레그 자동 귀속
        Long subReservationId = request.getSubReservationId();
        if (subReservationId == null) {
            List<SubReservation> subs = subReservationRepository.findByMasterReservationId(reservationId);
            if (subs.size() == 1) {
                subReservationId = subs.get(0).getId();
            }
        }

        // PaymentTransaction 생성 (subReservationId 포함)
        PaymentTransaction.PaymentTransactionBuilder txnBuilder = PaymentTransaction.builder()
                .masterReservationId(reservationId)
                .subReservationId(subReservationId)
                .transactionSeq(nextSeq)
                .paymentMethod(request.getPaymentMethod())
                .amount(payAmount)
                .memo(request.getMemo());

        // VAN 결제 시 VAN 필드 매핑
        if ("VAN".equals(request.getPaymentChannel()) && request.getVanResult() != null) {
            VanResultPayload van = request.getVanResult();
            if (!"0000".equals(van.getRespCode())) {
                throw new HolaException(ErrorCode.VAN_PAYMENT_FAILED);
            }
            // VAN 승인 금액 교차 검증
            if (van.getTransAmount() != null && van.getTransAmount() != payAmount.longValue()) {
                log.error("VAN 금액 불일치: 요청={}, 승인={}, reservationId={}", payAmount, van.getTransAmount(), reservationId);
                throw new HolaException(ErrorCode.VAN_PAYMENT_AMOUNT_MISMATCH);
            }
            // KPSP 원본 응답 전체를 저장 (VanResultPayload에 매핑되지 않은 필드 포함)
            String rawResponse = request.getVanRawJson() != null ? request.getVanRawJson() : toJson(van);
            txnBuilder
                    .paymentChannel("VAN")
                    .workstationId(request.getWorkstationId())
                    .vanProvider("KPSP")
                    .vanAuthCode(van.getAuthCode())
                    .vanRrn(van.getRrn())
                    .vanPan(van.getPan())
                    .vanIssuerCode(van.getIssuerCode())
                    .vanIssuerName(van.getIssuerName())
                    .vanAcquirerCode(van.getAcquirerCode())
                    .vanAcquirerName(van.getAcquirerName())
                    .vanTerminalId(van.getTerminalId())
                    .vanSequenceNo(van.getSequenceNo())
                    .vanRawResponse(rawResponse)
                    .approvalNo(van.getAuthCode());
        }

        try {
            transactionRepository.save(txnBuilder.build());
            payment.addPaidAmount(payAmount);
        } catch (OptimisticLockException | ObjectOptimisticLockingFailureException e) {
            throw new HolaException(ErrorCode.RESERVATION_PAYMENT_CONCURRENT_CONFLICT);
        }

        String channel = request.getPaymentChannel() != null ? request.getPaymentChannel() : "MANUAL";
        log.info("결제 처리: reservationId={}, subReservationId={}, channel={}, method={}, amount={}, totalPaid={}",
                reservationId, subReservationId, channel, request.getPaymentMethod(), payAmount, payment.getTotalPaidAmount());
        logPaymentChange(reservationId, subReservationId, "PAYMENT",
                request.getPaymentMethod(), channel, payAmount);

        List<PaymentAdjustment> adjustments = adjustmentRepository
                .findByMasterReservationIdOrderByAdjustmentSeqAsc(reservationId);
        List<PaymentTransaction> transactions = transactionRepository
                .findByMasterReservationIdOrderByTransactionSeqAsc(reservationId);
        List<LegPaymentInfo> legPayments = calculatePerLegPayments(reservationId);
        return reservationMapper.toPaymentSummaryResponse(payment, adjustments, transactions, legPayments);
    }

    @Override
    @Transactional
    public PaymentSummaryResponse processPaymentWithPgResult(Long propertyId, Long reservationId,
                                                              PaymentProcessRequest request, PaymentResult pgResult) {
        MasterReservation master = findMasterById(reservationId);
        validateReservationProperty(master, propertyId);

        String reservationStatus = master.getReservationStatus();
        if ("CHECKED_OUT".equals(reservationStatus) || "CANCELED".equals(reservationStatus) || "NO_SHOW".equals(reservationStatus)) {
            throw new HolaException(ErrorCode.RESERVATION_MODIFY_NOT_ALLOWED);
        }

        ReservationPayment payment = getOrCreatePayment(master);

        String paymentStatus = payment.getPaymentStatus();
        if ("PAID".equals(paymentStatus) || "OVERPAID".equals(paymentStatus)) {
            throw new HolaException(ErrorCode.RESERVATION_PAYMENT_ALREADY_COMPLETED);
        }

        recalculateAmounts(payment, reservationId);

        BigDecimal grandTotal = payment.getGrandTotal() != null ? payment.getGrandTotal() : BigDecimal.ZERO;
        BigDecimal totalPaid = payment.getTotalPaidAmount() != null ? payment.getTotalPaidAmount() : BigDecimal.ZERO;
        BigDecimal refund = payment.getRefundAmount() != null ? payment.getRefundAmount() : BigDecimal.ZERO;
        BigDecimal cancelFee = payment.getCancelFeeAmount() != null ? payment.getCancelFeeAmount() : BigDecimal.ZERO;
        BigDecimal netPaid = totalPaid.subtract(refund).subtract(cancelFee);
        BigDecimal remaining = grandTotal.subtract(netPaid);

        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            throw new HolaException(ErrorCode.RESERVATION_PAYMENT_ALREADY_COMPLETED);
        }

        BigDecimal payAmount = request.getAmount() != null ? request.getAmount() : remaining;

        if (payAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new HolaException(ErrorCode.RESERVATION_PAYMENT_AMOUNT_INVALID);
        }
        if (payAmount.compareTo(remaining) > 0) {
            throw new HolaException(ErrorCode.RESERVATION_PAYMENT_AMOUNT_EXCEEDED);
        }

        List<PaymentTransaction> existingTxns = transactionRepository
                .findByMasterReservationIdOrderByTransactionSeqAsc(reservationId);
        int nextSeq = existingTxns.isEmpty() ? 1 : existingTxns.get(existingTxns.size() - 1).getTransactionSeq() + 1;

        // subReservationId 결정: 요청에 없으면 싱글레그 자동 귀속
        Long subReservationId = request.getSubReservationId();
        if (subReservationId == null) {
            List<SubReservation> subs = subReservationRepository.findByMasterReservationId(reservationId);
            if (subs.size() == 1) {
                subReservationId = subs.get(0).getId();
            }
        }

        // PG 결과 포함 PaymentTransaction 생성 (subReservationId 포함)
        PaymentTransaction.PaymentTransactionBuilder txnBuilder = PaymentTransaction.builder()
                .masterReservationId(reservationId)
                .subReservationId(subReservationId)
                .transactionSeq(nextSeq)
                .paymentMethod(request.getPaymentMethod())
                .amount(payAmount)
                .memo(request.getMemo());

        // PG 필드 매핑
        if (pgResult != null) {
            txnBuilder
                    .pgProvider(pgResult.getPgProvider() != null ? pgResult.getPgProvider() : pgResult.getGatewayId())
                    .pgCno(pgResult.getPgCno())
                    .pgTransactionId(pgResult.getPgTransactionId())
                    .pgStatusCode(pgResult.getPgStatusCode())
                    .pgApprovalNo(pgResult.getPgApprovalNo())
                    .pgApprovalDate(pgResult.getPgApprovalDate())
                    .pgCardNo(pgResult.getPgCardNo())
                    .pgIssuerName(pgResult.getPgIssuerName())
                    .pgAcquirerName(pgResult.getPgAcquirerName())
                    .pgInstallmentMonth(pgResult.getInstallmentMonth())
                    .pgCardType(pgResult.getPgCardType())
                    .pgRawResponse(pgResult.getPgRawResponse());

            // approvalNo도 PG 승인번호로 설정
            if (pgResult.getApprovalNo() != null) {
                txnBuilder.approvalNo(pgResult.getApprovalNo());
            }
        }

        try {
            transactionRepository.save(txnBuilder.build());
            payment.addPaidAmount(payAmount);
        } catch (OptimisticLockException | ObjectOptimisticLockingFailureException e) {
            throw new HolaException(ErrorCode.RESERVATION_PAYMENT_CONCURRENT_CONFLICT);
        }

        log.info("PG 결제 처리: reservationId={}, subReservationId={}, pgProvider={}, pgCno={}, amount={}",
                reservationId, request.getSubReservationId(),
                pgResult != null ? pgResult.getPgProvider() : "N/A",
                pgResult != null ? pgResult.getPgCno() : "N/A",
                payAmount);
        logPaymentChange(reservationId, request.getSubReservationId(), "PAYMENT",
                request.getPaymentMethod(), "PG", payAmount);

        List<PaymentAdjustment> adjustments = adjustmentRepository
                .findByMasterReservationIdOrderByAdjustmentSeqAsc(reservationId);
        List<PaymentTransaction> transactions = transactionRepository
                .findByMasterReservationIdOrderByTransactionSeqAsc(reservationId);
        List<LegPaymentInfo> legPayments = calculatePerLegPayments(reservationId);
        return reservationMapper.toPaymentSummaryResponse(payment, adjustments, transactions, legPayments);
    }

    @Override
    @Transactional
    public PaymentAdjustmentResponse addAdjustment(Long propertyId, Long reservationId, PaymentAdjustmentRequest request) {
        MasterReservation master = findMasterById(reservationId);
        validateReservationProperty(master, propertyId);

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
        try {
            String sign = "+".equals(request.getAdjustmentSign()) ? "추가" : "할인";
            String desc = "금액 조정 " + sign + ": " + request.getTotalAmount().stripTrailingZeros().toPlainString() + "원"
                    + (request.getComment() != null ? " (" + request.getComment() + ")" : "");
            changeLogService.log(reservationId, null, "PAYMENT", "UPDATE",
                    "adjustment", null, request.getAdjustmentSign() + request.getTotalAmount().stripTrailingZeros().toPlainString(), desc);
        } catch (Exception e) {
            log.error("변경이력 기록 실패: {}", e.getMessage());
        }

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

        // 최초 예약 시점 1박 총액 보존 (null일 때 1회만 설정 — 업그레이드 후에도 불변)
        if (payment.getOriginalFirstNightTotal() == null) {
            BigDecimal firstNightTotal = getFirstNightTotalFromCharges(reservationId);
            payment.saveOriginalFirstNightTotal(firstNightTotal);
        }

        // 결제 상태 재판단
        payment.updatePaymentStatus();
    }

    // ─── PG 환불 처리 ──────────────────────────

    @Override
    @Transactional
    public PaymentTransaction processRefundWithPg(Long masterReservationId, BigDecimal refundAmount,
                                                   BigDecimal cancelFee, String memo) {
        // 둘 다 0이면 기록할 내용 없음
        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0 && cancelFee.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        List<PaymentTransaction> existingTxns = transactionRepository
                .findByMasterReservationIdOrderByTransactionSeqAsc(masterReservationId);

        // 싱글레그 자동 귀속: sub가 1개면 환불 거래에 subReservationId 자동 할당
        Long autoSubId = null;
        List<SubReservation> subs = subReservationRepository.findByMasterReservationId(masterReservationId);
        if (subs.size() == 1) {
            autoSubId = subs.get(0).getId();
        }

        // 환불 금액이 0이면 (수수료 전액 차감 케이스) 기록만
        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            int nextSeq = getNextSeq(existingTxns);
            PaymentTransaction feeTxn = PaymentTransaction.builder()
                    .masterReservationId(masterReservationId)
                    .subReservationId(autoSubId)
                    .transactionSeq(nextSeq)
                    .transactionType("REFUND")
                    .paymentMethod("CARD")
                    .amount(BigDecimal.ZERO)
                    .transactionStatus("COMPLETED")
                    .memo(memo)
                    .build();
            return transactionRepository.save(feeTxn);
        }

        // ── 결제수단별 환불 분배 ──
        // PG 결제 거래와 비-PG(현금/카드VAN) 결제 거래 분리
        List<PaymentTransaction> pgPayments = existingTxns.stream()
                .filter(t -> "PAYMENT".equals(t.getTransactionType()) && t.getPgCno() != null)
                .toList();
        List<PaymentTransaction> nonPgPayments = existingTxns.stream()
                .filter(t -> "PAYMENT".equals(t.getTransactionType()) && t.getPgCno() == null)
                .toList();

        // 기존 PG 환불 누적액 (COMPLETED된 REFUND 중 PG 필드가 있는 것)
        BigDecimal alreadyPgRefunded = existingTxns.stream()
                .filter(t -> "REFUND".equals(t.getTransactionType())
                        && "COMPLETED".equals(t.getTransactionStatus())
                        && t.getPgProvider() != null)
                .map(PaymentTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // PG 환불 가능액 = PG 결제 총액 - 기환불액
        BigDecimal totalPgPaid = pgPayments.stream()
                .map(PaymentTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pgRefundable = totalPgPaid.subtract(alreadyPgRefunded).max(BigDecimal.ZERO);

        // 분배: PG에서 가능한 만큼, 나머지는 비-PG(현금/카드) 환불
        BigDecimal pgRefundAmount = refundAmount.min(pgRefundable);
        BigDecimal nonPgRefundAmount = refundAmount.subtract(pgRefundAmount);

        PaymentTransaction firstResult = null;
        int seq = getNextSeq(existingTxns);

        // 1) PG 환불 처리
        if (pgRefundAmount.compareTo(BigDecimal.ZERO) > 0 && !pgPayments.isEmpty()) {
            PaymentTransaction originalPgTxn = pgPayments.get(0);
            boolean isFull = cancelFee.compareTo(BigDecimal.ZERO) == 0
                    && nonPgRefundAmount.compareTo(BigDecimal.ZERO) <= 0;
            BigDecimal remainAmount = totalPgPaid.subtract(alreadyPgRefunded);

            CancelPaymentRequest cancelRequest = CancelPaymentRequest.builder()
                    .pgCno(originalPgTxn.getPgCno())
                    .cancelType(isFull ? "FULL" : "PARTIAL")
                    .cancelAmount(pgRefundAmount)
                    .remainAmount(remainAmount)
                    .reason(memo)
                    .build();

            PaymentTransaction.PaymentTransactionBuilder pgRefundBuilder = PaymentTransaction.builder()
                    .masterReservationId(masterReservationId)
                    .subReservationId(autoSubId)
                    .transactionSeq(seq++)
                    .transactionType("REFUND")
                    .paymentMethod("CARD")
                    .amount(pgRefundAmount)
                    .memo(memo);

            try {
                PaymentResult result = paymentGateway.cancelPayment(cancelRequest);
                if (result.isSuccess()) {
                    pgRefundBuilder
                            .transactionStatus("COMPLETED")
                            .pgProvider(result.getPgProvider())
                            .pgCno(result.getPgCno())
                            .pgTransactionId(result.getPgTransactionId())
                            .pgStatusCode(result.getPgStatusCode())
                            .pgApprovalNo(result.getApprovalNo())
                            .pgCardNo(originalPgTxn.getPgCardNo())
                            .pgIssuerName(originalPgTxn.getPgIssuerName())
                            .pgAcquirerName(originalPgTxn.getPgAcquirerName())
                            .pgInstallmentMonth(originalPgTxn.getPgInstallmentMonth())
                            .pgCardType(originalPgTxn.getPgCardType());
                    log.info("PG 환불 성공: masterReservationId={}, pgCno={}, cancelPgCno={}, amount={}",
                            masterReservationId, originalPgTxn.getPgCno(), result.getPgCno(), pgRefundAmount);
                } else {
                    log.error("PG 환불 실패: masterReservationId={}, pgCno={}, error={}",
                            masterReservationId, originalPgTxn.getPgCno(), result.getErrorMessage());
                    pgRefundBuilder
                            .transactionStatus("PG_REFUND_FAILED")
                            .pgProvider(originalPgTxn.getPgProvider())
                            .pgCno(originalPgTxn.getPgCno())
                            .pgCardNo(originalPgTxn.getPgCardNo())
                            .pgIssuerName(originalPgTxn.getPgIssuerName())
                            .memo(memo + " [PG실패: " + result.getErrorMessage() + "]");
                }
            } catch (Exception e) {
                log.error("PG 환불 통신 오류: masterReservationId={}, pgCno={}",
                        masterReservationId, originalPgTxn.getPgCno(), e);
                pgRefundBuilder
                        .transactionStatus("PG_REFUND_FAILED")
                        .pgProvider(originalPgTxn.getPgProvider())
                        .pgCno(originalPgTxn.getPgCno())
                        .pgCardNo(originalPgTxn.getPgCardNo())
                        .pgIssuerName(originalPgTxn.getPgIssuerName())
                        .memo(memo + " [PG통신오류: " + e.getMessage() + "]");
            }

            firstResult = transactionRepository.save(pgRefundBuilder.build());
        }

        // 2) 비-PG 환불 처리 (현금/카드VAN — 관리자 수동 환불 확인)
        if (nonPgRefundAmount.compareTo(BigDecimal.ZERO) > 0) {
            // 비-PG 결제 수단/채널 판별 (최신 비-PG 결제 기준)
            PaymentTransaction lastNonPg = nonPgPayments.isEmpty() ? null
                    : nonPgPayments.get(nonPgPayments.size() - 1);
            String nonPgMethod = lastNonPg != null ? lastNonPg.getPaymentMethod() : "CASH";
            String nonPgChannel = lastNonPg != null ? lastNonPg.getPaymentChannel() : null;
            String methodLabel = "CASH".equals(nonPgMethod)
                    ? ("VAN".equals(nonPgChannel) ? "현금(VAN)" : "현금")
                    : ("VAN".equals(nonPgChannel) ? "카드(VAN)" : "카드");

            PaymentTransaction nonPgRefund = PaymentTransaction.builder()
                    .masterReservationId(masterReservationId)
                    .subReservationId(autoSubId)
                    .transactionSeq(seq)
                    .transactionType("REFUND")
                    .paymentMethod(nonPgMethod)
                    .paymentChannel(nonPgChannel)
                    .amount(nonPgRefundAmount)
                    .transactionStatus("MANUAL_CONFIRMED")
                    .memo(memo + " [" + methodLabel + " 환불 -- 관리자 확인 완료]")
                    .build();
            PaymentTransaction saved = transactionRepository.save(nonPgRefund);

            log.info("비-PG 환불 기록(수동확인): masterReservationId={}, method={}, amount={}",
                    masterReservationId, nonPgMethod, nonPgRefundAmount);

            if (firstResult == null) firstResult = saved;
        }

        // 3) PG 결제가 없고 비-PG 결제도 없는 경우 (방어 코드)
        if (firstResult == null) {
            PaymentTransaction fallback = PaymentTransaction.builder()
                    .masterReservationId(masterReservationId)
                    .subReservationId(autoSubId)
                    .transactionSeq(seq)
                    .transactionType("REFUND")
                    .paymentMethod("CASH")
                    .amount(refundAmount)
                    .transactionStatus("MANUAL_CONFIRMED")
                    .memo(memo + " [현금 환불 -- 관리자 확인 완료]")
                    .build();
            firstResult = transactionRepository.save(fallback);
        }

        return firstResult;
    }

    private int getNextSeq(List<PaymentTransaction> txns) {
        return txns.isEmpty() ? 1 : txns.get(txns.size() - 1).getTransactionSeq() + 1;
    }

    @Override
    @Transactional
    public PaymentSummaryResponse retryPgRefund(Long propertyId, Long reservationId, Long transactionId) {
        MasterReservation master = findMasterById(reservationId);
        validateReservationProperty(master, propertyId);

        PaymentTransaction failedTxn = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new HolaException(ErrorCode.RESERVATION_NOT_FOUND));

        // 소속 검증
        if (!failedTxn.getMasterReservationId().equals(reservationId)) {
            throw new HolaException(ErrorCode.RESERVATION_NOT_FOUND);
        }
        // 상태 검증: PG_REFUND_FAILED만 재시도 가능
        if (!"PG_REFUND_FAILED".equals(failedTxn.getTransactionStatus())) {
            throw new HolaException(ErrorCode.RESERVATION_MODIFY_NOT_ALLOWED);
        }

        // 원본 PG 거래번호 (실패 시 원본 pgCno가 저장됨)
        String originalPgCno = failedTxn.getPgCno();
        if (originalPgCno == null) {
            throw new HolaException(ErrorCode.PG_CANCEL_FAILED);
        }

        // 원결제금액 조회 (KICC remainAmount = 취소 가능 잔액 = 원결제금액)
        ReservationPayment payment = paymentRepository
                .findByMasterReservationId(reservationId).orElse(null);
        BigDecimal cancelFee = (payment != null && payment.getCancelFeeAmount() != null)
                ? payment.getCancelFeeAmount() : BigDecimal.ZERO;
        boolean isFull = cancelFee.compareTo(BigDecimal.ZERO) == 0;

        // 원본 PAYMENT 트랜잭션에서 원결제금액 조회
        List<PaymentTransaction> allTxns = transactionRepository
                .findByMasterReservationIdOrderByTransactionSeqAsc(reservationId);
        BigDecimal originalPaidAmount = allTxns.stream()
                .filter(t -> "PAYMENT".equals(t.getTransactionType()) && t.getPgCno() != null)
                .map(PaymentTransaction::getAmount)
                .findFirst().orElse(failedTxn.getAmount());

        CancelPaymentRequest cancelRequest = CancelPaymentRequest.builder()
                .pgCno(originalPgCno)
                .cancelType(isFull ? "FULL" : "PARTIAL")
                .cancelAmount(failedTxn.getAmount())
                .remainAmount(originalPaidAmount)
                .reason("PG 환불 재시도")
                .build();

        try {
            PaymentResult result = paymentGateway.cancelPayment(cancelRequest);
            if (result.isSuccess()) {
                failedTxn.updatePgRefundResult("COMPLETED", result.getPgCno(),
                        result.getApprovalNo(), result.getPgStatusCode(), result.getPgTransactionId());
                log.info("PG 환불 재시도 성공: txnId={}, cancelPgCno={}", transactionId, result.getPgCno());
            } else {
                failedTxn.updateMemo("PG 환불 재시도 실패: " + result.getErrorMessage());
                log.error("PG 환불 재시도 실패: txnId={}, error={}", transactionId, result.getErrorMessage());
            }
        } catch (Exception e) {
            failedTxn.updateMemo("PG 환불 재시도 통신 오류: " + e.getMessage());
            log.error("PG 환불 재시도 통신 오류: txnId={}", transactionId, e);
        }

        // 결제 정보 재조회하여 반환
        List<PaymentAdjustment> adjustments = adjustmentRepository
                .findByMasterReservationIdOrderByAdjustmentSeqAsc(reservationId);
        List<PaymentTransaction> transactions = transactionRepository
                .findByMasterReservationIdOrderByTransactionSeqAsc(reservationId);
        List<LegPaymentInfo> legPayments = calculatePerLegPayments(reservationId);
        return reservationMapper.toPaymentSummaryResponse(payment, adjustments, transactions, legPayments);
    }

    // ─── 개별 PG 결제 건 취소 ──────────────────────────

    @Override
    @Transactional
    public PaymentSummaryResponse cancelPgTransaction(Long propertyId, Long reservationId, Long transactionId) {
        MasterReservation master = findMasterById(reservationId);
        validateReservationProperty(master, propertyId);

        PaymentTransaction targetTxn = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new HolaException(ErrorCode.PG_CANCEL_NOT_ALLOWED));

        // 소속 검증
        if (!targetTxn.getMasterReservationId().equals(reservationId)) {
            throw new HolaException(ErrorCode.PG_CANCEL_NOT_ALLOWED);
        }
        // PAYMENT + PG 거래인지 검증
        if (!"PAYMENT".equals(targetTxn.getTransactionType()) || targetTxn.getPgCno() == null) {
            throw new HolaException(ErrorCode.PG_CANCEL_NOT_ALLOWED);
        }

        // 이미 이 pgCno에 대한 환불이 있는지 확인
        List<PaymentTransaction> allTxns = transactionRepository
                .findByMasterReservationIdOrderByTransactionSeqAsc(reservationId);
        BigDecimal totalPgPaid = allTxns.stream()
                .filter(t -> "PAYMENT".equals(t.getTransactionType()) && t.getPgCno() != null)
                .map(PaymentTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal alreadyPgRefunded = allTxns.stream()
                .filter(t -> "REFUND".equals(t.getTransactionType())
                        && "COMPLETED".equals(t.getTransactionStatus())
                        && t.getPgProvider() != null)
                .map(PaymentTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pgRefundable = totalPgPaid.subtract(alreadyPgRefunded).max(BigDecimal.ZERO);

        if (pgRefundable.compareTo(BigDecimal.ZERO) <= 0) {
            throw new HolaException(ErrorCode.PG_ALREADY_CANCELLED);
        }

        // 취소 금액 = 이 결제 건의 금액과 PG 환불 가능액 중 작은 값
        BigDecimal cancelAmount = targetTxn.getAmount().min(pgRefundable);
        boolean isFull = alreadyPgRefunded.compareTo(BigDecimal.ZERO) == 0
                && cancelAmount.compareTo(totalPgPaid) >= 0;

        CancelPaymentRequest cancelRequest = CancelPaymentRequest.builder()
                .pgCno(targetTxn.getPgCno())
                .cancelType(isFull ? "FULL" : "PARTIAL")
                .cancelAmount(cancelAmount)
                .remainAmount(totalPgPaid.subtract(alreadyPgRefunded))
                .reason("프론트데스크 PG 결제 취소")
                .build();

        int nextSeq = getNextSeq(allTxns);
        PaymentTransaction.PaymentTransactionBuilder refundBuilder = PaymentTransaction.builder()
                .masterReservationId(reservationId)
                .subReservationId(targetTxn.getSubReservationId())
                .transactionSeq(nextSeq)
                .transactionType("REFUND")
                .paymentMethod("CARD")
                .amount(cancelAmount)
                .paymentChannel("PG")
                .memo("프론트데스크 PG 결제 취소");

        try {
            PaymentResult result = paymentGateway.cancelPayment(cancelRequest);
            if (result.isSuccess()) {
                refundBuilder
                        .transactionStatus("COMPLETED")
                        .pgProvider(result.getPgProvider())
                        .pgCno(result.getPgCno())
                        .pgTransactionId(result.getPgTransactionId())
                        .pgStatusCode(result.getPgStatusCode())
                        .pgApprovalNo(result.getApprovalNo())
                        .pgCardNo(targetTxn.getPgCardNo())
                        .pgIssuerName(targetTxn.getPgIssuerName())
                        .pgAcquirerName(targetTxn.getPgAcquirerName())
                        .pgInstallmentMonth(targetTxn.getPgInstallmentMonth())
                        .pgCardType(targetTxn.getPgCardType());
                log.info("PG 개별 취소 성공: reservationId={}, txnId={}, pgCno={}, cancelPgCno={}, amount={}",
                        reservationId, transactionId, targetTxn.getPgCno(), result.getPgCno(), cancelAmount);
            } else {
                log.error("PG 개별 취소 실패: reservationId={}, txnId={}, error={}",
                        reservationId, transactionId, result.getErrorMessage());
                refundBuilder
                        .transactionStatus("PG_REFUND_FAILED")
                        .pgProvider(targetTxn.getPgProvider())
                        .pgCno(targetTxn.getPgCno())
                        .pgCardNo(targetTxn.getPgCardNo())
                        .pgIssuerName(targetTxn.getPgIssuerName())
                        .memo("프론트데스크 PG 결제 취소 [PG실패: " + result.getErrorMessage() + "]");
            }
        } catch (Exception e) {
            log.error("PG 개별 취소 통신 오류: reservationId={}, txnId={}", reservationId, transactionId, e);
            refundBuilder
                    .transactionStatus("PG_REFUND_FAILED")
                    .pgProvider(targetTxn.getPgProvider())
                    .pgCno(targetTxn.getPgCno())
                    .pgCardNo(targetTxn.getPgCardNo())
                    .pgIssuerName(targetTxn.getPgIssuerName())
                    .memo("프론트데스크 PG 결제 취소 [PG통신오류: " + e.getMessage() + "]");
        }

        PaymentTransaction refundTxn = transactionRepository.save(refundBuilder.build());

        // 성공 시 refundAmount 갱신 (VAN cancel 패턴과 동일)
        if ("COMPLETED".equals(refundTxn.getTransactionStatus())) {
            ReservationPayment pmtForUpdate = paymentRepository
                    .findByMasterReservationId(reservationId).orElse(null);
            if (pmtForUpdate != null) {
                pmtForUpdate.updateCancelRefund(BigDecimal.ZERO, cancelAmount);
            }
            logPaymentChange(reservationId, targetTxn.getSubReservationId(), "REFUND",
                    targetTxn.getPaymentMethod(), "PG", cancelAmount);
        }

        // 결제 금액 재계산
        recalculatePayment(reservationId);

        // 결제 정보 재조회
        ReservationPayment payment = paymentRepository.findByMasterReservationId(reservationId).orElse(null);
        List<PaymentAdjustment> adjustments = adjustmentRepository
                .findByMasterReservationIdOrderByAdjustmentSeqAsc(reservationId);
        List<PaymentTransaction> transactions = transactionRepository
                .findByMasterReservationIdOrderByTransactionSeqAsc(reservationId);
        List<LegPaymentInfo> legPayments = calculatePerLegPayments(reservationId);
        return reservationMapper.toPaymentSummaryResponse(payment, adjustments, transactions, legPayments);
    }

    // ─── Leg 단위 환불 처리 ──────────────────────────

    @Override
    @Transactional
    public PaymentTransaction processRefundForLeg(Long masterReservationId, Long subReservationId,
                                                   BigDecimal refundAmount, BigDecimal cancelFee, String memo) {
        // 둘 다 0이면 기록할 내용 없음
        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0 && cancelFee.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        List<PaymentTransaction> existingTxns = transactionRepository
                .findByMasterReservationIdOrderByTransactionSeqAsc(masterReservationId);

        // 환불 금액이 0이면 (수수료 전액 차감 케이스) 기록만
        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            int nextSeq = getNextSeq(existingTxns);
            PaymentTransaction feeTxn = PaymentTransaction.builder()
                    .masterReservationId(masterReservationId)
                    .subReservationId(subReservationId)
                    .transactionSeq(nextSeq)
                    .transactionType("REFUND")
                    .paymentMethod("CARD")
                    .amount(BigDecimal.ZERO)
                    .transactionStatus("COMPLETED")
                    .memo(memo)
                    .build();
            return transactionRepository.save(feeTxn);
        }

        // ── 해당 Leg의 결제 거래만 대상으로 환불 분배 ──
        List<PaymentTransaction> legPayments = existingTxns.stream()
                .filter(t -> "PAYMENT".equals(t.getTransactionType())
                        && subReservationId.equals(t.getSubReservationId()))
                .toList();

        List<PaymentTransaction> legPgPayments = legPayments.stream()
                .filter(t -> t.getPgCno() != null)
                .toList();
        List<PaymentTransaction> legNonPgPayments = legPayments.stream()
                .filter(t -> t.getPgCno() == null)
                .toList();

        // 기존 해당 Leg의 PG 환불 누적액
        BigDecimal alreadyPgRefunded = existingTxns.stream()
                .filter(t -> "REFUND".equals(t.getTransactionType())
                        && "COMPLETED".equals(t.getTransactionStatus())
                        && t.getPgProvider() != null
                        && subReservationId.equals(t.getSubReservationId()))
                .map(PaymentTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPgPaid = legPgPayments.stream()
                .map(PaymentTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pgRefundable = totalPgPaid.subtract(alreadyPgRefunded).max(BigDecimal.ZERO);

        BigDecimal pgRefundAmount = refundAmount.min(pgRefundable);
        BigDecimal nonPgRefundAmount = refundAmount.subtract(pgRefundAmount);

        PaymentTransaction firstResult = null;
        int seq = getNextSeq(existingTxns);

        // 1) PG 환불 처리
        if (pgRefundAmount.compareTo(BigDecimal.ZERO) > 0 && !legPgPayments.isEmpty()) {
            PaymentTransaction originalPgTxn = legPgPayments.get(0);
            boolean isFull = cancelFee.compareTo(BigDecimal.ZERO) == 0
                    && nonPgRefundAmount.compareTo(BigDecimal.ZERO) <= 0;
            BigDecimal remainAmount = totalPgPaid.subtract(alreadyPgRefunded);

            CancelPaymentRequest cancelRequest = CancelPaymentRequest.builder()
                    .pgCno(originalPgTxn.getPgCno())
                    .cancelType(isFull ? "FULL" : "PARTIAL")
                    .cancelAmount(pgRefundAmount)
                    .remainAmount(remainAmount)
                    .reason(memo)
                    .build();

            PaymentTransaction.PaymentTransactionBuilder pgRefundBuilder = PaymentTransaction.builder()
                    .masterReservationId(masterReservationId)
                    .subReservationId(subReservationId)
                    .transactionSeq(seq++)
                    .transactionType("REFUND")
                    .paymentMethod("CARD")
                    .amount(pgRefundAmount)
                    .memo(memo);

            try {
                PaymentResult result = paymentGateway.cancelPayment(cancelRequest);
                if (result.isSuccess()) {
                    pgRefundBuilder
                            .transactionStatus("COMPLETED")
                            .pgProvider(result.getPgProvider())
                            .pgCno(result.getPgCno())
                            .pgTransactionId(result.getPgTransactionId())
                            .pgStatusCode(result.getPgStatusCode())
                            .pgApprovalNo(result.getApprovalNo())
                            .pgCardNo(originalPgTxn.getPgCardNo())
                            .pgIssuerName(originalPgTxn.getPgIssuerName())
                            .pgAcquirerName(originalPgTxn.getPgAcquirerName())
                            .pgInstallmentMonth(originalPgTxn.getPgInstallmentMonth())
                            .pgCardType(originalPgTxn.getPgCardType());
                    log.info("Leg PG 환불 성공: masterReservationId={}, subReservationId={}, pgCno={}, amount={}",
                            masterReservationId, subReservationId, originalPgTxn.getPgCno(), pgRefundAmount);
                } else {
                    log.error("Leg PG 환불 실패: masterReservationId={}, subReservationId={}, pgCno={}, error={}",
                            masterReservationId, subReservationId, originalPgTxn.getPgCno(), result.getErrorMessage());
                    pgRefundBuilder
                            .transactionStatus("PG_REFUND_FAILED")
                            .pgProvider(originalPgTxn.getPgProvider())
                            .pgCno(originalPgTxn.getPgCno())
                            .pgCardNo(originalPgTxn.getPgCardNo())
                            .pgIssuerName(originalPgTxn.getPgIssuerName())
                            .memo(memo + " [PG실패: " + result.getErrorMessage() + "]");
                }
            } catch (Exception e) {
                log.error("Leg PG 환불 통신 오류: masterReservationId={}, subReservationId={}, pgCno={}",
                        masterReservationId, subReservationId, originalPgTxn.getPgCno(), e);
                pgRefundBuilder
                        .transactionStatus("PG_REFUND_FAILED")
                        .pgProvider(originalPgTxn.getPgProvider())
                        .pgCno(originalPgTxn.getPgCno())
                        .pgCardNo(originalPgTxn.getPgCardNo())
                        .pgIssuerName(originalPgTxn.getPgIssuerName())
                        .memo(memo + " [PG통신오류: " + e.getMessage() + "]");
            }

            firstResult = transactionRepository.save(pgRefundBuilder.build());
        }

        // 2) 비-PG 환불 처리 (현금/카드VAN — 관리자 수동 환불 확인)
        if (nonPgRefundAmount.compareTo(BigDecimal.ZERO) > 0) {
            PaymentTransaction lastNonPg = legNonPgPayments.isEmpty() ? null
                    : legNonPgPayments.get(legNonPgPayments.size() - 1);
            String nonPgMethod = lastNonPg != null ? lastNonPg.getPaymentMethod() : "CASH";
            String nonPgChannel = lastNonPg != null ? lastNonPg.getPaymentChannel() : null;
            String methodLabel = "CASH".equals(nonPgMethod)
                    ? ("VAN".equals(nonPgChannel) ? "현금(VAN)" : "현금")
                    : ("VAN".equals(nonPgChannel) ? "카드(VAN)" : "카드");

            PaymentTransaction nonPgRefund = PaymentTransaction.builder()
                    .masterReservationId(masterReservationId)
                    .subReservationId(subReservationId)
                    .transactionSeq(seq)
                    .transactionType("REFUND")
                    .paymentMethod(nonPgMethod)
                    .paymentChannel(nonPgChannel)
                    .amount(nonPgRefundAmount)
                    .transactionStatus("MANUAL_CONFIRMED")
                    .memo(memo + " [" + methodLabel + " 환불 -- 관리자 확인 완료]")
                    .build();
            PaymentTransaction saved = transactionRepository.save(nonPgRefund);

            log.info("Leg 비-PG 환불 기록(수동확인): masterReservationId={}, subReservationId={}, method={}, amount={}",
                    masterReservationId, subReservationId, nonPgMethod, nonPgRefundAmount);

            if (firstResult == null) firstResult = saved;
        }

        // 3) 결제건이 없는 경우 (방어 코드)
        if (firstResult == null) {
            PaymentTransaction fallback = PaymentTransaction.builder()
                    .masterReservationId(masterReservationId)
                    .subReservationId(subReservationId)
                    .transactionSeq(seq)
                    .transactionType("REFUND")
                    .paymentMethod("CASH")
                    .amount(refundAmount)
                    .transactionStatus("MANUAL_CONFIRMED")
                    .memo(memo + " [현금 환불 -- 관리자 확인 완료]")
                    .build();
            firstResult = transactionRepository.save(fallback);
        }

        return firstResult;
    }

    // ─── Leg별 결제 현황 계산 ──────────────────────────

    @Override
    public List<LegPaymentInfo> calculatePerLegPayments(Long masterReservationId) {
        List<SubReservation> subs = subReservationRepository.findByMasterReservationId(masterReservationId);
        if (subs.isEmpty()) return Collections.emptyList();

        List<PaymentTransaction> allTxns = transactionRepository
                .findByMasterReservationIdOrderByTransactionSeqAsc(masterReservationId);

        // RoomType 이름 일괄 조회
        Set<Long> roomTypeIds = subs.stream()
                .map(SubReservation::getRoomTypeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> roomTypeNames = roomTypeRepository.findAllById(roomTypeIds).stream()
                .collect(Collectors.toMap(RoomType::getId, RoomType::getRoomTypeCode));

        // sub_reservation_id가 NULL인 거래를 귀속할 대상 Leg 결정
        // 싱글레그: 유일한 Leg, 멀티레그: 첫 번째 활성(비취소) Leg
        Long unassignedTargetSubId = subs.size() == 1
                ? subs.get(0).getId()
                : subs.stream()
                    .filter(s -> !"CANCELED".equals(s.getRoomReservationStatus()))
                    .map(SubReservation::getId)
                    .findFirst().orElse(subs.get(0).getId());

        List<LegPaymentInfo> result = new ArrayList<>();

        for (SubReservation sub : subs) {
            Long subId = sub.getId();

            // Leg 요금 합계 (DailyCharge + services + serviceCharge)
            List<DailyCharge> charges = dailyChargeRepository.findBySubReservationId(subId);
            BigDecimal legRoom = BigDecimal.ZERO;
            BigDecimal legSvcChg = BigDecimal.ZERO;
            for (DailyCharge charge : charges) {
                legRoom = legRoom.add(charge.getSupplyPrice()).add(charge.getTax());
                legSvcChg = legSvcChg.add(charge.getServiceCharge());
            }
            // 얼리/레이트 요금
            BigDecimal earlyLateFee = BigDecimal.ZERO;
            if (sub.getEarlyCheckInFee() != null) earlyLateFee = earlyLateFee.add(sub.getEarlyCheckInFee());
            if (sub.getLateCheckOutFee() != null) earlyLateFee = earlyLateFee.add(sub.getLateCheckOutFee());

            BigDecimal legService = BigDecimal.ZERO;
            List<ReservationServiceItem> services = serviceItemRepository.findBySubReservationId(subId);
            for (ReservationServiceItem svc : services) {
                legService = legService.add(svc.getTotalPrice());
            }

            BigDecimal legTotal = legRoom.add(legSvcChg).add(legService).add(earlyLateFee);

            // 해당 Leg에 귀속된 결제 합계 (sub_reservation_id 일치 + NULL 귀속분)
            final Long targetId = subId;
            BigDecimal legPaid = allTxns.stream()
                    .filter(t -> "PAYMENT".equals(t.getTransactionType())
                            && (targetId.equals(t.getSubReservationId())
                                || (t.getSubReservationId() == null && targetId.equals(unassignedTargetSubId))))
                    .map(PaymentTransaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 해당 Leg에 귀속된 환불 합계
            BigDecimal legRefunded = allTxns.stream()
                    .filter(t -> "REFUND".equals(t.getTransactionType())
                            && (targetId.equals(t.getSubReservationId())
                                || (t.getSubReservationId() == null && targetId.equals(unassignedTargetSubId))))
                    .map(PaymentTransaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal legRemaining = legTotal.subtract(legPaid.subtract(legRefunded)).max(BigDecimal.ZERO);

            String roomTypeName = sub.getRoomTypeId() != null ? roomTypeNames.get(sub.getRoomTypeId()) : null;

            result.add(LegPaymentInfo.builder()
                    .subReservationId(subId)
                    .subReservationNo(sub.getSubReservationNo())
                    .roomTypeName(roomTypeName)
                    .roomReservationStatus(sub.getRoomReservationStatus())
                    .legTotal(legTotal)
                    .legPaid(legPaid)
                    .legRefunded(legRefunded)
                    .legRemaining(legRemaining)
                    .build());
        }

        return result;
    }

    // ─── 내부 헬퍼 ──────────────────────────

    private MasterReservation findMasterById(Long id) {
        return masterReservationRepository.findById(id)
                .orElseThrow(() -> new HolaException(ErrorCode.RESERVATION_NOT_FOUND));
    }

    /**
     * 예약이 해당 프로퍼티 소속인지 검증 (인가 우회 방지)
     */
    private void validateReservationProperty(MasterReservation master, Long propertyId) {
        if (!master.getProperty().getId().equals(propertyId)) {
            throw new HolaException(ErrorCode.RESERVATION_NOT_FOUND);
        }
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

    /**
     * 첫 번째 Leg의 1박 총액 조회 (DailyCharge 기반)
     */
    private BigDecimal getFirstNightTotalFromCharges(Long reservationId) {
        List<SubReservation> subs = subReservationRepository.findByMasterReservationId(reservationId);
        if (subs.isEmpty()) return BigDecimal.ZERO;

        List<DailyCharge> charges = dailyChargeRepository.findBySubReservationId(subs.get(0).getId());
        if (charges.isEmpty()) return BigDecimal.ZERO;

        DailyCharge first = charges.get(0);
        if (first.getTotal() != null) return first.getTotal();

        BigDecimal supply = first.getSupplyPrice() != null ? first.getSupplyPrice() : BigDecimal.ZERO;
        BigDecimal tax = first.getTax() != null ? first.getTax() : BigDecimal.ZERO;
        BigDecimal svc = first.getServiceCharge() != null ? first.getServiceCharge() : BigDecimal.ZERO;
        return supply.add(tax).add(svc);
    }

    // === VAN 결제 관련 메서드 ===

    @Override
    public VanCancelInfoResponse getVanCancelInfo(Long propertyId, Long reservationId, Long transactionId) {
        MasterReservation master = findMasterById(reservationId);
        validateReservationProperty(master, propertyId);

        PaymentTransaction txn = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new HolaException(ErrorCode.VAN_TRANSACTION_NOT_FOUND));

        if (!"VAN".equals(txn.getPaymentChannel())) {
            throw new HolaException(ErrorCode.VAN_CANCEL_NOT_ALLOWED);
        }
        if (!txn.getMasterReservationId().equals(reservationId)) {
            throw new HolaException(ErrorCode.VAN_TRANSACTION_NOT_FOUND);
        }

        // 이미 취소된 거래인지 확인 (같은 vanSequenceNo로 REFUND가 존재하면 취소 완료)
        List<PaymentTransaction> allTxns = transactionRepository
                .findByMasterReservationIdOrderByTransactionSeqAsc(reservationId);
        boolean alreadyCancelled = allTxns.stream()
                .anyMatch(t -> "REFUND".equals(t.getTransactionType())
                        && txn.getVanSequenceNo() != null
                        && txn.getVanSequenceNo().equals(t.getVanSequenceNo()));
        if (alreadyCancelled) {
            throw new HolaException(ErrorCode.VAN_CANCEL_ALREADY_DONE);
        }

        // 워크스테이션 정보 조회
        String wsNo = null;
        String kpspHost = "localhost";
        int kpspPort = 19090;
        if (txn.getWorkstationId() != null) {
            Workstation ws = workstationService.findById(txn.getWorkstationId());
            wsNo = ws.getWsNo();
            kpspHost = ws.getKpspHost();
            kpspPort = ws.getKpspPort();
        }

        return VanCancelInfoResponse.builder()
                .transactionId(txn.getId())
                .workstationId(txn.getWorkstationId())
                .authCode(txn.getVanAuthCode())
                .rrn(txn.getVanRrn())
                .amount(txn.getAmount())
                .sequenceNo(txn.getVanSequenceNo())
                .paymentMethod(txn.getPaymentMethod())
                .wsNo(wsNo)
                .kpspHost(kpspHost)
                .kpspPort(kpspPort)
                .build();
    }

    @Override
    @Transactional
    public PaymentSummaryResponse processVanCancel(Long propertyId, Long reservationId,
                                                     Long transactionId, VanResultPayload cancelResult) {
        MasterReservation master = findMasterById(reservationId);
        validateReservationProperty(master, propertyId);

        PaymentTransaction originalTxn = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new HolaException(ErrorCode.VAN_TRANSACTION_NOT_FOUND));

        if (!"VAN".equals(originalTxn.getPaymentChannel())) {
            throw new HolaException(ErrorCode.VAN_CANCEL_NOT_ALLOWED);
        }
        if (!originalTxn.getMasterReservationId().equals(reservationId)) {
            throw new HolaException(ErrorCode.VAN_TRANSACTION_NOT_FOUND);
        }

        // VAN 취소 결과 검증
        if (cancelResult == null || !"0000".equals(cancelResult.getRespCode())) {
            throw new HolaException(ErrorCode.VAN_PAYMENT_FAILED);
        }

        ReservationPayment payment = paymentRepository
                .findByMasterReservationId(reservationId)
                .orElseThrow(() -> new HolaException(ErrorCode.RESERVATION_PAYMENT_ALREADY_COMPLETED));

        // 거래 시퀀스 번호
        List<PaymentTransaction> existingTxns = transactionRepository
                .findByMasterReservationIdOrderByTransactionSeqAsc(reservationId);
        int nextSeq = existingTxns.isEmpty() ? 1 : existingTxns.get(existingTxns.size() - 1).getTransactionSeq() + 1;

        // REFUND 트랜잭션 생성
        PaymentTransaction refundTxn = PaymentTransaction.builder()
                .masterReservationId(reservationId)
                .subReservationId(originalTxn.getSubReservationId())
                .transactionSeq(nextSeq)
                .transactionType("REFUND")
                .paymentMethod(originalTxn.getPaymentMethod())
                .amount(originalTxn.getAmount())
                .paymentChannel("VAN")
                .workstationId(originalTxn.getWorkstationId())
                .vanProvider("KPSP")
                .vanAuthCode(cancelResult.getAuthCode())
                .vanRrn(cancelResult.getRrn())
                .vanPan(cancelResult.getPan())
                .vanIssuerCode(cancelResult.getIssuerCode())
                .vanIssuerName(cancelResult.getIssuerName())
                .vanAcquirerCode(cancelResult.getAcquirerCode())
                .vanAcquirerName(cancelResult.getAcquirerName())
                .vanTerminalId(cancelResult.getTerminalId())
                .vanSequenceNo(originalTxn.getVanSequenceNo())
                .vanRawResponse(toJson(cancelResult))
                .approvalNo(cancelResult.getAuthCode())
                .memo("VAN 취소 (원거래 #" + originalTxn.getTransactionSeq() + ")")
                .build();
        transactionRepository.save(refundTxn);

        // 환불 금액 누적
        payment.updateCancelRefund(BigDecimal.ZERO, originalTxn.getAmount());

        log.info("VAN 취소 처리: reservationId={}, originalTxnId={}, amount={}, transType={}",
                reservationId, transactionId, originalTxn.getAmount(), cancelResult.getTransType());
        logPaymentChange(reservationId, originalTxn.getSubReservationId(), "REFUND",
                originalTxn.getPaymentMethod(), "VAN", originalTxn.getAmount());

        List<PaymentAdjustment> adjustments = adjustmentRepository
                .findByMasterReservationIdOrderByAdjustmentSeqAsc(reservationId);
        List<PaymentTransaction> transactions = transactionRepository
                .findByMasterReservationIdOrderByTransactionSeqAsc(reservationId);
        List<LegPaymentInfo> legPayments = calculatePerLegPayments(reservationId);
        return reservationMapper.toPaymentSummaryResponse(payment, adjustments, transactions, legPayments);
    }

    @Override
    @Transactional
    public PaymentSummaryResponse processVanCancelManual(Long propertyId, Long reservationId,
                                                           Long transactionId, VanResultPayload manualPayload) {
        MasterReservation master = findMasterById(reservationId);
        validateReservationProperty(master, propertyId);

        PaymentTransaction originalTxn = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new HolaException(ErrorCode.VAN_TRANSACTION_NOT_FOUND));

        if (!"VAN".equals(originalTxn.getPaymentChannel())) {
            throw new HolaException(ErrorCode.VAN_CANCEL_NOT_ALLOWED);
        }
        if (!originalTxn.getMasterReservationId().equals(reservationId)) {
            throw new HolaException(ErrorCode.VAN_TRANSACTION_NOT_FOUND);
        }

        // 이미 취소된 거래인지 확인
        List<PaymentTransaction> allTxns = transactionRepository
                .findByMasterReservationIdOrderByTransactionSeqAsc(reservationId);
        boolean alreadyCancelled = allTxns.stream()
                .anyMatch(t -> "REFUND".equals(t.getTransactionType())
                        && originalTxn.getVanSequenceNo() != null
                        && originalTxn.getVanSequenceNo().equals(t.getVanSequenceNo()));
        if (alreadyCancelled) {
            throw new HolaException(ErrorCode.VAN_CANCEL_ALREADY_DONE);
        }

        ReservationPayment payment = paymentRepository
                .findByMasterReservationId(reservationId)
                .orElseThrow(() -> new HolaException(ErrorCode.RESERVATION_PAYMENT_ALREADY_COMPLETED));

        int nextSeq = allTxns.isEmpty() ? 1 : allTxns.get(allTxns.size() - 1).getTransactionSeq() + 1;

        // MANUAL_CONFIRMED REFUND 트랜잭션 생성
        PaymentTransaction refundTxn = PaymentTransaction.builder()
                .masterReservationId(reservationId)
                .subReservationId(originalTxn.getSubReservationId())
                .transactionSeq(nextSeq)
                .transactionType("REFUND")
                .paymentMethod(originalTxn.getPaymentMethod())
                .amount(originalTxn.getAmount())
                .paymentChannel("VAN")
                .workstationId(originalTxn.getWorkstationId())
                .vanProvider("KPSP")
                .vanSequenceNo(originalTxn.getVanSequenceNo())
                .transactionStatus("MANUAL_CONFIRMED")
                .approvalNo(manualPayload != null ? manualPayload.getAuthCode() : null)
                .memo("VAN 취소 수동 확인 (원거래 #" + originalTxn.getTransactionSeq() + ")")
                .build();
        transactionRepository.save(refundTxn);

        payment.updateCancelRefund(BigDecimal.ZERO, originalTxn.getAmount());

        log.info("VAN 취소 수동 확인: reservationId={}, originalTxnId={}, amount={}",
                reservationId, transactionId, originalTxn.getAmount());

        List<PaymentAdjustment> adjustments = adjustmentRepository
                .findByMasterReservationIdOrderByAdjustmentSeqAsc(reservationId);
        List<PaymentTransaction> transactions = transactionRepository
                .findByMasterReservationIdOrderByTransactionSeqAsc(reservationId);
        List<LegPaymentInfo> legPayments = calculatePerLegPayments(reservationId);
        return reservationMapper.toPaymentSummaryResponse(payment, adjustments, transactions, legPayments);
    }

    @Override
    @Transactional
    public String generateVanSequenceNo(Long workstationId) {
        Workstation ws = workstationService.findById(workstationId);
        String datePart = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String wsNoPart = String.format("%-2s", ws.getWsNo()).substring(0, Math.min(2, ws.getWsNo().length()));

        // DB 시퀀스로 유일한 번호 생성
        Long seqVal = transactionRepository.getNextVanSequence();
        String seqPart = String.format("%04d", seqVal % 10000);

        return datePart + wsNoPart + seqPart;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("VAN 응답 JSON 직렬화 실패", e);
            return "{}";
        }
    }

    /**
     * 결제/환불 변경이력 기록 헬퍼
     */
    private void logPaymentChange(Long reservationId, Long subReservationId,
                                   String txnType, String method, String channel, BigDecimal amount) {
        try {
            String methodLabel = "CARD".equals(method) ? "카드" : "CASH".equals(method) ? "현금" : method;
            String channelLabel = channel != null ? " (" + channel + ")" : "";
            boolean isRefund = "REFUND".equals(txnType);
            String desc = (isRefund ? "환불" : "결제") + ": " + methodLabel + channelLabel
                    + " " + amount.stripTrailingZeros().toPlainString() + "원";
            changeLogService.log(reservationId, subReservationId, "PAYMENT",
                    isRefund ? "REFUND" : "PAYMENT", "paymentTransaction",
                    null, amount.stripTrailingZeros().toPlainString(), desc);
        } catch (Exception e) {
            log.error("결제 변경이력 기록 실패: {}", e.getMessage());
        }
    }
}
