package com.hola.room.service;

import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.room.dto.request.PaidServiceOptionCreateRequest;
import com.hola.room.dto.request.PaidServiceOptionUpdateRequest;
import com.hola.room.dto.response.PaidServiceOptionResponse;
import com.hola.room.entity.PaidServiceOption;
import com.hola.room.mapper.PaidServiceOptionMapper;
import com.hola.room.repository.PaidServiceOptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 유료 서비스 옵션 서비스 구현
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaidServiceOptionServiceImpl implements PaidServiceOptionService {

    private final PaidServiceOptionRepository paidServiceOptionRepository;
    private final PaidServiceOptionMapper paidServiceOptionMapper;

    @Override
    public List<PaidServiceOptionResponse> getPaidServiceOptions(Long propertyId) {
        return paidServiceOptionRepository.findAllByPropertyIdOrderBySortOrderAscServiceNameKoAsc(propertyId)
                .stream()
                .map(paidServiceOptionMapper::toResponse)
                .toList();
    }

    @Override
    public PaidServiceOptionResponse getPaidServiceOption(Long id) {
        PaidServiceOption entity = findById(id);
        return paidServiceOptionMapper.toResponse(entity);
    }

    @Override
    @Transactional
    public PaidServiceOptionResponse createPaidServiceOption(Long propertyId, PaidServiceOptionCreateRequest request) {
        // 코드 중복 확인
        if (paidServiceOptionRepository.existsByPropertyIdAndServiceOptionCode(propertyId, request.getServiceOptionCode())) {
            throw new HolaException(ErrorCode.PAID_SERVICE_CODE_DUPLICATE);
        }

        PaidServiceOption entity = PaidServiceOption.builder()
                .propertyId(propertyId)
                .serviceOptionCode(request.getServiceOptionCode())
                .serviceNameKo(request.getServiceNameKo())
                .serviceNameEn(request.getServiceNameEn())
                .serviceType(request.getServiceType())
                .applicableNights(request.getApplicableNights())
                .currencyCode(request.getCurrencyCode())
                .vatIncluded(request.getVatIncluded() != null ? request.getVatIncluded() : true)
                .taxRate(request.getTaxRate())
                .supplyPrice(request.getSupplyPrice())
                .taxAmount(request.getTaxAmount())
                .vatIncludedPrice(request.getVatIncludedPrice())
                .quantity(request.getQuantity())
                .quantityUnit(request.getQuantityUnit())
                .adminMemo(request.getAdminMemo())
                .build();

        if (request.getUseYn() != null && !request.getUseYn()) {
            entity.deactivate();
        }

        PaidServiceOption saved = paidServiceOptionRepository.save(entity);
        log.info("유료 서비스 옵션 생성: {} ({}) - 프로퍼티: {}", saved.getServiceNameKo(), saved.getServiceOptionCode(), propertyId);

        return paidServiceOptionMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public PaidServiceOptionResponse updatePaidServiceOption(Long id, PaidServiceOptionUpdateRequest request) {
        PaidServiceOption entity = findById(id);

        entity.update(
                request.getServiceNameKo(),
                request.getServiceNameEn(),
                request.getServiceType(),
                request.getApplicableNights(),
                request.getCurrencyCode(),
                request.getVatIncluded() != null ? request.getVatIncluded() : true,
                request.getTaxRate(),
                request.getSupplyPrice(),
                request.getTaxAmount(),
                request.getVatIncludedPrice(),
                request.getQuantity(),
                request.getQuantityUnit(),
                request.getAdminMemo()
        );

        if (request.getUseYn() != null) {
            if (request.getUseYn()) {
                entity.activate();
            } else {
                entity.deactivate();
            }
        }

        log.info("유료 서비스 옵션 수정: {} ({})", entity.getServiceNameKo(), entity.getServiceOptionCode());
        return paidServiceOptionMapper.toResponse(entity);
    }

    @Override
    @Transactional
    public void deletePaidServiceOption(Long id) {
        PaidServiceOption entity = findById(id);

        // 사용 중인 항목은 삭제 불가
        if (Boolean.TRUE.equals(entity.getUseYn())) {
            throw new HolaException(ErrorCode.CANNOT_DELETE_ACTIVE);
        }

        entity.softDelete();
        log.info("유료 서비스 옵션 삭제: {} ({})", entity.getServiceNameKo(), entity.getServiceOptionCode());
    }

    @Override
    public boolean existsServiceOptionCode(Long propertyId, String serviceOptionCode) {
        return paidServiceOptionRepository.existsByPropertyIdAndServiceOptionCode(propertyId, serviceOptionCode);
    }

    private PaidServiceOption findById(Long id) {
        return paidServiceOptionRepository.findById(id)
                .orElseThrow(() -> new HolaException(ErrorCode.PAID_SERVICE_OPTION_NOT_FOUND));
    }
}
