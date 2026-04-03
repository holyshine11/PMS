---
status: awaiting_human_verify
trigger: "PMS 관리자에서 예약 취소 처리 시 취소 수수료가 적용되지 않고, 결제정보 상세 페이지에 취소 내역(REFUND 트랜잭션)이 전혀 표시되지 않는 문제"
created: 2026-03-30T00:00:00+09:00
updated: 2026-03-30T00:02:00+09:00
---

## Current Focus

hypothesis: CONFIRMED + FIXED
test: 컴파일 성공 (./gradlew :hola-reservation:compileJava BUILD SUCCESSFUL)
expecting: 취소 수수료>0이거나 환불>0인 모든 케이스에서 REFUND 트랜잭션 생성 + cancelInfoSection 올바르게 표시
next_action: 인간 검증 (실제 취소 시나리오 테스트)

## Symptoms

expected: 관리자가 예약 취소 시 CancellationPolicy에 따른 취소 수수료 계산, payment.updateCancelRefund() 호출, REFUND PaymentTransaction 기록, 결제정보 탭의 cancelInfoSection과 거래이력에 환불 정보 표시
actual: 취소 수수료 적용 없이 처리됨, 결제정보 상세 페이지에 취소/환불 내역 전혀 없음
errors: 에러 메시지 없이 취소 자체는 성공
reproduction: PMS 관리자 → 예약 상세 → 취소 실행
started: 방금 PG 취소/환불 기능 커밋(da0892e) 직후 발견

## Eliminated

- hypothesis: processRefundWithPg() 내부 nextSeq 계산 오류로 save 실패
  evidence: 코드 추적 결과 nextSeq 계산은 정상. existingTxns.isEmpty() → 1 또는 last+1 패턴 올바름
  timestamp: 2026-03-30T00:01:00+09:00

- hypothesis: processRefundWithPg() 메서드 자체 누락/미호출
  evidence: cancel() line 533-536에서 refundAmt > 0 조건부로 호출함. processRefundWithPg()는 존재하고 @Transactional 정상
  timestamp: 2026-03-30T00:01:00+09:00

- hypothesis: PaymentSummaryResponse 매핑 누락 (cancelFeeAmount/refundAmount)
  evidence: ReservationMapper.toPaymentSummaryResponse() line 304-305에서 둘 다 정상 매핑됨
  timestamp: 2026-03-30T00:01:00+09:00

- hypothesis: cancelFee 계산이 항상 0 반환 (정책 미설정)
  evidence: 이것은 정책 미설정 시 올바른 동작임. 버그는 다른 시나리오에 있음
  timestamp: 2026-03-30T00:01:00+09:00

## Evidence

- timestamp: 2026-03-30T00:01:00+09:00
  checked: ReservationServiceImpl.cancel() lines 519-537
  found: |
    `if (payment != null)` 조건으로 전체 블록을 감쌈.
    payment가 null이면 updateCancelRefund()가 아예 호출되지 않아
    DB에 cancelFeeAmount=0, refundAmount=0이 유지됨 (entity @Builder.Default BigDecimal.ZERO).
    실제로 ReservationPayment 레코드 자체가 없는 경우
    (결제 미등록 예약 취소 시) cancelFee가 계산되어도 기록 불가.
  implication: payment==null 케이스에서 취소 수수료 기록 완전 누락

- timestamp: 2026-03-30T00:01:00+09:00
  checked: ReservationServiceImpl.cancel() line 533
  found: |
    `if (refundAmt.compareTo(BigDecimal.ZERO) > 0)` 조건으로만 processRefundWithPg() 호출.
    cancelFee > 0이고 totalPaid == cancelFee이면 refundAmt = 0 → REFUND 트랜잭션 생성 안 됨.
    이 경우 cancelInfoSection의 적용 정책 설명(policyDesc)이 '-'로 표시됨
    (REFUND 트랜잭션의 memo에서 추출하는 로직이 renderCancelInfo에 있음).
  implication: 수수료와 결제액이 동일한 케이스에서 REFUND 트랜잭션 없음 → 정책 설명 표시 불가

- timestamp: 2026-03-30T00:01:00+09:00
  checked: reservation-payment.js renderCancelInfo() lines 646-698
  found: |
    cancelFee <= 0 && refund <= 0이면 section 숨김(line 650-653).
    정책 설명을 REFUND 트랜잭션 memo에서만 추출(line 659-665).
    cancelFeeAmount > 0이고 refundAmt=0인 경우: section은 표시되지만 policyDesc가 '-'.
  implication: refundAmt=0인 경우도 정책 설명을 올바르게 표시하려면 별도 보완 필요

- timestamp: 2026-03-30T00:01:00+09:00
  checked: CancellationPolicyServiceImpl.calculateCancelFee()
  found: |
    정책 미설정 시 CancelFeeResult(ZERO, ZERO, "취소 정책 미설정 - 무료 취소") 반환.
    이 경우 cancelFee=0, refundAmt=totalPaid → processRefundWithPg() 호출 여부는 totalPaid에 달림.
    totalPaid > 0이면 전액 환불 REFUND 트랜잭션 생성됨 → 이 케이스는 정상.
  implication: 정책 미설정+기결제 있는 경우는 올바르게 동작함. 버그는 수수료 있는 케이스에 집중됨

## Resolution

root_cause: |
  Bug 1 (주요): ReservationServiceImpl.cancel()에서 `if (payment != null)` 블록으로
  updateCancelRefund()와 processRefundWithPg()를 감쌈. payment가 null인 경우
  (ReservationPayment 레코드가 없는 예약 취소) cancelFee가 계산되어도 DB 기록이 완전히 생략됨.
  결제 없는 예약에서 취소 수수료가 적용되어야 할 때도 기록이 없어 cancelInfoSection 미표시.

  Bug 2 (보조): `if (refundAmt > 0)` 조건으로만 REFUND 트랜잭션 생성.
  cancelFee == totalPaid인 경우(수수료만큼만 결제) refundAmt = 0이라 REFUND 트랜잭션이 생성되지 않음.
  reservation-payment.js의 renderCancelInfo()가 정책 설명을 REFUND 트랜잭션 memo에서만 추출하므로
  이 케이스에서 "적용 정책" 컬럼이 '-'로 표시됨.

fix: |
  Bug 1: payment가 null인 경우도 cancelFee를 ReservationPayment 신규 생성 후 기록하거나,
  또는 payment==null이어도 로그/이력으로 남길 수 있도록 처리.
  실제로는 결제 없는 예약(totalPaid=0)에서 cancelFee>0이면 CANCEL_FEE_UNPAID로 이미 블록됨.
  따라서 Bug 1의 실제 영향은: payment==null이면 cancelFee==0(무료취소)이어야 취소 가능.
  이 경우 cancelFee=0, refundAmt=0이라 기록해도 표시 없음 → section 숨김이 정상.
  → Bug 1은 실질 UX 영향 없음. (단, payment==null + cancelFee>0 케이스는 이미 CANCEL_FEE_UNPAID로 차단됨)

  Bug 2 (실제 표시 버그): REFUND 트랜잭션을 refundAmt=0이어도 cancelFee>0이면 생성해야 함.
  또는 renderCancelInfo에서 정책 설명을 REFUND 트랜잭션 외부 소스(별도 필드 또는 직접 계산)에서 가져와야 함.

  실용적 Fix: `processRefundWithPg()` 호출 조건을 `refundAmt > 0`에서
  `refundAmt > 0 || cancelFee > 0`으로 변경하되, 내부에서 refundAmt=0인 경우
  PG 취소 API는 호출하지 않고 CANCEL_FEE 타입 트랜잭션만 기록하는 방식 OR
  취소 수수료 전용 `CANCEL_FEE` 트랜잭션을 별도로 항상 생성.

  실제 적용된 Fix:
  1. ReservationServiceImpl.cancel(): `if (refundAmt > 0)` → `if (refundAmt > 0 || cancelFee > 0)`
     + memo에 policyDescription 포함: "{policyDesc} / 취소 환불 (수수료: X원)"
  2. ReservationServiceImpl.processNoShow(): 동일하게 적용
  3. ReservationPaymentServiceImpl.processRefundWithPg(): early-return을
     `if (refundAmount <= 0 && cancelFee <= 0)` 로 완화 (둘 다 0일 때만 skip)
     PG API 호출 조건을 `if (originalPgTxn != null && refundAmount > 0)` 로 수정 (환불 0이면 PG API 미호출)
  4. reservation-payment.js renderCancelInfo(): policyDesc 추출 시 " / " 기준으로 분리하여
     memo의 앞부분(정책 설명)만 표시

verification: 컴파일 성공 (BUILD SUCCESSFUL). 인간 검증 대기 중.
files_changed:
  - hola-pms/hola-reservation/src/main/java/com/hola/reservation/service/ReservationServiceImpl.java
  - hola-pms/hola-reservation/src/main/java/com/hola/reservation/service/ReservationPaymentServiceImpl.java
  - hola-pms/hola-app/src/main/resources/static/js/reservation-payment.js
