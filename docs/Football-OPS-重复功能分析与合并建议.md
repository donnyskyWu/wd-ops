# Football SaaS 与 OPS 重复功能分析与合并规整建议

> **数据来源**：《OPS与SAAS合并规划.md》、《神鱼体育SaaS运营数据平台-产品需求与角色权限方案.md》、`docs/sql/shenyu-system.sql`、`docs/delivery/oa-menu-permission-map.csv`、ADR-047/049、PRD-v9.1

---

## 一、重复功能内容分析

### 1.1 高度重复功能（🔴 需优先合并）

| # | 重复功能 | Football SaaS 现状 | OPS 现状 | 合并建议 |
|---|---------|-------------------|---------|---------|
| 1 | **字典管理** | `system_dict_*` 表，`trade_*`/`pay_*`/`system_user_sex` 等平台枚举 | `sys_dict_type`/`sys_dict_data`（`dict_*` 类型），Ops SSOT | 保留 Football system 库字典为 SSOT，Ops 字典迁移 |
| 2 | **日志管理** | `system_login_log`、`system_operate_log` | M9 日志管理 | 合并到 Football system 库，Ops UI 改为读取 |
| 3 | **消息中心** | 短信/邮箱/站内信管理 | M9 消息管理 | 合并到 Football system 库 |
| 4 | **参数配置** | `infra_config`、运行时配置 | `sys_param`（Ops SSOT） | 按配置类型分域：业务参数归 Football，运营参数归 Ops |
| 5 | **定时任务** | `infra_job` | M8/M10 任务调度 | 统一使用 Football infra_job，Ops 采集任务注册为子任务 |

### 1.2 部分重叠功能（🔶 需整合设计）

| # | 重叠功能 | Football SaaS 现状 | OPS 现状 | 整合方向 |
|---|---------|-------------------|---------|---------|
| 6 | **作者管理** | 作者 CRUD、状态、私域/战绩开关、销售数据、渠道销售 | M1 作者分析、人效盘点 | 以 Football `author_user` 为作者 SSOT，Ops 通过扩展表 `oa_author_ext` 补充运营维度 |
| 7 | **公众号管理** | 公号 CRUD、粉丝同步、菜单、自动回复、素材、模板消息（微信 DataCube API） | M1 公众号分析 | 以 Football `mp_account` 为 SSOT，Ops 以 `oa_account_ext` 扩展分析维度 |
| 8 | **企微/个微管理** | 企微配置、活码、统计、获客链接、个微管理 | M4 企微账号/员工/日统计 | Football 管配置/活码/运营，Ops 管资产链（账号-实名-手机-SIM 绑定） |
| 9 | **财务/支付** | 订单 SSOT（`pay_all_order` 178K+ 行）、分账、提现、商户 | M3 订单归因、M5 成本/ROI | Football 管交易流水，Ops 管成本归因与 ROI 分析 |
| 10 | **报表统计** | 5 类报表（平台运营/作者销售/私域/公众号/投入产出） | M6 数据分析（8 张报表、漏斗、自定义查询、大屏） | 按报表用途分域：业务报表归 Football，运营分析报表归 Ops |
| 11 | **首页/仪表盘** | 数据看版（运营数据看板） | M0 首页（IP 组筛选、4 指标卡、待办） | 合并为一个统一仪表盘，支持 IP 组维度筛选 |

### 1.3 OPS 独有功能（❌ 需保留并接入）

| # | 功能模块 | 说明 | 合并策略 |
|---|---------|------|---------|
| 12 | **M2 内容生产 SOP** | DAG 工作流、计划编排、二级审核、AI 辅助、公推模板库、知识库 | 作为新模块接入 Football，复用 Football BPM 引擎 |
| 13 | **M3 绩效核算** | 考核模板、自动算分、订单归因、ROI | 作为新模块接入 Football |
| 14 | **M4 账号资产链** | 公司→实名人→手机→SIM→平台账号，五选择器强绑定 | 作为新模块接入 Football，数据独立存储 |
| 15 | **M7 作品监测** | 外部账号/作品、高/低粉、IP 主题、行业分析 | 作为新模块接入 Football |
| 16 | **M10 数据采集** | 四通道采集、任务调度、质量检查 | 作为独立微服务 `oa-server` 接入 Nacos + Gateway |

### 1.4 Football 独有功能（❌ 保持不变）

| # | 功能模块 | 说明 | 合并策略 |
|---|---------|------|---------|
| 17 | **会员体系** | 64000+ 用户管理、标签、风险计分、黑名单 | 保持不变，Ops 不介入 |
| 18 | **合伙人体系** | 合伙人审核/管理/订单/业绩/结算 | 保持不变，Ops 不介入 |
| 19 | **营销管理** | 代金券、活动管理、推广码、料码统计 | 保持不变，Ops 不介入 |
| 20 | **直播管理** | 直播间 CRUD、开播/回放、数据大屏 | 保持不变，Ops 不介入 |
| 21 | **赛事/玩法管理** | 资讯、热门赛事、竞彩配置、7 种玩法 | 保持不变，Ops 不介入 |
| 22 | **站点管理** | 域名配置、授权号配置、访问模式 | 保持不变，Ops 不介入 |
| 23 | **商城/CRM/ERP/AI/IoT** | 通用业务模块 | 保持不变，评估当前业务必要性 |

---

## 二、合并规整建议

### 2.1 合并原则

1. **Football SaaS 作为统一登录壳与 M9 身份 SSOT**：所有用户/角色/菜单权限统一在 Football system 库管理
2. **数据策略采用 ADR-050 五库拓扑**：配置留 `wd`，业务读 Football 四库（system/member/mp/pay），Ops 以扩展表（`oa_*_ext`）与 `@DS` 跨库适配层衔接
3. **Ops 业务以微服务 `oa-server` 接入 Nacos + Gateway**，前端挂载至 `football-front`（`:5777`），API 统一经 Gateway（`:48080`）
4. **保留双方独有功能**，避免重复建设

### 2.2 合并后功能菜单规划

#### 合并后一级目录（建议 18 个）

| 序号 | 目录 | 路径 | 来源 | 说明 |
|------|------|------|------|------|
| 1 | **工作台** | `/dashboard` | 合并 | Football 数据看版 + OPS M0 首页（统一仪表盘，支持 IP 组筛选） |
| 2 | **会员中心** | `/member` | Football | 会员管理/标签/等级/风险计分/黑名单 |
| 3 | **作者管理** | `/author` | 合并 | Football 作者信息/数据/申请/战绩 + OPS M1 作者分析/人效盘点 |
| 4 | **我的发布** | `/release` | 合并 | Football 方案/套餐/公推/直播总结 + OPS M2 内容生产 SOP/计划/审核 |
| 5 | **合伙人管理** | `/partner` | Football | 合伙人审核/管理/订单/业绩/结算 |
| 6 | **赛事管理** | `/match` | Football | 资讯/热门赛事/竞彩配置/玩法管理 |
| 7 | **营销管理** | `/marketing` | Football | 代金券/活动/推广码/料码统计/推广员统计 |
| 8 | **直播管理** | `/live` | Football | 直播间 CRUD/开播/回放/数据大屏 |
| 9 | **支付管理** | `/pay` | 合并 | Football 订单/充值/退款 + OPS M3 订单归因/M5 成本 ROI |
| 10 | **财务管理** | `/financial` | 合并 | Football 分账/提现/商户 + OPS M5 成本分析 |
| 11 | **数据分析** | `/report` | 合并 | Football 5 类报表 + OPS M6 8 张报表/漏斗/自定义查询/大屏 |
| 12 | **站点管理** | `/station` | Football | 域名配置/授权号配置/访问模式 |
| 13 | **公众号管理** | `/mp` | 合并 | Football 公号 CRUD/粉丝/菜单/素材/模板 + OPS M1 公众号分析 |
| 14 | **企微管理** | `/wecom` | 合并 | Football 企微配置/活码/获客 + OPS M4 企微账号/员工/日统计 |
| 15 | **账号资产** | `/assets` | **OPS 新增** | 公司→实名人→手机→SIM→平台账号，五选择器强绑定（OPS M4 独有） |
| 16 | **绩效核算** | `/performance` | **OPS 新增** | 考核模板、自动算分、订单归因、ROI（OPS M3 独有） |
| 17 | **竞品监测** | `/monitor` | **OPS 新增** | 外部账号/作品、高/低粉、IP 主题、行业分析（OPS M7 独有） |
| 18 | **系统管理** | `/system` | **合并** | 统一身份治理（Football SSOT）+ OPS 字典/参数/日志/消息/采集配置/AI 配置 |

### 2.3 建议移除/评估的模块

| 模块 | 建议 | 理由 |
|------|------|------|
| 商城系统（/mall） | **评估保留** | 当前业务是否需要？如无明确需求可暂时隐藏 |
| CRM 系统（/crm） | **完善权限后保留** | 角色已创建但未分配权限，需完善配置 |
| ERP 系统（/erp） | **评估保留** | 当前业务是否需要？如无明确需求可暂时隐藏 |
| AI 大模型（/ai） | **与 OPS M8 AI 配置整合** | OPS 有 AI 模型/Prompt 配置需求，可统一管理 |
| IoT 物联网（/iot） | **评估保留** | 当前业务是否需要？如无明确需求可暂时隐藏 |

### 2.4 合并优先级与实施顺序

| 阶段 | 任务 | 涉及模块 | 预估工作量 |
|------|------|----------|-----------|
| **Phase 1** | 统一登录壳 + 身份 SSOT 迁移 | 系统管理（M9） | 高 |
| **Phase 2** | 字典/日志/消息/参数合并 | 系统管理/基础设施 | 中 |
| **Phase 3** | 作者/公众号/企微数据整合 | 作者管理、公众号管理、企微管理 | 中 |
| **Phase 4** | 仪表盘合并（Football + OPS M0） | 工作台 | 中 |
| **Phase 5** | OPS 独有功能接入 | 账号资产、绩效核算、竞品监测 | 高 |
| **Phase 6** | 内容生产 SOP 接入（复用 BPM） | 我的发布 | 中 |
| **Phase 7** | 报表统计合并 | 数据分析 | 中 |
| **Phase 8** | 财务/支付/订单归因整合 | 支付管理、财务管理 | 中 |

### 2.5 技术架构建议

```
统一前端壳 football-front (:5777)
├── Football 原生菜单（15 个）
├── OPS 挂载菜单（5 个：账号资产、绩效核算、竞品监测、统一仪表盘合并页、系统管理扩展页）
└── 评估后保留菜单（商城/CRM/ERP/AI/IoT，按业务需要决定是否显示）

统一 API Gateway (:48080)
├── shenyu-system（system-server）→ 身份 SSOT
├── shenyu-member（member-server）→ 作者/会员
├── shenyu-mp（mp-server）→ 公众号
├── shenyu-pay（pay-server）→ 支付/财务
├── oa-server（新增微服务）→ OPS 业务（内容生产/绩效/资产链/采集/竞品）
└── 其他微服务（wecom/match/infra 等）

数据层（ADR-050 五库拓扑）
├── wd 库 → 配置留存（oa_* / sys_dict / sys_param）
├── system 库 → 身份治理 SSOT
├── member 库 → 作者/会员 SSOT
├── mp 库 → 公众号 SSOT
└── pay 库 → 订单/财务 SSOT
```

---

## 三、角色与权限体系分析

> **合并原则（ADR-047 D3/D4 · ADR-049 D4/D7）**：身份 SSOT = Football `system_users` / `system_role` / `system_menu`；Ops 业务权限前缀保留 `oa:*`，写入 `system_menu.permission` + `system_role_menu`；`sys_user` / `sys_role` / `sys_permission` 停止写入，只读过渡。

### 3.1 Football 现有角色清单

**存储**：`system_role`（`shenyu-system` 库）+ `system_role_menu` 菜单授权。

| 角色 ID | 角色名 | code | data_scope | 类型 | 说明 |
|--------|--------|------|------------|------|------|
| 1 | 超级管理员 | `super_admin` | 1（全部） | 内置 | 拥有全部菜单；集成后默认含 Ops `oa:*` 菜单块（6100–6999） |
| 2 | 普通角色 | `common` | 2（指定部门） | 内置 | 芋道默认角色，仅少量示例菜单 |
| 3 | CRM 管理员 | `crm_admin` | 1（全部） | 内置 | CRM 模块专属 |
| 109/111 | 租户管理员 | `tenant_admin` | 1（全部） | 内置 | 按租户自动生成（tenant_id=121/122 等） |
| 155 | 测试数据权限 | `test-dp` | 2（指定部门） | 自定义 | 测试用 |

**Football 权限模型**：

| 维度 | 实现 | 前缀示例 |
|------|------|---------|
| 菜单权限 | `system_menu.permission` → `system_role_menu` | `system:*`、`member:*`、`pay:*`、`mp:*`、`infra:*` |
| 数据范围 | `system_role.data_scope`（1 全部 / 2 指定 / 3 本部门 / 4 本部门及以下 / 5 仅本人） | — |
| 路由守卫 | `football-front` Vben 权限指令，登录后加载 `accessCodes` | 与菜单 permission 一致 |
| Ops 扩展 | S2 seed `system_menu` 6100–6999，`permission` 字段为 `oa:*` | `oa:ip-group:list` 等 |

**Football 业务菜单域**（`import-football-system-tables.sql` / 线上配置，按一级目录）：

| 一级目录 | 路径 | 权限前缀 | 合并后归属 |
|---------|------|---------|-----------|
| 系统管理 | `/system` | `system:*` | Football SSOT（用户/角色/菜单/部门） |
| 基础设施 | `/infra` | `infra:*` | Football（定时任务、配置、监控） |
| 会员中心 | `/member` | `member:*` | Football 独有 |
| 支付管理 | `/pay` | `pay:*` | 合并（+ Ops 归因/ROI） |
| 公众号管理 | `/mp` | `mp:*` | 合并（+ Ops 分析） |
| 工作流程 | `/bpm` | `bpm:*` | Football（Ops SOP 复用引擎） |
| 商城/CRM/ERP/AI/IoT | `/mall` `/crm` `/erp` `/ai` `/iot` | 各模块前缀 | 评估保留（见 §2.3） |
| 运营数据 | `/ops` | `oa:*` | Ops 挂载（S2 seed） |

> **现状缺口**：Football seed 中**无**与 Ops PRD 对齐的业务角色（运营组长/运营人员等），仅有框架级 `super_admin` / `common`；业务角色需在合并 Phase 1 于 Football 角色管理中新建。

### 3.2 OPS 现有角色/权限清单

#### 3.2.1 RBAC 角色（`sys_role`，**待废弃**）

| 角色 ID | code | 名称 | data_scope | seed 权限范围 | Dev Token |
|--------|------|------|------------|--------------|-----------|
| 1 | `OA_ADMIN` | 系统管理员 | ALL | 全部 `oa:*`（M9 + 业务） | `dev-token-oa-admin` |
| 2 | `TENANT_ADMIN` | 租户管理员 | ALL | 用户/角色查询（租户 B） | `dev-token-oa-tenantb` |
| 3 | `OPS_LEADER` | 运营组长 | ALL | `oa:user/role/permission/account` 查询 | `dev-token-oa-leader` |
| 4 | `OPS_OPERATOR` | 运营专员 | IP_GROUP | `oa:user/account` 查询 | `dev-token-oa-operator` |
| 5 | `FINANCE` | 财务 | ALL | `oa:user` 查询 | `dev-token-oa-finance` |

#### 3.2.2 岗位字典（`dict_position`，**保留**）

与用户 `position` 字段绑定，用于 SOP 执行人解析、绩效模板、人效统计；**非 RBAC 角色**。

| 岗位 code | 名称 | 典型用途 |
|-----------|------|---------|
| `OPS_LEADER` | 运营组长 | IP 组组长、一级内容审核默认角色（ADR-017） |
| `OPERATOR` | 运营专员 | 日常运营执行 |
| `EDITOR` | 内容编辑 | 内容创作 |
| `ANCHOR` | 主播 | IP 组成员、任务执行 |
| `LIVE_OPERATOR` | 直播运营 | 直播节点执行 |
| `SALES` | 销售 | 跟进节点 |

#### 3.2.3 审核配置角色（`sys_param`，**保留**）

| 参数键 | 默认值 | 说明 |
|--------|--------|------|
| `content.review.level1.role` | `OPS_LEADER` | 一级审核；值为 `OPS_LEADER` 时范围限定为 IP 组组长 |
| `content.review.level2.role` | `DEPT_HEAD` | 二级审核；对应 Football 自定义角色 code |

#### 3.2.4 权限点（`oa:*`）

**M9 身份类（废弃，改 Football `system:*`）**：

| 权限码 | 说明 | 状态 |
|--------|------|------|
| `oa:user:*` | 用户 CRUD | ❌ 废弃 → `system:user:*` |
| `oa:role:*` | 角色 CRUD + 授权 | ❌ 废弃 → `system:role:*` |
| `oa:tenant:*` | 租户 CRUD | ❌ 废弃 → `system:tenant:*` |
| `oa:permission:list` | 权限点查询 | ❌ 废弃 → `system:menu:*` |
| `oa:dept:*` | 部门 CRUD/钉钉同步 | ⚠️ 过渡期保留，最终归 Football `system:dept:*` |

**Ops 业务类（保留，写入 `system_menu`）**：

| 模块 | 菜单 list 权限（代表） | 按钮/动作权限（节选） |
|------|---------------------|---------------------|
| M0 首页 | `oa:home:view` | — |
| M1 运营 | `oa:ip-group:list`、`oa:author:list`、`oa:account-analysis:list`、`oa:fans-analysis:list`、`oa:internal-content:list`、`oa:efficiency:list` | — |
| M2 内容 | `oa:content:list`、`oa:plan:list`、`oa:sop:list`、`oa:task:list`、`oa:knowledge:list`、`oa:layout-template:list` | `oa:content:publish`、`oa:content:typeset`、`oa:layout-template:create/update/delete/import` |
| M3 绩效 | `oa:perf:list`、`oa:order-attribution:list` | — |
| M4 资产 | `oa:company:list`、`oa:platform-account:list`、`oa:personal-account:list`、`oa:phone:list`、`oa:realname:list`、`oa:simcard:list`、`oa:triple-rel:list` | `oa:account:list`（隐藏按钮） |
| M5 财务 | `oa:cost:list`、`oa:roi:list`、`oa:financial-analysis:list` | `oa:finance:list`（子页） |
| M6 分析 | `oa:report:list`、`oa:funnel-analysis:list`、`oa:custom-query:list`、`oa:metric:list`、`oa:metric-analysis:list`、`oa:screen:view`、`oa:screen-config:list` | `oa:analysis:list`（报表子页） |
| M7 监测 | `oa:external-account:list`、`oa:high-fans:list`、`oa:low-fans:list`、`oa:hot-works:list`、`oa:low-score:list`、`oa:ip-theme:list` | `oa:industry-data:list`、`oa:wechat-data:list` |
| M8 配置 | `oa:config:ai-model:list`、`oa:config:ai-prompt:list`、`oa:config:*-collect:list`、`oa:config:threshold:list`、`oa:metadata:query` | `oa:metadata:create/update/delete` |
| M9 运维 | `oa:dict:admin-list`、`oa:param:list`、`oa:log:login`、`oa:log:operation`、`oa:message:list` | `oa:dict/param/message` 写权限 |
| M10 采集 | `oa:collect:task:list`、`oa:collect:quality:list`、`oa:collect:log:list`、`oa:collect:bridge:list` | `oa:collect:list`（编辑子页） |

完整路由↔权限映射见 `docs/delivery/oa-menu-permission-map.csv`（96 路由；M9 user/role/tenant 已标记 `excluded_m9=Y`）。

#### 3.2.5 PRD 业务角色 × 能力矩阵（目标态参考）

来源：`完整PRD-v9.1-开发版.md` §4、`PRD-业务版-v9.1.md` §4。

| PRD 角色 | 数据范围 | 核心菜单域 |
|---------|---------|-----------|
| 系统管理员 | 全部 | 全部模块 RWD + 系统管理 + 配置管理 |
| 运营管理者 | 全部 | 首页、运营、内容 SOP 模板、绩效、报表、配置（RW） |
| 运营组长 | 本 IP 组 | IP 组、任务、内容、绩效发起、本组分析 |
| 运营人员 | 本人/本组 | 账号分析、任务执行、内容提交 |
| 主播/作者 | 本人 | 作品分析、任务、内容提交 |
| 内容创作者 | 本组 | 任务、内容创作 |
| 审核人员 | — | 内容审核、SOP 审核 |
| 数据分析师 | 全部（分析域 RWD） | 报表、指标、漏斗、自定义查询、作品监测 |
| 财务人员 | 本人/分析域 | 成本、ROI、财务类报表、人效 |
| 快手运营 | 本 IP 组 | 快手相关运营与分析 |

### 3.3 重复/冲突分析

| # | 冲突点 | Football 现状 | OPS 现状 | 合并策略 |
|---|--------|--------------|---------|---------|
| 1 | **身份 SSOT** | `system_users` / `system_role` / `system_menu` | `sys_user` / `sys_role` / `sys_permission` | Football 为准；Ops 表只读过渡后停写（ADR-049 D4） |
| 2 | **M9 管理页面** | `/system/user`、`/system/role` 原生菜单 | `ops/system-user`、`ops/system-role`（已隐藏排除） | 仅保留 Football 原生；Ops 侧废弃（ADR-049 D7） |
| 3 | **权限码命名空间** | `system:*`、`member:*`、`pay:*`… | `oa:*` | **并存**，不合并前缀；同一用户 authorities = Football 菜单权限 ∪ `oa:*` |
| 4 | **角色概念重叠** | `super_admin`（技术管理员） | `OA_ADMIN`（业务全权限） | 映射为同一 Football 角色；废弃 `OA_ADMIN` |
| 5 | **运营组长** | 无对应 `system_role` | `OPS_LEADER`（sys_role）+ `dict_position` | 新建 Football 角色 `ops_leader`；岗位字典保留 |
| 6 | **部门负责人** | 可用 `system_role` + data_scope=3/4 | `DEPT_HEAD`（仅审核参数引用） | 新建 Football 角色 `dept_head`；用于二级审核 |
| 7 | **数据范围模型** | `data_scope` 1–5（部门维度） | `ALL` / `IP_GROUP` / `SELF` | 扩展 Football data-permission：Ops 业务表叠加 IP 组过滤（ADR-047 D5） |
| 8 | **字典/参数/日志** | `system_dict_*`、`infra_config`、Football 日志 | `sys_dict_*`、`sys_param`、Ops 日志 | 页面分域保留（§1.1）；权限从 `oa:dict:*` 等迁入 `system_menu` |
| 9 | **Dev Token 鉴权** | Football OAuth2 + Redis | `sys_user_token` + `FootballAuthProvider.mergeOaPermissions` | 联调期双轨；生产仅 Football 登录态 |
| 10 | **CRM 管理员** | `crm_admin` 已存在但未配权限 | 无 | 合并后评估：隐藏或并入运营管理者 |

### 3.4 合并后角色体系

在 Football `system_role` 新建/保留以下角色；权限通过 `system_role_menu` 分配（Football 原生菜单 + Ops 6100–6999 菜单块）。

| 合并角色 | code（建议） | 职责 | 数据范围 | Football 菜单权限 | Ops `oa:*` 权限（菜单范围） |
|---------|-------------|------|---------|-------------------|---------------------------|
| **超级管理员** | `super_admin` | 平台运维、全功能 | 全部 | `system:*`、`infra:*` + 全部业务域 | 全部 `oa:*` |
| **租户管理员** | `tenant_admin` | 租户级管理 | 本租户全部 | 本租户 `system:user/role/menu` | 本租户全部 `oa:*` |
| **运营管理者** | `ops_manager` | 运营总监/部门负责人决策 | 全部 | 工作台、作者、发布、支付/财务读、报表 | M0–M3 RWD；M4–M7 R；M8 RW；M9 运维页 R；M10 R |
| **部门负责人** | `dept_head` | 二级内容审核、部门绩效 | 本部门及以下 | 工作台、发布（审核）、绩效结果 | `oa:content:list`（审核）、`oa:perf:list`；**审核**：`content.review.level2.role` |
| **运营组长** | `ops_leader` | IP 组组长、一级审核、绩效发起 | 本 IP 组 | 工作台、作者、发布 | M1 本组 RW；M2 本组 RW；M3 本组 RW；M6 本组 R；**审核**：`content.review.level1.role` + IP 组长范围 |
| **运营人员** | `ops_operator` | 日常运营执行 | 本 IP 组 / 本人 | 工作台 | M1 本组/本人 R；M2 任务/内容 RW（本人）；M6 本人 R |
| **内容创作者** | `content_creator` | 文案/剪辑/直播运营 | 本 IP 组 / 本人 | 发布（内容） | `oa:task/list`、`oa:content/list`、`oa:layout-template/list`；`oa:content:typeset` |
| **审核人员** | `content_reviewer` | 内容/SOP 合规审核 | 按审核配置 | 发布（审核入口） | `oa:content:list`、`oa:sop:list`（审核队列） |
| **主播/作者** | `anchor` | 主播/作者自助视图 | 仅本人 | 作者、发布（只读/提交） | M1 本人 R；M2 本人任务 R/W；M6 本人 R |
| **数据分析师** | `data_analyst` | 深度分析、竞品 | 全部（分析域） | 数据分析 | M6 RWD；M7 RWD；M1/M5 R；M8 元数据 R |
| **财务人员** | `finance` | 成本/ROI/财务核算 | 全部（财务域） | 支付、财务 | `oa:cost/roi/financial-analysis/perf`；M5 RWD；M6 财务报表 RWD |
| **快手运营** | `kuaishou_ops` | 快手专项 | 本 IP 组 | 运营、发布 | M1 快手账号 R；M2 本组 R；与运营组长类似但缩小平台范围 |
| **CRM 管理员** | `crm_admin` | CRM 模块（可选） | 全部 | `/crm` | 无 Ops 权限（或按需开报表只读） |

**岗位（`dict_position`）与 RBAC 角色关系**：

| 岗位 | 建议绑定的 RBAC 角色 | 说明 |
|------|---------------------|------|
| `OPS_LEADER` | `ops_leader` | 一人可兼岗位 + 角色；IP 组组长身份另由 `oa_ip_group.leader_user_id` 决定审核范围 |
| `OPERATOR` | `ops_operator` | 默认映射 |
| `EDITOR` | `content_creator` | 可叠加 `ops_operator` |
| `ANCHOR` | `anchor` | 主播自助 |
| `LIVE_OPERATOR` | `content_creator` | 直播节点执行 |
| `SALES` | `ops_operator` | 销售跟进节点 |

### 3.5 迁移建议

#### 3.5.1 OPS 角色 → Football 角色映射

| OPS `sys_role.code` | 合并后 Football 角色 | 处置 |
|--------------------|---------------------|------|
| `OA_ADMIN` | `super_admin` | **废弃** `sys_role`；权限并入 `system_role_menu` |
| `TENANT_ADMIN` | `tenant_admin` | **废弃**；按租户保留 Football 内置角色 |
| `OPS_LEADER` | `ops_leader`（新建） | **迁移**；seed 权限从 `sys_role_permission` 转为 `system_role_menu` |
| `OPS_OPERATOR` | `ops_operator`（新建） | **迁移**；`data_scope` 改为 IP 组扩展规则 |
| `FINANCE` | `finance`（新建） | **迁移** |
| — | `ops_manager` | **新建**（PRD 运营管理者，原无 seed 角色） |
| — | `dept_head` | **新建**（原仅存在于审核参数） |
| — | `content_creator` / `content_reviewer` / `anchor` / `data_analyst` / `kuaishou_ops` | **新建**（按 PRD 矩阵拆分） |

#### 3.5.2 废弃与保留清单

| 对象 | 处置 | 阶段 |
|------|------|------|
| `sys_user` / `sys_user_role` / `sys_user_token` | 停写；Dev Token 过渡后移除 | Phase 1 |
| `sys_role` / `sys_role_permission` | 停写；角色定义迁 Football | Phase 1 |
| `sys_permission` | 停写；权限点以 `system_menu.permission` 为准 | Phase 1 |
| `sys_tenant` | 停写；租户归 Football `system_tenant` | Phase 1 |
| `dict_position` | **保留** | 持续 |
| `content.review.level1/2.role` | **保留**；下拉数据源改读 Football `system_role` | Phase 1 |
| `oa:tenant:*` / `oa:user:*` / `oa:role:*` | API 标记 `@Deprecated`；菜单从 seed 排除 | Phase 1（已部分完成） |
| `FootballAuthProvider.mergeOaPermissions` | 联调完成后移除 sys 表合并逻辑 | Phase 2 |

#### 3.5.3 实施顺序（与 §2.4 Phase 1 对齐）

| 步骤 | 动作 | 产出 |
|------|------|------|
| 1 | 在 Football 新建 8 个业务角色（`ops_manager` … `kuaishou_ops`） | `system_role` 记录 |
| 2 | 按 §3.4 矩阵配置 `system_role_menu`（原生菜单 + 6100–6999） | 角色→菜单授权 |
| 3 | 将现有 `super_admin` 补全 Ops 菜单授权（参考 `seed-oa-system-menu.sql` role_id=1） | 管理员可访问全部 Ops 页 |
| 4 | 用户从 `sys_user` 映射到 `system_users`；`position` 字段迁移 | 用户-角色-岗位一致 |
| 5 | 验证 `FootballAuthProvider` 仅读 `system_role_menu` 即可加载 `oa:*` | 移除 `mergeOaPermissions` 依赖 |
| 6 | 按 PRD 权限矩阵跑 Gate AUTH + 各模块 P0 用例 | TESTCASES 100% |

---

*文档更新时间：2026-07-15*  
*数据来源：`OPS与SAAS合并规划.md` + `神鱼体育SaaS运营数据平台-产品需求与角色权限方案.md` + 代码库 RBAC seed（`V2/V12/V15__seed*.sql`、`seed-oa-system-menu.sql`、`import-football-system-tables.sql`）+ `oa-menu-permission-map.csv`*
