package com.hola.hotel.service;

import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.hotel.dto.request.EarlyLateFeePolicyRequest;
import com.hola.hotel.dto.request.EarlyLateFeePolicySaveRequest;
import com.hola.hotel.dto.response.EarlyLateFeePolicyResponse;
import com.hola.hotel.entity.EarlyLateFeePolicy;
import com.hola.hotel.entity.Property;
import com.hola.hotel.mapper.HotelMapper;
import com.hola.hotel.repository.EarlyLateFeePolicyRepository;
import com.hola.hotel.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EarlyLateFeePolicyServiceImpl implements EarlyLateFeePolicyService {

    private final EarlyLateFeePolicyRepository earlyLateFeePolicyRepository;
    private final PropertyRepository propertyRepository;
    private final HotelMapper hotelMapper;

    @Override
    public List<EarlyLateFeePolicyResponse> getEarlyLateFeePolicies(Long propertyId) {
        return earlyLateFeePolicyRepository.findAllByPropertyIdOrderBySortOrder(propertyId).stream()
                .map(hotelMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<EarlyLateFeePolicyResponse> getEarlyLateFeePoliciesByType(Long propertyId, String policyType) {
        return earlyLateFeePolicyRepository.findAllByPropertyIdAndPolicyTypeOrderBySortOrder(propertyId, policyType).stream()
                .map(hotelMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<EarlyLateFeePolicyResponse> saveEarlyLateFeePolicies(Long propertyId, EarlyLateFeePolicySaveRequest request) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new HolaException(ErrorCode.PROPERTY_NOT_FOUND));

        // 기존 데이터 전체 soft delete
        List<EarlyLateFeePolicy> existing = earlyLateFeePolicyRepository.findAllByPropertyIdOrderBySortOrder(propertyId);
        existing.forEach(EarlyLateFeePolicy::softDelete);

        // 빈 목록이면 삭제만 수행
        List<EarlyLateFeePolicyRequest> policies = request.getPolicies();
        if (policies == null || policies.isEmpty()) {
            log.info("얼리/레이트 요금 정책 전체 삭제: propertyId={}", propertyId);
            return Collections.emptyList();
        }

        // 시간 범위 검증 후 새로 insert
        for (int i = 0; i < policies.size(); i++) {
            EarlyLateFeePolicyRequest policy = policies.get(i);

            // 시작 시간이 종료 시간보다 이후인지 검증
            if (policy.getTimeFrom().compareTo(policy.getTimeTo()) > 0) {
                throw new HolaException(ErrorCode.EARLY_LATE_INVALID_TIME_RANGE);
            }

            EarlyLateFeePolicy entity = EarlyLateFeePolicy.builder()
                    .property(property)
                    .policyType(policy.getPolicyType())
                    .timeFrom(policy.getTimeFrom())
                    .timeTo(policy.getTimeTo())
                    .feeType(policy.getFeeType())
                    .feeValue(policy.getFeeValue())
                    .description(policy.getDescription())
                    .build();
            entity.changeSortOrder(i);
            earlyLateFeePolicyRepository.save(entity);
        }

        log.info("얼리/레이트 요금 정책 저장: propertyId={}, count={}", propertyId, policies.size());

        return earlyLateFeePolicyRepository.findAllByPropertyIdOrderBySortOrder(propertyId).stream()
                .map(hotelMapper::toResponse)
                .collect(Collectors.toList());
    }
}
