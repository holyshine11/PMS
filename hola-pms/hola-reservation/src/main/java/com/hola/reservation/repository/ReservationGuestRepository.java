package com.hola.reservation.repository;

import com.hola.reservation.entity.ReservationGuest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 동반 투숙객 Repository
 */
public interface ReservationGuestRepository extends JpaRepository<ReservationGuest, Long> {

    List<ReservationGuest> findBySubReservationId(Long subReservationId);

    @Modifying
    @Query("DELETE FROM ReservationGuest g WHERE g.subReservation.id = :subReservationId")
    void deleteAllBySubReservationId(@Param("subReservationId") Long subReservationId);
}
