package com.hola.room.service;

import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.room.dto.request.PaidServiceOptionCreateRequest;
import com.hola.room.dto.request.PaidServiceOptionUpdateRequest;
import com.hola.room.dto.response.PaidServiceOptionResponse;
import com.hola.room.entity.InventoryItem;
import com.hola.room.entity.PaidServiceOption;
import com.hola.room.entity.RoomTypePaidService;
import com.hola.room.entity.TransactionCode;
import com.hola.room.mapper.PaidServiceOptionMapper;
import com.hola.room.repository.InventoryItemRepository;
import com.hola.room.repository.PaidServiceOptionRepository;
import com.hola.room.repository.RoomTypePaidServiceRepository;
import com.hola.room.repository.TransactionCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private final RoomTypePaidServiceRepository roomTypePaidServiceRepository;
    private final TransactionCodeRepository transactionCodeRepository;
    private final InventoryItemRepository inventoryItemRepository;

    @Override
    public List<PaidServiceOptionResponse> getPaidServiceOptions(Long propertyId) {
        List<PaidServiceOption> options = paidServiceOptionRepository
                .findAllByPropertyIdOrderBySortOrderAscServiceNameKoAsc(propertyId);
        Map<Long, TransactionCode> tcMap = buildTransactionCodeMap(propertyId);
        return options.stream()
                .map(opt -> paidServiceOptionMapper.toResponse(opt, tcMap.get(opt.getTransactionCodeId())))
                .toList();
    }

    @Override
    public List<PaidServiceOptionResponse> getPaidServiceOptions(Long propertyId, Long roomTypeId) {
        if (roomTypeId == null) {
            return getPaidServiceOptions(propertyId);
        }

        // 객실타입에 매핑된 서비스 ID 조회
        List<RoomTypePaidService> mappings = roomTypePaidServiceRepository.findAllByRoomTypeId(roomTypeId);
        if (mappings.isEmpty()) {
            // 매핑이 없으면 전체 프로퍼티 서비스 폴백
            return getPaidServiceOptions(propertyId);
        }

        Set<Long> mappedIds = mappings.stream()
                .map(RoomTypePaidService::getPaidServiceOptionId)
                .collect(Collectors.toSet());

        Map<Long, TransactionCode> tcMap = buildTransactionCodeMap(propertyId);
        return paidServiceOptionRepository.findAllByPropertyIdOrderBySortOrderAscServiceNameKoAsc(propertyId)
                .stream()
                .filter(opt -> mappedIds.contains(opt.getId()))
                .map(opt -> paidServiceOptionMapper.toResponse(opt, tcMap.get(opt.getTransactionCodeId())))
                .toList();
    }

    @Override
    public PaidServiceOptionResponse getPaidServiceOption(Long id) {
        PaidServiceOption entity = findById(id);
        TransactionCode tc = entity.getTransactionCodeId() != null
                ? transactionCodeRepository.findById(entity.getTransactionCodeId()).orElse(null)
                : null;
        return paidServiceOptionMapper.toResponse(entity, tc);
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
                // Phase 2 확장
                .transactionCodeId(request.getTransactionCodeId())
                .postingFrequency(request.getPostingFrequency())
                .packageScope(request.getPackageScope() != null ? request.getPackageScope() : "PROPERTY_WIDE")
                .sellSeparately(request.getSellSeparately() != null ? request.getSellSeparately() : true)
                .inventoryItemId(request.getInventoryItemId())
                .build();

        if (request.getUseYn() != null && !request.getUseYn()) {
            entity.deactivate();
        }

        PaidServiceOption saved = paidServiceOptionRepository.save(entity);
        log.info("유료 서비스 옵션 생성: {} ({}) - 프로퍼티: {}", saved.getServiceNameKo(), saved.getServiceOptionCode(), propertyId);

        TransactionCode tc = saved.getTransactionCodeId() != null
                ? transactionCodeRepository.findById(saved.getTransactionCodeId()).orElse(null)
                : null;
        return paidServiceOptionMapper.toResponse(saved, tc);
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

        // Phase 2 확장 필드 수정
        entity.updatePackageFields(
                request.getTransactionCodeId(),
                request.getPostingFrequency(),
                request.getPackageScope(),
                request.getSellSeparately(),
                request.getInventoryItemId()
        );

        if (request.getUseYn() != null) {
            if (request.getUseYn()) {
                entity.activate();
            } else {
                entity.deactivate();
            }
        }

        log.info("유료 서비스 옵션 수정: {} ({})", entity.getServiceNameKo(), entity.getServiceOptionCode());
        TransactionCode tc = entity.getTransactionCodeId() != null
                ? transactionCodeRepository.findById(entity.getTransactionCodeId()).orElse(null)
                : null;
        return paidServiceOptionMapper.toResponse(entity, tc);
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

    /**
     * 프로퍼티의 전체 TransactionCode를 ID 기반 맵으로 구성
     */
    private Map<Long, TransactionCode> buildTransactionCodeMap(Long propertyId) {
        return transactionCodeRepository.findAllByPropertyIdOrderBySortOrderAscTransactionCodeAsc(propertyId)
                .stream()
                .collect(Collectors.toMap(TransactionCode::getId, Function.identity()));
    }
}
