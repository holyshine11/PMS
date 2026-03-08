package com.hola.reservation.repository;

import com.hola.reservation.entity.DailyCharge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 일별 요금 Repository
 */
public interface DailyChargeRepository extends JpaRepository<DailyCharge, Long> {

    List<DailyCharge> findBySubReservationId(Long subReservationId);

    @Modifying
    @Query("DELETE FROM DailyCharge d WHERE d.subReservation.id = :subReservationId")
    void deleteAllBySubReservationId(@Param("subReservationId") Long subReservationId);
}
