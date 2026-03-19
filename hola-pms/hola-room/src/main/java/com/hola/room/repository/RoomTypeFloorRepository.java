package com.hola.room.repository;

import com.hola.room.entity.RoomTypeFloor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 객실 타입 - 층/호수 매핑 Repository
 */
public interface RoomTypeFloorRepository extends JpaRepository<RoomTypeFloor, Long> {

    List<RoomTypeFloor> findAllByRoomTypeId(Long roomTypeId);

    @Modifying
    @Query("DELETE FROM RoomTypeFloor f WHERE f.roomTypeId = :roomTypeId")
    void deleteAllByRoomTypeId(@Param("roomTypeId") Long roomTypeId);

    long countByRoomTypeId(Long roomTypeId);

    /** 특정 층에 매핑된 호수 ID 목록 (중복 제거) */
    @Query("SELECT DISTINCT rtf.roomNumberId FROM RoomTypeFloor rtf WHERE rtf.floorId = :floorId")
    List<Long> findDistinctRoomNumberIdsByFloorId(@Param("floorId") Long floorId);

    /** 여러 객실타입에 매핑된 층/호수 벌크 조회 */
    @Query("SELECT rtf FROM RoomTypeFloor rtf WHERE rtf.roomTypeId IN :roomTypeIds")
    List<RoomTypeFloor> findAllByRoomTypeIdIn(@Param("roomTypeIds") List<Long> roomTypeIds);

    /** 특정 프로퍼티의 호수별 객실타입 매핑 조회 (Room Rack용) */
    @Query("SELECT rtf FROM RoomTypeFloor rtf " +
           "JOIN RoomType rt ON rt.id = rtf.roomTypeId " +
           "WHERE rt.propertyId = :propertyId AND rt.deletedAt IS NULL")
    List<RoomTypeFloor> findAllByPropertyId(@Param("propertyId") Long propertyId);
}
