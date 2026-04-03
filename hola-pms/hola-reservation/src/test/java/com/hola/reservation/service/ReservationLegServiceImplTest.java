package com.hola.reservation.service;

import com.hola.hotel.entity.Hotel;
import com.hola.hotel.entity.Property;
import com.hola.hotel.repository.RoomUnavailableRepository;
import com.hola.reservation.entity.MasterReservation;
import com.hola.reservation.entity.SubReservation;
import com.hola.reservation.mapper.ReservationMapper;
import com.hola.reservation.repository.SubReservationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ReservationLegServiceImpl 단위 테스트 — 얼리/레이트 요금 등록·해제
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationLegServiceImpl")
class ReservationLegServiceImplTest {

    @Mock private ReservationFinder finder;
    @Mock private SubReservationCreator subCreator;
    @Mock private SubReservationRepository subReservationRepository;
    @Mock private RoomUnavailableRepository roomUnavailableRepository;
    @Mock private ReservationMapper reservationMapper;
    @Mock private ReservationPaymentService paymentService;
    @Mock private RoomAvailabilityService availabilityService;
    @Mock private RateIncludedServiceHelper rateIncludedServiceHelper;
    @Mock private ReservationChangeLogService changeLogService;
    @Mock private EarlyLateCheckService earlyLateCheckService;

    @InjectMocks
    private ReservationLegServiceImpl legService;

    private static final Long PROPERTY_ID = 1L;
    private static final Long MASTER_ID = 100L;
    private static final Long LEG_ID = 200L;

    private Property createProperty() {
        Hotel hotel = Hotel.builder().hotelCode("HTL00001").hotelName("테스트").build();
        Property property = Property.builder()
                .hotel(hotel).propertyCode("GMP").propertyName("테스트")
                .checkInTime("15:00").checkOutTime("11:00")
                .build();
        setId(property, PROPERTY_ID);
        return property;
    }

    private MasterReservation createMaster(Property property) {
        MasterReservation master = MasterReservation.builder()
                .property(property)
                .masterReservationNo("GMP260601-0001")
                .confirmationNo("HK4F29XP")
                .reservationStatus("RESERVED")
                .masterCheckIn(LocalDate.of(2026, 6, 1))
                .masterCheckOut(LocalDate.of(2026, 6, 3))
                .guestNameKo("홍길동")
                .rateCodeId(10L)
                .isOtaManaged(false)
                .subReservations(new ArrayList<>())
                .build();
        setId(master, MASTER_ID);
        return master;
    }

    private SubReservation createSub(MasterReservation master) {
        SubReservation sub = SubReservation.builder()
                .masterReservation(master)
                .subReservationNo("GMP260601-0001-01")
                .roomReservationStatus("RESERVED")
                .roomTypeId(1L)
                .checkIn(LocalDate.of(2026, 6, 1))
                .checkOut(LocalDate.of(2026, 6, 3))
                .earlyCheckIn(false)
                .lateCheckOut(false)
                .earlyCheckInFee(BigDecimal.ZERO)
                .lateCheckOutFee(BigDecimal.ZERO)
                .guests(new ArrayList<>())
                .dailyCharges(new ArrayList<>())
                .services(new ArrayList<>())
                .build();
        setId(sub, LEG_ID);
        master.getSubReservations().add(sub);
        return sub;
    }

    private void setId(Object entity, Long id) {
        try {
            var field = findField(entity.getClass(), "id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException("ID 설정 실패", e);
        }
    }

    private java.lang.reflect.Field findField(Class<?> clazz, String name) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new RuntimeException("필드를 찾을 수 없음: " + name);
    }

    // ══════════════════════════════════════════════
    // 얼리/레이트 요금 등록
    // ══════════════════════════════════════════════
    @Nested
    @DisplayName("얼리/레이트 요금 등록 (registerEarlyLateFee)")
    class RegisterEarlyLateFee {

        @Test
        @DisplayName("얼리 체크인 요금 등록 → earlyCheckIn=true, 요금 세팅, 결제 재계산")
        void register_earlyCheckIn() {
            // given
            Property property = createProperty();
            MasterReservation master = createMaster(property);
            SubReservation sub = createSub(master);

            when(finder.findMasterById(MASTER_ID, PROPERTY_ID)).thenReturn(master);
            when(finder.findSubAndValidateOwnership(LEG_ID, master)).thenReturn(sub);
            when(earlyLateCheckService.calculateFeeByPolicyIndex(sub, "EARLY_CHECKIN", 1))
                    .thenReturn(new BigDecimal("50000"));

            // when
            BigDecimal fee = legService.registerEarlyLateFee(MASTER_ID, PROPERTY_ID, LEG_ID, "EARLY_CHECKIN", 1);

            // then
            assertThat(fee).isEqualByComparingTo("50000");
            assertThat(sub.getEarlyCheckIn()).isTrue();
            assertThat(sub.getEarlyCheckInFee()).isEqualByComparingTo("50000");
            verify(subReservationRepository).flush();
            verify(paymentService).recalculatePayment(MASTER_ID);
        }

        @Test
        @DisplayName("레이트 체크아웃 요금 등록 → lateCheckOut=true, 요금 세팅, 결제 재계산")
        void register_lateCheckOut() {
            // given
            Property property = createProperty();
            MasterReservation master = createMaster(property);
            SubReservation sub = createSub(master);

            when(finder.findMasterById(MASTER_ID, PROPERTY_ID)).thenReturn(master);
            when(finder.findSubAndValidateOwnership(LEG_ID, master)).thenReturn(sub);
            when(earlyLateCheckService.calculateFeeByPolicyIndex(sub, "LATE_CHECKOUT", 0))
                    .thenReturn(new BigDecimal("30000"));

            // when
            BigDecimal fee = legService.registerEarlyLateFee(MASTER_ID, PROPERTY_ID, LEG_ID, "LATE_CHECKOUT", 0);

            // then
            assertThat(fee).isEqualByComparingTo("30000");
            assertThat(sub.getLateCheckOut()).isTrue();
            assertThat(sub.getLateCheckOutFee()).isEqualByComparingTo("30000");
            verify(subReservationRepository).flush();
            verify(paymentService).recalculatePayment(MASTER_ID);
        }
    }

    // ══════════════════════════════════════════════
    // 얼리/레이트 요금 해제
    // ══════════════════════════════════════════════
    @Nested
    @DisplayName("얼리/레이트 요금 해제 (removeEarlyLateFee)")
    class RemoveEarlyLateFee {

        @Test
        @DisplayName("얼리 체크인 요금 해제 → earlyCheckIn=false, 요금 ZERO, 결제 재계산")
        void remove_earlyCheckIn() {
            // given
            Property property = createProperty();
            MasterReservation master = createMaster(property);
            SubReservation sub = createSub(master);
            sub.registerEarlyCheckInFee(new BigDecimal("50000")); // 사전 등록

            when(finder.findMasterById(MASTER_ID, PROPERTY_ID)).thenReturn(master);
            when(finder.findSubAndValidateOwnership(LEG_ID, master)).thenReturn(sub);

            // when
            legService.removeEarlyLateFee(MASTER_ID, PROPERTY_ID, LEG_ID, "EARLY_CHECKIN");

            // then
            assertThat(sub.getEarlyCheckIn()).isFalse();
            assertThat(sub.getEarlyCheckInFee()).isEqualByComparingTo(BigDecimal.ZERO);
            verify(subReservationRepository).flush();
            verify(paymentService).recalculatePayment(MASTER_ID);
        }

        @Test
        @DisplayName("레이트 체크아웃 요금 해제 → lateCheckOut=false, 요금 ZERO, 결제 재계산")
        void remove_lateCheckOut() {
            // given
            Property property = createProperty();
            MasterReservation master = createMaster(property);
            SubReservation sub = createSub(master);
            sub.registerLateCheckOutFee(new BigDecimal("30000")); // 사전 등록

            when(finder.findMasterById(MASTER_ID, PROPERTY_ID)).thenReturn(master);
            when(finder.findSubAndValidateOwnership(LEG_ID, master)).thenReturn(sub);

            // when
            legService.removeEarlyLateFee(MASTER_ID, PROPERTY_ID, LEG_ID, "LATE_CHECKOUT");

            // then
            assertThat(sub.getLateCheckOut()).isFalse();
            assertThat(sub.getLateCheckOutFee()).isEqualByComparingTo(BigDecimal.ZERO);
            verify(subReservationRepository).flush();
            verify(paymentService).recalculatePayment(MASTER_ID);
        }
    }
}
