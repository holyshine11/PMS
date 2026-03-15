package com.hola.fixture;

import com.hola.hotel.entity.CancellationFee;
import com.hola.hotel.entity.EarlyLateFeePolicy;
import com.hola.hotel.entity.Hotel;
import com.hola.hotel.entity.Property;
import com.hola.rate.entity.RatePricing;
import com.hola.rate.entity.RatePricingPerson;
import com.hola.reservation.entity.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 테스트 픽스처 팩토리
 * 엔티티/DTO 생성 헬퍼 메서드 모음
 */
public final class TestFixtures {

    private TestFixtures() {}

    // ========== Hotel & Property ==========

    public static Hotel createHotel() {
        return Hotel.builder()
                .hotelCode("HTL00001")
                .hotelName("테스트 호텔")
                .build();
    }

    public static Property createProperty() {
        return createProperty(createHotel());
    }

    public static Property createProperty(Hotel hotel) {
        return Property.builder()
                .hotel(hotel)
                .propertyCode("GMP")
                .propertyName("테스트 프로퍼티")
                .checkInTime("15:00")
                .checkOutTime("11:00")
                .taxRate(new BigDecimal("10"))
                .taxDecimalPlaces(0)
                .taxRoundingMethod("ROUND_DOWN")
                .serviceChargeRate(new BigDecimal("5"))
                .serviceChargeDecimalPlaces(0)
                .serviceChargeRoundingMethod("ROUND_DOWN")
                .starRating("5")
                .totalRooms(100)
                .build();
    }

    public static Property createPropertyNoTax(Hotel hotel) {
        return Property.builder()
                .hotel(hotel)
                .propertyCode("GMP")
                .propertyName("테스트 프로퍼티")
                .checkInTime("15:00")
                .checkOutTime("11:00")
                .taxRate(BigDecimal.ZERO)
                .taxDecimalPlaces(0)
                .serviceChargeRate(BigDecimal.ZERO)
                .serviceChargeDecimalPlaces(0)
                .build();
    }

    // ========== RatePricing ==========

    public static RatePricing createRatePricing(Long rateCodeId, LocalDate start, LocalDate end,
                                                  BigDecimal baseSupplyPrice) {
        return RatePricing.builder()
                .rateCodeId(rateCodeId)
                .startDate(start)
                .endDate(end)
                .dayMon(true).dayTue(true).dayWed(true).dayThu(true)
                .dayFri(true).daySat(true).daySun(true)
                .baseSupplyPrice(baseSupplyPrice)
                .baseTax(BigDecimal.ZERO)
                .baseTotal(baseSupplyPrice)
                .persons(new ArrayList<>())
                .build();
    }

    /**
     * 주중만 적용 요금표
     */
    public static RatePricing createWeekdayPricing(Long rateCodeId, LocalDate start, LocalDate end,
                                                     BigDecimal baseSupplyPrice) {
        return RatePricing.builder()
                .rateCodeId(rateCodeId)
                .startDate(start)
                .endDate(end)
                .dayMon(true).dayTue(true).dayWed(true).dayThu(true).dayFri(true)
                .daySat(false).daySun(false)
                .baseSupplyPrice(baseSupplyPrice)
                .baseTax(BigDecimal.ZERO)
                .baseTotal(baseSupplyPrice)
                .persons(new ArrayList<>())
                .build();
    }

    /**
     * 주말만 적용 요금표
     */
    public static RatePricing createWeekendPricing(Long rateCodeId, LocalDate start, LocalDate end,
                                                     BigDecimal baseSupplyPrice) {
        return RatePricing.builder()
                .rateCodeId(rateCodeId)
                .startDate(start)
                .endDate(end)
                .dayMon(false).dayTue(false).dayWed(false).dayThu(false).dayFri(false)
                .daySat(true).daySun(true)
                .baseSupplyPrice(baseSupplyPrice)
                .baseTax(BigDecimal.ZERO)
                .baseTotal(baseSupplyPrice)
                .persons(new ArrayList<>())
                .build();
    }

    public static RatePricingPerson createPricingPerson(String personType, int seq, BigDecimal supplyPrice) {
        return RatePricingPerson.builder()
                .personType(personType)
                .personSeq(seq)
                .supplyPrice(supplyPrice)
                .tax(BigDecimal.ZERO)
                .totalPrice(supplyPrice)
                .build();
    }

    // ========== Reservation ==========

    public static MasterReservation createMasterReservation(Property property) {
        return createMasterReservation(property, "RESERVED");
    }

    public static MasterReservation createMasterReservation(Property property, String status) {
        return MasterReservation.builder()
                .property(property)
                .masterReservationNo("GMP260310-0001")
                .confirmationNo("HK4F29XP")
                .reservationStatus(status)
                .masterCheckIn(LocalDate.of(2026, 3, 15))
                .masterCheckOut(LocalDate.of(2026, 3, 18))
                .guestNameKo("홍길동")
                .guestFirstNameEn("Gildong")
                .guestLastNameEn("Hong")
                .phoneCountryCode("+82")
                .phoneNumber("01012345678")
                .email("hong@test.com")
                .rateCodeId(1L)
                .marketCodeId(1L)
                .reservationChannelId(1L)
                .isOtaManaged(false)
                .subReservations(new ArrayList<>())
                .build();
    }

    public static MasterReservation createOtaMasterReservation(Property property) {
        MasterReservation master = createMasterReservation(property);
        return MasterReservation.builder()
                .property(property)
                .masterReservationNo("GMP260310-0002")
                .confirmationNo("OTA12345")
                .reservationStatus("RESERVED")
                .masterCheckIn(LocalDate.of(2026, 3, 15))
                .masterCheckOut(LocalDate.of(2026, 3, 18))
                .guestNameKo("김철수")
                .phoneNumber("01098765432")
                .email("kim@test.com")
                .rateCodeId(1L)
                .isOtaManaged(true)
                .otaReservationNo("BOOKING-12345")
                .subReservations(new ArrayList<>())
                .build();
    }

    public static SubReservation createSubReservation(MasterReservation master) {
        return SubReservation.builder()
                .masterReservation(master)
                .subReservationNo(master.getMasterReservationNo() + "-01")
                .roomReservationStatus("RESERVED")
                .roomTypeId(1L)
                .adults(2)
                .children(0)
                .checkIn(master.getMasterCheckIn())
                .checkOut(master.getMasterCheckOut())
                .earlyCheckIn(false)
                .lateCheckOut(false)
                .earlyCheckInFee(BigDecimal.ZERO)
                .lateCheckOutFee(BigDecimal.ZERO)
                .guests(new ArrayList<>())
                .dailyCharges(new ArrayList<>())
                .services(new ArrayList<>())
                .build();
    }

    public static DailyCharge createDailyCharge(SubReservation sub, LocalDate date,
                                                  BigDecimal supplyPrice, BigDecimal tax,
                                                  BigDecimal serviceCharge) {
        return DailyCharge.builder()
                .subReservation(sub)
                .chargeDate(date)
                .supplyPrice(supplyPrice)
                .tax(tax)
                .serviceCharge(serviceCharge)
                .total(supplyPrice.add(tax).add(serviceCharge))
                .build();
    }

    public static DailyCharge createDailyCharge(SubReservation sub, LocalDate date, BigDecimal supplyPrice) {
        return createDailyCharge(sub, date, supplyPrice, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    // ========== Payment ==========

    public static ReservationPayment createPayment(MasterReservation master) {
        return ReservationPayment.builder()
                .masterReservation(master)
                .paymentStatus("UNPAID")
                .totalRoomAmount(new BigDecimal("300000"))
                .totalServiceAmount(BigDecimal.ZERO)
                .totalServiceChargeAmount(new BigDecimal("15000"))
                .totalAdjustmentAmount(BigDecimal.ZERO)
                .totalEarlyLateFee(BigDecimal.ZERO)
                .grandTotal(new BigDecimal("345000"))
                .totalPaidAmount(BigDecimal.ZERO)
                .cancelFeeAmount(BigDecimal.ZERO)
                .refundAmount(BigDecimal.ZERO)
                .build();
    }

    public static PaymentTransaction createTransaction(Long masterReservationId, int seq,
                                                         String type, BigDecimal amount) {
        return PaymentTransaction.builder()
                .masterReservationId(masterReservationId)
                .transactionSeq(seq)
                .transactionType(type)
                .paymentMethod("CARD")
                .amount(amount)
                .currency("KRW")
                .transactionStatus("COMPLETED")
                .build();
    }

    public static PaymentAdjustment createAdjustment(Long masterReservationId, int seq,
                                                       String sign, BigDecimal totalAmount) {
        return PaymentAdjustment.builder()
                .masterReservationId(masterReservationId)
                .adjustmentSeq(seq)
                .adjustmentSign(sign)
                .supplyPrice(totalAmount)
                .tax(BigDecimal.ZERO)
                .totalAmount(totalAmount)
                .currency("KRW")
                .comment("테스트 조정")
                .build();
    }

    // ========== CancellationFee ==========

    public static CancellationFee createDateCancellationFee(Property property, int daysBefore,
                                                              String feeType, BigDecimal feeAmount) {
        return CancellationFee.builder()
                .property(property)
                .checkinBasis("DATE")
                .daysBefore(daysBefore)
                .feeType(feeType)
                .feeAmount(feeAmount)
                .build();
    }

    public static CancellationFee createNoShowCancellationFee(Property property,
                                                                String feeType, BigDecimal feeAmount) {
        return CancellationFee.builder()
                .property(property)
                .checkinBasis("NOSHOW")
                .daysBefore(null)
                .feeType(feeType)
                .feeAmount(feeAmount)
                .build();
    }

    // ========== EarlyLateFeePolicy ==========

    public static EarlyLateFeePolicy createEarlyCheckInPolicy(Property property,
                                                                 String timeFrom, String timeTo,
                                                                 String feeType, BigDecimal feeValue) {
        return EarlyLateFeePolicy.builder()
                .property(property)
                .policyType("EARLY_CHECKIN")
                .timeFrom(timeFrom)
                .timeTo(timeTo)
                .feeType(feeType)
                .feeValue(feeValue)
                .build();
    }

    public static EarlyLateFeePolicy createLateCheckOutPolicy(Property property,
                                                                 String timeFrom, String timeTo,
                                                                 String feeType, BigDecimal feeValue) {
        return EarlyLateFeePolicy.builder()
                .property(property)
                .policyType("LATE_CHECKOUT")
                .timeFrom(timeFrom)
                .timeTo(timeTo)
                .feeType(feeType)
                .feeValue(feeValue)
                .build();
    }

    // ========== ReservationServiceItem ==========

    public static ReservationServiceItem createServiceItem(SubReservation sub, BigDecimal unitPrice, int qty) {
        return ReservationServiceItem.builder()
                .subReservation(sub)
                .serviceType("PAID")
                .serviceOptionId(1L)
                .serviceDate(sub.getCheckIn())
                .quantity(qty)
                .unitPrice(unitPrice)
                .tax(BigDecimal.ZERO)
                .totalPrice(unitPrice.multiply(BigDecimal.valueOf(qty)))
                .build();
    }
}
