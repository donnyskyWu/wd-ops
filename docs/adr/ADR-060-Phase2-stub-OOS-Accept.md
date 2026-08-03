# ADR-060：Phase 2 stub 域 Accept（M10 collect / Douyin / internal）

| 字段 | 值 |
|------|---|
| 状态 | **Accepted** |
| 日期 | **2026-08-01** |
| 关联 | ADR-047 · ADR-048 · ADR-049 · ADR-052 · ADR-058 · [终态缺口计划](../delivery/OPS-FOOTBALL-终态缺口执行计划-20260731.md) · [GAP-INVENTORY](../delivery/e2e-artifacts/P5-MIGRATE-8-cutover/GAP-INVENTORY.md) |
| 触发 | 尾巴收口：剩余 stub「开放缺口」→ 产品书面 Accept / Phase 2 OOS |

## 1. 背景

P-A…P-G 已关闭可 Spec 真迁域。`DeferredCutoverStubController` 仍覆盖三类前缀。按 Spec 驱动铁律：未写明或依赖 Phase 2（M10）的不得发明实现；用户授权本阶段将下列缺口 **从「开放 guilt」移出**，记为 **Accepted permanent stub until Phase 2**。

## 2. 决策表（Stub disposition）

| 域 / 前缀 | Spec | 处置 | 理由 |
|-----------|------|------|------|
| M10 `collect/**` · `collector-bind/**` · `config/{external-source,external-collect,order-collect,internal-collect}` | API-M10 / PRD-M10；Phase 2 **Out of Scope**（phase-gate-protocol） | **OOS Accept**（永久 stub 至 Phase 2） | Gate 协议禁止本期实现采集；stub GET 空页 / 写 410 为安全桩 |
| `/account/douyin-followers/**`（DouyinFollowers） | Spec 薄弱 / 无独立强制 API 条款 | **OOS Accept**（永久 stub 至 Phase 2 或产品补 Spec） | 禁止发明 API；产品确认本阶段 Accept |
| `/internal/**`（PersonalWechat / Wework / TripleRel 等 Controllers） | API-M4 §2–7 有路径 Spec | **Accept stub（本阶段）** | legacy Controllers（git `7e5f1b709`）含 **奥创 sync**（`sync-friends/messages/devices`）与 **WeComAdapter**（M10 Channel）；无法在不发明 M10/奥创联调的前提下完整迁入。M4 主数据（Company/Realname/Phone/SimCard）已在非 `/internal` 路径真迁 |
| Parallel system user/role/dept/permission/tenant/dict/dev | Football Admin SSOT | **已 410**（非本 ADR 新决） | 既有 `PARALLEL_SYSTEM_CRUD_DEPRECATED` |

**未迁入代码**：不从 `legacy-archive` 恢复 `/internal/**` Controllers；不实现 M10 collector / DouyinFollowers。

## 3. 后果

1. GAP-INVENTORY / 终态缺口计划将上表标为 **Closed-Accept / Phase 2**，不再列为阻塞「开放缺口」。
2. `DeferredCutoverStubController` **保留**上述前缀，直至 Phase 2 Slice 显式卸载。
3. Phase 2 启动时：按 API-M10 / 补齐的 Douyin Spec / API-M4+奥创通道分别开 Slice；本 ADR 不预写实现细节。

## 4. 产品确认

用户于 2026-08-01 会话授权：将上述三项从终态尾巴 **Accept 关闭**（本阶段不实现）。

## 5. Carve-out（ADR-061 · 假设 A1）

产品于 **2026-08-01** 授权假设 A1，对本 ADR 的 M10 `collect/**` 做 **部分卸载**：

| 路径 / 能力 | 处置 |
|-------------|------|
| `/admin-api/ops/collect/task/**`（ensure / list / members / start / stop · `collect_enabled` 成员同步） | **本 Slice 真实现**（见 [ADR-061](./ADR-061-租户级统一采集任务.md)） |
| collect log 只读（page / detail） | **§5.2 真实现**（2026-08-02） |
| EXTERNAL 外部采集配置（账号 + 关键词 CRUD） | **§5.3 真实现**（2026-08-02） |
| collect quality · external-source/order-collect/internal-collect config · DouyinFollowers · `/internal/**` | **仍 OOS Accept / stub**（本 ADR §2 不变；collector-bind 见 §5.1） |
| UnifiedCollector 多账号实际跑批 | ADR-061 follow-up；不阻塞 A1 toggle/membership DoD |

`DeferredCutoverStubController` 须 **排除** 已挂真 Controller 的 `/collect/task/**`，避免映射冲突。

## 5.1 Carve-out（假设 A · CollectorBind / QR · 2026-08-02）

产品于 **2026-08-02** 授权假设 A，对本 ADR 的 M10 `collector-bind/**` 做 **部分卸载**（以 [API-M10](../engineering/API-M10-数据采集.md) §3 + [ADR-050](./ADR-050-M4-采集Tab扫码登录.md) 为准；从 git `2a64362^` 恢复至 `football-module-ops`）：

| 路径 / 能力 | 处置 |
|-------------|------|
| `/admin-api/ops/account/{id}/collector-bind`（GET/POST）· sync · test-connection | **本 Slice 真实现**（`CollectorAccountBindController` + `UnifiedCollectorAdapter`） |
| `/admin-api/ops/account/{id}/collector-bind/qr-login/{start,poll,cancel}` | **本 Slice 真实现**（`CollectorQrLoginService` 代理 unify-collector-api） |
| `/admin-api/ops/collector-bind/batch-import` | **本 Slice 真实现**（`CollectorBatchBindController`） |
| collect log / quality · internal-collect/external-source/order-collect · DouyinFollowers · `/internal/**` · SyncService 全量落库 | **仍 OOS Accept / stub**（本 ADR §2 不变；EXTERNAL config 见 §5.3） |

权限统一 `ops:account:list` / `ops:platform-account:list`（不用 `oa:*`）。  
`DeferredCutoverStubController` 须 **排除** 上述 collector-bind 路径，避免映射冲突。

## 5.2 Carve-out（collect log 只读 · 2026-08-02）

产品期望：统一任务「立即执行」后「查看日志」可见 `oa_collect_log` 行（API-M10 §1.6–1.7；ADR-061 run 已写库）。本阶段对 M10 `collect/log` 做 **只读卸载**：

| 路径 / 能力 | 处置 |
|-------------|------|
| `GET /admin-api/ops/collect/log/page` · `/list` · `/{id}` | **真实现**（`CollectLogController` + `CollectLogService`，读 `oa_collect_log` + 解析 `result_json`） |
| collect quality · bridge · internal-collect/external-source/order-collect · DouyinFollowers · `/internal/**` · SyncService 全量落库 | **仍 OOS Accept / stub**（本 ADR §2 不变；EXTERNAL config 见 §5.3） |

权限：`ops:collect:log:list`（兼容 `ops:collect:task:list`，任务页跳转日志）。  
`DeferredCutoverStubController` 须 **排除** `/collect/log/**`，避免映射冲突。

## 5.3 Carve-out（外部采集配置 · EXTERNAL account/keyword · 2026-08-02）

产品期望：**现在需要做外部账号的采集** — 配置侧须可新增/列表外部账号（非 410 stub）。本阶段对 M8 `config/external-collect` 做 **真实现卸载**（以 [API-M8](../engineering/API-M8-配置管理.md) §2 为准；从 git `2a64362^` 恢复至 `football-module-ops`）：

| 路径 / 能力 | 处置 |
|-------------|------|
| `GET/POST/PUT/DELETE /admin-api/ops/config/external-collect/{list,create,update,delete,toggle-status,test-connection,import}` | **真实现**（`ExternalCollectConfigController` + `CollectConfigService` · `scope=EXTERNAL` · 表 `oa_collect_config`） |
| `GET/POST/PUT/DELETE …/external-collect/keyword/{list,create,update,delete}` | **真实现**（`KeywordConfigService` · 表 `oa_config_keyword`） |
| `platformType` 字典 | **`dict_platform_type`**（对齐 FE 五平台；覆盖旧实现 `dict_third_platform`） |
| EXTERNAL 实际跑批 / `ExternalCollectorAdapter` / unify-collector 竞品通道 | **仍 OOS / follow-up**（本 Slice 仅配置 CRUD；不阻塞 FE 新增） |
| external-source · order-collect · internal-collect · DouyinFollowers · `/internal/**` · collect quality | **仍 OOS Accept / stub**（本 ADR §2 不变） |

权限：`ops:config:external-collect:list`（不用 `oa:*`）。  
`DeferredCutoverStubController` 须 **排除** `/config/external-collect/**`，避免映射冲突。

## 6. 变更记录

| 日期 | 说明 |
|------|------|
| 2026-08-01 | 初稿 Accepted；对齐本地库名 `shenyu-ops` 收口会话 |
| 2026-08-01 | §5 carve-out：ADR-061 统一采集任务 + collect_enabled 授权实现 |
| 2026-08-02 | §5.1 carve-out：假设 A CollectorBind/QR + batch-import 真实现 |
| 2026-08-02 | §5.2 carve-out：collect log 只读（page/detail）真实现；quality 仍 stub |
| 2026-08-02 | §5.3 carve-out：external-collect 账号/关键词配置 CRUD 真实现；跑批仍 follow-up |
| 2026-08-02 | §5.4 carve-out：ADR-026 钉钉通知恢复（sys_param 凭证 + plan start TASK_PENDING） |

## 5.4 Carve-out（钉钉通知恢复 · ADR-026 · 2026-08-02）

产品授权：**按 ADR-026 恢复钉钉通知**，凭证放 OPS 系统参数（非 yaml/git）。

| 路径 / 能力 | 处置 |
|-------------|------|
| `NotificationServiceImpl` · `DingTalkWorkNotifyClient` · `DingTalkRobotClient` · `DingTalkParamSupport` | **本 Slice 真实现**（读 `sys_param`；Feign `AdminUserApi` 取 `dingtalk_user_id`） |
| 内容计划启动 → PENDING 任务执行人 `TASK_PENDING` | **本 Slice**（站内信 + 钉钉双通道） |
| `MonitorAlertScanner`（粉丝/作品阈值） | **follow-up**（仍 OOS） |
| Flyway V170 dingtalk param seed | **占位**；用户在 M9 系统参数 UI 填真实凭证 |

`NoopNotificationService` 已移除；参数键见 ADR-026 §1 表。
