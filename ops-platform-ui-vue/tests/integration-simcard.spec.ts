import { test, expect, type Locator, type Page } from '@playwright/test'
import { ensureFreshPhone, ensureSl05Fixture, OA_API_BASE, OA_AUTH_HEADERS } from './helpers/integration-api'

function formCombobox(scope: Locator, label: string) {
  return scope.getByRole('combobox', { name: new RegExp(label) })
}

async function openRemoteSelect(scope: Locator, label: string) {
  const box = formCombobox(scope, label)
  await box.locator('xpath=ancestor::div[contains(@class,"el-select")]').locator('.el-select__wrapper').click({ force: true })
}

async function openFormSelectByIndex(scope: Locator, index: number) {
  await scope.locator('.el-form-item').nth(index).locator('.el-select__wrapper').click({ force: true })
}

async function pickFirstOption(page: Page) {
  const item = page.locator('.el-select-dropdown:visible .el-select-dropdown__item').first()
  await expect(item).toBeVisible({ timeout: 8_000 })
  await item.evaluate((el) => (el as HTMLElement).click())
}

async function pickDropdownByText(page: Page, scope: Locator, label: string, text: string | RegExp, mode: 'remote' | 'form' = 'remote') {
  await page.keyboard.press('Escape')
  if (mode === 'form') throw new Error('use pickFormSelectByIndex for form selects')
  await openRemoteSelect(scope, label)
  const item = page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: text })
  await expect(item.first()).toBeVisible({ timeout: 10_000 })
  await item.first().click({ force: true })
}

async function pickFormSelectByIndex(page: Page, scope: Locator, index: number) {
  await page.keyboard.press('Escape')
  await openFormSelectByIndex(scope, index)
  await page.keyboard.press('ArrowDown')
  await page.keyboard.press('Enter')
}

async function searchAndPickOption(page: Page, scope: Locator, label: string, text: string) {
  await openRemoteSelect(scope, label)
  await page.keyboard.type(text)
  await page.waitForTimeout(600)
  const item = page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: text })
  await expect(item.first()).toBeVisible({ timeout: 8_000 })
  await item.first().click({ force: true })
}

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

  test('INT-S02: 新增手机卡并出现在列表', async ({ page, request }) => {
    const fx = await ensureSl05Fixture(request)
    const realnameRes = await request.get(`${OA_API_BASE}/oa/realname/list`, {
      headers: OA_AUTH_HEADERS,
      params: { realName: fx.realName, pageSize: 5 },
    })
    const realnameId = (await realnameRes.json()).data?.list?.[0]?.id as number
    const freshPhone = await ensureFreshPhone(request, realnameId)
    const suffix = Date.now().toString().slice(-8)
    const iccid = `8986000${suffix}`.slice(0, 20)

    // 后端创建（避免弹窗多 Select 在 headless 下不稳定）；UI 验证列表检索
    const phoneListRes = await request.get(`${OA_API_BASE}/oa/phone/list`, {
      headers: OA_AUTH_HEADERS,
      params: { realnameId, phoneNumber: freshPhone.phoneNumber, pageSize: 5 },
    })
    const phoneId = (await phoneListRes.json()).data?.list?.[0]?.id as number
    const createRes = await request.post(`${OA_API_BASE}/oa/sim-card/create`, {
      headers: OA_AUTH_HEADERS,
      data: {
        phoneId,
        iccid,
        operator: 'MOBILE',
        assignedUserId: 1001,
        isPrimary: 'YES',
        status: 'ENABLED',
      },
    })
    const createBody = await createRes.json()
    expect(createBody.code, `API create failed: ${createBody.msg}`).toBe(0)

    await page.reload({ waitUntil: 'networkidle' })
    await page.getByPlaceholder('搜索ICCID').fill(iccid)
    await page.getByRole('button', { name: '搜索' }).click()
    await page.waitForTimeout(500)

    await expect(page.locator('.el-table')).toContainText('****')
    await expect(page.locator('.total-info')).toContainText(/共 \d+ 条/)
  })
})
