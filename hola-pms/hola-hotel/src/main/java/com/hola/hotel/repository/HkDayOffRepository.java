package com.hola.hotel.repository;

import com.hola.hotel.entity.HkDayOff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HkDayOffRepository extends JpaRepository<HkDayOff, Long> {

    /** 월별 휴무일 조회 (전체 하우스키퍼) */
    List<HkDayOff> findByPropertyIdAndDayOffDateBetween(Long propertyId, LocalDate start, LocalDate end);

    /** 특정 하우스키퍼의 월별 휴무일 */
    List<HkDayOff> findByPropertyIdAndHousekeeperIdAndDayOffDateBetween(
            Long propertyId, Long housekeeperId, LocalDate start, LocalDate end);

    /** 특정 날짜에 해당 하우스키퍼의 휴무일 존재 여부 */
    Optional<HkDayOff> findByPropertyIdAndHousekeeperIdAndDayOffDate(
            Long propertyId, Long housekeeperId, LocalDate date);

    /** 특정 날짜에 승인된 휴무일 목록 */
    List<HkDayOff> findByPropertyIdAndDayOffDateAndStatus(
            Long propertyId, LocalDate date, String status);
}
