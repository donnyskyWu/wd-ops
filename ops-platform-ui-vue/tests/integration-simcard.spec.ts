import { test, expect } from '@playwright/test'

/**
 * 前后端联调 · M4 S-05 手机卡管理
 * 前置: 后端 dev 已启动 (localhost:8080, MySQL wd, Flyway V7)
 */
test.describe('前后端联调 - 手机卡 @integration', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/simcard')
    await page.waitForLoadState('networkidle')
  })

  test('INT-S01: 列表从真实 API 加载', async ({ page }) => {
    await expect(page.locator('.el-table')).toBeVisible({ timeout: 10_000 })
    await expect(page.locator('.total-info')).toContainText(/共 \d+ 条/)
    await expect(page.getByText('89860000000000000001')).toHaveCount(0)
  })

  test('INT-S02: 新增手机卡并出现在列表', async ({ page }) => {
    const suffix = Date.now().toString().slice(-8)
    const phoneNumber = `137${suffix}`.slice(0, 11)
    const iccid = `8986000${suffix}`.slice(0, 20)

    await page.getByRole('button', { name: '新增手机卡' }).click()
    const dialog = page.locator('.el-dialog').filter({ hasText: '新增手机卡' })
    await expect(dialog).toBeVisible()

    await dialog.locator('input[placeholder*="11位手机号"]').fill(phoneNumber)
    await dialog.locator('input[placeholder*="ICCID"]').fill(iccid)
    // 选择运营商（必填）
    await dialog.locator('.el-form-item').filter({ hasText: '运营商' }).locator('.el-select').click()
    await page.locator('.el-select-dropdown:visible .el-select-dropdown__item').first().click()

    await dialog.getByRole('button', { name: '保存' }).click()
    await expect(page.getByText('保存成功')).toBeVisible({ timeout: 8_000 })

    await page.getByPlaceholder('搜索ICCID').fill(iccid)
    await page.getByRole('button', { name: '搜索' }).click()
    await page.waitForTimeout(500)

    await expect(page.locator('.masked-text').first()).toBeVisible()
  })
})
