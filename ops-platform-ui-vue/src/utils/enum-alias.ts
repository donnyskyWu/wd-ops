/**
 * 前端 enum ↔ 后端 dict value 集中 alias
 *
 * S-R3 关键修复：前端代码历史上用了自己的 enum 字面值（WECHAT_MP / WEWORK / VIDEO_ACCOUNT / WECHAT_WORK / SERVICE_ACCOUNT 当平台等），
 * 但后端 dict 真实值是 WECHAT_OFFICIAL / WEWORK / WECHAT_VIDEO / WEWORK / 平台=公众号+account_type=服务号。
 *
 * 全部 30+ 文件的 enum 字面值本 slice **不改**（破坏性大、跨模块），
 * 而是在 5 个高频调用页面（AccountAnalysis/FansAnalysis/ContentAnalysis/InternalContent/Efficiency）使用本工具转换。
 *
 * 后续 S-R4 slice：把所有 enum 字面值替换为本文件中的常量。
 */

export type PlatformType =
  | 'WECHAT_OFFICIAL'   // 公众号
  | 'WECHAT_VIDEO'      // 视频号
  | 'DOUYIN'            // 抖音
  | 'KUAISHOU'          // 快手
  | 'XIAOHONGSHU'       // 小红书
  | 'WEWORK'            // 企业微信
  | 'WECHAT_PERSONAL'   // 个微
  | 'ALL'

/**
 * 平台别名映射：前端旧值/不规范值 → 后端 dict 真实值
 *
 *   WECHAT_MP        → WECHAT_OFFICIAL
 *   VIDEO_ACCOUNT    → WECHAT_VIDEO
 *   WECHAT_WORK      → WEWORK
 *   SERVICE_ACCOUNT  → WECHAT_OFFICIAL（服务号 = 平台=公众号 + account_type=服务号）
 */
export const PLATFORM_ALIAS: Record<string, PlatformType> = {
  WECHAT_MP: 'WECHAT_OFFICIAL',
  VIDEO_ACCOUNT: 'WECHAT_VIDEO',
  WECHAT_WORK: 'WEWORK',
  WEWORK: 'WEWORK',
  SERVICE_ACCOUNT: 'WECHAT_OFFICIAL',
  DOUYIN: 'DOUYIN',
  KUAISHOU: 'KUAISHOU',
  XIAOHONGSHU: 'XIAOHONGSHU',
  ALL: 'ALL',
}

/**
 * 把前端值规范成后端值（永远返回后端 dict 真实值；空 → undefined）
 */
export function normalizePlatform(value: string | null | undefined): string | undefined {
  if (value === null || value === undefined || value === '') return undefined
  const v = String(value).toUpperCase()
  return PLATFORM_ALIAS[v] ?? v
}

/**
 * 平台中文 label（前端显示用）
 */
export const PLATFORM_LABEL: Record<PlatformType, string> = {
  WECHAT_OFFICIAL: '公众号',
  WECHAT_VIDEO: '视频号',
  DOUYIN: '抖音',
  KUAISHOU: '快手',
  XIAOHONGSHU: '小红书',
  WEWORK: '企微',
  WECHAT_PERSONAL: '个微',
  ALL: '全部',
}

// ==================== AccountStatus ====================

export type AccountStatus = 'NORMAL' | 'DISABLED'

/**
 * 账号状态别名：前端历史值 → 后端 dict 真实值
 *
 *   ACTIVE / ENABLED / '0'   → NORMAL
 *   INACTIVE / DISABLED / '1' → DISABLED
 *   BANNED                   → DISABLED
 */
export const ACCOUNT_STATUS_ALIAS: Record<string, AccountStatus> = {
  ACTIVE: 'NORMAL',
  ENABLED: 'NORMAL',
  NORMAL: 'NORMAL',
  '0': 'NORMAL',
  INACTIVE: 'DISABLED',
  DISABLED: 'DISABLED',
  BANNED: 'DISABLED',
  '1': 'DISABLED',
}

export function normalizeAccountStatus(value: string | null | undefined): string | undefined {
  if (value === null || value === undefined || value === '') return undefined
  return ACCOUNT_STATUS_ALIAS[String(value).toUpperCase()] ?? String(value)
}

// ==================== TimeDimension ====================
// 后端 dict_time_dimension 真实值：DAY / WEEK / MONTH（大写）
// 前端 enum 沿用小写 day/week/month（与 IT/历史一致），加载时 DictSelect 自动取后端大写，
// 此处仅作 API 入参归一化。

export const TIME_DIMENSION_ALIAS: Record<string, string> = {
  DAY: 'DAY',
  WEEK: 'WEEK',
  MONTH: 'MONTH',
  day: 'DAY',
  week: 'WEEK',
  month: 'MONTH',
  d: 'DAY',
  w: 'WEEK',
  m: 'MONTH',
}

/**
 * 归一化时间维度：大小写不敏感；空 → undefined（=不传，由后端按 day 处理）
 */
export function normalizeTimeDimension(value: string | null | undefined): string | undefined {
  if (value === null || value === undefined || value === '') return undefined
  return TIME_DIMENSION_ALIAS[String(value).toLowerCase()] ?? String(value).toUpperCase()
}

// ==================== ContentType ====================

export type ContentType = 'SHORT_VIDEO' | 'ARTICLE' | 'VIDEO' | 'IMAGE_TEXT' | 'LIVE' | 'ALL'

export const CONTENT_TYPE_ALIAS: Record<string, ContentType> = {
  SHORT_VIDEO: 'SHORT_VIDEO',
  ARTICLE: 'ARTICLE',
  VIDEO: 'VIDEO',
  LIVE: 'LIVE',
  IMAGE_TEXT: 'IMAGE_TEXT',
  ALL: 'ALL',
}

export function normalizeContentType(value: string | null | undefined): string | undefined {
  if (value === null || value === undefined || value === '') return undefined
  return CONTENT_TYPE_ALIAS[String(value).toUpperCase()] ?? String(value)
}

// ==================== AccountType ====================

export type AccountType = 'OFFICIAL_ACCOUNT' | 'PERSONAL_ACCOUNT' | 'SERVICE_ACCOUNT'

export const ACCOUNT_TYPE_LABEL: Record<AccountType, string> = {
  OFFICIAL_ACCOUNT: '官方账号',
  PERSONAL_ACCOUNT: '个人账号',
  SERVICE_ACCOUNT: '服务号',
}
