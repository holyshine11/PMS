package com.hola.hotel.repository;

import com.hola.hotel.entity.RoomNumber;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomNumberRepository extends JpaRepository<RoomNumber, Long> {

    List<RoomNumber> findAllByPropertyIdOrderBySortOrderAscRoomNumberAsc(Long propertyId);

    boolean existsByPropertyIdAndRoomNumber(Long propertyId, String roomNumber);

    long countByPropertyId(Long propertyId);
}
