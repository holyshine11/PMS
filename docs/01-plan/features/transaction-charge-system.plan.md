---
feature: transaction-charge-system
phase: plan
createdAt: 2026-03-16
level: Enterprise
module: hola-room (M02) + hola-rate (M03) + hola-reservation (M07)
---

# Plan: 트랜잭션 코드 & 패키지 체계 (Transaction/Charge System)

## 1. 개요

### 1.1 목적
Opera PMS 수준의 트랜잭션 코드(Transaction Code) + 패키지 코드(Package Code) 체계를 도입하여,
3~5성급 호텔의 다양한 부과 항목(엑스트라 베드, 미니바, 객실 업그레이드, 레이트 체크아웃 등)을
객실타입 독립적으로 관리하고, 객실타입별 차등 가격/가용성을 지원한다.

### 1.2 배경
- **현재 문제**: PaidServiceOption이 "서비스 정의 + 가격"을 1:1로 통합하여, 같은 서비스(엑스트라 베드)라도 객실타입별 가격 차등 불가
- **현재 문제**: RoomTypePaidService가 선택 가능 서비스를 필터링만 하고, 가격/수량/가용성 오버라이드 불가
- **현재 문제**: 재고 개념 없음 (엑스트라 베드 재고 추적 불가)
- **현재 문제**: 객실 업그레이드 메커니즘 미구현
- **Opera 참조**: Transaction Code(회계 계정) → Package Code(번들/규칙) → Reservation Posting 3레이어 구조
- **대상 고객**: 3~5성급 호텔 — 복잡한 요금 체계, 다양한 부과 항목, 정산/회계 연동 필수

### 1.3 범위

| 포함 | 제외 |
|------|------|
| Transaction Code 마스터 (프로퍼티 레벨) | Folio/Window 개념 (Phase 2 이후) |
| Transaction Code Group 계층 구조 | Charge Routing (Phase 2 이후) |
| PaidServiceOption → Package Code 확장 | POS 연동 (M06) |
| 객실타입별 가격 오버라이드 | End of Day 배치 자동 포스팅 (Phase 2) |
| 재고 관리 (엑스트라 베드/유아용 침대) | AR(Accounts Receivable) Ledger |
| 객실 업그레이드 (차액 계산 + 이력) | 그룹 예약 차지 분배 |
| Admin CRUD + 부킹엔진 연동 | 외부 결제 PG 연동 확장 |
| 기존 데이터 마이그레이션 | Night Audit 자동화 (Phase 2) |

---

## 2. 현재 구현 분석

### 2.1 기존 엔티티 구조

```
PaidServiceOption (rm_paid_service_option)
├─ propertyId, serviceOptionCode (unique)
├─ serviceType: VARCHAR(20) — 20종 (ROOM_UPGRADE, BED_EXTRA, BREAKFAST_PAID 등)
├─ applicableNights: FIRST_NIGHT_ONLY / ALL_NIGHTS / NOT_APPLICABLE
├─ 가격: supplyPrice, taxRate, taxAmount, vatIncludedPrice (단일 가격)
├─ quantity, quantityUnit
└─ BaseEntity 상속

FreeServiceOption (rm_free_service_option)
├─ propertyId, serviceOptionCode (unique)
├─ serviceType: VARCHAR(20) — 16종 (BED, VIEW, FLOOR 등)
├─ applicableNights
└─ 가격 없음 (무료)

RoomTypePaidService (rm_room_type_paid_service)
├─ roomTypeId, paidServiceOptionId (unique)
└─ quantity (기본 1) — 가격 오버라이드 없음

RateCodePaidService (rt_rate_code_paid_service)
├─ rateCodeId, paidServiceOptionId (unique)
└─ 레이트 포함 시 0원으로 자동 추가

ReservationServiceItem (rsv_reservation_service)
├─ subReservationId (FK)
├─ serviceType: PAID / RATE_INCLUDED / FREE
├─ serviceOptionId (PaidServiceOption 또는 FreeServiceOption ID)
├─ serviceDate, quantity, unitPrice, tax, totalPrice
└─ 경량 엔티티 (BaseEntity 미상속)

DailyCharge (rsv_daily_charge)
├─ subReservationId, chargeDate (unique)
└─ supplyPrice, tax, serviceCharge, total (객실 요금만)
```

### 2.2 현재 한계

| 항목 | 현재 상태 | 필요 상태 |
|------|-----------|-----------|
| 요금 분류 체계 | serviceType 문자열 (플랫) | 계층 구조 (Main→Sub→Code) |
| 객실타입별 가격 | 불가 (단일 가격) | 오버라이드 테이블 |
| 엑스트라 베드 | PaidServiceOption으로 관리 | 재고 추적 + 객실타입별 가용/가격 |
| 객실 업그레이드 | 미구현 | 차액 계산 + 이력 관리 |
| 부과 빈도 | applicableNights (3종) | Per Night/Per Stay/One Time/Custom |
| 재고 관리 | 없음 | 일자별 가용 수량 추적 |
| 회계 연동 | 없음 | Transaction Code → Revenue Group 매핑 |

---

## 3. 목표 아키텍처

### 3.1 Opera 참조 모델 (3레이어)

```
[Layer 1] Transaction Code — "무엇을 부과하는가" (회계 계정)
    ↓ (1:N)
[Layer 2] Package Code — "어떻게 부과하는가" (가격 + 빈도 + 재고)
    ↓ (N:M)
[Layer 3] Reservation Posting — "누구에게 부과되었는가" (예약 연결)
```

### 3.2 Hola PMS 적용 모델

기존 코드를 최대한 활용하면서 Opera 3레이어를 단계적으로 도입한다.

```
[신규] TransactionCode (트랜잭션 코드)
  ├─ code, nameKo, nameEn
  ├─ transactionGroupId (FK → TransactionCodeGroup)
  ├─ revenueCategory: LODGING / FOOD_BEVERAGE / MISC / TAX / NON_REVENUE
  └─ codeType: CHARGE (부과) / PAYMENT (결제)

[신규] TransactionCodeGroup (트랜잭션 코드 그룹)
  ├─ groupCode, groupNameKo, groupNameEn
  ├─ groupType: MAIN / SUB
  └─ parentGroupId (self-referencing, MAIN→SUB 계층)

[확장] PaidServiceOption → "PackageCode" 역할 흡수
  ├─ (기존 필드 유지)
  ├─ + transactionCodeId (FK → TransactionCode)
  ├─ + postingFrequency: PER_NIGHT / PER_STAY / ONE_TIME (applicableNights 대체)
  ├─ + inventoryItemId (FK → InventoryItem, nullable)
  ├─ + packageScope: PROPERTY_WIDE / ROOM_TYPE_SPECIFIC
  └─ + sellSeparately: Boolean (개별 판매 가능 여부)

[확장] RoomTypePaidService → 가격/가용성 오버라이드
  ├─ (기존 필드 유지)
  ├─ + overridePrice (BigDecimal, nullable — null이면 기본가)
  ├─ + maxQuantity (Integer — 객실타입별 최대 수량)
  └─ + available (Boolean — 해당 객실타입 가용 여부)

[신규] InventoryItem (재고 아이템)
  ├─ propertyId, itemCode, itemNameKo, itemNameEn
  ├─ itemType: EXTRA_BED / CRIB / ROLLAWAY / EQUIPMENT
  ├─ totalQuantity (총 보유 수량)
  └─ BaseEntity 상속

[신규] InventoryAvailability (일자별 재고)
  ├─ inventoryItemId (FK)
  ├─ availabilityDate
  ├─ availableCount (가용 수량)
  └─ reservedCount (예약 수량)

[확장] ReservationServiceItem → transactionCodeId 추가
  ├─ (기존 필드 유지)
  ├─ + transactionCodeId (FK → TransactionCode, nullable — 점진 마이그레이션)
  └─ + postingStatus: POSTED / PENDING / VOIDED (향후 Night Audit용)

[신규] RoomUpgradeHistory (객실 업그레이드 이력)
  ├─ subReservationId (FK)
  ├─ fromRoomTypeId, toRoomTypeId
  ├─ upgradedAt (LocalDateTime)
  ├─ upgradeType: COMPLIMENTARY / PAID / UPSELL
  ├─ priceDifference (BigDecimal)
  └─ reason (String)
```

### 3.3 전체 관계도

```
TransactionCodeGroup (Main)
  └─ TransactionCodeGroup (Sub)
       └─ TransactionCode
            └─ PaidServiceOption (= PackageCode)
                 ├─ RoomTypePaidService (객실타입별 가격/가용)
                 ├─ RateCodePaidService (레이트 포함)
                 ├─ InventoryItem (재고 연결)
                 └─ ReservationServiceItem (예약 포스팅)

SubReservation
  ├─ DailyCharge (일별 객실 요금)
  ├─ ReservationServiceItem (부가 서비스/차지)
  └─ RoomUpgradeHistory (업그레이드 이력)
```

---

## 4. 구현 단계 (Phase 분할)

### Phase 1: Transaction Code 마스터 (우선순위: HIGH)

**목표**: 부과 항목의 회계적 분류 체계 수립

| 작업 | 모듈 | 설명 |
|------|------|------|
| TransactionCodeGroup 엔티티/CRUD | hola-room (M02) | Main→Sub 2단계 계층 |
| TransactionCode 엔티티/CRUD | hola-room (M02) | 프로퍼티별 트랜잭션 코드 |
| Admin UI (리스트/등록/수정) | hola-app | 트랜잭션 코드 관리 화면 |
| Flyway 마이그레이션 | hola-app | 테이블 + 초기 데이터 |

**초기 데이터 (예시)**:
```
MAIN GROUP: LODGING (숙박)
  SUB GROUP: ROOM_CHARGE (객실 요금)
    TC: 1000 Room Revenue (객실 매출)
    TC: 1010 Extra Bed (엑스트라 베드)
    TC: 1020 Room Upgrade (객실 업그레이드)
    TC: 1030 Late Checkout (레이트 체크아웃)
    TC: 1040 Early Checkin (얼리 체크인)

MAIN GROUP: FOOD_BEVERAGE (식음)
  SUB GROUP: RESTAURANT (레스토랑)
    TC: 2000 Breakfast (조식)
    TC: 2010 Lunch (중식)
    TC: 2020 Dinner (석식)
  SUB GROUP: MINIBAR (미니바)
    TC: 2100 Minibar Consumption (미니바 소비)
  SUB GROUP: ROOM_SERVICE (룸서비스)
    TC: 2200 Room Service Food (룸서비스 음식)
    TC: 2210 Room Service Beverage (룸서비스 음료)

MAIN GROUP: MISCELLANEOUS (기타)
  SUB GROUP: LAUNDRY (세탁)
    TC: 5000 Laundry Service (세탁 서비스)
  SUB GROUP: SPA (스파)
    TC: 5100 Spa Treatment (스파 트리트먼트)
  SUB GROUP: TRANSPORT (교통)
    TC: 5200 Airport Transfer (공항 셔틀)
  SUB GROUP: PARKING (주차)
    TC: 5300 Parking Fee (주차 요금)
  SUB GROUP: DAMAGE (손해)
    TC: 5400 Damage Fee (손해 배상)
```

**예상 영향 파일**: 6개 (신규 엔티티 2개, Repository 2개, Service 1개, Controller 1개)
**Flyway**: V6_1_0

---

### Phase 2: PaidServiceOption 확장 (우선순위: HIGH)

**목표**: 기존 PaidServiceOption에 TransactionCode 연결 + PackageCode 역할 부여

| 작업 | 모듈 | 설명 |
|------|------|------|
| PaidServiceOption 필드 추가 | hola-room | transactionCodeId, postingFrequency, packageScope, sellSeparately |
| RoomTypePaidService 필드 추가 | hola-room | overridePrice, maxQuantity, available |
| PaidServiceOptionService 수정 | hola-room | 조회 시 객실타입별 가격 반환 로직 |
| Admin UI 수정 | hola-app | 서비스 등록/수정 폼에 TC 선택, 스코프 설정 |
| 기존 데이터 마이그레이션 | hola-app | 기존 serviceType → TransactionCode 매핑 |
| Flyway 마이그레이션 | hola-app | ALTER TABLE + 데이터 마이그레이션 |

**applicableNights → postingFrequency 매핑**:
```
FIRST_NIGHT_ONLY → ONE_TIME (or PER_STAY)
ALL_NIGHTS       → PER_NIGHT
NOT_APPLICABLE   → ONE_TIME
```

**예상 영향 파일**: 12개 (엔티티 2개, DTO 4개, Service 2개, Controller 1개, Mapper 1개, SQL 1개, JS 1개)
**Flyway**: V6_2_0

---

### Phase 3: 재고 관리 (우선순위: MEDIUM)

**목표**: 엑스트라 베드/유아용 침대 등 물리적 아이템 재고 추적

| 작업 | 모듈 | 설명 |
|------|------|------|
| InventoryItem 엔티티/CRUD | hola-room | 재고 아이템 마스터 |
| InventoryAvailability 엔티티 | hola-room | 일자별 가용 수량 |
| PaidServiceOption ↔ InventoryItem 연결 | hola-room | inventoryItemId FK |
| InventoryService (가용 확인/차감) | hola-room | 예약 시 재고 차감, 취소 시 복원 |
| Admin UI (재고 관리 화면) | hola-app | 아이템 등록, 수량 설정, 가용성 조회 |
| BookingService 연동 | hola-reservation | 예약 생성 시 재고 확인/차감 |
| Flyway 마이그레이션 | hola-app | 신규 테이블 |

**예상 영향 파일**: 10개
**Flyway**: V6_3_0

---

### Phase 4: 객실 업그레이드 (우선순위: MEDIUM)

**목표**: 객실타입 변경 + 차액 자동 계산 + 이력 관리

| 작업 | 모듈 | 설명 |
|------|------|------|
| RoomUpgradeHistory 엔티티 | hola-reservation | 업그레이드 이력 |
| RoomUpgradeService | hola-reservation | 업그레이드 로직 (차액 계산) |
| PriceCalculationService 연동 | hola-reservation | 새 roomType 기준 DailyCharge 재계산 |
| ReservationServiceImpl 수정 | hola-reservation | 업그레이드 API 엔드포인트 |
| Admin UI (업그레이드 버튼) | hola-app | 예약 상세에서 업그레이드 실행 |
| Flyway 마이그레이션 | hola-app | 신규 테이블 |

**업그레이드 처리 흐름**:
```
1. 대상 객실타입 선택 (가용 확인)
2. 차액 계산: 새 RateCode DailyCharge - 기존 DailyCharge
3. upgradeType 결정: COMPLIMENTARY(무료) / PAID(유료) / UPSELL
4. DailyCharge 재생성 (새 객실타입 기준)
5. SubReservation.roomTypeId 업데이트
6. RoomUpgradeHistory 기록
7. PAID인 경우 ReservationServiceItem에 TC:1020(Room Upgrade) 차지 추가
```

**예상 영향 파일**: 8개
**Flyway**: V6_4_0

---

### Phase 5: 예약 서비스 연동 강화 (우선순위: MEDIUM)

**목표**: ReservationServiceItem에 TransactionCode 연결 + 부킹엔진 연동

| 작업 | 모듈 | 설명 |
|------|------|------|
| ReservationServiceItem 확장 | hola-reservation | transactionCodeId, postingStatus 추가 |
| BookingServiceImpl 수정 | hola-reservation | 서비스 추가 시 TC 연결, 객실타입별 가격 적용 |
| RateIncludedServiceHelper 수정 | hola-reservation | TC 기반 포함 서비스 처리 |
| 부킹엔진 UI 수정 | hola-app | 객실타입별 가격 표시, 재고 가용성 표시 |
| 기존 데이터 마이그레이션 | hola-app | 기존 serviceOptionId → transactionCodeId 역매핑 |
| Flyway 마이그레이션 | hola-app | ALTER TABLE |

**예상 영향 파일**: 10개
**Flyway**: V6_5_0

---

## 5. 데이터 마이그레이션 전략

### 5.1 기존 serviceType → TransactionCode 자동 매핑

```sql
-- PaidServiceOption.serviceType → TransactionCode 매핑
BED_EXTRA       → TC:1010 Extra Bed (LODGING > ROOM_CHARGE)
ROOM_UPGRADE    → TC:1020 Room Upgrade (LODGING > ROOM_CHARGE)
BREAKFAST_PAID  → TC:2000 Breakfast (FOOD_BEVERAGE > RESTAURANT)
MEAL            → TC:2010 Meal (FOOD_BEVERAGE > RESTAURANT)
MINIBAR         → TC:2100 Minibar (FOOD_BEVERAGE > MINIBAR)
ROOM_SERVICE    → TC:2200 Room Service (FOOD_BEVERAGE > ROOM_SERVICE)
SPA_WELLNESS    → TC:5100 Spa (MISC > SPA)
LAUNDRY         → TC:5000 Laundry (MISC > LAUNDRY)
TRANSFER_PAID   → TC:5200 Transfer (MISC > TRANSPORT)
PARKING_PAID    → TC:5300 Parking (MISC > PARKING)
AMENITY_PREMIUM → TC:5500 Premium Amenity (MISC > AMENITY)
(기타)           → TC:9000 Miscellaneous (MISC > OTHERS)
```

### 5.2 마이그레이션 원칙

- **하위 호환성 유지**: serviceType 컬럼 즉시 삭제하지 않음 (deprecated 마킹)
- **NULL 허용**: transactionCodeId는 nullable로 추가, 점진 매핑
- **Flyway 순서**: DDL(테이블 생성) → DML(데이터 매핑) → ALTER(컬럼 추가) 순

---

## 6. 리스크 & 대응

| 리스크 | 영향도 | 대응 |
|--------|--------|------|
| 기존 PaidServiceOption API 호환성 | HIGH | 기존 필드 유지 + 신규 필드 optional |
| 부킹엔진 서비스 선택 UI 변경 | MEDIUM | 점진적 교체 (기존 동작 보장 후 확장) |
| 재고 동시성 이슈 (동시 예약) | HIGH | 비관적 락 (SELECT FOR UPDATE) |
| 대규모 마이그레이션 데이터 불일치 | MEDIUM | 사전 검증 스크립트 + 롤백 SQL |
| serviceType deprecated 전환 기간 | LOW | 6개월 병행 후 제거 |

---

## 7. 구현 우선순위 & 일정 제안

```
Phase 1: Transaction Code 마스터       ← 최우선 (기반)
Phase 2: PaidServiceOption 확장        ← Phase 1 직후
  ── 여기까지가 MVP (핵심 가치 전달) ──
Phase 3: 재고 관리                     ← 선택적
Phase 4: 객실 업그레이드               ← 선택적
Phase 5: 예약 서비스 연동 강화         ← Phase 1~2 완료 후
```

**Phase 1+2 완료 시 해결되는 문제**:
- ✅ 엑스트라 베드 객실타입별 가격 차등
- ✅ 상시 추가요금 객실타입 독립 관리 (packageScope: PROPERTY_WIDE)
- ✅ 회계적 분류 체계 (Transaction Code Group)
- ✅ 부과 빈도 확장 (PER_NIGHT / PER_STAY / ONE_TIME)

**Phase 3+4 추가 시 해결되는 문제**:
- ✅ 엑스트라 베드 재고 추적
- ✅ 객실 업그레이드 + 차액 계산

---

## 8. 영향받는 파일 요약

### 신규 파일 (Phase 1~5 전체)

| 파일 | 모듈 | 설명 |
|------|------|------|
| TransactionCodeGroup.java | hola-room | 트랜잭션 코드 그룹 엔티티 |
| TransactionCode.java | hola-room | 트랜잭션 코드 엔티티 |
| TransactionCodeGroupRepository.java | hola-room | 그룹 리포지토리 |
| TransactionCodeRepository.java | hola-room | 코드 리포지토리 |
| TransactionCodeService.java | hola-room | 서비스 인터페이스 |
| TransactionCodeServiceImpl.java | hola-room | 서비스 구현 |
| TransactionCodeApiController.java | hola-room | API 컨트롤러 |
| InventoryItem.java | hola-room | 재고 아이템 엔티티 (Phase 3) |
| InventoryAvailability.java | hola-room | 일자별 재고 (Phase 3) |
| InventoryService.java | hola-room | 재고 서비스 (Phase 3) |
| RoomUpgradeHistory.java | hola-reservation | 업그레이드 이력 (Phase 4) |
| RoomUpgradeService.java | hola-reservation | 업그레이드 서비스 (Phase 4) |

### 수정 파일

| 파일 | 모듈 | 변경 내용 |
|------|------|-----------|
| PaidServiceOption.java | hola-room | transactionCodeId, postingFrequency, packageScope 추가 |
| RoomTypePaidService.java | hola-room | overridePrice, maxQuantity, available 추가 |
| PaidServiceOptionServiceImpl.java | hola-room | 객실타입별 가격 반환 로직 |
| PaidServiceOptionApiController.java | hola-room | API 응답에 TC 정보 포함 |
| ReservationServiceItem.java | hola-reservation | transactionCodeId, postingStatus 추가 |
| BookingServiceImpl.java | hola-reservation | 객실타입별 가격 적용, 재고 확인 |
| RateIncludedServiceHelper.java | hola-reservation | TC 기반 처리 |
| PriceCalculationService.java | hola-reservation | 업그레이드 차액 계산 |
| ErrorCode.java | hola-common | 신규 에러 코드 (HOLA-25xx 트랜잭션) |

### Flyway 마이그레이션

| 파일 | 내용 |
|------|------|
| V6_1_0__create_transaction_code_tables.sql | TC Group + TC 테이블 + 초기 데이터 |
| V6_2_0__extend_paid_service_option.sql | PaidServiceOption ALTER + RoomTypePaidService ALTER + 데이터 매핑 |
| V6_3_0__create_inventory_tables.sql | InventoryItem + InventoryAvailability 테이블 (Phase 3) |
| V6_4_0__create_room_upgrade_history.sql | RoomUpgradeHistory 테이블 (Phase 4) |
| V6_5_0__extend_reservation_service_item.sql | ReservationServiceItem ALTER (Phase 5) |

---

## 9. 성공 기준

| 기준 | 측정 방법 |
|------|-----------|
| TransactionCode Admin CRUD 정상 동작 | 등록/수정/삭제/조회 E2E |
| 객실타입별 서비스 가격 차등 적용 | 같은 서비스, 다른 객실타입 → 다른 가격 확인 |
| 엑스트라 베드 재고 차감/복원 | 예약 생성 시 -1, 취소 시 +1 확인 |
| 객실 업그레이드 차액 정확도 | 수동 계산 vs 시스템 계산 비교 |
| 기존 예약 데이터 정합성 | 마이그레이션 후 기존 예약 조회 정상 |
| 부킹엔진 서비스 선택 정상 | 객실타입별 가격/가용성 표시 확인 |
