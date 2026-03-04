package com.hola.hotel.repository;

import com.hola.hotel.entity.Property;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PropertyRepository extends JpaRepository<Property, Long> {

    List<Property> findAllByHotelId(Long hotelId);

    Page<Property> findAllByHotelId(Long hotelId, Pageable pageable);

    boolean existsByHotelIdAndPropertyCode(Long hotelId, String propertyCode);

    long countByHotelId(Long hotelId);

    long countByDeletedAtIsNull();

    List<Property> findAllByHotelIdAndUseYnTrueOrderBySortOrderAscPropertyNameAsc(Long hotelId);

    boolean existsByHotelIdAndPropertyName(Long hotelId, String propertyName);

    long countByHotelIdAndPropertyCodeStartingWith(Long hotelId, String propertyCodePrefix);
}
