package com.hola.hotel.repository;

import com.hola.hotel.entity.Property;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PropertyRepository extends JpaRepository<Property, Long> {

    /**
     * 프로퍼티 코드로 활성 프로퍼티 조회 (부킹엔진 공개 API용)
     */
    Optional<Property> findByPropertyCodeAndUseYnTrue(String propertyCode);

    List<Property> findAllByHotelId(Long hotelId);

    Page<Property> findAllByHotelId(Long hotelId, Pageable pageable);

    boolean existsByHotelIdAndPropertyCode(Long hotelId, String propertyCode);

    long countByHotelId(Long hotelId);

    long countByDeletedAtIsNull();

    List<Property> findAllByHotelIdAndUseYnTrueOrderBySortOrderAscPropertyNameAsc(Long hotelId);

    boolean existsByHotelIdAndPropertyName(Long hotelId, String propertyName);

    long countByHotelIdAndPropertyCodeStartingWith(Long hotelId, String propertyCodePrefix);
}
