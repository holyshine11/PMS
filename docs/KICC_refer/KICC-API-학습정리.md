# KICC 이지페이 온라인 결제 API 학습 정리

> 작성일: 2026-03-25
> 목적: Hola PMS KICC PG 결제 연동을 위한 API 학습 자료
> 참고: KICC Online Payment Developer Doc.md, KICC Developer center.md

---

## 1. 환경 구성

### API 도메인
| 구분 | 도메인 |
|------|--------|
| **개발(테스트)** | `https://testpgapi.easypay.co.kr` |
| **운영** | `https://pgapi.easypay.co.kr` |

- 반드시 **HTTPS** 사용 (TLS 1.2 이상)
- 테스트 상점ID: `T5102001`

### 방화벽 설정
- **아웃바운드**: TCP 443 → KICC API 도메인
- **인바운드** (웹훅 수신 시):
  - 운영: `203.233.72.150`, `203.233.72.151`, `61.33.211.180`
  - 개발: `61.33.205.151`

---

## 2. 공통 사항

### 응답 형식
```json
{
    "resCd": "0000",    // "0000" = 성공, 그 외 = 실패
    "resMsg": "성공"
}
```

### API 멱등성 (Idempotency)
- `shopTransactionId` 필드에 **UUID** 전달 (Java: `UUID.randomUUID()`)
- 동일 키 재요청 시 → 중복 오류 반환
- 유효시간: 요청 당일 자정까지

### 메시지 인증값 (msgAuthValue)
- **HmacSHA256** 해시
- 승인 응답: `pgCno + "|" + amount + "|" + transactionDate`
- 취소/환불 요청: `pgCno + "|" + shopTransactionId`
- KICC 시크릿키로 HMAC 생성

### 특수문자 제한
- **금지**: `'`, `"`, `<`, `>`, `\`, `;`, `|`, 개행문자
- **주문번호**: 영문/숫자/`-`/`_` (최대 40자)
- **상품명**: 한글/영문/숫자/공백/기본부호 (최대 50Byte)

### 하위 호환성
- Jackson ObjectMapper 설정 필수:
```java
objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
```

---

## 3. 일반결제 (General Payment)

### 플로우
```
고객 → 상점: 주문하기
상점 → KICC: 거래등록 (POST /api/ep9/trades/webpay)
KICC → 상점: authPageUrl 응답
상점 → KICC: 결제창 호출 (GET {authPageUrl})
고객 → KICC: 결제수단 선택 & 인증
KICC → 상점: returnUrl로 이동 (authorizationId 전달)
상점 → KICC: 결제승인 (POST /api/ep9/trades/approval)
KICC → 상점: 최종 결제 결과
```

### 3.1 거래등록 (통합형)
```
POST https://{도메인}/api/ep9/trades/webpay
Content-Type: application/json; charset=utf-8
```

**필수 요청 필드:**
| 필드 | 타입 | 설명 |
|------|------|------|
| `mallId` | String(8) | KICC 상점ID |
| `shopOrderNo` | String(40) | 상점 주문번호 (Unique) |
| `amount` | Number | 결제 금액 |
| `payMethodTypeCode` | String(2) | 결제수단 ("00"=미지정, "11"=신용카드) |
| `currency` | String(2) | 통화 ("00"=원화) |
| `returnUrl` | String(256) | 인증완료 후 이동 URL |
| `deviceTypeCode` | String(20) | "pc" / "mobile" |
| `clientTypeCode` | String(2) | "00" (통합형 고정) |
| `orderInfo.goodsName` | String(50) | 상품명 |

**선택 필드:**
- `orderInfo.customerInfo`: 고객정보 (ID, 이름, 이메일, 연락처, 주소)
- `payMethodInfo.cardMethodInfo`: 카드 설정 (할부, 무이자, 카드사 제한 등)
- `taxInfo`: 복합과세 (과세/비과세/부가세 금액)
- `shopValueInfo`: 상점 커스텀 필드 (value1~7)

**요청 예시:**
```json
{
    "mallId": "T5102001",
    "shopOrderNo": "HOLA-RSV-20260325-001",
    "amount": 150000,
    "payMethodTypeCode": "11",
    "currency": "00",
    "clientTypeCode": "00",
    "returnUrl": "https://호텔도메인/api/v1/payment/kicc/return",
    "deviceTypeCode": "pc",
    "orderInfo": {
        "goodsName": "디럭스 더블룸 1박",
        "customerInfo": {
            "customerName": "홍길동",
            "customerContactNo": "01012345678"
        }
    }
}
```

**응답:**
```json
{
    "resCd": "0000",
    "resMsg": "정상처리",
    "authPageUrl": "{결제창 호출 URL}"
}
```

### 3.2 결제창 호출
```
GET {authPageUrl}
```
- 최소 결제창 사이즈: PC 720×680, Mobile 500×850
- 인증완료 → `returnUrl`로 POST 전송

**returnUrl 응답:**
| 필드 | 설명 |
|------|------|
| `resCd` | 결과코드 ("0000"=성공) |
| `shopOrderNo` | 상점 주문번호 |
| `authorizationId` | **인증 거래번호** (승인 요청 시 필수) |

### 3.3 결제승인
```
POST https://{도메인}/api/ep9/trades/approval
Content-Type: application/json; charset=utf-8
```
⚠️ **timeout 30초 필수 설정**

**필수 요청 필드:**
| 필드 | 타입 | 설명 |
|------|------|------|
| `mallId` | String(8) | 상점ID |
| `shopTransactionId` | String(60) | 멱등성 키 (UUID) |
| `authorizationId` | String(60) | 결제창에서 받은 인증 거래번호 |
| `shopOrderNo` | String(40) | 상점 주문번호 |
| `approvalReqDate` | String(8) | yyyyMMdd |

**응답 주요 필드:**
| 필드 | 설명 |
|------|------|
| `resCd` | "0000"=성공 |
| `pgCno` | **PG 거래고유번호** (취소/환불 시 필수) |
| `amount` | 결제금액 (**요청금액과 반드시 비교**) |
| `transactionDate` | 거래일시 (yyyyMMddHHmmss) |
| `statusCode` | "TS03"=매입요청 |
| `msgAuthValue` | 무결성 검증용 HMAC |
| `paymentInfo.approvalNo` | 승인번호 |
| `paymentInfo.cardInfo` | 카드번호(마스킹), 발급사, 매입사, 할부, 카드종류 |

**⚠️ 필수 검증 사항:**
1. 승인 응답의 `amount`와 요청 금액 일치 확인 → 불일치 시 즉시 취소
2. DB 처리 실패 시 즉시 취소
3. 네트워크 오류 시 → `retrieveTransaction`으로 상태 조회 후 처리

---

## 4. 정기결제 / 빌키 (Billing Key)

### 플로우
```
고객 → 상점: 카드 등록 요청
상점 → KICC: 거래등록 (amount=0, payMethodTypeCode="81")
KICC → 상점: authPageUrl (빌키 등록창 URL)
고객 → KICC: 카드정보 입력 & 인증
KICC → 상점: returnUrl로 이동 (authorizationId)
상점 → KICC: 빌키 발급 요청 (POST /api/ep9/trades/approval)
KICC → 상점: 빌키 응답 (paymentInfo.cardInfo.cardNo = 빌키)
--- 이후 관리자가 필요 시 ---
상점 → KICC: 빌키 결제 승인 (POST /api/trades/approval/batch)
```

### 4.1 빌키 등록창 요청
```
POST https://{도메인}/api/ep9/trades/webpay
```
- `amount`: **0** 고정
- `payMethodTypeCode`: **"81"** 고정
- `payMethodInfo.billKeyMethodInfo.certType`:
  - `"0"`: 카드번호 + 유효기간 + 생년월일 + 비밀번호 (가장 안전)
  - `"1"`: 카드번호 + 유효기간
  - `"2"`: 카드번호 + 유효기간 + 생년월일

**요청 예시:**
```json
{
    "mallId": "T5102001",
    "shopOrderNo": "HOLA-BK-20260325-001",
    "amount": 0,
    "payMethodTypeCode": "81",
    "currency": "00",
    "clientTypeCode": "00",
    "returnUrl": "https://호텔도메인/api/v1/payment/kicc/billkey/return",
    "deviceTypeCode": "pc",
    "orderInfo": {
        "goodsName": "카드 등록"
    },
    "payMethodInfo": {
        "billKeyMethodInfo": {
            "certType": "0"
        }
    }
}
```

### 4.2 빌키 발급
```
POST https://{도메인}/api/ep9/trades/approval
```
- 일반결제 승인과 동일한 엔드포인트
- 응답의 `paymentInfo.cardInfo.cardNo` = **빌키** (반드시 안전하게 저장)
- `paymentInfo.cardInfo.cardMaskNo` = 마스킹된 카드번호 (UI 표시용)

**⚠️ 호텔 PMS 적용 포인트:**
- 빌키는 AES-256 암호화 저장 (기존 `rsv_reservation_deposit` 패턴 활용)
- 카드 마스킹 번호와 발급사 정보를 함께 저장 (관리자 UI 표시용)

### 4.3 빌키 결제 승인 (서버 to 서버)
```
POST https://{도메인}/api/trades/approval/batch
```

**필수 요청 필드:**
| 필드 | 타입 | 설명 |
|------|------|------|
| `mallId` | String(8) | 상점ID |
| `shopTransactionId` | String(60) | 멱등성 키 |
| `shopOrderNo` | String(40) | 주문번호 |
| `approvalReqDate` | String(8) | yyyyMMdd |
| `amount` | Number | 결제금액 |
| `currency` | String(2) | "00" |
| `orderInfo.goodsName` | String(50) | 상품명 |
| `payMethodInfo.billKeyMethodInfo.batchKey` | String(60) | 발급받은 빌키 |
| `payMethodInfo.cardMethodInfo.installmentMonth` | Number | 할부개월 (0=일시불) |

**요청 예시:**
```json
{
    "mallId": "T5102001",
    "shopOrderNo": "HOLA-PAY-20260325-001",
    "shopTransactionId": "550e8400-e29b-41d4-a716-446655440000",
    "approvalReqDate": "20260325",
    "amount": 150000,
    "currency": "00",
    "orderInfo": {
        "goodsName": "디럭스 더블룸 숙박료"
    },
    "payMethodInfo": {
        "billKeyMethodInfo": {
            "batchKey": "{저장된 빌키}"
        },
        "cardMethodInfo": {
            "installmentMonth": 0
        }
    }
}
```

### 4.4 빌키 삭제
```
POST https://{도메인}/api/trades/removeBatchKey
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `mallId` | String(8) | 상점ID |
| `shopTransactionId` | String(60) | 멱등성 키 |
| `batchKey` | String(40) | 삭제할 빌키 |
| `removeReqDate` | String(8) | yyyyMMdd |

---

## 5. 결제 취소 (Cancel)

### 엔드포인트
```
POST https://{도메인}/api/trades/revise
```

### 변경 구분 코드 (reviseTypeCode)
| 코드 | 서비스명 | 설명 |
|------|---------|------|
| `40` | 전체취소 | 승인거래 전체 취소 |
| `32` | 부분취소 | 신용카드(간편결제 포함) 부분취소 |
| `33` | 부분취소 | 계좌이체/휴대폰 결제 부분취소 |

**필수 요청 필드:**
| 필드 | 타입 | 설명 |
|------|------|------|
| `mallId` | String(8) | 상점ID |
| `shopTransactionId` | String(60) | 멱등성 키 |
| `pgCno` | String(20) | 원거래 PG 거래고유번호 |
| `reviseTypeCode` | String(2) | 변경구분 코드 |
| `cancelReqDate` | String(8) | yyyyMMdd |
| `msgAuthValue` | String(200) | HMAC: `pgCno + "\|" + shopTransactionId` |

**부분취소 시 추가:**
| 필드 | 설명 |
|------|------|
| `amount` | 취소할 금액 |
| `remainAmount` | 취소 후 남을 잔액 (검증용) |

**요청 예시 (전체취소):**
```json
{
    "mallId": "T5102001",
    "shopTransactionId": "cancel-uuid-001",
    "pgCno": "2026032500001234",
    "reviseTypeCode": "40",
    "cancelReqDate": "20260325",
    "msgAuthValue": "{HmacSHA256 해시값}",
    "reviseMessage": "고객 요청 취소"
}
```

**응답 주요 필드:**
| 필드 | 설명 |
|------|------|
| `oriPgCno` | 원거래 PG번호 |
| `cancelPgCno` | 취소 PG번호 |
| `statusCode` | "TS02"=승인취소 |
| `cancelAmount` | 취소금액 |
| `remainAmount` | 잔여금액 |
| `reviseInfo.approvalNo` | 취소 승인번호 |

---

## 6. 결제 환불 (Refund)

### 엔드포인트
```
POST https://{도메인}/api/trades/revise  (취소와 동일)
```

### 환불 유형
| 유형 | reviseTypeCode | reviseSubTypeCode | 설명 |
|------|---------------|-------------------|------|
| 지결 전체환불 | `60` | `RF01` | D+1 영업일 환불 |
| 지결 부분환불 | `62` | `RF01` | D+1 영업일 부분환불 |
| 실시간 전체환불 (계좌인증) | `63` | `10` | 즉시 환불 (KICC 계좌인증) |
| 실시간 부분환불 (계좌인증) | `63` | `11` | 즉시 부분환불 |
| 실시간 전체환불 (계좌미인증) | `63` | `20` | 상점 계좌인증 후 환불 |
| 실시간 부분환불 (계좌미인증) | `63` | `21` | 상점 계좌인증 후 부분환불 |

**추가 필수 필드 (`refundInfo`):**
| 필드 | 타입 | 설명 |
|------|------|------|
| `refundBankCode` | String(3) | 환불 은행코드 |
| `refundAccountNo` | String(14) | 환불 계좌번호 |
| `refundDepositName` | String(50) | 예금주명 |

**⚠️ 호텔 PMS 적용 포인트:**
- 카드 결제 취소는 `reviseTypeCode: "40"/"32"` 사용 (환불이 아닌 취소)
- 가상계좌/계좌이체 결제만 `refundInfo` 포함한 환불 사용
- 호텔은 주로 카드 결제 → **취소 API** 위주로 사용

---

## 7. 거래상태 조회 (Query)

```
POST https://{도메인}/api/trades/retrieveTransaction
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `mallId` | String(8) | 상점ID |
| `shopTransactionId` | String(60) | 원거래 멱등성 키 |
| `transactionDate` | String(8) | yyyyMMdd |

- 응답: 승인/취소 응답과 동일 형식
- 전일~당일 거래만 지원
- msgAuthValue는 제공되지 않음

---

## 8. 웹훅 (노티) 연동

### 설정 방법
- KICC 가맹점 관리자 → 노티등록 메뉴에서 URL 등록
- 결제완료/입금완료/환불완료 시 노티 수신

### 웹훅 응답 주요 필드
```json
{
    "pgCno": "{PG거래번호}",
    "shopOrderNo": "{주문번호}",
    "amount": 150000,
    "statusCode": "TS03",
    "transactionDate": "20260325140000"
}
```

---

## 9. 주요 코드 참조

### 결제수단 코드 (payMethodTypeCode)
| 코드 | 결제수단 |
|------|---------|
| `00` | 미지정 |
| `11` | 신용카드 |
| `21` | 계좌이체 |
| `22` | 가상계좌 |
| `31` | 휴대폰 |
| `50` | 선불카드 |
| `60` | 간편결제 |
| `81` | 정기결제(빌키) |

### 거래상태 코드 (statusCode)
| 코드 | 상태 |
|------|------|
| `TS01` | 미승인 |
| `TS02` | 승인취소 |
| `TS03` | 매입요청 |

---

## 10. Hola PMS 적용 시 핵심 체크리스트

### 기존 인프라 활용
- [x] `PaymentGateway` 인터페이스 존재 → `KiccPaymentGateway` 구현체 추가
- [x] `ReservationPayment` 엔티티 → `pgCno`, `pgTransactionId` 컬럼 추가 필요
- [x] `PaymentTransaction` 엔티티 → KICC 응답 필드 매핑 확장
- [x] `rsv_reservation_deposit` 스키마 → 빌키 저장용 확장

### 보안 필수사항
- [ ] KICC mallId, secretKey → **환경변수** (절대 하드코딩 금지)
- [ ] 빌키 → **AES-256 암호화** 저장
- [ ] msgAuthValue → HmacSHA256 **응답 무결성 검증** 필수
- [ ] 결제 금액 → 요청/응답 **금액 비교 검증** 필수
- [ ] PG 통신 → **timeout 30초** 설정

### 호텔 업계 특수 고려사항
1. **예치금(Deposit)**: 체크인 시 보증금으로 빌키 사전 등록 → No-Show/미니바/룸서비스 등 후결제
2. **부분취소**: 객실비+부대시설 중 일부만 취소 → `reviseTypeCode: "32"` 활용
3. **체크아웃 일괄정산**: Folio 기반 → 여러 charge를 합산 후 한 건 결제
4. **OTA 연동**: 채널별 결제/정산 분리 관리
5. **법인카드 관리**: 기업 고객 → 빌키 등록 후 반복 결제
