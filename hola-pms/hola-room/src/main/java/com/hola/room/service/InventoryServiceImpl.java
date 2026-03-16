package com.hola.room.service;

import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.room.dto.request.InventoryAvailabilityBulkRequest;
import com.hola.room.dto.request.InventoryItemCreateRequest;
import com.hola.room.dto.request.InventoryItemUpdateRequest;
import com.hola.room.dto.response.InventoryAvailabilityResponse;
import com.hola.room.dto.response.InventoryItemResponse;
import com.hola.room.entity.InventoryAvailability;
import com.hola.room.entity.InventoryItem;
import com.hola.room.repository.InventoryAvailabilityRepository;
import com.hola.room.repository.InventoryItemRepository;
import com.hola.room.service.inventory.ExternalInventoryStrategy;
import com.hola.room.service.inventory.InternalInventoryStrategy;
import com.hola.room.service.inventory.InventoryManagementStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryServiceImpl implements InventoryService {

    private final InventoryItemRepository itemRepository;
    private final InventoryAvailabilityRepository availabilityRepository;
    private final InternalInventoryStrategy internalStrategy;
    private final ExternalInventoryStrategy externalStrategy;

    private InventoryManagementStrategy getStrategy(InventoryItem item) {
        return "EXTERNAL".equals(item.getManagementType()) ? externalStrategy : internalStrategy;
    }

    // ========== CRUD ==========

    @Override
    public List<InventoryItemResponse> getInventoryItems(Long propertyId, String managementType) {
        List<InventoryItem> items;
        if (managementType != null) {
            items = itemRepository.findAllByPropertyIdAndManagementTypeOrderBySortOrderAsc(propertyId, managementType);
        } else {
            items = itemRepository.findAllByPropertyIdOrderBySortOrderAscItemNameKoAsc(propertyId);
        }
        return items.stream().map(this::toResponse).toList();
    }

    @Override
    public InventoryItemResponse getInventoryItem(Long id) {
        return toResponse(findById(id));
    }

    @Override
    @Transactional
    public InventoryItemResponse createInventoryItem(Long propertyId, InventoryItemCreateRequest request) {
        if (itemRepository.existsByPropertyIdAndItemCode(propertyId, request.getItemCode())) {
            throw new HolaException(ErrorCode.INVENTORY_ITEM_CODE_DUPLICATE);
        }

        InventoryItem entity = InventoryItem.builder()
                .propertyId(propertyId)
                .itemCode(request.getItemCode())
                .itemNameKo(request.getItemNameKo())
                .itemNameEn(request.getItemNameEn())
                .itemType(request.getItemType())
                .managementType(request.getManagementType())
                .externalSystemCode(request.getExternalSystemCode())
                .totalQuantity(request.getTotalQuantity() != null ? request.getTotalQuantity() : 0)
                .build();

        InventoryItem saved = itemRepository.save(entity);
        log.info("재고 아이템 생성: {} ({}) - 프로퍼티: {}", saved.getItemNameKo(), saved.getItemCode(), propertyId);

        return toResponse(saved);
    }

    @Override
    @Transactional
    public InventoryItemResponse updateInventoryItem(Long id, InventoryItemUpdateRequest request) {
        InventoryItem entity = findById(id);

        entity.update(request.getItemNameKo(), request.getItemNameEn(), request.getItemType(),
                request.getManagementType(), request.getExternalSystemCode(), request.getTotalQuantity());

        if (request.getUseYn() != null) {
            if (Boolean.TRUE.equals(request.getUseYn())) {
                entity.activate();
            } else {
                entity.deactivate();
            }
        }

        log.info("재고 아이템 수정: {} ({})", entity.getItemNameKo(), entity.getItemCode());
        return toResponse(entity);
    }

    @Override
    @Transactional
    public void deleteInventoryItem(Long id) {
        InventoryItem entity = findById(id);
        if (Boolean.TRUE.equals(entity.getUseYn())) {
            throw new HolaException(ErrorCode.CANNOT_DELETE_ACTIVE);
        }
        entity.softDelete();
        log.info("재고 아이템 삭제: {} ({})", entity.getItemNameKo(), entity.getItemCode());
    }

    // ========== 가용성 ==========

    @Override
    public List<InventoryAvailabilityResponse> getAvailability(Long itemId, LocalDate from, LocalDate to) {
        InventoryItem item = findById(itemId);
        return getStrategy(item).getAvailability(itemId, from, to);
    }

    @Override
    @Transactional
    public void bulkSetAvailability(Long itemId, InventoryAvailabilityBulkRequest request) {
        InventoryItem item = findById(itemId);

        // EXTERNAL은 자체 가용성 설정 불가
        if ("EXTERNAL".equals(item.getManagementType())) {
            log.warn("외부 관리 아이템은 가용성 직접 설정 불가: {}", item.getItemCode());
            return;
        }

        LocalDate date = request.getFromDate();
        while (!date.isAfter(request.getToDate())) {
            final LocalDate currentDate = date;
            List<InventoryAvailability> existing = availabilityRepository
                    .findAllByInventoryItemIdAndAvailabilityDateBetweenOrderByAvailabilityDateAsc(
                            itemId, currentDate, currentDate);

            if (existing.isEmpty()) {
                availabilityRepository.save(InventoryAvailability.builder()
                        .inventoryItemId(itemId)
                        .availabilityDate(currentDate)
                        .availableCount(request.getAvailableCount())
                        .reservedCount(0)
                        .build());
            } else {
                existing.get(0).setAvailableCount(request.getAvailableCount());
            }
            date = date.plusDays(1);
        }

        log.info("재고 가용성 벌크 설정: itemId={}, from={}, to={}, count={}",
                itemId, request.getFromDate(), request.getToDate(), request.getAvailableCount());
    }

    // ========== 예약 연동 ==========

    @Override
    @Transactional
    public boolean reserveInventory(Long inventoryItemId, LocalDate from, LocalDate to, int quantity) {
        InventoryItem item = findById(inventoryItemId);
        return getStrategy(item).reserve(inventoryItemId, from, to, quantity);
    }

    @Override
    @Transactional
    public void releaseInventory(Long inventoryItemId, LocalDate from, LocalDate to, int quantity) {
        InventoryItem item = findById(inventoryItemId);
        getStrategy(item).release(inventoryItemId, from, to, quantity);
    }

    // ========== 내부 헬퍼 ==========

    private InventoryItem findById(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new HolaException(ErrorCode.INVENTORY_ITEM_NOT_FOUND));
    }

    private InventoryItemResponse toResponse(InventoryItem entity) {
        return InventoryItemResponse.builder()
                .id(entity.getId())
                .propertyId(entity.getPropertyId())
                .itemCode(entity.getItemCode())
                .itemNameKo(entity.getItemNameKo())
                .itemNameEn(entity.getItemNameEn())
                .itemType(entity.getItemType())
                .managementType(entity.getManagementType())
                .externalSystemCode(entity.getExternalSystemCode())
                .totalQuantity(entity.getTotalQuantity())
                .sortOrder(entity.getSortOrder())
                .useYn(entity.getUseYn())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
