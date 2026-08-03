# CHANGE-INVENTORY · P2-M10-UNIFIED-TASK-20260801

> ADR-061 假设 A1 · 租户级统一采集任务 + `collect_enabled`  
> 日期：2026-08-01

## 1. DB（shenyu-ops · Flyway）

| 对象 | 变更 | 文件 |
|------|------|------|
| `oa_account.collect_enabled` | TINYINT NOT NULL DEFAULT 0 · 是否采集 | `V167__tenant_unified_collect_task.sql` |
| `oa_account_ext.collect_enabled` | TINYINT NOT NULL DEFAULT 0 · 公众号路径 | 同上 |
| `oa_collect_task.is_unified` | TINYINT NULL · UK `(tenant_id, is_unified)` | 同上 |
| `oa_collect_task_account` | 新建成员表 | 同上 |
| `sys_param` | seed `collect.schedule.cron` = `0 0 23 * * ?`（tenant_id=1；运行时 ensure 补其他租户） | 同上 |
| `dict_collect_source` | 可选 seed `UNIFY_COLLECTOR` 标签 | 同上 |

**shenyu-system**：本 Slice **无**强制菜单/权限 DDL（`ops:collect:task:list` 已存在）。

## 2. Backend（football-module-ops）

| 区域 | 文件 |
|------|------|
| Flyway | `.../db/migration/V167__tenant_unified_collect_task.sql` |
| DO/Mapper | `CollectTaskDO` · `CollectTaskAccountDO` · mappers |
| Service | `UnifiedCollectTaskService` · `CollectTaskService`(+Impl) · schedule cron helper |
| Controller | `CollectTaskController`（`/admin-api/ops/collect/task/**`） |
| Account | `AccountDO` / Create/Update/RespVO + `PlatformAccountServiceImpl` sync membership |
| Stub carve-out | `DeferredCutoverStubController` 排除 `/collect/task/**` |
| Docs | ADR-061 · 本清单 · ADR-060 § carve-out · Spec 附注 |

## 3. Frontend（football-front）

| 文件 | 变更 |
|------|------|
| `api/ops/platform-account.ts` | `collectEnabled` |
| `api/ops/collect.ts` | `ensureUnified` · members · `isUnified` / `memberCount` |
| `views/ops/internal/PlatformAccountCollectTab.vue` | Switch「是否采集」 |
| `views/ops/collect/task.vue` | 展示统一任务；ensure on mount；弱化「每账号一任务」新建路径 |

## 4. Shipped vs Follow-up

| Must-ship | Status |
|-----------|--------|
| DB + seed | ✅ |
| collect_enabled toggle → membership | ✅ |
| ensureUnifiedTask + cron from param | ✅ |
| task list/get/members/start/stop | ✅ |
| FE Switch + unified task UI | ✅ |
| UnifiedCollector 多账号实际执行 | ✅ Follow-up 已落地（见 §6） |
| collector-bind 真迁 / SyncService 落库 / log·quality API | ⏳ 仍 ADR-060 / 后续 Slice |

## 5. Smoke（本地）

见同目录 [`SMOKE.md`](./SMOKE.md) · `RESULTS.json`：ensure / cron / legacy toggle / wechat toggle / page **全绿**（2026-08-01）。

Run follow-up：[`RUN-RESULTS.json`](./RUN-RESULTS.json) · `POST .../run` **code=0 非 1511**；日志 FAILED（collector 离线 / 未 bind）属预期。

## 6. Follow-up · 多账号 Run（2026-08-01）

| 区域 | 文件 |
|------|------|
| Runner | `UnifiedCollectRunService` · `ChannelAAccountCollectExecutor` · `CollectExecutionResult` · `CollectPlatformDefaults` · `CollectNextRunHelper` |
| Channel-A probe | `unified/UnifiedCollectorProbeClient` · `UnifiedCollectorApiException` |
| Cron | `CollectCronScheduler` + `@EnableScheduling`；`oa.collect.schedule.*` / `oa.unified-collector.*` |
| DAL | `CollectLogDO`/`Mapper` · `CollectorAccountBindDO`/`Mapper` |
| Config | `CollectProperties` · `UnifiedCollectorProperties` |
| Wire | `CollectTaskServiceImpl.run` → runner（不再抛 1511） |

行为：统一任务遍历 `oa_collect_task_account` → 按账号平台探测 → 聚合 SUCCESS/PARTIAL/FAILED（ADR-049）→ 写 `oa_collect_log.result_json`（`typeResults[]` · `persistMode=PROBE_COUNT_ONLY`）。
