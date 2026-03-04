package com.hola.hotel.repository;

import com.hola.hotel.entity.CancellationFee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CancellationFeeRepository extends JpaRepository<CancellationFee, Long> {

    List<CancellationFee> findAllByPropertyIdOrderBySortOrder(Long propertyId);
}
