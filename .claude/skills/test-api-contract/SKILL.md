---
description: Validate API contracts including request schema, response schema, status codes, auth, validation, and edge cases
---

# Purpose
API 명세와 실제 구현의 일치 여부를 검증한다.

# When to use
- 신규 API 개발 후
- Swagger/OpenAPI 기준 검증 시
- 프론트 연동 전 점검 시
- 인증/인가/권한 로직 확인 시

# Workflow
1. 엔드포인트 목록과 명세를 파악한다.
2. 각 API에 대해 정상 요청, 필수값 누락, 타입 오류, 권한 오류, 존재하지 않는 리소스, 중복 요청을 점검한다.
3. 상태코드와 응답 구조가 일관되는지 확인한다.
4. 페이징, 정렬, 필터, 날짜/시간값, nullable 필드를 점검한다.
5. 계약 위반 사항과 수정 우선순위를 정리한다.

# Output format
- API 목록
- 검증 항목
- 계약 일치 여부
- 발견 이슈
- 수정 권장사항