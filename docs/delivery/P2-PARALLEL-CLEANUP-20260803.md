# P2 平行 OPS / Football 系统能力物理清理

| 字段 | 值 |
|------|---|
| 日期 | 2026-08-03 |
| SSOT | [OPS-FOOTBALL-MERGE-CLEANUP-INVENTORY.md](./OPS-FOOTBALL-MERGE-CLEANUP-INVENTORY.md) · [ADR-058](../adr/ADR-058-OPS后端单仓与football-module-ops命名.md) |
| 范围 | 平行 system CRUD 审计 + 最小安全 diff 物理清理 |
| 状态 | ✅ 本 Slice 完成（compile 绿；无 commit） |

---

## 审计结论（全量平行清单）

### 已移除 / 此前已清理（无需本 Slice 再动）

| 域 | OPS 平行对象 | Football SSOT | 状态 |
|----|-------------|---------------|------|
| 用户 CRUD | `UserController` | `#/system/user` | 物理删 ✅ C-WP7；stub 410 保留 |
| 角色 CRUD | `RoleController` | `#/system/role` | 物理删 ✅；stub 410 保留 |
| 部门 CRUD + 钉钉 sync | `DeptController` | `#/system/dept` | 物理删 ✅；stub 410 保留 |
| 权限 CRUD | `PermissionController` | Football RBAC | 物理删 ✅；stub 410 保留 |
| 租户 CRUD | `TenantController` | `#/system/tenant` | 物理删 ✅；stub 410 保留 |
| 字典管理 | 菜单 6137 + `SystemDictController` 写 | `#/dict` (105) | 菜单已摘 ✅ V149 |
| 登录日志 | 菜单 6138 | `#/system/login-log` | 菜单已摘 ✅ V146 |
| 操作日志 | 菜单 6139 | `#/system/operate-log` (1040) | 菜单已摘 ✅ V147 |
| 作者管理 | 菜单 6155 + 写 API | `#/author/info` (5071) | 菜单已摘 ✅ V145 |
| 平行 FE 页 | UserManage / RoleManage / TenantManage / DictManage / LogManage / LoginLog | FootballAdminRedirect | 已删或 redirect ✅ |

### 本 Slice 修复 / 物理删除

| 对象 | 动作 | 理由 |
|------|------|------|
| `ParamManage` → `fetchRoleList` | 改调 Football `GET /system/role/simple-list` | 末位 FE 410 调用（`/ops/system/role/list`） |
| `AuthorController` create/update/delete | **物理删除** endpoint | D-AUTHOR-01；Football SSOT |
| `AuthorService` create/update/delete + `buildExt`/`markExtError` | **物理删除** | 同上 |
| `AuthorCreateReq` / `AuthorUpdateReq` DTO | **删除文件** | 无引用 |
| `#/api/ops/author` create/update/delete | **删除** deprecated 导出 | 无 OPS 视图调用 |
| `DeferredCutoverStubController` parallel system 段 | **保留** stub + 注释更新 | API 安全网；FE 已无正常流调用 |

### 明确保留（OPS 域 · 非平行）

| 对象 | 路径 | 说明 |
|------|------|------|
| `UserProfileController` | `GET /ops/system/user/profile` | 当前用户只读；Layout / ContentEditPanel |
| `ParamController` | `/ops/system/param/**` | `sys_param` OPS 自建 |
| `MessageController` | `/ops/system/message/**` | OPS 消息中心 + 钉钉推送域（非 Football Notify 平行管理台） |
| `DictController` 读 | `GET /ops/dict/data` · `/types` | DictSelect 薄封装 → Feign shenyu-system |
| `AuthorController` 读 + ext | list/page/dashboard/ops-list | OPS 扩展面 |
| `AuthorExtController` | `/ops/author-ext/**` | `oa_author_ext` 写 |
| `SysUserMapper` / `SysRoleMapper` | wd legacy | H2 IT / nickname 桥；**未删**（ADR-056 过渡） |

### 菜单 seed（6105 系统管理 OA）

| menu id | 名称 | 处置 |
|---------|------|------|
| 6137 | 字典配置 | 已移除（Football 105） |
| 6138 | 登录日志 | 已移除 |
| 6139 | 操作日志 | 已移除 |
| 6140 | **消息管理** | **保留** — OPS 域 |
| 6141 | **系统参数** | **保留** — OPS 域 |
| 6155 | 作者管理 | 已移除 |

---

## FE `/ops/system/*` 调用矩阵（post-cleanup）

| API 路径 | 调用方 | 处置 |
|----------|--------|------|
| `/ops/system/user/profile` | Layout, ContentEditPanel | ✅ 保留 |
| `/ops/system/role/list` | ~~ParamManage~~ | ❌ 已移除 → Football simple-list |
| `/ops/system/param/**` | ParamManage | ✅ 保留 |
| `/ops/system/message/**` | MessageManage | ✅ 保留 |
| `/ops/dict/data` | DictSelect | ✅ 保留（Feign 读） |
| `/ops/dict/types` | MetadataManage | ✅ 保留（Feign 读） |

---

## 变更文件

### 后端

- `controller/author/AuthorController.java` — 删写 endpoint
- `service/author/AuthorService.java` — 删 create/update/delete
- `service/author/AuthorServiceImpl.java` — 删 stub 写方法 + dead helpers
- `api/dto/author/AuthorCreateReq.java` — 删除
- `api/dto/author/AuthorUpdateReq.java` — 删除
- `controller/cutover/DeferredCutoverStubController.java` — 注释

### 前端

- `api/ops/system-user.ts` — `fetchRoleList` → Football
- `api/ops/author.ts` — 删 deprecated CRUD 导出

### 文档

- 本文 · ADR-058 §8 追加行

---

## 验证

```powershell
# 后端编译
cd football-backend-saas
mvn -pl football-module-ops/football-module-ops-server -am compile -DskipTests

# 手验（integration 环境）
# 1. 登录 → OPS Layout profile 正常
# 2. 系统参数 → 编辑 content.review.level*.role → 角色下拉有数据（非 410）
# 3. IP 组 / 内容列表 → 无 410
# 4. 消息管理 → list/send 正常
# 5. Network：无 /ops/system/role/list 请求
```

---

## 后续（不在本 Slice）

- `SysUserMapper` / `SysRoleMapper` 全量 Feign 迁移后删 wd legacy 表（V172 后仍保留）
- `DeferredCutoverStubController` parallel system 段可在监控确认零外部调用后删除
- `DictController` `/type/list` admin 别名（若零调用）可 410
- Message 域与 Football Notify 产品二选一（G-NTF-01）— 当前保留 OPS 实现
