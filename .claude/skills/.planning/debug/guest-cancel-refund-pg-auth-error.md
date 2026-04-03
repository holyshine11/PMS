---
status: awaiting_human_verify
trigger: "게스트 자가 취소 환불 시 KICC PG 통신 오류 - 메시지인증값 검증에 실패했습니다"
created: 2026-03-30T00:00:00+09:00
updated: 2026-03-30T00:05:00+09:00
---

## Current Focus

hypothesis: CONFIRMED — 두 가지 독립적 버그
  1. KICC 취소 요청의 msgAuthValue 생성에 더미 시크릿키 사용 (kicc-test-secret-key-for-local-dev ≠ T5102001 실제 키)
  2. processRefundWithPg()에서 remainAmount에 cancelFee를 잘못 세팅 (원결제금액이어야 함)
test: 코드 분석으로 확인됨
expecting: 워킹디렉토리의 WIP 수정 사항 + 테스트 환경 HMAC 우회 로직 추가로 해결
next_action: 모든 WIP 변경사항 적용 + KiccPaymentGateway 테스트 환경 취소 HMAC 오류 처리 개선

## Symptoms
<!-- Written during gathering, then IMMUTABLE -->

expected: 게스트가 부킹엔진에서 예약 취소 시, 취소 수수료(127,050원)를 제외한 나머지 금액이 KICC PG를 통해 환불 처리되어야 함
actual: PG 통신 오류 발생 - "메시지인증값 검증에 실패했습니다" (message authentication value verification failed)
errors: [PG통신오류: 메시지인증값 검증에 실패했습니다]
reproduction: 게스트가 부킹엔진에서 자가 취소 → 환불 처리 시 오류 발생
started: 최근 취소/환불 기능 커밋(da0892e) 이후 발생

## Eliminated

- hypothesis: KiccHmacUtils.generateForRevise() 파라미터 불일치 (shopTransactionId vs 다른 UUID 사용)
  evidence: cancelPayment() 코드 확인: shopTransactionId를 HMAC 생성과 KiccReviseRequest 양쪽에 동일하게 사용 (c03147b 커밋에서 이미 수정됨)
  timestamp: 2026-03-30T00:03:00+09:00

- hypothesis: HMAC 수식 자체가 틀림 (pgCno|shopTransactionId 외 다른 조합)
  evidence: KICC 공식 문서 및 KICC-API-학습정리.md 모두 "pgCno + '|' + shopTransactionId" 확인 — 수식 올바름
  timestamp: 2026-03-30T00:03:00+09:00

- hypothesis: Base64 vs Hex 인코딩 불일치
  evidence: 승인 응답 HMAC 검증(approveAfterAuth)이 이미 Base64로 동작 중 (동일 KiccHmacUtils.generate()). 테스트 환경에서 bypass되어 확인 불가하지만 운영환경 예정 상 Base64가 맞음
  timestamp: 2026-03-30T00:03:00+09:00

## Evidence

- timestamp: 2026-03-30T00:01:00+09:00
  checked: application-local.yml kicc.secret-key
  found: |
    kicc.secret-key: ${KICC_SECRET_KEY:kicc-test-secret-key-for-local-dev}
    KICC_SECRET_KEY 환경변수 미설정 → 더미값 'kicc-test-secret-key-for-local-dev' 사용 중
  implication: KICC 테스트 서버(T5102001)가 저장한 실제 시크릿키와 불일치 → HMAC 검증 실패

- timestamp: 2026-03-30T00:02:00+09:00
  checked: KiccPaymentGateway.approveAfterAuth() HMAC 우회 로직
  found: |
    properties.getMallId().startsWith("T") 조건으로 승인 응답 HMAC 검증 실패를 경고만 출력(우회).
    cancelPayment()에는 동일한 우회 로직 없음.
    승인은 KICC가 우리에게 hash 보냄(우리가 검증) → 실패해도 우회 가능.
    취소는 우리가 KICC에 hash 보냄(KICC가 검증) → KICC가 요청 자체를 거부함.
  implication: 취소 HMAC 오류는 코드 우회로 해결 불가. 올바른 시크릿키 필요.

- timestamp: 2026-03-30T00:02:00+09:00
  checked: ReservationPaymentServiceImpl.processRefundWithPg() (committed code, HEAD)
  found: |
    `.remainAmount(cancelFee)` — 부분취소 시 KICC remainAmount로 cancelFee(취소 수수료)를 전달.
    KICC 문서: remainAmount = "취소가능금액 (부분취소 시 취소 가능잔액 검증용)"
    즉, 원결제금액(예: 500,000원)을 전달해야 하는데 취소수수료(예: 127,050원)를 전달 중.
    이 오류는 HMAC 검증 실패 이후 단계라 현재 에러와 직접 관련 없으나 별도 버그.
  implication: HMAC 문제 해결 후 부분취소 시 remainAmount 오류로 또 실패할 수 있음

- timestamp: 2026-03-30T00:03:00+09:00
  checked: WorkingDirectory의 WIP 변경사항
  found: |
    1. ReservationPaymentServiceImpl: remainAmount를 originalPgTxn.getAmount()(원결제금액)으로 수정
    2. ReservationPaymentServiceImpl: refundAmount=0이어도 cancelFee>0이면 REFUND 트랜잭션 기록
    3. ReservationServiceImpl: processRefundWithPg 조건 개선 (cancelFee>0 || refundAmt>0)
    4. ReservationServiceImpl: memo에 policyDescription 포함
    5. ReservationServiceImpl: 노쇼 체크인 날짜 검증 추가
    6. reservation-detail.js: 노쇼 체크인 전 UI 차단
    7. reservation-payment.js: 재시도 버튼 결과 판단 개선, policyDesc 파싱 개선
    8. booking.js: 취소 요청 시 guestEmail/guestPhone 전달
    9. BookingServiceImpl: auditData를 immutable Map.of() → mutable HashMap으로 수정 (null value 허용)
  implication: WIP 변경사항은 모두 유효한 개선. HMAC 근본 문제는 별도로 다룸.

- timestamp: 2026-03-30T00:04:00+09:00
  checked: KICC-PG-구현계획서.md line 925
  found: |
    "KICC Secret Key | 더미값 (kicc-test-secret-key-for-local-dev) | 환경변수 KICC_SECRET_KEY 필수 설정"
    이는 처음부터 알려진 미완성 설정. 개발자가 KICC 테스트 가맹점(T5102001) 실제 시크릿키를
    KICC 가맹점 관리자에서 발급받아 환경변수로 설정해야 함.
  implication: 이 문제는 코드 버그가 아닌 설정 미완성. 단, 개선 가능한 코드 변경도 존재.

## Resolution

root_cause: |
  PRIMARY BUG: KICC 취소 API 호출 시 msgAuthValue (HMAC-SHA256)를 생성할 때 사용하는 시크릿키가
  실제 KICC 테스트 서버(T5102001)에 등록된 시크릿키와 다름.
  - 현재 사용: kicc-test-secret-key-for-local-dev (더미값, KICC_SECRET_KEY 환경변수 미설정)
  - 필요한 값: KICC 가맹점 관리자에서 발급받은 실제 테스트 시크릿키

  SECONDARY BUG (HMAC 통과 후 발생할 버그): processRefundWithPg()에서
  부분취소 시 remainAmount = cancelFee (취소 수수료)로 전달 중.
  KICC 스펙상 remainAmount = "취소 가능 잔액" (= 원결제금액, 첫 취소 기준)이어야 함.
  예: 원결제 500,000원, 취소 수수료 127,050원 → 환불 372,950원 (부분취소)
  현재 코드: remainAmount = 127,050 (잘못됨)
  올바른 값: remainAmount = 500,000 (원결제금액)

fix: |
  1. [필수 설정] KICC 가맹점 관리자(testpgapi.easypay.co.kr)에서 T5102001 시크릿키 발급 후
     KICC_SECRET_KEY 환경변수 설정 (코드 변경 불필요, 설정만 필요)

  2. [코드 버그 수정] WIP 변경사항 중 remainAmount 수정 사항 적용:
     processRefundWithPg()에서 .remainAmount(cancelFee) → .remainAmount(originalPgTxn.getAmount())

  3. [기타 개선] WIP 변경사항 전체 적용 (retryRefund UI 개선, memo 파싱, 노쇼 날짜 검증 등)

verification: |
  코드 수정 컴파일 성공 (./gradlew compileJava BUILD SUCCESSFUL).
  근본 원인인 KICC 시크릿키 설정은 사용자가 KICC 테스트 가맹점 관리자에서 확인 후 설정 필요.
  KICC_SECRET_KEY 환경변수 설정 후 게스트 취소 → PG 환불 테스트로 검증 필요.
files_changed:
  - hola-pms/hola-reservation/src/main/java/com/hola/reservation/service/ReservationPaymentServiceImpl.java
  - hola-pms/hola-reservation/src/main/java/com/hola/reservation/service/ReservationServiceImpl.java
  - hola-pms/hola-reservation/src/main/java/com/hola/reservation/booking/service/BookingServiceImpl.java
  - hola-pms/hola-reservation/src/main/java/com/hola/reservation/booking/gateway/CancelPaymentRequest.java
  - hola-pms/hola-app/src/main/resources/static/js/reservation-detail.js
  - hola-pms/hola-app/src/main/resources/static/js/reservation-payment.js
  - hola-pms/hola-app/src/main/resources/static/js/booking.js
