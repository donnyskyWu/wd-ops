/**
 * OA 文件上传（富文本图片等）
 * 使用原生 fetch + FormData，避免 axios 默认 Content-Type: application/json
 * 导致 multipart 请求被 Spring Security 拒绝（HTTP 403）。
 */
import { getFileAuthParams } from '@/utils/fileUrl'

export interface FileUploadVO {
  name: string
  key: string
  url: string
}

const IMAGE_ACCEPT = 'image/jpeg,image/png,image/gif,image/webp'
const MAX_IMAGE_BYTES = 5 * 1024 * 1024

export function validateImageFile(file: File): string | null {
  if (!file.type.startsWith('image/')) {
    const ext = file.name.split('.').pop()?.toLowerCase() || ''
    if (!['jpg', 'jpeg', 'png', 'gif', 'webp'].includes(ext)) {
      return '仅支持 jpg/png/gif/webp 图片'
    }
  }
  if (file.size > MAX_IMAGE_BYTES) {
    return '图片大小不能超过 5MB'
  }
  return null
}

export async function uploadContentImage(file: File): Promise<FileUploadVO> {
  const err = validateImageFile(file)
  if (err) throw new Error(err)

  const formData = new FormData()
  formData.append('file', file)

  const { token, tenantId } = getFileAuthParams()
  const headers: Record<string, string> = {
    'X-Tenant-Id': tenantId,
  }
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  const response = await fetch('/admin-api/oa/file/upload', {
    method: 'POST',
    headers,
    body: formData,
  })

  let body: { code?: number; msg?: string; data?: FileUploadVO } = {}
  try {
    body = await response.json()
  } catch {
    // non-JSON error body
  }

  if (!response.ok) {
    throw new Error(body.msg || `图片上传失败 (${response.status})`)
  }
  if (body.code !== undefined && body.code !== 0 && body.code !== 200) {
    throw new Error(body.msg || '图片上传失败')
  }
  if (!body.data) {
    throw new Error('图片上传失败：响应无数据')
  }
  return body.data
}

export { IMAGE_ACCEPT, MAX_IMAGE_BYTES }
