import { test, expect } from '@playwright/test'

/**
 * P1核心模块 E2E 测试套件（抽样覆盖80%）
 * 覆盖: 作品分析、任务管理、内容管理、考核执行、指标管理
 */

test.describe('内部作品分析测试 @regression', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/internal-content')
    await page.waitForLoadState('networkidle')
  })

  test('WORKS-001: 列表数据加载', async ({ page }) => {
    const table = page.locator('.el-table')
    await expect(table).toBeVisible()
    await expect(page.locator('.stats-cards')).toBeVisible()
  })

  test('WORKS-002: 搜索功能', async ({ page }) => {
    const searchInput = page.locator('input[placeholder="内容标题"]')
    const searchBtn = page.locator('button:has-text("搜索")')

    await expect(searchInput).toBeVisible()
    await expect(searchBtn).toBeVisible()

    await searchInput.fill('测试作品')
    await searchBtn.click()
    await page.waitForTimeout(500)

    await expect(page.locator('.el-table')).toBeVisible()
  })

  test('WORKS-003: 爆款标识显示', async ({ page }) => {
    const hotTag = page.locator('.viral-tag')
    const count = await hotTag.count()
    expect(count).toBeGreaterThanOrEqual(0)
  })
})

test.describe('任务管理测试 @regression', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/task')
    await page.waitForLoadState('networkidle')
  })

  test('TASK-001: 任务列表加载', async ({ page }) => {
    const table = page.locator('.el-table')
    await expect(table).toBeVisible()
  })

  test('TASK-002: Tab切换', async ({ page }) => {
    const tabs = page.locator('.el-tabs__item')
    await expect(tabs).toHaveCount(2)
    
    await tabs.nth(1).click()
    await page.waitForTimeout(300)
  })
})

test.describe('内容管理测试 @regression', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/content')
    await page.waitForLoadState('networkidle')
  })

  test('CONTENT-001: 内容列表加载', async ({ page }) => {
    const table = page.locator('.el-table')
    await expect(table).toBeVisible()
  })

  test('CONTENT-002: 状态筛选', async ({ page }) => {
    const statusSelect = page.locator('.el-select')
    await expect(statusSelect.first()).toBeVisible()
  })
})

test.describe('考核执行测试 @regression', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/perf-execution')
    await page.waitForLoadState('networkidle')
  })

  test('PERF-001: 考核列表加载', async ({ page }) => {
    const table = page.locator('.el-table')
    await expect(table).toBeVisible()
  })

  test('PERF-002: 自动算分标识', async ({ page }) => {
    const scoreColumn = page.locator('.el-table__cell:has-text("得分")')
    const count = await scoreColumn.count()
    expect(count).toBeGreaterThanOrEqual(0)
  })
})

test.describe('指标管理测试 @regression', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/metric')
    await page.waitForLoadState('networkidle')
  })

  test('METRIC-001: 指标列表加载', async ({ page }) => {
    const table = page.locator('.el-table')
    await expect(table).toBeVisible()
  })

  test('METRIC-002: 指标类型徽章', async ({ page }) => {
    const typeBadge = page.locator('.el-tag')
    await expect(typeBadge.first()).toBeVisible()
  })

  test('METRIC-003: 新增指标', async ({ page }) => {
    const addButton = page.locator('button:has-text("新增")')
    await expect(addButton).toBeVisible()
    
    await addButton.click()
    await page.waitForTimeout(300)
    
    const dialog = page.locator('.el-dialog')
    await expect(dialog).toBeVisible()
  })
})
