# WECHAT-EXTERNAL-COOKIE-PARAM-20260804

> 日期：2026-08-04 · 需求：公众号外部采集 Cookie 系统参数（P0 运维回退）

## 1. 参数定义

| 字段 | 值 |
|------|-----|
| **param_key** | `collect.external.wechat_official.cookie` |
| **param_name** | 公众号外部采集Cookie |
| **category** | `COLLECT`（复用现有 dict_param_category，无需新分类） |
| **param_type** | `STRING` |
| **Flyway** | `V177__wechat_external_collect_cookie_param.sql` |

## 2. 解析优先级（WECHAT_OFFICIAL）

1. `oa_tenant_collector_credential`（租户表 · AES-256 · source=`TENANT_CREDENTIAL`）
2. `sys_param.collect.external.wechat_official.cookie`（source=`SYS_PARAM`）
3. 环境变量 `WECHAT_OFFICIAL_COOKIE`（source=`ENV`）

## 3. UI 配置入口

| 页面 | 路径 |
|------|------|
| **系统参数** | 系统管理 → 系统参数 → **采集配置** Tab →「公众号外部采集Cookie」 |
| **外部采集配置** | 外部采集配置 → 外部账号 Tab 顶部 Alert 指向系统参数 |

ParamManage 对该键使用 **textarea + 脱敏列表展示**（与 dingtalk secret 同类 SENSITIVE_PARAM_KEYS）。

## 4. 安全说明

- `sys_param` 存 **明文**（与 `dingtalk.client-secret` 一致）；列表 UI 脱敏 `******`
- 租户级凭账号仍走 `oa_tenant_collector_credential.cookie_encrypted` **AES-256**
- 未对 sys_param 值做 AES 加密（ParamService 无 encrypt 钩子）

## 5. Files changed

| 文件 | 变更 |
|------|------|
| `V177__wechat_external_collect_cookie_param.sql` | seed sys_param |
| `ExternalCollectCredentialParamSupport.java` | 读 sys_param Cookie |
| `TenantCollectorCredentialResolver.java` | WECHAT_OFFICIAL sys_param + env 回退 |
| `ParamManage.vue` | textarea + 脱敏 |
| `system-param.ts` | `WECHAT_OFFICIAL_COOKIE_PARAM_KEY` · `SENSITIVE_PARAM_KEYS` |
| `ExternalCollectConfig.vue` | Alert 指向系统参数 |
| `apply_v177_wechat_external_cookie_param.py` | Beta apply 脚本 |
| `ADR-068-M10-统一外部数据采集任务.md` | §2.2 addendum + changelog |

## 6. 验证

| 项 | 结果 |
|----|------|
| `mvn compile` (ops-server) | ✅ PASS（2026-08-04） |
| ParamManage / system-param / ExternalCollectConfig lint | ✅ 无新增 lint |
| `pnpm typecheck` (web-ele) | ⚠️ 预存 `author.ts` TS1128，与本次改动无关 |
| `apply_v177_wechat_external_cookie_param.py` | ⚠️ 远程 DB 连接超时（需在有 mysql 客户端 + 网络可达 beta 的环境执行） |

```powershell
cd football-backend-saas/football-module-ops/football-module-ops-server
mvn -q compile -DskipTests
# exit 0
```

## 7. 手工测试步骤

1. **Apply 迁移**（beta Flyway 禁用时）：
   ```powershell
   python scripts/integration-config/apply_v177_wechat_external_cookie_param.py
   ```
2. 重启 ops-server（或本地 Flyway 自动跑 V177）
3. 登录 → **系统管理 → 系统参数 → 采集配置**
4. 编辑「公众号外部采集Cookie」，粘贴有效 mp.weixin.qq.com Session Cookie，保存
5. 外部采集配置 → 开启 WECHAT_OFFICIAL 账号「是否采集」（应不再报 1512）
6. 执行「统一外部数据采集任务」→ 日志中 config 41 应使用 Cookie 采集（非 skip）
7. （可选）清空 sys_param 值，设置 env `WECHAT_OFFICIAL_COOKIE`，验证第三优先级

## 8. 预期行为对比

| 场景 | 行为 |
|------|------|
| 三者皆空 | toggle 采集 → 1512；任务运行 → skip |
| 仅 sys_param 有值 | toggle 通过；任务 source=SYS_PARAM |
| 租户表 + sys_param 皆有 | 优先租户表 source=TENANT_CREDENTIAL |

---

**结论**：P0 运维可在系统参数录入公众号 Cookie，无需等待 P1 租户凭账号 UI。
