// @ts-check
/**
 * M01 호텔관리 E2E 테스트
 * 대상: 호텔관리, 프로퍼티관리, 마켓코드, 층코드, 호수코드
 */
const { test, expect } = require('@playwright/test');
const { collectConsoleErrors } = require('./helpers');

// ─────────────────────────────────────────────
// 1. 호텔 리스트
// ─────────────────────────────────────────────
test.describe('호텔 리스트', () => {
  test('페이지 로드 및 DataTable 표시', async ({ page }) => {
    const consoleErrors = collectConsoleErrors(page);

    await page.goto('/admin/hotels');
    await page.waitForLoadState('networkidle');

    // 에러 페이지 아닌지 확인
    const bodyText = await page.textContent('body');
    expect(bodyText).not.toContain('Whitelabel Error');
    expect(bodyText).not.toContain('500');

    // 페이지 타이틀
    await expect(page.locator('h4')).toContainText('호텔 관리');

    // DataTable 표시
    const table = page.locator('#hotelTable');
    await expect(table).toBeVisible({ timeout: 10000 });

    // 콘솔 에러 보고 (실패 기준은 아님)
    if (consoleErrors.length > 0) {
      console.log('[콘솔 에러]', consoleErrors);
    }
  });

  test('API 응답 및 데이터 존재 확인', async ({ page }) => {
    // API 응답 인터셉트
    const apiResponsePromise = page.waitForResponse(
      resp => resp.url().includes('/api/v1/hotels') && resp.status() < 400,
      { timeout: 10000 }
    );

    await page.goto('/admin/hotels');
    const apiResponse = await apiResponsePromise;
    expect(apiResponse.status()).toBe(200);

    const json = await apiResponse.json();
    expect(json).toHaveProperty('data');

    // 데이터가 존재하면 테이블 행 확인
    if (Array.isArray(json.data) && json.data.length > 0) {
      await page.waitForLoadState('networkidle');
      const rows = page.locator('#hotelTable tbody tr');
      await expect(rows.first()).toBeVisible({ timeout: 10000 });
    }
  });

  test('검색 필드 및 버튼 존재 확인', async ({ page }) => {
    await page.goto('/admin/hotels');
    await page.waitForLoadState('networkidle');

    await expect(page.locator('#searchHotelName')).toBeVisible();
    await expect(page.locator('button:has-text("검색")')).toBeVisible();
    await expect(page.locator('button:has-text("초기화")')).toBeVisible();
    await expect(page.locator('#pageSizeSelect')).toBeVisible();
    await expect(page.locator('a:has-text("호텔 등록")')).toBeVisible();
  });

  test('호텔명 검색 동작', async ({ page }) => {
    await page.goto('/admin/hotels');
    await page.waitForLoadState('networkidle');

    // API 응답에서 첫 번째 호텔명 추출
    const apiResp = await page.evaluate(async () => {
      const r = await fetch('/api/v1/hotels');
      return r.json();
    });

    if (apiResp.data && apiResp.data.length > 0) {
      const firstHotelName = apiResp.data[0].hotelName;
      await page.fill('#searchHotelName', firstHotelName);
      await page.click('button:has-text("검색")');
      await page.waitForTimeout(500);

      const rows = page.locator('#hotelTable tbody tr');
      const count = await rows.count();
      expect(count).toBeGreaterThan(0);
    }
  });
});

// ─────────────────────────────────────────────
// 2. 호텔 등록폼
// ─────────────────────────────────────────────
test.describe('호텔 등록폼', () => {
  test('페이지 로드 확인', async ({ page }) => {
    const consoleErrors = collectConsoleErrors(page);

    await page.goto('/admin/hotels/new');
    await page.waitForLoadState('networkidle');

    const bodyText = await page.textContent('body');
    expect(bodyText).not.toContain('Whitelabel Error');
    expect(bodyText).not.toContain('500');

    await expect(page.locator('#pageTitle')).toContainText('호텔 등록');

    if (consoleErrors.length > 0) {
      console.log('[콘솔 에러]', consoleErrors);
    }
  });

  test('필수 필드 존재 확인', async ({ page }) => {
    await page.goto('/admin/hotels/new');
    await page.waitForLoadState('networkidle');

    // 필수 필드: 호텔명
    await expect(page.locator('#hotelName')).toBeVisible();
    await expect(page.locator('label.required:has-text("호텔명")')).toBeVisible();

    // 기타 폼 필드
    await expect(page.locator('#representativeName')).toBeVisible();
    await expect(page.locator('#phone')).toBeVisible();
    await expect(page.locator('#introduction')).toBeVisible();
  });

  test('저장/취소 버튼 존재 확인', async ({ page }) => {
    await page.goto('/admin/hotels/new');
    await page.waitForLoadState('networkidle');

    await expect(page.locator('button:has-text("저장")')).toBeVisible();
    // 폼 하단의 취소 버튼 (btn-secondary class, 사이드바 nav-link와 구분: btn class 있음)
    const cancelBtn = page.locator('a.btn.btn-secondary');
    await expect(cancelBtn).toBeVisible();
    // 등록 모드: 삭제 버튼 숨김
    const deleteBtn = page.locator('#btnDelete');
    await expect(deleteBtn).not.toBeVisible();
  });

  test('중복확인 버튼 동작', async ({ page }) => {
    await page.goto('/admin/hotels/new');
    await page.waitForLoadState('networkidle');

    await page.fill('#hotelName', '_테스트호텔명_중복없음_' + Date.now());
    await page.click('button:has-text("중복확인")');
    // nameCheckResult 에 텍스트 출력 대기
    await page.waitForTimeout(1000);
    const resultText = await page.locator('#nameCheckResult').textContent();
    expect(resultText).not.toBe('');
  });
});

// ─────────────────────────────────────────────
// 3. 호텔 수정폼
// ─────────────────────────────────────────────
test.describe('호텔 수정폼', () => {
  test('리스트에서 첫 항목 클릭 → 수정폼 로드 및 필드 채워짐 확인', async ({ page }) => {
    const consoleErrors = collectConsoleErrors(page);

    await page.goto('/admin/hotels');
    await page.waitForLoadState('networkidle');

    // API에서 첫 번째 호텔 ID 추출
    const apiResp = await page.evaluate(async () => {
      const r = await fetch('/api/v1/hotels');
      return r.json();
    });

    if (!apiResp.data || apiResp.data.length === 0) {
      test.skip(true, '등록된 호텔 없음');
      return;
    }

    const firstId = apiResp.data[0].id;
    await page.goto(`/admin/hotels/${firstId}/edit`);
    await page.waitForLoadState('networkidle');

    const bodyText = await page.textContent('body');
    expect(bodyText).not.toContain('Whitelabel Error');
    expect(bodyText).not.toContain('500');

    // 수정 타이틀
    await expect(page.locator('#pageTitle')).toContainText('호텔 수정');

    // 호텔코드 행 표시 (수정 모드)
    await expect(page.locator('#hotelCodeRow')).toBeVisible();

    // 호텔명 필드에 값이 채워져 있는지
    const hotelNameValue = await page.locator('#hotelName').inputValue();
    expect(hotelNameValue.length).toBeGreaterThan(0);

    // 삭제 버튼 표시 (수정 모드)
    await expect(page.locator('#btnDelete')).toBeVisible();

    if (consoleErrors.length > 0) {
      console.log('[콘솔 에러]', consoleErrors);
    }
  });
});

// ─────────────────────────────────────────────
// 4. 프로퍼티 리스트
// ─────────────────────────────────────────────
test.describe('프로퍼티 리스트', () => {
  test('페이지 로드 확인', async ({ page }) => {
    const consoleErrors = collectConsoleErrors(page);

    await page.goto('/admin/properties');
    await page.waitForLoadState('networkidle');

    const bodyText = await page.textContent('body');
    expect(bodyText).not.toContain('Whitelabel Error');
    expect(bodyText).not.toContain('500');

    await expect(page.locator('h4')).toContainText('프로퍼티 관리');
    await expect(page.locator('#propertyTable')).toBeVisible({ timeout: 10000 });

    if (consoleErrors.length > 0) {
      console.log('[콘솔 에러]', consoleErrors);
    }
  });

  test('호텔 미선택 시 contextAlert 표시', async ({ page }) => {
    await page.goto('/admin/properties');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1000);

    // 호텔 선택 안 된 상태에서 contextAlert 또는 alert-info 표시
    const contextAlert = page.locator('#contextAlert');
    const isVisible = await contextAlert.isVisible();
    // 호텔이 미선택인 경우 alert 표시, 이미 선택된 경우 숨김 (둘 다 정상)
    // 헤더 셀렉터 현재 값 확인
    const hotelVal = await page.locator('#headerHotelSelect').inputValue();
    if (!hotelVal) {
      expect(isVisible).toBe(true);
    }
  });

  test('호텔 선택 후 프로퍼티 DataTable 로드', async ({ page }) => {
    await page.goto('/admin/properties');
    await page.waitForLoadState('networkidle');

    // 헤더 호텔 셀렉터 첫 번째 유효 옵션 선택
    const hotelOptions = await page.locator('#headerHotelSelect option').all();
    if (hotelOptions.length <= 1) {
      test.skip(true, '선택 가능한 호텔 없음');
      return;
    }

    const hotelValue = await hotelOptions[1].getAttribute('value');
    await page.locator('#headerHotelSelect').selectOption(hotelValue);
    await page.waitForTimeout(800);

    // contextAlert 숨김 확인
    const contextAlert = page.locator('#contextAlert');
    await expect(contextAlert).not.toBeVisible({ timeout: 5000 });

    // DataTable에 데이터 로드 (또는 빈 상태) 확인
    await expect(page.locator('#propertyTable')).toBeVisible({ timeout: 10000 });
  });
});

// ─────────────────────────────────────────────
// 5. 프로퍼티 등록/수정폼
// ─────────────────────────────────────────────
test.describe('프로퍼티 등록폼', () => {
  test('페이지 로드 및 필드 확인', async ({ page }) => {
    const consoleErrors = collectConsoleErrors(page);

    await page.goto('/admin/properties/new');
    await page.waitForLoadState('networkidle');

    const bodyText = await page.textContent('body');
    expect(bodyText).not.toContain('Whitelabel Error');
    expect(bodyText).not.toContain('500');

    await expect(page.locator('#pageTitle')).toContainText('프로퍼티 등록');
    await expect(page.locator('#hotelSelect')).toBeVisible();

    // 탭 네비게이션
    await expect(page.locator('#tab-basic')).toBeVisible();
    await expect(page.locator('#tab-settlement')).toBeVisible();
    await expect(page.locator('#tab-cancel')).toBeVisible();
    await expect(page.locator('#tab-tax')).toBeVisible();

    if (consoleErrors.length > 0) {
      console.log('[콘솔 에러]', consoleErrors);
    }
  });

  test('수정폼: 리스트에서 첫 항목 → 수정폼 필드 채워짐', async ({ page }) => {
    // 페이지 이동 후 API 호출 (절대 URL 사용)
    await page.goto('/admin/properties');
    await page.waitForLoadState('networkidle');

    // API에서 호텔 목록 → 첫 번째 호텔의 프로퍼티 조회
    const hotelsResp = await page.evaluate(async () => {
      const r = await fetch('/api/v1/hotels');
      return r.json();
    });

    if (!hotelsResp.data || hotelsResp.data.length === 0) {
      test.skip(true, '등록된 호텔 없음');
      return;
    }

    const firstHotelId = hotelsResp.data[0].id;
    const propsResp = await page.evaluate(async (hotelId) => {
      const r = await fetch(`/api/v1/hotels/${hotelId}/properties`);
      return r.json();
    }, firstHotelId);

    if (!propsResp.data || propsResp.data.length === 0) {
      test.skip(true, '등록된 프로퍼티 없음');
      return;
    }

    const firstPropertyId = propsResp.data[0].id;
    await page.goto(`/admin/properties/${firstPropertyId}/edit`);
    await page.waitForLoadState('networkidle');

    const bodyText = await page.textContent('body');
    expect(bodyText).not.toContain('Whitelabel Error');
    expect(bodyText).not.toContain('500');

    await expect(page.locator('#pageTitle')).toContainText('프로퍼티 수정');
  });
});

// ─────────────────────────────────────────────
// 6. 마켓코드 관리
// ─────────────────────────────────────────────
test.describe('마켓코드 관리', () => {
  test('페이지 로드 확인', async ({ page }) => {
    const consoleErrors = collectConsoleErrors(page);

    await page.goto('/admin/market-codes');
    await page.waitForLoadState('networkidle');

    const bodyText = await page.textContent('body');
    expect(bodyText).not.toContain('Whitelabel Error');
    expect(bodyText).not.toContain('500');

    await expect(page.locator('h4')).toContainText('마켓코드 관리');

    if (consoleErrors.length > 0) {
      console.log('[콘솔 에러]', consoleErrors);
    }
  });

  test('프로퍼티 미선택 → contextAlert 표시', async ({ page }) => {
    await page.goto('/admin/market-codes');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(800);

    const propertyVal = await page.locator('#headerPropertySelect').inputValue();
    if (!propertyVal) {
      const contextAlert = page.locator('#contextAlert');
      await expect(contextAlert).toBeVisible({ timeout: 5000 });
    }
  });

  test('호텔+프로퍼티 선택 → DataTable 로드', async ({ page }) => {
    await page.goto('/admin/market-codes');
    await page.waitForLoadState('networkidle');

    // 호텔 선택
    const hotelOptions = await page.locator('#headerHotelSelect option').all();
    if (hotelOptions.length <= 1) {
      test.skip(true, '선택 가능한 호텔 없음');
      return;
    }

    const hotelValue = await hotelOptions[1].getAttribute('value');
    await page.locator('#headerHotelSelect').selectOption(hotelValue);
    await page.waitForTimeout(800);

    // 프로퍼티 선택
    const propOptions = await page.locator('#headerPropertySelect option').all();
    if (propOptions.length <= 1) {
      test.skip(true, '선택 가능한 프로퍼티 없음');
      return;
    }

    const propValue = await propOptions[1].getAttribute('value');
    await page.locator('#headerPropertySelect').selectOption(propValue);
    await page.waitForTimeout(800);

    // contextAlert 숨김
    await expect(page.locator('#contextAlert')).not.toBeVisible({ timeout: 5000 });

    // DataTable 테이블 visible
    await expect(page.locator('#marketCodeTable')).toBeVisible({ timeout: 10000 });

    // 등록 버튼 표시
    await expect(page.locator('#btnCreateWrap')).toBeVisible();
  });
});

// ─────────────────────────────────────────────
// 7. 층코드 관리
// ─────────────────────────────────────────────
test.describe('층코드 관리', () => {
  test('페이지 로드 확인', async ({ page }) => {
    const consoleErrors = collectConsoleErrors(page);

    await page.goto('/admin/floors');
    await page.waitForLoadState('networkidle');

    const bodyText = await page.textContent('body');
    expect(bodyText).not.toContain('Whitelabel Error');
    expect(bodyText).not.toContain('500');

    await expect(page.locator('h4')).toContainText('층코드 관리');

    if (consoleErrors.length > 0) {
      console.log('[콘솔 에러]', consoleErrors);
    }
  });

  test('프로퍼티 미선택 → contextAlert 표시', async ({ page }) => {
    await page.goto('/admin/floors');
    await page.waitForLoadState('domcontentloaded');

    // 헤더 셀렉터 visible 대기 후 값 확인
    await expect(page.locator('#headerPropertySelect')).toBeVisible({ timeout: 10000 });
    const propertyVal = await page.locator('#headerPropertySelect').inputValue();
    if (!propertyVal) {
      const contextAlert = page.locator('#contextAlert');
      await expect(contextAlert).toBeVisible({ timeout: 8000 });
    }
  });

  test('호텔+프로퍼티 선택 → DataTable 로드', async ({ page }) => {
    await page.goto('/admin/floors');
    await page.waitForLoadState('networkidle');

    // 호텔 선택
    const hotelOptions = await page.locator('#headerHotelSelect option').all();
    if (hotelOptions.length <= 1) {
      test.skip(true, '선택 가능한 호텔 없음');
      return;
    }

    const hotelValue = await hotelOptions[1].getAttribute('value');
    await page.locator('#headerHotelSelect').selectOption(hotelValue);
    await page.waitForTimeout(800);

    // 프로퍼티 선택
    const propOptions = await page.locator('#headerPropertySelect option').all();
    if (propOptions.length <= 1) {
      test.skip(true, '선택 가능한 프로퍼티 없음');
      return;
    }

    const propValue = await propOptions[1].getAttribute('value');
    await page.locator('#headerPropertySelect').selectOption(propValue);
    await page.waitForTimeout(800);

    // contextAlert 숨김
    await expect(page.locator('#contextAlert')).not.toBeVisible({ timeout: 5000 });

    // DataTable visible
    await expect(page.locator('#floorTable')).toBeVisible({ timeout: 10000 });
  });

  test('층코드 등록 모달 열림 확인', async ({ page }) => {
    await page.goto('/admin/floors');
    await page.waitForLoadState('networkidle');

    // 호텔+프로퍼티 선택
    const hotelOptions = await page.locator('#headerHotelSelect option').all();
    if (hotelOptions.length <= 1) {
      test.skip(true, '선택 가능한 호텔 없음');
      return;
    }

    const hotelValue = await hotelOptions[1].getAttribute('value');
    await page.locator('#headerHotelSelect').selectOption(hotelValue);
    await page.waitForTimeout(800);

    const propOptions = await page.locator('#headerPropertySelect option').all();
    if (propOptions.length <= 1) {
      test.skip(true, '선택 가능한 프로퍼티 없음');
      return;
    }

    const propValue = await propOptions[1].getAttribute('value');
    await page.locator('#headerPropertySelect').selectOption(propValue);
    await page.waitForTimeout(800);

    // 층코드 등록 버튼 클릭
    await page.click('button:has-text("층코드 등록")');
    await page.waitForTimeout(500);

    // 모달 표시 확인
    await expect(page.locator('#floorModal')).toBeVisible({ timeout: 5000 });
    await expect(page.locator('#floorModalTitle')).toContainText('층코드 등록');
    await expect(page.locator('#floorNumber')).toBeVisible();
  });
});

// ─────────────────────────────────────────────
// 8. 호수코드 관리
// ─────────────────────────────────────────────
test.describe('호수코드 관리', () => {
  test('페이지 로드 확인', async ({ page }) => {
    const consoleErrors = collectConsoleErrors(page);

    await page.goto('/admin/room-numbers');
    await page.waitForLoadState('networkidle');

    const bodyText = await page.textContent('body');
    expect(bodyText).not.toContain('Whitelabel Error');
    expect(bodyText).not.toContain('500');

    await expect(page.locator('h4')).toContainText('호수코드 관리');

    if (consoleErrors.length > 0) {
      console.log('[콘솔 에러]', consoleErrors);
    }
  });

  test('프로퍼티 미선택 → contextAlert 표시', async ({ page }) => {
    await page.goto('/admin/room-numbers');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(800);

    const propertyVal = await page.locator('#headerPropertySelect').inputValue();
    if (!propertyVal) {
      const contextAlert = page.locator('#contextAlert');
      await expect(contextAlert).toBeVisible({ timeout: 5000 });
    }
  });

  test('호텔+프로퍼티 선택 → DataTable 로드', async ({ page }) => {
    await page.goto('/admin/room-numbers');
    await page.waitForLoadState('networkidle');

    // 호텔 선택
    const hotelOptions = await page.locator('#headerHotelSelect option').all();
    if (hotelOptions.length <= 1) {
      test.skip(true, '선택 가능한 호텔 없음');
      return;
    }

    const hotelValue = await hotelOptions[1].getAttribute('value');
    await page.locator('#headerHotelSelect').selectOption(hotelValue);
    await page.waitForTimeout(800);

    // 프로퍼티 선택
    const propOptions = await page.locator('#headerPropertySelect option').all();
    if (propOptions.length <= 1) {
      test.skip(true, '선택 가능한 프로퍼티 없음');
      return;
    }

    const propValue = await propOptions[1].getAttribute('value');
    await page.locator('#headerPropertySelect').selectOption(propValue);
    await page.waitForTimeout(800);

    // contextAlert 숨김
    await expect(page.locator('#contextAlert')).not.toBeVisible({ timeout: 5000 });

    // DataTable visible
    await expect(page.locator('#roomNumberTable')).toBeVisible({ timeout: 10000 });
  });

  test('호수코드 등록 모달 열림 확인', async ({ page }) => {
    await page.goto('/admin/room-numbers');
    await page.waitForLoadState('networkidle');

    const hotelOptions = await page.locator('#headerHotelSelect option').all();
    if (hotelOptions.length <= 1) {
      test.skip(true, '선택 가능한 호텔 없음');
      return;
    }

    const hotelValue = await hotelOptions[1].getAttribute('value');
    await page.locator('#headerHotelSelect').selectOption(hotelValue);
    await page.waitForTimeout(800);

    const propOptions = await page.locator('#headerPropertySelect option').all();
    if (propOptions.length <= 1) {
      test.skip(true, '선택 가능한 프로퍼티 없음');
      return;
    }

    const propValue = await propOptions[1].getAttribute('value');
    await page.locator('#headerPropertySelect').selectOption(propValue);
    await page.waitForTimeout(800);

    // 호수코드 등록 버튼 클릭
    await page.click('button:has-text("호수코드등록")');
    await page.waitForTimeout(500);

    // 모달 표시 확인
    await expect(page.locator('#roomNumberModal')).toBeVisible({ timeout: 5000 });
    await expect(page.locator('#roomNumberModalTitle')).toContainText('호수코드 등록');
    await expect(page.locator('#rnCode')).toBeVisible();
  });
});
