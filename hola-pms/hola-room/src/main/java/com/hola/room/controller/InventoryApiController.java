package com.hola.room.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.common.security.AccessControlService;
import com.hola.room.dto.request.InventoryAvailabilityBulkRequest;
import com.hola.room.dto.request.InventoryItemCreateRequest;
import com.hola.room.dto.request.InventoryItemUpdateRequest;
import com.hola.room.dto.response.InventoryAvailabilityResponse;
import com.hola.room.dto.response.InventoryItemResponse;
import com.hola.room.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "재고 관리", description = "재고 아이템 및 가용성 관리 API")
@RestController
@RequestMapping("/api/v1/properties/{propertyId}/inventory-items")
@RequiredArgsConstructor
public class InventoryApiController {

    private final AccessControlService accessControlService;
    private final InventoryService inventoryService;

    @Operation(summary = "아이템 목록 조회")
    @GetMapping
    public ResponseEntity<HolaResponse<List<InventoryItemResponse>>> getItems(
            @PathVariable Long propertyId,
            @RequestParam(required = false) String managementType) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(inventoryService.getInventoryItems(propertyId, managementType)));
    }

    @Operation(summary = "아이템 상세 조회")
    @GetMapping("/{id}")
    public ResponseEntity<HolaResponse<InventoryItemResponse>> getItem(
            @PathVariable Long propertyId,
            @PathVariable Long id) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(inventoryService.getInventoryItem(id)));
    }

    @Operation(summary = "아이템 등록")
    @PostMapping
    public ResponseEntity<HolaResponse<InventoryItemResponse>> createItem(
            @PathVariable Long propertyId,
            @Valid @RequestBody InventoryItemCreateRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(HolaResponse.success(inventoryService.createInventoryItem(propertyId, request)));
    }

    @Operation(summary = "아이템 수정")
    @PutMapping("/{id}")
    public ResponseEntity<HolaResponse<InventoryItemResponse>> updateItem(
            @PathVariable Long propertyId,
            @PathVariable Long id,
            @Valid @RequestBody InventoryItemUpdateRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(inventoryService.updateInventoryItem(id, request)));
    }

    @Operation(summary = "아이템 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<HolaResponse<Void>> deleteItem(
            @PathVariable Long propertyId,
            @PathVariable Long id) {
        accessControlService.validatePropertyAccess(propertyId);
        inventoryService.deleteInventoryItem(id);
        return ResponseEntity.ok(HolaResponse.success());
    }

    @Operation(summary = "일자별 가용성 조회")
    @GetMapping("/{id}/availability")
    public ResponseEntity<HolaResponse<List<InventoryAvailabilityResponse>>> getAvailability(
            @PathVariable Long propertyId,
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(inventoryService.getAvailability(id, from, to)));
    }

    @Operation(summary = "가용수량 벌크 설정", description = "기간 내 모든 일자에 동일한 가용 수량 설정")
    @PutMapping("/{id}/availability")
    public ResponseEntity<HolaResponse<Void>> bulkSetAvailability(
            @PathVariable Long propertyId,
            @PathVariable Long id,
            @Valid @RequestBody InventoryAvailabilityBulkRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        inventoryService.bulkSetAvailability(id, request);
        return ResponseEntity.ok(HolaResponse.success());
    }
}
