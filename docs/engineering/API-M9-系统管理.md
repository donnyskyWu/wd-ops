# API-M9-系统管理

> **版本**：v1.0 | 2026-06-07
> **关联全局规范**：[`GLOBAL-CONVENTIONS.md`](./GLOBAL-CONVENTIONS.md)

---

## 1. 用户 API

### 1.1 GET `/admin-api/system/user/list`

| 参数 | 字典 |
|------|------|
| status | `dict_user_status` |
| deptId | - |
| position | `dict_position` |

### 1.2 POST `/admin-api/system/user/create`

```json
{
  "username": "zhangsan",
  "nickname": "张三",
  "email": "zhangsan@xxx.com",
  "phone": "13800001111",
  "position": "OPS_LEADER",
  "ipGroupId": 1,
  "status": 1
}
```

**校验**：
- `position` `@InDict(type="dict_position")`
- `status` `@InDict(type="dict_user_status")`
- `ipGroupId` `@NotNull`

---

## 2. 角色 API

### 2.1 GET `/admin-api/system/role/list`

### 2.2 POST `/admin-api/system/role/assign-permission`

---

## 3. 租户 API

### 3.1 GET `/admin-api/system/tenant/list`

### 3.2 POST `/admin-api/system/tenant/create`

---

## 4. 字典 API（⭐）

### 4.1 GET `/admin-api/system/dict/type?type={type}`

**响应**：

```json
{
  "dictType": "dict_platform_type",
  "dictName": "平台类型",
  "items": [
    {"value": "WECHAT_OFFICIAL", "label": "公众号", "sort": 1, "status": 0, "colorType": "default"}
  ]
}
```

### 4.2 GET `/admin-api/system/dict/type-list`

字典 type 列表（用于下拉选择 dict_type）

### 4.3 POST `/admin-api/system/dict/create`

```json
{
  "dictType": "dict_value_placeholder",
  "dictName": "xxx",
  "items": [
    {"dictLabel": "xxx", "dictValue": "（具体值详见相应章节）
  ]
}
```

**校验**：
- `dictType` 命名 `dict_value_placeholder`
- `dictValue` 命名 `（具体值详见相应章节）
- `dictType` 唯一

### 4.4 PUT `/admin-api/system/dict/update`

### 4.5 DELETE `/admin-api/system/dict/{id}`

**业务**：停用后可删；启用且被引用 → 错误码 1502

---

## 5. 日志 API

### 5.1 GET `/admin-api/system/log/operation`

| 参数 | 字典 |
|------|------|
| module | `dict_log_module` |
| level | `dict_log_level` |

### 5.2 GET `/admin-api/system/log/login`

---

## 6. 错误码

| 错误码 | 含义 |
|--------|------|
| 1500~1504 | 全局 |
| 1502 | 字典被引用 |
| 2019 | 字典 type 重复 |
| 2020 | 字典 value 重复 |

---

*下一步：STATE / SLICES / CHECKLIST / TESTCASES。*

---

## 字典与数据安全（🔴 API 必含）

### 字典使用（dict-type）

| 字段 | dict-type | 取值约束 |
|------|-----------|----------|
| user_role | `dict_user_role` | 字典合法值，错误码 1503 |
| user_status | `dict_user_status` | 字典合法值，错误码 1503 |
| log_type | `dict_log_type` | 字典合法值，错误码 1503 |

### 数据安全

- **脱敏**：`用户密码（BCrypt 加密）、手机号、邮箱` 在 API 响应中按权限脱敏（前端展示如 `138****1234`）
- **加密**：所有凭证字段在数据库中以 **AES-256** 加密存储（key 由配置中心注入）
- **日志**：所有写操作记录 `sys_audit_log`（含 operatorId / operation / before / after）
- **传输**：HTTPS + JWT；敏感字段二次加密（前端公钥 + 后端私钥）

详见 [`GLOBAL-CONVENTIONS.md § 5`](./GLOBAL-CONVENTIONS.md) (数据安全)

---

## 字段字典映射表（🔴 API 必含）

下表列出 系统管理 模块所有需要走数据字典的字段。前端使用 `<DictSelect dict-type="..." />` 组件，后端 `@InDict` 注解校验。

| 字段 | dict-type | 取值范围 | 错误码（不合法时） |
|------|-----------|----------|--------------------|
| `userStatus` | `dict_user_status` | 启用/停用/锁定 | 1503 |
| `userRole` | `dict_user_role` | 超管/管理员/运营/编辑/查看 | 1503 |
| `logType` | `dict_log_type` | 登录/操作/异常/审计 | 1503 |
| `logLevel` | `dict_log_level` | DEBUG/INFO/WARN/ERROR | 1503 |

### 使用规范

1. **前端**：`<DictSelect v-model="form.userStatus" dict-type="dict_user_status" />`
2. **后端**：`@InDict("dict_user_status") private String userStatus;`
3. **字典维护**：在 M8 配置管理模块维护
4. **错误码**：字典值不在范围内 → 1503

详见 [`GLOBAL-CONVENTIONS.md § 2`](./GLOBAL-CONVENTIONS.md) (字典)
