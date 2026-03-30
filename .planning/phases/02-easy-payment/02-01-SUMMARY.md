---
phase: 02-easy-payment
plan: 01
subsystem: easy-pay
tags: [mock-gateway, test-profile, billkey, unit-test]
dependency_graph:
  requires: []
  provides: [MockPaymentGateway.registerTransaction, MockPaymentGateway.approveAfterAuth, EasyPayCardServiceImpl.test-safe]
  affects: [EasyPayApiController, EasyPayCardServiceImpl, MockPaymentGateway]
tech_stack:
  added: []
  patterns: [@Autowired(required=false) for optional @Profile beans, ReflectionTestUtils for optional field injection in tests]
key_files:
  created:
    - hola-pms/hola-reservation/src/test/java/com/hola/reservation/booking/service/EasyPayCardServiceImplTest.java
  modified:
    - hola-pms/hola-reservation/src/main/java/com/hola/reservation/booking/gateway/MockPaymentGateway.java
    - hola-pms/hola-reservation/src/main/java/com/hola/reservation/booking/controller/EasyPayApiController.java
    - hola-pms/hola-reservation/src/main/java/com/hola/reservation/booking/service/EasyPayCardServiceImpl.java
decisions:
  - "@Autowired(required=false) field injection used for KiccApiClient/KiccProperties — @RequiredArgsConstructor retained for other final fields"
  - "EasyPayApiController.billkeyReturn and payWithBillkey both guard kiccApiClient and kiccProperties null before use"
  - "EasyPayCardServiceImpl.deleteCard wraps KICC call in null check — DB soft delete always proceeds regardless of KICC availability"
metrics:
  duration: 6m
  completed: "2026-03-30"
  tasks_completed: 2
  files_modified: 4
---

# Phase 02 Plan 01: EasyPay Test-Profile Fix & MockGateway Enhancement Summary

**One-liner:** MockPaymentGateway now supports billkey registration flow (registerTransaction/approveAfterAuth) and EasyPay beans load cleanly in test profile via @Autowired(required=false) on KiccApiClient/KiccProperties.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | MockPaymentGateway 빌키 메서드 추가 + KiccApiClient test-safe 처리 | 700db53 | MockPaymentGateway.java, EasyPayApiController.java, EasyPayCardServiceImpl.java |
| 2 | EasyPayCardServiceImpl 단위 테스트 작성 | 099ab88 | EasyPayCardServiceImplTest.java |

## What Was Built

### Task 1: MockPaymentGateway + Test-Safe Bean Injection

**MockPaymentGateway.java:**
- Added `registerTransaction(RegisterRequest)` override: returns mock authPageUrl with `shopOrderNo` query param
- Added `approveAfterAuth(ApproveAfterAuthRequest)` override: returns mock PaymentResult with random MOCK- approval number

**EasyPayApiController.java:**
- Removed `final` from `kiccApiClient` and `kiccProperties` fields
- Added `@Autowired(required = false)` on both fields (field injection)
- `@RequiredArgsConstructor` still handles the other required beans
- Added null guards in `billkeyReturn()` and `payWithBillkey()` before KICC calls

**EasyPayCardServiceImpl.java:**
- Same pattern: `@Autowired(required = false)` on `kiccApiClient` and `kiccProperties`
- `deleteCard()` wrapped in null-check: KICC removeBatchKey only called when both non-null; DB soft delete always proceeds

### Task 2: EasyPayCardServiceImpl Unit Tests

8 tests across 4 nested groups:
- `getCardsByEmail`: returns card list / returns empty list
- `registerCard`: successful registration / EASY_PAY_CARD_LIMIT_EXCEEDED when count >= 5
- `deleteCard`: successful delete with KICC + soft delete / EMAIL_MISMATCH exception / KICC failure gracefully degrades to soft delete only
- `canRegisterMore`: true when count < 5 / false when count >= 5

## Verification

- `./gradlew compileJava` — BUILD SUCCESSFUL
- `./gradlew :hola-reservation:test --tests "*EasyPayCardServiceImplTest*"` — BUILD SUCCESSFUL (8 tests passed)
- `grep -c "registerTransaction" MockPaymentGateway.java` — 1
- `grep -c "required = false" EasyPayApiController.java` — 4 (2 each for kiccApiClient and kiccProperties)

## Deviations from Plan

**1. [Rule 2 - Missing Functionality] Added null guard for kiccProperties in billkeyReturn()**
- **Found during:** Task 1
- **Issue:** Plan specified null guard only for kiccApiClient in billkeyReturn(), but kiccProperties.getMallId() is called immediately after kiccApiClient usage — the same profile restriction applies
- **Fix:** Added null guard for kiccProperties before kiccProperties.getMallId() usage in billkeyReturn()
- **Files modified:** EasyPayApiController.java
- **Commit:** 700db53

## Known Stubs

None — all implemented functionality is complete and wired.

## Self-Check: PASSED

Files verified:
- FOUND: /Users/Dev/PMS/hola-pms/hola-reservation/src/main/java/com/hola/reservation/booking/gateway/MockPaymentGateway.java
- FOUND: /Users/Dev/PMS/hola-pms/hola-reservation/src/main/java/com/hola/reservation/booking/controller/EasyPayApiController.java
- FOUND: /Users/Dev/PMS/hola-pms/hola-reservation/src/main/java/com/hola/reservation/booking/service/EasyPayCardServiceImpl.java
- FOUND: /Users/Dev/PMS/hola-pms/hola-reservation/src/test/java/com/hola/reservation/booking/service/EasyPayCardServiceImplTest.java

Commits verified:
- FOUND: 700db53 (Task 1)
- FOUND: 099ab88 (Task 2)
