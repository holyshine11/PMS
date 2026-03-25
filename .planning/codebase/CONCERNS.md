# Codebase Concerns

**Analysis Date:** 2026-02-28

## Tech Debt

### ERP Integration Not Implemented

**Area:** Inventory Management Strategy Pattern

**Issue:** External ERP integration is stubbed with TODOs. Module architecture designed for SAP/ERP integration but all methods return mock data.

**Files:**
- `hola-pms/hola-room/src/main/java/com/hola/room/service/inventory/ExternalInventoryStrategy.java` (lines 21-42)

**Impact:**
- `getAvailableCount()` returns `Integer.MAX_VALUE` (always available)
- `reserve()` logs notification but doesn't validate against external system
- `release()` logs notification only
- `getAvailability()` returns empty list
- Real inventory conflicts may not be detected if ERP mode is selected

**Fix Approach:**
1. Implement actual ERP API client (configurable endpoint via environment variable)
2. Add API key authentication and timeout handling (default 5s with retry)
3. Create test doubles for unit tests (separate from ExternalInventoryStrategy)
4. Add circuit breaker pattern for ERP API failures
5. Log all ERP API calls with correlation IDs for debugging

---

### Frontend jQuery $.html() Direct HTML Injection

**Area:** XSS Vulnerability - Content Injection

**Issue:** Multiple JS files use jQuery `.html()` with potentially unsafe string concatenation. While current code appears to use static strings and data-bound variables, the pattern is fragile.

**Files:**
- `hola-pms/hola-app/src/main/resources/static/js/reservation-detail.js` (19 occurrences: lines 688, 997, 1500, 1529, 1565, 1606, 1672, 1675, 1692, 1741, 1761, 1768, 1784, 1804, 1938, 2340, 2382, 2416, 2430)
- `hola-pms/hola-app/src/main/resources/static/js/hk-attendance-page.js` (line 108)
- Other form/page files: `rate-code-form.js`, `promotion-code-form.js`, `reservation-form.js`, etc.

**Symptoms:**
- String concatenation like `$container.html(html)` where `html` is built from API responses
- Example: `$('#upgradeRoomTypeId').html('<option value="">선택</option>');` (safe currently)
- Risk: If API response contains user input (guest name, memo text), XSS injection possible

**Current Mitigation:**
- API responses are typed DTOs (not raw user input)
- Server-side masking of PII (guest names, phone numbers) via `NameMaskingUtil`
- Bootstrap/DataTables handle most list rendering safely

**Fragile Areas:**
- Memo text display in `reservation-detail.js` (lines 2340+)
- Custom HTML builders in `_roomStatusBadge()` and similar functions
- Any field that could contain guest-supplied text (notes, special requests)

**Fix Approach:**
1. Audit all `.html()` calls to identify data sources
2. For static HTML: keep as-is (e.g., `<option>` tags)
3. For dynamic content: use `.text()` or escaped helpers
4. Create centralized `SafeHtml` utility that validates/escapes before injection
5. Consider migrating to `.append($(...)` with jQuery objects for DOM-safe construction
6. Add CSP (Content-Security-Policy) header to prevent inline script execution

---

## Known Bugs

### Orphaned HK Task/Assignment Data (PARTIALLY FIXED)

**Issue:** HK tasks remain in INHOUSE state after check-out. Migration V8_9_0 attempted fix but data integrity checks missing.

**Files:**
- `hola-pms/hola-app/src/main/resources/db/migration/V8_9_0__fix_orphan_inhouse_data.sql`
- `hola-pms/hola-hotel/src/main/java/com/hola/hotel/service/HkAssignmentServiceImpl.java` (lines 100+)

**Symptoms:**
- HK staff see completed tasks in mobile app for past check-outs
- HK reports show tasks from closed reservations
- Orphan data may interfere with today's task counts

**Current Status:**
- Flyway migration added but no application-level constraint
- No foreign key cascade to HkTask/HkAssignment on SubReservation delete

**Fix Approach:**
1. Add `ON DELETE CASCADE` constraint from `sub_reservation` to `hk_task` and `hk_assignment`
2. Add pre-delete audit logging in `HousekeepingService.markReservationClosed()` to track cleaned records
3. Create scheduled job to detect orphaned tasks (SubReservationId pointing to deleted record)
4. Add test case: create HK task → check out reservation → verify task deleted or marked inactive

---

### JPQL Null Parameter Bytea Casting Error (RECURRING RISK)

**Issue:** Hibernate 6 + PostgreSQL fail when JPQL uses `(:param IS NULL OR column = :param)` pattern with non-trivial types. This is solved in current code but the pattern is easy to reintroduce.

**Files:**
- `hola-pms/hola-reservation/src/main/java/com/hola/reservation/service/ReservationServiceImpl.java` (line 107-116: Java filtering instead of JPQL)
- All repository methods with optional parameters

**Current Solution:**
- Code avoids JPQL null checks: "전체 조회 후 Java 필터링 (Hibernate 6 + PostgreSQL null 파라미터 타입 추론 이슈 회피)"
- Uses `.findByPropertyId()` then streams with Java predicates
- Pattern works but fetches full table for filtering

**Risk:**
- Future developer might write `JPQL: "... WHERE (:status IS NULL OR rsv.status = :status)"` not knowing the issue
- Affects any LIKE, =, >=, <= operators with null-able parameters
- Silent failure: query compiles, fails at runtime with bytea cast error

**Fix Approach:**
1. Add custom JPA Specification factory with null-safe helpers
2. Create `QueryBuilder` utility: `nullSafeEq(root, path, value)` → returns null predicate or equals predicate
3. Document in CLAUDE.md: "JPQL null patterns forbidden - use Specification or Java stream filtering"
4. Add pre-commit hook to detect `IS NULL OR` patterns in Java files
5. Consider Hibernate 6.5+ which may have fixed this (test on upgrade)

---

### Multi-File Entity Updates Not Transactional

**Area:** HkSection with Orphan Collections

**Issue:** `HkSection` has two `@OneToMany(orphanRemoval=true)` collections. When updating through web form, all files must be updated atomically or risk orphaned child records.

**Files:**
- `hola-pms/hola-hotel/src/main/java/com/hola/hotel/entity/HkSection.java` (lines 36-41)
- `hola-pms/hola-hotel/src/main/java/com/hola/hotel/service/HkAssignmentServiceImpl.java` (line 112: comment notes this pattern)

**Symptoms:**
- If form submission partially fails (e.g., some files upload, some fail), orphanRemoval cascade may delete unintended records
- If database transaction rolls back mid-update, collections may be in inconsistent state

**Current Mitigation:**
- Code uses `clear() → flush() → addAll()` pattern to ensure JPA sees collection changes (line 112)
- `@Transactional` at service method level

**Fragile Areas:**
- File upload + HkSection update: If file A uploads OK, file B fails, and transaction rolls back, orphaned file entries may remain
- No explicit transaction boundary around multi-step updates
- Flyway migrations (V8_5_1) added nullable FK but no NOT NULL constraint to detect partial deletes

**Fix Approach:**
1. Separate concerns: HkSection config update vs file upload (two endpoints, two transactions)
2. Add pre-delete cascade logging: log which child records are being deleted
3. Add database constraint: `ALTER TABLE hk_section_file ADD CONSTRAINT NOT NULL` to detect orphan attempts
4. Unit test: create HkSection with 2 files → update 1 file → verify other untouched
5. Document in service: "Orphan removal enabled - keep collection updates together in same @Transactional block"

---

## Security Considerations

### Session Context Pollution - HK Mobile vs Admin PMS

**Risk Level:** CRITICAL (Multiple incidents logged in MEMORY.md)

**Area:** Multi-Role Session Management

**Issue:** When same browser/session is used for both Admin PMS (SUPER_ADMIN) and HK Mobile (HOUSEKEEPER), `SecurityContext` can be overwritten. Session attribute `SPRING_SECURITY_CONTEXT` gets contaminated.

**Files:**
- `hola-pms/hola-common/src/main/java/com/hola/common/security/HkMobileSessionFilter.java` (correct: backup/restore at lines 34-56)
- `hola-pms/hola-common/src/main/java/com/hola/common/security/SecurityConfig.java` (lines 48-71, 170-171: filter ordering)

**Current Mitigations:**
- `HkMobileSessionFilter` uses try-finally to restore original SecurityContext (line 54-56)
- `sessionFixation().none()` disables session fixation protection for mobile (line 54)
- Filter added AFTER SecurityContextHolderFilter (line 171)
- Separate session attributes: `hkUserId` + `hkUserRole` vs admin auth

**Why Still Fragile:**
- Mobile filter runs AFTER JwtAuthenticationFilter (order matters: 0=Booking, 1=HkMobile, 2=JWT, 3=Web)
- If filter is accidentally moved earlier, admin request could be intercepted by mobile logic
- If try-finally is removed or catches exception badly, original context is lost
- No explicit test case for "same session, PMS tab + mobile tab concurrent requests"

**Recent Incidents:**
- early-late-policy page (3회 버그 발생)
- reservation-detail page
- Pattern: Context not restored → admin operation incorrectly authorized as HOUSEKEEPER

**Fix Approach:**
1. Add integration test: Same session → Admin API call → Mobile API call → Admin API call → all succeed with correct roles
2. Add explicit test: Verify SecurityContext is ORIGINAL_ADMIN after mobile filter chain
3. Move HkMobile filter to AFTER all auth filters (currently at `SecurityContextHolderFilter.class`)
4. Add guard: In AdminUserRepository, add query-level check to ensure role != HOUSEKEEPER (defense-in-depth)
5. Log all SecurityContext swaps to audit log (track which requests changed context)
6. Document in SecurityConfig: "DO NOT REMOVE try-finally from HkMobileSessionFilter - verified fix for context pollution"

---

### JWT Secret Not Rotated, No Token Revocation

**Area:** Token Lifecycle Management

**Issue:** JWT secret is read from `${jwt.secret}` environment variable (good) but no mechanism to revoke tokens or rotate keys.

**Files:**
- `hola-pms/hola-common/src/main/java/com/hola/common/security/JwtProvider.java` (line 28: secret loaded at startup)
- `hola-pms/hola-common/src/main/java/com/hola/common/config/JpaAuditingConfig.java` (no revocation list)

**Symptoms:**
- If dev accidentally commits a secret key in git history, all JWT tokens issued with that key remain valid forever
- Logout doesn't invalidate JWT (tokens valid until expiry, typically 1 hour)
- Compromised user account: attacker keeps access with stolen token for 1 hour
- Key rotation requires restart (downtime)

**Current Mitigations:**
- Access token expiry: 1 hour (default)
- Refresh token expiry: 7 days
- Tokens checked in `JwtAuthenticationFilter.validateToken()` (line 64-73)
- No environment variable leakage visible in code (secret injection at runtime)

**Risk Vector:**
- Database breach exposing user passwords (fixed by BCrypt `$2a$10` cost factor)
- Application logs accidentally printing tokens (search indicates none, but possible in future)
- Man-in-the-middle capturing token from HTTPS (HTTPS not enforced in dev profile)

**Fix Approach:**
1. Add token blacklist: `jwt_blacklist` table with `token_hash` + `expiry_date` index
2. On logout: insert token hash to blacklist (set expiry = actual token expiry)
3. In `JwtAuthenticationFilter.validateToken()`: check blacklist before claims validation
4. Implement `/api/v1/auth/logout` endpoint to blacklist current token
5. Add scheduled job: `DELETE FROM jwt_blacklist WHERE expiry_date < NOW()` (daily)
6. Separate keypairs for access vs refresh tokens (allows rotating access key without breaking refresh flow)
7. Document secret rotation procedure: generate new key → deploy with both old+new → validate for 24h → remove old key

---

### BookingApiKeyFilter Single Static Key

**Area:** Booking Engine API Authentication

**Issue:** Booking API (guest checkout) uses single API key stored in database. If compromised, attacker can submit fake bookings.

**Files:**
- `hola-pms/hola-reservation/src/main/java/com/hola/reservation/booking/security/BookingApiKeyFilter.java` (line 33: reads single header)
- `hola-pms/hola-app/src/main/resources/db/migration/V4_13_0__create_booking_api_key.sql`

**Current Mitigations:**
- API key stored in `booking_api_key` table (encrypted in DB)
- Filter checks header `API-KEY` matches database value
- CSRF disabled for `/api/v1/booking/**` (required for guest checkout)

**Risks:**
- Single key: if leaked, attacker creates unlimited bookings
- No key rotation mechanism in code
- No rate limiting visible (vulnerable to brute-force booking attacks)
- No request signing or timestamp validation (replay attacks possible)
- Guest payment confirmation email could be spoofed if booking submission forged

**Impact:**
- Financial: Fake bookings consume inventory, block real guests
- Revenue: Refund fraud (submit booking, receive confirmation, request refund)
- Data: Booking audit log polluted with fake entries

**Fix Approach:**
1. Implement HMAC-SHA256 request signing: client signs request body with API key
2. Add timestamp validation: reject requests older than 5 minutes
3. Add idempotency key: client provides nonce → server tracks to prevent duplicate submissions
4. Rate limiting: 100 bookings/hour per IP (configurable, with whitelist for OTA partners)
5. API key versioning: create new key periodically, deprecate old keys after grace period
6. Webhook signature verification: outbound emails should be signed with same HMAC
7. Add booking validation: verify payment info, guest details, check-in date sanity before persisting
8. Log all bookings: IP, user agent, payment method, timestamp → detect patterns

---

### No PII Data Masking in Logs or API Responses (Partial Fix)

**Area:** Personally Identifiable Information (PII) Exposure

**Issue:** While frontend masks guest names/phones with `HolaPms.maskName()` + `HolaPms.maskPhone()`, backend logs and API responses may contain unmasked data.

**Files:**
- `hola-pms/hola-common/src/main/java/com/hola/common/util/NameMaskingUtil.java` (frontend utility)
- Service logs in `ReservationServiceImpl`, `FrontDeskServiceImpl`, etc. (may log guest details)

**Current Mitigations:**
- Frontend masking: guest name shown as `김*홍`, phone as `010-****-1234`
- GDPR-style requirement noted in MEMORY.md: "개인정보 마스킹 필수"

**Fragile Areas:**
- Backend logs: if `log.info("Guest: " + guest.getName())` appears, unmasked PII in logs
- API responses: `ReservationDetailResponse` contains full guest name (DTO used by web UI, secure by browser, but exposed in network)
- Payment info: credit card last 4 digits should be masked (currently unclear if masked)
- Error messages: stack traces might contain sensitive data
- Audit logs: login attempts log username (OK), but failed payment details might be logged (risky)

**Search Results:**
- `NameMaskingUtil` exists but usage may be incomplete
- No `@Log(sensitive=true)` or similar annotations to auto-mask logs
- No encryption at rest for guest PII in database

**Fix Approach:**
1. Audit all `log.*()` calls for PII (guest name, phone, email, card numbers)
2. Create `SensitiveData` wrapper: `new SensitiveData(guestName)` → logs as `SensitiveData(...)`
3. In logging appender, replace `SensitiveData` tokens with `[REDACTED]`
4. Mask API responses: add DTO field `@JsonSerialize(using=MaskingSerializer.class)` for phone/email
5. Encrypt guest phone/email in database (pgcrypto extension or application-level encryption)
6. Audit trail: log user action (who accessed guest details) with timestamp, not the details themselves
7. Add data retention policy: auto-delete guest PII after check-out + 30 days (GDPR compliance)
8. Test: run application, capture logs, verify no unmasked guest identifiers in output

---

## Performance Bottlenecks

### Large Service Methods - ReservationServiceImpl

**Problem:** Single service class handles too many concerns, making it slow to compile and test.

**Files:**
- `hola-pms/hola-reservation/src/main/java/com/hola/reservation/service/ReservationServiceImpl.java` (1857 lines)

**Details:**
- Handles: create, update, delete, list, detail, status transitions, guest management, service items, deposits
- 80+ method signatures in the class
- Dependencies: 16+ injected repositories + services (hides complexity)
- Test class likely has 100+ test methods

**Impact:**
- Hard to navigate code (IDE search finds 50+ matches for `findBy` patterns)
- Single test compilation can trigger full Reservation module rebuild
- Making a change to one method affects entire class compilation
- Cognitive load: developer has to understand 1857 lines to make small change

**Fix Approach:**
1. Extract `GuestManagementService`: handle guest CRUD (guest table, masking)
2. Extract `ReservationStatusService`: manage state transitions + validations
3. Extract `ReservationItemService`: service item line items + rates
4. Extract `ReservationDepositService`: deposit tracking + history
5. Keep `ReservationServiceImpl` for orchestration + primary CRUD
6. Target: 3-4 files, 400-500 lines each
7. Test: ReservationServiceImplTest → 4 focused test classes, each 30-40 test methods

---

### Potential N+1 Queries - Missing Fetch Joins

**Area:** Collection Lazy Loading

**Issue:** While code contains `// N+1 방지` comments indicating awareness, some queries may still trigger unnecessary SELECT statements.

**Files:**
- `hola-pms/hola-reservation/src/main/java/com/hola/reservation/service/ReservationServiceImpl.java` (1535: Floor/RoomNumber/RoomType ID 수집)
- `hola-pms/hola-reservation/src/main/java/com/hola/reservation/service/FrontDeskServiceImpl.java` (80-105: SubReservation list conversion)
- `hola-pms/hola-hotel/src/main/java/com/hola/hotel/service/HkAssignmentServiceImpl.java` (642: batch lookup)

**Current Mitigation:**
- Code explicitly calls `default_batch_fetch_size: 100` in `application.yml` (line 16)
- Comments show developers understand the issue
- Bulk loading patterns visible (e.g., `floorRepository.findAllById(floorIds)`)

**Residual Risk:**
- `open-in-view: false` enforces no lazy loading in view layer (good)
- But if service calls `.get()` on lazy collection, triggers SELECT per parent row
- Example: `reservation.getSubReservations()` in loop without fetch join → N queries

**Search for Issues:**
- Multiple `forEach(rsv -> ...)` patterns visible in codebase
- If child entity is not loaded, forEach triggers SELECT per iteration
- Batch fetch helps but doesn't eliminate the issue completely

**Fix Approach:**
1. Add query analysis: run tests with SQL logging enabled, check for SELECT count
2. Create `@NamedQuery` or Specification with `LEFT JOIN FETCH` for all multi-row queries
3. Document: "All public service methods returning collections must use FETCH in JPQL"
4. Add QueryDSL or Specification helper: `ReservationQuery.withSubReservations()` method
5. Unit test N+1: `assertEquals(1, sqlExecutionCount)` using P6Spy or similar
6. Performance baseline: 1000 reservations should query < 10 times (currently unknown)

---

### Database Missing Indexes

**Area:** Query Performance at Scale

**Issue:** While some indexes exist (`BookingAuditLog` has 3), other critical paths may lack indexes.

**Files:**
- `hola-pms/hola-app/src/main/resources/db/migration/V4_15_0__add_reservation_lookup_indexes.sql` (shows some indexes added)
- No full index audit visible in migrations

**Likely Missing:**
- `rsv_master_reservation`: `(property_id, check_in_date, reservation_status)` (composite for dashboard queries)
- `rm_room_type`: `(property_id, use_yn)` (list view filtering)
- `hk_task`: `(property_id, created_at, status)` (HK dashboard aggregations)
- `hk_daily_attendance`: `(housekeeper_id, work_date)` (attendance history)

**Impact:**
- Dashboard queries with filtering can become sequential scans on 100k+ rows
- List pages slow down as data grows
- Concurrent requests contend for table locks during sequential scans

**Fix Approach:**
1. Baseline: measure query times for large datasets (1000+ reservations, 10k+ HK tasks)
2. Identify slow queries (EXPLAIN ANALYZE in PostgreSQL)
3. Add indexes for WHERE + ORDER BY clauses used in queries
4. Create Flyway migration: V9_X_0__add_missing_database_indexes.sql
5. Test index impact: before/after query times (expect 10-100x improvement for filtered queries)
6. Document in CLAUDE.md: "When adding new List endpoint with WHERE/ORDER, ensure corresponding index"

---

## Fragile Areas

### Property Context Dependency Pages (3-Time Bug Pattern)

**Risk Level:** HIGH (documented in MEMORY.md as 3회 버그 발생)

**Area:** Frontend Page Initialization

**Issue:** Pages depending on property selection require careful initialization pattern. Early bugs indicate pattern not enforced well.

**Files:**
- `hola-pms/hola-app/src/main/resources/static/js/reservation-detail.js` (lines 55-98: init pattern)
- `hola-pms/hola-app/src/main/resources/templates/admin/reservations/detail.html` (page structure)
- Pattern used in: early-late-policy, reservation-detail, and other property-scoped pages

**Fragile Pattern:**
```javascript
// UNSAFE: Conditional init prevents event binding
if (HolaPms.context.getPropertyId()) {
    init();  // ❌ If property not selected, bindEvents() never runs
}

// SAFE: Always init, check in reload()
init();  // ✅ Always runs
reload();  // Checks property inside
```

**Why Broken:**
- If developer puts context check OUTSIDE `init()`, the `hola:contextChange` event listener is never registered
- User switches property context later → page doesn't reload (listener missing)
- Page shows stale data from previous property selection

**Files at Risk:**
- Any page with `var init = function() { ... bindEvents(); ... }` called conditionally
- MUST be called unconditionally, with safety check INSIDE `reload()`

**Current Mitigation:**
- CLAUDE.md documents correct pattern (lines in UI rules section)
- Test case likely exists for this specific issue

**Fix Approach:**
1. Create reusable `PropertyContextPage` base object with template:
   ```javascript
   var page = PropertyContextPage({
       propertyRequired: true,
       bindings: { /* event definitions */ },
       reload: function() { /* fetch data */ }
   });
   ```
2. Enforce in code review: "All property-dependent pages must use PropertyContextPage template"
3. Lint rule: detect `init()` calls with `if` guards (warn/error)
4. Integration test: create page → don't select property → select property → verify page updates
5. Documentation: Add example page template to CLAUDE.md with annotations

---

### RateCode Pricing Complexity - Coupled Entities

**Area:** Rate Pricing with Multiple Strategies

**Issue:** `RateCode` → `RatePricing` → `RatePricingPerson` has complex cascading updates. Three-level entity hierarchy with orphanRemoval=true.

**Files:**
- `hola-pms/hola-rate/src/main/java/com/hola/rate/entity/RatePricing.java` (line 110: orphanRemoval=true)
- `hola-pms/hola-rate/src/main/java/com/hola/rate/service/RateCodeServiceImpl.java` (lines 226-379: deletion logic)

**Fragile Patterns:**
- Line 315: `ratePricingPersonRepository.deleteAllByRatePricingId(p.getId())` (explicit delete before cascade)
- Line 317: `ratePricingRepository.deleteAllByRateCodeId(rateCodeId)` (second delete)
- Line 379: Same pattern for single pricing update
- Orphan removal PLUS explicit deletes = redundant but defensive

**Why Fragile:**
- Two deletion mechanisms: orphanRemoval cascade + explicit @Query deletes
- If ordering changes, orphaned records possible
- Tests must verify: delete RateCode → all 3 levels deleted (not just parent)
- Future developer might think orphanRemoval handles it and skip explicit deletes

**Performance:**
- Explicit deletes are `@Modifying` (good), but executed separately from cascade
- 3 SQL DELETE statements per rate code deletion
- If rate code has 100+ pricing entries, loop calls `deleteAllByRatePricingId()` 100+ times

**Fix Approach:**
1. Document: "RateCode deletion: RatePricing + RatePricingPerson must be deleted in order due to orphanRemoval constraints"
2. Consolidate deletion: create `RateCodeDeletionService.delete(rateCodeId)` with explicit transaction + order control
3. Test matrix: create RateCode with 5 pricing entries × 3 persons each → delete → verify all deleted
4. Replace loop with bulk delete: `DELETE FROM RatePricingPerson WHERE rate_pricing_id IN (SELECT id FROM rate_pricing WHERE rate_code_id = :rateCodeId)`
5. Add index: `(rate_code_id, id)` on `rate_pricing` for deletion performance

---

### Dayuse Time Slot Calculation Not Fully Integrated

**Area:** New Feature Incomplete Integration

**Issue:** Dayuse (대실) feature added recently but integration with existing checkout flow incomplete. Project MEMORY indicates feature implemented but auto-checkout deferred.

**Files:**
- `hola-pms/hola-pms/hola-reservation/src/main/java/com/hola/reservation/vo/DayUseTimeSlot.java` (VO defined)
- `hola-pms/hola-app/src/main/resources/db/migration/V4_19_0__add_dayuse_support.sql` (schema)
- `hola-pms/hola-rate/src/main/java/com/hola/rate/entity/DayUseRate.java` (entity)

**Known Gaps (from project memory):**
- Auto check-out for dayuse deferred (blocked on Folio/EOD completion)
- Dayuse package pricing not unified with regular room pricing
- Time slot validation may conflict with night audit process

**Risk:**
- Dayuse checkout logic scattered (not in single, testable flow)
- If night audit runs at 3 AM, dayuse check-out at 6 PM previous day may not be detected
- Pricing discrepancy: dayuse rate may not be reconciled with inventory system
- Reports: dayuse revenue may not be separated from room revenue

**Fix Approach:**
1. Create `DayUseCheckoutService`: handles dayuse housekeeping, settlement, HK task cleanup
2. Create `DayUseInventoryValidator`: ensures dayuse package inventory deducted correctly
3. Add feature flag: `hola.dayuse.auto-checkout.enabled=false` (default off until Folio ready)
4. Test: create dayuse booking → advance time → verify auto-checkout triggers → verify HK task created → verify revenue correct
5. Document in CLAUDE.md: "Dayuse checkout flow: manual only until EOD implemented. Update when ready."

---

## Scaling Limits

### Database Schema-per-Tenant Limits

**Area:** Multi-Tenancy at Scale

**Issue:** Current architecture uses PostgreSQL schema-per-tenant (each hotel chain gets separate schema). This works for 10-100 tenants but has limits.

**Current Implementation:**
- `TenantContext` (ThreadLocal) holds tenant ID → schema name
- `TenantIdentifierResolver` (Hibernate) maps tenant to schema
- Flyway migrations run per-schema with `out-of-order: true`

**Scaling Bottlenecks:**
- PostgreSQL connection pool: each schema needs separate connection → 50 schemas = 50 connections per app instance
- Backup/restore: backing up 100 schemas takes 100x longer than single database
- Schema upgrade (Flyway): must run same migrations across 100 schemas sequentially (slow)
- Monitoring: no per-schema resource limits (one noisy tenant can consume all connections)

**When It Breaks:**
- Beyond 1000 active tenants with 10+ concurrent users each = 10,000+ connections needed
- Backup window (4h → 400h for 100 schemas)
- Rolling deployments: upgrading 100 schemas serially = hours of downtime

**Future Solution:** Database-per-tenant or row-level security (multi-tenant shared schema)

**Fix Approach:**
1. Baseline: measure current connection pool usage per tenant (expect 2-5 connections per active tenant)
2. Add metrics: per-schema query count, lock wait time, connection count
3. Upgrade path (future): create `shared_schema` mode alongside current schema-per-tenant
4. Document in CLAUDE.md: "Schema-per-tenant recommended for < 500 active tenants. Beyond that, evaluate database-per-tenant or RLS"

---

### Frontend Bundle Size

**Area:** JavaScript Performance

**Issue:** Largest JS files approach 2500+ lines, increasing parsing + execution time.

**Files (by size):**
- `reservation-detail.js`: 2477 lines
- `rate-code-form.js`: 1496 lines
- `reservation-form.js`: 1347 lines
- `booking.js`: 1183 lines
- `property-form.js`: 1077 lines

**Impact:**
- Initial page load: browser must parse + execute 2500 lines of JS
- No minification visible (production should minimize)
- Single JS file per page = no code reuse across pages
- Network: ~50KB gzip per file (typical)

**Symptom:**
- First contentful paint (FCP) delayed on slow connections (3G mobile)
- HK mobile app on 4G: expect 1-2 second delay before interactive

**Fix Approach:**
1. Modularize: extract common patterns into shared modules
   - `ReservationStateManager`: handle reservation status transitions (reusable in detail + list views)
   - `DataTableManager`: wrap DataTable initialization (reusable in all list pages)
   - `FormValidator`: common form validation logic
2. Use module bundler: Webpack/Rollup to split code + lazy load
3. Lazy load modal scripts: only parse modal JS when modal opened
4. Defer non-critical scripts: analytics, tracking scripts load async
5. Test: measure FCP before/after changes (target < 2s on 4G connection)

---

## Dependencies at Risk

### Bootstrap 5.3 + jQuery 3.7 - End of Life Concern

**Area:** Frontend Framework Lifecycle

**Issue:** Bootstrap 5.3 released Nov 2023, jQuery 3.7 Nov 2023. Both are current but have deprecation timelines.

**Current Stack:**
- Bootstrap 5.3 (no Bootstrap 6 announced yet)
- jQuery 3.7 (jQuery is in "maintenance mode", not adding features)
- DataTables 1.13 (current)

**Risk:**
- jQuery: no new versions planned, minimal security updates only
- Bootstrap: may release 6.0 in 2-3 years, breaking changes expected
- Transition effort: 20+ pages use jQuery + Bootstrap patterns

**Migration Path:**
- Option A: Move to React/Vue (requires rewrite of all pages)
- Option B: Replace jQuery with vanilla JS gradually (doable page-by-page)
- Option C: Upgrade Bootstrap to 6.0 when released, keep jQuery in maintenance mode

**Fix Approach:**
1. Document: "jQuery will be in maintenance mode. Plan gradual migration to vanilla JS or React in 2027+"
2. Reduce jQuery usage incrementally:
   - Replace `.ajax()` calls with `fetch()` API (vanilla JS, modern)
   - Replace `.html()` with vanilla DOM methods (prevents XSS issues)
   - Replace event delegation with vanilla event listeners
3. Create vanilla JS version of `HolaPms` namespace utilities
4. Test: ensure replacement utilities work identically (behavioral parity tests)

---

### Spring Boot 3.2.5 - Next Major Version Planning

**Area:** Framework Lifecycle Management

**Issue:** Spring Boot 3.2.5 released early 2024, Spring Boot 3.3+ expected mid-2024. Long-term support (LTS) planning needed.

**Current Dependencies:**
- Spring Boot 3.2.5 (current)
- Spring Framework 6.1.x (bundled)
- Spring Security 6.2.x (bundled)
- Spring Data JPA with Hibernate 6.4.x

**Deprecations Visible:**
- `EntityManager` direct usage (not recommended in modern Spring Data)
- `@WebMvcTest` patterns (Spring recommends `@MockMvcTest` in newer versions)
- Some `Security` XML config patterns still visible

**Upgrade Challenges:**
- Spring Boot 3.x dropped Java 8 support (minimum Java 17, current is 17 ✓)
- Spring Security 6.x changed filter chain API (SecurityConfig already uses new API ✓)
- Hibernate 6.x requires JPA 3.0 (already in use ✓)

**Risk:**
- Spring Boot 3.2 security updates end ~2025
- After 3.5 LTS release, pressure to upgrade (breaking changes likely)
- Deferring upgrades = accumulating security debt

**Fix Approach:**
1. Plan upgrade to Spring Boot 3.3 (or next LTS when released) for mid-2026
2. Audit deprecated APIs: `EntityManager` direct queries → use Spring Data methods
3. Test matrix: run tests on Java 17 + 21 (Java 21 LTS released Sept 2023)
4. Upgrade pilot: test on non-critical modules first (e.g., hola-rate)
5. Document: "Spring Boot upgrade plan: 3.2 → 3.x (Q2 2026 target)"

---

## Test Coverage Gaps

### HK Mobile Session Context Pollution - No Integration Test

**Untested Area:** Session context corruption scenario

**Files:**
- `hola-pms/hola-common/src/main/java/com/hola/common/security/HkMobileSessionFilter.java`
- No test visible for "Admin logged in + Mobile session switch + Admin API call" scenario

**Risk:**
- Fix (try-finally at line 54) implemented but not validated by test
- Regression risk high if filter order changes or exception handling added
- Only discovered through production incidents (3x noted in MEMORY.md)

**Missing Test:**
```java
@Test
public void adminApiCallWithMobileSessionShouldPreserveAdminContext() {
    // Login as SUPER_ADMIN
    loginAsAdmin();

    // Open mobile session in same browser
    mockMvc.perform(post("/api/v1/properties/1/hk-mobile/login")
        .session(session)
        .param("userId", "housekeeperId")
    );

    // Admin API should still work as SUPER_ADMIN
    mockMvc.perform(get("/api/v1/properties/1/room-classes")
        .session(session)
    ).andExpect(status().isOk());

    // Verify response is SUPER_ADMIN accessible (not HOUSEKEEPER restricted)
}
```

**Fix Approach:**
1. Add `HkMobileSessionFilterIntegrationTest` class
2. Test matrix:
   - Admin login → check admin role in SecurityContext
   - Admin + mobile switch → check mobile role in temporary context
   - Mobile request complete → check admin role restored
   - Concurrent requests from 2 sessions → each sees correct role
3. Run with `spring.test.database.replace=any` (TestContainers)
4. Verify: SecurityContext.getAuthentication().getPrincipal() returns correct user

---

### Orphan Data Cleanup Not Tested

**Untested Area:** Data integrity after deletions

**Files:**
- `hola-pms/hola-app/src/main/resources/db/migration/V8_9_0__fix_orphan_inhouse_data.sql` (migration applied)
- No test verifying orphan cleanup works

**Missing Test:**
```java
@Test
public void checkOutReservationShouldCleanupHkTasks() {
    // Create reservation + HK task
    SubReservation sub = createReservation(TODAY, TOMORROW);
    HkTask task = createHkTask(sub);

    // Check out
    checkOut(sub);

    // Task should be deleted or marked completed
    assertThat(hkTaskRepository.existsById(task.getId())).isFalse();
}
```

**Fix Approach:**
1. Add `HousekeepingCleanupTest` with orphan cleanup scenarios
2. Test checkout flow → verify child entity cleanup (HkTask, HkAssignment)
3. Test reservation deletion → verify all related data removed
4. Test cascading updates → partial failures don't leave orphaned records
5. Add audit logging test: verify cleanup logged for later validation

---

### Booking API Security Not Fully Tested

**Untested Area:** Booking engine API key validation, replay attacks, rate limiting

**Files:**
- `hola-pms/hola-reservation/src/main/java/com/hola/reservation/booking/security/BookingApiKeyFilter.java`
- No test for invalid/missing key, replay attack, or rate limit scenarios

**Missing Tests:**
```java
@Test
public void bookingWithoutApiKeyShouldFail() {
    mockMvc.perform(post("/api/v1/booking/")
        .header("API-KEY", "")
    ).andExpect(status().isUnauthorized());
}

@Test
public void bookingWithWrongKeyShouldFail() {
    mockMvc.perform(post("/api/v1/booking/")
        .header("API-KEY", "wrong-key")
    ).andExpect(status().isUnauthorized());
}

@Test
public void replayedBookingWithSameIdempotencyKeyShouldReturnSame() {
    String idempotencyKey = UUID.randomUUID().toString();

    // First request
    var response1 = mockMvc.perform(post("/api/v1/booking/")
        .header("API-KEY", validKey)
        .header("Idempotency-Key", idempotencyKey)
        .content(bookingJson)
    ).andReturn().getResponse();

    // Replay same request
    var response2 = mockMvc.perform(post("/api/v1/booking/")
        .header("API-KEY", validKey)
        .header("Idempotency-Key", idempotencyKey)
        .content(bookingJson)
    ).andReturn().getResponse();

    // Both should return same confirmation number (no double booking)
    assertThat(response1.getContentAsString()).isEqualTo(response2.getContentAsString());
}
```

**Fix Approach:**
1. Add `BookingApiSecurityTest` class
2. Test API key validation (invalid, missing, expired)
3. Test idempotency (replay attack prevention)
4. Test rate limiting (100+ bookings in 1 minute should fail)
5. Test request signing (HMAC validation if implemented)
6. Test timestamp validation (old requests rejected)

---

*Concerns audit: 2026-02-28*
