# WECHAT-OFFICIAL-COOKIE-PARAM-20260804

> 日期：2026-08-04 · 需求：ADR-068 §2.2 addendum · 公众号外部采集 Cookie 系统参数（P0 运维回退）

## 1. 参数定义

| 字段 | 值 |
|------|-----|
| **param_key** | `collect.external.wechat_official.cookie` |
| **param_name** | 外部公众号采集 Cookie |
| **category** | `COLLECT` |
| **param_type** | `STRING` |
| **env fallback** | `WECHAT_OFFICIAL_COOKIE` |
| **Flyway** | `V177__wechat_external_collect_cookie_param.sql` |
| **Beta apply** | `scripts/integration-config/apply_v177_wechat_external_cookie_param.py` |

## 2. 解析优先级（WECHAT_OFFICIAL）

`TenantCollectorCredentialResolver.resolve()`：

1. `oa_tenant_collector_credential`（AES-256 · source=`TENANT_CREDENTIAL`）
2. `sys_param.collect.external.wechat_official.cookie`（source=`SYS_PARAM`）
3. 环境变量 `WECHAT_OFFICIAL_COOKIE`（source=`ENV`）

**不复用** `oa_collector_account_bind`（ADR-068 Q8）。

## 3. UI 配置入口

| 页面 | 路径 |
|------|------|
| **系统参数** | 系统管理 → 系统参数 → **采集配置** Tab →「外部公众号采集 Cookie」 |
| **外部采集配置** | 外部采集配置 → 外部账号 Tab 顶部 Alert 指向系统参数 |

ParamManage 对该键：`textarea` 编辑 · 列表 `SENSITIVE_PARAM_KEYS` 脱敏 `******`（与 dingtalk secret 同类）。

## 4. 安全说明

| 层 | 策略 |
|----|------|
| 租户表 `oa_tenant_collector_credential.cookie_encrypted` | AES-256 |
| `sys_param` 值 | 与 `dingtalk.client-secret` 同模式：库内明文 · 列表 UI 脱敏（ParamService 无 encrypt 钩子） |

## 5. Files changed

| 文件 | 变更 |
|------|------|
| `V177__wechat_external_collect_cookie_param.sql` | seed sys_param |
| `ExternalCollectCredentialParamSupport.java` | 读 sys_param · ensureDefault |
| `TenantCollectorCredentialResolver.java` | WECHAT_OFFICIAL sys_param + env 回退 |
| `ParamManage.vue` | textarea + 脱敏 |
| `system-param.ts` | `WECHAT_OFFICIAL_COOKIE_PARAM_KEY` · `SENSITIVE_PARAM_KEYS` |
| `ExternalCollectConfig.vue` | Alert 指向系统参数 |
| `apply_v177_wechat_external_cookie_param.py` | Beta apply 脚本 |
| `ADR-068-M10-统一外部数据采集任务.md` | §2.2 addendum + changelog |

## 6. 验证

| 项 | 结果 |
|----|------|
| `mvn compile` (ops-server) | ✅ PASS |
| FE lints (`ParamManage.vue`, `system-param.ts`) | ✅ 无报错 |

## 7. 手工测试步骤

1. **Apply 迁移**（beta Flyway 禁用时）：
   ```powershell
   python scripts/integration-config/apply_v177_wechat_external_cookie_param.py
   ```
2. 重启 ops-server（或本地 Flyway 自动跑 V177）
3. 登录 → **系统管理 → 系统参数 → 采集配置**
4. 编辑「外部公众号采集 Cookie」，粘贴有效 mp.weixin.qq.com Session Cookie，保存
5. **外部采集配置** → 找到 WECHAT_OFFICIAL 账号（如 config id=41）→ 开启「是否采集」（应不再报 1512）
6. **采集任务** → 执行「统一外部数据采集任务」→ 日志中 config 41 应使用 Cookie 采集（source=SYS_PARAM，非 skip）
7. （可选）清空 sys_param 值，设置 env `WECHAT_OFFICIAL_COOKIE`，验证第三优先级
8. （可选）同时配置租户表凭账号，验证租户表优先（source=TENANT_CREDENTIAL）

## 8. 预期行为

| 场景 | toggle 采集 | 任务运行 |
|------|-------------|----------|
| 三者皆空 | 1512 阻断 | skip |
| 仅 sys_param 有值 | 通过 | source=SYS_PARAM |
| 租户表 + sys_param 皆有 | 通过 | 优先租户表 |

---

**结论**：P0 运维可在系统参数录入公众号 Cookie，无需等待 P1 租户凭账号 UI。
