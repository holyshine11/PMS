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
import java.util.List;

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
        List<DashboardPickupResponse.DailyPickup> dailyPickups = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            LocalDate date = today.plusDays(i);
            long count = 0;
            BigDecimal revenue = BigDecimal.ZERO;
            try {
                count = subReservationRepository.countOccupiedByDate(propertyId, date);
            } catch (Exception e) {
                // 쿼리 실패 시 0으로 처리
            }
            try {
                revenue = dailyChargeRepository.sumRevenueByPropertyAndDate(propertyId, date);
                if (revenue == null) revenue = BigDecimal.ZERO;
            } catch (Exception e) {
                // 쿼리 실패 시 0으로 처리
            }

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
        List<Property> properties = propertyRepository.findAllByUseYnTrue();
        List<DashboardPropertyKpiResponse> result = new ArrayList<>();

        for (Property property : properties) {
            result.add(getPropertyKpi(property.getId()));
        }

        // OCC% 내림차순 정렬
        result.sort((a, b) -> b.getOccupancyRate().compareTo(a.getOccupancyRate()));
        return result;
    }
}
