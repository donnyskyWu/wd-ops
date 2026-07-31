# OPS × Football Integration Feign 手验清单

| 字段 | 值 |
|------|---|
| 文档性质 | **Integration 手验记录模板**（G-* Feign 双跑 / cutover 验收） |
| 版本 | v1.1 |
| 日期 | 2026-07-30 |
| 关联 | [WORK-PLAN §8.6](./OPS-FOOTBALL-MERGE-WORK-PLAN.md#86-阻塞表blockers) · [D-FEIGN-IT](./OPS-FOOTBALL-MERGE-DECISIONS.md#d-feign-ith2-it-与-feign-路径验证策略86-b-feign-it) · [ADR-057](../adr/ADR-057-G-PAY-01-page-for-ops.md) · [OPS-DEV-DEPLOY-GUIDE](./OPS-DEV-DEPLOY-GUIDE.md) |
| IT 边界 | H2 `mvn verify` **不**断言 Feign 真调通；Feign 路径以本文 Integration 手验为准 |
| **Phase C 整包** | **GO**（localhost）— G-* 手验 Pass 7/0 ✅；**B-DS-RESIDUE ✅**；**C-WP7-PHYS 代码 ✅**；**B-WP4-ARCHIVE ✅** 2026-07-31（[REPORT](./e2e-artifacts/B-WP4-ARCHIVE-20260731/REPORT.md)） |

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
| **Feign 路径** | `POST http://127.0.0.1:48085/rpc-api/pay/order/page-for-ops` · Body: `OrderOpsPageReqDTO`（`startTime`/`endTime` 半开区间） |
| **OPS 消费** | `FootballOrderReadService` · `PayOrderApi.pageForOps`（ADR-057 / D-G-PAY-01 REV1 假设 B） |
| **@DS 回退** | 已 cutover Feign-only（pay DS 已删） |
| **前置** | **pay-server :48085 必须 UP**；`application-dev-nacos-local.yml` 已配置 `pay-server.url` |
| **手验步骤** | 1) `POST .../pay/order/page-for-ops` + `tenant-id` + 时间窗 → code=0；2) Gateway `football-order/list?startDate&endDate` + admin token → code=0；3) 不依赖 Admin `getOrderPage` 富化 |
| **期望** | OPS 10 列；tenant-id 隔离；无 permitted-ids / finance_channel 富化 |

### G-MEM-03　文章写

| 项 | 内容 |
|----|------|
| **Feign 路径** | `POST http://127.0.0.1:48087/rpc-api/member/article/create` · `PUT .../update` · `POST .../status-change` |
| **OPS 消费** | `MemberArticleWriteService` |
| **@DS 回退** | **已清**（C-WP7 getById Slice 2026-07-30：删 `AuthorArticleMapper` / member DS；`shelfStatus` 无 get RPC 降级为空） |
| **手验步骤** | 1) 内容生产同步创建文章；2) 更新标题/状态；3) Football Admin 侧可见 |
| **期望** | 写路径 Feign 成功；列表 `authorArticleId`/`footballSyncError` 可用；`shelfStatus` 可空 |

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
| 2026-07-30 | Integration localhost（无 -Beta） | AI hand-verify | G-SYS-01 | `GET :48081/.../simple-list` → code=0 n=65 | IP 组 member-candidates n=62 | **Pass** | 见 [G-STAR-HANDVERIFY-20260730](./e2e-artifacts/G-STAR-HANDVERIFY-20260730/REPORT.md) |
| | | | G-SYS-02 | `has-any-roles` code=0 | IP 组 update code=0；非法 leader→1004；无角色→1500 | **Pass** | Feign 角色校验生效 |
| | | | G-DICT-01 | `dict-data/list` dict_ip_group_level code=0 | 非法枚举 → **1503** | **Pass** | 非 500 |
| | | | G-INF-01 | infra :48082 UP | 内容配图上传 → upload.shenyu.com | **Pass** | |
| | | | G-PAY-01 | `POST :48085/.../order/page-for-ops` + tenant-id → **code=0** total≈183485 | `football-order/list` → **code=0** total≈183485 | **Pass** | ADR-057 假设 B；见 [G-PAY-01-FIX](./e2e-artifacts/G-STAR-HANDVERIFY-20260730/G-PAY-01-FIX.md) |
| | | | G-MEM-03 | member :48087 UP | content 36 → article 1000319，syncError=null | **Pass** | |
| | | | G-MP-01 | accountInfo/page total=166；followers RPC total=13 | account/list=182；followers OPS total=13 | **Pass** | 会话修 MpUserDTO epoch millis |
| | | | G-DING | — | — | 跳过 | 延期 |

**2026-07-30 业务手验结论**：Pass **7** / Fail **0** / Skip G-DING。G-PAY-01 已按假设 B 切 `page-for-ops`（ADR-057）— [G-PAY-01-FIX.md](./e2e-artifacts/G-STAR-HANDVERIFY-20260730/G-PAY-01-FIX.md)。详细证据：[G-STAR-HANDVERIFY-20260730/REPORT.md](./e2e-artifacts/G-STAR-HANDVERIFY-20260730/REPORT.md)。

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

**结论（当时）**：Phase C cutover 2026-07-29 — OPS 业务域 Feign-only + fail-fast；multidb 已移除 member/mp/pay。~~**G-PAY-01 Integration 手验仍阻塞**（pay-server 未启）~~ — **已废止**（2026-07-30 手验 Pass；见 §3 / [G-STAR-HANDVERIFY-20260730](./e2e-artifacts/G-STAR-HANDVERIFY-20260730/REPORT.md)）。

---

## 6. 2026-07-29 cutover 抽检

> 环境：`start-integration-all` 终态（system/infra/mp/member/oa/gateway UP）；**pay-server 未启动**（当日）。**业务手验已于 2026-07-30 补齐**（§3）。

| G-* | RPC / Health 抽检 | cutover 代码 | 结果 |
|-----|-------------------|--------------|------|
| G-SYS-01/02 | :48081 HTTP 200 | `FootballSystemUserValidator` Feign-only | **代码 cutover ✅** · **业务手验 Pass**（2026-07-30） |
| G-DICT-01 | :48081 HTTP 200 | `SystemDictAdapter` 读 Feign-only | **代码 cutover ✅** · **业务手验 Pass** |
| G-INF-01 | :48082 HTTP 200 | `LocalFileStorageService` 上传 Feign-only | **代码 cutover ✅** · **业务手验 Pass** |
| G-MEM-03 | :48087 HTTP 200 | `MemberArticleWriteService` 写 Feign-only | **代码 cutover ✅** · **业务手验 Pass** |
| G-MP-01 | :48086 HTTP 200 | `MpAccountDataService` Feign-only | **代码 cutover ✅** · **业务手验 Pass**（含 MpUser epoch 修复） |
| G-PAY-01 | :48085 DOWN（当日） | `FootballOrderReadServiceImpl` Feign-only | **代码 cutover ✅** · ~~Integration 阻塞~~ → **2026-07-30 Pass**（ADR-057 `page-for-ops`） |

详细记录：[CUTOVER-20260729-PHASE-C.md](./e2e-artifacts/CUTOVER-20260729-PHASE-C.md) · 业务签字：[G-STAR-HANDVERIFY-20260730/REPORT.md](./e2e-artifacts/G-STAR-HANDVERIFY-20260730/REPORT.md)

---

## 5. cutover 签字

**2026-07-29 Phase C cutover 执行**（OPS 生产路径 Feign-only；@DS 回退已移除）

| 签字项 | 日期 | 签字人 | 备注 |
|--------|------|--------|------|
| G-SYS-01/02 cutover | 2026-07-29 | AI cutover | RPC :48081 200；Validator Feign-only |
| G-DICT-01 cutover | 2026-07-29 | AI cutover | list/valid Feign-only |
| B-DS-RESIDUE | 2026-07-30 | AI | nickname=`getByIds`；roleCode→master；dict admin/types=410；删 Lookup/Dict Mapper/SystemReader；unit+烟测 Pass |
| G-INF-01 cutover | 2026-07-29 | AI cutover | 上传 Feign-only；legacy key 本地读保留 |
| G-PAY-01 cutover | 2026-07-29 | AI cutover | Feign-only；pay DS 已删 |
| G-PAY-01 业务手验 | 2026-07-30 | AI hand-verify | **Pass** · ADR-057 `page-for-ops`；见 [G-PAY-01-FIX](./e2e-artifacts/G-STAR-HANDVERIFY-20260730/G-PAY-01-FIX.md) |
| G-MEM-03 cutover | 2026-07-29 | AI cutover | 写 Feign-only |
| G-MEM-03 getById 清除 | 2026-07-30 | AI C-WP7 Slice | 删 `getById`/`AuthorArticleMapper`/member DS；shelfStatus 降级；MUST-HAVE 无 article get RPC |
| G-MEM-03 业务手验 | 2026-07-30 | AI hand-verify | **Pass** · content→article syncError=null |
| G-MP-01 cutover | 2026-07-29 | AI cutover | MpAccountDataService Feign-only；IN 分页抛错 |
| G-MP-01 业务手验 | 2026-07-30 | AI hand-verify | **Pass** · followers total 对齐；`MpUserDTO` epoch millis |

### 5.1 Integration RPC 抽检（2026-07-29）

| G-* | RPC 抽检 | 结果 | 备注 |
|-----|----------|------|------|
| G-SYS-01 | `GET :48081/.../simple-list` | **HTTP 200** | |
| G-DICT-01 | `GET :48081/.../dict-data/list?dictType=test` | **HTTP 200** | |
| G-INF-01 | `GET :48082/.../presigned-url?path=test` | **HTTP 200** | |
| G-MP-01 | `GET :48086/.../accountInfo/page` | **HTTP 200** | |
| G-MEM-03 | `POST :48087/.../article/create` | **HTTP 200** | |
| G-PAY-01 | `POST :48085/.../order/page` | **HTTP 200 · code 500**（当日） | Admin `getOrderPage` 富化路径；~~业务阻塞~~ → **废止**，改 `page-for-ops`（ADR-057 · 2026-07-30 Pass） |
| G-SYS-02 | `has-any-roles` / `simple-list` | **Pass（RPC）** | ~~IP 组保存待手验~~ → **2026-07-30 业务 Pass**；见 [G-PAY-G-SYS-20260729.md](./e2e-artifacts/G-PAY-G-SYS-20260729.md) |

### 5.2 MpUser 粉丝列表 Feign 手验（2026-07-29）

| 项 | RPC / OPS | accountId | total | 结果 |
|----|-----------|-----------|-------|------|
| RPC | `GET :48086/rpc-api/mp/mpUser/getUserPageByAccount?accountId=1000002&pageNo=1&pageSize=10` + `tenant-id:1` | mp_account **1000002** | **13** | **Pass** |
| OPS | `GET :48080/admin-api/oa/account/1000002/mp-followers?pageNo=1&pageSize=10` + dev-token | OPS 账号 **1000002** | **13** | **Pass** |
| 负例 | `GET .../account/9006/mp-followers` | 非公众号 | — | **Pass**（1500） |

详细记录：[MP-USER-FEIGN-20260729.md](./e2e-artifacts/MP-USER-FEIGN-20260729.md)

**未 cutover（文档化原因）**

| 项 | 原因 |
|----|------|
| ~~C-WP1 FootballAuthProvider token @DS~~ | **cutover + PHYS**：`GatewayAuthProvider`；TokenMapper/Redis/`FootballAuthProvider` **已删** |
| MemberAuthorReadService | 无 Feign 读路径（部分已 AuthorApi） |
| MpUserDataService | ~~无 Feign~~ → **2026-07-29 已 cutover** | 见 [MP-USER-FEIGN-20260729.md](./e2e-artifacts/MP-USER-FEIGN-20260729.md) |
| SystemDictAdapter admin CRUD / typeExists | 平行字典管理 deprecated → **410**（B-DS-RESIDUE）；无 Mapper |
| FootballSystemUserValidator loadNicknames / resolveRoleIdByCode | **B-DS-RESIDUE**：nickname=`getByIds`；roleCode→roleId=wd master |
| DevAuth / H2 legacy sys_user 桥接 | **C-WP7-PHYS**：生产 `dev-token.enabled=false`；仅 `application-test.yml` 开启 |
| C-WP0 平行 User/Role/Dept / 钉钉 sync / 作者 CUD | **✅ 业务码 410**（2026-07-30）；IT：`M9UserRoleS01IT`/`M9DeptS01IT`/`M1AuthorS04IT` |

### 5.3 C-WP1 鉴权切轨手验（2026-07-30）

| 项 | 结果 | 备注 |
|----|------|------|
| Gateway login | **Pass** | `POST :48080/.../auth/login` → accessToken |
| OA via Gateway | **Pass** | `GET .../oa/content/review-config` code=0；`.../oa/ip-group/list` code=0 |
| OA direct check fallback | **Pass** | 直连 `:48094` + Bearer（无 login-user）→ `checkAccessToken` code=0 |
| login-user / check expiresTime | **Pass** | epoch millis（DTO `Long`）；unit test 绿 |
| legacy-ds-token | **已移除** | C-WP7-PHYS 删配置与类；不可紧急回滚 |
| checkAccessToken Feign URL | **已配** | `oauth2TokenCommonApi` → `:48081` |
| C-WP7 getById→member DS | ✅ | `MemberArticleWriteService#getById` / `AuthorArticleMapper` / member DS 已删 |
| C-WP7-PHYS | ✅ 代码 | TokenMapper/Redis/`FootballAuthProvider`/死 `MpAccountMapper`/`SystemDsSmokeMapper` 已删；dev-token 仅 IT |
| C-WP7 / B-WP4 | ✅ | 表归档 localhost 签收执行 + 探测（[B-WP4-ARCHIVE-20260731](./e2e-artifacts/B-WP4-ARCHIVE-20260731/REPORT.md)） |

### 5.4 Phase C 整包 Gate 状态（2026-07-31 · B-WP4-ARCHIVE）

| 项 | 状态 |
|----|------|
| G-* Integration 业务手验（除 G-DING） | ✅ Pass 7 / Fail 0 / Skip 1 — [REPORT](./e2e-artifacts/G-STAR-HANDVERIFY-20260730/REPORT.md) |
| C-WP0 410 / C-WP1 鉴权 / C-WP7 getById | ✅ |
| 无生产路径 `@DS("system\|member\|mp\|pay")` | ✅ **满足**（main 源无上述 `@DS`；master overlay 保留） |
| C-WP7-PHYS 代码（dev-token 下线 · Mapper 物理删） | ✅ |
| B-WP4 表归档 | ✅ **localhost 完成**（签收 + 备份 + 停写 trigger + `sys_operation_log`→`archive_wd` + 探测） |
| **Phase C 整包** | **GO**（Integration localhost；远程/生产归档另窗） |

---

**版本** v1.1 · **日期** 2026-07-31
