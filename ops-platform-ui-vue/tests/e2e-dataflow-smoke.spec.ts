import { test, expect } from '@playwright/test'
import { E2E, isE2eSeedReady, fetchE2eFollowerCount } from './helpers/e2e-seed'

/**
 * E2E dataflow smoke tests (V57 seed · TRACE=E2E-DF-20260611)
 * Requires: backend localhost:8080 + Flyway V57/V58 applied
 */
test.describe('E2E dataflow smoke @smoke @e2e-df', () => {
  test.beforeAll(async ({ request }) => {
    const ready = await isE2eSeedReady(request)
    test.skip(!ready, 'E2E seed not available — start backend with V57/V58 applied')
  })

  test('E2E-01 内部作品分析: E2E works visible with read count', async ({ page }) => {
    await page.goto('/internal-content')
    await page.waitForLoadState('networkidle')

    const keyword = page.locator('input[placeholder*="内容标题"]')
    await keyword.fill(E2E.workTitlePrefix)
    await page.locator('button:has-text("搜索")').click()
    await page.waitForLoadState('networkidle')

    await expect(page.locator('.el-table')).toContainText(E2E.workTitlePrefix)
    const bodyText = await page.locator('.internal-content-page').innerText()
    expect(bodyText).toMatch(/5[,，]?200[,，]?000|520万/)
  })

  test('E2E-02 账号分析: account 91001 follower count', async ({ page, request }) => {
    const followerCount = await fetchE2eFollowerCount(request)
    expect(followerCount).toBeGreaterThanOrEqual(E2E.followerMin)

    await page.goto('/account-analysis')
    await page.waitForLoadState('networkidle')

    await page.locator('.el-tabs__item', { hasText: '视频号' }).click()
    await page.waitForLoadState('networkidle')

    const searchInput = page.locator('input[placeholder*="账号名称"]')
    await searchInput.fill('E2E-DF')
    await page.locator('button:has-text("搜索")').click()
    await page.waitForLoadState('networkidle')

    await expect(page.locator('.el-table')).toContainText(E2E.accountName)
    const formatted = followerCount.toLocaleString('zh-CN')
    await expect(page.locator('.el-table')).toContainText(formatted)
  })

  test('E2E-03 内部作品分析: seed content visible', async ({ page }) => {
    await page.goto('/internal-content')
    await page.waitForLoadState('networkidle')

    const keyword = page.locator('input[placeholder*="内容标题"]')
    await keyword.fill(E2E.workTitlePrefix)
    await page.locator('button:has-text("搜索")').click()
    await page.waitForLoadState('networkidle')

    const table = page.locator('.internal-content-page .el-table')
    await expect(table).toContainText(E2E.workTitlePrefix)
    const e2eRows = page.locator('.el-table__body tr', { hasText: E2E.workTitlePrefix })
    expect(await e2eRows.count()).toBeGreaterThanOrEqual(1)
    const firstRowText = await e2eRows.first().innerText()
    expect(firstRowText).toMatch(/1[,，]?800[,，]?000|3[,，]?200[,，]?000|5[,，]?200[,，]?000/)
  })

  test('E2E-04 爆款监测: E2E external works after V58', async ({ page }) => {
    await page.goto('/hot-works')
    await page.waitForLoadState('networkidle')

    await expect(page.locator('.hot-works-page')).toBeVisible()
    await expect(page.locator('.el-table, .el-empty').first()).toBeVisible({ timeout: 10_000 })

    const table = page.locator('.el-table')
    if (await table.count()) {
      await expect(table).toContainText(E2E.workTitlePrefix, { timeout: 10_000 })
      const bodyText = await page.locator('.hot-works-page').innerText()
      expect(bodyText).toMatch(/3[,，]?200[,，]?000|320万|5[,，]?200[,，]?000|520万/)
    }
  })

  test('E2E-05 高粉账号分析: follower ranking shows E2E account', async ({ page, request }) => {
    const followerCount = await fetchE2eFollowerCount(request)
    expect(followerCount).toBeGreaterThanOrEqual(E2E.followerMin)

    await page.goto('/high-fans-account')
    await page.waitForLoadState('networkidle')

    await page.locator('.el-tabs__item', { hasText: '视频号' }).click()
    await page.waitForLoadState('networkidle')

    const pageRoot = page.locator('.high-fans-account')
    await expect(pageRoot).toContainText(/E2E-DF/, { timeout: 10_000 })
    const formatted = followerCount.toLocaleString('zh-CN')
    await expect(pageRoot).toContainText(formatted)
  })

  test('E2E-06 首页仪表盘: KPI cards non-empty', async ({ page }) => {
    await page.goto('/dashboard')
    await page.waitForLoadState('networkidle')

    const kpiCards = page.locator('.kpi-card')
    await expect(kpiCards.first()).toBeVisible({ timeout: 10_000 })
    await expect(kpiCards).toHaveCount(4)

    for (let i = 0; i < 4; i++) {
      const valueText = await kpiCards.nth(i).locator('.kpi-value').innerText()
      expect(valueText.trim().length).toBeGreaterThan(0)
      expect(valueText).not.toBe('--')
    }
  })

  test('E2E-07 操作日志: audit entries present', async ({ page }) => {
    await page.goto('/system-log/operation')
    await page.waitForLoadState('networkidle')

    const table = page.locator('.el-table')
    const empty = page.locator('.el-empty')
    if (await table.count()) {
      const rows = page.locator('.el-table__body tr')
      await expect(rows.first()).toBeVisible({ timeout: 10_000 })
      expect(await rows.count()).toBeGreaterThanOrEqual(1)
    } else {
      await expect(empty).toBeVisible()
      test.skip(true, 'No operation logs in seed — non-blocking')
    }
  })
})
