# OPS × Football Integration Feign 手验清单

| 字段 | 值 |
|------|---|
| 文档性质 | **Integration 手验记录模板**（G-* Feign 双跑 / cutover 验收） |
| 版本 | v1.0 |
| 日期 | 2026-07-28 |
| 关联 | [WORK-PLAN §8.6](./OPS-FOOTBALL-MERGE-WORK-PLAN.md#86-阻塞表blockers) · [D-FEIGN-IT](./OPS-FOOTBALL-MERGE-DECISIONS.md#d-feign-ith2-it-与-feign-路径验证策略86-b-feign-it) · [OPS-DEV-DEPLOY-GUIDE](./OPS-DEV-DEPLOY-GUIDE.md) |
| IT 边界 | H2 `mvn verify` **不**断言 Feign 真调通；Feign 路径以本文 Integration 手验为准 |

---

## 1. 前置条件

### 1.1 启动栈

```powershell
# 仓库根目录 — Gate / Integration 默认路径
.\scripts\start-ops-dev.ps1
# 或
.\scripts\start-integration-all.ps1
```

登录：http://localhost:5777 · Gateway http://localhost:48080/admin-api · 租户 **1** · `admin` / `admin123`

### 1.2 服务端口（本地 Integration）

| 服务 | 端口 | Feign `spring.cloud.openfeign.client.config.*.url` | 备注 |
|------|------|-----------------------------------------------------|------|
| Gateway | 48080 | — | 浏览器 / Admin API |
| system-server | 48081 | `system-server` → `http://127.0.0.1:48081` | G-SYS / G-DICT |
| infra-server | 48082 | `infra-server` → `http://127.0.0.1:48082` | G-INF-01 |
| mp-server | 48086 | `mp-server` → `http://127.0.0.1:48086` | G-MP-01 |
| member-server | 48087 | `member-server` → `http://127.0.0.1:48087` | G-MEM-03（mock 或 `-FullMemberServer`） |
| **pay-server** | **48085** | **`pay-server` → `http://127.0.0.1:48085`** | G-PAY-01；**默认 Integration 脚本未启**，需单独起 JAR |
| match-server | 48088 | — | 非 G-* 手验范围 |
| oa-server | 48094 | — | OPS 进程；profile 含 `dev-nacos-local` |

**OPS Feign 直连配置 SSOT**：`ops-platform-server/ops-platform-module-oa/src/main/resources/application-dev-nacos-local.yml`

> **pay-server 说明**：端口 **48085**（见 [FULL-MERGE-RPC-ANALYSIS §1.1](./OPS-FOOTBALL-FULL-MERGE-RPC-ANALYSIS.md)）。`start-integration-all.ps1` **默认不启动** pay-server；G-PAY-01 手验前须单独启动 pay-server JAR 或确认 Beta 远程 Nacos 发现。

### 1.3 OPS 进程 Profile

oa-server 须激活：`dev,dev-nacos,dev-nacos-local`（及 multidb 若需五库读 fallback）。

---

## 2. G-* 手验步骤

> **双跑语义**：OPS 代码优先调 Feign；失败或不可用时回退 `@DS`。手验须证明 **Feign 路径可达且结果与 @DS 一致（或差异已文档化）**。

### G-SYS-01　用户 simple-list

| 项 | 内容 |
|----|------|
| **Feign 路径** | `GET http://127.0.0.1:48081/rpc-api/system/user/simple-list` |
| **OPS 消费** | `AdminUserApi.getSimpleUserList` · IP 组候选 / UserSelect 后端 |
| **@DS 回退** | `FootballSystemUserLookupMapper` @DS system |
| **手验步骤** | 1) curl RPC 返回 200 + 用户列表；2) OPS 页打开 IP 组编辑，UserSelect 有数据；3) oa-server 日志无 Feign 连续失败（或仅 startup 前） |
| **期望** | Feign 列表条数 ≥ @DS fallback（D-G-SYS-01 选项 B：与 Admin 同数据权限） |

### G-SYS-02　用户校验 / 角色

| 项 | 内容 |
|----|------|
| **Feign 路径** | `GET .../user/get?id=` · `GET .../user/valid?ids=` · `GET .../user/getUserListByRoleId?roleId=` · `GET .../permission/has-any-roles?userId=&roles=` |
| **OPS 消费** | `FootballSystemUserValidator` · `PermissionCommonApi` |
| **@DS 回退** | system DS + legacy union |
| **手验步骤** | 1) 保存 IP 组（含 leader/member UserSelect）；2) 触发需角色校验的操作；3) 故意提交禁用用户 id → 业务拒绝 |
| **期望** | 写入校验走 Feign；失败时回退 @DS 仍可用（双跑） |

### G-DICT-01　字典读 / @InDict

| 项 | 内容 |
|----|------|
| **Feign 路径** | `GET http://127.0.0.1:48081/rpc-api/system/dict-data/list?dictType={type}` · `GET .../dict-data/valid?dictType=&values=` |
| **OPS 消费** | `SystemDictAdapter` · `@InDict` 校验 |
| **@DS 回退** | `FootballSystemDictDataMapper` |
| **手验步骤** | 1) curl 某业务 dictType；2) 打开含 DictSelect 的表单（如平台类型）；3) 提交非法枚举值 → 1503 |
| **期望** | 读路径 Feign 200；sort 差异非阻塞（D-G-DICT-01 选项 B） |

### G-INF-01　文件上传 / 预签名

| 项 | 内容 |
|----|------|
| **Feign 路径** | `POST http://127.0.0.1:48082/rpc-api/infra/file/create` · `GET .../infra/file/presigned-url?path=` |
| **OPS 消费** | `LocalFileStorageService` Feign 双跑 |
| **@DS 回退** | 本地盘 `/oa/file` |
| **手验步骤** | 1) 内容生产上传配图；2) 预览/下载；3) 检查返回 URL 为 infra 域 |
| **期望** | 新上传走 Feign；历史本地 key 仍可代理 |

### G-PAY-01　订单列表

| 项 | 内容 |
|----|------|
| **Feign 路径** | `POST http://127.0.0.1:48085/rpc-api/pay/order/page` · Body: `OrderPageReqDTO` |
| **OPS 消费** | `FootballOrderReadService` · `PayOrderApi.getOrderPage` |
| **@DS 回退** | `FootballPayAllOrderReadMapper` @DS pay |
| **前置** | **pay-server :48085 必须 UP**；`application-dev-nacos-local.yml` 已配置 `pay-server.url` |
| **手验步骤** | 1) `curl -X POST .../pay/order/page -d '{"pageNo":1,"pageSize":10}'` 返回 200；2) OPS 订单归因/列表页有数据；3) 对比 Feign 与 @DS 列字段（§8.8 对表） |
| **期望** | 10 列 100% 覆盖；tenant-id Header 生效 |

### G-MEM-03　文章写

| 项 | 内容 |
|----|------|
| **Feign 路径** | `POST http://127.0.0.1:48087/rpc-api/member/article/create` · `PUT .../update` · `POST .../status-change` |
| **OPS 消费** | `MemberArticleWriteService` |
| **@DS 回退** | `AuthorArticleMapper` @DS member |
| **手验步骤** | 1) 内容生产同步创建文章；2) 更新标题/状态；3) Football Admin 侧可见 |
| **期望** | 写路径 Feign 成功；member 库与 RPC 一致 |

### G-MP-01　公众号

| 项 | 内容 |
|----|------|
| **Feign 路径** | `GET/POST/PUT http://127.0.0.1:48086/rpc-api/mp/accountInfo/*`（page/create/update/get/getMpAccountByAppId） |
| **OPS 消费** | `MpAccountDataService` |
| **@DS 回退** | `MpAccountMapper` @DS mp |
| **手验步骤** | 1) M4 资产链打开微信账号列表；2) 创建/更新账号（若产品仍允许 OPS 编排）；3) `oa_account_ext` 关联正确 |
| **期望** | get/create/update Feign 双跑；page 可能仍 @DS（WORK-PLAN §8.4） |

---

## 3. 记录模板

复制下表到 Gate 报告或 PR 评论。

| 日期 | 环境 | 执行人 | G-* | Feign 路径抽检 | OPS 业务步骤 | 结果 | 备注 |
|------|------|--------|-----|----------------|--------------|------|------|
| YYYY-MM-DD | dev-nacos-local / Integration | | G-SYS-01 | `GET :48081/.../simple-list` → HTTP ___ | IP 组 UserSelect | ☐ Pass ☐ Fail ☐ 待手验 | |
| | | | G-SYS-02 | `has-any-roles` / `getUser` | IP 组保存 | ☐ Pass ☐ Fail ☐ 待手验 | |
| | | | G-DICT-01 | `dict-data/list` | DictSelect 表单 | ☐ Pass ☐ Fail ☐ 待手验 | |
| | | | G-INF-01 | `infra/file/create` | 内容配图上传 | ☐ Pass ☐ Fail ☐ 待手验 | |
| | | | G-PAY-01 | `POST :48085/.../order/page` | 订单列表 | ☐ Pass ☐ Fail ☐ 待手验 | pay-server 需单独启动 |
| | | | G-MEM-03 | `member/article/create` | 内容同步 | ☐ Pass ☐ Fail ☐ 待手验 | |
| | | | G-MP-01 | `mp/accountInfo/page` | 微信账号 | ☐ Pass ☐ Fail ☐ 待手验 | |

---

## 4. 2026-07-28 会话抽检记录

> 环境：`start-integration-all` 已起（system/infra/mp/member/oa/gateway）；**pay-server 未启动**。

| G-* | RPC 抽检 | 结果 | 备注 |
|-----|----------|------|------|
| G-SYS-01 | `GET :48081/rpc-api/system/user/simple-list` | **HTTP 200** | Feign 目标可达 |
| G-DICT-01 | `GET :48081/rpc-api/system/dict-data/list?dictType=test` | **HTTP 200** | RPC 端点可达；OPS `@InDict` 业务步骤仍待手验 |
| G-INF-01 | `GET :48082/rpc-api/infra/file/presigned-url?path=test` | **HTTP 200** | RPC 端点可达 |
| G-MEM-03 | `POST :48087/rpc-api/member/article/create`（空 body） | **HTTP 200** | RPC 端点可达；内容同步全流程仍待手验 |
| G-MP-01 | `GET :48086/rpc-api/mp/accountInfo/page?pageNo=1&pageSize=10` | **HTTP 200** | RPC 端点可达；M4 页面步骤仍待手验 |
| G-PAY-01 | `POST :48085/rpc-api/pay/order/page` | **待手验** | :48085 无监听（curl `000`）；**配置已补** `pay-server.url` |
| G-SYS-02 | — | **待手验** | 需 OPS 业务步骤 + 日志 |

**结论**：配置已补 pay-server URL（`:48085`）；**G-PAY-01 及 OPS 双跑业务手验待 Integration 环境执行**（须先启动 pay-server）。

---

## 5. cutover 签字

全部 G-* 手验 **Pass** 后，方可在 WORK-PLAN 对应 C-WP 勾选「删 @DS」并更新 §8.6 阻塞表。

| 签字项 | 日期 | 签字人 |
|--------|------|--------|
| G-SYS-01/02 cutover | | |
| G-DICT-01 cutover | | |
| G-INF-01 cutover | | |
| G-PAY-01 cutover | | |
| G-MEM-03 cutover | | |
| G-MP-01 cutover | | |

---

**版本** v1.0 · **日期** 2026-07-28
