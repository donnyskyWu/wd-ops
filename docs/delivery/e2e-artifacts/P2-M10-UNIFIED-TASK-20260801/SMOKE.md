# SMOKE · P2-M10-UNIFIED-TASK-20260801

日期：2026-08-01 · 本地 `shenyu-ops` + ops-server `:48094` · Gateway `:48080`

| 检查项 | 结果 |
|--------|------|
| Flyway V167（`collect_enabled` / `is_unified` / `oa_collect_task_account` / `sys_param`） | ✅ |
| `POST /ops/collect/task/ensure-unified` code=0 · cron=`0 0 23 * * ?` · `isUnified=true` | ✅ |
| `GET /ops/collect/task/page` code=0 | ✅ |
| 账号 `9018` `collectEnabled=true` → 成员存在；`false` → 移除；再开 → 恢复（软删 UK） | ✅ |
| 公众号 `1000006` 仅开关（无 mp-server）→ `oa_account_ext.collect_enabled=1` + 成员 | ✅ |
| `sys_param.collect.schedule.cron` | ✅ |
| **Follow-up · `POST .../task/{id}/run`** | ✅ **code=0**（**非 1511**）· 遍历成员 · 写 `oa_collect_log` |

## Run follow-up（2026-08-01 续）

| 项 | 结果 |
|----|------|
| API | `POST /admin-api/ops/collect/task/10/run` → `code=0` `data=true` |
| 成员 | 2（`9018` KUAISHOU · `1000006` WECHAT_OFFICIAL） |
| 日志 | `oa_collect_log` status=**FAILED**（本地预期） |
| 原因摘要 | `9018` 未绑定 Collector；`1000006` unify-collector `http://127.0.0.1:8000` 不可达 |
| 任务行 | `lastRunAt` 已更新 · `failCount` 递增 |

Artifacts：`RUN-RESULTS.json` · `task-run.json` · `task-after-run.json` · `collect-log-latest.txt` · `members-before-run.json`。

**剩余缺口**：Channel-A SyncService 落库未迁入（本 run 为 `PROBE_COUNT_ONLY`）；collector-bind / 真 collector 联调；log/quality API 仍 ADR-060 stub。
