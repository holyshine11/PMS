package com.hola.hotel.repository;

import com.hola.hotel.entity.HkTaskLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 하우스키핑 작업 상태 변경 이력 Repository
 */
public interface HkTaskLogRepository extends JpaRepository<HkTaskLog, Long> {

    List<HkTaskLog> findByTaskIdOrderByChangedAtDesc(Long taskId);
}
