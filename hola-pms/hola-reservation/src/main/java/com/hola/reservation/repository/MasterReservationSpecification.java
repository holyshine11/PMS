package com.hola.reservation.repository;

import com.hola.reservation.entity.MasterReservation;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 마스터 예약 Specification 빌더
 * Hibernate 6 + PostgreSQL null 파라미터 타입 추론 이슈를 회피하기 위해
 * JPQL 대신 Specification 방식으로 동적 쿼리를 구성한다.
 */
public class MasterReservationSpecification {

    private MasterReservationSpecification() {
        // 유틸리티 클래스 — 인스턴스 생성 방지
    }

    /**
     * 예약 목록 검색 조건 조합
     *
     * @param propertyId  프로퍼티 ID (필수)
     * @param status      예약 상태 (선택)
     * @param checkInFrom 체크인 시작일 (선택)
     * @param checkInTo   체크인 종료일 (선택)
     * @param keyword     검색 키워드 — 예약번호, 예약자명, 전화번호, 확인번호 (선택)
     * @return Specification
     */
    public static Specification<MasterReservation> search(Long propertyId, String status,
                                                           LocalDate checkInFrom, LocalDate checkInTo,
                                                           String keyword) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 프로퍼티 필수
            predicates.add(cb.equal(root.get("property").get("id"), propertyId));

            // 상태 필터
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("reservationStatus"), status));
            }

            // 체크인 날짜 범위
            if (checkInFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("masterCheckIn"), checkInFrom));
            }
            if (checkInTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("masterCheckIn"), checkInTo));
            }

            // 키워드 검색 (OR 조건: 예약번호, 예약자명, 전화번호, 확인번호)
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.toLowerCase() + "%";
                Predicate reservationNo = cb.like(cb.lower(root.get("masterReservationNo")), pattern);
                Predicate guestName = cb.like(root.get("guestNameKo"), "%" + keyword + "%");
                Predicate phone = cb.like(root.get("phoneNumber"), "%" + keyword + "%");
                Predicate confirmNo = cb.like(cb.lower(root.get("confirmationNo")), pattern);
                predicates.add(cb.or(reservationNo, guestName, phone, confirmNo));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
