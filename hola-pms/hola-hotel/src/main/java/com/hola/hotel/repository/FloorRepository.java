package com.hola.hotel.repository;

import com.hola.hotel.entity.Floor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FloorRepository extends JpaRepository<Floor, Long> {

    List<Floor> findAllByPropertyIdOrderBySortOrderAscFloorNumberAsc(Long propertyId);

    boolean existsByPropertyIdAndFloorNumber(Long propertyId, String floorNumber);

    boolean existsByPropertyIdAndFloorNumberAndIdNot(Long propertyId, String floorNumber, Long id);

    long countByPropertyId(Long propertyId);
}
