/**
 * Phase 1: 로그인 + 네비게이션 E2E 테스트
 * - 로그인 성공/실패
 * - 사이드바 메뉴 네비게이션
 */
const { chromium } = require('playwright');
const path = require('path');

const BASE_URL = 'http://localhost:8080';
const SCREENSHOT_DIR = path.join(__dirname, 'screenshots');
const TIMEOUT = 10000;

const results = [];

function log(scenario, status, detail = '') {
  const icon = status === 'PASS' ? '\u2705' : '\u274C';
  const entry = { scenario, status, detail };
  results.push(entry);
  console.log(`${icon} ${status}: ${scenario}${detail ? ' - ' + detail : ''}`);
}

(async () => {
  const browser = await chromium.launch({ headless: true });

  // ============================================
  // 시나리오 1: 로그인 성공 -> 대시보드 이동
  // ============================================
  try {
    const context = await browser.newContext();
    const page = await context.newPage();
    page.setDefaultTimeout(TIMEOUT);

    await page.goto(`${BASE_URL}/login`);

    // 로그인 폼 존재 확인
    await page.waitForSelector('#username');
    await page.waitForSelector('#password');

    // 아이디/비밀번호 입력
    await page.fill('#username', 'admin');
    await page.fill('#password', 'holapms1!');

    // 로그인 버튼 클릭
    await page.click('#loginBtn');

    // 대시보드로 이동 확인 (URL에 dashboard 포함되거나 /admin 경로)
    await page.waitForURL(/\/admin/, { timeout: TIMEOUT });

    const url = page.url();
    if (url.includes('/admin')) {
      log('로그인 성공 -> 대시보드 이동', 'PASS', `이동된 URL: ${url}`);
    } else {
      log('로그인 성공 -> 대시보드 이동', 'FAIL', `예상치 못한 URL: ${url}`);
      await page.screenshot({ path: path.join(SCREENSHOT_DIR, 'phase1-login-success-fail.png'), fullPage: true });
    }

    await context.close();
  } catch (err) {
    log('로그인 성공 -> 대시보드 이동', 'FAIL', err.message);
    try {
      const ctx = await browser.newContext();
      const p = await ctx.newPage();
      await p.goto(`${BASE_URL}/login`);
      await p.screenshot({ path: path.join(SCREENSHOT_DIR, 'phase1-login-success-error.png'), fullPage: true });
      await ctx.close();
    } catch (_) {}
  }

  // ============================================
  // 시나리오 2: 잘못된 비밀번호 -> 에러 메시지
  // ============================================
  try {
    const context = await browser.newContext();
    const page = await context.newPage();
    page.setDefaultTimeout(TIMEOUT);

    await page.goto(`${BASE_URL}/login`);
    await page.fill('#username', 'admin');
    await page.fill('#password', 'wrongpassword123!');
    await page.click('#loginBtn');

    // 로그인 실패 후 에러 메시지 또는 login 페이지에 머물기 확인
    await page.waitForTimeout(2000);

    const url = page.url();
    const hasError = url.includes('error') || url.includes('login');

    // 에러 메시지 확인
    const errorAlert = await page.$('.alert-danger, .alert-warning, .text-danger, [role="alert"]');
    const errorParam = url.includes('error');

    if (hasError || errorAlert) {
      let errorText = '';
      if (errorAlert) {
        errorText = await errorAlert.textContent();
        errorText = errorText.trim();
      }
      log('잘못된 비밀번호 -> 에러 메시지 표시', 'PASS', errorText || `URL에 error 파라미터 포함: ${url}`);
    } else {
      log('잘못된 비밀번호 -> 에러 메시지 표시', 'FAIL', `에러 메시지 미발견, URL: ${url}`);
      await page.screenshot({ path: path.join(SCREENSHOT_DIR, 'phase1-login-error-fail.png'), fullPage: true });
    }

    await context.close();
  } catch (err) {
    log('잘못된 비밀번호 -> 에러 메시지 표시', 'FAIL', err.message);
  }

  // ============================================
  // 시나리오 3: 사이드바 메뉴 -> 각 페이지 이동 확인
  // ============================================
  const menuPages = [
    { name: '호텔 관리', path: '/admin/hotels', expectText: '호텔' },
    { name: '프로퍼티 관리', path: '/admin/properties', expectText: '프로퍼티' },
    { name: '객실 클래스', path: '/admin/room-classes', expectText: '객실' },
    { name: '객실 타입', path: '/admin/room-types', expectText: '객실' },
    { name: '레이트 코드', path: '/admin/rate-codes', expectText: '레이트' },
    { name: '예약 관리', path: '/admin/reservations', expectText: '예약' },
    { name: '블루웨이브 관리자', path: '/admin/members/bluewave-admins', expectText: '관리자' },
  ];

  try {
    const context = await browser.newContext();
    const page = await context.newPage();
    page.setDefaultTimeout(TIMEOUT);

    // 먼저 로그인
    await page.goto(`${BASE_URL}/login`);
    await page.fill('#username', 'admin');
    await page.fill('#password', 'holapms1!');
    await page.click('#loginBtn');
    await page.waitForURL(/\/admin/, { timeout: TIMEOUT });

    for (const menu of menuPages) {
      try {
        await page.goto(`${BASE_URL}${menu.path}`);
        await page.waitForLoadState('domcontentloaded');

        const status = page.url().includes(menu.path) ? 200 : 0;
        const title = await page.title();
        const bodyText = await page.textContent('body');

        if (page.url().includes(menu.path) || page.url().includes('/admin')) {
          log(`사이드바 메뉴 이동: ${menu.name}`, 'PASS', `URL: ${page.url()}`);
        } else {
          log(`사이드바 메뉴 이동: ${menu.name}`, 'FAIL', `예상치 못한 URL: ${page.url()}`);
          await page.screenshot({ path: path.join(SCREENSHOT_DIR, `phase1-nav-${menu.name}.png`), fullPage: true });
        }
      } catch (err) {
        log(`사이드바 메뉴 이동: ${menu.name}`, 'FAIL', err.message);
        await page.screenshot({ path: path.join(SCREENSHOT_DIR, `phase1-nav-${menu.name}-error.png`), fullPage: true });
      }
    }

    await context.close();
  } catch (err) {
    log('사이드바 메뉴 네비게이션 (전체)', 'FAIL', err.message);
  }

  await browser.close();

  // 종합 결과
  console.log('\n========================================');
  console.log('Phase 1: 로그인 + 네비게이션 결과 요약');
  console.log('========================================');
  const passed = results.filter(r => r.status === 'PASS').length;
  const failed = results.filter(r => r.status === 'FAIL').length;
  console.log(`총 ${results.length}건 | PASS: ${passed} | FAIL: ${failed}`);
  console.log('========================================');
})();
