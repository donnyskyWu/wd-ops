import { test, expect } from '@playwright/test'

/**
 * P2辅助模块 E2E 测试套件（抽样覆盖50%）
 * 覆盖: 自定义查询、数据大屏、爆款作品分析、配置管理
 */

test.describe('自定义查询测试 @regression', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/custom-query')
    await page.waitForLoadState('networkidle')
  })

  test('QUERY-001: 查询配置区加载', async ({ page }) => {
    const configCard = page.locator('.el-card').first()
    await expect(configCard).toBeVisible()
  })

  test('QUERY-002: 数据源选择', async ({ page }) => {
    const select = page.locator('.el-select').first()
    await expect(select).toBeVisible()
  })

  test('QUERY-003: 新建查询按钮', async ({ page }) => {
    // L-α 走查后 CustomQuery 改为 SQL 编辑器模式，故按钮文案改为"新建查询"+"执行选中"
    const newQueryButton = page.locator('button:has-text("新建查询")')
    await expect(newQueryButton).toBeVisible()
  })

  test('QUERY-004: 执行选中按钮', async ({ page }) => {
    const executeButton = page.locator('button:has-text("执行选中")')
    await expect(executeButton).toBeVisible()
  })
})

test.describe('数据大屏测试 @regression', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/data-screen')
    await page.waitForLoadState('networkidle')
  })

  test('SCREEN-001: KPI卡片加载', async ({ page }) => {
    const kpiCards = page.locator('.kpi-card')
    await expect(kpiCards.first()).toBeVisible()
  })

  test('SCREEN-002: 图表容器渲染', async ({ page }) => {
    const chartContainers = page.locator('[style*="height: 250px"]')
    await expect(chartContainers.first()).toBeVisible()
  })

  test('SCREEN-003: 全屏按钮', async ({ page }) => {
    const fullscreenButton = page.locator('button:has-text("全屏")')
    await expect(fullscreenButton).toBeVisible()
  })

  test('SCREEN-004: 刷新按钮', async ({ page }) => {
    const refreshButton = page.locator('button:has-text("刷新")')
    await expect(refreshButton).toBeVisible()
    
    await refreshButton.click()
    await page.waitForTimeout(500)
    
    // 验证提示消息
    const message = page.locator('.el-message:has-text("数据已刷新")')
    await expect(message).toBeVisible()
  })
})

test.describe('爆款作品分析测试 @regression', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/hot-works')
    await page.waitForLoadState('networkidle')
  })

  test('HOT-001: 统计卡片加载', async ({ page }) => {
    const statCards = page.locator('.el-statistic')
    await expect(statCards.first()).toBeVisible()
  })

  test('HOT-002: 爆款阈值输入', async ({ page }) => {
    const thresholdInput = page.locator('.el-input-number')
    await expect(thresholdInput).toBeVisible()
  })

  test('HOT-003: 排名标识', async ({ page }) => {
    const rankTags = page.locator('.el-tag')
    await expect(rankTags.first()).toBeVisible()
  })

  test('HOT-004: 爆款指数评分', async ({ page }) => {
    const rateComponent = page.locator('.el-rate')
    await expect(rateComponent.first()).toBeVisible()
  })
})

test.describe('配置管理测试 @regression', () => {
  test.beforeEach(async ({ page }) => {
    // 配置管理页面 - 使用第一个配置的路由
    await page.goto('/config-internal-collect')
    await page.waitForLoadState('networkidle')
  })

  test('CONFIG-001: 页面加载正常', async ({ page }) => {
    // 验证页面标题或内容
    const pageTitle = page.locator('.el-breadcrumb__item').last()
    await expect(pageTitle).toBeVisible()
  })

  test('CONFIG-002: 数据表格显示', async ({ page }) => {
    const table = page.locator('.el-table')
    await expect(table).toBeVisible()
  })
})

test.describe('系统管理测试 @regression', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/system-user')
    await page.waitForLoadState('networkidle')
  })

  test('SYSTEM-001: 用户管理列表', async ({ page }) => {
    await expect(page.locator('.user-manage')).toBeVisible()
    await expect(page.locator('.user-manage .el-table')).toBeVisible()
  })

  test('SYSTEM-002: 新增用户按钮', async ({ page }) => {
    const addButton = page.locator('.user-manage button:has-text("新增用户")')
    await expect(addButton).toBeVisible()
  })

  test('SYSTEM-003: 搜索与分页', async ({ page }) => {
    await expect(page.locator('.user-manage .el-form')).toBeVisible()
    await expect(page.locator('.user-manage button:has-text("查询")')).toBeVisible()
    await expect(page.locator('.user-manage .el-pagination')).toBeVisible()
  })
})
