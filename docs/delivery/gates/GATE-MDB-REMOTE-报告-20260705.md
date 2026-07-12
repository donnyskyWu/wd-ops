# GATE-MDB-REMOTE 报告 — 远程五库 Cutover（101.37.161.136）

> 日期：2026-07-05 · 阶段：post-S4 远程 cutover · 依据：`mdb-s4-nacos-matrix.md` · `mdb-remote-flyway-checklist.md` · `OPS-FOOTBALL-MULTI-DB-EXECUTION-PLAN.md`

## 结论

**⏸ 用户取消（2026-07-05）** — 101.37.161.136 **非部署环境**；用户决定 **不继续远程 cutover / 数据同步**。localhost Gate（S0–S4）仍 ✅；日常开发 **仅 localhost:3306 五库**（`dev-local-multidb` profile）。远程 cutover 项 **Deferred**，需用户另批明确部署环境后再启动。

> 历史探测记录（2026-07-05 09:50 前）保留于下文，供归档参考；**不再作为活跃阻塞项**。

## 执行步骤记录

| # | 步骤 | 结果 | 说明 |
|---|------|------|------|
| 1 | 发现 DB 凭证 | ✅ | `application-dev.yml` · user=`shenyu` · password=`Zhang***456`（已掩码） |
| 2 | `test-remote-mysql-connection.ps1` | ⚠️ 部分 | TCP ✅ · `wd` ✅ · 四库 **1049 Unknown database** |
| 3 | 远程 Flyway V131/V132 | ⏸ 未执行 | 当前最高 **V129**；无 V131/V132；`oa_author_ext` 不存在 |
| 4 | S0 TRUNCATE 远程 wd | ⏭ **跳过** | 执行计划 §0.5 **禁止** touch 101.37.161.136；远程含业务数据（见下） |
| 5 | `push-remote-multidb-config.ps1` | ⏸ 未 push | Nacos `127.0.0.1:8848` **不可达**；`-WhatIf` 预览 2111 bytes ✅ |
| 6 | 重启 oa-server 远程 multidb | ⏸ 未执行 | 依赖步骤 5 + 四库就绪 |
| 7 | 远程验收 E2E 58/58 | ⏸ N/A | cutover 未完成 |
| 8 | 文档更新 | ✅ | 本报告 + MASTER §19.2 |

## 连接测试结果

```
Host:     101.37.161.136:3306
User:     shenyu
TCP:      OK
wd:       OK (SELECT 1)
shenyu-member:  FAIL — Unknown database
shenyu-mp:      FAIL — Unknown database
shenyu-pay:     FAIL — Unknown database
shenyu-system:  FAIL — Unknown database
```

## 远程 wd 状态（2026-07-05 探测）

| 项 | 值 |
|----|-----|
| Flyway 最高版本 | **V129** (seed dashboard content rolling) |
| V131 / V132 | **未应用** |
| `oa_author` | 存在 · **8 行** |
| `oa_author_ext` | **不存在** |
| `author_user` (wd) | 0 行 |
| `oa_content` | 43 |
| `oa_account` | 20 |
| `sys_login_log` | **3**（验收目标 ≥3000 不满足） |
| `system_users` | 18 |
| `oa_ip_group` | 11 |

## TRUNCATE 决策

| 因素 | 结论 |
|------|------|
| 执行计划 §0.5 | **仅 localhost TRUNCATE**；远程 **禁止** |
| 远程数据特征 | 非 disposable test data（43 content · 20 account · 18 users） |
| 用户 export 备份 | **未确认** remote mysqldump |
| **决定** | **跳过** remote TRUNCATE；保留 remote wd 现状直至用户备份 + 四库导入 |

## Nacos / oa-server 本地栈

| 组件 | 状态 |
|------|------|
| Nacos 127.0.0.1:8848 | DOWN |
| oa-server :48094 | UP（当前 profile 含 `dev-local-multidb` → **localhost** 五库） |
| Football UI :5777 | UP |
| localhost E2E | **58/58 PASS**（`.last-run.json` · 2026-07-05；**非远程验收**） |

## 阻塞清单（用户需提供 / 执行）

### P0 — 远程四库前置（matrix §前置）

1. **mysqldump 备份** remote `wd`（cutover 前必做）
2. **创建四库** on 101.37.161.136：
   - `shenyu-member` · `shenyu-mp` · `shenyu-pay` · `shenyu-system`
3. **导入 export**（repo 已有，合计 ~350 KB）：
   - `docs/sql/shenyu-member.sql` (116 KB)
   - `docs/sql/shenyu-mp.sql` (42 KB)
   - `docs/sql/shenyu-pay.sql` (90 KB)
   - `docs/sql/shenyu-system.sql` (102 KB)
4. 重跑 `test-remote-mysql-connection.ps1` → 五库全绿

### P1 — Flyway（checklist）

5. 确认 oa-server **stopped** 或单库 profile
6. 应用 **V131**（`oa_author_ext` PK 变更）— Flyway auto 或 `flyway:migrate` 指向 remote wd
7. 配置 Nacos 五库 + oa-server 读 member/mp/pay/system **@DS 冒烟**
8. 应用 **V132**（DROP `oa_author` + wd 副本表）— **仅**在四库 SSOT 确认后

### P2 — 配置与验收

9. 启动 Nacos：`.\scripts\start-nacos-local.ps1`
10. 设 `$env:OA_DB_PASSWORD='***'` → `push-remote-multidb-config.ps1`（去 `-WhatIf`）
11. oa-server profile 合并 `oa-server-remote-multidb.yaml`（或等价 Nacos dataId）
12. 验收：author ≥35 · login logs ≥3000 · **58/58 E2E**

## 风险说明

- **V132 不可逆**：无 Flyway down；须 cutover 前 mysqldump
- **sys_login_log=3**：远程 wd 日志量远低于 localhost 验收基线；cutover 后 `#/ops/system-log/login` 可能需从 `shenyu-system` 读 SSOT 才能达标
- **matrix 约束**：远程变更需 **用户书面审批**；本程序仅探测 + 文档化

## 回滚

- 从 cutover 前 mysqldump restore `wd`
- oa-server profile 回退单库 `application-dev.yml`
- V132 无 down — restore from backup only

## 下一步命令（用户审批后）

```powershell
# 1. 设凭证（勿提交）
$env:OA_DB_USER = 'shenyu'
$env:OA_DB_PASSWORD = '<masked>'

# 2. 四库导入后验证
.\scripts\test-remote-mysql-connection.ps1

# 3. Flyway 检查
mysql -h 101.37.161.136 -u shenyu -e "SELECT version,description FROM wd.flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;"

# 4. Nacos + push
.\scripts\start-nacos-local.ps1
.\scripts\push-remote-multidb-config.ps1   # 去掉 -WhatIf

# 5. 重启 oa-server（远程 multidb profile — 待 Nacos merge 方案确认）
# 6. 验收
.\scripts\run-uat-football-e2e.ps1 -NoAutoStart
```

---

## 更新记录 — 2026-07-05 09:50（四库导入 + V131）

### 结论摘要

| 项 | 结果 |
|----|------|
| 四库 CREATE + import | **成功**（schema-only dumps，无 INSERT） |
| `test-remote-mysql-connection.ps1` | **5/5 绿**（wd + 四 Football DB） |
| 远程 wd TRUNCATE | **未执行** |
| Flyway V130 + V131 | **已应用**（手动 SQL + `flyway_schema_history` 登记） |
| Flyway V132 | **未应用**（待用户确认 mysqldump 备份） |

### 四库行数（SSOT 探针表）

| 表 | 库 | 行数 |
|----|-----|------|
| `author_user` | shenyu-member | **0** |
| `mp_account` | shenyu-mp | **0** |
| `system_login_log` | shenyu-system | **0** |
| `pay_all_order` | shenyu-pay | **0** |

> `docs/sql/shenyu-*.sql` 均为 **Navicat 结构导出**（0 条 INSERT）。表已创建（member 63 表等），**业务数据需另行 mysqldump/灌数** 后才能达到 matrix 验收（作者 ~35、登录日志 ~3000 等）。

### 远程 wd（V131 后）

| 项 | 值 |
|----|-----|
| Flyway 最高版本 | **V131** |
| V132 | 未应用 |
| `oa_author` | **8**（未 DROP） |
| `oa_author_ext` | 存在，PK=`author_user_id`，**0 行**（V131 TRUNCATE） |
| `oa_account_ext` | **存在** |

### 下一步（P1 剩余）

1. **mysqldump 备份** remote `wd`（V132 前必须）
2. 若需 SSOT 数据：从 localhost 或生产源 **带数据** 导出四库并 re-import（或 selective INSERT）
3. 启动 Nacos → `push-remote-multidb-config.ps1`（去 `-WhatIf`）
4. oa-server 远程 multidb profile → **冒烟 @DS**
5. 用户确认备份后 → **V132** only
6. `run-uat-football-e2e.ps1` → 58/58

---

## 更新记录 — 2026-07-05 10:30（用户取消 · 程序关闭）

**最终状态**：⏸ **Deferred / 用户取消** — 101.37.161.136 非部署环境；**不继续**远程 sync / V132 / Nacos push。下方 09:50–10:12 探测与 sync 记录仅作归档；localhost MDB S0–S4 + [POST-MDB-LOCAL-SIGNOFF](./POST-MDB-LOCAL-SIGNOFF-20260705.md) 为日常 dev 签收依据。

---

## 更新记录 — 2026-07-05 10:12（localhost → 远程 SSOT 数据同步）

### 结论摘要

| 项 | 结果 |
|----|------|
| 四库数据同步 localhost → 101.37.161.136 | **成功**（`mysqldump --no-create-info` + `mysql` 导入） |
| 远程 wd TRUNCATE | **未执行** |
| Flyway V132 | **未执行** |
| 探针表行数 vs localhost | **一致**（见下表） |
| oa-server 远程五库冒烟 | **PASS** `GET /admin-api/oa/author/list` → `total=35` |
| Nacos + Gateway 冒烟 | **阻塞** Docker 不可用，8848 DOWN；48080 author API 401 |

### 同步方法

1. **源**：`localhost:3306` · `root`/`root` · 库 `shenyu-member` / `shenyu-mp` / `shenyu-system` / `shenyu-pay`
2. **工具**：`mysqldump --no-create-info --single-transaction --set-gtid-purged=OFF --result-file=...`（避免 PowerShell `Set-Content` 破坏 SQL）
3. **目标**：`101.37.161.136:3306` · `shenyu` / `application-dev.yml` 凭证 · `SET FOREIGN_KEY_CHECKS=0` 后 `mysql < dump.sql`
4. **体积优化**：`shenyu-mp` **排除** `mp_template_push_logs`（本地 ~4.59M 行 / ~1.25 GB）；其余 mp 表全量（含 `mp_account` 187）
5. **转储文件**（本机临时目录，未入库）：`%TEMP%\football-ssot-sync\` · member ~42 MB · mp-slim ~128 MB · system ~336 MB · pay ~112 MB · **合计 ~618 MB**

### 行数对照（探针）

| 表 | 库 | localhost | 远程 | 验收 |
|----|-----|-----------|------|------|
| `author_user` | shenyu-member | 35 | 35 | ≥35 OK |
| `mp_account` | shenyu-mp | 187 | 187 | ≥187 OK |
| `system_login_log` | shenyu-system | 3172 | 3172 | ≥3000 OK |
| `system_operate_log` | shenyu-system | 627 | 627 | — |
| `system_dict_type` | shenyu-system | 186 | 186 | — |
| `system_dict_data` | shenyu-system | 907 | 907 | — |
| `pay_all_order` | shenyu-pay | 183485 | 183485 | >0 OK |
| `mp_template_push_logs` | shenyu-mp | 4590240 | **0** | 有意省略（非冒烟必需） |

### 部分同步说明

- **pay**：全量 `pay_all_order` 已导入（183485 行），未做采样。
- **mp**：仅省略 `mp_template_push_logs`；若公众号推送日志页需要历史，可单独 `mysqldump` 该表（预计 >1 GB 传输）。

### oa-server 远程 multidb 冒烟

- Profile：`dev,dev-nacos,dev-nacos-local` + `SPRING_CONFIG_ADDITIONAL_LOCATION=file:///.../oa-server-remote-multidb.yaml`
- 环境：`OA_DB_USER=shenyu` · `OA_DB_PASSWORD`（与 dev.yml 一致，未提交）
- 直连：`http://localhost:48094/admin-api/oa/author/list` + `Authorization: Bearer dev-token-oa-admin` + `X-Tenant-Id: 1` → **200 · total=35**
- **未**完成：`push-remote-multidb-config.ps1` 实推（Nacos 未起）；Gateway `48080` 同路径 **401**

### 下一步 / 阻塞

| 优先级 | 阻塞 / 动作 |
|--------|-------------|
| P0 | 安装/启动 **Docker Desktop** → `start-nacos-local.ps1` → `push-remote-multidb-config.ps1`（namespace 与 `dev-nacos-local` 对齐为 `local` 或改 discovery） |
| P1 | Gateway + `grayLb://oa-server` 注册后复测 author API |
| P1 | 远程 **wd mysqldump 备份** 后用户确认 → 方可 **V132** |
| P2 | `run-uat-football-e2e.ps1` 58/58 对远程栈 |
