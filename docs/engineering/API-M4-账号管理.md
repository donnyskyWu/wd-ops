# API-M4-账号管理

> **版本**：v1.0 | 2026-06-07
> **关联 PRD**：[`PRD-M4-账号管理.md`](../product/PRD-M4-账号管理.md)
> **关联 UX**：[`UX-M4-账号管理.md`](../product/UX-M4-账号管理.md)
> **关联全局规范**：[`GLOBAL-CONVENTIONS.md`](./GLOBAL-CONVENTIONS.md)
> **🔴 核心模块**：强关联 + 数据字典 + 加密

---

## 1. 公司管理 API

### 1.1 GET `/admin-api/oa/company/list`

### 1.2 POST `/admin-api/oa/company/create`

**请求体** `CompanyCreateReq`：

```json
{
  "companyName": "杭州某某传媒有限公司",
  "creditCode": "9133010（具体值详见相应章节）
  "industry": "传媒",
  "address": "...",
  "legalName": "李四",
  "legalIdCard": "330101199001011234",  // 后端 AES-256 加密
  "mpCapacityStandard": 50,
  "status": 1
}
```

**校验**：
- `companyName` `@NotBlank @Size(max=100)`
- `creditCode` `@NotBlank @Size(max=18)` + 18 位统一社会信用代码正则
- `legalIdCard` AES-256 加密
- `mpCapacityStandard` ≥ 0

### 1.3 POST `/admin-api/oa/company/{id}/expand`

**请求体**：

```json
{
  "newCapacity": 100,
  "reason": "业务扩张"
}
```

**业务**：
- `mp_capacity_standard = newCapacity`
- 追加到 `expansion_history`

### 1.4 GET `/admin-api/oa/company/{id}/mp-stats`

**响应**：

```json
{
  "companyId": 1,
  "capacity": 50,
  "registered": 30,
  "remaining": 20,
  "warning": false,
  "details": [
    {"accountName": "（具体值详见相应章节）
  ]
}
```

---

## 2. 实名人管理 API

### 2.1 GET `/admin-api/oa/internal/realname/list`

**响应字段脱敏**（视角色）：
- 身份证：中间 4 位 → `****`
- 手机：中间 4 位 → `****`

### 2.2 POST `/admin-api/oa/internal/realname/create`

**请求体**：

```json
{
  "realName": "张三",
  "idType": "ID_CARD",
  "idCard": "330101199001011234",
  "phone": "13800001111",
  "wechat": "zhangsan_wx",
  "gender": "MALE"
}
```

**校验**：
- `idType` `@InDict(type="dict_id_type")`
- `gender` `@InDict(type="dict_gender")`
- `idCard` 18 位身份证正则 + AES-256 加密

### 2.3 GET `/admin-api/oa/internal/realname/{id}/intermediaries`

### 2.4 POST `/admin-api/oa/internal/realname/{id}/intermediary`

**请求体**：

```json
{
  "intermediaryName": "李中介",
  "intermediaryPhone": "13900000000",
  "intermediaryWechat": "li_zj",
  "relationType": "INTERMEDIARY",
  "commissionRate": 10.0,
  "remark": "..."
}
```

**校验**：
- `relationType` `@InDict(type="dict_intermediary_relation")`
- `commissionRate` 0-100

### 2.5 DELETE `/admin-api/oa/internal/realname/{id}`

**业务**：
- 若被账号引用 → 错误码 1502

---

## 3. 手机管理 API

### 3.1 GET `/admin-api/oa/internal/phone/list`

### 3.2 POST `/admin-api/oa/internal/phone/create`

**请求体**：

```json
{
  "phoneNumber": "13800001111",
  "phoneModel": "iPhone 15 Pro",
  "keeper": 123,
  "status": 1
}
```

**校验**：
- `phoneNumber` 11 位 + 全局唯一
- `keeper` `@NotNull`：必须从 `<UserSelect />` 选
- `status` `@InDict(type="dict_phone_status")`

---

## 4. 手机卡管理 API

### 4.1 GET `/admin-api/oa/internal/sim-card/list`

**响应**（含跨平台账号聚合字段）：

```json
{
  "id": 1,
  "phoneNumber": "13800001111",
  "isPrimary": 1,
  "operator": "MOBILE",
  "iccid": "****",
  "status": 1,
  "totalLinkedAccounts": 5,
  "wechatMpCount": 1,
  "videoAccountCount": 1,
  "douyinCount": 1,
  "kuaishouCount": 1,
  "weworkCount": 1
}
```

### 4.2 POST `/admin-api/oa/internal/sim-card/create`

**请求体**：

```json
{
  "phoneNumber": "13800001111",
  "isPrimary": 1,
  "operator": "MOBILE",
  "assignedUserId": 123,
  "iccid": "8986011...",
  "status": 1
}
```

**校验**：
- `phoneNumber` 11 位 + 全局唯一
- `isPrimary` `@InDict(type="dict_yes_no")`
- `operator` `@InDict(type="dict_sim_operator")`
- `assignedUserId` `@NotNull`
- `iccid` 加密

### 4.3 GET `/admin-api/oa/internal/sim-card/{id}/linked-accounts`（⭐ v9.1 新增）

**请求参数**：

| 参数 | 类型 | 必填 | 字典 |
|------|------|------|------|
| platformType | String | ❌ | `dict_platform_type` |
| operator | String | ❌ | `dict_sim_operator` |

**响应**：

```json
{
  "phoneNumber": "13800001111",
  "totalCount": 5,
  "accounts": [
    {
      "platformType": "WECHAT_OFFICIAL",
      "platformLabel": "公众号",
      "accountName": "（具体值详见相应章节）
      "accountId": "wx_xxx",
      "status": "NORMAL",
      "linkedAt": "2025-01-15T10:00:00+08:00"
    },
    {
      "platformType": "DOUYIN",
      "platformLabel": "抖音",
      ...
    }
  ]
}
```

**业务**：
- 通过 `phone_id` 关联 `oa_internal_account`
- 通过 `phone_number` 模糊匹配
- 性能：≤ 1s

---

## 5. 平台账号管理 API（⭐⭐）

### 5.1 GET `/admin-api/oa/internal/account/list`

**请求参数**：

| 参数 | 类型 | 必填 | 字典 |
|------|------|------|------|
| platformType | String | ❌ | `dict_platform_type` |
| accountType | String | ❌ | `dict_account_type` |
| ipGroupId | Long | ❌ | - |
| realnameId | Long | ❌ | - |
| companyId | Long | ❌ | - |
| status | String | ❌ | `dict_account_status` |

### 5.2 POST `/admin-api/oa/internal/account/create`（🔴 关键）

**请求体** `AccountCreateReq`：

```json
{
  "platformType": "WECHAT_OFFICIAL",
  "accountName": "（具体值详见相应章节）
  "accountId": "wx_xxx",
  "accountType": "OFFICIAL_ACCOUNT",
  "ipGroupId": 1,
  "companyId": 1,            // 🔴 强关联 ID
  "realnameId": 1,           // 🔴 强关联 ID
  "phoneId": 1,              // 🔴 强关联 ID
  "simCardId": 1,            // 🔴 强关联 ID
  "intermediaryId": 2,       // 🔴 强关联 ID（实名人表）
  "cookie": "...",
  "status": "NORMAL"
}
```

**🔴 强校验**（GLOBAL-CONVENTIONS § 1 铁律一 + 5.3）：

| 字段 | 校验 |
|------|------|
| `platformType` | `@InDict(type="dict_platform_type")` |
| `accountType` | `@InDict(type="dict_account_type")` |
| `accountId` | 同平台唯一（错误码 2006） |
| `ipGroupId` | 必须存在 + 启用 + 用户有权限 |
| `companyId` | 必须存在 + 启用（错误码 1500/1501） |
| `realnameId` | 必须存在 + 启用 + 跨租户校验（错误码 1504） |
| `phoneId` | 同上 |
| `simCardId` | 同上 |
| `intermediaryId` | 同上 |
| `status` | `@InDict(type="dict_account_status")` |
| `cookie` | AES-256 加密 |

**强关联选择器约束**（详见 GLOBAL-CONVENTIONS § 3.2）：

```
- 只能选 status=启用 的实体（停用/注销的不可选）→ 错误码 1501
- 跨租户隔离：只能选本租户的实体 → 错误码 1504
- 已绑定到其他账号的实名人/手机/手机卡：
  * 默认不可选
  * 需提供"forceReplace=true"参数
  * 强制替换时原账号自动解绑
```

#### 5.2.1 `forceReplace` 参数规范

| 参数 | 类型 | 默认值 | 说明 |
|------|------|-------|------|
| `forceReplace` | Boolean | `false` | 是否强制替换已绑定的实体 |
| `reason` | String | - | 强制替换时**必填**，长度 5-200，描述替换原因 |

**校验规则**：
- `forceReplace=true` 时 `reason` 必填 → 错误码 1500
- `reason` 长度 5-200
- 不满足 → 拒绝并返回错误码 1500

### 5.3 PUT `/admin-api/oa/internal/account/update`

同 create + `id` + 可选 `forceReplace`。

### 5.4 DELETE `/admin-api/oa/internal/account/{id}`

**业务**：
- 软删除（`status=0`）
- 保留 90 天后物理删除

### 5.5 POST `/admin-api/oa/internal/account/{id}/replace`

强制替换实名人/手机/手机卡：

**请求体**：

```json
{
  "realnameId": 5,
  "phoneId": 10,
  "simCardId": 8,
  "reason": "..."
}
```

**业务**：
- 当前账号绑定新实体
- 旧实体被释放
- 记录到 `sys_audit_log`

---



## 5.6 强关联选择器组件（🔴 必看）

所有强关联属性必须通过下面前端组件选择，**禁止手动输入**：

| 选择器 | 用途 | 关联字段 | 约束 |
|--------|------|----------|------|
| `<RealNameSelect />` | 选择实名人 | `realnameId`、`intermediaryId` | 启用+本租户+未停用 |
| `<PhoneSelect />` | 选择手机 | `phoneId` | 启用+本租户+未停用 |
| `<SimCardSelect />` | 选择手机卡 | `simCardId` | 启用+本租户+未停用 |
| `<CompanySelect />` | 选择公司 | `companyId` | 启用+本租户+未停用 |
| `<AccountSelect />` | 选择平台账号 | `accountId` | 同平台+启用+本租户 |

### 5.6.1 行为约束

1. **禁用手动输入**：input 框只读或仅下拉模式
2. **远端搜索**：输入 ≥ 1 字符触发搜索
3. **跨租户过滤**：自动只显示本租户的实体
4. **停用过滤**：自动过滤 `status=停用/注销` 的实体
5. **已绑定提示**：已被其他账号绑定的实名人/手机/手机卡 默认不可选；需"强制替换"

### 5.6.2 强制替换流程

1. 用户选择"已绑定"实名人 → 弹出"强制替换"确认框
2. 确认 → 调用 API 时传 `forceReplace=true` + `reason`
3. 后端 → 旧账号自动解绑，新账号绑定
4. 记录到 `sys_audit_log`

详见 [`GLOBAL-CONVENTIONS.md § 4.1`](../engineering/GLOBAL-CONVENTIONS.md)


## 6. 个人账号 API

### 6.1 GET `/admin-api/oa/internal/personal-account/list`

### 6.2 GET `/admin-api/oa/internal/personal-account/{id}`

**响应**（奥创接口脱敏）：

```json
{
  "id": 1,
  "accountName": "张三",
  "wechatId": "zhangsan_wx",
  "phoneId": 1,
  "apiUrl": "****",
  "appId": "****",
  "appSecret": "****",
  "token": "****"
}
```

### 6.3 POST `/admin-api/oa/internal/personal-account/api-config`

**请求体**：

```json
{
  "id": 1,
  "apiUrl": "https://api.aocang.com",
  "appId": "...",
  "appSecret": "...",
  "token": "..."
}
```

**业务**：仅系统管理员可编辑；其他角色只读。

### 6.4 GET `/admin-api/oa/internal/wework/list`

---

## 7. 三方关联 API

### 7.1 POST `/admin-api/oa/internal/triple-rel/create`

**请求体**：

```json
{
  "personalWechatId": 1,
  "videoAccountIds": [10, 11],
  "weworkAccountId": 20
}
```

### 7.2 GET `/admin-api/oa/internal/triple-rel/graph?userId=xxx`

**响应**：

```json
{
  "userId": 1,
  "personalWechat": {
    "id": 1,
    "wechatId": "zhangsan_wx"
  },
  "videoAccounts": [
    {"id": 10, "accountName": "video_001"},
    {"id": 11, "accountName": "video_002"}
  ],
  "weworkAccount": {
    "id": 20,
    "corpName": "某某公司"
  }
}
```

---

## 8. 错误码

| 错误码 | 含义 |
|--------|------|
| 1500 | 关联实体不存在 |
| 1501 | 关联实体已停用/注销 |
| 1502 | 关联实体已被引用 |
| 1503 | 字典值不合法 |
| 1504 | 跨租户访问禁止 |
| 2006 | 同平台 account_id 重复 |
| 2014 | 强制替换需 reason |

---

*下一步：STATE / SLICES / CHECKLIST / TESTCASES。*
