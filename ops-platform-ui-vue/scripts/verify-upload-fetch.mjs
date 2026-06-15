/**
 * Browser-equivalent upload test via fetch + FormData through Vite proxy (port 3000).
 */
import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const imgPath = path.resolve(
  __dirname,
  '../../ops-platform-server/ops-platform-module-oa/data/uploads/1/content/cbb02829817a4e679e2610ed935c8b0b_test.png'
)

const PROXY = process.env.VITE_PROXY || 'http://localhost:3000'
const token = 'dev-token-oa-admin'
const tenantId = '1'

const formData = new FormData()
formData.append('file', new Blob([fs.readFileSync(imgPath)], { type: 'image/png' }), 'test.png')

const res = await fetch(`${PROXY}/admin-api/oa/file/upload`, {
  method: 'POST',
  headers: {
    Authorization: `Bearer ${token}`,
    'X-Tenant-Id': tenantId,
  },
  body: formData,
})

const body = await res.json()
if (res.status === 200 && (body.code === 0 || body.code === 200)) {
  console.log('SUCCESS fetch via proxy', res.status, body.code, body.data?.key)
} else {
  console.log('FAIL fetch via proxy', res.status, body)
  process.exit(1)
}
