package com.hola.hotel.repository;

import com.hola.hotel.entity.EarlyLateFeePolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EarlyLateFeePolicyRepository extends JpaRepository<EarlyLateFeePolicy, Long> {

    List<EarlyLateFeePolicy> findAllByPropertyIdOrderBySortOrder(Long propertyId);

    List<EarlyLateFeePolicy> findAllByPropertyIdAndPolicyTypeOrderBySortOrder(Long propertyId, String policyType);
}
