/**
 * 文件上传 — Phase A 对齐 D-INF-01：Football `/admin-api/infra/file/upload`
 *
 * Admin 返回 CommonResult&lt;String&gt;（url/path）。
 * 旧 `/admin-api/oa/file/*` 仅为本地盘过渡；历史 key 预览仍可走 oa/file/view。
 */
import { getFileAuthParams } from '@/utils/fileUrl'

export interface FileUploadVO {
  name: string
  key: string
  url: string
}

const IMAGE_ACCEPT = 'image/jpeg,image/png,image/gif,image/webp'
const MAX_IMAGE_BYTES = 5 * 1024 * 1024

/** @deprecated 过渡期本地盘预览前缀；新上传应使用 infra 返回的绝对/相对 url */
export const OA_FILE_VIEW_PREFIX = '/admin-api/oa/file/view?key='

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

function asUploadVo(name: string, urlOrPath: string): FileUploadVO {
  const url = urlOrPath
  // 业务字段：优先存 infra 返回的 url/path（可直接预览）；无独立 key 时 key=url
  return { name, key: urlOrPath, url }
}

export async function uploadContentImage(file: File): Promise<FileUploadVO> {
  const err = validateImageFile(file)
  if (err) throw new Error(err)

  const formData = new FormData()
  formData.append('file', file)

  const { token, tenantId } = getFileAuthParams()
  const headers: Record<string, string> = {
    'tenant-id': tenantId,
    'X-Tenant-Id': tenantId,
  }
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  const response = await fetch('/admin-api/infra/file/upload', {
    method: 'POST',
    headers,
    body: formData,
  })

  let body: { code?: number; msg?: string; data?: string | FileUploadVO } = {}
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
  if (body.data == null || body.data === '') {
    throw new Error('图片上传失败：响应无数据')
  }

  if (typeof body.data === 'string') {
    return asUploadVo(file.name, body.data)
  }
  const vo = body.data
  return {
    name: vo.name || file.name,
    key: vo.key || vo.url,
    url: vo.url || vo.key,
  }
}

export { IMAGE_ACCEPT, MAX_IMAGE_BYTES }
