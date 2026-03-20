package com.hola.hotel.repository;

import com.hola.hotel.entity.HkTaskSheet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * 하우스키핑 작업 시트 Repository
 */
public interface HkTaskSheetRepository extends JpaRepository<HkTaskSheet, Long> {

    List<HkTaskSheet> findByPropertyIdAndSheetDateOrderBySortOrder(Long propertyId, LocalDate sheetDate);

    List<HkTaskSheet> findByPropertyIdAndSheetDate(Long propertyId, LocalDate sheetDate);
}
