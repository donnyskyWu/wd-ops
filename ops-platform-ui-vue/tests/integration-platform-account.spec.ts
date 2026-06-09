import { test, expect } from '@playwright/test'

/**
 * 前后端联调 · M4 S-06 平台账号
 * 前置: 后端 dev + 已有公司/实名人（联调可现场创建）
 */
test.describe('前后端联调 - 平台账号 @integration', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/internal-account')
    await page.waitForLoadState('networkidle')
  })

  test('INT-A01: 列表从真实 API 加载', async ({ page }) => {
    await expect(page.locator('.el-table')).toBeVisible({ timeout: 10_000 })
    await expect(page.locator('.total-info')).toContainText(/共 \d+ 条/)
    await expect(page.getByText('知识变现研究院')).toHaveCount(0)
  })
})
