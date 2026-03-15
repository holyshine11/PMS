package com.hola.reservation.mapper;

import com.hola.hotel.entity.Hotel;
import com.hola.hotel.entity.Property;
import com.hola.reservation.entity.*;
import com.hola.reservation.dto.response.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ReservationMapper 테스트
 */
@DisplayName("ReservationMapper")
class ReservationMapperTest {

    private final ReservationMapper mapper = new ReservationMapper();

    private Property createProperty() {
        Hotel hotel = Hotel.builder().hotelCode("HTL00001").hotelName("테스트 호텔").build();
        return Property.builder()
                .hotel(hotel).propertyCode("GMP").propertyName("테스트")
                .checkInTime("15:00").checkOutTime("11:00")
                .taxRate(BigDecimal.ZERO).serviceChargeRate(BigDecimal.ZERO)
                .taxDecimalPlaces(0).serviceChargeDecimalPlaces(0)
                .build();
    }

    private MasterReservation createMaster() {
        return MasterReservation.builder()
                .property(createProperty())
                .masterReservationNo("GMP260315-0001")
                .confirmationNo("HK4F29XP")
                .reservationStatus("RESERVED")
                .masterCheckIn(LocalDate.of(2026, 3, 15))
                .masterCheckOut(LocalDate.of(2026, 3, 18))
                .guestNameKo("홍길동")
                .guestFirstNameEn("Gildong")
                .guestLastNameEn("Hong")
                .phoneNumber("01012345678")
                .email("hong@test.com")
                .isOtaManaged(false)
                .subReservations(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("리스트 응답 매핑 - 핵심 필드 검증")
    void toReservationListResponse_mapsFields() {
        MasterReservation master = createMaster();
        ReservationListResponse response = mapper.toReservationListResponse(master);

        assertThat(response.getMasterReservationNo()).isEqualTo("GMP260315-0001");
        assertThat(response.getConfirmationNo()).isEqualTo("HK4F29XP");
        assertThat(response.getReservationStatus()).isEqualTo("RESERVED");
        assertThat(response.getGuestNameKo()).isEqualTo("홍길동");
        assertThat(response.getMasterCheckIn()).isEqualTo(LocalDate.of(2026, 3, 15));
    }

    @Test
    @DisplayName("상세 응답 매핑 - 모든 필드 포함")
    void toReservationDetailResponse_mapsAllFields() {
        MasterReservation master = createMaster();
        ReservationDetailResponse response = mapper.toReservationDetailResponse(master);

        assertThat(response.getMasterReservationNo()).isEqualTo("GMP260315-0001");
        assertThat(response.getGuestFirstNameEn()).isEqualTo("Gildong");
        assertThat(response.getGuestLastNameEn()).isEqualTo("Hong");
        assertThat(response.getEmail()).isEqualTo("hong@test.com");
        assertThat(response.getIsOtaManaged()).isFalse();
        assertThat(response.getSubReservations()).isEmpty();
    }

    @Test
    @DisplayName("결제 요약 매핑 - remaining 계산 확인")
    void toPaymentSummaryResponse_calculatesRemaining() {
        MasterReservation master = createMaster();
        ReservationPayment payment = ReservationPayment.builder()
                .masterReservation(master)
                .paymentStatus("PARTIAL")
                .totalRoomAmount(new BigDecimal("300000"))
                .totalServiceAmount(BigDecimal.ZERO)
                .totalServiceChargeAmount(new BigDecimal("15000"))
                .totalAdjustmentAmount(BigDecimal.ZERO)
                .totalEarlyLateFee(BigDecimal.ZERO)
                .grandTotal(new BigDecimal("345000"))
                .totalPaidAmount(new BigDecimal("100000"))
                .cancelFeeAmount(BigDecimal.ZERO)
                .refundAmount(BigDecimal.ZERO)
                .build();

        PaymentSummaryResponse response = mapper.toPaymentSummaryResponse(
                payment, List.of(), List.of());

        assertThat(response.getGrandTotal()).isEqualByComparingTo("345000");
        assertThat(response.getTotalPaidAmount()).isEqualByComparingTo("100000");
        assertThat(response.getRemainingAmount()).isEqualByComparingTo("245000");
        assertThat(response.getPaymentStatus()).isEqualTo("PARTIAL");
    }

    @Test
    @DisplayName("카드번호 마스킹 - 앞4 + **** + 뒤4")
    void toReservationDepositResponse_masksCardNumber() {
        ReservationDeposit deposit = ReservationDeposit.builder()
                .depositMethod("CREDIT_CARD")
                .cardCompany("삼성카드")
                .cardNumberEncrypted("1234567890123456")
                .cardExpiryDate("12/28")
                .currency("KRW")
                .amount(new BigDecimal("100000"))
                .build();

        ReservationDepositResponse response = mapper.toReservationDepositResponse(deposit);
        assertThat(response.getCardNumberMasked()).isEqualTo("1234****3456");
    }

    @Test
    @DisplayName("게스트 응답 매핑")
    void toReservationGuestResponse_mapsFields() {
        ReservationGuest guest = ReservationGuest.builder()
                .guestSeq(1)
                .guestNameKo("김철수")
                .guestFirstNameEn("Cheolsu")
                .guestLastNameEn("Kim")
                .build();

        ReservationGuestResponse response = mapper.toReservationGuestResponse(guest);
        assertThat(response.getGuestNameKo()).isEqualTo("김철수");
        assertThat(response.getGuestSeq()).isEqualTo(1);
    }

    @Test
    @DisplayName("결제 거래 이력 매핑")
    void toPaymentTransactionResponse_mapsFields() {
        PaymentTransaction tx = PaymentTransaction.builder()
                .transactionSeq(1)
                .transactionType("PAYMENT")
                .paymentMethod("CARD")
                .amount(new BigDecimal("100000"))
                .currency("KRW")
                .transactionStatus("COMPLETED")
                .build();

        PaymentTransactionResponse response = mapper.toPaymentTransactionResponse(tx);
        assertThat(response.getTransactionType()).isEqualTo("PAYMENT");
        assertThat(response.getAmount()).isEqualByComparingTo("100000");
    }
}
