package com.hola.reservation.repository;

import com.hola.reservation.entity.ReservationChangeLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationChangeLogRepository extends JpaRepository<ReservationChangeLog, Long> {

    List<ReservationChangeLog> findAllByMasterReservationIdOrderByCreatedAtDesc(Long masterReservationId);
}
