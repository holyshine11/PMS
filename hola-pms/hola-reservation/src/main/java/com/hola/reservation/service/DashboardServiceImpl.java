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
import java.time.LocalDateTime;
import java.time.LocalTime;
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
        Property property = propertyRepository.findById(propertyId).orElse(null);
        String propertyName = property != null ? property.getPropertyName() : "";

        long totalRooms = roomNumberRepository.countByPropertyId(propertyId);
        long soldRooms = subReservationRepository.countSoldRooms(propertyId, today);
        BigDecimal totalRevenue = dailyChargeRepository.sumRevenueByPropertyAndDate(propertyId, today);

        // KPI 계산
        BigDecimal occupancyRate = BigDecimal.ZERO;
        BigDecimal adr = BigDecimal.ZERO;
        BigDecimal revPar = BigDecimal.ZERO;

        if (totalRooms > 0) {
            occupancyRate = BigDecimal.valueOf(soldRooms)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalRooms), 1, RoundingMode.HALF_UP);
            revPar = totalRevenue.divide(BigDecimal.valueOf(totalRooms), 0, RoundingMode.HALF_UP);
        }
        if (soldRooms > 0) {
            adr = totalRevenue.divide(BigDecimal.valueOf(soldRooms), 0, RoundingMode.HALF_UP);
        }

        return DashboardPropertyKpiResponse.builder()
                .propertyId(propertyId)
                .propertyName(propertyName)
                .totalRooms(totalRooms)
                .soldRooms(soldRooms)
                .totalRevenue(totalRevenue)
                .occupancyRate(occupancyRate)
                .adr(adr)
                .revPar(revPar)
                .build();
    }

    @Override
    public DashboardOperationResponse getOperation(Long propertyId) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        long arrivals = subReservationRepository.countArrivals(propertyId, today);
        long inHouse = subReservationRepository.countInHouse(propertyId, today);
        long departures = subReservationRepository.countDepartures(propertyId, today);
        long checkedInToday = subReservationRepository.countCheckedInToday(propertyId, startOfDay, endOfDay);
        long checkedOutToday = subReservationRepository.countCheckedOutToday(propertyId, startOfDay, endOfDay);

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
            long count = subReservationRepository.countOccupiedByDate(propertyId, date);
            BigDecimal revenue = dailyChargeRepository.sumRevenueByPropertyAndDate(propertyId, date);

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
        List<Property> properties = propertyRepository.findAll();
        List<DashboardPropertyKpiResponse> result = new ArrayList<>();

        for (Property property : properties) {
            result.add(getPropertyKpi(property.getId()));
        }

        // OCC% 내림차순 정렬
        result.sort((a, b) -> b.getOccupancyRate().compareTo(a.getOccupancyRate()));
        return result;
    }
}
