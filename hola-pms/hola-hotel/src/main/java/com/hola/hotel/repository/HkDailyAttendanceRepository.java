package com.hola.hotel.repository;

import com.hola.hotel.entity.HkDailyAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HkDailyAttendanceRepository extends JpaRepository<HkDailyAttendance, Long> {

    List<HkDailyAttendance> findByPropertyIdAndAttendanceDate(Long propertyId, LocalDate date);

    Optional<HkDailyAttendance> findByPropertyIdAndAttendanceDateAndHousekeeperId(
            Long propertyId, LocalDate date, Long housekeeperId);

    List<HkDailyAttendance> findByPropertyIdAndAttendanceDateBetween(
            Long propertyId, LocalDate startDate, LocalDate endDate);

    /** 특정 날짜에 가용한 하우스키퍼 ID 목록 */
    @Query("SELECT a.housekeeperId FROM HkDailyAttendance a " +
           "WHERE a.propertyId = :propertyId AND a.attendanceDate = :date AND a.isAvailable = true")
    List<Long> findAvailableHousekeeperIds(Long propertyId, LocalDate date);

    boolean existsByPropertyIdAndAttendanceDate(Long propertyId, LocalDate date);
}
