import { test, expect, type Locator, type Page } from '@playwright/test'
import { ensureSl05Fixture } from './helpers/integration-api'

function formCombobox(scope: Locator, label: string) {
  return scope.getByRole('combobox', { name: new RegExp(label) })
}

/** Element Plus 2.x：点击 wrapper 避免 placeholder 拦截 */
async function openSelect(scope: Locator, label: string) {
  const box = formCombobox(scope, label)
  await box.locator('xpath=ancestor::div[contains(@class,"el-select")]').locator('.el-select__wrapper').click()
}

async function pickFirstOption(page: Page) {
  const item = page.locator('.el-select-dropdown:visible .el-select-dropdown__item').first()
  await expect(item).toBeVisible({ timeout: 8_000 })
  await item.click({ force: true })
}

async function pickDropdownOption(page: Page, text: string | RegExp) {
  const item = page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: text })
  await expect(item.first()).toBeVisible({ timeout: 8_000 })
  await item.first().click({ force: true })
}

async function searchAndPickOption(page: Page, scope: Locator, label: string, text: string) {
  const wrapper = formCombobox(scope, label)
    .locator('xpath=ancestor::div[contains(@class,"el-select")]')
    .locator('.el-select__wrapper')
  await wrapper.click()
  await page.keyboard.type(text)
  await page.waitForTimeout(600)
  await pickDropdownOption(page, text)
}

test.describe('前后端联调 - 强关联选择器 @integration', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/internal-account')
    await page.waitForLoadState('networkidle')
  })

  test('INT-SL01: 新增弹窗公司选择器从 API 加载', async ({ page }) => {
    await page.getByRole('button', { name: '新增账号' }).click()
    const dialog = page.getByRole('dialog', { name: '新增账号' })
    await expect(dialog).toBeVisible()

    await openSelect(dialog, '公司')
    await expect(page.locator('.el-select-dropdown:visible .el-select-dropdown__item').first()).toBeVisible({
      timeout: 8_000,
    })
    await expect(page.getByText('知识变现研究院')).toHaveCount(0)
  })

  test('INT-SL02: 未选实名人时手机选择器禁用', async ({ page }) => {
    await page.getByRole('button', { name: '新增账号' }).click()
    const dialog = page.getByRole('dialog', { name: '新增账号' })
    await expect(formCombobox(dialog, '^手机$')).toBeDisabled()
  })

  test('INT-SL03: 未选手机时手机卡选择器禁用', async ({ page }) => {
    await page.getByRole('button', { name: '新增账号' }).click()
    const dialog = page.getByRole('dialog', { name: '新增账号' })
    await expect(formCombobox(dialog, '手机卡')).toBeDisabled()
  })

  test('INT-SL04: 选择公司后实名人可搜索', async ({ page }) => {
    await page.getByRole('button', { name: '新增账号' }).click()
    const dialog = page.getByRole('dialog', { name: '新增账号' })

    await openSelect(dialog, '公司')
    await pickFirstOption(page)

    const realnameBox = formCombobox(dialog, '实名人')
    await expect(realnameBox).toBeEnabled()
    await openSelect(dialog, '实名人')
    await expect(page.locator('.el-select-dropdown:visible .el-select-dropdown__item').first()).toBeVisible({
      timeout: 8_000,
    })
  })

  test('INT-SL05: 切换公司后手机选择器联动重置为禁用', async ({ page, request }) => {
    const fx = await ensureSl05Fixture(request)

    await page.getByRole('button', { name: '新增账号' }).click()
    const dialog = page.getByRole('dialog', { name: '新增账号' })

    await searchAndPickOption(page, dialog, '公司', fx.companyAName)

    await searchAndPickOption(page, dialog, '实名人', fx.realName)

    const phoneBox = formCombobox(dialog, '^手机$')
    await expect(phoneBox).toBeEnabled({ timeout: 5_000 })

    await searchAndPickOption(page, dialog, '公司', fx.companyBName)
    await expect(phoneBox).toBeDisabled({ timeout: 5_000 })
  })

  test('INT-SL06: 采集任务页 AccountSelect 按平台过滤', async ({ page }) => {
    await page.goto('/collect/task/0')
    await page.waitForLoadState('networkidle')

    const scope = page.locator('.task-edit-page')
    const accountBox = formCombobox(scope, '账号')
    await expect(accountBox).toBeVisible({ timeout: 8_000 })
    await expect(accountBox).toBeDisabled()

    await openSelect(scope, '平台')
    await pickFirstOption(page)
    await expect(accountBox).toBeEnabled()
  })
})
