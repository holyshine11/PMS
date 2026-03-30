# 취소/노쇼 수수료 정책

## 1. 수수료 기준 요금

### 1.1 기본 원칙
취소 수수료는 **1박 숙박 총요금** 기준으로 산정한다.

| 유형 | 수수료 기준 | 근거 |
|------|-----------|------|
| 일반 예약 (변경 없음) | 예약 시점 1박 총액 (공급가+세+봉사료) | 소비자분쟁해결기준 "숙박 총요금" |
| 무료(컴플리멘터리) 업그레이드 | **원래 예약 시점** 1박 총액 | 호텔 주도 업그레이드 → 고객 불이익 불가 |
| 유료 업그레이드 | 원래 1박 총액 + 업그레이드 차액(1박분) | 고객이 합의한 신규 총요금 기준 |

### 1.2 구현 방식
- `ReservationPayment.originalFirstNightTotal`: 최초 예약 시점 1박 총액 (불변)
- 무료 업그레이드: `originalFirstNightTotal` 사용
- 유료 업그레이드: `originalFirstNightTotal` + `RoomUpgradeHistory.priceDifference` / 숙박일수
- Leg 단위 취소: 해당 Leg의 DailyCharge 1박 총액 (현재 객실 요금 기준)

## 2. 수수료율 (소비자분쟁해결기준 2024.12 개정 기준)

| 시점 | 수수료율 | 비고 |
|------|---------|------|
| 체크인 10일 이상 전 | 0% | 무료 취소 |
| 체크인 7일 전 | 10% | |
| 체크인 5일 전 | 30% | |
| 체크인 3일 전 | 50% | |
| 체크인 1일 전 ~ 당일 | 80% | |
| 노쇼 | 100% | NOSHOW 정책 별도 |

> 프로퍼티별 `htl_cancellation_fee` 테이블에서 커스텀 설정 가능

## 3. 다중 객실(Multi-Leg) 취소

### 3.1 전체 예약 취소 (DELETE /reservations/{id})
- 마스터 레벨 `originalFirstNightTotal` 기준 수수료 적용
- 전체 Leg 일괄 CANCELED 처리
- 환불 = totalPaidAmount - cancelFee

### 3.2 개별 Leg 취소 (PUT /status → CANCELED, subReservationId 지정)
- **해당 Leg의 1박 DailyCharge 기준** 수수료 적용 (Per-Room, Opera PMS 표준)
- 해당 Leg만 CANCELED, 나머지 Leg는 유지
- grandTotal 재계산 → 초과결제분 환불 트랜잭션 자동 생성

## 4. 환불 분배

### 4.1 다중 결제수단 환불 순서
1. PG 결제건: PG 결제액 범위 내에서 환불 (KICC 부분취소 API)
2. 비-PG 결제건 (현금/카드VAN): DB REFUND 기록 + "현금 환불 필요" 안내

### 4.2 OTA 예약
- OTA 예약(isOtaManaged=true) 취소 시 PG 환불 API 호출 차단
- DB 기록만 남기고 "OTA 채널에서 환불 처리 필요" 메모

## 5. 근거 자료

| 출처 | 내용 |
|------|------|
| 소비자분쟁해결기준 (2024.12 개정) | 숙박 총요금 기준 취소 수수료율 |
| Oracle Opera PMS | Per-Room 취소, 레이트코드 기준 자동 재계산 |
| Mews PMS | 예약 생성 시 정책 고정 (원래 요금 보호) |
| Marriott/Hilton/Hyatt | 무료 업그레이드 → 원래 요금, 유료 → 합산 요금 |
| 한국 소비자보호원 | 호텔 일방 업그레이드 시 원래 요금 기준 |
