// @ts-check
const { test, expect } = require('@playwright/test');
const { login, selectHotelAndProperty, apiGet } = require('./helpers');

test.describe('01. 인증 및 기본 UI', () => {

  // TC-01: 정상 로그인
  test('TC-01: 정상 로그인 → /admin/dashboard 리다이렉트 + 사이드바 메뉴 표시', async ({ page }) => {
    await page.goto('/login');
    await page.fill('input[name="username"]', 'admin');
    await page.fill('input[name="password"]', 'holapms1!');
    await page.click('button[type="submit"]');
    await page.waitForURL('**/admin/dashboard', { timeout: 10000 });

    // URL 확인
    expect(page.url()).toContain('/admin/dashboard');

    // 사이드바 메뉴 존재 확인
    const sidebar = page.locator('nav.sidebar');
    await expect(sidebar).toBeVisible();

    // 대시보드 메뉴 링크 확인
    const dashboardLink = sidebar.locator('a:has-text("대시보드")');
    await expect(dashboardLink).toBeVisible();
  });

  // TC-02: 잘못된 비밀번호
  test('TC-02: 잘못된 비밀번호 → 에러 메시지 표시, /login 유지', async ({ page }) => {
    await page.goto('/login');
    await page.fill('input[name="username"]', 'admin');
    await page.fill('input[name="password"]', 'wrongpassword123');
    await page.click('button[type="submit"]');

    // 에러 메시지가 포함된 /login 페이지 대기
    await page.waitForURL('**/login?error=true', { timeout: 10000 });

    // 여전히 /login 페이지에 있는지 확인
    expect(page.url()).toContain('/login');

    // 에러 alert 표시 확인
    const errorAlert = page.locator('.alert-danger');
    await expect(errorAlert).toBeVisible();
    await expect(errorAlert).toContainText('아이디 또는 비밀번호가 일치하지 않습니다');
  });

  // TC-03: 미인증 상태 접근 차단
  test('TC-03: 미인증 상태로 /admin/dashboard 접근 → /login 리다이렉트', async ({ page }) => {
    // 로그인 없이 대시보드 직접 접근
    await page.goto('/admin/dashboard');

    // /login 페이지로 리다이렉트 확인
    await page.waitForURL('**/login', { timeout: 10000 });
    expect(page.url()).toContain('/login');
  });

  // TC-04: 대시보드 기본 표시
  test('TC-04: 대시보드 페이지 로드 + 호텔/프로퍼티 셀렉터 표시', async ({ page }) => {
    await login(page);

    // 대시보드 제목 확인
    const title = page.locator('h4:has-text("대시보드")');
    await expect(title).toBeVisible();

    // 호텔 셀렉터 표시 확인
    const hotelSelect = page.locator('#headerHotelSelect');
    await expect(hotelSelect).toBeVisible();

    // 프로퍼티 셀렉터 표시 확인
    const propertySelect = page.locator('#headerPropertySelect');
    await expect(propertySelect).toBeVisible();
  });

  // TC-05: 호텔/프로퍼티 셀렉터 컨텍스트
  test('TC-05: 호텔 선택 → 프로퍼티 목록 로드, 프로퍼티 선택 → sessionStorage 저장', async ({ page }) => {
    await login(page);

    // 호텔 셀렉터 대기 (옵션이 로드될 때까지)
    const hotelSelect = page.locator('#headerHotelSelect');
    await expect(hotelSelect).toBeVisible();

    // 호텔 옵션 로드 대기 (API 호출 후 옵션 추가됨)
    await page.waitForFunction(() => {
      const sel = document.getElementById('headerHotelSelect');
      return sel && sel.options.length > 1;
    }, { timeout: 10000 });

    // 첫 번째 호텔 선택
    const hotelOptions = await hotelSelect.locator('option').all();
    expect(hotelOptions.length).toBeGreaterThan(1);
    await hotelSelect.selectOption({ index: 1 });

    // 프로퍼티 목록 로드 대기
    const propertySelect = page.locator('#headerPropertySelect');
    await page.waitForFunction(() => {
      const sel = document.getElementById('headerPropertySelect');
      return sel && sel.options.length > 1;
    }, { timeout: 10000 });

    // 프로퍼티 옵션이 있는지 확인
    const propertyOptions = await propertySelect.locator('option').all();
    expect(propertyOptions.length).toBeGreaterThan(1);

    // 첫 번째 프로퍼티 선택
    await propertySelect.selectOption({ index: 1 });

    // sessionStorage에 저장 확인
    const storedPropertyId = await page.evaluate(() => {
      return sessionStorage.getItem('selectedPropertyId');
    });
    expect(storedPropertyId).not.toBeNull();
    expect(storedPropertyId).not.toBe('');
  });

  // TC-06: API 인증 체크
  test('TC-06: 미인증 API 호출 → 401, 인증 후 API 호출 → 200', async ({ page, request }) => {
    // 미인증 상태에서 API 호출 → 401
    const unauthResponse = await request.get('/api/v1/hotels/selector');
    expect(unauthResponse.status()).toBe(401);

    // 로그인 후 같은 API 호출 → 200
    await login(page);
    const authResult = await apiGet(page, '/api/v1/hotels/selector');
    expect(authResult.status).toBe(200);
  });

  // TC-07: 사이드바 접힘/펼침
  test('TC-07: 사이드바 토글 → 접힘(sidebar-collapsed) 확인 → 다시 펼침', async ({ page }) => {
    await login(page);

    // 토글 버튼 확인
    const toggleBtn = page.locator('#sidebarToggle');
    await expect(toggleBtn).toBeVisible();

    // 초기 상태: 접히지 않음
    const bodyInitial = await page.evaluate(() => {
      return document.body.classList.contains('sidebar-collapsed');
    });
    expect(bodyInitial).toBe(false);

    // 토글 클릭 → 접힘 확인
    await toggleBtn.click();
    const bodyCollapsed = await page.evaluate(() => {
      return document.body.classList.contains('sidebar-collapsed');
    });
    expect(bodyCollapsed).toBe(true);

    // 다시 토글 클릭 → 펼침 확인
    await toggleBtn.click();
    const bodyExpanded = await page.evaluate(() => {
      return document.body.classList.contains('sidebar-collapsed');
    });
    expect(bodyExpanded).toBe(false);
  });

});
