package com.hola.reservation.booking.repository;

import com.hola.reservation.booking.entity.EasyPayCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 간편결제 카드 리포지토리
 */
public interface EasyPayCardRepository extends JpaRepository<EasyPayCard, Long> {

    /** 이메일로 등록된 카드 목록 조회 (최신순) */
    List<EasyPayCard> findByEmailOrderByCreatedAtDesc(String email);

    /** 이메일별 카드 수 조회 */
    long countByEmail(String email);

    /** 빌키로 카드 조회 */
    Optional<EasyPayCard> findByBatchKey(String batchKey);
}
