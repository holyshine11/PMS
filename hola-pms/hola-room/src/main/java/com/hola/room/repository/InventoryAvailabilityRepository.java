package com.hola.room.repository;

import com.hola.room.entity.InventoryAvailability;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface InventoryAvailabilityRepository extends JpaRepository<InventoryAvailability, Long> {

    List<InventoryAvailability> findAllByInventoryItemIdAndAvailabilityDateBetweenOrderByAvailabilityDateAsc(
            Long inventoryItemId, LocalDate from, LocalDate to);

    /** 비관적 락 — 동시 예약 시 재고 충돌 방지 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM InventoryAvailability a WHERE a.inventoryItemId = :itemId " +
           "AND a.availabilityDate >= :from AND a.availabilityDate < :to ORDER BY a.availabilityDate")
    List<InventoryAvailability> findByItemIdAndDateRangeForUpdate(
            @Param("itemId") Long inventoryItemId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("SELECT a FROM InventoryAvailability a WHERE a.inventoryItemId = :itemId " +
           "AND a.availabilityDate >= :from AND a.availabilityDate < :to ORDER BY a.availabilityDate")
    List<InventoryAvailability> findByItemIdAndDateRange(
            @Param("itemId") Long inventoryItemId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
