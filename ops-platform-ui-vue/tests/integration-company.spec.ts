import { test, expect } from '@playwright/test'

/**
 * 前后端联调 · M4 S-01 公司管理
 * 前置: 后端 dev 已启动 (localhost:8080, MySQL wd)
 */
test.describe('前后端联调 - 公司管理 @integration', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/company')
    await page.waitForLoadState('networkidle')
  })

  test('INT-C01: 列表从真实 API 加载', async ({ page }) => {
    await expect(page.locator('.el-table')).toBeVisible({ timeout: 10_000 })
    await expect(page.locator('.total-info')).toContainText(/共 \d+ 条/)

    const rows = page.locator('.el-table__body tr')
    await expect(rows.first()).toBeVisible()

    // 不应出现旧 Mock 硬编码数据
    await expect(page.getByText('XX传媒有限公司')).toHaveCount(0)
    await expect(page.getByText('YY文化有限公司')).toHaveCount(0)
  })

  test('INT-C02: 新增公司并出现在列表', async ({ page }) => {
    const suffix = Date.now().toString().slice(-6)
    const companyName = `联调测试公司${suffix}`
    const creditCode = `91330100MA2H${suffix}`

    await page.getByRole('button', { name: '新增公司' }).click()
    const dialog = page.locator('.el-dialog').filter({ hasText: '新增公司' })
    await expect(dialog).toBeVisible()

    await dialog.locator('input[placeholder*="公司名称"]').fill(companyName)
    await dialog.locator('input[placeholder*="统一信用代码"]').fill(creditCode)
    await dialog.locator('input[placeholder*="法人姓名"]').fill('联调法人')

    await dialog.getByRole('button', { name: '保存' }).click()
    await expect(page.getByText('保存成功')).toBeVisible({ timeout: 8_000 })

    await page.getByPlaceholder('搜索公司名称').fill(companyName)
    await page.getByRole('button', { name: '搜索' }).click()
    await page.waitForTimeout(500)

    await expect(page.getByText(companyName)).toBeVisible()
  })

  test('INT-C03: Dev Token 鉴权头透传', async ({ page }) => {
    const apiResponses: number[] = []
    page.on('response', (res) => {
      if (res.url().includes('/admin-api/oa/company/list')) {
        apiResponses.push(res.status())
      }
    })

    await page.reload({ waitUntil: 'networkidle' })
    expect(apiResponses.some((s) => s === 200)).toBeTruthy()
  })
})
