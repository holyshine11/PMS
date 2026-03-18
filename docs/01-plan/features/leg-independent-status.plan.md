# Leg 독립 상태 관리 (Leg-Independent Status Management) Plan

> 작성일: 2026-03-18
> 우선순위: Critical
> 영향 범위: 백엔드 8개 + 프론트엔드 6개 + 테스트 3개 = **17개 파일**

---

## Executive Summary

| 항목 | 내용 |
|------|------|
| Feature | Leg(SubReservation) 독립 상태 관리 |
| 문제 | 마스터 단위 상태 변경으로 Leg별 독립 체크인/아웃 불가 |
| 해결 | Leg별 독립 상태 전이 + 마스터 상태 자동 도출(derived) |
| 예상 규모 | Java 8파일 + JS 6파일 + Test 3파일 |
| 리스크 | 결제 로직, 가용성 검사, 대시보드 KPI에 영향 |

---

## 1. 현재 문제점

### 1.1 마스터 단위 상태 변경의 한계

```
changeStatus(masterID, "CHECK_IN")
  ├── 마스터 상태 전이 검증: RESERVED → CHECK_IN ✓
  ├── 마스터 상태 변경: RESERVED → CHECK_IN
  └── 전체 Leg 동기화: 모든 non-CANCELED Leg → CHECK_IN
      ↑ 문제: 모든 Leg가 같은 상태를 강제받음
```

### 1.2 실제 발생하는 시나리오

| # | 시나리오 | 현재 동작 | 기대 동작 |
|---|---------|---------|---------|
| S1 | 투숙중 마스터에 Leg 추가 | 새 Leg = RESERVED (체크인 불가) | 새 Leg = 마스터 상태 상속 또는 독립 체크인 가능 |
| S2 | Leg 2개 중 1개만 체크아웃 | 불가 (마스터 단위 일괄 변경) | Leg1 체크아웃, Leg2 투숙중 유지 |
| S3 | Leg별 체크인 시점 다름 | 불가 | Leg1 오늘 체크인, Leg2 내일 체크인 |
| S4 | 예약 상세에서 Leg 상태 표시 | 마스터 상태만 표시 | Leg별 독립 상태 표시 |

---

## 2. 설계

### 2.1 핵심 원칙

1. **Leg(SubReservation)이 상태의 주체**: 상태 전이는 Leg 단위로 수행
2. **마스터 상태는 파생값**: Leg 상태들로부터 자동 도출 (수동 변경 안 함)
3. **하위 호환**: `subReservationId` 미지정 시 기존처럼 전체 Leg 일괄 변경
4. **기존 쿼리 영향 최소화**: SubReservationRepository의 쿼리는 이미 `roomReservationStatus` 기준이므로 변경 불필요

### 2.2 마스터 상태 도출 규칙 (deriveMasterStatus)

```java
/**
 * 마스터 상태 도출 (우선순위 기반)
 *
 * 활성 Leg = CANCELED/NO_SHOW 제외한 Leg들
 *
 * 규칙:
 * 1. 활성 Leg 없음 → 전부 CANCELED이면 CANCELED, 전부 NO_SHOW면 NO_SHOW
 * 2. 하나라도 INHOUSE → INHOUSE
 * 3. 하나라도 CHECK_IN → CHECK_IN
 * 4. 전부 CHECKED_OUT → CHECKED_OUT
 * 5. 하나라도 RESERVED → RESERVED
 */
private String deriveMasterStatus(List<SubReservation> subs) {
    List<String> activeStatuses = subs.stream()
        .filter(s -> !"CANCELED".equals(s.getRoomReservationStatus())
                  && !"NO_SHOW".equals(s.getRoomReservationStatus()))
        .map(SubReservation::getRoomReservationStatus)
        .toList();

    if (activeStatuses.isEmpty()) {
        // 전부 취소/노쇼
        boolean allCanceled = subs.stream().allMatch(s -> "CANCELED".equals(s.getRoomReservationStatus()));
        return allCanceled ? "CANCELED" : "NO_SHOW";
    }

    if (activeStatuses.contains("INHOUSE"))     return "INHOUSE";
    if (activeStatuses.contains("CHECK_IN"))     return "CHECK_IN";
    if (activeStatuses.stream().allMatch("CHECKED_OUT"::equals)) return "CHECKED_OUT";
    return "RESERVED";
}
```

### 2.3 상태 전이 매트릭스 (Leg 단위 — 변경 없음)

```
RESERVED  → CHECK_IN, CANCELED, NO_SHOW
CHECK_IN  → INHOUSE, CANCELED
INHOUSE   → CHECKED_OUT
CHECKED_OUT → (종료)
CANCELED    → (종료)
NO_SHOW     → (종료)
```

- 기존과 동일하지만 **Leg 단위**로 적용
- `CHECK_IN → CHECKED_OUT` 직접 전이는 제거 (반드시 INHOUSE 거쳐야 함)

### 2.4 API 변경

```
PUT /api/v1/properties/{propertyId}/reservations/{id}/status

요청 Body:
{
    "newStatus": "CHECK_IN",
    "subReservationId": 123     // 선택적 — null이면 전체 Leg 일괄 변경
}
```

| subReservationId | 동작 |
|------------------|------|
| **지정** | 해당 Leg만 상태 변경 → deriveMasterStatus() |
| **미지정 (null)** | 전이 가능한 모든 Leg 일괄 변경 → deriveMasterStatus() |

### 2.5 체크인/아웃 부수효과 (Leg 단위)

```
Leg CHECK_IN 시:
  ├── 해당 Leg의 actualCheckInTime 기록
  ├── 해당 Leg의 earlyCheckInFee 계산
  ├── 해당 Leg의 객실 FO 상태 → OCCUPIED
  └── 마스터 상태 재도출 (deriveMasterStatus)

Leg CHECKED_OUT 시:
  ├── 해당 Leg의 actualCheckOutTime 기록
  ├── 해당 Leg의 lateCheckOutFee 계산
  ├── 해당 Leg의 객실 FO → VACANT, HK → DIRTY
  ├── 결제 잔액 검증 (마스터 단위)
  └── 마스터 상태 재도출 (deriveMasterStatus)

결제 잔액 검증:
  - 마지막 활성 Leg 체크아웃 시에만 검증
  - 아직 투숙중인 Leg가 있으면 잔액 검증 스킵
```

---

## 3. 구현 Phase

### Phase 1: 백엔드 코어 (가장 중요)

| # | 파일 | 변경 내용 |
|---|------|---------|
| 1-1 | `ReservationStatusRequest.java` | `subReservationId` (Long, nullable) 필드 추가 |
| 1-2 | `ReservationServiceImpl.java` | `changeStatus()` 리팩토링 — Leg 단위 전이 + `deriveMasterStatus()` 추가 |
| 1-3 | `ReservationServiceImpl.java` | `cancel()` 메서드 — Leg별 취소 지원 (선택적) |
| 1-4 | `ReservationServiceImpl.java` | `changeStatus()` 내 CHECK_IN → CHECKED_OUT 직접 전이 제거 |

**changeStatus() 리팩토링 의사코드**:

```java
public void changeStatus(Long id, Long propertyId, ReservationStatusRequest request) {
    MasterReservation master = findMasterById(id, propertyId);
    String newStatus = request.getNewStatus();

    if (request.getSubReservationId() != null) {
        // ── Leg 단위 변경 ──
        SubReservation targetSub = findSubAndValidateOwnership(request.getSubReservationId(), master);
        String currentLegStatus = targetSub.getRoomReservationStatus();

        // Leg 상태 전이 검증
        Set<String> allowed = STATUS_TRANSITIONS.getOrDefault(currentLegStatus, Set.of());
        if (!allowed.contains(newStatus)) {
            throw new HolaException(ErrorCode.RESERVATION_STATUS_CHANGE_NOT_ALLOWED);
        }

        // 전제조건 검증 (체크인: 객실 배정 + OOO 확인)
        if ("CHECK_IN".equals(newStatus)) {
            validateCheckInPrerequisites(targetSub);
        }

        // 체크아웃: 마지막 활성 Leg인 경우에만 결제 잔액 검증
        if ("CHECKED_OUT".equals(newStatus)) {
            boolean isLastActiveLeg = master.getSubReservations().stream()
                .filter(s -> !s.getId().equals(targetSub.getId()))
                .filter(s -> !"CANCELED".equals(s.getRoomReservationStatus())
                          && !"CHECKED_OUT".equals(s.getRoomReservationStatus())
                          && !"NO_SHOW".equals(s.getRoomReservationStatus()))
                .findAny().isEmpty();

            if (isLastActiveLeg) {
                validateCheckOutBalance(master);
            }
        }

        // Leg 상태 변경 + 부수효과
        applyStatusChange(targetSub, newStatus);

    } else {
        // ── 전체 Leg 일괄 변경 (하위 호환) ──
        for (SubReservation sub : master.getSubReservations()) {
            if ("CANCELED".equals(sub.getRoomReservationStatus())) continue;
            Set<String> allowed = STATUS_TRANSITIONS.getOrDefault(sub.getRoomReservationStatus(), Set.of());
            if (allowed.contains(newStatus)) {
                applyStatusChange(sub, newStatus);
            }
        }
    }

    // 마스터 상태 자동 도출
    String derivedStatus = deriveMasterStatus(master.getSubReservations());
    master.updateStatus(derivedStatus);

    // 얼리/레이트 요금 발생 시 결제 재계산
    if ("CHECK_IN".equals(newStatus) || "CHECKED_OUT".equals(newStatus)) {
        paymentService.recalculatePayment(master.getId());
    }
}

/**
 * 개별 Leg 상태 변경 + 부수효과 (체크인/아웃 시 객실 상태 등)
 */
private void applyStatusChange(SubReservation sub, String newStatus) {
    sub.updateStatus(newStatus);
    LocalDateTime now = LocalDateTime.now();

    if ("CHECK_IN".equals(newStatus)) {
        BigDecimal earlyFee = earlyLateCheckService.calculateEarlyCheckInFee(sub, now);
        sub.recordCheckIn(now, earlyFee);
        if (sub.getRoomNumberId() != null) {
            RoomNumber room = roomNumberRepository.findById(sub.getRoomNumberId()).orElse(null);
            if (room != null) room.checkIn();
        }
    }

    if ("CHECKED_OUT".equals(newStatus)) {
        BigDecimal lateFee = earlyLateCheckService.calculateLateCheckOutFee(sub, now);
        sub.recordCheckOut(now, lateFee);
        if (sub.getRoomNumberId() != null) {
            RoomNumber room = roomNumberRepository.findById(sub.getRoomNumberId()).orElse(null);
            if (room != null) room.checkOut();
        }
    }
}
```

### Phase 2: 프론트엔드 — 운영현황 (Leg별 액션)

| # | 파일 | 변경 내용 |
|---|------|---------|
| 2-1 | `fd-operations-page.js` | `changeStatus()`에 `subReservationId` 전달 |
| 2-2 | `fd-operations-page.js` | `renderAction()`에서 `row.subReservationId` 사용 |
| 2-3 | `fd-operations-page.js` | 모달 액션 버튼에도 subReservationId 반영 |

**변경 핵심**: 운영현황 데이터는 이미 Leg 단위 (`FrontDeskOperationResponse`에 `subReservationId` 포함). API 호출 시 `subReservationId`를 함께 전달하면 됨.

```javascript
// 기존
changeStatus: function(reservationId, newStatus, message) {
    HolaPms.ajax({
        data: { newStatus: newStatus }
    });
}

// 변경
changeStatus: function(reservationId, newStatus, message, subReservationId) {
    HolaPms.ajax({
        data: { newStatus: newStatus, subReservationId: subReservationId || null }
    });
}
```

### Phase 3: 프론트엔드 — 예약 상세 페이지 (Leg별 상태 표시/변경)

| # | 파일 | 변경 내용 |
|---|------|---------|
| 3-1 | `reservation-detail.js` | 각 Leg 카드에 상태 뱃지 + 상태 변경 드롭다운 추가 |
| 3-2 | `reservation-detail.js` | `buildStatusChangeMenu()`를 Leg별로 호출 |
| 3-3 | `reservation-detail.js` | `changeStatus()`에 subReservationId 전달 |
| 3-4 | `reservation-detail.html` | Leg 카드에 상태 변경 UI 영역 추가 |

**UI 변경 이미지**:

```
┌─ 객실 #1 (GMP260318-0005-01) ──── [투숙중] ──── [상태 변경 ▾] ──── [↑ 업그레이드] [✕]
│  객실타입: RYL-K        층/호수: 미배정
│  체크인: 2026-03-18     체크아웃: 2026-03-19
│  ...
└────────────────────────────────────────────────────────────

┌─ 객실 #2 (GMP260318-0005-03) ──── [예약] ──── [상태 변경 ▾] ──── [↑ 업그레이드] [✕]
│  객실타입: ECO-S        층/호수: 3F / 302
│  체크인: 2026-03-18     체크아웃: 2026-03-23
│  ...
└────────────────────────────────────────────────────────────

마스터 상태: [투숙중] (자동 도출 — 수정 불가)
```

### Phase 4: 결제 로직 조정

| # | 파일 | 변경 내용 |
|---|------|---------|
| 4-1 | `ReservationPaymentServiceImpl.java` | 결제/조정 가드 — 마지막 Leg 체크아웃 시에만 차단 |
| 4-2 | `ReservationServiceImpl.java` | 체크아웃 잔액 검증 — 마지막 활성 Leg 판별 |

### Phase 5: 테스트 업데이트

| # | 파일 | 변경 내용 |
|---|------|---------|
| 5-1 | `ReservationServiceImplTest.java` | changeStatus 테스트 — Leg별 전이 + deriveMasterStatus 테스트 |
| 5-2 | `ReservationApiIntegrationTest.java` | 통합 테스트 — Leg별 체크인/아웃 시나리오 |

---

## 4. 영향 분석 — 변경 불필요 파일

| 파일 | 이유 |
|------|------|
| `SubReservationRepository.java` | 쿼리가 이미 `roomReservationStatus` (Leg 상태) 기준 → 변경 불필요 |
| `DashboardServiceImpl.java` | SubReservationRepository 의존 → 변경 불필요 |
| `FrontDeskServiceImpl.java` | SubReservationRepository 의존 → 변경 불필요 |
| `RoomAvailabilityService.java` | `roomReservationStatus` 기준 → 변경 불필요 |
| `BookingServiceImpl.java` | 신규 예약 생성 시 RESERVED → 변경 불필요 |
| `RoomUpgradeServiceImpl.java` | Leg 상태 기준 → 변경 불필요 (단, 마스터 상태 참조 부분 확인) |

---

## 5. 리스크 및 주의사항

| # | 리스크 | 대응 |
|---|--------|------|
| R1 | 마스터 상태가 파생값이 되면, 마스터 상태를 직접 검사하는 코드가 오동작할 수 있음 | 전수 검색 완료 — 13개 참조점 확인 (§3 영향분석 참조) |
| R2 | 전체 Leg 일괄 변경 시 일부 Leg가 전이 불가하면 어떻게 할 것인가 | 전이 가능한 Leg만 변경, 불가한 Leg는 스킵 (에러 아님) |
| R3 | 결제 잔액 검증 시점 — Leg별 체크아웃이면 마지막 Leg까지 결제 유예 가능 | 마지막 활성 Leg 체크아웃 시에만 잔액 검증 |
| R4 | 예약 상세 페이지의 마스터 "상태 변경" 드롭다운 처리 | 마스터 레벨 드롭다운 유지 (일괄 변경) + Leg별 드롭다운 추가 |
| R5 | 노쇼 처리 — 전체 예약 노쇼 vs Leg별 노쇼 | 노쇼는 마스터 단위 유지 (전체 Leg 일괄) |

---

## 6. 구현 순서 및 일정

```
Phase 1 (Day 1): 백엔드 코어
  ├── ReservationStatusRequest + subReservationId
  ├── changeStatus() 리팩토링
  ├── deriveMasterStatus() 구현
  ├── applyStatusChange() 분리
  └── compileJava 검증

Phase 2 (Day 1): 운영현황 프론트
  ├── fd-operations-page.js — subReservationId 전달
  └── 모달 — Leg별 액션 버튼

Phase 3 (Day 2): 예약 상세 프론트
  ├── reservation-detail.js — Leg별 상태 뱃지
  ├── Leg별 상태 변경 드롭다운
  └── 마스터 상태 표시 변경 (파생값 표시)

Phase 4 (Day 2): 결제/테스트
  ├── 결제 가드 조건 조정
  └── 테스트 업데이트

Phase 5 (Day 2): E2E 검증
  └── 시나리오 테스트 (S1~S4)
```

---

## 7. 검증 시나리오

| # | 시나리오 | 기대 결과 |
|---|---------|---------|
| T1 | Leg 1개 예약 → 체크인 → 투숙중 → 체크아웃 | 기존과 동일 동작 (하위 호환) |
| T2 | Leg 2개 예약 → 전체 체크인 | subReservationId 없이 호출 → 두 Leg 모두 체크인. 마스터=CHECK_IN |
| T3 | Leg 2개 중 1개만 체크인 | subReservationId 지정 → Leg1=CHECK_IN, Leg2=RESERVED. 마스터=CHECK_IN |
| T4 | 투숙중 마스터에 Leg 추가 | 새 Leg = 마스터 상태 상속 (INHOUSE) |
| T5 | Leg 2개 중 1개만 체크아웃 | Leg1=CHECKED_OUT, Leg2=INHOUSE. 마스터=INHOUSE. 결제 잔액 검증 스킵 |
| T6 | 마지막 Leg 체크아웃 | 결제 잔액 검증 실행. 마스터=CHECKED_OUT |
| T7 | 전체 취소 | 모든 Leg=CANCELED. 마스터=CANCELED |
| T8 | Leg별 상태가 다를 때 예약 상세 UI | 각 Leg에 독립 상태 뱃지 + 상태 변경 드롭다운 표시 |
| T9 | 운영현황에서 Leg별 액션 | ECO-S(예약)에는 "체크인", RYL-K(투숙중)에는 "체크아웃" 표시 |

---

*이 Plan이 확정되면 Phase 1부터 구현 시작.*
