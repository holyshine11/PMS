# Phase 02: 간편결제(빌키) 기능 - Context

**Gathered:** 2026-03-26
**Status:** Ready for planning

<domain>
## Phase Boundary

부킹엔진 checkout 화면에 KICC 빌키 기반 간편결제 기능 추가:
- 카드 등록 (빌키 발급): 게스트가 KICC 인증창을 통해 카드를 등록하고 빌키를 발급받아 저장
- 등록된 카드 삭제: 빌키를 KICC에서 제거하고 DB에서 soft delete
- 간편결제 실행: 저장된 빌키로 결제 → 예약 생성 → PMS 결제 내역 반영
- 데모용이므로 로그인 없이 게스트 이메일 기준으로 카드 연결

</domain>

<decisions>
## Implementation Decisions

### 게스트 식별 (D-01: 이메일 기반)
- **D-01:** 로그인 없이 게스트 이메일로 카드를 연결한다. checkout 화면에서 이메일 입력 후 간편결제 탭 전환 시 해당 이메일에 등록된 카드를 자동 조회한다.
- 이메일이 빈 상태에서 간편결제를 선택하면 이메일 입력을 먼저 요청하는 안내를 표시한다.

### 빌키 저장 (D-02: DB 테이블)
- **D-02:** `rsv_easy_pay_card` 새 테이블을 생성한다. BaseEntity 상속, soft delete 패턴 적용.
- 주요 컬럼: `email`, `batch_key`(빌키), `card_mask_no`(마스킹 카드번호), `issuer_name`(발급사명), `card_type`(신용/체크), `pg_cno`(PG 거래번호)
- 이메일 당 최대 5개 카드 제한. 5개 초과 시 프론트에서 추가 버튼 비활성화.
- Flyway 마이그레이션: `V4_21_0__create_easy_pay_card_table.sql` (V4 예약 대역)

### 카드 등록 플로우 (D-03: KICC 인증창 팝업)
- **D-03:** 기존 결제 팝업과 동일한 UX 패턴을 따른다.
  1. 프론트: 카드 추가 버튼 클릭 → `POST /api/v1/booking/payment/billkey/register` 호출
  2. 백엔드: `paymentGateway.registerTransaction()` (paymentMethod="BILLING", amount=0) → KICC 인증창 URL 반환
  3. 프론트: PC는 popup, Mobile은 redirect로 KICC 인증창 열기
  4. KICC 인증 완료 → `/api/v1/booking/payment/billkey/return` 콜백
  5. 백엔드: approval API 호출 → 빌키(batchKey) + 카드정보(cardMaskNo, issuerNm) 수신
  6. `EasyPayCard` 엔티티에 저장 → Redis에 결과 저장 (프론트 폴링용)
  7. 프론트: postMessage 또는 폴링으로 결과 수신 → 카드 목록 갱신

### 간편결제 실행 (D-04: 기존 createBookingWithPaymentResult 재활용)
- **D-04:** 간편결제로 예약 시:
  1. 프론트: 선택된 카드 + 예약정보 → `POST /api/v1/booking/properties/{code}/reservations/easy-pay` 호출
  2. 백엔드: `bookingService.validateBookingRequest()` 검증 후
  3. `KiccApiClient.approveBatchPayment()` 빌키 결제 승인
  4. 승인 성공 → `PaymentResult` 생성 → `bookingService.createBookingWithPaymentResult()` 호출
  5. 결과: MasterReservation + SubReservation + ReservationPayment + PaymentTransaction 모두 정상 생성
  6. PMS Admin에서 기존 결제 내역 UI로 확인 가능 (추가 Admin UI 불필요)

### 카드 삭제 (D-05: KICC + DB 동시 삭제)
- **D-05:** 카드 삭제 시 KICC `removeBatchKey` API 호출 후 DB soft delete.
- KICC 삭제 실패 시에도 DB에서는 삭제 처리 (데모 편의상 KICC 실패를 blocking하지 않음)
- API: `DELETE /api/v1/booking/easy-pay-cards/{cardId}?email={email}`

### Claude's Discretion
- 에러 처리 상세 전략 (네트워크 오류, KICC 타임아웃 등)
- 카드 등록 결과 Redis TTL 설정 (기존 결제 패턴 참고하여 결정)
- 프론트엔드 카드 렌더링 세부 디자인 (기존 `.easy-pay-card` CSS 활용)
- MockPaymentGateway에 빌키 관련 메서드 추가 방식

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### KICC PG 연동
- `hola-pms/hola-reservation/src/main/java/com/hola/reservation/booking/pg/kicc/KiccPaymentGateway.java` — 빌키 모드 거래등록 (payMethodTypeCode="81") 및 return URL 라우팅 로직
- `hola-pms/hola-reservation/src/main/java/com/hola/reservation/booking/pg/kicc/KiccApiClient.java` — approveBatchPayment(), removeBatchKey() API 클라이언트
- `hola-pms/hola-reservation/src/main/java/com/hola/reservation/booking/pg/kicc/dto/KiccBatchApprovalRequest.java` — 빌키 결제 요청 DTO
- `hola-pms/hola-reservation/src/main/java/com/hola/reservation/booking/pg/kicc/dto/KiccRemoveKeyRequest.java` — 빌키 삭제 요청 DTO
- `hola-pms/hola-reservation/src/main/java/com/hola/reservation/booking/pg/kicc/dto/KiccApprovalResponse.java` — 빌키 발급/결제 응답 (paymentInfo.cardInfo 내 batchKey, cardMaskNo, issuerNm)
- `hola-pms/hola-reservation/src/main/java/com/hola/reservation/booking/pg/kicc/dto/KiccCardInfo.java` — cardNo(빌키), cardMaskNo, issuerNm 필드 구조

### 기존 결제 플로우 (패턴 참고)
- `hola-pms/hola-reservation/src/main/java/com/hola/reservation/booking/controller/KiccPaymentApiController.java` — register/return/result 3단계 패턴, Redis 임시 데이터 저장 패턴
- `hola-pms/hola-reservation/src/main/java/com/hola/reservation/booking/service/BookingServiceImpl.java` — createBookingWithPaymentResult() 메서드 (빌키 결제 시 재활용)
- `hola-pms/hola-app/src/main/resources/templates/booking/payment-return.html` — KICC 콜백 후 postMessage/redirect 패턴

### 프론트엔드
- `hola-pms/hola-app/src/main/resources/templates/booking/checkout.html` — #easyPayPanel, #btnAddEasyCard, .easy-pay-card 이미 배치됨
- `hola-pms/hola-app/src/main/resources/static/js/booking.js` — CheckoutPage 모듈, initiateKiccPayment() 팝업 패턴
- `hola-pms/hola-app/src/main/resources/static/css/booking.css` — .easy-pay-* CSS 클래스 이미 완성

### 보안
- `hola-pms/hola-reservation/src/main/java/com/hola/reservation/booking/security/BookingSecurityConfig.java` — 부킹 API permitAll, BookingApiKeyFilter
- `hola-pms/hola-common/src/main/java/com/hola/common/security/SecurityConfig.java` — 전체 보안 체인 구성

### 엔티티/DB 참고
- `hola-pms/hola-app/src/main/resources/db/migration/V4_20_0__add_pg_transaction_fields.sql` — 최근 결제 관련 마이그레이션 (테이블 네이밍 참고)
- `hola-pms/hola-reservation/src/main/java/com/hola/reservation/entity/PaymentTransaction.java` — PG 트랜잭션 기록 엔티티 구조

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `KiccPaymentGateway.registerTransaction()`: `paymentMethod="BILLING"` 분기 이미 구현 → 빌키 거래등록에 그대로 사용 가능
- `KiccApiClient.approveBatchPayment()`: 빌키 결제 승인 메서드 완성 → 호출만 하면 됨
- `KiccApiClient.removeBatchKey()`: 빌키 삭제 메서드 완성 → 호출만 하면 됨
- `BookingServiceImpl.createBookingWithPaymentResult()`: 빌키 결제 후 예약+결제 기록 생성에 그대로 재활용
- `KiccPaymentApiController`: register → return → result 3단계 패턴 → 빌키 플로우도 동일 패턴으로 구현
- `checkout.html` + `booking.css`: 간편결제 UI 스켈레톤 완성 (패널, 카드, 추가 버튼)
- `booking.js` CheckoutPage: 결제수단 탭 전환, 카드 선택 이벤트 바인딩 준비됨

### Established Patterns
- **Redis 임시 데이터**: 30분 TTL로 `kicc:booking:{shopOrderNo}` 키에 저장 → 빌키도 동일 패턴 (`kicc:billkey:{shopOrderNo}`)
- **팝업 결제**: PC는 `window.open()` + `postMessage`, Mobile은 redirect + 폴링 → 빌키 등록도 동일
- **엔티티**: BaseEntity 상속, `@SQLRestriction("deleted_at IS NULL")`, soft delete → EasyPayCard 엔티티도 동일
- **API 패턴**: `HolaResponse.success(data)` 응답, `HolaException(ErrorCode.XXX)` 예외

### Integration Points
- `BookingSecurityConfig`: `/api/v1/booking/**` permitAll → 새 빌키 API도 이 경로 하위에 배치하면 별도 보안 설정 불필요
- `KiccPaymentApiController`에 빌키 관련 엔드포인트 추가 또는 새 컨트롤러(`EasyPayApiController`) 분리
- `BookingServiceImpl`에 빌키 결제 메서드 추가 또는 별도 `EasyPayService` 분리
- Flyway `V4_21_0` 버전으로 새 테이블 마이그레이션 추가

</code_context>

<specifics>
## Specific Ideas

- 데모 시연용이므로 에러 처리는 기본적인 수준만 구현 (사용자에게 알림 표시 정도)
- 로그인 기반이 아니므로 이메일이 카드 소유자 식별자 역할
- PMS Admin 화면은 기존 결제 내역 UI 그대로 사용 (간편결제든 일반결제든 PaymentTransaction에 동일하게 기록)
- 카드 최대 5개 제한은 메모리에서 확인된 요구사항

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 02-easy-payment*
*Context gathered: 2026-03-26*
