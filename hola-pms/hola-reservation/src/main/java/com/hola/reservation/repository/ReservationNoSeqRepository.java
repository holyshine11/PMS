package com.hola.reservation.repository;

import com.hola.reservation.entity.ReservationNoSeq;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.time.LocalDate;
import java.util.Optional;

/**
 * 예약번호 시퀀스 Repository
 */
public interface ReservationNoSeqRepository extends JpaRepository<ReservationNoSeq, Long> {

    /** 비관적 락으로 시퀀스 조회 (동시 예약번호 생성 시 중복 방지) */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ReservationNoSeq> findByPropertyIdAndSeqDate(Long propertyId, LocalDate seqDate);
}
