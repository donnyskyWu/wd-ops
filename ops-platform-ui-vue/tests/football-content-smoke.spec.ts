import { test, expect, type APIRequestContext, type Page } from '@playwright/test'

/**
 * TC-ID 映射（E2E 试点 · 见 docs/delivery/TESTCASES-M2-内容生产.md）
 * @smoke @p0 — Football Gate :5777 内容管理冒烟，非 TESTCASES 全量替代
 *
 * | Playwright ID       | 近似 TESTCASES              | 说明                          |
 * |---------------------|-----------------------------|-------------------------------|
 * | CONTENT-GATE-001    | （无 1:1）                  | 路由渲染 / Vite 白屏防护      |
 * | CONTENT-GATE-002    | （无 1:1）                  | 新增 drawer UX（遮罩/滑入）   |
 * | CONTENT-GATE-003    | TC-M2-005-07                | 查看 mode layoutHtml 排版     |
 * | CONTENT-GATE-004    | TC-M2-005-07                | 编辑 vs 查看截图 + 表格样式   |
 *
 * 证据归档：docs/delivery/e2e-artifacts/CONTENT-GATE-{date}/
 * 缺陷登记：docs/delivery/defects/DEF-{YYYYMMDD}-{序号}.md
 */

const ACCESS_STORE_KEY = 'yudao-vben-ele-5.5.9-dev-1.0.0-core-access'

async function seedFootballAuth(request: APIRequestContext, page: Page) {
  const loginRes = await request.post('http://localhost:48080/admin-api/system/auth/login', {
    headers: { 'tenant-id': '1', 'Content-Type': 'application/json' },
    data: { username: 'admin', password: 'admin123' },
    timeout: 60_000,
  })
  expect(loginRes.ok()).toBeTruthy()
  const loginBody = await loginRes.json()
  expect(loginBody.code).toBe(0)

  const permRes = await request.get('http://localhost:48080/admin-api/system/auth/get-permission-info', {
    headers: {
      'tenant-id': '1',
      Authorization: `Bearer ${loginBody.data.accessToken}`,
    },
    timeout: 60_000,
  })
  expect(permRes.ok()).toBeTruthy()
  const permBody = await permRes.json()
  expect(permBody.code).toBe(0)

  await page.addInitScript(
    ({ storeKey, accessToken, refreshToken, accessCodes, tenantId }) => {
      localStorage.setItem(
        storeKey,
        JSON.stringify({
          accessToken,
          refreshToken,
          accessCodes,
          tenantId,
          visitTenantId: tenantId,
        }),
      )
    },
    {
      storeKey: ACCESS_STORE_KEY,
      accessToken: loginBody.data.accessToken,
      refreshToken: loginBody.data.refreshToken,
      accessCodes: permBody.data.permissions ?? [],
      tenantId: 1,
    },
  )

  return {
    accessToken: loginBody.data.accessToken as string,
    authHeaders: {
      'tenant-id': '1',
      'X-Tenant-Id': '1',
      Authorization: `Bearer ${loginBody.data.accessToken}`,
    },
  }
}

async function openContentManagement(page: Page) {
  await page.goto('/ops/dashboard', { waitUntil: 'domcontentloaded', timeout: 60_000 })
  await page.waitForLoadState('networkidle', { timeout: 45_000 }).catch(() => {})

  await page.goto('/ops/production/content', { waitUntil: 'domcontentloaded', timeout: 60_000 })
  await page.waitForLoadState('networkidle', { timeout: 45_000 }).catch(() => {})
}

test.describe('Football Gate 内容管理 @smoke', () => {
  test('CONTENT-GATE-001: 内容管理页无 Vite 错误且路由渲染', async ({ page, request }) => {
    const pageErrors: string[] = []
    page.on('pageerror', (err) => {
      const msg = err.message
      if (msg.includes('Failed to resolve import') || msg.includes('Pre-transform error')) {
        pageErrors.push(msg)
      }
    })

    await seedFootballAuth(request, page)
    await openContentManagement(page)

    await expect(page).toHaveURL(/\/ops\/production\/content/)
    await expect(page).toHaveTitle(/内容管理/)

    await expect(page.getByText('未找到页面')).toHaveCount(0)

    const overlay = page.locator('vite-error-overlay, .vite-error-overlay')
    await expect(overlay).toHaveCount(0)

    expect(pageErrors, pageErrors.join('\n')).toHaveLength(0)

    // 白屏防护：页面须渲染筛选区与表格，而非仅 document.title
    await expect(page.locator('.content-page')).toBeVisible({ timeout: 15_000 })
    await expect(page.getByRole('button', { name: '新增内容' })).toBeVisible()
    await expect(page.locator('.el-table')).toBeVisible()
  })

  test('CONTENT-GATE-002: 新增内容 drawer 遮罩与滑入效果', async ({ page, request }) => {
    await seedFootballAuth(request, page)
    await openContentManagement(page)

    await page.getByRole('button', { name: '新增内容' }).click()

    const drawerOverlay = page.locator('.el-overlay.is-drawer').filter({ visible: true }).first()
    await expect(drawerOverlay).toBeVisible({ timeout: 15_000 })

    const drawer = drawerOverlay.locator('.el-drawer.open')
    await expect(drawer).toBeVisible()
    await expect(drawer).toHaveClass(/rtl/)
    await expect(drawer.getByText('新增内容')).toBeVisible()

    const overlayBg = await drawerOverlay.evaluate((el) => getComputedStyle(el).backgroundColor)
    expect(overlayBg).not.toBe('rgba(0, 0, 0, 0)')
    expect(overlayBg).not.toBe('transparent')
  })

  test('CONTENT-GATE-003: 查看模式保留 layoutHtml 富文本排版', async ({ page, request }) => {
    test.setTimeout(120_000)
    const CONTENT_TITLE_NEEDLE = '马维超-蓝鹰 VS 红狮队'
    const API_BASE = 'http://localhost:48080/admin-api'

    const { authHeaders } = await seedFootballAuth(request, page)

    const listRes = await request.get(`${API_BASE}/oa/content/list`, {
      headers: authHeaders,
      params: { pageNo: 1, pageSize: 10, title: '马维超' },
      timeout: 60_000,
    })
    expect(listRes.ok()).toBeTruthy()
    const listBody = await listRes.json()
    expect(listBody.code).toBe(0)
    const row = listBody.data?.list?.find((item: { title?: string }) =>
      item.title?.includes('马维超') && item.title?.includes('2026-07-25 20:30'),
    )
    expect(row, 'seed content not found').toBeTruthy()

    const detailRes = await request.get(`${API_BASE}/oa/content/${row.id}`, {
      headers: authHeaders,
      timeout: 60_000,
    })
    expect(detailRes.ok()).toBeTruthy()
    const detailBody = await detailRes.json()
    expect(detailBody.code).toBe(0)
    const layoutHtml: string = detailBody.data?.layoutHtml || ''
    expect(layoutHtml).toContain('layout-article')
    expect(layoutHtml).toMatch(/style=/)

    await openContentManagement(page)

    await page.locator('.table-search').getByLabel('标题').fill('马维超')
    await page.locator('.table-search').getByRole('button', { name: '搜索' }).click()
    await expect(page.locator('.el-table__row').filter({ hasText: '马维超' }).first()).toBeVisible({
      timeout: 30_000,
    })

    const targetRow = page.locator('.el-table__row').filter({ hasText: CONTENT_TITLE_NEEDLE }).first()
    await targetRow.getByRole('button', { name: '查看' }).click()

    const drawer = page.locator('.content-edit-drawer .el-drawer.open')
    await expect(drawer).toBeVisible({ timeout: 30_000 })
    await expect(drawer.getByText('查看内容')).toBeVisible()

    // Readonly view: separate 付费/免费 sections (not tabs)
    await expect(drawer.getByText('方案正文')).toHaveCount(0)
    await expect(drawer.locator('.body-content-tabs')).toHaveCount(0)
    await expect(drawer.getByRole('tab', { name: '免费内容' })).toHaveCount(0)
    await expect(drawer.getByRole('tab', { name: '付费内容' })).toHaveCount(0)
    await expect(drawer.locator('.el-form-item__label').filter({ hasText: /^付费内容$/ })).toBeVisible()
    await expect(drawer.locator('.el-form-item__label').filter({ hasText: /^免费内容$/ })).toBeVisible()

    const viewer = drawer.locator('.layout-viewer').first()
    await expect(viewer).toBeVisible({ timeout: 30_000 })
    await expect(viewer.locator('section.layout-article')).toBeVisible()
    await expect(viewer.locator('table')).toBeVisible()

    const viewInner = await viewer.innerHTML()
    expect(viewInner).toMatch(/style=/)
    expect(viewInner).toContain('<table')

    const headerCell = viewer.locator('th').first()
    await expect(headerCell).toBeVisible()
    const headerBg = await headerCell.evaluate((el) => getComputedStyle(el).backgroundColor)
    expect(headerBg).not.toBe('rgba(0, 0, 0, 0)')
    expect(headerBg).not.toBe('transparent')

    const bodyCell = viewer.locator('td').first()
    await expect(bodyCell).toBeVisible()
    const cellBorder = await bodyCell.evaluate((el) => getComputedStyle(el).borderTopWidth)
    expect(Number.parseFloat(cellBorder)).toBeGreaterThan(0)

    await page.keyboard.press('Escape')
    await expect(drawer).toHaveCount(0)

    await targetRow.getByRole('button', { name: '编辑' }).click()
    const editDrawer = page.locator('.content-edit-drawer .el-drawer.open')
    await expect(editDrawer).toBeVisible({ timeout: 30_000 })

    const editorHtml = await editDrawer.locator('.ProseMirror').first().innerHTML()
    expect(editorHtml).toMatch(/style=|<table|layout-article/i)
    expect(viewInner.length).toBeGreaterThan(100)
  })

  test('CONTENT-GATE-004: 编辑vs查看截图对比', async ({ page, request }) => {
    test.setTimeout(180_000)
    const timestamp = Date.now()
    const editScreenshotPath = `test-results/content-edit-${timestamp}.png`
    const viewScreenshotPath = `test-results/content-view-${timestamp}.png`

    await seedFootballAuth(request, page)
    await openContentManagement(page)
    await expect(page.locator('.content-page')).toBeVisible({ timeout: 15_000 })

    await page.locator('.table-search').getByLabel('标题').fill('马维超')
    await page.locator('.table-search').getByRole('button', { name: '搜索' }).click()
    const targetRow = page
      .locator('.el-table__row')
      .filter({ hasText: /马维超|蓝鹰 VS 红狮队/ })
      .first()
    await expect(targetRow).toBeVisible({ timeout: 30_000 })

    // --- Edit drawer ---
    await targetRow.getByRole('button', { name: '编辑' }).click()
    const editDrawer = page.getByRole('dialog', { name: '编辑内容' })
    await expect(editDrawer).toBeVisible({ timeout: 30_000 })
    await expect(editDrawer.getByLabel('标题')).toHaveValue(/马维超|蓝鹰 VS 红狮队/, { timeout: 60_000 })
    await expect(editDrawer.locator('.ProseMirror').first()).toBeVisible({ timeout: 60_000 })
    await expect(editDrawer.locator('.body-content-tabs')).toBeVisible({ timeout: 15_000 })

    const editPaidTableHtml = await editDrawer.locator('.ProseMirror table').first().innerHTML().catch(() => '')
    await editDrawer.screenshot({ path: editScreenshotPath })

    await editDrawer.getByRole('button', { name: '关闭此对话框' }).click()
    await expect(editDrawer).toHaveCount(0)

    // --- View drawer (readonly) ---
    await targetRow.getByRole('button', { name: '查看' }).click()
    const viewDrawer = page.getByRole('dialog', { name: '查看内容' })
    await expect(viewDrawer).toBeVisible({ timeout: 30_000 })
    await expect(viewDrawer.getByLabel('标题')).toHaveValue(/马维超|蓝鹰 VS 红狮队/, { timeout: 60_000 })

    await expect(viewDrawer.locator('.el-form-item__label').filter({ hasText: /^付费内容$/ })).toBeVisible({
      timeout: 30_000,
    })
    await expect(viewDrawer.locator('.el-form-item__label').filter({ hasText: /^免费内容$/ })).toBeVisible({
      timeout: 15_000,
    })
    await expect(viewDrawer.locator('.body-content-tabs')).toHaveCount(0)

    const paidViewer = viewDrawer.locator('.layout-viewer').first()
    const freeViewer = viewDrawer.locator('.layout-viewer').nth(1)
    await expect(paidViewer).toBeVisible({ timeout: 30_000 })
    await expect(paidViewer.locator('table')).toBeVisible()

    const headerCell = paidViewer.locator('th').first()
    await expect(headerCell).toBeVisible()
    const headerBg = await headerCell.evaluate((el) => getComputedStyle(el).backgroundColor)
    expect(headerBg, `th background should not be transparent, got ${headerBg}`).not.toBe('rgba(0, 0, 0, 0)')
    expect(headerBg).not.toBe('transparent')

    const bodyCell = paidViewer.locator('td').first()
    await expect(bodyCell).toBeVisible()
    const cellBorder = await bodyCell.evaluate((el) => getComputedStyle(el).borderTopWidth)
    expect(Number.parseFloat(cellBorder), `td border width should be > 0, got ${cellBorder}`).toBeGreaterThan(0)

    if (editPaidTableHtml) {
      const viewPaidTableHtml = await paidViewer.locator('table').first().innerHTML()
      expect(viewPaidTableHtml.length, 'view paid table should have content').toBeGreaterThan(0)
    }

    await viewDrawer.screenshot({ path: viewScreenshotPath })
    await expect(freeViewer).toBeVisible()
  })
})
