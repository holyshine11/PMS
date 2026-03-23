package com.hola.reservation.mapper;

import com.hola.hotel.entity.Property;
import com.hola.reservation.dto.request.ReservationCreateRequest;
import com.hola.reservation.dto.request.SubReservationRequest;
import com.hola.reservation.dto.request.ReservationGuestRequest;
import com.hola.reservation.dto.response.*;
import com.hola.reservation.entity.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 예약 Entity <-> DTO 변환 매퍼
 */
@Component
public class ReservationMapper {

    /**
     * 마스터 예약 생성 요청 → 엔티티 변환
     */
    public MasterReservation toMasterReservationEntity(ReservationCreateRequest request, Property property) {
        return MasterReservation.builder()
                .property(property)
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
    }

    /**
     * 서브 예약 요청 → 엔티티 변환
     */
    public SubReservation toSubReservationEntity(SubReservationRequest request, MasterReservation masterReservation) {
        return SubReservation.builder()
                .masterReservation(masterReservation)
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
    }

    /**
     * 투숙객 요청 → 엔티티 변환
     */
    public ReservationGuest toReservationGuestEntity(ReservationGuestRequest request, SubReservation subReservation) {
        return ReservationGuest.builder()
                .subReservation(subReservation)
                .guestSeq(request.getGuestSeq())
                .guestNameKo(request.getGuestNameKo())
                .guestFirstNameEn(request.getGuestFirstNameEn())
                .guestMiddleNameEn(request.getGuestMiddleNameEn())
                .guestLastNameEn(request.getGuestLastNameEn())
                .build();
    }

    /**
     * 마스터 예약 → 리스트 응답 변환
     */
    public ReservationListResponse toReservationListResponse(MasterReservation master) {
        // 첫 번째 서브 예약 기준 객실 정보 (이름은 서비스에서 조회 후 별도 세팅)
        return ReservationListResponse.builder()
                .id(master.getId())
                .masterReservationNo(master.getMasterReservationNo())
                .confirmationNo(master.getConfirmationNo())
                .reservationStatus(master.getReservationStatus())
                .masterCheckIn(master.getMasterCheckIn())
                .masterCheckOut(master.getMasterCheckOut())
                .guestNameKo(master.getGuestNameKo())
                .phoneNumber(master.getPhoneNumber())
                .isOtaManaged(master.getIsOtaManaged())
                .createdAt(master.getCreatedAt())
                .build();
    }

    /**
     * 마스터 예약 → 상세 응답 변환
     */
    public ReservationDetailResponse toReservationDetailResponse(MasterReservation master) {
        List<SubReservationResponse> subResponses = master.getSubReservations() != null
                ? master.getSubReservations().stream()
                    .map(this::toSubReservationResponse)
                    .collect(Collectors.toList())
                : Collections.emptyList();

        return ReservationDetailResponse.builder()
                .id(master.getId())
                .propertyId(master.getProperty() != null ? master.getProperty().getId() : null)
                .masterReservationNo(master.getMasterReservationNo())
                .confirmationNo(master.getConfirmationNo())
                .reservationStatus(master.getReservationStatus())
                .masterCheckIn(master.getMasterCheckIn())
                .masterCheckOut(master.getMasterCheckOut())
                .reservationDate(master.getReservationDate())
                .guestNameKo(master.getGuestNameKo())
                .guestFirstNameEn(master.getGuestFirstNameEn())
                .guestMiddleNameEn(master.getGuestMiddleNameEn())
                .guestLastNameEn(master.getGuestLastNameEn())
                .phoneCountryCode(master.getPhoneCountryCode())
                .phoneNumber(master.getPhoneNumber())
                .email(master.getEmail())
                .birthDate(master.getBirthDate())
                .gender(master.getGender())
                .nationality(master.getNationality())
                .rateCodeId(master.getRateCodeId())
                .marketCodeId(master.getMarketCodeId())
                .reservationChannelId(master.getReservationChannelId())
                .promotionType(master.getPromotionType())
                .promotionCode(master.getPromotionCode())
                .otaReservationNo(master.getOtaReservationNo())
                .isOtaManaged(master.getIsOtaManaged())
                .customerRequest(master.getCustomerRequest())
                .subReservations(subResponses)
                .createdAt(master.getCreatedAt())
                .updatedAt(master.getUpdatedAt())
                .build();
    }

    /**
     * 서브 예약 → 응답 변환
     */
    public SubReservationResponse toSubReservationResponse(SubReservation sub) {
        List<ReservationGuestResponse> guestResponses = sub.getGuests() != null
                ? sub.getGuests().stream()
                    .map(this::toReservationGuestResponse)
                    .collect(Collectors.toList())
                : Collections.emptyList();

        List<DailyChargeResponse> chargeResponses = sub.getDailyCharges() != null
                ? sub.getDailyCharges().stream()
                    .map(this::toDailyChargeResponse)
                    .collect(Collectors.toList())
                : Collections.emptyList();

        List<ReservationServiceResponse> serviceResponses = sub.getServices() != null
                ? sub.getServices().stream()
                    .map(this::toReservationServiceResponse)
                    .collect(Collectors.toList())
                : Collections.emptyList();

        return SubReservationResponse.builder()
                .id(sub.getId())
                .subReservationNo(sub.getSubReservationNo())
                .roomReservationStatus(sub.getRoomReservationStatus())
                .stayType(sub.getStayType())
                .dayUseStartTime(sub.getDayUseStartTime())
                .dayUseEndTime(sub.getDayUseEndTime())
                .roomTypeId(sub.getRoomTypeId())
                .floorId(sub.getFloorId())
                .roomNumberId(sub.getRoomNumberId())
                .adults(sub.getAdults())
                .children(sub.getChildren())
                .checkIn(sub.getCheckIn())
                .checkOut(sub.getCheckOut())
                .earlyCheckIn(sub.getEarlyCheckIn())
                .lateCheckOut(sub.getLateCheckOut())
                .actualCheckInTime(sub.getActualCheckInTime())
                .actualCheckOutTime(sub.getActualCheckOutTime())
                .earlyCheckInFee(sub.getEarlyCheckInFee())
                .lateCheckOutFee(sub.getLateCheckOutFee())
                .guests(guestResponses)
                .dailyCharges(chargeResponses)
                .services(serviceResponses)
                .build();
    }

    /**
     * 투숙객 → 응답 변환
     */
    public ReservationGuestResponse toReservationGuestResponse(ReservationGuest guest) {
        return ReservationGuestResponse.builder()
                .id(guest.getId())
                .guestSeq(guest.getGuestSeq())
                .guestNameKo(guest.getGuestNameKo())
                .guestFirstNameEn(guest.getGuestFirstNameEn())
                .guestMiddleNameEn(guest.getGuestMiddleNameEn())
                .guestLastNameEn(guest.getGuestLastNameEn())
                .build();
    }

    /**
     * 일별 요금 → 응답 변환
     */
    public DailyChargeResponse toDailyChargeResponse(DailyCharge charge) {
        return DailyChargeResponse.builder()
                .id(charge.getId())
                .chargeDate(charge.getChargeDate())
                .supplyPrice(charge.getSupplyPrice())
                .tax(charge.getTax())
                .serviceCharge(charge.getServiceCharge())
                .total(charge.getTotal())
                .build();
    }

    /**
     * 서비스 항목 → 응답 변환
     */
    public ReservationServiceResponse toReservationServiceResponse(ReservationServiceItem service) {
        return toReservationServiceResponse(service, null);
    }

    /**
     * 서비스 항목 → 응답 변환 (서비스명 포함)
     */
    public ReservationServiceResponse toReservationServiceResponse(ReservationServiceItem service, String serviceName) {
        return ReservationServiceResponse.builder()
                .id(service.getId())
                .serviceType(service.getServiceType())
                .serviceOptionId(service.getServiceOptionId())
                .serviceName(serviceName)
                .serviceDate(service.getServiceDate())
                .quantity(service.getQuantity())
                .unitPrice(service.getUnitPrice())
                .tax(service.getTax())
                .totalPrice(service.getTotalPrice())
                .build();
    }

    /**
     * 보증금 → 응답 변환 (카드번호 마스킹 처리)
     */
    public ReservationDepositResponse toReservationDepositResponse(ReservationDeposit deposit) {
        return ReservationDepositResponse.builder()
                .id(deposit.getId())
                .depositMethod(deposit.getDepositMethod())
                .cardCompany(deposit.getCardCompany())
                .cardNumberMasked(maskCardNumber(deposit.getCardNumberEncrypted()))
                .cardExpiryDate(deposit.getCardExpiryDate())
                .currency(deposit.getCurrency())
                .amount(deposit.getAmount())
                .build();
    }

    /**
     * 결제 요약 → 응답 변환
     */
    public PaymentSummaryResponse toPaymentSummaryResponse(ReservationPayment payment,
                                                            List<PaymentAdjustment> adjustments,
                                                            List<PaymentTransaction> transactions) {
        List<PaymentAdjustmentResponse> adjustmentResponses = adjustments != null
                ? adjustments.stream()
                    .map(this::toPaymentAdjustmentResponse)
                    .collect(Collectors.toList())
                : Collections.emptyList();

        List<PaymentTransactionResponse> transactionResponses = transactions != null
                ? transactions.stream()
                    .map(this::toPaymentTransactionResponse)
                    .collect(Collectors.toList())
                : Collections.emptyList();

        BigDecimal grandTotal = payment.getGrandTotal() != null ? payment.getGrandTotal() : BigDecimal.ZERO;
        BigDecimal totalPaid = payment.getTotalPaidAmount() != null ? payment.getTotalPaidAmount() : BigDecimal.ZERO;

        return PaymentSummaryResponse.builder()
                .id(payment.getId())
                .paymentStatus(payment.getPaymentStatus())
                .totalRoomAmount(payment.getTotalRoomAmount())
                .totalServiceAmount(payment.getTotalServiceAmount())
                .totalServiceChargeAmount(payment.getTotalServiceChargeAmount())
                .totalAdjustmentAmount(payment.getTotalAdjustmentAmount())
                .totalEarlyLateFee(payment.getTotalEarlyLateFee())
                .grandTotal(grandTotal)
                .totalPaidAmount(totalPaid)
                .cancelFeeAmount(payment.getCancelFeeAmount() != null ? payment.getCancelFeeAmount() : BigDecimal.ZERO)
                .refundAmount(payment.getRefundAmount() != null ? payment.getRefundAmount() : BigDecimal.ZERO)
                .remainingAmount(grandTotal.subtract(totalPaid).max(BigDecimal.ZERO))
                .paymentDate(payment.getPaymentDate())
                .paymentMethod(payment.getPaymentMethod())
                .adjustments(adjustmentResponses)
                .transactions(transactionResponses)
                .build();
    }

    /**
     * 결제 거래 이력 → 응답 변환
     */
    public PaymentTransactionResponse toPaymentTransactionResponse(PaymentTransaction transaction) {
        return PaymentTransactionResponse.builder()
                .id(transaction.getId())
                .transactionSeq(transaction.getTransactionSeq())
                .transactionType(transaction.getTransactionType())
                .paymentMethod(transaction.getPaymentMethod())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .transactionStatus(transaction.getTransactionStatus())
                .approvalNo(transaction.getApprovalNo())
                .memo(transaction.getMemo())
                .createdAt(transaction.getCreatedAt())
                .createdBy(transaction.getCreatedBy())
                .build();
    }

    /**
     * 결제 조정 → 응답 변환
     */
    public PaymentAdjustmentResponse toPaymentAdjustmentResponse(PaymentAdjustment adjustment) {
        return PaymentAdjustmentResponse.builder()
                .id(adjustment.getId())
                .adjustmentSeq(adjustment.getAdjustmentSeq())
                .currency(adjustment.getCurrency())
                .adjustmentSign(adjustment.getAdjustmentSign())
                .supplyPrice(adjustment.getSupplyPrice())
                .tax(adjustment.getTax())
                .totalAmount(adjustment.getTotalAmount())
                .comment(adjustment.getComment())
                .createdAt(adjustment.getCreatedAt())
                .createdBy(adjustment.getCreatedBy())
                .build();
    }

    /**
     * 메모 → 응답 변환
     */
    public ReservationMemoResponse toReservationMemoResponse(ReservationMemo memo) {
        return ReservationMemoResponse.builder()
                .id(memo.getId())
                .content(memo.getContent())
                .createdAt(memo.getCreatedAt())
                .createdBy(memo.getCreatedBy())
                .build();
    }

    /**
     * 카드번호 마스킹 (앞4 + **** + 뒤4)
     */
    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 8) {
            return cardNumber;
        }
        String first4 = cardNumber.substring(0, 4);
        String last4 = cardNumber.substring(cardNumber.length() - 4);
        return first4 + "****" + last4;
    }
}
