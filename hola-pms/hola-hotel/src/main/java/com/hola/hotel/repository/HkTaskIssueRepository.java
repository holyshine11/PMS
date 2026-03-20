package com.hola.hotel.repository;

import com.hola.hotel.entity.HkTaskIssue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 하우스키핑 작업 이슈/메모 Repository
 */
public interface HkTaskIssueRepository extends JpaRepository<HkTaskIssue, Long> {

    List<HkTaskIssue> findByTaskIdOrderByCreatedAtDesc(Long taskId);

    List<HkTaskIssue> findByPropertyIdAndResolvedFalseOrderByCreatedAtDesc(Long propertyId);

    List<HkTaskIssue> findByRoomNumberIdOrderByCreatedAtDesc(Long roomNumberId);
}
