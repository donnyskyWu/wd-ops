# SLICES-M9-系统管理

> **切片计划**：M9 系统管理
> **版本**：v1.1 | 2026-06-11

| Slice | 目标 | FR | 工时 |
|-------|------|----|------|
| S-01 | 用户 + 角色 + 权限 | FR-M9-001, 002 | 3.0 |
| S-07 | 部门树 + 钉钉同步 | FR-M9-001（ADR-013） | 2.0 |
| S-02 | 租户管理 | FR-M9-003 | 2.0 |
| S-03 | **字典管理**（核心） | FR-M9-005 | 3.0 |
| S-04 | 系统参数 | FR-M9-004 | 1.0 |
| S-05 | 日志管理 | FR-M9-006 | 2.0 |
| S-06 | 消息通知 | FR-M9-007 | 2.0 |

---

### S-07：部门树 + 钉钉同步（ADR-013）

**包含**：
- 迁移 `V41__m9_dept_dingtalk.sql`（`sys_dept`、`sys_user.dept_id`/`ding_user_id`、权限点）
- 后端：`DeptController` 6 端点 + `DingTalkClient`
- 前端：`UserManage.vue` 左右布局、部门树筛选、同步按钮
- 测试：`M9DeptS01IT`、`M9UserRoleS01IT` 扩展

**Out of Scope**：钉钉 OAuth SSO、同步删除用户、定时同步

---

## 全局规范

- `userStatus` / `tenantStatus` / `logLevel` / `logModule` 用 `<DictSelect />`
- `yesNo` / `gender` / `position` 用 `<DictSelect />`
- **字典管理**：新增字典 value 必须更新 [`GLOBAL-CONVENTIONS.md`](../engineering/GLOBAL-CONVENTIONS.md)

---

*下一步：CHECKLIST + TESTCASES。*

---

## AC 映射表（验收条件）

每个 Slice 都对应 PRD 中的一个或多个 AC（Acceptance Criteria），保证可追溯。

| Slice ID | 关联 AC | 标题 | 估时 |
|----------|---------|------|------|
| S-M9-01 | AC-M9-01 | 用户 CRUD + 状态机 | 1.5d |
| S-M9-02 | AC-M9-02 | 角色 + 权限分配 | 1d |
| S-M9-03 | AC-M9-03 | 操作日志查询 | 1d |

### 估算单位
- `d` = 人天（1 人 = 8 小时）
- 总估时 = sum of all slices

### 与测试用例的映射
每个 AC 对应 [`TESTCASES-*.md`](../delivery/) 中的 TC-F-* 用例。
