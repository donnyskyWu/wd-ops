# Ops × Football 多库复用可行性分析

> **日期**：2026-07-04（2026-07-05 实测修订；**2026-07-05 数据原则重分析**）  
> **状态**：**Executing S0**（ADR-050 Accepted · ADR-051 修订 · localhost TRUNCATE ✅ · V131 就绪）· **S0 本地四库已就绪**  
> **执行计划 SSOT**：[OPS-FOOTBALL-MULTI-DB-EXECUTION-PLAN.md](./OPS-FOOTBALL-MULTI-DB-EXECUTION-PLAN.md)  
> **关联**：[ADR-047](../adr/ADR-047-Football-Ops平台集成决策.md) · [ADR-049](../adr/ADR-049-Ops与Football数据归属与松耦合集成.md) · [ADR-050](../adr/ADR-050-Ops与Football多库复用总纲.md) · [ADR-051](../adr/ADR-051-Ops与Football多库复用-作者域.md) · [OPS-FOOTBALL-DATA-OWNERSHIP-ANALYSIS](./OPS-FOOTBALL-DATA-OWNERSHIP-ANALYSIS.md) · [INTEGRATION-PROGRESS](./INTEGRATION-PROGRESS.md)

---

## 0. 用户意图摘要

从 ADR-047/049 的 **单库 `wd` 共存** 模型，转向 **Football 原生分库**（`localhost:3306` 下 `shenyu-member` / `shenyu-mp` / `shenyu-pay` / `shenyu-system`），在以下域 **复用 Football 表与能力**：

- 作者、公众号账号、字典、登录日志、操作日志、消息管理

约束与偏好：

1. **DB 按业务块拆分**（非全部合并进单一 `wd`）
2. **`sys_param` 语义保留**，UI 改名为 **运营参数配置**，菜单迁入 **配置管理**
3. **公众号**：Football `mp_account` 为主表；Ops 建 **关联/扩展表**；查询/分析优先 Ops 表；创建/编辑 **双写**
4. **其余 Ops 业务表** → `wd`（localhost）
5. **全部连接** → `localhost:3306`，对齐 Football 各模块 `application-local.yaml` 分库模式
6. SQL 导出位于 `docs/sql/`（member / mp / pay / system）

---

## A. 可行性结论：**部分可行，需 ADR 修订 + 分阶段实施**

| 维度 | 评估 |
|------|------|
| **技术可行** | ✅ Football 各微服务已用 `spring.datasource.dynamic`；oa-server 可引入同类多数据源 |
| **业务可行** | ⚠️ **部分** — 微信公号、作者可对齐；字典/日志/消息需 schema/API 适配；`oa_account` 覆盖多平台，不能整表替换 `mp_account` |
| **集成成本** | 🔴 **高** — 与已签 ADR-049 多处冲突；80+ Flyway、P2b 只读订单、集成脚本均假设单库 `wd` |
| **风险可控性** | 双写 + 跨库只读可接受，但需明确 **无跨库事务**、对账与回滚策略 |

**一句话**：方向合理（回归 Football 生产分库），但 **不能一次性全量切换**；建议 **S0 本地四库 → S1 公众号 PoC → S2 作者/字典 → S3 日志消息 → S4 cutover**，并先修订 ADR-047 D2、ADR-049 D2/D4/D5/D7。

### A.0 本地库连通性（2026-07-05 实测）

| 项 | 结果 |
|----|------|
| 连接 | `localhost:3306`，凭证 **`root` / `root`**（与 `tmp_jar_extract` local 配置一致） |
| 库清单 | `wd`、`shenyu-member`、`shenyu-mp`、`shenyu-pay`、`shenyu-system`（另有 `football-saas`、`nacos` 等，非本期范围） |
| 表数量 | wd **136** · member **63** · mp **22** · pay **31** · system **59** |
| 数据状态 | **四库均已导入 schema + seed**（非 schema-only）；见下表 |

| 库 | 代表表 | 实测行数 |
|----|--------|----------|
| `wd` | `oa_author` / `oa_account` / `sys_dict_*` / `sys_param` / `sys_*_log` / `sys_message` | 8 / 20 / 94+360 / 11 / 3+706 / 92 |
| `shenyu-member` | `author_user` 及关联 | **35**（ID 68028–1000008）；`author_performance` 4347 等 |
| `shenyu-mp` | `mp_account` | **187**（156 条有 `bind_author_id`） |
| `shenyu-pay` | `pay_all_order` | **178,006** |
| `shenyu-system` | `system_dict_*` / 日志 / 通知 | 186+907 / 3172+627 / 35 |

---

## 数据原则：配置留 Ops，业务以 Football 为准

> **2026-07-05 用户确认**：wd 内现有业务 seed **均为测试数据，可 TRUNCATE**；Football 四库为业务 SSOT。Ops `wd` 保留 **配置/字典/参数/元数据/大屏与 AI 配置** 等「运营平台能力」，不再为历史 seed 做 8↔35 作者映射或 app_id backfill。

### 原则三条

| # | 原则 | 含义 |
|---|------|------|
| P1 | **配置留 Ops** | `sys_dict_*`（业务 `dict_*`）、`sys_param`、`sys_metadata_*`、`oa_ai_*`、`oa_threshold_config`、SOP/漏斗/指标/大屏 **定义类** 表 — **KEEP**，Flyway seed 保留 |
| P2 | **业务以 Football 为准** | 作者（`author_user`）、微信公号（`mp_account`）、订单（`pay_all_order`）、身份（`system_users`）— **读 Football 分库**；wd 内对应测试行 **可删** |
| P3 | **Ops 独有维度另表扩展** | IP 组、M4 资产链、非微信平台账号 — **schema 留 wd**；测试数据可空，新建时挂 Football ID |

### 硬约束：不改 Football 业务代码与逻辑

> ADR-050 §3.1 · 2026-07-05 用户原则

| 改造面 | 说明 |
|--------|------|
| **Ops 侧（允许）** | `oa-server` 多数据源、`@DS` Adapter、写时 sync；`wd` Flyway 扩展表（`oa_author_ext`、`oa_account_ext` 等） |
| **`football-front` 壳层（允许）** | Ops 挂载、路由、依赖链接 — **集成层**，非 Football 业务逻辑 |
| **Football 业务微服务（禁止）** | `football-backend-saas/**` 下 member-server、mp-server、pay-server、system-server 的 **业务代码与逻辑** |
| **Gateway（例外/冻结）** | 既有集成基建（路由、超时等）已完成，**不再新增改动** |
| **读 Football** | `oa-server` 只读跨库 `@DS`，或 Feign 调 **既有** Football API — **不得**要求 Football 侧改代码 |

### wd 表分类（136 表 · 2026-07-05 实测）

#### A — 必须保留（配置 / 字典 / 参数 / 元数据 / 模板定义）

| 表 | 说明 |
|----|------|
| `flyway_schema_history` | 迁移历史 |
| `sys_dict_type`, `sys_dict_data` | Ops 业务字典 SSOT（94 type / 360 data） |
| `sys_param` | 运营参数（11 行） |
| `sys_metadata_entity`, `sys_metadata_field` | M8 元数据 |
| `sys_notification_event` | 通知事件定义 |
| `oa_ai_model_config`, `oa_ai_prompt_config` | AI 模型与 Prompt |
| `oa_threshold_config` | 阈值配置（可含 ip_group 维度，配置本身保留） |
| `oa_config_keyword`, `oa_aocreate_api` | 关键词 / 接口配置 |
| `oa_metric` | 指标定义 |
| `oa_perf_template`, `oa_perf_template_item` | 绩效模板 |
| `oa_funnel`, `oa_funnel_step` | 漏斗定义 |
| `oa_custom_query`, `oa_dashboard` | 自定义查询 / 大屏配置 |
| `oa_sop_template`, `oa_sop_node` | SOP 模板（非实例） |
| `oa_typesetting_rule`, `oa_wechat_layout_template`, `oa_layout_style` | 排版 / 布局模板 |

#### B — 测试数据（可 TRUNCATE；新数据走 Football ID 或 Ops 新建）

| 分组 | 表 | 说明 |
|------|-----|------|
| **作者** | `oa_author`, `oa_author_ext` | 8 行 seed + PENDING_MAP **整表可空**；不再 backfill |
| **账号** | `oa_account` | 20 行（微信 8 + 非微信 12）**可全删**；微信改读 `mp_account` |
| **IP 组** | `oa_ip_group`, `oa_ip_group_member`, `oa_ip_group_anchor_rel` | **Football 无等价表**（见 §N.4）；11 行 seed 可删，保留 schema |
| **M4 资产** | `oa_company`, `oa_company_expansion`, `oa_realname`, `oa_realname_intermediary`, `oa_phone`, `oa_sim_card`, `oa_personal_wechat_account`, `oa_wework_account`, `oa_wework_employee`, `oa_account_wechat_video_wework_rel`, `oa_platform_account_fan_group` | 测试资产链 |
| **内容 / 计划 / 任务** | `oa_content`, `oa_production_content`, `oa_content_plan`, `oa_content_plan_competition`, `oa_content_plan_step`, `oa_task`, `oa_sop_review`, `oa_review_record`, `oa_content_publish_record`, `oa_content_data_import`, `oa_knowledge_base` | 业务实例；清空后新建用 `author_user_id` + `mp_account_id` |
| **订单归因** | `oa_order`, `oa_order_attribution` | Ops 侧归因测试数据 |
| **运营关系** | `oa_ops_anchor_rel` | 测试行 |
| **废弃身份** | `sys_user`, `sys_role`, `sys_role_permission`, `sys_user_role`, `sys_permission`, `sys_dept`, `sys_tenant`, `sys_user_token`, `sys_audit_log` | ADR-049 D4 已废弃；读 `system_users` |
| **wd 内 Football 副本** | `author_user`, `author_channel_sales`, `pay_all_order`, `pay_gold_order`, `system_*`（28 表） | 集成期同库副本；**应 DROP 或永久空表**，改 `@DS` 跨库 |
| **Demo** | `football_demo01_contact`, `football_demo02_category`, `football_demo03_*`, `oa_demo_item` | 演示数据 |

#### C — Ops 独有分析 / 采集（空表启动或后续 ETL）

| 分组 | 表 | 说明 |
|------|-----|------|
| **采集** | `oa_collect_config`, `oa_collect_task`, `oa_collect_log`, `oa_collector_account_bind`, `oa_aocreate_account` | 配置可留少量模板（C 偏 A）；日志/任务实例 **TRUNCATE** |
| **时序 / 统计** | `oa_content_daily`, `oa_follower_daily`, `oa_account_status_log`, `oa_account_cost`, `oa_personal_wechat_daily_stats`, `oa_wework_daily_stats` | 分析事实表；空启动，采集/ETL 后写入 |
| **多平台作品** | `oa_douyin_follower`, `oa_douyin_video`, `oa_kuaishou_video`, `oa_wechat_video_work`, `oa_xiaohongshu_note`, `oa_wechat_mp_article`, `oa_wechat_mp_follower`, `oa_wechat_official_cert_renewal` | 采集结果；账号 ID 将来指向 `oa_account` 或 ext |
| **绩效实例** | `oa_perf_record`, `oa_perf_item_record` | 跑批结果 |
| **其他** | `oa_external_work`, `oa_home_alert`, `oa_layout_import_job`, `oa_aochuang_*`, `oa_private_domain_conversion_bridge` | 实例 / 桥接 |
| **日志 / 消息（过渡）** | `sys_login_log`, `sys_operation_log`, `sys_message` | 测试数据可删；S3 改读 Football `system_*` |

### N.1 修订 — 作者策略（测试数据可弃 → **大幅简化**）

| 原方案（ADR-051 / V130） | **修订后** |
|--------------------------|------------|
| `oa_author_ext.id` = `oa_author.id` 锚点，保留 wd FK | **`oa_author_ext` PK = `author_user_id`**；`oa_author` **可 TRUNCATE + 停写 + S2 DROP** |
| 8 行 seed `PENDING_MAP` 人工映射 | **删除** — 无需映射 |
| 双写 `oa_author` + ext + member | **仅** member 写 `author_user` + wd 写 ext（`ip_group_id` 等） |
| `oa_content.author_id` 仍指 legacy id | **语义改为 `author_user.id`**（Flyway `V131` 改 COMMENT + 清空测试行） |

**`oa_author_ext` 瘦身为 Ops 扩展面**（非完整作者表）：

| 列 | 说明 |
|----|------|
| `author_user_id` | **PK** → `shenyu-member.author_user.id` |
| `tenant_id` | 租户 |
| `ip_group_id` | → `oa_ip_group.id`（**Ops 独有**） |
| `author_type` | 可选 · `dict_author_type` |
| `primary_mp_account_id` | 可选 · → `mp_account.id`（取代 `primary_account_id` → `oa_account`） |
| audit / deleted | 标准 |

**新建作者流程**（无 `oa_author`）：

```
1. @DS("member") INSERT author_user → author_user_id
2. @DS("master") INSERT oa_author_ext (author_user_id, ip_group_id, …)
3. wd 内 oa_content / oa_task / oa_order_attribution 的 author_id 直接存 author_user_id
```

### N.2 修订 — 公众号策略

| 平台 | SSOT | Ops 侧 |
|------|------|--------|
| **微信公众号** | `shenyu-mp.mp_account`（187 行） | `oa_account_ext(mp_account_id, ip_group_id, company_id, …)` **仅扩展**；**不再**维护 wd `oa_account` 微信行 |
| **抖音 / 快手 / 视频号 / 小红书 / 企微** | 无 Football 表 | `oa_account` **Ops SSOT**；现有 12 行测试数据 **TRUNCATE**，空表新建 |

- **丢弃**：wd 内 8 条 `WECHAT_OFFICIAL` seed + 全部非微信测试账号（用户确认）。
- **列表 UI**：微信 — `@DS("mp")` 读 `mp_account` + 内存 join `oa_account_ext`；非微信 — 仍读 `oa_account`。
- **创建微信公号**：member/mp 写 `author_user`（若需）+ mp 写 `mp_account` + wd 写 `oa_account_ext`；**不写** `oa_account`。

### N.3 修订 — 内容 / 计划 / 任务

- **KEEP schema**（Flyway 不动表结构，仅 `author_id` COMMENT 与可选 `account_id` 语义文档化）。
- **TRUNCATE** 全部 B 组实例表（content 43 / task 101 / plan 等）。
- 新建内容：`author_id` = `author_user.id`；微信 `account_id` 建议存 **`mp_account.id`**（或 ext 映射层统一解析）；非微信仍用 `oa_account.id`。
- **无需**迁移历史 `1002`（sys_user.id）混用 — 测试数据已弃。

### N.4 IP 组 — **仅 wd，Football 无等价**

| 证据 | 结论 |
|------|------|
| `shenyu-member` 无 `ip_group` 表 | IP 组为 **Ops 运营组织维度**，非 Football 域 |
| `author_user.parent_id` | 作者层级（如 105113 下 21 子作者），**≠** IP 组 |
| `oa_ip_group` 11 行 seed | 测试数据；**TRUNCATE 后** 二选一：① 空表 + UI 新建；② 最小 skeleton（1 大组 + 2 小组，不含作者绑定） |

IP 组与 Football 作者关系：**运行时**通过 `oa_author_ext.ip_group_id` 关联，不在 Football 写回。

### N.5 对 ADR-049 / ADR-051 / V130 的影响 — **可简化**

| 文档 | 原内容 | 修订 |
|------|--------|------|
| **ADR-049** §已确认 #1 | `oa_author` 保持独立 | **局部修订**：作者身份 SSOT = `author_user`；Ops 只保留 ext |
| **ADR-049** §已确认 #4 | 平台账号保持分离 | **修订**：微信 = `mp_account` + ext；非微信仍 Ops |
| **ADR-049** D2/D5/D7 | 字典/订单/页面归属 | **不变**（配置仍 Ops；订单跨库只读） |
| **ADR-051** D3 | 保留 `oa_author.id` FK 锚点 | **Supersede** → PK 改 `author_user_id`；允许 DROP `oa_author` |
| **ADR-051** D4 | S4 才停写 `oa_author` | **提前至 S1** — TRUNCATE 后即停写 |
| **ADR-051** §5 | 8 行 PENDING_MAP | **删除** — 不再需要 |
| **V130** | `id`=oa_author.id + backfill 8 行 | **需 V131 修订**：ALTER ext PK；或新环境跳过 backfill + DROP `oa_author` |
| **V131（建议）** | — | `oa_account_ext` 建表；`author_id` COMMENT；可选 DROP `oa_author` |

### N.6 风险变化（相对 §G）

| 原风险 | 修订后 |
|--------|--------|
| R5 作者 ID 8↔35 映射 | **消除** — 不迁移历史 |
| R3 `oa_account.id` 全网 FK | **降低** — 测试数据清空；微信改 `mp_account_id`；非微信新建 id |
| R1 双写漂移（历史 backfill） | **降低** — 无 backfill，仅新建双写 |
| 新风险 | IP 组空表时作者/阈值无组 — **最小 seed 1 组** 或 UI 强制先建组 |

### N.7 修订分阶段计划 S0–S4

#### S0 — 基建 + 数据清理（1 周）

- [x] 四库 + seed 就绪（§A.0）
- [x] **用户确认后** 执行 §N.8 TRUNCATE 脚本（wd B/C 实例表）— **2026-07-05 localhost ✅**
- [ ] oa-server `dev-local-multidb` profile + `@DS` smoke
- [x] **V131**：修订 `oa_author_ext`（PK=`author_user_id`）+ `oa_account_ext` 建表 — **2026-07-05 local 已应用**
- [x] ADR-050 签字 + **ADR-051 修订附录**（测试数据可弃）— **2026-07-05 Accepted**
- [ ] DROP wd 内 `system_*` / `pay_*` / `author_user` 副本表（或文档标记 deprecated）

#### S1 — 作者 + 微信公号（2 周，合并原 S1+S2 核心）

- [ ] `AuthorService`：`@DS("member")` 列表 + ext join `ip_group_id`
- [ ] 新建作者：member + ext（**无 oa_author**）
- [ ] `PlatformAccountSyncService`：微信读/写 `mp_account` + `oa_account_ext`
- [ ] `oa_content.author_id` 代码层按 `author_user_id` 解析
- [ ] UAT：作者列表（35 Football 作者 + ext 可选）、微信公号列表（187 mp）

#### S2 — 非微信平台 + IP 组（1–2 周）

- [ ] 非微信 `oa_account` CRUD（空表起步）
- [ ] IP 组最小 seed 或空表 + 创建向导
- [ ] M4 资产链与新账号 ID 对齐
- [ ] 字典双轨（平台 system / 业务 wd）Adapter

#### S3 — 日志 / 消息 / 采集（1–2 周）

- [ ] 登录/操作日志读 Football + Adapter
- [ ] 消息分菜单（Ops 广播 vs Football 站内信）
- [ ] 采集任务绑定 `mp_account_id` / `oa_account.id`

#### S4 — Cutover

- [ ] DROP `oa_author`（若仍存）
- [ ] 全环境分库配置 + Gate 重签
- [ ] 对账：ext 覆盖率（有 Football 作者但无 ip_group 的提示）

### N.8 TRUNCATE / KEEP 脚本清单（**仅文档，勿自动执行**）

> 执行前：**备份 wd**；确认 Flyway 已跑完；用户书面确认。

```sql
-- ========== KEEP（勿 TRUNCATE）==========
-- sys_dict_type, sys_dict_data, sys_param
-- sys_metadata_entity, sys_metadata_field, sys_notification_event
-- oa_ai_model_config, oa_ai_prompt_config, oa_threshold_config
-- oa_config_keyword, oa_aocreate_api
-- oa_metric, oa_perf_template, oa_perf_template_item
-- oa_funnel, oa_funnel_step, oa_custom_query, oa_dashboard
-- oa_sop_template, oa_sop_node
-- oa_typesetting_rule, oa_wechat_layout_template, oa_layout_style
-- flyway_schema_history

-- ========== TRUNCATE — 业务测试数据（B 组）==========
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE oa_order_attribution;
TRUNCATE TABLE oa_order;
TRUNCATE TABLE oa_content;
TRUNCATE TABLE oa_production_content;
TRUNCATE TABLE oa_content_plan_step;
TRUNCATE TABLE oa_content_plan_competition;
TRUNCATE TABLE oa_content_plan;
TRUNCATE TABLE oa_task;
TRUNCATE TABLE oa_sop_review;
TRUNCATE TABLE oa_review_record;
TRUNCATE TABLE oa_content_publish_record;
TRUNCATE TABLE oa_content_data_import;
TRUNCATE TABLE oa_knowledge_base;
TRUNCATE TABLE oa_author_ext;
TRUNCATE TABLE oa_author;
TRUNCATE TABLE oa_account;
TRUNCATE TABLE oa_ip_group_member;
TRUNCATE TABLE oa_ip_group_anchor_rel;
TRUNCATE TABLE oa_ip_group;
TRUNCATE TABLE oa_ops_anchor_rel;
-- M4 资产
TRUNCATE TABLE oa_account_wechat_video_wework_rel;
TRUNCATE TABLE oa_platform_account_fan_group;
TRUNCATE TABLE oa_company_expansion;
TRUNCATE TABLE oa_company;
TRUNCATE TABLE oa_realname_intermediary;
TRUNCATE TABLE oa_realname;
TRUNCATE TABLE oa_phone;
TRUNCATE TABLE oa_sim_card;
TRUNCATE TABLE oa_personal_wechat_account;
TRUNCATE TABLE oa_wework_employee;
TRUNCATE TABLE oa_wework_account;
-- 废弃 sys_*
TRUNCATE TABLE sys_user_role;
TRUNCATE TABLE sys_role_permission;
TRUNCATE TABLE sys_user_token;
TRUNCATE TABLE sys_user;
TRUNCATE TABLE sys_role;
TRUNCATE TABLE sys_permission;
TRUNCATE TABLE sys_dept;
TRUNCATE TABLE sys_audit_log;
-- wd 内 Football 副本（若存在数据）
TRUNCATE TABLE author_channel_sales;
TRUNCATE TABLE author_user;
TRUNCATE TABLE pay_gold_order;
TRUNCATE TABLE pay_all_order;
-- Demo
TRUNCATE TABLE football_demo01_contact;
TRUNCATE TABLE football_demo02_category;
TRUNCATE TABLE football_demo03_student;
TRUNCATE TABLE football_demo03_grade;
TRUNCATE TABLE football_demo03_course;
TRUNCATE TABLE oa_demo_item;
SET FOREIGN_KEY_CHECKS = 1;

-- ========== TRUNCATE — 采集 / 分析实例（C 组）==========
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE oa_collect_log;
TRUNCATE TABLE oa_collect_task;
TRUNCATE TABLE oa_collector_account_bind;
TRUNCATE TABLE oa_content_daily;
TRUNCATE TABLE oa_follower_daily;
TRUNCATE TABLE oa_account_status_log;
TRUNCATE TABLE oa_account_cost;
TRUNCATE TABLE oa_douyin_follower;
TRUNCATE TABLE oa_douyin_video;
TRUNCATE TABLE oa_kuaishou_video;
TRUNCATE TABLE oa_wechat_video_work;
TRUNCATE TABLE oa_xiaohongshu_note;
TRUNCATE TABLE oa_wechat_mp_article;
TRUNCATE TABLE oa_wechat_mp_follower;
TRUNCATE TABLE oa_perf_item_record;
TRUNCATE TABLE oa_perf_record;
TRUNCATE TABLE oa_external_work;
TRUNCATE TABLE oa_home_alert;
TRUNCATE TABLE sys_login_log;
TRUNCATE TABLE sys_operation_log;
TRUNCATE TABLE sys_message;
SET FOREIGN_KEY_CHECKS = 1;

-- ========== 可选：最小 IP 组 skeleton（TRUNCATE oa_ip_group 之后）==========
-- INSERT INTO oa_ip_group (id, tenant_id, group_name, group_type, parent_id, status, creator, updater)
-- VALUES (9000, 1, '默认大组', 1, NULL, 1, 'reset', 'reset'),
--        (9001, 1, '默认小组', 2, 9000, 1, 'reset', 'reset');
```

---

## B. 分域归属矩阵

| 域 | Football SSOT（目标库） | Ops 现状（`wd`） | 新方向建议 | 与 ADR-049 关系 |
|----|-------------------------|------------------|------------|-----------------|
| **作者** | `author_user`（shenyu-member） | `oa_author` | **✅ 已采纳 Option A**：`author_user` SSOT + `oa_author_ext`（Ops 扩展 + `author_user_id`）；`oa_author` 过渡保留 | ⚠️ 局部修订 ADR-049 — 见 [ADR-051](../adr/ADR-051-Ops与Football多库复用-作者域.md) |
| **公众号（微信）** | `mp_account`（shenyu-mp） | `oa_account`（`WECHAT_OFFICIAL` + V86 扩展） | **主表 mp_account + 扩展 `oa_account_ext`**；抖音/企微等仍纯 Ops | ❌ 冲突 — ADR-049：平台账号保持分离 |
| **跨平台账号（非微信）** | 无 | `oa_account`（DOUYIN/WEWORK/…）+ M4 资产链 | **继续 Ops SSOT**（`wd`） | ✅ 一致 |
| **字典** | `system_dict_*`（shenyu-system） | `sys_dict_*`（30+ 迁移 seed `dict_*`） | **双轨**：平台/infra 读 Football；**Ops 业务 `dict_*` 仍保留 `wd`** | ❌ 冲突 — ADR-049 D2：`sys_dict_*` = Ops SSOT |
| **登录/操作日志** | `system_login_log` / `system_operate_log` | `sys_login_log` / `sys_operation_log` | UI 改读 Football（跨库 Mapper 或 Feign system-server）；Ops 侧停写 | ❌ 冲突 — ADR-049 D7 保留 Ops 维护页 |
| **消息** | `system_notify_*` | `sys_message` | **语义不同** — Football 站内信模板 vs Ops 运营广播；分场景，不宜简单合并 | ⚠️ 部分冲突 |
| **运营参数** | `infra_config`（infra-server） | `sys_param` | **保留 `sys_param` in `wd`**；菜单改名「运营参数配置」，`parent_id` 6105→6110 | ✅ 一致（ADR-049 D3） |
| **订单** | `pay_all_order`（shenyu-pay） | P2b 同库只读 Mapper | 改为 **跨库只读** `@DS("pay")` 或 Feign pay-server | ⚠️ 实现变更，原则（只读、无 ETL）可保留 |
| **计划/内容/IP组/M4/采集** | 无 | 全部 `oa_*` | **保持 `wd` Ops SSOT** | ✅ 一致 |
| **身份 M9** | `system_users` 等 | 废弃 `sys_user` | 不变 | ✅ 一致 |

### B.1 SQL 导出表清单（`docs/sql/`）

| Schema | 表数量 | 代表表 |
|--------|--------|--------|
| **shenyu-member** | 63 | `author_user`, `member_user`, `member_user_follow`, `author_channel_sales`, `author_apply`… |
| **shenyu-mp** | 22 | `mp_account`, `mp_material`, `mp_message`, `mp_menu`, `mp_account_fans`… |
| **shenyu-pay** | 31 | `pay_all_order`, `pay_gold_order`, `finance_author_account`, `finance_transaction_detail`… |
| **shenyu-system** | **59** | `system_dict_*`, `system_login_log`, `system_operate_log`, `system_notify_*`, `system_users`, `system_menu`, `system_oauth2_*`, `infra_config`, `infra_*`, `app_*`, `system_tenant*`… |

> **导出特征（四库一致）**：Navicat **结构-only**（无 `INSERT`、无 `CREATE DATABASE`/`USE`）；导入前需先 `CREATE DATABASE`，本地表为空，须另补 seed（见 §M M2）。

#### shenyu-mp（22 表）

`ding_setting`, `mp_account`, `mp_account_attention`, `mp_account_fans`, `mp_account_group`, `mp_account_read`, `mp_account_user_bind`, `mp_auth`, `mp_auto_reply`, `mp_click_logs`, `mp_material`, `mp_menu`, `mp_message`, `mp_message_template`, `mp_mini_user`, `mp_other_even_logs`, `mp_pay_config_log`, `mp_push_template_fail_config`, `mp_tag`, `mp_template_config`, `mp_template_push_logs`, `mp_user`

#### shenyu-member — 作者/会员相关（节选）

`author_user`, `author_user_account`, `author_apply`, `author_config`, `author_performance`, `member_user`, `member_user_follow`, `kf_author`, `author_channel_sales`（member 域）

#### shenyu-pay — 订单/财务（节选）

`pay_all_order`, `pay_gold_order`, `pay_refund_order`, `finance_author_account`, `finance_author_split`, `finance_withdraw_record`

#### shenyu-system（59 表，2026-07-04 重导）

| 分组 | 代表表 | 复用目标 |
|------|--------|----------|
| 字典 | `system_dict_type`, `system_dict_data` | ✅ 平台字典 |
| 登录/操作日志 | `system_login_log`, `system_operate_log` | ✅ 有表；字段模型与 Ops 不同 → Adapter |
| 消息/通知 | `system_notify_*`, `system_notice*`, `system_mail_*`, `system_sms_*` | ⚠️ 非 `sys_message` 等价 |
| 用户/角色/菜单 | `system_users`, `system_role`, `system_menu`, `system_dept`… | ✅ 身份 SSOT |
| OAuth | `system_oauth2_*` | Token 校验 |
| 运营参数（Football） | `infra_config` | ⚠️ ≠ Ops `sys_param` |
| 租户 / App / Infra | `system_tenant*`, `app_*`, `infra_*` | Football 平台扩展 |

相比 `scripts/integration-config/import-football-system-tables.sql`：用户导出 **schema 更全**（含 infra/app/租户），但 **无 seed**；集成脚本面向单库 `wd` 且含 INSERT。

### B.1.1 Ops ↔ Football system 映射（复用目标）

| Ops（`wd` Flyway） | Football（`shenyu-system`） | 复用结论 |
|--------------------|----------------------------|----------|
| `sys_dict_*` | `system_dict_*` | **双轨**：平台读 Football；业务 `dict_*` 留 `wd`。列差异：`dict_value`↔`value`，`ENABLED`↔`0/1`；Football 字典无 `tenant_id` |
| `sys_login_log` | `system_login_log` | 表存在；Football 有 `log_type`/`trace_id`/`result` tinyint；Ops 为 `status` VARCHAR + `message` → **Adapter** |
| `sys_operation_log` | `system_operate_log` | 表存在；Ops 为 `module/action/level`；Football 为 `type/sub_type/biz_id/action` V2 → **UI/API 适配** |
| `sys_message` | `system_notify_*` + `system_notice*` | **无等价表**：Ops 运营广播/渠道队列 vs Football 站内信/公告 → **分菜单，不合并** |
| `sys_param` | `infra_config` | **不替换**：`sys_param` 继续留 `wd`；菜单改「运营参数配置」（ADR-049 D3） |

### B.2 Ops 侧关键表（`wd` Flyway）

| 表 | 迁移 | 用途 |
|----|------|------|
| `oa_author` | V17 | 运营作者（IP 组、primary_account_id、user_id） |
| `oa_account` | V7/V8/V86… | 跨平台账号聚合 + M4 扩展字段 |
| `sys_dict_type` / `sys_dict_data` | V1 + 30+ seed | 业务字典 `dict_*` |
| `sys_param` | V52 | 运营参数 |
| `sys_login_log` / `sys_operation_log` / `sys_message` | V52 | 日志/消息 |

### B.3 Schema 对比 — 公众号（2026-07-05 实测）

| 指标 | Ops `oa_account` | Football `mp_account` |
|------|------------------|------------------------|
| 行数 | **20** | **187** |
| 平台 | WECHAT_OFFICIAL 8 · WECHAT_VIDEO 5 · DOUYIN 3 · 其他 4 | 全部微信公众号 |
| 绑定作者 | 经 `oa_author.primary_account_id` | **156/187** 有 `bind_author_id` → `author_user.id` |
| 与对端映射 | **当前无 app_id 级对齐**（seed 占位符 vs 生产 app_id） | — |

| 字段语义 | Ops `oa_account` | Football `mp_account` |
|----------|------------------|-------------------------|
| 名称 | `account_name` | `name` |
| AppId | `external_account_id` / V112 `app_id` | `app_id` |
| 状态 | `NORMAL`/`DISABLED` (VARCHAR) | `status` 0/1 (tinyint) |
| Ops 专属 | M4 资产链字段 | **无** |

**结论**：只能对 `WECHAT_OFFICIAL` 双写；实测两库 **尚未建立 app_id 映射**，S1 backfill 为首要工作。

### B.4 Schema 对比 — 作者（2026-07-05 实测）

| | Ops `oa_author`（13 列） | Football `author_user`（62 列） |
|--|--------------------------|----------------------------------|
| 主键 ID 空间 | **9101–93001**（8 行 seed） | **68028–1000008**（35 行生产数据） |
| ID 重叠 | **0**（JOIN `o.id = a.id` = 0） | 同名 `nickname` 重叠亦 **0** |
| 名称 | `author_name` | `nickname` |
| 运营维度 | `ip_group_id`, `author_type`, `primary_account_id` → `oa_account` | **无** |
| 用户关联 | `user_id` → 遗留 `sys_user`（1002–1005） | `user_id` → 管理端用户（常与 id 同值） |
| Football 域 | 无 | `order_ratio`, `fans`, `captive_push_account`, 推送/私域/战绩等 **50+ 列** |
| 下游引用 | `oa_content`/`oa_task`/`oa_order_attribution` 等 **author_id → oa_author.id** | `mp_account.bind_author_id`、`pay_all_order.author_id`（61  distinct，值如 68028） |

**wd 内引用 `author_id` 的表**（information_schema 实测）：

| 表 | 行数（author_id 非空） | author_id 范围 |
|----|------------------------|----------------|
| `oa_content` | 18 | 1002–93001（⚠️ 混用 sys_user.id 与 oa_author.id） |
| `oa_production_content` | 14 | 9101–93001 |
| `oa_task` | 10 | 9101–9105 |
| `oa_order_attribution` | 13 | 9101–93001 |
| `pay_all_order`（wd 副本若存在） | — | Football pay 库用 **author_user.id** |

**代码现状**：`AuthorServiceImpl` / `AuthorController` 仍 **100% 读写 `oa_author`**（`AuthorDO` @TableName `oa_author`），未接 member 库。

#### B.4.1 作者策略决策（Q2 答复）— **✅ 已采纳 Option A；2026-07-05 测试数据可弃 → 见 §N.1 修订**

**是否必须拆成 `author_user` + `oa_author_ext`？** → **是，但 ext 可瘦身为仅 Ops 维度（ip_group_id 等），`oa_author` 可 DROP。**

> **落地状态**：ADR-051 Accepted；V130 已建表（**待 V131 修订 PK**）；Java 骨架 `OaAuthorExtDO` / `OaAuthorExtMapper`。  
> **2026-07-05 原则变更**：8 行 seed / PENDING_MAP **不再需要** — 见 §N.1。

| 选项 | 含义 | 实测结论 |
|------|------|----------|
| **A** `author_user` 主表 + `oa_author_ext` | Football 作者 SSOT；ext 仅存 **Ops 专属字段** + `author_user_id` FK | ✅ **推荐** — 与 pay/mp/member 现有 FK 一致 |
| **A′**（**新默认**）ext PK = `author_user_id`，DROP `oa_author` | 测试数据可弃，wd FK 直接存 `author_user_id` | ✅ **更低成本** — 见 §N.1 |
| **B** `oa_author` SSOT，同步到 `author_user` | Ops 驱动，反向写 Football | ❌ **方向错误** |
| **C** 仅映射表 | 最小改动 | ⚠️ **仅作极短期过渡**；有测试数据可弃时 **不推荐** |

**`oa_author_ext` 职责**（修订后）：

| 字段类 | 示例 | 归属 |
|--------|------|------|
| **PK / 映射** | `author_user_id` BIGINT PK | ext |
| **Ops 业务** | `ip_group_id`, `author_type`, `primary_mp_account_id`, `tenant_id` | ext |
| **作者身份/财务/推送** | `nickname`, `order_ratio`, `fans`, `status`… | **author_user** |

**推荐落地（Option A′）**：

1. **V131** 修订 ext：`author_user_id` PK；TRUNCATE `oa_author` / ext / 下游测试行。
2. 作者列表 API：`@DS("member")` 读 `author_user` + LEFT JOIN ext 补 IP 组。
3. 新建作者：member INSERT + ext INSERT（**无 oa_author**）。
4. `oa_content.author_id` / `oa_task.author_id` 语义 = **`author_user.id`**。
5. 订单/公号：一律 `author_user_id`；IP 组经 ext 反查。

### B.5 Schema 对比 — 字典 / 日志 / 消息（2026-07-05 实测）

| 域 | Ops（wd） | Football（shenyu-system） | 重叠 / 复用结论 |
|----|-----------|---------------------------|-----------------|
| **字典 type** | 94（`dict_*` 前缀，如 `dict_author_type`） | 186（`system_user_sex`, `infra_*`, `common_status`…） | **type 名零重叠**（collation 不同亦无法 JOIN）→ **双轨确认** |
| **字典 data** | 360 行 | 907 行 | 各管各的；Ops 业务枚举 **不可迁** |
| **登录日志** | 3 行 · `status` VARCHAR + `message` | 3172 行 · `result` tinyint + `log_type`/`trace_id` | Football **可支撑 Ops UI**，需 **Adapter** |
| **操作日志** | 706 行 · `module/action/level` | 627 行 · `type/sub_type/biz_id` | 同上，列模型 V2 差异 |
| **消息** | 92 行 · `title/category/channel/receiver` 运营广播 | 35 行 · `system_notify_message` 模板站内信 | **语义不同，不可合并**；Ops 广播留 wd |
| **运营参数** | `sys_param` 11 行 | `infra_config`（Football 平台） | **不替换**（ADR-049 D3） |

---

## C. 公众号双写方案

### C.1 架构草图

```
┌─────────────────────────────────────────────────────────┐
│  Ops UI / PlatformAccountController                      │
└───────────────────────────┬─────────────────────────────┘
                            │ create/update (WECHAT_OFFICIAL)
                            ▼
              ┌─────────────────────────────┐
              │  PlatformAccountSyncService  │  ← 应用层编排（推荐）
              └─────────────┬───────────────┘
         ┌──────────────────┼──────────────────┐
         ▼                  ▼                  ▼
   @DS("mp")           @DS("master"/wd)    可选 Feign mp-server
   mp_account          oa_account_ext
   (shenyu-mp)         (Ops 专属字段)
```

### C.2 扩展表设计（Flyway on `wd`）

```sql
CREATE TABLE oa_account_ext (
  id                  BIGINT PRIMARY KEY,          -- 建议保留历史 oa_account.id
  tenant_id           BIGINT NOT NULL,
  mp_account_id       BIGINT NOT NULL,             -- → shenyu-mp.mp_account.id
  platform_type       VARCHAR(32) NOT NULL DEFAULT 'WECHAT_OFFICIAL',
  -- 从现 oa_account 迁入的 Ops 专属字段
  company_id          BIGINT NULL,
  realname_id         BIGINT NULL,
  intermediary_id     BIGINT NULL,
  ip_group_id         BIGINT NULL,
  phone_id            BIGINT NULL,
  sim_card_id         BIGINT NULL,
  cookie_encrypted    VARCHAR(512) NULL,
  trademark_name      VARCHAR(128) NULL,
  qualification_type  VARCHAR(32) NULL,
  usage_status        VARCHAR(32) NULL,
  admin_user_id       BIGINT NULL,                 -- → system_users.id
  sync_status         VARCHAR(32) NOT NULL DEFAULT 'SYNCED',
  sync_error          VARCHAR(512) NULL,
  creator             VARCHAR(64) DEFAULT 'system',
  create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater             VARCHAR(64) DEFAULT 'system',
  update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted             SMALLINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_ext_mp (tenant_id, mp_account_id),
  KEY idx_ext_ip_group (tenant_id, ip_group_id)
) COMMENT='公众号 Ops 扩展（关联 mp_account）';
```

**查询策略**：

- **列表/分析/报表**：优先 JOIN `oa_account_ext` + 内存补全 `mp_account` 主字段（或 denormalize 常用列到 ext）
- **微信凭证/推送**：读 `mp_account`（Football mp-server 可能并发写）
- **非微信平台**：仍读完整 `oa_account`（无 mp 映射）

### C.3 同步机制对比

| 方案 | 优点 | 缺点 | 建议 |
|------|------|------|------|
| **应用层双写** | 可控、可测；不改 Football Java | 无原子事务；需幂等与对账 | **S1 PoC 首选** |
| **Feign mp-server API** | 走 Football 校验 | 需 mp-server 全栈；API 契约待摸清 | 集成环境可选 |
| **DB Trigger** | — | 跨库不可行 | **禁止** |
| **CDC/定时对账** | 修复漂移 | 延迟 | S3+ 补偿 |

### C.4 一致性风险

| 场景 | 后果 | 缓解 |
|------|------|------|
| mp 写入成功、ext 失败 | 孤儿 `mp_account` | 补偿删除 / `sync_status=PENDING` + 重试 |
| Football 后台直接改 mp | ext 陈旧 | 展示以 mp 为准；定时 reconcile |
| `bind_author_id` 与 `oa_author.id` 不一致 | 订单/采集关联错误 | 先建 `oa_author_ext.author_user_id` |
| 历史 `oa_account.id` 被 50+ 表引用 | 大规模 FK 迁移 | **保留 ext.id = 原 oa_account.id** |

---

## D. oa-server 多数据源技术方案

### D.1 现状

| 项 | 现状 |
|----|------|
| 数据源 | 单库 `jdbc:mysql://101.37.161.136:3306/wd`（`application-dev.yml`） |
| Flyway | 仅 `wd` |
| 跨 Football 读 | `FootballPayAllOrderReadMapper`、`FootballOAuth2TokenMapper` 同库 `@Select` |
| 依赖 | 无 `dynamic-datasource`（pom 仅 mybatis-plus） |

### D.2 Football 参考配置

Football 原生 `application-local.yaml`（member/mp/pay）：

```yaml
spring:
  datasource:
    dynamic:
      primary: master
      datasource:
        master:
          url: jdbc:mysql://192.168.10.47:3306/shenyu-member  # 各模块不同库
        slave:
          lazy: true
          url: jdbc:mysql://192.168.10.47:3306/shenyu-member
```

集成期 overlay（`football-integration-overlay.yml`）曾 **统一改为 `wd`** — 新方向需 **改回分库**。

### D.3 oa-server 目标配置

```yaml
spring:
  datasource:
    dynamic:
      primary: master
      strict: true
      datasource:
        master:    # wd — Flyway + 全部 oa_* + sys_param + sys_dict(业务)
          url: jdbc:mysql://localhost:3306/wd?...
        member:
          url: jdbc:mysql://localhost:3306/shenyu-member?...
        mp:
          url: jdbc:mysql://localhost:3306/shenyu-mp?...
        pay:
          url: jdbc:mysql://localhost:3306/shenyu-pay?...
        system:
          url: jdbc:mysql://localhost:3306/shenyu-system?...
```

**实现要点**：

1. 引入 `dynamic-datasource-spring-boot-starter`（对齐 Football BOM 版本）
2. `@DS("master")` 默认；跨库 Mapper 显式 `@DS("mp")` / `@DS("pay")` 等
3. **禁止跨库 JOIN**；列表页应用层组装
4. Flyway **仅 primary=master（wd）**
5. 双写 = 两阶段 + 可选 `oa_sync_outbox` 补偿表

### D.4 Feign vs 直连 Mapper

| 场景 | 推荐 |
|------|------|
| 公众号 CRUD | 应用层双写 **或** Feign mp-server |
| 字典读（平台类） | Feign system-server **或** `@DS("system")` 只读 |
| 订单 P2b | `@DS("pay")` 只读（替代同库 Mapper） |
| 作者列表 | `@DS("member")` 只读 + `oa_author_ext` 内存 join |
| OAuth2 token 校验 | `@DS("system")` 读 `system_oauth2_*` |

---

## E. 迁移：101.37.161.136 → localhost:3306

| 文件/位置 | 当前 | 目标 |
|-----------|------|------|
| `ops-platform-server/.../application-dev.yml` | `101.37.161.136/wd` | `localhost:3306/wd` + dynamic 子库 |
| `application-dev-local.yml`（可选） | — | 本地凭证 override |
| `scripts/integration-config/football-integration-overlay.yml` | 全指向 `wd` | 分库 URL |
| `scripts/integration-config/system-server-local.yaml` | `wd` | `shenyu-system` |
| `football-module-*/application-local.yaml` | `192.168.10.47/shenyu-*` | `localhost:3306/shenyu-*` |
| `football-module-*/application-local-nacos.yml` | 集成期 `wd` | 改回分库 |
| Nacos DataId | 远程 `wd` | `push-integration-config-to-nacos.ps1` 批量替换 |
| Seed/Import 脚本 | merge 到 `wd` | 四库独立 import + `wd` 仅 Ops |

**本地前置**：

```powershell
# 示例：导入四库（需 MySQL localhost:3306 已启动）
mysql -h localhost -u root -p < docs/sql/shenyu-member.sql
mysql -h localhost -u root -p < docs/sql/shenyu-mp.sql
mysql -h localhost -u root -p < docs/sql/shenyu-pay.sql
mysql -h localhost -u root -p shenyu_system < docs/sql/shenyu-system.sql
# 四库导出均为 schema-only；导入后须补 seed（见 §M M2）
# wd 由 oa-server Flyway 自动迁移
```

---

## F. 变更清单（Inventory）

### F.1 后端（oa-server）

| 项 | 规模 | 说明 |
|----|------|------|
| dynamic-datasource + 5 数据源 | M | 新 profile `dev-local-multidb` |
| `PlatformAccountSyncService` 双写 | L | 仅 WECHAT_OFFICIAL |
| `AuthorService` → member 读 + ext | L | 字段映射层 |
| Dict 读/管适配 | M | 业务 dict 留 wd 或 Adapter |
| 日志/消息 Controller | M | 跨库或 Feign |
| `FootballPayAllOrderReadMapper` | S | `@DS("pay")` |
| Flyway V130+ ext 表 + backfill | M | 历史数据迁移 |
| IT / 多库 test profile | M | docker-compose 四库 |

### F.2 Flyway / Seed

- Flyway **仍仅 `wd`**
- 新增：`V130__oa_author_ext.sql` ✅ · `V131__oa_account_ext.sql`（公众号，待做）
- 跨库 seed 脚本：`scripts/integration-config/seed-multidb-*.py`

### F.3 前端 / 菜单

| 项 | 变更 |
|----|------|
| 菜单 seed | 6141 改名「运营参数配置」；`parent_id` 6105→**6110**（配置管理） |
| `ops-platform-ui-vue` Layout | standalone 侧栏同步 |
| `football-front` seed | `seed-oa-system-menu.sql` 更新 |
| 平台账号页 | 合并 mp 主字段 + Ops 扩展列 |
| 字典页 | API 字段适配（`dict_value` vs `value`） |
| 作者页 | `author_user` 子集 + IP 组来自 ext |

### F.4 集成脚本 / E2E / Gate

- `football-integration-overlay.yml` 分库化
- `start-integration-all.ps1` 前置四库 check — **GATE-MDB 验收唯一启动入口**
- `run-uat-football-e2e.ps1` · `uat-football-ops-login.spec.ts` — **58 路由回归基线**（:5777 登录链）
- `verify-ops-pages*.py`、`uat-*` 多库数据依赖
- **验收约束**（2026-07-05）：GATE-MDB-S0～S4 **必须** Football UI `:5777` 登录操作签收；curl/API probe · standalone `:3000` **非 Gate**（见 [EXECUTION-PLAN §0.6](./OPS-FOOTBALL-MULTI-DB-EXECUTION-PLAN.md#06-验收总则强制-gate) · ADR-050 §3.2）
- 新 Gate：**GATE-INT-S0b**（多库 smoke）
- `INTEGRATION-PROGRESS.md`、`MASTER-EXECUTION-TRACKER.md` 更新

---

## G. 风险 Register

| # | 风险 | 严重度 | 缓解 |
|---|------|--------|------|
| R1 | 双写数据漂移 | 高 | `sync_status`、对账 job、幂等键（tenant+app_id） |
| R2 | 跨库无分布式事务 | 高 | Saga/补偿；禁止 silent partial success |
| R3 | `oa_account.id` 全网 FK | 高 | ext.id 保留原 id；或视图兼容层 |
| R4 | 字典 schema 不兼容 | 中 | 业务 dict 不合并；Adapter 转换 |
| R5 | author ID 8↔35 映射 | ~~中~~ **消除** | 测试数据可弃，不做历史映射（§N.6） |
| R6 | 四库导出无 seed 数据 | ~~低~~ **已缓解** | 2026-07-05 实测四库均有生产级 seed；字典/日志仍需 Adapter |
| R7 | ADR/Gate 失效 | 中 | ADR-050 supersede 后再开发 |
| R8 | Nacos/脚本仍指 wd | 中 | S0 config matrix 统一 |
| R9 | Dev 环境复杂度 | 中 | `docker-compose-multidb.yml` |
| R10 | Football 禁止改 Java | 低 | 全部适配在 oa-server |

---

## H. 分阶段计划

> **2026-07-05**：以 §N.7 修订版为准（测试数据可弃简化路径）。下表保留作对照。

### S0 — 本地多库基建（1–2 周）

- [x] shenyu-system schema 导出（59 表，`docs/sql/shenyu-system.sql`）
- [x] localhost 创建四库并导入 `docs/sql/*.sql`（**2026-07-05 实测：schema + seed 已就绪**）
- [x] 四库 **补 seed 数据**（member 35 作者 / mp 187 公号 / pay 17.8 万订单 / system 字典+日志）
- [ ] **用户确认** §N.8 TRUNCATE wd 测试数据
- [ ] oa-server `dev-local-multidb` profile
- [ ] V131 修订 ext + `oa_account_ext`
- [ ] ADR-050 草案 + ADR-051 修订 + 产品 sign-off
- [ ] Smoke：各 `@DS` 连通；Flyway 仍只跑 wd

### S1 — 作者 + 微信公号（2 周 · 合并原 S1+S2 核心）

- [ ] `AuthorService` member 读 + ext join（**无 oa_author**）
- [ ] `oa_account_ext` + `PlatformAccountSyncService`（微信）
- [ ] `author_id` 语义切换为 `author_user_id`
- [ ] UAT：35 作者 / 187 公号列表

### S2 — 非微信 + IP 组 + 字典（1–2 周）

- [ ] 非微信 `oa_account` 空表 CRUD
- [ ] IP 组最小 seed 或 UI 新建
- [ ] 字典双轨 Adapter

### S3 — 日志 + 消息 + 采集（1–2 周）

- [ ] 登录/操作日志 UI 切 Football 数据源
- [ ] 消息分场景（运营广播 vs 站内信）
- [ ] 采集 bind 改用 `mp_account_id`

### S4 — Cutover

- [ ] DROP `oa_author`（若仍存在）
- [ ] 废弃 wd 内 football 表副本
- [ ] 对账报告 + Gate 重签
- [ ] 更新 INTEGRATION-PROGRESS / MASTER

---

## I. 待用户决策（Open Questions）

| # | 问题 | 选项 |
|---|------|------|
| Q1 | 是否 Supersede ADR-047 D2 + ADR-049 D2/D4/D7？ | 必须签字后再开发 |
| Q2 | 作者：`oa_author` 完全替换 vs ext 映射？ | **✅ Option A**；**2026-07-05 追加 A′**：DROP `oa_author`，ext PK=`author_user_id` — §N.1 |
| Q3 | 字典：Ops `dict_*` 迁入 system vs 双轨？ | **推荐双轨** |
| Q4 | 公众号双写范围？ | 仅 WECHAT → `mp_account` + ext；非微信仍 `oa_account` |
| Q7 | 历史 `oa_account.id` 是否保持不变？ | **测试数据可弃** → 微信用 `mp_account_id`；非微信新建 id |
| Q10 | **是否执行 §N.8 TRUNCATE？** | **✅ 2026-07-05 用户确认；localhost/wd 已执行** |
| Q11 | IP 组清空后：空表 vs 最小 skeleton？ | **✅ 1 大组 + 2 小组（9000–9002）** |

---

## J. 与 ADR-049 矛盾对照

| ADR-049 | 新方向 | 处理 |
|---------|--------|------|
| D2 `sys_dict_*` = Ops SSOT | 复用 Football 字典 | 修订或限定「平台→system，业务→wd」 |
| 平台账号保持分离 | mp 主表 + ext 双写 | 修订 §已确认 #4 |
| `oa_author` 独立 | 复用 `author_user` | 修订 + ext 表 |
| 订单同库只读 | 跨库 `@DS("pay")` | 原则保留 |
| Ops sys_* 页面 | 日志/消息读 Football | UI 留壳，数据源切换 |
| ADR-047 单库 | 分库 | **ADR-050 supersede D2** |

---

## K. ADR-050 大纲（建议新建）

```markdown
# ADR-050：Ops 多库集成与 Football 表复用

| 字段 | 值 |
|------|---|
| 状态 | Proposed |
| Supersedes | ADR-047 §4.3（单库）、ADR-049 D2/D4/D5（部分） |

## 决策
- D1：四库 localhost（member/mp/pay/system）+ wd（Ops 扩展）
- D2：mp_account 主表 + oa_account_ext 双写（仅 WECHAT_OFFICIAL）
- D3：author_user 主表 + oa_author_ext（IP 组/运营维度）
- D4：字典双轨 — system_dict（平台）+ sys_dict（Ops 业务 dict_*）
- D5：sys_param 留 wd；菜单「运营参数配置」归配置管理
- D6：日志/消息 — 读 Football；Ops sys_* 表 deprecate 写入
- D7：订单跨库只读 pay_all_order；禁止 ETL
- D8：无跨库事务；应用层 Saga + 对账

## 后果 / 迁移 / 回滚
（引用本文 §H）
```

---

## L. 主要证据路径

| 路径 | 说明 |
|------|------|
| `docs/sql/shenyu-{member,mp,pay,system}.sql` | 用户导出 schema |
| `docs/adr/ADR-047-Football-Ops平台集成决策.md` | 单库 D2 |
| `docs/adr/ADR-049-Ops与Football数据归属与松耦合集成.md` | 表归属已签决策 |
| `docs/delivery/OPS-FOOTBALL-DATA-OWNERSHIP-ANALYSIS.md` | 四问分析 |
| `ops-platform-server/.../application-dev.yml` | oa 单库配置 |
| `football-backend-saas/football-module-*/application-local.yaml` | Football 分库原生配置 |
| `scripts/integration-config/football-integration-overlay.yml` | 集成期单库 overlay |
| `ops-platform-server/.../db/migration/V7,V17,V52,V86` | oa_account/author/sys_* |
| `ops-platform-server/.../FootballPayAllOrderReadMapper.java` | P2b 同库只读 |
| `scripts/integration-config/seed-oa-system-menu.sql` | 菜单 6105/6110/6141 |

---

## M. 用户待办清单

| # | 事项 | 优先级 | 状态 |
|---|------|--------|------|
| M1 | localhost 创建四库并导入 `docs/sql/*.sql` | P0 | ✅ **2026-07-05**（wd + 四库均存在，136/63/22/31/59 表） |
| M2 | system/member/mp/pay **补 seed** | P0 | ✅ **2026-07-05**（见 §A.0 行数） |
| M3 | ADR-050 签字 + **ADR-051 修订**（测试数据可弃） | P0 | ✅ **2026-07-05** |
| M8 | **用户确认 §N.8 TRUNCATE** 后执行 wd 清理 | P0 | ✅ **2026-07-05 localhost** |
| M9 | Flyway **V131** 修订 ext PK + `oa_account_ext` | P1 | ✅ **2026-07-05 local** |
| M4 | oa-server multidb profile + 连通 smoke | P1 | 待做 |
| M5 | 字典/日志 Adapter 设计（非 1:1 列映射） | P1 | 待做 |
| M6 | 消息分场景决策（Ops 广播 vs Football 站内信） | P1 | 待做 |
| M7 | 确认 `author_channel_sales` 表归属（integration 脚本有、member/system 导出无） | P2 | 待做 |

---

## 变更记录

| 日期 | 作者 | 说明 |
|------|------|------|
| 2026-07-04 | 架构分析 | 初版：多库复用可行性 + 分阶段计划 |
| 2026-07-05 | 重分析 | **localhost:3306 实测**：四库+seed 就绪；§A.0 连通性；§B.3–B.5 行数/重叠；§B.4.1 作者策略 Option A；M1/M2 ✅ |
| 2026-07-05 | 落地 | **作者 Option A 已采纳**：ADR-051 + V130 + OaAuthorExtDO/Mapper 骨架 |
| 2026-07-05 | **数据原则重分析** | 用户确认「配置留 Ops、业务 Football SSOT、测试数据可弃」；新增 **§数据原则** + §N.1–N.8；简化作者/公号/IP 组；修订 S0–S4；TRUNCATE 脚本清单 |
| 2026-07-05 | **S0 执行** | [EXECUTION-PLAN](./OPS-FOOTBALL-MULTI-DB-EXECUTION-PLAN.md) 创建；ADR-050 Accepted；ADR-051 修订；localhost TRUNCATE + IP skeleton；V131 local |
