import { getLedIpGroups } from '@/api/ip-group'
import { extractApiErrorMessage } from '@/utils/apiError'

const OPS_DATA_ADMIN_ROLES = ['ROLE_OA_ADMIN', 'ROLE_TENANT_ADMIN']

function readUserPermissions(): string[] {
  try {
    const cached = localStorage.getItem('userPermissions')
    return cached ? (JSON.parse(cached) as string[]) : []
  } catch {
    return []
  }
}

/** 系统管理员（dataScope=ALL 近似判定，供前端 UX 分支） */
export function isOpsDataAdmin(): boolean {
  const perms = readUserPermissions()
  return OPS_DATA_ADMIN_ROLES.some((role) => perms.includes(role))
}

/** 识别后端 403 / 无权限业务错误 */
export function isForbiddenError(e: unknown): boolean {
  const msg = extractApiErrorMessage(e, '')
  return /无权限|403|forbidden/i.test(msg)
}

/**
 * 人效盘点（6156）：admin 或 IP 组长可筛组员；普通用户仅看自己
 */
export async function canViewTeamProductivity(): Promise<boolean> {
  if (isOpsDataAdmin()) {
    return true
  }
  try {
    const led = await getLedIpGroups()
    return Array.isArray(led) && led.length > 0
  } catch {
    return false
  }
}

/** 分析页 IP 组筛选说明：后端已强制数据范围，前端仅缩小 */
export const DATA_SCOPE_FILTER_HINT =
  '列表已按您的数据权限过滤；不选 IP 组时展示权限范围内全部数据'

export const DATA_SCOPE_EMPTY_TEXT = '暂无数据（已按数据权限过滤）'
