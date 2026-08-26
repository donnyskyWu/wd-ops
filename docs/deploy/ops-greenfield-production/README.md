# Ops Greenfield 生产部署包

**版本:** 2026-08-25  
**场景:** Football 全栈已就绪，**零 Ops**（无 `shenyu-ops` 库、无 Ops 菜单/字典/RBAC）  
**服务:** `ops-server`（端口 `48094`，Flyway 终态 **V191**）

本目录为 **唯一 SSOT 部署包**。DBA 执行 3 条 mysql；DevOps 按 [OPERATIONS-GUIDE.md](./OPERATIONS-GUIDE.md) Part B 部署应用。

## 快速执行（DBA）

替换占位符后，在仓库根目录执行：

```bash
# 1. Ops schema（186 SQL + flyway history，含 V190/V191 sys_* 清理）
mysql -h {{OPS_DB_HOST}} -P {{OPS_DB_PORT}} -u {{OPS_DB_USER}} -p {{OPS_DB_NAME}} \
  < docs/deploy/ops-greenfield-production/sql/01-shenyu-ops-schema.sql

# 2. System 菜单 / 字典 / RBAC
mysql -h {{SYSTEM_DB_HOST}} -u {{SYSTEM_DB_USER}} -p {{SYSTEM_DB_NAME}} \
  < docs/deploy/ops-greenfield-production/sql/02-shenyu-system-menus.sql

# 3. Ops 种子（先编辑 sql/03 内 {{WORK_TASK_DEFAULT_*}} 占位符）
mysql -h {{OPS_DB_HOST}} -u {{OPS_DB_USER}} -p {{OPS_DB_NAME}} \
  < docs/deploy/ops-greenfield-production/sql/03-shenyu-ops-seeds.sql
```

## shenyu-ops 终态表清单（V191 后）

| 类别 | 表 | 说明 |
|------|-----|------|
| **保留 · 业务** | `oa_*` | Ops 域 SSOT（content、ip_group、work_task、report、collect…） |
| **保留 · 配置** | `sys_param` · `sys_message` | 系统参数 / 站内消息 |
| **保留 · M6 元数据** | `sys_metadata_entity` · `sys_metadata_field` | 报表/自定义查询引擎 |
| **保留 · 通知去重** | `sys_notification_event` | 通知幂等 ledger |
| **V191 DROP** | `sys_tenant` · `sys_user*` · `sys_role*` · `sys_permission` | 身份 → shenyu-system Feign（ADR-056） |
| **V190 DROP** | `sys_dict_*` · `sys_operation_log` | 字典 → Feign；操作日志无 Java 引用 |

验收脚本：`sql/verify-schema.sql`（含 V190/V191 负检 + `flyway_v190`/`flyway_v191`）。

建库、验收、DevOps 部署、冒烟 → **[OPERATIONS-GUIDE.md](./OPERATIONS-GUIDE.md)**
