# 부킹엔진 심층 Gap 분석 보고서

> **분석 대상**: 산화정보통신 부킹엔진 API 연동 정의서 (2022.03) + 조선 Opera Configuration (GJJ Base)
> **비교 대상**: Hola PMS 부킹엔진 현재 구현
> **분석일**: 2026-03-14

---

## 요약

| 구분 | 산화정보통신 API (17 endpoints) | Opera Configuration (GJJ) | Hola PMS 현재 |
|------|:---:|:---:|:---:|
| 예약 CRUD | ✅ | - | ✅ |
| 가용성 조회 | ✅ | - | ✅ |
| 일자별 요금 (NET/TAX/SVC) | ✅ | ✅ | ✅ |
| 취소 정책 (다단계) | ✅ | - | ❌ |
| 노쇼 정책 | ✅ | - | ❌ |
| Deposit Rule | - | ✅ | ❌ |
| 게스트 셀프서비스 | ✅ | - | ❌ |
| 카드 BIN 검증 | ✅ | - | ❌ |
| Rate 3단계 체계 | - | ✅ | ⚠️ |
| 패키지 코드 | - | ✅ | ❌ |
| Source 코드 체계 | - | ✅ | ❌ |
| 채널 전환 관리 | - | ✅ | ⚠️ |
| 부가 서비스/옵션 재고 | ✅ | ✅ | ⚠️ |
| 결제 요약 (7항목) | ✅ | - | ⚠️ |
| 다국어/다통화 | ✅ | ✅ | ❌ |

---

## 1. 취소 정책 (Cancellation Policy) — 🔴 미구현 (HIGH)

### 산화정보통신 참조
```
cncl_lev_code: 1, 2 (다단계 취소 레벨)
cncl_pot_code: D1 (당일), D2 (2일전) ...
cncl_cost_percent: 0%, 50%, 100%
cncl_appl_time: 적용 시각 (예: 18:00)
```
- 취소 레벨별로 다른 수수료율 적용
- 체크인 N일전/당일 기준 차등 적용
- 시간 기반 마감 (예: 당일 18시 이후 100%)

### Hola PMS 현황
- `CancellationFee` 엔티티 존재 (`daysBefore`, `feeAmount`, `feeType`)
- **BUT: 부킹엔진에서 전혀 활용 안 됨**
- 취소 API 엔드포인트 없음
- 취소 수수료 계산 로직 없음

### 권장 구현
1. `CancellationPolicy` 서비스 생성 — 체크인까지 남은 일수 기반 수수료 계산
2. `POST /api/v1/booking/reservations/{confirmationNo}/cancel` 엔드포인트
3. 취소 전 수수료 미리보기 API
4. `PaymentTransaction`에 REFUND 타입 추가

---

## 2. 노쇼 정책 (No-Show Policy) — 🔴 미구현 (HIGH)

### 산화정보통신 참조
```
noshow_policy_useyn: Y/N
noshow_cost: 노쇼 비용
noshow_appl_std_rate_code: 적용 기준 레이트코드
```
- 레이트코드별 노쇼 비용 설정
- 노쇼 시 1박 요금 또는 정액 부과

### Hola PMS 현황
- `reservationStatus = NO_SHOW` 상태값만 존재
- 노쇼 정책 엔티티/로직 전무

### 권장 구현
1. `NoShowPolicy` 엔티티 (propertyId, feeType, feeAmount, applicableRateCode)
2. 체크인 미도착 시 자동 노쇼 전환 배치
3. 노쇼 요금 자동 계산 및 결제 처리

---

## 3. Deposit Rule — 🔴 미구현 (HIGH)

### Opera 참조
```
Type: Flat / Percent / Night Percentage / Nights
Amount: 금액 또는 비율
Days Before Arrival: 도착 N일 전 보증금 요구
Days After Booking: 예약 N일 후 보증금 요구
```
- 4가지 보증금 유형 (정액/비율/1박%/N박)
- 도착 전/예약 후 기준 이중 타임라인

### Hola PMS 현황
- `ReservationDeposit` 엔티티 존재 (카드정보 암호화 저장)
- **정책 엔진 없음** — 언제, 얼마를 보증금으로 요구하는지 규칙 없음

### 권장 구현
1. `DepositRule` 엔티티 (type, amount, daysBeforeArrival, daysAfterBooking)
2. 레이트코드별 Deposit Rule 연결
3. 보증금 자동 청구/환불 로직

---

## 4. Rate Code 3단계 체계 — 🟡 부분 구현 (MEDIUM)

### Opera 참조
```
Rate Class → Rate Category → Rate Code
예: CLASS=Corporate → CATEGORY=Samsung → CODE=SAM2026
```
- Rate Category에 복수 Rate Code 연결
- Rate Code별 속성:
  - **Min/Max Stay Through** (최소/최대 연박)
  - **Min/Max Advance Booking** (최소/최대 사전예약일)
  - **Channel Allowed** (채널 허용 여부)
  - **Commission %** (수수료율)
  - **적용 Room Types** (특정 객실타입만 적용)

### Hola PMS 현황
- `rateCategory` 문자열 필드 존재 (Rate Class 없음)
- `minStayDays`, `maxStayDays` 존재 ✅
- **Min/Max Advance Booking 없음** ❌
- **Channel Allowed 없음** ❌
- **Commission % 없음** ❌
- **Rate Code-Room Type 매핑 없음** ❌

### 권장 구현
1. `RateCode`에 필드 추가:
   - `minAdvanceBookingDays`, `maxAdvanceBookingDays`
   - `channelAllowed` (boolean)
   - `commissionPercent` (BigDecimal)
2. `RateCodeRoomType` 매핑 테이블 생성
3. Rate Class 엔티티 (선택적 — 현 단계에서는 `rateCategory` 문자열로 충분)

---

## 5. 패키지 코드 (Package Code) — 🔴 미구현 (MEDIUM)

### Opera 참조
```
Package Code: 코드, 설명, Forecast Group
Transaction: Code + Coverage
Tax Inclusive: Y/N
Attributes: Included in Rate / Add Rate Separate / Combined Line
Posting Rhythm: 매일/체크인/체크아웃 등
Formula + Calculation Rule: Per Room
Item Inventory: 재고 연동
Season별 가격 설정
```
- 숙박+서비스 번들 상품
- 포스팅 리듬 (일자별/체크인시/체크아웃시 과금)
- 세금 포함/별도 옵션
- 재고 관리 연동

### Hola PMS 현황
- `PromotionCode.promotionType = "PACKAGE"` 문자열만 존재
- 실제 패키지 엔티티/로직 없음

### 권장 구현 (Phase 3)
1. `PackageCode` 엔티티 (code, name, transactionCode, taxInclusive, postingRhythm)
2. `PackageDetail` 시즌별 가격 테이블
3. Rate Code-Package 연결
4. 패키지 아이템 재고 관리

---

## 6. Source 코드 체계 — 🔴 미구현 (MEDIUM)

### Opera 참조
```
Source Group → Source Code
예: CRS → P(Phone), OTA → OTAO(OTA Online)
HTL → WLK(Walk-in), GDS → GDS
```
- 예약 출처(Source)를 Market Code와 별개로 관리
- Revenue 분석의 핵심 차원

### Hola PMS 현황
- `MarketCode` 존재 (시장 세그먼트)
- `ReservationChannel` 존재 (채널 타입)
- **Source Code 별도 관리 없음** — Channel과 혼용

### 권장 구현
1. `SourceCode` 엔티티 (sourceGroup, sourceCode, description)
2. `MasterReservation.sourceCodeId` FK 추가
3. Rate Code별 Source 연결

---

## 7. 게스트 셀프서비스 (My Page) — 🔴 미구현 (MEDIUM)

### 산화정보통신 참조
```
selectReservationList: 내 예약 목록 (type: Book/Old/Cancel)
selectReservationDetail: 예약 상세
updateReservation: 투숙객 정보 자가 수정
cancelReservation: 자가 취소 (수수료 자동 계산)
```
- 회원/비회원 예약 조회
- 예약 목록 타입별 분류 (예정/과거/취소)
- 투숙객 정보만 자가 수정 가능 (날짜/객실 변경은 호텔측)
- 취소 시 수수료 미리 보여주고 최종 확인

### Hola PMS 현황
- Confirmation 페이지에서 이메일 인증 후 단건 조회만 가능
- 예약 목록 조회 없음
- 자가 수정/취소 없음

### 권장 구현
1. `GET /api/v1/booking/my-reservations?email=&phone=` — 예약 목록
2. `PUT /api/v1/booking/reservations/{confirmationNo}/guest` — 투숙객 정보 수정
3. `POST /api/v1/booking/reservations/{confirmationNo}/cancel` — 자가 취소
4. `GET /api/v1/booking/reservations/{confirmationNo}/cancel-fee` — 취소 수수료 미리보기
5. 프론트엔드 My Page 구현

---

## 8. 카드 BIN 검증 — 🟡 미구현 (LOW)

### 산화정보통신 참조
```
creditCardCheck API
입력: credit_card_no (카드번호)
출력: credit_card_type (VI/MS/AX/JC/UN/CD)
```
- 카드 BIN(앞 6자리)으로 카드사 판별
- VI=Visa, MS=Master, AX=Amex, JC=JCB, UN=UnionPay, CD=Domestic

### Hola PMS 현황
- 미구현 (MockPaymentGateway만 존재)

### 권장 구현 (실결제 연동 시)
- PG사 연동 시 자동 지원됨 (KICC, KG이니시스 등)
- 별도 BIN API보다는 PG사 SDK 활용 권장

---

## 9. 결제 요약 7항목 — 🟡 부분 구현 (MEDIUM)

### 산화정보통신 참조
```
payment_info:
  cancel: 취소금액
  total: 총금액
  rate: 객실금액
  commission: 수수료
  payment: 결제금액
  no_show: 노쇼금액
  refund: 환불금액
```

### Hola PMS 현황
- `ReservationPayment`: totalRoomAmount, totalServiceAmount, totalServiceChargeAmount, totalAdjustmentAmount, totalEarlyLateFee, grandTotal, totalPaidAmount
- **누락**: commission(수수료), cancel(취소금액), no_show(노쇼금액), refund(환불금액)

### 권장 구현
`ReservationPayment`에 필드 추가:
- `commissionAmount` (채널 수수료)
- `cancelFeeAmount` (취소 수수료)
- `noShowFeeAmount` (노쇼 수수료)
- `refundAmount` (환불 금액)

---

## 10. Item Inventory (부가서비스 재고) — 🟡 부분 구현 (LOW)

### Opera 참조
```
Item Name, Item Code, Department
Quantity In Stock (재고 수량)
Default Quantity (기본 제공 수량)
Available From / To (제공 기간)
Outside of Stay Days (투숙 외 일자 제공 여부)
```
- 조식, 액티비티 등 부가서비스의 재고 관리
- 일자별 가용 수량 추적

### Hola PMS 현황
- `FreeServiceOption`, `PaidServiceOption` 엔티티 존재
- `ReservationServiceItem`으로 예약에 서비스 연결
- **재고 수량 관리 없음** ❌
- **가용 기간 관리 없음** ❌

### 권장 구현
1. `PaidServiceOption`에 `quantityInStock`, `availableFrom`, `availableTo` 추가
2. 일자별 재고 차감 로직

---

## 11. 회원/비회원 예약 구분 — 🟡 미구현 (LOW)

### 산화정보통신 참조
```
join_yn: 회원가입 여부
guest_member_yn: 회원 여부
membership_info: 회원 등급/번호
```
- 회원 전용 요금 적용
- 비회원도 예약 가능하되 회원 혜택 제외

### Hola PMS 현황
- 현재 부킹엔진은 비회원 전용 (로그인 없이 예약)
- 회원 시스템과 연동 없음

### 권장 구현 (Phase 3)
- 게스트 회원 시스템 구축 시 연동
- 현 단계에서는 비회원 예약으로 충분

---

## 12. 다국어/다통화 지원 — 🟡 미구현 (LOW, Phase 3)

### 산화정보통신 + Opera 공통
- 영문/한글 이름 병기
- 다통화 결제 지원
- 다국어 UI

### Hola PMS 현황
- 엔티티에 `nameKo`, `nameEn` 이중 필드 존재 ✅
- `currency = "KRW"` 하드코딩
- UI 한국어 전용

### 권장 구현 (Phase 3)
- 해외 고객 대상 서비스 확장 시 구현
- 현재 국내 3~5성급 타겟이므로 후순위

---

## 구현 우선순위 매트릭스

| 우선순위 | 기능 | 난이도 | 업무 영향 | 추천 Phase |
|:---:|------|:---:|:---:|:---:|
| **P0** | 취소 정책 엔진 + 자가 취소 | 중 | 높음 | Phase 1 |
| **P0** | 노쇼 정책 | 중 | 높음 | Phase 1 |
| **P1** | Deposit Rule 엔진 | 중 | 높음 | Phase 1 |
| **P1** | 게스트 셀프서비스 (My Page) | 중 | 높음 | Phase 1 |
| **P1** | Rate Code 확장 (Advance Booking, Channel, Commission) | 하 | 중 | Phase 1 |
| **P1** | 결제 요약 확장 (commission, cancel, refund, noshow) | 하 | 중 | Phase 1 |
| **P2** | Source 코드 체계 | 하 | 중 | Phase 2 |
| **P2** | 부가서비스 재고 관리 | 하 | 낮음 | Phase 2 |
| **P3** | 패키지 코드 | 높 | 중 | Phase 3 |
| **P3** | 카드 BIN 검증 (PG 연동 시) | 낮 | 낮음 | Phase 3 |
| **P3** | 회원 예약 연동 | 중 | 낮음 | Phase 3 |
| **P3** | 다국어/다통화 | 높 | 낮음 | Phase 3 |

---

## 핵심 결론

### 잘 되어 있는 것 ✅
1. **예약 코어 CRUD** — 산화정보통신 17개 API 중 핵심 5개(가용성, 요금, 생성, 조회, 확인) 구현 완료
2. **일자별 요금 분리** (Supply/Tax/ServiceCharge) — 업계 표준과 일치
3. **결제 이력 관리** — PaymentTransaction + PaymentAdjustment
4. **감사 로그** — BookingAuditLog + idempotency 중복방지
5. **가용성 3단계 검증** (L1 객실, L2 재고, L3 오버부킹)
6. **Rate Code 기본 체계** — 판매기간, 최소/최대 연박, 요일별 가격

### 핵심 Gap 🔴
1. **정책 엔진 부재** — 취소/노쇼/보증금 엔티티는 있으나 실행 로직 없음
2. **게스트 자율성 부재** — 예약 후 조회만 가능, 수정/취소 불가
3. **Revenue 관리 부족** — Commission%, Source Code, Channel 제어 없음
4. **결제 완결성** — 환불/취소금/노쇼금 추적 필드 없음

> 현재 Hola PMS는 **"예약 생성 → 확인"까지는 업계 수준**이나,
> **"예약 후 라이프사이클 관리"(취소, 노쇼, 환불, 셀프서비스)가 미흡**합니다.
> Phase 1에서 취소 정책 + 노쇼 정책 + 게스트 셀프서비스를 우선 구현하면
> 실 운영 수준의 부킹엔진이 됩니다.
