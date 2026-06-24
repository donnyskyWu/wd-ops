# ADR-047: M4 平台账号凭证 SSOT 与 Collector 映射

> **状态**: 已采纳 | **日期**: 2026-06-22
> **决策人**: 产品 + 开发团队
> **关联**: [ADR-014](./ADR-014-M8-配置管理数据模型.md) · [ADR-045](./ADR-045-M10-奥创多账号与设备同步.md) · [ADR-025](./ADR-025-M4-公众号字段扩展与续费认证.md) · [M10-三通道采集计划](../delivery/M10-三通道采集计划.md)

## 背景

M4 平台账号管理已具备档案、续费认证、Cookie 等字段；M8 `oa_collect_config`（`scope=INTERNAL`）因 M4 早期不完整而承载平台类内部采集凭证与运营字段，形成 **双 SSOT** 技术债。

`unify-collector-api`（Channel-A · api.json）已纳入仓库。实证结论：

- **微信公众号 MVP** 100% 依赖 **Cookie + Token**（`mp.weixin.qq.com` 后台抓取）；现有 `follower-list`、`articles-with-data`、`publish-list` 等端点 **不读 AppSecret**
- AppSecret（OpenAPI `access_token`）理论上可覆盖粉丝 openid 分页、用户资料、datacube 等 **子集**，**不能**替代 Cookie 驱动的 MVP 端点
- Collector 内部使用 `acc_{platform}_{hash}` 作为运行时账号 ID，与业务侧 `oa_account.id` 需显式映射

产品确认三项决策（本 ADR 编码）：

| 编号 | 问题 | 决策 |
|------|------|------|
| **Q1** | AppSecret 在 M4 的定位 | **仅档案可选**：`app_id` / `app_secret_encrypted` 保留于 `oa_account` 供档案/续费/Phase 2；**MVP 采集 bind 不读 AppSecret** |
| **Q2** | 快手 `field_mapping` 落点 | **账号级**：`oa_account.field_mapping`（跟凭证一起走 bind），**非** `oa_collect_task` |
| **Q3** | M8 平台 INTERNAL Tab 退役节奏 | **硬切**：与 M4「采集」Tab 同 Gate 上线；**无**长期只读并行；`V111` 一次性迁移 |

### 已确认原则（沿用）

- **M4 = SSOT** 平台账号 + 凭证（Channel-A）
- **全表 `tenant_id` 隔离**；业务优先，加密可分阶段强化
- **消除 M8 平台类内部采集重复配置**；M8 保留阈值/AI/外部源/订单库及 **ADR-045 例外**（奥创/企微/个微）
- **调度归 M10**：`collect_frequency` → `oa_collect_task`；连接诊断 → bind 表

---

## 决策

### 1. 凭证 SSOT — `oa_account`

平台类内部采集（Channel-A）凭证 **统一落 M4**，不再经 `oa_collect_config`(INTERNAL) 写入。

#### 1.1 通用与平台扩展字段

| 字段 | 平台 | 说明 | MVP 采集 |
|------|------|------|----------|
| `cookie_encrypted` | 全平台 | AES-256；已有（V8） | ✅ 主凭证 |
| `mp_token_encrypted` | `WECHAT_OFFICIAL` | 公众号后台 Token；自 M8 `auth_token_encrypted` 迁入 | ✅ |
| `auth_token_encrypted` | `KUAISHOU` 等 | 平台专用 Token（如 cp.kuaishou.com `kuaishou.web.cp.api_st`） | ✅ |
| `field_mapping` | `KUAISHOU` | JSON/TEXT；**账号级**字段映射 | ✅ bind 时携带 |
| `app_id` | 可选 | 明文；档案/续费/OpenAPI Phase 2 | ❌ MVP bind 不读 |
| `app_secret_encrypted` | 可选 | AES-256；档案/续费/OpenAPI Phase 2 | ❌ MVP bind 不读 |

> **Q1**：表单可展示 `app_id` / `app_secret`，须标注「不参与 MVP 采集」；避免运营误以为配 Secret 即可采全量数据。

#### 1.2 分平台 MVP 凭证集（对接 unify-collector-api）

| 平台 | collector `platform` | MVP 凭证 | 登录方式 |
|------|---------------------|----------|----------|
| **WECHAT_OFFICIAL** | `wechat_mp` | **cookie + mp_token** | Cookie 导入 / QR 扫码 |
| **WECHAT_VIDEO** | `wechat_channels` | **cookie** | 扫码 / Cookie 导入 |
| **DOUYIN** | `douyin` | **cookie**（含 `sessionid`） | import-cookie |
| **KUAISHOU** | `kuaishou` | **cookie + auth_token** + **field_mapping** | import-cookie（cp 域） |
| **XIAOHONGSHU** | `xiaohongshu` | **cookie** 或扫码 | `/auth/login` / `/auth/cookie` |
| **BILIBILI** | `bilibili` | **cookie** | 官方 QR API |
| **WECHAT_SERVICE** | — | — | collector 501，**Out of MVP** |

**例外（不经 M4 bind）**：企微 → `oa_wework_account` + `WeComAdapter`；个微/奥创 → `oa_aocreate_api` + `AochuangAdapter`（ADR-045）。

#### 1.3 运营字段新归属（自 M8 INTERNAL 迁出）

| 原 `oa_collect_config` 字段 | 新归属 |
|-----------------------------|--------|
| `collect_frequency` | `oa_collect_task.frequency` / cron |
| `field_mapping` | **`oa_account`**（快手等，Q2） |
| `is_live` | `oa_collect_task` 任务范围或账号扩展布尔 |
| `conn_status` | **`oa_collector_account_bind.conn_status`** |
| `app_id` / `app_secret` / `cookie` / `auth_token` | **`oa_account`** |
| `account_id` FK 重复行 | **消除**（账号即主体） |

**UX**：M4 账号详情新增 **「采集」Tab** — 凭证编辑、扫码绑定、测试连接、bind 状态、跳转 M10 建任务；**不**在此重复 frequency CRUD。

---

### 2. Collector 双 ID 映射 — `oa_collector_account_bind`

| 概念 | 说明 |
|------|------|
| **业务 ID** | `oa_account.id`（M4 SSOT） |
| **采集 ID** | collector `acc_{platform}_{hash}`（`unify-collector-api` 内部） |

#### 2.1 Schema 摘要（Flyway **V110**）

| 列 | 类型 | 约束 | 说明 |
|----|------|------|------|
| `id` | BIGINT | PK | |
| `tenant_id` | BIGINT | NOT NULL | 租户隔离 |
| `oa_account_id` | BIGINT | FK → `oa_account.id` | 业务账号 |
| `collector_account_id` | VARCHAR(64) | NOT NULL | collector 返回的 `acc_xxx` |
| `platform_type` | VARCHAR(64) | NOT NULL | 冗余便于查询 |
| `bind_status` | VARCHAR(32) | `@InDict` | BOUND / PENDING / FAILED |
| `conn_status` | VARCHAR(32) | `@InDict dict_conn_status` | 最近探活 |
| `last_bind_at` | TIMESTAMP | NULL | |
| `last_health_check_at` | TIMESTAMP | NULL | |
| 审计列 | | | creator / create_time / updater / update_time / deleted |

**唯一键**：

- `UK (tenant_id, oa_account_id)` — 一 OA 账号至多一条 bind（Q2 账号级映射）
- `UK (tenant_id, collector_account_id)` — 一 collector 账号仅绑一 OA 行

#### 2.2 绑定流程

```
M4 保存凭证（oa_account）
        │
        ▼
POST collector /api/v1/accounts/import  (platform + credential JSON)
        │
        ▼
返回 acc_xxx → 写入 oa_collector_account_bind
        │
        ▼
M10 UnifiedCollectorAdapter: oa_account_id → bind → 所有调用 account_id=acc_xxx
```

凭证变更 → 更新 collector credential（或 delete + re-import）；bind 行保留 `oa_account_id` 不变。

---

### 3. 三通道对齐（M10-三通道采集计划）

| 通道 | Adapter | 凭证 SSOT | 映射表 |
|------|---------|-----------|--------|
| **A · api.json** | `UnifiedCollectorAdapter` | **`oa_account`** | **`oa_collector_account_bind`** |
| **B · 奥创** | `AochuangAdapter` | `oa_aocreate_api` + `oa_aocreate_account[]` | 无（ADR-045） |
| **C · 企微** | `WeComAdapter` | `oa_wework_account` | 无 |

共享：`oa_collect_task` / `oa_collect_log` / 重试 / 幂等框架。

**实现切片对齐**（规划，非本 ADR 实现）：

| 通道 | 切片 | 本 ADR 嵌入点 |
|------|------|---------------|
| A | M10-API-S-01~S-04 | V110 bind 表 · V111 迁移 · M4 采集 Tab · Adapter |
| B | M10-AO-S-00~S-07 | 不经 bind；凭证仍在 M8 个微 Tab |
| C | M10-WECOM-S-01~S-04 | 不经 bind |

---

### 4. M8 INTERNAL 平台 Tab 退役 — 硬切时间线

| 阶段 | 动作 |
|------|------|
| **ADR-047（本切片）** | 文档 Gate；标记 INTERNAL 平台 Tab **Deprecated** |
| **Spec 增量（Slice 1）** | ADR-014 v3；PRD-M4/M8/M10 delta |
| **V110** | `oa_collector_account_bind` + `oa_account` 凭证扩展列 |
| **M10-API-S-02~S-04** | Adapter + M4 采集 Tab + 任务壳对接 |
| **V111（同 Gate 硬切）** | `oa_collect_config`(INTERNAL) → `oa_account` 凭证 + bind **一次性迁移**；隐藏 M8 平台 INTERNAL Tab；API 停止新写入 |
| **V112+（可选清理）** | INTERNAL 行 soft-delete；冻结 `oa_collect_config` 平台冗余列 |

> **Q3**：**禁止**长期 M8+M4 双写只读并行；迁移窗口仅允许只读旧 Tab + 一次性脚本，与 M4「采集」Tab 同 Gate 上线即切。

**M8 保留**（不受影响）：外部采集、关键词、阈值、AI、订单库、**个微奥创 Tab**、**企微 Tab**。

---

### 5. AppSecret Phase 2 边界

| 阶段 | 范围 |
|------|------|
| **MVP（本 ADR）** | 公众号采集 **仅 Cookie + mp_token**；bind / `UnifiedCollectorAdapter` **不读取** `app_secret_encrypted` |
| **M4 档案** | `app_id` / `app_secret_encrypted` **可选保留** — 续费认证、档案备查、与 ADR-025 公众号扩展并存 |
| **Phase 2（子 ADR）** | OpenAPI **混合通道**：粉丝 openid 全量分页（`user/get`）、datacube 等 AppSecret 可覆盖子集；**仍不能**替代 Cookie 驱动的图文互动/发表记录/竞品链；collector 需新模块；须单独 Gate |

**与 M2 发布边界**：M2 微信公众号发布（`draft/add`、`freepublish/submit`）凭证策略见 P2-M2-PUB-00；采集 bind 与发布 access_token **职责分离**，可复用 M4 同账号 `app_id`/`app_secret` 档案字段，但 MVP 采集不依赖之。

---

## 6. 对 ADR-014 的修订指针

[ADR-014](./ADR-014-M8-配置管理数据模型.md) v2.1 将 `config_internal_account` 映射至 `oa_collect_config`(INTERNAL) + `account_id` FK。**本 ADR 撤销该映射对平台类内部采集的 SSOT 含义**：

| ADR-014 条目 | 修订 |
|--------------|------|
| `config_internal_account` → `oa_collect_config`(INTERNAL) | **Deprecated（平台类）**；凭证 SSOT 改 **M4 `oa_account`** |
| `oa_collect_config.account_id` FK | 迁移后 **消除** 平台 INTERNAL 重复行；不再新增 |
| 企微 / 个微 Tab 拆分 | **不变**；仍走 `oa_wework_account` / `oa_aocreate_api`（ADR-045） |
| 外部采集 / 阈值 / AI | **不变**；仍用 `oa_collect_config` 其他 scope |

完整 Spec 修订见 Slice 1（ADR-014 v3 正文）。

---

## 7. 迁移切片引用

| Flyway | 名称（建议） | 范围 | 依赖 |
|--------|--------------|------|------|
| **V110** | `collector_account_bind.sql` | `oa_collector_account_bind` 表；`oa_account` 增 `mp_token_encrypted`、`auth_token_encrypted`、`field_mapping`、可选 `app_id`/`app_secret_encrypted` | Spec 增量 |
| **V111** | `migrate_internal_collect_to_m4.sql` | INTERNAL 行凭证 → `oa_account`；生成 bind；隐藏 M8 Tab 数据基线 | V110 + M10-API-S-04 |
| **V112+** | 清理（可选） | INTERNAL soft-delete；列冻结 | V111 生产验证 |

> **本切片（Slice 0）仅 ADR，不实现 Flyway / 代码。**

---

## 8. 实现切片（规划索引）

| Slice | 目标 | ADR 决策 |
|-------|------|----------|
| **M4-COL-S-00** | ADR-047（本文） | Q1~Q3 文档 Gate |
| **M4-COL-S-01** | Spec 增量 | ADR-014 v3 |
| **M10-API-S-01** | V110 + 实体/Mapper | bind 表 · 账号扩展列 |
| **M10-API-S-02** | `UnifiedCollectorAdapter` + bind API | 凭证模板 · 不读 AppSecret |
| **M10-API-S-03** | M4 采集 Tab UI | Cookie/Token · 扫码 · 测试连接 |
| **M10-API-S-04** | 任务壳对接 | accountId → bind → collector |
| **M8-COL-S-01** | V111 迁移 + Tab 硬切 | Q3 同 Gate |

---

## 9. 铁律遵守

- 全表 `tenant_id` 隔离
- 凭证 AES-256 + 响应脱敏
- 枚举 `@InDict`；跨租户 1504
- `oa_account_id` 绑定用选择器 + 后端存在性校验（1501）
- MVP bind **禁止**读取 `app_secret_encrypted` 组装 collector credential

---

## 10. 后果

| 正面 | 负面 |
|------|------|
| 消除 M4/M8 双 SSOT，运营单点维护凭证 | V111 迁移需生产数据一次性脚本与回滚预案 |
| Channel-A 映射清晰，Adapter 可测 | Cookie 时效（2–7 天）需 M10 探活 + relogin 告警 |
| AppSecret 档案与采集职责分离，降低误配 | Phase 2 OpenAPI 混合需额外 ADR 与 collector 模块 |
| 与 ADR-045 奥创/企微例外并存，边界明确 | M4 账号详情 UI 复杂度上升（采集 Tab） |

---

## 11. 待后续 Spec 同步（非本 ADR 实现范围）

- `PRD-M4` 凭证 + 采集 Tab · `PRD-M8` 删除 CFG-002~007 平台类 · `PRD-M10` Channel-A source
- `API-M4` / `API-M8` / `API-M10` 端点 delta
- `STATE-M8` INTERNAL 平台 Tab Deprecated 标记
- `SLICES-M10-API` 与 `M10-三通道采集计划.md` §4 对齐更新
