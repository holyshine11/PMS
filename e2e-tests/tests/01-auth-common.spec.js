// @ts-check
const { test, expect } = require('@playwright/test');
const { collectConsoleErrors } = require('./helpers');

// =====================================================================
// 헬퍼: 직접 로그인 (storageState 의존 없이)
// =====================================================================
async function loginDirectly(page) {
  await page.goto('/login');
  await page.fill('input[name="username"]', 'admin');
  await page.fill('input[name="password"]', 'holapms1!');
  await page.click('button[type="submit"]');
  await page.waitForURL('**/admin/dashboard', { timeout: 15000 });
}

// =====================================================================
// 1. 로그인 테스트
// - storageState 없이 직접 로그인 동작을 검증하므로 별도 context 사용
// =====================================================================
test.describe('1. 로그인 테스트', () => {

  test('1-1. 올바른 자격증명으로 로그인 성공 → /admin/dashboard 이동', async ({ browser }) => {
    const context = await browser.newContext();
    const page = await context.newPage();
    const consoleErrors = collectConsoleErrors(page);

    try {
      await page.goto('/login');
      await expect(page).toHaveTitle(/로그인/);

      await page.fill('input[name="username"]', 'admin');
      await page.fill('input[name="password"]', 'holapms1!');
      await page.click('button[type="submit"]');

      // 로그인 성공 → 대시보드로 이동
      await page.waitForURL('**/admin/dashboard', { timeout: 15000 });
      await expect(page).toHaveURL(/\/admin\/dashboard/);
      await expect(page).toHaveTitle(/대시보드/);

      if (consoleErrors.length > 0) {
        console.warn('[콘솔 에러]', consoleErrors);
      }
    } finally {
      await context.close();
    }
  });

  test('1-2. 잘못된 비밀번호로 로그인 실패 → 에러 메시지 표시', async ({ browser }) => {
    const context = await browser.newContext();
    const page = await context.newPage();

    try {
      await page.goto('/login');
      await page.fill('input[name="username"]', 'admin');
      await page.fill('input[name="password"]', 'wrongpassword123!');
      await page.click('button[type="submit"]');

      // 로그인 페이지에 머물러야 함 (error 파라미터 포함)
      await page.waitForURL(/\/login/, { timeout: 10000 });
      await expect(page).toHaveURL(/\/login/);

      // 에러 메시지 표시 확인
      const errorAlert = page.locator('.alert-danger');
      await expect(errorAlert).toBeVisible({ timeout: 5000 });
      await expect(errorAlert).toContainText('아이디 또는 비밀번호가 일치하지 않습니다');
    } finally {
      await context.close();
    }
  });

  test('1-3. 빈 필드로 로그인 시도 → 브라우저 내장 또는 JS 검증으로 차단', async ({ browser }) => {
    // 빈 필드 제출 시 세 가지 경우가 가능:
    // A) 브라우저 HTML5 required 검증 → "Please fill out this field" 팝업 표시, 페이지 이동 없음
    // B) JS 커스텀 검증 → is-invalid 클래스 추가, e.preventDefault()로 차단
    // C) 서버로 전송 → /login?error 리다이렉트 → .alert-danger 표시
    // 공통: 로그인 페이지에서 벗어나지 않아야 함
    const context = await browser.newContext();
    const page = await context.newPage();

    try {
      await page.goto('/login');

      // username, password 모두 비워두고 제출
      await page.locator('input[name="username"]').fill('');
      await page.locator('input[name="password"]').fill('');
      await page.locator('button[type="submit"]').click();

      // 잠시 대기
      await page.waitForTimeout(2000);

      // 핵심 검증: 로그인 페이지에 머물러야 함 (대시보드로 가면 안 됨)
      await expect(page).not.toHaveURL(/\/admin\/dashboard/);
      await expect(page).toHaveURL(/\/login/);

      // 추가 검증: 브라우저 HTML5 required 검증 or JS 검증 or 서버 에러 중 하나 작동
      // HTML5 required: input validity 체크
      const usernameValidity = await page.locator('#username').evaluate(el => el.validity.valid);
      const hasInvalidClass = await page.locator('#username').evaluate(el => el.classList.contains('is-invalid'));
      const hasAlertDanger = await page.locator('.alert-danger').isVisible().catch(() => false);

      // 세 가지 중 하나 이상이 검증을 차단했음 확인
      const validationWorked = !usernameValidity || hasInvalidClass || hasAlertDanger;
      expect(validationWorked).toBe(true);
    } finally {
      await context.close();
    }
  });

});

// =====================================================================
// 2. 대시보드 테스트
// - 1번 테스트가 별도 browser context를 사용하므로 auth.json 세션 영향 없음
// - 단, 안전을 위해 beforeEach에서 직접 로그인 처리
// =====================================================================
test.describe('2. 대시보드 테스트', () => {

  test.beforeEach(async ({ page }) => {
    // auth.json 세션이 만료될 수 있으므로 직접 로그인
    await page.goto('/admin/dashboard');
    // 로그인 페이지로 리다이렉트되면 직접 로그인
    if (page.url().includes('/login')) {
      await loginDirectly(page);
    }
  });

  test('2-1. /admin/dashboard 접근 시 페이지 정상 로드', async ({ page }) => {
    const consoleErrors = collectConsoleErrors(page);

    await page.waitForLoadState('networkidle');

    await expect(page).toHaveTitle(/대시보드/);
    await expect(page).toHaveURL(/\/admin\/dashboard/);

    // 에러 페이지가 아닌지 확인
    const body = await page.textContent('body');
    expect(body).not.toContain('Whitelabel Error');
    expect(body).not.toMatch(/HTTP Status 500|HTTP Status 404/);

    // 페이지 타이틀 h4 존재 확인
    await expect(page.locator('h4').first()).toBeVisible();

    if (consoleErrors.length > 0) {
      console.warn('[콘솔 에러]', consoleErrors);
    }
  });

  test('2-2. SUPER_ADMIN 사이드바 메뉴 전체 표시 확인', async ({ page }) => {
    await page.waitForLoadState('networkidle');

    // 사이드바 존재 확인 (nav 태그 + sidebar 클래스)
    const sidebar = page.locator('nav.sidebar').first();
    await expect(sidebar).toBeVisible({ timeout: 10000 });

    // 주요 메뉴 링크 href 확인
    const expectedHrefs = [
      '/admin/hotels',
      '/admin/properties',
      '/admin/members/bluewave-admins',
      '/admin/members/hotel-admins',
      '/admin/members/property-admins',
      '/admin/roles/hotel-admins',
      '/admin/room-classes',
      '/admin/room-types',
      '/admin/rate-codes',
      '/admin/promotion-codes',
      '/admin/reservations',
      '/admin/reservation-channels',
    ];

    for (const href of expectedHrefs) {
      const link = page.locator(`a[href="${href}"]`);
      await expect(link).toBeVisible({ timeout: 5000 });
    }
  });

  test('2-3. 헤더 호텔/프로퍼티 셀렉터 표시 확인', async ({ page }) => {
    await page.waitForLoadState('networkidle');

    // 헤더 호텔 셀렉터
    const hotelSelect = page.locator('#headerHotelSelect');
    await expect(hotelSelect).toBeVisible({ timeout: 5000 });

    // 헤더 프로퍼티 셀렉터
    const propertySelect = page.locator('#headerPropertySelect');
    await expect(propertySelect).toBeVisible({ timeout: 5000 });
  });

});

// =====================================================================
// 3. 컨텍스트 전환 테스트
// =====================================================================
test.describe('3. 컨텍스트 전환 테스트', () => {

  test.beforeEach(async ({ page }) => {
    // auth.json 세션 만료 대비 직접 로그인
    await page.goto('/admin/dashboard');
    if (page.url().includes('/login')) {
      await loginDirectly(page);
    }
  });

  test('3-1. 호텔 셀렉터에 옵션 로드 확인', async ({ page }) => {
    await page.waitForLoadState('networkidle');

    const hotelSelect = page.locator('#headerHotelSelect');
    await expect(hotelSelect).toBeVisible();

    // 호텔 셀렉터 옵션이 로드될 때까지 대기 (JS Ajax 완료 후)
    await expect(async () => {
      const options = await hotelSelect.locator('option[value]:not([value=""])').all();
      expect(options.length).toBeGreaterThan(0);
    }).toPass({ timeout: 10000 });

    // 빈 옵션 + 최소 1개 이상의 호텔 옵션 확인
    const allOptions = await hotelSelect.locator('option').all();
    expect(allOptions.length).toBeGreaterThan(1);
  });

  test('3-2. 호텔 선택 → 프로퍼티 셀렉터 옵션 로드 확인', async ({ page }) => {
    await page.waitForLoadState('networkidle');

    const hotelSelect = page.locator('#headerHotelSelect');

    // 호텔 옵션 로드 대기
    await expect(async () => {
      const opts = await hotelSelect.locator('option[value]:not([value=""])').all();
      expect(opts.length).toBeGreaterThan(0);
    }).toPass({ timeout: 10000 });

    // 첫 번째 비-빈 호텔 선택
    const hotelOptions = await hotelSelect.locator('option[value]:not([value=""])').all();
    const firstHotelValue = await hotelOptions[0].getAttribute('value');

    // 프로퍼티 API 응답 대기를 위해 이벤트를 먼저 등록
    const propertyApiPromise = page.waitForResponse(
      resp => resp.url().includes('/api/v1/properties/selector'),
      { timeout: 10000 }
    );
    await hotelSelect.selectOption(firstHotelValue);
    const propertyResp = await propertyApiPromise;
    expect(propertyResp.status()).toBe(200);

    // 프로퍼티 셀렉터에 옵션 로드 확인
    const propertySelect = page.locator('#headerPropertySelect');
    await expect(async () => {
      const opts = await propertySelect.locator('option[value]:not([value=""])').all();
      expect(opts.length).toBeGreaterThan(0);
    }).toPass({ timeout: 5000 });
  });

  test('3-3. 프로퍼티 선택 → sessionStorage에 저장 확인', async ({ page }) => {
    await page.waitForLoadState('networkidle');

    const hotelSelect = page.locator('#headerHotelSelect');

    // 호텔 옵션 로드 대기
    await expect(async () => {
      const opts = await hotelSelect.locator('option[value]:not([value=""])').all();
      expect(opts.length).toBeGreaterThan(0);
    }).toPass({ timeout: 10000 });

    // 첫 번째 호텔 선택
    const hotelOptions = await hotelSelect.locator('option[value]:not([value=""])').all();
    const firstHotelValue = await hotelOptions[0].getAttribute('value');

    const propertyApiPromise = page.waitForResponse(
      resp => resp.url().includes('/api/v1/properties/selector'),
      { timeout: 10000 }
    );
    await hotelSelect.selectOption(firstHotelValue);
    await propertyApiPromise;
    await page.waitForTimeout(500);

    // 첫 번째 프로퍼티 선택
    const propertySelect = page.locator('#headerPropertySelect');
    await expect(async () => {
      const opts = await propertySelect.locator('option[value]:not([value=""])').all();
      expect(opts.length).toBeGreaterThan(0);
    }).toPass({ timeout: 5000 });

    const propertyOptions = await propertySelect.locator('option[value]:not([value=""])').all();
    const firstPropertyValue = await propertyOptions[0].getAttribute('value');

    await propertySelect.selectOption(firstPropertyValue);
    await page.waitForTimeout(800);

    // sessionStorage에 hotelId, propertyId 저장 확인
    const sessionData = await page.evaluate(() => {
      const result = {};
      Object.keys(sessionStorage).forEach(k => { result[k] = sessionStorage.getItem(k); });
      return result;
    });

    // sessionStorage에 property 관련 키가 존재해야 함
    const hasPropertyContext = Object.entries(sessionData).some(
      ([key, val]) =>
        key.toLowerCase().includes('property') ||
        val === firstPropertyValue ||
        val === String(firstPropertyValue)
    );
    expect(hasPropertyContext).toBe(true);
  });

  test('3-4. 페이지 새로고침 후 컨텍스트 유지 확인', async ({ page }) => {
    await page.waitForLoadState('networkidle');

    const hotelSelect = page.locator('#headerHotelSelect');

    // 호텔 옵션 로드 대기
    await expect(async () => {
      const opts = await hotelSelect.locator('option[value]:not([value=""])').all();
      expect(opts.length).toBeGreaterThan(0);
    }).toPass({ timeout: 10000 });

    // 호텔 선택
    const hotelOptions = await hotelSelect.locator('option[value]:not([value=""])').all();
    const firstHotelValue = await hotelOptions[0].getAttribute('value');

    const propertyApiPromise = page.waitForResponse(
      resp => resp.url().includes('/api/v1/properties/selector'),
      { timeout: 10000 }
    );
    await hotelSelect.selectOption(firstHotelValue);
    await propertyApiPromise;
    await page.waitForTimeout(500);

    // 프로퍼티 선택
    const propertySelect = page.locator('#headerPropertySelect');
    await expect(async () => {
      const opts = await propertySelect.locator('option[value]:not([value=""])').all();
      expect(opts.length).toBeGreaterThan(0);
    }).toPass({ timeout: 5000 });

    const propertyOptions = await propertySelect.locator('option[value]:not([value=""])').all();
    const firstPropertyValue = await propertyOptions[0].getAttribute('value');
    await propertySelect.selectOption(firstPropertyValue);
    await page.waitForTimeout(800);

    // 새로고침 전 sessionStorage 값 저장
    const beforeRefresh = await page.evaluate(() => {
      const result = {};
      Object.keys(sessionStorage).forEach(k => { result[k] = sessionStorage.getItem(k); });
      return result;
    });

    // 컨텍스트가 저장된 키가 있어야 함
    expect(Object.keys(beforeRefresh).length).toBeGreaterThan(0);

    // 페이지 새로고침
    await page.reload();
    await page.waitForLoadState('networkidle');

    // 새로고침 후 sessionStorage 값 유지 확인
    // sessionStorage는 탭이 살아있는 동안 유지됨
    const afterRefresh = await page.evaluate(() => {
      const result = {};
      Object.keys(sessionStorage).forEach(k => { result[k] = sessionStorage.getItem(k); });
      return result;
    });

    for (const [key, val] of Object.entries(beforeRefresh)) {
      expect(afterRefresh[key]).toBe(val);
    }
  });

});

// =====================================================================
// 4. 마이프로필 테스트
// =====================================================================
test.describe('4. 마이프로필 테스트', () => {

  test.beforeEach(async ({ page }) => {
    await page.goto('/admin/my-profile');
    if (page.url().includes('/login')) {
      await loginDirectly(page);
      await page.goto('/admin/my-profile');
    }
  });

  test('4-1. /admin/my-profile 접근 시 폼 표시 확인', async ({ page }) => {
    const consoleErrors = collectConsoleErrors(page);

    await page.waitForLoadState('networkidle');
    await expect(page).not.toHaveURL(/\/login/);

    // 프로필 폼 존재 확인
    const profileForm = page.locator('#profileForm');
    await expect(profileForm).toBeVisible({ timeout: 5000 });

    // 주요 필드 존재 확인
    await expect(page.locator('#loginId')).toBeVisible();
    await expect(page.locator('#memberNumber')).toBeVisible();
    await expect(page.locator('#accountType')).toBeVisible();
    await expect(page.locator('#userName')).toBeVisible();

    if (consoleErrors.length > 0) {
      console.warn('[콘솔 에러]', consoleErrors);
    }
  });

  test('4-2. 현재 로그인 사용자 정보 표시 확인 (admin)', async ({ page }) => {
    const consoleErrors = collectConsoleErrors(page);

    await page.waitForLoadState('networkidle');

    // JS가 API를 호출하여 채워주는 값을 기다림
    // loginId 필드에 'admin' 텍스트가 들어올 때까지 대기
    await expect(async () => {
      const loginIdText = await page.locator('#loginId').textContent();
      expect(loginIdText?.trim()).toBe('admin');
    }).toPass({ timeout: 10000 });

    // accountType 또는 권한 정보 표시 확인 (기본값 '-' 이 아닌 실제 값)
    await expect(async () => {
      const accountTypeText = await page.locator('#accountType').textContent();
      expect(accountTypeText?.trim()).not.toBe('-');
      expect(accountTypeText?.trim()).not.toBe('');
    }).toPass({ timeout: 5000 });

    if (consoleErrors.length > 0) {
      console.warn('[콘솔 에러]', consoleErrors);
    }
  });

});

// =====================================================================
// 5. 로그아웃 테스트
// =====================================================================
test.describe('5. 로그아웃 테스트', () => {

  test('5-1. 로그아웃 → /login 페이지 이동 확인', async ({ browser }) => {
    // 별도 컨텍스트로 직접 로그인 후 로그아웃
    const context = await browser.newContext();
    const page = await context.newPage();

    try {
      await loginDirectly(page);
      await page.waitForLoadState('networkidle');

      // 로그아웃은 form POST /logout
      const logoutBtn = page.locator('form[action="/logout"] button[type="submit"]');
      await expect(logoutBtn).toBeVisible({ timeout: 5000 });
      await logoutBtn.click();

      // /login 페이지로 이동 확인
      await page.waitForURL(/\/login/, { timeout: 10000 });
      await expect(page).toHaveURL(/\/login/);
      await expect(page).toHaveTitle(/로그인/);
    } finally {
      await context.close();
    }
  });

  test('5-2. 로그아웃 후 대시보드 접근 시 로그인 페이지로 리다이렉트', async ({ browser }) => {
    const context = await browser.newContext();
    const page = await context.newPage();

    try {
      // 로그인
      await loginDirectly(page);

      // 로그아웃
      const logoutBtn = page.locator('form[action="/logout"] button[type="submit"]');
      await logoutBtn.click();
      await page.waitForURL(/\/login/, { timeout: 10000 });

      // 로그아웃 후 대시보드 직접 접근 시도
      await page.goto('/admin/dashboard');
      await page.waitForURL(/\/login/, { timeout: 10000 });
      await expect(page).toHaveURL(/\/login/);
    } finally {
      await context.close();
    }
  });

});
