package com.hola.reservation.service;

import com.hola.common.auth.entity.AdminUser;
import com.hola.common.enums.StayType;
import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.common.security.AccessControlService;
import com.hola.hotel.entity.Property;
import com.hola.hotel.repository.FloorRepository;
import com.hola.hotel.repository.MarketCodeRepository;
import com.hola.hotel.repository.PropertyRepository;
import com.hola.hotel.repository.RoomNumberRepository;
import com.hola.rate.entity.RateCode;
import com.hola.rate.repository.RateCodeRepository;
import com.hola.reservation.booking.service.CancellationPolicyService;
import com.hola.reservation.booking.service.CancellationPolicyService.CancelFeeResult;
import com.hola.reservation.dto.request.*;
import com.hola.reservation.dto.response.*;
import com.hola.reservation.dto.response.RateChangePreviewResponse;
import com.hola.reservation.entity.*;
import com.hola.reservation.mapper.ReservationMapper;
import com.hola.reservation.repository.*;
import com.hola.room.repository.PaidServiceOptionRepository;
import com.hola.room.repository.RoomTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ReservationServiceImpl 단위 테스트
 *
 * 테스트 범위:
 * - CREATE (7): 정상 생성, 과거 날짜, 날짜 역전, 레이트코드 미선택, 만료, 숙박일수 위반, 객실 충돌
 * - UPDATE (5): 정상, CHECKED_OUT 불가, OTA 제한, 레이트/날짜 재검증, 서브레그 충돌
 * - STATUS CHANGE (8): 정상 전이 3개, 무효 전이, CANCELED 스킵, 얼리/레이트 요금, NO_SHOW 환불
 * - CANCEL (4): RESERVED 취소, CHECK_IN 취소, INHOUSE 불가, REFUND 거래 생성
 * - DELETE (3): SUPER_ADMIN 정상, 비권한, 비CHECKED_OUT
 * - FILTER/LIST (3): 상태, 날짜 범위, 키워드
 * - LEGS (5): 추가, 수정+충돌, 삭제 RESERVED만, 날짜 동기화, CANCELED 상태 삭제 불가
 */
@ExtendWith(MockitoExtension.class)
class ReservationServiceImplTest {

    @Mock private MasterReservationRepository masterReservationRepository;
    @Mock private SubReservationRepository subReservationRepository;
    @Mock private ReservationGuestRepository reservationGuestRepository;
    @Mock private DailyChargeRepository dailyChargeRepository;
    @Mock private ReservationDepositRepository reservationDepositRepository;
    @Mock private ReservationMemoRepository reservationMemoRepository;
    @Mock private PropertyRepository propertyRepository;
    @Mock private RateCodeRepository rateCodeRepository;
    @Mock private MarketCodeRepository marketCodeRepository;
    @Mock private FloorRepository floorRepository;
    @Mock private RoomNumberRepository roomNumberRepository;
    @Mock private RoomTypeRepository roomTypeRepository;
    @Mock private PaidServiceOptionRepository paidServiceOptionRepository;
    @Mock private ReservationServiceItemRepository serviceItemRepository;
    @Mock private ReservationMapper reservationMapper;
    @Mock private ReservationNumberGenerator numberGenerator;
    @Mock private RoomAvailabilityService availabilityService;
    @Mock private PriceCalculationService priceCalculationService;
    @Mock private EarlyLateCheckService earlyLateCheckService;
    @Mock private ReservationPaymentService paymentService;
    @Mock private AccessControlService accessControlService;
    @Mock private CancellationPolicyService cancellationPolicyService;
    @Mock private ReservationPaymentRepository reservationPaymentRepository;
    @Mock private PaymentTransactionRepository paymentTransactionRepository;
    @Mock private RateIncludedServiceHelper rateIncludedServiceHelper;
    @Mock private com.hola.rate.repository.DayUseRateRepository dayUseRateRepository;
    @Mock private jakarta.persistence.EntityManager entityManager;
    @Mock private com.hola.hotel.service.HousekeepingService housekeepingService;
    @Mock private com.hola.hotel.repository.RoomUnavailableRepository roomUnavailableRepository;
    @Mock private com.hola.room.service.InventoryService inventoryService;
    @Mock private ReservationFinder finder;
    @Mock private SubReservationCreator subCreator;
    @Mock private ReservationChangeLogService changeLogService;
    @Mock private RoomInfoResolver roomInfoResolver;

    @InjectMocks
    private ReservationServiceImpl reservationService;

    // 공통 테스트 상수
    private static final Long PROPERTY_ID = 1L;
    private static final Long MASTER_ID = 100L;
    private static final Long SUB_ID = 200L;
    private static final Long RATE_CODE_ID = 10L;
    private static final LocalDate CHECK_IN = LocalDate.of(2026, 6, 1);
    private static final LocalDate CHECK_OUT = LocalDate.of(2026, 6, 3);

    private Property property;
    private RateCode rateCode;

    @BeforeEach
    void setUp() {
        property = Property.builder()
                .propertyCode("GMP")
                .propertyName("그랜드 호텔")
                .build();

        rateCode = RateCode.builder()
                .saleStartDate(LocalDate.of(2026, 1, 1))
                .saleEndDate(LocalDate.of(2026, 12, 31))
                .minStayDays(1)
                .maxStayDays(30)
                .build();

        // Phase 1에서 추가된 OOO/OOS 기간 체크 기본 스텁 (NPE 방지)
        lenient().when(roomUnavailableRepository.findOverlapping(any(), any(), any()))
                .thenReturn(Collections.emptyList());
    }

    // ──────────────────────────────────────────────
    // 헬퍼 메서드
    // ──────────────────────────────────────────────

    /**
     * 기본 MasterReservation 생성 (리플렉션으로 id 세팅)
     */
    private MasterReservation createMaster(String status) {
        MasterReservation master = MasterReservation.builder()
                .property(property)
                .masterReservationNo("GMP260601-0001")
                .confirmationNo("HK4F29XP")
                .reservationStatus(status)
                .masterCheckIn(CHECK_IN)
                .masterCheckOut(CHECK_OUT)
                .guestNameKo("홍길동")
                .rateCodeId(RATE_CODE_ID)
                .isOtaManaged(false)
                .subReservations(new ArrayList<>())
                .build();
        setId(master, MASTER_ID);
        setId(property, PROPERTY_ID);
        return master;
    }

    /**
     * SubReservation 생성
     */
    private SubReservation createSub(MasterReservation master, Long id, String status) {
        SubReservation sub = SubReservation.builder()
                .masterReservation(master)
                .subReservationNo("GMP260601-0001-01")
                .roomReservationStatus(status)
                .roomTypeId(1L)
                .floorId(1L)
                .roomNumberId(1L)
                .adults(2)
                .children(0)
                .checkIn(CHECK_IN)
                .checkOut(CHECK_OUT)
                .earlyCheckIn(false)
                .lateCheckOut(false)
                .earlyCheckInFee(BigDecimal.ZERO)
                .lateCheckOutFee(BigDecimal.ZERO)
                .guests(new ArrayList<>())
                .dailyCharges(new ArrayList<>())
                .services(new ArrayList<>())
                .build();
        setId(sub, id);
        master.getSubReservations().add(sub);
        return sub;
    }

    /**
     * 기본 생성 요청 DTO 빌드
     */
    private ReservationCreateRequest createRequest() {
        return ReservationCreateRequest.builder()
                .masterCheckIn(CHECK_IN)
                .masterCheckOut(CHECK_OUT)
                .guestNameKo("홍길동")
                .rateCodeId(RATE_CODE_ID)
                .subReservations(List.of(
                        SubReservationRequest.builder()
                                .roomTypeId(1L)
                                .floorId(1L)
                                .roomNumberId(1L)
                                .adults(2)
                                .children(0)
                                .checkIn(CHECK_IN)
                                .checkOut(CHECK_OUT)
                                .build()
                ))
                .build();
    }

    /**
     * 기본 수정 요청 DTO 빌드
     */
    private ReservationUpdateRequest updateRequest() {
        return ReservationUpdateRequest.builder()
                .masterCheckIn(CHECK_IN)
                .masterCheckOut(CHECK_OUT)
                .guestNameKo("홍길동")
                .rateCodeId(RATE_CODE_ID)
                .subReservations(List.of(
                        SubReservationRequest.builder()
                                .id(SUB_ID)
                                .roomTypeId(1L)
                                .floorId(1L)
                                .roomNumberId(1L)
                                .adults(2)
                                .children(0)
                                .checkIn(CHECK_IN)
                                .checkOut(CHECK_OUT)
                                .build()
                ))
                .build();
    }

    /**
     * getById() 호출에 필요한 공통 스터빙
     */
    private void stubGetById(MasterReservation master) {
        ReservationDetailResponse detailResponse = ReservationDetailResponse.builder()
                .id(master.getId())
                .propertyId(PROPERTY_ID)
                .masterReservationNo(master.getMasterReservationNo())
                .reservationStatus(master.getReservationStatus())
                .masterCheckIn(master.getMasterCheckIn())
                .masterCheckOut(master.getMasterCheckOut())
                .rateCodeId(master.getRateCodeId())
                .subReservations(Collections.emptyList())
                .build();

        lenient().when(finder.findMasterById(master.getId(), PROPERTY_ID))
                .thenReturn(master);
        lenient().when(reservationMapper.toReservationDetailResponse(master))
                .thenReturn(detailResponse);
        lenient().when(reservationDepositRepository.findByMasterReservationId(master.getId()))
                .thenReturn(Collections.emptyList());
        lenient().when(reservationMemoRepository.findByMasterReservationIdOrderByCreatedAtDesc(master.getId()))
                .thenReturn(Collections.emptyList());
        lenient().when(paymentService.getPaymentSummary(PROPERTY_ID, master.getId()))
                .thenReturn(PaymentSummaryResponse.builder().build());
    }

    /**
     * 리플렉션으로 BaseEntity.id 설정
     */
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
    // CREATE 테스트 (7개)
    // ══════════════════════════════════════════════
    @Nested
    @DisplayName("예약 생성 (create)")
    class CreateTests {

        @Test
        @DisplayName("정상 예약 생성 - 마스터 + 서브 + 결제 재계산")
        void create_정상() {
            // given
            ReservationCreateRequest request = createRequest();
            MasterReservation savedMaster = createMaster("RESERVED");

            when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(property));
            when(numberGenerator.generateMasterReservationNo(property)).thenReturn("GMP260601-0001");
            when(numberGenerator.generateConfirmationNo()).thenReturn("HK4F29XP");
            when(masterReservationRepository.save(any(MasterReservation.class))).thenReturn(savedMaster);
            stubGetById(savedMaster);

            // when
            ReservationDetailResponse result = reservationService.create(PROPERTY_ID, request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getMasterReservationNo()).isEqualTo("GMP260601-0001");
            verify(masterReservationRepository).save(any(MasterReservation.class));
            verify(paymentService).recalculatePayment(MASTER_ID);
        }

        @Test
        @DisplayName("과거 체크인 날짜 - RESERVATION_CHECKIN_PAST_DATE")
        void create_과거체크인() {
            // given
            ReservationCreateRequest request = ReservationCreateRequest.builder()
                    .masterCheckIn(LocalDate.of(2025, 1, 1))
                    .masterCheckOut(LocalDate.of(2025, 1, 3))
                    .rateCodeId(RATE_CODE_ID)
                    .subReservations(List.of(SubReservationRequest.builder()
                            .checkIn(LocalDate.of(2025, 1, 1))
                            .checkOut(LocalDate.of(2025, 1, 3))
                            .build()))
                    .build();
            when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(property));

            // when & then
            assertThatThrownBy(() -> reservationService.create(PROPERTY_ID, request))
                    .isInstanceOf(HolaException.class)
                    .satisfies(ex -> assertThat(((HolaException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.RESERVATION_CHECKIN_PAST_DATE));
        }

        @Test
        @DisplayName("체크아웃이 체크인 이전 - SUB_RESERVATION_DATE_INVALID")
        void create_날짜역전() {
            // given
            ReservationCreateRequest request = ReservationCreateRequest.builder()
                    .masterCheckIn(CHECK_IN)
                    .masterCheckOut(CHECK_IN.minusDays(1))
                    .rateCodeId(RATE_CODE_ID)
                    .subReservations(List.of(SubReservationRequest.builder()
                            .checkIn(CHECK_IN)
                            .checkOut(CHECK_IN.minusDays(1))
                            .build()))
                    .build();
            when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(property));

            // when & then
            assertThatThrownBy(() -> reservationService.create(PROPERTY_ID, request))
                    .isInstanceOf(HolaException.class)
                    .satisfies(ex -> assertThat(((HolaException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.SUB_RESERVATION_DATE_INVALID));
        }

        @Test
        @DisplayName("레이트코드 미선택 - RESERVATION_RATE_REQUIRED")
        void create_레이트코드없음() {
            // given
            ReservationCreateRequest request = ReservationCreateRequest.builder()
                    .masterCheckIn(CHECK_IN)
                    .masterCheckOut(CHECK_OUT)
                    .rateCodeId(null)
                    .subReservations(List.of(SubReservationRequest.builder()
                            .checkIn(CHECK_IN)
                            .checkOut(CHECK_OUT)
                            .build()))
                    .build();
            when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(property));

            // when & then
            assertThatThrownBy(() -> reservationService.create(PROPERTY_ID, request))
                    .isInstanceOf(HolaException.class)
                    .satisfies(ex -> assertThat(((HolaException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.RESERVATION_RATE_REQUIRED));
        }

        @Test
        @DisplayName("레이트코드 판매기간 만료 - RESERVATION_RATE_EXPIRED")
        void create_레이트만료() {
            // given
            ReservationCreateRequest request = createRequest();

            when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(property));
            doThrow(new HolaException(ErrorCode.RESERVATION_RATE_EXPIRED))
                    .when(subCreator).validateRateCode(eq(RATE_CODE_ID), any(), any());

            // when & then
            assertThatThrownBy(() -> reservationService.create(PROPERTY_ID, request))
                    .isInstanceOf(HolaException.class)
                    .satisfies(ex -> assertThat(((HolaException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.RESERVATION_RATE_EXPIRED));
        }

        @Test
        @DisplayName("최소 숙박일수 위반 - RESERVATION_STAY_DAYS_VIOLATION")
        void create_숙박일수위반() {
            // given
            ReservationCreateRequest request = createRequest();

            when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(property));
            doThrow(new HolaException(ErrorCode.RESERVATION_STAY_DAYS_VIOLATION))
                    .when(subCreator).validateRateCode(eq(RATE_CODE_ID), any(), any());

            // when & then
            assertThatThrownBy(() -> reservationService.create(PROPERTY_ID, request))
                    .isInstanceOf(HolaException.class)
                    .satisfies(ex -> assertThat(((HolaException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.RESERVATION_STAY_DAYS_VIOLATION));
        }

        @Test
        @DisplayName("객실 충돌 - SUB_RESERVATION_ROOM_CONFLICT")
        void create_객실충돌() {
            // given
            ReservationCreateRequest request = createRequest();

            when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(property));
            when(numberGenerator.generateMasterReservationNo(property)).thenReturn("GMP260601-0001");
            when(numberGenerator.generateConfirmationNo()).thenReturn("HK4F29XP");
            MasterReservation savedMaster = createMaster("RESERVED");
            when(masterReservationRepository.save(any(MasterReservation.class))).thenReturn(savedMaster);
            // subCreator.create() 내부에서 객실 충돌 발생
            doThrow(new HolaException(ErrorCode.SUB_RESERVATION_ROOM_CONFLICT))
                    .when(subCreator).create(any(), any(), anyInt(), any());

            // when & then
            assertThatThrownBy(() -> reservationService.create(PROPERTY_ID, request))
                    .isInstanceOf(HolaException.class)
                    .satisfies(ex -> assertThat(((HolaException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.SUB_RESERVATION_ROOM_CONFLICT));
        }

        @Test
        @DisplayName("프로퍼티 미존재 - PROPERTY_NOT_FOUND")
        void create_프로퍼티없음() {
            // given
            ReservationCreateRequest request = createRequest();
            when(propertyRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> reservationService.create(999L, request))
                    .isInstanceOf(HolaException.class)
                    .satisfies(ex -> assertThat(((HolaException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.PROPERTY_NOT_FOUND));
        }
    }

    // ══════════════════════════════════════════════
    // UPDATE 테스트 (5개)
    // ══════════════════════════════════════════════
    @Nested
    @DisplayName("예약 수정 (update)")
    class UpdateTests {

        @Test
        @DisplayName("정상 예약 수정 - 마스터 정보 업데이트")
        void update_정상() {
            // given
            MasterReservation master = createMaster("RESERVED");
            SubReservation sub = createSub(master, SUB_ID, "RESERVED");
            ReservationUpdateRequest request = updateRequest();

            when(finder.findMasterById(MASTER_ID, PROPERTY_ID)).thenReturn(master);
            // update() → isDayUseRateCode() → rateCodeRepository.findById()
            lenient().when(rateCodeRepository.findById(RATE_CODE_ID)).thenReturn(Optional.of(rateCode));
            when(finder.findSubAndValidateOwnership(SUB_ID, master)).thenReturn(sub);
            when(availabilityService.hasRoomConflict(eq(1L), any(), any(), eq(SUB_ID),
                    any(), any())).thenReturn(false);
            stubGetById(master);

            // when
            ReservationDetailResponse result = reservationService.update(MASTER_ID, PROPERTY_ID, request);

            // then
            assertThat(result).isNotNull();
            verify(paymentService).recalculatePayment(MASTER_ID);
        }

        @Test
        @DisplayName("CHECKED_OUT 상태 수정 불가 - RESERVATION_MODIFY_NOT_ALLOWED")
        void update_체크아웃상태() {
            // given
            MasterReservation master = createMaster("CHECKED_OUT");
            ReservationUpdateRequest request = updateRequest();
            when(finder.findMasterById(MASTER_ID, PROPERTY_ID)).thenReturn(master);
            doThrow(new HolaException(ErrorCode.RESERVATION_MODIFY_NOT_ALLOWED))
                    .when(finder).validateModifiable(master);

            // when & then
            assertThatThrownBy(() -> reservationService.update(MASTER_ID, PROPERTY_ID, request))
                    .isInstanceOf(HolaException.class)
                    .satisfies(ex -> assertThat(((HolaException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.RESERVATION_MODIFY_NOT_ALLOWED));
        }

        @Test
        @DisplayName("CANCELED 상태 수정 불가 - RESERVATION_MODIFY_NOT_ALLOWED")
        void update_취소상태() {
            // given
            MasterReservation master = createMaster("CANCELED");
            ReservationUpdateRequest request = updateRequest();
            when(finder.findMasterById(MASTER_ID, PROPERTY_ID)).thenReturn(master);
            doThrow(new HolaException(ErrorCode.RESERVATION_MODIFY_NOT_ALLOWED))
                    .when(finder).validateModifiable(master);

            // when & then
            assertThatThrownBy(() -> reservationService.update(MASTER_ID, PROPERTY_ID, request))
                    .isInstanceOf(HolaException.class)
                    .satisfies(ex -> assertThat(((HolaException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.RESERVATION_MODIFY_NOT_ALLOWED));
        }

        @Test
        @DisplayName("OTA 예약 수정 제한 - RESERVATION_OTA_EDIT_RESTRICTED")
        void update_OTA제한() {
            // given
            MasterReservation master = MasterReservation.builder()
                    .property(property)
                    .masterReservationNo("GMP260601-0001")
                    .confirmationNo("HK4F29XP")
                    .reservationStatus("RESERVED")
                    .masterCheckIn(CHECK_IN)
                    .masterCheckOut(CHECK_OUT)
                    .rateCodeId(RATE_CODE_ID)
                    .isOtaManaged(true)
                    .subReservations(new ArrayList<>())
                    .build();
            setId(master, MASTER_ID);
            setId(property, PROPERTY_ID);

            ReservationUpdateRequest request = updateRequest();
            when(finder.findMasterById(MASTER_ID, PROPERTY_ID)).thenReturn(master);

            // when & then
            assertThatThrownBy(() -> reservationService.update(MASTER_ID, PROPERTY_ID, request))
                    .isInstanceOf(HolaException.class)
                    .satisfies(ex -> assertThat(((HolaException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.RESERVATION_OTA_EDIT_RESTRICTED));
        }

        @Test
        @DisplayName("레이트코드 변경 시 재검증 수행")
        void update_레이트코드변경_재검증() {
            // given
            MasterReservation master = createMaster("RESERVED");
            SubReservation sub = createSub(master, SUB_ID, "RESERVED");

            Long newRateCodeId = 20L;
            ReservationUpdateRequest request = ReservationUpdateRequest.builder()
                    .masterCheckIn(CHECK_IN)
                    .masterCheckOut(CHECK_OUT)
                    .guestNameKo("홍길동")
                    .rateCodeId(newRateCodeId)
                    .subReservations(List.of(SubReservationRequest.builder()
                            .id(SUB_ID)
                            .roomTypeId(1L)
                            .roomNumberId(1L)
                            .checkIn(CHECK_IN)
                            .checkOut(CHECK_OUT)
                            .build()))
                    .build();

            when(finder.findMasterById(MASTER_ID, PROPERTY_ID)).thenReturn(master);
            when(finder.findSubAndValidateOwnership(SUB_ID, master)).thenReturn(sub);
            when(availabilityService.hasRoomConflict(eq(1L), any(), any(), eq(SUB_ID),
                    any(), any())).thenReturn(false);
            stubGetById(master);

            // when
            reservationService.update(MASTER_ID, PROPERTY_ID, request);

            // then - 레이트코드 변경이므로 subCreator.validateRateCode 호출됨
            verify(subCreator).validateRateCode(eq(newRateCodeId), any(), any());
        }
    }

    // STATUS CHANGE / CANCEL 테스트 → ReservationStatusServiceImpl로 이동됨
    // LEGS 테스트 → ReservationLegServiceImpl로 이동됨
    // ══════════════════════════════════════════════
    // DELETE 테스트 (3개)
    // ══════════════════════════════════════════════
    @Nested
    @DisplayName("예약 삭제 (deleteReservation)")
    class DeleteTests {

        @Test
        @DisplayName("SUPER_ADMIN + CHECKED_OUT - 정상 삭제")
        void delete_정상() {
            // given
            MasterReservation master = createMaster("CHECKED_OUT");
            SubReservation sub = createSub(master, SUB_ID, "CHECKED_OUT");
            AdminUser superAdmin = AdminUser.builder()
                    .loginId("admin")
                    .role("SUPER_ADMIN")
                    .build();

            when(accessControlService.getCurrentUser()).thenReturn(superAdmin);
            when(finder.findMasterById(MASTER_ID, PROPERTY_ID)).thenReturn(master);

            // when
            reservationService.deleteReservation(MASTER_ID, PROPERTY_ID);

            // then - soft delete 확인
            assertThat(master.isDeleted()).isTrue();
            assertThat(sub.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("비SUPER_ADMIN - RESERVATION_DELETE_UNAUTHORIZED")
        void delete_권한없음() {
            // given
            AdminUser hotelAdmin = AdminUser.builder()
                    .loginId("hotel_admin")
                    .role("HOTEL_ADMIN")
                    .build();

            when(accessControlService.getCurrentUser()).thenReturn(hotelAdmin);

            // when & then
            assertThatThrownBy(() -> reservationService.deleteReservation(MASTER_ID, PROPERTY_ID))
                    .isInstanceOf(HolaException.class)
                    .satisfies(ex -> assertThat(((HolaException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.RESERVATION_DELETE_UNAUTHORIZED));
        }

        @Test
        @DisplayName("RESERVED 상태 삭제 불가 - RESERVATION_DELETE_NOT_ALLOWED")
        void delete_상태불가() {
            // given
            MasterReservation master = createMaster("RESERVED");
            AdminUser superAdmin = AdminUser.builder()
                    .loginId("admin")
                    .role("SUPER_ADMIN")
                    .build();

            when(accessControlService.getCurrentUser()).thenReturn(superAdmin);
            when(finder.findMasterById(MASTER_ID, PROPERTY_ID)).thenReturn(master);

            // when & then
            assertThatThrownBy(() -> reservationService.deleteReservation(MASTER_ID, PROPERTY_ID))
                    .isInstanceOf(HolaException.class)
                    .satisfies(ex -> assertThat(((HolaException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.RESERVATION_DELETE_NOT_ALLOWED));
        }
    }

    // ══════════════════════════════════════════════
    // FILTER/LIST 테스트 (3개)
    // ══════════════════════════════════════════════
    @Nested
    @DisplayName("예약 리스트 조회 (getList)")
    class ListTests {

        @Test
        @DisplayName("상태별 필터링 - RESERVED만 조회")
        @SuppressWarnings("unchecked")
        void getList_상태필터() {
            // given
            MasterReservation reserved = createMaster("RESERVED");

            when(masterReservationRepository.findAll(any(Specification.class), any(Sort.class)))
                    .thenReturn(List.of(reserved));

            ReservationListResponse listResponse = ReservationListResponse.builder()
                    .id(MASTER_ID)
                    .masterReservationNo("GMP260601-0001")
                    .reservationStatus("RESERVED")
                    .build();
            when(reservationMapper.toReservationListResponse(reserved)).thenReturn(listResponse);

            // when
            List<ReservationListResponse> result = reservationService.getList(
                    PROPERTY_ID, "RESERVED", null, null, null);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getReservationStatus()).isEqualTo("RESERVED");
        }

        @Test
        @DisplayName("날짜 범위 필터링")
        @SuppressWarnings("unchecked")
        void getList_날짜범위() {
            // given
            MasterReservation inRange = createMaster("RESERVED");

            when(masterReservationRepository.findAll(any(Specification.class), any(Sort.class)))
                    .thenReturn(List.of(inRange));

            ReservationListResponse listResponse = ReservationListResponse.builder()
                    .id(MASTER_ID)
                    .masterReservationNo("GMP260601-0001")
                    .build();
            when(reservationMapper.toReservationListResponse(inRange)).thenReturn(listResponse);

            // when - 6월만 조회
            List<ReservationListResponse> result = reservationService.getList(
                    PROPERTY_ID, null,
                    LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30),
                    null);

            // then
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("키워드 필터링 - 예약번호/게스트명/전화번호")
        @SuppressWarnings("unchecked")
        void getList_키워드() {
            // given
            MasterReservation master = createMaster("RESERVED");

            when(masterReservationRepository.findAll(any(Specification.class), any(Sort.class)))
                    .thenReturn(List.of(master));

            ReservationListResponse listResponse = ReservationListResponse.builder()
                    .id(MASTER_ID)
                    .masterReservationNo("GMP260601-0001")
                    .guestNameKo("홍길동")
                    .build();
            when(reservationMapper.toReservationListResponse(master)).thenReturn(listResponse);

            // when - 게스트명으로 검색
            List<ReservationListResponse> result = reservationService.getList(
                    PROPERTY_ID, null, null, null, "홍길동");

            // then
            assertThat(result).hasSize(1);
        }
    }

    // ══════════════════════════════════════════════
    // ══════════════════════════════════════════════
    // 기타 테스트
    // ══════════════════════════════════════════════
    @Nested
    @DisplayName("기타 기능")
    class MiscTests {

        @Test
        @DisplayName("예약 상세 조회 (getById) - 정상")
        void getById_정상() {
            // given
            MasterReservation master = createMaster("RESERVED");
            SubReservation sub = createSub(master, SUB_ID, "RESERVED");

            ReservationDetailResponse detailResponse = ReservationDetailResponse.builder()
                    .id(MASTER_ID)
                    .propertyId(PROPERTY_ID)
                    .masterReservationNo("GMP260601-0001")
                    .reservationStatus("RESERVED")
                    .masterCheckIn(CHECK_IN)
                    .masterCheckOut(CHECK_OUT)
                    .rateCodeId(RATE_CODE_ID)
                    .subReservations(Collections.emptyList())
                    .build();

            when(finder.findMasterById(MASTER_ID, PROPERTY_ID)).thenReturn(master);
            when(reservationMapper.toReservationDetailResponse(master)).thenReturn(detailResponse);
            when(reservationDepositRepository.findByMasterReservationId(MASTER_ID))
                    .thenReturn(Collections.emptyList());
            when(reservationMemoRepository.findByMasterReservationIdOrderByCreatedAtDesc(MASTER_ID))
                    .thenReturn(Collections.emptyList());
            when(paymentService.getPaymentSummary(PROPERTY_ID, MASTER_ID))
                    .thenReturn(PaymentSummaryResponse.builder().build());

            // when
            ReservationDetailResponse result = reservationService.getById(MASTER_ID, PROPERTY_ID);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(MASTER_ID);
            assertThat(result.getReservationStatus()).isEqualTo("RESERVED");
        }

        @Test
        @DisplayName("예약 미존재 시 - RESERVATION_NOT_FOUND")
        void getById_미존재() {
            // given
            when(finder.findMasterById(999L, PROPERTY_ID))
                    .thenThrow(new HolaException(ErrorCode.RESERVATION_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> reservationService.getById(999L, PROPERTY_ID))
                    .isInstanceOf(HolaException.class)
                    .satisfies(ex -> assertThat(((HolaException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.RESERVATION_NOT_FOUND));
        }

        @Test
        @DisplayName("프로퍼티 불일치 시 - RESERVATION_NOT_FOUND")
        void getById_프로퍼티불일치() {
            // given
            when(finder.findMasterById(MASTER_ID, 999L))
                    .thenThrow(new HolaException(ErrorCode.RESERVATION_NOT_FOUND));

            // when & then - 다른 프로퍼티 ID로 조회
            assertThatThrownBy(() -> reservationService.getById(MASTER_ID, 999L))
                    .isInstanceOf(HolaException.class)
                    .satisfies(ex -> assertThat(((HolaException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.RESERVATION_NOT_FOUND));
        }

        @Test
        @DisplayName("최대 숙박일수 초과 시 - RESERVATION_STAY_DAYS_VIOLATION")
        void create_최대숙박일수초과() {
            // given
            // 5박 예약 시도 (최대 2박)
            ReservationCreateRequest request = ReservationCreateRequest.builder()
                    .masterCheckIn(CHECK_IN)
                    .masterCheckOut(CHECK_IN.plusDays(5))
                    .rateCodeId(RATE_CODE_ID)
                    .subReservations(List.of(SubReservationRequest.builder()
                            .checkIn(CHECK_IN)
                            .checkOut(CHECK_IN.plusDays(5))
                            .build()))
                    .build();

            when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(property));
            doThrow(new HolaException(ErrorCode.RESERVATION_STAY_DAYS_VIOLATION))
                    .when(subCreator).validateRateCode(eq(RATE_CODE_ID), any(), any());

            // when & then
            assertThatThrownBy(() -> reservationService.create(PROPERTY_ID, request))
                    .isInstanceOf(HolaException.class)
                    .satisfies(ex -> assertThat(((HolaException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.RESERVATION_STAY_DAYS_VIOLATION));
        }

        @Test
        @DisplayName("NO_SHOW 상태 수정 불가 - RESERVATION_MODIFY_NOT_ALLOWED")
        void update_NO_SHOW상태() {
            // given
            MasterReservation master = createMaster("NO_SHOW");
            ReservationUpdateRequest request = updateRequest();
            when(finder.findMasterById(MASTER_ID, PROPERTY_ID)).thenReturn(master);
            doThrow(new HolaException(ErrorCode.RESERVATION_MODIFY_NOT_ALLOWED))
                    .when(finder).validateModifiable(master);

            // when & then
            assertThatThrownBy(() -> reservationService.update(MASTER_ID, PROPERTY_ID, request))
                    .isInstanceOf(HolaException.class)
                    .satisfies(ex -> assertThat(((HolaException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.RESERVATION_MODIFY_NOT_ALLOWED));
        }
    }

    // ══════════════════════════════════════════════
    // 레이트코드 변경 미리보기 (previewRateChange)
    // ══════════════════════════════════════════════
    @Nested
    @DisplayName("레이트코드 변경 미리보기 (previewRateChange)")
    class PreviewRateChangeTests {

        @Test
        @DisplayName("정상 미리보기 - 현재/새 요금 차액 계산")
        void previewRateChange_정상() {
            // given
            MasterReservation master = createMaster("RESERVED");
            SubReservation sub = createSub(master, SUB_ID, "RESERVED");

            // 현재 DailyCharge
            DailyCharge charge1 = DailyCharge.builder()
                    .chargeDate(CHECK_IN).supplyPrice(new BigDecimal("100000"))
                    .tax(BigDecimal.ZERO).serviceCharge(BigDecimal.ZERO).total(new BigDecimal("100000"))
                    .build();
            DailyCharge charge2 = DailyCharge.builder()
                    .chargeDate(CHECK_IN.plusDays(1)).supplyPrice(new BigDecimal("100000"))
                    .tax(BigDecimal.ZERO).serviceCharge(BigDecimal.ZERO).total(new BigDecimal("100000"))
                    .build();
            sub.getDailyCharges().add(charge1);
            sub.getDailyCharges().add(charge2);

            // 현재 레이트코드
            RateCode currentRate = RateCode.builder()
                    .rateCode("STD").rateNameKo("스탠다드")
                    .saleStartDate(CHECK_IN.minusMonths(1)).saleEndDate(CHECK_OUT.plusMonths(1))
                    .build();
            setId(currentRate, RATE_CODE_ID);

            // 새 레이트코드
            Long newRateCodeId = 20L;
            RateCode newRate = RateCode.builder()
                    .rateCode("DLX").rateNameKo("디럭스")
                    .saleStartDate(CHECK_IN.minusMonths(1)).saleEndDate(CHECK_OUT.plusMonths(1))
                    .build();
            setId(newRate, newRateCodeId);

            when(finder.findMasterById(MASTER_ID, PROPERTY_ID)).thenReturn(master);
            when(rateCodeRepository.findById(RATE_CODE_ID)).thenReturn(Optional.of(currentRate));
            when(rateCodeRepository.findById(newRateCodeId)).thenReturn(Optional.of(newRate));
            when(roomInfoResolver.resolveRoomTypeCodes(any())).thenReturn(Map.of(1L, "STD"));

            // 새 요금 계산 결과 (2박 × 150,000)
            DailyCharge newCharge1 = DailyCharge.builder()
                    .chargeDate(CHECK_IN).supplyPrice(new BigDecimal("150000"))
                    .tax(BigDecimal.ZERO).serviceCharge(BigDecimal.ZERO).total(new BigDecimal("150000"))
                    .build();
            DailyCharge newCharge2 = DailyCharge.builder()
                    .chargeDate(CHECK_IN.plusDays(1)).supplyPrice(new BigDecimal("150000"))
                    .tax(BigDecimal.ZERO).serviceCharge(BigDecimal.ZERO).total(new BigDecimal("150000"))
                    .build();
            when(priceCalculationService.calculateDailyCharges(eq(newRateCodeId), any(), any(), any(),
                    anyInt(), anyInt(), any()))
                    .thenReturn(List.of(newCharge1, newCharge2));

            // when
            RateChangePreviewResponse result = reservationService.previewRateChange(MASTER_ID, PROPERTY_ID, newRateCodeId);

            // then
            assertThat(result.getCurrentRateCodeId()).isEqualTo(RATE_CODE_ID);
            assertThat(result.getNewRateCodeId()).isEqualTo(newRateCodeId);
            assertThat(result.getCurrentTotal()).isEqualByComparingTo("200000");
            assertThat(result.getNewTotal()).isEqualByComparingTo("300000");
            assertThat(result.getDifference()).isEqualByComparingTo("100000");
            assertThat(result.getLegs()).hasSize(1);
            assertThat(result.getLegs().get(0).getDifference()).isEqualByComparingTo("100000");
        }

        @Test
        @DisplayName("존재하지 않는 레이트코드 → RATE_CODE_NOT_FOUND")
        void previewRateChange_레이트없음() {
            // given
            MasterReservation master = createMaster("RESERVED");
            when(finder.findMasterById(MASTER_ID, PROPERTY_ID)).thenReturn(master);
            lenient().when(rateCodeRepository.findById(RATE_CODE_ID)).thenReturn(Optional.empty());
            when(rateCodeRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> reservationService.previewRateChange(MASTER_ID, PROPERTY_ID, 999L))
                    .isInstanceOf(HolaException.class)
                    .satisfies(ex -> assertThat(((HolaException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.RATE_CODE_NOT_FOUND));
        }

        @Test
        @DisplayName("취소된 Leg은 미리보기에서 제외")
        void previewRateChange_취소Leg제외() {
            // given
            MasterReservation master = createMaster("RESERVED");
            SubReservation activeSub = createSub(master, SUB_ID, "RESERVED");
            SubReservation canceledSub = createSub(master, 300L, "CANCELED");

            DailyCharge charge = DailyCharge.builder()
                    .chargeDate(CHECK_IN).supplyPrice(new BigDecimal("100000"))
                    .tax(BigDecimal.ZERO).serviceCharge(BigDecimal.ZERO).total(new BigDecimal("100000"))
                    .build();
            activeSub.getDailyCharges().add(charge);

            Long newRateCodeId = 20L;
            RateCode newRate = RateCode.builder()
                    .rateCode("DLX").rateNameKo("디럭스")
                    .saleStartDate(CHECK_IN.minusMonths(1)).saleEndDate(CHECK_OUT.plusMonths(1))
                    .build();
            setId(newRate, newRateCodeId);

            when(finder.findMasterById(MASTER_ID, PROPERTY_ID)).thenReturn(master);
            lenient().when(rateCodeRepository.findById(RATE_CODE_ID)).thenReturn(Optional.empty());
            when(rateCodeRepository.findById(newRateCodeId)).thenReturn(Optional.of(newRate));
            when(roomInfoResolver.resolveRoomTypeCodes(any())).thenReturn(Map.of(1L, "STD"));

            DailyCharge newCharge = DailyCharge.builder()
                    .chargeDate(CHECK_IN).supplyPrice(new BigDecimal("120000"))
                    .tax(BigDecimal.ZERO).serviceCharge(BigDecimal.ZERO).total(new BigDecimal("120000"))
                    .build();
            when(priceCalculationService.calculateDailyCharges(eq(newRateCodeId), any(), any(), any(),
                    anyInt(), anyInt(), any()))
                    .thenReturn(List.of(newCharge));

            // when
            RateChangePreviewResponse result = reservationService.previewRateChange(MASTER_ID, PROPERTY_ID, newRateCodeId);

            // then — 취소된 Leg은 제외, 활성 Leg만 포함
            assertThat(result.getLegs()).hasSize(1);
            assertThat(result.getLegs().get(0).getLegId()).isEqualTo(SUB_ID);
        }
    }
}
