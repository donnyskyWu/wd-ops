# ADR-064：OPS 六角色 RBAC 矩阵（Football `system_role`）

| 字段 | 值 |
|------|---|
| 编号 | ADR-064 |
| 标题 | OPS 业务六角色功能权限 + 数据域 + 审核角色参数 |
| 状态 | **Accepted**（产品确认 2026-08-02） |
| 日期 | 2026-08-02 |
| 关联 Spec | `完整PRD-v9.1-开发版.md` §4 · PRD-M1/M2 · `docs/delivery/oa-menu-permission-map.csv` · `scripts/integration-config/seed-oa-system-menu.sql` |
| 关联 ADR | ADR-017（审核参数默认值变更）· ADR-047/049（Football 菜单/身份）· ADR-056（用户 SSOT）· ADR-066（组长视为成员） |
| 关联工程 | `docs/engineering/OPS-RBAC-DATA-SCOPE.md` |
| 排除 | `super_admin` / 系统管理员（已有全量菜单，本 ADR 不改其授权） |

---

## 1. 背景

旧 PRD / 合并建议中角色偏多（运营组长、部门负责人、内容创作者、审核人员、主播、快手运营等），与 Football 落地角色不一致；审核参数仍默认 `OPS_LEADER` / `DEPT_HEAD`（ADR-017），与现网内置 `ip_group_leader` 脱节。

产品确认：在排除系统管理员后，固化 **额外 6 个业务角色**，并据此配置 `system_role` + `system_role_menu`（Ops 菜单块 6100–6175）及内容审核参数。

---

## 2. 决策（locked）

### 2.1 六角色定义

| code | name | Football `data_scope` | 数据域语义（业务） |
|------|------|----------------------|-------------------|
| `ip_group_leader` | IP组长 | `5`（非 ALL；登录后按 member/`ledIpGroupIds` → IP_GROUP） | **IP_GROUP**：以担任组长的 IP 组为主，监测/账号类走成员组 |
| `ops_manager` | 运营主管 | `1`（ALL） | 租户全量 |
| `finance` | 财务人员 | `1`（ALL） | **财务域**：成本/ROI/财务分析/绩效结果租户级；账号成本仍可走 member 过滤 |
| `content_editor` | 内容编辑 | `5` | **SELF + 本组只读**；**不审**（无 6118） |
| `ops_operator` | 运营 | `5` | **IP_GROUP + SELF**（主播/快手并入本角色） |
| `data_analyst` | 数据分析 | `1`（ALL） | **分析域 ALL**；自定义查询/漏斗等仍 SELF 过滤（6125/6128） |

### 2.2 与旧 PRD 角色映射

| 新角色 | 合并自 |
|--------|--------|
| `ip_group_leader` | 运营组长 + 一级审核职责 + 部分 `OPS_LEADER` seed |
| `ops_manager` | 运营管理者 + 二级审核职责 + `DEPT_HEAD` 审核参数位 |
| `finance` | 财务人员 |
| `content_editor` | 内容创作者 + 剪辑/直播运营岗位（**不含**审核） |
| `ops_operator` | 运营人员 + 主播/作者 + 快手运营 |
| `data_analyst` | 数据分析师 |

### 2.3 内容审核参数（修订 ADR-017 默认值）

| 参数键 | 原默认（ADR-017） | **新默认（本 ADR）** | 行为 |
|--------|------------------|---------------------|------|
| `content.review.level1.role` | `OPS_LEADER` | **`ip_group_leader`** | 一级：配置为该 code（或兼容旧值 `OPS_LEADER`）时走 **IP 组组长范围**（`ledIpGroupIds` / `leader_user_id`） |
| `content.review.level2.role` | `DEPT_HEAD` | **`ops_manager`** | 二级：持有该角色的用户可审租户内全部待二级内容 |

- `content_editor`：**不授予** 菜单 6118，无通过/驳回按钮；可提交审核。
- 兼容：代码仍识别旧值 `OPS_LEADER` 为「IP 组长范围」特殊语义，避免未迁移环境失效。

### 2.4 已确认的建议项

| # | 项 | 确认结论 |
|---|----|---------|
| 1 | 审核角色 code | `ip_group_leader` / `ops_manager` |
| 3 | 全部任务 6175 | **仅** IP组长 + 运营主管 |
| 5 | M10 采集 | 运营主管按矩阵授予（R/RW 菜单）；数据分析 **R** |
| 6 | 财务 × M7 监测 | 财务 **R**；分析 **RWD** |
| 8 | 内容编辑 × 人效 6156 | **不授予** |

（§6 其余建议：发布/排版按钮挂载、SOP 审核岗位、计划终止岗位 → 角色，不在本 ADR 强制改代码，另开任务。）

### 2.5 菜单授权原则

- SSOT 菜单源：`seed-oa-system-menu.sql` + `oa-menu-permission-map.csv`（权限码 seed 为 `ops:*`）。
- 分角色菜单 ID 清单：见落地脚本 `scripts/integration-config/seed-ops-six-roles-rbac.sql`（与确认稿 §5 对齐）。
- **不删除、不改写** `super_admin`（role_id=1）的 `system_role_menu`。
- 应用顺序：先 Ops 菜单 seed，再本角色 seed（菜单 seed 会 `DELETE` 6100–6999 的 role_menu 后只回填 role_id=1）。

---

## 3. 后果

| 层 | 变更 |
|----|------|
| Football `shenyu-system` | 幂等 upsert 6 角色；按角色 REPLACE Ops 段 `system_role_menu` |
| `shenyu-ops.sys_param` | Flyway V169 + Beta 直更：审核角色默认 → 新 code |
| `ContentReviewConfigService` | 默认角色与「IP 组长范围」识别对齐 `ip_group_leader`（兼容 `OPS_LEADER`） |
| ADR-017 | 默认值以本 ADR 为准；特殊语义角色 code 扩展 |
| 文档 | `OPS-RBAC-DATA-SCOPE.md` 审核参数表应交叉引用本 ADR |

---

## 4. 非目标

- 不新建 `dept_head` / `content_reviewer` / `anchor` / `kuaishou_ops` 等额外角色
- 不在本任务重写各模块 Service 数据过滤（沿用 `OpsDataScopeSupport` / 各 `*DataScopeSupport`）
- 不改 `super_admin` 菜单集
- 不强制迁移存量用户角色绑定（仅提供角色定义与菜单；用户指派另做）

---

## 5. 落地清单

- [x] 本 ADR
- [x] `scripts/integration-config/seed-ops-six-roles-rbac.sql`（utf8mb4，Python apply）
- [x] `V169__content_review_roles_six_rbac.sql` + `ContentReviewConfigService` 默认/特殊语义
- [x] Beta `shenyu-system` 应用并校验角色列表与 role_menu 计数（见 `docs/delivery/e2e-artifacts/OPS-SIX-ROLES-RBAC-20260802/`）
- [x] Beta `shenyu-ops` 审核参数值校验（level1=`ip_group_leader`，level2=`ops_manager`）
