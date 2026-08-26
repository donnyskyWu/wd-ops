# WORK-TASK-MATRIX-E2E-20260821

**日期** 2026-08-21 · **问题** 任务管理 Tab「加载任务矩阵失败」

## 根因

`getMatrix` / `getMatrixSummary` 调用 `WorkTaskMatchPoolService.listConfiguredInRange`，查询表 `oa_work_task_match_pool`（V185）。  
本地与 Beta **均未执行 V185**，MySQL 1146 → 前端 Toast「加载任务矩阵失败」。

## 修复

1. **V185 Flyway** 仅保留建表 DDL（菜单改走 `shenyu-system` apply 脚本，避免 Flyway 在 ops 库写 `system_menu` 失败）
2. **`WorkTaskMatchPoolServiceImpl`** 增加 `isPoolSchemaReady()`：表未迁移时矩阵降级为旧逻辑（空池 + 原 assignment 矩阵），避免再次 500
3. **`apply_v185_match_pool.py`** 同步 local + beta：
   - `shenyu-ops`：`oa_work_task_match_pool` + `oa_work_task_match_pool_item` + flyway 185
   - `shenyu-system`：菜单 6197 `ops:work-task:match-config` + role 1/161

## E2E 结果（API smoke）

脚本：`scripts/integration-config/smoke_work_task_matrix_e2e.py`  
环境：Gateway `:48080` · admin/admin123 · tenant=1

| 步骤 | 结果 |
|------|------|
| GET `/ops/work-task/matrix` | code=0，rows=5 |
| GET `/ops/work-task/matrix/summary` | code=0，totalMatchRows=5 |
| GET/PUT `/ops/work-task/match-pool` | code=0 |
| 配置 1 场赛后 matrix | rows=1（配置赛事为行 SSOT） |

**结论：PASS**

## 后续

- 部署含 `isPoolSchemaReady` 的后端 JAR（防御性，非阻塞）
- 运营主管需重新登录以加载 `ops:work-task:match-config` 权限（Beta 已 seed）
