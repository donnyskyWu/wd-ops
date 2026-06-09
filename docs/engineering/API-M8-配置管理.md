# API-M8-配置管理

> **版本**：v1.0 | 2026-06-07
> **关联全局规范**：[`GLOBAL-CONVENTIONS.md`](./GLOBAL-CONVENTIONS.md)

---

## 1. 内部采集 API

### 1.1 GET `/admin-api/oa/config/internal-collect/list`

### 1.2 POST `/admin-api/oa/config/internal-collect/create`

```json
{
  "platformType": "WECHAT_OFFICIAL",
  "accountId": 123,
  "collectFrequency": "DAILY",
  "collectMethod": "INTERNAL"
}
```

**校验**：
- `platformType` `@InDict(type="dict_platform_type")`
- `accountId` `@NotNull`
- `collectFrequency` `@InDict(type="dict_collect_frequency")`
- `collectMethod` `@InDict(type="dict_collect_method")`

---

## 2. 阈值配置 API

### 2.1 GET `/admin-api/oa/config/threshold/list`

### 2.2 POST `/admin-api/oa/config/threshold/create`

```json
{
  "platformType": "DOUYIN",
  "ipGroupId": 1,
  "metric": "HIT_THRESHOLD",
  "threshold": 1000000
}
```

---

## 3. AI 模型 API

### 3.1 POST `/admin-api/oa/config/ai-model/create`

```json
{
  "modelName": "gpt-4",
  "apiKey": "sk-...",   // 加密
  "baseUrl": "https://api.openai.com",
  "maxTokens": 4096,
  "temperature": 0.7
}
```

---

*下一步：STATE / SLICES / CHECKLIST / TESTCASES。*

---

## 字典与数据安全（🔴 API 必含）

### 字典使用（dict-type）

| 字段 | dict-type | 取值约束 |
|------|-----------|----------|
| * (所有字典的元数据) | `dict_* (所有字典的元数据)` | 字典合法值，错误码 1503 |

### 数据安全

- **脱敏**：`系统配置中的密钥、API Token` 在 API 响应中按权限脱敏（前端展示如 `138****1234`）
- **加密**：所有凭证字段在数据库中以 **AES-256** 加密存储（key 由配置中心注入）
- **日志**：所有写操作记录 `sys_audit_log`（含 operatorId / operation / before / after）
- **传输**：HTTPS + JWT；敏感字段二次加密（前端公钥 + 后端私钥）

详见 [`GLOBAL-CONVENTIONS.md § 5`](./GLOBAL-CONVENTIONS.md) (数据安全)

---

## 字段字典映射表（🔴 API 必含）

下表列出 配置管理 模块所有需要走数据字典的字段。前端使用 `<DictSelect dict-type="..." />` 组件，后端 `@InDict` 注解校验。

| 字段 | dict-type | 取值范围 | 错误码（不合法时） |
|------|-----------|----------|--------------------|
| `configType` | `dict_config_type` | 系统/业务/集成/界面 | 1503 |
| `valueType` | `dict_value_type` | 字符串/数字/布尔/JSON | 1503 |
| `effectiveRange` | `dict_effective_range` | 全局/IP组/用户/作者 | 1503 |

### 使用规范

1. **前端**：`<DictSelect v-model="form.configType" dict-type="dict_config_type" />`
2. **后端**：`@InDict("dict_config_type") private String configType;`
3. **字典维护**：在 M8 配置管理模块维护
4. **错误码**：字典值不在范围内 → 1503

详见 [`GLOBAL-CONVENTIONS.md § 2`](./GLOBAL-CONVENTIONS.md) (字典)
