# CHECKLIST-M9-系统管理

> **M9 自检清单** | 版本 v1.1 | 2026-06-11

---

## 1. 范围与 FR

- [ ] 6 个 Slice 全部完成
- [ ] 7 个 FR 全部实现
- [ ] **字典管理**核心模块
- [ ] `sys_dept` 表 + V41 迁移
- [ ] 部门树 CRUD + `GET /dept/tree`
- [ ] 钉钉部门/人员手动同步（`oa:dept:sync-dingtalk` / `oa:user:sync-dingtalk`）
- [ ] 用户 `deptId` / `dingUserId` 持久化 + 列表按部门筛选
- [ ] 钉钉 SSO **未实现**（Phase 2，见 ADR-013）

## 2. 全局规范（🔴 必查）

- [ ] `userStatus` / `tenantStatus` / `logLevel` / `logModule` 用 `<DictSelect />`
- [ ] `yesNo` / `gender` / `position` 用 `<DictSelect />`
- [ ] 字典 value 命名规范（全大写+下划线）
- [ ] **新增字典后必须更新 `GLOBAL-CONVENTIONS.md`**
- [ ] 跨租户隔离

## 3. 状态机

- [ ] 用户 3 状态
- [ ] 租户 4 状态

## 4. 测试

- [ ] 单元测试覆盖 80%+
- [ ] AC 全部通过
- [ ] 字典 type/value 唯一性测试
- [ ] 字典被引用测试

## 5. 文档

- [ ] PRD 同步
- [ ] Swagger 文档
- [ ] 字典项已登记

## 6. Sign-off

| 角色 | 姓名 | 日期 |
|------|------|------|
| 开发 |  |  |
| 测试 |  |  |
| 产品 |  |  |
| 架构 |  |  |
