package com.hola.hotel.repository;

import com.hola.hotel.entity.HkCleaningPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 룸타입별 청소 정책 Repository
 */
public interface HkCleaningPolicyRepository extends JpaRepository<HkCleaningPolicy, Long> {

    List<HkCleaningPolicy> findByPropertyIdOrderBySortOrder(Long propertyId);

    Optional<HkCleaningPolicy> findByPropertyIdAndRoomTypeId(Long propertyId, Long roomTypeId);

    boolean existsByPropertyIdAndRoomTypeId(Long propertyId, Long roomTypeId);
}
