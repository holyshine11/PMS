# Hola PMS - Gap Analysis Report

> **Feature**: hola (Hola PMS 전체)
> **Analysis Date**: 2026-03-15
> **Overall Match Rate**: 92% (PASS)
> **Previous Match Rate**: 93% (2026-02-28)

---

## 1. 카테고리별 점수

| Category | Score | Status |
|----------|:-----:|:------:|
| API Design Match | 95% | ✅ PASS |
| Data Model Match | 93% | ✅ PASS |
| Feature Completeness (Phase 0-1) | 92% | ✅ PASS |
| Architecture Compliance | 97% | ✅ PASS |
| Convention Compliance | 95% | ✅ PASS |
| Security Implementation | 98% | ✅ PASS |
| Booking Engine Completeness | 88% | ⚠️ WARNING |
| Test Coverage | 75% | ⚠️ WARNING |
| **Overall** | **92%** | **✅ PASS** |

---

## 2. 구현 통계 (설계 vs 실제)

| Metric | Design Plan | Actual | Delta |
|--------|:-----------:|:------:|:-----:|
| Entity classes | 40 | **44** | +4 |
| REST Controllers | - | **25** | - |
| View Controllers | - | **20** | - |
| Service Impls | 22 | **25** | +3 |
| Flyway migrations | 42 | **62** | +20 |
| HTML templates | 42 | **50** | +8 |
| JS files | 38 | **41** | +3 |
| Test classes | 0 | **22** (164 tests) | +22 |
| Error codes | ~80 | **~108** | +28 |

---

## 3. 미구현 항목 (설계 YES → 구현 NO) - 7건

| # | Item | Severity | Module | Phase |
|---|------|----------|--------|-------|
| 1 | JWT Refresh Token (`POST /api/v1/auth/refresh`) | MEDIUM | M00 | Phase 1 |
| 2 | M10 Front Desk / Room Rack (전체 모듈) | HIGH | M10 | Phase 1 backlog |
| 3 | Rooms-to-RatePlan 역방향 조회 API | LOW | M08 | Phase 3 |
| 4 | Card BIN 검증 API | LOW | M08 | Phase 3 |
| 5 | System Management UI (그룹/공통코드 CRUD) | MEDIUM | M00 | Phase 2 |
| 6 | Redis Session 클러스터링 | LOW | Infra | Phase 4 |
| 7 | Webhook Events (예약/체크인/체크아웃) | LOW | M07 | Phase 3 |

---

## 4. 추가 구현 항목 (설계 NO → 구현 YES) - 10건

| # | Item | Impact | Notes |
|---|------|--------|-------|
| 1 | Booking Engine (M08) - 18 API endpoints | HIGH | Phase 3 → 조기 구현 |
| 2 | API Key Authentication (BookingSecurityConfig) | HIGH | 산하 호환 보안 체인 |
| 3 | Multi-currency Support (ExchangeRate) | MEDIUM | KRW/USD/JPY/CNY |
| 4 | Booking Audit Log | MEDIUM | 게스트 활동 추적 |
| 5 | Property Image/Terms 엔티티 | MEDIUM | 부킹 콘텐츠용 |
| 6 | 22 Test Classes (164 tests) | HIGH | 단위+통합 테스트 |
| 7 | Payment Transaction Versioning | MEDIUM | Optimistic locking |
| 8 | Booking Reservation Modify | MEDIUM | 게스트 셀프서비스 |
| 9 | Promotion Code Validation | MEDIUM | 실시간 프로모션 검증 |
| 10 | Reservation Lookup by Email | LOW | 게스트 예약 조회 |

---

## 5. 아키텍처 준수 현황

### ✅ 정상 준수
- **Triple Security Filter Chain**: @Order(0) Booking API Key, @Order(1) JWT Admin API, @Order(2) Web Session
- **모듈 의존성 방향**: 설계 명세 100% 준수 (hola-reservation → hola-common, hola-room, hola-rate)
- **패키지 구조**: 5개 모듈 모두 controller/service/repository/dto/entity/mapper 패턴 일관 적용
- **44개 Entity** 전부 BaseEntity 상속 + `@SQLRestriction("deleted_at IS NULL")` soft delete
- **HolaResponse / BookingResponse** 이중 응답 래퍼 정상 분리
- **Mapper 패턴**: 수동 @Component 매퍼 일관 사용
- **DataTable 패턴**: HolaPms.dataTableDefaults 일관 사용
- **API 경로 규칙**: RESTful `/api/v1/{resource}` 100% 준수

### ⚠️ 주의 사항
- Booking Engine이 Phase 3 예정이었으나 조기 구현됨 → 설계서 업데이트 필요
- 일부 모듈(M10 Front Desk)이 Phase 1 backlog에 남아 있음

---

## 6. 최근 수정 사항 반영 확인 (11건 fix)

| # | Fix Item | Status |
|---|----------|--------|
| 1 | OTA 결제 정책 (호텔 측 요금 결제 허용) | ✅ 반영 완료 |
| 2 | BookingApiIntegrationTest 응답 포맷 수정 | ✅ 반영 완료 |
| 3 | EarlyLateCheckService 정렬 순서 | ✅ 반영 완료 |
| 4 | SecurityConfig API 에러 핸들러 추가 | ✅ 반영 완료 |
| 5 | CHECK_IN→CHECKED_OUT 상태 전환 추가 | ✅ 반영 완료 |
| 6 | PropertyApiController null hotelId 처리 | ✅ 반영 완료 |
| 7 | BookingServiceImpl 결제 재계산 | ✅ 반영 완료 |
| 8 | unitPrice vatIncludedPrice 일관성 | ✅ 반영 완료 |
| 9 | grandTotal 음수 하한 적용 | ✅ 반영 완료 |
| 10 | OTA 객실 변경 차단 (addLeg/updateLeg/deleteLeg) | ✅ 반영 완료 |
| 11 | OTA 서비스 추가/삭제 UI 복원 | ✅ 반영 완료 |

---

## 7. 즉시 조치 권장 사항

| Priority | Action | Effort |
|----------|--------|--------|
| HIGH | M10 Front Desk / Room Rack 모듈 설계 착수 | 2-3주 |
| MEDIUM | JWT Refresh Token 엔드포인트 구현 | 0.5일 |
| MEDIUM | System Management UI (공통코드) 구현 | 1주 |
| LOW | RateCodeService, PromotionCodeService 테스트 추가 | 1-2일 |
| LOW | 시스템 개발 계획서 통계 업데이트 | 0.5일 |

---

## 8. 테스트 현황

- **총 테스트**: 164개 (22 클래스)
- **전체 통과**: ✅ BUILD SUCCESSFUL
- **커버리지 영역**:
  - Unit: ReservationService, PaymentService, EarlyLateCheckService, BookingService 등
  - Integration: BookingApiIntegrationTest (18 시나리오)
- **미커버 영역**: RateCodeService, PromotionCodeService, HotelService, PropertyService

---

## 9. 결론

**Match Rate 92%** — Phase 0~1 핵심 모듈(M01, M02, M03, M07) 구현 완료. Phase 3 예정 Booking Engine(M08)까지 조기 구현하여 설계 대비 초과 달성. 11건 결함 수정 후 전체 164개 테스트 통과. M10 Front Desk 모듈과 JWT Refresh Token 구현이 다음 우선순위.
