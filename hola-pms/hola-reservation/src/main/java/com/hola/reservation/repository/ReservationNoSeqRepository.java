package com.hola.reservation.repository;

import com.hola.reservation.entity.ReservationNoSeq;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

/**
 * 예약번호 시퀀스 Repository
 */
public interface ReservationNoSeqRepository extends JpaRepository<ReservationNoSeq, Long> {

    Optional<ReservationNoSeq> findByPropertyIdAndSeqDate(Long propertyId, LocalDate seqDate);
}
