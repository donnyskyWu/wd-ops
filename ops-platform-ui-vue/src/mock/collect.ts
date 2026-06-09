/**
 * M10 数据采集 - Mock 数据
 */
import type { CollectTaskVO, CollectLogVO, QualityCheckVO, QualityLogVO } from '@/api/collect'

export const mockCollectTasks: CollectTaskVO[] = [
  {
    id: 1,
    name: '抖音-AI技术前沿 主页数据',
    platformType: 'DOUYIN',
    accountId: 1001,
    accountName: 'AI技术前沿',
    method: 'API',
    source: 'OFFICIAL',
    frequency: 'HOUR',
    cron: '0 0 * * * ?',
    status: 'ENABLED',
    lastRunAt: '2026-06-08 10:00:00',
    nextRunAt: '2026-06-08 11:00:00',
    runCount: 240,
    failCount: 3,
    createdAt: '2026-05-01 10:00:00',
  },
  {
    id: 2,
    name: '快手-知识变现 视频数据',
    platformType: 'KUAISHOU',
    accountId: 1002,
    accountName: '知识变现研究院',
    method: 'SCRAPER',
    source: 'THIRD_PARTY',
    frequency: 'DAY',
    cron: '0 0 2 * * ?',
    status: 'ENABLED',
    lastRunAt: '2026-06-08 02:00:00',
    nextRunAt: '2026-06-09 02:00:00',
    runCount: 38,
    failCount: 0,
    createdAt: '2026-05-15 10:00:00',
  },
  {
    id: 3,
    name: '视频号-财经日报 订单数据',
    platformType: 'VIDEO_ACCOUNT',
    accountId: 1003,
    accountName: '财经日报',
    method: 'API',
    source: 'OFFICIAL',
    frequency: 'HALF_HOUR',
    cron: '0 0/30 * * * ?',
    status: 'DISABLED',
    lastRunAt: '2026-06-07 23:30:00',
    nextRunAt: '-',
    runCount: 145,
    failCount: 12,
    createdAt: '2026-04-20 10:00:00',
  },
]

export const mockCollectLogs: CollectLogVO[] = [
  { id: 1, taskId: 1, taskName: '抖音-AI技术前沿 主页数据', status: 'SUCCESS', startAt: '2026-06-08 10:00:00', durationMs: 3200, recordCount: 28, errorMessage: '' },
  { id: 2, taskId: 1, taskName: '抖音-AI技术前沿 主页数据', status: 'FAILED', startAt: '2026-06-08 09:00:00', durationMs: 15000, recordCount: 0, errorMessage: '接口超时' },
  { id: 3, taskId: 2, taskName: '快手-知识变现 视频数据', status: 'SUCCESS', startAt: '2026-06-08 02:00:00', durationMs: 5800, recordCount: 120, errorMessage: '' },
  { id: 4, taskId: 3, taskName: '视频号-财经日报 订单数据', status: 'PARTIAL', startAt: '2026-06-07 23:30:00', durationMs: 8500, recordCount: 15, errorMessage: '部分接口 429' },
]

export const mockQualityChecks: QualityCheckVO[] = [
  { id: 1, name: '账号粉丝数非空', checkType: 'NOT_NULL', level: 'ERROR', tableName: 'oa_account', rule: 'follower_count IS NOT NULL', enabled: true, lastCheckAt: '2026-06-08 09:00:00', passRate: 99.5 },
  { id: 2, name: '内容标题长度检查', checkType: 'RANGE', level: 'WARN', tableName: 'oa_content', rule: 'CHAR_LENGTH(title) BETWEEN 5 AND 100', enabled: true, lastCheckAt: '2026-06-08 09:00:00', passRate: 87.3 },
  { id: 3, name: '订单金额一致性', checkType: 'CONSISTENT', level: 'ERROR', tableName: 'oa_order', rule: 'amount == quantity * unit_price', enabled: true, lastCheckAt: '2026-06-08 09:00:00', passRate: 100 },
  { id: 4, name: '作品发布平台分布', checkType: 'DISTRIBUTION', level: 'INFO', tableName: 'oa_works', rule: 'platform_type IN allowed_list', enabled: false, lastCheckAt: '-', passRate: undefined },
]

export const mockQualityLogs: QualityLogVO[] = [
  { id: 1, checkId: 1, checkName: '账号粉丝数非空', checkTime: '2026-06-08 09:00:00', totalCount: 2450, passCount: 2438, failCount: 12, level: 'ERROR' },
  { id: 2, checkId: 2, checkName: '内容标题长度检查', checkTime: '2026-06-08 09:00:00', totalCount: 1280, passCount: 1117, failCount: 163, level: 'WARN' },
  { id: 3, checkId: 3, checkName: '订单金额一致性', checkTime: '2026-06-08 09:00:00', totalCount: 5680, passCount: 5680, failCount: 0, level: 'ERROR' },
]
