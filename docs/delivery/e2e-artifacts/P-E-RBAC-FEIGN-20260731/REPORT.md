# P-E RBAC 纯 Feign / 退役 MasterTokenMapper — 报告

| 字段 | 值 |
|------|---|
| 日期 | **2026-07-31** |
| Slice | P-E / ADR-056 · CLEANUP §1.2 |
| 状态 | **✅ 完成** |

## 范围落地

| 项 | 动作 |
|----|------|
| `FootballOAuth2MasterTokenMapper` | **已删**（main 源）；生产不再 `@DS("master")` 读 `system_users` / `system_menu` / `system_role` 做 RBAC |
| `LoginUserAssemblySupport` | Feign `AdminUserApi.getUser` + Gateway `login-user`（username/isAdmin）+ `PermissionCommonApi.hasAnyRoles`；**不再**预装 `system_menu.permission` |
| `@PreAuthorize` | 11 Controllers：`hasAuthority` → `@opsPerm.hasAuthority` → Feign `hasAnyPermissions`（Football Spec 已有；非新造端点） |
| `FootballSystemUserValidator` | 去 master；roleCode 列用户 = `getSimpleUserList` + `hasAnyRoles`；username 桥接尽力 Feign keyword |
| `IpGroupAccessSupport` / `ContentReviewConfigService` | 去 MasterTokenMapper |
| ADR-056 | `LoginUser.userId` = shenyu-system id 不变；validators 仍 Feign SSOT |

## 冒烟（RESULTS.json）

| Check | 结果 |
|-------|------|
| GW account / content / ip-group list | code=0 |
| 直连 `:48094` account / content / ip-group list | code=0 |
| RPC `has-any-permissions?permissions=ops:account:list` | true |
| **合计（有 overlay）** | **7/7** |
| Overlay 临时 RENAME `system_users` 后 GW+直连 | **4/4** code=0；ops 日志无 `system_users` / Table missing SQL error |
| Overlay | 验证后已 RENAME 回；`COUNT(*)=19` |

Admin 登录：`admin` / `admin123`，tenant=1。

## Residual（IT / 历史）

| 残留 | 说明 |
|------|------|
| H2 IT `sys_user*` | B-WP4 归档后生产无表；`mergeOaPermissions` / `SysUserMapper` 仍 try/catch，供 H2 IT fallback（ADR-056 D5） |
| `football-ops.system_users` 物理表 | **未 DROP**（本 Slice 只退代码依赖）；可另窗归档 |
| Feign `AdminUserRespDTO` 无 username | 登录 username 依赖 Gateway `login-user` info；非阻塞 admin 冒烟 |
| `ip_group_leader` 角色 | 本地 `shenyu-system` 可能未灌该角色；组长候选走 Feign `hasAnyRoles`，需 system 侧有角色数据（既有 seed 脚本，非本 Slice） |

## 同切片未做

P-F / P-G 重做；未 commit。
