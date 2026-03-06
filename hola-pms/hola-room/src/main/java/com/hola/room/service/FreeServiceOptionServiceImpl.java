package com.hola.room.service;

import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.room.dto.request.FreeServiceOptionCreateRequest;
import com.hola.room.dto.request.FreeServiceOptionUpdateRequest;
import com.hola.room.dto.response.FreeServiceOptionResponse;
import com.hola.room.entity.FreeServiceOption;
import com.hola.room.mapper.FreeServiceOptionMapper;
import com.hola.room.repository.FreeServiceOptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 무료 서비스 옵션 서비스 구현
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FreeServiceOptionServiceImpl implements FreeServiceOptionService {

    private final FreeServiceOptionRepository freeServiceOptionRepository;
    private final FreeServiceOptionMapper freeServiceOptionMapper;

    @Override
    public List<FreeServiceOptionResponse> getFreeServiceOptions(Long propertyId) {
        return freeServiceOptionRepository.findAllByPropertyIdOrderBySortOrderAscServiceNameKoAsc(propertyId)
                .stream()
                .map(freeServiceOptionMapper::toResponse)
                .toList();
    }

    @Override
    public FreeServiceOptionResponse getFreeServiceOption(Long id) {
        FreeServiceOption entity = findById(id);
        return freeServiceOptionMapper.toResponse(entity);
    }

    @Override
    @Transactional
    public FreeServiceOptionResponse createFreeServiceOption(Long propertyId, FreeServiceOptionCreateRequest request) {
        // 코드 중복 확인
        if (freeServiceOptionRepository.existsByPropertyIdAndServiceOptionCode(propertyId, request.getServiceOptionCode())) {
            throw new HolaException(ErrorCode.FREE_SERVICE_CODE_DUPLICATE);
        }

        FreeServiceOption entity = FreeServiceOption.builder()
                .propertyId(propertyId)
                .serviceOptionCode(request.getServiceOptionCode())
                .serviceNameKo(request.getServiceNameKo())
                .serviceNameEn(request.getServiceNameEn())
                .serviceType(request.getServiceType())
                .applicableNights(request.getApplicableNights())
                .quantity(request.getQuantity())
                .quantityUnit(request.getQuantityUnit())
                .build();

        // 사용여부 설정
        if (request.getUseYn() != null && !request.getUseYn()) {
            entity.deactivate();
        }

        FreeServiceOption saved = freeServiceOptionRepository.save(entity);
        log.info("무료 서비스 옵션 생성: {} ({}) - 프로퍼티: {}", saved.getServiceNameKo(), saved.getServiceOptionCode(), propertyId);

        return freeServiceOptionMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public FreeServiceOptionResponse updateFreeServiceOption(Long id, FreeServiceOptionUpdateRequest request) {
        FreeServiceOption entity = findById(id);

        entity.update(
                request.getServiceNameKo(),
                request.getServiceNameEn(),
                request.getServiceType(),
                request.getApplicableNights(),
                request.getQuantity(),
                request.getQuantityUnit()
        );

        // 사용여부 토글
        if (request.getUseYn() != null) {
            if (request.getUseYn()) {
                entity.activate();
            } else {
                entity.deactivate();
            }
        }

        log.info("무료 서비스 옵션 수정: {} ({})", entity.getServiceNameKo(), entity.getServiceOptionCode());
        return freeServiceOptionMapper.toResponse(entity);
    }

    @Override
    @Transactional
    public void deleteFreeServiceOption(Long id) {
        FreeServiceOption entity = findById(id);

        // 사용 중인 항목은 삭제 불가
        if (Boolean.TRUE.equals(entity.getUseYn())) {
            throw new HolaException(ErrorCode.CANNOT_DELETE_ACTIVE);
        }

        entity.softDelete();
        log.info("무료 서비스 옵션 삭제: {} ({})", entity.getServiceNameKo(), entity.getServiceOptionCode());
    }

    @Override
    public boolean existsServiceOptionCode(Long propertyId, String serviceOptionCode) {
        return freeServiceOptionRepository.existsByPropertyIdAndServiceOptionCode(propertyId, serviceOptionCode);
    }

    private FreeServiceOption findById(Long id) {
        return freeServiceOptionRepository.findById(id)
                .orElseThrow(() -> new HolaException(ErrorCode.FREE_SERVICE_OPTION_NOT_FOUND));
    }
}
