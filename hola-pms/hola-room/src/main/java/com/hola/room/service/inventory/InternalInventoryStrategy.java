package com.hola.room.service.inventory;

import com.hola.room.dto.response.InventoryAvailabilityResponse;
import com.hola.room.entity.InventoryAvailability;
import com.hola.room.repository.InventoryAvailabilityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 자체 재고 관리 구현 (INTERNAL)
 * rm_inventory_availability 테이블 기반, 비관적 락으로 동시성 제어
 */
@Slf4j
@Component("internalInventoryStrategy")
@RequiredArgsConstructor
public class InternalInventoryStrategy implements InventoryManagementStrategy {

    private final InventoryAvailabilityRepository availabilityRepository;

    @Override
    public int getAvailableCount(Long inventoryItemId, LocalDate date) {
        List<InventoryAvailability> list = availabilityRepository
                .findAllByInventoryItemIdAndAvailabilityDateBetweenOrderByAvailabilityDateAsc(
                        inventoryItemId, date, date);
        if (list.isEmpty()) return 0;
        return list.get(0).getRemainingCount();
    }

    @Override
    @Transactional
    public boolean reserve(Long inventoryItemId, LocalDate fromDate, LocalDate toDate, int quantity) {
        // 비관적 락으로 조회
        List<InventoryAvailability> availabilities =
                availabilityRepository.findByItemIdAndDateRangeForUpdate(inventoryItemId, fromDate, toDate);

        // 요청 기간의 모든 일자에 가용성 레코드가 존재하는지 확인
        long expectedDays = ChronoUnit.DAYS.between(fromDate, toDate);
        if (availabilities.size() < expectedDays) {
            log.warn("가용성 미설정 날짜 존재: itemId={}, expected={}, found={}",
                    inventoryItemId, expectedDays, availabilities.size());
            return false;
        }

        // 모든 일자에 충분한 재고 확인
        for (InventoryAvailability avail : availabilities) {
            if (avail.getRemainingCount() < quantity) {
                log.warn("재고 부족: itemId={}, date={}, remaining={}, requested={}",
                        inventoryItemId, avail.getAvailabilityDate(), avail.getRemainingCount(), quantity);
                return false;
            }
        }

        // 차감
        for (InventoryAvailability avail : availabilities) {
            avail.reserve(quantity);
        }

        log.info("재고 차감: itemId={}, from={}, to={}, qty={}", inventoryItemId, fromDate, toDate, quantity);
        return true;
    }

    @Override
    @Transactional
    public void release(Long inventoryItemId, LocalDate fromDate, LocalDate toDate, int quantity) {
        List<InventoryAvailability> availabilities =
                availabilityRepository.findByItemIdAndDateRange(inventoryItemId, fromDate, toDate);

        for (InventoryAvailability avail : availabilities) {
            avail.release(quantity);
        }

        log.info("재고 복원: itemId={}, from={}, to={}, qty={}", inventoryItemId, fromDate, toDate, quantity);
    }

    @Override
    public List<InventoryAvailabilityResponse> getAvailability(Long inventoryItemId, LocalDate from, LocalDate to) {
        return availabilityRepository
                .findAllByInventoryItemIdAndAvailabilityDateBetweenOrderByAvailabilityDateAsc(
                        inventoryItemId, from, to)
                .stream()
                .map(a -> InventoryAvailabilityResponse.builder()
                        .id(a.getId())
                        .availabilityDate(a.getAvailabilityDate())
                        .availableCount(a.getAvailableCount())
                        .reservedCount(a.getReservedCount())
                        .remainingCount(a.getRemainingCount())
                        .build())
                .toList();
    }
}
