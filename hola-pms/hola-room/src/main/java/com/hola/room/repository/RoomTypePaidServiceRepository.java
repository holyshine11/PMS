package com.hola.room.repository;

import com.hola.room.entity.RoomTypePaidService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoomTypePaidServiceRepository extends JpaRepository<RoomTypePaidService, Long> {

    List<RoomTypePaidService> findAllByRoomTypeId(Long roomTypeId);

    @Modifying
    @Query("DELETE FROM RoomTypePaidService p WHERE p.roomTypeId = :roomTypeId")
    void deleteAllByRoomTypeId(@Param("roomTypeId") Long roomTypeId);
}
