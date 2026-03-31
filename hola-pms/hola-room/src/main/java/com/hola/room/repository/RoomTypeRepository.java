package com.hola.room.repository;

import com.hola.room.entity.RoomType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 객실 타입 Repository
 */
public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {

    List<RoomType> findAllByPropertyIdOrderBySortOrderAscRoomTypeCodeAsc(Long propertyId);

    Page<RoomType> findAllByPropertyIdOrderBySortOrderAscRoomTypeCodeAsc(Long propertyId, Pageable pageable);

    boolean existsByPropertyIdAndRoomTypeCode(Long propertyId, String roomTypeCode);

    boolean existsByRoomClassId(Long roomClassId);
}
