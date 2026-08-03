# Local DB rename: `wd` → `football-ops`（2026-07-31）

| 字段 | 值 |
|------|---|
| 范围 | **仅 localhost:3306** |
| 动作 | `CREATE DATABASE football-ops` + `mysqldump wd \| mysql football-ops` |
| 备份 | **`wd` 保留**（未 DROP） |
| Beta | **未改**（仍 `shenyu-ops` @ 110.42.49.224） |

## MySQL 验证

| Schema | tables | flyway_schema_history |
|--------|--------|------------------------|
| `wd` | 108 | 166 |
| `football-ops` | 108 | 166 |

抽样：`football-ops.oa_ip_group`=20，`sys_param`=11。

证据：`smoke-mysql.txt`。

## 配置已改（本地默认）

| 文件 | 变更 |
|------|------|
| `football-module-ops-server/.../application.yaml` | JDBC `/wd` → `/football-ops` |
| `scripts/lib/integration-preflight.ps1` | 预检库列表 `wd` → `football-ops` |
| `scripts/integration-config/apply-*.py` / `export-wd-schema.py` | 默认 `--database football-ops` |
| `scripts/integration-config/s0-wd-*.sql` | `USE \`football-ops\`` |
| `OPS-DEV-DEPLOY-GUIDE` / `OPS-TEST-DB` / 终态缺口计划 / MERGE-WORK-PLAN | 本地 SSOT=`football-ops`，Beta=`shenyu-ops` |

**未改**：`ops-test-beta-multidb.yml`、`*-overlay-beta.yml`、远程 `101.37.*` overlays。

## 进程说明

当前本机 `ops-server :48094` 命令行含 `ops-test-beta-multidb.yml`（**-Beta**），连远程 `shenyu-ops`，**与本次本地改名无关**。  
本地生效需：停 Beta 进程后以默认（非 `-Beta`）重启，例如 `.\scripts\start-ops-dev.ps1`（无 `-Beta`），使 JAR 读 `application.yaml` → `football-ops`。

Health（当前 Beta 进程）：`{"status":"UP"}`（仍为远程库）。
