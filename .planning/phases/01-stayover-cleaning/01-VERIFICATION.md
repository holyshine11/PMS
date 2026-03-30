---
phase: 01-stayover-cleaning
verified: 2026-03-30T00:00:00Z
status: verified
score: 7/7 must-haves verified
gaps: []
---

# Phase 1: Stayover Cleaning Management Verification Report

**Phase Goal:** Implement occupied room (stayover) cleaning management with "Default + Override" policy architecture, automated OC to OD transitions, policy-based task generation, DND handling, and admin UI.
**Verified:** 2026-03-26T20:00:00Z
**Status:** gaps_found
**Re-verification:** No -- initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | HkCleaningPolicy entity follows BaseEntity pattern with soft delete and cross-module FK | VERIFIED | Entity extends BaseEntity, has @SQLRestriction("deleted_at IS NULL"), roomTypeId is Long (not @ManyToOne), unique constraint on (property_id, room_type_id) |
| 2 | V8_10_0 migration SQL matches entity field definitions exactly | VERIFIED | All 7 HkConfig columns, all HkCleaningPolicy columns, all 3 HkTask DND columns, and 2 RoomNumber DND columns match Java entity fields in name, type, and defaults |
| 3 | Policy resolution engine correctly merges defaults with overrides | VERIFIED | pick() generic helper returns override value if non-null, else falls back to HkConfig default. All 8 policy fields use this pattern. |
| 4 | OC to OD daily transition and DND day increment work correctly | VERIFIED | transitionOccupiedRoomsToDirty() queries OC rooms, sets hkStatus=DIRTY, and increments consecutiveDndDays for OCCUPIED+DND rooms. No accessControlService calls. |
| 5 | Policy-based stayover task auto-generation respects all policy fields | VERIFIED | generateStayoverTasks() queries OD rooms with roomTypeId, resolves policy, checks stayoverEnabled, respects frequency, credit, priority, and scheduledTime. Skips rooms with existing active tasks. |
| 6 | DND handling correctly implements all 3 policies (SKIP, RETRY_AFTERNOON, FORCE_AFTER_DAYS) | VERIFIED | processDndRooms() switches on dndPolicy: SKIP increments counter; RETRY_AFTERNOON creates task at 14:00; FORCE_AFTER_DAYS calls room.clearDnd() when consecutiveDndDays >= maxDays then creates HIGH priority task. |
| 7 | Scheduler can auto-generate stayover tasks and process DND rooms without SecurityContext | VERIFIED | resolvePolicy() auth check removed (comment-only). generateDailyTasks() auth check removed (controller handles it). HkSchedulerService now calls generateDailyTasks() directly. All scheduler-path methods are auth-free. |

**Score:** 7/7 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `V8_10_0__add_stayover_cleaning_policy.sql` | DB migration for schema changes | VERIFIED | 71 lines. Adds 7 columns to hk_config, creates hk_cleaning_policy table with unique constraint + partial index, adds 3 DND columns to hk_task, adds 2 DND columns to htl_room_number. |
| `HkConfig.java` | Entity with 7 new stayover/DND/schedule fields | VERIFIED | 122 lines. All 7 new fields with @Builder.Default matching migration defaults. update() method accepts all 16 parameters (9 existing + 7 new). |
| `HkCleaningPolicy.java` | New entity for room type overrides | VERIFIED | 74 lines. Extends BaseEntity, @SQLRestriction, @UniqueConstraint, roomTypeId as Long (cross-module FK), all override fields nullable, update() method. |
| `HkTask.java` | Entity with 3 new DND tracking fields | VERIFIED | 200 lines. dndSkipped (Boolean, default false), dndSkipCount (Integer, default 0), scheduledTime (String, length 5). All with @Builder.Default. |
| `RoomNumber.java` | Entity with DND tracking fields + setDnd/clearDnd/incrementDndDays | VERIFIED | 118 lines. dndSince (LocalDate), consecutiveDndDays (Integer, default 0). setDnd() sets DND status + initializes dndSince. clearDnd() resets to DIRTY + clears DND fields. incrementDndDays() safely increments. |
| `HkCleaningPolicyService.java` | Service interface | VERIFIED | 25 lines. 4 methods: resolvePolicy, getAllPolicies, createOrUpdate, deletePolicy. |
| `HkCleaningPolicyServiceImpl.java` | Service with policy resolution engine | VERIFIED | 219 lines. @Transactional(readOnly=true) class-level. Generic pick() helper. resolvePolicy merges 8 fields. getAllPolicies uses native query for cross-module room types. createOrUpdate uses upsert pattern. deletePolicy uses softDelete(). |
| `HousekeepingService.java` | Interface with 3 new stayover methods | VERIFIED | 79 lines. transitionOccupiedRoomsToDirty, generateStayoverTasks, processDndRooms declared with correct signatures and "SecurityContext 불필요" documentation. |
| `HousekeepingServiceImpl.java` | Implementation of stayover automation | VERIFIED | ~1077 lines. transitionOccupiedRoomsToDirty (no auth check), generateStayoverTasks (no auth check, uses policy), processDndRooms (no auth check, all 3 DND policies), updateConfig() passes all 16 params, getConfig() fallback includes all 7 new fields, buildRoomStatusSummary includes DND count. |
| `HkSchedulerService.java` | Scheduler running every minute | VERIFIED | 82 lines. @Scheduled(cron = "0 * * * * *"), per-property error isolation with try/catch, respects odTransitionTime and dailyTaskGenTime, checks stayoverEnabled before generating. |
| `RoomStatusService.java` | calcStatusCode handles DND | VERIFIED | Line 30: `if ("DND".equals(hkStatus)) return "DND";` -- properly handles DND before the FO/HK matrix. |
| `HkCleaningPolicyApiController.java` | REST API for policy CRUD | VERIFIED | 54 lines. @RestController at /api/v1/properties/{propertyId}/hk-cleaning-policies. GET (list), POST (createOrUpdate), DELETE /{roomTypeId}. All call accessControlService.validatePropertyAccess(). Returns HolaResponse.success(). |
| `HousekeepingApiController.java` | Manual execution endpoints | VERIFIED | transition-od (POST), generate-stayover-tasks (POST), process-dnd (POST) all present at lines 446-475. All call accessControlService.validatePropertyAccess(). |
| `HkCleaningPolicyRepository.java` | Repository interface | VERIFIED | 19 lines. findByPropertyIdOrderBySortOrder, findByPropertyIdAndRoomTypeId, existsByPropertyIdAndRoomTypeId. |
| `RoomNumberRepository.java` | New query methods | VERIFIED | findOccupiedCleanRooms (JPQL), findOccupiedDirtyRoomsWithRoomTypeId (native), findRoomTypeIdByRoomNumberId (native), findRoomTypesByPropertyId (native cross-module). |
| `HkConfigUpdateRequest.java` | DTO with 7 new fields | VERIFIED | 31 lines. All 16 fields present (9 existing + 7 new: stayoverEnabled, stayoverFrequency, turndownEnabled, dndPolicy, dndMaxSkipDays, dailyTaskGenTime, odTransitionTime). |
| `HkConfigResponse.java` | Response DTO with 7 new fields | VERIFIED | 37 lines. All 16 fields + id + propertyId. |
| `ResolvedCleaningPolicy.java` | Resolved policy DTO | VERIFIED | 23 lines. All 8 policy fields + overridden flag. Uses primitives (boolean, int) for resolved values (never null after resolution). |
| `HkCleaningPolicyResponse.java` | Policy response DTO | VERIFIED | 31 lines. All override fields (nullable) + roomTypeName, roomTypeCode, overridden flag. |
| `HkDashboardResponse.java` | Dashboard with DND count | VERIFIED | RoomStatusSummary inner class has `int dnd` field at line 47. |
| `HkTaskMapper.java` | Mapper with toResponse(HkConfig) | VERIFIED | toResponse(HkConfig) at line 76 maps all 7 new fields: stayoverEnabled, stayoverFrequency, turndownEnabled, dndPolicy, dndMaxSkipDays, dailyTaskGenTime, odTransitionTime. |
| `ErrorCode.java` | HK_CLEANING_POLICY_NOT_FOUND error code | VERIFIED | Line 209: HK_CLEANING_POLICY_NOT_FOUND("HOLA-8090"), HK_CLEANING_POLICY_DUPLICATE("HOLA-8091"). Both in HOLA-8xxx range per convention. |
| `HolaPmsApplication.java` | @EnableScheduling annotation | VERIFIED | Line 8: @EnableScheduling present on main application class. |
| `settings.html` | 3-tab UI with cleaning policy tab | VERIFIED | 339 lines. 3 tabs (일반 설정, 구역 관리, 청소 정책). General tab has stayover/turndown/DND/schedule fields. Policy tab has room type table. Policy modal supports null values (select with "기본값 사용"). Reset button with d-none toggle. Cards use border-0 shadow-sm. |
| `hk-settings-page.js` | JS for policy management | VERIFIED | 445 lines. Uses HolaPms.ajax, HolaPms.modal.show/hide, HolaPms.alert. loadPolicies/renderPolicyTable/openPolicyModal/savePolicy/resetPolicy all present. toNull/toBool converters send null for empty values. Property context pattern correctly followed (init unconditional, reload checks propertyId, hola:contextChange bound). |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| HkSchedulerService | HousekeepingService.transitionOccupiedRoomsToDirty | Direct method call | WIRED | Line 57: `housekeepingService.transitionOccupiedRoomsToDirty(propertyId)` |
| HkSchedulerService | HousekeepingService.generateStayoverTasks | Direct method call | WIRED | Line 68: `housekeepingService.generateStayoverTasks(propertyId, today)` |
| HkSchedulerService | HousekeepingService.processDndRooms | Direct method call | WIRED | Line 79: `housekeepingService.processDndRooms(propertyId, today)` |
| generateStayoverTasks | HkCleaningPolicyService.resolvePolicy | Direct method call | WIRED | Line 680: `cleaningPolicyService.resolvePolicy(propertyId, roomTypeId)` — auth-free after fix |
| processDndRooms | HkCleaningPolicyService.resolvePolicy | Direct method call | WIRED | Line 722: `cleaningPolicyService.resolvePolicy(propertyId, roomTypeId)` — auth-free after fix |
| HousekeepingServiceImpl | HkCleaningPolicyService | Constructor injection | WIRED | Line 46: `private final HkCleaningPolicyService cleaningPolicyService;` |
| updateConfig | HkConfig.update(16 params) | Method call | WIRED | Lines 510-527: All 16 params passed in correct order matching HkConfig.update() signature |
| getConfig fallback | HkConfigResponse.builder | Builder pattern | WIRED | Lines 474-492: All 7 new fields included in fallback builder with correct defaults |
| buildRoomStatusSummary | DND count query | Repository call | WIRED | Line 1063: `roomNumberRepository.countByPropertyIdAndHkStatus(propertyId, "DND")` mapped to .dnd() in builder |
| JS loadPolicies | GET /hk-cleaning-policies | HolaPms.ajax | WIRED | Line 310: URL correctly assembled |
| JS savePolicy | POST /hk-cleaning-policies | HolaPms.ajax | WIRED | Line 411: Sends JSON with null-converted values |
| JS resetPolicy | DELETE /hk-cleaning-policies/{roomTypeId} | HolaPms.ajax | WIRED | Line 430: Correctly calls DELETE with roomTypeId |
| JS saveConfig | PUT /housekeeping/config | HolaPms.ajax | WIRED | Line 119: Sends all 16 fields including 7 new ones |
| JS loadConfig | GET /housekeeping/config | HolaPms.ajax | WIRED | Line 71: Maps all 7 new fields to form elements |
| HkTaskMapper.toResponse(HkConfig) | HkConfigResponse | All 7 new fields | WIRED | Lines 89-95: stayoverEnabled, stayoverFrequency, turndownEnabled, dndPolicy, dndMaxSkipDays, dailyTaskGenTime, odTransitionTime all mapped |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|--------------|--------|--------------------|--------|
| settings.html (General tab) | Config fields | GET /housekeeping/config -> HkConfig DB | Yes, DB query via hkConfigRepository | FLOWING |
| settings.html (Policy tab) | Policy list | GET /hk-cleaning-policies -> rm_room_type + hk_cleaning_policy DB | Yes, native query + JPA query | FLOWING |
| HkSchedulerService | HkConfig list | hkConfigRepository.findAll() | Yes, DB query | FLOWING |
| generateStayoverTasks | OD rooms + roomTypeId | findOccupiedDirtyRoomsWithRoomTypeId() native query | Yes, live DB query | FLOWING |
| processDndRooms | DND rooms | findByPropertyIdAndHkStatusOrderByRoomNumberAsc() | Yes, live DB query | FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Project compiles | `./gradlew compileJava` | BUILD SUCCESSFUL in 477ms, 10 tasks UP-TO-DATE | PASS |
| @EnableScheduling present | grep in HolaPmsApplication.java | @EnableScheduling annotation confirmed at line 8 | PASS |
| No TODO/FIXME in new service code | grep in HkCleaningPolicyServiceImpl, HousekeepingServiceImpl, HkSchedulerService | No matches found | PASS |

### Requirements Coverage

No formal REQUIREMENTS.md was found for this phase. Verification is based on the PLAN.md architecture description and the user-provided checklist.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| ~~HkCleaningPolicyServiceImpl.java~~ | ~~43~~ | ~~accessControlService in resolvePolicy()~~ | ~~BLOCKER~~ | **FIXED**: auth check removed, comment documents that caller (controller) handles auth. |
| ~~HkSchedulerService.java~~ | ~~76-77~~ | ~~generateDailyTasks not called from scheduler~~ | ~~INFO~~ | **FIXED**: scheduler now calls generateDailyTasks(propertyId, today) directly. |
| HkCleaningPolicyServiceImpl.java | 69, 97, 149 | accessControlService in CRUD methods | OK | Only called from API controllers — auth there is correct. |

### Human Verification Required

### 1. Policy Tab UI Visual Rendering

**Test:** Navigate to HK Settings, select a property, click "청소 정책" tab.
**Expected:** Room types listed with "기본값" or "오버라이드" badges. Clicking edit opens modal with null-capable fields (dropdowns showing "기본값 사용").
**Why human:** Cannot verify visual rendering, badge styling, modal behavior programmatically.

### 2. Reset Policy Button Behavior

**Test:** Create an override for a room type, then click "기본값으로 초기화" button in the policy modal.
**Expected:** Override is deleted (soft delete), badge changes from "오버라이드" to "기본값", all override values disappear from the table row.
**Why human:** Requires interactive UI flow with visual confirmation.

### 3. Scheduler Runtime Behavior (After Bug Fix)

**Test:** After fixing the resolvePolicy auth issue, wait for the configured time or manually trigger via POST /transition-od, /generate-stayover-tasks, /process-dnd.
**Expected:** Tasks are created in the database for OD rooms. DND rooms are processed according to policy.
**Why human:** Requires running server with test data and observing database state changes over time.

### 4. DND Force After Days End-to-End

**Test:** Set a room to DND, increment consecutiveDndDays to >= dndMaxSkipDays (e.g., 3), then run processDndRooms.
**Expected:** Room's hkStatus changes from DND to DIRTY, consecutiveDndDays resets to 0, a HIGH priority stayover task is created with note "DND 3일 초과 강제 청소".
**Why human:** Requires specific data setup and state verification across multiple tables.

### Gaps Summary

**All blockers resolved.** The auth checks were removed from `resolvePolicy()` and `generateDailyTasks()` — both methods are now callable from the scheduler without SecurityContext. API-level auth is enforced by the calling controllers.

**All aspects verified successfully:**
- Schema/entity consistency is exact (column names, types, defaults, constraints).
- Policy resolution engine correctly implements the "Default + Override" pattern with the generic pick() helper.
- OC to OD transition logic is correct and auth-free.
- DND handling implements all 3 policies correctly (SKIP, RETRY_AFTERNOON, FORCE_AFTER_DAYS).
- HkConfig.update() 16-parameter signature matches the request DTO and call site.
- getConfig() fallback includes all 7 new fields.
- buildRoomStatusSummary() includes DND count.
- RoomStatusService.calcStatusCode() handles "DND".
- HkTaskMapper.toResponse(HkConfig) maps all 7 new fields.
- Admin UI follows all conventions (3 tabs, property context pattern, HolaPms.ajax/alert/modal, card border-0 shadow-sm, no fw-bold on labels).
- REST API follows RESTful patterns with proper accessControlService calls.
- @EnableScheduling is on HolaPmsApplication.
- Scheduler has per-property error isolation with try/catch.
- No TODO/FIXME/placeholder patterns found.
- Project compiles successfully.

---

_Verified: 2026-03-26T20:00:00Z_
_Verifier: Claude (gsd-verifier)_
