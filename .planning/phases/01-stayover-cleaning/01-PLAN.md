# Phase 1: Stayover Cleaning Management - Implementation Plan

**Created:** 2026-03-26
**Status:** Ready for review
**Estimated Plans:** 5 (Wave 1~3)

---

## Overview

투숙 중 객실 청소 관리 기능. "기본값 + 룸타입별 오버라이드" 구조로 100+ 프로퍼티의 다양한 청소 정책을 설정 기반으로 대응.

### Architecture

```
HkConfig (프로퍼티 기본값)           ← 기존 테이블 확장
    │
    ├── stayoverFrequency: 1        (1일 N회, 기본 1)
    ├── stayoverEnabled: true       (스테이오버 자동 생성 여부)
    ├── turndownEnabled: false      (턴다운 자동 생성 여부)
    ├── dndPolicy: SKIP             (SKIP | RETRY_AFTERNOON | FORCE_AFTER_DAYS)
    ├── dndMaxSkipDays: 3           (FORCE_AFTER_DAYS 시 강제 청소까지 일수)
    ├── dailyTaskGenTime: "06:00"   (일일 작업 자동 생성 시각)
    └── odTransitionTime: "05:00"   (OC→OD 전환 시각)

HkCleaningPolicy (룸타입별 오버라이드)  ← 신규 테이블
    │
    ├── propertyId + roomTypeId (복합 유니크)
    ├── stayoverFrequency: 2        (이 룸타입은 1일 2회)
    ├── stayoverEnabled: true
    ├── turndownEnabled: true       (스위트룸은 턴다운도 제공)
    ├── stayoverCredit: 0.8         (스위트룸은 크레딧 더 높음)
    ├── turndownCredit: 0.5
    ├── stayoverPriority: HIGH      (기본 우선순위)
    └── dndPolicy: null             (null = 프로퍼티 기본값 사용)
```

### Task Generation Flow

```
[매일 05:00] OC→OD 일괄 전환 (configurable per property)
      │
[매일 06:00] 정책 기반 작업 자동 생성
      │
      ├── 각 OD 객실에 대해:
      │     1. roomTypeId로 HkCleaningPolicy 조회
      │     2. 없으면 → HkConfig 기본값 사용
      │     3. stayoverEnabled 체크
      │     4. stayoverFrequency만큼 작업 생성 (시간대 분배)
      │     5. DND 체크 (hkStatus == DND이면 정책에 따라 처리)
      │
      └── 기존 generateDailyTasks()는 "수동 실행" 용도로 유지
           (스케줄러가 동일 로직을 자동 실행)
```

---

## Wave 1: Schema + Entity (의존성 없음)

### Plan 1-1: DB Migration — HkConfig 확장 + HkCleaningPolicy 신규

**목적:** 프로퍼티 기본 정책 필드 추가 및 룸타입별 오버라이드 테이블 생성

<files_to_modify>
- `hola-app/src/main/resources/db/migration/V8_5_0__add_stayover_cleaning_policy.sql` (신규)
</files_to_modify>

<read_first>
- `hola-app/src/main/resources/db/migration/V8_1_0__create_hk_config_table.sql`
- `hola-app/src/main/resources/db/migration/V8_2_0__create_hk_task_tables.sql`
- `hola-hotel/src/main/java/com/hola/hotel/entity/HkConfig.java`
</read_first>

<action>
Flyway 마이그레이션 V8_5_0 작성:

**1) hk_config 테이블에 컬럼 추가:**

```sql
-- 스테이오버 정책 기본값
ALTER TABLE hk_config ADD COLUMN stayover_frequency INTEGER DEFAULT 1;
ALTER TABLE hk_config ADD COLUMN stayover_enabled BOOLEAN DEFAULT false;
ALTER TABLE hk_config ADD COLUMN turndown_enabled BOOLEAN DEFAULT false;

-- DND 정책
ALTER TABLE hk_config ADD COLUMN dnd_policy VARCHAR(30) DEFAULT 'SKIP';
ALTER TABLE hk_config ADD COLUMN dnd_max_skip_days INTEGER DEFAULT 3;

-- 스케줄러 시간
ALTER TABLE hk_config ADD COLUMN daily_task_gen_time VARCHAR(5) DEFAULT '06:00';
ALTER TABLE hk_config ADD COLUMN od_transition_time VARCHAR(5) DEFAULT '05:00';

COMMENT ON COLUMN hk_config.stayover_frequency IS '1일 스테이오버 횟수 (기본 1)';
COMMENT ON COLUMN hk_config.stayover_enabled IS '스테이오버 자동 생성 활성화';
COMMENT ON COLUMN hk_config.turndown_enabled IS '턴다운 자동 생성 활성화';
COMMENT ON COLUMN hk_config.dnd_policy IS 'DND 정책: SKIP, RETRY_AFTERNOON, FORCE_AFTER_DAYS';
COMMENT ON COLUMN hk_config.dnd_max_skip_days IS 'FORCE_AFTER_DAYS 시 강제 청소까지 최대 일수';
COMMENT ON COLUMN hk_config.daily_task_gen_time IS '일일 작업 자동 생성 시각 (HH:mm)';
COMMENT ON COLUMN hk_config.od_transition_time IS 'OC→OD 일괄 전환 시각 (HH:mm)';
```

**2) hk_cleaning_policy 테이블 신규 생성:**

```sql
CREATE TABLE hk_cleaning_policy (
    id              BIGSERIAL PRIMARY KEY,
    property_id     BIGINT NOT NULL,
    room_type_id    BIGINT NOT NULL,

    -- 청소 정책 오버라이드 (null = 프로퍼티 기본값 사용)
    stayover_enabled    BOOLEAN,
    stayover_frequency  INTEGER,
    turndown_enabled    BOOLEAN,
    stayover_credit     DECIMAL(3,1),
    turndown_credit     DECIMAL(3,1),
    stayover_priority   VARCHAR(10),
    dnd_policy          VARCHAR(30),
    dnd_max_skip_days   INTEGER,

    -- 메모
    note            VARCHAR(500),

    -- BaseEntity 필드
    use_yn          BOOLEAN DEFAULT true,
    sort_order      INTEGER DEFAULT 0,
    created_at      TIMESTAMP DEFAULT NOW(),
    created_by      VARCHAR(50),
    updated_at      TIMESTAMP DEFAULT NOW(),
    updated_by      VARCHAR(50),
    deleted_at      TIMESTAMP,

    CONSTRAINT uk_hk_cleaning_policy UNIQUE (property_id, room_type_id)
);

CREATE INDEX idx_hk_cleaning_policy_property ON hk_cleaning_policy (property_id);

COMMENT ON TABLE hk_cleaning_policy IS '룸타입별 청소 정책 오버라이드';
COMMENT ON COLUMN hk_cleaning_policy.stayover_enabled IS 'null=프로퍼티 기본값, true/false=오버라이드';
COMMENT ON COLUMN hk_cleaning_policy.stayover_frequency IS 'null=프로퍼티 기본값, 정수=오버라이드';
COMMENT ON COLUMN hk_cleaning_policy.stayover_priority IS 'null=NORMAL, RUSH/HIGH/NORMAL/LOW';
```

**3) hk_task 테이블에 DND 추적 컬럼 추가:**

```sql
ALTER TABLE hk_task ADD COLUMN dnd_skipped BOOLEAN DEFAULT false;
ALTER TABLE hk_task ADD COLUMN dnd_skip_count INTEGER DEFAULT 0;
ALTER TABLE hk_task ADD COLUMN scheduled_time VARCHAR(5);

COMMENT ON COLUMN hk_task.dnd_skipped IS 'DND로 인해 스킵된 작업';
COMMENT ON COLUMN hk_task.dnd_skip_count IS 'DND 연속 스킵 횟수';
COMMENT ON COLUMN hk_task.scheduled_time IS '예정 청소 시간대 (HH:mm)';
```

**4) RoomNumber에 DND 상태 추가:**

```sql
-- hkStatus에 'DND' 값 추가 (기존: CLEAN, DIRTY, INSPECTED, PICKUP, OOO, OOS)
-- DND는 투숙객이 방해금지를 설정한 상태
COMMENT ON COLUMN htl_room_number.hk_status IS '청소상태: CLEAN, DIRTY, INSPECTED, PICKUP, DND, OOO, OOS';

-- DND 추적 컬럼
ALTER TABLE htl_room_number ADD COLUMN dnd_since DATE;
ALTER TABLE htl_room_number ADD COLUMN consecutive_dnd_days INTEGER DEFAULT 0;

COMMENT ON COLUMN htl_room_number.dnd_since IS 'DND 시작 날짜';
COMMENT ON COLUMN htl_room_number.consecutive_dnd_days IS 'DND 연속 일수';
```
</action>

<acceptance_criteria>
- V8_5_0 마이그레이션 파일이 존재하고 ./gradlew build 성공
- hk_config에 stayover_frequency, stayover_enabled, turndown_enabled, dnd_policy, dnd_max_skip_days, daily_task_gen_time, od_transition_time 컬럼 존재
- hk_cleaning_policy 테이블이 (property_id, room_type_id) 유니크 제약 조건을 가짐
- hk_task에 dnd_skipped, dnd_skip_count, scheduled_time 컬럼 존재
- htl_room_number에 dnd_since, consecutive_dnd_days 컬럼 존재
</acceptance_criteria>

---

### Plan 1-2: Entity + Repository + DTO

**목적:** HkCleaningPolicy 엔티티, HkConfig 확장, 관련 DTO/Mapper 생성

<files_to_modify>
- `hola-hotel/src/main/java/com/hola/hotel/entity/HkCleaningPolicy.java` (신규)
- `hola-hotel/src/main/java/com/hola/hotel/entity/HkConfig.java` (수정)
- `hola-hotel/src/main/java/com/hola/hotel/entity/HkTask.java` (수정)
- `hola-hotel/src/main/java/com/hola/hotel/entity/RoomNumber.java` (수정)
- `hola-hotel/src/main/java/com/hola/hotel/repository/HkCleaningPolicyRepository.java` (신규)
- `hola-hotel/src/main/java/com/hola/hotel/dto/request/HkCleaningPolicyRequest.java` (신규)
- `hola-hotel/src/main/java/com/hola/hotel/dto/response/HkCleaningPolicyResponse.java` (신규)
- `hola-hotel/src/main/java/com/hola/hotel/dto/request/HkConfigUpdateRequest.java` (수정)
- `hola-hotel/src/main/java/com/hola/hotel/dto/response/HkConfigResponse.java` (수정)
</files_to_modify>

<read_first>
- `hola-hotel/src/main/java/com/hola/hotel/entity/HkConfig.java`
- `hola-hotel/src/main/java/com/hola/hotel/entity/HkTask.java`
- `hola-hotel/src/main/java/com/hola/hotel/entity/RoomNumber.java`
- `hola-hotel/src/main/java/com/hola/hotel/dto/request/HkConfigUpdateRequest.java`
- `hola-hotel/src/main/java/com/hola/hotel/dto/response/HkConfigResponse.java`
</read_first>

<action>
**1) HkCleaningPolicy 엔티티 신규 생성:**

```java
@Entity
@Table(name = "hk_cleaning_policy")
@SQLRestriction("deleted_at IS NULL")
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor @Builder
public class HkCleaningPolicy extends BaseEntity {

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "room_type_id", nullable = false)
    private Long roomTypeId;

    // 청소 정책 오버라이드 (null = 프로퍼티 기본값 사용)
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

    public void update(...) { /* 필드별 setter */ }
}
```

**2) HkConfig에 신규 필드 추가:**
- stayoverFrequency (Integer, default 1)
- stayoverEnabled (Boolean, default false) — 기존 autoCreateStayover를 대체/보완
- turndownEnabled (Boolean, default false)
- dndPolicy (String, default "SKIP")
- dndMaxSkipDays (Integer, default 3)
- dailyTaskGenTime (String, default "06:00")
- odTransitionTime (String, default "05:00")
- update() 메서드에 신규 필드 추가

**3) HkTask에 DND 관련 필드 추가:**
- dndSkipped (Boolean, default false)
- dndSkipCount (Integer, default 0)
- scheduledTime (String)

**4) RoomNumber에 DND 관련 필드 추가:**
- dndSince (LocalDate)
- consecutiveDndDays (Integer, default 0)
- setDnd() 메서드: hkStatus="DND", dndSince=today
- clearDnd() 메서드: hkStatus="DIRTY", dndSince=null, consecutiveDndDays=0

**5) Repository:**
```java
public interface HkCleaningPolicyRepository extends JpaRepository<HkCleaningPolicy, Long> {
    List<HkCleaningPolicy> findByPropertyId(Long propertyId);
    Optional<HkCleaningPolicy> findByPropertyIdAndRoomTypeId(Long propertyId, Long roomTypeId);
    void deleteByPropertyIdAndRoomTypeId(Long propertyId, Long roomTypeId);
}
```

**6) DTO:** Request/Response에 모든 새 필드 포함. Response에 roomTypeName 추가 (조회 시 조인).
</action>

<acceptance_criteria>
- HkCleaningPolicy 엔티티가 BaseEntity 상속, @SQLRestriction 적용
- HkConfig에 7개 신규 필드 추가됨
- HkCleaningPolicyRepository에 findByPropertyId, findByPropertyIdAndRoomTypeId 메서드 존재
- 모든 DTO에 신규 필드 포함
- ./gradlew compileJava 성공
</acceptance_criteria>

---

## Wave 2: Service Layer (Wave 1 의존)

### Plan 2-1: 정책 해석 엔진 + CRUD 서비스

**목적:** "기본값 + 오버라이드" 정책 해석 로직과 정책 CRUD 서비스

<files_to_modify>
- `hola-hotel/src/main/java/com/hola/hotel/service/HkCleaningPolicyService.java` (신규)
- `hola-hotel/src/main/java/com/hola/hotel/service/HkCleaningPolicyServiceImpl.java` (신규)
- `hola-hotel/src/main/java/com/hola/hotel/dto/response/ResolvedCleaningPolicy.java` (신규)
</files_to_modify>

<read_first>
- `hola-hotel/src/main/java/com/hola/hotel/service/HousekeepingServiceImpl.java`
- `hola-hotel/src/main/java/com/hola/hotel/entity/HkConfig.java`
- `hola-hotel/src/main/java/com/hola/hotel/entity/HkCleaningPolicy.java` (Plan 1-2에서 생성)
</read_first>

<action>
**1) ResolvedCleaningPolicy DTO:**

정책 해석 결과를 담는 불변 객체. HkConfig + HkCleaningPolicy를 병합한 최종 정책.

```java
@Getter @Builder
public class ResolvedCleaningPolicy {
    private final boolean stayoverEnabled;
    private final int stayoverFrequency;
    private final boolean turndownEnabled;
    private final BigDecimal stayoverCredit;
    private final BigDecimal turndownCredit;
    private final String stayoverPriority;
    private final String dndPolicy;        // SKIP, RETRY_AFTERNOON, FORCE_AFTER_DAYS
    private final int dndMaxSkipDays;
    private final boolean isOverridden;    // 오버라이드 적용 여부 (UI 표시용)
}
```

**2) HkCleaningPolicyService 인터페이스:**

```java
public interface HkCleaningPolicyService {
    // 정책 해석: roomTypeId에 대한 최종 정책 반환
    ResolvedCleaningPolicy resolvePolicy(Long propertyId, Long roomTypeId);

    // CRUD
    List<HkCleaningPolicyResponse> getPolicies(Long propertyId);
    HkCleaningPolicyResponse getPolicy(Long propertyId, Long roomTypeId);
    HkCleaningPolicyResponse createOrUpdate(Long propertyId, HkCleaningPolicyRequest request);
    void deletePolicy(Long propertyId, Long roomTypeId);

    // 일괄 조회: 프로퍼티의 모든 룸타입에 대한 해석된 정책 목록
    List<ResolvedCleaningPolicyWithRoomType> getAllResolvedPolicies(Long propertyId);
}
```

**3) HkCleaningPolicyServiceImpl 핵심 로직:**

```java
public ResolvedCleaningPolicy resolvePolicy(Long propertyId, Long roomTypeId) {
    HkConfig config = hkConfigRepository.findByPropertyId(propertyId)
            .orElseGet(() -> createDefaultConfig(propertyId));

    // 룸타입별 오버라이드 조회
    HkCleaningPolicy override = hkCleaningPolicyRepository
            .findByPropertyIdAndRoomTypeId(propertyId, roomTypeId)
            .orElse(null);

    boolean isOverridden = (override != null);

    return ResolvedCleaningPolicy.builder()
            // 각 필드: override가 null이 아니면 override 값 사용, 아니면 config 기본값
            .stayoverEnabled(resolve(override, HkCleaningPolicy::getStayoverEnabled,
                                     config.getStayoverEnabled()))
            .stayoverFrequency(resolve(override, HkCleaningPolicy::getStayoverFrequency,
                                       config.getStayoverFrequency()))
            .turndownEnabled(resolve(override, HkCleaningPolicy::getTurndownEnabled,
                                     config.getTurndownEnabled()))
            .stayoverCredit(resolve(override, HkCleaningPolicy::getStayoverCredit,
                                    config.getDefaultStayoverCredit()))
            .turndownCredit(resolve(override, HkCleaningPolicy::getTurndownCredit,
                                    config.getDefaultTurndownCredit()))
            .stayoverPriority(resolve(override, HkCleaningPolicy::getStayoverPriority,
                                      "NORMAL"))
            .dndPolicy(resolve(override, HkCleaningPolicy::getDndPolicy,
                               config.getDndPolicy()))
            .dndMaxSkipDays(resolve(override, HkCleaningPolicy::getDndMaxSkipDays,
                                    config.getDndMaxSkipDays()))
            .isOverridden(isOverridden)
            .build();
}

// 제네릭 resolve: override 값이 null이면 기본값 사용
private <T> T resolve(HkCleaningPolicy override, Function<HkCleaningPolicy, T> getter, T defaultValue) {
    if (override == null) return defaultValue;
    T value = getter.apply(override);
    return value != null ? value : defaultValue;
}
```

**4) getAllResolvedPolicies**: 프로퍼티의 모든 룸타입 목록 조회 → 각각에 resolvePolicy 적용.
roomTypeId → roomTypeName 매핑은 hola-room의 RoomType을 Long ID로 조회하는 별도 쿼리.
</action>

<acceptance_criteria>
- resolvePolicy()가 오버라이드 없는 룸타입에 대해 HkConfig 기본값 반환
- resolvePolicy()가 오버라이드 있는 룸타입에 대해 오버라이드 값 반환 (null 필드는 기본값 폴백)
- getAllResolvedPolicies()가 프로퍼티 내 모든 룸타입에 대해 해석된 정책 반환
- isOverridden 플래그가 정확히 설정됨
- CRUD 동작: 생성/수정/삭제/조회
- ./gradlew compileJava 성공
</acceptance_criteria>

---

### Plan 2-2: 스테이오버 작업 생성 엔진 + OD 전환 로직

**목적:** 정책 기반 스테이오버 작업 자동 생성 로직 및 OC→OD 일괄 전환

<files_to_modify>
- `hola-hotel/src/main/java/com/hola/hotel/service/HousekeepingServiceImpl.java` (수정)
- `hola-hotel/src/main/java/com/hola/hotel/service/HousekeepingService.java` (수정)
- `hola-hotel/src/main/java/com/hola/hotel/repository/RoomNumberRepository.java` (수정)
</files_to_modify>

<read_first>
- `hola-hotel/src/main/java/com/hola/hotel/service/HousekeepingServiceImpl.java` (전체)
- `hola-hotel/src/main/java/com/hola/hotel/service/HkCleaningPolicyServiceImpl.java` (Plan 2-1)
- `hola-hotel/src/main/java/com/hola/hotel/repository/RoomNumberRepository.java`
- `hola-hotel/src/main/java/com/hola/hotel/entity/RoomNumber.java`
</read_first>

<action>
**1) RoomNumberRepository에 쿼리 추가:**

```java
// OC 상태 객실 조회 (일일 OD 전환 대상)
@Query("SELECT r FROM RoomNumber r WHERE r.propertyId = :propertyId " +
       "AND r.foStatus = 'OCCUPIED' AND r.hkStatus = 'CLEAN'")
List<RoomNumber> findOccupiedCleanRooms(@Param("propertyId") Long propertyId);

// DND 상태 객실 조회
@Query("SELECT r FROM RoomNumber r WHERE r.propertyId = :propertyId " +
       "AND r.foStatus = 'OCCUPIED' AND r.hkStatus = 'DND'")
List<RoomNumber> findOccupiedDndRooms(@Param("propertyId") Long propertyId);

// 투숙 중 + DIRTY 객실 (roomTypeId 포함 조회를 위한 네이티브 쿼리)
@Query(value = "SELECT rn.*, rtf.room_type_id FROM htl_room_number rn " +
       "JOIN rm_room_type_floor rtf ON rn.floor_id = rtf.floor_id AND rn.property_id = rtf.property_id " +
       "WHERE rn.property_id = :propertyId AND rn.fo_status = 'OCCUPIED' " +
       "AND rn.hk_status = 'DIRTY' AND rn.deleted_at IS NULL", nativeQuery = true)
List<Object[]> findOccupiedDirtyRoomsWithRoomType(@Param("propertyId") Long propertyId);
```

**2) HousekeepingService 인터페이스에 메서드 추가:**

```java
/** OC→OD 일괄 전환. 전환된 객실 수 반환 */
int transitionOccupiedRoomsToDirty(Long propertyId);

/** 정책 기반 스테이오버 작업 생성. 생성된 작업 수 반환 */
int generateStayoverTasks(Long propertyId, LocalDate date);

/** DND 처리: 정책에 따라 스킵/재시도/강제 청소 처리. 처리 결과 반환 */
DndProcessResult processDndRooms(Long propertyId, LocalDate date);
```

**3) transitionOccupiedRoomsToDirty 구현:**

```java
@Transactional
public int transitionOccupiedRoomsToDirty(Long propertyId) {
    List<RoomNumber> ocRooms = roomNumberRepository.findOccupiedCleanRooms(propertyId);
    for (RoomNumber room : ocRooms) {
        room.updateHkStatus("DIRTY", null);
    }
    // DND 객실: consecutiveDndDays 증가
    List<RoomNumber> dndRooms = roomNumberRepository.findOccupiedDndRooms(propertyId);
    for (RoomNumber room : dndRooms) {
        room.incrementDndDays(); // consecutiveDndDays++
    }
    log.info("OC→OD 전환: propertyId={}, 전환={}건, DND={}건",
             propertyId, ocRooms.size(), dndRooms.size());
    return ocRooms.size();
}
```

**4) generateStayoverTasks 구현 (정책 기반):**

```java
@Transactional
public int generateStayoverTasks(Long propertyId, LocalDate date) {
    List<Object[]> odRoomsWithType = roomNumberRepository.findOccupiedDirtyRoomsWithRoomType(propertyId);
    int createdCount = 0;

    for (Object[] row : odRoomsWithType) {
        Long roomNumberId = ((Number) row[0]).longValue();  // rn.id
        Long roomTypeId = ((Number) row[lastIdx]).longValue();  // rtf.room_type_id

        // 정책 해석
        ResolvedCleaningPolicy policy = cleaningPolicyService.resolvePolicy(propertyId, roomTypeId);

        if (!policy.isStayoverEnabled()) continue;

        // 이미 오늘 활성 작업이 있으면 스킵
        if (hkTaskRepository.existsActiveTaskByRoomNumberIdAndTaskDate(roomNumberId, date)) continue;

        // frequency에 따라 작업 생성
        for (int i = 0; i < policy.getStayoverFrequency(); i++) {
            HkTask task = HkTask.builder()
                    .propertyId(propertyId)
                    .roomNumberId(roomNumberId)
                    .taskType("STAYOVER")
                    .taskDate(date)
                    .priority(policy.getStayoverPriority())
                    .credit(policy.getStayoverCredit())
                    .scheduledTime(calculateScheduledTime(i, policy.getStayoverFrequency()))
                    .build();
            applyRushPriority(task, propertyId);
            hkTaskRepository.save(task);
            createdCount++;
        }
    }
    return createdCount;
}

// 시간대 분배: frequency=1이면 "10:00", frequency=2이면 "10:00"/"16:00"
private String calculateScheduledTime(int index, int total) {
    int startHour = 10;
    int endHour = 18;
    int interval = (endHour - startHour) / total;
    int hour = startHour + (interval * index);
    return String.format("%02d:00", hour);
}
```

**5) processDndRooms 구현:**

```java
@Transactional
public DndProcessResult processDndRooms(Long propertyId, LocalDate date) {
    List<RoomNumber> dndRooms = roomNumberRepository.findOccupiedDndRooms(propertyId);
    int skipped = 0, forced = 0, retried = 0;

    for (RoomNumber room : dndRooms) {
        Long roomTypeId = getRoomTypeId(room);
        ResolvedCleaningPolicy policy = cleaningPolicyService.resolvePolicy(propertyId, roomTypeId);

        switch (policy.getDndPolicy()) {
            case "SKIP":
                skipped++;
                break;
            case "RETRY_AFTERNOON":
                // 오후 시간대로 STAYOVER 작업 생성 (스킵 마킹)
                createDndRetryTask(propertyId, room, date, policy);
                retried++;
                break;
            case "FORCE_AFTER_DAYS":
                if (room.getConsecutiveDndDays() >= policy.getDndMaxSkipDays()) {
                    // 강제 청소: DND 해제, DIRTY 전환, 작업 생성
                    room.clearDnd();
                    createStayoverTask(propertyId, room, date, policy, "HIGH");
                    forced++;
                } else {
                    skipped++;
                }
                break;
        }
    }
    return new DndProcessResult(skipped, retried, forced);
}
```
</action>

<acceptance_criteria>
- transitionOccupiedRoomsToDirty()가 OC 상태 객실을 OD로 전환
- generateStayoverTasks()가 정책의 stayoverEnabled=false인 룸타입 작업 미생성
- generateStayoverTasks()가 frequency=2일 때 2개 작업 생성 (시간대 분배)
- processDndRooms()가 SKIP/RETRY_AFTERNOON/FORCE_AFTER_DAYS 정책별 올바른 처리
- DND FORCE_AFTER_DAYS: consecutiveDndDays >= dndMaxSkipDays일 때 강제 청소 작업 생성
- 기존 generateDailyTasks()는 변경 없이 유지 (하위 호환)
- ./gradlew compileJava 성공
</acceptance_criteria>

---

## Wave 3: Scheduler + API + UI (Wave 2 의존)

### Plan 3-1: 스케줄러 + API 컨트롤러

**목적:** 일일 자동 작업 생성 스케줄러 및 정책 관리 REST API

<files_to_modify>
- `hola-hotel/src/main/java/com/hola/hotel/service/HkSchedulerService.java` (신규)
- `hola-hotel/src/main/java/com/hola/hotel/service/HkSchedulerServiceImpl.java` (신규)
- `hola-hotel/src/main/java/com/hola/hotel/controller/HkCleaningPolicyApiController.java` (신규)
- `hola-hotel/src/main/java/com/hola/hotel/controller/HousekeepingApiController.java` (수정)
- `hola-app/src/main/java/com/hola/app/HolaApplication.java` (수정 - @EnableScheduling)
</files_to_modify>

<read_first>
- `hola-hotel/src/main/java/com/hola/hotel/controller/HousekeepingApiController.java`
- `hola-hotel/src/main/java/com/hola/hotel/service/HousekeepingServiceImpl.java`
- `hola-app/src/main/java/com/hola/app/HolaApplication.java`
- `hola-hotel/src/main/java/com/hola/hotel/repository/HkConfigRepository.java`
</read_first>

<action>
**1) HolaApplication에 @EnableScheduling 추가**

**2) HkSchedulerServiceImpl:**

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class HkSchedulerServiceImpl implements HkSchedulerService {

    private final HkConfigRepository hkConfigRepository;
    private final HousekeepingService housekeepingService;
    private final PropertyRepository propertyRepository;

    /**
     * 매 분마다 실행 — 프로퍼티별 설정된 시간에 맞춰 작업 생성
     * (프로퍼티마다 odTransitionTime, dailyTaskGenTime이 다를 수 있으므로)
     */
    @Scheduled(cron = "0 * * * * *")  // 매 분 실행
    public void checkAndRunDailyTasks() {
        String currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        List<HkConfig> allConfigs = hkConfigRepository.findAll();

        for (HkConfig config : allConfigs) {
            try {
                // OC→OD 전환 시간 체크
                if (currentTime.equals(config.getOdTransitionTime())) {
                    int converted = housekeepingService.transitionOccupiedRoomsToDirty(config.getPropertyId());
                    log.info("스케줄러 OC→OD 전환: propertyId={}, 전환={}건", config.getPropertyId(), converted);
                }

                // 일일 작업 생성 시간 체크
                if (currentTime.equals(config.getDailyTaskGenTime())) {
                    // 스테이오버 작업 생성
                    if (Boolean.TRUE.equals(config.getStayoverEnabled())) {
                        int created = housekeepingService.generateStayoverTasks(
                                config.getPropertyId(), LocalDate.now());
                        log.info("스케줄러 스테이오버 작업 생성: propertyId={}, 생성={}건",
                                config.getPropertyId(), created);
                    }

                    // 기존 generateDailyTasks도 실행 (VD→CHECKOUT 포함)
                    if (Boolean.TRUE.equals(config.getAutoCreateCheckout())) {
                        housekeepingService.generateDailyTasks(config.getPropertyId(), LocalDate.now());
                    }

                    // DND 처리
                    housekeepingService.processDndRooms(config.getPropertyId(), LocalDate.now());
                }
            } catch (Exception e) {
                log.error("HK 스케줄러 오류: propertyId={}, error={}", config.getPropertyId(), e.getMessage(), e);
                // 한 프로퍼티 오류가 다른 프로퍼티에 영향을 주지 않도록 개별 try/catch
            }
        }
    }
}
```

**3) HkCleaningPolicyApiController:**

```java
@RestController
@RequestMapping("/api/v1/properties/{propertyId}/hk-cleaning-policies")
@RequiredArgsConstructor
public class HkCleaningPolicyApiController {

    // GET /                          → 전체 정책 목록 (오버라이드 + 기본값 표시)
    // GET /resolved                  → 모든 룸타입의 해석된 최종 정책
    // GET /{roomTypeId}              → 특정 룸타입 정책
    // POST /                         → 정책 생성/수정
    // DELETE /{roomTypeId}           → 오버라이드 삭제 (기본값으로 복귀)
}
```

**4) HousekeepingApiController에 수동 실행 엔드포인트 추가:**

```java
// POST /api/v1/properties/{propertyId}/housekeeping/transition-od
// — OC→OD 수동 전환 (스케줄러 대신 수동 실행용)

// POST /api/v1/properties/{propertyId}/housekeeping/generate-stayover-tasks
// — 스테이오버 작업 수동 생성

// POST /api/v1/properties/{propertyId}/housekeeping/process-dnd
// — DND 처리 수동 실행
```
</action>

<acceptance_criteria>
- @EnableScheduling이 HolaApplication에 적용됨
- 스케줄러가 매 분 실행되어 프로퍼티별 설정 시간에 작업 생성
- 한 프로퍼티 오류가 다른 프로퍼티에 영향 없음 (개별 try/catch)
- HkCleaningPolicy CRUD API 5개 엔드포인트 동작
- GET /resolved가 모든 룸타입에 대해 해석된 정책 반환 (isOverridden 포함)
- 수동 실행 엔드포인트 3개 동작
- ./gradlew compileJava 성공
</acceptance_criteria>

---

### Plan 3-2: Admin UI — HK 설정 페이지 확장

**목적:** 프로퍼티 기본 정책 설정 UI + 룸타입별 오버라이드 관리 UI

<files_to_modify>
- `hola-app/src/main/resources/templates/housekeeping/settings.html` (수정)
- `hola-app/src/main/resources/static/js/hk-settings-page.js` (수정)
</files_to_modify>

<read_first>
- `hola-app/src/main/resources/templates/housekeeping/settings.html`
- `hola-app/src/main/resources/static/js/hk-settings-page.js`
- `hola-app/src/main/resources/static/js/hola-common.js`
</read_first>

<action>
**1) settings.html 확장 — 3번째 탭 추가: "청소 정책"**

기존 탭: 일반 설정 | 구역 관리
추가 탭: **청소 정책**

```
┌─ 일반 설정 ─┬─ 구역 관리 ─┬─ 청소 정책 ─┐
│                                         │
│  [프로퍼티 기본 정책]                      │
│  ┌───────────────────────────────────┐   │
│  │ 스테이오버 자동 생성  [ON/OFF]       │   │
│  │ 기본 횟수           [1] 회/일       │   │
│  │ 턴다운 자동 생성     [ON/OFF]       │   │
│  │ DND 정책           [드롭다운]       │   │
│  │ DND 최대 스킵일수   [3] 일          │   │
│  │ OD 전환 시각        [05:00]        │   │
│  │ 작업 생성 시각       [06:00]        │   │
│  └───────────────────────────────────┘   │
│                                         │
│  [룸타입별 오버라이드]                      │
│  ┌──────────────────────────────────────┐│
│  │ 룸타입명    │ 스테이오버 │ 턴다운 │ 상태  ││
│  │───────────┼────────┼──────┼──────││
│  │ 스탠다드    │ 1회/일  │ OFF  │기본값 ││
│  │ 디럭스     │ 1회/일  │ OFF  │기본값 ││
│  │ 스위트     │ 2회/일  │ ON   │오버라이드││
│  │ 프레지덴셜  │ 2회/일  │ ON   │오버라이드││
│  └──────────────────────────────────────┘│
│                                         │
│  * "기본값" = 프로퍼티 설정 그대로 적용      │
│  * "오버라이드" 클릭 → 편집 모달           │
│  * [+ 오버라이드 추가] 버튼               │
│                                         │
└─────────────────────────────────────────┘
```

**2) 룸타입 오버라이드 편집 모달:**

```
┌─ [스위트] 청소 정책 오버라이드 ─────────────────┐
│                                              │
│  스테이오버 활성화    [ON]  ← null이면 기본값 사용  │
│  스테이오버 횟수      [2]                       │
│  스테이오버 크레딧    [0.8]                      │
│  스테이오버 우선순위  [HIGH ▼]                    │
│  턴다운 활성화       [ON]                       │
│  턴다운 크레딧       [0.5]                      │
│  DND 정책           [프로퍼티 기본값 ▼]           │
│                                              │
│  [기본값으로 초기화]          [취소] [저장]       │
└──────────────────────────────────────────────┘
```

- "기본값으로 초기화" 버튼: 오버라이드 삭제 (DELETE API 호출)
- 각 필드에 placeholder로 현재 프로퍼티 기본값 표시

**3) hk-settings-page.js 확장:**

- `loadCleaningPolicies()`: GET /resolved API 호출 → 테이블 렌더링
- 상태 뱃지: `isOverridden ? '<span class="badge bg-primary">오버라이드</span>' : '<span class="badge bg-secondary">기본값</span>'`
- 편집 모달: 오버라이드 필드별 입력, null 처리 (빈 값 = null = 기본값 사용)
- 기본 정책 저장: 기존 HkConfig update API에 신규 필드 포함

**4) 일반 설정 탭 확장:**

기존 설정에 신규 필드 추가:
- 스테이오버 활성화 토글 (기존 autoCreateStayover 대체)
- 기본 횟수 number input
- 턴다운 활성화 토글
- DND 정책 select
- DND 최대 스킵 일수
- OD 전환 시각 time input
- 작업 생성 시각 time input
</action>

<acceptance_criteria>
- HK 설정 페이지에 "청소 정책" 탭이 추가됨
- 프로퍼티 기본 정책 폼에 7개 신규 필드가 표시되고 저장됨
- 룸타입별 오버라이드 테이블에 모든 룸타입이 표시됨
- "기본값"/"오버라이드" 뱃지가 isOverridden 값에 따라 정확히 표시됨
- 오버라이드 편집 모달에서 값을 비우면 null로 저장됨 (기본값 복귀)
- "기본값으로 초기화" 버튼 클릭 시 오버라이드 삭제됨
- 모든 UI가 HolaPms 네임스페이스 패턴을 따름 (HolaPms.ajax, HolaPms.alert 등)
- card border-0 shadow-sm 스타일 적용
</acceptance_criteria>

---

## Risk & Considerations

### 1. 스케줄러 동시성 (위험도: 중간)
- **시나리오**: 스케줄러가 동시에 여러 프로퍼티 작업 생성 시 DB 부하
- **대응**: 프로퍼티별 순차 처리 (현재 설계). 필요시 프로퍼티 ID 기준 파티셔닝 가능
- **검증**: 로컬에서 100개 프로퍼티 시뮬레이션 실행 시간 측정

### 2. RoomType 크로스 모듈 참조 (위험도: 중간)
- **시나리오**: hola-room의 RoomType ID를 hola-hotel의 HkCleaningPolicy에서 참조
- **대응**: Long roomTypeId로 FK 없이 참조 (기존 크로스 모듈 원칙 준수). 삭제된 룸타입은 orphan 정책이 됨
- **검증**: 룸타입 삭제 시 관련 정책 자동 정리 필요 여부 확인

### 3. DND 상태 전환과 기존 hkStatus 충돌 (위험도: 높음)
- **시나리오**: DND를 hkStatus에 추가하면 기존 RoomStatusService.calcStatusCode()와 UI에 영향
- **대응**: calcStatusCode()에 DND 케이스 추가, 대시보드 roomStatusSummary에 DND 카운트 추가
- **검증**: 기존 HK 보드, 대시보드, 모바일 UI에서 DND 상태 정상 표시 확인

### 4. generateDailyTasks() 하위 호환 (위험도: 낮음)
- **시나리오**: 기존 "청소 작업 일괄 생성" 버튼이 의도대로 동작해야 함
- **대응**: 기존 메서드는 변경하지 않음. 새 generateStayoverTasks()를 별도 추가
- **검증**: 기존 대시보드 버튼 기능 변경 없음 확인

### 5. 멀티테넌시 (위험도: 낮음)
- **시나리오**: 스케줄러가 모든 테넌트의 HkConfig를 조회해야 함
- **대응**: TenantFilter는 HTTP 요청에만 적용. 스케줄러는 직접 쿼리로 우회 가능
- **검증**: 스케줄러에서 TenantContext 없이도 프로퍼티별 조회 가능 확인

---

## Summary

| Wave | Plan | 내용 | 의존 |
|------|------|------|------|
| 1 | 1-1 | DB Migration | 없음 |
| 1 | 1-2 | Entity/Repository/DTO | 1-1 |
| 2 | 2-1 | 정책 해석 엔진 + CRUD | 1-2 |
| 2 | 2-2 | 스테이오버 작업 생성 + OD 전환 | 2-1 |
| 3 | 3-1 | 스케줄러 + API | 2-2 |
| 3 | 3-2 | Admin UI | 3-1 |

**총 파일 변경 예상:**
- 신규: ~12 파일 (Entity, Service, Controller, Repository, DTO, Migration, JS, HTML 각 1-2)
- 수정: ~10 파일 (기존 Entity/Service/Controller/DTO/UI 확장)
