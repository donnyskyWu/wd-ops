import { test, expect } from '@playwright/test'

/**
 * 前后端联调 · M4 S-02 实名人管理
 * 前置: 后端 dev 已启动 (localhost:8080, MySQL wd)
 */
test.describe('前后端联调 - 实名人 @integration', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/realname')
    await page.waitForLoadState('networkidle')
  })

  test('INT-R01: 列表从真实 API 加载', async ({ page }) => {
    await expect(page.locator('.el-table')).toBeVisible({ timeout: 10_000 })
    await expect(page.locator('.total-info')).toContainText(/共 \d+ 条/)
    // seed-assets 灌入后应可见 SEED 样本；身份证/手机须脱敏，不得出现完整明文
    await expect(page.getByText('SEED-张三')).toBeVisible()
    await expect(page.locator('.el-table')).toContainText('****')
    await expect(page.getByText('330101199001011234')).toHaveCount(0)
    await expect(page.getByText(/^138\d{8}$/)).toHaveCount(0)
  })

  test('INT-R02: 新增实名人并出现在列表', async ({ page }) => {
    const suffix = Date.now().toString().slice(-6)
    const realName = `联调实名人${suffix}`

    await page.getByRole('button', { name: '新增实名人' }).click()
    const dialog = page.locator('.el-dialog').filter({ hasText: '新增实名人' })
    await expect(dialog).toBeVisible()

    await dialog.locator('input[placeholder*="真实姓名"]').fill(realName)
    await dialog.locator('input[placeholder*="身份证号"]').fill('330101199001011234')
    await dialog.locator('input[placeholder*="手机号"]').fill(`138${suffix}01`.slice(0, 11))

    await dialog.getByRole('button', { name: '保存' }).click()
    await expect(page.getByText('保存成功')).toBeVisible({ timeout: 8_000 })

    await page.getByPlaceholder('搜索姓名').fill(realName)
    await page.getByRole('button', { name: '搜索' }).click()
    await page.waitForTimeout(500)

    await expect(page.getByText(realName)).toBeVisible()
    await expect(page.locator('.masked-text').first()).toContainText('*')
  })
})
