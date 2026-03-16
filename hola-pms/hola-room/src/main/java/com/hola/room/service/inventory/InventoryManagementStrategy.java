package com.hola.room.service.inventory;

import com.hola.room.dto.response.InventoryAvailabilityResponse;

import java.time.LocalDate;
import java.util.List;

/**
 * 재고 관리 전략 인터페이스
 * INTERNAL(자체관리) / EXTERNAL(외부 ERP 연동) 구현체로 분리
 */
public interface InventoryManagementStrategy {

    /** 특정 일자 가용 수량 조회 */
    int getAvailableCount(Long inventoryItemId, LocalDate date);

    /** 재고 차감 (예약 시). 실패하면 false 반환 */
    boolean reserve(Long inventoryItemId, LocalDate fromDate, LocalDate toDate, int quantity);

    /** 재고 복원 (취소 시) */
    void release(Long inventoryItemId, LocalDate fromDate, LocalDate toDate, int quantity);

    /** 기간 내 가용성 조회 */
    List<InventoryAvailabilityResponse> getAvailability(Long inventoryItemId, LocalDate from, LocalDate to);
}
