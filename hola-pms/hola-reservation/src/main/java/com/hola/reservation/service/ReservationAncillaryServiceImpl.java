package com.hola.reservation.service;

import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.reservation.dto.request.ReservationDepositRequest;
import com.hola.reservation.dto.request.ReservationServiceRequest;
import com.hola.reservation.dto.response.ReservationDepositResponse;
import com.hola.reservation.dto.response.ReservationMemoResponse;
import com.hola.reservation.dto.response.ReservationServiceResponse;
import com.hola.reservation.entity.*;
import com.hola.reservation.mapper.ReservationMapper;
import com.hola.reservation.repository.*;
import com.hola.room.entity.PaidServiceOption;
import com.hola.room.repository.PaidServiceOptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 예약 부속 서비스 구현 — 메모, 예치금, 유료 서비스 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationAncillaryServiceImpl implements ReservationAncillaryService {

    private final ReservationFinder finder;
    private final ReservationMemoRepository reservationMemoRepository;
    private final ReservationDepositRepository reservationDepositRepository;
    private final ReservationServiceItemRepository serviceItemRepository;
    private final PaidServiceOptionRepository paidServiceOptionRepository;
    private final ReservationMapper reservationMapper;
    private final ReservationPaymentService paymentService;
    private final ReservationChangeLogService changeLogService;

    @Override
    public List<ReservationMemoResponse> getMemos(Long reservationId, Long propertyId) {
        finder.findMasterById(reservationId, propertyId);
        return reservationMemoRepository.findByMasterReservationIdOrderByCreatedAtDesc(reservationId)
                .stream().map(reservationMapper::toReservationMemoResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ReservationMemoResponse addMemo(Long reservationId, Long propertyId, String content) {
        MasterReservation master = finder.findMasterById(reservationId, propertyId);
        ReservationMemo memo = ReservationMemo.builder()
                .masterReservationId(master.getId())
                .content(content)
                .build();
        memo = reservationMemoRepository.save(memo);
        try {
            String preview = content != null && content.length() > 50 ? content.substring(0, 50) + "..." : content;
            changeLogService.log(master.getId(), null, "RESERVATION", "ADD_MEMO",
                    null, null, preview, "메모 추가: " + preview);
        } catch (Exception e) {
            log.error("변경이력 기록 실패: {}", e.getMessage());
        }
        return reservationMapper.toReservationMemoResponse(memo);
    }

    @Override
    @Transactional
    public ReservationDepositResponse addDeposit(Long reservationId, Long propertyId, ReservationDepositRequest request) {
        MasterReservation master = finder.findMasterById(reservationId, propertyId);
        finder.validateModifiable(master);
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
        try {
            changeLogService.log(master.getId(), null, "RESERVATION", "ADD_DEPOSIT",
                    null, null, request.getAmount().stripTrailingZeros().toPlainString(),
                    "보증금 등록: " + request.getDepositMethod() + " " + request.getAmount().stripTrailingZeros().toPlainString() + "원");
        } catch (Exception e) {
            log.error("변경이력 기록 실패: {}", e.getMessage());
        }
        return reservationMapper.toReservationDepositResponse(deposit);
    }

    @Override
    @Transactional
    public ReservationDepositResponse updateDeposit(Long reservationId, Long propertyId, Long depositId, ReservationDepositRequest request) {
        finder.findMasterById(reservationId, propertyId);
        ReservationDeposit deposit = reservationDepositRepository.findById(depositId)
                .orElseThrow(() -> new HolaException(ErrorCode.DEPOSIT_NOT_FOUND));
        if (!deposit.getMasterReservation().getId().equals(reservationId)) {
            throw new HolaException(ErrorCode.DEPOSIT_NOT_FOUND);
        }
        BigDecimal prevAmount = deposit.getAmount();
        String prevMethod = deposit.getDepositMethod();
        deposit.update(request.getDepositMethod(), request.getCardCompany(),
                request.getCardNumberEncrypted(), request.getCardCvcEncrypted(),
                request.getCardExpiryDate(), request.getCardPasswordEncrypted(),
                request.getCurrency(), request.getAmount());
        log.info("예치금 수정: depositId={}", depositId);
        // 금액 또는 방법이 실제 변경된 경우에만 이력 기록
        boolean depositChanged = !java.util.Objects.equals(prevAmount, request.getAmount())
                || !java.util.Objects.equals(prevMethod, request.getDepositMethod());
        if (depositChanged) {
            try {
                changeLogService.log(reservationId, null, "RESERVATION", "UPDATE",
                        "deposit", prevMethod + " " + prevAmount.stripTrailingZeros().toPlainString() + "원",
                        request.getDepositMethod() + " " + request.getAmount().stripTrailingZeros().toPlainString() + "원",
                        "보증금 수정: " + prevAmount.stripTrailingZeros().toPlainString() + "원 → " + request.getAmount().stripTrailingZeros().toPlainString() + "원");
            } catch (Exception e) {
                log.error("변경이력 기록 실패: {}", e.getMessage());
            }
        }
        return reservationMapper.toReservationDepositResponse(deposit);
    }

    @Override
    @Transactional
    public ReservationServiceResponse addService(Long masterReservationId, Long subReservationId,
                                                  Long propertyId, ReservationServiceRequest request) {
        MasterReservation master = finder.findMasterById(masterReservationId, propertyId);
        finder.validateModifiable(master);

        SubReservation sub = finder.findSubAndValidateOwnership(subReservationId, master);

        // 서비스 일자가 체크인~체크아웃 당일 범위 내인지 검증
        // (체크아웃 당일 조식 등 관리자 재량 서비스 허용)
        if (request.getServiceDate() != null) {
            if (request.getServiceDate().isBefore(sub.getCheckIn())
                    || request.getServiceDate().isAfter(sub.getCheckOut())) {
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
        try {
            changeLogService.log(master.getId(), sub.getId(), "SERVICE", "ADD_SERVICE",
                    null, null, option.getServiceNameKo(),
                    "서비스 추가: " + option.getServiceNameKo() + " (" + totalPrice.stripTrailingZeros().toPlainString() + "원)");
        } catch (Exception e) {
            log.error("변경이력 기록 실패: {}", e.getMessage());
        }

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
        MasterReservation master = finder.findMasterById(masterReservationId, propertyId);
        finder.validateModifiable(master);

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
        try {
            String serviceName = serviceItem.getServiceOptionId() != null
                    ? paidServiceOptionRepository.findById(serviceItem.getServiceOptionId())
                        .map(PaidServiceOption::getServiceNameKo).orElse("서비스")
                    : "서비스";
            changeLogService.log(master.getId(), subReservationId, "SERVICE", "REMOVE_SERVICE",
                    null, serviceName, null,
                    "서비스 삭제: " + serviceName + " (" + serviceItem.getTotalPrice().stripTrailingZeros().toPlainString() + "원)");
        } catch (Exception e) {
            log.error("변경이력 기록 실패: {}", e.getMessage());
        }
    }
}
