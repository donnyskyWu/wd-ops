# 后端开发计划 v1.0

> **生成时间**：2026-06-09
> **范围**：M0–M10 共 11 个模块的后端开发排期、依赖关系、验收方法
> **依据**：`完整PRD-v9.1-开发版.md` + `docs/engineering/`（API×11 + STATE×11）+ `docs/adr/`（4 个 ADR）+ `GLOBAL-CONVENTIONS.md` + `TECH-CONSTRAINTS.md` + `QUALITY-GATES.md`
> **目标读者**：后端架构师 / 模块 Owner / 项目经理
> **关联文档**：[`PROJECT-OVERVIEW.md`](../engineering/PROJECT-OVERVIEW.md) · [`DEV-PLAN.md`](../engineering/DEV-PLAN.md) · [`GLOBAL-CONVENTIONS.md`](../engineering/GLOBAL-CONVENTIONS.md) · [`QUALITY-GATES.md`](../engineering/QUALITY-GATES.md)

---

## 0. 一句话总览

> **三大基座先行**（M8 字典 + M9 用户租户 + M4 账号资产中心）→
> **核心业务并行**（M1/M2/M3 依赖 M4 完成后三线并进）→
> **扩展业务并行**（M5/M6/M7/M10 依赖 M1+M2+M4）→
> **聚合层殿后**（M0 首页）。
>
> **关键风险**：M4 是头号瓶颈（被 7 个模块强依赖，5 选择器目标全在此）；M8 字典漏一个 `dict_type` 就会让全盘停摆；4 份 STATE 文档存在状态机重复/冲突，**阶段 3 启动前必须先统一**。

---

## 1. 模块依赖总览

### 1.1 模块全景（11 个）

| ID | 模块 | 定位 | 核心实体 | 阶段 |
|----|------|------|---------|------|
| **M0** | 首页仪表盘 | 只读聚合 | `home_metrics_cache` / `todo` / `quick_action` | 4 |
| **M1** | 运营管理 | IP 组中枢 | `oa_ip_group` / `oa_author` / `oa_content` / `oa_follower_daily` | 2 |
| **M2** | 内容生产 | SOP + 任务 + 内容 | `oa_sop_template` / `oa_task` / `oa_content` / `oa_knowledge_base` | 2 |
| **M3** | 绩效核算 | 模板 + 记录 + 归因 | `oa_perf_template` / `oa_perf_record` / `oa_order_attribution` | 2 |
| **M4** | 账号管理 | **资产中心（枢纽）** | `oa_company` / `oa_realname` / `oa_phone` / `oa_sim_card` / `oa_account` | **1（P0）** |
| **M5** | 财务管理 | 成本 + ROI | `oa_account_cost` / `oa_cost_process_record` / `oa_finance_record` | 3 |
| **M6** | 数据分析 | 指标 + 报表 + 漏斗 | `oa_metric` / `oa_report` / `oa_funnel` / `oa_dashboard` | 3 |
| **M7** | 作品监测 | 外部监测 | `oa_monitor_task` / `oa_alert` / `oa_external_account` | 3 |
| **M8** | 配置管理 | 字典 + 阈值 + AI 配置 | `sys_dict_type/data` / `oa_threshold_config` / `oa_ai_model` | **1（P0）** |
| **M9** | 系统管理 | 用户/角色/租户/日志 | `sys_user` / `sys_role` / `sys_tenant` / `sys_audit_log` | **1（P0）** |
| **M10** | 数据采集 | 任务 + 质量 + 凭据 | `oa_collect_task` / `oa_collect_log` / `oa_collect_credential` | 3 |

### 1.2 依赖层次（4 层架构）

```
┌──────────────────────────────────────────────────────────────────────┐
│ L4  聚合层           M0  首页仪表盘（只读）                            │
│       ↓ 读 M1/M2/M3/M4/M8/M9 状态                                     │
├──────────────────────────────────────────────────────────────────────┤
│ L3  扩展业务层       M5 财务 | M6 数据分析 | M7 监测 | M10 采集        │
│       ↓ 调用 M1/M2/M3/M4 实体 API + 字典 + 租户                        │
├──────────────────────────────────────────────────────────────────────┤
│ L2  核心业务层       M1 运营 | M2 内容 | M3 绩效                      │
│       ↓ 引用 M4 的 5 类强选择器 + 字典 + 用户                          │
├──────────────────────────────────────────────────────────────────────┤
│ L1  基础实体层       M4 账号管理（公司/实名人/手机/卡/账号）           │
│       ↓ 依赖 M8 字典 + M9 用户/租户/审计                               │
├──────────────────────────────────────────────────────────────────────┤
│ L0  横/纵基座        M8 配置（字典）| M9 系统（用户/租户/日志）         │
│       ↓ 不依赖任何 OA 模块（仅依赖 yudao 框架）                        │
└──────────────────────────────────────────────────────────────────────┘
```

### 1.3 关键依赖矩阵（强依赖 1 / 弱依赖 0.5 / 无 0）

> **行 = 下游（依赖方）**，**列 = 上游（被依赖方）**

| 下游 \ 上游 | M0 | M1 | M2 | M3 | **M4** | M5 | M6 | M7 | **M8** | **M9** | M10 |
|-------------|----|----|----|----|----|----|----|----|----|----|-----|
| M0 首页 | - | 1 | 1 | 1 | 0.5 | 0 | 0 | 0 | 1 | 1 | 0 |
| M1 运营 | 0 | - | 0 | 0 | **1** | 0 | 0 | 0 | 1 | 1 | 0 |
| M2 内容 | 0 | 1 | - | 0 | **1** | 0 | 0 | 0 | 1 | 1 | 0 |
| M3 绩效 | 0 | 1 | 1 | - | **1** | 1 | 0 | 0 | 1 | 1 | 0 |
| **M4 账号** | 0 | 0 | 0 | 0 | - | 0 | 0 | 0 | 1 | 1 | 0 |
| M5 财务 | 0 | 1 | 0 | 1 | **1** | - | 0 | 0 | 1 | 1 | 0 |
| M6 数据分析 | 0 | 1 | 1 | 1 | **1** | 1 | - | 0 | 1 | 1 | 0 |
| M7 监测 | 0 | 0 | 1 | 0 | **1** | 0 | 0 | - | 1 | 1 | 0 |
| **M8 配置** | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | - | 1 | 0 |
| **M9 系统** | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | - | 0 |
| M10 采集 | 0 | 1 | 0 | 0 | **1** | 0 | 0 | 0 | 1 | 1 | - |

**矩阵关键观察**：
- **M4 被 7 个模块强依赖**（M1/M2/M3/M5/M6/M7/M10），是头号枢纽
- **M8 字典被全 10 个 OA 模块依赖**（横向基座）
- **M9 用户/租户/审计被全 10 个 OA 模块依赖**（纵向基座）
- **M3 依赖面最广**（M1+M2+M4+M5+ 基座），需要四源数据

### 1.4 关键实体跨模块引用图

```
[维护方 W / 引用方 R / 消费方 C]
                     M0  M1  M2  M3  M4  M5  M6  M7  M8  M9  M10
oa_account  ⭐        .   R   R   R   W   R   R   R   .   .   R
oa_ip_group           .   W   R   R   .   R   R   R   .   .   R
oa_realname           .   R   .   .   W   R   R   .   .   .   .
oa_company            .   .   .   .   W   R   R   .   .   .   .
oa_sim_card           .   .   .   .   W   .   .   R   .   .   .
oa_author             .   W   R   R   .   R   R   .   .   .   .
oa_content            .   R   W   R   R   R   R   R   .   .   R
oa_sop_template       .   .   W   .   .   .   .   .   .   .   .
oa_perf_record        .   .   .   W   .   R   R   .   .   .   .
oa_account_cost       .   .   .   .   W→关联  W   R   .   .   .   .
oa_order_attribution  .   .   .   W   .   R   R   .   .   .   .
oa_metric             .   .   .   R   .   .   W   .   .   .   .
oa_alert              .   R   R   .   .   R   R   W   .   .   R
sys_dict_data  ⭐     .   R   R   R   R   R   R   R   W   R   R
sys_user              .   R   R   R   R   R   R   R   .   W   R
sys_tenant            .   R   R   R   R   R   R   R   .   W   R
```

---

## 2. 分阶段开发计划（4 阶段 5 步）

### 阶段 0：基础设施（第 1 周，共 5 天）

> **目标**：搭好 yudao 框架 + 三大横纵基座的"骨架"，所有后续模块的依赖项就位。

| 工作项 | 负责人建议 | 工期 | 验收标准 |
|--------|----------|------|---------|
| 后端框架拉取 + 数据库初始化脚本 | 后端架构师 | 1d | `ops-platform-server` 能 `mvn spring-boot:run` 启动；`sys_user/role/tenant/audit_log/dict_type/dict_data` 六张表自动建表 |
| 多租户拦截器 + MyBatis tenant 注入 | 后端架构师 | 1d | 任意 SELECT 自动 `WHERE tenant_id = ?`；跨租户访问抛 1504 |
| AES-256 工具类（idCard/phone/cookie/apiKey/iccid/appSecret/token 加密） | 后端架构师 | 0.5d | `aesUtil.encrypt("13800000000")` 返回密文；DB 字段 `xxx_encrypted` 落库；日志不打印明文 |
| 统一错误码 + BusinessException 工具类 | 后端架构师 | 0.5d | `BusinessException.of(1500, "实人不存在")` 抛 1500；RuntimeException 全部替换 |
| 审计 AOP（@AuditLog 注解 + sys_audit_log 写入） | 后端架构师 | 0.5d | `@AuditLog(module="oa_account", action="DELETE")` 自动写 before/after；事务回滚时审计不写 |
| 全局基线 SQL：`db/init_dict.sql`（80+ 字典初始化） | 后端架构师 | 1d | 导入后 `select count(*) from sys_dict_data` ≥ 200 行；所有 `dict_platform_type`/`dict_account_status`/`dict_industry`/`dict_carrier`/`dict_review_status`/`dict_collect_status` 6 个核心字典齐备 |

**阶段 0 验收**：
- ✅ `mvn test` 全绿
- ✅ 集成测试：登录 → 切租户 → 跨租户访问 → 返回 1504
- ✅ 代码 lint：`grep -r "RuntimeException" src/main/` 应为 0 行

---

### 阶段 1：三大基座（2 周）

> **目标**：把 M8 字典 + M9 用户/租户 + M4 账号资产中心全部交付。**这是头号瓶颈期**。

#### 1.1 M9 系统管理（5d）
- **基础**：yudao 自带 `sys_user/sys_role/sys_tenant/sys_audit_log` 二次扩展
- **API**：`/admin-api/system/*`（用户/角色/租户/日志/参数）
- **要点**：
  - 用户状态统一为 `ENABLED/DISABLED/LOCKED`（与 `dict_user_status` 一致）— 解决 STATE-M9 冲突 ❺
  - 租户上下文 `TenantContextHolder.set/get/clear`，AOP 切面在 `@PreAuthorize` 前注入
  - 角色权限点命名规范 `oa:{resource}:{action}`（如 `oa:account:create`）
- **验收**（详见 §3.1）：
  - ✅ 单元测试：用户 CRUD + 角色绑定 + 跨租户隔离
  - ✅ 集成测试：登录 → 获取 token → 携 `X-Tenant-Id` 调用任意接口 → 切换租户后查不到原数据
  - ✅ 自动化：`./mvnw test -Dtest=TenantIsolationIT`

#### 1.2 M8 配置管理（5d，可与 M9 并行）
- **基础**：`sys_dict_type/sys_dict_data` 继承 + OA 扩展
- **API**：
  - `POST /admin-api/config/dict/data` 增/改/删/查
  - `GET /admin-api/config/dict/type-list?type=dict_platform_type`
  - `POST /admin-api/config/threshold`（阈值规则）
  - `POST /admin-api/config/ai-model`（AI 模型 + AES 加密 apiKey）
  - `POST /admin-api/config/internal-collect` 等 4 个采集配置
- **要点**：
  - 新增 dict_type **必须先开 ADR**（GLOBAL-CONVENTIONS § 12）
  - `dict_ai_model.api_key` 字段 AES-256 加密落库
  - 字典启用/停用：`@InDict` 校验时只查 `enabled=1` 的
- **验收**（详见 §3.2）：
  - ✅ 单元：CRUD + 字典 value 唯一性
  - ✅ 集成：`@InDict(type="dict_xxx")` 失败时返回 1503
  - ✅ 自动化：`grep "dict_xxx" db/init_dict.sql` 应能找到全部登记的 type

#### 1.3 M4 账号管理（10d，**最关键**）
> ⚠️ **风险点**：M4 被 7 个模块强依赖，**5 选择器的目标全在此**，任一字段 bug 都会让 7 个模块联调失败。**必须 100% 验收通过才能进入阶段 2**。

- **基础**：`oa_company/realname/phone/sim_card/account/oa_account_cost`
- **API**：5 大类 30+ 接口（详见 `API-M4`）
- **要点**：
  - 5 选择器 API 必查 4 条链路：存在（1500）/ 启用（1501）/ 跨租户（1504）/ 已绑定（1502）
  - 实名人/手机/手机卡默认不可重复绑定；选 `forceReplace=true` + `reason(5-200字)` 走强制替换流程，写审计
  - 账号-IP 组多对多表 `oa_ip_group_account_rel` 加 `UNIQUE(account_id)` 约束（一对一）
  - AES-256 加密：`id_card_encrypted` / `phone_encrypted` / `cookie_encrypted` / `iccid_encrypted`
  - IP 组删除保护（ADR-M1-002）：仅"无子数据 + 24h 内"可物理删，否则 1005
- **验收**（详见 §3.3）：
  - ✅ 单元：5 选择器目标 30+ 字段的 4 链路 100% 覆盖
  - ✅ 集成：跨租户访问 → 1504；重复绑定 → 1502；强制替换流程 → 审计日志写入
  - ✅ 自动化：`./mvnw test -Dtest=SelectorIntegrationIT`（5 选择器联动测试必须 100% 通过）
  - ✅ Pilot 抽检：随机抽 20 条数据进行 CRUD + 4 链路 + 强制替换全跑

**阶段 1 验收门槛**：
- ✅ M8 字典初始化 200+ 行全部就绪
- ✅ M9 用户/角色/租户/审计全部通过租户隔离测试
- ✅ M4 5 选择器 100% 联动通过 + Pilot 抽检无缺陷
- ✅ 阶段 1 不通过 = **全项目停摆**

---

### 阶段 2：核心业务（3 周）

> **目标**：M1/M2/M3 并行开发。每个模块独立 Sprint，互不阻塞（除 M3 需 M1+M2 部分就绪）。

#### 2.1 M1 运营管理（10d）
- **基础**：`oa_ip_group/oa_author/oa_content/oa_follower_daily/oa_content_data_import`
- **依赖**：M4（5 选择器）+ M8（`dict_ip_group_type` 等）+ M9
- **API**：`/admin-api/operation/*` 25+ 接口
- **要点**：
  - 树形 IP 组：父子 + 路径（`path` 字段"/1/3/5/"）+ 拖拽排序
  - 补录数据 vs API 数据合并（ADR-M1-001）：`ContentDailyMergeService.merge()`，API 优先
  - 强制替换：已用实名人/手机/手机卡默认不可选；选 `forceReplace=true` + `reason` 走强制替换
- **验收**（详见 §3.4）：
  - ✅ 单元：IP 组树形 CRUD + 拖拽 + 补录合并
  - ✅ 集成：作者看板 5 维度 KPI + 趋势 + Top 榜
  - ✅ 自动化：作者分析 5 维度 + IP 组筛选联动

#### 2.2 M2 内容生产（10d，可与 M1 并行）
- **基础**：`oa_sop_template/oa_sop_node/oa_task/oa_content/oa_knowledge_base`
- **依赖**：M1（IP 组）+ M4（账号）+ M8（`dict_content_type`/`dict_sop_node_*`/`dict_ai_model`）+ M9
- **API**：`/admin-api/production/*` 30+ 接口
- **要点**：
  - SOP 模板 LogicFlow 拖拽 → JSON 存 `oa_sop_template.flow_json`
  - SOP 节点 8 状态机（STATE-M2 § 1.1）：解决 `DONE vs APPROVED` 语义边界含糊（OQ #4）
  - 任务多级审核：初审 → 复审 → 终审 → 发布
  - AI 集成：M8 `oa_ai_model` 配置 + 提示词 → 走 `oa_knowledge_base` 检索
- **验收**（详见 §3.5）：
  - ✅ 单元：SOP 8 状态机完整路径
  - ✅ 集成：LogicFlow JSON 存读 + 任务多级审核 + AI 生成链路
  - ✅ 自动化：8 状态机转换测试（每个 from-to 至少 1 个 case）

#### 2.3 M3 绩效核算（10d，**最后启动 1 周**）
- **基础**：`oa_perf_template/oa_perf_record/oa_perf_item_record/oa_order_attribution`
- **依赖**：M1（`oa_author`）+ M2（`oa_content`）+ M4（账号/成本/订单）+ M8 + M9
- **API**：`/admin-api/perf/*` 20+ 接口
- **要点**：
  - 考核模板 + 指标项（权重/公式）→ 拼装 `metric_formula`（注意 SQL 注入 OQ #3：加白名单/审批流）
  - 考核记录创建时，**自动按 `targetUser.position` 匹配模板**（OQ #1：M3 阶段需澄清）
  - 订单归因：M2 `oa_content`（发布账号+时间）+ 订单表 → 归因计算
  - 状态机：草稿 → 待审 → 已审 → 已发
- **验收**（详见 §3.6）：
  - ✅ 单元：模板 CRUD + 记录自动生成 + 订单归因算法
  - ✅ 集成：3 源数据（作者+内容+账号）联动
  - ✅ 自动化：归因计算结果与 Excel 用例对比

**阶段 2 验收门槛**：
- ✅ M1+M2 单元测试覆盖率 ≥ 80%
- ✅ M3 性能压测：单次归因计算 ≤ 500ms（10 万订单）
- ✅ Pilot 抽检：3 模块各抽 20 条数据人工复核

---

### 阶段 3：扩展业务（4 周）

> **目标**：M5/M6/M7/M10 并行。每个模块独立 Sprint，互不阻塞。

#### 3.1 M5 财务管理（8d）
- **基础**：`oa_account_cost/oa_cost_process_record/oa_finance_record`
- **依赖**：M1（IP 组+作者）+ M3（订单归因）+ M4 + M8 + M9
- **API**：`/admin-api/finance/*` 15+ 接口
- **要点**：
  - 状态机统一为 5 态：草稿 → 初审 → 复审 → 终审 → 已支付（解决 STATE-M5 冲突 ❷）
  - ROI 计算：5min `@Scheduled` 轮询（ADR-001：本地定时替 MQ）
  - AES 加密：金额/凭证/银行账号
- **验收**：详见 §3.7

#### 3.2 M6 数据分析（10d，可与 M5 并行）
- **基础**：`oa_metric/oa_report/oa_funnel/oa_dashboard`
- **依赖**：M1+M2+M3+M4（数据源）+ M8 + M9
- **API**：`/admin-api/analysis/*` 20+ 接口（8 张报表 + 漏斗 + 自定义查询 + 大屏）
- **要点**：
  - 指标公式 SQL 注入防护：白名单 + 审批流（OQ #3）
  - 报表导出走 `@Async + 任务表`（ADR-001）
  - 自定义查询 SQL 编辑器：只读租户 + LIMIT 上限
- **验收**：详见 §3.8

#### 3.3 M7 作品监测（8d，可与 M5/M6 并行）
- **基础**：`oa_monitor_task/oa_alert/oa_external_account`
- **依赖**：M4（被监测账号）+ M2（作品指标）+ M8 + M9
- **API**：`/admin-api/monitor/*` 12+ 接口
- **要点**：
  - 外部账号 vs `oa_account`：共用一张表（OQ #5：M7 阶段需澄清）
  - 监测定时：抓取 + 阈值比对 → `oa_alert` 写告警
  - 通知通道：v1 简化为站内消息（M9 `sys_message`），后续接短信/钉钉
- **验收**：详见 §3.9

#### 3.4 M10 数据采集（8d）
- **基础**：`oa_collect_task/oa_collect_log/oa_collect_credential/oa_data_quality_check`
- **依赖**：M4（账号凭据）+ M8（`dict_collect_*`）+ M9
- **API**：`/admin-api/collect/*` 10+ 接口
- **要点**：
  - 状态机统一为 5 态：PENDING/RUNNING/SUCCESS/FAILED/PARTIAL（解决 STATE-M10 冲突 ❶，与 `dict_collect_status` 一致）
  - 凭据 AES-256 加密（`api_key_encrypted`/`username_encrypted`）
  - 同步调用 + 重试 3 次（ADR-001）
  - 补全 schema（OQ #6）
- **验收**：详见 §3.10

**阶段 3 启动前必做**：
- 解决 4 个 STATE 文档状态机冲突/重复（见 §6.1）
- 解决 6 个 OQ 待澄清项（见 §6.2）

**阶段 3 验收门槛**：
- ✅ 4 模块单元测试覆盖率 ≥ 80%
- ✅ 集成：M5 ROI 计算结果与 M3 归因结果对账（误差 ≤ 0.01%）
- ✅ M6 8 张报表导出成功率 100%（10 万行级）

---

### 阶段 4：聚合层 + 全链路收口（1 周）

> **目标**：M0 首页聚合 + 全链路 e2e 测试。

#### 4.1 M0 首页（3d）
- **基础**：`home_metrics_cache`（定时刷）+ `todo`（聚合各模块待办）+ `quick_action`（权限过滤）
- **依赖**：M1+M2+M3+M4+M8+M9 全部就绪
- **API**：`/admin-api/home/*` 5+ 接口
- **要点**：
  - 待办优先级：SOP > 发布 > 绩效 > 集成（OQ #2：M0 阶段澄清）
  - 9 大模块 KPI 缓存：`@Scheduled` 每 5min 刷新
- **验收**：详见 §3.11

#### 4.2 全链路 e2e + 上线准备（2d）
- **E2E 测试脚本**（10 条主流程）：
  1. 登录 → 选租户 → 创建公司 → 创建实名人 → 绑手机 → 创建账号
  2. 创建 IP 组 → 拖拽排序 → 创建作者 → 关联实名人
  3. 创建 SOP 模板 → 拖拽节点 → 创建任务 → 走审核 → 发布内容
  4. 创建考核模板 → 创建考核记录 → 看得分
  5. 录入订单 → 归因 → ROI 计算
  6. 创建采集任务 → 看日志 → 看质量检查
  7. 创建监测账号 → 触发告警 → 站内消息
  8. 切换租户 → 跨租户访问 → 拒绝
  9. 切回首页 → 9 模块 KPI 实时刷新
  10. 导出 8 张报表 + 漏斗 + 自定义查询
- **性能压测**（100 并发）：首屏 ≤ 1.5s，5 接口并发
- **安全扫描**：SQL 注入 / XSS / CSRF / 越权
- **文档收口**：`docs/delivery/项目验收报告.md` 更新

**阶段 4 验收门槛 = 上线门槛**：
- ✅ 10 条 e2e 主流程 100% 通过
- ✅ 100 并发压测：p99 ≤ 1.5s，无 5xx
- ✅ 安全扫描 0 高危
- ✅ 全部 4 份 STATE 文档状态机已统一
- ✅ 全部 6 个 OQ 待澄清项已关闭

---

## 3. 各模块验收测试方法（11 个模块）

> 通用原则（参考 [`QUALITY-GATES.md`](../engineering/QUALITY-GATES.md)）：
> - 单元测试覆盖率 ≥ 80%
> - 集成测试：跨租户 + 字典 + 状态机 + 4 链路（1500/1501/1502/1504）
> - 性能压测：100 并发 p99 ≤ 1.5s
> - 自动化 CI：`./mvnw verify` 全绿
> - 业务 Pilot 抽检：每模块随机 20 条数据人工复核

### 3.1 M9 系统管理 验收

| 维度 | 验收点 | 工具/方法 |
|-----|-------|---------|
| 单元 | User/Role/Tenant CRUD + 状态机 | JUnit 5 + Mockito |
| 单元 | 密码 BCrypt 加密 + 登录失败锁定 | JUnit 5 |
| 集成 | 登录 → 携 `X-Tenant-Id` → 跨租户 1504 | Spring Boot Test |
| 集成 | 角色权限点 `oa:account:create` 校验 | Spring Security Test |
| 性能 | 100 并发登录 p99 ≤ 500ms | JMeter |
| 业务 | Pilot：3 个用户+5 个角色+2 租户切换 | 人工 |

**关键 TC**（[`TESTCASES-M9-系统管理.md`](../delivery/TESTCASES-M9-系统管理.md)）：
- 跨租户访问 → 1504
- 角色权限点缺失 → 403
- 操作日志 100% 落 `sys_audit_log`

### 3.2 M8 配置管理 验收

| 维度 | 验收点 | 工具/方法 |
|-----|-------|---------|
| 单元 | Dict CRUD + value 唯一性 + 缓存 | JUnit 5 |
| 单元 | 阈值规则引擎（4 维度 + 5 级别） | JUnit 5 |
| 单元 | AI 模型 AES 加密 + 提示词模板渲染 | JUnit 5 |
| 集成 | `@InDict(type="dict_xxx")` 失败 → 1503 | Spring Boot Test |
| 集成 | 字典启用/停用：停用后下拉不可见 | Spring Boot Test |
| 业务 | Pilot：80+ 字典 type 各抽 1 条数据 | 人工 |

**关键 TC**（[`TESTCASES-M8-配置管理.md`](../delivery/TESTCASES-M8-配置管理.md)）：
- `db/init_dict.sql` 必须 200+ 行
- 6 个核心字典（platform_type / account_status / industry / carrier / review_status / collect_status）必齐
- AI apiKey 落库为密文，日志不打印明文

### 3.3 M4 账号管理 验收（**最关键**）

| 维度 | 验收点 | 工具/方法 |
|-----|-------|---------|
| 单元 | 5 选择器 30+ 字段 CRUD | JUnit 5 |
| 单元 | 4 链路 100% 覆盖（1500/1501/1502/1504） | JUnit 5 |
| 单元 | AES 加密（idCard/phone/cookie/iccid） | JUnit 5 |
| 单元 | IP 组删除保护（ADR-M1-002） | JUnit 5 |
| 集成 | **5 选择器联动测试**（联动 = 7 模块解阻） | `SelectorIntegrationIT` |
| 集成 | 强制替换流程：选 `forceReplace=true` + `reason` → 解绑+绑定+审计 | Spring Boot Test |
| 集成 | 跨租户访问 → 1504 | Spring Boot Test |
| 业务 | Pilot：30 个实名人/30 个账号/20 个 IP 组 | 人工 |

**关键 TC**（[`TESTCASES-M4-账号管理.md`](../delivery/TESTCASES-M4-账号管理.md)）：
- 5 选择器联动 **必须 100% 通过**（这是阶段 1 唯一硬性门槛）
- 强制替换审计日志必含 before/after 全字段
- IP 组删除：无子数据 → 200；有子数据 → 1005

### 3.4 M1 运营管理 验收

| 维度 | 验收点 | 工具/方法 |
|-----|-------|---------|
| 单元 | IP 组树形 CRUD + 拖拽排序 | JUnit 5 |
| 单元 | 补录数据 vs API 数据合并（ADR-M1-001） | JUnit 5 |
| 单元 | 作者看板 5 维度 KPI 计算 | JUnit 5 |
| 集成 | IP 组筛选联动 5 接口 | Spring Boot Test |
| 集成 | 作者分析（5 维度 + 趋势 + Top 榜） | Spring Boot Test |
| 性能 | 5 接口并发 ≤ 1.5s | JMeter |
| 业务 | Pilot：5 个 IP 组/20 个作者/3 个看板 | 人工 |

**关键 TC**（[`TESTCASES-M1-运营管理.md`](../delivery/TESTCASES-M1-运营管理.md)）：
- 树形 path 字段拼接正确
- 补录数据：同一日同账号 → API 优先
- 5 维度 KPI 计算结果与 Excel 对账

### 3.5 M2 内容生产 验收

| 维度 | 验收点 | 工具/方法 |
|-----|-------|---------|
| 单元 | SOP 8 状态机完整路径 | JUnit 5 |
| 单元 | LogicFlow JSON 存读一致性 | JUnit 5 |
| 单元 | 任务多级审核（初审/复审/终审） | JUnit 5 |
| 单元 | AI 提示词模板渲染 + 模型调用 | JUnit 5 + 模拟 |
| 集成 | SOP 模板 → 任务 → 内容发布 链路 | Spring Boot Test |
| 集成 | 知识库检索 + AI 生成 | Spring Boot Test |
| 业务 | Pilot：3 个 SOP 模板/10 个任务/5 个 AI 生成 | 人工 |

**关键 TC**（[`TESTCASES-M2-内容生产.md`](../delivery/TESTCASES-M2-内容生产.md)）：
- 8 状态机：每个 from-to 至少 1 个 case
- LogicFlow JSON 拖拽节点后保存 → 重新加载无损
- AI 提示词变量替换正确

### 3.6 M3 绩效核算 验收

| 维度 | 验收点 | 工具/方法 |
|-----|-------|---------|
| 单元 | 考核模板 + 指标项（权重+公式） | JUnit 5 |
| 单元 | 考核记录自动生成（按 `position` 匹配模板） | JUnit 5 |
| 单元 | 订单归因算法（3 源：作者+内容+账号） | JUnit 5 |
| 单元 | 状态机：草稿→待审→已审→已发 | JUnit 5 |
| 集成 | 3 源数据联动 + 得分计算 | Spring Boot Test |
| 性能 | 单次归因 ≤ 500ms（10 万订单） | JMeter |
| 业务 | Pilot：3 个模板/20 个记录/100 个订单 | 人工 |

**关键 TC**（[`TESTCASES-M3-绩效核算.md`](../delivery/TESTCASES-M3-绩效核算.md)）：
- 归因计算结果与 Excel 对账
- 指标公式 SQL 注入：恶意 SQL → 拒绝
- 自动按 `position` 匹配模板（OQ #1 已澄清）

### 3.7 M5 财务管理 验收

| 维度 | 验收点 | 工具/方法 |
|-----|-------|---------|
| 单元 | 5 态状态机（草稿→初审→复审→终审→已支付） | JUnit 5 |
| 单元 | ROI 计算（5 维度：IP组/作者/账号/平台/时间） | JUnit 5 |
| 单元 | 金额/凭证/银行账号 AES 加密 | JUnit 5 |
| 集成 | 定时任务：5min 轮询 + 增量计算 | Spring Boot Test |
| 集成 | 与 M3 归因结果对账（误差 ≤ 0.01%） | Spring Boot Test |
| 业务 | Pilot：3 个账号/10 个 ROI 报表 | 人工 |

### 3.8 M6 数据分析 验收

| 维度 | 验收点 | 工具/方法 |
|-----|-------|---------|
| 单元 | 指标公式白名单 + SQL 注入防护 | JUnit 5 |
| 单元 | 8 张报表 SQL 模板 + 缓存 | JUnit 5 |
| 单元 | 漏斗 5 步骤定义 + 转化率计算 | JUnit 5 |
| 单元 | 自定义查询：租户过滤 + LIMIT 上限 | JUnit 5 |
| 集成 | 报表导出异步任务（10 万行） | Spring Boot Test |
| 集成 | 大屏 KPI 5min 定时刷新 | Spring Boot Test |
| 业务 | Pilot：8 张报表各 1 次 | 人工 |

### 3.9 M7 作品监测 验收

| 维度 | 验收点 | 工具/方法 |
|-----|-------|---------|
| 单元 | 监测任务调度 + 阈值比对 | JUnit 5 |
| 单元 | 告警生成（5 级别） | JUnit 5 |
| 集成 | 抓取失败 → 告警 → 站内消息 | Spring Boot Test |
| 集成 | 外部账号 vs `oa_account` 共表策略 | Spring Boot Test |
| 业务 | Pilot：5 个监测任务/10 个告警 | 人工 |

### 3.10 M10 数据采集 验收

| 维度 | 验收点 | 工具/方法 |
|-----|-------|---------|
| 单元 | 5 态状态机（PENDING/RUNNING/SUCCESS/FAILED/PARTIAL） | JUnit 5 |
| 单元 | 凭据 AES 加密（apiKey/username） | JUnit 5 |
| 单元 | 同步调用 + 重试 3 次 | JUnit 5 |
| 集成 | 数据质量检查（5 维度：完整性/准确性/及时性/一致性/唯一性） | Spring Boot Test |
| 集成 | 采集日志全链路追踪 | Spring Boot Test |
| 业务 | Pilot：3 个采集任务/5 个质量检查 | 人工 |

### 3.11 M0 首页 验收

| 维度 | 验收点 | 工具/方法 |
|-----|-------|---------|
| 单元 | 9 模块 KPI 聚合算法 | JUnit 5 |
| 单元 | 待办优先级排序（SOP>发布>绩效>集成） | JUnit 5 |
| 单元 | 快捷入口权限过滤 | JUnit 5 |
| 集成 | 9 模块 KPI 5min 定时刷新 | Spring Boot Test |
| 集成 | 切换租户后 KPI 切换 | Spring Boot Test |
| 性能 | 首屏 ≤ 1.5s，5 接口并发 | JMeter |
| 业务 | Pilot：3 个租户/3 种角色 | 人工 |

---

## 4. 排期总览

```
Week  1         2         3         4         5         6         7         8         9         10
──────────────────────────────────────────────────────────────────────────────────────────────────
S0  ████████ (基础设施)
S1         ████████████ (M9+M8+M4)  ←  阶段 1 硬门槛
S2                  ████████████████████████████ (M1/M2/M3 并行)
S3                                    ████████████████████████████████████████ (M5/M6/M7/M10)
S4                                                                       ██████ (M0+e2e)

里程碑:
  W1 EOW  →  阶段 0 验收
  W3 EOW  →  阶段 1 验收（P0 硬门槛：M4 5 选择器 100% 通过）
  W6 EOW  →  阶段 2 验收（M3 归因性能压测通过）
  W9 EOW  →  阶段 3 验收（4 模块全跑通）
  W10 EOW →  阶段 4 验收（10 条 e2e + 安全扫描 + 上线）
```

**总工期**：**10 周**（不含 buffer）+ 建议预留 2 周 buffer = **12 周约 3 个月**

---

## 5. 资源与人员建议

| 角色 | 数量 | 主要职责 |
|-----|------|---------|
| 后端架构师 | 1 | 阶段 0 + 跨模块技术决策 + 阶段 1 头号瓶颈支持 |
| 高级后端 | 2-3 | M4 / M1+M2 / M3 主力开发 |
| 中级后端 | 2-3 | M5 / M6 / M7 / M10 + 字典初始化 |
| 测试工程师 | 1-2 | 自动化测试 + Pilot 抽检 + 性能压测 |
| 前端联调 | 1-2 | 阶段 2 末开始联调（前端已就绪） |
| PM/Owner | 1 | 阶段 3 启动前关闭 4 状态机冲突 + 6 OQ |

---

## 6. 风险与澄清清单

### 6.1 必须解决的文档冲突（4 项，**阶段 3 启动前关闭**）

| # | 冲突位置 | 解决建议 |
|---|---------|---------|
| ❶ | STATE-M10 状态机两套（5 态 vs 6 态） | 统一为 `PENDING/RUNNING/SUCCESS/FAILED/PARTIAL`，与 `dict_collect_status` 一致 |
| ❷ | STATE-M5 状态机两套（无 vs 5 态） | 统一为 5 态：草稿→初审→复审→终审→已支付 |
| ❸ | STATE-M6 vs STATE-M7 命名易混 | 文档命名 OK，实现注意分别建表（`oa_report_task` vs `oa_alert`） |
| ❹ | STATE-M9 用户状态（ENABLED/DISABLED/LOCKED vs 待激活） | 统一为 ENABLED/DISABLED/LOCKED |

### 6.2 必须澄清的 OQ 待办（6 项，**按阶段关闭**）

| # | 位置 | 阶段 | 待澄清 |
|---|------|-----|--------|
| OQ#1 | API-M3 § 2.2 | 阶段 2 | M3 考核记录自动按 `targetUser.position` 匹配模板的规则 |
| OQ#2 | STATE-M0 § 1.1 | 阶段 4 | 待办优先级（SOP>发布>绩效>集成）+ 过期标红 |
| OQ#3 | API-M6 § 1.2 | 阶段 3 | 指标 SQL 注入防护粒度（白名单 + 审批流） |
| OQ#4 | STATE-M2 § 1.1 | 阶段 2 | SOP 8 状态中 `DONE` vs `APPROVED` 语义边界 |
| OQ#5 | API-M7 § 1 | 阶段 3 | 外部账号 vs `oa_account` 共表 or 另起表 |
| OQ#6 | API-M10 § 1.5 | 阶段 3 | 采集任务表 schema 补全（重试 3 次） |

### 6.3 头号风险

- **M4 账号管理 = 头号瓶颈**：5 选择器 100% 联动测试是阶段 1 唯一硬性门槛，**任一字段 bug 都会让 7 个模块联调失败**
- **M8 字典初始化漏一个 `dict_type` = 对应模块启动即崩**：阶段 0 末必须做"全字段引用扫描"
- **M3 依赖面最广**：M1+M2 任一延期都会阻塞 M3，建议 M3 后启动 1 周

---

## 7. 上线前 Checklist（汇总自各阶段）

- [ ] 阶段 0：6 大基线（拦截器/加密/错误码/审计/字典/AOP）就绪
- [ ] 阶段 1：M8 字典 200+ / M9 租户隔离 / **M4 5 选择器 100% 联动**
- [ ] 阶段 2：M1+M2 单测 ≥ 80% / M3 归因 ≤ 500ms / 3 模块 Pilot 通过
- [ ] 阶段 3：4 模块单测 ≥ 80% / M5 ROI 与 M3 对账误差 ≤ 0.01% / M6 报表导出 100% 成功
- [ ] 阶段 4：10 条 e2e 主流程 100% / 100 并发 p99 ≤ 1.5s / 安全扫描 0 高危
- [ ] 4 个 STATE 状态机冲突全部统一
- [ ] 6 个 OQ 待澄清全部关闭
- [ ] `docs/delivery/项目验收报告.md` 更新

---

## 8. 附录：关键决策引用

- **ADR-001**：中间件简化（`ConcurrentHashMap` 替 Redis / `@Async + 任务表` 替 MQ / `@Scheduled + 任务表` 替 XXL-JOB / 本地文件 替 MinIO）
- **ADR-002**：前端规范源选择（Vue 3 + TS + Element Plus）
- **ADR-M1-001**：补录数据 vs API 数据 → **API 优先**
- **ADR-M1-002**：IP 组删除保护（无子数据 + 24h 内）

---

*输出时间：2026-06-09 | 工期估算：10 周（不含 buffer，建议预留 2 周）| 适用版本：v9.1-开发版*
