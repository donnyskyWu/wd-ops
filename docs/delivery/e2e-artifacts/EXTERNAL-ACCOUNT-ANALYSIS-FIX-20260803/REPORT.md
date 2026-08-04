# EXTERNAL-ACCOUNT-ANALYSIS-FIX-20260803

> 外部账号分析：列表账号名错误 + 详情无采集作品

## Root cause

| # | 现象 | 根因 |
|---|------|------|
| 1 | 列表「账号名称」不对（如 `账号 #12` 或采集配置名） | `GET /ops/monitor/external/list` 返回 `ExternalWorkVO` **未填充 `accountName`**；前端 `mapExternalWork` 回退为 `` `账号 #${accountId}` ``，聚合后列表展示错误 |
| 2 | 详情看不到采集作品 / 作品数为 0 | **双重原因**：① 非 admin 用户数据权限对 `ip_group_id IN (...)` 过滤，Channel-D 写入的 `oa_external_work.ip_group_id` 为 **NULL**，被整批排除；② 详情抽屉仅 `el-descriptions` 汇总，**未展示作品明细**（即使 API 有数据） |

### 数据流（修复前）

```
ExternalAccountCollectExecutor
  → oa_external_account.display_name（profile nickname）
  → oa_external_work（account_id FK，ip_group_id = null）

MonitorServiceImpl.externalList
  → 仅查 oa_external_work，toVO 无 accountName
  → ipGroup scope: IN scopedGroups（不含 NULL）

ExternalAccountAnalysis.vue
  → aggregateWorksByAccount(works) → 列表 accountName 来自 work.accountName（空）
  → 详情 drawer 无作品 table
```

## Fix

| 文件 | 变更 |
|------|------|
| `ExternalWorkVO.java` | 新增 `accountName` |
| `MonitorServiceImpl.java` | 批量加载 `oa_external_account.display_name`；数据权限 `ip_group_id IN scope OR ip_group_id IS NULL` |
| `ExternalAccountAnalysis.vue` | 缓存 tab 级 works；详情抽屉增加「采集作品」表格 |

## Before / After

| 场景 | Before | After |
|------|--------|-------|
| 抖音 tab 列表（config 42 采集后） | `账号 #<id>` 或空名 | 显示 collector profile nickname（如「毒舌电影」） |
| 非 admin + 外部采集作品 | 列表/聚合为空 | 可见 `ip_group_id IS NULL` 的外部竞品作品 |
| 详情抽屉 | 仅汇总字段，无作品行 | 展示该账号下已加载作品（标题/播放/点赞/时间） |

## Verification

### Compile

```text
mvn -pl football-module-ops/football-module-ops-server -am compile -DskipTests
→ BUILD SUCCESS (2026-08-03)
```

### Manual smoke（beta :48094 / FE :5777）

1. 重启 ops-server（加载 MonitorServiceImpl 变更）
2. 确认 config id=42 已执行统一外部采集（200 条 DOUYIN works）
3. 打开 `#/ops/external-account` → 抖音 tab
4. 列表「账号名称」= profile nickname（非 `账号 #`）
5. 点击「详情」→ 「采集作品」表格有行数据

### API 抽检

```http
GET /admin-api/ops/monitor/external/list?platformType=DOUYIN&pageNum=1&pageSize=5
```

期望每条 `list[]` 含 `accountName`（来自 `oa_external_account.display_name`）。

## Notes

- 详情作品来自当前 tab 已加载的前 200 条；超出分页需后续加 account 维度 API（非本 bug 最小修复范围）
- 未改 `ExternalAccountCollectExecutor` 写入逻辑（display_name 已正确落库）
