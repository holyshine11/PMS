package com.hola.room.repository;

import com.hola.room.entity.TransactionCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionCodeRepository extends JpaRepository<TransactionCode, Long> {

    List<TransactionCode> findAllByPropertyIdOrderBySortOrderAscTransactionCodeAsc(Long propertyId);

    List<TransactionCode> findAllByTransactionGroupIdOrderBySortOrderAscTransactionCodeAsc(Long transactionGroupId);

    List<TransactionCode> findAllByPropertyIdAndRevenueCategoryOrderBySortOrderAsc(Long propertyId, String revenueCategory);

    boolean existsByPropertyIdAndTransactionCode(Long propertyId, String transactionCode);

    long countByTransactionGroupId(Long transactionGroupId);
}
