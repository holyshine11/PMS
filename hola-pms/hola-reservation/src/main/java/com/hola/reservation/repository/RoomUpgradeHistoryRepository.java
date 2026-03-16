package com.hola.reservation.repository;

import com.hola.reservation.entity.RoomUpgradeHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomUpgradeHistoryRepository extends JpaRepository<RoomUpgradeHistory, Long> {

    List<RoomUpgradeHistory> findAllBySubReservationIdOrderByUpgradedAtDesc(Long subReservationId);
}
