# Ops × Football 数据归属与松耦合集成分析

> **日期**：2026-07-04  
> **状态**：Accepted（产品确认 2026-07-04，见 §产品确认）  
> **关联**：[ADR-047](../adr/ADR-047-Football-Ops平台集成决策.md) · [ADR-049-Ops与Football数据归属与松耦合集成](../adr/ADR-049-Ops与Football数据归属与松耦合集成.md) · [INTEGRATION-PROGRESS](./INTEGRATION-PROGRESS.md)

---

## 执行摘要

本仓库证据确认 **ADR-047 松耦合模型**：单库 `wd`，Football 拥有 M9 身份层（`system_*`），Ops 拥有业务数据（`oa_*`）及 **遗留并行** `sys_*` 层（字典/参数/日志）。**不存在 `oa_dict_*` 表**——业务字典位于 **`sys_dict_type` / `sys_dict_data`**，与 Football 的 **`system_dict_type` / `system_dict_data`** 分离。

**关键结论**：`sys_dict_*` 为 Ops SSOT（尽管前缀为 `sys_`），不与 Football 字典合并；`sys_param` 为 Ops 专属；M9 身份表废弃；订单同步待定；独立 UI 保留为开发/QA 入口。

---

## Q1 — Ops 字典与系统参数：是否迁移至 Ops 独立维护？

### 证据

| 层级 | Ops（oa-server Flyway） | Football（import 脚本） |
|------|-------------------------|-------------------------|
| **业务字典** | `sys_dict_type`、`sys_dict_data` — V1 基线 + 30+ 迁移 seed `dict_*` 类型 | `system_dict_type`、`system_dict_data` — 平台字典（`system_user_sex`、`trade_order_status`、`pay_*` 等） |
| **字典读 API** | `GET /admin-api/oa/dict/data` → `DictController` → `sys_dict_*` | `GET /admin-api/system/dict-data/*`（Football system-server） |
| **字典管理 CRUD** | `SystemDictController`，路径 `/admin-api/oa/system/dict` **及** `/admin-api/system/dict` | Football 内置字典管理 |
| **系统参数** | `sys_param`（V52）+ `ParamController` → `/admin-api/oa/system/param/*` | `infra_config`（infra-server；seed 中有字典类型 `infra_config_type`，**表不在** `import-football-system-tables.sql` 中） |

ADR-047 §5.2 明确 **保留** 于 oa-server：

- 业务字典 `/admin-api/oa/dict/**`
- 系统参数 `/admin-api/oa/system/param`

菜单 seed（`scripts/integration-config/seed-oa-system-menu.sql`）：

- **6137 字典配置** → `#/ops/system-dict`，权限 `oa:dict:admin-list`，组件 `ops/system/DictManage`
- **6141 系统参数** → `#/ops/system-param`，权限 `oa:param:list`

M11 Spec（`docs/engineering/API-M11-字典管理.md`）SSOT：`sys_dict_type.type` / `sys_dict_data.dict_value`。

### 建议

| 资产 | 归属 | 迁移路径 | 保留在 Football `system_*` |
|------|------|----------|------------------------------|
| **`dict_*` 业务枚举**（author、platform、plan、collect 等） | **Ops 专属** — 保留 `sys_dict_*` | S3 可选重命名为 `oa_dict_*` 以清晰化；**禁止**合并入 `system_dict_*`（schema 差异：`dict_value` vs `value`，`ENABLED` vs `0/1`） | 仅平台/infra/trade/member 字典类型 |
| **字典读（`DictController`）** | Ops | 不变；Football 壳经 Gateway → oa-server | — |
| **字典管理（`SystemDictController`）** | Ops | 保留 `oa:*` 权限；对 Ops 类型隐藏 Football 重复字典管理 | Football 管理 `system_*`、`infra_*`、`trade_*` 类型 |
| **`sys_param`** | **Ops 专属** | 不迁移至 `infra_config`，除非产品明确决策 | `infra_config` 用于 Football/infra 运行时键 |
| **M9 遗留字典类型**（`dict_user_status` 等） | 随 M9 页面废弃 | UserSelect 迁移至 `system_users` 后移除 | — |

**关键决策**：将 **`sys_dict_*` 视为 Ops SSOT**（尽管前缀为 `sys_`）——非 Football 字典。共存于 `wd` 安全，因类型命名空间不同（`dict_author_type` vs `trade_order_status`）。

---

## Q2 — Ops 独立 UI：独立部署/测试入口

### 证据

| 模式 | 前端 | 后端 | 鉴权 |
|------|------|------|------|
| **独立（原始）** | `ops-platform-ui-vue` → **:3000**（`vite.config.ts`） | oa-server → **:8080**（`application.yml`） | `VITE_API_TOKEN=dev-token-oa-admin`，proxy `/admin-api` → 8080 |
| **集成（当前目标）** | `football-front` → **:5777** | Gateway **:48080** → oa-server **:48094**（Nacos profile） | Football 登录 + Bearer token |

`ops-platform-ui-vue/README.md` 仍文档化独立流程（8080 + 3000）。**5173 未使用**（Football Vite 使用 5777）。

`mount-ops-all.py` 批量复制 Ops UI 至 `football-front/apps/web-ele/src/views/ops/**` — 集成路径，非独立模式。

ADR-047 §5.1：M9 页面（`/system-user`、`/system-role`、`/system-tenant`）在 Ops UI **已废弃**；seed 已排除，但 `ops-platform-ui-vue/src/router/index.ts` 仍保留 `/system-user` 路由。

### 建议 — 最小独立入口（非阻塞）

1. **保持 `ops-platform-ui-vue` 可运行**，对接 oa-server profile `dev` only（8080）——无需 Nacos/Gateway/Football。
2. **独立模式菜单子集**：全部业务模块 + **系统管理(OA)** 块（字典、参数、日志、消息）；**排除** M9 用户/角色/租户页面（路由与 seed 对齐）。
3. **开发鉴权**：继续 Dev Token（ADR-003）；独立模式走 `sys_user` token 路径；集成模式走 `FootballAuthProvider` → `system_users` + 可选 `sys_permission` 合并。
4. **不阻塞 Football 集成**：独立模式保留为 **开发/QA 入口**；生产路径仍为 5777 + 48080（S5「下线独立 Ops 入口」，见 `INTEGRATION-PROGRESS.md` §3）。

可选：新增 `.env.standalone`，`VITE_API_BASE_URL=/admin-api/oa` 指向 8080；集成模式使用 Gateway URL——无需代码合并。

---

## Q3 — `sys_*` 中哪些适合 Ops 独立维护？

### Ops Flyway `sys_*` 表（完整列表）

| 表 | 创建于 | 建议 |
|----|--------|------|
| `sys_tenant` | V1 | **废弃** — Football `system_tenant` SSOT（ADR-047 D3） |
| `sys_user`、`sys_user_token`、`sys_role`、`sys_user_role` | V1/V12 | **废弃** — 停止写入；只读兼容直至选择器迁移完成 |
| `sys_permission`、`sys_role_permission` | V12 | **废弃** — `oa:*` 现已在 `system_menu.permission` |
| `sys_dict_type`、`sys_dict_data` | V1 | **Ops 专属** — 业务字典 SSOT |
| `sys_audit_log` | V1 | **废弃** — 已被 `sys_operation_log` / Football 日志取代 |
| `sys_dept` | V41 | **过渡** — 对齐 `system_dept` 或只读 |
| `sys_param` | V52 | **Ops 专属** |
| `sys_operation_log`、`sys_login_log`、`sys_message` | V52 | **Ops 专属**（ADR-047 §5.2） |
| `sys_metadata_entity`、`sys_metadata_field` | V96 | **Ops 专属**（M8） |
| `sys_notification_event` | V88 | **Ops 专属**（M9 扩展，非身份） |

### Football `system_*`（`import-football-system-tables.sql` 子集，已导入 `wd`）

`system_dept`、`system_dict_*`、`system_login_log`、`system_mail_*`、`system_menu`、`system_notice`、`system_notify_*`、`system_oauth2_*`、`system_operate_log`、`system_post`、`system_role`、`system_role_menu`、`system_sms_*`、`system_social_*`、`system_tenant`、`system_tenant_package`、`system_user_post`、`system_user_role`、`system_users`，以及 `author_channel_sales`、`football_demo*`。

脚本头注释：**「Does NOT touch sys_* / oa_* tables.」**

### 归属矩阵

| 类别 | Football 专属 | Ops 专属 | 共享只读 | 废弃 |
|------|---------------|----------|----------|------|
| 身份（user/role/tenant/menu/oauth） | `system_users`、`system_role`、`system_menu`、`system_oauth2_*` | — | — | `sys_user`、`sys_role`、`sys_tenant`、`sys_permission` |
| 业务字典 | `system_dict_*`（platform/trade/member 类型） | `sys_dict_*`（`dict_*` 类型） | — | — |
| 参数 | `infra_config`（infra-server） | `sys_param` | — | — |
| 日志/消息 | `system_login_log`、`system_operate_log`、`system_notify_*` | `sys_login_log`、`sys_operation_log`、`sys_message` | — | `sys_audit_log` |
| 元数据 | — | `sys_metadata_*` | — | — |
| 业务数据 | `author_channel_sales`、`member_*`、`trade_*`（不在 Ops flyway） | 全部 `oa_*` | `system_users.id` 被 BIGINT 字段引用 | — |

---

## Q4 — Ops ↔ Football 业务关联

### 关联表

| 实体 | Ops 表 | Football 表 | 耦合度 | 集成建议 |
|------|--------|-------------|--------|----------|
| **公众号/平台账号** | `oa_account`（+ V86 微信字段）、`oa_personal_wechat_account`、`oa_wework_account`、`oa_collector_account_bind` | 仓库 flyway 中无 | **无** | **保持分离** — Ops SSOT；Football 无 MP 账号模型 |
| **粉丝** | `oa_follower_daily`、`oa_wechat_mp_follower`、`oa_douyin_follower`、`oa_platform_account_fan_group` | `member_user_follow`（Football JAR mappers，`tmp_patch*`，C 端 SaaS 粉丝） | **语义层** — 不同域 | **保持分离**；产品若需关联 MP openid ↔ member 可建 **同步表** |
| **订单** | `oa_order_attribution`（归因配置/结果展示）；`oa_order` seed 仅历史/演示 | `trade_order` / `pay_order`（同库 `wd`，Football trade 模块 SSOT） | **松耦合 — 已确认** | **同库只读跨查**：oa-server read-only Mapper / SQL view 直读 `trade_*`、`pay_*`；Ops **不写订单、不做 ETL**；列表与归因页消费只读层 |
| **方案/计划** | `oa_content_plan`、`oa_content_plan_step`、`oa_sop_template`、`oa_task` | 无（Football「方案」= trade/product scheme，非内容计划） | **无** | **保持分离** |
| **作者 ↔ 用户** | `oa_author.user_id`、`oa_ops_anchor_rel.ops_user_id` → **当前 `sys_user.id`** | `system_users.id`（管理端）、`member_user`（C 端） | **中等** — ID 空间不一致风险 | **FK 语义迁移至 `system_users.id`**；经 Football system API 或 DB 只读；**`member_user` ≠ Ops author** |
| **作者 ↔ 渠道销售** | `oa_author` | `author_channel_sales.author_id` | **潜在** — 同名，未验证关联 | 关联前建显式映射表 `oa_author_football_channel_id` |
| **公司/手机/SIM/实名人 (M4)** | `oa_company`、`oa_phone`、`oa_sim_card`、`oa_realname`、`oa_realname_intermediary` | 无 | **无** | **保持分离** — Ops 专属资产台账 |
| **IP 组/内容/采集** | `oa_ip_group*`、`oa_content`、`oa_collect_*`、各平台采集表 | 无 | **无** | **保持分离** |

### Ops API 锚点

- 平台账号：M4 API 于 `oa_account` / internal-account 页面；微信公众号：`oa_wechat_mp_*`（V112/V116）
- 计划：`GET /admin-api/oa/plan/list`（M2）
- 订单归因：`GET /admin-api/oa/order-attribution/*`（M3）
- 作者：M1 API 于 `oa_author`
- UserSelect 仍调用 **`/oa/system/user/list`** → **`sys_user`**（`UserSelect.vue`、`UserController.java`）—— **集成债务**

### 鉴权桥接（现有）

`FootballAuthProvider` 经 OAuth2 token 读取 `system_users`；用户名匹配遗留 `sys_user` 时可选合并 `sys_permission` — 证据表明 **过渡性双身份**，非统一 schema。

---

## 关键决策点

1. **确认 `sys_dict_*` 保持 Ops SSOT** — 不合并至 Football `system_dict_*`。
2. **确认 `sys_param` vs `infra_config` 边界** — Ops 业务调参 vs Football/infra 运行时。
3. **S3 延期范围**：表重命名（`sys_dict_*` → `oa_dict_*`？）vs 仅文档化保留现名。
4. **用户 ID 迁移**：`oa_author.user_id`、`oa_ip_group_member.user_id`、`ops_user_id` 等字段从 `sys_user.id` → **`system_users.id`**；UserSelect 应调用 **`/admin-api/system/user/simple-list`**（只读）或 oa-server 代理。
5. ~~**订单集成**~~ → **已确认**：同库只读 `trade_order`/`pay_order`，无 ETL（见 §产品确认）。
6. **`author_channel_sales`**：**延期** — `author_id` 与 `oa_author.id` 映射待产品后续决策。
7. **独立 UI 下线（S5）**：3000+8080 仅作 dev，或长期保留 QA 入口。

---

## 产品确认（2026-07-04）

| # | 主题 | 确认结论 |
|---|------|----------|
| 1 | **用户** | `UserSelect` 等统一 **`system_users`**；`oa_author` **保持独立**，用户引用迁移非本期 |
| 2 | **订单** | Ops **只读** `trade_order`/`pay_order`（同库 cross-query / view / Mapper）；**无 ETL、无写入** |
| 3 | **粉丝** | **不合并** `oa_*` ↔ `member_*` |
| 4 | **平台账号 / 计划 / M4** | **保持分离**，试用后优化 |
| 5 | **Ops-only `sys_*` 页面** | **保留在 Ops 壳** — `ops/system/*`（6137–6141）+ `ops/config/MetadataManage`（6165）；M9 user/role/tenant **仅 Football** |

---

## 主要证据路径

| 路径 | 说明 |
|------|------|
| `docs/adr/ADR-047-Football-Ops平台集成决策.md` | D2 单库、D3 M9→Football、§5.2 保留 OA 字典/参数 |
| `ops-platform-server/.../db/migration/V1__baseline.sql` | `sys_dict_*`、遗留 M9 表 |
| `ops-platform-server/.../db/migration/V52__m9_param_log_message.sql` | `sys_param` |
| `DictController.java` vs `SystemDictController.java` | 字典读/管分离 |
| `scripts/integration-config/import-football-system-tables.sql` | Football `system_*` 子集 |
| `scripts/integration-config/import-football-pay-tables.sql` | Football `pay_all_order` / `pay_gold_order`（P2b） |
| `scripts/integration-config/seed-oa-system-menu.sql` | 菜单 6137、6141 |
| `ops-platform-ui-vue/README.md` + `vite.config.ts` | 3000 → 8080 |
| `docs/delivery/INTEGRATION-PROGRESS.md` | S3 待启动、S5 独立入口下线 |
| `scripts/mount-ops-all.py` | Football 集成复制路径 |

---

## P2b — 订单只读跨查（设计 spike + 最小实现，2026-07-04）

### 表归属发现（grep `football-backend-saas`）

| ADR-049 名称 | 仓库内是否存在 | Football 实际 SSOT | 模块 |
|--------------|----------------|-------------------|------|
| `trade_order` | **否** — 无 `football-module-trade`；`ruoyi-vue-pro.sql` 仅含 `trade_order_*` **字典**，无 DDL | — | — |
| `pay_order`（ruoyi 商城支付单） | **否** — 无对应 DO/DDL | — | — |
| — | **是** | **`pay_all_order`** | `football-module-pay` · `AllOrderDO` |
| — | **是** | **`pay_gold_order`** | `football-module-pay` · `GoldOrderDO`（鱼币充值，非内容订单） |

**结论**：ADR-049 中 `trade_order`/`pay_order` 为 ruoyi-vue-pro 模板用语；本 Football 部署的**业务订单 SSOT 为 `pay_all_order`**。Ops 只读层应 cross-query 该表（及按需 `pay_gold_order`），**禁止** ETL 至 `oa_order`。

### `pay_all_order` 关键列（来自 `AllOrderDO`）

| 列 | 类型/含义 | Ops 映射用途 |
|----|-----------|--------------|
| `id` | PK | 与 `oa_order_attribution.order_id` 未来 join 键（待产品确认 FK 语义） |
| `tenant_id` | 租户 | **必过滤**（同 `TenantContextHolder`） |
| `order_no` | 订单号 | 列表展示，对齐 `OrderAttributionVO.orderNo` |
| `user_id` | C 端会员 | 买家维度 |
| `author_id` | Football 作者 | 与 `oa_author` 映射 **Deferred**（≠ `oa_author.id` 未验证） |
| `amount` / `pay_amount` | 订单/实付金额 | 对齐 `revenue` / 财务 ROI |
| `status` | 0待支付 1成功 2失败 3取消 | 列表默认 `status=1` 可选 |
| `order_type` | 0方案 1订阅 2专栏 | 订单类型 |
| `pay_time` / `create_time` | 时间 | 日期范围筛选 |
| `deleted` | 逻辑删 | **必过滤 `deleted=0`** |
| 分成列 | `author_divide`, `partner_divide`, … | 财务明细 **Out of Scope** P2b |

`pay_gold_order` 列：`id`, `order_no`, `user_id`, `amount`, `pay_amount`, `status`, `pay_time`, `tenant_id`, `deleted` 等（`GoldOrderDO`）。

### 只读 Mapper 设计（oa-server，同库 `wd`）

- **模式**：与 `FootballOAuth2TokenMapper` 相同 — 同数据源 `@Select` 注解 Mapper，**无 `@DS` 切换**（单库）。
- **实现类**：`FootballPayAllOrderReadMapper` — **仅 SELECT**；禁止 `BaseMapper`/`insert`/`update`/`delete`。
- **API**：`GET /admin-api/oa/football-order/list` — 分页读 `pay_all_order`。
- **权限**：`@PreAuthorize("hasAnyAuthority('oa:order-attribution:list','oa:roi:list')")`（菜单 6142 / 6147 已有 perm）。
- **写入禁令**：Ops 不得 INSERT/UPDATE/DELETE `pay_*`；`oa_order` / `oa_order_attribution` 仍为 Ops 归因结果表（seed/演示），**不做 Football 订单 ETL**。

### 与 `oa_order_attribution` 展示契约（待产品）

| 字段 | 当前 Ops | P2b 只读层 | 缺口 |
|------|----------|------------|------|
| `order_id` | → `oa_order.id`（seed） | → `pay_all_order.id` | FK 语义切换需 ADR 补丁 |
| `order_no` | 经 `oa_order` join | 直读 `pay_all_order.order_no` | — |
| `author_id` | `oa_author.id` | `pay_all_order.author_id`（Football 作者） | **映射表 Deferred** |
| `account_id` / `ip_group_id` / `ops_user_id` | Ops 归因维度 | Football 订单**无**对应列 | 归因规则需离线/job 或手工配置，**非 cross-query** |
| `revenue` | Ops 表字段 | 建议 `pay_amount`（实付） | 产品确认 |

### 实现文件（oa-server）

| 文件 | 职责 |
|------|------|
| `dal/mysql/football/FootballPayAllOrderReadMapper.java` | SELECT `pay_all_order` |
| `dal/dataobject/football/FootballPayAllOrderReadDO.java` | 只读投影 |
| `service/football/FootballOrderReadServiceImpl.java` | 租户 + 日期过滤 |
| `controller/football/FootballOrderReadController.java` | REST + `oa:*` perm |

### Curl 探针

**表导入**（2026-07-04，`wd` 原无 `pay_*` 表）：

```bash
python scripts/integration-config/apply-import-football-pay-tables.py
# → pay_all_order ×2, pay_gold_order ×1（integration seed）
mysql ... -e "SHOW TABLES LIKE 'pay_%';"
```

**集成 smoke**（Gateway :48080，需 Football 登录 token 或具备 `oa:order-attribution:list` / `oa:roi:list` 的 Bearer；`dev-token-oa-admin` 无上述 perm → code=403）：

```bash
# 1) POST /admin-api/system/auth/login → accessToken
curl -s "http://localhost:48080/admin-api/oa/football-order/list?startDate=2026-01-01&endDate=2026-07-04&pageNum=1&pageSize=5" \
  -H "Authorization: Bearer <accessToken>" \
  -H "X-Tenant-Id: 1" -H "tenant-id: 1"
# 2026-07-04 实测: code=0, total=2, list[0].sourceTable=pay_all_order
```

**Standalone dev**（:8080 + `dev-token-oa-admin`）同路径可用，但 dev-token 须先具备 6142/6147 对应 `sys_permission`（或临时去掉 `@PreAuthorize` 仅限 IT）。

**前置**：`wd` 库须存在 `pay_all_order` / `pay_gold_order`；DDL 来源 `football-module-pay` DO（无 ruoyi-vue-pro.sql）；见 `scripts/integration-config/import-football-pay-tables.sql`。
