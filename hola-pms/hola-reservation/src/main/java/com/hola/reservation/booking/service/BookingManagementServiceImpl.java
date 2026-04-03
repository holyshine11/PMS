package com.hola.reservation.booking.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.hotel.entity.Property;
import com.hola.rate.entity.RateCode;
import com.hola.rate.entity.DayUseRate;
import com.hola.rate.repository.DayUseRateRepository;
import com.hola.rate.repository.RateCodeRepository;
import com.hola.reservation.booking.dto.request.BookingModifyRequest;
import com.hola.reservation.booking.dto.response.*;
import com.hola.reservation.booking.entity.BookingAuditLog;
import com.hola.reservation.booking.repository.BookingAuditLogRepository;
import com.hola.reservation.entity.DailyCharge;
import com.hola.reservation.entity.MasterReservation;
import com.hola.reservation.entity.ReservationServiceItem;
import com.hola.reservation.entity.SubReservation;
import com.hola.reservation.repository.DailyChargeRepository;
import com.hola.reservation.repository.MasterReservationRepository;
import com.hola.reservation.repository.ReservationServiceItemRepository;
import com.hola.reservation.repository.SubReservationRepository;
import com.hola.reservation.service.PriceCalculationService;
import com.hola.reservation.service.ReservationPaymentService;
import com.hola.reservation.service.RoomAvailabilityService;
import com.hola.room.repository.PaidServiceOptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 부킹엔진 예약 관리 서비스 구현
 * - 취소 수수료 조회, 예약 취소, 예약 조회, 예약 수정
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingManagementServiceImpl implements BookingManagementService {

    private final BookingHelper helper;
    private final MasterReservationRepository masterReservationRepository;
    private final SubReservationRepository subReservationRepository;
    private final DailyChargeRepository dailyChargeRepository;
    private final ReservationPaymentService reservationPaymentService;
    private final CancellationPolicyService cancellationPolicyService;
    private final RoomAvailabilityService roomAvailabilityService;
    private final PriceCalculationService priceCalculationService;
    private final RateCodeRepository rateCodeRepository;
    private final DayUseRateRepository dayUseRateRepository;
    private final ReservationServiceItemRepository reservationServiceItemRepository;
    private final PaidServiceOptionRepository paidServiceOptionRepository;
    private final BookingAuditLogRepository bookingAuditLogRepository;
    private final ObjectMapper objectMapper;
    private final com.hola.reservation.repository.ReservationPaymentRepository reservationPaymentRepository;
    private final com.hola.reservation.repository.PaymentTransactionRepository paymentTransactionRepository;
    private final com.hola.room.service.InventoryService inventoryService;

    @Override
    public CancelFeePreviewResponse getCancelFeePreview(String confirmationNo, String email) {
        MasterReservation master = findAndVerifyReservation(confirmationNo, email);

        // 취소 가능 상태 확인
        String status = master.getReservationStatus();
        if ("CANCELED".equals(status)) {
            throw new HolaException(ErrorCode.BOOKING_ALREADY_CANCELED);
        }
        if (!"RESERVED".equals(status) && !"CHECK_IN".equals(status)) {
            throw new HolaException(ErrorCode.BOOKING_CANCEL_NOT_ALLOWED);
        }

        // 1박 요금 조회
        BigDecimal firstNightAmount = getFirstNightTotal(master.getId());

        // 취소 수수료 계산
        Long propertyId = master.getProperty().getId();
        var cancelResult = cancellationPolicyService.calculateCancelFee(
                propertyId, master.getMasterCheckIn(), firstNightAmount);

        // 총 결제액 조회
        BigDecimal totalPaid = BigDecimal.ZERO;
        var payment = reservationPaymentRepository.findByMasterReservationId(master.getId()).orElse(null);
        if (payment != null) {
            totalPaid = payment.getTotalPaidAmount() != null ? payment.getTotalPaidAmount() : BigDecimal.ZERO;
        }
        BigDecimal refundAmt = totalPaid.subtract(cancelResult.feeAmount()).max(BigDecimal.ZERO);

        // PG 결제 정보 조회
        boolean isPgPayment = false;
        String pgCardNo = null;
        String pgIssuerName = null;
        var txns = paymentTransactionRepository
                .findByMasterReservationIdOrderByTransactionSeqAsc(master.getId());
        var pgTxn = txns.stream()
                .filter(t -> "PAYMENT".equals(t.getTransactionType()) && t.getPgCno() != null)
                .findFirst().orElse(null);
        if (pgTxn != null) {
            isPgPayment = true;
            pgCardNo = pgTxn.getPgCardNo();
            pgIssuerName = pgTxn.getPgIssuerName();
        }

        return CancelFeePreviewResponse.builder()
                .confirmationNo(confirmationNo)
                .reservationStatus(status)
                .guestNameKo(master.getGuestNameKo())
                .checkIn(master.getMasterCheckIn().toString())
                .checkOut(master.getMasterCheckOut().toString())
                .firstNightAmount(firstNightAmount)
                .cancelFeeAmount(cancelResult.feeAmount())
                .cancelFeePercent(cancelResult.feePercent())
                .totalPaidAmount(totalPaid)
                .refundAmount(refundAmt)
                .policyDescription(cancelResult.policyDescription())
                .pgPayment(isPgPayment)
                .pgCardNo(pgCardNo)
                .pgIssuerName(pgIssuerName)
                .build();
    }

    @Override
    @Transactional
    public CancelBookingResponse cancelBooking(String confirmationNo, String email,
                                                String clientIp, String userAgent) {
        MasterReservation master = findAndVerifyReservation(confirmationNo, email);

        // 취소 가능 상태 확인
        String status = master.getReservationStatus();
        if ("CANCELED".equals(status)) {
            throw new HolaException(ErrorCode.BOOKING_ALREADY_CANCELED);
        }
        if (!"RESERVED".equals(status) && !"CHECK_IN".equals(status)) {
            throw new HolaException(ErrorCode.BOOKING_CANCEL_NOT_ALLOWED);
        }

        // 취소 수수료 계산
        BigDecimal firstNightAmount = getFirstNightTotal(master.getId());
        Long propertyId = master.getProperty().getId();
        var cancelResult = cancellationPolicyService.calculateCancelFee(
                propertyId, master.getMasterCheckIn(), firstNightAmount);

        // 결제 정보 업데이트
        BigDecimal cancelFee = cancelResult.feeAmount();
        BigDecimal refundAmt = BigDecimal.ZERO;
        Boolean pgRefundSuccess = null;
        String pgRefundApprovalNo = null;
        var payment = reservationPaymentRepository.findByMasterReservationId(master.getId()).orElse(null);
        if (payment != null) {
            BigDecimal totalPaid = payment.getTotalPaidAmount() != null ? payment.getTotalPaidAmount() : BigDecimal.ZERO;
            BigDecimal existingRefund = payment.getRefundAmount() != null ? payment.getRefundAmount() : BigDecimal.ZERO;
            BigDecimal existingCancelFee = payment.getCancelFeeAmount() != null ? payment.getCancelFeeAmount() : BigDecimal.ZERO;
            BigDecimal netPaid = totalPaid.subtract(existingRefund).subtract(existingCancelFee);
            refundAmt = netPaid.subtract(cancelFee).max(BigDecimal.ZERO);
            payment.updateCancelRefund(cancelFee, refundAmt);

            // PG 환불 포함 REFUND 거래 기록
            if (refundAmt.compareTo(BigDecimal.ZERO) > 0 || cancelFee.compareTo(BigDecimal.ZERO) > 0) {
                var refundTxn = reservationPaymentService.processRefundWithPg(
                        master.getId(), refundAmt, cancelFee,
                        "게스트 자가 취소 환불 (수수료: " + cancelFee + "원)");
                // PG 환불 결과 저장 (응답 DTO에 반영)
                if (refundTxn != null && refundTxn.getPgCno() != null) {
                    pgRefundSuccess = "COMPLETED".equals(refundTxn.getTransactionStatus());
                    pgRefundApprovalNo = refundTxn.getPgApprovalNo();
                }
            }

            // 취소 후 REFUNDED 상태로 확정 (grandTotal은 원본 유지 -- 감사 추적용)
            payment.setPaymentStatusRefunded();
        }

        // 서비스 항목 재고 복원
        for (SubReservation sub : master.getSubReservations()) {
            List<ReservationServiceItem> serviceItems = reservationServiceItemRepository
                    .findBySubReservationId(sub.getId());
            for (ReservationServiceItem item : serviceItems) {
                if (item.getServiceOptionId() != null) {
                    paidServiceOptionRepository.findById(item.getServiceOptionId()).ifPresent(option -> {
                        if (option.getInventoryItemId() != null) {
                            inventoryService.releaseInventory(
                                    option.getInventoryItemId(), sub.getCheckIn(), sub.getCheckOut(),
                                    item.getQuantity());
                        }
                    });
                }
            }
        }

        // 상태 변경
        master.updateStatus("CANCELED");
        for (SubReservation sub : master.getSubReservations()) {
            sub.updateStatus("CANCELED");
        }

        // 감사 로그 기록 (STEP 6)
        java.util.Map<String, Object> auditData = new java.util.HashMap<>();
        auditData.put("verificationValue", email != null ? email : "");
        auditData.put("cancelFee", cancelFee);
        auditData.put("refund", refundAmt);
        saveAuditLog(master.getId(), confirmationNo, "BOOKING_CANCELED", "WEBSITE",
                auditData, null, clientIp, userAgent, null);

        log.info("게스트 자가 취소: confirmationNo={}, cancelFee={}, refund={}",
                confirmationNo, cancelFee, refundAmt);

        return CancelBookingResponse.builder()
                .confirmationNo(confirmationNo)
                .status("CANCELED")
                .cancelFeeAmount(cancelFee)
                .refundAmount(refundAmt)
                .pgRefundSuccess(pgRefundSuccess)
                .pgRefundApprovalNo(pgRefundApprovalNo)
                .build();
    }

    @Override
    public List<BookingLookupResponse> lookupReservations(String email, String lastName) {
        if (email == null || email.isBlank() || lastName == null || lastName.isBlank()) {
            throw new HolaException(ErrorCode.BOOKING_GUEST_VERIFICATION_FAILED,
                    "이메일과 영문 성(Last Name)이 필요합니다.");
        }

        List<MasterReservation> reservations = masterReservationRepository.findByEmailAndLastName(email, lastName);
        if (reservations.isEmpty()) {
            throw new HolaException(ErrorCode.BOOKING_CONFIRMATION_NOT_FOUND,
                    "일치하는 예약을 찾을 수 없습니다.");
        }

        return reservations.stream()
                .map(master -> {
                    // 영문 이름 조합
                    String nameEn = buildEnglishName(master);

                    // 총 결제금액 조회
                    BigDecimal totalAmount = BigDecimal.ZERO;
                    var payment = reservationPaymentRepository.findByMasterReservationId(master.getId()).orElse(null);
                    if (payment != null && payment.getTotalPaidAmount() != null) {
                        totalAmount = payment.getTotalPaidAmount();
                    }

                    Property property = master.getProperty();
                    return BookingLookupResponse.builder()
                            .confirmationNo(master.getConfirmationNo())
                            .reservationStatus(master.getReservationStatus())
                            .guestNameKo(master.getGuestNameKo())
                            .guestNameEn(nameEn)
                            .propertyName(property.getPropertyName())
                            .propertyCode(property.getPropertyCode())
                            .checkIn(master.getMasterCheckIn())
                            .checkOut(master.getMasterCheckOut())
                            .roomCount(master.getSubReservations().size())
                            .totalAmount(totalAmount)
                            .currency("KRW")
                            .createdAt(master.getCreatedAt())
                            .build();
                })
                .toList();
    }

    @Override
    @Transactional
    public BookingModifyResponse modifyBooking(String confirmationNo, BookingModifyRequest request) {
        // 1. 이메일 검증 + 예약 조회
        MasterReservation master = findAndVerifyReservation(confirmationNo, request.getEmail());

        // 2. 수정 가능한 상태인지 확인 (RESERVED만 수정 가능)
        if (!"RESERVED".equals(master.getReservationStatus())) {
            throw new HolaException(ErrorCode.BOOKING_CANCEL_NOT_ALLOWED,
                    "예약 대기(RESERVED) 상태에서만 수정이 가능합니다.");
        }

        // 3. 날짜 유효성 검증
        helper.validateDateRange(request.getCheckIn(), request.getCheckOut());

        Property property = master.getProperty();

        // 4. 기존 총액 계산
        BigDecimal previousAmount = BigDecimal.ZERO;
        var existingPayment = reservationPaymentRepository.findByMasterReservationId(master.getId()).orElse(null);
        if (existingPayment != null && existingPayment.getTotalPaidAmount() != null) {
            previousAmount = existingPayment.getTotalPaidAmount();
        }

        // 5. 서브예약 순회하며 날짜/인원 변경 + 요금 재계산
        BigDecimal newTotalAmount = BigDecimal.ZERO;
        for (SubReservation sub : master.getSubReservations()) {
            int newAdults = request.getAdults() != null ? request.getAdults() : sub.getAdults();
            int newChildren = request.getChildren() != null ? request.getChildren() : sub.getChildren();

            // 가용성 확인 (자기 자신 제외하여 날짜 변경 시 false negative 방지)
            int availableCount = roomAvailabilityService.getAvailableRoomCount(
                    sub.getRoomTypeId(), request.getCheckIn(), request.getCheckOut(),
                    List.of(sub.getId()));
            if (availableCount <= 0) {
                throw new HolaException(ErrorCode.BOOKING_NO_AVAILABILITY);
            }

            // 서브예약 날짜/인원 업데이트
            sub.update(sub.getRoomTypeId(), sub.getFloorId(), sub.getRoomNumberId(),
                    newAdults, newChildren, request.getCheckIn(), request.getCheckOut(),
                    sub.getEarlyCheckIn(), sub.getLateCheckOut());

            // 기존 일별 요금 삭제 후 재계산 (orphanRemoval=true이므로 clear()+flush() 패턴 사용)
            sub.getDailyCharges().clear();
            dailyChargeRepository.flush();

            // Dayuse 레이트코드 분기
            RateCode modifyRateCode = rateCodeRepository.findById(master.getRateCodeId()).orElse(null);
            List<DailyCharge> newCharges;
            if (modifyRateCode != null && modifyRateCode.isDayUse()) {
                // Dayuse: DayUseRate 기반 요금 (봉사료/세금 포함)
                List<DayUseRate> dayUseRates = dayUseRateRepository
                        .findByRateCodeIdAndUseYnTrueOrderBySortOrderAsc(master.getRateCodeId());
                if (dayUseRates.isEmpty()) {
                    throw new HolaException(ErrorCode.DAY_USE_RATE_NOT_FOUND);
                }
                DailyCharge dc = helper.buildDayUseDailyCharge(
                        dayUseRates.get(0).getSupplyPrice(), property, request.getCheckIn());
                newCharges = List.of(DailyCharge.builder()
                        .subReservation(sub)
                        .chargeDate(dc.getChargeDate())
                        .supplyPrice(dc.getSupplyPrice())
                        .serviceCharge(dc.getServiceCharge())
                        .tax(dc.getTax())
                        .total(dc.getTotal())
                        .build());
            } else {
                // 숙박: 기존 일별 요금 계산
                newCharges = priceCalculationService.calculateDailyCharges(
                        master.getRateCodeId(), property,
                        request.getCheckIn(), request.getCheckOut(),
                        newAdults, newChildren, sub);
            }
            sub.getDailyCharges().addAll(newCharges);

            BigDecimal subTotal = newCharges.stream()
                    .map(DailyCharge::getTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            newTotalAmount = newTotalAmount.add(subTotal);
        }

        // 6. 마스터 예약 날짜 동기화
        master.syncDates(request.getCheckIn(), request.getCheckOut());

        // 게스트 정보 변경 (제공된 경우만)
        if (request.getGuestNameKo() != null) {
            master.update(request.getCheckIn(), request.getCheckOut(),
                    request.getGuestNameKo(),
                    master.getGuestFirstNameEn(), master.getGuestMiddleNameEn(), master.getGuestLastNameEn(),
                    master.getPhoneCountryCode(),
                    request.getPhoneNumber() != null ? request.getPhoneNumber() : master.getPhoneNumber(),
                    master.getEmail(),
                    master.getBirthDate(), master.getGender(), master.getNationality(),
                    master.getRateCodeId(), master.getMarketCodeId(), master.getReservationChannelId(),
                    master.getPromotionType(), master.getPromotionCode(),
                    master.getOtaReservationNo(), master.getIsOtaManaged(),
                    request.getCustomerRequest() != null ? request.getCustomerRequest() : master.getCustomerRequest());
        }

        // 7. 차액 계산
        BigDecimal priceDifference = newTotalAmount.subtract(previousAmount);
        String message;
        if (priceDifference.compareTo(BigDecimal.ZERO) > 0) {
            message = "추가 결제가 필요합니다: " + priceDifference.toPlainString() + "원";
        } else if (priceDifference.compareTo(BigDecimal.ZERO) < 0) {
            message = "차액 환불 예정: " + priceDifference.abs().toPlainString() + "원";
        } else {
            message = "요금 변동 없이 예약이 수정되었습니다.";
        }

        log.info("예약 수정 완료: confirmationNo={}, 이전={}, 신규={}, 차액={}",
                confirmationNo, previousAmount, newTotalAmount, priceDifference);

        // 결제 정보 재계산
        reservationPaymentService.recalculatePayment(master.getId());

        return BookingModifyResponse.builder()
                .confirmationNo(confirmationNo)
                .status("MODIFIED")
                .checkIn(request.getCheckIn())
                .checkOut(request.getCheckOut())
                .adults(master.getSubReservations().isEmpty() ? 0 : master.getSubReservations().get(0).getAdults())
                .children(master.getSubReservations().isEmpty() ? 0 : master.getSubReservations().get(0).getChildren())
                .previousAmount(previousAmount)
                .newAmount(newTotalAmount)
                .priceDifference(priceDifference)
                .message(message)
                .build();
    }

    // ===== Private Helper Methods =====

    /**
     * 영문 이름 조합 (FirstName + MiddleName + LastName)
     */
    private String buildEnglishName(MasterReservation master) {
        StringBuilder sb = new StringBuilder();
        if (master.getGuestFirstNameEn() != null) sb.append(master.getGuestFirstNameEn());
        if (master.getGuestMiddleNameEn() != null) sb.append(" ").append(master.getGuestMiddleNameEn());
        if (master.getGuestLastNameEn() != null) sb.append(" ").append(master.getGuestLastNameEn());
        return sb.toString().trim();
    }

    /**
     * 확인번호 + 이메일로 예약 조회 및 검증
     */
    private MasterReservation findAndVerifyReservation(String confirmationNo, String verificationValue) {
        MasterReservation master = masterReservationRepository.findByConfirmationNo(confirmationNo)
                .orElseThrow(() -> new HolaException(ErrorCode.BOOKING_CONFIRMATION_NOT_FOUND));

        // 이메일 또는 전화번호 검증 -- 값이 없으면 검증 생략 (데모)
        if (verificationValue != null && !verificationValue.isBlank()) {
            boolean verified = verificationValue.equalsIgnoreCase(master.getEmail())
                    || verificationValue.equals(master.getPhoneNumber());
            if (!verified) {
                throw new HolaException(ErrorCode.BOOKING_GUEST_VERIFICATION_FAILED);
            }
        }

        return master;
    }

    /**
     * 취소/노쇼 수수료 기준 1박 총액 조회
     * 원본 1박 총액(originalFirstNightTotal) 우선, 없으면 DailyCharge에서 조회 (하위호환)
     */
    private BigDecimal getFirstNightTotal(Long masterReservationId) {
        // 1. 원본 1박 총액 우선 (업그레이드 전 요금)
        var payment = reservationPaymentRepository
                .findByMasterReservationId(masterReservationId).orElse(null);
        if (payment != null && payment.getOriginalFirstNightTotal() != null
                && payment.getOriginalFirstNightTotal().compareTo(BigDecimal.ZERO) > 0) {
            return payment.getOriginalFirstNightTotal();
        }

        // 2. 하위호환: DailyCharge에서 조회
        List<SubReservation> subs = subReservationRepository.findByMasterReservationId(masterReservationId);
        if (subs.isEmpty()) return BigDecimal.ZERO;

        List<DailyCharge> charges = dailyChargeRepository.findBySubReservationId(subs.get(0).getId());
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
     * 부킹 감사 로그 저장
     */
    private void saveAuditLog(Long masterReservationId, String confirmationNo,
                               String eventType, String channel,
                               Object requestPayload, Object responsePayload,
                               String clientIp, String userAgent, String idempotencyKey) {
        try {
            BookingAuditLog auditLog = BookingAuditLog.builder()
                    .masterReservationId(masterReservationId)
                    .confirmationNo(confirmationNo)
                    .eventType(eventType)
                    .channel(channel)
                    .requestPayload(objectMapper.writeValueAsString(requestPayload))
                    .responsePayload(responsePayload != null ? objectMapper.writeValueAsString(responsePayload) : null)
                    .clientIp(clientIp)
                    .userAgent(userAgent)
                    .idempotencyKey(idempotencyKey)
                    .build();
            bookingAuditLogRepository.save(auditLog);
        } catch (JsonProcessingException e) {
            log.warn("부킹 감사 로그 JSON 직렬화 실패: {}", e.getMessage());
        }
    }
}
