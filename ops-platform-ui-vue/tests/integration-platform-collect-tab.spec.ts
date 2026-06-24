import { test, expect } from '@playwright/test'

/**
 * M10 P2 · M4 采集 Tab 冒烟
 * 前置: 前端 dev (3000) + 后端 dev (8080) 已启动
 */
test.describe('前后端联调 - 平台账号采集 Tab @integration', () => {
  test('INT-M10-A01: 详情页采集 Tab 与批量绑定按钮可见', async ({ page }) => {
    await page.goto('/platform-account/9001?tab=collect')
    await page.waitForLoadState('networkidle')

    await expect(page.getByRole('tab', { name: '采集' })).toBeVisible({ timeout: 10_000 })
    await expect(page.getByRole('button', { name: '批量绑定未绑定账号' })).toBeVisible()
    await expect(page.getByText('绑定状态')).toBeVisible()
    await expect(page.getByText('采集凭证')).toBeVisible()
  })
})
