package com.hola.reservation.service;

import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.hotel.entity.Hotel;
import com.hola.hotel.entity.Property;
import com.hola.hotel.service.WorkstationService;
import com.hola.reservation.dto.request.PaymentAdjustmentRequest;
import com.hola.reservation.dto.request.PaymentProcessRequest;
import com.hola.reservation.dto.request.VanResultPayload;
import com.hola.reservation.dto.response.PaymentAdjustmentResponse;
import com.hola.reservation.dto.response.PaymentSummaryResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hola.reservation.entity.*;
import com.hola.reservation.mapper.ReservationMapper;
import com.hola.reservation.repository.*;
import com.hola.room.repository.RoomTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ReservationPaymentServiceImpl 단위 테스트 (20개)
 */
@DisplayName("ReservationPaymentServiceImpl - 예약 결제 서비스")
@ExtendWith(MockitoExtension.class)
class ReservationPaymentServiceImplTest {

    @InjectMocks
    private ReservationPaymentServiceImpl service;

    @Mock
    private MasterReservationRepository masterReservationRepository;
    @Mock
    private ReservationPaymentRepository paymentRepository;
    @Mock
    private PaymentAdjustmentRepository adjustmentRepository;
    @Mock
    private PaymentTransactionRepository transactionRepository;
    @Mock
    private SubReservationRepository subReservationRepository;
    @Mock
    private DailyChargeRepository dailyChargeRepository;
    @Mock
    private ReservationServiceItemRepository serviceItemRepository;
    @Mock
    private RoomTypeRepository roomTypeRepository;
    @Mock
    private ReservationMapper reservationMapper;
    @Mock
    private WorkstationService workstationService;
    @Mock
    private ObjectMapper objectMapper;

    // 공통 테스트 픽스처
    private Hotel hotel;
    private Property property;
    private MasterReservation master;
    private SubReservation defaultSub;

    private static final Long PROPERTY_ID = 1L;
    private static final Long RESERVATION_ID = 1L;
    private static final Long SUB_ID = 100L;

    @BeforeEach
    void setUp() {
        hotel = Hotel.builder()
                .hotelCode("GMP")
                .hotelName("그랜드 호텔")
                .build();
        setId(hotel, 1L);

        property = Property.builder()
                .hotel(hotel)
                .propertyCode("GMP01")
                .propertyName("그랜드 호텔 본관")
                .build();
        setId(property, 1L);

        master = MasterReservation.builder()
                .property(property)
                .masterReservationNo("GMP260310-0001")
                .confirmationNo("A1B2C3")
                .reservationStatus("RESERVED")
                .isOtaManaged(false)
                .build();
        setId(master, RESERVATION_ID);

        defaultSub = createSub(SUB_ID, "RESERVED");
    }

    // ─── 헬퍼 메서드 ──────────────────────────

    /**
     * BaseEntity의 private id 필드에 값 설정 (리플렉션)
     */
    private void setId(Object entity, Long id) {
        try {
            Field field = findField(entity.getClass(), "id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException("setId 실패", e);
        }
    }

    private Field findField(Class<?> clazz, String name) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new RuntimeException("필드를 찾을 수 없습니다: " + name);
    }

    /**
     * 기본 ReservationPayment 생성
     */
    private ReservationPayment createPayment(String status, BigDecimal grandTotal, BigDecimal totalPaid) {
        ReservationPayment payment = ReservationPayment.builder()
                .masterReservation(master)
                .paymentStatus(status)
                .totalRoomAmount(grandTotal)
                .totalServiceAmount(BigDecimal.ZERO)
                .totalServiceChargeAmount(BigDecimal.ZERO)
                .totalAdjustmentAmount(BigDecimal.ZERO)
                .grandTotal(grandTotal)
                .totalPaidAmount(totalPaid)
                .build();
        setId(payment, 10L);
        return payment;
    }

    /**
     * SubReservation 생성
     */
    private SubReservation createSub(Long id, String status) {
        SubReservation sub = SubReservation.builder()
                .masterReservation(master)
                .subReservationNo("GMP260310-0001-" + id)
                .roomReservationStatus(status)
                .build();
        setId(sub, id);
        return sub;
    }

    /**
     * DailyCharge 생성
     */
    private DailyCharge createDailyCharge(SubReservation sub, int supplyPrice, int tax, int serviceCharge) {
        return DailyCharge.builder()
                .subReservation(sub)
                .supplyPrice(new BigDecimal(supplyPrice))
                .tax(new BigDecimal(tax))
                .serviceCharge(new BigDecimal(serviceCharge))
                .total(new BigDecimal(supplyPrice + tax + serviceCharge))
                .build();
    }

    /**
     * ReservationServiceItem 생성
     */
    private ReservationServiceItem createServiceItem(SubReservation sub, int totalPrice) {
        return ReservationServiceItem.builder()
                .subReservation(sub)
                .serviceType("PAID")
                .quantity(1)
                .unitPrice(new BigDecimal(totalPrice))
                .totalPrice(new BigDecimal(totalPrice))
                .build();
    }

    /**
     * PaymentAdjustment 생성
     */
    private PaymentAdjustment createAdjustment(int seq, String sign, int totalAmount) {
        return PaymentAdjustment.builder()
                .masterReservationId(RESERVATION_ID)
                .adjustmentSeq(seq)
                .adjustmentSign(sign)
                .supplyPrice(new BigDecimal(totalAmount))
                .tax(BigDecimal.ZERO)
                .totalAmount(new BigDecimal(totalAmount))
                .comment("테스트 조정")
                .build();
    }

    /**
     * PaymentProcessRequest 생성
     */
    private PaymentProcessRequest createPayRequest(String method, BigDecimal amount) {
        return new PaymentProcessRequest(method, amount, "테스트 결제");
    }

    /**
     * 마스터 예약 조회 스텁
     */
    private void stubMasterFound() {
        when(masterReservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(master));
    }

    /**
     * recalculateAmounts 내부 호출용 스텁: grandTotal이 특정 값이 되도록 DailyCharge 설정
     * supplyPrice + tax = roomTotal, serviceCharge 별도
     */
    private void stubRecalculateWithTotal(int supplyPrice, int tax, int serviceCharge) {
        when(subReservationRepository.findByMasterReservationId(RESERVATION_ID))
                .thenReturn(List.of(defaultSub));
        when(dailyChargeRepository.findBySubReservationId(SUB_ID))
                .thenReturn(List.of(createDailyCharge(defaultSub, supplyPrice, tax, serviceCharge)));
        when(serviceItemRepository.findBySubReservationId(SUB_ID))
                .thenReturn(Collections.emptyList());
        when(adjustmentRepository.findByMasterReservationIdOrderByAdjustmentSeqAsc(RESERVATION_ID))
                .thenReturn(Collections.emptyList());
    }

    // ═══════════════════════════════════════════════
    //  1. getPaymentSummary (2개)
    // ═══════════════════════════════════════════════

    @Nested
    @DisplayName("getPaymentSummary - 결제 요약 조회")
    class GetPaymentSummary {

        @Test
        @DisplayName("결제 정보가 존재하면 매퍼를 통해 응답 반환")
        void existingPayment_returnsMappedResponse() {
            // given
            stubMasterFound();
            ReservationPayment payment = createPayment("PARTIAL", new BigDecimal("300000"), new BigDecimal("100000"));
            when(paymentRepository.findByMasterReservationId(RESERVATION_ID))
                    .thenReturn(Optional.of(payment));

            List<PaymentAdjustment> adjustments = List.of(createAdjustment(1, "+", 10000));
            List<PaymentTransaction> transactions = List.of(
                    PaymentTransaction.builder()
                            .masterReservationId(RESERVATION_ID).transactionSeq(1)
                            .paymentMethod("CARD").amount(new BigDecimal("100000")).build()
            );
            when(adjustmentRepository.findByMasterReservationIdOrderByAdjustmentSeqAsc(RESERVATION_ID))
                    .thenReturn(adjustments);
            when(transactionRepository.findByMasterReservationIdOrderByTransactionSeqAsc(RESERVATION_ID))
                    .thenReturn(transactions);

            PaymentSummaryResponse expected = PaymentSummaryResponse.builder()
                    .paymentStatus("PARTIAL").grandTotal(new BigDecimal("300000")).build();
            when(reservationMapper.toPaymentSummaryResponse(eq(payment), eq(adjustments), eq(transactions), any()))
                    .thenReturn(expected);

            // when
            PaymentSummaryResponse result = service.getPaymentSummary(PROPERTY_ID, RESERVATION_ID);

            // then
            assertThat(result.getPaymentStatus()).isEqualTo("PARTIAL");
            verify(reservationMapper).toPaymentSummaryResponse(eq(payment), eq(adjustments), eq(transactions), any());
        }

        @Test
        @DisplayName("결제 정보가 없으면 UNPAID 상태의 빈 응답 반환")
        void noPayment_returnsEmptyUnpaidResponse() {
            // given
            stubMasterFound();
            when(paymentRepository.findByMasterReservationId(RESERVATION_ID))
                    .thenReturn(Optional.empty());

            // when
            PaymentSummaryResponse result = service.getPaymentSummary(PROPERTY_ID, RESERVATION_ID);

            // then
            assertThat(result.getPaymentStatus()).isEqualTo("UNPAID");
            assertThat(result.getGrandTotal()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getTotalPaidAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getRemainingAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getTransactions()).isEmpty();
            verifyNoInteractions(reservationMapper);
        }
    }

    // ═══════════════════════════════════════════════
    //  2. processPayment (8개)
    // ═══════════════════════════════════════════════

    @Nested
    @DisplayName("processPayment - 결제 처리")
    class ProcessPayment {

        @Test
        @DisplayName("정상 전액 결제 - 잔액만큼 결제 처리")
        void normalFullPay_processesCorrectly() {
            // given
            stubMasterFound();
            ReservationPayment payment = createPayment("UNPAID", new BigDecimal("300000"), BigDecimal.ZERO);
            when(paymentRepository.findByMasterReservationId(RESERVATION_ID))
                    .thenReturn(Optional.of(payment));

            // recalculateAmounts: supply(300000) + tax(0) = 300000 room, serviceCharge(0)
            stubRecalculateWithTotal(300000, 0, 0);

            when(transactionRepository.findByMasterReservationIdOrderByTransactionSeqAsc(RESERVATION_ID))
                    .thenReturn(Collections.emptyList());
            when(transactionRepository.save(any(PaymentTransaction.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            PaymentSummaryResponse expected = PaymentSummaryResponse.builder()
                    .paymentStatus("PAID").build();
            when(reservationMapper.toPaymentSummaryResponse(any(), any(), any(), any()))
                    .thenReturn(expected);

            PaymentProcessRequest request = createPayRequest("CARD", new BigDecimal("300000"));

            // when
            PaymentSummaryResponse result = service.processPayment(PROPERTY_ID, RESERVATION_ID, request);

            // then
            assertThat(result.getPaymentStatus()).isEqualTo("PAID");
            verify(transactionRepository).save(any(PaymentTransaction.class));
        }

        @Test
        @DisplayName("부분 결제 (amount=null) - 잔액 전액 자동 결제")
        void nullAmount_paysRemainingBalance() {
            // given
            stubMasterFound();
            // 이미 100000 결제됨
            ReservationPayment payment = createPayment("PARTIAL", new BigDecimal("300000"), new BigDecimal("100000"));
            when(paymentRepository.findByMasterReservationId(RESERVATION_ID))
                    .thenReturn(Optional.of(payment));

            // recalculate 후 grandTotal = 300000
            stubRecalculateWithTotal(300000, 0, 0);

            when(transactionRepository.findByMasterReservationIdOrderByTransactionSeqAsc(RESERVATION_ID))
                    .thenReturn(Collections.emptyList());
            when(transactionRepository.save(any(PaymentTransaction.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(reservationMapper.toPaymentSummaryResponse(any(), any(), any(), any()))
                    .thenReturn(PaymentSummaryResponse.builder().build());

            PaymentProcessRequest request = createPayRequest("CASH", null); // null = 잔액 전액

            // when
            service.processPayment(PROPERTY_ID, RESERVATION_ID, request);

            // then
            ArgumentCaptor<PaymentTransaction> captor = ArgumentCaptor.forClass(PaymentTransaction.class);
            verify(transactionRepository).save(captor.capture());
            // 잔액 = 300000 - 100000 = 200000
            assertThat(captor.getValue().getAmount()).isEqualByComparingTo(new BigDecimal("200000"));
        }

        @Test
        @DisplayName("결제 금액 0 - RESERVATION_PAYMENT_AMOUNT_INVALID 예외")
        void zeroAmount_throwsInvalidAmount() {
            // given
            stubMasterFound();
            ReservationPayment payment = createPayment("UNPAID", new BigDecimal("300000"), BigDecimal.ZERO);
            when(paymentRepository.findByMasterReservationId(RESERVATION_ID))
                    .thenReturn(Optional.of(payment));

            // recalculate 후 grandTotal = 300000
            stubRecalculateWithTotal(300000, 0, 0);

            PaymentProcessRequest request = createPayRequest("CARD", BigDecimal.ZERO);

            // when & then
            assertThatThrownBy(() -> service.processPayment(PROPERTY_ID, RESERVATION_ID, request))
                    .isInstanceOf(HolaException.class)
                    .extracting(e -> ((HolaException) e).getErrorCode())
                    .isEqualTo(ErrorCode.RESERVATION_PAYMENT_AMOUNT_INVALID);
        }

        @Test
        @DisplayName("잔액 초과 결제 - RESERVATION_PAYMENT_AMOUNT_EXCEEDED 예외")
        void exceededAmount_throwsExceeded() {
            // given
            stubMasterFound();
            ReservationPayment payment = createPayment("UNPAID", new BigDecimal("300000"), BigDecimal.ZERO);
            when(paymentRepository.findByMasterReservationId(RESERVATION_ID))
                    .thenReturn(Optional.of(payment));

            // recalculate 후 grandTotal = 300000
            stubRecalculateWithTotal(300000, 0, 0);

            PaymentProcessRequest request = createPayRequest("CARD", new BigDecimal("500000"));

            // when & then
            assertThatThrownBy(() -> service.processPayment(PROPERTY_ID, RESERVATION_ID, request))
                    .isInstanceOf(HolaException.class)
                    .extracting(e -> ((HolaException) e).getErrorCode())
                    .isEqualTo(ErrorCode.RESERVATION_PAYMENT_AMOUNT_EXCEEDED);
        }

        @Test
        @DisplayName("잔액 없는 PAID 상태 - RESERVATION_PAYMENT_ALREADY_COMPLETED 예외")
        void alreadyPaid_throwsAlreadyCompleted() {
            // given - grandTotal == totalPaid == 300000 → remaining == 0
            stubMasterFound();
            ReservationPayment payment = createPayment("PAID", new BigDecimal("300000"), new BigDecimal("300000"));
            when(paymentRepository.findByMasterReservationId(RESERVATION_ID))
                    .thenReturn(Optional.of(payment));
            // recalculateAmounts에서 재계산해도 grandTotal = 300000 으로 동일 (remaining = 0)
            stubRecalculateWithTotal(300000, 0, 0);

            PaymentProcessRequest request = createPayRequest("CARD", new BigDecimal("100000"));

            // when & then
            assertThatThrownBy(() -> service.processPayment(PROPERTY_ID, RESERVATION_ID, request))
                    .isInstanceOf(HolaException.class)
                    .extracting(e -> ((HolaException) e).getErrorCode())
                    .isEqualTo(ErrorCode.RESERVATION_PAYMENT_ALREADY_COMPLETED);
        }

        @Test
        @DisplayName("OTA 예약도 정상 결제 가능")
        void otaReservation_processesNormally() {
            // given - OTA 예약이어도 결제 차단 없음
            MasterReservation otaMaster = MasterReservation.builder()
                    .property(property)
                    .masterReservationNo("GMP260310-0002")
                    .confirmationNo("OTA123")
                    .reservationStatus("RESERVED")
                    .isOtaManaged(true)
                    .build();
            setId(otaMaster, 2L);
            when(masterReservationRepository.findById(2L)).thenReturn(Optional.of(otaMaster));

            ReservationPayment payment = ReservationPayment.builder()
                    .masterReservation(otaMaster)
                    .paymentStatus("UNPAID")
                    .totalRoomAmount(new BigDecimal("300000"))
                    .totalServiceAmount(BigDecimal.ZERO)
                    .totalServiceChargeAmount(BigDecimal.ZERO)
                    .totalAdjustmentAmount(BigDecimal.ZERO)
                    .grandTotal(new BigDecimal("300000"))
                    .totalPaidAmount(BigDecimal.ZERO)
                    .build();
            setId(payment, 20L);
            when(paymentRepository.findByMasterReservationId(2L))
                    .thenReturn(Optional.of(payment));

            // recalculateAmounts 스텁 - grandTotal이 300000이 되도록 설정
            SubReservation otaSub = createSub(300L, "RESERVED");
            when(subReservationRepository.findByMasterReservationId(2L))
                    .thenReturn(List.of(otaSub));
            when(dailyChargeRepository.findBySubReservationId(300L))
                    .thenReturn(List.of(createDailyCharge(otaSub, 300000, 0, 0)));
            when(serviceItemRepository.findBySubReservationId(300L))
                    .thenReturn(Collections.emptyList());
            when(adjustmentRepository.findByMasterReservationIdOrderByAdjustmentSeqAsc(2L))
                    .thenReturn(Collections.emptyList());

            when(transactionRepository.findByMasterReservationIdOrderByTransactionSeqAsc(2L))
                    .thenReturn(Collections.emptyList());
            when(transactionRepository.save(any(PaymentTransaction.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            PaymentSummaryResponse expected = PaymentSummaryResponse.builder()
                    .paymentStatus("PAID").build();
            when(reservationMapper.toPaymentSummaryResponse(any(), any(), any(), any()))
                    .thenReturn(expected);

            PaymentProcessRequest request = createPayRequest("CARD", new BigDecimal("300000"));

            // when - OTA 예약도 정상 결제 처리
            PaymentSummaryResponse result = service.processPayment(PROPERTY_ID, 2L, request);

            // then
            assertThat(result.getPaymentStatus()).isEqualTo("PAID");
            verify(transactionRepository).save(any(PaymentTransaction.class));
        }

        @Test
        @DisplayName("CHECKED_OUT 상태 예약 - RESERVATION_MODIFY_NOT_ALLOWED 예외")
        void checkedOutReservation_throwsModifyNotAllowed() {
            // given
            MasterReservation coMaster = MasterReservation.builder()
                    .property(property)
                    .masterReservationNo("GMP260310-0003")
                    .confirmationNo("CO1234")
                    .reservationStatus("CHECKED_OUT")
                    .isOtaManaged(false)
                    .build();
            setId(coMaster, 3L);
            when(masterReservationRepository.findById(3L)).thenReturn(Optional.of(coMaster));

            PaymentProcessRequest request = createPayRequest("CARD", new BigDecimal("100000"));

            // when & then
            assertThatThrownBy(() -> service.processPayment(PROPERTY_ID, 3L, request))
                    .isInstanceOf(HolaException.class)
                    .extracting(e -> ((HolaException) e).getErrorCode())
                    .isEqualTo(ErrorCode.RESERVATION_MODIFY_NOT_ALLOWED);
        }

        @Test
        @DisplayName("거래 시퀀스 번호 자동 증가 - 기존 2건이면 nextSeq=3")
        void seqIncrement_correctlyAssigned() {
            // given
            stubMasterFound();
            // grandTotal=300000, paid=100000 -> remaining=200000
            ReservationPayment payment = createPayment("PARTIAL", new BigDecimal("300000"), new BigDecimal("100000"));
            when(paymentRepository.findByMasterReservationId(RESERVATION_ID))
                    .thenReturn(Optional.of(payment));

            // recalculate 후 grandTotal = 300000
            stubRecalculateWithTotal(300000, 0, 0);

            PaymentTransaction txn1 = PaymentTransaction.builder()
                    .masterReservationId(RESERVATION_ID).transactionSeq(1)
                    .paymentMethod("CARD").amount(new BigDecimal("50000")).build();
            PaymentTransaction txn2 = PaymentTransaction.builder()
                    .masterReservationId(RESERVATION_ID).transactionSeq(2)
                    .paymentMethod("CASH").amount(new BigDecimal("50000")).build();
            when(transactionRepository.findByMasterReservationIdOrderByTransactionSeqAsc(RESERVATION_ID))
                    .thenReturn(List.of(txn1, txn2));
            when(transactionRepository.save(any(PaymentTransaction.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(reservationMapper.toPaymentSummaryResponse(any(), any(), any(), any()))
                    .thenReturn(PaymentSummaryResponse.builder().build());

            PaymentProcessRequest request = createPayRequest("CARD", new BigDecimal("100000"));

            // when
            service.processPayment(PROPERTY_ID, RESERVATION_ID, request);

            // then
            ArgumentCaptor<PaymentTransaction> captor = ArgumentCaptor.forClass(PaymentTransaction.class);
            verify(transactionRepository).save(captor.capture());
            assertThat(captor.getValue().getTransactionSeq()).isEqualTo(3);
        }
    }

    // ═══════════════════════════════════════════════
    //  3. addAdjustment (4개)
    // ═══════════════════════════════════════════════

    @Nested
    @DisplayName("addAdjustment - 금액 조정 추가")
    class AddAdjustment {

        @Test
        @DisplayName("할증 (+) 조정 등록 성공")
        void surchargeAdjustment_savesCorrectly() {
            // given
            stubMasterFound();
            when(paymentRepository.findByMasterReservationId(RESERVATION_ID))
                    .thenReturn(Optional.empty());
            when(adjustmentRepository.findByMasterReservationIdOrderByAdjustmentSeqAsc(RESERVATION_ID))
                    .thenReturn(Collections.emptyList());
            when(adjustmentRepository.save(any(PaymentAdjustment.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // recalculatePayment 내부: getOrCreatePayment + recalculateAmounts
            when(paymentRepository.save(any(ReservationPayment.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(subReservationRepository.findByMasterReservationId(RESERVATION_ID))
                    .thenReturn(Collections.emptyList());

            PaymentAdjustmentResponse expected = PaymentAdjustmentResponse.builder()
                    .adjustmentSeq(1).adjustmentSign("+").totalAmount(new BigDecimal("20000")).build();
            when(reservationMapper.toPaymentAdjustmentResponse(any(PaymentAdjustment.class)))
                    .thenReturn(expected);

            PaymentAdjustmentRequest request = PaymentAdjustmentRequest.builder()
                    .adjustmentSign("+")
                    .supplyPrice(new BigDecimal("20000"))
                    .tax(BigDecimal.ZERO)
                    .totalAmount(new BigDecimal("20000"))
                    .comment("할증 조정")
                    .build();

            // when
            PaymentAdjustmentResponse result = service.addAdjustment(PROPERTY_ID, RESERVATION_ID, request);

            // then
            assertThat(result.getAdjustmentSign()).isEqualTo("+");
            assertThat(result.getTotalAmount()).isEqualByComparingTo(new BigDecimal("20000"));
            verify(adjustmentRepository).save(any(PaymentAdjustment.class));
        }

        @Test
        @DisplayName("할인 (-) 조정 등록 성공")
        void discountAdjustment_savesCorrectly() {
            // given
            stubMasterFound();
            when(paymentRepository.findByMasterReservationId(RESERVATION_ID))
                    .thenReturn(Optional.empty());
            when(adjustmentRepository.findByMasterReservationIdOrderByAdjustmentSeqAsc(RESERVATION_ID))
                    .thenReturn(Collections.emptyList());
            when(adjustmentRepository.save(any(PaymentAdjustment.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // recalculatePayment 내부
            when(paymentRepository.save(any(ReservationPayment.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(subReservationRepository.findByMasterReservationId(RESERVATION_ID))
                    .thenReturn(Collections.emptyList());

            PaymentAdjustmentResponse expected = PaymentAdjustmentResponse.builder()
                    .adjustmentSeq(1).adjustmentSign("-").totalAmount(new BigDecimal("15000")).build();
            when(reservationMapper.toPaymentAdjustmentResponse(any(PaymentAdjustment.class)))
                    .thenReturn(expected);

            PaymentAdjustmentRequest request = PaymentAdjustmentRequest.builder()
                    .adjustmentSign("-")
                    .supplyPrice(new BigDecimal("15000"))
                    .tax(BigDecimal.ZERO)
                    .totalAmount(new BigDecimal("15000"))
                    .comment("할인 조정")
                    .build();

            // when
            PaymentAdjustmentResponse result = service.addAdjustment(PROPERTY_ID, RESERVATION_ID, request);

            // then
            assertThat(result.getAdjustmentSign()).isEqualTo("-");
            verify(adjustmentRepository).save(any(PaymentAdjustment.class));
        }

        @Test
        @DisplayName("CHECKED_OUT 상태 예약 - RESERVATION_PAYMENT_MODIFY_NOT_ALLOWED 예외")
        void checkedOutReservation_throwsModifyNotAllowed() {
            // given
            MasterReservation coMaster = MasterReservation.builder()
                    .property(property)
                    .masterReservationNo("GMP260310-0004")
                    .confirmationNo("CO4567")
                    .reservationStatus("CHECKED_OUT")
                    .isOtaManaged(false)
                    .build();
            setId(coMaster, 4L);
            when(masterReservationRepository.findById(4L)).thenReturn(Optional.of(coMaster));

            PaymentAdjustmentRequest request = PaymentAdjustmentRequest.builder()
                    .adjustmentSign("+")
                    .supplyPrice(new BigDecimal("10000"))
                    .tax(BigDecimal.ZERO)
                    .totalAmount(new BigDecimal("10000"))
                    .comment("테스트")
                    .build();

            // when & then
            assertThatThrownBy(() -> service.addAdjustment(PROPERTY_ID, 4L, request))
                    .isInstanceOf(HolaException.class)
                    .extracting(e -> ((HolaException) e).getErrorCode())
                    .isEqualTo(ErrorCode.RESERVATION_PAYMENT_MODIFY_NOT_ALLOWED);
        }

        @Test
        @DisplayName("시퀀스 자동 증가 - 기존 2건이면 nextSeq=3")
        void seqAutoIncrement_correctlyAssigned() {
            // given
            stubMasterFound();
            when(paymentRepository.findByMasterReservationId(RESERVATION_ID))
                    .thenReturn(Optional.empty());

            PaymentAdjustment adj1 = createAdjustment(1, "+", 10000);
            PaymentAdjustment adj2 = createAdjustment(2, "-", 5000);
            when(adjustmentRepository.findByMasterReservationIdOrderByAdjustmentSeqAsc(RESERVATION_ID))
                    .thenReturn(List.of(adj1, adj2));
            when(adjustmentRepository.save(any(PaymentAdjustment.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // recalculatePayment 내부
            when(paymentRepository.save(any(ReservationPayment.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(subReservationRepository.findByMasterReservationId(RESERVATION_ID))
                    .thenReturn(Collections.emptyList());

            when(reservationMapper.toPaymentAdjustmentResponse(any(PaymentAdjustment.class)))
                    .thenReturn(PaymentAdjustmentResponse.builder().adjustmentSeq(3).build());

            PaymentAdjustmentRequest request = PaymentAdjustmentRequest.builder()
                    .adjustmentSign("+")
                    .supplyPrice(new BigDecimal("30000"))
                    .tax(BigDecimal.ZERO)
                    .totalAmount(new BigDecimal("30000"))
                    .comment("추가 조정")
                    .build();

            // when
            service.addAdjustment(PROPERTY_ID, RESERVATION_ID, request);

            // then
            ArgumentCaptor<PaymentAdjustment> captor = ArgumentCaptor.forClass(PaymentAdjustment.class);
            verify(adjustmentRepository).save(captor.capture());
            assertThat(captor.getValue().getAdjustmentSeq()).isEqualTo(3);
        }
    }

    // ═══════════════════════════════════════════════
    //  4. recalculatePayment (6개)
    // ═══════════════════════════════════════════════

    @Nested
    @DisplayName("recalculatePayment - 결제 금액 재계산")
    class RecalculatePayment {

        @Test
        @DisplayName("객실요금 + 서비스요금 + 조정금액 정상 합산")
        void roomServiceAdjustment_calculatesCorrectly() {
            // given
            stubMasterFound();
            ReservationPayment payment = createPayment("UNPAID", BigDecimal.ZERO, BigDecimal.ZERO);
            when(paymentRepository.findByMasterReservationId(RESERVATION_ID))
                    .thenReturn(Optional.of(payment));

            SubReservation sub = createSub(100L, "RESERVED");
            when(subReservationRepository.findByMasterReservationId(RESERVATION_ID))
                    .thenReturn(List.of(sub));

            // 객실 요금: supplyPrice(100000) + tax(10000) = 110000, serviceCharge(5000)
            DailyCharge charge = createDailyCharge(sub, 100000, 10000, 5000);
            when(dailyChargeRepository.findBySubReservationId(100L))
                    .thenReturn(List.of(charge));

            // 서비스 요금: 20000
            ReservationServiceItem svcItem = createServiceItem(sub, 20000);
            when(serviceItemRepository.findBySubReservationId(100L))
                    .thenReturn(List.of(svcItem));

            // 조정: +10000
            PaymentAdjustment adj = createAdjustment(1, "+", 10000);
            when(adjustmentRepository.findByMasterReservationIdOrderByAdjustmentSeqAsc(RESERVATION_ID))
                    .thenReturn(List.of(adj));

            // when
            service.recalculatePayment(RESERVATION_ID);

            // then
            // totalRoom = 100000+10000 = 110000
            assertThat(payment.getTotalRoomAmount()).isEqualByComparingTo(new BigDecimal("110000"));
            // totalService = 20000
            assertThat(payment.getTotalServiceAmount()).isEqualByComparingTo(new BigDecimal("20000"));
            // totalServiceCharge = 5000
            assertThat(payment.getTotalServiceChargeAmount()).isEqualByComparingTo(new BigDecimal("5000"));
            // totalAdjustment = +10000
            assertThat(payment.getTotalAdjustmentAmount()).isEqualByComparingTo(new BigDecimal("10000"));
            // grandTotal = 110000 + 20000 + 5000 + 10000 + 0 (earlyLate) = 145000
            assertThat(payment.getGrandTotal()).isEqualByComparingTo(new BigDecimal("145000"));
        }

        @Test
        @DisplayName("CANCELED 상태 서브 예약은 합산에서 제외")
        void canceledSubExcluded_calculatesCorrectly() {
            // given
            stubMasterFound();
            ReservationPayment payment = createPayment("UNPAID", BigDecimal.ZERO, BigDecimal.ZERO);
            when(paymentRepository.findByMasterReservationId(RESERVATION_ID))
                    .thenReturn(Optional.of(payment));

            SubReservation activeSub = createSub(100L, "RESERVED");
            SubReservation canceledSub = createSub(101L, "CANCELED");
            when(subReservationRepository.findByMasterReservationId(RESERVATION_ID))
                    .thenReturn(List.of(activeSub, canceledSub));

            DailyCharge activeCharge = createDailyCharge(activeSub, 100000, 10000, 5000);
            when(dailyChargeRepository.findBySubReservationId(100L))
                    .thenReturn(List.of(activeCharge));

            when(serviceItemRepository.findBySubReservationId(100L))
                    .thenReturn(Collections.emptyList());
            when(adjustmentRepository.findByMasterReservationIdOrderByAdjustmentSeqAsc(RESERVATION_ID))
                    .thenReturn(Collections.emptyList());

            // when
            service.recalculatePayment(RESERVATION_ID);

            // then
            // CANCELED 서브의 DailyCharge는 조회 자체를 하지 않음 (verify로 확인)
            verify(dailyChargeRepository, never()).findBySubReservationId(101L);
            assertThat(payment.getTotalRoomAmount()).isEqualByComparingTo(new BigDecimal("110000"));
            assertThat(payment.getGrandTotal()).isEqualByComparingTo(new BigDecimal("115000"));
        }

        @Test
        @DisplayName("얼리 체크인 + 레이트 체크아웃 요금 포함")
        void earlyLateFeeIncluded_calculatesCorrectly() {
            // given
            stubMasterFound();
            ReservationPayment payment = createPayment("UNPAID", BigDecimal.ZERO, BigDecimal.ZERO);
            when(paymentRepository.findByMasterReservationId(RESERVATION_ID))
                    .thenReturn(Optional.of(payment));

            SubReservation sub = SubReservation.builder()
                    .masterReservation(master)
                    .subReservationNo("GMP260310-0001-200")
                    .roomReservationStatus("INHOUSE")
                    .earlyCheckInFee(new BigDecimal("30000"))
                    .lateCheckOutFee(new BigDecimal("20000"))
                    .build();
            setId(sub, 200L);
            when(subReservationRepository.findByMasterReservationId(RESERVATION_ID))
                    .thenReturn(List.of(sub));

            DailyCharge charge = createDailyCharge(sub, 100000, 10000, 5000);
            when(dailyChargeRepository.findBySubReservationId(200L))
                    .thenReturn(List.of(charge));
            when(serviceItemRepository.findBySubReservationId(200L))
                    .thenReturn(Collections.emptyList());
            when(adjustmentRepository.findByMasterReservationIdOrderByAdjustmentSeqAsc(RESERVATION_ID))
                    .thenReturn(Collections.emptyList());

            // when
            service.recalculatePayment(RESERVATION_ID);

            // then
            assertThat(payment.getTotalEarlyLateFee()).isEqualByComparingTo(new BigDecimal("50000"));
            // grandTotal = 110000(room) + 0(service) + 5000(serviceCharge) + 0(adj) + 50000(earlyLate) = 165000
            assertThat(payment.getGrandTotal()).isEqualByComparingTo(new BigDecimal("165000"));
        }

        @Test
        @DisplayName("grandTotal이 0 이하이면 결제 상태를 PAID로 설정")
        void grandTotalZeroOrBelow_statusSetToPaid() {
            // given
            stubMasterFound();
            ReservationPayment payment = createPayment("UNPAID", BigDecimal.ZERO, BigDecimal.ZERO);
            when(paymentRepository.findByMasterReservationId(RESERVATION_ID))
                    .thenReturn(Optional.of(payment));

            SubReservation sub = createSub(100L, "RESERVED");
            when(subReservationRepository.findByMasterReservationId(RESERVATION_ID))
                    .thenReturn(List.of(sub));

            DailyCharge charge = createDailyCharge(sub, 100000, 10000, 5000);
            when(dailyChargeRepository.findBySubReservationId(100L))
                    .thenReturn(List.of(charge));
            when(serviceItemRepository.findBySubReservationId(100L))
                    .thenReturn(Collections.emptyList());

            // 할인 조정으로 grandTotal을 0 이하로 만듦: -200000
            PaymentAdjustment adj = createAdjustment(1, "-", 200000);
            when(adjustmentRepository.findByMasterReservationIdOrderByAdjustmentSeqAsc(RESERVATION_ID))
                    .thenReturn(List.of(adj));

            // when
            service.recalculatePayment(RESERVATION_ID);

            // then
            // grandTotal = 110000 + 0 + 5000 + (-200000) + 0 = -85000 → 음수 하한 적용으로 0
            assertThat(payment.getGrandTotal()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(payment.getPaymentStatus()).isEqualTo("PAID");
        }

        @Test
        @DisplayName("결제 정보가 없으면 신규 생성 후 재계산")
        void noPaymentExists_createsNewPayment() {
            // given
            stubMasterFound();
            when(paymentRepository.findByMasterReservationId(RESERVATION_ID))
                    .thenReturn(Optional.empty());

            ReservationPayment newPayment = createPayment("UNPAID", BigDecimal.ZERO, BigDecimal.ZERO);
            when(paymentRepository.save(any(ReservationPayment.class)))
                    .thenReturn(newPayment);

            when(subReservationRepository.findByMasterReservationId(RESERVATION_ID))
                    .thenReturn(Collections.emptyList());
            when(adjustmentRepository.findByMasterReservationIdOrderByAdjustmentSeqAsc(RESERVATION_ID))
                    .thenReturn(Collections.emptyList());

            // when
            service.recalculatePayment(RESERVATION_ID);

            // then
            verify(paymentRepository).save(any(ReservationPayment.class));
        }

        @Test
        @DisplayName("조정 금액 + / - 부호에 따라 합산/차감 처리")
        void adjustmentsSigns_calculatesCorrectly() {
            // given
            stubMasterFound();
            ReservationPayment payment = createPayment("UNPAID", BigDecimal.ZERO, BigDecimal.ZERO);
            when(paymentRepository.findByMasterReservationId(RESERVATION_ID))
                    .thenReturn(Optional.of(payment));

            when(subReservationRepository.findByMasterReservationId(RESERVATION_ID))
                    .thenReturn(Collections.emptyList());

            // +30000, -10000, +5000 = 순 조정 = +25000
            PaymentAdjustment adj1 = createAdjustment(1, "+", 30000);
            PaymentAdjustment adj2 = createAdjustment(2, "-", 10000);
            PaymentAdjustment adj3 = createAdjustment(3, "+", 5000);
            when(adjustmentRepository.findByMasterReservationIdOrderByAdjustmentSeqAsc(RESERVATION_ID))
                    .thenReturn(List.of(adj1, adj2, adj3));

            // when
            service.recalculatePayment(RESERVATION_ID);

            // then
            assertThat(payment.getTotalAdjustmentAmount()).isEqualByComparingTo(new BigDecimal("25000"));
            // grandTotal = 0(room) + 0(service) + 0(serviceCharge) + 25000(adj) + 0(earlyLate) = 25000
            assertThat(payment.getGrandTotal()).isEqualByComparingTo(new BigDecimal("25000"));
        }
    }

    // ═══════════════════════════════════════════════
    //  VAN 결제 테스트
    // ═══════════════════════════════════════════════

    @Nested
    @DisplayName("processPayment - VAN 결제")
    class VanPaymentTest {

        private VanResultPayload createVanResult(String respCode, String transType) {
            return new VanResultPayload(
                    true, respCode, "approve", "승인완료",
                    transType, "20260402AD0001", "12345678",
                    "BC", "비씨카드", "KB", "국민카드",
                    "4321****1234", "A12345", "0788888"
            );
        }

        @Test
        @DisplayName("VAN 카드결제 성공 - VAN 필드가 PaymentTransaction에 매핑됨")
        void vanCardPayment_success() {
            // given
            stubMasterFound();
            ReservationPayment payment = createPayment("UNPAID", new BigDecimal("100000"), BigDecimal.ZERO);
            when(paymentRepository.findByMasterReservationId(RESERVATION_ID)).thenReturn(Optional.of(payment));
            stubRecalculateWithTotal(90909, 9091, 0);
            when(transactionRepository.findByMasterReservationIdOrderByTransactionSeqAsc(RESERVATION_ID))
                    .thenReturn(Collections.emptyList());

            PaymentProcessRequest request = new PaymentProcessRequest("CARD", new BigDecimal("100000"), "VAN 카드결제");
            request.setSubReservationId(SUB_ID);
            // VAN 필드는 리플렉션으로 설정
            setField(request, "paymentChannel", "VAN");
            setField(request, "workstationId", 1L);
            setField(request, "vanResult", createVanResult("0000", "I1"));

            when(reservationMapper.toPaymentSummaryResponse(any(), any(), any(), any()))
                    .thenReturn(PaymentSummaryResponse.builder().paymentStatus("PAID").build());

            // when
            service.processPayment(PROPERTY_ID, RESERVATION_ID, request);

            // then
            ArgumentCaptor<PaymentTransaction> captor = ArgumentCaptor.forClass(PaymentTransaction.class);
            verify(transactionRepository).save(captor.capture());
            PaymentTransaction saved = captor.getValue();

            assertThat(saved.getPaymentChannel()).isEqualTo("VAN");
            assertThat(saved.getVanProvider()).isEqualTo("KPSP");
            assertThat(saved.getVanAuthCode()).isEqualTo("A12345");
            assertThat(saved.getVanRrn()).isEqualTo("12345678");
            assertThat(saved.getVanPan()).isEqualTo("4321****1234");
            assertThat(saved.getVanIssuerName()).isEqualTo("비씨카드");
            assertThat(saved.getVanAcquirerName()).isEqualTo("국민카드");
            assertThat(saved.getWorkstationId()).isEqualTo(1L);
            assertThat(saved.getApprovalNo()).isEqualTo("A12345");
        }

        @Test
        @DisplayName("VAN 결제 거절 (respCode != 0000) - VAN_PAYMENT_FAILED 예외")
        void vanPayment_declined() {
            // given
            stubMasterFound();
            ReservationPayment payment = createPayment("UNPAID", new BigDecimal("100000"), BigDecimal.ZERO);
            when(paymentRepository.findByMasterReservationId(RESERVATION_ID)).thenReturn(Optional.of(payment));
            stubRecalculateWithTotal(90909, 9091, 0);
            when(transactionRepository.findByMasterReservationIdOrderByTransactionSeqAsc(RESERVATION_ID))
                    .thenReturn(Collections.emptyList());

            PaymentProcessRequest request = new PaymentProcessRequest("CARD", new BigDecimal("100000"), null);
            request.setSubReservationId(SUB_ID);
            setField(request, "paymentChannel", "VAN");
            setField(request, "workstationId", 1L);
            setField(request, "vanResult", createVanResult("9999", "I1"));

            // when & then
            assertThatThrownBy(() -> service.processPayment(PROPERTY_ID, RESERVATION_ID, request))
                    .isInstanceOf(HolaException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VAN_PAYMENT_FAILED);
        }

        @Test
        @DisplayName("수동결제 (paymentChannel=null) - VAN 필드 없이 기존 동작 유지")
        void manualPayment_noVanFields() {
            // given
            stubMasterFound();
            ReservationPayment payment = createPayment("UNPAID", new BigDecimal("50000"), BigDecimal.ZERO);
            when(paymentRepository.findByMasterReservationId(RESERVATION_ID)).thenReturn(Optional.of(payment));
            stubRecalculateWithTotal(45455, 4545, 0);
            when(transactionRepository.findByMasterReservationIdOrderByTransactionSeqAsc(RESERVATION_ID))
                    .thenReturn(Collections.emptyList());

            PaymentProcessRequest request = new PaymentProcessRequest("CASH", new BigDecimal("50000"), "수동결제");
            request.setSubReservationId(SUB_ID);

            when(reservationMapper.toPaymentSummaryResponse(any(), any(), any(), any()))
                    .thenReturn(PaymentSummaryResponse.builder().paymentStatus("PAID").build());

            // when
            service.processPayment(PROPERTY_ID, RESERVATION_ID, request);

            // then
            ArgumentCaptor<PaymentTransaction> captor = ArgumentCaptor.forClass(PaymentTransaction.class);
            verify(transactionRepository).save(captor.capture());
            PaymentTransaction saved = captor.getValue();

            assertThat(saved.getPaymentChannel()).isNull();
            assertThat(saved.getVanAuthCode()).isNull();
            assertThat(saved.getWorkstationId()).isNull();
        }

        private void setField(Object target, String fieldName, Object value) {
            try {
                Field field = target.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
            } catch (Exception e) {
                throw new RuntimeException("setField 실패: " + fieldName, e);
            }
        }
    }

    @Nested
    @DisplayName("getVanCancelInfo - VAN 취소 정보 조회")
    class VanCancelInfoTest {

        @Test
        @DisplayName("VAN 거래의 취소 정보 정상 반환")
        void vanCancelInfo_success() {
            // given
            stubMasterFound();
            PaymentTransaction vanTxn = PaymentTransaction.builder()
                    .masterReservationId(RESERVATION_ID)
                    .transactionSeq(1)
                    .paymentMethod("CARD")
                    .amount(new BigDecimal("100000"))
                    .paymentChannel("VAN")
                    .vanAuthCode("A12345")
                    .vanRrn("12345678")
                    .vanSequenceNo("20260402AD0001")
                    .workstationId(1L)
                    .build();
            setId(vanTxn, 50L);

            when(transactionRepository.findById(50L)).thenReturn(Optional.of(vanTxn));
            when(transactionRepository.findByMasterReservationIdOrderByTransactionSeqAsc(RESERVATION_ID))
                    .thenReturn(List.of(vanTxn));

            com.hola.hotel.entity.Workstation ws = com.hola.hotel.entity.Workstation.builder()
                    .wsNo("ADMIN").kpspHost("localhost").kpspPort(19090).build();
            when(workstationService.findById(1L)).thenReturn(ws);

            // when
            var result = service.getVanCancelInfo(PROPERTY_ID, RESERVATION_ID, 50L);

            // then
            assertThat(result.getAuthCode()).isEqualTo("A12345");
            assertThat(result.getRrn()).isEqualTo("12345678");
            assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("100000"));
            assertThat(result.getWsNo()).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("VAN이 아닌 거래 취소 시도 - VAN_CANCEL_NOT_ALLOWED 예외")
        void vanCancelInfo_notVanTxn() {
            // given
            stubMasterFound();
            PaymentTransaction manualTxn = PaymentTransaction.builder()
                    .masterReservationId(RESERVATION_ID)
                    .transactionSeq(1)
                    .paymentMethod("CASH")
                    .amount(new BigDecimal("50000"))
                    .build();
            setId(manualTxn, 60L);

            when(transactionRepository.findById(60L)).thenReturn(Optional.of(manualTxn));

            // when & then
            assertThatThrownBy(() -> service.getVanCancelInfo(PROPERTY_ID, RESERVATION_ID, 60L))
                    .isInstanceOf(HolaException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VAN_CANCEL_NOT_ALLOWED);
        }
    }

    @Nested
    @DisplayName("processVanCancel - VAN 취소 처리")
    class VanCancelTest {

        @Test
        @DisplayName("VAN 취소 성공 - REFUND 트랜잭션 생성")
        void vanCancel_success() {
            // given
            stubMasterFound();
            ReservationPayment payment = createPayment("PAID", new BigDecimal("100000"), new BigDecimal("100000"));
            when(paymentRepository.findByMasterReservationId(RESERVATION_ID)).thenReturn(Optional.of(payment));

            PaymentTransaction originalTxn = PaymentTransaction.builder()
                    .masterReservationId(RESERVATION_ID)
                    .subReservationId(SUB_ID)
                    .transactionSeq(1)
                    .paymentMethod("CARD")
                    .amount(new BigDecimal("100000"))
                    .paymentChannel("VAN")
                    .vanSequenceNo("20260402AD0001")
                    .workstationId(1L)
                    .build();
            setId(originalTxn, 50L);

            when(transactionRepository.findById(50L)).thenReturn(Optional.of(originalTxn));
            when(transactionRepository.findByMasterReservationIdOrderByTransactionSeqAsc(RESERVATION_ID))
                    .thenReturn(List.of(originalTxn));

            VanResultPayload cancelResult = new VanResultPayload(
                    true, "0000", "approve", "취소승인",
                    "I4", "20260402AD0001", "87654321",
                    "BC", "비씨카드", "KB", "국민카드",
                    "4321****1234", "C67890", "0788888"
            );

            when(reservationMapper.toPaymentSummaryResponse(any(), any(), any(), any()))
                    .thenReturn(PaymentSummaryResponse.builder().paymentStatus("REFUNDED").build());

            // when
            service.processVanCancel(PROPERTY_ID, RESERVATION_ID, 50L, cancelResult);

            // then
            ArgumentCaptor<PaymentTransaction> captor = ArgumentCaptor.forClass(PaymentTransaction.class);
            verify(transactionRepository).save(captor.capture());
            PaymentTransaction saved = captor.getValue();

            assertThat(saved.getTransactionType()).isEqualTo("REFUND");
            assertThat(saved.getPaymentChannel()).isEqualTo("VAN");
            assertThat(saved.getVanAuthCode()).isEqualTo("C67890");
            assertThat(saved.getAmount()).isEqualByComparingTo(new BigDecimal("100000"));
        }
    }
}
