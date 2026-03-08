package com.hola.reservation.repository;

import com.hola.reservation.entity.ReservationMemo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 예약 메모 Repository
 */
public interface ReservationMemoRepository extends JpaRepository<ReservationMemo, Long> {

    List<ReservationMemo> findByMasterReservationIdOrderByCreatedAtDesc(Long masterReservationId);
}
