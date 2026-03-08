package com.hola.reservation.repository;

import com.hola.reservation.entity.ReservationServiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 예약 서비스 항목 Repository
 */
public interface ReservationServiceItemRepository extends JpaRepository<ReservationServiceItem, Long> {

    List<ReservationServiceItem> findBySubReservationId(Long subReservationId);

    @Modifying
    @Query("DELETE FROM ReservationServiceItem s WHERE s.subReservation.id = :subReservationId")
    void deleteAllBySubReservationId(@Param("subReservationId") Long subReservationId);
}
