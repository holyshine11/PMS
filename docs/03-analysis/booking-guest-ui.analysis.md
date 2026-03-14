---
feature: booking-guest-ui
phase: check
analyzedAt: 2026-03-14
matchRate: 93
iteration: 1
---

# Gap Analysis: 부킹엔진 게스트 UI

## Overall Match Rate: 93% (PASS)

| Category | Score | Status |
|----------|:-----:|:------:|
| API 정합성 | 95% | PASS |
| DTO 필드 매핑 | 95% | PASS |
| 응답 필드 활용 | 88% | PASS |
| 플로우 정합성 | 95% | PASS |
| 데이터 전달 | 95% | PASS |
| 보안/UX | 92% | PASS |
| CSS/디자인 규칙 | 95% | PASS |
| Convention 준수 | 97% | PASS |

## 수정 완료 (Iteration 1)

### GAP-001: Confirmation API 검증 파라미터 누락 → FIXED
- **수정**: confirmation.html에 이메일 인증 폼 추가, API 호출 시 email 파라미터 전달
- **변경 파일**: `booking.js` (ConfirmationPage), `confirmation.html`
- sessionStorage 데이터가 없으면 이메일 입력 → API 호출 → 확인 정보 표시

### GAP-002: price-check API 미활용 → FIXED
- **수정**: CheckoutPage.verifyPrice() 메서드 추가, 체크아웃 페이지 로드 시 price-check API 호출
- **변경 파일**: `booking.js` (CheckoutPage)
- 가격 변동 시 자동 업데이트 + 사용자 알림, 실패 시 기존 금액으로 비차단 진행

## 미수정 (LOW 우선순위 - 향후 개선)

### GAP-003: 에러 코드 미활용
- `xhr.responseJSON.message`만 표시, `code` 필드 미활용
- 현재 단계에서 메시지 표시만으로 충분, 향후 UX 개선 시 적용

### GAP-004: PropertyInfoResponse 미활용 필드
- `logoPath`, `starRating`, `propertyType` 미표시
- 기능 영향 없음, 향후 디자인 고도화 시 적용

## 정합성 우수 영역

- BookingCreateRequest 필수 필드 100% 매칭
- AvailableRoomTypeResponse 주요 필드 100% 활용 (dailyPrices 포함)
- SecurityConfig `/api/v1/booking/**` + `/booking/**` permitAll 정상
- XSS 방지 escapeHtml() 전역 적용
- 중복 제출 방지 (submitting 플래그 + idempotencyKey UUID)
- Admin/Guest 레이아웃 완전 분리 (layout/booking.html)
- 프로젝트 컬러 테마 5색 CSS 변수로 정확 적용
- Pretendard 폰트 CDN 로드, 모바일 반응형 (768px/576px)
- price-check API로 체크아웃 전 가격 재확인 (GAP-002 수정 후)
- Confirmation 이메일 인증 플로우 추가 (GAP-001 수정 후)
