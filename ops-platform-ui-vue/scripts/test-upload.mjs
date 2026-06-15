/**
 * Mimics frontend uploadContentImage() after fix — must return 200
 */
import axios from 'axios'
import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const imgPath = path.resolve(
  __dirname,
  '../../ops-platform-server/ops-platform-module-oa/data/uploads/1/content/cbb02829817a4e679e2610ed935c8b0b_test.png'
)

const service = axios.create({
  baseURL: '/admin-api',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

service.interceptors.request.use((config) => {
  const token = 'dev-token-oa-admin'
  const tenantId = '1'
  config.headers.Authorization = `Bearer ${token}`
  config.headers['X-Tenant-Id'] = tenantId

  if (config.data instanceof FormData) {
    if (config.headers?.setContentType) config.headers.setContentType(undefined)
    if (config.headers?.delete) {
      config.headers.delete('Content-Type')
      config.headers.delete('content-type')
    }
    config.transformRequest = [(data) => data]
  }
  return config
})

const request = {
  post(config) {
    const { url, data, ...rest } = config
    return service.post(url, data, rest)
  },
}

const formData = new FormData()
formData.append('file', new Blob([fs.readFileSync(imgPath)], { type: 'image/png' }), 'test.png')

try {
  const res = await request.post({
    url: 'http://localhost:8080/admin-api/oa/file/upload',
    data: formData,
    headers: { 'Content-Type': undefined },
    transformRequest: [(data) => data],
  })
  console.log('SUCCESS', res.status, res.data?.code)
} catch (err) {
  console.log('FAIL', err.response?.status, err.response?.data ?? err.message)
  process.exit(1)
}
