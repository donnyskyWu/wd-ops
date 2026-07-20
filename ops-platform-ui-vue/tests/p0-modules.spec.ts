import { test, expect } from '@playwright/test'

/**
 * P0模块 E2E 测试套件
 * 覆盖: IP组管理、作者管理
 */

test.describe('IP组管理测试 @smoke', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/ip-group')
    await page.waitForLoadState('networkidle')
  })

  test('IPG-001: 列表数据加载（树形结构）', async ({ page }) => {
    // IP 组是左树+右 5 Tab 布局
    const tree = page.locator('.el-tree').first()
    await expect(tree).toBeVisible({ timeout: 8_000 })
  })

  test('IPG-002: 搜索功能', async ({ page }) => {
    // 找到 IP 组树搜索输入框
    const searchInput = page.locator('input[placeholder*="搜索组名"], input[placeholder*="IP组"]').first()
    await expect(searchInput).toBeVisible({ timeout: 5_000 })

    // 输入搜索内容
    await searchInput.fill('测试')
    await page.waitForTimeout(300)
  })

  test('IPG-003: 新增按钮可见', async ({ page }) => {
    // "新建大组" 按钮
    const addButton = page.locator('button:has-text("新建大组"), button:has-text("新增")').first()
    await expect(addButton).toBeVisible({ timeout: 5_000 })
  })
})

test.describe('作者看板测试 @smoke', () => {
  test('AUTH-001: 看板 deep-link 可加载', async ({ page }) => {
    await page.goto('/author/9101/dashboard')
    await page.waitForLoadState('networkidle')
    await expect(page.locator('.author-dashboard, .el-card').first()).toBeVisible({ timeout: 8_000 })
  })
})
