package com.hola.room.service;

import com.hola.room.dto.request.*;
import com.hola.room.dto.response.*;

import java.util.List;

public interface TransactionCodeService {

    // === 그룹 ===
    List<TransactionCodeGroupTreeResponse> getGroupTree(Long propertyId);
    TransactionCodeGroupResponse createGroup(Long propertyId, TransactionCodeGroupCreateRequest request);
    TransactionCodeGroupResponse updateGroup(Long id, TransactionCodeGroupUpdateRequest request);
    void deleteGroup(Long id);

    // === 코드 ===
    List<TransactionCodeResponse> getTransactionCodes(Long propertyId, Long groupId, String revenueCategory);
    TransactionCodeResponse getTransactionCode(Long id);
    TransactionCodeResponse createTransactionCode(Long propertyId, TransactionCodeCreateRequest request);
    TransactionCodeResponse updateTransactionCode(Long id, TransactionCodeUpdateRequest request);
    void deleteTransactionCode(Long id);
    boolean existsTransactionCode(Long propertyId, String transactionCode);
    List<TransactionCodeSelectorResponse> getSelector(Long propertyId, String revenueCategory);
}
