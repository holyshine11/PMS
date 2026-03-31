---
status: awaiting_human_verify
trigger: "admin-cancel-no-payment-record: PMS 관리자 취소 시 예약 상태만 변경되고, 부분 취소 결제 처리 내역이 생성되지 않는 문제"
created: 2026-03-30T00:00:00+09:00
updated: 2026-03-30T09:00:00+09:00
---

## Current Focus
<!-- OVERWRITE on each update - reflects NOW -->

hypothesis: CONFIRMED — 관리자 화면의 Leg 레벨 취소 버튼이 PUT /status API(changeStatus)를 호출하는데, 이 경로에는 결제 처리 로직이 없음
test: DB 조회로 GMP260330-0014 결제 내역 확인 + 코드 양쪽 경로 비교
expecting: changeStatus()에 CANCELED 처리 로직 추가 + JS에서 취소 미리보기 모달 라우팅 추가
next_action: 컴파일 검증 완료, 서버 동작 확인 필요

## Symptoms
<!-- Written during gathering, then IMMUTABLE -->

expected: PMS 관리자가 예약을 취소하면, 예약 상태 변경 + PG 부분취소 API 호출 + 부분취소 결제 내역(rsv_payment_transaction) 생성
actual: 예약 상태만 CANCELLED로 변경되고, 부분 취소 결제 처리 내역이 남지 않음. PG 취소 API 호출 여부도 불분명.
errors: 명시적 에러 메시지 없음 (상태만 바뀌고 조용히 끝남)
reproduction: 예약번호 GMP260330-0014로 PMS 관리자 화면에서 취소 처리
started: 게스트 자가 취소는 정상 동작 확인됨. 관리자 취소 경로만 문제.

## Eliminated
<!-- APPEND only - prevents re-investigating -->

- hypothesis: cancel() 메서드 자체에 payment 처리가 없다
  evidence: cancel() (DELETE /{id} 엔드포인트)에는 processRefundWithPg 호출이 있음. 단, 관리자 화면이 이 경로를 안 탔음.
  timestamp: 2026-03-30T09:00:00+09:00

- hypothesis: processRefundWithPg 조건 버그 (refundAmt > 0 조건)
  evidence: WIP 변경으로 이미 수정됨. 그러나 근본 원인은 호출 자체가 없었던 것.
  timestamp: 2026-03-30T09:00:00+09:00

## Evidence
<!-- APPEND only - facts discovered -->

- timestamp: 2026-03-30T09:00:00+09:00
  checked: DB — rsv_reservation_payment for reservation id=817 (GMP260330-0014)
  found: total_paid_amount=435600, cancel_fee_amount=0.00, refund_amount=0.00, payment_status=PAID
  implication: payment.updateCancelRefund()가 전혀 호출되지 않았음 (0으로 업데이트된 게 아님, 초기값 그대로)

- timestamp: 2026-03-30T09:00:00+09:00
  checked: DB — rsv_payment_transaction for master_reservation_id=817
  found: 거래 1건 (PAYMENT/COMPLETED/435600원) 만 있고 REFUND 거래 없음
  implication: processRefundWithPg가 전혀 호출되지 않았음

- timestamp: 2026-03-30T09:00:00+09:00
  checked: reservation-detail.js leg-status-change click handler (line 1137)
  found: newStatus === 'CANCELED' 분기 없이 바로 self.changeStatus(newStatus, legId) 호출
  implication: 취소 수수료 미리보기 모달을 거치지 않고 PUT /status 직접 호출

- timestamp: 2026-03-30T09:00:00+09:00
  checked: ReservationServiceImpl.changeStatus() — lines 721-724
  found: NO_SHOW 시 processNoShow() 호출하지만, CANCELED 시 대응 메서드 없음
  implication: changeStatus() 경로에서 CANCELED 처리 시 PG 환불/거래 기록이 누락됨

- timestamp: 2026-03-30T09:00:00+09:00
  checked: compilaition of hola-reservation after fix
  found: BUILD SUCCESSFUL — processCancel() 메서드 추가 및 changeStatus()에 CANCELED 분기 추가 완료
  implication: 코드 수준 수정 완료

## Resolution
<!-- OVERWRITE as understanding evolves -->

root_cause: |
  관리자 화면의 Leg 레벨 취소 버튼(.leg-status-change)이 confirmStatusChange()를 거치지 않고
  PUT /status API(changeStatus 서비스)를 직접 호출함.
  changeStatus()는 NO_SHOW → processNoShow()는 호출하지만,
  CANCELED에 해당하는 processCancel() 호출이 없어서
  결제 정보 업데이트(updateCancelRefund)와 PG 환불 API 호출(processRefundWithPg)이 전혀 이뤄지지 않음.

fix: |
  1. ReservationServiceImpl.changeStatus(): derivedStatus가 CANCELED로 확정되고 이전 상태가 CANCELED가 아닌 경우에만
     processCancel() 호출 추가 (NO_SHOW의 processNoShow() 패턴과 동일)
  2. ReservationServiceImpl: private processCancel() 메서드 추가
     (cancel() 메서드의 결제 처리 로직과 동일, 취소 수수료 검증 + updateCancelRefund + processRefundWithPg)
  3. reservation-detail.js: .leg-status-change 클릭 핸들러에서 CANCELED/NO_SHOW 시
     showCancelPreview()로 라우팅 추가 (마스터 레벨 확인과 동일한 UX)

verification:
files_changed:
  - hola-reservation/src/main/java/com/hola/reservation/service/ReservationServiceImpl.java
  - hola-app/src/main/resources/static/js/reservation-detail.js
