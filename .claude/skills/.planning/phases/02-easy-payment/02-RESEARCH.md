# Phase 02: 간편결제(빌키) — Research

**Researched:** 2026-03-30
**Domain:** KICC 이지페이 빌키(Billing Key) PG 연동, Spring Boot 결제 서비스
**Confidence:** HIGH — 기존 코드 직접 분석, KICC API 문서 학습 정리 파일 참조

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**D-01: 게스트 식별 — 이메일 기반**
로그인 없이 게스트 이메일로 카드를 연결한다. checkout 화면에서 이메일 입력 후 간편결제 탭 전환 시 해당 이메일에 등록된 카드를 자동 조회한다. 이메일이 빈 상태에서 간편결제를 선택하면 이메일 입력을 먼저 요청하는 안내를 표시한다.

**D-02: 빌키 저장 — DB 테이블**
`rsv_easy_pay_card` 새 테이블을 생성한다. BaseEntity 상속, soft delete 패턴 적용. 주요 컬럼: `email`, `batch_key`(빌키), `card_mask_no`(마스킹 카드번호), `issuer_name`(발급사명), `card_type`(신용/체크), `pg_cno`(PG 거래번호). 이메일 당 최대 5개 카드 제한. Flyway 마이그레이션: `V4_21_0__create_easy_pay_card_table.sql`

**D-03: 카드 등록 플로우 — KICC 인증창 팝업**
기존 결제 팝업과 동일한 UX 패턴:
1. 프론트: 카드 추가 버튼 → `POST /api/v1/booking/easy-pay/register`
2. 백엔드: registerTransaction() (paymentMethod="BILLING", amount=0) → KICC 인증창 URL 반환
3. 프론트: PC는 popup, Mobile은 redirect
4. KICC 인증 완료 → `/api/v1/booking/easy-pay/billkey-return` 콜백
5. 백엔드: approval API 호출 → 빌키(batchKey) + 카드정보 수신
6. EasyPayCard 엔티티에 저장 → Redis에 결과 저장 (프론트 폴링용)
7. 프론트: postMessage로 결과 수신 → 카드 목록 갱신

**D-04: 간편결제 실행 — createBookingWithPaymentResult 재활용**
1. 프론트: 선택된 카드 + 예약정보 → `POST /api/v1/booking/easy-pay/pay?propertyCode=...&cardId=...`
2. 백엔드: validateBookingRequest() 검증 후
3. KiccApiClient.approveBatchPayment() 빌키 결제 승인
4. PaymentResult 생성 → bookingService.createBookingWithPaymentResult() 호출
5. MasterReservation + SubReservation + ReservationPayment + PaymentTransaction 정상 생성
6. PMS Admin에서 기존 결제 내역 UI로 확인 가능

**D-05: 카드 삭제 — KICC + DB 동시**
카드 삭제 시 KICC removeBatchKey API 호출 후 DB soft delete. KICC 삭제 실패 시에도 DB에서는 삭제 처리(데모 편의). API: `DELETE /api/v1/booking/easy-pay/cards/{cardId}?email={email}`

### Claude's Discretion
- 에러 처리 상세 전략 (네트워크 오류, KICC 타임아웃 등)
- 카드 등록 결과 Redis TTL 설정 (기존 결제 패턴 참고)
- 프론트엔드 카드 렌더링 세부 디자인 (기존 `.easy-pay-card` CSS 활용)
- MockPaymentGateway에 빌키 관련 메서드 추가 방식

### Deferred Ideas (OUT OF SCOPE)
None — discussion stayed within phase scope
</user_constraints>

---

## Summary

Phase 02 구현 상태를 직접 코드 분석으로 확인한 결과, **백엔드 핵심 구현이 이미 완료되어 있다.** `EasyPayApiController`, `EasyPayCardService/Impl`, `EasyPayCard` 엔티티, `EasyPayCardRepository`, `EasyPayCardResponse`, `billkey-return.html`, `V4_21_0__create_easy_pay_card_table.sql` 마이그레이션까지 모두 커밋된 상태다. 프론트엔드(`booking.js`)도 `loadEasyPayCards`, `renderEasyPayCards`, `registerBillkey`, `deleteEasyPayCard`, `submitEasyPayBooking` 함수가 완성되어 있다.

즉, 이 Phase는 **신규 개발이 아닌 완성된 코드의 통합 검증 및 부족한 부분 보완** 작업이다. 남은 주요 작업은 (1) MockPaymentGateway에 빌키 관련 메서드 추가, (2) 모바일 리다이렉트 플로우 폴링 처리 보완, (3) 빌드/컴파일 통과 확인, (4) end-to-end 시나리오 수동 검증이다.

**Primary recommendation:** 코드가 이미 구현되어 있으므로 플래너는 "신규 작성"이 아닌 "검증·연결·보완" 태스크로 계획을 구성해야 한다.

---

## 현황 분석 (CRITICAL — 구현 완료 여부)

### 이미 구현 완료된 파일들

| 파일 | 경로 | 상태 |
|------|------|------|
| `EasyPayApiController` | `hola-reservation/.../booking/controller/` | 완성 (전체 엔드포인트 구현) |
| `EasyPayCardService` (인터페이스) | `hola-reservation/.../booking/service/` | 완성 |
| `EasyPayCardServiceImpl` | `hola-reservation/.../booking/service/` | 완성 |
| `EasyPayCard` (엔티티) | `hola-reservation/.../booking/entity/` | 완성 |
| `EasyPayCardRepository` | `hola-reservation/.../booking/repository/` | 완성 |
| `EasyPayCardResponse` (DTO) | `hola-reservation/.../booking/dto/response/` | 완성 |
| `billkey-return.html` | `hola-app/.../templates/booking/` | 완성 |
| `V4_21_0__create_easy_pay_card_table.sql` | `hola-app/.../db/migration/` | 완성 |
| ErrorCode (HOLA-43xx 간편결제 에러) | `hola-common/.../exception/ErrorCode.java` | 완성 |
| `booking.js` CheckoutPage 간편결제 함수들 | `hola-app/.../static/js/` | 완성 |
| `.easy-pay-*` CSS 클래스 | `hola-app/.../static/css/booking.css` | 완성 |

### 미완성 또는 보완 필요 항목

| 항목 | 세부 내용 | 우선순위 |
|------|-----------|---------|
| `MockPaymentGateway` 빌키 메서드 | `registerTransaction`, `approveAfterAuth` default 메서드 없어서 테스트 프로파일에서 EasyPayApiController 호출 시 `UnsupportedOperationException` 발생 가능 | HIGH |
| 모바일 폴링 플로우 | 모바일 리다이렉트 후 빌키 등록 결과를 `/easy-pay/billkey-result`로 폴링해야 하는데, `booking.js`에 `window.history.back()` 처리는 있지만 폴링 로직 없음 | MEDIUM |
| EasyPayApiController 빌드 확인 | `@Profile("!test")` 없는 `KiccApiClient` 주입이 test 프로파일에서 문제 발생 가능 | HIGH |

---

## Standard Stack

### 기존 활용 스택 (추가 의존성 불필요)

| 라이브러리 | 버전 | 역할 |
|-----------|------|------|
| Spring Boot | 3.2.5 | 프레임워크 |
| Spring Data JPA | 3.2.5 | DB 접근 |
| Flyway | (Boot 포함) | DB 마이그레이션 |
| Spring Data Redis | 3.2.5 | 임시 데이터 TTL 저장 |
| RestTemplate (kiccRestTemplate) | (Boot 포함) | KICC API HTTP 호출 |
| Thymeleaf | 3.1.x | billkey-return.html |
| Bootstrap 5.3 / jQuery 3.7 | (기존) | 프론트엔드 |

**설치 불필요** — 모든 의존성이 기존 프로젝트에 포함되어 있다.

---

## Architecture Patterns

### 빌키 등록 플로우 (3단계)

```
프론트 (checkout.html)
  ↓ POST /api/v1/booking/easy-pay/register (email, customerName, customerPhone)
EasyPayApiController.registerBillkey()
  ↓ paymentGateway.registerTransaction(paymentMethod="BILLING", amount=0)
KiccPaymentGateway.registerTransaction()  ← payMethodTypeCode="81", returnUrl="/api/v1/booking/easy-pay/billkey-return"
  ↓ KICC /api/ep9/trades/webpay
  ↓ authPageUrl 반환
프론트: PC → window.open(authPageUrl) / Mobile → redirect
  ↓ KICC 인증창에서 카드입력
  ↓ POST /api/v1/booking/easy-pay/billkey-return (resCd, shopOrderNo, authorizationId)
EasyPayApiController.billkeyReturn()
  ↓ KiccApiClient.approvePayment() ← 동일 엔드포인트 /api/ep9/trades/approval
  ↓ paymentInfo.cardInfo.cardNo = batchKey (빌키)
  ↓ EasyPayCardService.registerCard() → DB 저장
  ↓ Redis 결과 저장 (kicc:billkey-result:{shopOrderNo}, 10분 TTL)
  ↓ billkey-return.html → window.opener.postMessage(BILLKEY_REGISTER_COMPLETE)
프론트: postMessage 수신 → loadEasyPayCards() → 카드 목록 갱신
```

### 빌키 결제 플로우 (서버-to-서버)

```
프론트 (checkout.html)
  ↓ POST /api/v1/booking/easy-pay/pay?propertyCode=...&cardId=...
EasyPayApiController.payWithBillkey()
  ↓ bookingService.validateBookingRequest()
  ↓ EasyPayCardRepository.findById(cardId) ← batchKey 획득
  ↓ KiccApiClient.approveBatchPayment() ← /api/trades/approval/batch
  ↓ PaymentResult 수동 빌드 (cardEntity에서 issuerName, cardMaskNo 채움)
  ↓ bookingService.createBookingWithPaymentResult() ← 기존 메서드 재사용
  ↓ {success, confirmationNo, confirmation} 반환
프론트: confirmation 저장 → /booking/{code}/confirmation/{no} 이동
```

### Redis 임시 데이터 패턴

| 키 | TTL | 저장 시점 | 용도 |
|----|-----|-----------|------|
| `kicc:billkey:{shopOrderNo}` | 30분 | register 호출 시 | 이메일 임시 저장 (콜백에서 사용) |
| `kicc:billkey-result:{shopOrderNo}` | 10분 | billkey-return 처리 완료 후 | 프론트 폴링용 |

이 패턴은 기존 `kicc:booking:` / `kicc:result:` 패턴과 동일하다 (KiccPaymentApiController 참조).

### API 엔드포인트 구조

```
/api/v1/booking/easy-pay/
├── GET  /cards?email={email}              → 카드 목록 조회
├── GET  /cards/can-register?email={email} → 등록 가능 여부
├── POST /register                          → 빌키 등록 Step 1 (KICC URL 반환)
├── POST /billkey-return                    → 빌키 등록 Step 2 (KICC 콜백)
├── GET  /billkey-result?shopOrderNo={no}   → 결과 폴링
├── DELETE /cards/{cardId}?email={email}   → 카드 삭제
└── POST /pay?propertyCode=...&cardId=...  → 빌키 결제 + 예약 생성
```

보안: `BookingSecurityConfig` (`@Order(0)`) → `/api/v1/booking/**` → `permitAll`, STATELESS. 별도 보안 설정 불필요.

---

## Don't Hand-Roll

| 문제 | 직접 구현 금지 | 사용할 것 | 이유 |
|------|--------------|-----------|------|
| 빌키 거래등록 | 직접 KICC HTTP 요청 | `paymentGateway.registerTransaction(paymentMethod="BILLING")` | `KiccPaymentGateway`에 이미 "81" 분기 구현 |
| 빌키 결제 승인 | 직접 HTTP POST | `KiccApiClient.approveBatchPayment()` | 완성된 클라이언트 메서드 |
| 빌키 삭제 | 직접 HTTP POST | `KiccApiClient.removeBatchKey()` | 완성된 클라이언트 메서드 |
| 예약 생성 | 별도 예약 생성 로직 | `bookingService.createBookingWithPaymentResult()` | 멱등성 체크, 재고 감소, 결제 기록까지 처리 |
| soft delete | `DELETE` SQL | `card.softDelete()` (BaseEntity) | `@SQLRestriction` 자동 필터링 |
| 에러 응답 | 직접 JSON | `HolaException(ErrorCode.XXX)` | 전역 핸들러가 표준 응답 형식 보장 |

---

## Common Pitfalls

### Pitfall 1: EasyPayApiController의 test 프로파일 의존성 문제

**What goes wrong:** `EasyPayApiController`가 `KiccApiClient`를 직접 주입한다. `KiccApiClient`는 `@Profile("!test")`이므로 테스트 환경에서 Bean이 없어 `NoSuchBeanDefinitionException` 발생 가능.

**Why it happens:** 빌키 발급 후 batchKey/카드정보 추출을 위해 `paymentGateway.approveAfterAuth()` 대신 `KiccApiClient`를 직접 호출하도록 구현됨.

**How to avoid:** `MockPaymentGateway`에 `registerTransaction`/`approveAfterAuth` 메서드를 추가하거나, `EasyPayApiController`에 `@Profile("!test")` 추가. 또는 `EasyPayApiController`의 `KiccApiClient` 직접 의존성을 제거하고 `paymentGateway.approveAfterAuth()`를 통해 처리.

**Warning signs:** `./gradlew test` 실행 시 context load 실패.

### Pitfall 2: 빌키 발급 응답의 cardInfo 구조

**What goes wrong:** KICC 빌키 발급 응답에서 `paymentInfo.cardInfo.cardNo`가 batchKey인데, `issuerNm`/`issuerName` 필드명이 테스트/운영 환경마다 다를 수 있다.

**Why it happens:** `KiccCardInfo`가 `@JsonAlias({"issuerName", "issuerNm"})` 두 필드를 모두 지원하지만, 실제 응답은 KICC API 버전에 따라 달라짐.

**How to avoid:** `EasyPayApiController.billkeyReturn()`의 issuerName fallback 체인이 이미 구현됨: `issuerName → resolveIssuerName(issuerCode) → acquirerName → resolveIssuerName(acquirerCode)`. 로그에서 cardInfo 전체를 확인할 것 (디버그 로그 이미 있음).

### Pitfall 3: 모바일 리다이렉트 후 카드 목록 미갱신

**What goes wrong:** 모바일에서 빌키 등록 시 checkout → KICC 인증창 → billkey-return → `window.history.back()`으로 복귀하지만, 카드 목록 자동 갱신이 안 될 수 있다.

**Why it happens:** `window.history.back()` 후 페이지가 bfcache에서 복원되면 JavaScript가 재실행되지 않아 `BILLKEY_REGISTER_COMPLETE` postMessage도 수신 안 됨.

**How to avoid:** `booking.js`의 `registerBillkey()`에서 모바일 경로는 `sessionStorage.setItem('hola_billkey_shopOrderNo', res.shopOrderNo)`를 저장하고, checkout 페이지 `init()` 또는 `pageshow` 이벤트에서 이 값을 체크해 `loadEasyPayCards()` 재호출.

### Pitfall 4: 이메일 미입력 시 간편결제 탭 클릭

**What goes wrong:** 이메일 필드가 비어있는 상태에서 간편결제 탭 클릭 시 `loadEasyPayCards()`가 빈 배열을 렌더링하고 카드 추가 버튼만 표시된다. 구현상 가이드 메시지 없음.

**Why it happens:** `booking.js` CheckoutPage의 `loadEasyPayCards()` 함수에서 이메일이 없으면 `renderEasyPayCards([])` 호출만 하고 별도 안내 없음.

**How to avoid:** D-01 결정에 따라 이메일 없을 때 `#easyPayCards`에 "이메일을 먼저 입력해주세요" 안내 텍스트를 표시하도록 `renderEasyPayCards()` 수정 필요.

### Pitfall 5: KICC HMAC 검증 — 빌키 발급 응답

**What goes wrong:** 빌키 발급은 `amount=0`이므로 HMAC 검증을 일반 결제와 같이 적용하면 검증 실패.

**Why it happens:** `KiccPaymentGateway.approveAfterAuth()`의 금액 검증 로직 (`expectedAmount != null` 비교)이 amount=0 케이스를 처리하지 않을 수 있다.

**How to avoid:** `EasyPayApiController.billkeyReturn()`에서는 `KiccApiClient.approvePayment()`를 직접 호출하므로 `KiccPaymentGateway`의 금액 검증을 거치지 않는다. 이 경로는 이미 안전하게 우회되어 있음.

### Pitfall 6: JPQL null 파라미터 (프로젝트 기술 함정 #1)

**What goes wrong:** `EasyPayCardRepository.findByEmailOrderByCreatedAtDesc(email)`에서 email이 null이면 bytea 캐스팅 에러.

**How to avoid:** Controller/Service에서 email null 체크 후 `HolaException` 던지기. 이미 `EasyPayApiController.registerBillkey()`에서 `email == null || email.isBlank()` 체크하고 있음.

---

## Code Examples

### 빌키 등록 Step 1 — paymentGateway 호출 패턴

```java
// EasyPayApiController.registerBillkey() — 이미 구현됨
// paymentMethod="BILLING" → KiccPaymentGateway에서 payMethodTypeCode="81", amount=0으로 자동 변환
RegisterResult result = paymentGateway.registerTransaction(RegisterRequest.builder()
    .orderId(shopOrderNo)
    .amount(BigDecimal.ZERO)
    .goodsName("간편결제 카드 등록")
    .customerName(customerName)
    .customerPhone(customerPhone)
    .customerEmail(email)
    .paymentMethod("BILLING")
    .deviceType(deviceType)
    .build());
// → KiccPaymentGateway.buildReturnUrl("81") → "/api/v1/booking/easy-pay/billkey-return"
```

### 빌키 결제 승인 — KiccBatchApprovalRequest 구조

```java
// EasyPayApiController.payWithBillkey() — 이미 구현됨
KiccBatchApprovalRequest batchRequest = KiccBatchApprovalRequest.builder()
    .mallId(kiccProperties.getMallId())
    .shopTransactionId(UUID.randomUUID().toString())
    .shopOrderNo(shopOrderNo)
    .approvalReqDate(LocalDate.now().format(DATE_FMT))
    .amount(validation.getGrandTotal().longValue())
    .orderInfo(KiccBatchApprovalRequest.OrderInfo.builder().goodsName("호텔 숙박").build())
    .payMethodInfo(KiccBatchApprovalRequest.PayMethodInfo.builder()
        .billKeyMethodInfo(BillKeyMethodInfo.builder().batchKey(cardEntity.getBatchKey()).build())
        .cardMethodInfo(CardMethodInfo.builder().installmentMonth(0).build())
        .build())
    .build();
KiccApprovalResponse approvalResponse = kiccApiClient.approveBatchPayment(batchRequest);
```

### MockPaymentGateway 빌키 메서드 추가 패턴 (Claude's Discretion)

```java
// MockPaymentGateway에 추가 필요
@Override
public RegisterResult registerTransaction(RegisterRequest request) {
    String mockUrl = "http://localhost:8080/booking/mock-billkey?shopOrderNo=" + request.getOrderId();
    return RegisterResult.success(mockUrl, request.getOrderId());
}

@Override
public PaymentResult approveAfterAuth(ApproveAfterAuthRequest request) {
    return PaymentResult.builder()
        .success(true).gatewayId("MOCK").pgProvider("MOCK")
        .pgCno("MOCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
        .amount(request.getExpectedAmount())
        .approvalNo("MOCK-APPROVAL").pgCardType("신용")
        .processedAt(java.time.LocalDateTime.now()).build();
}
```

### 프론트엔드 이메일 안내 패턴 (booking.js 수정 필요)

```javascript
// renderEasyPayCards() 내 이메일 없을 때 안내 추가
renderEasyPayCards: function(cards) {
    var email = $('#email').val().trim();
    if (!email) {
        // D-01: 이메일 없을 때 안내 표시
        $('#easyPayCards').html(
            '<div class="text-muted small p-3 text-center">' +
            '<i class="fas fa-info-circle me-1"></i>' +
            '이메일을 먼저 입력하면 등록된 카드를 확인할 수 있습니다.</div>'
        );
        return;
    }
    // 기존 카드 렌더링 로직...
}
```

---

## KICC API 핵심 참고사항

### 빌키 등록 vs 일반결제 차이점

| 구분 | 일반결제 | 빌키 등록 |
|------|---------|----------|
| `payMethodTypeCode` | `"11"` (신용카드) | `"81"` (정기결제) |
| `amount` | 실결제금액 | **반드시 0** |
| 응답 `cardInfo.cardNo` | 마스킹 카드번호 | **빌키 (batchKey)** |
| 이후 결제 엔드포인트 | `/api/ep9/trades/approval` | `/api/trades/approval/batch` |
| returnUrl | `/payment/return` | `/easy-pay/billkey-return` |

### KICC 테스트 환경

- 테스트 도메인: `https://testpgapi.easypay.co.kr`
- 테스트 상점ID: `T5102001`
- `KiccPaymentGateway.approveAfterAuth()`에 테스트 환경 HMAC 검증 우회 로직 있음 (`mallId.startsWith("T")`)

---

## 프론트엔드 구현 현황

`booking.js` CheckoutPage에 이미 구현된 함수:

| 함수 | 구현 상태 | 비고 |
|------|-----------|------|
| `loadEasyPayCards()` | 완성 | `GET /easy-pay/cards?email=` 호출 |
| `renderEasyPayCards(cards)` | 완성 | `.easy-pay-card` 렌더링 |
| `registerBillkey()` | 완성 | PC popup / mobile redirect |
| `deleteEasyPayCard(cardId)` | 완성 | `DELETE /easy-pay/cards/{id}` |
| `submitEasyPayBooking()` | 완성 | `POST /easy-pay/pay` |
| `validateGuestInfo()` | 완성 | 이메일/이름/전화 체크 |
| `postMessage` 수신 핸들러 | 완성 | `BILLKEY_REGISTER_COMPLETE` |

**보완 필요:**
- `renderEasyPayCards()`: 이메일 없을 때 안내 메시지 추가 (D-01)
- 모바일 리다이렉트 후 카드 목록 갱신 처리 (`pageshow` 이벤트)

`.easy-pay-*` CSS 클래스: `booking.css`에 완성됨 (`.easy-pay-panel`, `.easy-pay-cards`, `.easy-pay-card`, `.easy-pay-add` 등).

---

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|-------------|-----------|---------|----------|
| PostgreSQL | `rsv_easy_pay_card` 테이블 | ✓ | 16 | — |
| Redis | 빌키 등록 임시 데이터 | ✓ | 7+ | — |
| KICC 테스트 API | 실제 빌키 발급/결제 | ✓ (외부) | testpgapi.easypay.co.kr | Mock (test 프로파일) |
| Java 17 | 빌드 | ✓ | 17 | — |

**Missing dependencies with no fallback:** 없음

**Note:** KICC 테스트 API 접근은 네트워크 환경에 의존. 로컬 개발 시 Mock 프로파일 사용.

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 5 + Mockito + TestContainers PostgreSQL 16 |
| Config file | `hola-app/src/test/resources/application-test.yml` |
| Quick run command | `./gradlew :hola-reservation:test --tests "com.hola.reservation.booking.*"` |
| Full suite command | `./gradlew test` |

### Phase Requirements → Test Map

| 요구사항 | 동작 | 테스트 유형 | 자동화 명령 | 파일 존재 |
|---------|------|-----------|-------------|---------|
| 카드 목록 조회 (D-01) | 이메일로 등록된 카드 반환 | unit | `./gradlew :hola-reservation:test --tests "*EasyPayCardServiceImplTest*"` | ❌ Wave 0 |
| 카드 등록 (D-02) | DB 저장 + 5개 제한 | unit | 위와 동일 | ❌ Wave 0 |
| 카드 삭제 (D-05) | soft delete + KICC 호출 | unit | 위와 동일 | ❌ Wave 0 |
| 빌키 결제 + 예약 생성 (D-04) | PaymentTransaction 생성 확인 | manual | 부킹엔진 시나리오 테스트 | N/A |

### Sampling Rate
- **Per task commit:** `./gradlew :hola-reservation:compileJava` (빌드 통과)
- **Per wave merge:** `./gradlew :hola-reservation:test`
- **Phase gate:** `./gradlew build` green

### Wave 0 Gaps
- [ ] `hola-reservation/src/test/.../EasyPayCardServiceImplTest.java` — 카드 등록/삭제/조회/제한 단위 테스트
- [ ] `MockPaymentGateway` 빌키 메서드 추가 후 통합 테스트 context load 확인

---

## Open Questions

1. **EasyPayApiController의 KiccApiClient 직접 의존 문제**
   - What we know: `EasyPayApiController`가 `@Profile("!test")` Bean인 `KiccApiClient`를 직접 주입함
   - What's unclear: 실제로 test profile에서 context load 실패가 발생하는지, 아니면 다른 방식으로 해결되어 있는지
   - Recommendation: `./gradlew :hola-app:test` 실행하여 확인. 실패 시 `EasyPayApiController`에 `@Profile("!test")` 추가하거나 Mock Bean 제공

2. **모바일 빌키 등록 후 카드 목록 갱신**
   - What we know: `window.history.back()` 후 bfcache 복원 시 JS 재실행 없음
   - What's unclear: 실제 모바일 환경에서 재현 가능한지, `pageshow` 이벤트가 bfcache 케이스를 포함하는지
   - Recommendation: `pageshow` 이벤트 + `event.persisted` 체크로 `loadEasyPayCards()` 재호출 구현

---

## Sources

### Primary (HIGH confidence)
- 직접 코드 분석: `EasyPayApiController.java`, `EasyPayCardServiceImpl.java`, `EasyPayCard.java`, `KiccPaymentGateway.java`, `KiccApiClient.java` 등 21개 파일
- `KICC_refer/KICC-API-학습정리.md` — KICC API 플로우, 필드 명세
- `.planning/phases/02-easy-payment/02-CONTEXT.md` — 확정된 설계 결정

### Secondary (MEDIUM confidence)
- `booking.js` CheckoutPage 전체 분석 — 프론트엔드 구현 현황
- `booking.css` `.easy-pay-*` CSS 현황 확인

---

## Metadata

**Confidence breakdown:**
- 구현 완료 현황: HIGH — 코드 직접 확인
- KICC API 명세: HIGH — 학습 정리 문서 참조
- 미완성 항목 식별: HIGH — 코드 갭 분석
- 테스트 전략: MEDIUM — 단위 테스트 파일 미존재 확인

**Research date:** 2026-03-30
**Valid until:** 2026-04-30 (KICC API는 안정적, 코드 분석은 현 시점 기준)
