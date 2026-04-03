package com.hola.hotel.repository;

import com.hola.hotel.entity.RoomNumber;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoomNumberRepository extends JpaRepository<RoomNumber, Long> {

    List<RoomNumber> findAllByPropertyIdOrderBySortOrderAscRoomNumberAsc(Long propertyId);

    Page<RoomNumber> findAllByPropertyIdOrderBySortOrderAscRoomNumberAsc(Long propertyId, Pageable pageable);

    boolean existsByPropertyIdAndRoomNumber(Long propertyId, String roomNumber);

    long countByPropertyId(Long propertyId);

    /**
     * 전체 프로퍼티 객실 수 벌크 조회 (GROUP BY propertyId)
     * 반환: Object[] = {propertyId (Long), count (Long)}
     */
    @Query("SELECT r.property.id, COUNT(r) FROM RoomNumber r " +
           "GROUP BY r.property.id")
    List<Object[]> countByPropertyIdBulk();

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

    /**
     * VD(빈방+청소필요) 객실 목록 - 하우스키핑 일일 작업 생성용
     */
    @Query("SELECT r FROM RoomNumber r WHERE r.property.id = :propertyId " +
           "AND r.foStatus = 'VACANT' AND r.hkStatus = 'DIRTY' " +
           "ORDER BY r.roomNumber ASC")
    List<RoomNumber> findVacantDirtyRooms(@Param("propertyId") Long propertyId);

    /**
     * OD(투숙중+청소필요) 객실 목록 - 스테이오버 작업 생성용
     */
    @Query("SELECT r FROM RoomNumber r WHERE r.property.id = :propertyId " +
           "AND r.foStatus = 'OCCUPIED' AND r.hkStatus = 'DIRTY' " +
           "ORDER BY r.roomNumber ASC")
    List<RoomNumber> findOccupiedDirtyRooms(@Param("propertyId") Long propertyId);

    // === 하우스키핑 구역 배정용: 객실→층 매핑 ===

    /** 특정 객실의 층 ID 조회 (rm_room_type_floor 테이블 활용) */
    @Query(value = "SELECT DISTINCT rtf.floor_id FROM rm_room_type_floor rtf " +
                   "WHERE rtf.room_number_id = :roomNumberId LIMIT 1",
           nativeQuery = true)
    Long findFloorIdByRoomNumberId(@Param("roomNumberId") Long roomNumberId);

    /** OC(투숙중+청소완료) 객실 목록 - OD 전환용 */
    @Query("SELECT r FROM RoomNumber r WHERE r.property.id = :propertyId " +
           "AND r.foStatus = 'OCCUPIED' AND r.hkStatus = 'CLEAN' " +
           "ORDER BY r.roomNumber ASC")
    List<RoomNumber> findOccupiedCleanRooms(@Param("propertyId") Long propertyId);

    /** OD 객실 + roomTypeId 조회 (스테이오버 작업 생성용) */
    @Query(value =
        "SELECT rn.id AS room_number_id, " +
        "       (SELECT rtf.room_type_id FROM rm_room_type_floor rtf " +
        "        WHERE rtf.room_number_id = rn.id LIMIT 1) AS room_type_id " +
        "FROM htl_room_number rn " +
        "WHERE rn.property_id = :propertyId " +
        "  AND rn.fo_status = 'OCCUPIED' AND rn.hk_status = 'DIRTY' " +
        "  AND rn.deleted_at IS NULL " +
        "ORDER BY rn.room_number",
        nativeQuery = true)
    List<Object[]> findOccupiedDirtyRoomsWithRoomTypeId(@Param("propertyId") Long propertyId);

    /** 특정 객실의 룸타입 ID 조회 */
    @Query(value = "SELECT rtf.room_type_id FROM rm_room_type_floor rtf " +
           "WHERE rtf.room_number_id = :roomNumberId LIMIT 1",
           nativeQuery = true)
    Long findRoomTypeIdByRoomNumberId(@Param("roomNumberId") Long roomNumberId);

    /** 프로퍼티별 룸타입 기본 정보 조회 (크로스 모듈 네이티브 쿼리) */
    @Query(value = "SELECT rt.id, rt.room_type_code, rt.description " +
           "FROM rm_room_type rt " +
           "WHERE rt.property_id = :propertyId AND rt.deleted_at IS NULL " +
           "ORDER BY rt.sort_order, rt.room_type_code",
           nativeQuery = true)
    List<Object[]> findRoomTypesByPropertyId(@Param("propertyId") Long propertyId);
}
