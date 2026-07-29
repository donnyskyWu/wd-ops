import { test, expect, type APIRequestContext, type Page } from '@playwright/test'
import * as fs from 'node:fs'
import * as path from 'node:path'
import { fileURLToPath } from 'node:url'

/**
 * M6 数据分析 E2E — Football Gate :5777
 * SSOT: docs/engineering/E2E-AGENT-METHOD.md · TESTCASES-M6-数据分析.md
 *
 * | Playwright ID | TESTCASES 映射     | 说明 |
 * |---------------|-------------------|------|
 * | M6-E2E-001    | V165 元数据        | oa_douyin_video 可读（抖音采集） |
 * | M6-E2E-002    | TC-M6-001-01      | UI 新建 COUNT 指标 |
 * | M6-E2E-003    | TC-M6-001-01      | 指标分析 |
 * | M6-E2E-004    | TC-M6-005-01/02   | 自定义查询 + 保存 PUBLISHED |
 * | M6-E2E-005    | TC-M6-007-01      | 大屏 METRIC+QUERY |
 * | M6-E2E-006    | TC-M6-006-01      | 自建大屏全屏 |
 */

const E2E_DATE = '20260727'
const API_BASE = 'http://localhost:48080/admin-api'
const ACCESS_STORE_KEY = 'yudao-vben-ele-5.5.9-dev-1.0.0-core-access'

/** V165 元数据；Douyin 采集写入 oa_douyin_video（非 oa_content） */
const ENV = {
  metadataEntity: 'oa_douyin_video',
  metadataEntityName: '抖音视频表',
  displayField: 'title',
  queryValueKey: 'play_count',
} as const

const SEED = {
  queryId: 9861,
  queryName: 'SEED-近30天内容列表',
  dashboardId: 98601,
  dashboardName: '内部运营大屏',
} as const

const OPS_ROUTES = {
  metadata: '/ops/config/config-metadata',
  metric: '/ops/analysis/metric',
  metricAnalysis: '/ops/analysis/metric-analysis',
  customQuery: '/ops/analysis/custom-query',
  screenConfig: '/ops/analysis/screen-config',
  screenById: (id: number) => `/ops/screen/${id}`,
} as const

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const REPO_ROOT = path.resolve(__dirname, '../..')
const ARTIFACT_DIR = path.join(REPO_ROOT, 'docs/delivery/e2e-artifacts', `M6-E2E-${E2E_DATE}`)
const DEFECTS_DIR = path.join(REPO_ROOT, 'docs/delivery/defects')

type CaseStatus = 'Pass' | 'Fail' | 'Blocked'

interface CaseRecord {
  tcId: string
  testcasesRef: string
  description: string
  status: CaseStatus
  screenshot?: string
  note?: string
}

const caseRecords: CaseRecord[] = []
const screenshotIndex: string[] = []
let defectSeq = 1

const runState = {
  ts: Date.now(),
  createdMetricName: '',
  createdMetricCode: '',
  createdMetricId: null as number | null,
  createdQueryName: '',
  createdQueryId: null as number | null,
  savedDashboardId: null as number | null,
}
runState.createdMetricCode = `E2E_M6_${runState.ts}`
runState.createdMetricName = `E2E指标${runState.ts}`
runState.createdQueryName = `E2E查询${runState.ts}`

let authCache: {
  accessToken: string
  refreshToken: string
  accessCodes: string[]
  authHeaders: Record<string, string>
} | null = null
let authInitDone = false
let chainBlocked = false
let envProbe = {
  targetRowCount: -1,
  oaContentRowCount: -1,
  oaDouyinVideoRowCount: -1,
  seedQueryTotal: -1,
  seedWidgetCount: 0,
  seedNonZeroWidgets: 0,
}

async function probeTableCount(
  request: APIRequestContext,
  authHeaders: Record<string, string>,
  table: string,
) {
  const res = await request.post(`${API_BASE}/oa/metric/preview`, {
    headers: authHeaders,
    data: {
      metricFormula: `SELECT COUNT(*) AS metric_value FROM ${table} t WHERE t.tenant_id = :tenantId AND t.deleted = 0`,
    },
  })
  const body = await res.json()
  return Number(body.data?.rows?.[0]?.metric_value ?? 0)
}

async function probeTargetTable(request: APIRequestContext, authHeaders: Record<string, string>) {
  const [targetRowCount, oaContentRowCount, oaDouyinVideoRowCount] = await Promise.all([
    probeTableCount(request, authHeaders, ENV.metadataEntity),
    probeTableCount(request, authHeaders, 'oa_content'),
    probeTableCount(request, authHeaders, 'oa_douyin_video'),
  ])
  const execRes = await request.post(
    `${API_BASE}/oa/query/${SEED.queryId}/execute?pageNum=1&pageSize=10`,
    { headers: authHeaders },
  )
  const execBody = await execRes.json()
  const dashRes = await request.get(
    `${API_BASE}/oa/dashboard/${SEED.dashboardId}/data?startDate=2026-06-01&endDate=2026-07-27`,
    { headers: authHeaders },
  )
  const dashBody = await dashRes.json()
  const widgets = (dashBody.data?.widgets ?? []) as Array<{ payload?: { value?: number } }>
  return {
    targetRowCount,
    oaContentRowCount,
    oaDouyinVideoRowCount,
    seedQueryTotal: Number(execBody.data?.total ?? 0),
    seedWidgetCount: widgets.length,
    seedNonZeroWidgets: widgets.filter((w) => Number(w.payload?.value ?? 0) > 0).length,
  }
}

async function seedFootballAuth(request: APIRequestContext, page: Page) {
  if (!authCache) {
    const loginRes = await request.post(`${API_BASE}/system/auth/login`, {
      headers: { 'tenant-id': '1', 'Content-Type': 'application/json' },
      data: { username: 'admin', password: 'admin123' },
      timeout: 60_000,
    })
    const loginBody = await loginRes.json()
    expect(loginBody.code).toBe(0)
    const permRes = await request.get(`${API_BASE}/system/auth/get-permission-info`, {
      headers: { 'tenant-id': '1', Authorization: `Bearer ${loginBody.data.accessToken}` },
    })
    const permBody = await permRes.json()
    authCache = {
      accessToken: loginBody.data.accessToken as string,
      refreshToken: loginBody.data.refreshToken as string,
      accessCodes: permBody.data.permissions ?? [],
      authHeaders: {
        'tenant-id': '1',
        'X-Tenant-Id': '1',
        Authorization: `Bearer ${loginBody.data.accessToken}`,
      },
    }
  }
  const storePayload = {
    storeKey: ACCESS_STORE_KEY,
    accessToken: authCache.accessToken,
    refreshToken: authCache.refreshToken,
    accessCodes: authCache.accessCodes,
    tenantId: 1,
  }
  if (!authInitDone) {
    await page.addInitScript(
      ({ storeKey, accessToken, refreshToken, accessCodes, tenantId }) => {
        localStorage.setItem(
          storeKey,
          JSON.stringify({ accessToken, refreshToken, accessCodes, tenantId, visitTenantId: tenantId }),
        )
      },
      storePayload,
    )
    authInitDone = true
  }
  await page.goto('/', { waitUntil: 'domcontentloaded', timeout: 30_000 }).catch(() => {})
  await page.evaluate(
    ({ storeKey, accessToken, refreshToken, accessCodes, tenantId }) => {
      localStorage.setItem(
        storeKey,
        JSON.stringify({ accessToken, refreshToken, accessCodes, tenantId, visitTenantId: tenantId }),
      )
    },
    storePayload,
  )
  return { authHeaders: authCache.authHeaders }
}

async function checkEnvPorts() {
  const probe = async (url: string) => {
    try {
      const res = await fetch(url, { signal: AbortSignal.timeout(5_000) })
      return res.ok || res.status === 404
    } catch {
      return false
    }
  }
  const [p5777, p48080, p48094] = await Promise.all([
    probe('http://127.0.0.1:5777/'),
    probe('http://127.0.0.1:48080/admin-api/system/tenant/simple-list'),
    probe('http://127.0.0.1:48094/actuator/health'),
  ])
  return { p5777, p48080, p48094 }
}

async function gotoOpsPage(page: Page, route: string) {
  await page.goto('/ops/dashboard', { waitUntil: 'domcontentloaded', timeout: 60_000 })
  await page.waitForTimeout(1500)
  await page.goto(route, { waitUntil: 'domcontentloaded', timeout: 60_000 })
  await page.waitForTimeout(2000)
  await expect(page.getByText('未找到页面')).toHaveCount(0, { timeout: 15_000 })
  await expect(page.locator('vite-error-overlay, .vite-error-overlay')).toHaveCount(0)
}

async function pickSelectOption(page: Page, optionPattern: RegExp | string) {
  const dropdown = page.locator('.el-select-dropdown:visible').last()
  await expect(dropdown).toBeVisible({ timeout: 10_000 })
  await dropdown.getByText(optionPattern).first().click()
  await page.waitForTimeout(400)
}

async function pickDialogSourceType(page: Page, dlg: ReturnType<typeof widgetDialog>, label: string) {
  await dlg.locator('.el-form-item').filter({ hasText: '数据源' }).locator('.el-select').click()
  await pickSelectOption(page, label)
  await page.locator('.el-select-dropdown:visible').last().waitFor({ state: 'hidden', timeout: 5_000 }).catch(() => {})
}

function formItemByLabel(dlg: ReturnType<typeof widgetDialog>, label: string | RegExp) {
  const pattern =
    typeof label === 'string'
      ? new RegExp(`^\\*?\\s*${label.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\s*$`)
      : label
  return dlg.locator('.el-form-item').filter({
    has: dlg.locator('.el-form-item__label').filter({ hasText: pattern }),
  })
}

function widgetDialog(page: Page) {
  return page.getByRole('dialog').filter({ hasText: /编辑(指标卡|组件|图表|列表|今日统计)/ })
}

async function captureStep(page: Page, tcId: string, step: string, status: 'pass' | 'fail') {
  const ts = Date.now()
  const filename = `${tcId}_${step}_${status}_${ts}.png`
  const testPath = path.join('test-results', filename)
  await fs.promises.mkdir(path.dirname(testPath), { recursive: true })
  await page.screenshot({ path: testPath, fullPage: true })
  screenshotIndex.push(`test-results/${filename}`)
  return { relPath: `test-results/${filename}` }
}

function recordCase(record: CaseRecord) {
  caseRecords.push(record)
}

function registerDefect(opts: {
  tcId: string
  testcasesRef: string
  phenomenon: string
  expected: string
  steps: string[]
  screenshot?: string
  api?: string
}) {
  const defId = `DEF-${E2E_DATE}-${String(defectSeq++).padStart(3, '0')}`
  fs.mkdirSync(DEFECTS_DIR, { recursive: true })
  fs.writeFileSync(
    path.join(DEFECTS_DIR, `${defId}.md`),
    `## ${defId}

- **关联用例**: ${opts.tcId} / ${opts.testcasesRef}
- **优先级**: P0
- **现象**: ${opts.phenomenon}
- **预期**: ${opts.expected}
- **复现步骤**:
${opts.steps.map((s, i) => `  ${i + 1}. ${s}`).join('\n')}
- **环境**: 本地 Gate；:5777 / :48080 / :48094
- **证据**: 截图 ${opts.screenshot ?? '—'}；API ${opts.api ?? '—'}
- **限定修复范围**: M6 数据分析（环境 seed / 非 Football master）
- **状态**: Open
`,
    'utf8',
  )
}

async function copyArtifactsAndReport(env: { p5777: boolean; p48080: boolean; p48094: boolean }) {
  fs.mkdirSync(ARTIFACT_DIR, { recursive: true })
  for (const rel of screenshotIndex) {
    const src = path.join(REPO_ROOT, 'ops-platform-ui-vue', rel)
    if (fs.existsSync(src)) fs.copyFileSync(src, path.join(ARTIFACT_DIR, path.basename(rel)))
  }
  const passCount = caseRecords.filter((c) => c.status === 'Pass').length
  const failCount = caseRecords.filter((c) => c.status === 'Fail').length
  const blockedCount = caseRecords.filter((c) => c.status === 'Blocked').length
  const total = caseRecords.length
  const report = `# E2E 测试报告 — M6 数据分析 — ${E2E_DATE}

## 1. 概要
- 范围: M6 全链路（元数据→指标→分析→查询→大屏配置→自建大屏全屏；数据源 ${ENV.metadataEntity}）
- 环境: Gate（admin/admin123 租户 1）；:5777 / :48080 / :48094
- 结论: ${failCount === 0 && blockedCount === 0 ? '✅ 通过' : '❌ 未通过'}（Pass ${passCount}/${total}，Fail ${failCount}，Blocked ${blockedCount}）
- Playwright: \`ops-platform-ui-vue/tests/m6-metadata-screen-e2e.spec.ts\`
- seed 探测: ${ENV.metadataEntity}=${envProbe.targetRowCount} 行；oa_content=${envProbe.oaContentRowCount}；oa_douyin_video=${envProbe.oaDouyinVideoRowCount}；query9861 total=${envProbe.seedQueryTotal}；98601 widgets=${envProbe.seedWidgetCount} nonZero=${envProbe.seedNonZeroWidgets}

## 2. 环境
| 服务 | 地址 | 状态 |
|------|------|------|
| 前端 | :5777 | ${env.p5777 ? 'UP' : 'DOWN'} |
| Gateway | :48080 | ${env.p48080 ? 'UP' : 'DOWN'} |
| oa-server | :48094 | ${env.p48094 ? 'UP' : 'DOWN'} |

## 3. 运行时数据
| 类型 | 值 |
|------|-----|
| 元数据 | ${ENV.metadataEntity} (V165) |
| 种子查询 | ${SEED.queryName} / id=${SEED.queryId} |
| 种子大屏 | ${SEED.dashboardName} / id=${SEED.dashboardId} |
| 新建指标 | ${runState.createdMetricCode} / id=${runState.createdMetricId ?? '—'} |
| 新建查询 | ${runState.createdQueryName} / id=${runState.createdQueryId ?? '—'} |
| 新建大屏 | id=${runState.savedDashboardId ?? '—'} |

## 4. 用例结果
| TC-ID | TESTCASES | 描述 | 结果 | 截图 | 备注 |
|-------|-----------|------|------|------|------|
${caseRecords.map((c) => `| ${c.tcId} | ${c.testcasesRef} | ${c.description} | ${c.status} | ${c.screenshot ?? '—'} | ${c.note ?? ''} |`).join('\n')}

## 5. 缺陷
${caseRecords.filter((c) => c.status === 'Fail' || c.status === 'Blocked').map((c) => `- ${c.tcId}: docs/delivery/defects/DEF-${E2E_DATE}-*.md`).join('\n') || '（无）'}

## 6. 证据索引
${screenshotIndex.map((s) => `- \`${s}\` → \`docs/delivery/e2e-artifacts/M6-E2E-${E2E_DATE}/${path.basename(s)}\``).join('\n') || '（无）'}

## 7. 阻塞 / 风险
- TC-M6-006-03/04、TC-M6-007-02 未自动化
- Douyin 采集数据在 oa_douyin_video（oa_content=0）；E2E 数据源 ${ENV.metadataEntity}
- seed 98601 / query 9861 仍依赖 oa_content，seed 项可能 Blocked

## 8. 签字
- E2E Agent: ${E2E_DATE}
`
  fs.writeFileSync(path.join(ARTIFACT_DIR, 'REPORT.md'), report, 'utf8')
}

function blockCase(tcId: string, ref: string, desc: string, note: string, shot?: string) {
  recordCase({ tcId, testcasesRef: ref, description: desc, status: 'Blocked', note, screenshot: shot })
}

test.describe('M6 数据分析全链路 @p0 @m6-e2e', () => {
  test.describe.configure({ timeout: 600_000 })
  let envStatus = { p5777: false, p48080: false, p48094: false }

  test.beforeAll(async () => {
    fs.mkdirSync(ARTIFACT_DIR, { recursive: true })
    envStatus = await checkEnvPorts()
  })
  test.afterAll(async () => {
    await copyArtifactsAndReport(envStatus)
  })

  test('M6-E2E-001~006: 元数据→指标→分析→查询→大屏→全屏', async ({ page, request }) => {
    if (!envStatus.p5777 || !envStatus.p48080 || !envStatus.p48094) {
      throw new Error(`环境未就绪: ${JSON.stringify(envStatus)}`)
    }

    const { authHeaders } = await seedFootballAuth(request, page)
    envProbe = await probeTargetTable(request, authHeaders)

    if (envProbe.targetRowCount === 0) {
      registerDefect({
        tcId: 'ENV-seed',
        testcasesRef: 'V165 oa_douyin_video',
        phenomenon: `tenant=1 ${ENV.metadataEntity} COUNT=0；oa_content=${envProbe.oaContentRowCount}；oa_douyin_video=${envProbe.oaDouyinVideoRowCount}`,
        expected: `${ENV.metadataEntity} 有 Douyin 采集行（数据不在 oa_content）`,
        steps: ['POST /oa/metric/preview COUNT', `POST /oa/query/${SEED.queryId}/execute`, `GET /oa/dashboard/${SEED.dashboardId}/data`],
        api: 'metric/preview',
      })
    }

    // M6-E2E-001
    await test.step(`M6-E2E-001: 元数据 ${ENV.metadataEntity}`, async () => {
      const tcId = 'M6-E2E-001'
      try {
        await gotoOpsPage(page, OPS_ROUTES.metadata)
        const row = page.locator('.el-table__row').filter({ hasText: ENV.metadataEntity }).first()
        await expect(row).toBeVisible({ timeout: 30_000 })
        await row.getByRole('button', { name: '字段维护' }).click()
        const fieldDrawer = page.locator('.el-drawer.open')
        await expect(fieldDrawer).toBeVisible({ timeout: 15_000 })
        await expect(fieldDrawer.locator('.el-table__row').first()).toBeVisible({ timeout: 30_000 })
        await page.keyboard.press('Escape')
        const shot = await captureStep(page, tcId, 'metadata_entity', 'pass')
        recordCase({ tcId, testcasesRef: 'V165', description: `${ENV.metadataEntity} 可读`, status: 'Pass', screenshot: shot.relPath })
      } catch (e) {
        const shot = await captureStep(page, tcId, 'metadata_entity', 'fail')
        registerDefect({ tcId, testcasesRef: 'V165', phenomenon: String(e), expected: '元数据可读', steps: [OPS_ROUTES.metadata], screenshot: shot.relPath })
        recordCase({ tcId, testcasesRef: 'V165', description: '元数据', status: 'Fail', screenshot: shot.relPath, note: String(e) })
        chainBlocked = true
      }
    })

    // M6-E2E-002
    await test.step('M6-E2E-002: 新建指标', async () => {
      const tcId = 'M6-E2E-002'
      if (chainBlocked) return blockCase(tcId, 'TC-M6-001-01', '新建指标', '前置失败')
      try {
        await gotoOpsPage(page, OPS_ROUTES.metric)
        await page.getByRole('button', { name: '新增指标' }).click()
        const dlg = page.getByRole('dialog').filter({ hasText: /新增指标/ })
        await dlg.getByLabel('指标名称').fill(runState.createdMetricName)
        await dlg.getByLabel('指标编码').fill(runState.createdMetricCode)
        const builder = dlg.locator('.metric-builder')
        await builder.locator('.el-form-item').filter({ hasText: '数据源' }).locator('.el-select').click()
        await pickSelectOption(page, new RegExp(`${ENV.metadataEntityName}|Douyin Video|${ENV.metadataEntity}`))
        await builder.getByRole('button', { name: '生成公式' }).click()
        await dlg.getByRole('button', { name: '保存' }).click()
        await expect(page.getByText('保存成功')).toBeVisible()
        const metric = (
          await (
            await request.get(`${API_BASE}/oa/metric/list`, {
              headers: authHeaders,
              params: { pageNo: 1, pageSize: 50, keyword: runState.createdMetricCode },
            })
          ).json()
        ).data?.list?.find((m: { metricCode: string }) => m.metricCode === runState.createdMetricCode)
        runState.createdMetricId = metric.id
        const shot = await captureStep(page, tcId, 'metric_create', 'pass')
        recordCase({ tcId, testcasesRef: 'TC-M6-001-01', description: `新建 ${runState.createdMetricCode}`, status: 'Pass', screenshot: shot.relPath })
      } catch (e) {
        const shot = await captureStep(page, tcId, 'metric_create', 'fail')
        registerDefect({ tcId, testcasesRef: 'TC-M6-001-01', phenomenon: String(e), expected: '新建指标', steps: [OPS_ROUTES.metric], screenshot: shot.relPath })
        recordCase({ tcId, testcasesRef: 'TC-M6-001-01', description: '新建指标', status: 'Fail', screenshot: shot.relPath, note: String(e) })
        chainBlocked = true
      }
    })

    // M6-E2E-003 — COUNT 指标聚合行（即使 oa_content=0 也有 1 行）
    await test.step('M6-E2E-003: 指标分析', async () => {
      const tcId = 'M6-E2E-003'
      if (chainBlocked) return blockCase(tcId, 'TC-M6-001-01', '指标分析', '前置失败')
      try {
        await gotoOpsPage(page, OPS_ROUTES.metricAnalysis)
        await page.locator('.search-card .el-select').first().click()
        await pickSelectOption(page, runState.createdMetricName)
        await page.getByRole('button', { name: '运行分析' }).click()
        await expect(page.getByText('分析完成')).toBeVisible({ timeout: 60_000 })
        await expect(page.locator('.row-count').first()).toContainText(/共\s+[1-9]\d*\s+行/)
        const shot = await captureStep(page, tcId, 'metric_analysis', 'pass')
        recordCase({ tcId, testcasesRef: 'TC-M6-001-01', description: '指标分析完成', status: 'Pass', screenshot: shot.relPath })
      } catch (e) {
        const shot = await captureStep(page, tcId, 'metric_analysis', 'fail')
        registerDefect({ tcId, testcasesRef: 'TC-M6-001-01', phenomenon: String(e), expected: '分析完成', steps: [OPS_ROUTES.metricAnalysis], screenshot: shot.relPath })
        recordCase({ tcId, testcasesRef: 'TC-M6-001-01', description: '指标分析', status: 'Fail', screenshot: shot.relPath, note: String(e) })
        chainBlocked = true
      }
    })

    // M6-E2E-004
    await test.step('M6-E2E-004: 自定义查询', async () => {
      const tcId = 'M6-E2E-004'
      if (chainBlocked) return blockCase(tcId, 'TC-M6-005-01/02', '自定义查询', '前置失败')
      let seedPreviewBlocked = envProbe.seedQueryTotal === 0
      try {
        await gotoOpsPage(page, OPS_ROUTES.customQuery)

        await page.getByRole('tab', { name: '我的查询' }).click()
        const seedRow = page.locator('.list-card .el-table__row').filter({ hasText: SEED.queryName }).first()
        if (await seedRow.isVisible({ timeout: 10_000 }).catch(() => false)) {
          await seedRow.getByRole('button', { name: '执行' }).click()
          const resultDialog = page.getByRole('dialog').filter({ hasText: /查询结果/ })
          await expect(resultDialog).toBeVisible()
          await expect(page.getByText('执行成功')).toBeVisible({ timeout: 60_000 })
          const dialogPanel = resultDialog.locator('.query-result-panel')
          if (!seedPreviewBlocked) {
            await expect(dialogPanel.locator('.row-count')).toContainText(/共\s+[1-9]\d*\s+行/)
            await expect(dialogPanel.locator('.el-table__row').first()).toBeVisible()
          }
          await page.keyboard.press('Escape')
        } else {
          seedPreviewBlocked = true
        }

        await page.getByRole('tab', { name: '自定义查询' }).click()
        const builder = page.locator('.query-builder')
        await builder.locator('.el-form-item').filter({ hasText: '数据源' }).locator('.el-select').click()
        await pickSelectOption(page, new RegExp(`${ENV.metadataEntityName}|Douyin Video|${ENV.metadataEntity}`))
        await builder.locator('.el-form-item').filter({ hasText: '展示字段' }).locator('.el-select').click()
        await pickSelectOption(page, /标题|title/)
        await page.keyboard.press('Escape')
        await builder.locator('.el-form-item').filter({ hasText: '展示字段' }).locator('.el-select').click()
        await pickSelectOption(page, /播放数|play_count/)
        await page.keyboard.press('Escape')
        await page.getByRole('button', { name: '执行查询' }).click()
        await expect(page.getByText('执行成功')).toBeVisible({ timeout: 60_000 })
        await expect(page.locator('.query-result-panel .row-count').first()).toContainText(/共\s+[1-9]\d*\s+行/)

        await page.getByRole('button', { name: '保存为我的查询' }).click()
        const saveDlg = page.getByRole('dialog').filter({ hasText: '保存为我的查询' })
        await saveDlg.getByLabel('查询名称').fill(runState.createdQueryName)
        await saveDlg.locator('.el-form-item').filter({ hasText: '状态' }).locator('.el-select').click()
        await pickSelectOption(page, '已发布')
        await saveDlg.getByRole('button', { name: '保存' }).click()
        await expect(page.getByText('保存成功')).toBeVisible()
        const query = (
          await (
            await request.get(`${API_BASE}/oa/query/list`, {
              headers: authHeaders,
              params: { pageNum: 1, pageSize: 50, status: 'PUBLISHED' },
            })
          ).json()
        ).data?.list?.find((q: { queryName: string }) => q.queryName === runState.createdQueryName)
        runState.createdQueryId = query.id

        const shot = await captureStep(page, tcId, 'custom_query_save', envProbe.targetRowCount > 0 ? 'pass' : 'fail')
        recordCase({
          tcId,
          testcasesRef: 'TC-M6-005-01/02',
          description:
            envProbe.targetRowCount > 0
              ? `${ENV.metadataEntity} 查询 ${envProbe.targetRowCount} 行 + 保存 id=${runState.createdQueryId}`
              : `保存 PUBLISHED id=${runState.createdQueryId}`,
          status: envProbe.targetRowCount > 0 ? 'Pass' : 'Blocked',
          screenshot: shot.relPath,
          note: seedPreviewBlocked ? 'seed9861 无行（依赖 oa_content）' : undefined,
        })
      } catch (e) {
        const shot = await captureStep(page, tcId, 'custom_query_save', 'fail')
        registerDefect({ tcId, testcasesRef: 'TC-M6-005-02', phenomenon: String(e), expected: '保存 PUBLISHED', steps: [OPS_ROUTES.customQuery], screenshot: shot.relPath })
        recordCase({ tcId, testcasesRef: 'TC-M6-005-01/02', description: '自定义查询', status: 'Fail', screenshot: shot.relPath, note: String(e) })
        chainBlocked = true
      }
    })

    // M6-E2E-005
    await test.step('M6-E2E-005: 大屏配置', async () => {
      const tcId = 'M6-E2E-005'
      if (chainBlocked || !runState.createdMetricId || !runState.createdQueryId) {
        return blockCase(tcId, 'TC-M6-007-01', '大屏配置', '无 metricId/queryId')
      }
      const dashboardName = `E2E-M6-${runState.ts}`
      try {
        await gotoOpsPage(page, OPS_ROUTES.screenConfig)
        await expect(page.locator('.screen-config-page, .ops-page')).toBeVisible({ timeout: 20_000 })
        await page.waitForResponse(
          (r) => r.url().includes('/oa/dashboard-config/list') && r.status() === 200,
          { timeout: 30_000 },
        ).catch(() => {})
        await page.waitForTimeout(1000)

        const toolbarSelect = page.locator('.config-toolbar .el-select').first()
        const kpiCard = page.locator('.config-card').filter({ hasText: '指标卡配置' })
        const selectNewTemplate = async () => {
          await toolbarSelect.click()
          const newTplOption = page.locator('.el-select-dropdown:visible').getByText('+ 新建模板', { exact: true })
          await expect(newTplOption).toBeVisible({ timeout: 10_000 })
          await newTplOption.click()
          await expect(page.locator('.config-left').getByLabel('模板名称')).toHaveValue('', { timeout: 10_000 })
          await expect(kpiCard.getByText('暂无指标卡')).toBeVisible({ timeout: 15_000 })
        }
        await selectNewTemplate()
        await page.locator('.config-left').getByLabel('模板名称').fill(dashboardName)
        await expect(page.locator('.config-left').getByLabel('模板名称')).toHaveValue(dashboardName)
        if (!(await kpiCard.getByText('暂无指标卡').isVisible())) {
          await selectNewTemplate()
        }

        const kpi = kpiCard
        await kpi.getByRole('button', { name: '添加' }).click()
        let dlg = widgetDialog(page)
        await expect(dlg).toBeVisible({ timeout: 15_000 })
        await dlg.getByLabel('标题').fill('E2E-METRIC')
        await pickDialogSourceType(page, dlg, '自定义指标')
        await dlg.locator('.el-select').nth(1).click()
        await pickSelectOption(page, runState.createdMetricName)
        await dlg.getByRole('button', { name: '确定' }).click()
        await expect(widgetDialog(page)).toHaveCount(0, { timeout: 15_000 })

        await kpi.getByRole('button', { name: '添加' }).click()
        dlg = widgetDialog(page)
        await expect(dlg).toBeVisible({ timeout: 15_000 })
        await dlg.getByLabel('标题').fill('E2E-QUERY')
        await pickDialogSourceType(page, dlg, '自定义查询')
        await dlg.locator('.el-select').nth(1).click()
        await pickSelectOption(page, runState.createdQueryName)
        await dlg.getByLabel('值字段').fill(ENV.queryValueKey)
        await dlg.getByRole('button', { name: '确定' }).click()
        await expect(dlg).toHaveCount(0, { timeout: 15_000 })

        const saveWait = page.waitForResponse(
          (r) =>
            (r.url().includes('/oa/dashboard/create') || r.url().includes('/oa/dashboard/full-update')) &&
            r.status() === 200,
          { timeout: 90_000 },
        )
        await page.getByRole('button', { name: '保存' }).click()
        await saveWait
        await expect(page.getByText('保存成功')).toBeVisible({ timeout: 30_000 })

        const created = (
          await (
            await request.get(`${API_BASE}/oa/dashboard-config/list`, {
              headers: authHeaders,
              params: { pageNum: 1, pageSize: 20 },
            })
          ).json()
        ).data?.list?.find((d: { dashboardName: string }) => d.dashboardName === dashboardName)
        expect(created?.id).toBeTruthy()
        runState.savedDashboardId = created.id
        const shot = await captureStep(page, tcId, 'screen_config_save', 'pass')
        recordCase({
          tcId,
          testcasesRef: 'TC-M6-007-01',
          description: `METRIC(${runState.createdMetricId})+QUERY(${runState.createdQueryId}) 大屏 id=${runState.savedDashboardId}`,
          status: 'Pass',
          screenshot: shot.relPath,
        })
      } catch (e) {
        let shot = { relPath: '—' }
        try {
          shot = await captureStep(page, tcId, 'screen_config_save', 'fail')
        } catch {
          /* page may be closed on timeout */
        }
        registerDefect({ tcId, testcasesRef: 'TC-M6-007-01', phenomenon: String(e), expected: '大屏保存', steps: [OPS_ROUTES.screenConfig], screenshot: shot.relPath })
        recordCase({ tcId, testcasesRef: 'TC-M6-007-01', description: '大屏配置', status: 'Fail', screenshot: shot.relPath, note: String(e) })
      }
    })

    // M6-E2E-006 — 自建大屏或 seed 98601 全屏
    await test.step('M6-E2E-006: 数据大屏全屏', async () => {
      const tcId = 'M6-E2E-006'
      if (chainBlocked) return blockCase(tcId, 'TC-M6-006-01', '数据大屏', '前置失败')
      const screenId = runState.savedDashboardId ?? SEED.dashboardId
      const screenLabel = runState.savedDashboardId ? `E2E-M6-${runState.ts}` : SEED.dashboardName
      try {
        const dashBody = await (
          await request.get(
            `${API_BASE}/oa/dashboard/${screenId}/data?startDate=2026-06-01&endDate=2026-07-27`,
            { headers: authHeaders },
          )
        ).json()
        expect((dashBody.data?.widgets ?? []).length).toBeGreaterThan(0)
        await gotoOpsPage(page, OPS_ROUTES.screenById(screenId))
        await expect(page.locator('.data-screen-fullscreen, .ops-page')).toBeVisible({ timeout: 20_000 })
        expect(await page.locator('.kpi-value, .stat-value').count()).toBeGreaterThan(0)

        const nonZero = envProbe.seedNonZeroWidgets > 0 || envProbe.targetRowCount > 0
        if (!nonZero) {
          const shot = await captureStep(page, tcId, 'data_screen', 'fail')
          recordCase({
            tcId,
            testcasesRef: 'TC-M6-006-01',
            description: `${screenLabel} 布局 OK；widget 数据全 0`,
            status: 'Blocked',
            screenshot: shot.relPath,
            note: runState.savedDashboardId ? '自建大屏' : 'seed 98601',
          })
          return
        }

        await page.waitForFunction(
          () =>
            Array.from(document.querySelectorAll('.kpi-value, .stat-value')).some((el) => {
              const t = (el.textContent || '').trim()
              return t && t !== '-' && t !== '—' && t !== '0'
            }),
          { timeout: 45_000 },
        )
        const shot = await captureStep(page, tcId, 'data_screen', 'pass')
        recordCase({
          tcId,
          testcasesRef: 'TC-M6-006-01',
          description: `${screenLabel} id=${screenId} widgets 非空`,
          status: 'Pass',
          screenshot: shot.relPath,
        })
      } catch (e) {
        const shot = await captureStep(page, tcId, 'data_screen', 'fail')
        registerDefect({ tcId, testcasesRef: 'TC-M6-006-01', phenomenon: String(e), expected: '大屏非空', steps: [OPS_ROUTES.screenById(screenId)], screenshot: shot.relPath })
        recordCase({ tcId, testcasesRef: 'TC-M6-006-01', description: '数据大屏', status: 'Fail', screenshot: shot.relPath, note: String(e) })
      }
    })

    const failed = caseRecords.filter((c) => c.status === 'Fail')
    if (failed.length) throw new Error(`M6 E2E 失败: ${failed.map((f) => f.tcId).join(', ')}`)
  })
})
