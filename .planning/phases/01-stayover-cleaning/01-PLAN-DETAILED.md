# Stayover Cleaning - 세부 구현 계획

**Created:** 2026-03-26
**기반 코드 분석 완료:** HousekeepingServiceImpl(903줄), HkConfig, RoomNumber, RoomStatusService, HkTaskMapper, settings.html/js, ErrorCode, V8 마이그레이션 13개

---

## Wave 1: DB + Entity 기반 (독립 실행 가능)

---

### Task 1-1. Flyway 마이그레이션 V8_10_0

**파일:** `hola-app/src/main/resources/db/migration/V8_10_0__add_stayover_cleaning_policy.sql`

> 기존 V8_9_0까지 존재. V8_10_0 사용.

```sql
-- ============================================================
-- V8_10_0: 스테이오버 청소 정책 (HkConfig 확장 + HkCleaningPolicy 신규)
-- ============================================================

-- 1) hk_config 컬럼 추가 (프로퍼티 기본 정책)
ALTER TABLE hk_config ADD COLUMN IF NOT EXISTS stayover_frequency INTEGER DEFAULT 1;
ALTER TABLE hk_config ADD COLUMN IF NOT EXISTS stayover_enabled BOOLEAN DEFAULT false;
ALTER TABLE hk_config ADD COLUMN IF NOT EXISTS turndown_enabled BOOLEAN DEFAULT false;
ALTER TABLE hk_config ADD COLUMN IF NOT EXISTS dnd_policy VARCHAR(30) DEFAULT 'SKIP';
ALTER TABLE hk_config ADD COLUMN IF NOT EXISTS dnd_max_skip_days INTEGER DEFAULT 3;
ALTER TABLE hk_config ADD COLUMN IF NOT EXISTS daily_task_gen_time VARCHAR(5) DEFAULT '06:00';
ALTER TABLE hk_config ADD COLUMN IF NOT EXISTS od_transition_time VARCHAR(5) DEFAULT '05:00';

COMMENT ON COLUMN hk_config.stayover_frequency IS '1일 스테이오버 횟수 (기본 1)';
COMMENT ON COLUMN hk_config.stayover_enabled IS '스테이오버 자동 생성 활성화';
COMMENT ON COLUMN hk_config.turndown_enabled IS '턴다운 자동 생성 활성화';
COMMENT ON COLUMN hk_config.dnd_policy IS 'DND 정책: SKIP, RETRY_AFTERNOON, FORCE_AFTER_DAYS';
COMMENT ON COLUMN hk_config.dnd_max_skip_days IS 'FORCE_AFTER_DAYS 정책 시 강제 청소까지 최대 일수';
COMMENT ON COLUMN hk_config.daily_task_gen_time IS '일일 작업 자동 생성 시각 (HH:mm)';
COMMENT ON COLUMN hk_config.od_transition_time IS 'OC→OD 일괄 전환 시각 (HH:mm)';

-- 2) hk_cleaning_policy 테이블 (룸타입별 오버라이드)
CREATE TABLE IF NOT EXISTS hk_cleaning_policy (
    id                  BIGSERIAL PRIMARY KEY,
    property_id         BIGINT NOT NULL,
    room_type_id        BIGINT NOT NULL,

    -- 오버라이드 필드 (null = 프로퍼티 기본값 사용)
    stayover_enabled    BOOLEAN,
    stayover_frequency  INTEGER,
    turndown_enabled    BOOLEAN,
    stayover_credit     DECIMAL(3,1),
    turndown_credit     DECIMAL(3,1),
    stayover_priority   VARCHAR(10),
    dnd_policy          VARCHAR(30),
    dnd_max_skip_days   INTEGER,
    note                VARCHAR(500),

    -- BaseEntity 표준 필드
    use_yn              BOOLEAN DEFAULT true NOT NULL,
    sort_order          INTEGER DEFAULT 0,
    created_at          TIMESTAMP DEFAULT NOW(),
    created_by          VARCHAR(50),
    updated_at          TIMESTAMP DEFAULT NOW(),
    updated_by          VARCHAR(50),
    deleted_at          TIMESTAMP,

    CONSTRAINT uk_hk_cleaning_policy UNIQUE (property_id, room_type_id)
);

CREATE INDEX IF NOT EXISTS idx_hk_cleaning_policy_property
    ON hk_cleaning_policy (property_id) WHERE deleted_at IS NULL;

COMMENT ON TABLE hk_cleaning_policy IS '룸타입별 청소 정책 오버라이드 (null 필드 = 프로퍼티 HkConfig 기본값 사용)';

-- 3) hk_task DND 추적 컬럼
ALTER TABLE hk_task ADD COLUMN IF NOT EXISTS dnd_skipped BOOLEAN DEFAULT false;
ALTER TABLE hk_task ADD COLUMN IF NOT EXISTS dnd_skip_count INTEGER DEFAULT 0;
ALTER TABLE hk_task ADD COLUMN IF NOT EXISTS scheduled_time VARCHAR(5);

COMMENT ON COLUMN hk_task.dnd_skipped IS 'DND 스킵 여부';
COMMENT ON COLUMN hk_task.scheduled_time IS '예정 청소 시간대 (HH:mm)';

-- 4) htl_room_number DND 추적 컬럼
ALTER TABLE htl_room_number ADD COLUMN IF NOT EXISTS dnd_since DATE;
ALTER TABLE htl_room_number ADD COLUMN IF NOT EXISTS consecutive_dnd_days INTEGER DEFAULT 0;

COMMENT ON COLUMN htl_room_number.dnd_since IS 'DND 시작 날짜';
COMMENT ON COLUMN htl_room_number.consecutive_dnd_days IS 'DND 연속 일수';
```

---

### Task 1-2. HkConfig 엔티티 확장

**파일:** `hola-hotel/src/main/java/com/hola/hotel/entity/HkConfig.java`

**추가할 필드** (기존 `rushThresholdMinutes` 아래에):

```java
// 스테이오버 정책
@Column(name = "stayover_enabled")
@Builder.Default
private Boolean stayoverEnabled = false;

@Column(name = "stayover_frequency")
@Builder.Default
private Integer stayoverFrequency = 1;

@Column(name = "turndown_enabled")
@Builder.Default
private Boolean turndownEnabled = false;

// DND 정책
@Column(name = "dnd_policy", length = 30)
@Builder.Default
private String dndPolicy = "SKIP";

@Column(name = "dnd_max_skip_days")
@Builder.Default
private Integer dndMaxSkipDays = 3;

// 스케줄 시각
@Column(name = "daily_task_gen_time", length = 5)
@Builder.Default
private String dailyTaskGenTime = "06:00";

@Column(name = "od_transition_time", length = 5)
@Builder.Default
private String odTransitionTime = "05:00";
```

**update() 메서드 확장** — 기존 9개 파라미터에 7개 추가:

```java
public void update(Boolean inspectionRequired, Boolean autoCreateCheckout, Boolean autoCreateStayover,
                   BigDecimal defaultCheckoutCredit, BigDecimal defaultStayoverCredit,
                   BigDecimal defaultTurndownCredit, BigDecimal defaultDeepCleanCredit,
                   BigDecimal defaultTouchUpCredit, Integer rushThresholdMinutes,
                   // 신규 파라미터
                   Boolean stayoverEnabled, Integer stayoverFrequency, Boolean turndownEnabled,
                   String dndPolicy, Integer dndMaxSkipDays,
                   String dailyTaskGenTime, String odTransitionTime) {
    // 기존 필드 유지
    this.inspectionRequired = inspectionRequired;
    this.autoCreateCheckout = autoCreateCheckout;
    this.autoCreateStayover = autoCreateStayover;
    this.defaultCheckoutCredit = defaultCheckoutCredit;
    this.defaultStayoverCredit = defaultStayoverCredit;
    this.defaultTurndownCredit = defaultTurndownCredit;
    this.defaultDeepCleanCredit = defaultDeepCleanCredit;
    this.defaultTouchUpCredit = defaultTouchUpCredit;
    this.rushThresholdMinutes = rushThresholdMinutes;
    // 신규 필드
    this.stayoverEnabled = stayoverEnabled;
    this.stayoverFrequency = stayoverFrequency;
    this.turndownEnabled = turndownEnabled;
    this.dndPolicy = dndPolicy;
    this.dndMaxSkipDays = dndMaxSkipDays;
    this.dailyTaskGenTime = dailyTaskGenTime;
    this.odTransitionTime = odTransitionTime;
}
```

> **영향 범위:** `HousekeepingServiceImpl.updateConfig()` (line 502) 에서 `config.update()` 호출 부분 수정 필요.

---

### Task 1-3. HkCleaningPolicy 엔티티 신규

**파일:** `hola-hotel/src/main/java/com/hola/hotel/entity/HkCleaningPolicy.java`

```java
package com.hola.hotel.entity;

import com.hola.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;
import java.math.BigDecimal;

/**
 * 룸타입별 청소 정책 오버라이드
 * null 필드 = 프로퍼티 HkConfig 기본값 사용
 */
@Entity
@Table(name = "hk_cleaning_policy",
       uniqueConstraints = @UniqueConstraint(columnNames = {"property_id", "room_type_id"}))
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class HkCleaningPolicy extends BaseEntity {

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "room_type_id", nullable = false)
    private Long roomTypeId;

    @Column(name = "stayover_enabled")
    private Boolean stayoverEnabled;

    @Column(name = "stayover_frequency")
    private Integer stayoverFrequency;

    @Column(name = "turndown_enabled")
    private Boolean turndownEnabled;

    @Column(name = "stayover_credit", precision = 3, scale = 1)
    private BigDecimal stayoverCredit;

    @Column(name = "turndown_credit", precision = 3, scale = 1)
    private BigDecimal turndownCredit;

    @Column(name = "stayover_priority", length = 10)
    private String stayoverPriority;

    @Column(name = "dnd_policy", length = 30)
    private String dndPolicy;

    @Column(name = "dnd_max_skip_days")
    private Integer dndMaxSkipDays;

    @Column(name = "note", length = 500)
    private String note;

    public void update(Boolean stayoverEnabled, Integer stayoverFrequency,
                       Boolean turndownEnabled, BigDecimal stayoverCredit,
                       BigDecimal turndownCredit, String stayoverPriority,
                       String dndPolicy, Integer dndMaxSkipDays, String note) {
        this.stayoverEnabled = stayoverEnabled;
        this.stayoverFrequency = stayoverFrequency;
        this.turndownEnabled = turndownEnabled;
        this.stayoverCredit = stayoverCredit;
        this.turndownCredit = turndownCredit;
        this.stayoverPriority = stayoverPriority;
        this.dndPolicy = dndPolicy;
        this.dndMaxSkipDays = dndMaxSkipDays;
        this.note = note;
    }
}
```

---

### Task 1-4. HkTask 엔티티 확장

**파일:** `hola-hotel/src/main/java/com/hola/hotel/entity/HkTask.java`

기존 `note` 필드 아래에 추가:

```java
@Column(name = "dnd_skipped")
@Builder.Default
private Boolean dndSkipped = false;

@Column(name = "dnd_skip_count")
@Builder.Default
private Integer dndSkipCount = 0;

@Column(name = "scheduled_time", length = 5)
private String scheduledTime;
```

---

### Task 1-5. RoomNumber 엔티티 확장

**파일:** `hola-hotel/src/main/java/com/hola/hotel/entity/RoomNumber.java`

**추가 필드** (기존 `hkMemo` 아래에):

```java
@Column(name = "dnd_since")
private LocalDate dndSince;

@Column(name = "consecutive_dnd_days")
@Builder.Default
private Integer consecutiveDndDays = 0;
```

**추가 메서드:**

```java
/**
 * DND 설정
 */
public void setDnd() {
    this.hkStatus = "DND";
    this.hkUpdatedAt = LocalDateTime.now();
    if (this.dndSince == null) {
        this.dndSince = LocalDate.now();
    }
}

/**
 * DND 해제 → DIRTY 전환
 */
public void clearDnd() {
    this.hkStatus = "DIRTY";
    this.hkUpdatedAt = LocalDateTime.now();
    this.dndSince = null;
    this.consecutiveDndDays = 0;
}

/**
 * DND 연속 일수 증가 (일일 배치에서 호출)
 */
public void incrementDndDays() {
    this.consecutiveDndDays = (this.consecutiveDndDays != null ? this.consecutiveDndDays : 0) + 1;
}
```

> **import 추가:** `java.time.LocalDate` (기존 `java.time.LocalDateTime`만 있음)

---

### Task 1-6. Repository + DTO + Mapper

**신규 파일 3개:**

**a) `hola-hotel/.../repository/HkCleaningPolicyRepository.java`**

```java
package com.hola.hotel.repository;

import com.hola.hotel.entity.HkCleaningPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface HkCleaningPolicyRepository extends JpaRepository<HkCleaningPolicy, Long> {
    List<HkCleaningPolicy> findByPropertyIdOrderBySortOrder(Long propertyId);
    Optional<HkCleaningPolicy> findByPropertyIdAndRoomTypeId(Long propertyId, Long roomTypeId);
    boolean existsByPropertyIdAndRoomTypeId(Long propertyId, Long roomTypeId);
}
```

**b) `hola-hotel/.../dto/request/HkCleaningPolicyRequest.java`**

```java
package com.hola.hotel.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class HkCleaningPolicyRequest {
    private Long roomTypeId;
    private Boolean stayoverEnabled;
    private Integer stayoverFrequency;
    private Boolean turndownEnabled;
    private BigDecimal stayoverCredit;
    private BigDecimal turndownCredit;
    private String stayoverPriority;
    private String dndPolicy;
    private Integer dndMaxSkipDays;
    private String note;
}
```

**c) `hola-hotel/.../dto/response/HkCleaningPolicyResponse.java`**

```java
package com.hola.hotel.dto.response;

import lombok.*;
import java.math.BigDecimal;

@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class HkCleaningPolicyResponse {
    private Long id;
    private Long propertyId;
    private Long roomTypeId;
    private String roomTypeName;     // 조회 시 조인
    private String roomTypeCode;     // 조회 시 조인
    private Boolean stayoverEnabled;
    private Integer stayoverFrequency;
    private Boolean turndownEnabled;
    private BigDecimal stayoverCredit;
    private BigDecimal turndownCredit;
    private String stayoverPriority;
    private String dndPolicy;
    private Integer dndMaxSkipDays;
    private String note;
    private boolean overridden;      // true=오버라이드 있음, false=프로퍼티 기본값
}
```

**수정 파일 3개:**

**d) `HkConfigUpdateRequest.java`** — 7개 필드 추가:

```java
private Boolean stayoverEnabled;
private Integer stayoverFrequency;
private Boolean turndownEnabled;
private String dndPolicy;
private Integer dndMaxSkipDays;
private String dailyTaskGenTime;
private String odTransitionTime;
```

**e) `HkConfigResponse.java`** — 동일 7개 필드 추가

**f) `HkTaskMapper.toResponse(HkConfig)` 메서드** — 7개 필드 매핑 추가:

```java
// 기존 .rushThresholdMinutes(config.getRushThresholdMinutes()) 아래에
.stayoverEnabled(config.getStayoverEnabled())
.stayoverFrequency(config.getStayoverFrequency())
.turndownEnabled(config.getTurndownEnabled())
.dndPolicy(config.getDndPolicy())
.dndMaxSkipDays(config.getDndMaxSkipDays())
.dailyTaskGenTime(config.getDailyTaskGenTime())
.odTransitionTime(config.getOdTransitionTime())
```

---

### Task 1-7. ErrorCode 추가

**파일:** `hola-common/src/main/java/com/hola/common/exception/ErrorCode.java`

기존 `HK_CONCURRENT_MODIFICATION` (HOLA-8080) 아래에:

```java
HK_CLEANING_POLICY_NOT_FOUND("HOLA-8090", "청소 정책을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
HK_CLEANING_POLICY_DUPLICATE("HOLA-8091", "해당 룸타입에 이미 청소 정책이 존재합니다.", HttpStatus.CONFLICT),
```

---

### Task 1-8. RoomStatusService.calcStatusCode() DND 추가

**파일:** `hola-hotel/src/main/java/com/hola/hotel/service/RoomStatusService.java`

```java
// 기존 switch 문에 DND 케이스 추가:
static String calcStatusCode(String hkStatus, String foStatus) {
    if ("OOO".equals(hkStatus)) return "OOO";
    if ("OOS".equals(hkStatus)) return "OOS";
    if ("DND".equals(hkStatus)) return "DND";   // ← 추가
    String fo = "OCCUPIED".equals(foStatus) ? "O" : "V";
    String hk;
    switch (hkStatus) {
        case "CLEAN": hk = "C"; break;
        case "INSPECTED": hk = "I"; break;
        case "PICKUP": hk = "P"; break;
        default: hk = "D"; break;
    }
    return fo + hk;
}
```

**영향:** RoomStatusServiceImpl.getRoomRackItems(), HK 대시보드 roomStatusSummary에 DND 카운트 추가 필요

**파일:** `hola-hotel/src/main/java/com/hola/hotel/service/HousekeepingServiceImpl.java` line 884~901

`buildRoomStatusSummary()`에 DND 카운트 추가:

```java
long dnd = roomNumberRepository.countByPropertyIdAndHkStatus(propertyId, "DND");
// .dnd((int) dnd) 추가
```

**파일:** `hola-hotel/src/main/java/com/hola/hotel/dto/response/HkDashboardResponse.java`

`RoomStatusSummary`에 `private int dnd;` 필드 추가.

---

## Wave 2: Service Layer (Wave 1 완료 후)

---

### Task 2-1. HkCleaningPolicyService (CRUD + 정책 해석)

**신규 파일 2개:**

**a) `hola-hotel/.../service/HkCleaningPolicyService.java`**

```java
package com.hola.hotel.service;

import com.hola.hotel.dto.request.HkCleaningPolicyRequest;
import com.hola.hotel.dto.response.HkCleaningPolicyResponse;
import java.math.BigDecimal;
import java.util.List;

public interface HkCleaningPolicyService {

    /** 해석된 최종 정책 (기본값 + 오버라이드 병합) */
    ResolvedCleaningPolicy resolvePolicy(Long propertyId, Long roomTypeId);

    /** 프로퍼티의 모든 룸타입에 대한 정책 목록 (해석 결과 포함) */
    List<HkCleaningPolicyResponse> getAllPolicies(Long propertyId);

    /** 오버라이드 생성 또는 수정 */
    HkCleaningPolicyResponse createOrUpdate(Long propertyId, HkCleaningPolicyRequest request);

    /** 오버라이드 삭제 (기본값으로 복귀) */
    void deletePolicy(Long propertyId, Long roomTypeId);
}
```

**b) `hola-hotel/.../service/HkCleaningPolicyServiceImpl.java`**

핵심 로직 — `resolvePolicy()`:

```java
@Override
public ResolvedCleaningPolicy resolvePolicy(Long propertyId, Long roomTypeId) {
    HkConfig config = hkConfigRepository.findByPropertyId(propertyId)
            .orElseGet(() -> HkConfig.builder().propertyId(propertyId).build());

    HkCleaningPolicy override = hkCleaningPolicyRepository
            .findByPropertyIdAndRoomTypeId(propertyId, roomTypeId)
            .orElse(null);

    return ResolvedCleaningPolicy.builder()
            .stayoverEnabled(pick(override, HkCleaningPolicy::getStayoverEnabled,
                                  config.getStayoverEnabled()))
            .stayoverFrequency(pick(override, HkCleaningPolicy::getStayoverFrequency,
                                    config.getStayoverFrequency()))
            .turndownEnabled(pick(override, HkCleaningPolicy::getTurndownEnabled,
                                  config.getTurndownEnabled()))
            .stayoverCredit(pick(override, HkCleaningPolicy::getStayoverCredit,
                                 config.getDefaultStayoverCredit()))
            .turndownCredit(pick(override, HkCleaningPolicy::getTurndownCredit,
                                 config.getDefaultTurndownCredit()))
            .stayoverPriority(pick(override, HkCleaningPolicy::getStayoverPriority,
                                   "NORMAL"))
            .dndPolicy(pick(override, HkCleaningPolicy::getDndPolicy,
                            config.getDndPolicy()))
            .dndMaxSkipDays(pick(override, HkCleaningPolicy::getDndMaxSkipDays,
                                 config.getDndMaxSkipDays()))
            .overridden(override != null)
            .build();
}

private <T> T pick(HkCleaningPolicy override, Function<HkCleaningPolicy, T> getter, T fallback) {
    if (override == null) return fallback;
    T val = getter.apply(override);
    return val != null ? val : fallback;
}
```

`getAllPolicies()` — 프로퍼티의 모든 룸타입 목록을 가져와서 각각 resolvePolicy:

```java
@Override
public List<HkCleaningPolicyResponse> getAllPolicies(Long propertyId) {
    accessControlService.validatePropertyAccess(propertyId);

    // hola-room 모듈의 RoomType을 직접 참조하지 않고 네이티브 쿼리로 조회
    // (크로스 모듈 FK 원칙: Long ID 참조만)
    List<Object[]> roomTypes = roomTypeQueryRepository.findRoomTypesByPropertyId(propertyId);
    List<HkCleaningPolicy> overrides = hkCleaningPolicyRepository.findByPropertyIdOrderBySortOrder(propertyId);
    Map<Long, HkCleaningPolicy> overrideMap = overrides.stream()
            .collect(Collectors.toMap(HkCleaningPolicy::getRoomTypeId, p -> p));

    HkConfig config = hkConfigRepository.findByPropertyId(propertyId).orElse(null);

    return roomTypes.stream().map(rt -> {
        Long roomTypeId = ((Number) rt[0]).longValue();
        String roomTypeCode = (String) rt[1];
        String description = (String) rt[2];
        HkCleaningPolicy ov = overrideMap.get(roomTypeId);

        return HkCleaningPolicyResponse.builder()
                .id(ov != null ? ov.getId() : null)
                .propertyId(propertyId)
                .roomTypeId(roomTypeId)
                .roomTypeName(description)
                .roomTypeCode(roomTypeCode)
                .stayoverEnabled(ov != null ? ov.getStayoverEnabled() : null)
                .stayoverFrequency(ov != null ? ov.getStayoverFrequency() : null)
                .turndownEnabled(ov != null ? ov.getTurndownEnabled() : null)
                .stayoverCredit(ov != null ? ov.getStayoverCredit() : null)
                .turndownCredit(ov != null ? ov.getTurndownCredit() : null)
                .stayoverPriority(ov != null ? ov.getStayoverPriority() : null)
                .dndPolicy(ov != null ? ov.getDndPolicy() : null)
                .dndMaxSkipDays(ov != null ? ov.getDndMaxSkipDays() : null)
                .note(ov != null ? ov.getNote() : null)
                .overridden(ov != null)
                .build();
    }).collect(Collectors.toList());
}
```

**c) `hola-hotel/.../dto/response/ResolvedCleaningPolicy.java`** (신규)

```java
package com.hola.hotel.dto.response;

import lombok.*;
import java.math.BigDecimal;

@Getter @Builder
public class ResolvedCleaningPolicy {
    private final boolean stayoverEnabled;
    private final int stayoverFrequency;
    private final boolean turndownEnabled;
    private final BigDecimal stayoverCredit;
    private final BigDecimal turndownCredit;
    private final String stayoverPriority;
    private final String dndPolicy;
    private final int dndMaxSkipDays;
    private final boolean overridden;
}
```

**d) RoomType 조회용 네이티브 쿼리** — `RoomNumberRepository`에 추가 (hola-room 모듈 직접 참조 회피):

```java
/** 프로퍼티별 룸타입 기본 정보 조회 (크로스 모듈 네이티브 쿼리) */
@Query(value = "SELECT rt.id, rt.room_type_code, rt.description " +
       "FROM rm_room_type rt " +
       "WHERE rt.property_id = :propertyId AND rt.deleted_at IS NULL " +
       "ORDER BY rt.sort_order, rt.room_type_code",
       nativeQuery = true)
List<Object[]> findRoomTypesByPropertyId(@Param("propertyId") Long propertyId);
```

---

### Task 2-2. HousekeepingService 확장 (OD 전환 + 스테이오버 생성 + DND)

**파일 수정:** `HousekeepingService.java` + `HousekeepingServiceImpl.java`

**인터페이스 추가 (HousekeepingService.java):**

```java
// === 스테이오버 자동화 ===

/** OC→OD 일괄 전환 (매일 새벽 실행). 전환된 객실 수 반환 */
int transitionOccupiedRoomsToDirty(Long propertyId);

/** 정책 기반 스테이오버 작업 자동 생성. 생성된 작업 수 반환 */
int generateStayoverTasks(Long propertyId, LocalDate date);

/** DND 객실 정책 기반 처리. 처리 결과 반환 */
Map<String, Integer> processDndRooms(Long propertyId, LocalDate date);
```

**구현 (HousekeepingServiceImpl.java):**

의존성 추가:

```java
private final HkCleaningPolicyService cleaningPolicyService;
```

**a) transitionOccupiedRoomsToDirty:**

```java
@Override
@Transactional
public int transitionOccupiedRoomsToDirty(Long propertyId) {
    // OC 객실 → DIRTY 전환
    List<RoomNumber> ocRooms = roomNumberRepository.findOccupiedCleanRooms(propertyId);
    for (RoomNumber room : ocRooms) {
        room.updateHkStatus("DIRTY", null);
    }

    // DND 객실: 연속 일수 증가
    List<RoomNumber> dndRooms = roomNumberRepository.findByPropertyIdAndHkStatusOrderByRoomNumberAsc(
            propertyId, "DND");
    for (RoomNumber room : dndRooms) {
        if ("OCCUPIED".equals(room.getFoStatus())) {
            room.incrementDndDays();
        }
    }

    log.info("OC→OD 전환: propertyId={}, 전환={}건, DND 일수증가={}건",
             propertyId, ocRooms.size(), dndRooms.size());
    return ocRooms.size();
}
```

RoomNumberRepository에 추가할 쿼리:

```java
@Query("SELECT r FROM RoomNumber r WHERE r.property.id = :propertyId " +
       "AND r.foStatus = 'OCCUPIED' AND r.hkStatus = 'CLEAN' " +
       "ORDER BY r.roomNumber ASC")
List<RoomNumber> findOccupiedCleanRooms(@Param("propertyId") Long propertyId);
```

**b) generateStayoverTasks (정책 기반):**

```java
@Override
@Transactional
public int generateStayoverTasks(Long propertyId, LocalDate date) {
    LocalDate targetDate = date != null ? date : LocalDate.now();

    // OD 객실 + roomTypeId 조회 (네이티브 쿼리)
    List<Object[]> odRooms = roomNumberRepository.findOccupiedDirtyRoomsWithRoomTypeId(propertyId);
    int created = 0;

    for (Object[] row : odRooms) {
        Long roomNumberId = ((Number) row[0]).longValue();
        Long roomTypeId = row[1] != null ? ((Number) row[1]).longValue() : null;

        // 룸타입 없는 객실은 프로퍼티 기본 정책 사용
        ResolvedCleaningPolicy policy = cleaningPolicyService.resolvePolicy(
                propertyId, roomTypeId != null ? roomTypeId : 0L);

        if (!policy.isStayoverEnabled()) continue;

        // 이미 오늘 활성 STAYOVER 작업이 있으면 스킵
        if (hkTaskRepository.existsActiveTaskByRoomNumberIdAndTaskDate(roomNumberId, targetDate)) continue;

        // frequency만큼 작업 생성
        for (int i = 0; i < policy.getStayoverFrequency(); i++) {
            String scheduledTime = calculateScheduledTime(i, policy.getStayoverFrequency());
            HkTask task = HkTask.builder()
                    .propertyId(propertyId)
                    .roomNumberId(roomNumberId)
                    .taskType("STAYOVER")
                    .taskDate(targetDate)
                    .priority(policy.getStayoverPriority())
                    .credit(policy.getStayoverCredit())
                    .scheduledTime(scheduledTime)
                    .build();
            applyRushPriority(task, propertyId);
            hkTaskRepository.save(task);
            created++;
        }
    }

    log.info("스테이오버 작업 생성: propertyId={}, date={}, 생성={}건", propertyId, targetDate, created);
    return created;
}

/**
 * 시간대 분배: frequency별 청소 시작 시간 계산
 * frequency=1 → "10:00"
 * frequency=2 → "10:00", "16:00"
 * frequency=3 → "09:00", "13:00", "17:00"
 */
private String calculateScheduledTime(int index, int total) {
    if (total <= 1) return "10:00";
    int startHour = 9;
    int endHour = 18;
    int gap = (endHour - startHour) / total;
    int hour = startHour + (gap * index);
    return String.format("%02d:00", hour);
}
```

RoomNumberRepository에 추가할 네이티브 쿼리:

```java
/** OD 객실 + roomTypeId 조회 (스테이오버 작업 생성용) */
@Query(value =
    "SELECT rn.id AS room_number_id, " +
    "       (SELECT rtf.room_type_id FROM rm_room_type_floor rtf " +
    "        WHERE rtf.room_number_id = rn.id LIMIT 1) AS room_type_id " +
    "FROM htl_room_number rn " +
    "WHERE rn.property_id = :propertyId " +
    "  AND rn.fo_status = 'OCCUPIED' AND rn.hk_status = 'DIRTY' " +
    "  AND rn.deleted_at IS NULL " +
    "ORDER BY rn.room_number",
    nativeQuery = true)
List<Object[]> findOccupiedDirtyRoomsWithRoomTypeId(@Param("propertyId") Long propertyId);
```

**c) processDndRooms:**

```java
@Override
@Transactional
public Map<String, Integer> processDndRooms(Long propertyId, LocalDate date) {
    List<RoomNumber> dndRooms = roomNumberRepository.findByPropertyIdAndHkStatusOrderByRoomNumberAsc(
            propertyId, "DND");
    int skipped = 0, retried = 0, forced = 0;

    for (RoomNumber room : dndRooms) {
        if (!"OCCUPIED".equals(room.getFoStatus())) continue;

        // roomTypeId 조회
        Long roomTypeId = roomNumberRepository.findRoomTypeIdByRoomNumberId(room.getId());
        ResolvedCleaningPolicy policy = cleaningPolicyService.resolvePolicy(
                propertyId, roomTypeId != null ? roomTypeId : 0L);

        String dndPolicy = policy.getDndPolicy();
        if (dndPolicy == null) dndPolicy = "SKIP";

        switch (dndPolicy) {
            case "SKIP":
                skipped++;
                break;

            case "RETRY_AFTERNOON":
                // 오후 시간대 STAYOVER 작업 생성 (DND 마킹)
                if (!hkTaskRepository.existsActiveTaskByRoomNumberIdAndTaskDate(room.getId(), date)) {
                    HkTask task = HkTask.builder()
                            .propertyId(propertyId)
                            .roomNumberId(room.getId())
                            .taskType("STAYOVER")
                            .taskDate(date)
                            .priority("NORMAL")
                            .credit(policy.getStayoverCredit())
                            .scheduledTime("14:00")
                            .dndSkipped(true)
                            .dndSkipCount(room.getConsecutiveDndDays())
                            .note("DND 오후 재시도")
                            .build();
                    hkTaskRepository.save(task);
                }
                retried++;
                break;

            case "FORCE_AFTER_DAYS":
                int maxDays = policy.getDndMaxSkipDays();
                if (room.getConsecutiveDndDays() != null && room.getConsecutiveDndDays() >= maxDays) {
                    // 강제 청소: DND 해제 → DIRTY → 작업 생성
                    room.clearDnd();
                    if (!hkTaskRepository.existsActiveTaskByRoomNumberIdAndTaskDate(room.getId(), date)) {
                        HkTask task = HkTask.builder()
                                .propertyId(propertyId)
                                .roomNumberId(room.getId())
                                .taskType("STAYOVER")
                                .taskDate(date)
                                .priority("HIGH")
                                .credit(policy.getStayoverCredit())
                                .scheduledTime("10:00")
                                .note("DND " + maxDays + "일 초과 강제 청소")
                                .build();
                        hkTaskRepository.save(task);
                    }
                    forced++;
                } else {
                    skipped++;
                }
                break;
        }
    }

    log.info("DND 처리: propertyId={}, 스킵={}, 재시도={}, 강제={}",
             propertyId, skipped, retried, forced);
    return Map.of("skipped", skipped, "retried", retried, "forced", forced);
}
```

RoomNumberRepository에 roomTypeId 조회 쿼리 추가:

```java
/** 특정 ��실의 룸타입 ID 조회 */
@Query(value = "SELECT rtf.room_type_id FROM rm_room_type_floor rtf " +
       "WHERE rtf.room_number_id = :roomNumberId LIMIT 1",
       nativeQuery = true)
Long findRoomTypeIdByRoomNumberId(@Param("roomNumberId") Long roomNumberId);
```

---

### Task 2-3. HousekeepingServiceImpl.updateConfig() 수정

**파일:** `HousekeepingServiceImpl.java` line 491~514

기존 `config.update()` 호출에 신규 파라미터 추가:

```java
config.update(
    request.getInspectionRequired(),
    request.getAutoCreateCheckout(),
    request.getAutoCreateStayover(),
    request.getDefaultCheckoutCredit(),
    request.getDefaultStayoverCredit(),
    request.getDefaultTurndownCredit(),
    request.getDefaultDeepCleanCredit(),
    request.getDefaultTouchUpCredit(),
    request.getRushThresholdMinutes(),
    // 신규
    request.getStayoverEnabled(),
    request.getStayoverFrequency(),
    request.getTurndownEnabled(),
    request.getDndPolicy(),
    request.getDndMaxSkipDays(),
    request.getDailyTaskGenTime(),
    request.getOdTransitionTime()
);
```

기존 `getConfig()` (line 466~487)에서 기본값 반환 시에도 신규 필드 추가:

```java
.stayoverEnabled(false)
.stayoverFrequency(1)
.turndownEnabled(false)
.dndPolicy("SKIP")
.dndMaxSkipDays(3)
.dailyTaskGenTime("06:00")
.odTransitionTime("05:00")
```

---

## Wave 3: 스케줄러 + API + UI (Wave 2 완료 후)

---

### Task 3-1. @EnableScheduling + HkSchedulerService

**a) `HolaPmsApplication.java` 수정:**

```java
@SpringBootApplication
@EnableScheduling    // ← 추가
public class HolaPmsApplication {
```

> import 추가: `org.springframework.scheduling.annotation.EnableScheduling`

**b) 신규 파일: `hola-hotel/.../service/HkSchedulerService.java`**

```java
package com.hola.hotel.service;

import com.hola.hotel.entity.HkConfig;
import com.hola.hotel.repository.HkConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HkSchedulerService {

    private final HkConfigRepository hkConfigRepository;
    private final HousekeepingService housekeepingService;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * 매 분 실행 — 프로퍼티별 설정 시각에 맞춰 작업 생성
     * 프로퍼티마다 odTransitionTime / dailyTaskGenTime이 다를 수 있으므로
     * 1분 단위로 체크하여 해당 시각에 맞는 프로퍼티만 처리
     */
    @Scheduled(cron = "0 * * * * *")
    public void processHousekeepingSchedule() {
        String now = LocalTime.now().format(TIME_FMT);
        List<HkConfig> configs = hkConfigRepository.findAll();

        for (HkConfig config : configs) {
            try {
                processPropertySchedule(config, now);
            } catch (Exception e) {
                // 한 프로퍼티 오류가 다른 프로퍼티에 영향 미치지 않도록
                log.error("HK 스케줄러 오류: propertyId={}, time={}, error={}",
                        config.getPropertyId(), now, e.getMessage(), e);
            }
        }
    }

    private void processPropertySchedule(HkConfig config, String currentTime) {
        Long propertyId = config.getPropertyId();
        LocalDate today = LocalDate.now();

        // 1) OC→OD 전환 시각
        String odTime = config.getOdTransitionTime() != null ? config.getOdTransitionTime() : "05:00";
        if (currentTime.equals(odTime)) {
            int converted = housekeepingService.transitionOccupiedRoomsToDirty(propertyId);
            if (converted > 0) {
                log.info("[스케줄러] OC→OD: propertyId={}, {}건", propertyId, converted);
            }
        }

        // 2) 일일 작업 생성 시각
        String genTime = config.getDailyTaskGenTime() != null ? config.getDailyTaskGenTime() : "06:00";
        if (currentTime.equals(genTime)) {
            // 스테이오버
            if (Boolean.TRUE.equals(config.getStayoverEnabled())) {
                int stayover = housekeepingService.generateStayoverTasks(propertyId, today);
                if (stayover > 0) {
                    log.info("[스케줄러] 스테이오버: propertyId={}, {}건", propertyId, stayover);
                }
            }

            // 기존 일일 작업 (VD→CHECKOUT도 포함)
            if (Boolean.TRUE.equals(config.getAutoCreateCheckout())) {
                housekeepingService.generateDailyTasks(propertyId, today);
            }

            // DND 처리
            housekeepingService.processDndRooms(propertyId, today);
        }
    }
}
```

> **주의:** `generateDailyTasks()`는 내부에서 `accessControlService.validatePropertyAccess()` 를 호출합니다. 스케줄러에서는 SecurityContext가 없으므로, 스케줄러 전용 메서드를 만들거나 accessControl 체크를 우회해야 합니다.
>
> **해결 방법:** `generateStayoverTasks()`와 `transitionOccupiedRoomsToDirty()`는 accessControl 체크 없이 구현 (내부 메서드). 기존 `generateDailyTasks()`는 스케줄러 전용 오버로드 추가:
>
> ```java
> /** 스케줄러 전용 (accessControl 체크 없음) */
> int generateDailyTasksInternal(Long propertyId, LocalDate date);
> ```

---

### Task 3-2. HkCleaningPolicyApiController

**신규 파일:** `hola-hotel/.../controller/HkCleaningPolicyApiController.java`

```java
package com.hola.hotel.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.common.security.AccessControlService;
import com.hola.hotel.dto.request.HkCleaningPolicyRequest;
import com.hola.hotel.dto.response.HkCleaningPolicyResponse;
import com.hola.hotel.service.HkCleaningPolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "청소 정책 관리")
@RestController
@RequestMapping("/api/v1/properties/{propertyId}/hk-cleaning-policies")
@RequiredArgsConstructor
public class HkCleaningPolicyApiController {

    private final HkCleaningPolicyService cleaningPolicyService;
    private final AccessControlService accessControlService;

    @Operation(summary = "전체 정책 목록", description = "모든 룸타입의 정책 (오버라이드 여부 표시)")
    @GetMapping
    public ResponseEntity<HolaResponse<List<HkCleaningPolicyResponse>>> getAllPolicies(
            @PathVariable Long propertyId) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(cleaningPolicyService.getAllPolicies(propertyId)));
    }

    @Operation(summary = "정책 생성/수정", description = "룸타입별 오버라이드 설정 (null 필드 = 기본값 사용)")
    @PostMapping
    public ResponseEntity<HolaResponse<HkCleaningPolicyResponse>> createOrUpdate(
            @PathVariable Long propertyId,
            @RequestBody HkCleaningPolicyRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(
                cleaningPolicyService.createOrUpdate(propertyId, request)));
    }

    @Operation(summary = "오버라이드 삭제", description = "룸타입 오버라이드 제거 (프로퍼티 기본값으로 복귀)")
    @DeleteMapping("/{roomTypeId}")
    public ResponseEntity<HolaResponse<Void>> deletePolicy(
            @PathVariable Long propertyId, @PathVariable Long roomTypeId) {
        accessControlService.validatePropertyAccess(propertyId);
        cleaningPolicyService.deletePolicy(propertyId, roomTypeId);
        return ResponseEntity.ok(HolaResponse.success());
    }
}
```

---

### Task 3-3. HousekeepingApiController 수동 실행 엔드포인트

**파일 수정:** `HousekeepingApiController.java` (기존 `generateDailyTasks` 아래에 추가)

```java
@Operation(summary = "OC→OD 수동 전환", description = "투숙 중 + 청소완료 객실을 일괄 DIRTY 전환")
@PostMapping("/transition-od")
public ResponseEntity<HolaResponse<Map<String, Object>>> transitionToDirty(
        @PathVariable Long propertyId) {
    accessControlService.validatePropertyAccess(propertyId);
    int count = housekeepingService.transitionOccupiedRoomsToDirty(propertyId);
    return ResponseEntity.ok(HolaResponse.success(Map.of("convertedCount", count)));
}

@Operation(summary = "스테이오버 작업 생성", description = "정책 기반 스테이오버 청소 작업 일괄 생성")
@PostMapping("/generate-stayover-tasks")
public ResponseEntity<HolaResponse<Map<String, Object>>> generateStayoverTasks(
        @PathVariable Long propertyId,
        @RequestBody(required = false) Map<String, String> body) {
    accessControlService.validatePropertyAccess(propertyId);
    LocalDate date = (body != null && body.get("date") != null)
            ? LocalDate.parse(body.get("date")) : LocalDate.now();
    int count = housekeepingService.generateStayoverTasks(propertyId, date);
    return ResponseEntity.ok(HolaResponse.success(Map.of("createdCount", count)));
}

@Operation(summary = "DND 처리", description = "DND 객실 정책 기반 처리 (스킵/재시도/강제)")
@PostMapping("/process-dnd")
public ResponseEntity<HolaResponse<Map<String, Integer>>> processDnd(
        @PathVariable Long propertyId,
        @RequestBody(required = false) Map<String, String> body) {
    accessControlService.validatePropertyAccess(propertyId);
    LocalDate date = (body != null && body.get("date") != null)
            ? LocalDate.parse(body.get("date")) : LocalDate.now();
    return ResponseEntity.ok(HolaResponse.success(housekeepingService.processDndRooms(propertyId, date)));
}
```

---

### Task 3-4. settings.html 탭 추가

**파일:** `hola-app/src/main/resources/templates/housekeeping/settings.html`

**변경 1:** 탭 목록에 "청소 정책" 탭 추가 (line 18~29 `<ul>` 안에):

```html
<li class="nav-item">
    <a class="nav-link" data-bs-toggle="tab" href="#tabCleaningPolicy">
        <i class="fas fa-broom me-1"></i>청소 정책
    </a>
</li>
```

**변경 2:** 일반 설정 탭 (line 34~91) 안에 신규 필드 추가 — `cfgRushThreshold` 아래, 저장 버튼 위:

```html
<h6 class="fw-bold mt-4 mb-3">스테이오버 정책</h6>
<div class="row mb-3">
    <label class="col-sm-3 col-form-label">스테이오버 자동 생성</label>
    <div class="col-sm-3">
        <div class="form-check form-switch mt-1">
            <input class="form-check-input" type="checkbox" id="cfgStayoverEnabled">
        </div>
    </div>
    <label class="col-sm-3 col-form-label">기본 횟수 (일)</label>
    <div class="col-sm-3">
        <input type="number" class="form-control" id="cfgStayoverFrequency" min="1" max="3" value="1">
    </div>
</div>
<div class="row mb-3">
    <label class="col-sm-3 col-form-label">턴다운 자동 생성</label>
    <div class="col-sm-3">
        <div class="form-check form-switch mt-1">
            <input class="form-check-input" type="checkbox" id="cfgTurndownEnabled">
        </div>
    </div>
</div>
<div class="row mb-3">
    <label class="col-sm-3 col-form-label">DND 정책</label>
    <div class="col-sm-3">
        <select class="form-select" id="cfgDndPolicy">
            <option value="SKIP">스킵 (무시)</option>
            <option value="RETRY_AFTERNOON">오후 재시도</option>
            <option value="FORCE_AFTER_DAYS">N일 후 강제</option>
        </select>
    </div>
    <label class="col-sm-3 col-form-label">DND 최대 스킵 (일)</label>
    <div class="col-sm-3">
        <input type="number" class="form-control" id="cfgDndMaxSkipDays" min="1" max="7" value="3">
    </div>
</div>

<h6 class="fw-bold mt-4 mb-3">스케줄 시각</h6>
<div class="row mb-3">
    <label class="col-sm-3 col-form-label">OD 전환 시각</label>
    <div class="col-sm-3">
        <input type="time" class="form-control" id="cfgOdTransitionTime" value="05:00">
    </div>
    <label class="col-sm-3 col-form-label">작업 생성 시각</label>
    <div class="col-sm-3">
        <input type="time" class="form-control" id="cfgDailyTaskGenTime" value="06:00">
    </div>
</div>
```

**변경 3:** 탭 컨텐츠에 "청소 정책" 탭 추가 (기존 `tabSections` div 뒤에):

```html
<!-- 청소 정책 탭 -->
<div class="tab-pane fade" id="tabCleaningPolicy">
    <div class="card border-0 shadow-sm">
        <div class="card-body">
            <h6 class="fw-bold mb-3">룸타입별 청소 정책</h6>
            <p class="text-muted small mb-3">
                오버라이드가 없는 룸타입은 위 "일반 설정"의 기본값이 적용됩니다.
            </p>
            <table class="table table-hover mb-0" id="policyTable">
                <thead class="table-light">
                <tr>
                    <th>룸타입</th>
                    <th class="text-center">스테이오버</th>
                    <th class="text-center">턴다운</th>
                    <th class="text-center">DND</th>
                    <th class="text-center">상태</th>
                    <th class="text-center" style="width:100px;">관리</th>
                </tr>
                </thead>
                <tbody id="policyBody"></tbody>
            </table>
        </div>
    </div>
</div>
```

**변경 4:** 오버라이드 편집 모달 추가 (구역 모달 뒤에):

```html
<!-- 청소 정책 오버라이드 모달 -->
<div class="modal fade" id="policyModal" tabindex="-1">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title" id="policyModalTitle">청소 정책 오버라이드</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body">
                <input type="hidden" id="policyRoomTypeId">
                <div class="mb-3 text-muted small">비어있는 필드는 프로퍼티 기본값이 적용됩니다.</div>
                <div class="row mb-3">
                    <label class="col-sm-4 col-form-label">스테이오버 활성화</label>
                    <div class="col-sm-8">
                        <select class="form-select" id="polStayoverEnabled">
                            <option value="">기본값 사용</option>
                            <option value="true">ON</option>
                            <option value="false">OFF</option>
                        </select>
                    </div>
                </div>
                <div class="row mb-3">
                    <label class="col-sm-4 col-form-label">스테이오버 횟수</label>
                    <div class="col-sm-8">
                        <input type="number" class="form-control" id="polStayoverFrequency" min="1" max="3" placeholder="기본값 사용">
                    </div>
                </div>
                <div class="row mb-3">
                    <label class="col-sm-4 col-form-label">스테이오버 크레딧</label>
                    <div class="col-sm-8">
                        <input type="number" class="form-control" id="polStayoverCredit" step="0.1" min="0" placeholder="기본값 사용">
                    </div>
                </div>
                <div class="row mb-3">
                    <label class="col-sm-4 col-form-label">스테이오버 우선순위</label>
                    <div class="col-sm-8">
                        <select class="form-select" id="polStayoverPriority">
                            <option value="">기본값 사용</option>
                            <option value="RUSH">긴급</option>
                            <option value="HIGH">높음</option>
                            <option value="NORMAL">보통</option>
                            <option value="LOW">낮음</option>
                        </select>
                    </div>
                </div>
                <div class="row mb-3">
                    <label class="col-sm-4 col-form-label">턴다운 활성화</label>
                    <div class="col-sm-8">
                        <select class="form-select" id="polTurndownEnabled">
                            <option value="">기본값 사용</option>
                            <option value="true">ON</option>
                            <option value="false">OFF</option>
                        </select>
                    </div>
                </div>
                <div class="row mb-3">
                    <label class="col-sm-4 col-form-label">턴다운 크레딧</label>
                    <div class="col-sm-8">
                        <input type="number" class="form-control" id="polTurndownCredit" step="0.1" min="0" placeholder="기본값 사용">
                    </div>
                </div>
                <div class="row mb-3">
                    <label class="col-sm-4 col-form-label">DND 정책</label>
                    <div class="col-sm-8">
                        <select class="form-select" id="polDndPolicy">
                            <option value="">기본값 사용</option>
                            <option value="SKIP">스킵</option>
                            <option value="RETRY_AFTERNOON">오후 재시도</option>
                            <option value="FORCE_AFTER_DAYS">N일 후 강제</option>
                        </select>
                    </div>
                </div>
                <div class="row mb-3">
                    <label class="col-sm-4 col-form-label">DND 최대 스킵 (일)</label>
                    <div class="col-sm-8">
                        <input type="number" class="form-control" id="polDndMaxSkipDays" min="1" max="7" placeholder="기본값 사용">
                    </div>
                </div>
                <div class="row mb-3">
                    <label class="col-sm-4 col-form-label">메모</label>
                    <div class="col-sm-8">
                        <textarea class="form-control" id="polNote" rows="2"></textarea>
                    </div>
                </div>
            </div>
            <div class="modal-footer d-flex justify-content-between">
                <button type="button" class="btn btn-outline-danger" id="btnResetPolicy">
                    <i class="fas fa-undo me-1"></i>기본값��로 초기화
                </button>
                <div>
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">취소</button>
                    <button type="button" class="btn btn-primary" id="btnSavePolicy">저장</button>
                </div>
            </div>
        </div>
    </div>
</div>
```

---

### Task 3-5. hk-settings-page.js 확장

**파일:** `hola-app/src/main/resources/static/js/hk-settings-page.js`

**loadConfig() 확장** (line 66~75 뒤에):

```javascript
$('#cfgStayoverEnabled').prop('checked', c.stayoverEnabled);
$('#cfgStayoverFrequency').val(c.stayoverFrequency || 1);
$('#cfgTurndownEnabled').prop('checked', c.turndownEnabled);
$('#cfgDndPolicy').val(c.dndPolicy || 'SKIP');
$('#cfgDndMaxSkipDays').val(c.dndMaxSkipDays || 3);
$('#cfgOdTransitionTime').val(c.odTransitionTime || '05:00');
$('#cfgDailyTaskGenTime').val(c.dailyTaskGenTime || '06:00');
```

**saveConfig() 확장** (line 83~93 data 객체에):

```javascript
stayoverEnabled: $('#cfgStayoverEnabled').is(':checked'),
stayoverFrequency: parseInt($('#cfgStayoverFrequency').val()) || 1,
turndownEnabled: $('#cfgTurndownEnabled').is(':checked'),
dndPolicy: $('#cfgDndPolicy').val() || 'SKIP',
dndMaxSkipDays: parseInt($('#cfgDndMaxSkipDays').val()) || 3,
dailyTaskGenTime: $('#cfgDailyTaskGenTime').val() || '06:00',
odTransitionTime: $('#cfgOdTransitionTime').val() || '05:00'
```

**신규 함수 추가 (HkSettings 객체 안):**

```javascript
// === 청소 정책 ===

loadPolicies: function () {
    var self = this;
    HolaPms.ajax({
        url: '/api/v1/properties/' + self.propertyId + '/hk-cleaning-policies',
        method: 'GET',
        success: function (res) {
            if (res.success) {
                self.policies = res.data || [];
                self.renderPolicyTable();
            }
        }
    });
},

renderPolicyTable: function () {
    var $body = $('#policyBody');
    $body.empty();

    if (this.policies.length === 0) {
        $body.append('<tr><td colspan="6" class="text-center text-muted py-3">등록된 룸타입이 없습니다.</td></tr>');
        return;
    }

    this.policies.forEach(function (p) {
        var badge = p.overridden
            ? '<span class="badge bg-primary">오버라이드</span>'
            : '<span class="badge bg-secondary">기본값</span>';
        var stayover = p.overridden && p.stayoverEnabled != null
            ? (p.stayoverEnabled ? (p.stayoverFrequency || '-') + '회/일' : 'OFF')
            : '-';
        var turndown = p.overridden && p.turndownEnabled != null
            ? (p.turndownEnabled ? 'ON' : 'OFF')
            : '-';
        var dnd = p.overridden && p.dndPolicy
            ? p.dndPolicy
            : '-';

        $body.append(
            '<tr>' +
            '<td>' + HolaPms.escapeHtml(p.roomTypeCode || '') +
                ' <span class="text-muted small">(' + HolaPms.escapeHtml(p.roomTypeName || '') + ')</span></td>' +
            '<td class="text-center">' + stayover + '</td>' +
            '<td class="text-center">' + turndown + '</td>' +
            '<td class="text-center">' + dnd + '</td>' +
            '<td class="text-center">' + badge + '</td>' +
            '<td class="text-center">' +
                '<button class="btn btn-sm btn-outline-primary btn-edit-policy" data-room-type-id="' + p.roomTypeId + '">' +
                    '<i class="fas fa-edit"></i>' +
                '</button>' +
            '</td>' +
            '</tr>'
        );
    });
},

openPolicyModal: function (roomTypeId) {
    var self = this;
    var policy = self.policies.find(function (p) { return p.roomTypeId === roomTypeId; });
    if (!policy) return;

    $('#policyModalTitle').text(
        (policy.roomTypeCode || '') + ' 청소 정책' + (policy.overridden ? ' (오버라이드)' : ' (기본값)')
    );
    $('#policyRoomTypeId').val(roomTypeId);

    // 오버라이드 값 채우기 (null이면 빈 값 = "기본값 사용")
    $('#polStayoverEnabled').val(policy.stayoverEnabled != null ? String(policy.stayoverEnabled) : '');
    $('#polStayoverFrequency').val(policy.stayoverFrequency || '');
    $('#polStayoverCredit').val(policy.stayoverCredit || '');
    $('#polStayoverPriority').val(policy.stayoverPriority || '');
    $('#polTurndownEnabled').val(policy.turndownEnabled != null ? String(policy.turndownEnabled) : '');
    $('#polTurndownCredit').val(policy.turndownCredit || '');
    $('#polDndPolicy').val(policy.dndPolicy || '');
    $('#polDndMaxSkipDays').val(policy.dndMaxSkipDays || '');
    $('#polNote').val(policy.note || '');

    // "기본값으로 초기화" 버튼: 오버라이드가 있을 때만 표시
    $('#btnResetPolicy').toggle(policy.overridden);

    HolaPms.modal.show('#policyModal');
},

savePolicy: function () {
    var self = this;
    var roomTypeId = parseInt($('#policyRoomTypeId').val());

    // 빈 문자열 → null (기본값 사용)
    var toNull = function (v) { return v === '' || v === undefined ? null : v; };
    var toBool = function (v) { return v === 'true' ? true : v === 'false' ? false : null; };

    var data = {
        roomTypeId: roomTypeId,
        stayoverEnabled: toBool($('#polStayoverEnabled').val()),
        stayoverFrequency: toNull($('#polStayoverFrequency').val()) ? parseInt($('#polStayoverFrequency').val()) : null,
        stayoverCredit: toNull($('#polStayoverCredit').val()) ? parseFloat($('#polStayoverCredit').val()) : null,
        stayoverPriority: toNull($('#polStayoverPriority').val()),
        turndownEnabled: toBool($('#polTurndownEnabled').val()),
        turndownCredit: toNull($('#polTurndownCredit').val()) ? parseFloat($('#polTurndownCredit').val()) : null,
        dndPolicy: toNull($('#polDndPolicy').val()),
        dndMaxSkipDays: toNull($('#polDndMaxSkipDays').val()) ? parseInt($('#polDndMaxSkipDays').val()) : null,
        note: toNull($('#polNote').val())
    };

    HolaPms.ajax({
        url: '/api/v1/properties/' + self.propertyId + '/hk-cleaning-policies',
        method: 'POST',
        data: JSON.stringify(data),
        success: function (res) {
            if (res.success) {
                HolaPms.modal.hide('#policyModal');
                HolaPms.alert('success', '정책이 저장되었습니다.');
                self.loadPolicies();
            }
        }
    });
},

resetPolicy: function () {
    var self = this;
    var roomTypeId = parseInt($('#policyRoomTypeId').val());
    if (!confirm('이 룸타입의 오버라이드를 삭제하고 프로퍼티 기본값으로 복귀하시겠습니까?')) return;

    HolaPms.ajax({
        url: '/api/v1/properties/' + self.propertyId + '/hk-cleaning-policies/' + roomTypeId,
        method: 'DELETE',
        success: function (res) {
            if (res.success) {
                HolaPms.modal.hide('#policyModal');
                HolaPms.alert('success', '기본값으로 초기화되었습니다.');
                self.loadPolicies();
            }
        }
    });
}
```

**bindEvents() 확장:**

```javascript
// 청소 정책
$(document).on('click', '.btn-edit-policy', function () {
    self.openPolicyModal($(this).data('room-type-id'));
});
$('#btnSavePolicy').on('click', function () { self.savePolicy(); });
$('#btnResetPolicy').on('click', function () { self.resetPolicy(); });
```

**reload() 확장** (line 51~54 뒤에):

```javascript
this.loadPolicies();
```

**init 변수 추가:**

```javascript
policies: [],
```

---

## 파일 변경 요약

| 구분 | 파일 | 작업 |
|------|------|------|
| **신규** | `V8_10_0__add_stayover_cleaning_policy.sql` | Flyway 마이그레이션 |
| **신규** | `HkCleaningPolicy.java` | 엔티티 |
| **신규** | `HkCleaningPolicyRepository.java` | 레포지토리 |
| **신규** | `HkCleaningPolicyRequest.java` | 요청 DTO |
| **신규** | `HkCleaningPolicyResponse.java` | 응답 DTO |
| **신규** | `ResolvedCleaningPolicy.java` | 해석 결과 DTO |
| **신규** | `HkCleaningPolicyService.java` | 서비스 인터페이스 |
| **신규** | `HkCleaningPolicyServiceImpl.java` | 서비스 구현 |
| **신규** | `HkCleaningPolicyApiController.java` | REST API |
| **신규** | `HkSchedulerService.java` | 스케줄러 |
| **수정** | `HkConfig.java` | 7개 필드 + update() 확장 |
| **수정** | `HkTask.java` | 3개 필드 추가 |
| **수정** | `RoomNumber.java` | 2개 필드 + 3개 메서드 |
| **수정** | `RoomNumberRepository.java` | 3개 쿼리 추가 |
| **수정** | `HkConfigUpdateRequest.java` | 7개 필드 추가 |
| **수정** | `HkConfigResponse.java` | 7개 필드 추가 |
| **수정** | `HkTaskMapper.java` | toResponse(HkConfig) 7개 필드 |
| **수정** | `HousekeepingService.java` | 3개 메서드 추가 |
| **수정** | `HousekeepingServiceImpl.java` | 3개 구현 + updateConfig/getConfig 수정 |
| **수정** | `HousekeepingApiController.java` | 3개 엔드포인트 추가 |
| **수정** | `RoomStatusService.java` | calcStatusCode DND 추가 |
| **수정** | `HkDashboardResponse.java` | RoomStatusSummary DND 필드 |
| **수정** | `ErrorCode.java` | 2개 에러코드 추가 |
| **수정** | `HolaPmsApplication.java` | @EnableScheduling |
| **수정** | `settings.html` | 탭 + 폼 + 모달 추가 |
| **수정** | `hk-settings-page.js` | 정책 CRUD + loadConfig/saveConfig 확장 |

**총: 신규 10개, 수정 16개 = 26개 파일**
