package com.hola.reservation.service;

import com.hola.hotel.entity.Property;
import com.hola.reservation.entity.ReservationNoSeq;
import com.hola.reservation.repository.MasterReservationRepository;
import com.hola.reservation.repository.ReservationNoSeqRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 예약번호 생성기
 * - 마스터: {propCode}{YYMMDD}-{4자리 일별 시퀀스}  예) GMP260308-0001
 * - 서브:   {masterNo}-{2자리 leg seq}             예) GMP260308-0001-01
 * - 확인번호: 8자리 영숫자 (게스트 안내용)             예) HK4F29XP
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationNumberGenerator {

    private final ReservationNoSeqRepository reservationNoSeqRepository;
    private final MasterReservationRepository masterReservationRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyMMdd");
    private static final int CONFIRMATION_NO_RETRY = 3;

    /**
     * 마스터 예약번호 생성
     * 동시성 보호: PESSIMISTIC_WRITE 락 + INSERT 충돌 시 재조회
     */
    @Transactional
    public String generateMasterReservationNo(Property property) {
        String propCode = property.getPropertyCode();
        LocalDate today = LocalDate.now();
        String dateStr = today.format(DATE_FMT);

        // 비관적 락으로 시퀀스 조회 (행이 있으면 FOR UPDATE 락 획득)
        ReservationNoSeq seq = reservationNoSeqRepository
                .findByPropertyIdAndSeqDate(property.getId(), today)
                .orElseGet(() -> getOrCreateSeq(property.getId(), today));

        int nextSeq = seq.incrementAndGet();
        reservationNoSeqRepository.save(seq);

        String reservationNo = propCode + dateStr + "-" + String.format("%04d", nextSeq);
        log.debug("마스터 예약번호 생성: {}", reservationNo);
        return reservationNo;
    }

    /**
     * 시퀀스 행 생성 (INSERT 충돌 시 재조회로 폴백)
     * 두 트랜잭션이 동시에 새 행 INSERT를 시도하면 UniqueConstraint 위반 발생 → 재조회
     */
    private ReservationNoSeq getOrCreateSeq(Long propertyId, LocalDate seqDate) {
        try {
            return reservationNoSeqRepository.save(
                    ReservationNoSeq.builder()
                            .propertyId(propertyId)
                            .seqDate(seqDate)
                            .lastSeq(0)
                            .build()
            );
        } catch (DataIntegrityViolationException e) {
            // 다른 트랜잭션이 먼저 INSERT → 재조회 (비관적 락 적용)
            log.debug("시퀀스 INSERT 충돌, 재조회: propertyId={}, date={}", propertyId, seqDate);
            return reservationNoSeqRepository.findByPropertyIdAndSeqDate(propertyId, seqDate)
                    .orElseThrow(() -> new IllegalStateException("시퀀스 조회 실패"));
        }
    }

    /**
     * 서브 예약번호 생성
     */
    public String generateSubReservationNo(String masterReservationNo, int legSeq) {
        String subNo = masterReservationNo + "-" + String.format("%02d", legSeq);
        log.debug("서브 예약번호 생성: {}", subNo);
        return subNo;
    }

    /**
     * 확인번호 생성 (8자리 영숫자, 중복 시 재시도)
     */
    public String generateConfirmationNo() {
        for (int i = 0; i < CONFIRMATION_NO_RETRY; i++) {
            String uuid = UUID.randomUUID().toString().replace("-", "").toUpperCase();
            String confirmationNo = uuid.substring(0, 8);
            if (!masterReservationRepository.existsByConfirmationNo(confirmationNo)) {
                return confirmationNo;
            }
            log.warn("확인번호 충돌 발생, 재시도 {}/{}: {}", i + 1, CONFIRMATION_NO_RETRY, confirmationNo);
        }
        // 최종 폴백: 12자리로 확장
        String uuid = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        return uuid.substring(0, 12);
    }
}
