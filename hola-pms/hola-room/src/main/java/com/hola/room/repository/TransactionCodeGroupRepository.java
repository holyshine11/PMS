package com.hola.room.repository;

import com.hola.room.entity.TransactionCodeGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionCodeGroupRepository extends JpaRepository<TransactionCodeGroup, Long> {

    List<TransactionCodeGroup> findAllByPropertyIdOrderBySortOrderAscGroupNameKoAsc(Long propertyId);

    List<TransactionCodeGroup> findAllByPropertyIdAndGroupTypeOrderBySortOrderAsc(Long propertyId, String groupType);

    List<TransactionCodeGroup> findAllByParentGroupIdOrderBySortOrderAsc(Long parentGroupId);

    boolean existsByPropertyIdAndGroupCode(Long propertyId, String groupCode);

    long countByParentGroupId(Long parentGroupId);
}
