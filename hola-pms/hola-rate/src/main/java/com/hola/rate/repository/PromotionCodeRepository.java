package com.hola.rate.repository;

import com.hola.rate.entity.PromotionCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 프로모션 코드 리포지토리
 */
public interface PromotionCodeRepository extends JpaRepository<PromotionCode, Long> {

    List<PromotionCode> findAllByPropertyIdOrderBySortOrderAscPromotionCodeAsc(Long propertyId);

    boolean existsByPropertyIdAndPromotionCode(Long propertyId, String promotionCode);

}
