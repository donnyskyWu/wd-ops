# ADR-052: Ops 外部竞品四平台采集通道（Channel-D）

> **状态**: 草案（待 Gate 立项） | **日期**: 2026-07-08
> **决策人**: 产品 + 开发团队（用户确认立项）
> **关联**: [ADR-014](./ADR-014-M8-配置管理数据模型.md) · [ADR-047](./ADR-047-M4-平台账号凭证SSOT与Collector映射.md) · [ADR-049](./ADR-049-M10-全量采集与展示桥接.md) · [PRD-M7](../product/PRD-M7-作品监测.md) · [PRD-M8](../product/PRD-M8-配置管理.md) · [PRD-M10](../product/PRD-M10-数据采集.md) · [M10-EXTERNAL-四平台竞品采集-SLICE](../delivery/M10-EXTERNAL-四平台竞品采集-SLICE.md)

## 背景

### 现状审计结论（2026-07-08）

| 层 | 结论 |
|----|------|
| **unify-collector-api**（外部仓库 · 本仓库 `api.json` SSOT） | 快手 external **最完整**（`user-videos`）；公众号 **部分**（`search-account` + `article-collect`，依赖运营自有号 Cookie）；抖音 **单视频**（`parse-video`）；视频号 external **未实现**（仅有 stub 路由） |
| **Ops M10** | Channel-A **INTERNAL MVP** 已落地（`UnifiedCollectorAdapter` + bind）；**无** `ExternalCollectorAdapter`；**无** `method=EXTERNAL` 执行路由 |
| **Ops M8** | 外部竞品账号配置 UI 已存在（`scope=EXTERNAL`, `sub_type=account`）；配置 **未** 驱动采集执行 |
| **Ops M7** | 监测页读 `oa_external_work` seed；**无** 真实采集灌入链路 |

M7 PRD 明确：竞品实时抓取 **Out of Scope**，依赖 M10 数据采集（§2.2）。本 ADR 定义 **Channel-D** 与 Channel-A 的分工，并给出四平台 phased Gate。

---

## 决策

### 1. 通道命名与分工

| 通道 | Adapter | 采集对象 | 凭证 SSOT | 映射 |
|------|---------|----------|-----------|------|
| **A · INTERNAL** | `UnifiedCollectorAdapter` | **自有**平台账号 | M4 `oa_account` + `oa_collector_account_bind` | `acc_{platform}_{hash}` |
| **D · EXTERNAL** | `ExternalCollectorAdapter`（新建） | **竞品**外部账号 | **竞品标识**：M8 `oa_collect_config`（`scope=EXTERNAL`, `sub_type=account`）；**运营凭账号**：`oa_tenant_collector_credential`（租户级 · §3.4） | **无 bind**；`account_identifier` = 平台 user_id / fakeid / sec_uid 等 |
| B · 奥创 | `AochuangAdapter` | 个微 | `oa_aocreate_api` | 不变（ADR-045） |
| C · 企微 | `WeComAdapter` | 企微应用 | `oa_wework_account` | 不变（ADR-048） |

**硬约束**：

- Channel-D **禁止** 复用 `oa_collector_account_bind` 与 M4 自有账号 bind 流程
- Channel-D 调用 collector **`/api/v1/external/*`** 及经 ADR 批准的 **`/api/v1/internal/wechat-mp/*`**（公众号竞品图文须运营自有号 Cookie，见 §3）
- Ops **不** 在本仓库实现 collector 爬虫逻辑；缺口在 **unify-collector-api** 立项补齐

### 2. 平台能力矩阵与 collector 缺口

> 来源：仓库根 `api.json` + 2026-07-08 代码走查。

| 平台 | `platform_type` | Collector 已有路由 | 能力评级 | Ops 待建 | Collector 待补 |
|------|-----------------|-------------------|----------|----------|----------------|
| **快手** | `KUAISHOU` | `GET /api/v1/external/kuaishou/user-videos?user_id=` | ✅ **可用** | Adapter + 落库 + 任务路由 | 粉丝/profile 日聚合（可选 P1+） |
| **公众号** | `WECHAT_OFFICIAL` | `GET .../internal/wechat-mp/search-account` · `POST .../article-collect` | 🟡 **部分** | 搜索绑定 fakeid + 图文采集 + 落库 | 外部账号粉丝/profile **无**公开 API |
| **抖音** | `DOUYIN` | `GET .../external/douyin/parse-video?video_url=` | 🟡 **单视频** | 任务模型 + 单视频落库 | **`user-profile` / `user-videos` external 路由** |
| **视频号** | `WECHAT_VIDEO` | `GET .../external/wechat-channels/follower-stats` · `search-video`（参数/实现 stub） | ❌ **未实现** | 任务壳 + Gate 占位 | **external user 搜索 / 作品列表 / 粉丝** 全链路 |

#### 2.1 unify-collector-api 待补清单（外部 repo · 本 ADR 仅登记）

| 优先级 | 平台 | 建议路由 | 说明 |
|--------|------|----------|------|
| P0 | 快手 | （已有 `user-videos`） | 稳定 pagination + rate limit 文档 |
| P1 | 公众号 | （已有 `search-account` / `article-collect`） | 明确 `account_id` = 运营自有 bind 号 |
| P2 | 抖音 | `GET /api/v1/external/douyin/user-profile` · `.../user-videos` | 输入 `sec_uid` 或 share URL |
| P3 | 视频号 | `GET /api/v1/external/wechat-channels/search-user` · `.../user-works` · `.../follower-stats`（实参） | 当前 stub 需重写 |

### 3. 数据模型

#### 3.1 配置 SSOT（已有 · 不新建表）

| 概念 | 表 | 说明 |
|------|-----|------|
| 外部竞品账号配置 | `oa_collect_config` | `scope=EXTERNAL`, `sub_type=account`；`platform_type` → `dict_third_platform`；`account_identifier` = 平台 ID |
| 关键词监测 | `oa_config_keyword` | M7 扩展；本切片 P0 不阻塞 |

M8 UI：`/ops/config-external-collect` · API `/admin-api/oa/config/external-collect/*`（已实现）。

#### 3.2 采集落库（新建 + 复用）

| 表 | 决策 | 说明 |
|----|------|------|
| **`oa_external_account`** | **新建（V133+）** | 竞品账号 **快照**；`collect_config_id` FK → `oa_collect_config.id`；字段：`platform_type`, `external_user_id`, `display_name`, `follower_count`, `work_count`, `avatar_url`, `last_synced_at` |
| **`oa_external_work`** | **复用** | 已有 M7 消费；`account_id` FK → **`oa_external_account.id`**；增 `collect_config_id`, `platform_work_id`（幂等 UK `(tenant_id, platform_type, platform_work_id)`）, **`comment_count`** |
| **`oa_external_follower_daily`** | **新建（V133+）** | M7 高/低粉分析；UK `(tenant_id, external_account_id, stat_date)` |

**不** 写入 Channel-A 表（`oa_douyin_video`, `oa_kuaishou_video`, `oa_wechat_mp_article` 等）。

**不** 写入 M4 `oa_account`（竞品非自有资产）。

#### 3.4 运营凭账号 SSOT（新建 · 租户级）

> **产品决策（2026-07-08 ✅）**：公众号 / 平台 **operator 凭账号**（Cookie / Token，如 mp.weixin.qq.com 运营会话）存放于 **租户级**，**非** 采集任务级，**非** `oa_collect_config` 列内嵌密钥。

**表选型**：**新建 `oa_tenant_collector_credential`**（V133+ Flyway）。

**不** 扩展 `oa_collect_config`（`scope=EXTERNAL_SOURCE`）理由：该 scope 语义为第三方数据源 API（新榜 / 飞瓜 · `api_url` + `api_key`），与 collector operator 会话（Cookie 轮换、`expire_at`、平台 ToS 风险）生命周期不同；混用会违反 ADR-014 scope 字段约束。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | BIGINT | PK | |
| `tenant_id` | BIGINT | NOT NULL · UK 分量 | 租户隔离（1504） |
| `platform` | VARCHAR(64) | NOT NULL · UK 分量 | `dict_third_platform`：`KUAISHOU` · `WECHAT_OFFICIAL` · `DOUYIN` · `WECHAT_VIDEO` 等 |
| `credential_profile` | VARCHAR(64) | NOT NULL · UK 分量 · 默认 `'default'` | 同租户同平台多套凭账号（如主号 / 备用号）；任务可选指定 |
| `profile_name` | VARCHAR(128) | | 展示名（如「运营主公众号会话」） |
| `cookie_encrypted` | TEXT | AES-256 | 平台 Cookie 原文加密存储；响应脱敏 |
| `auth_token_encrypted` | VARCHAR(512) | 可空 · AES-256 | 快手等需独立 token 时 |
| `expire_at` | DATETIME | 可空 | 会话预计失效时间；到期触发 `EXPIRED` + M8 告警 |
| `conn_status` | VARCHAR(20) | 默认 `DISCONNECTED` | `CONNECTED` / `DISCONNECTED`（探活结果） |
| `status` | VARCHAR(32) | 默认 `ENABLED` | `ENABLED` / `DISABLED` / `EXPIRED` |
| `last_verified_at` | DATETIME | 可空 | 最近一次 collector 探活 |
| `remark` | VARCHAR(512) | | |
| 审计列 | — | | `creator` · `create_time` · `updater` · `update_time` · `deleted` |

**UK**：`(tenant_id, platform, credential_profile, deleted)` — 每租户每平台每 profile **一套**凭账号，该租户下全部 Channel-D 任务共享。

**解析规则**（`ExternalCollectorAdapter` / `TenantCollectorCredentialResolver`）：

1. 从 `TenantContext` 取 `tenant_id`
2. 从 `oa_collect_config.platform_type` 取 `platform`
3. 任务 `oa_collect_task.credential_profile`（V133+ 可空列，默认 `'default'`）或租户默认 profile
4. 查 `oa_tenant_collector_credential` → 解密后注入 collector HTTP 请求
5. **禁止**在 `oa_collect_config` 或任务行写入 `cookie` / `token` 明文或密文

**分平台凭账号策略**：

| 平台 | P 阶段 | 凭账号来源 | 说明 |
|------|--------|------------|------|
| **快手** | P0 | **租户级** > 部署级 env | 租户表有 `ENABLED` 行则优先；否则回退 `KUAI_SHOU_COOKIE`（及可选 `KUAI_SHOU_AUTH_TOKEN`）环境变量；P0 Gate 可用 env，P1+ 建议租户录入 |
| **公众号** | P1 | **租户级**（必须） | 租户 `WECHAT_OFFICIAL` 会话 Cookie 作为 bridge，供 collector `/internal/wechat-mp/search-account` · `article-collect`；`account_id` 参数传 collector bind 映射 id（由凭账号解析，**非**竞品 `account_identifier`） |
| **抖音** | P2 | 租户级（若路由需登录态） | 公开 external 路由优先无凭账号；若 collector 文档要求 Cookie 则走本表 |
| **视频号** | P3 | 租户级（预期必须） | collector stub 补齐后按平台要求配置 |

**M8 UI（P1+ Slice）**：`/ops/config-external-collect` 增「租户采集凭账号」子 Tab 或独立 `/ops/config-tenant-credential`；CRUD + 脱敏展示 + 探活按钮。

#### 3.5 任务与日志（复用 M10 框架）

| 字段 | Channel-D 取值 |
|------|----------------|
| `oa_collect_task.method` | **`EXTERNAL`**（新增字典值 · Flyway） |
| `oa_collect_task.source` | **`UNIFY_COLLECTOR_EXTERNAL`**（新增） |
| `oa_collect_task.account_id` | **`NULL`**；改 FK **`collect_config_id`** → `oa_collect_config.id`（V133 增列，nullable；INTERNAL 任务仍用 `account_id`） |
| `oa_collect_task.data_type` | 见 Slice § dataType 枚举 |
| `oa_collect_task.credential_profile` | 可空 · 默认 `'default'`；引用 §3.4 UK 第三分量 · **不存密钥** |
| `oa_collect_log.result_json` | 沿用 ADR-049 `typeResults[]` 结构 |

### 4. 端到端数据流

```
M8 外部账号配置 (oa_collect_config EXTERNAL)
        │
        ▼
M10 采集任务 (method=EXTERNAL, collect_config_id, credential_profile?)
        │
        ▼
CollectExecutionService ──route──► ExternalCollectorAdapter
        │                              │
        │                              ▼
        │                    oa_tenant_collector_credential
        │                    (tenant_id + platform + profile → cookie/token)
        ▼
unify-collector-api (/external/* 或批准的 /internal/wechat-mp/*)
        │
        ▼
落库 oa_external_account / oa_external_work / oa_external_follower_daily
        │
        ▼
M7 作品监测 / M6 外部竞品大屏 (scope=EXTERNAL)
```

**与 Channel-A 展示桥接分离**：M1 `CollectedDataQueryService` **不** 读 Channel-D 表；M7 `MonitorService` 读 `oa_external_*`。

### 5. 推荐优先级矩阵

| 顺序 | 平台 | ROI 理由 | 首 shippable |
|------|------|----------|--------------|
| **P0** | 快手 | collector 路由最全；无需 bind；结构化 user-videos | **Gate P0：1 竞品号 → 作品列表落库 → M7 列表可见** |
| **P1** | 公众号 | 竞品图文是核心监测场景；collector 路由已有 | Gate P1：search-account + article-collect E2E |
| **P2** | 抖音 | 竞品量大；collector 仅单视频 | Gate P2：user-videos 路由 + Ops 全量作品 |
| **P3** | 视频号 | 双端缺口最大 | Gate P3：collector + Ops 联调签收 |

### 6. 分平台 Gate 通过条件

| Gate | 平台 | Checklist |
|------|------|-----------|
| **GATE-EXT-P0** | 快手 | M8 配置 1 条 · 任务 run SUCCESS · `oa_external_work` ≥1 · M7 hot-works API 非空 · IT `M10ExternalKuaishouS01IT` P0 100% |
| **GATE-EXT-P1** | 公众号 | 租户级 `oa_tenant_collector_credential`(WECHAT_OFFICIAL) 可用 · search-account 解析 fakeid · article-collect ≥1 行 · `content_type=ARTICLE` |
| **GATE-EXT-P2** | 抖音 | collector `user-videos` 上线 · 任务按 `sec_uid` 全量分页 · 幂等 UK 无重复 |
| **GATE-EXT-P3** | 视频号 | collector external user 三路由 · Ops 同步 · M7 + 大屏 KPI 非 seed |

### 7. 风险与缓解

| 风险 | 影响 | 缓解 |
|------|------|------|
| **合规 / ToS** | 竞品爬取法律与平台协议 | 仅采 **公开** 数据；租户级开关；审计日志；频率 ≤ M8 `collect_frequency` |
| **Rate limit / 封禁** | 任务 FAILED / PARTIAL | 指数退避（复用 M10 重试）；collector circuit breaker；单租户 QPS 上限 ADR 增量 |
| **Collector stub** | 视频号/抖音 Gate 阻塞 | P0 不依赖；P2/P3 前 **collector Gate 子项** 必须 ✅ |
| **公众号依赖运营会话** | 无租户级 Cookie 则 search/article 不可用 | `oa_tenant_collector_credential` 租户级维护 + `expire_at` 告警；**非**任务级、**非** `oa_collect_config` 内嵌 |
| **Schema 漂移** | M7 `account_id` 语义混淆 | V133 迁移 + PRD-M7 增量；seed 回填 |

---

## 产品决策

| # | 问题 | **决议** | 状态 | 阻塞 P0 |
|---|------|----------|------|---------|
| **Q1** | `dict_collect_method` 新增 `EXTERNAL` vs 复用 `CRAWLER` | **A — 新增 `EXTERNAL`** | 待 Gate | 否（P0 可先 hardcode IT） |
| **Q2** | 任务主体 FK | **A — `collect_config_id`** | ✅ 2026-07-08 | 是 |
| **Q3** | 公众号 / 平台 operator 凭账号（Cookie/Token）存放层级？ | **A — 租户级**：`oa_tenant_collector_credential`；任务经 `tenant_id` + `platform` + 可选 `credential_profile` 引用；**禁止**任务级或 `oa_collect_config` 内嵌密钥 | ✅ **已确认**（2026-07-08） | P1 |
| **Q4** | 抖音竞品标识 | **A — `account_identifier` = sec_uid** | 待 Gate | P2 |
| **Q5** | M7 是否切真实采集后下线 seed | **B — seed 保留为 demo；真实数据优先** | 待 Gate | 否 |

---

## Out of Scope（本 ADR）

- M8 `EXTERNAL_SOURCE` 第三方 API（新榜/飞瓜）对接 — 与 Channel-D **并行** 但 **不同 Slice**
- 小红书 / B 站 external — Phase 2-B
- 采集 → `oa_content_daily` 同步
- FR-M10-002 数据质量规则（Channel-D 专用规则另开 ADR）

---

## 后果

- 新增 [M10-EXTERNAL-四平台竞品采集-SLICE](../delivery/M10-EXTERNAL-四平台竞品采集-SLICE.md) 为执行 SSOT
- `MASTER-EXECUTION-TRACKER` §17.3 登记 P2-M10-D
- P0 实现前须用户确认 **Q2 + 各平台字段映射表**（见 Slice § API 契约）— **P0 快手映射 ✅ 2026-07-08**
- collector 缺口在 **unify-collector-api** 仓库单独立项，Ops 仅 HTTP Client 对接
