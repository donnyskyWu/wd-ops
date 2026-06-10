# ADR-009：API 路径前缀分配（oa vs system）

| 字段 | 值 |
|------|---|
| 编号 | ADR-009 |
| 标题 | `yudao-module-oa` 全部 HTTP API 统一在 `/admin-api/oa/` 下 |
| 状态 | ✅ Accepted |
| 日期 | 2026-06-10 |
| 触发 | S-R17 B19 · S-R23-Mike |
| 决策人 | 后端架构 |

---

## 1. 背景

`yudao-module-oa` 内同时存在两套顶层前缀：

| 前缀 | 示例 | 模块 |
|------|------|------|
| `/admin-api/oa/*` | `/oa/config/threshold`、`/oa/dict/data` | M0–M8、M10 |
| `/admin-api/system/*` | `/system/user/list` | M9 用户/角色/租户 |

前后端虽一致，但新人易误以为 M9 也在 `/oa/` 下，且与「单模块单前缀」直觉不符。

---

## 2. 决策

### 2.1 规范路径（Canonical）

**`yudao-module-oa` 全部端点以 `/admin-api/oa/` 为唯一规范前缀**：

| 域 | 规范路径 | 说明 |
|----|----------|------|
| 业务域 M0–M7 | `/admin-api/oa/{resource}` | 不变 |
| 配置 M8 | `/admin-api/oa/config/{type}` | 不变 |
| 系统治理 M9 | `/admin-api/oa/system/{user,role,tenant,permission}` | **S-R23 迁入** |
| 字典横切 | `/admin-api/oa/dict/*` | ADR-006 已定 |
| 待建 M9 扩展 | `/admin-api/oa/system/{param,log,message}` | Phase 2 占位 |

### 2.2 兼容别名（Deprecated）

M9 四个 Controller 保留 **双 `@RequestMapping`**：

```java
@RequestMapping({"/admin-api/oa/system/user", "/admin-api/system/user"})
```

- 旧路径 `/admin-api/system/*` 仍可用，避免外部脚本断裂
- 新代码、前端 `api/*.ts`、IT **必须使用** `/oa/system/*`
- 计划在 Phase 2 移除别名

---

## 3. 影响面

| 层 | 变更 |
|----|------|
| 后端 | User/Role/Tenant/PermissionController 双路径 |
| 前端 | `system-user.ts`、`system-tenant.ts`、`UserSelect.vue` → `/oa/system/*` |
| IT | M9*、GateS2AuthIT 改用规范路径；`M9SystemPathPrefixIT` 测双路径 |
| 文档 | PRD §3A.2.10、API-M9 v1.1、GLOBAL-CONVENTIONS 字典 URL |

---

## 4. 不改动项

- 前端 **路由** `/system/user`（Vue Router）与 API 前缀无关，不改
- M8 `/oa/config/*` 已在规范前缀下，不改
- `DictController` 保持 `/oa/dict`（ADR-006）

---

## 5. Sign-off

| 角色 | 签字 |
|------|------|
| 架构 | ☑ S-R23 |
| 产品 | ☐ |
