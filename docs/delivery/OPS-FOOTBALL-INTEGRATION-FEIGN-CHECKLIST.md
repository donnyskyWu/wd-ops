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

**结论**：Phase C cutover 2026-07-29 — OPS 业务域 Feign-only + fail-fast；multidb 已移除 member/mp/pay。**G-PAY-01 Integration 手验仍阻塞**（pay-server 未启）。

---

## 6. 2026-07-29 cutover 抽检

> 环境：`start-integration-all` 终态（system/infra/mp/member/oa/gateway UP）；**pay-server 未启动**。

| G-* | RPC / Health 抽检 | cutover 代码 | 结果 |
|-----|-------------------|--------------|------|
| G-SYS-01/02 | :48081 HTTP 200 | `FootballSystemUserValidator` Feign-only | **代码 cutover ✅** · 业务手验待 |
| G-DICT-01 | :48081 HTTP 200 | `SystemDictAdapter` 读 Feign-only | **代码 cutover ✅** |
| G-INF-01 | :48082 HTTP 200 | `LocalFileStorageService` 上传 Feign-only | **代码 cutover ✅** |
| G-MEM-03 | :48087 HTTP 200 | `MemberArticleWriteService` 写 Feign-only | **代码 cutover ✅** |
| G-MP-01 | :48086 HTTP 200 | `MpAccountDataService` Feign-only | **代码 cutover ✅** |
| G-PAY-01 | :48085 DOWN | `FootballOrderReadServiceImpl` Feign-only | **代码 cutover ✅** · Integration **阻塞** |

详细记录：[CUTOVER-20260729-PHASE-C.md](./e2e-artifacts/CUTOVER-20260729-PHASE-C.md)

---

## 5. cutover 签字

**2026-07-29 Phase C cutover 执行**（OPS 生产路径 Feign-only；@DS 回退已移除）

| 签字项 | 日期 | 签字人 | 备注 |
|--------|------|--------|------|
| G-SYS-01/02 cutover | 2026-07-29 | AI cutover | RPC :48081 200；Validator Feign-only |
| G-DICT-01 cutover | 2026-07-29 | AI cutover | list/valid Feign-only；admin 写仍 @DS |
| G-INF-01 cutover | 2026-07-29 | AI cutover | 上传 Feign-only；legacy key 本地读保留 |
| G-PAY-01 cutover | 2026-07-29 | AI cutover | Feign-only；pay DS 已删；:48085 手验待 pay-server |
| G-MEM-03 cutover | 2026-07-29 | AI cutover | 写 Feign-only；getById 仍 @DS member |
| G-MP-01 cutover | 2026-07-29 | AI cutover | MpAccountDataService Feign-only；IN 分页抛错 |

### 5.1 Integration RPC 抽检（2026-07-29）

| G-* | RPC 抽检 | 结果 | 备注 |
|-----|----------|------|------|
| G-SYS-01 | `GET :48081/.../simple-list` | **HTTP 200** | |
| G-DICT-01 | `GET :48081/.../dict-data/list?dictType=test` | **HTTP 200** | |
| G-INF-01 | `GET :48082/.../presigned-url?path=test` | **HTTP 200** | |
| G-MP-01 | `GET :48086/.../accountInfo/page` | **HTTP 200** | |
| G-MEM-03 | `POST :48087/.../article/create` | **HTTP 200** | |
| G-PAY-01 | `POST :48085/.../order/page` | **HTTP 000** | pay-server 未启动；G-PAY-01 代码 cutover 已完成 |
| G-SYS-02 | OPS 业务步骤 | **待手验** | Feign-only 已落地 |

**未 cutover（文档化原因）**

| 项 | 原因 |
|----|------|
| C-WP1 FootballAuthProvider token @DS | 需 Gateway/check 切轨（D-SYS-03） |
| MemberAuthorReadService | 无 Feign 读路径 |
| MpUserDataService | 无 Feign |
| SystemDictAdapter admin CRUD / typeExists | 平行字典管理 deprecated；仍 @DS |
| FootballSystemUserValidator loadNicknames / resolveRoleIdByCode | roleId 映射与昵称批量读仍需 system @DS |
| DevAuth / H2 legacy sys_user 桥接 | master DS；非 Football 库直连业务路径 |

---

**版本** v1.0 · **日期** 2026-07-28
