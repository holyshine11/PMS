package com.hola.hotel.repository;

import com.hola.hotel.entity.HkSection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HkSectionRepository extends JpaRepository<HkSection, Long> {

    List<HkSection> findByPropertyIdOrderBySortOrder(Long propertyId);
}
