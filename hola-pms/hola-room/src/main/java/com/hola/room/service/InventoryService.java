package com.hola.room.service;

import com.hola.room.dto.request.InventoryAvailabilityBulkRequest;
import com.hola.room.dto.request.InventoryItemCreateRequest;
import com.hola.room.dto.request.InventoryItemUpdateRequest;
import com.hola.room.dto.response.InventoryAvailabilityResponse;
import com.hola.room.dto.response.InventoryItemResponse;

import java.time.LocalDate;
import java.util.List;

public interface InventoryService {

    // CRUD
    List<InventoryItemResponse> getInventoryItems(Long propertyId, String managementType);
    InventoryItemResponse getInventoryItem(Long id);
    InventoryItemResponse createInventoryItem(Long propertyId, InventoryItemCreateRequest request);
    InventoryItemResponse updateInventoryItem(Long id, InventoryItemUpdateRequest request);
    void deleteInventoryItem(Long id);

    // 가용성
    List<InventoryAvailabilityResponse> getAvailability(Long itemId, LocalDate from, LocalDate to);
    void bulkSetAvailability(Long itemId, InventoryAvailabilityBulkRequest request);

    // 예약 연동
    boolean reserveInventory(Long inventoryItemId, LocalDate from, LocalDate to, int quantity);
    void releaseInventory(Long inventoryItemId, LocalDate from, LocalDate to, int quantity);
}
