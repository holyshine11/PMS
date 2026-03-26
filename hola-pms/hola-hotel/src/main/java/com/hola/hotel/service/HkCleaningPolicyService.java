package com.hola.hotel.service;

import com.hola.hotel.dto.request.HkCleaningPolicyRequest;
import com.hola.hotel.dto.response.HkCleaningPolicyResponse;
import com.hola.hotel.dto.response.ResolvedCleaningPolicy;

import java.util.List;

/**
 * 청소 정책 서비스 인터페이스
 */
public interface HkCleaningPolicyService {

    /** 해석된 최종 정책 (프로퍼티 기본값 + 룸타입 오버라이드 병합) */
    ResolvedCleaningPolicy resolvePolicy(Long propertyId, Long roomTypeId);

    /** 프로퍼티의 모든 룸타입에 대한 정책 목록 (오버라이드 여부 포함) */
    List<HkCleaningPolicyResponse> getAllPolicies(Long propertyId);

    /** 오버라이드 생성 또는 수정 */
    HkCleaningPolicyResponse createOrUpdate(Long propertyId, HkCleaningPolicyRequest request);

    /** 오버라이드 삭제 (프로퍼티 기본값으로 복귀) */
    void deletePolicy(Long propertyId, Long roomTypeId);
}
