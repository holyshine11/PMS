# Plan: 프론트데스크 운영현황 전체 상태 필터 지원

## Executive Summary

| 항목 | 내용 |
|------|------|
| Feature | front-desk-filter |
| 작성일 | 2026-03-18 |
| 예상 소요 | 30분 |

### Value Delivered

| 관점 | 내용 |
|------|------|
| Problem | 운영현황 필터에서 '체크아웃', '취소', '노쇼' 클릭 시 데이터가 표시되지 않음 |
| Solution | 통합 조회 API 추가로 오늘 관련 전 상태 SubReservation을 한 번에 조회 |
| Function UX Effect | 6개 상태 필터 모두 정상 동작, API 호출 3회→1회로 성능 개선 |
| Core Value | 프론트데스크 직원이 모든 상태의 예약을 한 화면에서 확인·관리 가능 |

## 1. 현황 분석

### 근본 원인
- 백엔드 API 3개(`/arrivals`, `/in-house`, `/departures`)가 특정 상태만 반환
- `CHECKED_OUT`, `CANCELED`, `NO_SHOW` 상태 데이터를 조회하는 API 없음
- 프론트엔드에 6개 필터 버튼이 있지만, 해당 상태 데이터가 `allData`에 존재하지 않아 빈 결과

### 현재 API별 반환 상태

| API | 조건 | 반환 상태 |
|-----|------|-----------|
| `/arrivals` | checkIn=today | RESERVED |
| `/in-house` | checkOut>=today | CHECK_IN, INHOUSE |
| `/departures` | checkOut=today | CHECK_IN, INHOUSE |

## 2. 구현 계획

### 접근 방식
기존 3개 API(요약카드용)를 유지하고, **통합 조회 API 1개 추가** + **프론트엔드 데이터 로드 전환**

### 변경 파일

| # | 파일 | 변경 내용 |
|---|------|-----------|
| 1 | `SubReservationRepository.java` | `findAllOperations()` 쿼리 추가 |
| 2 | `FrontDeskService.java` | `getAllOperations()` 인터페이스 추가 |
| 3 | `FrontDeskServiceImpl.java` | `getAllOperations()` 구현 |
| 4 | `FrontDeskApiController.java` | `GET /all` 엔드포인트 추가 |
| 5 | `fd-operations-page.js` | `loadAllData()` → 단일 API 호출로 전환 |

### 통합 쿼리 조건
오늘과 관련된 모든 SubReservation:
- `checkIn = today` (오늘 도착: RESERVED, CHECK_IN, CANCELED, NO_SHOW)
- `checkIn <= today AND checkOut >= today` (현재 체류: CHECK_IN, INHOUSE)
- `checkOut = today AND status = CHECKED_OUT` (오늘 퇴실 완료)

### 기존 호환성
- 요약카드 `/summary` API 및 3개 개별 API → 유지 (요약 건수 계산용)
- 프론트엔드 `specialFilter` (요약카드 클릭) → 그대로 동작
- 상태 필터 버튼 HTML → 변경 없음

## 3. 구현 순서
1. Repository: `findAllOperations()` JPQL 추가
2. Service: 인터페이스 + 구현체에 `getAllOperations()` 추가
3. Controller: `GET /all` 엔드포인트 추가
4. JS: `loadAllData()`에서 단일 `/all` API 호출로 변경, 기존 3개 API는 요약카드 건수 보정용으로만 유지
