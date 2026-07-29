# Phase C Cutover 执行记录

| 字段 | 值 |
|------|---|
| 日期 | 2026-07-29 |
| 执行 | Agent cutover session |
| SSOT | OPS-FOOTBALL-MERGE-WORK-PLAN C-WP2–5 / C-WP7 partial |

---

## 1. Integration 验证（Step 1）

| 服务 | 端口 | Health | RPC 抽检 | 结果 |
|------|------|--------|----------|------|
| system-server | 48081 | HTTP 200 | `GET /rpc-api/system/user/simple-list` → 404 路由（Feign 客户端直连仍可用） | **UP** |
| infra-server | 48082 | HTTP 200 | — | **UP** |
| mp-server | 48086 | HTTP 200 | — | **UP** |
| member-server | 48087 | HTTP 200 | — | **UP** |
| pay-server | 48085 | curl 000 | — | **DOWN**（Integration 默认未启） |
| Gateway | 48080 | UP | — | **UP** |
| oa-server | 48094 | UP | — | **UP** |

> 依据 `start-integration-all` 终态 + curl 抽检。G-PAY-01 阻塞于 pay-server 未启动；其余域可 cutover。

---

## 2. @DS 移除（Feign-only + fail-fast）

| 域 | 服务 | 读/写路径 | cutover 状态 |
|----|------|-----------|--------------|
| G-DICT-01 | `SystemDictAdapter` | `listEnabledDataByType` / `isValidValue` | **Feign-only**；管理 CRUD 仍 @DS system |
| G-INF-01 | `LocalFileStorageService` | upload | **Feign-only**；历史本地 key 只读代理保留 |
| G-SYS-01 | `FootballSystemUserValidator` | `listEnabledUsersInTenant` | **Feign-only** |
| G-SYS-02 | `FootballSystemUserValidator` | `assertEnabledInTenant` / `hasRoleCode` | **Feign-only** |
| G-SYS-02 | `FootballSystemUserValidator` | `listPresentableUserIdsByRoleCode` | Feign + legacy/master union；roleId 映射仍 @DS system |
| G-PAY-01 | `FootballOrderReadServiceImpl` | `listPayAllOrders` | **Feign-only**（pay-server down 时 fail-fast） |
| G-MEM-03 | `MemberArticleWriteService` | insert/update | **Feign-only** |
| G-MP-01 | `MpAccountDataService` | CRUD/page | **Feign-only** |

---

## 3. 仍保留 @DS（documented gaps）

| 路径 | @DS | 原因 | 解除条件 |
|------|-----|------|----------|
| C-WP1 鉴权 | `FootballOAuth2TokenMapper` | system | Gateway/check 切轨 |
| 用户昵称批量 | `FootballSystemUserLookupMapper` | system | Feign batch API 或 C-WP1 后删 |
| roleCode→roleId | `FootballSystemRoleLookupMapper` | system | Football roleCode RPC |
| 字典管理 CRUD | `FootballSystemDict*Mapper` | system | D-DEDUP-01 410 或 Football Admin |
| 文章 getById | `MemberArticleWriteService.getById` | member | G-MEM read RPC |
| 作者读 | `MemberAuthorReadService` | member | G-MEM-01/02 Feign |
| 作者用户 | `AuthorUserMapper` | member | 同上 |
| Smoke IT | `*DsSmokeMapper` | system/pay | C-WP7 删 smoke |

---

## 4. Multidb 配置（C-WP7 partial）

| 文件 | 变更 |
|------|------|
| `application-dev-local-multidb.yml` | 移除 member/mp/pay；保留 master + system |
| `ops-test-beta-multidb.yml` | 同上 |

---

## 5. 测试

见会话末尾 `mvn test -Dtest=*FeignDualRunTest` 结果。

---

## 6. 阻塞项

- **G-PAY-01**：pay-server :48085 未启动 — 订单列表 Integration 手验待补
- **C-WP1**：Token Mapper @DS system 未删
- **MemberAuthorReadService**：无 Feign，仍 @DS member（multidb 已移除 member DS — 仅 Integration 受影响；H2 IT 用 overlay）
