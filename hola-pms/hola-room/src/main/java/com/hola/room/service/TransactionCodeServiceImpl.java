package com.hola.room.service;

import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.room.dto.request.*;
import com.hola.room.dto.response.*;
import com.hola.room.entity.TransactionCode;
import com.hola.room.entity.TransactionCodeGroup;
import com.hola.room.mapper.TransactionCodeMapper;
import com.hola.room.repository.PaidServiceOptionRepository;
import com.hola.room.repository.TransactionCodeGroupRepository;
import com.hola.room.repository.TransactionCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionCodeServiceImpl implements TransactionCodeService {

    private final TransactionCodeGroupRepository groupRepository;
    private final TransactionCodeRepository codeRepository;
    private final PaidServiceOptionRepository paidServiceOptionRepository;
    private final TransactionCodeMapper mapper;

    // ========== 그룹 ==========

    @Override
    public List<TransactionCodeGroupTreeResponse> getGroupTree(Long propertyId) {
        List<TransactionCodeGroup> allGroups =
                groupRepository.findAllByPropertyIdOrderBySortOrderAscGroupNameKoAsc(propertyId);

        // MAIN 그룹 필터링
        List<TransactionCodeGroup> mainGroups = allGroups.stream()
                .filter(g -> "MAIN".equals(g.getGroupType()))
                .toList();

        // MAIN별 SUB 그룹 매핑하여 트리 구성
        return mainGroups.stream()
                .map(main -> {
                    List<TransactionCodeGroupTreeResponse> children = allGroups.stream()
                            .filter(g -> "SUB".equals(g.getGroupType()) && main.getId().equals(g.getParentGroupId()))
                            .map(sub -> mapper.toGroupTreeResponse(sub, List.of()))
                            .toList();
                    return mapper.toGroupTreeResponse(main, children);
                })
                .toList();
    }

    @Override
    @Transactional
    public TransactionCodeGroupResponse createGroup(Long propertyId, TransactionCodeGroupCreateRequest request) {
        // 중복 확인
        if (groupRepository.existsByPropertyIdAndGroupCode(propertyId, request.getGroupCode())) {
            throw new HolaException(ErrorCode.TC_GROUP_CODE_DUPLICATE);
        }

        // SUB 그룹인 경우 상위 MAIN 그룹 필수 + 존재 확인
        if ("SUB".equals(request.getGroupType())) {
            if (request.getParentGroupId() == null) {
                throw new HolaException(ErrorCode.INVALID_INPUT, "SUB 그룹은 상위 그룹을 지정해야 합니다.");
            }
            groupRepository.findById(request.getParentGroupId())
                    .orElseThrow(() -> new HolaException(ErrorCode.TC_GROUP_NOT_FOUND));
        }

        TransactionCodeGroup entity = TransactionCodeGroup.builder()
                .propertyId(propertyId)
                .groupCode(request.getGroupCode())
                .groupNameKo(request.getGroupNameKo())
                .groupNameEn(request.getGroupNameEn())
                .groupType(request.getGroupType())
                .parentGroupId(request.getParentGroupId())
                .build();

        if (request.getSortOrder() != null) {
            entity.changeSortOrder(request.getSortOrder());
        }

        TransactionCodeGroup saved = groupRepository.save(entity);
        log.info("트랜잭션 코드 그룹 생성: {} ({}) - 프로퍼티: {}", saved.getGroupNameKo(), saved.getGroupCode(), propertyId);

        return mapper.toGroupResponse(saved);
    }

    @Override
    @Transactional
    public TransactionCodeGroupResponse updateGroup(Long id, TransactionCodeGroupUpdateRequest request) {
        TransactionCodeGroup entity = findGroupById(id);

        entity.update(request.getGroupNameKo(), request.getGroupNameEn(), request.getSortOrder());

        if (request.getUseYn() != null) {
            if (Boolean.TRUE.equals(request.getUseYn())) {
                entity.activate();
            } else {
                entity.deactivate();
            }
        }

        log.info("트랜잭션 코드 그룹 수정: {} ({})", entity.getGroupNameKo(), entity.getGroupCode());
        return mapper.toGroupResponse(entity);
    }

    @Override
    @Transactional
    public void deleteGroup(Long id) {
        TransactionCodeGroup entity = findGroupById(id);

        // MAIN 그룹인 경우 하위 SUB 존재 확인
        if ("MAIN".equals(entity.getGroupType())) {
            long childCount = groupRepository.countByParentGroupId(id);
            if (childCount > 0) {
                throw new HolaException(ErrorCode.TC_GROUP_HAS_CHILDREN);
            }
        }

        // 하위 트랜잭션 코드 존재 확인
        long codeCount = codeRepository.countByTransactionGroupId(id);
        if (codeCount > 0) {
            throw new HolaException(ErrorCode.TC_GROUP_HAS_CODES);
        }

        entity.softDelete();
        log.info("트랜잭션 코드 그룹 삭제: {} ({})", entity.getGroupNameKo(), entity.getGroupCode());
    }

    // ========== 코드 ==========

    @Override
    public List<TransactionCodeResponse> getTransactionCodes(Long propertyId, Long groupId, String revenueCategory) {
        List<TransactionCode> codes;

        if (groupId != null) {
            codes = codeRepository.findAllByTransactionGroupIdOrderBySortOrderAscTransactionCodeAsc(groupId);
        } else if (revenueCategory != null) {
            codes = codeRepository.findAllByPropertyIdAndRevenueCategoryOrderBySortOrderAsc(propertyId, revenueCategory);
        } else {
            codes = codeRepository.findAllByPropertyIdOrderBySortOrderAscTransactionCodeAsc(propertyId);
        }

        // 그룹명 조인을 위한 맵 구성
        Map<Long, TransactionCodeGroup> groupMap = buildGroupMap(propertyId);

        return codes.stream()
                .map(code -> mapper.toCodeResponse(code, groupMap))
                .toList();
    }

    @Override
    public TransactionCodeResponse getTransactionCode(Long id) {
        TransactionCode entity = findCodeById(id);
        Map<Long, TransactionCodeGroup> groupMap = buildGroupMap(entity.getPropertyId());
        return mapper.toCodeResponse(entity, groupMap);
    }

    @Override
    @Transactional
    public TransactionCodeResponse createTransactionCode(Long propertyId, TransactionCodeCreateRequest request) {
        // 코드 중복 확인
        if (codeRepository.existsByPropertyIdAndTransactionCode(propertyId, request.getTransactionCode())) {
            throw new HolaException(ErrorCode.TC_CODE_DUPLICATE);
        }

        // 그룹 존재 확인
        groupRepository.findById(request.getTransactionGroupId())
                .orElseThrow(() -> new HolaException(ErrorCode.TC_GROUP_NOT_FOUND));

        TransactionCode entity = TransactionCode.builder()
                .propertyId(propertyId)
                .transactionGroupId(request.getTransactionGroupId())
                .transactionCode(request.getTransactionCode())
                .codeNameKo(request.getCodeNameKo())
                .codeNameEn(request.getCodeNameEn())
                .revenueCategory(request.getRevenueCategory())
                .codeType(request.getCodeType())
                .build();

        if (request.getSortOrder() != null) {
            entity.changeSortOrder(request.getSortOrder());
        }

        TransactionCode saved = codeRepository.save(entity);
        log.info("트랜잭션 코드 생성: {} ({}) - 프로퍼티: {}", saved.getCodeNameKo(), saved.getTransactionCode(), propertyId);

        Map<Long, TransactionCodeGroup> groupMap = buildGroupMap(propertyId);
        return mapper.toCodeResponse(saved, groupMap);
    }

    @Override
    @Transactional
    public TransactionCodeResponse updateTransactionCode(Long id, TransactionCodeUpdateRequest request) {
        TransactionCode entity = findCodeById(id);

        // 그룹 변경 시 존재 확인
        groupRepository.findById(request.getTransactionGroupId())
                .orElseThrow(() -> new HolaException(ErrorCode.TC_GROUP_NOT_FOUND));

        entity.update(request.getCodeNameKo(), request.getCodeNameEn(),
                request.getTransactionGroupId(), request.getRevenueCategory(),
                request.getSortOrder());

        if (request.getUseYn() != null) {
            if (Boolean.TRUE.equals(request.getUseYn())) {
                entity.activate();
            } else {
                entity.deactivate();
            }
        }

        log.info("트랜잭션 코드 수정: {} ({})", entity.getCodeNameKo(), entity.getTransactionCode());

        Map<Long, TransactionCodeGroup> groupMap = buildGroupMap(entity.getPropertyId());
        return mapper.toCodeResponse(entity, groupMap);
    }

    @Override
    @Transactional
    public void deleteTransactionCode(Long id) {
        TransactionCode entity = findCodeById(id);

        // 사용 중(활성) 상태면 삭제 불가
        if (Boolean.TRUE.equals(entity.getUseYn())) {
            throw new HolaException(ErrorCode.TC_IN_USE);
        }

        // 하위 PaidServiceOption에서 참조 중이면 삭제 불가
        if (paidServiceOptionRepository.existsByTransactionCodeId(id)) {
            throw new HolaException(ErrorCode.TC_IN_USE);
        }

        entity.softDelete();
        log.info("트랜잭션 코드 삭제: {} ({})", entity.getCodeNameKo(), entity.getTransactionCode());
    }

    @Override
    public boolean existsTransactionCode(Long propertyId, String transactionCode) {
        return codeRepository.existsByPropertyIdAndTransactionCode(propertyId, transactionCode);
    }

    @Override
    public List<TransactionCodeSelectorResponse> getSelector(Long propertyId, String revenueCategory) {
        List<TransactionCode> codes;
        if (revenueCategory != null) {
            codes = codeRepository.findAllByPropertyIdAndRevenueCategoryOrderBySortOrderAsc(propertyId, revenueCategory);
        } else {
            codes = codeRepository.findAllByPropertyIdOrderBySortOrderAscTransactionCodeAsc(propertyId);
        }

        return codes.stream()
                .filter(c -> Boolean.TRUE.equals(c.getUseYn()))
                .map(mapper::toSelectorResponse)
                .toList();
    }

    // ========== 내부 헬퍼 ==========

    private TransactionCodeGroup findGroupById(Long id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new HolaException(ErrorCode.TC_GROUP_NOT_FOUND));
    }

    private TransactionCode findCodeById(Long id) {
        return codeRepository.findById(id)
                .orElseThrow(() -> new HolaException(ErrorCode.TC_NOT_FOUND));
    }

    /**
     * 프로퍼티의 전체 그룹을 ID 기반 맵으로 구성 (Mapper에서 그룹명 조인용)
     */
    private Map<Long, TransactionCodeGroup> buildGroupMap(Long propertyId) {
        return groupRepository.findAllByPropertyIdOrderBySortOrderAscGroupNameKoAsc(propertyId)
                .stream()
                .collect(Collectors.toMap(TransactionCodeGroup::getId, Function.identity()));
    }
}
