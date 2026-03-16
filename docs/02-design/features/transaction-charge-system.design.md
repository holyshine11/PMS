---
feature: transaction-charge-system
phase: design
createdAt: 2026-03-16
level: Enterprise
module: hola-room (M02) + hola-rate (M03) + hola-reservation (M07)
planRef: docs/01-plan/features/transaction-charge-system.plan.md
---

# Design: 트랜잭션 코드 & 패키지 체계 (Transaction/Charge System)

## 1. ERD (Entity Relationship Diagram)

```
┌─────────────────────────────┐
│  rm_transaction_code_group  │
│  (트랜잭션 코드 그룹)       │
├─────────────────────────────┤
│ PK id                       │
│ FK property_id → htl_property│
│    group_code               │
│    group_name_ko            │
│    group_name_en            │
│    group_type (MAIN/SUB)    │
│ FK parent_group_id → self   │
│    sort_order, use_yn       │
│    BaseEntity 필드          │
└──────────┬──────────────────┘
           │ 1:N
           ▼
┌─────────────────────────────┐
│    rm_transaction_code      │
│    (트랜잭션 코드)          │
├─────────────────────────────┤
│ PK id                       │
│ FK property_id → htl_property│
│ FK transaction_group_id     │
│    → rm_transaction_code_group│
│    transaction_code         │
│    code_name_ko             │
│    code_name_en             │
│    revenue_category         │
│    (LODGING/FOOD_BEVERAGE/  │
│     MISC/TAX/NON_REVENUE)   │
│    code_type (CHARGE/PAYMENT)│
│    sort_order, use_yn       │
│    BaseEntity 필드          │
└──────────┬──────────────────┘
           │ 1:N
           ▼
┌─────────────────────────────┐       ┌──────────────────────────────┐
│  rm_paid_service_option     │       │    rm_inventory_item         │
│  (유료 서비스 = PackageCode)│       │    (재고 아이템)             │
├─────────────────────────────┤       ├──────────────────────────────┤
│ PK id                       │       │ PK id                        │
│ FK property_id              │       │ FK property_id               │
│ FK transaction_code_id (new)│       │    item_code                 │
│    → rm_transaction_code    │       │    item_name_ko              │
│    service_option_code      │       │    item_name_en              │
│    service_name_ko/en       │       │    item_type                 │
│    service_type (deprecated)│       │    management_type           │
│    posting_frequency (new)  │       │    (INTERNAL/EXTERNAL)       │
│    package_scope (new)      │       │    external_system_code      │
│    sell_separately (new)    │       │    total_quantity             │
│ FK inventory_item_id (new)  │       │    BaseEntity 필드           │
│    → rm_inventory_item      │       └──────────┬───────────────────┘
│    가격/세금 필드 (기존)    │                  │ 1:N
│    BaseEntity 필드          │                  ▼
└──┬───────────┬──────────────┘       ┌──────────────────────────────┐
   │           │                      │  rm_inventory_availability   │
   │ 1:N       │ 1:N                  │  (일자별 재고)               │
   │           │                      ├──────────────────────────────┤
   ▼           ▼                      │ PK id                        │
┌──────────┐ ┌────────────────┐       │ FK inventory_item_id         │
│rm_room_  │ │rt_rate_code_   │       │    availability_date         │
│type_paid │ │paid_service    │       │    available_count           │
│_service  │ │ (기존)         │       │    reserved_count            │
│(확장)    │ └────────────────┘       └──────────────────────────────┘
├──────────┤
│ PK id    │
│ room_type│
│ paid_svc │
│ quantity │
│ override_│       ┌─────────────────────────────┐
│  price   │       │  rsv_reservation_service    │
│  (new)   │       │  (예약 서비스 = Posting)    │
│ max_     │       ├─────────────────────────────┤
│  quantity│       │ PK id                       │
│  (new)   │       │ FK sub_reservation_id       │
│ available│       │ FK transaction_code_id (new)│
│  (new)   │       │    service_type             │
└──────────┘       │    service_option_id        │
                   │    posting_status (new)     │
                   │    service_date             │
                   │    quantity, unit_price     │
                   │    tax, total_price         │
                   │    created_at, updated_at   │
                   └─────────────────────────────┘

┌─────────────────────────────┐
│  rsv_room_upgrade_history   │
│  (객실 업그레이드 이력)     │
├─────────────────────────────┤
│ PK id                       │
│ FK sub_reservation_id       │
│ FK from_room_type_id        │
│ FK to_room_type_id          │
│    upgraded_at              │
│    upgrade_type             │
│    (COMPLIMENTARY/PAID/     │
│     UPSELL)                 │
│    price_difference         │
│    reason                   │
│    created_by, created_at   │
└─────────────────────────────┘
```

---

## 2. 엔티티 상세 설계

### 2.1 TransactionCodeGroup (신규)

**모듈**: hola-room
**테이블**: `rm_transaction_code_group`
**상속**: BaseEntity

```java
@Entity
@Table(name = "rm_transaction_code_group",
       uniqueConstraints = @UniqueConstraint(columnNames = {"property_id", "group_code"}))
@SQLRestriction("deleted_at IS NULL")
@Getter @Builder @NoArgsConstructor @AllArgsConstructor
public class TransactionCodeGroup extends BaseEntity {

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "group_code", nullable = false, length = 20)
    private String groupCode;               // LODGING, FOOD_BEVERAGE 등

    @Column(name = "group_name_ko", nullable = false, length = 100)
    private String groupNameKo;

    @Column(name = "group_name_en", length = 100)
    private String groupNameEn;

    @Column(name = "group_type", nullable = false, length = 10)
    private String groupType;               // MAIN / SUB

    @Column(name = "parent_group_id")
    private Long parentGroupId;             // SUB인 경우 MAIN 그룹 ID

    public void update(String groupNameKo, String groupNameEn, Integer sortOrder) {
        this.groupNameKo = groupNameKo;
        this.groupNameEn = groupNameEn;
        if (sortOrder != null) this.changeSortOrder(sortOrder);
    }
}
```

### 2.2 TransactionCode (신규)

**모듈**: hola-room
**테이블**: `rm_transaction_code`
**상속**: BaseEntity

```java
@Entity
@Table(name = "rm_transaction_code",
       uniqueConstraints = @UniqueConstraint(columnNames = {"property_id", "transaction_code"}))
@SQLRestriction("deleted_at IS NULL")
@Getter @Builder @NoArgsConstructor @AllArgsConstructor
public class TransactionCode extends BaseEntity {

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "transaction_group_id", nullable = false)
    private Long transactionGroupId;        // FK → TransactionCodeGroup (SUB 레벨)

    @Column(name = "transaction_code", nullable = false, length = 10)
    private String transactionCode;         // "1000", "1010", "2000" 등

    @Column(name = "code_name_ko", nullable = false, length = 200)
    private String codeNameKo;

    @Column(name = "code_name_en", length = 200)
    private String codeNameEn;

    @Column(name = "revenue_category", nullable = false, length = 20)
    private String revenueCategory;         // LODGING, FOOD_BEVERAGE, MISC, TAX, NON_REVENUE

    @Column(name = "code_type", nullable = false, length = 10)
    private String codeType;                // CHARGE / PAYMENT

    public void update(String codeNameKo, String codeNameEn, Long transactionGroupId,
                       String revenueCategory, Integer sortOrder) {
        this.codeNameKo = codeNameKo;
        this.codeNameEn = codeNameEn;
        this.transactionGroupId = transactionGroupId;
        this.revenueCategory = revenueCategory;
        if (sortOrder != null) this.changeSortOrder(sortOrder);
    }
}
```

### 2.3 PaidServiceOption 확장 (기존 + 신규 필드)

**추가 필드**:

```java
// === 신규 필드 (Phase 2) ===

@Column(name = "transaction_code_id")
private Long transactionCodeId;             // FK → TransactionCode (nullable, 점진 매핑)

@Column(name = "posting_frequency", length = 20)
private String postingFrequency;            // PER_NIGHT / PER_STAY / ONE_TIME
                                            // (applicableNights deprecated 후 대체)

@Column(name = "package_scope", length = 20, nullable = false)
@Builder.Default
private String packageScope = "PROPERTY_WIDE";  // PROPERTY_WIDE / ROOM_TYPE_SPECIFIC

@Column(name = "sell_separately", nullable = false)
@Builder.Default
private Boolean sellSeparately = true;      // 개별 판매 가능 여부

// === 신규 필드 (Phase 3) ===

@Column(name = "inventory_item_id")
private Long inventoryItemId;               // FK → InventoryItem (nullable)
```

**update() 메서드 확장**:
```java
public void update(/* 기존 파라미터 */,
                   Long transactionCodeId, String postingFrequency,
                   String packageScope, Boolean sellSeparately,
                   Long inventoryItemId) {
    // 기존 업데이트 로직 유지
    this.transactionCodeId = transactionCodeId;
    this.postingFrequency = postingFrequency;
    this.packageScope = packageScope != null ? packageScope : "PROPERTY_WIDE";
    this.sellSeparately = sellSeparately != null ? sellSeparately : true;
    this.inventoryItemId = inventoryItemId;
}
```

### 2.4 RoomTypePaidService 확장 (기존 + 신규 필드)

```java
// === 신규 필드 (Phase 2) ===

@Column(name = "override_price", precision = 15, scale = 2)
private BigDecimal overridePrice;           // null이면 기본가 사용

@Column(name = "max_quantity")
private Integer maxQuantity;                // 객실타입별 최대 수량 (null이면 무제한)

@Column(name = "available", nullable = false)
@Builder.Default
private Boolean available = true;           // 해당 객실타입 가용 여부
```

### 2.5 InventoryItem (신규 — Phase 3)

**모듈**: hola-room
**테이블**: `rm_inventory_item`
**상속**: BaseEntity

```java
@Entity
@Table(name = "rm_inventory_item",
       uniqueConstraints = @UniqueConstraint(columnNames = {"property_id", "item_code"}))
@SQLRestriction("deleted_at IS NULL")
@Getter @Builder @NoArgsConstructor @AllArgsConstructor
public class InventoryItem extends BaseEntity {

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "item_code", nullable = false, length = 30)
    private String itemCode;

    @Column(name = "item_name_ko", nullable = false, length = 200)
    private String itemNameKo;

    @Column(name = "item_name_en", length = 200)
    private String itemNameEn;

    @Column(name = "item_type", nullable = false, length = 20)
    private String itemType;                // EXTRA_BED, CRIB, ROLLAWAY, EQUIPMENT

    @Column(name = "management_type", nullable = false, length = 10)
    private String managementType;          // INTERNAL / EXTERNAL

    @Column(name = "external_system_code", length = 50)
    private String externalSystemCode;      // SAP 등 외부 시스템 아이템 코드 (EXTERNAL일 때)

    @Column(name = "total_quantity", nullable = false)
    @Builder.Default
    private Integer totalQuantity = 0;      // INTERNAL: 총 보유 수량, EXTERNAL: 참조용

    public void update(String itemNameKo, String itemNameEn, String itemType,
                       String managementType, String externalSystemCode,
                       Integer totalQuantity) {
        this.itemNameKo = itemNameKo;
        this.itemNameEn = itemNameEn;
        this.itemType = itemType;
        this.managementType = managementType;
        this.externalSystemCode = externalSystemCode;
        this.totalQuantity = totalQuantity != null ? totalQuantity : 0;
    }
}
```

### 2.6 InventoryAvailability (신규 — Phase 3)

**모듈**: hola-room
**테이블**: `rm_inventory_availability`
**상속**: 없음 (경량 엔티티)

```java
@Entity
@Table(name = "rm_inventory_availability",
       uniqueConstraints = @UniqueConstraint(columnNames = {"inventory_item_id", "availability_date"}))
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor @Builder
public class InventoryAvailability {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inventory_item_id", nullable = false)
    private Long inventoryItemId;           // FK → InventoryItem

    @Column(name = "availability_date", nullable = false)
    private LocalDate availabilityDate;

    @Column(name = "available_count", nullable = false)
    private Integer availableCount;         // 가용 수량

    @Column(name = "reserved_count", nullable = false)
    @Builder.Default
    private Integer reservedCount = 0;      // 예약된 수량

    // 재고 차감 (예약 시)
    public boolean reserve(int qty) {
        if (availableCount - reservedCount < qty) return false;
        this.reservedCount += qty;
        return true;
    }

    // 재고 복원 (취소 시)
    public void release(int qty) {
        this.reservedCount = Math.max(0, this.reservedCount - qty);
    }

    // 실제 가용 수량
    public int getRemainingCount() {
        return availableCount - reservedCount;
    }
}
```

### 2.7 ReservationServiceItem 확장 (기존 + 신규 필드)

```java
// === 신규 필드 (Phase 5) ===

@Column(name = "transaction_code_id")
private Long transactionCodeId;             // FK → TransactionCode (nullable)

@Column(name = "posting_status", length = 10)
@Builder.Default
private String postingStatus = "POSTED";    // POSTED / PENDING / VOIDED
```

### 2.8 RoomUpgradeHistory (신규 — Phase 4)

**모듈**: hola-reservation
**테이블**: `rsv_room_upgrade_history`
**상속**: 없음 (경량 엔티티)

```java
@Entity
@Table(name = "rsv_room_upgrade_history")
@EntityListeners(AuditingEntityListener.class)
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor @Builder
public class RoomUpgradeHistory {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sub_reservation_id", nullable = false)
    private Long subReservationId;          // FK → SubReservation

    @Column(name = "from_room_type_id", nullable = false)
    private Long fromRoomTypeId;

    @Column(name = "to_room_type_id", nullable = false)
    private Long toRoomTypeId;

    @Column(name = "upgraded_at", nullable = false)
    private LocalDateTime upgradedAt;

    @Column(name = "upgrade_type", nullable = false, length = 20)
    private String upgradeType;             // COMPLIMENTARY / PAID / UPSELL

    @Column(name = "price_difference", precision = 15, scale = 2)
    private BigDecimal priceDifference;     // 총 차액 (잔여 숙박일 기준)

    @Column(name = "reason", length = 500)
    private String reason;                  // 업그레이드 사유

    @CreatedBy
    @Column(name = "created_by", length = 50)
    private String createdBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
```

---

## 3. API 명세

### 3.1 Transaction Code Group API (Phase 1)

**Base Path**: `/api/v1/properties/{propertyId}/transaction-code-groups`

| Method | Path | 설명 | Request | Response |
|--------|------|------|---------|----------|
| GET | `/` | 그룹 트리 조회 | — | `List<TransactionCodeGroupTreeResponse>` |
| POST | `/` | 그룹 등록 | `TransactionCodeGroupCreateRequest` | `TransactionCodeGroupResponse` (201) |
| PUT | `/{id}` | 그룹 수정 | `TransactionCodeGroupUpdateRequest` | `TransactionCodeGroupResponse` |
| DELETE | `/{id}` | 그룹 삭제 (소프트) | — | `Void` |

**TransactionCodeGroupCreateRequest**:
```java
@Getter @NoArgsConstructor
public class TransactionCodeGroupCreateRequest {
    @NotBlank @Size(max = 20)
    private String groupCode;           // LODGING, FOOD_BEVERAGE 등

    @NotBlank @Size(max = 100)
    private String groupNameKo;

    @Size(max = 100)
    private String groupNameEn;

    @NotBlank
    private String groupType;           // MAIN / SUB

    private Long parentGroupId;         // SUB인 경우 필수

    private Integer sortOrder;
}
```

**TransactionCodeGroupTreeResponse**:
```java
@Getter @Builder @NoArgsConstructor @AllArgsConstructor
public class TransactionCodeGroupTreeResponse {
    private Long id;
    private String groupCode;
    private String groupNameKo;
    private String groupNameEn;
    private String groupType;
    private Integer sortOrder;
    private Boolean useYn;
    private List<TransactionCodeGroupTreeResponse> children;  // SUB 그룹 목록
}
```

### 3.2 Transaction Code API (Phase 1)

**Base Path**: `/api/v1/properties/{propertyId}/transaction-codes`

| Method | Path | 설명 | Request | Response |
|--------|------|------|---------|----------|
| GET | `/` | 코드 목록 조회 | `?groupId=&revenueCategory=` | `List<TransactionCodeResponse>` |
| GET | `/{id}` | 코드 상세 | — | `TransactionCodeResponse` |
| POST | `/` | 코드 등록 | `TransactionCodeCreateRequest` | `TransactionCodeResponse` (201) |
| PUT | `/{id}` | 코드 수정 | `TransactionCodeUpdateRequest` | `TransactionCodeResponse` |
| DELETE | `/{id}` | 코드 삭제 (소프트) | — | `Void` |
| GET | `/check-code` | 코드 중복 확인 | `?transactionCode=` | `Map<String, Boolean>` |
| GET | `/selector` | 드롭다운 선택자 | `?revenueCategory=` | `List<TransactionCodeSelectorResponse>` |

**TransactionCodeCreateRequest**:
```java
@Getter @NoArgsConstructor
public class TransactionCodeCreateRequest {
    @NotNull
    private Long transactionGroupId;

    @NotBlank @Size(max = 10)
    private String transactionCode;     // "1000", "1010" 등

    @NotBlank @Size(max = 200)
    private String codeNameKo;

    @Size(max = 200)
    private String codeNameEn;

    @NotBlank
    private String revenueCategory;     // LODGING, FOOD_BEVERAGE, MISC, TAX, NON_REVENUE

    @NotBlank
    private String codeType;            // CHARGE / PAYMENT

    private Integer sortOrder;
}
```

**TransactionCodeResponse**:
```java
@Getter @Builder @NoArgsConstructor @AllArgsConstructor
public class TransactionCodeResponse {
    private Long id;
    private Long propertyId;
    private Long transactionGroupId;
    private String transactionGroupNameKo;  // 조인 조회
    private String mainGroupNameKo;         // 상위 메인 그룹명
    private String transactionCode;
    private String codeNameKo;
    private String codeNameEn;
    private String revenueCategory;
    private String codeType;
    private Integer sortOrder;
    private Boolean useYn;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

**TransactionCodeSelectorResponse** (드롭다운용):
```java
@Getter @Builder @NoArgsConstructor @AllArgsConstructor
public class TransactionCodeSelectorResponse {
    private Long id;
    private String transactionCode;
    private String codeNameKo;
    private String revenueCategory;
}
```

### 3.3 PaidServiceOption 확장 API (Phase 2)

기존 API 유지, Request/Response에 신규 필드 추가:

**PaidServiceOptionCreateRequest 추가 필드**:
```java
private Long transactionCodeId;         // nullable (점진 매핑)
private String postingFrequency;        // PER_NIGHT / PER_STAY / ONE_TIME
private String packageScope;            // PROPERTY_WIDE / ROOM_TYPE_SPECIFIC (기본: PROPERTY_WIDE)
private Boolean sellSeparately;         // 기본: true
private Long inventoryItemId;           // nullable (Phase 3)
```

**PaidServiceOptionResponse 추가 필드**:
```java
private Long transactionCodeId;
private String transactionCodeName;     // 조인: TransactionCode.codeNameKo
private String transactionCodeValue;    // 조인: TransactionCode.transactionCode
private String postingFrequency;
private String packageScope;
private Boolean sellSeparately;
private Long inventoryItemId;
private String inventoryItemName;       // 조인: InventoryItem.itemNameKo (Phase 3)
```

### 3.4 RoomTypePaidService 확장 (Phase 2)

**기존 객실타입 수정 API 내에서 처리** (별도 API 없음):
객실타입 등록/수정 시 `paidServices` 배열에 확장 필드 포함.

```java
// RoomType 등록/수정 Request 내부
public static class PaidServiceMapping {
    @NotNull
    private Long paidServiceOptionId;
    private Integer quantity;           // 기본 1
    private BigDecimal overridePrice;   // null이면 기본가
    private Integer maxQuantity;        // null이면 무제한
    private Boolean available;          // 기본 true
}
```

### 3.5 Inventory API (Phase 3)

**Base Path**: `/api/v1/properties/{propertyId}/inventory-items`

| Method | Path | 설명 | Request | Response |
|--------|------|------|---------|----------|
| GET | `/` | 아이템 목록 | `?managementType=` | `List<InventoryItemResponse>` |
| GET | `/{id}` | 아이템 상세 | — | `InventoryItemResponse` |
| POST | `/` | 아이템 등록 | `InventoryItemCreateRequest` | `InventoryItemResponse` (201) |
| PUT | `/{id}` | 아이템 수정 | `InventoryItemUpdateRequest` | `InventoryItemResponse` |
| DELETE | `/{id}` | 아이템 삭제 | — | `Void` |
| GET | `/{id}/availability` | 일자별 가용성 | `?from=&to=` | `List<InventoryAvailabilityResponse>` |
| PUT | `/{id}/availability` | 가용수량 벌크 설정 | `InventoryAvailabilityBulkRequest` | `Void` |

**InventoryItemCreateRequest**:
```java
@Getter @NoArgsConstructor
public class InventoryItemCreateRequest {
    @NotBlank @Size(max = 30)
    private String itemCode;

    @NotBlank @Size(max = 200)
    private String itemNameKo;

    @Size(max = 200)
    private String itemNameEn;

    @NotBlank
    private String itemType;            // EXTRA_BED, CRIB, ROLLAWAY, EQUIPMENT

    @NotBlank
    private String managementType;      // INTERNAL / EXTERNAL

    @Size(max = 50)
    private String externalSystemCode;  // EXTERNAL일 때 SAP 아이템 코드

    @Min(0)
    private Integer totalQuantity;      // INTERNAL: 보유 수량, EXTERNAL: 참조용
}
```

### 3.6 Room Upgrade API (Phase 4)

**Base Path**: `/api/v1/properties/{propertyId}/reservations/{subReservationId}`

| Method | Path | 설명 | Request | Response |
|--------|------|------|---------|----------|
| GET | `/upgrade/available-types` | 업그레이드 가능 객실타입 | — | `List<UpgradeAvailableTypeResponse>` |
| GET | `/upgrade/preview` | 업그레이드 미리보기 | `?toRoomTypeId=` | `UpgradePreviewResponse` |
| POST | `/upgrade` | 업그레이드 실행 | `RoomUpgradeRequest` | `RoomUpgradeResponse` |
| GET | `/upgrade/history` | 업그레이드 이력 | — | `List<RoomUpgradeHistoryResponse>` |

**RoomUpgradeRequest**:
```java
@Getter @NoArgsConstructor
public class RoomUpgradeRequest {
    @NotNull
    private Long toRoomTypeId;

    @NotBlank
    private String upgradeType;         // COMPLIMENTARY / PAID / UPSELL

    @Size(max = 500)
    private String reason;
}
```

**UpgradePreviewResponse**:
```java
@Getter @Builder @NoArgsConstructor @AllArgsConstructor
public class UpgradePreviewResponse {
    private Long fromRoomTypeId;
    private String fromRoomTypeName;
    private Long toRoomTypeId;
    private String toRoomTypeName;
    private BigDecimal currentTotalCharge;      // 현재 잔여 숙박 총액
    private BigDecimal newTotalCharge;          // 업그레이드 후 잔여 숙박 총액
    private BigDecimal priceDifference;         // 차액
    private List<DailyChargeDiff> dailyDiffs;   // 일자별 차액 상세
}
```

---

## 4. 재고 관리 — 하이브리드 아키텍처 (Phase 3)

### 4.1 인터페이스 설계

```java
// 재고 관리 전략 인터페이스
public interface InventoryManagementStrategy {

    // 특정 일자 가용 수량 조회
    int getAvailableCount(Long inventoryItemId, LocalDate date);

    // 재고 차감 (예약 시)
    boolean reserve(Long inventoryItemId, LocalDate fromDate, LocalDate toDate, int quantity);

    // 재고 복원 (취소 시)
    void release(Long inventoryItemId, LocalDate fromDate, LocalDate toDate, int quantity);

    // 기간 내 가용성 조회
    List<InventoryAvailabilityResponse> getAvailability(Long inventoryItemId,
                                                         LocalDate from, LocalDate to);
}
```

### 4.2 INTERNAL 구현 (자체 관리)

```java
@Component("internalInventoryStrategy")
@RequiredArgsConstructor
public class InternalInventoryStrategy implements InventoryManagementStrategy {

    private final InventoryAvailabilityRepository availabilityRepository;

    @Override
    @Transactional
    public boolean reserve(Long inventoryItemId, LocalDate fromDate, LocalDate toDate, int quantity) {
        // 비관적 락으로 동시성 제어
        List<InventoryAvailability> availabilities =
            availabilityRepository.findByItemIdAndDateRangeForUpdate(
                inventoryItemId, fromDate, toDate);

        // 모든 일자에 충분한 재고 있는지 확인
        for (InventoryAvailability avail : availabilities) {
            if (avail.getRemainingCount() < quantity) {
                return false;   // 재고 부족
            }
        }

        // 차감
        for (InventoryAvailability avail : availabilities) {
            avail.reserve(quantity);
        }

        return true;
    }

    @Override
    @Transactional
    public void release(Long inventoryItemId, LocalDate fromDate, LocalDate toDate, int quantity) {
        List<InventoryAvailability> availabilities =
            availabilityRepository.findByItemIdAndDateRange(inventoryItemId, fromDate, toDate);

        for (InventoryAvailability avail : availabilities) {
            avail.release(quantity);
        }
    }
}
```

### 4.3 EXTERNAL 구현 (외부 ERP 어댑터)

```java
@Component("externalInventoryStrategy")
@RequiredArgsConstructor
public class ExternalInventoryStrategy implements InventoryManagementStrategy {

    // 향후 SAP/외부 시스템 연동 시 구현
    // 현재는 항상 가용으로 반환 (외부에서 관리)

    @Override
    public int getAvailableCount(Long inventoryItemId, LocalDate date) {
        // TODO: 외부 ERP API 호출
        return Integer.MAX_VALUE;   // 외부 관리 → 항상 가용
    }

    @Override
    public boolean reserve(Long inventoryItemId, LocalDate fromDate, LocalDate toDate, int quantity) {
        // TODO: 외부 ERP에 예약 통보
        log.info("외부 재고 시스템 예약 통보: itemId={}, from={}, to={}, qty={}",
                 inventoryItemId, fromDate, toDate, quantity);
        return true;
    }

    @Override
    public void release(Long inventoryItemId, LocalDate fromDate, LocalDate toDate, int quantity) {
        // TODO: 외부 ERP에 해제 통보
        log.info("외부 재고 시스템 해제 통보: itemId={}, from={}, to={}, qty={}",
                 inventoryItemId, fromDate, toDate, quantity);
    }
}
```

### 4.4 전략 라우터

```java
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryItemRepository inventoryItemRepository;
    private final InternalInventoryStrategy internalStrategy;
    private final ExternalInventoryStrategy externalStrategy;

    private InventoryManagementStrategy getStrategy(InventoryItem item) {
        return "EXTERNAL".equals(item.getManagementType())
               ? externalStrategy
               : internalStrategy;
    }

    @Override
    @Transactional
    public boolean reserveInventory(Long inventoryItemId, LocalDate from, LocalDate to, int qty) {
        InventoryItem item = findById(inventoryItemId);
        return getStrategy(item).reserve(inventoryItemId, from, to, qty);
    }

    @Override
    @Transactional
    public void releaseInventory(Long inventoryItemId, LocalDate from, LocalDate to, int qty) {
        InventoryItem item = findById(inventoryItemId);
        getStrategy(item).release(inventoryItemId, from, to, qty);
    }
}
```

---

## 5. 서비스 레이어 설계

### 5.1 TransactionCodeService

```java
public interface TransactionCodeService {
    // 그룹
    List<TransactionCodeGroupTreeResponse> getGroupTree(Long propertyId);
    TransactionCodeGroupResponse createGroup(Long propertyId, TransactionCodeGroupCreateRequest request);
    TransactionCodeGroupResponse updateGroup(Long id, TransactionCodeGroupUpdateRequest request);
    void deleteGroup(Long id);

    // 코드
    List<TransactionCodeResponse> getTransactionCodes(Long propertyId, Long groupId, String revenueCategory);
    TransactionCodeResponse getTransactionCode(Long id);
    TransactionCodeResponse createTransactionCode(Long propertyId, TransactionCodeCreateRequest request);
    TransactionCodeResponse updateTransactionCode(Long id, TransactionCodeUpdateRequest request);
    void deleteTransactionCode(Long id);
    boolean existsTransactionCode(Long propertyId, String transactionCode);
    List<TransactionCodeSelectorResponse> getSelector(Long propertyId, String revenueCategory);
}
```

### 5.2 InventoryService (Phase 3)

```java
public interface InventoryService {
    // CRUD
    List<InventoryItemResponse> getInventoryItems(Long propertyId, String managementType);
    InventoryItemResponse getInventoryItem(Long id);
    InventoryItemResponse createInventoryItem(Long propertyId, InventoryItemCreateRequest request);
    InventoryItemResponse updateInventoryItem(Long id, InventoryItemUpdateRequest request);
    void deleteInventoryItem(Long id);

    // 가용성
    List<InventoryAvailabilityResponse> getAvailability(Long itemId, LocalDate from, LocalDate to);
    void bulkSetAvailability(Long itemId, InventoryAvailabilityBulkRequest request);

    // 예약 연동
    boolean reserveInventory(Long inventoryItemId, LocalDate from, LocalDate to, int quantity);
    void releaseInventory(Long inventoryItemId, LocalDate from, LocalDate to, int quantity);
}
```

### 5.3 RoomUpgradeService (Phase 4)

```java
public interface RoomUpgradeService {
    List<UpgradeAvailableTypeResponse> getAvailableTypes(Long subReservationId);
    UpgradePreviewResponse previewUpgrade(Long subReservationId, Long toRoomTypeId);
    RoomUpgradeResponse executeUpgrade(Long subReservationId, RoomUpgradeRequest request);
    List<RoomUpgradeHistoryResponse> getUpgradeHistory(Long subReservationId);
}
```

**executeUpgrade 흐름**:
```
1. SubReservation 조회 + 상태 검증 (RESERVED/CHECK_IN/INHOUSE만 가능)
2. 대상 객실타입 가용성 확인
3. 남은 숙박일 기준 차액 계산 (PriceCalculationService)
4. 기존 DailyCharge 삭제 (남은 일자만)
5. 새 DailyCharge 생성 (새 객실타입 + 레이트 기준)
6. SubReservation.roomTypeId 업데이트
7. PAID 타입이면 ReservationServiceItem 추가 (TC:1020 Room Upgrade)
8. RoomUpgradeHistory 기록
9. 기존 재고 복원 + 새 재고 차감 (해당 시)
```

---

## 6. Flyway DDL 상세

### V6_1_0__create_transaction_code_tables.sql

```sql
-- 트랜잭션 코드 그룹
CREATE TABLE rm_transaction_code_group (
    id                  BIGSERIAL       PRIMARY KEY,
    property_id         BIGINT          NOT NULL REFERENCES htl_property(id),
    group_code          VARCHAR(20)     NOT NULL,
    group_name_ko       VARCHAR(100)    NOT NULL,
    group_name_en       VARCHAR(100),
    group_type          VARCHAR(10)     NOT NULL,      -- MAIN / SUB
    parent_group_id     BIGINT          REFERENCES rm_transaction_code_group(id),
    sort_order          INTEGER         NOT NULL DEFAULT 0,
    use_yn              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP,
    created_by          VARCHAR(50),
    updated_by          VARCHAR(50),
    deleted_at          TIMESTAMP,
    UNIQUE (property_id, group_code)
);

CREATE INDEX idx_rm_tc_group_property ON rm_transaction_code_group(property_id);
CREATE INDEX idx_rm_tc_group_parent ON rm_transaction_code_group(parent_group_id);

COMMENT ON TABLE rm_transaction_code_group IS '트랜잭션 코드 그룹';
COMMENT ON COLUMN rm_transaction_code_group.group_type IS 'MAIN: 대분류, SUB: 소분류';
COMMENT ON COLUMN rm_transaction_code_group.parent_group_id IS 'SUB 그룹의 상위 MAIN 그룹 ID';

-- 트랜잭션 코드
CREATE TABLE rm_transaction_code (
    id                      BIGSERIAL       PRIMARY KEY,
    property_id             BIGINT          NOT NULL REFERENCES htl_property(id),
    transaction_group_id    BIGINT          NOT NULL REFERENCES rm_transaction_code_group(id),
    transaction_code        VARCHAR(10)     NOT NULL,
    code_name_ko            VARCHAR(200)    NOT NULL,
    code_name_en            VARCHAR(200),
    revenue_category        VARCHAR(20)     NOT NULL,  -- LODGING, FOOD_BEVERAGE, MISC, TAX, NON_REVENUE
    code_type               VARCHAR(10)     NOT NULL DEFAULT 'CHARGE',  -- CHARGE / PAYMENT
    sort_order              INTEGER         NOT NULL DEFAULT 0,
    use_yn                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP,
    created_by              VARCHAR(50),
    updated_by              VARCHAR(50),
    deleted_at              TIMESTAMP,
    UNIQUE (property_id, transaction_code)
);

CREATE INDEX idx_rm_tc_property ON rm_transaction_code(property_id);
CREATE INDEX idx_rm_tc_group ON rm_transaction_code(transaction_group_id);
CREATE INDEX idx_rm_tc_category ON rm_transaction_code(revenue_category);

COMMENT ON TABLE rm_transaction_code IS '트랜잭션 코드';
COMMENT ON COLUMN rm_transaction_code.transaction_code IS '코드 번호 (예: 1000, 2000)';
COMMENT ON COLUMN rm_transaction_code.revenue_category IS '매출 분류 (LODGING/FOOD_BEVERAGE/MISC/TAX/NON_REVENUE)';
COMMENT ON COLUMN rm_transaction_code.code_type IS 'CHARGE: 부과, PAYMENT: 결제';

-- 초기 데이터는 프로퍼티별로 생성해야 하므로 별도 DML 마이그레이션에서 처리
-- 또는 프로퍼티 생성 시 기본 TC 세트 자동 생성 로직 추가
```

### V6_2_0__extend_paid_service_option.sql

```sql
-- PaidServiceOption 확장 (PackageCode 역할)
ALTER TABLE rm_paid_service_option
    ADD COLUMN transaction_code_id  BIGINT REFERENCES rm_transaction_code(id),
    ADD COLUMN posting_frequency    VARCHAR(20),
    ADD COLUMN package_scope        VARCHAR(20) NOT NULL DEFAULT 'PROPERTY_WIDE',
    ADD COLUMN sell_separately      BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX idx_rm_paid_svc_tc ON rm_paid_service_option(transaction_code_id);

COMMENT ON COLUMN rm_paid_service_option.transaction_code_id IS '트랜잭션 코드 FK (점진 매핑)';
COMMENT ON COLUMN rm_paid_service_option.posting_frequency IS 'PER_NIGHT/PER_STAY/ONE_TIME (applicableNights 대체)';
COMMENT ON COLUMN rm_paid_service_option.package_scope IS 'PROPERTY_WIDE: 전체/ROOM_TYPE_SPECIFIC: 객실타입 한정';
COMMENT ON COLUMN rm_paid_service_option.sell_separately IS '개별 판매 가능 여부';

-- RoomTypePaidService 확장 (가격/가용성 오버라이드)
ALTER TABLE rm_room_type_paid_service
    ADD COLUMN override_price   NUMERIC(15,2),
    ADD COLUMN max_quantity     INTEGER,
    ADD COLUMN available        BOOLEAN NOT NULL DEFAULT TRUE;

COMMENT ON COLUMN rm_room_type_paid_service.override_price IS '객실타입별 가격 오버라이드 (NULL이면 기본가)';
COMMENT ON COLUMN rm_room_type_paid_service.max_quantity IS '객실타입별 최대 수량 (NULL이면 무제한)';
COMMENT ON COLUMN rm_room_type_paid_service.available IS '해당 객실타입 가용 여부';

-- 기존 applicableNights → postingFrequency 데이터 매핑
UPDATE rm_paid_service_option
SET posting_frequency = CASE
    WHEN applicable_nights = 'ALL_NIGHTS'       THEN 'PER_NIGHT'
    WHEN applicable_nights = 'FIRST_NIGHT_ONLY' THEN 'ONE_TIME'
    WHEN applicable_nights = 'NOT_APPLICABLE'   THEN 'ONE_TIME'
    ELSE 'ONE_TIME'
END
WHERE posting_frequency IS NULL;
```

### V6_3_0__create_inventory_tables.sql (Phase 3)

```sql
-- 재고 아이템 마스터
CREATE TABLE rm_inventory_item (
    id                      BIGSERIAL       PRIMARY KEY,
    property_id             BIGINT          NOT NULL REFERENCES htl_property(id),
    item_code               VARCHAR(30)     NOT NULL,
    item_name_ko            VARCHAR(200)    NOT NULL,
    item_name_en            VARCHAR(200),
    item_type               VARCHAR(20)     NOT NULL,       -- EXTRA_BED, CRIB, ROLLAWAY, EQUIPMENT
    management_type         VARCHAR(10)     NOT NULL DEFAULT 'INTERNAL',  -- INTERNAL / EXTERNAL
    external_system_code    VARCHAR(50),                     -- SAP 등 외부 시스템 코드
    total_quantity          INTEGER         NOT NULL DEFAULT 0,
    sort_order              INTEGER         NOT NULL DEFAULT 0,
    use_yn                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP,
    created_by              VARCHAR(50),
    updated_by              VARCHAR(50),
    deleted_at              TIMESTAMP,
    UNIQUE (property_id, item_code)
);

CREATE INDEX idx_rm_inv_item_property ON rm_inventory_item(property_id);

COMMENT ON TABLE rm_inventory_item IS '재고 아이템 마스터';
COMMENT ON COLUMN rm_inventory_item.management_type IS 'INTERNAL: 자체관리, EXTERNAL: 외부 ERP 연동';
COMMENT ON COLUMN rm_inventory_item.external_system_code IS '외부 시스템(SAP 등) 아이템 식별 코드';

-- 일자별 재고 가용성
CREATE TABLE rm_inventory_availability (
    id                  BIGSERIAL       PRIMARY KEY,
    inventory_item_id   BIGINT          NOT NULL REFERENCES rm_inventory_item(id),
    availability_date   DATE            NOT NULL,
    available_count     INTEGER         NOT NULL DEFAULT 0,
    reserved_count      INTEGER         NOT NULL DEFAULT 0,
    UNIQUE (inventory_item_id, availability_date)
);

CREATE INDEX idx_rm_inv_avail_item_date ON rm_inventory_availability(inventory_item_id, availability_date);

COMMENT ON TABLE rm_inventory_availability IS '일자별 재고 가용성';
COMMENT ON COLUMN rm_inventory_availability.available_count IS '총 가용 수량';
COMMENT ON COLUMN rm_inventory_availability.reserved_count IS '예약된 수량';

-- PaidServiceOption에 재고 연결 필드 추가
ALTER TABLE rm_paid_service_option
    ADD COLUMN inventory_item_id BIGINT REFERENCES rm_inventory_item(id);

COMMENT ON COLUMN rm_paid_service_option.inventory_item_id IS '연결된 재고 아이템 ID';
```

### V6_4_0__create_room_upgrade_history.sql (Phase 4)

```sql
CREATE TABLE rsv_room_upgrade_history (
    id                      BIGSERIAL       PRIMARY KEY,
    sub_reservation_id      BIGINT          NOT NULL REFERENCES rsv_sub_reservation(id),
    from_room_type_id       BIGINT          NOT NULL REFERENCES rm_room_type(id),
    to_room_type_id         BIGINT          NOT NULL REFERENCES rm_room_type(id),
    upgraded_at             TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    upgrade_type            VARCHAR(20)     NOT NULL,       -- COMPLIMENTARY, PAID, UPSELL
    price_difference        NUMERIC(15,2),
    reason                  VARCHAR(500),
    created_by              VARCHAR(50),
    created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_rsv_upgrade_sub ON rsv_room_upgrade_history(sub_reservation_id);

COMMENT ON TABLE rsv_room_upgrade_history IS '객실 업그레이드 이력';
COMMENT ON COLUMN rsv_room_upgrade_history.upgrade_type IS 'COMPLIMENTARY: 무료, PAID: 유료, UPSELL: 업셀';
COMMENT ON COLUMN rsv_room_upgrade_history.price_difference IS '잔여 숙박일 기준 총 차액';
```

### V6_5_0__extend_reservation_service_item.sql (Phase 5)

```sql
ALTER TABLE rsv_reservation_service
    ADD COLUMN transaction_code_id  BIGINT REFERENCES rm_transaction_code(id),
    ADD COLUMN posting_status       VARCHAR(10) NOT NULL DEFAULT 'POSTED';

CREATE INDEX idx_rsv_service_tc ON rsv_reservation_service(transaction_code_id);

COMMENT ON COLUMN rsv_reservation_service.transaction_code_id IS '트랜잭션 코드 FK';
COMMENT ON COLUMN rsv_reservation_service.posting_status IS 'POSTED/PENDING/VOIDED';

-- service_type 컬럼 사이즈 확장 (RATE_INCLUDED 이미 VARCHAR(20))
-- 추가 필요 없음 (V5_18_0에서 이미 20으로 확장됨)
```

---

## 7. 에러 코드 추가

```java
// Transaction Code (HOLA-25xx)
TRANSACTION_CODE_GROUP_NOT_FOUND("HOLA-2500", "트랜잭션 코드 그룹을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
TRANSACTION_CODE_GROUP_DUPLICATE("HOLA-2501", "동일 프로퍼티 내 이미 존재하는 그룹 코드입니다.", HttpStatus.CONFLICT),
TRANSACTION_CODE_GROUP_HAS_CHILDREN("HOLA-2502", "하위 그룹이 존재하여 삭제할 수 없습니다.", HttpStatus.BAD_REQUEST),
TRANSACTION_CODE_GROUP_HAS_CODES("HOLA-2503", "하위 트랜잭션 코드가 존재하여 삭제할 수 없습니다.", HttpStatus.BAD_REQUEST),
TRANSACTION_CODE_NOT_FOUND("HOLA-2510", "트랜잭션 코드를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
TRANSACTION_CODE_DUPLICATE("HOLA-2511", "동일 프로퍼티 내 이미 존재하는 트랜잭션 코드입니다.", HttpStatus.CONFLICT),
TRANSACTION_CODE_IN_USE("HOLA-2512", "사용 중인 트랜잭션 코드는 삭제할 수 없습니다.", HttpStatus.BAD_REQUEST),

// Inventory (HOLA-26xx)
INVENTORY_ITEM_NOT_FOUND("HOLA-2600", "재고 아이템을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
INVENTORY_ITEM_CODE_DUPLICATE("HOLA-2601", "동일 프로퍼티 내 이미 존재하는 아이템 코드입니다.", HttpStatus.CONFLICT),
INVENTORY_NOT_AVAILABLE("HOLA-2610", "해당 기간에 재고가 부족합니다.", HttpStatus.CONFLICT),
INVENTORY_AVAILABILITY_NOT_SET("HOLA-2611", "해당 기간의 재고 가용성이 설정되지 않았습니다.", HttpStatus.BAD_REQUEST),

// Room Upgrade (HOLA-41xx)
UPGRADE_NOT_ALLOWED("HOLA-4100", "현재 예약 상태에서는 업그레이드할 수 없습니다.", HttpStatus.BAD_REQUEST),
UPGRADE_SAME_ROOM_TYPE("HOLA-4101", "동일한 객실타입으로 업그레이드할 수 없습니다.", HttpStatus.BAD_REQUEST),
UPGRADE_ROOM_TYPE_NOT_AVAILABLE("HOLA-4102", "대상 객실타입에 가용 객실이 없습니다.", HttpStatus.CONFLICT),
UPGRADE_RATE_NOT_FOUND("HOLA-4103", "대상 객실타입에 적용 가능한 레이트가 없습니다.", HttpStatus.NOT_FOUND),
```

---

## 8. 구현 순서 (Phase별 파일 단위)

### Phase 1: Transaction Code 마스터

```
순서  파일                                    모듈
────  ──────────────────────────────────────  ──────────
1-1   V6_1_0__create_transaction_code_tables.sql  hola-app
1-2   TransactionCodeGroup.java               hola-room/entity
1-3   TransactionCode.java                    hola-room/entity
1-4   TransactionCodeGroupRepository.java     hola-room/repository
1-5   TransactionCodeRepository.java          hola-room/repository
1-6   TransactionCodeGroupResponse.java       hola-room/dto/response
1-7   TransactionCodeGroupTreeResponse.java   hola-room/dto/response
1-8   TransactionCodeGroupCreateRequest.java  hola-room/dto/request
1-9   TransactionCodeGroupUpdateRequest.java  hola-room/dto/request
1-10  TransactionCodeResponse.java            hola-room/dto/response
1-11  TransactionCodeSelectorResponse.java    hola-room/dto/response
1-12  TransactionCodeCreateRequest.java       hola-room/dto/request
1-13  TransactionCodeUpdateRequest.java       hola-room/dto/request
1-14  TransactionCodeMapper.java              hola-room/mapper
1-15  TransactionCodeService.java             hola-room/service (interface)
1-16  TransactionCodeServiceImpl.java         hola-room/service
1-17  TransactionCodeApiController.java       hola-room/controller
1-18  ErrorCode.java 추가 (HOLA-25xx)        hola-common
1-19  transaction-code.html (리스트)          hola-app/templates
1-20  transaction-code.js                     hola-app/static/js
```

### Phase 2: PaidServiceOption 확장

```
순서  파일                                    모듈
────  ──────────────────────────────────────  ──────────
2-1   V6_2_0__extend_paid_service_option.sql  hola-app
2-2   PaidServiceOption.java (필드 추가)      hola-room/entity
2-3   RoomTypePaidService.java (필드 추가)    hola-room/entity
2-4   PaidServiceOptionCreateRequest.java 수정 hola-room/dto
2-5   PaidServiceOptionUpdateRequest.java 수정 hola-room/dto
2-6   PaidServiceOptionResponse.java 수정      hola-room/dto
2-7   PaidServiceOptionMapper.java 수정        hola-room/mapper
2-8   PaidServiceOptionServiceImpl.java 수정   hola-room/service
2-9   PaidServiceOptionApiController.java 수정 hola-room/controller
2-10  paid-service-option-form.js 수정         hola-app/static/js
2-11  paid-service-option-form.html 수정       hola-app/templates
```

### Phase 3: 재고 관리

```
순서  파일                                    모듈
────  ──────────────────────────────────────  ──────────
3-1   V6_3_0__create_inventory_tables.sql     hola-app
3-2   InventoryItem.java                      hola-room/entity
3-3   InventoryAvailability.java              hola-room/entity
3-4   InventoryItemRepository.java            hola-room/repository
3-5   InventoryAvailabilityRepository.java    hola-room/repository
3-6   InventoryItem DTO (Request/Response)    hola-room/dto
3-7   InventoryItemMapper.java                hola-room/mapper
3-8   InventoryManagementStrategy.java        hola-room/service (interface)
3-9   InternalInventoryStrategy.java          hola-room/service
3-10  ExternalInventoryStrategy.java          hola-room/service
3-11  InventoryService.java (interface)       hola-room/service
3-12  InventoryServiceImpl.java               hola-room/service
3-13  InventoryApiController.java             hola-room/controller
3-14  ErrorCode.java 추가 (HOLA-26xx)        hola-common
3-15  BookingServiceImpl.java (재고 연동)     hola-reservation
3-16  inventory-item.html                     hola-app/templates
3-17  inventory-item.js                       hola-app/static/js
```

### Phase 4: 객실 업그레이드

```
순서  파일                                    모듈
────  ──────────────────────────────────────  ──────────
4-1   V6_4_0__create_room_upgrade_history.sql hola-app
4-2   RoomUpgradeHistory.java                 hola-reservation/entity
4-3   RoomUpgradeHistoryRepository.java       hola-reservation/repository
4-4   RoomUpgrade DTO (Request/Response)      hola-reservation/dto
4-5   RoomUpgradeService.java (interface)     hola-reservation/service
4-6   RoomUpgradeServiceImpl.java             hola-reservation/service
4-7   RoomUpgradeApiController.java           hola-reservation/controller
4-8   ErrorCode.java 추가 (HOLA-41xx)        hola-common
4-9   reservation-detail.html (업그레이드 버튼) hola-app/templates
4-10  reservation-detail.js (업그레이드 모달)  hola-app/static/js
```

### Phase 5: 예약 서비스 연동 강화

```
순서  파일                                    모듈
────  ──────────────────────────────────────  ──────────
5-1   V6_5_0__extend_reservation_service_item.sql hola-app
5-2   ReservationServiceItem.java (필드 추가) hola-reservation/entity
5-3   BookingServiceImpl.java (TC 연결 로직)  hola-reservation
5-4   RateIncludedServiceHelper.java (TC 처리) hola-reservation
5-5   booking.js (객실타입별 가격 표시)       hola-app/static/js
5-6   reservation-form.html 수정              hola-app/templates
```

---

## 9. 주요 비즈니스 규칙

### 9.1 객실타입별 서비스 가격 결정 우선순위

```
1. RoomTypePaidService.overridePrice (NOT NULL) → 이 가격 사용
2. RoomTypePaidService.overridePrice = NULL → PaidServiceOption.vatIncludedPrice (기본가)
3. RoomTypePaidService 매핑 없음 + packageScope = PROPERTY_WIDE → 기본가
4. RoomTypePaidService 매핑 없음 + packageScope = ROOM_TYPE_SPECIFIC → 사용 불가
```

### 9.2 재고 차감 타이밍

```
예약 생성 시: inventoryItemId가 있는 서비스 → 체크인~체크아웃 기간 재고 차감
예약 취소 시: 동일 기간 재고 복원
예약 수정 시 (날짜 변경): 기존 기간 복원 → 새 기간 차감
객실 업그레이드 시: 기존 서비스 재고 복원 → 새 객실타입 서비스 재확인
```

### 9.3 TransactionCode 삭제 제한

```
- 하위 PaidServiceOption이 존재하면 삭제 불가
- ReservationServiceItem에서 참조 중이면 삭제 불가
- useYn = false(비활성)은 가능, softDelete()는 참조 검증 후
```

### 9.4 객실 업그레이드 허용 조건

```
- 예약 상태: RESERVED, CHECK_IN, INHOUSE만 가능
- CHECKED_OUT, CANCELED, NO_SHOW는 불가
- 같은 객실타입 → 불가
- 대상 객실타입에 적용 가능한 레이트코드 존재해야 함
- COMPLIMENTARY: 차액 0원 처리 (무료 업그레이드)
- PAID: 차액을 ReservationServiceItem으로 부과
- UPSELL: PAID와 동일하되, 업셀 사유 기록
```
