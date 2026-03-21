package com.hola.hotel.repository;

import com.hola.hotel.entity.Hotel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HotelRepository extends JpaRepository<Hotel, Long> {

    boolean existsByHotelCode(String hotelCode);

    Optional<Hotel> findByHotelCode(String hotelCode);

    // JPQL null 파라미터 안티패턴 회피 — 조건별 개별 메서드 분리
    Page<Hotel> findAllBy(Pageable pageable);

    Page<Hotel> findAllByHotelNameContaining(String hotelName, Pageable pageable);

    Page<Hotel> findAllByUseYn(Boolean useYn, Pageable pageable);

    Page<Hotel> findAllByHotelNameContainingAndUseYn(String hotelName, Boolean useYn, Pageable pageable);

    boolean existsByHotelNameAndDeletedAtIsNull(String hotelName);

    boolean existsByHotelNameAndDeletedAtIsNullAndIdNot(String hotelName, Long id);

    long countByDeletedAtIsNull();

    @Query(value = "SELECT nextval('htl_hotel_code_seq')", nativeQuery = true)
    Long getNextHotelCodeSequence();

    List<Hotel> findAllByUseYnTrueOrderBySortOrderAscHotelNameAsc();
}
