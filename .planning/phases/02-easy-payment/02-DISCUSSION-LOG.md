# Phase 02: 간편결제(빌키) 기능 - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-03-26
**Phase:** 02-easy-payment
**Areas discussed:** 게스트 식별, 빌키 저장 방식, 카드 등록 플로우, 간편결제 실행, 카드 삭제
**Mode:** --auto (all decisions auto-selected with recommended defaults)

---

## 게스트 식별

| Option | Description | Selected |
|--------|-------------|----------|
| 이메일 기반 | checkout 폼의 이메일로 카드 연결, 이메일 입력 시 자동 조회 | ✓ |
| 전화번호 기반 | 전화번호로 카드 연결 | |
| sessionStorage | 브라우저 세션 한정, 새 창에서 유실 | |
| 전체 노출 (데모) | 등록된 모든 카드를 표시 (단일 데모 환경) | |

**User's choice:** [auto] 이메일 기반 (recommended default)
**Notes:** 데모용이지만 실제 서비스 확장성도 고려. 이메일이 가장 자연스러운 식별자.

---

## 빌키 저장 방식

| Option | Description | Selected |
|--------|-------------|----------|
| DB 테이블 (rsv_easy_pay_card) | BaseEntity 상속, soft delete, 이메일 당 최대 5개 | ✓ |
| Redis | 임시 저장, 서버 재시작 시 유실 가능 | |
| localStorage | 브라우저 한정, 보안 취약 | |

**User's choice:** [auto] DB 테이블 (recommended default)
**Notes:** 빌키는 민감 데이터이므로 서버 측 저장이 필수. BaseEntity 패턴으로 일관성 유지.

---

## 카드 등록 플로우

| Option | Description | Selected |
|--------|-------------|----------|
| KICC 인증창 팝업 | 기존 결제 팝업과 동일 패턴, PC popup / Mobile redirect | ✓ |
| 직접 API 호출 | 카드번호 직접 입력 받아 API 호출 (PCI-DSS 문제) | |

**User's choice:** [auto] KICC 인증창 팝업 (recommended default)
**Notes:** PCI-DSS 준수 + 기존 팝업 패턴 재활용으로 구현 비용 최소화.

---

## 간편결제 실행

| Option | Description | Selected |
|--------|-------------|----------|
| createBookingWithPaymentResult 재활용 | 빌키 결제 승인 후 기존 메서드로 예약+결제 생성 | ✓ |
| 별도 플로우 구현 | 빌키 결제 전용 예약 생성 로직 | |

**User's choice:** [auto] createBookingWithPaymentResult 재활용 (recommended default)
**Notes:** 기존 메서드가 PaymentResult만 받으면 동작하므로, 빌키 결제 승인 결과를 PaymentResult로 변환하면 그대로 사용 가능.

---

## 카드 삭제

| Option | Description | Selected |
|--------|-------------|----------|
| KICC + DB 동시 삭제 | KICC removeBatchKey 호출 + DB soft delete, KICC 실패 시에도 DB 삭제 | ✓ |
| DB만 삭제 | KICC에는 남겨두고 DB에서만 삭제 | |
| KICC 우선 | KICC 성공 시에만 DB 삭제 | |

**User's choice:** [auto] KICC + DB 동시 삭제 (recommended default)
**Notes:** 데모 환경에서 KICC 통신 실패가 카드 삭제를 blocking하지 않도록 함.

---

## Claude's Discretion

- 에러 처리 상세 전략
- Redis TTL 설정
- MockPaymentGateway 빌키 메서드 추가
- 프론트 카드 렌더링 디자인 디테일

## Deferred Ideas

None
