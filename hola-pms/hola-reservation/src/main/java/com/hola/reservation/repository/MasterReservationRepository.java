package com.hola.reservation.repository;

import com.hola.reservation.entity.MasterReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 마스터 예약 Repository
 */
public interface MasterReservationRepository extends JpaRepository<MasterReservation, Long> {

    List<MasterReservation> findByPropertyIdAndReservationStatus(Long propertyId, String reservationStatus);

    Optional<MasterReservation> findByMasterReservationNo(String masterReservationNo);

    Optional<MasterReservation> findByConfirmationNo(String confirmationNo);

    List<MasterReservation> findByPropertyIdOrderByReservationDateDesc(Long propertyId);
}
