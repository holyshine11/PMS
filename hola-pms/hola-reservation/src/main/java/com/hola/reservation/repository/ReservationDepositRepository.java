package com.hola.reservation.repository;

import com.hola.reservation.entity.ReservationDeposit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 예약 보증금 Repository
 */
public interface ReservationDepositRepository extends JpaRepository<ReservationDeposit, Long> {

    List<ReservationDeposit> findByMasterReservationId(Long masterReservationId);
}
