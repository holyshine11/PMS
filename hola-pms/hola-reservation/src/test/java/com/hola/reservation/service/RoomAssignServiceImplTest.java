package com.hola.reservation.service;

import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.hotel.entity.Floor;
import com.hola.hotel.entity.Hotel;
import com.hola.hotel.entity.Property;
import com.hola.hotel.entity.RoomNumber;
import com.hola.hotel.repository.FloorRepository;
import com.hola.hotel.repository.PropertyRepository;
import com.hola.hotel.repository.RoomNumberRepository;
import com.hola.hotel.repository.RoomUnavailableRepository;
import com.hola.rate.entity.RateCode;
import com.hola.rate.entity.RateCodeRoomType;
import com.hola.rate.repository.RateCodeRepository;
import com.hola.rate.repository.RateCodeRoomTypeRepository;
import com.hola.reservation.booking.dto.response.RoomAssignAvailabilityResponse;
import com.hola.reservation.dto.response.RoomNumberAvailabilityResponse;
import com.hola.reservation.entity.DailyCharge;
import com.hola.reservation.entity.MasterReservation;
import com.hola.reservation.entity.SubReservation;
import com.hola.reservation.repository.SubReservationRepository;
import com.hola.room.entity.RoomClass;
import com.hola.room.entity.RoomType;
import com.hola.room.entity.RoomTypeFloor;
import com.hola.room.repository.RoomClassRepository;
import com.hola.room.repository.RoomTypeFloorRepository;
import com.hola.room.repository.RoomTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RoomAssignServiceImpl 단위 테스트
 *
 * 테스트 범위:
 * - getAvailability: 정상, 빈 객실타입, 충돌 감지, HK 상태 비가용, 추천 정렬
 * - getFloorRoomAvailability: 정상, 빈 호수, 충돌, HK 상태
 * - findRateCodeForRoomType: 매핑 우선순위
 * - 프로퍼티/레이트코드 미존재 예외
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RoomAssignServiceImpl")
class RoomAssignServiceImplTest {

    @Mock private PropertyRepository propertyRepository;
    @Mock private RateCodeRepository rateCodeRepository;
    @Mock private RateCodeRoomTypeRepository rateCodeRoomTypeRepository;
    @Mock private RoomTypeRepository roomTypeRepository;
    @Mock private RoomClassRepository roomClassRepository;
    @Mock private RoomTypeFloorRepository roomTypeFloorRepository;
    @Mock private FloorRepository floorRepository;
    @Mock private RoomNumberRepository roomNumberRepository;
    @Mock private SubReservationRepository subReservationRepository;
    @Mock private PriceCalculationService priceCalculationService;
    @Mock private RoomUnavailableRepository roomUnavailableRepository;
    @Mock private RoomAvailabilityService roomAvailabilityService;

    @InjectMocks
    private RoomAssignServiceImpl roomAssignService;

    // 공통 테스트 상수
    private static final Long PROPERTY_ID = 1L;
    private static final Long ROOM_TYPE_ID = 10L;
    private static final Long RATE_CODE_ID = 20L;
    private static final Long FLOOR_ID = 30L;
    private static final Long ROOM_NUMBER_ID_1 = 40L;
    private static final Long ROOM_NUMBER_ID_2 = 41L;
    private static final Long ROOM_CLASS_ID = 50L;
    private static final LocalDate CHECK_IN = LocalDate.of(2026, 6, 1);
    private static final LocalDate CHECK_OUT = LocalDate.of(2026, 6, 3);

    private Property property;
    private RateCode rateCode;
    private RoomType roomType;
    private RoomClass roomClass;
    private Floor floor;
    private RoomNumber roomNumber1;
    private RoomNumber roomNumber2;

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

        rateCode = RateCode.builder()
                .currency("KRW")
                .saleStartDate(LocalDate.of(2026, 1, 1))
                .saleEndDate(LocalDate.of(2026, 12, 31))
                .minStayDays(1)
                .maxStayDays(30)
                .build();
        setId(rateCode, RATE_CODE_ID);

        roomType = RoomType.builder()
                .propertyId(PROPERTY_ID)
                .roomClassId(ROOM_CLASS_ID)
                .roomTypeCode("STD-D")
                .description("스탠다드 더블")
                .maxAdults(2)
                .maxChildren(1)
                .roomSize(new BigDecimal("28.5"))
                .build();
        setId(roomType, ROOM_TYPE_ID);

        roomClass = RoomClass.builder()
                .propertyId(PROPERTY_ID)
                .roomClassCode("STD")
                .roomClassName("Standard")
                .build();
        setId(roomClass, ROOM_CLASS_ID);

        floor = Floor.builder()
                .property(property)
                .floorNumber("5")
                .floorName("5층")
                .build();
        setId(floor, FLOOR_ID);

        roomNumber1 = RoomNumber.builder()
                .property(property)
                .roomNumber("501")
                .descriptionKo("501호")
                .hkStatus("CLEAN")
                .build();
        setId(roomNumber1, ROOM_NUMBER_ID_1);

        roomNumber2 = RoomNumber.builder()
                .property(property)
                .roomNumber("502")
                .descriptionKo("502호")
                .hkStatus("CLEAN")
                .build();
        setId(roomNumber2, ROOM_NUMBER_ID_2);
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

    private RoomTypeFloor createRoomTypeFloor(Long roomTypeId, Long floorId, Long roomNumberId) {
        return RoomTypeFloor.builder()
                .roomTypeId(roomTypeId)
                .floorId(floorId)
                .roomNumberId(roomNumberId)
                .build();
    }

    private SubReservation createConflictSub(Long roomNumberId, String guestName) {
        MasterReservation conflictMaster = MasterReservation.builder()
                .property(property)
                .masterReservationNo("GMP260601-9999")
                .confirmationNo("CONFLICT1")
                .reservationStatus("CHECK_IN")
                .masterCheckIn(CHECK_IN)
                .masterCheckOut(CHECK_OUT)
                .guestNameKo(guestName)
                .build();
        setId(conflictMaster, 999L);

        SubReservation conflictSub = SubReservation.builder()
                .masterReservation(conflictMaster)
                .subReservationNo("GMP260601-9999-01")
                .roomReservationStatus("CHECK_IN")
                .roomTypeId(ROOM_TYPE_ID)
                .roomNumberId(roomNumberId)
                .checkIn(CHECK_IN)
                .checkOut(CHECK_OUT)
                .adults(2)
                .children(0)
                .build();
        setId(conflictSub, 998L);
        return conflictSub;
    }

    /**
     * getAvailability에 필요한 공통 스터빙
     */
    private void stubCommonAvailability() {
        when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(property));
        when(rateCodeRepository.findById(RATE_CODE_ID)).thenReturn(Optional.of(rateCode));

        // 요금 계산은 비활성으로 처리 (커버리지 없음)
        when(priceCalculationService.hasPricingCoverage(any(), any(), any())).thenReturn(false);

        // 레이트코드-객실타입 매핑
        when(rateCodeRoomTypeRepository.findAllByRateCodeId(RATE_CODE_ID))
                .thenReturn(List.of(RateCodeRoomType.builder()
                        .rateCodeId(RATE_CODE_ID)
                        .roomTypeId(ROOM_TYPE_ID)
                        .build()));

        // 객실타입 목록
        when(roomTypeRepository.findAllByPropertyIdOrderBySortOrderAscRoomTypeCodeAsc(PROPERTY_ID))
                .thenReturn(List.of(roomType));

        // RoomClass 벌크 조회
        when(roomClassRepository.findAllById(anyList())).thenReturn(List.of(roomClass));

        // RoomTypeFloor
        List<RoomTypeFloor> rtfs = List.of(
                createRoomTypeFloor(ROOM_TYPE_ID, FLOOR_ID, ROOM_NUMBER_ID_1),
                createRoomTypeFloor(ROOM_TYPE_ID, FLOOR_ID, ROOM_NUMBER_ID_2)
        );
        when(roomTypeFloorRepository.findAllByRoomTypeIdIn(anyList())).thenReturn(rtfs);

        // 레이트코드-객실타입 매핑 (findRateCodeForRoomType 내부)
        when(rateCodeRoomTypeRepository.findAllByRoomTypeId(ROOM_TYPE_ID))
                .thenReturn(List.of(RateCodeRoomType.builder()
                        .rateCodeId(RATE_CODE_ID)
                        .roomTypeId(ROOM_TYPE_ID)
                        .build()));

        // Floor, RoomNumber 벌크 조회
        when(floorRepository.findAllById(anyCollection())).thenReturn(List.of(floor));
        when(roomNumberRepository.findAllById(anyCollection()))
                .thenReturn(List.of(roomNumber1, roomNumber2));

        // 충돌 없음
        when(subReservationRepository.findConflictsByRoomNumberIds(anyList(), any(), any(), anyList()))
                .thenReturn(Collections.emptyList());
    }

    // ══════════════════════════════════════════════
    // getAvailability 테스트
    // ══════════════════════════════════════════════
    @Nested
    @DisplayName("객실 배정 가용성 조회 (getAvailability)")
    class GetAvailabilityTests {

        @Test
        @DisplayName("정상 조회 - 가용 객실 2개, 추천 표시")
        void getAvailability_정상_가용객실() {
            // given
            stubCommonAvailability();

            // when
            RoomAssignAvailabilityResponse result = roomAssignService.getAvailability(
                    PROPERTY_ID, ROOM_TYPE_ID, RATE_CODE_ID,
                    CHECK_IN, CHECK_OUT, 2, 0, null);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getNights()).isEqualTo(2);
            assertThat(result.getCurrency()).isEqualTo("KRW");
            assertThat(result.getRoomTypeGroups()).hasSize(1);

            var group = result.getRoomTypeGroups().get(0);
            assertThat(group.getRoomTypeCode()).isEqualTo("STD-D");
            assertThat(group.isRecommended()).isTrue();
            assertThat(group.getRoomClassName()).isEqualTo("Standard");
            assertThat(group.getFloors()).hasSize(1);

            var floorGroup = group.getFloors().get(0);
            assertThat(floorGroup.getFloorName()).isEqualTo("5층");
            assertThat(floorGroup.getTotalRooms()).isEqualTo(2);
            assertThat(floorGroup.getAvailableRooms()).isEqualTo(2);
        }

        @Test
        @DisplayName("프로퍼티 미존재 시 예외")
        void getAvailability_프로퍼티미존재_예외() {
            // given
            when(propertyRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> roomAssignService.getAvailability(
                    999L, ROOM_TYPE_ID, RATE_CODE_ID,
                    CHECK_IN, CHECK_OUT, 2, 0, null))
                    .isInstanceOf(HolaException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.PROPERTY_NOT_FOUND);
        }

        @Test
        @DisplayName("레이트코드 미존재 시 예외")
        void getAvailability_레이트코드미존재_예외() {
            // given
            when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(property));
            when(rateCodeRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> roomAssignService.getAvailability(
                    PROPERTY_ID, ROOM_TYPE_ID, 999L,
                    CHECK_IN, CHECK_OUT, 2, 0, null))
                    .isInstanceOf(HolaException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.RATE_CODE_NOT_FOUND);
        }

        @Test
        @DisplayName("객실타입이 없는 경우 빈 그룹 반환")
        void getAvailability_빈객실타입_빈그룹() {
            // given
            when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(property));
            when(rateCodeRepository.findById(RATE_CODE_ID)).thenReturn(Optional.of(rateCode));
            when(priceCalculationService.hasPricingCoverage(any(), any(), any())).thenReturn(false);
            when(rateCodeRoomTypeRepository.findAllByRateCodeId(RATE_CODE_ID)).thenReturn(Collections.emptyList());
            when(roomTypeRepository.findAllByPropertyIdOrderBySortOrderAscRoomTypeCodeAsc(PROPERTY_ID))
                    .thenReturn(Collections.emptyList());

            // when
            RoomAssignAvailabilityResponse result = roomAssignService.getAvailability(
                    PROPERTY_ID, ROOM_TYPE_ID, RATE_CODE_ID,
                    CHECK_IN, CHECK_OUT, 2, 0, null);

            // then
            assertThat(result.getRoomTypeGroups()).isEmpty();
            assertThat(result.getCurrency()).isEqualTo("KRW");
        }

        @Test
        @DisplayName("충돌 예약이 있는 객실은 available=false")
        void getAvailability_충돌예약_비가용() {
            // given
            stubCommonAvailability();
            // 501호에 충돌 예약
            SubReservation conflict = createConflictSub(ROOM_NUMBER_ID_1, "김철수");
            when(subReservationRepository.findConflictsByRoomNumberIds(anyList(), any(), any(), anyList()))
                    .thenReturn(List.of(conflict));

            // when
            RoomAssignAvailabilityResponse result = roomAssignService.getAvailability(
                    PROPERTY_ID, ROOM_TYPE_ID, RATE_CODE_ID,
                    CHECK_IN, CHECK_OUT, 2, 0, null);

            // then
            var rooms = result.getRoomTypeGroups().get(0).getFloors().get(0).getRooms();
            // 가용 객실 먼저 정렬되므로
            assertThat(rooms).hasSize(2);
            // 502가 가용, 501이 비가용
            var availableRoom = rooms.stream().filter(r -> r.isAvailable()).findFirst().orElse(null);
            var unavailableRoom = rooms.stream().filter(r -> !r.isAvailable()).findFirst().orElse(null);
            assertThat(availableRoom).isNotNull();
            assertThat(availableRoom.getRoomNumber()).isEqualTo("502");
            assertThat(unavailableRoom).isNotNull();
            assertThat(unavailableRoom.getRoomNumber()).isEqualTo("501");
            assertThat(unavailableRoom.getConflictReservationNumber()).isEqualTo("GMP260601-9999");
        }

        @Test
        @DisplayName("HK 상태가 OOO/OOS인 객실은 available=false")
        void getAvailability_HK비가용_표시() {
            // given
            stubCommonAvailability();

            // 501호의 HK 상태를 OOO로 변경
            RoomNumber oooRoom = RoomNumber.builder()
                    .property(property)
                    .roomNumber("501")
                    .descriptionKo("501호")
                    .hkStatus("OOO")
                    .build();
            setId(oooRoom, ROOM_NUMBER_ID_1);

            when(roomNumberRepository.findAllById(anyCollection()))
                    .thenReturn(List.of(oooRoom, roomNumber2));

            // when
            RoomAssignAvailabilityResponse result = roomAssignService.getAvailability(
                    PROPERTY_ID, ROOM_TYPE_ID, RATE_CODE_ID,
                    CHECK_IN, CHECK_OUT, 2, 0, null);

            // then
            var rooms = result.getRoomTypeGroups().get(0).getFloors().get(0).getRooms();
            var oooItem = rooms.stream()
                    .filter(r -> "501".equals(r.getRoomNumber()))
                    .findFirst().orElse(null);
            assertThat(oooItem).isNotNull();
            assertThat(oooItem.isAvailable()).isFalse();
            assertThat(oooItem.getUnavailableType()).isEqualTo("OOO");

            var floorGroup = result.getRoomTypeGroups().get(0).getFloors().get(0);
            assertThat(floorGroup.getAvailableRooms()).isEqualTo(1); // 502만 가용
        }
    }

    // ══════════════════════════════════════════════
    // getFloorRoomAvailability 테스트
    // ══════════════════════════════════════════════
    @Nested
    @DisplayName("층별 호수 가용성 조회 (getFloorRoomAvailability)")
    class GetFloorRoomAvailabilityTests {

        @Test
        @DisplayName("정상 조회 - 가용 객실 2개")
        void getFloorRoomAvailability_정상() {
            // given
            when(roomTypeFloorRepository.findDistinctRoomNumberIdsByFloorId(FLOOR_ID))
                    .thenReturn(List.of(ROOM_NUMBER_ID_1, ROOM_NUMBER_ID_2));
            when(roomNumberRepository.findAllById(anyList()))
                    .thenReturn(List.of(roomNumber1, roomNumber2));
            when(subReservationRepository.findConflictsByRoomNumberIds(anyList(), any(), any(), anyList()))
                    .thenReturn(Collections.emptyList());

            // when
            List<RoomNumberAvailabilityResponse> result = roomAssignService.getFloorRoomAvailability(
                    FLOOR_ID, CHECK_IN, CHECK_OUT, null);

            // then
            assertThat(result).hasSize(2);
            assertThat(result).allMatch(RoomNumberAvailabilityResponse::isAvailable);
        }

        @Test
        @DisplayName("빈 호수 목록 시 빈 리스트 반환")
        void getFloorRoomAvailability_빈호수_빈리스트() {
            // given
            when(roomTypeFloorRepository.findDistinctRoomNumberIdsByFloorId(FLOOR_ID))
                    .thenReturn(Collections.emptyList());

            // when
            List<RoomNumberAvailabilityResponse> result = roomAssignService.getFloorRoomAvailability(
                    FLOOR_ID, CHECK_IN, CHECK_OUT, null);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("충돌 예약이 있는 객실 - 마스킹된 게스트명 표시")
        void getFloorRoomAvailability_충돌_게스트명마스킹() {
            // given
            when(roomTypeFloorRepository.findDistinctRoomNumberIdsByFloorId(FLOOR_ID))
                    .thenReturn(List.of(ROOM_NUMBER_ID_1));
            when(roomNumberRepository.findAllById(anyList()))
                    .thenReturn(List.of(roomNumber1));

            SubReservation conflict = createConflictSub(ROOM_NUMBER_ID_1, "홍길동");
            when(subReservationRepository.findConflictsByRoomNumberIds(anyList(), any(), any(), anyList()))
                    .thenReturn(List.of(conflict));

            // when
            List<RoomNumberAvailabilityResponse> result = roomAssignService.getFloorRoomAvailability(
                    FLOOR_ID, CHECK_IN, CHECK_OUT, null);

            // then
            assertThat(result).hasSize(1);
            RoomNumberAvailabilityResponse room = result.get(0);
            assertThat(room.isAvailable()).isFalse();
            assertThat(room.getConflictReservationNo()).isEqualTo("GMP260601-9999");
            // 마스킹된 이름 확인 (정확한 패턴은 NameMaskingUtil 의존)
            assertThat(room.getConflictGuestName()).isNotEqualTo("홍길동");
        }

        @Test
        @DisplayName("DIRTY 상태 객실 - HK 미완료 불가")
        void getFloorRoomAvailability_DIRTY_비가용() {
            // given
            RoomNumber dirtyRoom = RoomNumber.builder()
                    .property(property)
                    .roomNumber("501")
                    .descriptionKo("501호")
                    .hkStatus("DIRTY")
                    .build();
            setId(dirtyRoom, ROOM_NUMBER_ID_1);

            when(roomTypeFloorRepository.findDistinctRoomNumberIdsByFloorId(FLOOR_ID))
                    .thenReturn(List.of(ROOM_NUMBER_ID_1));
            when(roomNumberRepository.findAllById(anyList()))
                    .thenReturn(List.of(dirtyRoom));
            when(subReservationRepository.findConflictsByRoomNumberIds(anyList(), any(), any(), anyList()))
                    .thenReturn(Collections.emptyList());

            // when
            List<RoomNumberAvailabilityResponse> result = roomAssignService.getFloorRoomAvailability(
                    FLOOR_ID, CHECK_IN, CHECK_OUT, null);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).isAvailable()).isFalse();
            assertThat(result.get(0).getUnavailableType()).isEqualTo("DIRTY");
        }

        @Test
        @DisplayName("자기 자신 제외 (excludeSubId) 동작 확인")
        void getFloorRoomAvailability_자기자신제외() {
            // given
            Long mySubId = 500L;

            when(roomTypeFloorRepository.findDistinctRoomNumberIdsByFloorId(FLOOR_ID))
                    .thenReturn(List.of(ROOM_NUMBER_ID_1));
            when(roomNumberRepository.findAllById(anyList()))
                    .thenReturn(List.of(roomNumber1));

            // 자기 자신 예약만 충돌로 반환
            SubReservation mySub = createConflictSub(ROOM_NUMBER_ID_1, "내 예약");
            setId(mySub, mySubId);
            when(subReservationRepository.findConflictsByRoomNumberIds(anyList(), any(), any(), anyList()))
                    .thenReturn(List.of(mySub));

            // excludeSubId와 동일한 SubReservation 조회 (Dayuse 체크용)
            when(subReservationRepository.findById(mySubId)).thenReturn(Optional.of(mySub));

            // when
            List<RoomNumberAvailabilityResponse> result = roomAssignService.getFloorRoomAvailability(
                    FLOOR_ID, CHECK_IN, CHECK_OUT, mySubId);

            // then - 자기 자신은 제외되어 가용으로 처리
            assertThat(result).hasSize(1);
            assertThat(result.get(0).isAvailable()).isTrue();
        }
    }
}
