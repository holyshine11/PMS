package com.hola.reservation.repository;

import com.hola.reservation.entity.PaymentAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 결제 조정 Repository
 */
public interface PaymentAdjustmentRepository extends JpaRepository<PaymentAdjustment, Long> {

    List<PaymentAdjustment> findByMasterReservationIdOrderByAdjustmentSeqAsc(Long masterReservationId);
}
