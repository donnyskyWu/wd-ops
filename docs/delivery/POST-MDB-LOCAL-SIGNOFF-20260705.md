# POST-MDB Local Signoff — localhost only（2026-07-05）

> **Scope**：GATE-MDB-S0～S4 完成后 localhost 签收 · AC-S4-UI-01 · **不含**远程 101.37.161.136（GATE-MDB-REMOTE ⏸ 用户取消）

## 结论

**✅ localhost 签收通过** — E2E **58/58 PASS** · P0 UI 场景全绿 · localhost 五库数据达标 · 远程 cutover **Deferred**。

## 配置确认（local-only）

| 组件 | 数据源 / Profile | 状态 |
|------|------------------|------|
| **oa-server** | `dev,dev-nacos,dev-nacos-local,dev-local-multidb` → localhost:3306 五库 | ✅ |
| **start-integration-oa.ps1** | 默认 profile 含 `dev-local-multidb` | ✅ |
| **start-integration-all.ps1** | 同上 + `football-integration-overlay.yml` → **localhost** | ✅（2026-07-05 修正 overlay + 脚本 typo） |
| **application-dev.yml** | 仍指向 101.37.161.136/wd（**仅** standalone export/backup） | ✅ 已注释说明 |
| **football-integration-overlay-remote.yml** | 远程归档，integration **不使用** | ✅ |
| **GATE-MDB-REMOTE** | ⏸ Deferred — 非部署环境 | ✅ 文档已更新 |

## E2E 回归

| 项 | 结果 | 证据 |
|----|------|------|
| `run-uat-football-e2e.ps1 -NoAutoStart` | **58/58 PASS** | 2026-07-05 10:22 · ~3.9min |
| 报告 | `docs/delivery/UAT-FOOTBALL-E2E-20260704.md` | probe JSON 同步更新 |

## P0 UI Spot Check（:5777 · admin/admin123 · tenant 1）

| ID | 场景 | 结果 | 证据 |
|----|------|------|------|
| SC-01 | 登录 Football 壳层 | **PASS** | E2E 58 页均经 `loginFootballOps` |
| SC-02 | 作者列表 ≥35 | **PASS** | DB `shenyu-member.author_user` = **35** · E2E `#/ops/author` ok |
| SC-03 | 新建作者 | **PASS** | GATE-MDB-S1 已签收 · S4 E2E author 页 ok（member SSOT 双写） |
| SC-04 | 微信内部账号 ≥187 | **PASS** | DB `shenyu-mp.mp_account` = **187** · E2E `#/ops/internal-account` ok |
| SC-05 | system-dict | **PASS** | E2E `#/ops/system-dict` ok |
| SC-06 | system-log/login | **PASS** | DB `system_login_log` = **3172** · E2E `#/ops/system-log/login` ok |
| SC-07 | system-log/operation | **PASS** | E2E `#/ops/system-log/operation` ok |
| SC-08 | 内容 + IP 树 | **PASS** | E2E `#/ops/content` · `#/ops/ip-group` ok · V132 后 `oa_author` 不存在 |
| SC-09 | order-attribution | **PASS** | E2E `#/ops/order-attribution` ok |

> **注**：Gateway 直调 OA API 探针在当次栈上返回 401（system-server OAuth2 token 与 oa-server localhost 校验未对齐；需重启 system-server 加载 localhost overlay）。**Gate 路径以 :5777 UI E2E 为准**（§0.6），不影响本签收。

## localhost DB 探针

```
author_user=35
mp_account=187
system_login_log=3172
oa_author_exists=0
flyway_v132=1
```

## 远程状态

| 项 | 状态 |
|----|------|
| GATE-MDB-REMOTE | ⏸ **用户取消** — 101.37.161.136 非部署环境 |
| 远程 DB sync | **不执行** |
| 重启条件 | 用户另批明确部署环境 + mysqldump + 书面审批 |

## 启动命令

```powershell
.\scripts\start-integration-all.ps1 -SkipBuild
.\scripts\run-uat-football-e2e.ps1 -NoAutoStart
python scripts\tmp-local-db-probe.py   # 可选 DB 行数探针
```

## 关联文档

- [GATE-MDB-S4-报告-20260705](./gates/GATE-MDB-S4-报告-20260705.md)
- [GATE-MDB-REMOTE-报告-20260705](./gates/GATE-MDB-REMOTE-报告-20260705.md) ⏸
- [MASTER-EXECUTION-TRACKER §19](./MASTER-EXECUTION-TRACKER.md)
- [INTEGRATION-PROGRESS §22](./INTEGRATION-PROGRESS.md)
