/**
 * Phase 4 보충: OTA 예약 상세에서 서비스 추가 버튼 표시 여부 검증
 * OTA 예약 ID: 252, 비OTA 예약 ID: 253
 */
const { chromium } = require('playwright');
const path = require('path');

const BASE_URL = 'http://localhost:8080';
const SCREENSHOT_DIR = path.join(__dirname, 'screenshots');
const TIMEOUT = 10000;

const results = [];

function log(scenario, status, detail = '') {
  const icon = status === 'PASS' ? '\u2705' : '\u274C';
  results.push({ scenario, status, detail });
  console.log(`${icon} ${status}: ${scenario}${detail ? ' - ' + detail : ''}`);
}

async function login(page) {
  await page.goto(`${BASE_URL}/login`);
  await page.fill('#username', 'admin');
  await page.fill('#password', 'holapms1!');
  await page.click('#loginBtn');
  await page.waitForURL(/\/admin/, { timeout: TIMEOUT });
}

async function setContext(page) {
  await page.evaluate(() => {
    sessionStorage.setItem('selectedHotelId', '6');
    sessionStorage.setItem('selectedPropertyId', '4');
  });
}

(async () => {
  const browser = await chromium.launch({ headless: true });

  const OTA_RESERVATION_ID = 252;
  const NON_OTA_RESERVATION_ID = 253;

  // ============================================
  // 시나리오 1: OTA 예약 상세 - 서비스 추가 버튼 숨김 확인
  // ============================================
  try {
    const context = await browser.newContext();
    const page = await context.newPage();
    page.setDefaultTimeout(TIMEOUT);

    await login(page);
    await setContext(page);

    await page.goto(`${BASE_URL}/admin/reservations/${OTA_RESERVATION_ID}`);
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(3000);

    // 상세 탭 중 "상세정보" 탭 클릭 (서브예약 정보가 여기 있을 수 있음)
    const detailTab = await page.$('a:has-text("상세정보"), button:has-text("상세정보"), [href*="detail-tab"]');
    if (detailTab) {
      await detailTab.click();
      await page.waitForTimeout(1500);
    }

    // 서비스 추가 버튼 검색
    const addServiceBtns = await page.$$('button');
    let serviceAddBtnFound = false;
    let serviceAddBtnVisible = false;
    let serviceAddBtnText = '';

    for (const btn of addServiceBtns) {
      const text = await btn.textContent();
      if (text.includes('서비스 추가') || text.includes('서비스') || text.includes('add-on') || text.includes('Add Service')) {
        serviceAddBtnFound = true;
        serviceAddBtnVisible = await btn.isVisible();
        serviceAddBtnText = text.trim();
        break;
      }
    }

    // OTA 관리 모드 토글 확인
    const otaToggle = await page.$('#otaManaged, [name="otaManaged"], input[type="checkbox"]');
    const otaInfo = await page.$('text=OTA 관리, text=OTA 정보, text=OTA 수정제한');
    const bodyText = await page.textContent('body');
    const hasOtaSection = bodyText.includes('OTA 관리') || bodyText.includes('OTA 수정제한') || bodyText.includes('OTA 정보');

    if (serviceAddBtnFound) {
      if (serviceAddBtnVisible) {
        log('OTA 예약: 서비스 추가 버튼 숨김', 'FAIL', `OTA 예약(${OTA_RESERVATION_ID})에서 서비스 추가 버튼이 표시됨: "${serviceAddBtnText}"`);
      } else {
        log('OTA 예약: 서비스 추가 버튼 숨김', 'PASS', `OTA 예약(${OTA_RESERVATION_ID})에서 서비스 추가 버튼 정상 숨김`);
      }
    } else {
      log('OTA 예약: 서비스 추가 버튼 숨김', 'PASS', `OTA 예약(${OTA_RESERVATION_ID}) 상세에서 서비스 추가 버튼 미존재 (정상)`);
    }

    if (hasOtaSection) {
      log('OTA 예약: OTA 정보 섹션 표시', 'PASS', 'OTA 관련 정보 섹션 확인됨');
    } else {
      log('OTA 예약: OTA 정보 섹션 표시', 'FAIL', 'OTA 관련 정보 섹션 미발견');
    }

    await page.screenshot({ path: path.join(SCREENSHOT_DIR, 'phase4-ota-detail.png'), fullPage: true });
    await context.close();
  } catch (err) {
    log('OTA 예약 상세 검증', 'FAIL', err.message);
  }

  // ============================================
  // 시나리오 2: 비OTA 예약 상세 - 기본 기능 확인
  // ============================================
  try {
    const context = await browser.newContext();
    const page = await context.newPage();
    page.setDefaultTimeout(TIMEOUT);

    await login(page);
    await setContext(page);

    await page.goto(`${BASE_URL}/admin/reservations/${NON_OTA_RESERVATION_ID}`);
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(3000);

    const bodyText = await page.textContent('body');
    const hasReservationInfo = bodyText.includes('예약') && (bodyText.includes('체크인') || bodyText.includes('예약번호'));

    if (hasReservationInfo) {
      log('비OTA 예약: 상세 폼 표시', 'PASS', `예약(${NON_OTA_RESERVATION_ID}) 상세 정보 정상 표시`);
    } else {
      log('비OTA 예약: 상세 폼 표시', 'FAIL', '예약 상세 정보 미발견');
    }

    // 탭 구조 확인
    const tabs = await page.$$('.nav-link, .nav-tabs .nav-item a');
    const tabTexts = [];
    for (const tab of tabs) {
      const text = await tab.textContent();
      if (text.trim()) tabTexts.push(text.trim());
    }

    if (tabTexts.length > 0) {
      log('비OTA 예약: 탭 구조', 'PASS', `탭 목록: ${tabTexts.join(', ')}`);
    }

    await page.screenshot({ path: path.join(SCREENSHOT_DIR, 'phase4-non-ota-detail.png'), fullPage: true });
    await context.close();
  } catch (err) {
    log('비OTA 예약 상세 검증', 'FAIL', err.message);
  }

  // ============================================
  // 시나리오 3: 예약 상세 - 결제정보 탭
  // ============================================
  try {
    const context = await browser.newContext();
    const page = await context.newPage();
    page.setDefaultTimeout(TIMEOUT);

    await login(page);
    await setContext(page);

    await page.goto(`${BASE_URL}/admin/reservations/${NON_OTA_RESERVATION_ID}`);
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(2000);

    // 결제정보 탭 클릭
    const paymentTab = await page.$('a:has-text("결제정보"), button:has-text("결제정보"), [href*="payment"]');
    if (paymentTab) {
      await paymentTab.click();
      await page.waitForTimeout(2000);

      const bodyText = await page.textContent('body');
      const hasPaymentInfo = bodyText.includes('결제') || bodyText.includes('금액') || bodyText.includes('요금') || bodyText.includes('카드');

      if (hasPaymentInfo) {
        log('예약 상세: 결제정보 탭', 'PASS', '결제정보 탭 내용 정상 표시');
      } else {
        log('예약 상세: 결제정보 탭', 'FAIL', '결제정보 내용 미발견');
      }
    } else {
      log('예약 상세: 결제정보 탭', 'FAIL', '결제정보 탭 버튼 미발견');
    }

    await page.screenshot({ path: path.join(SCREENSHOT_DIR, 'phase4-payment-tab.png'), fullPage: true });
    await context.close();
  } catch (err) {
    log('예약 상세: 결제정보 탭', 'FAIL', err.message);
  }

  await browser.close();

  // 종합 결과
  console.log('\n========================================');
  console.log('Phase 4 보충: OTA 검증 결과 요약');
  console.log('========================================');
  const passed = results.filter(r => r.status === 'PASS').length;
  const failed = results.filter(r => r.status === 'FAIL').length;
  console.log(`총 ${results.length}건 | PASS: ${passed} | FAIL: ${failed}`);
  console.log('========================================');
})();
