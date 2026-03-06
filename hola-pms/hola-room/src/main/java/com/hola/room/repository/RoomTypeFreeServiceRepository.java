package com.hola.room.repository;

import com.hola.room.entity.RoomTypeFreeService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoomTypeFreeServiceRepository extends JpaRepository<RoomTypeFreeService, Long> {

    List<RoomTypeFreeService> findAllByRoomTypeId(Long roomTypeId);

    @Modifying
    @Query("DELETE FROM RoomTypeFreeService f WHERE f.roomTypeId = :roomTypeId")
    void deleteAllByRoomTypeId(@Param("roomTypeId") Long roomTypeId);
}
