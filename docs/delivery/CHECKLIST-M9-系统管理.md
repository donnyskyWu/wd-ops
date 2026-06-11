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

---

## 7. M9 扩展功能（S-04~S-06，2026-06-11）

| # | 功能 | 后端 | 前端 | 状态 |
|---|------|------|------|------|
| 63 | 系统参数 FR-M9-004 | `ParamController` + `sys_param` V52 | `/system-param` → 真 API | [x] |
| 64 | 字典配置 FR-M9-005 | `SystemDictController` CRUD | `/system-dict` → 真 API | [x] |
| 65 | 操作日志 FR-M9-006 | `LogController` + `sys_operation_log` | `/system-log/operation` | [x] |
| 66 | 登录日志 FR-M9-006 | `LogController` + `sys_login_log` | `/system-log/login` | [x] |
| 67 | 消息管理 FR-M9-007 | `MessageController` + `sys_message` | `/system-message` | [x] |
| 73 | 头部个人中心 | `UserController.profile` 只读脱敏资料 | Header 用户下拉可查看 | [x] |
| 74 | 头部消息中心未读 | `MessageController` unread/count/read | Header 徽标 + 未读列表 + 查看后更新 | [x] |

**迁移**：`V52__m9_param_log_message.sql`

**测试**：`M9SystemExtendedIT`

**Spec 备注**：
- 系统参数 / 消息 API 路径来自 PRD + 前端契约，API-M9 未详述 request body（param/message）
- 消息发送 v1 为站内记录，不含 M10 外部渠道投递
- 头部个人中心无独立 PRD，按缺省只读视图实现：展示当前登录用户的用户名、昵称、邮箱、手机号掩码、岗位、部门、角色、状态；不提供敏感字段或编辑能力
- 头部消息中心基于 `sys_message.read_time` 实现未读状态；收件人匹配当前登录用户的 userId / username / nickname / email
- 操作日志由 `@AuditLog` AOP 写入 + seed 样例；2026-06-11 补齐 OA 后端 CUD / 状态变更 / 导入 / 导出 / 测试连接等显式操作日志覆盖，并登记 `ANALYTICS` / `REPORT` / `CONFIG` / `COLLECT` 模块分类；新写入日志的 `action` / `content` 记录中文业务语义（如 `公司管理 / 新增公司`），`module` 保留分类枚举用于筛选
- 登录日志 v1 为 seed + 查询，Dev Token 不做实时写入
