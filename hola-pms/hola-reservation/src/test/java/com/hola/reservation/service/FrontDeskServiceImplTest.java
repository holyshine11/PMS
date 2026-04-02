package com.hola.reservation.service;

import com.hola.hotel.entity.ReservationChannel;
import com.hola.hotel.entity.RoomNumber;
import com.hola.hotel.repository.ReservationChannelRepository;
import com.hola.hotel.repository.RoomNumberRepository;
import com.hola.reservation.dto.response.FrontDeskOperationResponse;
import com.hola.reservation.entity.MasterReservation;
import com.hola.reservation.entity.ReservationPayment;
import com.hola.reservation.entity.SubReservation;
import com.hola.reservation.repository.ReservationPaymentRepository;
import com.hola.reservation.repository.SubReservationRepository;
import com.hola.hotel.entity.Property;
import com.hola.hotel.entity.Hotel;
import com.hola.room.entity.RoomType;
import com.hola.room.repository.RoomTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * FrontDeskServiceImpl 단위 테스트
 *
 * 테스트 범위:
 * - getArrivals: 도착 예정 리스트 + DTO 변환
 * - getInHouse: 투숙중 리스트
 * - getDepartures: 출발 예정 리스트
 * - getAllOperations: 전체 운영현황
 * - getSummary: 카운트 요약
 * - toResponseList: 빈 리스트, 벌크 조회, null 안전
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FrontDeskServiceImpl")
class FrontDeskServiceImplTest {

    @Mock private SubReservationRepository subReservationRepository;
    @Mock private ReservationPaymentRepository paymentRepository;
    @Mock private RoomTypeRepository roomTypeRepository;
    @Mock private RoomNumberRepository roomNumberRepository;
    @Mock private ReservationChannelRepository reservationChannelRepository;

    @InjectMocks
    private FrontDeskServiceImpl frontDeskService;

    // 공통 테스트 상수
    private static final Long PROPERTY_ID = 1L;
    private static final Long MASTER_ID = 100L;
    private static final Long SUB_ID = 200L;
    private static final Long ROOM_TYPE_ID = 10L;
    private static final Long ROOM_NUMBER_ID = 20L;
    private static final Long CHANNEL_ID = 30L;

    private Property property;
    private MasterReservation master;
    private SubReservation sub;

    @BeforeEach
    void setUp() {
        Hotel hotel = Hotel.builder().hotelName("올라 서울 호텔").build();
        setId(hotel, 1L);

        property = Property.builder()
                .hotel(hotel)
                .propertyCode("GMP")
                .propertyName("올라 그랜드 명동")
                .build();
        setId(property, PROPERTY_ID);

        master = MasterReservation.builder()
                .property(property)
                .masterReservationNo("GMP260601-0001")
                .confirmationNo("HK4F29XP")
                .reservationStatus("RESERVED")
                .masterCheckIn(LocalDate.of(2026, 6, 1))
                .masterCheckOut(LocalDate.of(2026, 6, 3))
                .guestNameKo("홍길동")
                .guestLastNameEn("Hong")
                .phoneNumber("01012345678")
                .email("hong@test.com")
                .reservationChannelId(CHANNEL_ID)
                .isOtaManaged(false)
                .build();
        setId(master, MASTER_ID);

        sub = SubReservation.builder()
                .masterReservation(master)
                .subReservationNo("GMP260601-0001-01")
                .roomReservationStatus("RESERVED")
                .roomTypeId(ROOM_TYPE_ID)
                .roomNumberId(ROOM_NUMBER_ID)
                .adults(2)
                .children(0)
                .checkIn(LocalDate.of(2026, 6, 1))
                .checkOut(LocalDate.of(2026, 6, 3))
                .build();
        setId(sub, SUB_ID);
    }

    // ──────────────────────────────────────────────
    // 헬퍼 메서드
    // ──────────────────────────────────────────────

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

    /**
     * toResponseList 벌크 조회에 필요한 공통 스터빙
     */
    private void stubBulkLookups() {
        RoomType roomType = RoomType.builder()
                .roomTypeCode("STD-D")
                .description("스탠다드 더블")
                .build();
        setId(roomType, ROOM_TYPE_ID);

        RoomNumber roomNumber = RoomNumber.builder()
                .roomNumber("101")
                .build();
        setId(roomNumber, ROOM_NUMBER_ID);

        ReservationChannel channel = ReservationChannel.builder()
                .channelName("Direct")
                .build();
        setId(channel, CHANNEL_ID);

        when(roomTypeRepository.findAllById(anyCollection()))
                .thenReturn(List.of(roomType));
        when(roomNumberRepository.findAllById(anyCollection()))
                .thenReturn(List.of(roomNumber));
        when(paymentRepository.findAllByMasterReservationIdIn(anySet()))
                .thenReturn(Collections.emptyList());
        when(reservationChannelRepository.findAllById(anyCollection()))
                .thenReturn(List.of(channel));
    }

    // ══════════════════════════════════════════════
    // getArrivals 테스트
    // ══════════════════════════════════════════════
    @Nested
    @DisplayName("도착 예정 리스트 (getArrivals)")
    class GetArrivalsTests {

        @Test
        @DisplayName("정상 조회 - 서브예약 → DTO 변환 검증")
        void getArrivals_정상_DTO변환() {
            // given
            when(subReservationRepository.findArrivals(eq(PROPERTY_ID), any(LocalDate.class)))
                    .thenReturn(List.of(sub));
            stubBulkLookups();

            // when
            List<FrontDeskOperationResponse> result = frontDeskService.getArrivals(PROPERTY_ID);

            // then
            assertThat(result).hasSize(1);
            FrontDeskOperationResponse response = result.get(0);
            assertThat(response.getReservationId()).isEqualTo(MASTER_ID);
            assertThat(response.getSubReservationId()).isEqualTo(SUB_ID);
            assertThat(response.getMasterReservationNo()).isEqualTo("GMP260601-0001");
            assertThat(response.getGuestNameKo()).isEqualTo("홍길동");
            assertThat(response.getRoomTypeName()).isEqualTo("STD-D");
            assertThat(response.getRoomNumber()).isEqualTo("101");
            assertThat(response.getAdults()).isEqualTo(2);
            assertThat(response.getNights()).isEqualTo(2);
            assertThat(response.getReservationChannelName()).isEqualTo("Direct");
            assertThat(response.getIsOtaManaged()).isFalse();
        }

        @Test
        @DisplayName("빈 리스트 반환 시 빈 결과")
        void getArrivals_빈리스트() {
            // given
            when(subReservationRepository.findArrivals(eq(PROPERTY_ID), any(LocalDate.class)))
                    .thenReturn(Collections.emptyList());

            // when
            List<FrontDeskOperationResponse> result = frontDeskService.getArrivals(PROPERTY_ID);

            // then
            assertThat(result).isEmpty();
            // 벌크 조회 호출되지 않아야 함
            verifyNoInteractions(roomTypeRepository, roomNumberRepository, paymentRepository);
        }
    }

    // ══════════════════════════════════════════════
    // getInHouse 테스트
    // ══════════════════════════════════════════════
    @Nested
    @DisplayName("투숙중 리스트 (getInHouse)")
    class GetInHouseTests {

        @Test
        @DisplayName("정상 조회")
        void getInHouse_정상() {
            // given
            when(subReservationRepository.findInHouse(eq(PROPERTY_ID), any(LocalDate.class)))
                    .thenReturn(List.of(sub));
            stubBulkLookups();

            // when
            List<FrontDeskOperationResponse> result = frontDeskService.getInHouse(PROPERTY_ID);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getSubReservationNo()).isEqualTo("GMP260601-0001-01");
        }
    }

    // ══════════════════════════════════════════════
    // getDepartures 테스트
    // ══════════════════════════════════════════════
    @Nested
    @DisplayName("출발 예정 리스트 (getDepartures)")
    class GetDeparturesTests {

        @Test
        @DisplayName("정상 조회")
        void getDepartures_정상() {
            // given
            when(subReservationRepository.findDepartures(eq(PROPERTY_ID), any(LocalDate.class)))
                    .thenReturn(List.of(sub));
            stubBulkLookups();

            // when
            List<FrontDeskOperationResponse> result = frontDeskService.getDepartures(PROPERTY_ID);

            // then
            assertThat(result).hasSize(1);
        }
    }

    // ══════════════════════════════════════════════
    // getAllOperations 테스트
    // ══════════════════════════════════════════════
    @Nested
    @DisplayName("전체 운영현황 (getAllOperations)")
    class GetAllOperationsTests {

        @Test
        @DisplayName("정상 조회")
        void getAllOperations_정상() {
            // given
            when(subReservationRepository.findAllOperations(eq(PROPERTY_ID), any(LocalDate.class)))
                    .thenReturn(List.of(sub));
            stubBulkLookups();

            // when
            List<FrontDeskOperationResponse> result = frontDeskService.getAllOperations(PROPERTY_ID);

            // then
            assertThat(result).hasSize(1);
        }
    }

    // ══════════════════════════════════════════════
    // getSummary 테스트
    // ══════════════════════════════════════════════
    @Nested
    @DisplayName("운영 요약 (getSummary)")
    class GetSummaryTests {

        @Test
        @DisplayName("정상 카운트 요약 반환")
        void getSummary_정상() {
            // given
            LocalDate today = LocalDate.now();

            when(subReservationRepository.countArrivals(PROPERTY_ID, today)).thenReturn(10L);
            when(subReservationRepository.countInHouse(PROPERTY_ID, today)).thenReturn(50L);
            when(subReservationRepository.countDepartures(PROPERTY_ID, today)).thenReturn(15L);
            when(subReservationRepository.countCheckedInToday(PROPERTY_ID, today)).thenReturn(8L);
            when(subReservationRepository.countCheckedOutToday(PROPERTY_ID, today)).thenReturn(12L);

            // when
            Map<String, Long> result = frontDeskService.getSummary(PROPERTY_ID);

            // then
            assertThat(result).containsEntry("arrivals", 10L);
            assertThat(result).containsEntry("inHouse", 50L);
            assertThat(result).containsEntry("departures", 15L);
            assertThat(result).containsEntry("checkedInToday", 8L);
            assertThat(result).containsEntry("checkedOutToday", 12L);
        }
    }

    // ══════════════════════════════════════════════
    // toResponseList 엣지 케이스 테스트
    // ══════════════════════════════════════════════
    @Nested
    @DisplayName("DTO 변환 엣지 케이스 (toResponseList)")
    class ToResponseListEdgeCases {

        @Test
        @DisplayName("객실타입/번호/채널이 null인 경우 안전하게 null 반환")
        void toResponseList_null참조_안전처리() {
            // given - roomTypeId, roomNumberId가 null인 서브예약
            SubReservation nullRoomSub = SubReservation.builder()
                    .masterReservation(master)
                    .subReservationNo("GMP260601-0001-02")
                    .roomReservationStatus("RESERVED")
                    .roomTypeId(null)
                    .roomNumberId(null)
                    .adults(1)
                    .children(0)
                    .checkIn(LocalDate.of(2026, 6, 1))
                    .checkOut(LocalDate.of(2026, 6, 3))
                    .build();
            setId(nullRoomSub, 201L);

            when(subReservationRepository.findArrivals(eq(PROPERTY_ID), any(LocalDate.class)))
                    .thenReturn(List.of(nullRoomSub));
            // 벌크 조회에서 빈 결과
            when(paymentRepository.findAllByMasterReservationIdIn(anySet()))
                    .thenReturn(Collections.emptyList());
            when(reservationChannelRepository.findAllById(anyCollection()))
                    .thenReturn(Collections.emptyList());

            // when
            List<FrontDeskOperationResponse> result = frontDeskService.getArrivals(PROPERTY_ID);

            // then
            assertThat(result).hasSize(1);
            FrontDeskOperationResponse response = result.get(0);
            assertThat(response.getRoomTypeName()).isNull();
            assertThat(response.getRoomNumber()).isNull();
            assertThat(response.getPaymentStatus()).isNull();
        }

        @Test
        @DisplayName("결제 정보가 있는 경우 paymentStatus 반환")
        void toResponseList_결제정보포함() {
            // given
            when(subReservationRepository.findArrivals(eq(PROPERTY_ID), any(LocalDate.class)))
                    .thenReturn(List.of(sub));

            RoomType roomType = RoomType.builder().roomTypeCode("STD-D").build();
            setId(roomType, ROOM_TYPE_ID);
            RoomNumber roomNumber = RoomNumber.builder().roomNumber("101").build();
            setId(roomNumber, ROOM_NUMBER_ID);
            ReservationChannel channel = ReservationChannel.builder().channelName("Direct").build();
            setId(channel, CHANNEL_ID);

            ReservationPayment payment = ReservationPayment.builder()
                    .masterReservation(master)
                    .paymentStatus("PAID")
                    .build();

            when(roomTypeRepository.findAllById(anyCollection())).thenReturn(List.of(roomType));
            when(roomNumberRepository.findAllById(anyCollection())).thenReturn(List.of(roomNumber));
            when(paymentRepository.findAllByMasterReservationIdIn(anySet()))
                    .thenReturn(List.of(payment));
            when(reservationChannelRepository.findAllById(anyCollection()))
                    .thenReturn(List.of(channel));

            // when
            List<FrontDeskOperationResponse> result = frontDeskService.getArrivals(PROPERTY_ID);

            // then
            assertThat(result.get(0).getPaymentStatus()).isEqualTo("PAID");
        }
    }
}
