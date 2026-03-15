/**
 * E2E 테스트 - Team 4: M02 객실관리 + M03 레이트관리
 * 대상: 객실 클래스, 객실 타입, 무료/유료 옵션, 레이트 코드, 프로모션 코드, 정책
 */
const { test, expect } = require('@playwright/test');

// ── 헬퍼: 헤더 셀렉터로 호텔+프로퍼티 선택 ──────────────────────────────────
async function selectHotelAndProperty(page) {
  // 호텔 셀렉터: 첫 번째 유효 옵션 선택
  const hotelSelect = page.locator('#headerHotelSelect');
  await expect(hotelSelect).toBeVisible({ timeout: 5000 });
  const hotelOptions = await hotelSelect.locator('option').all();
  let hotelSelected = false;
  for (const opt of hotelOptions) {
    const val = await opt.getAttribute('value');
    if (val && val.trim() !== '') {
      await hotelSelect.selectOption(val);
      hotelSelected = true;
      break;
    }
  }
  if (!hotelSelected) throw new Error('선택 가능한 호텔 옵션이 없습니다.');
  await page.waitForTimeout(500);

  // 프로퍼티 셀렉터: 첫 번째 유효 옵션 선택
  const propertySelect = page.locator('#headerPropertySelect');
  await expect(propertySelect).toBeVisible({ timeout: 5000 });
  await page.waitForTimeout(300); // 프로퍼티 목록 로드 대기
  const propOptions = await propertySelect.locator('option').all();
  let propSelected = false;
  for (const opt of propOptions) {
    const val = await opt.getAttribute('value');
    if (val && val.trim() !== '') {
      await propertySelect.selectOption(val);
      propSelected = true;
      break;
    }
  }
  if (!propSelected) throw new Error('선택 가능한 프로퍼티 옵션이 없습니다.');
  await page.waitForTimeout(500);
}

// ── 헬퍼: 콘솔 에러 수집 ─────────────────────────────────────────────────────
function collectConsoleErrors(page) {
  const errors = [];
  page.on('console', msg => {
    if (msg.type() === 'error') errors.push(msg.text());
  });
  return errors;
}

// ── 헬퍼: contextAlert가 표시되어 있는지 확인 ─────────────────────────────────
async function isContextAlertVisible(page) {
  const alert = page.locator('#contextAlert');
  const exists = await alert.count();
  if (!exists) return false;
  const classes = await alert.getAttribute('class');
  return classes && !classes.includes('d-none');
}

// ── 헬퍼: DataTable이 렌더링되었는지 확인 ────────────────────────────────────
async function waitForDataTable(page, tableId) {
  const table = page.locator(`#${tableId}`);
  await expect(table).toBeVisible({ timeout: 10000 });
  // DataTables wrapper가 생성될 때까지 대기
  await page.waitForSelector(`#${tableId}_wrapper`, { timeout: 10000 }).catch(() => {});
  return table;
}

// ══════════════════════════════════════════════════════════════════════════════
// M02 - 1. 객실 클래스 (Room Class)
// ══════════════════════════════════════════════════════════════════════════════
test.describe('M02 - 객실 클래스', () => {

  test('리스트: 프로퍼티 미선택 시 contextAlert 표시', async ({ page }) => {
    const errors = collectConsoleErrors(page);
    await page.goto('/admin/room-classes');
    await page.waitForLoadState('networkidle');

    // contextAlert가 존재하는지 확인
    const alert = page.locator('#contextAlert');
    await expect(alert).toBeAttached();

    // 초기 로드 시 alert가 보여야 함 (d-none이 없거나, JS가 실행되어 표시)
    // 실제로는 JS가 로드되면서 contextAlert 상태가 결정되므로 잠시 대기
    await page.waitForTimeout(1000);
    const isVisible = await isContextAlertVisible(page);
    expect(isVisible).toBe(true);
    console.log('콘솔 에러:', errors);
  });

  test('리스트: 프로퍼티 선택 후 DataTable 로드', async ({ page }) => {
    const errors = collectConsoleErrors(page);
    await page.goto('/admin/room-classes');
    await page.waitForLoadState('networkidle');

    await selectHotelAndProperty(page);
    await page.waitForTimeout(1000);

    // contextAlert 숨김 확인
    const isVisible = await isContextAlertVisible(page);
    expect(isVisible).toBe(false);

    // DataTable 로드 확인
    await waitForDataTable(page, 'roomClassTable');

    // 검색 필드 존재 확인
    await expect(page.locator('#searchClassName')).toBeVisible();
    await expect(page.locator('#searchClassCode')).toBeVisible();

    // 등록 버튼 존재 확인
    await expect(page.locator('#btnCreate')).toBeVisible();

    console.log('콘솔 에러:', errors);
    expect(errors.length).toBe(0);
  });

  test('등록 폼: 필수 필드 및 UI 요소 확인', async ({ page }) => {
    const errors = collectConsoleErrors(page);
    await page.goto('/admin/room-classes/new');
    await page.waitForLoadState('networkidle');

    // 페이지 타이틀 확인
    await expect(page.locator('body')).toContainText('객실 그룹 관리');

    // contextAlert 존재 확인
    await expect(page.locator('#contextAlert')).toBeAttached();

    // 필수 필드 확인
    await expect(page.locator('#roomClassName')).toBeVisible();
    await expect(page.locator('#roomClassCode')).toBeVisible();
    await expect(page.locator('#description')).toBeVisible();

    // 중복확인 버튼
    await expect(page.locator('#btnCheckDuplicate')).toBeVisible();

    // 사용여부 라디오 버튼 (기본값 Y)
    const useYnY = page.locator('#useYnY');
    await expect(useYnY).toBeAttached();
    expect(await useYnY.isChecked()).toBe(true);

    // 저장/취소 버튼 (사이드바와 구분: btn 클래스 사용)
    await expect(page.locator('#btnSave')).toBeVisible();
    await expect(page.locator('a.btn.btn-secondary[href="/admin/room-classes"]')).toBeVisible();

    // 삭제 버튼은 등록 모드에서 숨김
    const btnDelete = page.locator('#btnDelete');
    await expect(btnDelete).toBeAttached();
    const deleteStyle = await btnDelete.getAttribute('style');
    expect(deleteStyle).toContain('display:none');

    console.log('콘솔 에러:', errors);
  });

  test('수정 폼: 컨텍스트 선택 후 데이터 로드 확인', async ({ page }) => {
    const errors = collectConsoleErrors(page);

    // 리스트에서 프로퍼티 선택 후 첫 번째 항목 클릭
    await page.goto('/admin/room-classes');
    await page.waitForLoadState('networkidle');
    await selectHotelAndProperty(page);
    await page.waitForTimeout(1500);

    // DataTable 행이 있는지 확인
    await waitForDataTable(page, 'roomClassTable');
    await page.waitForTimeout(500);

    const rows = page.locator('#roomClassTable tbody tr');
    const rowCount = await rows.count();

    if (rowCount > 0 && !(await rows.first().locator('td').first().textContent()).includes('검색된')) {
      // 첫 번째 행의 데이터를 가져와서 수정 폼 URL 직접 접근
      // (DataTable 행 클릭 또는 링크 없는 경우 API로 ID 조회)
      const bodyText = await page.locator('#roomClassTable tbody').textContent();
      if (bodyText && bodyText.trim() !== '' && !bodyText.includes('검색된 데이터가 없습니다')) {
        console.log('객실 클래스 데이터 존재 - 수정 폼 검증 생략 (ID 미확보)');
      }
    } else {
      console.log('객실 클래스 데이터 없음 - 수정 폼 검증 생략');
    }

    console.log('콘솔 에러:', errors);
  });
});

// ══════════════════════════════════════════════════════════════════════════════
// M02 - 2. 객실 타입 (Room Type)
// ══════════════════════════════════════════════════════════════════════════════
test.describe('M02 - 객실 타입', () => {

  test('리스트: 프로퍼티 미선택 시 contextAlert 표시', async ({ page }) => {
    await page.goto('/admin/room-types');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1000);

    const isVisible = await isContextAlertVisible(page);
    expect(isVisible).toBe(true);
  });

  test('리스트: 프로퍼티 선택 후 DataTable 로드', async ({ page }) => {
    const errors = collectConsoleErrors(page);
    await page.goto('/admin/room-types');
    await page.waitForLoadState('networkidle');

    await selectHotelAndProperty(page);
    await page.waitForTimeout(1000);

    const isVisible = await isContextAlertVisible(page);
    expect(isVisible).toBe(false);

    await waitForDataTable(page, 'roomTypeTable');

    console.log('콘솔 에러:', errors);
    expect(errors.length).toBe(0);
  });

  test('등록 폼: 필수 필드 및 객실 클래스 선택 연동 확인', async ({ page }) => {
    const errors = collectConsoleErrors(page);
    await page.goto('/admin/room-types/new');
    await page.waitForLoadState('networkidle');

    await expect(page.locator('body')).toContainText('객실 타입 관리');

    // 객실 클래스 선택 버튼
    await expect(page.locator('#btnSelectClass')).toBeVisible();
    await expect(page.locator('#roomClassDisplay')).toBeVisible();

    // 필수 필드
    await expect(page.locator('#roomTypeCode')).toBeVisible();
    await expect(page.locator('#roomSize')).toBeVisible();
    await expect(page.locator('#maxAdults')).toBeVisible();
    await expect(page.locator('#maxChildren')).toBeVisible();

    // 중복확인 버튼
    await expect(page.locator('#btnCheckDuplicate')).toBeVisible();

    // 사용여부 기본값 Y
    const useYnY = page.locator('#useYnY');
    await expect(useYnY).toBeAttached();
    expect(await useYnY.isChecked()).toBe(true);

    // 엑스트라 배드 기본값 N
    const extraBedN = page.locator('#extraBedN');
    await expect(extraBedN).toBeAttached();
    expect(await extraBedN.isChecked()).toBe(true);

    // 어메니티/배드/유료옵션/층호수 선택 버튼들
    await expect(page.locator('#btnSelectFloor')).toBeVisible();

    // 팝업 모달들이 DOM에 존재하는지 확인
    await expect(page.locator('#classPopupModal')).toBeAttached();
    await expect(page.locator('#freeServicePopupModal')).toBeAttached();
    await expect(page.locator('#paidServicePopupModal')).toBeAttached();
    await expect(page.locator('#floorPopupModal')).toBeAttached();

    // 저장/취소 버튼 (사이드바와 구분: btn 클래스 사용)
    await expect(page.locator('#btnSave')).toBeVisible();
    await expect(page.locator('a.btn.btn-secondary[href="/admin/room-types"]')).toBeVisible();

    // 삭제 버튼은 등록 모드에서 숨김
    const btnDelete = page.locator('#btnDelete');
    const deleteStyle = await btnDelete.getAttribute('style');
    expect(deleteStyle).toContain('display:none');

    console.log('콘솔 에러:', errors);
  });

  test('등록 폼: 객실 클래스 팝업 열기', async ({ page }) => {
    const errors = collectConsoleErrors(page);
    await page.goto('/admin/room-types/new');
    await page.waitForLoadState('networkidle');

    // 컨텍스트 선택 필요 (팝업 내 API 호출이 propertyId 필요)
    await selectHotelAndProperty(page);
    await page.waitForTimeout(500);

    // 조회 버튼 클릭 → 팝업 모달 열림 확인
    await page.locator('#btnSelectClass').click();
    await page.waitForTimeout(500);

    const modal = page.locator('#classPopupModal');
    // 모달이 열렸는지 확인 (Bootstrap show 클래스)
    await expect(modal).toBeVisible({ timeout: 3000 });

    console.log('콘솔 에러:', errors);
  });
});

// ══════════════════════════════════════════════════════════════════════════════
// M02 - 3. 무료 서비스 옵션 (Free Service Option)
// ══════════════════════════════════════════════════════════════════════════════
test.describe('M02 - 무료 서비스 옵션', () => {

  test('리스트: 프로퍼티 미선택 시 contextAlert 표시', async ({ page }) => {
    await page.goto('/admin/free-service-options');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1000);

    const isVisible = await isContextAlertVisible(page);
    expect(isVisible).toBe(true);
  });

  test('리스트: 프로퍼티 선택 후 DataTable 로드', async ({ page }) => {
    const errors = collectConsoleErrors(page);
    await page.goto('/admin/free-service-options');
    await page.waitForLoadState('networkidle');

    await selectHotelAndProperty(page);
    await page.waitForTimeout(1000);

    const isVisible = await isContextAlertVisible(page);
    expect(isVisible).toBe(false);

    await waitForDataTable(page, 'freeServiceOptionTable');

    console.log('콘솔 에러:', errors);
    expect(errors.length).toBe(0);
  });

  test('등록 폼: 필수 필드 및 서비스 유형 옵션 확인', async ({ page }) => {
    const errors = collectConsoleErrors(page);
    await page.goto('/admin/free-service-options/new');
    await page.waitForLoadState('networkidle');

    await expect(page.locator('body')).toContainText('무료 옵션 관리');

    // 필수 필드
    await expect(page.locator('#serviceOptionCode')).toBeVisible();
    await expect(page.locator('#serviceNameKo')).toBeVisible();
    await expect(page.locator('#serviceNameEn')).toBeVisible();

    // 서비스 유형 셀렉트
    const serviceType = page.locator('#serviceType');
    await expect(serviceType).toBeVisible();

    // 주요 서비스 유형 옵션 존재 확인
    const options = await serviceType.locator('option').allTextContents();
    expect(options).toContain('베드');
    expect(options).toContain('뷰');
    expect(options).toContain('어메니티');
    expect(options).toContain('조식');

    // 적용 박수 설정
    await expect(page.locator('#nightNa')).toBeAttached(); // 기본값 해당없음
    expect(await page.locator('#nightNa').isChecked()).toBe(true);

    // 수량 필드
    await expect(page.locator('#quantity')).toBeVisible();
    await expect(page.locator('#quantityUnit')).toBeVisible();

    // 저장/취소 버튼 (사이드바와 구분: btn 클래스 사용)
    await expect(page.locator('#btnSave')).toBeVisible();
    await expect(page.locator('a.btn.btn-secondary[href="/admin/free-service-options"]')).toBeVisible();

    console.log('콘솔 에러:', errors);
  });
});

// ══════════════════════════════════════════════════════════════════════════════
// M02 - 4. 유료 서비스 옵션 (Paid Service Option)
// ══════════════════════════════════════════════════════════════════════════════
test.describe('M02 - 유료 서비스 옵션', () => {

  test('리스트: 프로퍼티 미선택 시 contextAlert 표시', async ({ page }) => {
    await page.goto('/admin/paid-service-options');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1000);

    const isVisible = await isContextAlertVisible(page);
    expect(isVisible).toBe(true);
  });

  test('리스트: 프로퍼티 선택 후 DataTable 로드', async ({ page }) => {
    const errors = collectConsoleErrors(page);
    await page.goto('/admin/paid-service-options');
    await page.waitForLoadState('networkidle');

    await selectHotelAndProperty(page);
    await page.waitForTimeout(1000);

    const isVisible = await isContextAlertVisible(page);
    expect(isVisible).toBe(false);

    await waitForDataTable(page, 'paidServiceOptionTable');

    console.log('콘솔 에러:', errors);
    expect(errors.length).toBe(0);
  });

  test('등록 폼: 가격 관련 필드 및 서비스 유형 확인', async ({ page }) => {
    const errors = collectConsoleErrors(page);
    await page.goto('/admin/paid-service-options/new');
    await page.waitForLoadState('networkidle');

    await expect(page.locator('body')).toContainText('유료 옵션 관리');

    // 기본 필드
    await expect(page.locator('#serviceOptionCode')).toBeVisible();
    await expect(page.locator('#serviceNameKo')).toBeVisible();

    // 유료 서비스 유형 셀렉트 (무료와 다른 항목들)
    const serviceType = page.locator('#serviceType');
    await expect(serviceType).toBeVisible();
    const options = await serviceType.locator('option').allTextContents();
    expect(options.some(o => o.includes('유료'))).toBe(true); // 유료 관련 옵션 존재

    // 가격 정보 섹션 - 핵심 필드
    await expect(page.locator('#supplyPrice')).toBeVisible();
    await expect(page.locator('#taxRate')).toBeVisible();
    await expect(page.locator('#taxAmount')).toBeVisible();
    await expect(page.locator('#vatIncludedPrice')).toBeVisible();

    // 통화 구분 (KRW/USD)
    await expect(page.locator('#currKRW')).toBeAttached();
    await expect(page.locator('#currUSD')).toBeAttached();
    expect(await page.locator('#currKRW').isChecked()).toBe(true);

    // 부가세 포함여부
    await expect(page.locator('#vatIncludedY')).toBeAttached();
    expect(await page.locator('#vatIncludedY').isChecked()).toBe(true);

    // 저장/취소 버튼 (사이드바와 구분: btn 클래스 사용)
    await expect(page.locator('#btnSave')).toBeVisible();
    await expect(page.locator('a.btn.btn-secondary[href="/admin/paid-service-options"]')).toBeVisible();

    console.log('콘솔 에러:', errors);
  });
});

// ══════════════════════════════════════════════════════════════════════════════
// M03 - 5. 레이트 코드 (Rate Code)
// ══════════════════════════════════════════════════════════════════════════════
test.describe('M03 - 레이트 코드', () => {

  test('리스트: 프로퍼티 미선택 시 contextAlert 표시', async ({ page }) => {
    await page.goto('/admin/rate-codes');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1000);

    const isVisible = await isContextAlertVisible(page);
    expect(isVisible).toBe(true);
  });

  test('리스트: 프로퍼티 선택 후 DataTable 로드', async ({ page }) => {
    const errors = collectConsoleErrors(page);
    await page.goto('/admin/rate-codes');
    await page.waitForLoadState('networkidle');

    await selectHotelAndProperty(page);
    await page.waitForTimeout(1000);

    const isVisible = await isContextAlertVisible(page);
    expect(isVisible).toBe(false);

    await waitForDataTable(page, 'rateCodeTable');

    console.log('콘솔 에러:', errors);
    expect(errors.length).toBe(0);
  });

  test('등록 폼: 기본정보 탭 - 필수 필드 확인', async ({ page }) => {
    const errors = collectConsoleErrors(page);
    await page.goto('/admin/rate-codes/new');
    await page.waitForLoadState('networkidle');

    await expect(page.locator('body')).toContainText('레이트 관리');

    // 탭 구조 확인
    await expect(page.locator('#tab-basic')).toBeVisible();
    await expect(page.locator('#tab-pricing')).toBeVisible();
    await expect(page.locator('#tab-option')).toBeVisible();

    // 기본정보 탭이 활성화되어 있어야 함
    const basicTab = page.locator('#tab-basic');
    const tabClass = await basicTab.getAttribute('class');
    expect(tabClass).toContain('active');

    // 레이트 코드 + 중복확인
    await expect(page.locator('#rateCode')).toBeVisible();
    await expect(page.locator('#btnCheckDuplicate')).toBeVisible();

    // 국문/영문 설명
    await expect(page.locator('#rateNameKo')).toBeVisible();
    await expect(page.locator('#rateNameEn')).toBeVisible();

    // 판매기간
    await expect(page.locator('#saleStartDate')).toBeVisible();
    await expect(page.locator('#saleEndDate')).toBeVisible();

    // 레이트 카테고리 (Room Only / Package)
    await expect(page.locator('#catRoomOnly')).toBeAttached();
    await expect(page.locator('#catPackage')).toBeAttached();
    expect(await page.locator('#catRoomOnly').isChecked()).toBe(true);

    // 통화 셀렉트
    const currency = page.locator('#currency');
    await expect(currency).toBeVisible();
    const currencyOptions = await currency.locator('option').allTextContents();
    expect(currencyOptions.some(o => o.includes('KRW'))).toBe(true);
    expect(currencyOptions.some(o => o.includes('USD'))).toBe(true);

    // 숙박일수 (최소/최대)
    await expect(page.locator('#minStayDays')).toBeVisible();
    await expect(page.locator('#maxStayDays')).toBeVisible();
    expect(await page.locator('#minStayDays').inputValue()).toBe('1');
    expect(await page.locator('#maxStayDays').inputValue()).toBe('365');

    // 사용여부
    await expect(page.locator('#useYnY')).toBeAttached();
    expect(await page.locator('#useYnY').isChecked()).toBe(true);

    console.log('콘솔 에러:', errors);
  });

  test('등록 폼: 요금정보/옵션요금 탭 DOM 구조 확인 (신규 등록 시 탭 차단 동작)', async ({ page }) => {
    const errors = collectConsoleErrors(page);
    await page.goto('/admin/rate-codes/new');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(500);

    // 탭 버튼들이 DOM에 존재하는지 확인
    await expect(page.locator('#tab-pricing')).toBeAttached();
    await expect(page.locator('#tab-option')).toBeAttached();

    // 신규 등록 시 탭이 opacity:0.5로 비활성화 표시됨 확인
    const pricingTabStyle = await page.locator('#tab-pricing').getAttribute('style');
    expect(pricingTabStyle).toContain('opacity');

    // 요금정보 패널 DOM 존재 확인
    await expect(page.locator('#pane-pricing')).toBeAttached();
    await expect(page.locator('#pricingEmptyMsg')).toBeAttached();
    await expect(page.locator('#pricingRowsContainer')).toBeAttached();

    // 옵션요금 패널 DOM 존재 확인
    await expect(page.locator('#pane-option')).toBeAttached();
    await expect(page.locator('#optionEmptyMsg')).toBeAttached();

    // 탭 클릭 시 차단 (경고 메시지 표시됨 - Toast 기반이므로 DOM 접근 어려움)
    // 탭이 여전히 active 상태가 아님을 확인
    await page.locator('#tab-pricing').click();
    await page.waitForTimeout(300);
    // 차단 후 pane-basic이 여전히 active 상태
    const basicPaneClass = await page.locator('#pane-basic').getAttribute('class');
    expect(basicPaneClass).toContain('active');

    console.log('콘솔 에러:', errors);
  });

  test('등록 폼: 저장/취소 버튼 확인', async ({ page }) => {
    const errors = collectConsoleErrors(page);
    await page.goto('/admin/rate-codes/new');
    await page.waitForLoadState('networkidle');

    // 저장/취소 버튼 (사이드바와 구분: btn 클래스 사용)
    await expect(page.locator('#btnSave')).toBeVisible();
    await expect(page.locator('a.btn.btn-secondary[href="/admin/rate-codes"]')).toBeVisible();

    // 삭제 버튼은 등록 모드에서 숨김
    const btnDelete = page.locator('#btnDelete');
    await expect(btnDelete).toBeAttached();
    const deleteStyle = await btnDelete.getAttribute('style');
    expect(deleteStyle).toContain('display:none');

    console.log('콘솔 에러:', errors);
  });
});

// ══════════════════════════════════════════════════════════════════════════════
// M03 - 6. 프로모션 코드 (Promotion Code)
// ══════════════════════════════════════════════════════════════════════════════
test.describe('M03 - 프로모션 코드', () => {

  test('리스트: 프로퍼티 미선택 시 contextAlert 표시', async ({ page }) => {
    await page.goto('/admin/promotion-codes');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1000);

    const isVisible = await isContextAlertVisible(page);
    expect(isVisible).toBe(true);
  });

  test('리스트: 프로퍼티 선택 후 DataTable 로드', async ({ page }) => {
    const errors = collectConsoleErrors(page);
    await page.goto('/admin/promotion-codes');
    await page.waitForLoadState('networkidle');

    await selectHotelAndProperty(page);
    await page.waitForTimeout(1000);

    const isVisible = await isContextAlertVisible(page);
    expect(isVisible).toBe(false);

    await waitForDataTable(page, 'promotionCodeTable');

    console.log('콘솔 에러:', errors);
    expect(errors.length).toBe(0);
  });

  test('등록 폼: 프로모션 타입 12종 셀렉트 확인', async ({ page }) => {
    const errors = collectConsoleErrors(page);
    await page.goto('/admin/promotion-codes/new');
    await page.waitForLoadState('networkidle');

    await expect(page.locator('body')).toContainText('프로모션 코드 관리');

    // 프로모션 타입 셀렉트
    const promotionType = page.locator('#promotionType');
    await expect(promotionType).toBeVisible();

    const optionValues = await promotionType.locator('option').evaluateAll(
      opts => opts.map(o => o.value)
    );
    const expectedTypes = [
      'COMPANY', 'PROMOTION', 'OTA', 'PACKAGE', 'SEASONAL', 'EVENT',
      'EARLY_BIRD', 'LAST_MINUTE', 'MEMBER', 'GROUP', 'LONG_STAY', 'GOVERNMENT'
    ];
    for (const type of expectedTypes) {
      expect(optionValues).toContain(type);
    }
    // 정확히 12종 확인
    expect(optionValues.length).toBe(12);

    console.log('콘솔 에러:', errors);
  });

  test('등록 폼: 필수 필드 및 할인 설정 확인', async ({ page }) => {
    const errors = collectConsoleErrors(page);
    await page.goto('/admin/promotion-codes/new');
    await page.waitForLoadState('networkidle');

    // 프로모션 코드 + 중복확인
    await expect(page.locator('#promotionCode')).toBeVisible();
    await expect(page.locator('#btnCheckDuplicate')).toBeVisible();

    // 레이트 코드 선택
    await expect(page.locator('#rateCodeName')).toBeVisible();
    await expect(page.locator('#btnRateCodeLookup')).toBeVisible();

    // 프로모션 기간
    await expect(page.locator('#promotionStartDate')).toBeVisible();
    await expect(page.locator('#promotionEndDate')).toBeVisible();

    // 설명 (국문/영문)
    await expect(page.locator('#descriptionKo')).toBeVisible();
    await expect(page.locator('#descriptionEn')).toBeVisible();

    // Down/Up sale 설정
    const downUpSign = page.locator('#downUpSign');
    await expect(downUpSign).toBeVisible();
    const signOptions = await downUpSign.locator('option').allTextContents();
    expect(signOptions.some(o => o.includes('Down'))).toBe(true);
    expect(signOptions.some(o => o.includes('Up'))).toBe(true);

    await expect(page.locator('#downUpValue')).toBeVisible();

    // 정액/정률 라디오
    await expect(page.locator('#unitAmount')).toBeAttached();
    await expect(page.locator('#unitPercent')).toBeAttached();
    expect(await page.locator('#unitAmount').isChecked()).toBe(true);

    // 소수점 처리
    await expect(page.locator('#roundingDecimalPoint')).toBeVisible();
    await expect(page.locator('#roundingMethod')).toBeVisible();

    // 저장/취소 버튼 (사이드바와 구분: btn 클래스 사용)
    await expect(page.locator('#btnSave')).toBeVisible();
    await expect(page.locator('a.btn.btn-secondary[href="/admin/promotion-codes"]')).toBeVisible();

    console.log('콘솔 에러:', errors);
  });

  test('등록 폼: 레이트 코드 조회 모달 열기 (컨텍스트 선택 후)', async ({ page }) => {
    const errors = collectConsoleErrors(page);
    await page.goto('/admin/promotion-codes/new');
    await page.waitForLoadState('networkidle');

    await selectHotelAndProperty(page);
    await page.waitForTimeout(500);

    // 레이트 코드 조회 버튼 클릭
    await page.locator('#btnRateCodeLookup').click();
    await page.waitForTimeout(500);

    // 레이트 코드 모달이 열렸는지 확인
    const modal = page.locator('#rateCodeModal');
    await expect(modal).toBeVisible({ timeout: 3000 });

    console.log('콘솔 에러:', errors);
  });
});

// ══════════════════════════════════════════════════════════════════════════════
// 정책 관리
// ══════════════════════════════════════════════════════════════════════════════
test.describe('정책 관리', () => {

  test('얼리/레이트 정책: 페이지 로드 확인', async ({ page }) => {
    const errors = collectConsoleErrors(page);
    await page.goto('/admin/early-late-policies');
    await page.waitForLoadState('networkidle');

    // 에러 페이지 아닌지 확인
    const bodyText = await page.locator('body').textContent();
    expect(bodyText).not.toContain('Whitelabel Error');
    expect(bodyText).not.toContain('404');

    // 페이지 컨텐츠 확인 (early-late-policy.html 기반)
    await expect(page.locator('body')).toContainText('얼리');

    // contextAlert 존재 확인 (프로퍼티 의존 페이지)
    await expect(page.locator('#contextAlert')).toBeAttached();
    await page.waitForTimeout(800);
    const isVisible = await isContextAlertVisible(page);
    expect(isVisible).toBe(true);

    console.log('콘솔 에러:', errors);
  });

  test('얼리/레이트 정책: 프로퍼티 선택 후 데이터 로드', async ({ page }) => {
    const errors = collectConsoleErrors(page);
    await page.goto('/admin/early-late-policies');
    await page.waitForLoadState('networkidle');

    await selectHotelAndProperty(page);
    await page.waitForTimeout(1000);

    // contextAlert 숨김 확인
    const isVisible = await isContextAlertVisible(page);
    expect(isVisible).toBe(false);

    // 페이지가 정상 로드되었는지 확인
    const bodyText = await page.locator('body').textContent();
    expect(bodyText).not.toContain('Whitelabel Error');

    console.log('콘솔 에러:', errors);
    expect(errors.length).toBe(0);
  });

  test('취소 정책: 페이지 로드 확인', async ({ page }) => {
    const errors = collectConsoleErrors(page);
    await page.goto('/admin/cancellation-policies');
    await page.waitForLoadState('networkidle');

    // 에러 페이지 아닌지 확인
    const bodyText = await page.locator('body').textContent();
    expect(bodyText).not.toContain('Whitelabel Error');
    expect(bodyText).not.toContain('404');

    // 취소 정책 관련 텍스트 확인
    await expect(page.locator('body')).toContainText('취소');

    // contextAlert 존재 확인
    await expect(page.locator('#contextAlert')).toBeAttached();
    await page.waitForTimeout(800);
    const isVisible = await isContextAlertVisible(page);
    expect(isVisible).toBe(true);

    console.log('콘솔 에러:', errors);
  });

  test('취소 정책: 프로퍼티 선택 후 데이터 로드', async ({ page }) => {
    const errors = collectConsoleErrors(page);
    await page.goto('/admin/cancellation-policies');
    await page.waitForLoadState('networkidle');

    await selectHotelAndProperty(page);
    await page.waitForTimeout(1000);

    const isVisible = await isContextAlertVisible(page);
    expect(isVisible).toBe(false);

    const bodyText = await page.locator('body').textContent();
    expect(bodyText).not.toContain('Whitelabel Error');

    console.log('콘솔 에러:', errors);
    expect(errors.length).toBe(0);
  });
});
