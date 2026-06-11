# UX-M9-系统管理

> **版本**：v1.1 | 2026-06-11
> **关联 PRD**：[`PRD-M9-系统管理.md`](./PRD-M9-系统管理.md)
> **全局规范**：[`GLOBAL-CONVENTIONS.md`](../engineering/GLOBAL-CONVENTIONS.md)

---


> **视觉规范参考**：[`开发规范/UI设计与开发规范.md`](../../开发规范/UI设计与开发规范.md)（仅原型/设计阶段）
> **实现技术栈**：[`TECH-CONSTRAINTS.md § 1.2`](../engineering/TECH-CONSTRAINTS.md)（Vue 3 + Element Plus）
> **决策记录**：[`ADR-002`](../../adr/ADR-002-前端规范源选择.md)

## 1. 页面清单

| 页面 | 路由 | FR |
|------|------|-----|
| 用户管理（部门树） | `/system/user` | FR-M9-001 + ADR-013 |
| 角色权限 | `/system/role` | FR-M9-002 |
| 租户管理 | `/system/tenant` | FR-M9-003 |
| 系统参数 | `/system/config` | FR-M9-004 |
| **字典管理** | `/system/dict` | FR-M9-005 |
| 操作日志 | `/system/log/operation` | FR-M9-006 |
| 登录日志 | `/system/log/login` | FR-M9-006 |
| 消息通知 | `/system/message` | FR-M9-007 |

---

## 2. P-M9-001 用户管理（实现 2026-06-11）

```
+------------------+----------------------------------------+
| 部门树 (240px)   | 用户列表 + 筛选 + 表格                  |
| 搜索部门         | 用户名/姓名模糊、角色、状态              |
| 同步钉钉部门     | [新增用户]                              |
| 同步钉钉人员     |                                        |
| 部门 CRUD        |                                        |
+------------------+----------------------------------------+
```

| 控件 | 类型 | 说明 |
|------|------|------|
| TREE-DEPT | `el-tree` | 点击筛选 `deptId`；模糊 `filter-node-method` |
| BTN-SYNC-DEPT | 按钮 | `POST /dept/sync-dingtalk` |
| BTN-SYNC-USER | 按钮 | `POST /dept/sync-dingtalk-users` |
| F-DEPT | 表单 | 用户 create/update 可选 `deptId` |
| COL-DEPT | 列 | `deptName` |

**Phase 2 Out of Scope**：钉钉 OAuth 登录（ADR-003 / ADR-013）

---

## 3. 字典管理（⭐）

### 3.1 列表

| 控件 | 字典 |
|------|------|
| F-DICT-TYPE | `<Input />` |
| F-DICT-NAME | `<Input />` |
| F-STATUS | `<DictSelect dict-type="dict_yes_no" />` |

### 3.2 字典项编辑

- `dictType` 全局唯一
- `dictValue` 全大写+下划线
- 新增后自动通知全局规范文档（人工 + 流程提醒）

---

*下一步：API / STATE / SLICES / CHECKLIST / TESTCASES。*
