---
description: Run focused regression testing around changed code and adjacent workflows
---

# Purpose
최근 수정으로 인해 기존 기능이 깨졌는지 확인한다.

# When to use
- 기능 수정 직후
- 핫픽스 반영 후
- PR 검토 전
- 배포 전 점검 시

# Workflow
1. 최근 변경 파일과 영향 범위를 파악한다.
2. 직접 영향 기능과 인접 기능을 구분한다.
3. 최소 핵심 회귀 시나리오를 선정한다.
4. 변경된 기능뿐 아니라 연결된 주요 기능도 함께 점검한다.
5. 테스트 명령 또는 수동 검증 절차를 실행한다.
6. 실패 항목은 변경 파일과 연결해서 원인 후보를 제시한다.
7. 통과/실패 결과와 잔여 리스크를 정리한다.

# Output format
- 변경 범위
- 영향 기능
- 회귀 테스트 시나리오
- 통과/실패 결과
- 발견 이슈
- 원인 후보
- 리스크 및 권장 조치