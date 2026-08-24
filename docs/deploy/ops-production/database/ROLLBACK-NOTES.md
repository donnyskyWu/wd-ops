# Rollback Notes — Ops Production Deploy Pack

**Version:** 2026-08-24  
**Scope:** FR-M2-010 工作任务管理 (V181–V189) · M6 私域报表 MVP (V184)

---

## General

| Item | Rollback complexity | Notes |
|------|---------------------|-------|
| Application JAR downgrade | Low | No schema change in app-only rollback if DB already applied |
| Flyway history | Medium | If Flyway ran V181–V189, downgrade JAR without reversing history may cause drift |
| Data in new tables | High | `oa_work_task_*` / `oa_report_weekly_feedback` may contain production data after go-live |

**Recommendation:** Prefer forward-fix over schema rollback once users have registered work tasks.

---

## Per-component rollback

### 1. Ops schema (01-ops-schema, 05-report-schema)

| Object | Rollback SQL (destructive) | Safe? |
|--------|---------------------------|-------|
| `oa_work_task_sheet` | `DROP TABLE IF EXISTS oa_work_task_sheet;` | Only if empty / no production use |
| `oa_work_task_assignment` | `DROP TABLE IF EXISTS oa_work_task_assignment;` | Only if empty |
| `oa_report_weekly_feedback` | `DROP TABLE IF EXISTS oa_report_weekly_feedback;` | Loses U-column feedback text |
| `oa_task.work_task_assignment_id` | `ALTER TABLE oa_task DROP COLUMN work_task_assignment_id;` | Breaks traceability if tasks were generated from work-task confirm |

### 2. System menus (02-system-menus)

```sql
-- Soft-delete menus (preferred)
UPDATE system_menu SET deleted = b'1', updater = 'rollback-v183'
WHERE permission IN ('ops:work-task:list', 'ops:work-task:register', 'ops:work-task:manage')
  AND deleted = b'0';

-- Remove role bindings
UPDATE system_role_menu SET deleted = b'1', updater = 'rollback-v183'
WHERE menu_id IN (SELECT id FROM system_menu WHERE permission LIKE 'ops:work-task:%');
```

Do **not** hard-delete menu IDs 6194–6196 if other envs reference them.

### 3. System dicts (03-system-dicts)

Dict types are shared. **Do not delete** `dict_marketing_plan_type` etc. if other modules reference them. Rollback only if confirmed exclusive to work-task.

### 4. Ops seeds (04-ops-seeds)

| Seed | Rollback |
|------|----------|
| `oa_ai_prompt_config` scene=`WORK_TASK_WIN_PREDICTION` | `UPDATE ... SET deleted=1 WHERE scene='WORK_TASK_WIN_PREDICTION'` |
| `sys_param` work_task.* | `UPDATE sys_param SET deleted=1 WHERE param_key LIKE 'work_task.%'` |

---

## Application config rollback

| Setting | Rollback |
|---------|----------|
| `xxl.job.enabled=true` | Set `XXL_JOB_ENABLED=false` → disables all XXL handlers |
| `oa.work-task.win-prediction.enabled=true` | Set `enabled: false` → Job no-ops even if scheduled |
| `oa.work-task.win-prediction.scheduled-fallback-enabled` | Must stay `false` in prod |

---

## Flyway manual history (if DBA applied SQL without Flyway)

If scripts were applied manually and Flyway must stay consistent:

```sql
INSERT INTO flyway_schema_history
(installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
SELECT COALESCE(MAX(installed_rank),0)+1, '181', 'm2 work task foundation', 'SQL',
       'V181__m2_work_task_foundation.sql', NULL, 'manual-rollback-marker', NOW(), 0, 1
FROM flyway_schema_history
WHERE NOT EXISTS (SELECT 1 FROM flyway_schema_history WHERE version='181');
-- Repeat for 182, 183 as needed
```

To **undo** a mistaken Flyway record (only before production traffic):

```sql
DELETE FROM flyway_schema_history WHERE version IN ('181','182','183','184','185','186','187','188') AND installed_by LIKE 'manual%';
```

---

## Match proxy

Match proxy (`MatchProxyService`) has **no schema change**. Rollback = revert `oa.match.internal-base-url` to previous match-server address.

---

## XXL-JOB handlers

Disable in xxl-job-admin UI (stop job) before app rollback. Handlers:

- `workTaskWinPredictionJobHandler` — implemented, prod-required for 红黑判定
- `collectCronScanJobHandler` / `monitorAlertScanJobHandler` — ADR-070 planned; not in current ops-server codebase snapshot
