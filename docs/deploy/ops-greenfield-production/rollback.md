# 回滚说明

**版本:** 2026-08-25 · Greenfield baseline + V181–V191

## 原则

| 阶段 | 建议 |
|------|------|
| 投产前（无业务数据） | 删空库 + 软删 system 菜单 — 最简单 |
| 投产后（有工作任务/报表数据） | **优先 forward-fix**，勿轻易 DROP 表 |

应用仅回滚 JAR 时，若 Flyway 已执行 V181+，须同步处理 `flyway_schema_history`，否则版本漂移。

## shenyu-ops

```sql
-- 投产前：整库回滚
DROP DATABASE IF EXISTS `{{OPS_DB_NAME}}`;

-- 投产后：仅种子
UPDATE oa_ai_prompt_config SET deleted=1 WHERE scene='WORK_TASK_WIN_PREDICTION';
UPDATE sys_param SET deleted=1 WHERE param_key LIKE 'work_task.%';
```

## shenyu-system

```sql
-- 软删工作任务菜单
UPDATE system_menu SET deleted=b'1', updater='rollback-v183'
WHERE permission LIKE 'ops:work-task:%' AND deleted=b'0';

-- 软删六角色（确认无生产用户绑定）
UPDATE system_role SET deleted=b'1'
WHERE code IN ('ip_group_leader','ops_manager','finance','content_editor','data_analyst','collect_operator');

-- Greenfield baseline 6100–6199
UPDATE system_menu SET deleted=b'1' WHERE id BETWEEN 6100 AND 6199;
```

**勿删** `dict_marketing_plan_type` 等共享字典（其他模块可能引用）。

## 应用配置

| 项 | 回滚 |
|----|------|
| XXL-JOB | admin UI 停止 `workTaskWinPredictionJobHandler` |
| 预测开关 | `oa.work-task.win-prediction.enabled: false` |
| Match proxy | 还原 `oa.match.internal-base-url` |

## Flyway history（手工灌库误操作时）

```sql
DELETE FROM flyway_schema_history
WHERE version IN ('181','182','183','184','185','186','187','188','189','190','191')
  AND installed_by LIKE 'manual%';
```

## V190/V191 增量回滚（仅投产前、无业务依赖 legacy sys_* 时）

> 投产后 **勿** 重建 `sys_tenant`/`sys_user*`/`sys_role*` — 应用已 Feign 至 shenyu-system（ADR-056）。

```sql
-- 仅当误跑 V190/V191 且需恢复 harness 表做对比时（非生产推荐路径）
-- 优先 forward-fix：重新部署旧 JAR + 从备份还原表
SELECT version, success FROM flyway_schema_history WHERE version IN ('190','191');
```
