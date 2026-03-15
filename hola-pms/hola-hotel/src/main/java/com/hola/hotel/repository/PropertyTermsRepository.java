package com.hola.hotel.repository;

import com.hola.hotel.entity.PropertyTerms;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 프로퍼티 이용약관 Repository
 */
public interface PropertyTermsRepository extends JpaRepository<PropertyTerms, Long> {

    List<PropertyTerms> findAllByPropertyIdOrderBySortOrderAsc(Long propertyId);
}
