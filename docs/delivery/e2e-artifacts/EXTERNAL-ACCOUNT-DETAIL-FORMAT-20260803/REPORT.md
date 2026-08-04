# EXTERNAL-ACCOUNT-DETAIL-FORMAT-20260803

> 外部账号分析详情抽屉：发布时间 + 账号基本信息展示格式修复

## Root cause

| # | 现象 | 根因 |
|---|------|------|
| 1 | 发布时间显示 `1785639617000` 等原始数字 | 后端 `LocalDateTime` 经 `TimestampLocalDateTimeSerializer` 序列化为 **epoch 毫秒**；详情表格列直接绑定 `publishTime` prop，**未调用 `formatDateTime`** |
| 2 | 账号基本信息字段/数值格式不对 | 抽屉使用单列 `el-descriptions` + 通用标签「总播放/阅读」，与列表各 tab 列定义不一致；数值未 `toLocaleString('zh-CN')`；空值无 `-` 占位 |

### 数据流（修复前）

```
GET /ops/monitor/external/list
  → publishTime: 1785639617000 (Jackson ms timestamp)

mapExternalWork
  → publishTime: String(1785639617000)  // "1785639617000"

ExternalAccountAnalysis drawer
  → <el-table-column prop="publishTime" />  // 原样渲染
  → descriptions 混用 views/readingCount/followerCount
```

**结论**：后端 VO 正常；问题在前端 **缺少 formatter** + **map 层 String 化 epoch** 使 `dayjs("1785639617000")` 无法解析。

## Fix

| 文件 | 变更 |
|------|------|
| `utils/ops/index.ts` | `formatDateTime` 增强：支持 epoch ms 数字、纯数字字符串（秒/毫秒自动判断）、ISO |
| `utils/ops/monitor-map.ts` | 新增 `normalizePublishTime`；`mapExternalWork` 保留数值型 timestamp |
| `views/ops/account/ExternalAccountAnalysis.vue` | 详情抽屉「账号基本信息」按 tab 对齐列表字段；数字/百分比/日期统一格式化 |

**未改后端**：API 返回格式符合项目 Jackson 约定，与其他 ops 页一致。

## Before / After

| 场景 | Before | After |
|------|--------|-------|
| 发布时间 | `1785639617000` | `2026-08-02 09:40:17`（示例，依采集数据） |
| 空发布时间 | 空白 | `-` |
| 抖音基本信息 | 单列 + 「总播放/阅读」混用 sum/max | 双列：总播放(估)/获赞数/平均播放/互动率，与列表一致 |
| 获赞数 | `311246`（无千分位） | `311,246` |
| 互动率 | `12.5%`（裸数字拼接） | `12.50%`（`formatPercent`） |

## Verification

- `read_lints` on changed Vue/TS files → **无新增 lint**
- 手动：打开 `#/ops/external-account` → 抖音 tab → 详情 → 确认基本信息与作品表时间/数字格式

## Notes

- `formatDateTime` 增强为全局 ops 工具，其他 monitor 页若传入 epoch 字符串亦受益
- 采集写入仍用 `ExternalAccountCollectExecutor.fillWorkFields`（epoch **秒** → `LocalDateTime`）；与 API ms 序列化不冲突
