package com.hola.room.repository;

import com.hola.room.entity.FreeServiceOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 무료 서비스 옵션 Repository
 */
public interface FreeServiceOptionRepository extends JpaRepository<FreeServiceOption, Long> {

    List<FreeServiceOption> findAllByPropertyIdOrderBySortOrderAscServiceNameKoAsc(Long propertyId);

    boolean existsByPropertyIdAndServiceOptionCode(Long propertyId, String serviceOptionCode);

    long countByPropertyId(Long propertyId);
}
