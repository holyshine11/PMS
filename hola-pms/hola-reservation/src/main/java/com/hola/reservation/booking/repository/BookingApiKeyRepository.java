package com.hola.reservation.booking.repository;

import com.hola.reservation.booking.entity.BookingApiKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 부킹엔진 API Key Repository
 */
public interface BookingApiKeyRepository extends JpaRepository<BookingApiKey, Long> {

    Optional<BookingApiKey> findByVendorIdAndActiveTrue(String vendorId);
}
