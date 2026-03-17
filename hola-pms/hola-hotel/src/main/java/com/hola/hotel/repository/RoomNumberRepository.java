package com.hola.hotel.repository;

import com.hola.hotel.entity.RoomNumber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoomNumberRepository extends JpaRepository<RoomNumber, Long> {

    List<RoomNumber> findAllByPropertyIdOrderBySortOrderAscRoomNumberAsc(Long propertyId);

    boolean existsByPropertyIdAndRoomNumber(Long propertyId, String roomNumber);

    long countByPropertyId(Long propertyId);

    // === 프론트데스크/Room Rack 쿼리 ===

    /**
     * 상태별 객실 수 집계
     */
    long countByPropertyIdAndHkStatusAndFoStatus(Long propertyId, String hkStatus, String foStatus);

    long countByPropertyIdAndHkStatus(Long propertyId, String hkStatus);

    /**
     * 상태별 조회
     */
    List<RoomNumber> findByPropertyIdAndHkStatusOrderByRoomNumberAsc(Long propertyId, String hkStatus);

    List<RoomNumber> findByPropertyIdAndFoStatusOrderByRoomNumberAsc(Long propertyId, String foStatus);

    /**
     * VC(빈방+청소완료) 객실 목록 - 체크인 시 추천용
     */
    @Query("SELECT r FROM RoomNumber r WHERE r.property.id = :propertyId " +
           "AND r.foStatus = 'VACANT' AND r.hkStatus = 'CLEAN' " +
           "ORDER BY r.roomNumber ASC")
    List<RoomNumber> findVacantCleanRooms(@Param("propertyId") Long propertyId);
}
