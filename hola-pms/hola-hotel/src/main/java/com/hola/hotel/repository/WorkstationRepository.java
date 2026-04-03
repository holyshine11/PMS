package com.hola.hotel.repository;

import com.hola.hotel.entity.Workstation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkstationRepository extends JpaRepository<Workstation, Long> {

    List<Workstation> findByPropertyIdAndStatusAndUseYnTrueOrderBySortOrderAsc(Long propertyId, String status);

    Optional<Workstation> findByIdAndUseYnTrue(Long id);

    default List<Workstation> findActiveByPropertyId(Long propertyId) {
        return findByPropertyIdAndStatusAndUseYnTrueOrderBySortOrderAsc(propertyId, "ACTIVE");
    }
}
