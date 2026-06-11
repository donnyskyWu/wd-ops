# PRD-M9-系统管理

> **业务域**：M9 系统管理
> **功能模块**：用户 + 角色 + 租户 + 字典 + 日志 + 消息
> **详细设计章节**：5.35
> **版本**：v1.1 | 2026-06-11
> **全局规范**：[`docs/engineering/GLOBAL-CONVENTIONS.md`](./../engineering/GLOBAL-CONVENTIONS.md)

---

## 0. 元信息

| 字段 | 值 |
|------|---|
| 模块 | M9 系统管理 |
| 业务域 | 系统（SYSTEM） |
| 详细设计 | `## 5.35` |

---

## 1. 概述

系统级管理：用户/角色/权限/租户/字典/日志/消息。**字典管理是核心**（关联所有模块）。

### 实现补充（2026-06-11，ADR-013）

**FR-M9-001 扩展**：用户管理页左侧部门树 + 钉钉组织同步。

| 能力 | 说明 |
|------|------|
| 部门树 | `sys_dept` 表；左侧 `el-tree` 筛选 `GET /user/list?deptId=` |
| 部门 CRUD | 本地创建/编辑/删除（有子部门或用户 → 1502） |
| 钉钉同步 | 手动触发 `sync-dingtalk`（部门）、`sync-dingtalk-users`（人员） |
| 用户扩展 | `sys_user.dept_id`、`ding_user_id`；`ding_user_id` 供后续消息/SSO（Phase 2） |

**Out of Scope**：钉钉 OAuth 登录、同步删除本地用户、定时自动同步（见 ADR-013）。

---

## 2. 范围

| FR 编号 | 名称 | 优先级 |
|---------|------|--------|
| FR-M9-001 | 用户管理 | P0 |
| FR-M9-002 | 角色权限管理 | P0 |
| FR-M9-003 | 租户管理（v7.0 新增） | P0 |
| FR-M9-004 | 系统参数配置 | P0 |
| FR-M9-005 | **字典管理**（核心） | P0 |
| FR-M9-006 | 日志管理（操作日志 + 登录日志） | P0 |
| FR-M9-007 | 消息通知 | P1 |

---

## 3. 关键 FR 详述

### FR-M9-005 字典管理（⭐ 核心）

#### 数据项

| 字段 | 控件 | 字典 |
|------|------|------|
| `dictType` | `<Input />`（唯一） | - |
| `dictName` | `<Input />` | - |
| `dictLabel` | `<Input />` | - |
| `dictValue` | `<Input />` | - |
| `sort` | `<InputNumber />` | - |
| `status` | `<DictSelect dict-type="dict_yes_no" />` | 字典 |
| `colorType` | `<Select />` | 固定值（default/primary/success/warning/danger） |
| `remark` | `<TextArea />` | - |

#### 业务规则

- `dictType` 命名规范：`dict_value_placeholder`（小写+下划线）
- `dictValue` 命名规范：全大写+下划线
- 停用值不可删除（保留历史）
- 新增/修改字典 value 必须同步更新 [`GLOBAL-CONVENTIONS.md`](../engineering/GLOBAL-CONVENTIONS.md) § 2

#### 验收标准

**AC-M9-005-1**（字典 type 唯一）
**AC-M9-005-2**（value 命名）
**AC-M9-005-3**（停用不可删）

---

### FR-M9-006 日志管理

#### 业务

- 操作日志：所有 CRUD + 审核 + 强制替换 → 审计追踪
- 登录日志：登录/登出 + IP + 设备

#### 验收标准

**AC-M9-006-1**（操作日志记录）
**AC-M9-006-2**（登录日志）
**AC-M9-006-3**（日志保留 90 天）

---

## 4. 关联属性

| 字段 | 字典 |
|------|------|
| `userStatus` | `dict_user_status` |
| `tenantStatus` | `dict_tenant_status` |
| `logLevel` | `dict_log_level` |
| `logModule` | `dict_log_module` |
| `yesNo` | `dict_yes_no` |
| `gender` | `dict_gender` |

---

*下一步：UX / API / STATE / SLICES / CHECKLIST / TESTCASES。*
