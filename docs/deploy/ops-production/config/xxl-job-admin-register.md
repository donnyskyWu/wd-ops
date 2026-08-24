# XXL-JOB Admin Registration — ops-server (ADR-070)

**Version:** 2026-08-19  
**Executor appname:** `football-ops-executor` (explicit — NOT `ops-server`)  
**Admin:** Shared with `football-module-mp` per ADR-070 Q1

---

## 1. Executor configuration (application)

From `application-prod.yaml` / `application.yaml`:

```yaml
xxl:
  job:
    enabled: true
    accessToken: ${XXL_JOB_ACCESS_TOKEN:a1b2c3d4e5f67890}
    admin:
      addresses: ${XXL_JOB_ADMIN_ADDRESSES:http://127.0.0.1:9090/xxl-job-admin}
    executor:
      appname: football-ops-executor   # MUST be literal — do not use ${spring.application.name}
      ip:
      port: -1                       # auto
      logpath: ${user.home}/logs/xxl-job/ops-server
      logretentiondays: 30
```

**Verify after deploy:** xxl-job-admin → 执行器管理 → AppName `football-ops-executor` → 在线机器列表含 ops-server IP.

---

## 2. Job handlers

### Implemented in current codebase

| Handler | Class | Cron (SSOT) | @TenantJob | Purpose |
|---------|-------|-------------|------------|---------|
| `workTaskWinPredictionJobHandler` | `WorkTaskWinPredictionJob` | `0 0 * * * ?` | Yes | FR-M2-010 S-19 红黑赛后判定 (ADR-072) |

**Registration steps (xxl-job-admin → 任务管理 → 新增):**

| Field | Value |
|-------|-------|
| 执行器 | `football-ops-executor` |
| 任务描述 | 工作任务红黑预测 Job |
| 路由策略 | 第一个 |
| Cron | `0 0 * * * ?` |
| 运行模式 | BEAN |
| JobHandler | `workTaskWinPredictionJobHandler` |
| 阻塞处理策略 | 单机串行 |
| 任务超时时间 | 0 (不限制) 或 3600 |
| 失败重试次数 | 3 (ADR-070 Q5 推荐) |

**Preconditions:**

- `oa.work-task.win-prediction.enabled=true`
- `oa.work-task.win-prediction.scheduled-fallback-enabled=false` (prod)
- AI prompt `WORK_TASK_WIN_PREDICTION` seeded
- `oa.match.internal-base-url` reachable

---

### ADR-070 planned (M10 collect — not in current ops-server Java tree)

These handlers are specified in [ADR-070](../../adr/ADR-070-Ops抓取统一XXL-JOB调度.md) but **not present** in the current `football-module-ops-server` source snapshot. Register when M10 collect Slice lands:

| Handler | Cron | Purpose |
|---------|------|---------|
| `collectCronScanJobHandler` | `0 * * * * ?` | Unified collect task scan (ADR-061/068) |
| `monitorAlertScanJobHandler` | `0 0/30 * * * ?` | Threshold alert scan (ADR-069 P1) |

When implemented, use retry policy per ADR-070 Q5: 3 retries, intervals 1/5/15 min.

---

## 3. Disable / rollback

| Action | Effect |
|--------|--------|
| Set `XXL_JOB_ENABLED=false` | Executor stops registering; no remote triggers |
| Stop job in admin UI | Handler code remains but not scheduled |
| Set `oa.work-task.win-prediction.enabled=false` | Handler runs but no-ops immediately |

**Local dev fallback (NOT for prod):**

```yaml
oa:
  work-task:
    win-prediction:
      scheduled-fallback-enabled: true  # enables @Scheduled when xxl.job.enabled=false
```

---

## 4. Post-registration test

1. xxl-job-admin → 任务管理 → 工作任务红黑预测 Job → **执行一次**
2. Check 调度日志 → status SUCCESS
3. Check ops-server log: `WorkTaskWinPredictionJob` / `processCurrentTenant`
4. For tenant with CONFIRMED sheet + finished match + content: `win_prediction` updates from UNKNOWN

---

## 5. Related config

| Key | Default | Notes |
|-----|---------|-------|
| `oa.work-task.win-prediction.match-end-buffer-minutes` | 120 | Wait after match end before judging |
| `oa.collect.schedule.scan-cron` | `0 * * * * ?` | Fallback SSOT for collect handler when admin has no cron |
