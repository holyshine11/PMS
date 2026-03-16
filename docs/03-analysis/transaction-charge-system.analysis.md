---
feature: transaction-charge-system
phase: check
analyzedAt: 2026-03-16
matchRate: 91
iterationCount: 1
---

# Gap Analysis: transaction-charge-system

## Match Rate: 82% (70항목 중 54 OK + 7 WARN + 9 MISS)

## Category Scores

| Category | Score |
|----------|:-----:|
| 엔티티 설계 | 96% |
| API 명세 | 82% |
| 재고 하이브리드 아키텍처 | 100% |
| Flyway DDL | 100% |
| 에러 코드 | 73% |
| 구현 순서/파일 목록 | 88% |
| 비즈니스 규칙 | 70% |

## MISS 항목 (9건)

| # | 항목 | 영향도 |
|---|------|--------|
| 1 | BookingServiceImpl 재고 연동 (예약 생성/취소/수정 시) | **HIGH** |
| 2 | PaidServiceOption DTO에 inventoryItemId/inventoryItemName 필드 | MEDIUM |
| 3 | GET /upgrade/available-types API + DTO | MEDIUM |
| 4 | PAID 업그레이드 시 ReservationServiceItem 자동 부과 | MEDIUM |
| 5 | 업그레이드 시 대상 객실타입 레이트 존재 검증 | MEDIUM |
| 6 | 에러코드 3건 (HOLA-2611, 4102, 4103) | LOW |
| 7 | TC 삭제 시 PaidServiceOption/ReservationServiceItem 참조 체크 | LOW |
| 8 | UpgradePreviewResponse.dailyDiffs 필드 | LOW |
| 9 | InventoryItemMapper.java 별도 파일 | LOW (inline 대체) |

## 권장 조치 우선순위

1. **HIGH**: BookingServiceImpl 재고 연동
2. **MEDIUM**: PaidServiceOption DTO inventoryItemId 추가
3. **MEDIUM**: available-types API + PAID 부과 연동 + 레이트 검증
4. **LOW**: 에러코드 3건 + TC 참조 체크
