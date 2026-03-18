package com.hola.reservation.repository;

import com.hola.reservation.entity.ReservationPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 예약 결제 Repository
 */
public interface ReservationPaymentRepository extends JpaRepository<ReservationPayment, Long> {

    Optional<ReservationPayment> findByMasterReservationId(Long masterReservationId);

    List<ReservationPayment> findAllByMasterReservationIdIn(Set<Long> masterReservationIds);
}
