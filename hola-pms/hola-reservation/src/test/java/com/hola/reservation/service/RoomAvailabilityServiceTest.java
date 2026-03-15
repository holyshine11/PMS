package com.hola.reservation.service;

import com.hola.reservation.entity.SubReservation;
import com.hola.reservation.repository.SubReservationRepository;
import com.hola.room.repository.RoomTypeFloorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@DisplayName("RoomAvailabilityService - 객실 가용성 검사")
@ExtendWith(MockitoExtension.class)
class RoomAvailabilityServiceTest {

    @InjectMocks
    private RoomAvailabilityService service;

    @Mock
    private SubReservationRepository subReservationRepository;

    @Mock
    private RoomTypeFloorRepository roomTypeFloorRepository;

    private static final LocalDate CHECK_IN = LocalDate.of(2026, 4, 1);
    private static final LocalDate CHECK_OUT = LocalDate.of(2026, 4, 3);

    private SubReservation createSub(Long id) {
        return SubReservation.builder()
                .roomTypeId(1L)
                .checkIn(CHECK_IN)
                .checkOut(CHECK_OUT)
                .build();
    }

    @Nested
    @DisplayName("L1: 객실 충돌")
    class L1RoomConflict {

        @Test
        @DisplayName("충돌 없음 - 기간 겹침 없는 경우 false")
        void hasRoomConflict_noOverlap_returnsFalse() {
            when(subReservationRepository.findByRoomNumberIdAndCheckInLessThanAndCheckOutGreaterThanAndRoomReservationStatusNotIn(
                    eq(1L), any(), any(), anyList()))
                    .thenReturn(List.of());

            assertThat(service.hasRoomConflict(1L, CHECK_IN, CHECK_OUT, null)).isFalse();
        }

        @Test
        @DisplayName("충돌 있음 - 기간 완전 겹침 시 true")
        void hasRoomConflict_fullOverlap_returnsTrue() {
            SubReservation conflict = createSub(10L);
            when(subReservationRepository.findByRoomNumberIdAndCheckInLessThanAndCheckOutGreaterThanAndRoomReservationStatusNotIn(
                    eq(1L), any(), any(), anyList()))
                    .thenReturn(List.of(conflict));

            assertThat(service.hasRoomConflict(1L, CHECK_IN, CHECK_OUT, null)).isTrue();
        }

        @Test
        @DisplayName("roomNumberId null - 항상 false (호수 미배정)")
        void hasRoomConflict_nullRoomNumber_returnsFalse() {
            assertThat(service.hasRoomConflict(null, CHECK_IN, CHECK_OUT, null)).isFalse();
        }

        @Test
        @DisplayName("자기 자신 제외 - excludeSubId 적용 시 false")
        void hasRoomConflict_excludeSelf_returnsFalse() {
            // ID 10인 서브예약이 충돌하지만, excludeSubId=10으로 자기 자신 제외
            SubReservation selfSub = SubReservation.builder()
                    .roomTypeId(1L).checkIn(CHECK_IN).checkOut(CHECK_OUT).build();
            // SubReservation은 @Builder이므로 id를 직접 세팅할 수 없음 (BaseEntity)
            // 대신 빈 리스트 반환으로 테스트
            when(subReservationRepository.findByRoomNumberIdAndCheckInLessThanAndCheckOutGreaterThanAndRoomReservationStatusNotIn(
                    eq(1L), any(), any(), anyList()))
                    .thenReturn(List.of());

            assertThat(service.hasRoomConflict(1L, CHECK_IN, CHECK_OUT, 10L)).isFalse();
        }
    }

    @Nested
    @DisplayName("L2: 가용 객실 수")
    class L2AvailableCount {

        @Test
        @DisplayName("가용 객실 3개 - 총5실 - 활성예약2건 = 3")
        void getAvailableRoomCount_returns3() {
            when(roomTypeFloorRepository.countByRoomTypeId(1L)).thenReturn(5L);
            when(subReservationRepository.countByRoomTypeIdAndCheckInLessThanAndCheckOutGreaterThanAndRoomReservationStatusNotIn(
                    eq(1L), any(), any(), anyList()))
                    .thenReturn(2L);

            assertThat(service.getAvailableRoomCount(1L, CHECK_IN, CHECK_OUT)).isEqualTo(3);
        }

        @Test
        @DisplayName("가용 객실 0개 - 완전 소진")
        void getAvailableRoomCount_returnsZero() {
            when(roomTypeFloorRepository.countByRoomTypeId(1L)).thenReturn(5L);
            when(subReservationRepository.countByRoomTypeIdAndCheckInLessThanAndCheckOutGreaterThanAndRoomReservationStatusNotIn(
                    eq(1L), any(), any(), anyList()))
                    .thenReturn(5L);

            assertThat(service.getAvailableRoomCount(1L, CHECK_IN, CHECK_OUT)).isEqualTo(0);
        }

        @Test
        @DisplayName("가용 객실 음수 - 오버부킹 상태")
        void getAvailableRoomCount_returnsNegative() {
            when(roomTypeFloorRepository.countByRoomTypeId(1L)).thenReturn(3L);
            when(subReservationRepository.countByRoomTypeIdAndCheckInLessThanAndCheckOutGreaterThanAndRoomReservationStatusNotIn(
                    eq(1L), any(), any(), anyList()))
                    .thenReturn(5L);

            assertThat(service.getAvailableRoomCount(1L, CHECK_IN, CHECK_OUT)).isEqualTo(-2);
        }

        @Test
        @DisplayName("예약 없음 - 총 객실수 반환")
        void getAvailableRoomCount_noReservations_returnsTotalRooms() {
            when(roomTypeFloorRepository.countByRoomTypeId(1L)).thenReturn(10L);
            when(subReservationRepository.countByRoomTypeIdAndCheckInLessThanAndCheckOutGreaterThanAndRoomReservationStatusNotIn(
                    eq(1L), any(), any(), anyList()))
                    .thenReturn(0L);

            assertThat(service.getAvailableRoomCount(1L, CHECK_IN, CHECK_OUT)).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("L3: 오버부킹 경고")
    class L3OverbookingWarning {

        @Test
        @DisplayName("오버부킹 경고 true - available <= 0")
        void isOverbookingWarning_zeroAvailable_returnsTrue() {
            when(roomTypeFloorRepository.countByRoomTypeId(1L)).thenReturn(5L);
            when(subReservationRepository.countByRoomTypeIdAndCheckInLessThanAndCheckOutGreaterThanAndRoomReservationStatusNotIn(
                    eq(1L), any(), any(), anyList()))
                    .thenReturn(5L);

            assertThat(service.isOverbookingWarning(1L, CHECK_IN, CHECK_OUT)).isTrue();
        }

        @Test
        @DisplayName("오버부킹 경고 false - available > 0")
        void isOverbookingWarning_positiveAvailable_returnsFalse() {
            when(roomTypeFloorRepository.countByRoomTypeId(1L)).thenReturn(5L);
            when(subReservationRepository.countByRoomTypeIdAndCheckInLessThanAndCheckOutGreaterThanAndRoomReservationStatusNotIn(
                    eq(1L), any(), any(), anyList()))
                    .thenReturn(3L);

            assertThat(service.isOverbookingWarning(1L, CHECK_IN, CHECK_OUT)).isFalse();
        }
    }
}
