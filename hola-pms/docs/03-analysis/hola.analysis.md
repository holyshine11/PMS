# Gap Analysis Report: Hola PMS 예약 모듈

- **분석일**: 2026-02-28
- **대상 모듈**: hola-reservation (M07)
- **Match Rate**: 91%

## 종합 점수

| 카테고리 | 점수 | 상태 |
|----------|:----:|:----:|
| 설계 일치도 | 92% | PASS |
| 아키텍처 준수 | 88% | WARNING |
| 컨벤션 준수 | 94% | PASS |
| **종합** | **91%** | **PASS** |

## Match Rate 상세

| 체크 항목 | 배점 | 득점 | 비율 |
|-----------|:----:|:----:|:----:|
| accessControlService 호출 | 10 | 10 | 100% |
| @Valid 적용 | 10 | 9.3 | 93% |
| DTO 검증 어노테이션 | 10 | 9.0 | 90% |
| HolaResponse 통일 | 10 | 8.5 | 85% |
| Soft Delete 준수 | 10 | 9.5 | 95% |
| ErrorCode enum 사용 | 10 | 10 | 100% |
| 상태 전이 완전성 | 10 | 9.5 | 95% |
| 결제 금액 정확성 | 10 | 9.2 | 92% |
| 날짜 유효성 검증 | 10 | 9.0 | 90% |
| 동시성 보호 | 10 | 7.2 | 72% |
| **합계** | **100** | **91.2** | **91%** |

## 이번 세션에서 수정 완료된 항목

| # | 항목 | 수정 내용 | 영향 |
|---|------|----------|------|
| H1 | 결제/금액 DTO @Valid | 4개 DTO에 검증 어노테이션 + 컨트롤러 @Valid 추가 | HIGH |
| H2 | 예약 수정 과거 날짜 | update()에 과거 체크인 검증 추가 | HIGH |
| H3 | 예치금 소속 검증 | depositId가 reservationId에 속하는지 검증 | HIGH |
| H5 | 레이트 수수료 기준 | 마지막 투숙일 기준으로 수정 | HIGH |
| H6 | modifyBooking 자기제외 | getAvailableRoomCount(excludeSubIds) 오버로드 | HIGH |
| H7 | 인덱스 추가 | LOWER(email,lastName) expression index | HIGH |
| H8 | grandTotal 음수 | 기결제+grandTotal<=0 시 OVERPAID 처리 | HIGH |

## 잔여 이슈 (수정 미진행)

### Critical - 동시성 보호 (설계 결정 필요)

| # | 문제 | 위치 | 권장 조치 |
|---|------|------|----------|
| C1 | 객실 가용성 TOCTOU | RoomAvailabilityService | DB UNIQUE 제약 or 비관적 락 |
| C2 | 결제 동시성 미보호 | ReservationPaymentServiceImpl | @Version 낙관적 락 |
| C3 | 시퀀스 생성 레이스 | ReservationNumberGenerator | @Lock(PESSIMISTIC_WRITE) 강화 |

### Medium - 검증/컨벤션

| # | 문제 | 위치 | 권장 조치 |
|---|------|------|----------|
| M1 | 메모 등록 Map 사용 | ReservationApiController | 전용 DTO 생성 |
| M2 | ReservationGuestRequest 검증 부재 | dto/request | @NotBlank 추가 |
| M3 | SubReservationRequest.roomTypeId | dto/request | @NotNull 추가 |
| M4 | 프로모션 할인 미반영 | PriceCalculationService | 할인 로직 구현 |
| M5 | BookingResponse vs HolaResponse | BookingApiController | CLAUDE.md 문서 업데이트 |

## 결론

Match Rate 91%로 PASS 기준(90%) 충족. 핵심 비즈니스 로직은 설계대로 충실히 구현됨.
가장 시급한 개선은 동시성 보호(C1~C3)이며 운영 전 반드시 보강 필요.
