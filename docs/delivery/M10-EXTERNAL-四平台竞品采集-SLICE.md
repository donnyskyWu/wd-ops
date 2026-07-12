# M10-EXTERNAL：四平台竞品采集 Slice 计划

> **版本**：v1.0 | 2026-07-08
> **状态**：🔵 已立项（待 GATE-EXT-P0 启动开发）
> **SSOT 决策**：[ADR-052](../adr/ADR-052-Ops外部竞品四平台采集通道.md)
> **依赖**：M8 外部采集配置 ✅ · M10 任务/日志框架 ✅ · unify-collector-api（外部 repo）
> **原则**：**一片一会话**；collector 路由 **不在本仓库实现**

---

## 0. 目标

打通 **M8 外部竞品配置 → M10 Channel-D 执行 → M7/M6 展示** 全链路，分四平台 phased 交付。

| 平台 | P 阶段 | 首 shippable |
|------|--------|--------------|
| 快手 | **P0** | 竞品 `user-videos` E2E |
| 公众号 | P1 | search-account + article-collect |
| 抖音 | P2 | user-profile + user-videos（collector 先补） |
| 视频号 | P3 | external user 全链路（collector 先补） |

---

## 1. 推荐优先级矩阵

| 优先级 | 项 | 理由 | 依赖 |
|--------|-----|------|------|
| 🥇 **P0** | 快手 user-videos | collector **唯一完整** external 列表 API；无 bind；ROI 最高 | 无 collector 缺口 |
| 🥈 **P1** | 公众号 article | 竞品 **图文** 是 M7 核心场景；路由已有 | 租户级 `oa_tenant_collector_credential`(WECHAT_OFFICIAL) |
| 🥉 **P2** | 抖音 user-videos | 竞品量大；当前 **仅单视频** | collector P2 路由 |
| 4 **P3** | 视频号 external | **双端空白**；成本最高 | collector P3 路由 |

**不建议顺序**：视频号 → 抖音 → 快手（逆 ROI）。

---

## 2. 架构摘要

```
Channel-A (INTERNAL)          Channel-D (EXTERNAL)
UnifiedCollectorAdapter       ExternalCollectorAdapter  ← P0 骨架
oa_account + bind             oa_collect_config (M8) — 竞品标识 only
                              oa_tenant_collector_credential — 运营凭账号（租户级）
oa_*_video / oa_wechat_mp_*   oa_external_account / oa_external_work / oa_external_follower_daily
M1 CollectedDataQueryService  M7 MonitorService + M6 EXTERNAL 大屏
```

---

## 2.1 运营凭账号模型（✅ Q3 已决议）

> SSOT：[ADR-052 §3.4](../adr/ADR-052-Ops外部竞品四平台采集通道.md#34-运营凭账号-ssot新建--租户级)

| 决策项 | 内容 |
|--------|------|
| **层级** | **租户级** — 每租户每平台（每 `credential_profile`）一套凭账号，该租户全部 EXTERNAL 任务共享 |
| **表** | **新建 `oa_tenant_collector_credential`**（V133+）；**不**扩展 `oa_collect_config` / `EXTERNAL_SOURCE` |
| **任务引用** | `tenant_id`（上下文）+ `platform`（来自 `collect_config`）+ 可选 `credential_profile`（任务列，默认 `'default'`） |
| **禁止** | 在 `oa_collect_config` 或任务行内嵌 `cookie` / `token` |

**核心字段**：`tenant_id` · `platform` · `credential_profile` · `cookie_encrypted` · `auth_token_encrypted` · `expire_at` · `status` · `conn_status`

**凭账号解析优先级（快手 P0）**：

```
1. oa_tenant_collector_credential (tenant_id + KUAISHOU + profile, status=ENABLED)
2. 部署 env：KUAI_SHOU_COOKIE（+ 可选 KUAI_SHOU_AUTH_TOKEN）
3. 均无 → 任务 FAILED · 错误码 1501（凭账号缺失）
```

**公众号 P1**：必须配置租户级 `WECHAT_OFFICIAL` 会话；`search-account` / `article-collect` 的 `account_id` 由凭账号解析注入 collector，**非**竞品 `account_identifier`。

---

## 3. 分阶段交付

### P0 — Spec + Adapter 骨架 + 快手 E2E（预估 5–7 人日）

**Gate**：`GATE-EXT-P0`

| # | 交付物 | 说明 |
|---|--------|------|
| P0-1 | ADR-052 ✅ | 本文档 + MASTER 登记 |
| P0-2 | Flyway **V136** | `oa_external_account` · `oa_external_follower_daily` · `oa_collect_task.collect_config_id` · `oa_collect_task.credential_profile` · `oa_tenant_collector_credential` · `dict_collect_method.EXTERNAL` · `dict_collect_source.UNIFY_COLLECTOR_EXTERNAL` · `dict_collect_data_type` 外部枚举 |
| P0-3 | `ExternalCollectorAdapter` 骨架 | `service/collect/external/` · stub IT 可跑 |
| P0-4 | `ExternalCollectorApiClient` | HTTP 封装 `GET /api/v1/external/kuaishou/user-videos`；凭账号经 `TenantCollectorCredentialResolver`（租户表 > env） |
| P0-5 | `KuaishouExternalWorkSyncService` | 分页 → upsert `oa_external_work` + 刷新 `oa_external_account` |
| P0-6 | `CollectExecutionService` 路由 | `method=EXTERNAL` + `platform=KUAISHOU` + `dataType=EXT_KUAISHOU_USER_VIDEOS` |
| P0-7 | IT **`M10ExternalKuaishouS01IT`** | MockWebServer + stub collector 响应 |
| P0-8 | Spec 增量 | API-M10 § Channel-D · PRD-M10 § EXTERNAL · CHECKLIST-M10 §11 |

**P0 验收场景（人工）**：

1. M8 新建快手竞品：`account_identifier` = 快手 user_id
2. M10 创建任务：`method=EXTERNAL`, `collect_config_id`, `dataType=EXT_KUAISHOU_USER_VIDEOS`
3. `POST .../collect/task/{id}/run` → `SUCCESS`
4. `oa_external_work` 有新行；M7 `/monitor/hot-works` 可见

**P0 首 shippable 里程碑**：**快手 1 竞品号作品列表采集 + M7 展示**（约 **1 周** dev + Gate，含 Spec 评审）。

---

### P1 — 公众号 search-account + article-collect（预估 5–6 人日）

**Gate**：`GATE-EXT-P1`

| # | 交付物 | 说明 |
|---|--------|------|
| P1-1 | `oa_tenant_collector_credential` CRUD | M8 租户级凭账号 Tab · `WECHAT_OFFICIAL` 会话录入 + 探活；任务可选 `credential_profile` |
| P1-2 | `WechatMpExternalAccountSyncService` | `search-account` → 写 `oa_external_account.external_user_id=fakeid` |
| P1-3 | `WechatMpExternalArticleSyncService` | `article-collect` → `oa_external_work`（`content_type=ARTICLE`） |
| P1-4 | dataType | `EXT_WECHAT_MP_SEARCH` · `EXT_WECHAT_MP_ARTICLE_LIST` |
| P1-5 | IT | `M10ExternalWechatMpS01IT` |

**Collector 依赖**：已有 `/internal/wechat-mp/search-account` · `/article-collect`（**无新增**）。

**阻塞**：租户级公众号运营会话（`oa_tenant_collector_credential` · `WECHAT_OFFICIAL` · `status=ENABLED`）可用。

---

### P2 — 抖音 external user（预估 7–9 人日）

**Gate**：`GATE-EXT-P2`

| # | 交付物 | 说明 |
|---|--------|------|
| P2-0 | **collector 子 Gate** | 新增 `GET /api/v1/external/douyin/user-profile` · `user-videos` |
| P2-1 | `DouyinExternalAccountSyncService` | sec_uid → follower 快照 → `oa_external_follower_daily` |
| P2-2 | `DouyinExternalWorkSyncService` | user-videos 分页 → `oa_external_work` |
| P2-3 | dataType | `EXT_DOUYIN_USER_PROFILE` · `EXT_DOUYIN_USER_VIDEOS` |
| P2-4 | IT | `M10ExternalDouyinS01IT` |

**Collector 缺口**：当前仅 `parse-video`（单 URL）；**P2-0 未 ✅ 禁止 Ops 开发**。

---

### P3 — 视频号 external（预估 10–12 人日）

**Gate**：`GATE-EXT-P3`

| # | 交付物 | 说明 |
|---|--------|------|
| P3-0 | **collector 子 Gate** | `search-user` · `user-works` · 实参 `follower-stats` |
| P3-1 | `WechatChannelsExternal*SyncService` | 账号 + 作品 + 粉丝日聚合 |
| P3-2 | dataType | `EXT_WECHAT_VIDEO_USER` · `EXT_WECHAT_VIDEO_WORK_LIST` · `EXT_WECHAT_VIDEO_FOLLOWER_STATS` |
| P3-3 | IT + M6 大屏 KPI | 替换 seed `external_works` 为采集源 |

**Collector 缺口**：现有 `/external/wechat-channels/*` 为 **stub**；**P3-0 未 ✅ 禁止 Ops 开发**。

---

## 4. oa-server API 契约增量

> 完整 OpenAPI 在 P0 Gate 前写入 `API-M10-数据采集.md` § Channel-D。

### 4.1 采集任务 create/update（增量字段）

```json
{
  "taskName": "快手竞品-辛巴-作品",
  "platformType": "KUAISHOU",
  "method": "EXTERNAL",
  "source": "UNIFY_COLLECTOR_EXTERNAL",
  "collectConfigId": 96001,
  "accountId": null,
  "credentialProfile": "default",
  "dataType": "EXT_KUAISHOU_USER_VIDEOS",
  "frequency": "DAILY",
  "status": "ENABLED"
}
```

**校验**：

- `method=EXTERNAL` → `collectConfigId` **必填**；`accountId` **必须 null**；`credentialProfile` 可空（默认 `default`）
- `collectConfigId` 须指向 `oa_collect_config` 且 `scope=EXTERNAL`, `sub_type=account`, `platform_type` 与任务一致
- 凭账号由 `tenant_id` + `platform_type` + `credentialProfile` 解析；**不在此 API 传 cookie**
- `method=INTERNAL` → 沿用 ADR-049（`accountId` 必填）

### 4.2 新增 dataType 枚举（`dict_collect_data_type`）

| 值 | 平台 | P 阶段 |
|----|------|--------|
| `EXT_KUAISHOU_USER_VIDEOS` | 快手 | P0 |
| `EXT_WECHAT_MP_SEARCH` | 公众号 | P1 |
| `EXT_WECHAT_MP_ARTICLE_LIST` | 公众号 | P1 |
| `EXT_DOUYIN_USER_PROFILE` | 抖音 | P2 |
| `EXT_DOUYIN_USER_VIDEOS` | 抖音 | P2 |
| `EXT_WECHAT_VIDEO_USER` | 视频号 | P3 |
| `EXT_WECHAT_VIDEO_WORK_LIST` | 视频号 | P3 |
| `EXT_WECHAT_VIDEO_FOLLOWER_STATS` | 视频号 | P3 |

### 4.3 Collector HTTP 映射（Ops Client）

| dataType | HTTP | 关键参数 |
|----------|------|----------|
| `EXT_KUAISHOU_USER_VIDEOS` | `GET /api/v1/external/kuaishou/user-videos` | `user_id` ← `account_identifier` |
| `EXT_WECHAT_MP_SEARCH` | `GET /api/v1/internal/wechat-mp/search-account` | `keyword`, `account_id` ← 租户凭账号解析 |
| `EXT_WECHAT_MP_ARTICLE_LIST` | `POST /api/v1/internal/wechat-mp/article-collect` | `account_name`, `account_id`, `limit` ← 凭账号解析 |
| `EXT_DOUYIN_USER_VIDEOS` | `GET /api/v1/external/douyin/user-videos` | **collector P2 新增** · `sec_uid` |
| `EXT_WECHAT_VIDEO_*` | TBD | **collector P3 新增** |

### 4.4 落库字段映射（P0 快手 · ✅ 2026-07-08 已批）

| Collector 字段 | `oa_external_work` | 备注 |
|----------------|-------------------|------|
| `photo_id` / `id` | `platform_work_id`（新列） | UK `(tenant_id, platform_type, platform_work_id)` 幂等 |
| `caption` / `title` | `title` | |
| `view_count` | `play_count` | |
| `like_count` | `like_count` | |
| `comment_count` | `comment_count` | collector 有则映射 |
| `share_url` / `video_url` | `work_url` | **优先分享/播放链接** |
| `create_time` | `publish_time` | |
| — | `is_external=1` | 固定 |
| — | `account_id` | → **`oa_external_account.id`**（非 M4 `oa_account`） |
| — | `collect_config_id` | FK `oa_collect_config.id` |

| Collector 字段 | `oa_external_account` | 备注 |
|----------------|----------------------|------|
| `user_id` | `external_user_id` | |
| `user_name` | `display_name` | |
| `fan_count` | `follower_count` | 列表响应若有 |
| — | `collect_config_id` | FK |

---

## 5. IT / Gate Checklist

### 5.1 P0 Gate Checklist（`GATE-EXT-P0`）

- [ ] ADR-052 用户 Sign-off
- [ ] V133 迁移 + `SeedVerificationIT` 通过
- [ ] `M10ExternalKuaishouS01IT` P0 **100%**
- [ ] `CollectExecutionService` EXTERNAL 路由单元覆盖
- [ ] MockWebServer HTTP 契约与 `api.json` 一致
- [ ] M8 配置 1 条 + 任务 run → `oa_external_work` ≥1
- [ ] M7 hot-works API 返回采集行（`is_external=1`）
- [ ] `mvn verify`（oa-module 范围）无失败
- [ ] Gate 报告：`docs/delivery/gates/GATE-EXT-P0-报告-{YYYYMMDD}.md`
- [ ] MASTER §17.3 更新为 🟡/✅

### 5.2 跨阶段回归

| 阶段 | 必绿 |
|------|------|
| P0+ | Channel-A P0 冒烟（`M10ApiCollectorExecChannelAStubIT`） |
| P1+ | P0 快手 IT |
| P2+ | P0 + P1 IT |
| P3+ | 全 EXTERNAL IT + M7 走查 6 页 |

### 5.3 测试类规划

| IT 类 | 阶段 | 覆盖 |
|-------|------|------|
| `M10ExternalAdapterSkeletonIT` | P0 | Adapter bean · 未路由 dataType → 1500 |
| `M10ExternalKuaishouS01IT` | P0 | user-videos 落库 |
| `M10ExternalWechatMpS01IT` | P1 | search + article |
| `M10ExternalDouyinS01IT` | P2 | profile + videos |
| `M10ExternalWechatVideoS01IT` | P3 | 全类型 |
| `M10ExternalCollectConfigValidationIT` | P0 | collect_config_id 校验 1501/1504 |

---

## 6. 用户下一步审批清单

| # | 待批项 | 影响 | 状态 |
|---|--------|------|------|
| 1 | **GATE-EXT-P0 范围**：仅快手 user-videos + 三表 schema + `oa_tenant_collector_credential` | 启动 P0 开发 | ✅ 2026-07-08 |
| 2 | **Q2** `collect_config_id` vs 复用 `account_id` 存外部 ID | Flyway V136 | ✅ **`collect_config_id`** |
| 3 | **P0 字段映射表** §4.4 各列是否足够 M7 | 避免返工 | ✅ 2026-07-08（含 `comment_count` · `work_url` 分享链接 · UK · `account_id`→`oa_external_account.id`） |
| 4 | ~~**公众号 operator 凭账号** 配置位置（M8 租户级 vs 任务级）~~ | P1 设计 | ✅ **租户级** · `oa_tenant_collector_credential`（2026-07-08） |
| 5 | **collector 仓库** P2/P3 路由 owner 与 timeline | P2/P3 排期 | 待批 |

---

## 7. 文件范围（P0 实现时）

| 允许修改 | 禁止 |
|----------|------|
| `service/collect/external/**` | `unify-collector-api/**` |
| `CollectExecutionService.java`（EXTERNAL 分支） | Channel-A Adapter 行为变更 |
| `db/migration/V136*` | M4 bind 流程 |
| `M10External*IT.java` | 全量 PRD 重写 |
| `docs/engineering/API-M10*` 增量 | |

---

*下一步 Slice 会话*：**M10-EXT-P0-S-01** — V136 + `ExternalCollectorAdapter` + 快手 IT（@ ADR-052 + 本文 § P0）。
