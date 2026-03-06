package com.hola.room.repository;

import com.hola.room.entity.RoomClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * 객실 클래스 Repository
 */
public interface RoomClassRepository extends JpaRepository<RoomClass, Long> {

    List<RoomClass> findAllByPropertyIdOrderBySortOrderAscRoomClassNameAsc(Long propertyId);

    boolean existsByPropertyIdAndRoomClassCode(Long propertyId, String roomClassCode);

    boolean existsByPropertyIdAndRoomClassNameAndIdNot(Long propertyId, String roomClassName, Long id);

    long countByPropertyId(Long propertyId);

    @Query(value = "SELECT nextval('rm_room_class_code_seq')", nativeQuery = true)
    Long getNextRoomClassCodeSequence();
}
