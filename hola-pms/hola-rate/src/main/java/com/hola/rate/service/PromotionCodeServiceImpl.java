package com.hola.rate.service;

import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.rate.dto.request.PromotionCodeCreateRequest;
import com.hola.rate.dto.request.PromotionCodeUpdateRequest;
import com.hola.rate.dto.response.PromotionCodeListResponse;
import com.hola.rate.dto.response.PromotionCodeResponse;
import com.hola.rate.entity.PromotionCode;
import com.hola.rate.mapper.PromotionCodeMapper;
import com.hola.rate.repository.PromotionCodeRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PromotionCodeServiceImpl implements PromotionCodeService {

    private final PromotionCodeRepository promotionCodeRepository;
    private final PromotionCodeMapper promotionCodeMapper;
    private final EntityManager entityManager;

    @Override
    public List<PromotionCodeListResponse> getPromotionCodes(Long propertyId) {
        return promotionCodeRepository.findAllByPropertyIdOrderBySortOrderAscPromotionCodeAsc(propertyId)
                .stream()
                .map(pc -> promotionCodeMapper.toListResponse(pc, getRateCodeName(pc.getRateCodeId())))
                .toList();
    }

    @Override
    public PromotionCodeResponse getPromotionCode(Long id) {
        PromotionCode pc = findById(id);
        return promotionCodeMapper.toResponse(pc, getRateCodeName(pc.getRateCodeId()));
    }

    @Override
    @Transactional
    public PromotionCodeResponse createPromotionCode(Long propertyId, PromotionCodeCreateRequest request) {
        // 코드 중복 확인
        if (promotionCodeRepository.existsByPropertyIdAndPromotionCode(propertyId, request.getPromotionCode())) {
            throw new HolaException(ErrorCode.PROMOTION_CODE_DUPLICATE);
        }
        // 기간 검증
        validatePeriod(request.getPromotionStartDate(), request.getPromotionEndDate());

        PromotionCode pc = PromotionCode.builder()
                .propertyId(propertyId)
                .rateCodeId(request.getRateCodeId())
                .promotionCode(request.getPromotionCode())
                .promotionStartDate(request.getPromotionStartDate())
                .promotionEndDate(request.getPromotionEndDate())
                .descriptionKo(request.getDescriptionKo())
                .descriptionEn(request.getDescriptionEn())
                .promotionType(request.getPromotionType())
                .downUpSign(request.getDownUpSign())
                .downUpValue(request.getDownUpValue())
                .downUpUnit(request.getDownUpUnit())
                .roundingDecimalPoint(request.getRoundingDecimalPoint() != null ? request.getRoundingDecimalPoint() : 0)
                .roundingDigits(request.getRoundingDigits() != null ? request.getRoundingDigits() : 0)
                .roundingMethod(request.getRoundingMethod())
                .build();

        if (request.getUseYn() != null && !request.getUseYn()) {
            pc.deactivate();
        }
        if (request.getSortOrder() != null) {
            pc.changeSortOrder(request.getSortOrder());
        }

        PromotionCode saved = promotionCodeRepository.save(pc);
        log.info("프로모션 코드 생성: {} - 프로퍼티: {}", saved.getPromotionCode(), propertyId);
        return promotionCodeMapper.toResponse(saved, getRateCodeName(saved.getRateCodeId()));
    }

    @Override
    @Transactional
    public PromotionCodeResponse updatePromotionCode(Long id, PromotionCodeUpdateRequest request) {
        PromotionCode pc = findById(id);
        validatePeriod(request.getPromotionStartDate(), request.getPromotionEndDate());

        pc.update(
                request.getRateCodeId(),
                request.getPromotionStartDate(),
                request.getPromotionEndDate(),
                request.getDescriptionKo(),
                request.getDescriptionEn(),
                request.getPromotionType(),
                request.getDownUpSign(),
                request.getDownUpValue(),
                request.getDownUpUnit(),
                request.getRoundingDecimalPoint(),
                request.getRoundingDigits(),
                request.getRoundingMethod()
        );

        if (request.getUseYn() != null) {
            if (request.getUseYn()) pc.activate(); else pc.deactivate();
        }
        if (request.getSortOrder() != null) {
            pc.changeSortOrder(request.getSortOrder());
        }

        log.info("프로모션 코드 수정: {}", pc.getPromotionCode());
        return promotionCodeMapper.toResponse(pc, getRateCodeName(pc.getRateCodeId()));
    }

    @Override
    @Transactional
    public void deletePromotionCode(Long id) {
        PromotionCode pc = findById(id);
        // 사용 중인 항목은 삭제 불가
        if (Boolean.TRUE.equals(pc.getUseYn())) {
            throw new HolaException(ErrorCode.CANNOT_DELETE_ACTIVE);
        }
        pc.softDelete();
        log.info("프로모션 코드 삭제: {}", pc.getPromotionCode());
    }

    @Override
    public boolean existsPromotionCode(Long propertyId, String promotionCode) {
        return promotionCodeRepository.existsByPropertyIdAndPromotionCode(propertyId, promotionCode);
    }

    private PromotionCode findById(Long id) {
        return promotionCodeRepository.findById(id)
                .orElseThrow(() -> new HolaException(ErrorCode.PROMOTION_CODE_NOT_FOUND));
    }

    private void validatePeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new HolaException(ErrorCode.PROMOTION_INVALID_PERIOD);
        }
    }

    private String getRateCodeName(Long rateCodeId) {
        if (rateCodeId == null) return null;
        try {
            Object result = entityManager
                    .createNativeQuery("SELECT rate_code FROM rt_rate_code WHERE id = :id AND deleted_at IS NULL")
                    .setParameter("id", rateCodeId)
                    .getSingleResult();
            return result != null ? result.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
