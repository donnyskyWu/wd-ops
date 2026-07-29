# OPS × Football RPC：仅必须项（Must-Have）

| 字段 | 值 |
|------|---|
| 文档性质 | **精简交付清单**（仅 must-have；非完整分析） |
| 版本 | v1.0 |
| 日期 | 2026-07-23 |
| 决策 SSOT | [OPS-FOOTBALL-FULL-MERGE-RPC-ANALYSIS.md](./OPS-FOOTBALL-FULL-MERGE-RPC-ANALYSIS.md) **v1.8**（完整分析归档） |
| 清理清单 | [OPS-FOOTBALL-MERGE-CLEANUP-INVENTORY.md](./OPS-FOOTBALL-MERGE-CLEANUP-INVENTORY.md)（合并后去冗余） |
| **执行工作计划** | **[OPS-FOOTBALL-MERGE-WORK-PLAN.md](./OPS-FOOTBALL-MERGE-WORK-PLAN.md)**（v1.0 · 2026-07-24）— **Phase A 前端 → B 数据库 → C 后端** |
| 状态 | Draft for Review — §7 接口说明书为 **提案（待 Football 评审）**，未实现 |

> 本文只保留已拍板后的 **必须做** 项。已裁剪 / Out of Scope 见文末「明确不做」；细节与历史对照见完整分析归档。  
> **§7 实现状态以 `football-backend-saas` `ops` 分支实码为准**；2026-07-28 审计见 [WORK-PLAN §8.7](./OPS-FOOTBALL-MERGE-WORK-PLAN.md#87-football-rpc-实码审计2026-07-28)。

---

## 1. 原则结论

| 项 | 内容 |
|----|------|
| **原则** | OPS **只访问 `wd`**；其他库（system / member / mp / pay 等）一律通过 **Football 模块 API**（RPC/Feign），禁止 `@DS` 直连 |
| **去重** | **D-DEDUP-01**：Football 为 SSOT；禁止在 OPS 平行建设已有管理能力；只保留 OPS 独有业务 must-have |
| **结论** | **可以达到，但不是现状**。需：① Football 补齐裁剪后缺口 API；② OPS 去掉跨库 `@DS` 改走 Feign；③ 调整 / Supersede **ADR-050** §3.1 |

---

## 2. 拍板摘要

| ID | 事项 | 决议 |
|----|------|------|
| **D-INF-01** | 文件上传 / 存储 | **统一接入** Football `FileApi` / `/admin-api/infra/file`；淘汰 OPS 本地盘长期方案 |
| **D-AUTHOR-01** | 作者主数据 | OPS **只读不写**（get/list/simple-list + `authorLevel`）；CUD 归 Football Admin |
| **D-DING-02** | 钉钉通讯录同步 | **不做** Feign；OPS 不维护人员/部门。**G-DING-01 通用推送仍保留** |
| **D-DEDUP-01** | 不重复建设 | 已有 Admin/RPC **不**平行实现；用户/部门/菜单/字典管理与操作日志读 UI **不下沉 OPS** |
| **D-SYS-03** | Token 可信来源 | **不新建** introspect；优先 Gateway login-user 透传；必要时复用 `OAuth2TokenCommonApi.checkAccessToken` |

---

## 3. OPS 需要 Football 的能力（仅 must-have）

| 域 | OPS 用途 |
|----|----------|
| 用户 / 鉴权 | Gateway/`check`→LoginUser；UserSelect；租户内启用 / 角色校验（ADR-056） |
| 字典 | 后端 `@InDict` / 薄封装读（管理 UI 不下沉） |
| 文件 | 上传 / 预览 / 下载（统一 `FileApi`） |
| 操作日志 | **写**已接 Feign（读走 Football 原生菜单，不平行） |
| 作者 / 等级 | **只读** + **authorLevel**（不写） |
| 文章 / 方案 | 内容生产同步：create / update / 上下架 |
| 公众号 | 账号 page / create / update + 读（+ `oa_account_ext`） |
| 订单 | 运营只读列表 / 归因字段 |
| 钉钉 | 通用工作通知（≠ MP 告警；通讯录同步不做） |

---

## 4. Football 已满足 / 可复用（不用新建）

| 能力 | 形态 | 备注 |
|------|------|------|
| Token 校验 | Gateway 透传 + `OAuth2TokenCommonApi.checkAccessToken` | **D-SYS-03**：不新建 introspect |
| 操作日志写 | Feign `OperateLogCommonApi` | OPS **已接入** |
| 操作 / 登录日志读 UI | Football Admin 菜单 | OPS **不平行**读页 |
| 用户 / 部门 / 菜单 / 字典**管理** | Admin + 菜单 | **不下沉 OPS** |
| 字典读（前端） | Admin `/system/dict-*` | 前端可直调；后端校验仍要 Feign |
| 文件存储 | `FileApi` + `/infra/file` | **API 已有**；OPS 未切轨 |
| 用户 get / list / valid | `AdminUserApi` | **缺**后端 simple-list Feign |
| 作者 / 文章 / 公众号读子集 | Author / Article / Mp Feign | 作者只读；文章/公众号**写**仍缺 |
| 订单统计类 | `PayOrderApi` | 列表字段对齐未证实 |
| 钉钉 MP 告警 | `MpMessageApi` | ≠ 通用运营推送 |

---

## 5. Football 仍需支持的缺口 + 人日粗估

> 人日为 Football 侧粗估（规划用），**不含** OPS 切轨与联调。是否做 **G-DING-01** 主导上限。

| 缺口 | 难度 | 人日 | 说明 |
|------|------|------|------|
| G-SYS-01 用户 simple-list Feign（**后端**） | S | 1–2 | 前端可走 Admin；后端 IP 组等必须 Feign |
| G-SYS-02 用户 / 角色校验改 RPC | S–M | 2–4 | 扩现有 Api |
| G-DICT-01 字典 list-by-type 契约（**后端**） | S | 1–3 | 有 `DictDataApi`，或需扩 `sort` 等 |
| G-INF-01 文件契约 / 租户对齐 | S | 0–2 | API 已有 |
| G-MEM-01 作者等级进 DTO | S | 0.5–1 | 读 DTO 暴露 |
| G-MEM-02 作者只读 Feign | S | 1–2 | get/list/simple-list；**不含** CUD |
| G-MEM-03 文章写 Feign | M | 3–6 | Admin→Feign |
| G-MP-01 公众号 page / 写 Feign | M | 2–4 | Admin→Feign |
| G-PAY-01 订单运营列表对齐 | S–M | 2–5 | 对表后复用或薄封装 |
| G-DING-01 通用钉钉推送 API | L | 8–15 | 新建 SSOT |
| **合计（Football）** | — | **约 16–38** | Token introspect / 作者 CUD / 通讯录 / 日志读平行 **不计** |

---

## 6. OPS 自建保留域

IP 组、SOP / 任务 / 绩效、`sys_param`、`oa_*` / `oa_*_ext`、非微信账号等运营自有能力继续落 **`wd`**，以 Football 主键做关联即可。

---

## 7. 必须做的接口说明书

> **状态**：提案（待 Football 评审），**未实现**。  
> **惯例**：前缀 `/rpc-api`；包装 `CommonResult<T>`；分页 `PageResult<T>`；头 `tenant-id`；雪花 id 建议字符串（ADR-056）。

### 7.0 Token（复用，不写新 introspect 规格）

| 项 | 内容 |
|----|------|
| 决议 | **D-SYS-03**：不新建 `/rpc-api/.../token/introspect` |
| 推荐默认 | Gateway 已鉴权后向下游透传 login-user（`user-id` / `tenant-id` / 权限上下文） |
| 备选 | 复用已有 `OAuth2TokenCommonApi.checkAccessToken` → `GET /rpc-api/system/oauth2/token/check?accessToken=` |
| OPS 动作 | `FootballAuthProvider` 消费 Gateway 头或调用 `check`；终态停止 `@DS("system")` 直读 token 表 |

---

### 7.1 G-SYS-01 用户 simple-list Feign（后端必须）

> - **浏览器 / UserSelect**：可直调 `GET /admin-api/system/user/simple-list`，无需 Feign。  
> - **oa-server 后端**（IP 组候选等）：**必须**本 Feign。

| 项 | 内容 |
|----|------|
| 服务 | `system-server` |
| Feign | 扩展 `AdminUserApi` |
| Path | `GET /rpc-api/system/user/simple-list` |
| 对齐 | Admin `UserController#getSimpleUserList`（默认 `status=ENABLE`） |

**入参**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| （Header）`tenant-id` | Long | 是 | |
| `keyword` | String | 否 | 昵称模糊（扩展点） |
| `status` | Integer | 否 | 默认仅启用 |
| `deptId` | Long | 否 | 部门过滤（可选） |

**出参** `CommonResult<List<AdminUserSimpleRespDTO>>`：`id, nickname, deptId, deptName`；建议含 `status`。

```http
GET /rpc-api/system/user/simple-list?keyword=张
tenant-id: 1
```

```json
{ "code": 0, "data": [{ "id": "1024", "nickname": "张三", "deptId": 10, "deptName": "运营部", "status": 0 }], "msg": "" }
```

**开放问题**：是否需「忽略数据权限、仅按租户返回全量启用用户」开关（IP 组场景）。

---

### 7.2 G-SYS-02 用户 / 角色校验 RPC

| 项 | 内容 |
|----|------|
| 服务 | `system-server` |
| Feign | 扩展 `AdminUserApi` + `PermissionApi`；对齐 ADR-056 |

#### 7.2.1 用户在租户内且启用

`GET /rpc-api/system/user/assert-enabled?id=` + `tenant-id` 头  
**响应** `CommonResult<Boolean>`（失败抛业务错误，OPS 可映射 1501/1504）

#### 7.2.2 是否拥有角色 code

优先复用 `PermissionCommonApi.hasAnyRoles`：  
`GET /rpc-api/system/permission/has-any-roles?userId=&roles=` → `CommonResult<Boolean>`

#### 7.2.3 （建议）按角色 code 列用户 id

`GET /rpc-api/system/permission/user-ids-by-role-code?roleCode=` → `CommonResult<List<Long>>`  
（现有按 roleId 不足时新建）

**备注**：`resolveStorableUserId` / `resolvePresentableUserId` 可留 OPS；Football 只保证 Football id 的租户/启用/角色校验。

---

### 7.3 G-DICT-01 字典 list-by-type / @InDict RPC

> - **浏览器 DictSelect**：可直调 Admin `GET /admin-api/system/dict-data/type`。  
> - **后端 `@InDict`**：必须服务端 RPC，不能用前端 Admin 代替。

| 项 | 内容 |
|----|------|
| 服务 | `system-server` |
| Feign | **已有** `DictDataApi`；补齐 DTO |
| Path | `GET /rpc-api/system/dict-data/list?dictType=`；校验 `GET /rpc-api/system/dict-data/valid` |

**扩展 `DictDataRespDTO`**：`label, value, dictType, status`（Integer）+ **`sort`（新增）**

```http
GET /rpc-api/system/dict-data/list?dictType=dict_platform_type
```

```json
{ "code": 0, "data": [{ "dictType": "dict_platform_type", "label": "微信公众号", "value": "wechat_mp", "status": 0, "sort": 1 }], "msg": "" }
```

```http
GET /rpc-api/system/dict-data/valid?dictType=dict_platform_type&values=wechat_mp
```

---

### 7.4 G-INF-01 FileApi 契约（D-INF-01）

> API **已存在**；Football 侧重契约/租户对齐；OPS 切轨淘汰本地盘。

| 项 | 内容 |
|----|------|
| 服务 | `infra-server` |
| Feign | 已有 `FileApi` |

| 能力 | Path | 说明 |
|------|------|------|
| 创建（bytes） | `POST /rpc-api/infra/file/create` | Body：`name, directory, type, content` → `CommonResult<String>` url/path |
| 预签名读 | `GET /rpc-api/infra/file/presigned-url?url=&expirationSeconds=` | `CommonResult<String>` |
| 前端上传（推荐） | `POST /admin-api/infra/file/upload` 等 | multipart / 预签名；见 Admin |

**OPS 约定**：业务表存 infra 返回的 url/path；过渡期 `/oa/file/upload` 可代理→`FileApi`；终态下线本地流预览。

---

### 7.5 G-MEM-01 / G-MEM-02 作者只读 + authorLevel

#### G-MEM-01：DTO 暴露 `authorLevel`

| 字段 | 类型 | 说明 |
|------|------|------|
| authorLevel | Integer | `0`=作者 / `1`=专家；无新 path，读 DTO 自动带上 |

#### G-MEM-02：只读 Feign（扩展 `AuthorApi`）

| 能力 | Path | 响应 |
|------|------|------|
| 按 id | `GET /rpc-api/member/author/getAuthor?id=` | `AuthorSimpleRespDTO`（含 level） |
| 批量 ids | `GET/POST .../list-by-ids` | `List<AuthorSimpleRespDTO>` |
| simple-list | `GET .../simple-list` | 筛：`nickname, status, authorLevel` |
| page（若只读页需要） | `GET .../page` | `PageResult<...>` |

**明确不纳入 OPS**：create / update / updateStatus / delete（D-AUTHOR-01）。运营扩展写 `oa_author_ext`。

---

### 7.6 G-MEM-03 文章写 Feign

| 项 | 内容 |
|----|------|
| 服务 | `member-server` |
| Feign | 扩展 `ArticleApi` |
| 对齐 | Admin `/create` `/update` `/status-change` |

| 能力 | Path | 要点 |
|------|------|------|
| 创建 | `POST /rpc-api/member/article/create` | 最小集：`authorId, title, content, freeContent?, status(-1 草稿), price, privilegeTypes, refundType, matchType, schedulePublishStatus` → `CommonResult<Long>` |
| 更新 | `POST .../update` | `id` 必填；`freeContent` 仅非 null 时覆盖（ADR-054） |
| 上下架 | `POST .../status-change` | `{ id, status }`：`0` 下架 / `1` 上架 → `Boolean` |

```json
{
  "authorId": "1001",
  "title": "周末竞足方案",
  "content": "<p>付费内容</p>",
  "status": -1,
  "price": 88.00,
  "privilegeTypes": [2],
  "refundType": 0,
  "matchType": 1,
  "schedulePublishStatus": 0
}
```

---

### 7.7 G-MP-01 公众号 page / 写 Feign

| 项 | 内容 |
|----|------|
| 服务 | `mp-server` |
| Feign | 扩展 `MpAccountInfoApi`（或 `MpAccountAdminApi`） |

| 能力 | Path |
|------|------|
| 分页 | `GET/POST /rpc-api/mp/account/page`（`name, account, appId, authorId, status...`） |
| 创建 | `POST .../create`：`name, account, appId, appSecret, token` 等 → `Long` id |
| 更新 | `POST .../update`：Create 字段 + `id` |
| 详情 | `GET .../get?id=` |

**不进本 API**：`oa_account_ext`（公司/实名/手机/管理员等）。

---

### 7.8 G-PAY-01 订单运营列表

| 项 | 内容 |
|----|------|
| 服务 | `pay-server` |
| Feign | 扩展 `PayOrderApi`（如 `pageForOps`）；若现有方法已 100% 覆盖则文档化复用、可不新建 |
| Path | `POST /rpc-api/pay/order/page-for-ops` |

**入参**：`startTime, endTime`（必填）、`authorId?, status?, pageNo, pageSize` + `tenant-id`  
**出参字段**：`id, orderNo, userId, authorId, amount, payAmount, status, orderType, payTime, createTime`（只读；归因仍在 wd）

---

### 7.9 G-DING-01 通用钉钉推送

| 项 | 内容 |
|----|------|
| 服务 | **`system-server`（推荐 SSOT）** |
| Feign | **新建** `DingTalkMessageApi` |
| Path | `POST /rpc-api/system/dingtalk/message/send` |
| 勿复用 | `MpMessageApi.sendDingMessage`（公众号告警维度） |

**入参** `DingTalkMessageSendReqDTO`：`userId` 与 `dingUserId` 二选一；`title, content` 必填；`contentType` 默认 `markdown`  
**响应** `CommonResult<Boolean>`（或带 msgId）

```json
{ "userId": "1024", "title": "任务提醒", "content": "## 待办\n请处理 IP 组审核", "contentType": "markdown" }
```

---

## 8. 明确不做（不展开接口）

| 项 | 决议 |
|----|------|
| Token Introspect **新建** | **D-SYS-03**：复用 Gateway / `check` |
| 钉钉通讯录同步 Feign | **D-DING-02** |
| 作者 CUD Feign | **D-AUTHOR-01**（管理归 Football Admin） |
| 操作日志**读**平行页 / 平行分页 API | **D-DEDUP-01**：挂 Football 原生菜单 |
| 用户 / 部门 / 菜单 / 字典**管理**下沉 OPS | **D-DEDUP-01**：一律 Football Admin |

---

**关联**：完整分析与已裁剪项说明 → [OPS-FOOTBALL-FULL-MERGE-RPC-ANALYSIS.md](./OPS-FOOTBALL-FULL-MERGE-RPC-ANALYSIS.md)  
**合并后去冗余清理** → [OPS-FOOTBALL-MERGE-CLEANUP-INVENTORY.md](./OPS-FOOTBALL-MERGE-CLEANUP-INVENTORY.md)  
**团队执行顺序（A 前端 → B 库 → C 后端）** → [OPS-FOOTBALL-MERGE-WORK-PLAN.md](./OPS-FOOTBALL-MERGE-WORK-PLAN.md)
