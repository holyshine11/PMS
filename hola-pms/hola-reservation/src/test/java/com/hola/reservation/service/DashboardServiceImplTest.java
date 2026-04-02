package com.hola.reservation.service;

import com.hola.hotel.entity.Hotel;
import com.hola.hotel.entity.Property;
import com.hola.hotel.repository.PropertyRepository;
import com.hola.hotel.repository.RoomNumberRepository;
import com.hola.reservation.dto.response.DashboardOperationResponse;
import com.hola.reservation.dto.response.DashboardPickupResponse;
import com.hola.reservation.dto.response.DashboardPropertyKpiResponse;
import com.hola.reservation.repository.DailyChargeRepository;
import com.hola.reservation.repository.SubReservationRepository;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DashboardServiceImpl 단위 테스트
 *
 * 테스트 범위:
 * - getPropertyKpi: 정상, 제로 객실, 매출 없음, Dayuse 분리, 전일대비
 * - getOperation: 운영현황 카운트
 * - getPickup: 7일 예약 추이
 * - getAllPropertyKpis: 벌크 조회, 정렬, 빈 프로퍼티
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardServiceImpl")
class DashboardServiceImplTest {

    @Mock private SubReservationRepository subReservationRepository;
    @Mock private DailyChargeRepository dailyChargeRepository;
    @Mock private RoomNumberRepository roomNumberRepository;
    @Mock private PropertyRepository propertyRepository;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    // 공통 테스트 상수
    private static final Long PROPERTY_ID = 1L;
    private static final Long HOTEL_ID = 10L;

    private Property property;
    private Hotel hotel;

    @BeforeEach
    void setUp() {
        hotel = Hotel.builder()
                .hotelName("올라 서울 호텔")
                .build();
        setId(hotel, HOTEL_ID);

        property = Property.builder()
                .hotel(hotel)
                .propertyCode("GMP")
                .propertyName("올라 그랜드 명동")
                .build();
        setId(property, PROPERTY_ID);
    }

    // ──────────────────────────────────────────────
    // 헬퍼 메서드
    // ──────────────────────────────────────────────

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
    // getPropertyKpi 테스트
    // ══════════════════════════════════════════════
    @Nested
    @DisplayName("프로퍼티 KPI 조회 (getPropertyKpi)")
    class GetPropertyKpiTests {

        @Test
        @DisplayName("정상 조회 - 판매객실, 매출, OCC%, ADR, RevPAR 계산")
        void getPropertyKpi_정상_KPI계산() {
            // given
            LocalDate today = LocalDate.now();
            LocalDate yesterday = today.minusDays(1);

            when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(property));
            when(roomNumberRepository.countByPropertyId(PROPERTY_ID)).thenReturn(100L);

            // 오늘: 판매 80, Dayuse 10 → 숙박 70, 매출 7,000,000, Dayuse 매출 500,000 → 숙박 매출 6,500,000
            when(subReservationRepository.countSoldRooms(PROPERTY_ID, today)).thenReturn(80L);
            when(subReservationRepository.countDayUseRooms(PROPERTY_ID, today)).thenReturn(10L);
            when(dailyChargeRepository.sumRevenueByPropertyAndDate(PROPERTY_ID, today))
                    .thenReturn(new BigDecimal("7000000"));
            when(dailyChargeRepository.sumDayUseRevenueByPropertyAndDate(PROPERTY_ID, today))
                    .thenReturn(new BigDecimal("500000"));

            // 어제: 판매 60, Dayuse 5 → 숙박 55
            when(subReservationRepository.countSoldRooms(PROPERTY_ID, yesterday)).thenReturn(60L);
            when(subReservationRepository.countDayUseRooms(PROPERTY_ID, yesterday)).thenReturn(5L);
            when(dailyChargeRepository.sumRevenueByPropertyAndDate(PROPERTY_ID, yesterday))
                    .thenReturn(new BigDecimal("5500000"));
            when(dailyChargeRepository.sumDayUseRevenueByPropertyAndDate(PROPERTY_ID, yesterday))
                    .thenReturn(new BigDecimal("300000"));

            // when
            DashboardPropertyKpiResponse result = dashboardService.getPropertyKpi(PROPERTY_ID);

            // then
            assertThat(result.getPropertyId()).isEqualTo(PROPERTY_ID);
            assertThat(result.getHotelId()).isEqualTo(HOTEL_ID);
            assertThat(result.getPropertyName()).isEqualTo("올라 그랜드 명동");
            assertThat(result.getTotalRooms()).isEqualTo(100L);
            assertThat(result.getSoldRooms()).isEqualTo(80L);
            assertThat(result.getDayUseRooms()).isEqualTo(10L);
            assertThat(result.getDayUseRevenue()).isEqualByComparingTo(new BigDecimal("500000"));
            assertThat(result.getTotalRevenue()).isEqualByComparingTo(new BigDecimal("7000000"));

            // OCC% = 70/100 * 100 = 70.0
            assertThat(result.getOccupancyRate()).isEqualByComparingTo(new BigDecimal("70.0"));
            // ADR = 6,500,000 / 70 = 92857 (HALF_UP)
            assertThat(result.getAdr()).isEqualByComparingTo(new BigDecimal("92857"));
            // RevPAR = 6,500,000 / 100 = 65000
            assertThat(result.getRevPar()).isEqualByComparingTo(new BigDecimal("65000"));

            // 어제 KPI 존재 확인
            assertThat(result.getYesterdayOccupancyRate()).isNotNull();
            assertThat(result.getYesterdayAdr()).isNotNull();
            assertThat(result.getYesterdayRevPar()).isNotNull();
            assertThat(result.getYesterdayRevenue()).isEqualByComparingTo(new BigDecimal("5500000"));
        }

        @Test
        @DisplayName("객실 수가 0인 경우 - OCC/RevPAR은 0, ADR도 0")
        void getPropertyKpi_제로객실_OCC_ADR_RevPAR_제로() {
            // given
            LocalDate today = LocalDate.now();
            LocalDate yesterday = today.minusDays(1);

            when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(property));
            when(roomNumberRepository.countByPropertyId(PROPERTY_ID)).thenReturn(0L);

            when(subReservationRepository.countSoldRooms(eq(PROPERTY_ID), any())).thenReturn(0L);
            when(subReservationRepository.countDayUseRooms(eq(PROPERTY_ID), any())).thenReturn(0L);
            when(dailyChargeRepository.sumRevenueByPropertyAndDate(eq(PROPERTY_ID), any()))
                    .thenReturn(BigDecimal.ZERO);
            when(dailyChargeRepository.sumDayUseRevenueByPropertyAndDate(eq(PROPERTY_ID), any()))
                    .thenReturn(BigDecimal.ZERO);

            // when
            DashboardPropertyKpiResponse result = dashboardService.getPropertyKpi(PROPERTY_ID);

            // then
            assertThat(result.getTotalRooms()).isEqualTo(0L);
            assertThat(result.getOccupancyRate()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getAdr()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getRevPar()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("매출이 없는 경우 (전부 무료 배정) - 지표 정상 계산")
        void getPropertyKpi_매출없음_지표제로() {
            // given
            LocalDate today = LocalDate.now();
            LocalDate yesterday = today.minusDays(1);

            when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(property));
            when(roomNumberRepository.countByPropertyId(PROPERTY_ID)).thenReturn(50L);

            when(subReservationRepository.countSoldRooms(eq(PROPERTY_ID), eq(today))).thenReturn(10L);
            when(subReservationRepository.countDayUseRooms(eq(PROPERTY_ID), eq(today))).thenReturn(0L);
            when(dailyChargeRepository.sumRevenueByPropertyAndDate(eq(PROPERTY_ID), eq(today)))
                    .thenReturn(BigDecimal.ZERO);
            when(dailyChargeRepository.sumDayUseRevenueByPropertyAndDate(eq(PROPERTY_ID), eq(today)))
                    .thenReturn(BigDecimal.ZERO);

            // 어제
            when(subReservationRepository.countSoldRooms(eq(PROPERTY_ID), eq(yesterday))).thenReturn(0L);
            when(subReservationRepository.countDayUseRooms(eq(PROPERTY_ID), eq(yesterday))).thenReturn(0L);
            when(dailyChargeRepository.sumRevenueByPropertyAndDate(eq(PROPERTY_ID), eq(yesterday)))
                    .thenReturn(BigDecimal.ZERO);
            when(dailyChargeRepository.sumDayUseRevenueByPropertyAndDate(eq(PROPERTY_ID), eq(yesterday)))
                    .thenReturn(BigDecimal.ZERO);

            // when
            DashboardPropertyKpiResponse result = dashboardService.getPropertyKpi(PROPERTY_ID);

            // then
            assertThat(result.getSoldRooms()).isEqualTo(10L);
            // OCC% = 10/50 * 100 = 20.0
            assertThat(result.getOccupancyRate()).isEqualByComparingTo(new BigDecimal("20.0"));
            // ADR = 0 / 10 = 0
            assertThat(result.getAdr()).isEqualByComparingTo(BigDecimal.ZERO);
            // RevPAR = 0 / 50 = 0
            assertThat(result.getRevPar()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("프로퍼티가 존재하지 않는 경우 - null 안전 처리")
        void getPropertyKpi_프로퍼티없음_null안전() {
            // given
            LocalDate today = LocalDate.now();
            LocalDate yesterday = today.minusDays(1);

            when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.empty());
            when(roomNumberRepository.countByPropertyId(PROPERTY_ID)).thenReturn(0L);
            when(subReservationRepository.countSoldRooms(eq(PROPERTY_ID), any())).thenReturn(0L);
            when(subReservationRepository.countDayUseRooms(eq(PROPERTY_ID), any())).thenReturn(0L);
            when(dailyChargeRepository.sumRevenueByPropertyAndDate(eq(PROPERTY_ID), any()))
                    .thenReturn(BigDecimal.ZERO);
            when(dailyChargeRepository.sumDayUseRevenueByPropertyAndDate(eq(PROPERTY_ID), any()))
                    .thenReturn(BigDecimal.ZERO);

            // when
            DashboardPropertyKpiResponse result = dashboardService.getPropertyKpi(PROPERTY_ID);

            // then - null property 시 propertyName 빈 문자열, hotelId null
            assertThat(result.getPropertyName()).isEmpty();
            assertThat(result.getHotelId()).isNull();
        }
    }

    // ══════════════════════════════════════════════
    // getOperation 테스트
    // ══════════════════════════════════════════════
    @Nested
    @DisplayName("운영현황 조회 (getOperation)")
    class GetOperationTests {

        @Test
        @DisplayName("정상 운영현황 카운트 반환")
        void getOperation_정상() {
            // given
            LocalDate today = LocalDate.now();

            when(subReservationRepository.countArrivals(PROPERTY_ID, today)).thenReturn(15L);
            when(subReservationRepository.countInHouse(PROPERTY_ID, today)).thenReturn(60L);
            when(subReservationRepository.countDepartures(PROPERTY_ID, today)).thenReturn(20L);
            when(subReservationRepository.countCheckedInToday(PROPERTY_ID, today)).thenReturn(12L);
            when(subReservationRepository.countCheckedOutToday(PROPERTY_ID, today)).thenReturn(18L);

            // when
            DashboardOperationResponse result = dashboardService.getOperation(PROPERTY_ID);

            // then
            assertThat(result.getArrivals()).isEqualTo(15L);
            assertThat(result.getInHouse()).isEqualTo(60L);
            assertThat(result.getDepartures()).isEqualTo(20L);
            assertThat(result.getCheckedInToday()).isEqualTo(12L);
            assertThat(result.getCheckedOutToday()).isEqualTo(18L);
        }

        @Test
        @DisplayName("모든 카운트가 0인 경우")
        void getOperation_제로카운트() {
            // given
            LocalDate today = LocalDate.now();

            when(subReservationRepository.countArrivals(PROPERTY_ID, today)).thenReturn(0L);
            when(subReservationRepository.countInHouse(PROPERTY_ID, today)).thenReturn(0L);
            when(subReservationRepository.countDepartures(PROPERTY_ID, today)).thenReturn(0L);
            when(subReservationRepository.countCheckedInToday(PROPERTY_ID, today)).thenReturn(0L);
            when(subReservationRepository.countCheckedOutToday(PROPERTY_ID, today)).thenReturn(0L);

            // when
            DashboardOperationResponse result = dashboardService.getOperation(PROPERTY_ID);

            // then
            assertThat(result.getArrivals()).isZero();
            assertThat(result.getInHouse()).isZero();
            assertThat(result.getDepartures()).isZero();
            assertThat(result.getCheckedInToday()).isZero();
            assertThat(result.getCheckedOutToday()).isZero();
        }
    }

    // ══════════════════════════════════════════════
    // getPickup 테스트
    // ══════════════════════════════════════════════
    @Nested
    @DisplayName("7일 예약 추이 (getPickup)")
    class GetPickupTests {

        @Test
        @DisplayName("정상 7일 추이 데이터 반환")
        void getPickup_정상_7일데이터() {
            // given
            LocalDate today = LocalDate.now();
            LocalDate endDate = today.plusDays(6);

            // 매출 벌크 조회 결과
            List<Object[]> revenueRows = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                revenueRows.add(new Object[]{today.plusDays(i), new BigDecimal("100000").multiply(BigDecimal.valueOf(i + 1))});
            }
            when(dailyChargeRepository.sumRevenueByPropertyAndDateRange(PROPERTY_ID, today, endDate))
                    .thenReturn(revenueRows);

            // 체류 예약 수
            for (int i = 0; i < 7; i++) {
                when(subReservationRepository.countOccupiedByDate(eq(PROPERTY_ID), eq(today.plusDays(i))))
                        .thenReturn((long) (10 + i));
            }

            // when
            DashboardPickupResponse result = dashboardService.getPickup(PROPERTY_ID);

            // then
            assertThat(result.getDailyPickups()).hasSize(7);
            assertThat(result.getDailyPickups().get(0).getDate()).isEqualTo(today);
            assertThat(result.getDailyPickups().get(0).getReservationCount()).isEqualTo(10L);
            assertThat(result.getDailyPickups().get(0).getRevenue()).isEqualByComparingTo(new BigDecimal("100000"));
            assertThat(result.getDailyPickups().get(6).getDate()).isEqualTo(today.plusDays(6));
            assertThat(result.getDailyPickups().get(6).getReservationCount()).isEqualTo(16L);
        }

        @Test
        @DisplayName("매출 데이터가 없는 날짜는 ZERO 처리")
        void getPickup_매출없는날짜_ZERO() {
            // given
            LocalDate today = LocalDate.now();
            LocalDate endDate = today.plusDays(6);

            // 일부 날짜에만 매출 존재 (day 0, day 3만)
            List<Object[]> revenueRows = List.of(
                    new Object[]{today, new BigDecimal("200000")},
                    new Object[]{today.plusDays(3), new BigDecimal("150000")}
            );
            when(dailyChargeRepository.sumRevenueByPropertyAndDateRange(PROPERTY_ID, today, endDate))
                    .thenReturn(revenueRows);

            for (int i = 0; i < 7; i++) {
                when(subReservationRepository.countOccupiedByDate(eq(PROPERTY_ID), eq(today.plusDays(i))))
                        .thenReturn(0L);
            }

            // when
            DashboardPickupResponse result = dashboardService.getPickup(PROPERTY_ID);

            // then
            assertThat(result.getDailyPickups().get(0).getRevenue()).isEqualByComparingTo(new BigDecimal("200000"));
            assertThat(result.getDailyPickups().get(1).getRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getDailyPickups().get(3).getRevenue()).isEqualByComparingTo(new BigDecimal("150000"));
            assertThat(result.getDailyPickups().get(6).getRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("체류 예약 수 쿼리 실패 시 0으로 대체")
        void getPickup_쿼리실패_제로대체() {
            // given
            LocalDate today = LocalDate.now();
            LocalDate endDate = today.plusDays(6);

            when(dailyChargeRepository.sumRevenueByPropertyAndDateRange(PROPERTY_ID, today, endDate))
                    .thenReturn(Collections.emptyList());

            // 일부 날짜에서 예외 발생
            when(subReservationRepository.countOccupiedByDate(eq(PROPERTY_ID), eq(today)))
                    .thenThrow(new RuntimeException("DB 연결 오류"));
            for (int i = 1; i < 7; i++) {
                when(subReservationRepository.countOccupiedByDate(eq(PROPERTY_ID), eq(today.plusDays(i))))
                        .thenReturn(5L);
            }

            // when
            DashboardPickupResponse result = dashboardService.getPickup(PROPERTY_ID);

            // then - 예외 발생 날짜는 count=0, 나머지는 5
            assertThat(result.getDailyPickups().get(0).getReservationCount()).isZero();
            assertThat(result.getDailyPickups().get(1).getReservationCount()).isEqualTo(5L);
        }
    }

    // ══════════════════════════════════════════════
    // getAllPropertyKpis 테스트
    // ══════════════════════════════════════════════
    @Nested
    @DisplayName("전체 프로퍼티 KPI 목록 (getAllPropertyKpis)")
    class GetAllPropertyKpisTests {

        @Test
        @DisplayName("프로퍼티가 없는 경우 빈 리스트 반환")
        void getAllPropertyKpis_빈프로퍼티_빈리스트() {
            // given
            when(propertyRepository.findAllByUseYnTrue()).thenReturn(Collections.emptyList());

            // when
            List<DashboardPropertyKpiResponse> result = dashboardService.getAllPropertyKpis();

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("복수 프로퍼티 벌크 조회 + OCC% 내림차순 정렬")
        void getAllPropertyKpis_복수프로퍼티_정렬() {
            // given
            LocalDate today = LocalDate.now();
            LocalDate yesterday = today.minusDays(1);

            Property property2 = Property.builder()
                    .hotel(hotel)
                    .propertyCode("GMS")
                    .propertyName("올라 명동 스위트")
                    .build();
            setId(property2, 2L);

            when(propertyRepository.findAllByUseYnTrue()).thenReturn(List.of(property, property2));

            // 총 객실 수 벌크
            when(roomNumberRepository.countByPropertyIdBulk()).thenReturn(List.of(
                    new Object[]{1L, 100L},
                    new Object[]{2L, 50L}
            ));

            // 오늘 판매객실 벌크
            when(subReservationRepository.countSoldRoomsBulk(today)).thenReturn(List.of(
                    new Object[]{1L, 30L},  // property1: 30/100 = 30%
                    new Object[]{2L, 40L}   // property2: 40/50 = 80%
            ));
            when(subReservationRepository.countDayUseRoomsBulk(today)).thenReturn(List.of(
                    new Object[]{1L, 0L},
                    new Object[]{2L, 0L}
            ));

            // 오늘 매출 벌크
            when(dailyChargeRepository.sumRevenueBulkByDate(today)).thenReturn(List.of(
                    new Object[]{1L, new BigDecimal("3000000")},
                    new Object[]{2L, new BigDecimal("4000000")}
            ));
            when(dailyChargeRepository.sumDayUseRevenueBulkByDate(today)).thenReturn(List.of(
                    new Object[]{1L, BigDecimal.ZERO},
                    new Object[]{2L, BigDecimal.ZERO}
            ));

            // 어제 데이터 (빈 결과로 처리)
            when(subReservationRepository.countSoldRoomsBulk(yesterday)).thenReturn(Collections.emptyList());
            when(subReservationRepository.countDayUseRoomsBulk(yesterday)).thenReturn(Collections.emptyList());
            when(dailyChargeRepository.sumRevenueBulkByDate(yesterday)).thenReturn(Collections.emptyList());
            when(dailyChargeRepository.sumDayUseRevenueBulkByDate(yesterday)).thenReturn(Collections.emptyList());

            // when
            List<DashboardPropertyKpiResponse> result = dashboardService.getAllPropertyKpis();

            // then - OCC% 내림차순: property2(80%) > property1(30%)
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getPropertyName()).isEqualTo("올라 명동 스위트");
            assertThat(result.get(0).getOccupancyRate()).isEqualByComparingTo(new BigDecimal("80.0"));
            assertThat(result.get(1).getPropertyName()).isEqualTo("올라 그랜드 명동");
            assertThat(result.get(1).getOccupancyRate()).isEqualByComparingTo(new BigDecimal("30.0"));
        }
    }
}
