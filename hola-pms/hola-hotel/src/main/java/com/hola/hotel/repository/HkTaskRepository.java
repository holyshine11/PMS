package com.hola.hotel.repository;

import com.hola.hotel.entity.HkTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 하우스키핑 작업 Repository
 */
public interface HkTaskRepository extends JpaRepository<HkTask, Long> {

    List<HkTask> findByPropertyIdAndTaskDateOrderBySortOrder(Long propertyId, LocalDate taskDate);

    List<HkTask> findByPropertyIdAndTaskDate(Long propertyId, LocalDate taskDate);

    List<HkTask> findByAssignedToAndTaskDate(Long assignedTo, LocalDate taskDate);

    List<HkTask> findByTaskSheetId(Long taskSheetId);

    // 상태별 카운트
    @Query("SELECT t.status, COUNT(t) FROM HkTask t " +
           "WHERE t.propertyId = :propertyId AND t.taskDate = :taskDate " +
           "GROUP BY t.status")
    List<Object[]> countByPropertyIdAndTaskDateGroupByStatus(
            @Param("propertyId") Long propertyId,
            @Param("taskDate") LocalDate taskDate);

    // 하우스키퍼별 집계
    @Query("SELECT t.assignedTo, t.status, COUNT(t), SUM(t.credit) FROM HkTask t " +
           "WHERE t.propertyId = :propertyId AND t.taskDate = :taskDate AND t.assignedTo IS NOT NULL " +
           "GROUP BY t.assignedTo, t.status")
    List<Object[]> countByPropertyIdAndTaskDateGroupByAssignedToAndStatus(
            @Param("propertyId") Long propertyId,
            @Param("taskDate") LocalDate taskDate);

    // 하우스키퍼별 평균 소요시간
    @Query("SELECT t.assignedTo, AVG(t.durationMinutes) FROM HkTask t " +
           "WHERE t.propertyId = :propertyId AND t.taskDate = :taskDate " +
           "AND t.assignedTo IS NOT NULL AND t.durationMinutes IS NOT NULL " +
           "GROUP BY t.assignedTo")
    List<Object[]> avgDurationByPropertyIdAndTaskDateGroupByAssignedTo(
            @Param("propertyId") Long propertyId,
            @Param("taskDate") LocalDate taskDate);

    // 날짜 범위 조회 (이력 조회 최적화)
    List<HkTask> findByPropertyIdAndTaskDateBetweenOrderByTaskDateDescCreatedAtDesc(
            Long propertyId, LocalDate from, LocalDate to);

    // 특정 객실의 오늘 작업 존재 여부
    boolean existsByRoomNumberIdAndTaskDate(Long roomNumberId, LocalDate taskDate);
}
