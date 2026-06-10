import { test, expect, type Locator, type Page } from '@playwright/test'

function formCombobox(scope: Locator, label: string) {
  return scope.getByRole('combobox', { name: new RegExp(label) })
}

async function openSelect(scope: Locator, label: string) {
  const box = formCombobox(scope, label)
  await box.locator('xpath=ancestor::div[contains(@class,"el-select")]').locator('.el-select__wrapper').click()
}

async function pickFirstOption(page: Page) {
  const item = page.locator('.el-select-dropdown:visible .el-select-dropdown__item').first()
  await expect(item).toBeVisible({ timeout: 8_000 })
  await item.click({ force: true })
}

/**
 * 前后端联调 · M4 S-04 手机管理
 * 前置: 后端 dev 已启动 (localhost:8080, MySQL wd, Flyway V6)
 */
test.describe('前后端联调 - 手机管理 @integration', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/phone')
    await page.waitForLoadState('networkidle')
  })

  test('INT-P01: 列表从真实 API 加载', async ({ page }) => {
    await expect(page.locator('.el-table')).toBeVisible({ timeout: 10_000 })
    await expect(page.locator('.total-info')).toContainText(/共 \d+ 条/)
    // 不应出现旧 Mock 硬编码
    await expect(page.getByText('13800138001')).toHaveCount(0)
  })

  test('INT-P02: 新增手机并出现在列表', async ({ page }) => {
    const suffix = Date.now().toString().slice(-8)
    const phoneNumber = `139${suffix}`.slice(0, 11)

    await page.getByRole('button', { name: '新增手机信息' }).click()
    const dialog = page.locator('.el-dialog').filter({ hasText: '新增手机信息' })
    await expect(dialog).toBeVisible()

    await dialog.locator('input[placeholder*="11位手机号"]').fill(phoneNumber)
    await dialog.locator('input[placeholder*="内部编号"]').fill(`PH-${suffix}`)
    await dialog.locator('input[placeholder*="iPhone"]').fill('联调测试机')
    // ADR-011：保管人必填（UserSelect）
    await openSelect(dialog, '保管人')
    await pickFirstOption(page)

    await dialog.getByRole('button', { name: '保存' }).click()
    await expect(page.getByText('保存成功')).toBeVisible({ timeout: 8_000 })

    await page.getByPlaceholder('搜索手机号/编码/机型').fill(`PH-${suffix}`)
    await page.getByRole('button', { name: '搜索' }).click()
    await page.waitForTimeout(500)

    await expect(page.getByText(`PH-${suffix}`)).toBeVisible()
    await expect(page.locator('.masked-text').first()).toContainText('*')
  })
})
