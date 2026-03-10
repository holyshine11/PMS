package com.hola.hotel.service;

import com.hola.hotel.dto.request.EarlyLateFeePolicySaveRequest;
import com.hola.hotel.dto.response.EarlyLateFeePolicyResponse;

import java.util.List;

public interface EarlyLateFeePolicyService {

    /** 전체 조회 */
    List<EarlyLateFeePolicyResponse> getEarlyLateFeePolicies(Long propertyId);

    /** 타입별 조회 */
    List<EarlyLateFeePolicyResponse> getEarlyLateFeePoliciesByType(Long propertyId, String policyType);

    /** 전체 저장 (기존 soft delete + 재등록) */
    List<EarlyLateFeePolicyResponse> saveEarlyLateFeePolicies(Long propertyId, EarlyLateFeePolicySaveRequest request);
}
