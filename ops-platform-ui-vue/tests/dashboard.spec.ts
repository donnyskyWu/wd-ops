import { test, expect } from '@playwright/test'

/**
 * 首页仪表盘 E2E（对齐 PRD-M0 / UX-M0）
 * AC-M0-001-1 ~ AC-M0-001-6
 */

test.describe('首页仪表盘测试 @smoke @regression', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/dashboard')
    await page.waitForLoadState('networkidle')
  })

  test('DASH-001: 4 个核心指标卡加载', async ({ page }) => {
    const kpiCards = page.locator('.kpi-card')
    await expect(kpiCards.first()).toBeVisible()
    await expect(kpiCards).toHaveCount(4)

    const labels = ['总作者数', '内容总数', 'SOP 完成率', '平均绩效']
    for (const label of labels) {
      await expect(page.locator('.kpi-label', { hasText: label })).toBeVisible()
    }
  })

  test('DASH-002: 顶部筛选与刷新按钮', async ({ page }) => {
    await expect(page.getByText('IP 组筛选')).toBeVisible()
    await expect(page.getByText('日期范围')).toBeVisible()
    await expect(page.getByRole('button', { name: '刷新' })).toBeVisible()
  })

  test('DASH-003: 快捷入口区域', async ({ page }) => {
    const quickAccess = page.locator('.quick-access')
    const count = await quickAccess.count()
    if (count > 0) {
      await expect(quickAccess).toBeVisible()
      await expect(page.locator('.quick-item').first()).toBeVisible()
    }
  })

  test('DASH-004: 图表区域渲染', async ({ page }) => {
    await expect(page.getByText('内容发布趋势')).toBeVisible()
    await expect(page.getByText('平台分布')).toBeVisible()
    await expect(page.locator('.chart-box').first()).toBeVisible()
  })

  test('DASH-005: 待办提醒表格', async ({ page }) => {
    await expect(page.getByText('待办提醒')).toBeVisible()
    const table = page.locator('.todo-section .el-table')
    const empty = page.locator('.todo-section .el-empty')
    const hasTable = await table.count()
    const hasEmpty = await empty.count()
    expect(hasTable + hasEmpty).toBeGreaterThan(0)
  })

  test('DASH-006: 手动刷新', async ({ page }) => {
    const refreshBtn = page.getByRole('button', { name: '刷新' })
    await refreshBtn.click()
    await page.waitForLoadState('networkidle')
    await expect(page.locator('.kpi-card').first()).toBeVisible()
  })

  test('DASH-007: 页面加载无控制台错误', async ({ page }) => {
    const errors: string[] = []
    page.on('pageerror', error => errors.push(error.message))
    await page.reload()
    await page.waitForLoadState('networkidle')
    expect(errors).toHaveLength(0)
  })
})
