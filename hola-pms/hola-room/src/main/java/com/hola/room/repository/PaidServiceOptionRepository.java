package com.hola.room.repository;

import com.hola.room.entity.PaidServiceOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 유료 서비스 옵션 Repository
 */
public interface PaidServiceOptionRepository extends JpaRepository<PaidServiceOption, Long> {

    List<PaidServiceOption> findAllByPropertyIdOrderBySortOrderAscServiceNameKoAsc(Long propertyId);

    boolean existsByPropertyIdAndServiceOptionCode(Long propertyId, String serviceOptionCode);

    long countByPropertyId(Long propertyId);
}
