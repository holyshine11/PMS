package com.hola.reservation.service;

import com.hola.hotel.entity.Property;
import com.hola.hotel.repository.PropertyRepository;
import com.hola.hotel.repository.RoomNumberRepository;
import com.hola.reservation.dto.response.DashboardOperationResponse;
import com.hola.reservation.dto.response.DashboardPickupResponse;
import com.hola.reservation.dto.response.DashboardPropertyKpiResponse;
import com.hola.reservation.repository.DailyChargeRepository;
import com.hola.reservation.repository.SubReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 대시보드 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final SubReservationRepository subReservationRepository;
    private final DailyChargeRepository dailyChargeRepository;
    private final RoomNumberRepository roomNumberRepository;
    private final PropertyRepository propertyRepository;

    @Override
    public DashboardPropertyKpiResponse getPropertyKpi(Long propertyId) {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        Property property = propertyRepository.findById(propertyId).orElse(null);
        String propertyName = property != null ? property.getPropertyName() : "";

        long totalRooms = roomNumberRepository.countByPropertyId(propertyId);

        // 오늘 KPI
        long soldRooms = subReservationRepository.countSoldRooms(propertyId, today);
        long dayUseRooms = subReservationRepository.countDayUseRooms(propertyId, today);
        long overnightSold = soldRooms - dayUseRooms;
        BigDecimal totalRevenue = dailyChargeRepository.sumRevenueByPropertyAndDate(propertyId, today);
        BigDecimal dayUseRevenue = dailyChargeRepository.sumDayUseRevenueByPropertyAndDate(propertyId, today);
        BigDecimal overnightRevenue = totalRevenue.subtract(dayUseRevenue);

        BigDecimal occupancyRate = BigDecimal.ZERO;
        BigDecimal adr = BigDecimal.ZERO;
        BigDecimal revPar = BigDecimal.ZERO;

        // OCC%/ADR/RevPAR은 숙박 객실만 기준 (Dayuse는 별도 표시)
        if (totalRooms > 0) {
            occupancyRate = BigDecimal.valueOf(overnightSold)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalRooms), 1, RoundingMode.HALF_UP);
            revPar = overnightRevenue.divide(BigDecimal.valueOf(totalRooms), 0, RoundingMode.HALF_UP);
        }
        if (overnightSold > 0) {
            adr = overnightRevenue.divide(BigDecimal.valueOf(overnightSold), 0, RoundingMode.HALF_UP);
        }

        // 어제 KPI (전일 대비 트렌드 계산용) — 오늘과 동일 기준(숙박만)으로 계산
        long yesterdaySoldAll = subReservationRepository.countSoldRooms(propertyId, yesterday);
        long yesterdayDayUse = subReservationRepository.countDayUseRooms(propertyId, yesterday);
        long yesterdayOvernightSold = yesterdaySoldAll - yesterdayDayUse;
        BigDecimal yesterdayRevenue = dailyChargeRepository.sumRevenueByPropertyAndDate(propertyId, yesterday);
        BigDecimal yesterdayDayUseRevenue = dailyChargeRepository.sumDayUseRevenueByPropertyAndDate(propertyId, yesterday);
        BigDecimal yesterdayOvernightRevenue = yesterdayRevenue.subtract(yesterdayDayUseRevenue);

        BigDecimal yesterdayOcc = BigDecimal.ZERO;
        BigDecimal yesterdayAdr = BigDecimal.ZERO;
        BigDecimal yesterdayRevPar = BigDecimal.ZERO;

        if (totalRooms > 0) {
            yesterdayOcc = BigDecimal.valueOf(yesterdayOvernightSold)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalRooms), 1, RoundingMode.HALF_UP);
            yesterdayRevPar = yesterdayOvernightRevenue.divide(BigDecimal.valueOf(totalRooms), 0, RoundingMode.HALF_UP);
        }
        if (yesterdayOvernightSold > 0) {
            yesterdayAdr = yesterdayOvernightRevenue.divide(BigDecimal.valueOf(yesterdayOvernightSold), 0, RoundingMode.HALF_UP);
        }

        Long hotelId = property != null ? property.getHotel().getId() : null;

        return DashboardPropertyKpiResponse.builder()
                .propertyId(propertyId)
                .hotelId(hotelId)
                .propertyName(propertyName)
                .totalRooms(totalRooms)
                .soldRooms(soldRooms)
                .dayUseRooms(dayUseRooms)
                .dayUseRevenue(dayUseRevenue)
                .totalRevenue(totalRevenue)
                .occupancyRate(occupancyRate)
                .adr(adr)
                .revPar(revPar)
                .yesterdayOccupancyRate(yesterdayOcc)
                .yesterdayAdr(yesterdayAdr)
                .yesterdayRevPar(yesterdayRevPar)
                .yesterdayRevenue(yesterdayRevenue)
                .build();
    }

    @Override
    public DashboardOperationResponse getOperation(Long propertyId) {
        LocalDate today = LocalDate.now();

        long arrivals = subReservationRepository.countArrivals(propertyId, today);
        long inHouse = subReservationRepository.countInHouse(propertyId, today);
        long departures = subReservationRepository.countDepartures(propertyId, today);
        long checkedInToday = subReservationRepository.countCheckedInToday(propertyId, today);
        long checkedOutToday = subReservationRepository.countCheckedOutToday(propertyId, today);

        return DashboardOperationResponse.builder()
                .arrivals(arrivals)
                .inHouse(inHouse)
                .departures(departures)
                .checkedInToday(checkedInToday)
                .checkedOutToday(checkedOutToday)
                .build();
    }

    @Override
    public DashboardPickupResponse getPickup(Long propertyId) {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(6);

        // 7일간 매출을 날짜 범위 한 번의 쿼리로 조회 (14 쿼리 → 1 쿼리)
        Map<LocalDate, BigDecimal> revenueMap = new HashMap<>();
        List<Object[]> revenueRows = dailyChargeRepository.sumRevenueByPropertyAndDateRange(
                propertyId, today, endDate);
        for (Object[] row : revenueRows) {
            LocalDate date = (LocalDate) row[0];
            BigDecimal revenue = (BigDecimal) row[1];
            revenueMap.put(date, revenue);
        }

        // 체류 예약 수는 날짜별 겹침 조건이라 단순 GROUP BY 불가 — 기존 쿼리 재사용하되 에러 핸들링 유지
        List<DashboardPickupResponse.DailyPickup> dailyPickups = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = today.plusDays(i);
            long count = 0;
            try {
                count = subReservationRepository.countOccupiedByDate(propertyId, date);
            } catch (Exception e) {
                // 쿼리 실패 시 0으로 처리
            }
            BigDecimal revenue = revenueMap.getOrDefault(date, BigDecimal.ZERO);

            dailyPickups.add(DashboardPickupResponse.DailyPickup.builder()
                    .date(date)
                    .reservationCount(count)
                    .revenue(revenue)
                    .build());
        }

        return DashboardPickupResponse.builder()
                .dailyPickups(dailyPickups)
                .build();
    }

    @Override
    public List<DashboardPropertyKpiResponse> getAllPropertyKpis() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        List<Property> properties = propertyRepository.findAllByUseYnTrue();
        if (properties.isEmpty()) {
            return List.of();
        }

        // 벌크 조회: 객실 수, 판매객실 수, Dayuse 수, 매출, Dayuse 매출 (오늘/어제 각각)
        // 기존: 프로퍼티당 10개 쿼리 × N개 = 10N+1 → 개선: 8개 쿼리 (프로퍼티 수 무관)
        Map<Long, Long> totalRoomsMap = toMapLong(roomNumberRepository.countByPropertyIdBulk());

        Map<Long, Long> soldRoomsMap = toMapLong(subReservationRepository.countSoldRoomsBulk(today));
        Map<Long, Long> dayUseRoomsMap = toMapLong(subReservationRepository.countDayUseRoomsBulk(today));
        Map<Long, BigDecimal> revenueMap = toMapBigDecimal(dailyChargeRepository.sumRevenueBulkByDate(today));
        Map<Long, BigDecimal> dayUseRevenueMap = toMapBigDecimal(dailyChargeRepository.sumDayUseRevenueBulkByDate(today));

        Map<Long, Long> yesterdaySoldMap = toMapLong(subReservationRepository.countSoldRoomsBulk(yesterday));
        Map<Long, Long> yesterdayDayUseMap = toMapLong(subReservationRepository.countDayUseRoomsBulk(yesterday));
        Map<Long, BigDecimal> yesterdayRevenueMap = toMapBigDecimal(dailyChargeRepository.sumRevenueBulkByDate(yesterday));
        Map<Long, BigDecimal> yesterdayDayUseRevenueMap = toMapBigDecimal(dailyChargeRepository.sumDayUseRevenueBulkByDate(yesterday));

        List<DashboardPropertyKpiResponse> result = new ArrayList<>();

        for (Property property : properties) {
            Long propertyId = property.getId();
            long totalRooms = totalRoomsMap.getOrDefault(propertyId, 0L);

            // 오늘 KPI
            long soldRooms = soldRoomsMap.getOrDefault(propertyId, 0L);
            long dayUseRooms = dayUseRoomsMap.getOrDefault(propertyId, 0L);
            long overnightSold = soldRooms - dayUseRooms;
            BigDecimal totalRevenue = revenueMap.getOrDefault(propertyId, BigDecimal.ZERO);
            BigDecimal dayUseRevenue = dayUseRevenueMap.getOrDefault(propertyId, BigDecimal.ZERO);
            BigDecimal overnightRevenue = totalRevenue.subtract(dayUseRevenue);

            BigDecimal occupancyRate = BigDecimal.ZERO;
            BigDecimal adr = BigDecimal.ZERO;
            BigDecimal revPar = BigDecimal.ZERO;

            if (totalRooms > 0) {
                occupancyRate = BigDecimal.valueOf(overnightSold)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalRooms), 1, RoundingMode.HALF_UP);
                revPar = overnightRevenue.divide(BigDecimal.valueOf(totalRooms), 0, RoundingMode.HALF_UP);
            }
            if (overnightSold > 0) {
                adr = overnightRevenue.divide(BigDecimal.valueOf(overnightSold), 0, RoundingMode.HALF_UP);
            }

            // 어제 KPI
            long yesterdaySoldAll = yesterdaySoldMap.getOrDefault(propertyId, 0L);
            long yesterdayDayUse = yesterdayDayUseMap.getOrDefault(propertyId, 0L);
            long yesterdayOvernightSold = yesterdaySoldAll - yesterdayDayUse;
            BigDecimal yesterdayRevenue = yesterdayRevenueMap.getOrDefault(propertyId, BigDecimal.ZERO);
            BigDecimal yesterdayDayUseRevenue = yesterdayDayUseRevenueMap.getOrDefault(propertyId, BigDecimal.ZERO);
            BigDecimal yesterdayOvernightRevenue = yesterdayRevenue.subtract(yesterdayDayUseRevenue);

            BigDecimal yesterdayOcc = BigDecimal.ZERO;
            BigDecimal yesterdayAdr = BigDecimal.ZERO;
            BigDecimal yesterdayRevPar = BigDecimal.ZERO;

            if (totalRooms > 0) {
                yesterdayOcc = BigDecimal.valueOf(yesterdayOvernightSold)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalRooms), 1, RoundingMode.HALF_UP);
                yesterdayRevPar = yesterdayOvernightRevenue.divide(BigDecimal.valueOf(totalRooms), 0, RoundingMode.HALF_UP);
            }
            if (yesterdayOvernightSold > 0) {
                yesterdayAdr = yesterdayOvernightRevenue.divide(BigDecimal.valueOf(yesterdayOvernightSold), 0, RoundingMode.HALF_UP);
            }

            Long hotelId = property.getHotel() != null ? property.getHotel().getId() : null;

            result.add(DashboardPropertyKpiResponse.builder()
                    .propertyId(propertyId)
                    .hotelId(hotelId)
                    .propertyName(property.getPropertyName())
                    .totalRooms(totalRooms)
                    .soldRooms(soldRooms)
                    .dayUseRooms(dayUseRooms)
                    .dayUseRevenue(dayUseRevenue)
                    .totalRevenue(totalRevenue)
                    .occupancyRate(occupancyRate)
                    .adr(adr)
                    .revPar(revPar)
                    .yesterdayOccupancyRate(yesterdayOcc)
                    .yesterdayAdr(yesterdayAdr)
                    .yesterdayRevPar(yesterdayRevPar)
                    .yesterdayRevenue(yesterdayRevenue)
                    .build());
        }

        // OCC% 내림차순 정렬
        result.sort((a, b) -> b.getOccupancyRate().compareTo(a.getOccupancyRate()));
        return result;
    }

    // ─── private helpers ──────────────────────────

    /**
     * Object[] {Long propertyId, Long count} 리스트 → Map 변환
     */
    private Map<Long, Long> toMapLong(List<Object[]> rows) {
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            Long propertyId = (Long) row[0];
            Long count = (Long) row[1];
            map.put(propertyId, count);
        }
        return map;
    }

    /**
     * Object[] {Long propertyId, BigDecimal amount} 리스트 → Map 변환
     */
    private Map<Long, BigDecimal> toMapBigDecimal(List<Object[]> rows) {
        Map<Long, BigDecimal> map = new HashMap<>();
        for (Object[] row : rows) {
            Long propertyId = (Long) row[0];
            BigDecimal amount = (BigDecimal) row[1];
            map.put(propertyId, amount);
        }
        return map;
    }
}
