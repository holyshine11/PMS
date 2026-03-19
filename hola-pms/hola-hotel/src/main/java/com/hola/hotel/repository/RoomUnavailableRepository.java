package com.hola.hotel.repository;

import com.hola.hotel.entity.RoomUnavailable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * OOO/OOS 객실 Repository
 */
public interface RoomUnavailableRepository extends JpaRepository<RoomUnavailable, Long> {

    List<RoomUnavailable> findByPropertyIdOrderByFromDateDesc(Long propertyId);

    List<RoomUnavailable> findByPropertyIdAndUnavailableTypeOrderByFromDateDesc(Long propertyId, String unavailableType);

    /**
     * 현재 유효한 OOO/OOS 목록 (from_date <= today <= through_date)
     */
    @Query("SELECT r FROM RoomUnavailable r WHERE r.propertyId = :propertyId " +
           "AND r.fromDate <= :today AND r.throughDate >= :today " +
           "ORDER BY r.unavailableType, r.fromDate")
    List<RoomUnavailable> findActiveByPropertyId(@Param("propertyId") Long propertyId, @Param("today") LocalDate today);

    /**
     * 특정 객실의 겹치는 기간 OOO/OOS 확인
     */
    @Query("SELECT r FROM RoomUnavailable r WHERE r.roomNumberId = :roomNumberId " +
           "AND r.fromDate <= :throughDate AND r.throughDate >= :fromDate")
    List<RoomUnavailable> findOverlapping(@Param("roomNumberId") Long roomNumberId,
                                           @Param("fromDate") LocalDate fromDate,
                                           @Param("throughDate") LocalDate throughDate);

    /**
     * 특정 객실의 겹치는 기간 OOO/OOS 확인 (특정 ID 제외 - 수정 시 자기 자신 제외용)
     */
    @Query("SELECT r FROM RoomUnavailable r WHERE r.roomNumberId = :roomNumberId " +
           "AND r.fromDate <= :throughDate AND r.throughDate >= :fromDate " +
           "AND r.id != :excludeId")
    List<RoomUnavailable> findOverlappingExclude(@Param("roomNumberId") Long roomNumberId,
                                                  @Param("fromDate") LocalDate fromDate,
                                                  @Param("throughDate") LocalDate throughDate,
                                                  @Param("excludeId") Long excludeId);
}
