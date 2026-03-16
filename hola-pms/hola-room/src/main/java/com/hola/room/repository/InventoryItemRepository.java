package com.hola.room.repository;

import com.hola.room.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    List<InventoryItem> findAllByPropertyIdOrderBySortOrderAscItemNameKoAsc(Long propertyId);

    List<InventoryItem> findAllByPropertyIdAndManagementTypeOrderBySortOrderAsc(Long propertyId, String managementType);

    boolean existsByPropertyIdAndItemCode(Long propertyId, String itemCode);
}
