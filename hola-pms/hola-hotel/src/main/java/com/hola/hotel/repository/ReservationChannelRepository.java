package com.hola.hotel.repository;

import com.hola.hotel.entity.ReservationChannel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReservationChannelRepository extends JpaRepository<ReservationChannel, Long> {
    List<ReservationChannel> findByPropertyIdOrderBySortOrderAsc(Long propertyId);
    Optional<ReservationChannel> findByPropertyIdAndChannelCode(Long propertyId, String channelCode);
    boolean existsByPropertyIdAndChannelCode(Long propertyId, String channelCode);
}
