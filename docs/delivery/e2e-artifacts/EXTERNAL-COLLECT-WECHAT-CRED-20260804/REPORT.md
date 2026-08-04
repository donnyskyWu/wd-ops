# EXTERNAL-COLLECT-WECHAT-CRED-20260804

> 日期：2026-08-04 · 现象：统一外部数据采集任务 **部分成功**，config:41 公众号凭账号缺失

## 1. Root cause（config 41）

| 字段 | 值 |
|------|-----|
| `id` | **41** |
| `tenant_id` | 1 |
| `scope` | EXTERNAL |
| `platform_type` | **WECHAT_OFFICIAL** |
| `config_name` | 腾讯云开发者 |
| `account_identifier` | `QcloudCommunity` |
| `collect_enabled` | **1**（已加入统一外部任务成员） |
| `status` | ENABLED |

**根因**：租户 1 在 `oa_tenant_collector_credential` 中 **无任何行**（`COUNT(*)=0`），尤其缺少 `platform=WECHAT_OFFICIAL` + `credential_profile=default` + `status=ENABLED` 的运营会话 Cookie。

执行链路：

1. `ExternalCollectRunService.executeExternalUnified` 遍历成员 config 39/40（DOUYIN）+ **41（WECHAT_OFFICIAL）**
2. DOUYIN 两配置成功采集 ~451 条 → 计入 `recordCount`
3. config 41 进入 `ExternalAccountCollectExecutor.collectWechatOfficialAccount`
4. `TenantCollectorCredentialResolver.resolve(tenantId=1, WECHAT_OFFICIAL, default)` → **empty**
5. 返回 failure → `CollectExecutionResult.aggregate` 记 **PARTIAL**

错误抛出位置：

```136:141:football-backend-saas/football-module-ops/football-module-ops-server/src/main/java/football/module/ops/service/collect/external/ExternalAccountCollectExecutor.java
// (fix 前) collectWechatOfficialAccount 内 cred.isPresent() == false
```

凭账号解析（ADR-052 §3.4 · ADR-068 §2.2）：

```27:48:football-backend-saas/football-module-ops/football-module-ops-server/src/main/java/football/module/ops/service/collect/external/TenantCollectorCredentialResolver.java
// 查 oa_tenant_collector_credential；WECHAT_OFFICIAL 无 env 回退（仅 KUAISHOU 可回退 KUAI_SHOU_COOKIE）
```

## 2. `oa_tenant_collector_credential` 所需字段（公众号）

| 字段 | 公众号要求 |
|------|-----------|
| `tenant_id` | 当前租户 |
| `platform` | `WECHAT_OFFICIAL` |
| `credential_profile` | `default`（任务未指定 profile 时） |
| `cookie_encrypted` | **必填** · mp.weixin.qq.com 运营后台会话 Cookie · AES-256 |
| `auth_token_encrypted` | 可空 |
| `status` | `ENABLED` |
| `expire_at` | 建议填写，便于过期告警 |

**不复用**内部账号 Cookie / `oa_collector_account_bind`（ADR-068 Q8 · ADR-052 §3.4）。

## 3. 用户须做什么 vs 代码修复

### 用户/运维（使 config 41 真正采集成功）

1. 在 `oa_tenant_collector_credential` 为 tenant_id=1 插入一行：
   - `platform=WECHAT_OFFICIAL`
   - `credential_profile=default`
   - `cookie_encrypted=<AES 加密后的运营会话 Cookie>`
   - `status=ENABLED`
2. （P1+ UI 未上线前）可由 DBA/脚本录入；UI 规划见 UX-M8「租户采集凭账号」Tab
3. 重新执行「统一外部数据采集任务」

**仅关闭 config 41「是否采集」**也可消除 PARTIAL，但不会采集公众号竞品。

### 代码修复（本次，已改未提交）

| 变更 | 目的 |
|------|------|
| 凭账号缺失 → **SKIPPED**（非 failure） | DOUYIN 成功 + 公众号缺凭账号 → 日志 **SUCCESS** + 跳过说明，不再误导性 PARTIAL |
| 集中化 `missingCredentialMessage()` | 明确表名、profile、配置入口 |
| 开启 `collect_enabled` 时校验（1512） | 阻止新开通公众号采集而无凭账号 |
| `ExternalCollectConfig.vue` 告警 + 表单提示 | 运营可见前置条件 |
| `OaErrorCodes.COLLECTOR_TENANT_CREDENTIAL_MISSING` (1512) | 统一业务错误码 |

## 4. Files changed

| 文件 | 变更 |
|------|------|
| `football-module-ops-server/.../OaErrorCodes.java` | +1512 |
| `.../TenantCollectorCredentialResolver.java` | `requiresTenantCredential` / `hasCredential` / `missingCredentialMessage` |
| `.../CollectExecutionResult.java` | `skipped()` · `TypeOutcome.skip` · aggregate 跳过语义 |
| `.../ExternalAccountCollectExecutor.java` | 前置 skip · 改进快手文案 |
| `.../CollectConfigServiceImpl.java` | 开启采集前校验凭账号 |
| `.../KeywordConfigServiceImpl.java` | 同上（关键词） |
| `.../UnifiedCollectRunService.java` | 日志写入 skip 说明 · typeResults.skipped |
| `football-front/.../ExternalCollectConfig.vue` | 告警 + 表单 hint + toggle 错误展示 |
| `docs/delivery/e2e-artifacts/EXTERNAL-COLLECT-WECHAT-CRED-20260804/*` | 本报告 + smoke |

## 5. 验证

| 项 | 结果 |
|----|------|
| `mvn compile` (ops-server) | ✅ 通过 |
| Smoke（**部署前** beta 48080） | ❌ 仍为旧行为：`logStatus=PARTIAL`，toggle 未拦截（需重启 ops-server） |
| Smoke（**部署后**预期） | SUCCESS + `已跳过（凭账号未配置）: config:41: ...`；重新开启 41 采集返回 1512 |

```bash
python docs/delivery/e2e-artifacts/EXTERNAL-COLLECT-WECHAT-CRED-20260804/smoke_wechat_cred.py
```

## 6. Re-run 是否会成功？

| 场景 | 结果 |
|------|------|
| **不录入凭账号**，仅 redeploy 本次代码 | ✅ 任务整体 **SUCCESS**（451 条 DOUYIN）；config 41 **跳过**并带说明 |
| **录入 WECHAT_OFFICIAL 凭账号** + redeploy | ✅ config 41 应能 search-account + article-collect（取决于 Cookie 有效性与 collector） |
| **不 redeploy、不录入凭账号** | ❌ 仍为 PARTIAL + 旧错误文案 |

## 7. 前端 UI 现状

- `ExternalCollectConfig.vue`：**无**租户凭账号 CRUD（Spec P1+，`/config-tenant-credential` 未实现）
- 本次仅增加 **说明性 Alert**；完整录入 UI 待 M8 P1 Slice

---

**结论**：config 41 本身配置正确；失败原因为 **租户级公众号运营凭账号未配置**（符合 ADR-052/068 设计）。用户须先录入 `oa_tenant_collector_credential`；代码侧改为 skip + 拦截 + UI 提示，避免「部分成功」误导。
