# POST-MDB 本地签收 — localhost 五库 · Football 集成

> 日期：2026-07-05 · 范围：GATE-MDB-S0～S4 完成后 · localhost `dev-local-multidb` · **不含** 101.37.161.136 远程 cutover（⏸ Deferred）

## 结论

| 维度 | 结果 | 说明 |
|------|------|------|
| **Football E2E** | ✅ **58/58 PASS** | `.\scripts\run-uat-football-e2e.ps1 -NoAutoStart` · 2026-07-05 10:22 · ~3.9min |
| **DB SSOT 探针** | ✅ PASS | author 35 · login 3172 · mp 187 · dict 179 |
| **Gateway API smoke** | ✅ **4/4 PASS** | 附录 A · §23 #1 修复后 2026-07-05 |
| **远程 cutover** | ⏸ N/A | 用户取消 · 见 [GATE-MDB-REMOTE](./GATE-MDB-REMOTE-报告-20260705.md) |

**签收判定**：**MDB localhost 程序 ✅ 全量签收**（E2E + 数据 SSOT + Gateway API smoke）。

---

## 1. 环境

| 项 | 值 |
|----|-----|
| 启动 | 栈已运行（5777/48080/48094 LISTEN）；oa-server 重启 profile `dev,dev-nacos,dev-nacos-local,dev-local-multidb` |
| MySQL | localhost:3306 · root/root · 五库 wd + shenyu-member/mp/pay/system |
| 登录 | admin / admin123 · tenant 1 |
| UI | http://localhost:5777 |
| Gateway | http://localhost:48080/admin-api |

---

## 2. E2E 回归

```powershell
.\scripts\run-uat-football-e2e.ps1 -NoAutoStart
```

| 指标 | 结果 |
|------|------|
| Playwright `@uat-football` | **58 passed** · 0 failed |
| 报告 | [UAT-FOOTBALL-E2E-20260704](../UAT-FOOTBALL-E2E-20260704.md) |
| Probe JSON | [uat-football-e2e-20260704-probe.json](../uat-football-e2e-20260704-probe.json) |

---

## 3. DB SSOT 探针（localhost:3306）

| 探针 | 库.表 | 实测 | 阈值 | 结果 |
|------|-------|------|------|------|
| 作者 | `shenyu-member.author_user` | **35** | ≥35 | ✅ |
| 登录日志 | `shenyu-system.system_login_log` | **3172** | ≥3000 | ✅ |
| 微信公号 | `shenyu-mp.mp_account` | **187** | >0 | ✅ |
| 字典类型 | `shenyu-system.system_dict_type` | **179** | >0 | ✅ |
| dev-token | `wd.sys_user_token` | **0 行** | — | ⚠️ S0 TRUNCATE 后缺失（standalone 用；非 Gate 路径） |

---

## 4. Gateway API smoke（admin 登录链）

脚本：`scripts/post-mdb-local-smoke.py`（2026-07-05）

### 4.1 登录

| 探针 | 结果 |
|------|------|
| `POST /admin-api/system/auth/login` | ✅ code=0 · accessToken 32 chars |

### 4.2 OA API（Bearer + `X-Tenant-Id: 1`）

| 探针 | HTTP | code | 阈值 | 结果 |
|------|------|------|------|------|
| `GET /admin-api/oa/author/list` | 401 | 401 | total≥35 | ❌ |
| `GET /admin-api/oa/account/list` | 401 | 401 | code=0 | ❌ |
| `GET /admin-api/oa/system/log/login` | 401 | 401 | total≥3000 | ❌ |
| `GET /admin-api/oa/system/dict/list` | 401 | 401 | code=0 | ❌ |

### 4.3 根因分析

| 观测 | 说明 |
|------|------|
| `token_in_wd=0` · `token_in_system=0` | 登录后 token **未写入** MySQL（Redis-only） |
| `FootballAuthProvider` | 仅 `system_oauth2_access_token` DB 查表 |
| `wd.sys_user_token` | S0 TRUNCATE 清空 · `dev-token-oa-admin` 不可用 |
| E2E 仍 58/58 | Playwright 以页面 DOM smoke 为主；多数页未断言 primary API `code=0` |
| 本地补丁 | `FootballOAuth2TokenMapper` 已加 `@DS("system")`（待 #1 Redis/持久化方案后复测） |

**对照**：GATE-INT-S1（2026-07-03）曾验 `GET /admin-api/oa/ip-group/tree` code=0；MDB S0 TRUNCATE + system-server Redis token 策略后回归。

---

## 5. 配置签收

| 项 | 状态 |
|----|------|
| `start-integration-all.ps1` | ✅ `OaProfiles = dev,dev-nacos,dev-nacos-local,dev-local-multidb` |
| `start-integration-oa.ps1` | ✅ 默认 profile 含 `dev-local-multidb` · header 注释 localhost:3306 |
| `application-dev.yml` | ✅ header：legacy 101.37.161.136 · integration 用 overlay |
| `application-dev-local-multidb.yml` | ✅ localhost:3306 五库 dynamic DS |
| 远程 wd | ✅ **未修改** |

---

## 6. 关联 Gate

| Gate | 状态 |
|------|------|
| GATE-MDB-S0～S4 | ✅ |
| GATE-MDB-REMOTE | ⏸ Deferred（用户取消） |
| 下一动作 | ~~INTEGRATION-PROGRESS §23 #1~~ ✅ 2026-07-05 · 见附录 A |

---

## 附录 A — §23 #1 鉴权修复复签（2026-07-05）

### 根因（双因素）

| 因素 | 说明 |
|------|------|
| Token 存储 | system-server 登录后 token 在 **Redis**（`oauth2_access_token:{token}`）+ **wd** master；`FootballAuthProvider` 仅查 `shenyu-system.system_oauth2_access_token` → 401 |
| RBAC 数据源 | `oa:*` 菜单权限在 **wd.system_menu**（62 条）；shenyu-system 无 oa 菜单 seed → 鉴权通过后部分 API 403 |

### 修复（oa-server only）

| 文件 | 变更 |
|------|------|
| `FootballOAuth2TokenRedisReader.java` | Jedis 读 Redis token JSON（`expiresTime` epoch millis） |
| `FootballOAuth2MasterTokenMapper.java` | `@DS("master")` wd token/user/RBAC |
| `FootballAuthProvider.java` | Redis → wd user → shenyu-system username 映射；权限从 wd master 加载 |
| `FootballOAuth2RedisProperties.java` | integration Redis 配置 |
| `application-dev-local-multidb.yml` | `oa.auth.football-redis.enabled=true` · Redis 127.0.0.1:6379/123456 |
| `pom.xml` | `redis.clients:jedis` |

`football-integration-overlay.yml` Redis 配置与 system-server 一致（127.0.0.1:6379 password 123456）· **未改 Football 业务代码**。

### API smoke 前后

| 探针 | 修复前 | 修复后 |
|------|--------|--------|
| `author/list` | 401 | 200 · code=0 · total=35 |
| `account/list` | 401 | 200 · code=0 |
| `system/log/login` | 401 | 200 · code=0 · total=3151 |
| `system/dict/list` | 401 | 200 · code=0 · total=869 |

脚本：`python scripts/post-mdb-local-smoke.py` · probe：`docs/delivery/post-mdb-local-smoke-20260705-probe.json`

### E2E 复跑

```powershell
.\scripts\run-uat-football-e2e.ps1 -NoAutoStart
```

| 指标 | 结果 |
|------|------|
| Playwright `@uat-football` | **58 passed** · 0 failed · ~7.4min · 2026-07-05 post-#1 |

---

## 7. 命令复现

```powershell
# 栈（已运行时 SkipBuild）
.\scripts\start-integration-all.ps1 -SkipBuild

# E2E Gate
.\scripts\run-uat-football-e2e.ps1 -NoAutoStart

# API smoke（§23 #1 修复后 4/4 PASS）
python scripts/post-mdb-local-smoke.py
# probe: docs/delivery/post-mdb-local-smoke-20260705-probe.json
```
