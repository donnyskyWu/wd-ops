import { test, expect } from '@playwright/test'

test.describe('前后端联调 - 三方关联 @integration', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/triple-rel')
    await page.waitForLoadState('networkidle')
  })

  test('INT-TR01: 列表与统计从真实 API 加载', async ({ page }) => {
    await expect(page.locator('.el-table')).toBeVisible({ timeout: 10_000 })
    await expect(page.locator('.total-info')).toContainText(/共 \d+ 条/)
    await expect(page.getByText('张三的微信')).toHaveCount(0)
  })
})
