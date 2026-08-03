# Local DB rename: `football-ops` → `shenyu-ops`（2026-08-01）

| 字段 | 值 |
|------|---|
| 范围 | **仅 localhost:3306** |
| 动作 | `CREATE DATABASE shenyu-ops` + `mysqldump football-ops \| mysql shenyu-ops` |
| 备份 | **`football-ops` 与 `wd` 保留**（未 DROP） |
| Beta | **未改**（仍 `shenyu-ops` @ 110.42.49.224） |

## MySQL 验证

| Schema | tables | flyway_schema_history | oa_ip_group | sys_param |
|--------|--------|------------------------|-------------|-----------|
| `wd` | 108 | — | — | — |
| `football-ops` | 108 | — | — | — |
| `shenyu-ops` | 108 | 167 | 20 | 11 |

证据：`smoke-mysql.txt`。

## 配置已改（本地默认）

| 文件 | 变更 |
|------|------|
| `football-module-ops-server/.../application.yaml` | JDBC `/football-ops` → `/shenyu-ops` |
| `scripts/lib/integration-preflight.ps1` | 预检库列表 → `shenyu-ops` |
| `scripts/integration-config/apply-*.py` / `export-wd-schema.py` | 默认 `--database shenyu-ops` |
| `scripts/integration-config/s0-wd-*.sql` | `USE \`shenyu-ops\`` |
| `OPS-DEV-DEPLOY-GUIDE` / `OPS-TEST-DB` / 终态缺口计划 / MERGE-WORK-PLAN | 本地 SSOT=`shenyu-ops`（与 Beta 同名） |

**未改**：`ops-test-beta-multidb.yml`、`*-overlay-beta.yml`、远程 `101.37.*` overlays。

## Stub disposition（同会话）

见 [ADR-060](../../../adr/ADR-060-Phase2-stub-OOS-Accept.md)：M10 collect / Douyin / `/internal/**` → Closed-Accept。

## 进程说明 / Smoke

| 项 | 结果 |
|----|------|
| 重建 | `mvn -pl football-module-ops/football-module-ops-server -am package -DskipTests`（先停旧 :48094） |
| 启动 | `java -jar …/football-module-ops-server.jar`（**非** `-Beta`） |
| Flyway 日志 | `Database: jdbc:mysql://127.0.0.1:3306/shenyu-ops …` · `Schema shenyu-ops is up to date` |
| Health | `{"status":"UP"}`（`ops-health.json`） |
| MySQL processlist | `root` Sleep on DB **`shenyu-ops`** |
| API | `ip-group/list` **code=0 total=20** · `param/list` **code=0 total=11**（RESULTS.json） |

`ops-startup.log` 含 Flyway 行作连接证据。
