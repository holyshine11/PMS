package com.hola.reservation.service;

import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.reservation.entity.MasterReservation;
import com.hola.reservation.entity.SubReservation;
import com.hola.reservation.repository.MasterReservationRepository;
import com.hola.reservation.repository.SubReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 예약 도메인 공유 유틸리티 — 엔티티 조회 + 검증
 */
@Component
@RequiredArgsConstructor
public class ReservationFinder {

    private final MasterReservationRepository masterReservationRepository;
    private final SubReservationRepository subReservationRepository;

    // 수정 불가 상태
    private static final Set<String> IMMUTABLE_STATUSES = Set.of("CHECKED_OUT", "CANCELED", "NO_SHOW");

    /**
     * 마스터 예약 조회 + 프로퍼티 소속 검증
     */
    public MasterReservation findMasterById(Long id, Long propertyId) {
        MasterReservation master = masterReservationRepository.findById(id)
                .orElseThrow(() -> new HolaException(ErrorCode.RESERVATION_NOT_FOUND));
        if (!master.getProperty().getId().equals(propertyId)) {
            throw new HolaException(ErrorCode.RESERVATION_NOT_FOUND);
        }
        return master;
    }

    /**
     * 서브예약 조회 + 마스터 소속 검증
     */
    public SubReservation findSubAndValidateOwnership(Long subId, MasterReservation master) {
        SubReservation sub = subReservationRepository.findById(subId)
                .orElseThrow(() -> new HolaException(ErrorCode.SUB_RESERVATION_NOT_FOUND));
        if (!sub.getMasterReservation().getId().equals(master.getId())) {
            throw new HolaException(ErrorCode.SUB_RESERVATION_MASTER_MISMATCH);
        }
        return sub;
    }

    /**
     * 수정 불가 상태 검증 (CHECKED_OUT, CANCELED, NO_SHOW)
     */
    public void validateModifiable(MasterReservation master) {
        if (IMMUTABLE_STATUSES.contains(master.getReservationStatus())) {
            throw new HolaException(ErrorCode.RESERVATION_MODIFY_NOT_ALLOWED);
        }
    }

    /**
     * 키워드 필터 (예약번호, 예약자명, 전화번호, 확인번호)
     */
    public boolean filterByKeyword(MasterReservation r, String keyword) {
        if (keyword == null || keyword.isBlank()) return true;
        String kw = keyword.toLowerCase();
        return (r.getMasterReservationNo() != null && r.getMasterReservationNo().toLowerCase().contains(kw))
                || (r.getGuestNameKo() != null && r.getGuestNameKo().contains(kw))
                || (r.getPhoneNumber() != null && r.getPhoneNumber().contains(kw))
                || (r.getConfirmationNo() != null && r.getConfirmationNo().toLowerCase().contains(kw));
    }
}
