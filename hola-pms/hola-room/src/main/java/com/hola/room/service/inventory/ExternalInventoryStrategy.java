package com.hola.room.service.inventory;

import com.hola.room.dto.response.InventoryAvailabilityResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 외부 ERP 연동 재고 관리 구현 (EXTERNAL)
 * SAP 등 외부 시스템에서 재고를 관리하는 경우
 * 현재는 항상 가용으로 반환하고, 외부 시스템에 통보만 수행
 */
@Slf4j
@Component("externalInventoryStrategy")
public class ExternalInventoryStrategy implements InventoryManagementStrategy {

    @Override
    public int getAvailableCount(Long inventoryItemId, LocalDate date) {
        // TODO: 외부 ERP API 호출
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean reserve(Long inventoryItemId, LocalDate fromDate, LocalDate toDate, int quantity) {
        // TODO: 외부 ERP에 예약 통보
        log.info("외부 재고 시스템 예약 통보: itemId={}, from={}, to={}, qty={}",
                inventoryItemId, fromDate, toDate, quantity);
        return true;
    }

    @Override
    public void release(Long inventoryItemId, LocalDate fromDate, LocalDate toDate, int quantity) {
        // TODO: 외부 ERP에 해제 통보
        log.info("외부 재고 시스템 해제 통보: itemId={}, from={}, to={}, qty={}",
                inventoryItemId, fromDate, toDate, quantity);
    }

    @Override
    public List<InventoryAvailabilityResponse> getAvailability(Long inventoryItemId, LocalDate from, LocalDate to) {
        // TODO: 외부 ERP에서 가용성 조회
        return List.of();
    }
}
