package com.hola.room.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.common.security.AccessControlService;
import com.hola.room.dto.request.*;
import com.hola.room.dto.response.*;
import com.hola.room.service.TransactionCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "트랜잭션 코드", description = "트랜잭션 코드 그룹 및 코드 관리 API")
@RestController
@RequiredArgsConstructor
public class TransactionCodeApiController {

    private final AccessControlService accessControlService;
    private final TransactionCodeService transactionCodeService;

    // ========== 그룹 API ==========

    @Operation(summary = "그룹 트리 조회", description = "MAIN → SUB 계층 구조로 조회")
    @GetMapping("/api/v1/properties/{propertyId}/transaction-code-groups")
    public ResponseEntity<HolaResponse<List<TransactionCodeGroupTreeResponse>>> getGroupTree(
            @PathVariable Long propertyId) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(transactionCodeService.getGroupTree(propertyId)));
    }

    @Operation(summary = "그룹 등록")
    @PostMapping("/api/v1/properties/{propertyId}/transaction-code-groups")
    public ResponseEntity<HolaResponse<TransactionCodeGroupResponse>> createGroup(
            @PathVariable Long propertyId,
            @Valid @RequestBody TransactionCodeGroupCreateRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        TransactionCodeGroupResponse response = transactionCodeService.createGroup(propertyId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(HolaResponse.success(response));
    }

    @Operation(summary = "그룹 수정")
    @PutMapping("/api/v1/properties/{propertyId}/transaction-code-groups/{id}")
    public ResponseEntity<HolaResponse<TransactionCodeGroupResponse>> updateGroup(
            @PathVariable Long propertyId,
            @PathVariable Long id,
            @Valid @RequestBody TransactionCodeGroupUpdateRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(transactionCodeService.updateGroup(id, request)));
    }

    @Operation(summary = "그룹 삭제", description = "하위 그룹/코드가 없는 경우만 삭제 가능")
    @DeleteMapping("/api/v1/properties/{propertyId}/transaction-code-groups/{id}")
    public ResponseEntity<HolaResponse<Void>> deleteGroup(
            @PathVariable Long propertyId,
            @PathVariable Long id) {
        accessControlService.validatePropertyAccess(propertyId);
        transactionCodeService.deleteGroup(id);
        return ResponseEntity.ok(HolaResponse.success());
    }

    // ========== 코드 API ==========

    @Operation(summary = "트랜잭션 코드 목록 조회", description = "그룹ID 또는 매출분류로 필터링 가능")
    @GetMapping("/api/v1/properties/{propertyId}/transaction-codes")
    public ResponseEntity<HolaResponse<List<TransactionCodeResponse>>> getTransactionCodes(
            @PathVariable Long propertyId,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) String revenueCategory) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(
                transactionCodeService.getTransactionCodes(propertyId, groupId, revenueCategory)));
    }

    @Operation(summary = "트랜잭션 코드 상세 조회")
    @GetMapping("/api/v1/properties/{propertyId}/transaction-codes/{id}")
    public ResponseEntity<HolaResponse<TransactionCodeResponse>> getTransactionCode(
            @PathVariable Long propertyId,
            @PathVariable Long id) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(transactionCodeService.getTransactionCode(id)));
    }

    @Operation(summary = "트랜잭션 코드 등록")
    @PostMapping("/api/v1/properties/{propertyId}/transaction-codes")
    public ResponseEntity<HolaResponse<TransactionCodeResponse>> createTransactionCode(
            @PathVariable Long propertyId,
            @Valid @RequestBody TransactionCodeCreateRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        TransactionCodeResponse response = transactionCodeService.createTransactionCode(propertyId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(HolaResponse.success(response));
    }

    @Operation(summary = "트랜잭션 코드 수정")
    @PutMapping("/api/v1/properties/{propertyId}/transaction-codes/{id}")
    public ResponseEntity<HolaResponse<TransactionCodeResponse>> updateTransactionCode(
            @PathVariable Long propertyId,
            @PathVariable Long id,
            @Valid @RequestBody TransactionCodeUpdateRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(transactionCodeService.updateTransactionCode(id, request)));
    }

    @Operation(summary = "트랜잭션 코드 삭제", description = "비활성(useYn=false) 상태만 삭제 가능")
    @DeleteMapping("/api/v1/properties/{propertyId}/transaction-codes/{id}")
    public ResponseEntity<HolaResponse<Void>> deleteTransactionCode(
            @PathVariable Long propertyId,
            @PathVariable Long id) {
        accessControlService.validatePropertyAccess(propertyId);
        transactionCodeService.deleteTransactionCode(id);
        return ResponseEntity.ok(HolaResponse.success());
    }

    @Operation(summary = "코드 중복 확인")
    @GetMapping("/api/v1/properties/{propertyId}/transaction-codes/check-code")
    public ResponseEntity<HolaResponse<Map<String, Boolean>>> checkCode(
            @PathVariable Long propertyId,
            @RequestParam String transactionCode) {
        accessControlService.validatePropertyAccess(propertyId);
        boolean duplicate = transactionCodeService.existsTransactionCode(propertyId, transactionCode);
        return ResponseEntity.ok(HolaResponse.success(Map.of("duplicate", duplicate)));
    }

    @Operation(summary = "드롭다운 선택자", description = "활성 코드만 반환, 매출분류 필터 가능")
    @GetMapping("/api/v1/properties/{propertyId}/transaction-codes/selector")
    public ResponseEntity<HolaResponse<List<TransactionCodeSelectorResponse>>> getSelector(
            @PathVariable Long propertyId,
            @RequestParam(required = false) String revenueCategory) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(
                transactionCodeService.getSelector(propertyId, revenueCategory)));
    }
}
