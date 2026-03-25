# KICC PG 결제 통합 구현 계획서

> 작성일: 2026-03-25
> 최종 수정: 2026-03-25
> 프로젝트: Hola PMS (3~5성급 호텔 클라우드 PMS)
> 대상 PG: KICC 이지페이 (EasyPay)
> 상태: Phase 1~2 완료 / Phase 3~6 미착수

---

## 목차
1. [현황 분석](#1-현황-분석)
2. [전체 구현 로드맵](#2-전체-구현-로드맵)
3. [Phase 1: KICC PG 기반 인프라 구축](#3-phase-1)
4. [Phase 2: 부킹엔진 결제 연동](#4-phase-2)
5. [Phase 3: 빌키 관리 & PMS 관리자 결제](#5-phase-3)
6. [Phase 4: 취소/환불 처리](#6-phase-4)
7. [Phase 5: Folio 기반 정산관리](#7-phase-5)
8. [Phase 6: 야간정산(Night Audit) & EOD](#8-phase-6)
9. [호텔 업계 표준 고려사항](#9-호텔-업계-표준)
10. [기술적 주의사항](#10-기술적-주의사항)

---

## 1. 현황 분석 {#1-현황-분석}

### 이미 구현된 것
| 항목 | 상태 | 위치 |
|------|------|------|
| PaymentGateway 인터페이스 | ✅ 완료 | `hola-reservation/booking/gateway/PaymentGateway.java` |
| MockPaymentGateway | ✅ 완료 | `hola-reservation/booking/gateway/MockPaymentGateway.java` |
| ReservationPayment 엔티티 (낙관적 락) | ✅ 완료 | `rsv_reservation_payment` |
| PaymentTransaction 엔티티 | ✅ 완료 | `rsv_payment_transaction` |
| PaymentAdjustment 엔티티 | ✅ 완료 | `rsv_payment_adjustment` |
| 결제 서비스 (부분결제, 금액조정) | ✅ 완료 | `ReservationPaymentServiceImpl` |
| 취소수수료 정책 | ✅ 완료 | `CancellationPolicyServiceImpl` |
| 일별 요금 계산 (봉사료/VAT) | ✅ 완료 | `PriceCalculationService` |
| 예치금 DB 스키마 | ⚠️ 스키마만 | `rsv_reservation_deposit` (엔티티 없음) |
| 프론트엔드 결제 UI | ✅ 기본 | `reservation-payment.js` |

### 미구현 (신규 개발 필요)
| 항목 | 우선순위 |
|------|---------|
| KICC PG 실제 연동 (KiccPaymentGateway) | 🔴 최우선 |
| 빌키 관리 (등록/결제/삭제) | 🔴 높음 |
| PG 취소/환불 실제 연동 | 🔴 높음 |
| 예치금 엔티티/서비스 (Deposit) | 🟡 중간 |
| Folio (투숙 기간 비용 원장) | 🟡 중간 |
| 야간정산 (Night Audit) / EOD | 🟠 후순위 |
| 정산 리포트 / 매출 통계 | 🟠 후순위 |
| 웹훅 수신 처리 | 🟡 중간 |

---

## 2. 전체 구현 로드맵 {#2-전체-구현-로드맵}

```
Phase 1: KICC PG 기반 인프라 ──────────── [3~4일]
  └─ 설정, 클라이언트, 게이트웨이 구현체

Phase 2: 부킹엔진 결제 연동 ──────────── [3~4일]
  └─ 프론트 예약 → 테스트PG 결제 → PMS 결제내역 확인

Phase 3: 빌키 관리 & PMS 관리자 결제 ─── [4~5일]
  └─ 카드 등록, 빌키 저장, 관리자 수기결제

Phase 4: 취소/환불 처리 ──────────────── [3~4일]
  └─ 전체취소, 부분취소, 환불, 웹훅

Phase 5: Folio 기반 정산관리 ─────────── [5~6일]
  └─ Folio 엔티티, 차지 포스팅, 정산 UI

Phase 6: 야간정산 & EOD ──────────────── [4~5일]
  └─ Night Audit, 일일 마감, 정산 리포트
```

**총 예상: 22~28일 (Phase별 순차 진행)**

---

## 3. Phase 1: KICC PG 기반 인프라 구축 {#3-phase-1}

### 목표
KICC 이지페이 API와 통신할 수 있는 기반 인프라 구축

### Step 1-1: KICC 설정 관리
**파일**: `application.yml` + `KiccProperties.java`

```yaml
# application-local.yml
kicc:
  mall-id: T5102001           # 테스트 상점ID
  secret-key: ${KICC_SECRET_KEY}  # 환경변수
  api-domain: https://testpgapi.easypay.co.kr
  return-base-url: http://localhost:8080
  timeout-seconds: 30
  billing-cert-type: "0"      # 카드인증: 번호+유효기간+생년월일+비밀번호
```

```java
@ConfigurationProperties(prefix = "kicc")
@Validated
public class KiccProperties {
    @NotBlank private String mallId;
    @NotBlank private String secretKey;
    @NotBlank private String apiDomain;
    @NotBlank private String returnBaseUrl;
    private int timeoutSeconds = 30;
    private String billingCertType = "0";
}
```

### Step 1-2: KICC HTTP 클라이언트
**파일**: `KiccApiClient.java`

```java
@Component
public class KiccApiClient {
    private final RestTemplate restTemplate;
    private final KiccProperties properties;

    // 거래등록
    public KiccRegisterResponse registerTransaction(KiccRegisterRequest request);
    // 결제승인
    public KiccApprovalResponse approvePayment(KiccApprovalRequest request);
    // 빌키 결제
    public KiccApprovalResponse approveBatchPayment(KiccBatchApprovalRequest request);
    // 빌키 삭제
    public KiccRemoveKeyResponse removeBatchKey(KiccRemoveKeyRequest request);
    // 결제 취소/환불
    public KiccReviseResponse revisePayment(KiccReviseRequest request);
    // 거래상태 조회
    public KiccApprovalResponse retrieveTransaction(KiccQueryRequest request);

    // 메시지 인증값 생성 (HmacSHA256)
    private String generateMsgAuthValue(String... parts);
    // 메시지 인증값 검증
    private boolean verifyMsgAuthValue(String received, String... parts);
}
```

### Step 1-3: KICC DTO 정의
**디렉토리**: `hola-reservation/src/main/java/com/hola/reservation/pg/kicc/dto/`

| DTO 클래스 | 용도 |
|-----------|------|
| `KiccRegisterRequest` | 거래등록 요청 |
| `KiccRegisterResponse` | 거래등록 응답 (authPageUrl) |
| `KiccApprovalRequest` | 결제승인/빌키발급 요청 |
| `KiccApprovalResponse` | 승인 응답 (pgCno, paymentInfo) |
| `KiccBatchApprovalRequest` | 빌키 결제 승인 요청 |
| `KiccReviseRequest` | 취소/환불 요청 |
| `KiccReviseResponse` | 취소/환불 응답 |
| `KiccRemoveKeyRequest` | 빌키 삭제 요청 |
| `KiccRemoveKeyResponse` | 빌키 삭제 응답 |
| `KiccQueryRequest` | 거래상태 조회 요청 |
| `KiccPaymentInfo` | 결제수단별 응답 (공통) |
| `KiccCardInfo` | 카드 결제 상세 |

### Step 1-4: KiccPaymentGateway 구현
**파일**: `KiccPaymentGateway.java` (기존 PaymentGateway 인터페이스 구현)

```java
@Component
@Primary  // MockPaymentGateway 대체
@Profile("!test")  // 테스트 환경에서는 Mock 사용
public class KiccPaymentGateway implements PaymentGateway {

    @Override
    public PaymentResult authorize(PaymentRequest request) {
        // 1. KICC 거래등록 → authPageUrl 반환 (프론트에서 결제창 호출)
        // 2. returnUrl 콜백 처리 → authorizationId 수신
        // 3. KICC 결제승인 → pgCno 수신
        // 4. 금액 검증 + msgAuthValue 검증
    }

    @Override
    public PaymentResult cancel(String approvalNo) {
        // KICC 결제취소 API 호출
    }

    @Override
    public String getGatewayId() {
        return "KICC";
    }
}
```

> ⚠️ **아키텍처 결정**: 기존 PaymentGateway 인터페이스는 동기식(authorize → 즉시 결과)으로 설계됨.
> KICC는 3단계(거래등록 → 결제창 → 승인) 비동기 방식이므로 **인터페이스 확장 필요**.
> `PaymentGateway`에 `registerTransaction()`, `approveAfterAuth()` 메서드 추가 고려.

### Step 1-5: DB 마이그레이션
**파일**: `V4_21_0__add_pg_transaction_fields.sql`

```sql
-- PaymentTransaction에 PG 관련 필드 추가
ALTER TABLE rsv_payment_transaction
    ADD COLUMN pg_provider VARCHAR(20),           -- 'KICC', 'MOCK'
    ADD COLUMN pg_cno VARCHAR(20),                -- PG 거래고유번호
    ADD COLUMN pg_transaction_id VARCHAR(60),     -- 멱등성 키
    ADD COLUMN pg_status_code VARCHAR(10),        -- 거래상태 코드
    ADD COLUMN pg_approval_no VARCHAR(100),       -- PG 승인번호
    ADD COLUMN pg_approval_date VARCHAR(14),      -- PG 승인일시
    ADD COLUMN pg_card_no VARCHAR(20),            -- 마스킹 카드번호
    ADD COLUMN pg_issuer_name VARCHAR(50),        -- 발급사명
    ADD COLUMN pg_acquirer_name VARCHAR(50),      -- 매입사명
    ADD COLUMN pg_installment_month INTEGER DEFAULT 0,  -- 할부개월
    ADD COLUMN pg_card_type VARCHAR(10),          -- 신용/체크/기프트
    ADD COLUMN pg_raw_response TEXT;              -- 원본 응답 JSON (감사 추적용)
```

### Step 1-6: RestTemplate 설정
**파일**: `KiccConfig.java`

```java
@Configuration
public class KiccConfig {
    @Bean("kiccRestTemplate")
    public RestTemplate kiccRestTemplate(KiccProperties properties) {
        // timeout 30초 설정
        // Jackson ObjectMapper: FAIL_ON_UNKNOWN_PROPERTIES = false
        // UTF-8 인코딩
        // 에러 핸들러
    }
}
```

### Phase 1 체크리스트
- [ ] `KiccProperties` + application-local.yml 설정
- [ ] `KiccApiClient` HTTP 클라이언트 구현
- [ ] KICC 요청/응답 DTO 10종 생성
- [ ] `KiccPaymentGateway` 구현체 (PaymentGateway 인터페이스)
- [ ] HmacSHA256 메시지 인증값 생성/검증 유틸
- [ ] Flyway `V4_21_0` PG 필드 마이그레이션
- [ ] RestTemplate 설정 (timeout 30초, UTF-8)
- [ ] 단위 테스트: 메시지 인증값, DTO 직렬화/역직렬화

---

## 4. Phase 2: 부킹엔진 결제 연동 {#4-phase-2}

### 목표
부킹엔진(프론트 예약 화면)에서 KICC 테스트 PG로 결제 → PMS에서 결제 내역 확인

### Step 2-1: 결제 플로우 컨트롤러
**파일**: `KiccPaymentApiController.java`

```
POST /api/v1/booking/payment/register     → KICC 거래등록, authPageUrl 반환
POST /api/v1/booking/payment/return       → KICC returnUrl 콜백 수신 (authorizationId)
POST /api/v1/booking/payment/approve      → KICC 결제승인 요청
GET  /api/v1/booking/payment/result       → 결제 결과 페이지
```

### Step 2-2: 부킹엔진 결제 프론트엔드
**파일**: `booking-payment.js` (신규) or `booking-complete.html` 수정

**결제 플로우 (사용자 관점):**
1. 예약정보 입력 → "결제하기" 클릭
2. PMS 서버에 거래등록 요청 → `authPageUrl` 수신
3. KICC 결제창 팝업/리다이렉트 (신용카드, 간편결제 등)
4. 고객 카드사 인증 완료
5. `returnUrl`로 `authorizationId` 전달
6. PMS 서버에서 KICC 승인 요청
7. 결제 완료 → 예약 확정 → 결제 내역 저장

**프론트엔드 구현 포인트:**
```javascript
// 거래등록 → 결제창 호출
async function initiatePayment(reservationData) {
    // 1. PMS 서버에 거래등록
    const response = await HolaPms.ajax({
        url: '/api/v1/booking/payment/register',
        method: 'POST',
        data: {
            reservationId: reservationData.id,
            amount: reservationData.totalAmount,
            goodsName: `${reservationData.roomClassName} ${reservationData.nights}박`,
            customerName: reservationData.guestName,
            customerContactNo: reservationData.phone
        }
    });

    // 2. KICC 결제창 팝업 (PC) or 리다이렉트 (Mobile)
    if (isMobile()) {
        window.location.href = response.data.authPageUrl;
    } else {
        window.open(response.data.authPageUrl, 'kiccPay', 'width=720,height=680');
    }
}
```

### Step 2-3: 결제 결과 저장 서비스
**확장**: `BookingServiceImpl` 수정

```java
// 기존 MockPaymentGateway 호출 → KiccPaymentGateway로 교체
// 승인 결과 → PaymentTransaction 엔티티에 PG 정보 저장
// ReservationPayment 상태 갱신
```

### Step 2-4: PMS 관리자 결제내역 확인
**기존 UI 활용**: `reservation-payment.js`에 PG 정보 표시 추가

- 결제 거래 이력에 PG 승인번호, 카드사, 할부개월 표시
- PG 거래번호(pgCno) 표시 (취소 시 사용)

### Phase 2 체크리스트
- [ ] `KiccPaymentApiController` 결제 플로우 API 3종
- [ ] 부킹엔진 결제 프론트엔드 (결제창 팝업/리다이렉트)
- [ ] `returnUrl` 콜백 처리 (authorizationId → 승인 요청)
- [ ] SecurityConfig: 부킹 결제 URL 인증 설정 (API-KEY or 세션)
- [ ] 결제 결과 → PaymentTransaction 저장 (PG 필드 매핑)
- [ ] PMS 관리자 UI에 PG 결제 정보 표시
- [ ] KICC 테스트 서버로 E2E 결제 테스트
- [ ] 금액 불일치 시 자동 취소 로직
- [ ] 네트워크 오류 시 거래상태 조회 fallback

---

## 5. Phase 3: 빌키 관리 & PMS 관리자 결제 {#5-phase-3}

### 목표
카드 정보를 빌키로 등록, PMS 관리자가 빌키로 수기 결제 가능

### 호텔 업계 빌키 활용 시나리오
1. **체크인 시 예치금 보증**: 카드 등록(빌키 발급) → 필요 시 과금
2. **No-Show 수수료 과금**: 사전 등록된 카드로 자동 과금
3. **미니바/룸서비스 후결제**: 체크아웃 시 등록 카드로 일괄 결제
4. **법인 고객 반복 결제**: 기업 카드 등록 → 월별 청구

### Step 3-1: 빌키 엔티티 (예치금 테이블 활용)
**파일**: `ReservationDeposit.java` (기존 스키마 `rsv_reservation_deposit` 활용)

```java
@Entity
@Table(name = "rsv_reservation_deposit")
public class ReservationDeposit {
    @Id @GeneratedValue
    private Long id;

    @Column(name = "master_reservation_id")
    private Long masterReservationId;

    // 빌키 정보 (AES-256 암호화)
    @Column(name = "bill_key_encrypted")
    private String billKeyEncrypted;          // KICC 빌키 (암호화 저장)

    @Column(name = "card_mask_no")
    private String cardMaskNo;                // 마스킹 카드번호 (UI용)

    @Column(name = "card_issuer_name")
    private String cardIssuerName;            // 발급사명

    @Column(name = "card_issuer_code")
    private String cardIssuerCode;            // 발급사 코드

    @Column(name = "bill_key_pg_cno")
    private String billKeyPgCno;              // 빌키 발급 시 PG 거래번호

    @Enumerated(EnumType.STRING)
    @Column(name = "deposit_type")
    private DepositType depositType;          // CARD_ON_FILE, PRE_AUTH, GUARANTEE

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private DepositStatus status;             // ACTIVE, USED, EXPIRED, DELETED

    @Column(name = "registered_at")
    private LocalDateTime registeredAt;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Column(name = "registered_by")
    private String registeredBy;              // 등록자 (관리자 or 게스트)
}
```

### Step 3-2: 빌키 등록 플로우
**API:**
```
POST /api/v1/properties/{propertyId}/reservations/{rsvId}/deposit/register
  → KICC 빌키 등록창 URL 반환

POST /api/v1/properties/{propertyId}/deposit/kicc/return
  → KICC returnUrl 콜백 → 빌키 발급 → 암호화 저장

GET  /api/v1/properties/{propertyId}/reservations/{rsvId}/deposit
  → 등록된 카드 목록 (마스킹 번호 + 카드사)

DELETE /api/v1/properties/{propertyId}/reservations/{rsvId}/deposit/{depositId}
  → 빌키 삭제 (KICC API + DB)
```

### Step 3-3: 빌키 결제 (관리자 수기 결제)
**API:**
```
POST /api/v1/properties/{propertyId}/reservations/{rsvId}/deposit/{depositId}/charge
```

**요청:**
```json
{
    "amount": 150000,
    "description": "디럭스 더블룸 숙박료",
    "installmentMonth": 0
}
```

**처리 흐름:**
1. 관리자가 예약 상세 → 결제 탭 → "등록 카드로 결제" 클릭
2. 금액, 항목, 할부개월 입력
3. PMS 서버 → KICC 빌키 결제 API (`/api/trades/approval/batch`) 호출
4. 결과 → `PaymentTransaction` 저장
5. `ReservationPayment` 금액 갱신

### Step 3-4: 관리자 UI
**파일**: `reservation-payment.js` 확장

- 등록 카드 목록 표시 (마스킹 번호, 카드사, 등록일)
- "카드 등록" 버튼 → 빌키 등록창 팝업
- "등록 카드로 결제" 버튼 → 금액 입력 모달
- "카드 삭제" 버튼 → 빌키 삭제 확인

### Step 3-5: Flyway 마이그레이션
**파일**: `V4_22_0__extend_reservation_deposit_for_billkey.sql`

```sql
-- 기존 rsv_reservation_deposit 테이블에 빌키 관련 컬럼 추가/변경
ALTER TABLE rsv_reservation_deposit
    ADD COLUMN IF NOT EXISTS bill_key_encrypted VARCHAR(500),
    ADD COLUMN IF NOT EXISTS card_mask_no VARCHAR(40),
    ADD COLUMN IF NOT EXISTS card_issuer_name VARCHAR(50),
    ADD COLUMN IF NOT EXISTS card_issuer_code VARCHAR(10),
    ADD COLUMN IF NOT EXISTS bill_key_pg_cno VARCHAR(20),
    ADD COLUMN IF NOT EXISTS deposit_type VARCHAR(20) DEFAULT 'CARD_ON_FILE',
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE',
    ADD COLUMN IF NOT EXISTS registered_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS expired_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS registered_by VARCHAR(100);

-- 기존 수기 카드정보 컬럼은 유지 (하위호환)
-- card_number_encrypted, card_cvc_encrypted 등은 레거시로 남김
```

### Phase 3 체크리스트
- [ ] `ReservationDeposit` 엔티티 + Repository
- [ ] AES-256 암호화 유틸 (빌키 암/복호화)
- [ ] 빌키 등록 플로우 (거래등록 → 등록창 → 발급 → 저장)
- [ ] 빌키 결제 API (관리자 수기 결제)
- [ ] 빌키 삭제 API (KICC + DB 동시)
- [ ] `DepositApiController` + `DepositService`
- [ ] Flyway `V4_22_0` 마이그레이션
- [ ] 관리자 UI: 카드 목록, 등록, 결제, 삭제
- [ ] 보안: 빌키 접근 권한 (PROPERTY_ADMIN 이상)

---

## 6. Phase 4: 취소/환불 처리 {#6-phase-4}

### 목표
PMS 관리자가 결제 건 전체/부분 취소 및 환불 가능

### 호텔 취소/환불 시나리오
| 시나리오 | KICC API | 처리방식 |
|---------|----------|---------|
| 예약 취소 (전액 환불) | reviseTypeCode="40" | 전체취소 |
| 예약 변경 (차액 환불) | reviseTypeCode="32" | 부분취소 |
| No-Show 수수료 제외 환불 | reviseTypeCode="32" | 부분취소 (수수료 차감) |
| 체크아웃 후 과다 청구 정정 | reviseTypeCode="32" | 부분취소 |
| OTA 예약 취소 | reviseTypeCode="40" | 전체취소 (채널 정책 적용) |

### Step 4-1: 취소/환불 서비스
**파일**: `PaymentCancellationService.java`

```java
public interface PaymentCancellationService {
    // 전체 취소
    CancelResult cancelFull(Long reservationId, Long transactionId, String reason);

    // 부분 취소
    CancelResult cancelPartial(Long reservationId, Long transactionId,
                               BigDecimal cancelAmount, String reason);

    // 환불 (가상계좌/계좌이체)
    RefundResult refund(Long reservationId, Long transactionId,
                        RefundRequest refundRequest);

    // 거래상태 조회
    TransactionStatus queryStatus(Long transactionId);
}
```

### Step 4-2: 취소/환불 API
```
POST /api/v1/properties/{propertyId}/reservations/{rsvId}/payment/cancel
  Body: { "transactionId": 123, "cancelType": "FULL", "reason": "고객 요청" }

POST /api/v1/properties/{propertyId}/reservations/{rsvId}/payment/cancel-partial
  Body: { "transactionId": 123, "cancelAmount": 50000, "reason": "객실 변경 차액" }

POST /api/v1/properties/{propertyId}/reservations/{rsvId}/payment/refund
  Body: { "transactionId": 123, "refundBankCode": "003",
          "refundAccountNo": "1234567890", "refundDepositName": "홍길동" }

POST /api/v1/properties/{propertyId}/reservations/{rsvId}/payment/query
  Body: { "transactionId": 123 }
```

### Step 4-3: 취소 수수료 연동
기존 `CancellationPolicyServiceImpl`과 연동:
1. 취소 요청 → 취소 수수료 계산
2. 수수료 > 0 → 부분취소 (원금 - 수수료)
3. 수수료 = 0 → 전체취소
4. 취소 수수료 → `PaymentTransaction`(CANCEL_FEE)로 기록

### Step 4-4: 웹훅 수신 컨트롤러
**파일**: `KiccWebhookController.java`

```
POST /api/v1/webhook/kicc/payment    → 결제완료 노티
POST /api/v1/webhook/kicc/refund     → 환불완료 노티 (지결환불)
POST /api/v1/webhook/kicc/deposit    → 가상계좌 입금 노티
```

- IP 화이트리스트 필터 적용 (KICC 서버 IP만 허용)
- SecurityConfig: 웹훅 URL은 인증 bypass

### Step 4-5: 관리자 UI
**파일**: `reservation-payment.js` 확장

- 결제 이력에 "전체취소", "부분취소" 버튼 추가
- 부분취소 모달: 취소 금액 입력, 잔여 금액 표시
- 취소 사유 입력 필수
- 취소 결과 즉시 반영 (상태 뱃지 갱신)

### Phase 4 체크리스트
- [ ] `PaymentCancellationService` 인터페이스 + 구현
- [ ] 전체취소 API (KICC reviseTypeCode="40")
- [ ] 부분취소 API (KICC reviseTypeCode="32")
- [ ] 환불 API (가상계좌용, reviseTypeCode="60"/"63")
- [ ] 거래상태 조회 API (retrieveTransaction)
- [ ] 취소수수료 정책 연동 (CancellationPolicyService)
- [ ] 웹훅 수신 컨트롤러 (IP 화이트리스트)
- [ ] SecurityConfig: 웹훅 URL bypass
- [ ] 관리자 UI: 취소/부분취소 기능
- [ ] 취소 트랜잭션 기록 (PaymentTransaction type=CANCEL_FEE/REFUND)

---

## 7. Phase 5: Folio 기반 정산관리 {#7-phase-5}

### 목표
호텔 투숙 기간 발생 비용을 Folio로 관리하고 체크아웃 시 정산

### Folio 개념 (호텔 업계 표준)
```
Guest Folio (투숙객 원장)
├── Room Charge (객실비) ─── DailyCharge에서 자동 포스팅
├── Service Charge (부대시설) ─── TC(TransactionCode) 기반
│   ├── F&B (레스토랑, 룸서비스, 미니바)
│   ├── Laundry (세탁)
│   ├── Spa/Fitness
│   └── Telephone
├── Tax (세금) ─── 봉사료 + VAT
├── Payment (결제) ─── 카드/현금/빌키
├── Adjustment (조정) ─── 할인/추가요금
└── Balance = 총 Charge - 총 Payment
```

### Step 5-1: Folio 엔티티
**파일**: `Folio.java`, `FolioEntry.java`

```java
@Entity
@Table(name = "rsv_folio")
public class Folio extends BaseEntity {
    @Column(name = "sub_reservation_id")
    private Long subReservationId;

    @Column(name = "folio_number")
    private String folioNumber;          // "F-0001" 시퀀스

    @Enumerated(EnumType.STRING)
    private FolioType folioType;         // GUEST, GROUP, COMPANY, HOUSE

    private BigDecimal totalDebit;       // 총 차변 (Charge)
    private BigDecimal totalCredit;      // 총 대변 (Payment)
    private BigDecimal balance;          // 잔액

    @Enumerated(EnumType.STRING)
    private FolioStatus status;          // OPEN, SETTLED, CLOSED
}

@Entity
@Table(name = "rsv_folio_entry")
public class FolioEntry extends BaseEntity {
    @Column(name = "folio_id")
    private Long folioId;

    @Column(name = "transaction_code_id")
    private Long transactionCodeId;      // TC 코드 참조

    @Enumerated(EnumType.STRING)
    private EntryType entryType;         // DEBIT(차변/Charge), CREDIT(대변/Payment)

    private BigDecimal amount;
    private BigDecimal taxAmount;
    private BigDecimal serviceChargeAmount;

    private String description;
    private LocalDate postingDate;       // 포스팅 일자
    private String postedBy;             // 포스팅 직원

    // PG 결제 연결
    @Column(name = "payment_transaction_id")
    private Long paymentTransactionId;
}
```

### Step 5-2: Folio 서비스
```java
public interface FolioService {
    // Folio 생성 (체크인 시 자동)
    Folio createFolio(Long subReservationId, FolioType type);

    // Charge 포스팅
    FolioEntry postCharge(Long folioId, PostChargeRequest request);

    // Payment 포스팅 (결제 완료 시)
    FolioEntry postPayment(Long folioId, Long paymentTransactionId);

    // 잔액 조회
    BigDecimal getBalance(Long folioId);

    // Folio 정산 (체크아웃)
    void settleFolio(Long folioId);

    // Folio 이전 (객실 이동 시)
    void transferEntry(Long fromFolioId, Long toFolioId, Long entryId);

    // 일별 자동 포스팅 (Night Audit 시)
    void autoPostDailyCharges(Long propertyId, LocalDate date);
}
```

### Step 5-3: Folio UI (관리자)
**파일**: `folio-page.js` (신규)

| 기능 | 설명 |
|------|------|
| Folio 조회 | 투숙객별 Folio 목록 (DEBIT/CREDIT/Balance) |
| Charge 등록 | TC 선택 → 금액 → 포스팅 |
| 결제 등록 | 현금/카드/빌키 결제 → CREDIT 포스팅 |
| 정산 (Settle) | 잔액 0 확인 → Folio Close |
| 인쇄 | 투숙 명세서 출력 |

### Step 5-4: Flyway 마이그레이션
**파일**: `V9_0_0__create_folio_tables.sql`

```sql
CREATE TABLE rsv_folio (...);
CREATE TABLE rsv_folio_entry (...);
CREATE INDEX idx_folio_sub_reservation ON rsv_folio(sub_reservation_id);
CREATE INDEX idx_folio_entry_posting_date ON rsv_folio_entry(posting_date);
```

### Phase 5 체크리스트
- [ ] `Folio`, `FolioEntry` 엔티티 설계 및 구현
- [ ] `FolioService` 인터페이스 + 구현
- [ ] Folio 자동 생성 (체크인 연동)
- [ ] Charge 포스팅 (TC 기반)
- [ ] Payment 포스팅 (결제 연동)
- [ ] Folio 정산 (체크아웃 연동)
- [ ] Flyway 마이그레이션
- [ ] Folio 관리자 UI (CRUD + 인쇄)
- [ ] 체크아웃 시 잔액 검증 (기존 로직 연동)

---

## 8. Phase 6: 야간정산 (Night Audit) & EOD {#8-phase-6}

### 목표
일일 영업 마감, 객실비 자동 포스팅, 매출 리포트 생성

### Night Audit 프로세스 (호텔 업계 표준)
```
1. 재실 게스트 검증 → 미체크인 예약 확인
2. 일별 객실비 자동 포스팅 → DailyCharge → Folio DEBIT
3. 미결제 Folio 확인 → Balance > 0 경고
4. 객실 상태 → 하우스키핑 연동 (Occupied → Dirty)
5. 객실 재고 롤오버 → 다음날 재고 생성
6. 일일 마감 → EOD 리포트 생성
7. 영업일 변경 → business_date 갱신
```

### Step 6-1: Night Audit 서비스
```java
public interface NightAuditService {
    // Night Audit 실행
    NightAuditResult executeNightAudit(Long propertyId, LocalDate businessDate);

    // 일별 Charge 자동 포스팅
    int autoPostRoomCharges(Long propertyId, LocalDate businessDate);

    // 미결제 Folio 리포트
    List<OutstandingFolio> getOutstandingFolios(Long propertyId);

    // EOD 리포트 생성
    EodReport generateEodReport(Long propertyId, LocalDate businessDate);

    // 영업일 변경
    void rolloverBusinessDate(Long propertyId);
}
```

### Step 6-2: 정산 리포트 엔티티
```java
@Entity
@Table(name = "rsv_daily_settlement")
public class DailySettlement extends BaseEntity {
    @Column(name = "property_id")
    private Long propertyId;

    private LocalDate businessDate;

    // 매출 요약
    private BigDecimal totalRoomRevenue;
    private BigDecimal totalServiceRevenue;
    private BigDecimal totalTax;
    private BigDecimal totalServiceCharge;
    private BigDecimal grandTotal;

    // 결제 요약
    private BigDecimal totalCardPayment;
    private BigDecimal totalCashPayment;
    private BigDecimal totalBillKeyPayment;
    private BigDecimal totalCancellation;
    private BigDecimal totalRefund;

    // 객실 요약
    private Integer totalRooms;
    private Integer occupiedRooms;
    private Double occupancyRate;
    private BigDecimal adr;              // Average Daily Rate
    private BigDecimal revPar;           // Revenue Per Available Room

    @Enumerated(EnumType.STRING)
    private SettlementStatus status;     // OPEN, AUDITED, CLOSED
}
```

### Step 6-3: 정산 UI
**파일**: `settlement-page.js` (신규)

| 기능 | 설명 |
|------|------|
| 일일 정산 | Night Audit 실행 + EOD 리포트 |
| 매출 현황 | 일별/주별/월별 매출 차트 |
| 결제 내역 | 기간별 결제/취소/환불 집계 |
| 미수금 관리 | 미결제 Folio 목록 |
| KPI 대시보드 | 점유율, ADR, RevPAR |

### Phase 6 체크리스트
- [ ] `NightAuditService` 구현
- [ ] 일별 Charge 자동 포스팅 로직
- [ ] `DailySettlement` 엔티티 + 마이그레이션
- [ ] EOD 리포트 생성 로직
- [ ] 영업일 관리 (business_date)
- [ ] 정산 관리자 UI
- [ ] 매출 통계 차트 (일별/주별/월별)
- [ ] KPI 지표: 점유율, ADR, RevPAR

---

## 9. 호텔 업계 표준 고려사항 {#9-호텔-업계-표준}

### 3~5성급 호텔 결제 패턴
1. **예치금 보증 (Guarantee)**
   - 체크인 시 카드 등록 (빌키) → 보증금 홀드 아님, 과금 가능 상태
   - No-Show, 미니바, 룸서비스 등 사후 과금에 사용
   - 체크아웃 시 등록 카드로 잔액 일괄 결제

2. **OTA 연동 결제**
   - Booking.com: 가상카드 (VCC) 결제 → 호텔 수수료 정산
   - Expedia: 선불/후불 모델
   - 자사 예약: 직접 PG 결제

3. **그룹 예약 정산**
   - Master Folio (그룹 전체) + Individual Folio (개인 부대비용)
   - 기업 계약: 월말 일괄 정산 (Invoice)

4. **부분 결제 / 분할 결제**
   - 숙박비: 카드 A, 부대비용: 카드 B
   - 일부 현금, 일부 카드

5. **환율 관리**
   - 외국인 투숙객: KRW 기준 결제 (환율 표시는 참고용)
   - DCC(Dynamic Currency Conversion): 향후 확장

### 관리자 편의성
- **원클릭 결제**: 등록 카드 → 금액 입력 → 즉시 결제
- **영수증 이메일**: 결제 완료 시 자동 발송 (향후)
- **취소/환불 사유**: 필수 입력, 감사 추적
- **관리자 권한**: PROPERTY_ADMIN 이상만 취소/환불 가능

### 사용자(게스트) 편의성
- **다양한 결제수단**: 신용카드, 간편결제(카카오/네이버/토스)
- **모바일 최적화**: 모바일 결제창 자동 감지
- **결제 확인 알림**: 예약 확인 이메일에 결제 정보 포함

---

## 10. 기술적 주의사항 {#10-기술적-주의사항}

### 보안
| 항목 | 조치 |
|------|------|
| KICC mallId / secretKey | `application.yml` 환경변수 참조 (`${KICC_SECRET_KEY}`) |
| 빌키 (batchKey) | AES-256-GCM 암호화 후 DB 저장 |
| 카드번호 | PMS에 절대 저장 금지 (마스킹 번호만 저장) |
| msgAuthValue | 모든 KICC 응답에서 HMAC 검증 필수 |
| 웹훅 IP | KICC 서버 IP 화이트리스트 필터 |
| PCI DSS | 빌키 사용으로 카드 원문 미저장 → SAQ-A 수준 |
| 결제 금액 | 요청-응답 금액 비교 검증 → 불일치 시 자동 취소 |

### 성능 / 안정성
| 항목 | 조치 |
|------|------|
| KICC API timeout | 30초 (KICC 권고) |
| 멱등성 | shopTransactionId에 UUID 사용 (중복 결제 방지) |
| 네트워크 실패 | `retrieveTransaction`으로 상태 조회 → 재시도 또는 취소 |
| DB 트랜잭션 | 결제 성공 → DB 저장 실패 시 KICC 취소 API 호출 |
| 동시성 제어 | `ReservationPayment` 낙관적 락 (기존 @Version 활용) |
| Jackson 설정 | `FAIL_ON_UNKNOWN_PROPERTIES = false` (하위 호환성) |

### 기존 코드 영향도
| 기존 코드 | 변경사항 |
|----------|---------|
| `PaymentGateway` 인터페이스 | 메서드 추가: `registerTransaction()`, `approveAfterAuth()` |
| `MockPaymentGateway` | `@Profile("test")` 추가 (테스트 전용) |
| `BookingServiceImpl` | KICC 3단계 플로우 적용 |
| `ReservationPaymentServiceImpl` | PG 정보 저장 로직 추가 |
| `PaymentTransaction` 엔티티 | PG 관련 컬럼 추가 |
| `rsv_reservation_deposit` | 빌키 관련 컬럼 추가 |
| `reservation-payment.js` | PG 정보 표시, 취소 버튼 추가 |
| `SecurityConfig` | 웹훅 URL 인증 bypass, 결제 returnUrl 설정 |

### 테스트 전략
| 단계 | 방법 |
|------|------|
| 단위 테스트 | DTO 직렬화, HMAC 생성/검증, 금액 비교 로직 |
| 통합 테스트 | MockPaymentGateway + @Profile("test") |
| PG 연동 테스트 | KICC 개발서버 (`testpgapi.easypay.co.kr`) + 테스트 상점ID |
| E2E 테스트 | 부킹엔진 → 결제 → PMS 확인 → 취소 전체 플로우 |

---

## 구현 우선순위 요약

| 순서 | Phase | 핵심 결과물 | 선행 조건 |
|------|-------|-----------|----------|
| 1 | Phase 1: PG 인프라 | KiccApiClient, KiccPaymentGateway | KICC 테스트 상점ID |
| 2 | Phase 2: 부킹 결제 | 프론트 결제 → PMS 확인 | Phase 1 |
| 3 | Phase 3: 빌키 관리 | 카드 등록, 관리자 결제 | Phase 1 |
| 4 | Phase 4: 취소/환불 | 전체/부분 취소, 웹훅 | Phase 2 |
| 5 | Phase 5: Folio | 비용 원장, 정산 | Phase 2, 3 |
| 6 | Phase 6: Night Audit | 일일 마감, 리포트 | Phase 5 |

> **Phase 1~4를 먼저 완료하면 결제의 기본 기능이 동작합니다.**
> Phase 5~6은 운영 고도화로, 체크인/아웃 흐름과 밀접하게 연동됩니다.

---

## 11. 운영 배포 필수 체크리스트 (CRITICAL)

> **이 섹션은 운영 환경 배포 전 반드시 확인해야 합니다.**
> 누락 시 결제 장애 또는 보안 사고가 발생할 수 있습니다.

### 환경변수 (필수 설정)

| 환경변수 | 용도 | 현재 상태 | 비고 |
|---------|------|----------|------|
| `KICC_SECRET_KEY` | KICC HMAC 시크릿키 | **미설정** (더미값 사용 중) | KICC 가맹점 관리자에서 발급 |

**현재 로컬 설정** (`application-local.yml`):
```yaml
kicc:
  mall-id: T5102001                    # ← 운영: 실제 가맹점 ID로 교체
  secret-key: ${KICC_SECRET_KEY:kicc-test-secret-key-for-local-dev}  # ← 운영: 반드시 환경변수 설정
  api-domain: https://testpgapi.easypay.co.kr   # ← 운영: https://pgapi.easypay.co.kr
  return-base-url: http://localhost:8080         # ← 운영: 실제 도메인 (HTTPS)
```

### 운영 application-prod.yml 작성 필요

```yaml
kicc:
  mall-id: ${KICC_MALL_ID}            # 운영 가맹점 ID
  secret-key: ${KICC_SECRET_KEY}       # 운영 시크릿키 (KICC 가맹점 관리자에서 발급)
  api-domain: https://pgapi.easypay.co.kr
  return-base-url: https://실제도메인.com
  timeout-seconds: 30
  billing-cert-type: "0"
```

### 배포 전 체크리스트

- [ ] **KICC 가맹점 계약 완료** — 운영 상점ID(mallId) + 시크릿키(secretKey) 발급
- [ ] **환경변수 설정** — `KICC_MALL_ID`, `KICC_SECRET_KEY` 서버에 설정
- [ ] **API 도메인 변경** — `testpgapi` → `pgapi.easypay.co.kr`
- [ ] **returnUrl 도메인 변경** — `localhost:8080` → 운영 HTTPS 도메인
- [ ] **방화벽 설정** — 아웃바운드 443 (KICC API), 인바운드 웹훅 IP 허용
  - 운영: `203.233.72.150`, `203.233.72.151`, `61.33.211.180`
- [ ] **HMAC 검증 동작 확인** — 테스트 환경은 `mallId.startsWith("T")` 조건으로 HMAC 실패를 경고만 출력. 운영에서는 **자동으로 강제 검증** 활성화됨 (코드 수정 불필요)
- [ ] **HTTPS 필수** — KICC API는 TLS 1.2 이상만 허용
- [ ] **결제 E2E 테스트** — 운영 상점ID로 실결제 → 취소 테스트 수행

### 현재 개발 환경 임시 조치 사항

| 항목 | 현재 상태 | 운영 전환 시 |
|------|----------|-------------|
| KICC Secret Key | 더미값 (`kicc-test-secret-key-for-local-dev`) | 환경변수 `KICC_SECRET_KEY` 필수 설정 |
| HMAC 검증 | 테스트 상점ID(`T*`)에서 실패 시 경고만 출력 | 운영 상점ID에서는 자동으로 강제 검증 (코드 수정 불필요) |
| PG 도메인 | `testpgapi.easypay.co.kr` | `pgapi.easypay.co.kr` |
| Return URL | `http://localhost:8080` | `https://운영도메인` |
