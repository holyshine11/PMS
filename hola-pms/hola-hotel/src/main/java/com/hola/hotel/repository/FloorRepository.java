package com.hola.hotel.repository;

import com.hola.hotel.entity.Floor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FloorRepository extends JpaRepository<Floor, Long> {

    List<Floor> findAllByPropertyIdOrderBySortOrderAscFloorNumberAsc(Long propertyId);

    boolean existsByPropertyIdAndFloorNumber(Long propertyId, String floorNumber);

    boolean existsByPropertyIdAndFloorNumberAndIdNot(Long propertyId, String floorNumber, Long id);

    long countByPropertyId(Long propertyId);

    /** 해당 층에 매핑된 호수가 존재하는지 확인 (객실타입-층-호수 매핑 테이블 참조) */
    @Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END " +
                   "FROM rm_room_type_floor rtf WHERE rtf.floor_id = :floorId",
           nativeQuery = true)
    boolean existsRoomMappingByFloorId(@Param("floorId") Long floorId);
}
