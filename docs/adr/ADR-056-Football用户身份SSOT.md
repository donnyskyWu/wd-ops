# ADR-056：Football 用户身份 SSOT（shenyu-system system_users.id）

| 字段 | 值 |
|------|---|
| 编号 | ADR-056 |
| 标题 | Football 用户身份 SSOT — shenyu-system `system_users.id` |
| 状态 | **Accepted** |
| 日期 | 2026-07-22 |
| 决策人 | 架构 / 集成 |
| 关联 | [ADR-047](./ADR-047-Football-Ops平台集成决策.md) · [ADR-049](./ADR-049-Ops与Football数据归属与松耦合集成.md) · [ADR-050](./ADR-050-Ops与Football多库复用总纲.md) |

---

## 1. 背景

Football × Ops 集成后，用户选择器（`UserSelect`）、登录上下文（`LoginUser.userId`）与 Football 前端均使用 **shenyu-system** 库 `system_users.id`。历史上 Ops standalone 曾写入 wd master  overlay 或 legacy `sys_user.id`，导致：

- IP 组长 / 成员、任务执行人、考核对象等与 UserSelect 回显不一致
- 数据权限 `resolveMembershipUserIds` 桥接失败
- 操作日志 `@Trans` 用户解析错误

[IpGroupServiceImpl](../ops-platform-server/ops-platform-module-oa/src/main/java/cn/iocoder/yudao/module/oa/service/ipgroup/IpGroupServiceImpl.java) 与 [FootballSystemUserValidator](../ops-platform-server/ops-platform-module-oa/src/main/java/cn/iocoder/yudao/module/oa/service/support/FootballSystemUserValidator.java) 已确立正确模式；本 ADR 将其提升为**全局强制规则**。

---

## 2. 决策

| # | 规则 | 说明 |
|---|------|------|
| D1 | **身份 SSOT** | 用户身份以 **shenyu-system `system_users.id`** 为准；`LoginUser.userId` = 该 id |
| D2 | **写入 normalize** | 凡来自 `UserSelect` / API 的用户 id 字段，**写入 OA 表前**须调用 `FootballSystemUserValidator.resolveStorableUserId(submittedId, tenantId)` |
| D3 | **读出 presentable** | 回显给 `UserSelect` / 前端选择器时，须 `resolvePresentableUserId(storedId)`（stored 可能为历史 wd/legacy id） |
| D4 | **校验** | 业务校验用 `assertInTenant` / `assertEnabledInTenant` / `hasRoleCode`；**禁止**仅用 wd master `system_users` 或 `SysUserMapper.selectById` 作为唯一校验 |
| D5 | **读取 shenyu 优先** | 角色 / 用户列表 SSOT = shenyu-system（`FootballOAuth2TokenMapper` / `FootballSystemUserSystemReader`）；wd master overlay 与 legacy `sys_user` **仅** H2 集成测试 fallback |
| D6 | **禁止新写 wd id** | Football 集成激活时，**不得**将 UserSelect 提交的 id normalize 为 wd master `system_users.id` 再持久化 |
| D7 | **username 桥接只读** | legacy / wd id → shenyu id 的 username 桥接仅用于**读**历史数据、数据权限 union；**新写入**不走 wd 归一化 |
| D8 | **JSON 精度** | 前端 / API 对用户 id 使用 **字符串** 传递 snowflake 大整数（已有约定延续） |

---

## 3. 适用字段（非穷举）

| 模块 | 字段 |
|------|------|
| IP 组 | `leader_user_id`, `oa_ip_group_member.user_id` |
| 任务 / 计划 | `oa_task.assignee_id`, 计划步骤 `assignee_ids` |
| 内容 | `creator_user_id`（UserSelect 变更时） |
| 绩效 | `target_user_id` |
| M4 资产 | `keeper_id`, `assigned_user_id`, `admin_user_id` |
| 运营关系 | `ops_user_id`, `anchor_user_id`（系统用户域；作者 id 见 ADR-051） |
| 操作日志 | operate-log `userId` → shenyu id（见 OaLogRecordServiceImpl） |

**不在本期**：`oa_author.user_id` 批量迁移（仍见 ADR-049 待决）；作者主键 `author_user_id` 保持作者域 SSOT。

---

## 4. 实现约定

### 4.1 必用组件

- `FootballSystemUserValidator` — 校验、normalize、display、角色
- `FootballSystemUserSystemReader` — shenyu-system JDBC 读（避免 master 事务 DS 粘连）
- `IpGroupAccessSupport.resolveMembershipUserIds` — 数据权限 union（读路径）

### 4.2 代码审查 Checklist

- [ ] 新增/修改 `*_user_id` 写入是否调用 `resolveStorableUserId`
- [ ] UserSelect 回显是否 `resolvePresentableUserId`
- [ ] 是否误用 `@DS("master")` 查用户做**写入前**唯一校验
- [ ] 角色列表是否优先 `footballOAuth2TokenMapper`（shenyu）而非 master

---

## 5. 历史数据

已持久化的 wd master / legacy `sys_user.id`：

- **读**：username 桥接 + `IpGroupAccessSupport` union（兼容）
- **写**：新数据一律 shenyu id；**批量 backfill 迁移不在本 ADR 范围**（按模块单独评估）

---

## 6. 后果

- 各模块 Service 写入路径对齐 IP 组修复模式
- H2 IT 仍可通过 legacy `sys_user` fallback 绿
- Cursor 规则 `football-user-ssot.mdc` 强制 AI / 人工遵守
