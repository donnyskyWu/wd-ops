import { test, expect } from '@playwright/test'
import path from 'path'
import { fileURLToPath } from 'url'
import fs from 'fs'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const testImage = path.resolve(
  __dirname,
  '../../ops-platform-server/ops-platform-module-oa/data/uploads/1/content/cbb02829817a4e679e2610ed935c8b0b_test.png',
)

test('browser fetch FormData upload via Vite proxy returns 200', async ({ page }) => {
  await page.goto('/content/edit')

  const imageBytes = fs.readFileSync(testImage)
  const result = await page.evaluate(async (bytes) => {
    const formData = new FormData()
    formData.append('file', new Blob([new Uint8Array(bytes)], { type: 'image/png' }), 'test.png')

    const token = 'dev-token-oa-admin'
    const tenantId = '1'
    const res = await fetch('/admin-api/oa/file/upload', {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${token}`,
        'X-Tenant-Id': tenantId,
      },
      body: formData,
    })
    const body = await res.json()
    return { status: res.status, code: body.code, url: body.data?.url }
  }, Array.from(imageBytes))

  expect(result.status, 'browser fetch upload must not be 403').toBe(200)
  expect(result.code).toBe(0)
  expect(result.url).toContain('/oa/file/view')
})

test('RichTextEditor image upload returns 200', async ({ page }) => {
  await page.goto('/layout-template/create')
  await page.getByRole('tab', { name: '富文本编辑' }).click()

  const uploadResponse = page.waitForResponse(
    (res) => res.url().includes('/admin-api/oa/file/upload') && res.request().method() === 'POST',
  )

  await page.getByRole('button', { name: '插入图片' }).click()
  await page.locator('.rte-image-input').setInputFiles(testImage)

  const response = await uploadResponse
  expect(response.status(), 'editor upload should not be 403').toBe(200)

  const json = await response.json()
  expect(json.code).toBe(0)
  expect(json.data?.url).toContain('/oa/file/view')
})
