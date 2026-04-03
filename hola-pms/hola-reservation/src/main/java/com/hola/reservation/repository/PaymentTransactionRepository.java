package com.hola.reservation.repository;

import com.hola.reservation.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * 결제 거래 이력 Repository
 */
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    List<PaymentTransaction> findByMasterReservationIdOrderByTransactionSeqAsc(Long masterReservationId);

    /** Leg별 결제 거래 조회 */
    List<PaymentTransaction> findBySubReservationIdOrderByTransactionSeqAsc(Long subReservationId);

    /** 벌크 조회: 여러 마스터 예약의 결제 거래 한 번에 조회 */
    List<PaymentTransaction> findByMasterReservationIdIn(List<Long> masterReservationIds);

    /** VAN 시퀀스 번호 생성용 DB 시퀀스 */
    @Query(value = "SELECT nextval('van_sequence_no_seq')", nativeQuery = true)
    Long getNextVanSequence();
}
