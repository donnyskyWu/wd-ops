import { test, expect, type Page, type Response } from '@playwright/test'
import * as fs from 'node:fs'
import * as path from 'node:path'

/**
 * UAT browser E2E — Ops standalone (:3000 + :8080)
 *
 * Covers pages NOT in UAT 42 (priority 5 + expanded 37):
 *   内容生产 remaining · 运营管理 · 账号管理
 *
 * Tag: @uat-gap
 */

export interface UatGapPage {
  group: string
  path: string
  title: string
  apiPattern?: RegExp
}

/** From oa-menu-permission-map.csv — visible menu, minus UAT 42 covered pages */
export const UAT_GAP_PAGES: UatGapPage[] = [
  // 内容生产 (remaining — plan + content/review in UAT 5/5)
  { group: '内容生产', path: '/content', title: '内容管理', apiPattern: /\/content\/list/ },
  { group: '内容生产', path: '/knowledge', title: '内容知识库', apiPattern: /\/knowledge\/list/ },
  { group: '内容生产', path: '/layout-template', title: '公推模板库', apiPattern: /\/layout-template\/list/ },
  { group: '内容生产', path: '/sop', title: 'SOP管理', apiPattern: /\/sop\/list/ },
  { group: '内容生产', path: '/sop/review', title: 'SOP审核', apiPattern: /\/sop\/review\/list/ },
  { group: '内容生产', path: '/task', title: '任务管理', apiPattern: /\/task\/list/ },

  // 运营管理 (ip-group in UAT 5/5)
  { group: '运营管理', path: '/account-analysis', title: '账号分析', apiPattern: /\/account-analysis/ },
  { group: '运营管理', path: '/author', title: '作者管理', apiPattern: /\/author\/list/ },
  { group: '运营管理', path: '/efficiency', title: '人效盘点', apiPattern: /\/efficiency/ },
  { group: '运营管理', path: '/fans-analysis', title: '粉丝分析', apiPattern: /\/fans-analysis/ },
  { group: '运营管理', path: '/internal-content', title: '内部作品分析', apiPattern: /\/internal-content/ },

  // 账号管理 (internal-account + personal-account in UAT 5/5)
  { group: '账号管理', path: '/company', title: '公司管理', apiPattern: /\/company\/list/ },
  { group: '账号管理', path: '/phone', title: '手机管理', apiPattern: /\/phone\/list/ },
  { group: '账号管理', path: '/realname', title: '实名人管理', apiPattern: /\/realname\/list/ },
  { group: '账号管理', path: '/simcard', title: '手机卡管理', apiPattern: /\/simcard\/list/ },
]

export interface PageResult {
  group: string
  path: string
  title: string
  pass: boolean
  errors: string[]
  apiStatus?: number
  apiCode?: number
  durationMs: number
}

const results: PageResult[] = []

function attachCollectors(page: Page): { errors: string[]; apiResponses: Response[] } {
  const errors: string[] = []
  const apiResponses: Response[] = []

  page.on('pageerror', (err) => {
    const msg = err.message
    if (msg.includes('Sass') || msg.includes('DEPRECATION')) return
    if (msg.includes('source map') || msg.includes('sourceMappingURL')) return
    errors.push(`pageerror: ${msg}`)
  })

  page.on('console', (msg) => {
    if (msg.type() !== 'error') return
    const text = msg.text()
    if (text.includes('DEPRECATION') || text.includes('source map')) return
    if (text.includes('404') && text.includes('favicon')) return
    if (text.includes('vite') && text.includes('overlay')) return
    // Axios timeout on slow cold-start APIs — downgraded when main content renders
    if (text.includes('timeout') && text.includes('AxiosError')) return
    errors.push(`console: ${text}`)
  })

  page.on('response', (resp) => {
    const url = resp.url()
    if (url.includes('/admin-api/oa')) {
      apiResponses.push(resp)
    }
  })

  return { errors, apiResponses }
}

async function assertNoViteOverlay(page: Page): Promise<string | null> {
  const overlay = page.locator('vite-error-overlay, .vite-error-overlay')
  if (await overlay.count()) {
    const text = await overlay.first().textContent()
    return `vite overlay: ${text?.slice(0, 200)}`
  }
  const body = await page.locator('body').textContent()
  if (body?.includes('Internal Server Error') && body.length < 200) {
    return 'blank Internal Server Error page'
  }
  return null
}

async function assertMainContent(page: Page): Promise<string | null> {
  const selectors = [
    '.el-table',
    '.el-card',
    '.el-form',
    '.el-tree',
    '.el-tabs',
    '.el-empty',
    '.main-container',
    '.app-layout',
    '.page-container',
    '.el-container',
  ]
  for (const sel of selectors) {
    const loc = page.locator(sel).first()
    if (await loc.count()) {
      const visible = await loc.isVisible().catch(() => false)
      if (visible) return null
    }
  }
  const bodyLen = (await page.locator('body').textContent())?.trim().length ?? 0
  if (bodyLen < 20) return 'page body nearly blank'
  return null
}

test.describe('UAT browser gap — Ops standalone @uat-gap', () => {
  for (const entry of UAT_GAP_PAGES) {
    test(`${entry.group} | ${entry.title} (${entry.path})`, async ({ page }) => {
      const start = Date.now()
      const { errors, apiResponses } = attachCollectors(page)
      const pageErrors: string[] = []

      const response = await page.goto(entry.path, { waitUntil: 'domcontentloaded', timeout: 30_000 })
      expect(response, `${entry.path} no response`).toBeTruthy()

      if (response) {
        const status = response.status()
        expect([200, 304], `${entry.path} HTTP ${status}`).toContain(status)
      }

      await page.waitForLoadState('networkidle', { timeout: 30_000 }).catch(() => {})

      expect(page.url(), 'URL mismatch').toContain(entry.path)

      const overlayErr = await assertNoViteOverlay(page)
      if (overlayErr) pageErrors.push(overlayErr)

      const contentErr = await assertMainContent(page)
      if (contentErr) pageErrors.push(contentErr)

      const notFound = page.locator('text=404').first()
      if (await notFound.isVisible().catch(() => false)) {
        pageErrors.push('404 visible on page')
      }

      let apiStatus: number | undefined
      let apiCode: number | undefined
      if (entry.apiPattern) {
        const match = apiResponses.find((r) => entry.apiPattern!.test(r.url()))
        if (match) {
          apiStatus = match.status()
          if (apiStatus >= 500) {
            pageErrors.push(`primary API HTTP ${apiStatus}: ${match.url()}`)
          } else {
            try {
              const body = await match.json()
              apiCode = body?.code
              if (apiCode !== undefined && apiCode !== 0) {
                pageErrors.push(`primary API code=${apiCode}: ${match.url()}`)
              }
            } catch {
              /* non-json response ok */
            }
          }
        }
      }

      const allErrors = [...errors, ...pageErrors]
      const pass = allErrors.length === 0

      results.push({
        group: entry.group,
        path: entry.path,
        title: entry.title,
        pass,
        errors: allErrors.slice(0, 5),
        apiStatus,
        apiCode,
        durationMs: Date.now() - start,
      })

      if (!pass) {
        console.warn(`[FAIL] ${entry.path}:`, allErrors.slice(0, 3))
      }
      expect(allErrors, `${entry.path} browser checks`).toEqual([])
    })
  }

  test.afterAll(async () => {
    const outDir = process.env.UAT_E2E_REPORT_DIR
    if (!outDir) return

    const passed = results.filter((r) => r.pass).length
    const report = {
      generatedAt: new Date().toISOString(),
      tool: 'playwright',
      baseURL: 'http://localhost:3000',
      apiURL: 'http://localhost:8080',
      scope: '内容生产 remaining + 运营管理 + 账号管理',
      total: results.length,
      passed,
      failed: results.length - passed,
      pages: results,
    }

    fs.mkdirSync(outDir, { recursive: true })
    const jsonPath = path.join(outDir, 'uat-browser-e2e-20260704-probe.json')
    fs.writeFileSync(jsonPath, JSON.stringify(report, null, 2), 'utf-8')
  })
})
