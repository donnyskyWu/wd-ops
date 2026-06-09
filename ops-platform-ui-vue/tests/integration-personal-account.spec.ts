import { test, expect } from '@playwright/test'

/**
 * 前后端联调 · M4 S-08 个人账号
 */
test.describe('前后端联调 - 个人账号 @integration', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/personal-account')
    await page.waitForLoadState('networkidle')
  })

  test('INT-PA01: 企微列表从真实 API 加载', async ({ page }) => {
    await expect(page.locator('.el-table')).toBeVisible({ timeout: 10_000 })
    await expect(page.locator('.total-info')).toContainText(/共 \d+ 条/)
    await expect(page.getByText('运营部-企微账号1')).toHaveCount(0)
  })

  test('INT-PA02: 切换个微 Tab 加载列表', async ({ page }) => {
    await page.getByRole('tab', { name: '个人微信' }).click()
    await expect(page.locator('.el-table')).toBeVisible({ timeout: 10_000 })
    await expect(page.locator('.total-info')).toContainText(/共 \d+ 条/)
  })
})
