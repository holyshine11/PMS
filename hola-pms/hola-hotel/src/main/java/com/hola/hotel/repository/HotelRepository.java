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

    @Query("SELECT h FROM Hotel h WHERE " +
           "(:hotelName IS NULL OR h.hotelName LIKE %:hotelName%) " +
           "AND (:useYn IS NULL OR h.useYn = :useYn)")
    Page<Hotel> findAllByHotelNameAndUseYn(@Param("hotelName") String hotelName,
                                            @Param("useYn") Boolean useYn,
                                            Pageable pageable);

    boolean existsByHotelNameAndDeletedAtIsNull(String hotelName);

    boolean existsByHotelNameAndDeletedAtIsNullAndIdNot(String hotelName, Long id);

    long countByDeletedAtIsNull();

    @Query(value = "SELECT nextval('htl_hotel_code_seq')", nativeQuery = true)
    Long getNextHotelCodeSequence();

    List<Hotel> findAllByUseYnTrueOrderBySortOrderAscHotelNameAsc();
}
