# MP-USER Feign Integration 手验记录

| 字段 | 值 |
|------|---|
| 日期 | 2026-07-29 |
| 环境 | dev-nacos-local · localhost 五库 |
| 执行人 | AI Integration |
| 关联 commit | OPS c15737f · Football submodule 1e1cedb54 |
| 范围 | G-MP-01 粉丝列表 · `MpUserDataService` Feign-only · `GET /rpc-api/mp/mpUser/getUserPageByAccount` |

---

## 1. 服务状态

| 服务 | 端口 | 结果 |
|------|------|------|
| gateway | 48080 | UP |
| mp-server | 48086 | UP（重建后） |
| oa-server | 48094 | UP |
| football-front | 5777 | UP |

## 2. 前置修复（阻塞项）

| 问题 | 处理 | 备注 |
|------|------|------|
| 旧 mp-server JAR 无 `getUserPageByAccount` | `mvn -pl football-module-mp/football-module-mp-server -am clean package -DskipTests` 后重启 | RPC 原返回 404 |
| mp-server 连错库（overlay → shenyu-system） | 新增 `scripts/integration-config/mp-integration-overlay.yml` 指向 `localhost/shenyu-mp` | 原返回 501 表结构未导入 |
| 本地 `mp_account.app_id` 为空导致启动崩溃 | 本地 DB：`UPDATE mp_account SET app_id=CONCAT('wx-local-', id) WHERE app_id IS NULL`（4 行） | 仅本地 Integration；未改 Football 代码 |
| `start-integration-all.ps1` 本地未挂 mp overlay | 已改为默认加载 `mp-integration-overlay.yml` | 防止后续手验复发 |

## 3. RPC 直调

**URL**

```
GET http://127.0.0.1:48086/rpc-api/mp/mpUser/getUserPageByAccount?accountId=1000002&pageNo=1&pageSize=10
Header: tenant-id: 1
```

**accountId**：`1000002`（Football `mp_account.id`，公众号「神鱼体育」）

| 指标 | 值 |
|------|---|
| HTTP | 200 |
| body.code | 0 |
| data.total | **13** |
| data.list.length | **10** |
| DB 对照 `shenyu-mp.mp_user WHERE account_id=1000002` | **13** |

## 4. OPS 业务 API

**URL**

```
GET http://127.0.0.1:48080/admin-api/oa/account/1000002/mp-followers?pageNo=1&pageSize=10
Header: Authorization: Bearer dev-token-oa-admin
Header: X-Tenant-Id: 1
```

| 指标 | 值 |
|------|---|
| HTTP | 200 |
| body.code | 0 |
| data.total | **13** |
| data.list.length | **10** |
| 首条 openid | `oBY7v1w1P8Y9uVQlOjeSxfMyuPyE` |
| 与 RPC 一致性 | total / openid / nickname 与 RPC 一致 |

**负例**

```
GET .../admin-api/oa/account/9006/mp-followers
→ code 1500, msg 含「仅公众号账号支持粉丝列表查询」
```

## 5. 浏览器（可选）

| 项 | 值 |
|----|---|
| 路由 | http://localhost:5777/platform-account/1000002?tab=mp-followers |
| 页面 | 平台账号详情 · Tab「粉丝列表」 |
| 手验 | 未在本会话做 Playwright 截图；API 层已通过 |

## 6. 结论

| 项 | 结果 |
|----|------|
| RPC `getUserPageByAccount` | **Pass** |
| OPS `/oa/account/{id}/mp-followers` Feign 路径 | **Pass** |
| MpUserDataService Feign-only cutover | **Pass** |

---

**版本** v1.0 · **日期** 2026-07-29
