---
status: complete
phase: rsv-payment
source: git diff (uncommitted reservation/payment changes)
started: 2026-04-03T10:00:00+09:00
updated: 2026-04-03T11:50:00+09:00
---

## Current Test

[testing complete]

## Tests

### 1. VAN 카드결제 버튼 및 모달
expected: 예약 상세 페이지 결제 영역에 "VAN 카드결제" 버튼(파란색)이 표시된다. 클릭 시 워크스테이션 선택 + 결제금액이 표시된 모달이 열리고, KPSP 연동을 통해 카드결제를 진행할 수 있다.
result: pass

### 2. VAN 현금결제 버튼 및 모달
expected: "VAN 현금결제" 버튼(녹색)을 클릭하면 워크스테이션 선택 + 현금영수증 옵션이 포함된 모달이 열린다. KPSP 현금영수증 발급과 함께 결제가 처리된다.
result: issue
reported: "수동 결제는 안보이고 VAN 현금 결제만 존재 함. Leg별 버튼에서 현금결제가 바로 VAN 현금결제로 연결되고, 수동 현금결제 옵션이 없음"
severity: minor

### 3. 수동 현금결제
expected: "수동 현금결제" 버튼(회색 outline)을 클릭하면 기존 현금결제 모달이 열린다. 금액/메모 입력 후 VAN 연동 없이 직접 결제 기록을 남길 수 있다.
result: pass

### 4. 거래 내역 테이블 - PG/VAN 구분 표시
expected: 거래 내역에서 카드 결제 시 "카드(PG)" 또는 "카드(VAN)"으로 구분 표시된다. VAN 현금은 "현금(VAN)"으로 표시. 내용 컬럼에 카드사/승인번호 등 상세 정보가 한 행에 표시된다.
result: pass

### 5. VAN 거래 취소
expected: VAN으로 결제된 거래의 내용 컬럼에 "VAN 취소" 버튼이 표시된다. 클릭 시 KPSP 취소 연동 후 REFUND 트랜잭션이 생성되고 결제 잔액이 갱신된다.
result: issue
reported: "(a) 카드 VAN 취소 시 KPSP에서는 취소 완료되나 PMS에 REFUND 미반영 (할부 미입력 시 통신실패 응답 → PMS 실패 판단하지만 VAN측 이미 취소) (b) 이미 취소된 VAN 거래에 VAN 취소 버튼 잔존 — 현금 취소는 정상"
severity: major

### 6. 멀티 Leg 카드/현금 결제 (Leg별)
expected: 멀티 Leg 예약에서 각 Leg 카드에 있는 카드/현금 결제 버튼으로 해당 Leg에 대해서만 VAN 결제가 진행된다. 거래 내역에 올바른 Leg가 매핑된다.
result: pass

### 7. 서비스 추가 - 체크아웃 당일 허용
expected: Leg의 서비스 추가 시 날짜 선택 범위가 체크인일~체크아웃 당일까지 허용된다. (기존: 체크아웃 전일까지)
result: pass

### 8. 서비스 추가 - 미저장 Leg 안전장치
expected: 새로 추가한 Leg(아직 저장 안 됨)에서 "서비스 추가" 버튼이 표시되지 않는다. 또는 클릭 시 "객실을 먼저 저장한 후 서비스를 추가해주세요" 안내 메시지가 표시된다.
result: pass

### 9. 마지막 Leg 삭제 차단
expected: 예약에 활성 Leg가 1개만 남은 상태에서 삭제 시도 시 오류 메시지가 표시되고 삭제가 차단된다.
result: pass

### 10. 종료 Leg 편집 차단
expected: CANCELED/CHECKED_OUT/NO_SHOW 상태의 Leg는 입력 필드가 비활성화되고, 업그레이드/삭제/검색/클리어/서비스추가 버튼이 숨겨진다.
result: pass

### 11. 취소 처리 - Leg별 환불 분배
expected: 멀티 Leg PG 결제 예약을 취소하면 각 Leg별로 올바른 subReservationId와 결제수단으로 환불이 처리된다. (현금 Leg에 PG 환불 시도 안 함)
result: issue
reported: "(a) 환불 거래 수단 컬럼에 PG/VAN 채널 구분 없음 (b) 취소 모달에서 VAN 취소 직접 실행 버튼 부재 — 관리자 수동 처리 안내만 표시 (c) 환불 수수료 정책 적용 정확성 별도 검증 필요"
severity: major

### 12. 취소/환불 정보 UI
expected: 취소된 예약의 결제 정보에서 Leg별 상세 환불 카드(PG/현금/VAN 구분)와 전체 요약이 표시된다.
result: issue
reported: "금액 계산/구조는 정확하나 renderTxnDetail에서 VAN 채널 구분 미표시 — txn.pgCno만 체크하여 PG만 구분, VAN은 그냥 카드/현금으로 표시"
severity: minor

### 13. VIEW 모드 VAN 버튼 숨김
expected: 읽기 전용 모드(VIEW)에서 "VAN 카드결제", "VAN 현금결제", "수동 현금결제" 버튼이 모두 숨겨진다.
result: skipped
reason: 권한 없는 경우 메뉴 자체가 숨겨지므로 해당 없음

## Summary

total: 13
passed: 8
issues: 4
pending: 0
skipped: 1
blocked: 0

## Gaps

- truth: "Leg별 결제 버튼에 수동 현금결제 옵션이 글로벌 영역과 동일하게 존재해야 함"
  status: failed
  reason: "User reported: Leg별 버튼에 수동 현금결제 없이 현금결제가 바로 VAN 현금결제로 연결됨"
  severity: minor
  test: 2
  root_cause: "reservation-detail.js의 Leg별 pay-method=cash가 openVanCashPaymentModal로 라우팅됨, openCashPaymentModal 옵션 미제공"
  artifacts:
    - path: "hola-pms/hola-app/src/main/resources/static/js/reservation-detail.js"
      issue: "Leg별 현금결제 버튼이 VAN 현금결제만 호출"
  missing:
    - "Leg별 결제 버튼에 수동 현금결제 옵션 추가 또는 VAN/수동 선택 UI 제공"

- truth: "VAN 카드 취소 시 KPSP 취소 완료된 건이 PMS에도 정확히 반영되어야 함"
  status: failed
  reason: "User reported: 할부 미입력 시 통신실패 응답으로 PMS에 미반영되나 VAN측은 이미 취소 완료"
  severity: major
  test: 5
  root_cause: "KPSP 카드 취소 시 할부 개월수 필수 전송 누락 또는 응답 코드 '0000' 외 성공 코드 미처리"
  artifacts:
    - path: "hola-pms/hola-app/src/main/resources/static/js/reservation-payment.js"
      issue: "processVanCancel에서 KPSP 취소 요청 시 할부 정보 누락 가능"
  missing:
    - "KPSP 카드 취소 시 원거래 할부 정보 전송"
    - "취소 실패 시에도 VAN 측 상태 확인 로직"

- truth: "이미 취소된 VAN 거래에는 VAN 취소 버튼이 표시되지 않아야 함"
  status: failed
  reason: "User reported: 현금 취소 후 REFUND(#3) 생성되었으나 원거래(#2)에 VAN 취소 버튼 잔존"
  severity: major
  test: 5
  root_cause: "renderPaymentTransactions에서 PAYMENT 타입이면 무조건 VAN 취소 버튼 표시, 동일 거래에 대한 REFUND 존재 여부 미체크"
  artifacts:
    - path: "hola-pms/hola-app/src/main/resources/static/js/reservation-payment.js"
      issue: "VAN 취소 버튼 표시 조건에 이미 취소된 거래 제외 로직 누락"
  missing:
    - "동일 vanSequenceNo 또는 원거래 ID로 REFUND가 존재하면 VAN 취소 버튼 숨김"

- truth: "환불 거래 내역의 수단 컬럼에 PG/VAN 채널이 구분되어야 함"
  status: failed
  reason: "User reported: 환불 내역 수단이 카드/현금으로만 표시, PG/VAN 구분 없음"
  severity: major
  test: 11
  root_cause: "renderPaymentTransactions의 methodLabel 분기가 PAYMENT에만 채널 구분 적용, REFUND에는 미적용"
  artifacts:
    - path: "hola-pms/hola-app/src/main/resources/static/js/reservation-payment.js"
      issue: "methodLabel PG/VAN 구분이 PAYMENT 타입에만 적용됨"
  missing:
    - "REFUND 타입에도 paymentChannel 기반 카드(PG)/카드(VAN) 구분 적용"

- truth: "취소/환불 정보 renderTxnDetail에서도 VAN 채널이 구분 표시되어야 함"
  status: failed
  reason: "Code review: renderTxnDetail이 txn.pgCno만 체크하여 PG 구분, VAN은 일반 카드/현금으로 표시"
  severity: minor
  test: 12
  root_cause: "renderTxnDetail 함수에 paymentChannel === 'VAN' 분기 누락"
  artifacts:
    - path: "hola-pms/hola-app/src/main/resources/static/js/reservation-payment.js"
      issue: "renderTxnDetail에서 VAN 채널 미구분"
  missing:
    - "txn.paymentChannel === 'VAN' 조건 추가하여 카드(VAN)/현금(VAN) 표시"
